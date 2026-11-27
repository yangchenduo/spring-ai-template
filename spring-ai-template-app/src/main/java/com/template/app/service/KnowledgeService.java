package com.template.app.service;

import com.template.common.dto.KnowledgeInfo;
import com.template.common.dto.KnowledgeDoc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeService {

    KnowledgeInfo createKnowledge(String name, String description);

    List<KnowledgeInfo> listKnowledge();

    KnowledgeInfo getKnowledge(String id);

    void deleteKnowledge(String id);

    KnowledgeDoc uploadDocument(String knowledgeId, MultipartFile file);

    List<KnowledgeDoc> listDocuments(String knowledgeId);

    /**
     * Deletes a document together with its embedded chunks and stored raw file.
     */
    KnowledgeDoc deleteDocument(String knowledgeId, String docId);

    /**
     * Re-queues a document for asynchronous processing (retry after failure or re-ingest).
     */
    KnowledgeDoc reprocessDocument(String knowledgeId, String docId);

    /**
     * Async pipeline: parse, chunk, embed and store a document.
     * Called by the RabbitMQ consumer; throws on failure so the message
     * is dead-lettered while the document row is marked failed.
     */
    void processDocument(String docId);
}
