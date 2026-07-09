package com.tourismqa.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置聚合对象。
 * 使用场景：
 * 绑定 `application.yml` 中 `app.*` 前缀配置，向业务层暴露安全、模型与知识图谱参数。
 * 核心职责：
 * 1. 汇总系统级配置分组。
 * 2. 为服务组件提供类型安全的配置访问入口。
 *
 * <p>框架作用：`@ConfigurationProperties` 由 Spring Boot 在启动时完成属性绑定。</p>
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Llm llm = new Llm();
    private final KnowledgeGraph knowledgeGraph = new KnowledgeGraph();
    private final Realtime realtime = new Realtime();

    public Security getSecurity() {
        return security;
    }

    public Llm getLlm() {
        return llm;
    }

    public KnowledgeGraph getKnowledgeGraph() {
        return knowledgeGraph;
    }

    public Realtime getRealtime() {
        return realtime;
    }

    /**
     * 安全配置分组。
     */
    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMinutes = 720;
        private boolean firstUserAdmin = true;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getJwtExpirationMinutes() {
            return jwtExpirationMinutes;
        }

        public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
            this.jwtExpirationMinutes = jwtExpirationMinutes;
        }

        public boolean isFirstUserAdmin() {
            return firstUserAdmin;
        }

        public void setFirstUserAdmin(boolean firstUserAdmin) {
            this.firstUserAdmin = firstUserAdmin;
        }
    }

    /**
     * 大模型配置分组。
     */
    public static class Llm {
        private String defaultProvider = "siliconflow";
        private String defaultModel;
        private Map<String, Provider> providers = new HashMap<>();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public Map<String, Provider> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, Provider> providers) {
            this.providers = providers;
        }
    }

    /**
     * 模型提供方配置。
     */
    public static class Provider {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private List<Model> models = new ArrayList<>();

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

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public List<Model> getModels() {
            return models;
        }

        public void setModels(List<Model> models) {
            this.models = models;
        }
    }

    /**
     * 单个模型配置项。
     */
    public static class Model {
        private String id;
        private String displayName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * 知识图谱功能配置分组。
     */
    public static class KnowledgeGraph {
        private boolean enabled = true;
        private int contextNodeLimit = 6;
        private int contextRelationshipLimit = 24;
        private int contextTermLimit = 8;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getContextNodeLimit() {
            return contextNodeLimit;
        }

        public void setContextNodeLimit(int contextNodeLimit) {
            this.contextNodeLimit = contextNodeLimit;
        }

        public int getContextRelationshipLimit() {
            return contextRelationshipLimit;
        }

        public void setContextRelationshipLimit(int contextRelationshipLimit) {
            this.contextRelationshipLimit = contextRelationshipLimit;
        }

        public int getContextTermLimit() {
            return contextTermLimit;
        }

        public void setContextTermLimit(int contextTermLimit) {
            this.contextTermLimit = contextTermLimit;
        }
    }

    /**
     * 实时上下文配置分组。
     */
    public static class Realtime {
        private boolean enabled = true;
        private final Weather weather = new Weather();
        private final AttractionStatus attractionStatus = new AttractionStatus();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Weather getWeather() {
            return weather;
        }

        public AttractionStatus getAttractionStatus() {
            return attractionStatus;
        }
    }

    /**
     * 天气增强配置。
     */
    public static class Weather {
        private boolean enabled = true;
        private String baseUrl = "https://api.open-meteo.com";
        private String timezone = "Asia/Shanghai";
        private int forecastDays = 3;
        private int maxForecastDays = 7;

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

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public int getForecastDays() {
            return forecastDays;
        }

        public void setForecastDays(int forecastDays) {
            this.forecastDays = forecastDays;
        }

        public int getMaxForecastDays() {
            return maxForecastDays;
        }

        public void setMaxForecastDays(int maxForecastDays) {
            this.maxForecastDays = maxForecastDays;
        }
    }

    /**
     * 景点开放状态增强配置。
     */
    public static class AttractionStatus {
        private boolean enabled = true;
        private int maxItems = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }
    }
}
