package com.template.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.template.app.config.RabbitMqConfig;
import com.template.app.entity.KnowledgeBaseEntity;
import com.template.app.entity.KnowledgeDocEntity;
import com.template.app.exception.DocumentProcessingException;
import com.template.app.mapper.KnowledgeBaseMapper;
import com.template.app.mapper.KnowledgeDocMapper;
import com.template.app.rag.RagMetadata;
import com.template.app.service.KnowledgeService;
import com.template.app.storage.DocumentStorage;
import com.template.common.dto.KnowledgeDoc;
import com.template.common.dto.KnowledgeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "md", "docx", "doc");
    private static final int MAX_ERROR_MSG_LENGTH = 2000;

    /** OpenAI-compatible providers cap the number of inputs per embeddings request. */
    private static final int EMBEDDING_BATCH_SIZE = 16;

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final RabbitTemplate rabbitTemplate;
    private final VectorStore vectorStore;
    private final DocumentStorage documentStorage;

    public KnowledgeServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocMapper knowledgeDocMapper,
                                RabbitTemplate rabbitTemplate,
                                VectorStore vectorStore,
                                DocumentStorage documentStorage) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.vectorStore = vectorStore;
        this.documentStorage = documentStorage;
    }

    // ---------------------------------------------------------------
    // Knowledge base management
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public KnowledgeInfo createKnowledge(String name, String description) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setDocCount(0);
        entity.setTotalChunks(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.insert(entity);

        return toInfo(entity);
    }

    @Override
    public List<KnowledgeInfo> listKnowledge() {
        List<KnowledgeBaseEntity> list = knowledgeBaseMapper.selectList(null);
        return list.stream().map(this::toInfo).toList();
    }

    @Override
    public KnowledgeInfo getKnowledge(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + id);
        }
        return toInfo(entity);
    }

    /**
     * Deletes the knowledge base together with its documents, stored files and
     * embedded chunks. Intentionally not transactional: the vector store is an
     * external system, so the steps are ordered to be safely re-runnable.
     */
    @Override
    public void deleteKnowledge(String id) {
        // 1. Drop all embedded chunks of this knowledge base
        vectorStore.delete(RagMetadata.KNOWLEDGE_ID + " == '" + id + "'");

        // 2. Drop stored raw files (best effort)
        List<KnowledgeDocEntity> docs = knowledgeDocMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocEntity>()
                        .eq(KnowledgeDocEntity::getKnowledgeId, id));
        docs.forEach(doc -> documentStorage.delete(doc.getStoragePath()));

        // 3. Drop business rows (doc rows are also cascaded by FK)
        knowledgeDocMapper.delete(new LambdaQueryWrapper<KnowledgeDocEntity>()
                .eq(KnowledgeDocEntity::getKnowledgeId, id));
        knowledgeBaseMapper.deleteById(id);

        log.info("Deleted knowledge base {} ({} documents, {} chunks)", id, docs.size(),
                docs.stream().mapToInt(d -> nullSafe(d.getChunkCount())).sum());
    }

    // ---------------------------------------------------------------
    // Document upload / listing / deletion
    // ---------------------------------------------------------------

    /**
     * Stores the raw file, then hands off to the async pipeline via RabbitMQ.
     * Intentionally not transactional: the MQ message must only be sent after
     * the document row is visible to the consumer.
     */
    @Override
    public KnowledgeDoc uploadDocument(String knowledgeId, MultipartFile file) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeId);
        if (kb == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeId);
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + ext + ". Allowed: " + ALLOWED_TYPES);
        }

        // Persist the raw file first - without it the consumer could never read the content
        String storagePath;
        try {
            storagePath = documentStorage.store(knowledgeId, originalName, file.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file: " + originalName, e);
        }

        KnowledgeDocEntity docEntity = new KnowledgeDocEntity();
        docEntity.setKnowledgeId(knowledgeId);
        docEntity.setFileName(originalName);
        docEntity.setDocType(ext);
        docEntity.setFileSize(file.getSize());
        docEntity.setStatus(STATUS_PENDING);
        docEntity.setChunkCount(0);
        docEntity.setStoragePath(storagePath);
        docEntity.setCreatedAt(LocalDateTime.now());
        knowledgeDocMapper.insert(docEntity);

        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_DOC_PROCESS,
                Map.of("docId", docEntity.getId(), "knowledgeId", knowledgeId));

        kb.setDocCount(kb.getDocCount() + 1);
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);

        log.info("Uploaded document {} ({}) to knowledge base {}", docEntity.getId(), originalName, knowledgeId);
        return toDoc(docEntity);
    }

    @Override
    public List<KnowledgeDoc> listDocuments(String knowledgeId) {
        List<KnowledgeDocEntity> list = knowledgeDocMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocEntity>()
                        .eq(KnowledgeDocEntity::getKnowledgeId, knowledgeId)
                        .orderByDesc(KnowledgeDocEntity::getCreatedAt));
        return list.stream().map(this::toDoc).toList();
    }

    @Override
    public KnowledgeDoc deleteDocument(String knowledgeId, String docId) {
        KnowledgeDocEntity doc = requireDoc(knowledgeId, docId);

        // 1. Drop embedded chunks, 2. raw file, 3. business row, 4. refresh counters
        vectorStore.delete(RagMetadata.DOC_ID + " == '" + docId + "'");
        documentStorage.delete(doc.getStoragePath());
        knowledgeDocMapper.deleteById(docId);

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeId);
        if (kb != null) {
            kb.setDocCount(Math.max(0, kb.getDocCount() - 1));
            kb.setTotalChunks(Math.max(0, kb.getTotalChunks() - nullSafe(doc.getChunkCount())));
            kb.setUpdatedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(kb);
        }

        log.info("Deleted document {} from knowledge base {}", docId, knowledgeId);
        return toDoc(doc);
    }

    /**
     * Re-queues a (failed or completed) document for processing. The pipeline
     * removes previous chunks of the document before re-ingesting, so this is
     * safe to call repeatedly.
     */
    @Override
    public KnowledgeDoc reprocessDocument(String knowledgeId, String docId) {
        KnowledgeDocEntity doc = requireDoc(knowledgeId, docId);
        doc.setStatus(STATUS_PENDING);
        doc.setErrorMsg(null);
        doc.setChunkCount(0);
        knowledgeDocMapper.updateById(doc);

        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_DOC_PROCESS,
                Map.of("docId", docId, "knowledgeId", knowledgeId));

        log.info("Re-queued document {} for processing", docId);
        return toDoc(doc);
    }

    // ---------------------------------------------------------------
    // Async processing pipeline (invoked by RabbitMqConsumer)
    // ---------------------------------------------------------------

    @Override
    public void processDocument(String docId) {
        KnowledgeDocEntity doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            log.warn("Ignoring processing request for unknown document: {}", docId);
            return;
        }
        log.info("Processing document {}: {}", docId, doc.getFileName());
        try {
            doc.setStatus(STATUS_PROCESSING);
            doc.setErrorMsg(null);
            knowledgeDocMapper.updateById(doc);

            List<Document> chunks = parseSplitAndEnrich(doc);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("No text content could be extracted from the document");
            }

            // Idempotent re-ingestion: drop previous chunks of this document first
            vectorStore.delete(RagMetadata.DOC_ID + " == '" + docId + "'");
            for (int i = 0; i < chunks.size(); i += EMBEDDING_BATCH_SIZE) {
                vectorStore.add(chunks.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, chunks.size())));
            }

            int previousChunks = nullSafe(doc.getChunkCount());
            doc.setStatus(STATUS_COMPLETED);
            doc.setChunkCount(chunks.size());
            knowledgeDocMapper.updateById(doc);
            updateKbChunkTotal(doc.getKnowledgeId(), chunks.size() - previousChunks);

            log.info("Document {} processed into {} chunks", docId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to process document {}", docId, e);
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMsg(truncate(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            knowledgeDocMapper.updateById(doc);
            // Rethrow so the container rejects the message to the DLQ
            // (spring.rabbitmq.listener.simple.default-requeue-rejected=false)
            throw new DocumentProcessingException(docId, e);
        }
    }

    /**
     * Raw file -> Tika text extraction -> token-based chunking -> metadata stamping.
     * Every chunk carries knowledgeId/docId/fileName (see {@link RagMetadata}).
     */
    private List<Document> parseSplitAndEnrich(KnowledgeDocEntity doc) throws IOException {
        if (doc.getStoragePath() == null || doc.getStoragePath().isBlank()) {
            throw new IllegalStateException(
                    "Document has no stored file - it predates the storage pipeline or the upload failed; "
                            + "re-upload the document");
        }
        Resource resource = documentStorage.load(doc.getStoragePath());
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> parsed = reader.get();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(parsed);

        return chunks.stream()
                .map(chunk -> chunk.mutate()
                        .metadata(RagMetadata.KNOWLEDGE_ID, doc.getKnowledgeId())
                        .metadata(RagMetadata.DOC_ID, doc.getId())
                        .metadata(RagMetadata.FILE_NAME, doc.getFileName())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private KnowledgeDocEntity requireDoc(String knowledgeId, String docId) {
        KnowledgeDocEntity doc = knowledgeDocMapper.selectById(docId);
        if (doc == null || !doc.getKnowledgeId().equals(knowledgeId)) {
            throw new IllegalArgumentException(
                    "Document not found in knowledge base " + knowledgeId + ": " + docId);
        }
        return doc;
    }

    private void updateKbChunkTotal(String knowledgeId, int delta) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeId);
        if (kb != null) {
            kb.setTotalChunks(Math.max(0, kb.getTotalChunks() + delta));
            kb.setUpdatedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(kb);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String truncate(String message) {
        if (message == null) return "unknown error";
        return message.length() <= MAX_ERROR_MSG_LENGTH ? message
                : message.substring(0, MAX_ERROR_MSG_LENGTH) + "...";
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private KnowledgeInfo toInfo(KnowledgeBaseEntity entity) {
        return KnowledgeInfo.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .docCount(entity.getDocCount())
                .totalChunks(entity.getTotalChunks())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private KnowledgeDoc toDoc(KnowledgeDocEntity entity) {
        return KnowledgeDoc.builder()
                .id(entity.getId())
                .knowledgeId(entity.getKnowledgeId())
                .fileName(entity.getFileName())
                .docType(entity.getDocType())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus())
                .chunkCount(nullSafe(entity.getChunkCount()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
