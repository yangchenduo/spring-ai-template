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
}
