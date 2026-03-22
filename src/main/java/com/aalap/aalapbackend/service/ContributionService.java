package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.dto.UserInfo;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ContributionService {
    ContributionRepository contributionRepository;
    NoolRepository noolRepository;
    Cloudinary cloudinary; // 1. Injecting our new Cloudinary Bean

    @Autowired
    public ContributionService(ContributionRepository contributionRepository, NoolRepository noolRepository, Cloudinary cloudinary) {
        this.contributionRepository = contributionRepository;
        this.noolRepository = noolRepository;
        this.cloudinary = cloudinary;
    }

    public ContributionResponse addContribution(long noolId, String role, String description, MultipartFile file, Integer bpm, String musicalKey) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId).orElse(null);
        if(nool == null){
            throw new NoolNotFoundException("Thread not found!!");
        }

        // --- NEW: COMPOSER VALIDATION & SETTINGS ---
        if (role != null && role.trim().equalsIgnoreCase("Composer")) {

            // 1. Check if a composer already exists in this thread
            List<Contribution> existingContributions = contributionRepository.findByNool(nool);
            for (Contribution c : existingContributions) {
                if (c.getRole() != null && c.getRole().trim().equalsIgnoreCase("Composer")) {
                    throw new RuntimeException("This session already has a Composer. Only one Composer is allowed per track!");
                }
            }

            // 2. If no composer exists, save the Tempo and Scale to the master Thread
            nool.setBpm(bpm);
            nool.setMusicalKey(musicalKey);
            noolRepository.save(nool);
        }

        // 3. Upload the file to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String cloudUrl = uploadResult.get("secure_url").toString();

        // 4. Save the Contribution exactly like before
        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(cloudUrl);
        Contribution newContribution = contributionRepository.save(contribution);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(newContribution.getUser().getId());
        userInfo.setName(newContribution.getUser().getName());
        userInfo.setEmail(newContribution.getUser().getEmail());

        ContributionResponse contributionResponse = new ContributionResponse();
        contributionResponse.setUser(userInfo);
        contributionResponse.setDescription(newContribution.getDescription());
        contributionResponse.setRole(newContribution.getRole());
        contributionResponse.setFilePath(newContribution.getFilePath());
        contributionResponse.setId(newContribution.getId());
        contributionResponse.setCreatedAt(newContribution.getCreatedAt());
        contributionResponse.setNoolId(newContribution.getNool().getId());

        return contributionResponse;
    }
}