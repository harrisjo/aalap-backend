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

    private final ContributionRepository contributionRepository;
    private final NoolRepository noolRepository;
    private final Cloudinary cloudinary;

    @Autowired
    public ContributionService(ContributionRepository contributionRepository,
                               NoolRepository noolRepository,
                               Cloudinary cloudinary) {
        this.contributionRepository = contributionRepository;
        this.noolRepository = noolRepository;
        this.cloudinary = cloudinary;
    }

    public ContributionResponse addContribution(long noolId, String role, String description,
                                                MultipartFile file, Integer bpm, String musicalKey) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread not found!"));

        if (role != null && role.trim().equalsIgnoreCase("Composer")) {
            List<Contribution> existing = contributionRepository.findByNool(nool);
            for (Contribution c : existing) {
                if (c.getRole() != null && c.getRole().trim().equalsIgnoreCase("Composer")) {
                    throw new DuplicateRoleException("This session already has a Composer.");
                }
            }
            nool.setBpm(bpm);
            nool.setMusicalKey(musicalKey);
            noolRepository.save(nool);
        }

        // Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();
        String resourceType = uploadResult.get("resource_type").toString(); // Get Cloudinary's type

        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(cloudUrl);
        contribution.setFileType(resourceType); // Save "video" or "raw"

        Contribution saved = contributionRepository.save(contribution);
        return toResponse(saved);
    }

    public void deleteContribution(long contributionId) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found"));

        if ((long) contribution.getUser().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own contributions");
        }

        if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
            String publicId = extractPublicId(contribution.getFilePath());
            // Use the type stored in DB for safe deletion
            String rType = contribution.getFileType() != null ? contribution.getFileType() : "auto";
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", rType));
        }

        contributionRepository.deleteById(contributionId);
    }

    public ContributionResponse reuploadContributionFile(long contributionId, MultipartFile newFile) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found"));

        if ((long) contribution.getUser().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own contributions");
        }

        // Destroy old file
        if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
            String oldPublicId = extractPublicId(contribution.getFilePath());
            String oldRType = contribution.getFileType() != null ? contribution.getFileType() : "auto";
            cloudinary.uploader().destroy(oldPublicId, ObjectUtils.asMap("resource_type", oldRType));
        }

        // Upload new and get new type
        Map uploadResult = cloudinary.uploader().upload(newFile.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        contribution.setFilePath(uploadResult.get("secure_url").toString());
        contribution.setFileType(uploadResult.get("resource_type").toString());

        Contribution saved = contributionRepository.save(contribution);
        return toResponse(saved);
    }

    private String extractPublicId(String cloudUrl) {
        String afterUpload = cloudUrl.split("/upload/")[1];
        if (afterUpload.matches("v\\d+/.*")) { afterUpload = afterUpload.replaceFirst("v\\d+/", ""); }
        int dotIndex = afterUpload.lastIndexOf('.');
        if (dotIndex > 0) { afterUpload = afterUpload.substring(0, dotIndex); }
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
        response.setFileType(c.getFileType()); // Map to DTO
        response.setNoolId(c.getNool().getId());
        response.setCreatedAt(c.getCreatedAt());
        return response;
    }
}