package com.tourismqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourismqa.entity.ExplorePost;

public interface ExplorePostRepository extends JpaRepository<ExplorePost, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("select p from ExplorePost p order by p.createdAt desc")
    List<ExplorePost> findFeed();

    @EntityGraph(attributePaths = "user")
    @Query("select p from ExplorePost p where p.id = :postId")
    Optional<ExplorePost> findByIdWithUser(@Param("postId") Long postId);

    @EntityGraph(attributePaths = "user")
    List<ExplorePost> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
