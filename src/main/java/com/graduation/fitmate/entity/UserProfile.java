package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer age;
    private String gender;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String fitnessGoal;
    private String activityLevel;
    private String availableEquipment;
    private String injuryNotes;
    private Boolean kneeSensitive;
    private Boolean backSensitive;
    private String exercisePreference;
    private String posturePreference;
    private String targetAreas;
    private Integer weeklyFrequency;
    private Integer preferredDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
