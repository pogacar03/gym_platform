package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String sourceType;
    private String sourceName;
    private String language;
    private String topic;
    private String tags;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
