package com.template.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String name;
    private String description;
    private Integer docCount;
    private Integer totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
