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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    UserRepository userRepository;
    NoolRepository nolRepository;
    ContributionRepository contributionRepository;

    public UserService(UserRepository userRepository, NoolRepository nolRepository, ContributionRepository contributionRepository) {
        this.userRepository = userRepository;
        this.nolRepository = nolRepository;
        this.contributionRepository = contributionRepository;
    }

    public UserProfileResponse getUserProfile(long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NullUserException("User does not exists!");
        }

        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(user.getId());
        userProfileResponse.setName(user.getName());
        userProfileResponse.setEmail(user.getEmail());
        userProfileResponse.setBio(user.getBio());
        userProfileResponse.setCreatedAt(user.getCreatedAt());
        List<Nool> threadsCreatedByCurrUser = nolRepository.findByCreatedBy(user);
        List<Contribution> contributionsOfCurrUser = contributionRepository.findByUser(user);

        List<ThreadSummary> threadsOfCurrUser = new ArrayList<ThreadSummary>();
        List<ContributionResponse> contributionsOfCurrUserResponse = new ArrayList<ContributionResponse>();
        for(Nool nool : threadsCreatedByCurrUser){
            List<Contribution> noolContributions =  contributionRepository.findByNool(nool);
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

            Map<String, List<String>> rolesWithContributors = new LinkedHashMap<String, List<String>>();

            for(Contribution contribution : noolContributions) {
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
            threadsOfCurrUser.add(threadSummary);

        }

        for(Contribution contribution : contributionsOfCurrUser){
            ContributionResponse contributionResponse = new ContributionResponse();
            contributionResponse.setId(contribution.getId());
            contributionResponse.setRole(contribution.getRole());
            contributionResponse.setNoolId(contribution.getNool().getId());
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
}
