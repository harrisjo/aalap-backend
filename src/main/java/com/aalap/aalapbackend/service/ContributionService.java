package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.dto.UserInfo;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.DuplicateRoleException;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ContributionService {

    ContributionRepository contributionRepository;
    NoolRepository noolRepository;
    Cloudinary cloudinary;

    @Autowired
    public ContributionService(ContributionRepository contributionRepository,
                               NoolRepository noolRepository,
                               Cloudinary cloudinary) {
        this.contributionRepository = contributionRepository;
        this.noolRepository = noolRepository;
        this.cloudinary = cloudinary;
    }

    // ─── ADD CONTRIBUTION ────────────────────────────────────────────────────────

    public ContributionResponse addContribution(long noolId, String role, String description,
                                                MultipartFile file, Integer bpm, String musicalKey) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread not found!"));

        if (role != null && role.trim().equalsIgnoreCase("Composer")) {
            List<Contribution> existing = contributionRepository.findByNool(nool);
            for (Contribution c : existing) {
                if (c.getRole() != null && c.getRole().trim().equalsIgnoreCase("Composer")) {
                    throw new DuplicateRoleException("This session already has a Composer. Only one Composer is allowed per track!");
                }
            }
            nool.setBpm(bpm);
            nool.setMusicalKey(musicalKey);
            noolRepository.save(nool);
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();

        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(cloudUrl);
        Contribution saved = contributionRepository.save(contribution);

        return toResponse(saved);
    }

    // ─── DELETE CONTRIBUTION ─────────────────────────────────────────────────────

    public void deleteContribution(long contributionId) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found"));

        // FIX 1: Safely cast to primitive 'long' to avoid object reference mismatch bugs
        if ((long) contribution.getUser().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own contributions");
        }

        // If this was the Composer stem, clear BPM and key from the thread
        if (contribution.getRole() != null && contribution.getRole().trim().equalsIgnoreCase("Composer")) {
            Nool nool = contribution.getNool();
            nool.setBpm(null);
            nool.setMusicalKey(null);
            noolRepository.save(nool);
        }

        // FIX 2: Explicitly tell Cloudinary what type of file it is destroying
        if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
            String publicId = extractPublicId(contribution.getFilePath());
            String rType = extractResourceType(contribution.getFilePath()); // New helper

            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", rType));
        }

        // Delete from DB
        contributionRepository.deleteById(contributionId);
    }

    // ─── REUPLOAD CONTRIBUTION FILE ───────────────────────────────────────────────

    public ContributionResponse reuploadContributionFile(long contributionId, MultipartFile newFile) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found"));

        // FIX 1: Safe ID comparison
        if ((long) contribution.getUser().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own contributions");
        }

        // FIX 2: Explicitly tell Cloudinary the correct resource type to destroy
        if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
            String oldPublicId = extractPublicId(contribution.getFilePath());
            String oldRType = extractResourceType(contribution.getFilePath());

            cloudinary.uploader().destroy(oldPublicId, ObjectUtils.asMap("resource_type", oldRType));
        }

        // Upload new file to Cloudinary (Upload STILL supports "auto")
        Map uploadResult = cloudinary.uploader().upload(newFile.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String newCloudUrl = uploadResult.get("secure_url").toString();

        // Update file path in DB
        contribution.setFilePath(newCloudUrl);
        Contribution saved = contributionRepository.save(contribution);

        return toResponse(saved);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────────

    // NEW HELPER: Cloudinary stores audio files as "video" and text files as "raw".
    // We parse the URL to find out which one it is so we can delete it safely.
    private String extractResourceType(String cloudUrl) {
        if (cloudUrl.contains("/video/")) {
            return "video"; // Audio files
        } else if (cloudUrl.contains("/raw/")) {
            return "raw";   // Text files like lyrics
        }
        return "image";     // Default fallback
    }

    /**
     * Extracts the Cloudinary public_id from a secure_url.
     * e.g. https://res.cloudinary.com/mycloud/video/upload/v1234567890/filename.mp3
     * → filename
     */
    private String extractPublicId(String cloudUrl) {
        String afterUpload = cloudUrl.split("/upload/")[1];
        // Strip version prefix like "v1234567890/"
        if (afterUpload.matches("v\\d+/.*")) {
            afterUpload = afterUpload.replaceFirst("v\\d+/", "");
        }
        // Strip file extension
        int dotIndex = afterUpload.lastIndexOf('.');
        if (dotIndex > 0) {
            afterUpload = afterUpload.substring(0, dotIndex);
        }
        return afterUpload;
    }

    private ContributionResponse toResponse(Contribution c) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(c.getUser().getId());
        userInfo.setName(c.getUser().getName());
        userInfo.setEmail(c.getUser().getEmail());

        ContributionResponse response = new ContributionResponse();
        response.setId(c.getId());
        response.setUser(userInfo);
        response.setRole(c.getRole());
        response.setDescription(c.getDescription());
        response.setFilePath(c.getFilePath());
        response.setNoolId(c.getNool().getId());
        response.setCreatedAt(c.getCreatedAt());
        return response;
    }
}