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

    public ContributionResponse addContribution(long noolId, String role, String description, MultipartFile file) throws IOException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId).orElse(null);
        if(nool == null){
            throw new NoolNotFoundException("Thread not found!!");
        }

        // 2. Upload the file to Cloudinary
        // "resource_type", "auto" tells Cloudinary to automatically figure out if it's an mp3, wav, jpeg, or text file
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));

        // 3. Get the permanent streaming URL they give us back
        String cloudUrl = uploadResult.get("secure_url").toString();

        // 4. Save it to MySQL exactly like before!
        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(cloudUrl); // Now storing the permanent Cloudinary URL!
        Contribution newContribution = contributionRepository.save(contribution);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(newContribution.getUser().getId());
        userInfo.setName(newContribution.getUser().getName());
        userInfo.setEmail(newContribution.getUser().getEmail());

        ContributionResponse contributionResponse = new ContributionResponse();
        contributionResponse.setUser(userInfo);
        contributionResponse.setDescription(newContribution.getDescription());
        contributionResponse.setRole(newContribution.getRole());
        contributionResponse.setFilePath(newContribution.getFilePath()); // This will pass the cloud URL to the frontend
        contributionResponse.setId(newContribution.getId());
        contributionResponse.setCreatedAt(newContribution.getCreatedAt());
        contributionResponse.setNoolId(newContribution.getNool().getId());

        return contributionResponse;
    }
}