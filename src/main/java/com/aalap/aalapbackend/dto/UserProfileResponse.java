package com.aalap.aalapbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileResponse {
    long id;
    String name;
    String email;
    String bio;
    LocalDateTime createdAt;
    List<ThreadSummary> threadsCreated;
    List<ContributionResponse>  contributions;
}
