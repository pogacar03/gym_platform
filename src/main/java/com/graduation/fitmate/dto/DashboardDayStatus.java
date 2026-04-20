package com.graduation.fitmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardDayStatus {
    private String label;
    private boolean planned;
    private boolean completed;
}
