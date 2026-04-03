package com.aalap.aalapbackend.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ThreadSummary {
    long id;
    String title;
    String description;
    UserInfo createdBy;
    Date createdAt;
    int contributionCount;
    Map<String, List<String>> rolesWithContributors;
    List<Long> contributorIds;   // unique contributor user IDs — used for accurate dedup on the client
}
