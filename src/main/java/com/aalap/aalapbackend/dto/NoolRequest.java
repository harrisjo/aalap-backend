package com.aalap.aalapbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoolRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be 200 characters or less")
    private String title;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;
}