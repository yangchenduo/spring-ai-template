package com.template.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KnowledgeInfo implements Serializable {

    private String id;
    private String name;
    private String description;
    private int docCount;
    private int totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
