package com.tourismqa.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourismqa.dto.AdminUserItemResponse;
import com.tourismqa.dto.AdminUserPublishedCommentResponse;
import com.tourismqa.dto.AdminUserPublishedContentResponse;
import com.tourismqa.dto.AdminUserPublishedPostCommentItemResponse;
import com.tourismqa.dto.AdminUserPublishedPostResponse;
import com.tourismqa.dto.AdminUserPublishedRoutePointResponse;
import com.tourismqa.dto.AdminUserPublishedRouteResponse;
import com.tourismqa.dto.AdminUserUpdateRequest;
import com.tourismqa.dto.ExploreSharedRouteResponse;
import com.tourismqa.dto.RoutePointDto;
import com.tourismqa.entity.ExplorePost;
import com.tourismqa.entity.ExplorePostComment;
import com.tourismqa.dto.UpdateUserRoleRequest;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ExplorePostCommentRepository;
import com.tourismqa.repository.ExplorePostRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 管理端用户维护服务。
 * 使用场景：
 * 为后台管理员提供用户列表查询、信息调整与封禁管理能力。
 * 核心职责：
 * 1. 输出后台所需的用户视图数据。
 * 2. 执行角色变更、封禁状态修改与基础资料调整。
 * 3. 统一校验“至少保留一个可用管理员”的系统约束。
 */
@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final ExplorePostRepository explorePostRepository;
    private final ExplorePostCommentRepository explorePostCommentRepository;
    private final ObjectMapper objectMapper;

    public AdminUserService(UserAccountRepository userAccountRepository,
                            ExplorePostRepository explorePostRepository,
                            ExplorePostCommentRepository explorePostCommentRepository,
                            ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.explorePostRepository = explorePostRepository;
        this.explorePostCommentRepository = explorePostCommentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminUserItemResponse> listUsers() {
        return userAccountRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserItemResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        UserAccount user = requireUser(userId);
        validateAdminRetention(user, request.role(), user.isBanned());
        user.setRole(request.role());
        return toResponse(userAccountRepository.save(user));
    }

    @Transactional
    public AdminUserItemResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        UserAccount user = requireUser(userId);

        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "昵称不能为空");
            }
            user.setDisplayName(displayName);
        }

        if (request.email() != null) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "邮箱不能为空");
            }
            if (userAccountRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new ApiException(HttpStatus.CONFLICT.value(), "邮箱已被其他账号使用");
            }
            user.setEmail(email);
        }

        UserRole nextRole = request.role() == null ? user.getRole() : request.role();
        boolean nextBanned = request.banned() == null ? user.isBanned() : request.banned();
        validateAdminRetention(user, nextRole, nextBanned);

        user.setRole(nextRole);
        user.setBanned(nextBanned);
        user.setBanReason(nextBanned ? trimToNull(request.banReason()) : null);

        return toResponse(userAccountRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AdminUserPublishedContentResponse getPublishedContent(Long userId) {
        requireUser(userId);

        List<ExplorePost> publishedPosts = explorePostRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        Map<Long, Integer> commentCountMap = buildCommentCountMap(publishedPosts);
        Map<Long, List<AdminUserPublishedPostCommentItemResponse>> postCommentsMap =
                buildPostCommentsMap(publishedPosts, userId);

        List<AdminUserPublishedPostResponse> posts = publishedPosts.stream()
                .filter(post -> !hasRoute(post))
                .map(post -> new AdminUserPublishedPostResponse(
                        post.getId(),
                        firstNonBlank(post.getTitle(), "未命名帖子"),
                        trimToNull(post.getContent()),
                        deserializeImageUrls(post.getImageUrls()),
                        trimToNull(post.getLocationTag()),
                        post.getCreatedAt(),
                        commentCountMap.getOrDefault(post.getId(), 0),
                        postCommentsMap.getOrDefault(post.getId(), List.of())
                ))
                .toList();

        List<AdminUserPublishedRouteResponse> routes = publishedPosts.stream()
                .filter(this::hasRoute)
                .map(post -> {
                    ExploreSharedRouteResponse route = deserializeRoute(post.getRouteJson());
                    if (route == null) {
                        return null;
                    }
                    return new AdminUserPublishedRouteResponse(
                            post.getId(),
                            firstNonBlank(route.title(), firstNonBlank(post.getTitle(), "未命名路线")),
                            trimToNull(post.getContent()),
                            firstNonBlank(route.summary(), trimToNull(post.getContent())),
                            firstImageUrl(post.getImageUrls()),
                            trimToNull(post.getLocationTag()),
                            firstNonBlank(route.destination(), trimToNull(post.getLocationTag())),
                            route.days() == null ? 1 : route.days(),
                            trimToNull(route.interests()),
                            trimToNull(route.budget()),
                            trimToNull(route.departure()),
                            post.getClickCount(),
                            post.getApplyCount(),
                            post.getCreatedAt(),
                            commentCountMap.getOrDefault(post.getId(), 0),
                            mapRoutePoints(route.points()),
                            route.tips() == null ? List.of() : route.tips().stream()
                                    .map(this::trimToNull)
                                    .filter(tip -> tip != null)
                                    .toList(),
                            postCommentsMap.getOrDefault(post.getId(), List.of())
                    );
                })
                .filter(route -> route != null)
                .sorted(Comparator.comparing(AdminUserPublishedRouteResponse::createdAt).reversed())
                .toList();

        List<AdminUserPublishedCommentResponse> comments = explorePostCommentRepository.findByUserIdWithPost(userId)
                .stream()
                .map(comment -> new AdminUserPublishedCommentResponse(
                        comment.getId(),
                        comment.getPost().getId(),
                        firstNonBlank(
                                trimToNull(comment.getPost().getTitle()),
                                hasRoute(comment.getPost()) ? "未命名路线" : "未命名帖子"
                        ),
                        hasRoute(comment.getPost()),
                        comment.getContent(),
                        comment.getCreatedAt()
                ))
                .toList();

        return new AdminUserPublishedContentResponse(posts, routes, comments);
    }

    private UserAccount requireUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "用户不存在"));
    }

    private Map<Long, Integer> buildCommentCountMap(List<ExplorePost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> commentCountMap = new HashMap<>();
        explorePostCommentRepository.findByPostIdsWithUser(
                posts.stream().map(ExplorePost::getId).toList()
        ).forEach(comment -> commentCountMap.merge(comment.getPost().getId(), 1, Integer::sum));
        return commentCountMap;
    }

    private Map<Long, List<AdminUserPublishedPostCommentItemResponse>> buildPostCommentsMap(
            List<ExplorePost> posts,
            Long userId
    ) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<AdminUserPublishedPostCommentItemResponse>> postCommentsMap = new HashMap<>();
        List<ExplorePostComment> comments = explorePostCommentRepository.findByPostIdsWithUser(
                posts.stream().map(ExplorePost::getId).toList()
        );
        comments.forEach(comment -> postCommentsMap.computeIfAbsent(comment.getPost().getId(), key -> new java.util.ArrayList<>())
                .add(new AdminUserPublishedPostCommentItemResponse(
                        comment.getId(),
                        firstNonBlank(trimToNull(comment.getUser().getDisplayName()), comment.getUser().getUsername()),
                        comment.getUser().getId().equals(userId),
                        comment.getContent(),
                        comment.getCreatedAt()
                )));
        return postCommentsMap;
    }

    private AdminUserItemResponse toResponse(UserAccount user) {
        return new AdminUserItemResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getRole() == UserRole.ADMIN,
                user.isBanned(),
                user.getBanReason(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private void validateAdminRetention(UserAccount currentUser, UserRole nextRole, boolean nextBanned) {
        boolean currentActiveAdmin = currentUser.getRole() == UserRole.ADMIN && !currentUser.isBanned();
        boolean nextActiveAdmin = nextRole == UserRole.ADMIN && !nextBanned;
        if (!currentActiveAdmin || nextActiveAdmin) {
            return;
        }

        long activeAdminCount = userAccountRepository.countByRoleAndBannedFalse(UserRole.ADMIN);
        if (activeAdminCount <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "至少保留一个可用管理员账号");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasRoute(ExplorePost post) {
        return trimToNull(post.getRouteJson()) != null;
    }

    private List<AdminUserPublishedRoutePointResponse> mapRoutePoints(List<RoutePointDto> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        return points.stream()
                .sorted(Comparator.comparingInt(RoutePointDto::day)
                        .thenComparingInt(RoutePointDto::order))
                .map(point -> new AdminUserPublishedRoutePointResponse(
                        point.day(),
                        point.order(),
                        trimToNull(point.name()),
                        trimToNull(point.description())
                ))
                .toList();
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    private ExploreSharedRouteResponse deserializeRoute(String routeJson) {
        String normalized = trimToNull(routeJson);
        if (normalized == null) {
            return null;
        }
        try {
            return objectMapper.readValue(normalized, ExploreSharedRouteResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "共享路线数据读取失败");
        }
    }

    private List<String> deserializeImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> urls = objectMapper.readValue(
                imageUrlsJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return urls == null ? List.of() : urls;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String firstImageUrl(String imageUrlsJson) {
        List<String> urls = deserializeImageUrls(imageUrlsJson);
        return urls.isEmpty() ? null : urls.get(0);
    }
}
