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
import com.aalap.aalapbackend.security.TokenBlacklistService;
import com.aalap.aalapbackend.util.GravatarUtil;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final UserRepository userRepository;
    private final NoolRepository nolRepository;
    private final ContributionRepository contributionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final Cloudinary cloudinary;

    public UserService(UserRepository userRepository,
                       NoolRepository nolRepository,
                       ContributionRepository contributionRepository,
                       PasswordEncoder passwordEncoder,
                       TokenBlacklistService tokenBlacklistService,
                       Cloudinary cloudinary) {
        this.userRepository = userRepository;
        this.nolRepository = nolRepository;
        this.contributionRepository = contributionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
        this.cloudinary = cloudinary;
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
        userProfileResponse.setGravatarUrl(GravatarUtil.getUrl(user.getEmail()));
        userProfileResponse.setProfilePicture(user.getProfilePicture());
        userProfileResponse.setCreatedAt(user.getCreatedAt());

        List<Nool> threadsCreatedByCurrUser = nolRepository.findByCreatedBy(user);
        List<Contribution> contributionsOfCurrUser = contributionRepository.findByUser(user);

        // ── Batch-load all contributions for the user's threads in ONE query ──────
        // Then group by nool ID so the loop below is pure in-memory work (no N+1).
        Map<Long, List<Contribution>> contributionsByNoolId = new HashMap<>();
        if (!threadsCreatedByCurrUser.isEmpty()) {
            List<Contribution> threadContributions = contributionRepository.findByNoolIn(threadsCreatedByCurrUser);
            for (Contribution c : threadContributions) {
                contributionsByNoolId
                    .computeIfAbsent(c.getNool().getId(), k -> new ArrayList<>())
                    .add(c);
            }
        }

        List<ThreadSummary> threadsOfCurrUser = new ArrayList<>();
        List<ContributionResponse> contributionsOfCurrUserResponse = new ArrayList<>();

        for (Nool nool : threadsCreatedByCurrUser) {
            List<Contribution> noolContributions = contributionsByNoolId
                    .getOrDefault(nool.getId(), new ArrayList<>());

            ThreadSummary threadSummary = new ThreadSummary();
            threadSummary.setId(nool.getId());
            threadSummary.setTitle(nool.getTitle());
            threadSummary.setDescription(nool.getDescription());
            threadSummary.setCreatedAt(nool.getCreatedAt());
            threadSummary.setContributionCount(noolContributions.size());

            UserInfo userInfo = new UserInfo();
            userInfo.setId(nool.getCreatedBy().getId());
            userInfo.setName(nool.getCreatedBy().getName());
            threadSummary.setCreatedBy(userInfo);

            Map<String, List<String>> rolesWithContributors = new LinkedHashMap<>();
            Set<Long> contributorIds = new LinkedHashSet<>();
            for (Contribution contribution : noolContributions) {
                rolesWithContributors
                    .computeIfAbsent(contribution.getRole(), k -> new ArrayList<>())
                    .add(contribution.getUser().getName());
                contributorIds.add(contribution.getUser().getId());
            }
            threadSummary.setRolesWithContributors(rolesWithContributors);
            threadSummary.setContributorIds(new ArrayList<>(contributorIds));
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
            userInfo.setGravatarUrl(GravatarUtil.getUrl(contribution.getUser().getEmail()));
            userInfo.setProfilePicture(contribution.getUser().getProfilePicture());
            contributionResponse.setUser(userInfo);
            contributionsOfCurrUserResponse.add(contributionResponse);
        }

        userProfileResponse.setContributions(contributionsOfCurrUserResponse);
        userProfileResponse.setThreadsCreated(threadsOfCurrUser);
        return userProfileResponse;
    }

    // ─── UPLOAD PROFILE PICTURE ───────────────────────────────────────────────────

    @Transactional
    public UserProfileResponse updateProfilePicture(MultipartFile file) throws IOException {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG, WebP, or GIF images are accepted for profile pictures");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profile picture must be 5 MB or less");
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("resource_type", "image", "folder", "profile_pictures"));
        String cloudUrl = uploadResult.get("secure_url").toString();

        user.setProfilePicture(cloudUrl);
        userRepository.save(user);

        return getUserProfile(user.getId());
    }

    // ─── REMOVE PROFILE PICTURE ───────────────────────────────────────────────────

    @Transactional
    public void removeProfilePicture() {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setProfilePicture(null);
        userRepository.save(user);
    }

    // ─── LEAVE AALAP (SOFT DELETE) ────────────────────────────────────────────────
    // We anonymize the user's PII instead of deleting their rows.
    // Audio stems and threads are intentionally preserved — other collaborators'
    // master mixes must not be broken when one contributor leaves.

    @Transactional
    public void deleteUserAccount(String password) {
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // ── Re-authentication: verify password before any changes ──────────────
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }

        // ── Blacklist outstanding JWTs immediately ─────────────────────────────
        tokenBlacklistService.invalidate(user.getId());

        // ── Anonymize PII ──────────────────────────────────────────────────────
        user.setName("Deleted User");

        // Replace the real email with a unique non-functioning placeholder.
        // This frees the original address so the person can re-register later.
        // The domain ".invalid" is RFC-2606 reserved and can never exist.
        user.setEmail("deleted_" + user.getId() + "@aalap.invalid");

        user.setBio(null);

        // Overwrite the password hash with a random value so the account
        // can never be unlocked through any credential-guessing attack.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Mark as soft-deleted — blocks login and JWT acceptance via isEnabled().
        user.setDeleted(true);

        userRepository.save(user);

        // ── Threads, contributions, and Cloudinary files are preserved ─────────
        // Stems belong to the collaborative project, not just the uploader.
        // Other musicians can still play, download, and build on them.
    }
}