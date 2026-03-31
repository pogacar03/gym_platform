package com.graduation.fitmate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String displayName;
    @NotBlank
    private String password;
}

