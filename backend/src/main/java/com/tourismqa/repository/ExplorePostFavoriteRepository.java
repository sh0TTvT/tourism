package com.tourismqa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.ExplorePostFavorite;

public interface ExplorePostFavoriteRepository extends JpaRepository<ExplorePostFavorite, Long> {

    List<ExplorePostFavorite> findByPost_IdIn(Collection<Long> postIds);

    Optional<ExplorePostFavorite> findByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_Id(Long postId);
}
