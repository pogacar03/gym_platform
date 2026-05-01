package com.graduation.fitmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeDocumentForm {
    @NotBlank
    @Size(max = 160)
    private String title;

    @NotBlank
    @Size(max = 32)
    private String sourceType = "GUIDELINE";

    @Size(max = 160)
    private String sourceName;

    @NotBlank
    @Size(max = 16)
    private String language = "zh_CN";

    @Size(max = 64)
    private String topic;

    @Size(max = 255)
    private String tags;

    @NotBlank
    private String content;
}
