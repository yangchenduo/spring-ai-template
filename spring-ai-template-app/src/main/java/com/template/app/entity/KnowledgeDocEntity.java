package com.template.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_doc")
public class KnowledgeDocEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String knowledgeId;
    private String fileName;
    private String docType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private String storagePath;
    private String errorMsg;
    private LocalDateTime createdAt;
}
