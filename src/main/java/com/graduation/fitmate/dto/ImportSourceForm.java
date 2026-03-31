package com.graduation.fitmate.dto;

import lombok.Data;

@Data
public class ImportSourceForm {
    private String name;
    private String sourceType = "YOUTUBE_CHANNEL";
    private String externalId;
    private String channelName;
    private String defaultGoal;
    private String defaultEquipment;
    private String defaultPosture;
    private Boolean autoApproveConfident = false;
    private Boolean enabled = true;
}

