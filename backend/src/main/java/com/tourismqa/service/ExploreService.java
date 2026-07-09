package com.tourismqa.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.dto.ExplorePostCommentRequest;
import com.tourismqa.dto.ExplorePostCommentResponse;
import com.tourismqa.dto.ExplorePostCreateRequest;
import com.tourismqa.dto.ExplorePostResponse;
import com.tourismqa.dto.ExploreSharedRouteRequest;
import com.tourismqa.dto.ExploreSharedRouteResponse;
import com.tourismqa.dto.RoutePointDto;
import com.tourismqa.dto.RoutePointSaveRequest;
import com.tourismqa.entity.ExplorePost;
import com.tourismqa.entity.ExplorePostComment;
import com.tourismqa.entity.ExplorePostCommentLike;
import com.tourismqa.entity.ExplorePostFavorite;
import com.tourismqa.entity.ExplorePostLike;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ExplorePostCommentRepository;
import com.tourismqa.repository.ExplorePostCommentLikeRepository;
import com.tourismqa.repository.ExplorePostFavoriteRepository;
import com.tourismqa.repository.ExplorePostLikeRepository;
import com.tourismqa.repository.ExplorePostRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

@Service
public class ExploreService {

    private final ExplorePostRepository explorePostRepository;
    private final ExplorePostLikeRepository explorePostLikeRepository;
    private final ExplorePostFavoriteRepository explorePostFavoriteRepository;
    private final ExplorePostCommentRepository explorePostCommentRepository;
    private final ExplorePostCommentLikeRepository explorePostCommentLikeRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    public ExploreService(ExplorePostRepository explorePostRepository,
                          ExplorePostLikeRepository explorePostLikeRepository,
                          ExplorePostFavoriteRepository explorePostFavoriteRepository,
                          ExplorePostCommentRepository explorePostCommentRepository,
                          ExplorePostCommentLikeRepository explorePostCommentLikeRepository,
                          UserAccountRepository userAccountRepository,
                          ObjectMapper objectMapper) {
        this.explorePostRepository = explorePostRepository;
        this.explorePostLikeRepository = explorePostLikeRepository;
        this.explorePostFavoriteRepository = explorePostFavoriteRepository;
        this.explorePostCommentRepository = explorePostCommentRepository;
        this.explorePostCommentLikeRepository = explorePostCommentLikeRepository;
        this.userAccountRepository = userAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ExplorePostResponse> listPosts(UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        return buildPostResponses(explorePostRepository.findFeed(), user.getId());
    }

    @Transactional
    public ExplorePostResponse createPost(ExplorePostCreateRequest request, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        String title = normalizeTitle(request.title(), request.content(), request.route());
        String content = trimToNull(request.content());
        String imageUrlsJson = normalizeImageUrls(request.imageUrls());
        ExploreSharedRouteResponse route = normalizeRoute(request.route());
        if (title == null && content == null && route == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "帖子内容和分享路线不能同时为空");
        }

        ExplorePost post = new ExplorePost();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrls(imageUrlsJson);
        post.setLocationTag(route == null ? null : route.destination());
        post.setRouteJson(serializeRoute(route));
        ExplorePost saved = explorePostRepository.save(post);
        return buildPostResponse(saved.getId(), user.getId());
    }

    @Transactional
    public void deletePost(Long postId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requirePost(postId);
        ensureCanDeletePost(post, user);

        explorePostCommentLikeRepository.deleteByComment_Post_Id(postId);
        explorePostCommentRepository.deleteByPost_Id(postId);
        explorePostLikeRepository.deleteByPost_Id(postId);
        explorePostFavoriteRepository.deleteByPost_Id(postId);
        explorePostRepository.delete(post);
    }

    @Transactional
    public ExplorePostResponse setLike(Long postId, boolean liked, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requirePost(postId);
        explorePostLikeRepository.findByPost_IdAndUser_Id(postId, user.getId()).ifPresentOrElse(existing -> {
            if (!liked) {
                explorePostLikeRepository.delete(existing);
            }
        }, () -> {
            if (liked) {
                ExplorePostLike entity = new ExplorePostLike();
                entity.setPost(post);
                entity.setUser(user);
                explorePostLikeRepository.save(entity);
            }
        });
        return buildPostResponse(postId, user.getId());
    }

    @Transactional
    public ExplorePostResponse setFavorite(Long postId, boolean favorited, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requirePost(postId);
        explorePostFavoriteRepository.findByPost_IdAndUser_Id(postId, user.getId()).ifPresentOrElse(existing -> {
            if (!favorited) {
                explorePostFavoriteRepository.delete(existing);
            }
        }, () -> {
            if (favorited) {
                ExplorePostFavorite entity = new ExplorePostFavorite();
                entity.setPost(post);
                entity.setUser(user);
                explorePostFavoriteRepository.save(entity);
            }
        });
        return buildPostResponse(postId, user.getId());
    }

    @Transactional
    public ExplorePostResponse addComment(Long postId,
                                          ExplorePostCommentRequest request,
                                          UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requirePost(postId);

        ExplorePostComment comment = new ExplorePostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(request.content().trim());
        explorePostCommentRepository.save(comment);

        return buildPostResponse(postId, user.getId());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requirePost(postId);
        ExplorePostComment comment = requireComment(commentId);
        validateCommentBelongsToPost(postId, comment);
        ensureCanDeleteComment(post, comment, user);

        explorePostCommentLikeRepository.deleteByComment_Id(commentId);
        explorePostCommentRepository.delete(comment);
    }

    @Transactional
    public ExplorePostResponse recordRouteClick(Long postId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requireRoutePost(postId);
        post.setClickCount(post.getClickCount() + 1);
        explorePostRepository.save(post);
        return buildPostResponse(postId, user.getId());
    }

    @Transactional
    public ExplorePostResponse recordRouteApply(Long postId, UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        ExplorePost post = requireRoutePost(postId);
        post.setApplyCount(post.getApplyCount() + 1);
        explorePostRepository.save(post);
        return buildPostResponse(postId, user.getId());
    }

    @Transactional
    public ExplorePostResponse setCommentLike(Long postId,
                                              Long commentId,
                                              boolean liked,
                                              UserPrincipal principal) {
        UserAccount user = requireUser(principal);
        requirePost(postId);
        ExplorePostComment comment = requireComment(commentId);
        validateCommentBelongsToPost(postId, comment);

        explorePostCommentLikeRepository.findByComment_IdAndUser_Id(commentId, user.getId()).ifPresentOrElse(existing -> {
            if (!liked) {
                explorePostCommentLikeRepository.delete(existing);
            }
        }, () -> {
            if (liked) {
                ExplorePostCommentLike entity = new ExplorePostCommentLike();
                entity.setComment(comment);
                entity.setUser(user);
                explorePostCommentLikeRepository.save(entity);
            }
        });

        return buildPostResponse(postId, user.getId());
    }

    private UserAccount requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "请先登录后再使用探索功能");
        }
        return userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户不存在"));
    }

    private ExplorePost requirePost(Long postId) {
        return explorePostRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "帖子不存在"));
    }

    private ExplorePost requireRoutePost(Long postId) {
        ExplorePost post = requirePost(postId);
        if (post.getRouteJson() == null || post.getRouteJson().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "该帖子不包含共享路线");
        }
        return post;
    }

    private ExplorePostComment requireComment(Long commentId) {
        return explorePostCommentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "评论不存在"));
    }

    private void validateCommentBelongsToPost(Long postId, ExplorePostComment comment) {
        if (!postId.equals(comment.getPost().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "评论与帖子不匹配");
        }
    }

    private void ensureCanDeletePost(ExplorePost post, UserAccount user) {
        if (isAdmin(user) || user.getId().equals(post.getUser().getId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权限删除该帖子");
    }

    private void ensureCanDeleteComment(ExplorePost post, ExplorePostComment comment, UserAccount user) {
        if (isAdmin(user)
                || user.getId().equals(comment.getUser().getId())
                || user.getId().equals(post.getUser().getId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN.value(), "无权限删除该评论");
    }

    private boolean isAdmin(UserAccount user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private ExplorePostResponse buildPostResponse(Long postId, Long viewerUserId) {
        ExplorePost post = explorePostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "帖子不存在"));
        return buildPostResponses(List.of(post), viewerUserId).get(0);
    }

    private List<ExplorePostResponse> buildPostResponses(List<ExplorePost> posts, Long viewerUserId) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
                .map(ExplorePost::getId)
                .toList();

        Map<Long, Integer> likeCounts = new HashMap<>();
        Map<Long, Integer> favoriteCounts = new HashMap<>();
        Set<Long> likedPostIds = new HashSet<>();
        Set<Long> favoritedPostIds = new HashSet<>();

        explorePostLikeRepository.findByPost_IdIn(postIds).forEach(like -> {
            Long postId = like.getPost().getId();
            likeCounts.merge(postId, 1, Integer::sum);
            if (viewerUserId.equals(like.getUser().getId())) {
                likedPostIds.add(postId);
            }
        });

        explorePostFavoriteRepository.findByPost_IdIn(postIds).forEach(favorite -> {
            Long postId = favorite.getPost().getId();
            favoriteCounts.merge(postId, 1, Integer::sum);
            if (viewerUserId.equals(favorite.getUser().getId())) {
                favoritedPostIds.add(postId);
            }
        });

        Map<Long, List<ExplorePostCommentResponse>> commentMap = new LinkedHashMap<>();
        List<ExplorePostComment> comments = explorePostCommentRepository.findByPostIdsWithUser(postIds);
        Map<Long, Integer> commentLikeCounts = new HashMap<>();
        Set<Long> likedCommentIds = new HashSet<>();
        List<Long> commentIds = comments.stream()
                .map(ExplorePostComment::getId)
                .toList();

        if (!commentIds.isEmpty()) {
            explorePostCommentLikeRepository.findByComment_IdIn(commentIds).forEach(like -> {
                Long commentId = like.getComment().getId();
                commentLikeCounts.merge(commentId, 1, Integer::sum);
                if (viewerUserId.equals(like.getUser().getId())) {
                    likedCommentIds.add(commentId);
                }
            });
        }

        comments.forEach(comment -> commentMap
                .computeIfAbsent(comment.getPost().getId(), key -> new ArrayList<>())
                .add(new ExplorePostCommentResponse(
                        comment.getId(),
                        displayNameOf(comment.getUser()),
                        comment.getCreatedAt(),
                        comment.getContent(),
                        viewerUserId.equals(comment.getUser().getId()),
                        likedCommentIds.contains(comment.getId()),
                        commentLikeCounts.getOrDefault(comment.getId(), 0)
                )));

        commentMap.replaceAll((ignored, items) -> sortComments(items));

        return posts.stream()
                .sorted(Comparator.comparing(ExplorePost::getCreatedAt).reversed())
                .map(post -> {
                    ExploreSharedRouteResponse route = deserializeRoute(post.getRouteJson());
                    Long postId = post.getId();
                    return new ExplorePostResponse(
                            postId,
                            displayNameOf(post.getUser()),
                            post.getCreatedAt(),
                            firstNonBlank(post.getTitle(), deriveTitle(post.getContent(), route)),
                            post.getContent(),
                            deserializeImageUrls(post.getImageUrls()),
                            firstNonBlank(post.getLocationTag(), route == null ? null : route.destination()),
                            viewerUserId.equals(post.getUser().getId()),
                            likedPostIds.contains(postId),
                            favoritedPostIds.contains(postId),
                            likeCounts.getOrDefault(postId, 0),
                            favoriteCounts.getOrDefault(postId, 0),
                            post.getClickCount(),
                            post.getApplyCount(),
                            commentMap.getOrDefault(postId, List.of()),
                            route
                    );
                })
                .toList();
    }

    private List<ExplorePostCommentResponse> sortComments(List<ExplorePostCommentResponse> comments) {
        return comments.stream()
                .sorted((left, right) -> {
                    if (left.own() != right.own()) {
                        return left.own() ? -1 : 1;
                    }
                    if (left.own() && right.own()) {
                        int createdCompare = compareInstantDesc(left.createdAt(), right.createdAt());
                        return createdCompare != 0 ? createdCompare : compareLongDesc(left.id(), right.id());
                    }
                    int likeCompare = Integer.compare(right.likeCount(), left.likeCount());
                    if (likeCompare != 0) {
                        return likeCompare;
                    }
                    int createdCompare = compareInstantDesc(left.createdAt(), right.createdAt());
                    return createdCompare != 0 ? createdCompare : compareLongDesc(left.id(), right.id());
                })
                .toList();
    }

    private ExploreSharedRouteResponse normalizeRoute(ExploreSharedRouteRequest route) {
        if (route == null) {
            return null;
        }

        List<RoutePointDto> points = normalizePoints(route.points());
        if (points.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "共享路线至少需要一个点位");
        }

        return new ExploreSharedRouteResponse(
                route.title().trim(),
                route.summary().trim(),
                route.destination().trim(),
                Math.min(Math.max(route.days() == null ? 3 : route.days(), 1), 14),
                trimToNull(route.interests()),
                trimToNull(route.budget()),
                trimToNull(route.departure()),
                points,
                normalizeTips(route.tips())
        );
    }

    private List<RoutePointDto> normalizePoints(List<RoutePointSaveRequest> points) {
        if (points == null) {
            return List.of();
        }
        return points.stream()
                .map(point -> new RoutePointDto(
                        point.day() == null ? 1 : point.day(),
                        point.order() == null ? 1 : point.order(),
                        point.name() == null ? "" : point.name().trim(),
                        trimToNull(point.description()),
                        point.latitude(),
                        point.longitude()
                ))
                .sorted(Comparator.comparingInt(RoutePointDto::day).thenComparingInt(RoutePointDto::order))
                .toList();
    }

    private List<String> normalizeTips(List<String> tips) {
        if (tips == null) {
            return List.of();
        }
        return tips.stream()
                .map(this::trimToNull)
                .filter(item -> item != null)
                .distinct()
                .toList();
    }

    private String serializeRoute(ExploreSharedRouteResponse route) {
        if (route == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(route);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "共享路线序列化失败");
        }
    }

    private ExploreSharedRouteResponse deserializeRoute(String routeJson) {
        if (routeJson == null || routeJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(routeJson, ExploreSharedRouteResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "共享路线数据读取失败");
        }
    }

    private String displayNameOf(UserAccount user) {
        return firstNonBlank(user.getDisplayName(), user.getUsername());
    }

    private String normalizeTitle(String title, String content, ExploreSharedRouteRequest route) {
        String normalized = trimToNull(title);
        if (normalized != null) {
            return normalized;
        }
        if (route != null) {
            String routeTitle = trimToNull(route.title());
            if (routeTitle != null) {
                return routeTitle;
            }
            String destination = trimToNull(route.destination());
            if (destination != null) {
                return destination + " 路线";
            }
        }
        return deriveTitle(content, null);
    }

    private String deriveTitle(String content, ExploreSharedRouteResponse route) {
        if (route != null) {
            return firstNonBlank(trimToNull(route.title()), trimToNull(route.destination()) == null
                    ? "路线分享"
                    : trimToNull(route.destination()) + " 路线");
        }
        String normalizedContent = trimToNull(content);
        if (normalizedContent == null) {
            return "未命名帖子";
        }
        String singleLine = normalizedContent.replaceAll("\\s+", " ");
        return singleLine.length() > 26 ? singleLine.substring(0, 26) + "..." : singleLine;
    }

    private String normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = new java.util.ArrayList<>();
        for (String url : imageUrls) {
            String trimmed = trimToNull(url);
            if (trimmed != null) {
                if (trimmed.length() > 4_000_000) {
                    throw new ApiException(HttpStatus.BAD_REQUEST.value(), "图片内容过大，请换一张更小的图片");
                }
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "图片数据处理失败");
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

    private int compareInstantDesc(java.time.Instant left, java.time.Instant right) {
        return right.compareTo(left);
    }

    private int compareLongDesc(Long left, Long right) {
        return Long.compare(right == null ? 0L : right, left == null ? 0L : left);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
