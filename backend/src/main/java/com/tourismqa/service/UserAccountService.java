package com.tourismqa.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourismqa.dto.ChangePasswordRequest;
import com.tourismqa.dto.UpdateUserPreferencesRequest;
import com.tourismqa.dto.UpdateUserProfileRequest;
import com.tourismqa.dto.UserProfileResponse;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

/**
 * 用户资料与偏好维护服务。
 * 使用场景：
 * 为用户端个人中心提供资料查看、资料更新、密码修改与偏好设置能力。
 * 核心职责：
 * 1. 校验当前登录用户身份并装载用户实体。
 * 2. 维护用户资料、口令与偏好字段的一致性。
 * 3. 将用户实体映射为前端可直接消费的资料响应结构。
 */
@Service
public class UserAccountService {

    private static final Set<String> MEMORY_STRATEGIES = Set.of(
            "STANDARD",
            "RECENT_ONLY",
            "PRIVACY_FIRST"
    );

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(UserPrincipal principal) {
        return toProfileResponse(requireUser(principal));
    }

    @Transactional
    public UserProfileResponse updateProfile(UserPrincipal principal, UpdateUserProfileRequest request) {
        UserAccount user = requireUser(principal);
        String displayName = request.displayName().trim();
        String email = normalizeEmail(request.email());

        if (email != null && userAccountRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "邮箱已被其他账号使用");
        }

        user.setDisplayName(displayName);
        user.setEmail(email);
        return toProfileResponse(userAccountRepository.save(user));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        UserAccount user = requireUser(principal);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "当前密码不正确");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "新密码不能与当前密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);
    }

    @Transactional
    public UserProfileResponse updatePreferences(UserPrincipal principal, UpdateUserPreferencesRequest request) {
        UserAccount user = requireUser(principal);
        user.setPreferredDeparture(trimToNull(request.preferredDeparture()));
        user.setBudgetPreference(trimToNull(request.budgetPreference()));
        user.setTravelPreferences(trimToNull(request.travelPreferences()));
        user.setInterestTags(joinInterestTags(request.interestTags()));
        user.setMemoryStrategy(normalizeMemoryStrategy(request.memoryStrategy()));
        return toProfileResponse(userAccountRepository.save(user));
    }

    public UserProfileResponse toProfileResponse(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getRole().name().equals("ADMIN"),
                user.getPreferredDeparture(),
                user.getBudgetPreference(),
                user.getTravelPreferences(),
                splitInterestTags(user.getInterestTags()),
                normalizeMemoryStrategy(user.getMemoryStrategy()),
                user.isBanned()
        );
    }

    public List<String> splitInterestTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private UserAccount requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "请先登录");
        }
        return userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户不存在"));
    }

    private String normalizeMemoryStrategy(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!MEMORY_STRATEGIES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "不支持的记忆策略");
        }
        return normalized;
    }

    private String joinInterestTags(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Set<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("[,，]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .forEach(tags::add);
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
