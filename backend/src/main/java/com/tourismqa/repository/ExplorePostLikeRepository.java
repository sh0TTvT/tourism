package com.tourismqa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.ExplorePostLike;

public interface ExplorePostLikeRepository extends JpaRepository<ExplorePostLike, Long> {

    List<ExplorePostLike> findByPost_IdIn(Collection<Long> postIds);

    Optional<ExplorePostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_Id(Long postId);
}
