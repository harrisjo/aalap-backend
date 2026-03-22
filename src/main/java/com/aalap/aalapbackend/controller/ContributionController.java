package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.service.ContributionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/threads")
public class ContributionController {
    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping(value = "/{threadId}/contributions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContributionResponse createContribution(
            @PathVariable long threadId,
            @RequestParam String role,
            @RequestParam String description,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Integer bpm,
            @RequestParam(required = false) String musicalKey) throws IOException {

        return contributionService.addContribution(threadId, role, description, file, bpm, musicalKey);
    }
}