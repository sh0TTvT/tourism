package com.tourismqa.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "explore_post_comment_likes", indexes = {
        @Index(name = "idx_explore_comment_like_comment_created", columnList = "comment_id, created_at"),
        @Index(name = "idx_explore_comment_like_user_created", columnList = "user_id, created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_explore_comment_like_comment_user", columnNames = {"comment_id", "user_id"})
})
public class ExplorePostCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private ExplorePostComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @jakarta.persistence.Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ExplorePostComment getComment() {
        return comment;
    }

    public void setComment(ExplorePostComment comment) {
        this.comment = comment;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
