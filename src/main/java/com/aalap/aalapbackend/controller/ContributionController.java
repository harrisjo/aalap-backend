package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.service.ContributionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // ─── ADD CONTRIBUTION ────────────────────────────────────────────────────────

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

    // ─── DELETE CONTRIBUTION ─────────────────────────────────────────────────────
    // Only the owner of the contribution can delete it.
    // The service layer verifies ownership — if someone else tries, it throws 403.

    @DeleteMapping("/contributions/{contributionId}")
    public ResponseEntity<Void> deleteContribution(@PathVariable long contributionId) throws IOException {
        contributionService.deleteContribution(contributionId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ─── REUPLOAD CONTRIBUTION FILE ───────────────────────────────────────────────
    // Replaces the audio file of an existing contribution.
    // Role and description stay the same — only the file changes.
    // Only the owner of the contribution can reupload.

    @PutMapping(value = "/contributions/{contributionId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContributionResponse reuploadContributionFile(
            @PathVariable long contributionId,
            @RequestParam MultipartFile file) throws IOException {

        return contributionService.reuploadContributionFile(contributionId, file);
    }
}