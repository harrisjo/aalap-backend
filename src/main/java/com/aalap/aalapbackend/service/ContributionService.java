package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.dto.UserInfo;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ContributionService {
    ContributionRepository contributionRepository;
    NoolRepository noolRepository;
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public ContributionService(ContributionRepository contributionRepository, NoolRepository noolRepository) {
        this.contributionRepository = contributionRepository;
        this.noolRepository = noolRepository;
    }

    public ContributionResponse addContribution(long noolId, String role, String description, MultipartFile file) throws IOException {
        User user = (User)  SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Nool nool = noolRepository.findById(noolId).orElse(null);
        if(nool == null){
            throw new NoolNotFoundException("Thread not found!!");
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath);

        Contribution contribution = new Contribution();
        contribution.setNool(nool);
        contribution.setDescription(description);
        contribution.setUser(user);
        contribution.setRole(role);
        contribution.setFilePath(filePath.toString());
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
