package com.aalap.aalapbackend.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ContributionResponse {
    long id;
    UserInfo user;
    String role;
    long noolId;
    String description;
    String filePath;
    String fileType; // NEW FIELD
    Date createdAt;
}