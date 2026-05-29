package com.template.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeDoc implements Serializable {

    private String id;
    private String knowledgeId;
    private String fileName;
    private String docType;
    private long fileSize;
    private String status; // pending, processing, completed, failed
    private int chunkCount;
    private LocalDateTime createdAt;
}
