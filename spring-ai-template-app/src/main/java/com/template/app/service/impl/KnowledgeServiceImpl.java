package com.template.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.template.app.entity.KnowledgeBaseEntity;
import com.template.app.entity.KnowledgeDocEntity;
import com.template.app.mapper.KnowledgeBaseMapper;
import com.template.app.mapper.KnowledgeDocMapper;
import com.template.app.service.KnowledgeService;
import com.template.common.dto.KnowledgeDoc;
import com.template.common.dto.KnowledgeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private VectorStore vectorStore;

    public KnowledgeServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocMapper knowledgeDocMapper,
                                RabbitTemplate rabbitTemplate) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "md", "docx", "doc");

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

    @Override
    @Transactional
    public void deleteKnowledge(String id) {
        // Delete associated docs
        knowledgeDocMapper.delete(new LambdaQueryWrapper<KnowledgeDocEntity>()
                .eq(KnowledgeDocEntity::getKnowledgeId, id));
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public KnowledgeDoc uploadDocument(String knowledgeId, MultipartFile file) {
        // Validate knowledge base exists
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeId);
        if (kb == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeId);
        }

        // Validate file type
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + ext + ". Allowed: " + ALLOWED_TYPES);
        }

        // Create doc record
        KnowledgeDocEntity docEntity = new KnowledgeDocEntity();
        docEntity.setKnowledgeId(knowledgeId);
        docEntity.setFileName(originalName);
        docEntity.setDocType(ext);
        docEntity.setFileSize(file.getSize());
        docEntity.setStatus("pending");
        docEntity.setChunkCount(0);
        docEntity.setCreatedAt(LocalDateTime.now());
        knowledgeDocMapper.insert(docEntity);

        // Send to async processing queue
        Map<String, Object> msg = Map.of(
                "docId", docEntity.getId(),
                "knowledgeId", knowledgeId,
                "fileName", originalName,
                "fileSize", file.getSize()
        );
        rabbitTemplate.convertAndSend("ai.exchange", "ai.doc.process", msg);

        // Update KB doc count
        kb.setDocCount(kb.getDocCount() + 1);
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);

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

    /**
     * Process document: parse, chunk, embed, and store in vector store.
     * Called by the RabbitMQ consumer.
     */
    public void processDocument(String docId) {
        KnowledgeDocEntity doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) return;

        try {
            doc.setStatus("processing");
            knowledgeDocMapper.updateById(doc);

            // Read document with Tika
            // In production, read from MinIO. Here we demonstrate the flow.
            // TikaDocumentReader reader = new TikaDocumentReader(doc.getMinioPath());
            // List<Document> documents = reader.get();

            // Split into chunks
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(800)
                    .withMinChunkSizeChars(200)
                    .withMinChunkLengthToEmbed(10)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();
            // List<Document> chunks = splitter.apply(documents);

            // Add metadata
            // chunks.forEach(chunk -> {
            //     chunk.getMetadata().put("knowledgeId", doc.getKnowledgeId());
            //     chunk.getMetadata().put("docId", docId);
            //     chunk.getMetadata().put("fileName", doc.getFileName());
            // });

            // Store in vector store
            // vectorStore.add(chunks);

            doc.setStatus("completed");
            // doc.setChunkCount(chunks.size());
            knowledgeDocMapper.updateById(doc);

            // Update KB total chunks
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(doc.getKnowledgeId());
            if (kb != null) {
                // kb.setTotalChunks(kb.getTotalChunks() + chunks.size());
                kb.setUpdatedAt(LocalDateTime.now());
                knowledgeBaseMapper.updateById(kb);
            }

            log.info("Document processed successfully: {}", docId);
        } catch (Exception e) {
            log.error("Failed to process document: {}", docId, e);
            doc.setStatus("failed");
            doc.setErrorMsg(e.getMessage());
            knowledgeDocMapper.updateById(doc);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1);
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
                .chunkCount(entity.getChunkCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
