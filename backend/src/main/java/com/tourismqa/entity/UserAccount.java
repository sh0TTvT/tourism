package com.tourismqa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * 用户账户聚合根实体。
 * 使用场景：
 * 用于承载认证、授权与用户展示信息，并映射到关系库 `users` 表。
 * 核心职责：
 * 1. 维护用户基本身份字段与角色信息。
 * 2. 在实体生命周期回调中维护审计时间戳。
 * 3. 提供授权判断所需的稳定角色读取语义。
 * 设计说明：
 * `role` 字段采用 `EnumType.STRING` 存储，避免枚举顺序变更导致数据语义漂移。
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "preferred_departure", length = 120)
    private String preferredDeparture;

    @Column(name = "budget_preference", length = 80)
    private String budgetPreference;

    @Column(name = "travel_preferences", length = 255)
    private String travelPreferences;

    @Column(name = "interest_tags", length = 255)
    private String interestTags;

    @Column(name = "memory_strategy", nullable = false, length = 40)
    private String memoryStrategy = "STANDARD";

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "banned", nullable = false)
    private boolean banned;

    @Column(name = "ban_reason", length = 255)
    private String banReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 首次持久化前初始化审计字段与默认角色。
     */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        // 统一以同一时间戳初始化创建与更新时间，便于后续审计比对。
        this.createdAt = now;
        this.updatedAt = now;
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.memoryStrategy == null || this.memoryStrategy.isBlank()) {
            this.memoryStrategy = "STANDARD";
        }
    }

    /**
     * 更新前刷新更新时间戳。
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * 获取用户主键。
     *
     * @return 用户主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户主键。
     *
     * @param id 用户主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取用户邮箱。
     *
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置用户邮箱。
     *
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取密码哈希值。
     *
     * @return 密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希值。
     *
     * @param passwordHash 密码哈希
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取显示名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置显示名称。
     *
     * @param displayName 显示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPreferredDeparture() {
        return preferredDeparture;
    }

    public void setPreferredDeparture(String preferredDeparture) {
        this.preferredDeparture = preferredDeparture;
    }

    public String getBudgetPreference() {
        return budgetPreference;
    }

    public void setBudgetPreference(String budgetPreference) {
        this.budgetPreference = budgetPreference;
    }

    public String getTravelPreferences() {
        return travelPreferences;
    }

    public void setTravelPreferences(String travelPreferences) {
        this.travelPreferences = travelPreferences;
    }

    public String getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(String interestTags) {
        this.interestTags = interestTags;
    }

    public String getMemoryStrategy() {
        return memoryStrategy == null || memoryStrategy.isBlank() ? "STANDARD" : memoryStrategy;
    }

    public void setMemoryStrategy(String memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    /**
     * 获取用户角色。
     *
     * @return 角色枚举；当底层值为空时回退为 USER
     */
    public UserRole getRole() {
        return role == null ? UserRole.USER : role;
    }

    /**
     * 设置用户角色。
     *
     * @param role 角色枚举
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
