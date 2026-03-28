package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.ContributionResponse;
import com.aalap.aalapbackend.dto.ThreadSummary;
import com.aalap.aalapbackend.dto.UserInfo;
import com.aalap.aalapbackend.dto.UserProfileResponse;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NullUserException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import com.aalap.aalapbackend.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final NoolRepository nolRepository;
    private final ContributionRepository contributionRepository;
    private final NoolService noolService;
    private final ContributionService contributionService;

    public UserService(UserRepository userRepository,
                       NoolRepository nolRepository,
                       ContributionRepository contributionRepository,
                       @Lazy NoolService noolService,
                       @Lazy ContributionService contributionService) {
        this.userRepository = userRepository;
        this.nolRepository = nolRepository;
        this.contributionRepository = contributionRepository;
        this.noolService = noolService;
        this.contributionService = contributionService;
    }

    public UserProfileResponse getUserProfile(long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NullUserException("User does not exist!");
        }

        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(user.getId());
        userProfileResponse.setName(user.getName());
        userProfileResponse.setEmail(user.getEmail());
        userProfileResponse.setBio(user.getBio());
        userProfileResponse.setCreatedAt(user.getCreatedAt());

        List<Nool> threadsCreatedByCurrUser = nolRepository.findByCreatedBy(user);
        List<Contribution> contributionsOfCurrUser = contributionRepository.findByUser(user);

        List<ThreadSummary> threadsOfCurrUser = new ArrayList<>();
        List<ContributionResponse> contributionsOfCurrUserResponse = new ArrayList<>();

        for (Nool nool : threadsCreatedByCurrUser) {
            List<Contribution> noolContributions = contributionRepository.findByNool(nool);
            ThreadSummary threadSummary = new ThreadSummary();
            threadSummary.setId(nool.getId());
            threadSummary.setTitle(nool.getTitle());
            threadSummary.setDescription(nool.getDescription());
            threadSummary.setCreatedAt(nool.getCreatedAt());
            threadSummary.setContributionCount(noolContributions.size());

            UserInfo userInfo = new UserInfo();
            userInfo.setId(nool.getCreatedBy().getId());
            userInfo.setName(nool.getCreatedBy().getName());
            userInfo.setEmail(nool.getCreatedBy().getEmail());
            threadSummary.setCreatedBy(userInfo);

            Map<String, List<String>> rolesWithContributors = new LinkedHashMap<>();

            for (Contribution contribution : noolContributions) {
                String role = contribution.getRole();
                String name = contribution.getUser().getName();
                if (rolesWithContributors.containsKey(role)) {
                    rolesWithContributors.get(role).add(name);
                } else {
                    List<String> names = new ArrayList<>();
                    names.add(name);
                    rolesWithContributors.put(role, names);
                }
            }
            threadSummary.setRolesWithContributors(rolesWithContributors);
            threadsOfCurrUser.add(threadSummary);
        }

        for (Contribution contribution : contributionsOfCurrUser) {
            ContributionResponse contributionResponse = new ContributionResponse();
            contributionResponse.setId(contribution.getId());
            contributionResponse.setRole(contribution.getRole());
            contributionResponse.setNoolId(contribution.getNool().getId());
            contributionResponse.setNoolTitle(contribution.getNool().getTitle());
            contributionResponse.setDescription(contribution.getDescription());
            contributionResponse.setFilePath(contribution.getFilePath());
            contributionResponse.setCreatedAt(contribution.getCreatedAt());

            UserInfo userInfo = new UserInfo();
            userInfo.setId(contribution.getUser().getId());
            userInfo.setName(contribution.getUser().getName());
            userInfo.setEmail(contribution.getUser().getEmail());
            contributionResponse.setUser(userInfo);
            contributionsOfCurrUserResponse.add(contributionResponse);
        }

        userProfileResponse.setContributions(contributionsOfCurrUserResponse);
        userProfileResponse.setThreadsCreated(threadsOfCurrUser);
        return userProfileResponse;
    }

    // ─── LEAVE AALAP (DELETE ACCOUNT) ─────────────────────────────────────────────

    @Transactional(readOnly = false)
    public void deleteUserAccount() throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 1. Delete all threads created by the user
        // This leverages NoolService to wipe master files and all stems inside these threads.
        List<Nool> userThreads = nolRepository.findByCreatedBy(user);
        for (Nool thread : userThreads) {
            noolService.deleteNool(thread.getId());
        }

        // 2. Delete the user's remaining stems (contributions in OTHER people's threads)
        // This leverages ContributionService to safely delete the Cloudinary files and handle role cleanups.
        List<Contribution> userContributions = contributionRepository.findByUser(user);
        for (Contribution contribution : userContributions) {
            contributionService.deleteContribution(contribution.getId());
        }

        // 3. Delete the user record itself
        userRepository.delete(user);
    }
}