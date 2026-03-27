package com.aalap.aalapbackend.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ThreadResponse {
    long id;
    String title;
    String description;
    UserInfo createdBy;
    Date createdAt;
    List<ContributionResponse> contributions;
    private String masterAudioUrl;
    private Integer bpm;
    private String musicalKey;
}
