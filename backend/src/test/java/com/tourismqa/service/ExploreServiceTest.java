package com.tourismqa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.entity.ExplorePost;
import com.tourismqa.entity.ExplorePostComment;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.ExplorePostCommentLikeRepository;
import com.tourismqa.repository.ExplorePostCommentRepository;
import com.tourismqa.repository.ExplorePostFavoriteRepository;
import com.tourismqa.repository.ExplorePostLikeRepository;
import com.tourismqa.repository.ExplorePostRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    @Mock
    private ExplorePostRepository explorePostRepository;

    @Mock
    private ExplorePostLikeRepository explorePostLikeRepository;

    @Mock
    private ExplorePostFavoriteRepository explorePostFavoriteRepository;

    @Mock
    private ExplorePostCommentRepository explorePostCommentRepository;

    @Mock
    private ExplorePostCommentLikeRepository explorePostCommentLikeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private ExploreService exploreService;

    @BeforeEach
    void setUp() {
        exploreService = new ExploreService(
                explorePostRepository,
                explorePostLikeRepository,
                explorePostFavoriteRepository,
                explorePostCommentRepository,
                explorePostCommentLikeRepository,
                userAccountRepository,
                new ObjectMapper()
        );
    }

    @Test
    void deletePost_allowsOwnerAndCleansAssociations() {
        UserAccount owner = user(1L, UserRole.USER);
        ExplorePost post = post(10L, owner);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(explorePostRepository.findById(10L)).thenReturn(Optional.of(post));

        exploreService.deletePost(10L, new UserPrincipal(owner));

        verify(explorePostCommentLikeRepository).deleteByComment_Post_Id(10L);
        verify(explorePostCommentRepository).deleteByPost_Id(10L);
        verify(explorePostLikeRepository).deleteByPost_Id(10L);
        verify(explorePostFavoriteRepository).deleteByPost_Id(10L);
        verify(explorePostRepository).delete(post);
    }

    @Test
    void deletePost_rejectsNonOwner() {
        UserAccount actor = user(2L, UserRole.USER);
        UserAccount owner = user(1L, UserRole.USER);
        ExplorePost post = post(10L, owner);

        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(explorePostRepository.findById(10L)).thenReturn(Optional.of(post));

        ApiException ex = assertThrows(ApiException.class,
                () -> exploreService.deletePost(10L, new UserPrincipal(actor)));

        assertEquals(403, ex.getStatus());
        verify(explorePostRepository, never()).delete(post);
        verify(explorePostCommentRepository, never()).deleteByPost_Id(10L);
    }

    @Test
    void deleteComment_allowsPostOwnerToDeleteCommentsInsidePost() {
        UserAccount postOwner = user(1L, UserRole.USER);
        UserAccount commentAuthor = user(2L, UserRole.USER);
        ExplorePost post = post(10L, postOwner);
        ExplorePostComment comment = comment(20L, post, commentAuthor);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(postOwner));
        when(explorePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(explorePostCommentRepository.findById(20L)).thenReturn(Optional.of(comment));

        exploreService.deleteComment(10L, 20L, new UserPrincipal(postOwner));

        verify(explorePostCommentLikeRepository).deleteByComment_Id(20L);
        verify(explorePostCommentRepository).delete(comment);
    }

    @Test
    void deleteComment_rejectsUnrelatedUser() {
        UserAccount actor = user(3L, UserRole.USER);
        UserAccount postOwner = user(1L, UserRole.USER);
        UserAccount commentAuthor = user(2L, UserRole.USER);
        ExplorePost post = post(10L, postOwner);
        ExplorePostComment comment = comment(20L, post, commentAuthor);

        when(userAccountRepository.findById(3L)).thenReturn(Optional.of(actor));
        when(explorePostRepository.findById(10L)).thenReturn(Optional.of(post));
        when(explorePostCommentRepository.findById(20L)).thenReturn(Optional.of(comment));

        ApiException ex = assertThrows(ApiException.class,
                () -> exploreService.deleteComment(10L, 20L, new UserPrincipal(actor)));

        assertEquals(403, ex.getStatus());
        verify(explorePostCommentRepository, never()).delete(comment);
    }

    private UserAccount user(Long id, UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setDisplayName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        return user;
    }

    private ExplorePost post(Long id, UserAccount owner) {
        ExplorePost post = new ExplorePost();
        post.setId(id);
        post.setUser(owner);
        post.setTitle("post");
        return post;
    }

    private ExplorePostComment comment(Long id, ExplorePost post, UserAccount author) {
        ExplorePostComment comment = new ExplorePostComment();
        comment.setPost(post);
        comment.setUser(author);
        comment.setContent("comment");
        return comment;
    }
}
