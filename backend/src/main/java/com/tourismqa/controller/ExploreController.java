package com.tourismqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.ExplorePostCommentRequest;
import com.tourismqa.dto.ExplorePostCreateRequest;
import com.tourismqa.dto.ExplorePostResponse;
import com.tourismqa.security.UserPrincipal;
import com.tourismqa.service.ExploreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/explore/posts")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @GetMapping
    public List<ExplorePostResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.listPosts(principal);
    }

    @PostMapping
    public ExplorePostResponse create(@Valid @RequestBody ExplorePostCreateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.createPost(request, principal);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        exploreService.deletePost(postId, principal);
    }

    @PutMapping("/{postId}/like")
    public ExplorePostResponse like(@PathVariable Long postId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setLike(postId, true, principal);
    }

    @DeleteMapping("/{postId}/like")
    public ExplorePostResponse unlike(@PathVariable Long postId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setLike(postId, false, principal);
    }

    @PutMapping("/{postId}/favorite")
    public ExplorePostResponse favorite(@PathVariable Long postId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setFavorite(postId, true, principal);
    }

    @DeleteMapping("/{postId}/favorite")
    public ExplorePostResponse unfavorite(@PathVariable Long postId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setFavorite(postId, false, principal);
    }

    @PostMapping("/{postId}/comments")
    public ExplorePostResponse comment(@PathVariable Long postId,
                                       @Valid @RequestBody ExplorePostCommentRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.addComment(postId, request, principal);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long postId,
                              @PathVariable Long commentId,
                              @AuthenticationPrincipal UserPrincipal principal) {
        exploreService.deleteComment(postId, commentId, principal);
    }

    @PutMapping("/{postId}/route-click")
    public ExplorePostResponse recordRouteClick(@PathVariable Long postId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.recordRouteClick(postId, principal);
    }

    @PutMapping("/{postId}/route-apply")
    public ExplorePostResponse recordRouteApply(@PathVariable Long postId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.recordRouteApply(postId, principal);
    }

    @PutMapping("/{postId}/comments/{commentId}/like")
    public ExplorePostResponse likeComment(@PathVariable Long postId,
                                           @PathVariable Long commentId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setCommentLike(postId, commentId, true, principal);
    }

    @DeleteMapping("/{postId}/comments/{commentId}/like")
    public ExplorePostResponse unlikeComment(@PathVariable Long postId,
                                             @PathVariable Long commentId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return exploreService.setCommentLike(postId, commentId, false, principal);
    }
}
