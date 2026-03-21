package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.*;
import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.NoolNotFoundException;
import com.aalap.aalapbackend.repository.ContributionRepository;
import com.aalap.aalapbackend.repository.NoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NoolService {
    NoolRepository noolRepository;
    ContributionRepository contributionRepository;

    @Autowired
    public NoolService(NoolRepository noolRepository, ContributionRepository contributionRepository) {
        this.noolRepository = noolRepository;
        this.contributionRepository = contributionRepository;
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
}