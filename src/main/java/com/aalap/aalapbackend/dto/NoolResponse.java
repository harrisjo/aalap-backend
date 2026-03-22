package com.aalap.aalapbackend.dto;

import lombok.Data;

import java.util.Date;

@Data
public class NoolResponse {
    long id;
    String title;
    String description;
    UserInfo createdBy;
    Date createdAt;
    private String masterAudioUrl;
}
