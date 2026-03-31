package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("import_source")
public class ImportSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String sourceType;
    private String externalId;
    private String channelName;
    private String defaultGoal;
    private String defaultEquipment;
    private String defaultPosture;
    private Boolean autoApproveConfident;
    private Boolean enabled;
    private LocalDateTime lastImportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

