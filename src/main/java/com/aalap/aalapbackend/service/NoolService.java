package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.*;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@Transactional
public class NoolService {
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
        userInfo.setEmail(user.getEmail());

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
        noolUserInfo.setEmail(nool.getCreatedBy().getEmail());

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
            userInfo.setEmail(contribution.getUser().getEmail());
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

    public List<ThreadSummary> getAllNools() {
        List<Nool> nools = noolRepository.findAll();

        List<Contribution> allContributions = contributionRepository.findByNoolIn(nools);
        Map<Long, List<Contribution>> contributionsByNoolId = new HashMap<>();
        for(Contribution c : allContributions) {
            Long noolId = c.getNool().getId();
            if(!contributionsByNoolId.containsKey(noolId)) {
                contributionsByNoolId.put(noolId, new ArrayList<>());
            }
            contributionsByNoolId.get(noolId).add(c);
        }

        List<ThreadSummary> threadSummaries = new ArrayList<>();
        for(Nool nool : nools) {
            ThreadSummary threadSummary = new ThreadSummary();
            List<Contribution> contributions = contributionsByNoolId
                    .getOrDefault(nool.getId(), new ArrayList<>());

            threadSummary.setId(nool.getId());
            threadSummary.setTitle(nool.getTitle());
            threadSummary.setDescription(nool.getDescription());
            threadSummary.setCreatedAt(nool.getCreatedAt());
            threadSummary.setContributionCount(contributions.size());

            UserInfo noolUserInfo = new UserInfo();
            noolUserInfo.setId(nool.getCreatedBy().getId());
            noolUserInfo.setName(nool.getCreatedBy().getName());
            noolUserInfo.setEmail(nool.getCreatedBy().getEmail());
            threadSummary.setCreatedBy(noolUserInfo);

            Map<String, List<String>> rolesWithContributors = new LinkedHashMap<>();
            for(Contribution contribution : contributions) {
                String role = contribution.getRole();
                String name = contribution.getUser().getName();
                if(rolesWithContributors.containsKey(role)) {
                    rolesWithContributors.get(role).add(name);
                } else {
                    List<String> names = new ArrayList<>();
                    names.add(name);
                    rolesWithContributors.put(role, names);
                }
            }
            threadSummary.setRolesWithContributors(rolesWithContributors);
            threadSummaries.add(threadSummary);
        }
        return threadSummaries;
    }

    public NoolResponse uploadMasterAudio(Long noolId, MultipartFile file) throws IOException {
        Nool nool = noolRepository.findById(noolId).orElse(null);
        if(nool == null) {
            throw new NoolNotFoundException("Thread does not exist");
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();

        nool.setMasterFilePath(cloudUrl);
        Nool updatedNool = noolRepository.save(nool);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(updatedNool.getCreatedBy().getId());
        userInfo.setName(updatedNool.getCreatedBy().getName());
        userInfo.setEmail(updatedNool.getCreatedBy().getEmail());

        NoolResponse response = new NoolResponse();
        response.setId(updatedNool.getId());
        response.setTitle(updatedNool.getTitle());
        response.setDescription(updatedNool.getDescription());
        response.setCreatedBy(userInfo);
        response.setCreatedAt(updatedNool.getCreatedAt());
        response.setMasterAudioUrl(updatedNool.getMasterFilePath());

        return response;
    }

    // ─── DELETE THREAD ────────────────────────────────────────────────────────
    // Only the creator of the thread can delete it.
    // Deletes all contribution files from Cloudinary, the master mix from
    // Cloudinary, all contribution rows from DB, then the thread itself.

    public void deleteNool(Long noolId) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId)
                .orElseThrow(() -> new NoolNotFoundException("Thread not found"));

        // Only the creator can delete the thread
        if ((long) nool.getCreatedBy().getId() != (long) loggedInUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this thread");
        }

        // 1. Delete all contribution files from Cloudinary
        List<Contribution> contributions = contributionRepository.findByNool(nool);
        for (Contribution contribution : contributions) {
            if (contribution.getFilePath() != null && !contribution.getFilePath().isBlank()) {
                try {
                    String publicId = extractPublicId(contribution.getFilePath());
                    String resourceType = extractResourceType(contribution.getFilePath());
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
                } catch (Exception e) {
                    // Log but don't block deletion if Cloudinary removal fails
                    System.err.println("Could not delete contribution file from Cloudinary: " + e.getMessage());
                }
            }
        }

        // 2. Delete all contributions from DB
        contributionRepository.deleteAll(contributions);

        // 3. Delete master mix from Cloudinary if it exists
        if (nool.getMasterFilePath() != null && !nool.getMasterFilePath().isBlank()) {
            try {
                String publicId = extractPublicId(nool.getMasterFilePath());
                String resourceType = extractResourceType(nool.getMasterFilePath());
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
            } catch (Exception e) {
                System.err.println("Could not delete master file from Cloudinary: " + e.getMessage());
            }
        }

        // 4. Delete the thread from DB
        noolRepository.delete(nool);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

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