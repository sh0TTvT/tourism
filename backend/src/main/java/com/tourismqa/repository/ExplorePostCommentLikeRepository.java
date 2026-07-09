package com.tourismqa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.ExplorePostCommentLike;

public interface ExplorePostCommentLikeRepository extends JpaRepository<ExplorePostCommentLike, Long> {

    List<ExplorePostCommentLike> findByComment_IdIn(Collection<Long> commentIds);

    Optional<ExplorePostCommentLike> findByComment_IdAndUser_Id(Long commentId, Long userId);

    void deleteByComment_Id(Long commentId);

    void deleteByComment_Post_Id(Long postId);
}
