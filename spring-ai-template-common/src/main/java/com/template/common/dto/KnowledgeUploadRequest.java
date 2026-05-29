package com.template.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class KnowledgeUploadRequest implements Serializable {

    @NotBlank(message = "knowledgeName must not be blank")
    private String knowledgeName;

    private String description;

    /**
     * Document type: pdf, txt, md, docx
     */
    private String docType;
}
