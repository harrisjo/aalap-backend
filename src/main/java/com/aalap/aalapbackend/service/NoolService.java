package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.*;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import com.aalap.aalapbackend.util.GravatarUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@Transactional
public class NoolService {

    // ─── Allowed MIME types for audio uploads ─────────────────────────────────
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg",
            "audio/flac", "audio/x-flac", "audio/aac", "audio/mp4",
            "audio/webm", "audio/3gpp"
    );

    NoolRepository noolRepository;
    ContributionRepository contributionRepository;
    Cloudinary cloudinary;

    @Autowired
    public NoolService(NoolRepository noolRepository, ContributionRepository contributionRepository, Cloudinary cloudinary) {
        this.noolRepository = noolRepository;
        this.contributionRepository = contributionRepository;
        this.cloudinary = cloudinary;
    }

    public NoolResponse createNool(NoolRequest noolRequest) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Nool nool = new Nool();
        nool.setTitle(noolRequest.getTitle());
        nool.setDescription(noolRequest.getDescription());
        nool.setCreatedBy(user);
        Nool createdNool = noolRepository.save(nool);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setGravatarUrl(GravatarUtil.getUrl(user.getEmail()));
        userInfo.setProfilePicture(user.getProfilePicture());

        NoolResponse noolResponse = new NoolResponse();
        noolResponse.setId(createdNool.getId());
        noolResponse.setTitle(createdNool.getTitle());
        noolResponse.setDescription(createdNool.getDescription());
        noolResponse.setCreatedBy(userInfo);
        noolResponse.setCreatedAt(createdNool.getCreatedAt());
        noolResponse.setMasterAudioUrl(createdNool.getMasterFilePath());
        return noolResponse;
    }

    public ThreadResponse getNool(Long noolId) {
        Nool nool = noolRepository.findById(noolId).orElse(null);
        if(nool == null) {
            throw new NoolNotFoundException("Thread does not exist");
        }

        UserInfo noolUserInfo = new UserInfo();
        noolUserInfo.setId(nool.getCreatedBy().getId());
        noolUserInfo.setName(nool.getCreatedBy().getName());
        noolUserInfo.setGravatarUrl(GravatarUtil.getUrl(nool.getCreatedBy().getEmail()));
        noolUserInfo.setProfilePicture(nool.getCreatedBy().getProfilePicture());

        List<Contribution> contributions = contributionRepository.findByNool(nool);
        List<ContributionResponse> contributionResponses = new ArrayList<>();

        for(Contribution contribution : contributions) {
            ContributionResponse cr = new ContributionResponse();
            cr.setId(contribution.getId());
            cr.setRole(contribution.getRole());
            cr.setDescription(contribution.getDescription());
            cr.setFilePath(contribution.getFilePath());
            cr.setCreatedAt(contribution.getCreatedAt());
            cr.setNoolId(contribution.getNool().getId());

            UserInfo userInfo = new UserInfo();
            userInfo.setId(contribution.getUser().getId());
            userInfo.setName(contribution.getUser().getName());
            userInfo.setGravatarUrl(GravatarUtil.getUrl(contribution.getUser().getEmail()));
            userInfo.setProfilePicture(contribution.getUser().getProfilePicture());
            cr.setUser(userInfo);

            contributionResponses.add(cr);
        }

        ThreadResponse threadResponse = new ThreadResponse();
        threadResponse.setId(nool.getId());
        threadResponse.setTitle(nool.getTitle());
        threadResponse.setDescription(nool.getDescription());
        threadResponse.setContributions(contributionResponses);
        threadResponse.setCreatedBy(noolUserInfo);
        threadResponse.setCreatedAt(nool.getCreatedAt());
        threadResponse.setMasterAudioUrl(nool.getMasterFilePath());
        threadResponse.setBpm(nool.getBpm());
        threadResponse.setMusicalKey(nool.getMusicalKey());
        return threadResponse;
    }

    public Page<ThreadSummary> getAllNools(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Nool> noolPage  = noolRepository.findAll(pageable);
        List<Nool> nools     = noolPage.getContent();

        if (nools.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        List<Contribution> allContributions = contributionRepository.findByNoolIn(nools);
        Map<Long, List<Contribution>> contributionsByNoolId = new HashMap<>();
        for (Contribution c : allContributions) {
            contributionsByNoolId
                .computeIfAbsent(c.getNool().getId(), k -> new ArrayList<>())
                .add(c);
        }

        List<ThreadSummary> threadSummaries = new ArrayList<>();
        for (Nool nool : nools) {
            List<Contribution> contribs = contributionsByNoolId
                    .getOrDefault(nool.getId(), new ArrayList<>());

            ThreadSummary threadSummary = new ThreadSummary();
            threadSummary.setId(nool.getId());
            threadSummary.setTitle(nool.getTitle());
            threadSummary.setDescription(nool.getDescription());
            threadSummary.setCreatedAt(nool.getCreatedAt());
            threadSummary.setContributionCount(contribs.size());

            UserInfo noolUserInfo = new UserInfo();
            noolUserInfo.setId(nool.getCreatedBy().getId());
            noolUserInfo.setName(nool.getCreatedBy().getName());
            noolUserInfo.setGravatarUrl(GravatarUtil.getUrl(nool.getCreatedBy().getEmail()));
            noolUserInfo.setProfilePicture(nool.getCreatedBy().getProfilePicture());
            threadSummary.setCreatedBy(noolUserInfo);

            Map<String, List<String>> rolesWithContributors = new LinkedHashMap<>();
            Set<Long> contributorIds = new LinkedHashSet<>();
            for (Contribution contribution : contribs) {
                rolesWithContributors
                    .computeIfAbsent(contribution.getRole(), k -> new ArrayList<>())
                    .add(contribution.getUser().getName());
                contributorIds.add(contribution.getUser().getId());
            }
            threadSummary.setRolesWithContributors(rolesWithContributors);
            threadSummary.setContributorIds(new ArrayList<>(contributorIds));
            threadSummaries.add(threadSummary);
        }
        return new PageImpl<>(threadSummaries, pageable, noolPage.getTotalElements());
    }

    public NoolResponse uploadMasterAudio(Long noolId, MultipartFile file) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread does not exist"));

        // Only the creator of the thread can upload the master mix
        if ((long) nool.getCreatedBy().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can upload the master mix");
        }

        // ── File type validation ────────────────────────────────────────────────
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only audio files are accepted for the master mix");
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();

        nool.setMasterFilePath(cloudUrl);
        Nool updatedNool = noolRepository.save(nool);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(updatedNool.getCreatedBy().getId());
        userInfo.setName(updatedNool.getCreatedBy().getName());
        userInfo.setGravatarUrl(GravatarUtil.getUrl(updatedNool.getCreatedBy().getEmail()));

        NoolResponse response = new NoolResponse();
        response.setId(updatedNool.getId());
        response.setTitle(updatedNool.getTitle());
        response.setDescription(updatedNool.getDescription());
        response.setCreatedBy(userInfo);
        response.setCreatedAt(updatedNool.getCreatedAt());
        response.setMasterAudioUrl(updatedNool.getMasterFilePath());

        return response;
    }

    // ─── DELETE THREAD ─────────────────────────────────────────────────────────

    public void deleteNool(Long noolId) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread not found"));

        // Only the creator can delete the thread
        if ((long) nool.getCreatedBy().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this thread");
        }

        List<Contribution> contributions = contributionRepository.findByNool(nool);
        for (Contribution contribution : contributions) {
            if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
                try {
                    String publicId    = extractPublicId(contribution.getFilePath());
                    String resourceType = extractResourceType(contribution.getFilePath());
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
                } catch (Exception e) {
                    log.warn("Could not delete contribution file from Cloudinary: {}", e.getMessage());
                }
            }
        }

        contributionRepository.deleteAll(contributions);

        if (nool.getMasterFilePath() != null && !nool.getMasterFilePath().isBlank()) {
            try {
                String publicId    = extractPublicId(nool.getMasterFilePath());
                String resourceType = extractResourceType(nool.getMasterFilePath());
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
            } catch (Exception e) {
                log.warn("Could not delete master file from Cloudinary: {}", e.getMessage());
            }
        }

        noolRepository.delete(nool);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private String extractResourceType(String cloudUrl) {
        if (cloudUrl.contains("/video/")) return "video";
        if (cloudUrl.contains("/raw/"))  return "raw";
        return "image";
    }

    private String extractPublicId(String cloudUrl) {
        String afterUpload = cloudUrl.split("/upload/")[1];
        if (afterUpload.matches("v\\d+/.*")) {
            afterUpload = afterUpload.replaceFirst("v\\d+/", "");
        }
        int dotIndex = afterUpload.lastIndexOf('.');
        if (dotIndex > 0) {
            afterUpload = afterUpload.substring(0, dotIndex);
        }
        return afterUpload;
    }
}

