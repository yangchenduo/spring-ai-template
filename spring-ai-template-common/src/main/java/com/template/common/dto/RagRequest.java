package com.template.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RagRequest implements Serializable {

    @NotBlank(message = "question must not be blank")
    @Size(max = 4096, message = "question must not exceed 4096 characters")
    private String question;

    /**
     * Knowledge base IDs to search against. If null, searches all.
     */
    private List<String> knowledgeIds;

    /**
     * Top K results to retrieve from vector store
     */
    private Integer topK;

    /**
     * Similarity threshold (0.0 - 1.0)
     */
    private Double similarityThreshold;

    /**
     * AI provider override
     */
    private String provider;
}
