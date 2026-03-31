package com.graduation.fitmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportSourceStats {
    private long total;
    private long pending;
    private long approved;
    private long rejected;
}

