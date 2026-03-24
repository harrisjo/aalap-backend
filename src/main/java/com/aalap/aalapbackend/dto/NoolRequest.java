package com.aalap.aalapbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoolRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
}