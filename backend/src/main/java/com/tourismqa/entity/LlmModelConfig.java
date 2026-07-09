package com.tourismqa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * LlmModelConfig 领域实体。
 * 使用场景：映射数据库持久化结构并承载核心业务数据。
 * 核心职责：定义字段约束、实体关系及基础生命周期行为。
 */
@Entity
@Table(name = "llm_models", uniqueConstraints = {
        @UniqueConstraint(name = "uk_llm_model_provider_id", columnNames = {"provider", "model_id"})
}, indexes = {
        @Index(name = "idx_llm_model_enabled_default", columnList = "enabled, is_default")
})
public class LlmModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "model_id", nullable = false, length = 160)
    private String modelId;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "is_default", nullable = false)
    private boolean defaultModel = false;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_check_passed")
    private Boolean lastCheckPassed;

    @Column(name = "last_check_message", length = 255)
    private String lastCheckMessage;

    @Column(name = "total_call_count", nullable = false)
    private long totalCallCount = 0;

    @Column(name = "successful_call_count", nullable = false)
    private long successfulCallCount = 0;

    @Column(name = "failed_call_count", nullable = false)
    private long failedCallCount = 0;

    @Column(name = "total_latency_ms", nullable = false)
    private long totalLatencyMs = 0;

    @Column(name = "average_latency_ms")
    private Long averageLatencyMs;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs;

    @Column(name = "last_called_at")
    private Instant lastCalledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(boolean defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Boolean getLastCheckPassed() {
        return lastCheckPassed;
    }

    public void setLastCheckPassed(Boolean lastCheckPassed) {
        this.lastCheckPassed = lastCheckPassed;
    }

    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    public void setLastCheckMessage(String lastCheckMessage) {
        this.lastCheckMessage = lastCheckMessage;
    }

    public long getTotalCallCount() {
        return totalCallCount;
    }

    public void setTotalCallCount(long totalCallCount) {
        this.totalCallCount = totalCallCount;
    }

    public long getSuccessfulCallCount() {
        return successfulCallCount;
    }

    public void setSuccessfulCallCount(long successfulCallCount) {
        this.successfulCallCount = successfulCallCount;
    }

    public long getFailedCallCount() {
        return failedCallCount;
    }

    public void setFailedCallCount(long failedCallCount) {
        this.failedCallCount = failedCallCount;
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public Long getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(Long averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public Long getLastLatencyMs() {
        return lastLatencyMs;
    }

    public void setLastLatencyMs(Long lastLatencyMs) {
        this.lastLatencyMs = lastLatencyMs;
    }

    public Instant getLastCalledAt() {
        return lastCalledAt;
    }

    public void setLastCalledAt(Instant lastCalledAt) {
        this.lastCalledAt = lastCalledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
