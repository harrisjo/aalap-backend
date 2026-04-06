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
import com.aalap.aalapbackend.util.GravatarUtil;
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
import java.util.Set;

@Service
@Transactional
public class ContributionService {

    // ─── Allowed MIME types for contribution files ────────────────────────────
    // Audio types for instruments, vocals, production stems.
    // Text types for lyricists uploading lyrics/sheet notation.
    // No executables, archives, scripts, or binary formats permitted.
    private static final Set<String> ALLOWED_CONTRIBUTION_TYPES = Set.of(
            // Audio
            "audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg",
            "audio/flac", "audio/x-flac", "audio/aac", "audio/mp4",
            "audio/webm", "audio/3gpp",
            // Text (lyrics, sheet notation, tabs)
            "text/plain"
    );

    private final ContributionRepository contributionRepository;
    private final NoolRepository noolRepository;
    private final Cloudinary cloudinary;
    private final EmailService emailService;

    @Autowired
    public ContributionService(ContributionRepository contributionRepository,
                               NoolRepository noolRepository,
                               Cloudinary cloudinary,
                               EmailService emailService) {
        this.contributionRepository = contributionRepository;
        this.noolRepository = noolRepository;
        this.cloudinary = cloudinary;
        this.emailService = emailService;
    }

    public ContributionResponse addContribution(long noolId, String role, String description,
                                                MultipartFile file, Integer bpm, String musicalKey) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread not found!"));

        // ── Input length limits ───────────────────────────────────────────────
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        if (role.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be 100 characters or less");
        }
        if (description != null && description.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must be 2000 characters or less");
        }

        // ── File type validation ──────────────────────────────────────────────
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTRIBUTION_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only audio files (MP3, WAV, FLAC, etc.) or text files are accepted for contributions");
        }

        if (role.trim().equalsIgnoreCase("Composer")) {
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

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();
        String resourceType = uploadResult.get("resource_type").toString();

        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(cloudUrl);
        contribution.setFileType(resourceType);

        Contribution saved = contributionRepository.save(contribution);

        // ── Notify thread owner (fire-and-forget) ─────────────────────────────
        // Don't email if the contributor IS the thread owner.
        User threadOwner = nool.getCreatedBy();
        if (threadOwner.getId() != user.getId()) {
            emailService.sendContributionNotification(
                    threadOwner.getEmail(),
                    threadOwner.getName(),
                    nool.getTitle(),
                    user.getName(),
                    role,
                    nool.getId());
        }

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

        // ── File type validation ──────────────────────────────────────────────
        String contentType = newFile.getContentType();
        if (contentType == null || !ALLOWED_CONTRIBUTION_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only audio files (MP3, WAV, FLAC, etc.) or text files are accepted for contributions");
        }

        if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
            String oldPublicId = extractPublicId(contribution.getFilePath());
            String oldRType = contribution.getFileType() != null ? contribution.getFileType() : "auto";
            cloudinary.uploader().destroy(oldPublicId, ObjectUtils.asMap("resource_type", oldRType));
        }

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
        userInfo.setGravatarUrl(GravatarUtil.getUrl(c.getUser().getEmail()));
        userInfo.setProfilePicture(c.getUser().getProfilePicture());
        // email intentionally omitted — see UserInfo.java

        ContributionResponse response = new ContributionResponse();
        response.setId(c.getId());
        response.setUser(userInfo);
        response.setRole(c.getRole());
        response.setDescription(c.getDescription());
        response.setFilePath(c.getFilePath());
        response.setFileType(c.getFileType());
        response.setNoolId(c.getNool().getId());
        response.setCreatedAt(c.getCreatedAt());
        return response;
    }
}

