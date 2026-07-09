package com.tourismqa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourismqa.entity.ExplorePostComment;

public interface ExplorePostCommentRepository extends JpaRepository<ExplorePostComment, Long> {

    @Query("""
            select c
            from ExplorePostComment c
            join fetch c.post
            join fetch c.user
            where c.post.id in :postIds
            order by c.createdAt asc, c.id asc
            """)
    List<ExplorePostComment> findByPostIdsWithUser(@Param("postIds") Collection<Long> postIds);

    @Query("""
            select c
            from ExplorePostComment c
            join fetch c.post p
            where c.user.id = :userId
            order by c.createdAt desc, c.id desc
            """)
    List<ExplorePostComment> findByUserIdWithPost(@Param("userId") Long userId);

    void deleteByPost_Id(Long postId);
}
