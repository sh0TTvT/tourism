package com.tourismqa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 外部服务配置实体。
 * 使用场景：
 * 持久化地图、天气等外部依赖服务的运行配置、人工测试结果与心跳检测结果。
 */
@Entity
@Table(name = "managed_external_services", uniqueConstraints = {
        @UniqueConstraint(name = "uk_managed_service_key", columnNames = {"service_key"})
}, indexes = {
        @Index(name = "idx_managed_service_key_enabled", columnList = "service_key, enabled")
})
public class ManagedExternalServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_key", nullable = false, length = 40)
    private String serviceKey;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Lob
    @Column(name = "settings_json", nullable = false, columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_check_passed")
    private Boolean lastCheckPassed;

    @Column(name = "last_check_message", length = 255)
    private String lastCheckMessage;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_heartbeat_passed")
    private Boolean lastHeartbeatPassed;

    @Column(name = "last_heartbeat_message", length = 255)
    private String lastHeartbeatMessage;

    @Column(name = "last_heartbeat_latency_ms")
    private Long lastHeartbeatLatencyMs;

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

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
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

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Boolean getLastHeartbeatPassed() {
        return lastHeartbeatPassed;
    }

    public void setLastHeartbeatPassed(Boolean lastHeartbeatPassed) {
        this.lastHeartbeatPassed = lastHeartbeatPassed;
    }

    public String getLastHeartbeatMessage() {
        return lastHeartbeatMessage;
    }

    public void setLastHeartbeatMessage(String lastHeartbeatMessage) {
        this.lastHeartbeatMessage = lastHeartbeatMessage;
    }

    public Long getLastHeartbeatLatencyMs() {
        return lastHeartbeatLatencyMs;
    }

    public void setLastHeartbeatLatencyMs(Long lastHeartbeatLatencyMs) {
        this.lastHeartbeatLatencyMs = lastHeartbeatLatencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
