package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.service.ContributionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/threads")
public class ContributionController {
    ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping("/{threadId}/contributions")
    public ContributionResponse createContribution(@PathVariable long threadId, @RequestParam String role, @RequestParam String description, @RequestParam MultipartFile file) throws IOException {
       return contributionService.addContribution(threadId, role, description, file);
    }
}
