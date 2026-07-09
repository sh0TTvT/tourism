package com.tourismqa.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tourismqa.config.AppProperties;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.repository.LlmModelConfigRepository;

/**
 * 模型配置初始化器。
 * 使用场景：
 * 应用首次启动且模型表为空时，将配置文件中的模型信息写入数据库。
 * 核心职责：
 * 1. 过滤未支持/重复模型配置。
 * 2. 初始化可用模型并设置默认模型。
 * 3. 保证初始化过程事务一致性。
 *
 * <p>框架作用：`@Component` + `@EventListener(ApplicationReadyEvent.class)` 在启动完成后执行一次。</p>
 */
@Component
public class LlmModelInitializer {

    private final AppProperties appProperties;
    private final LlmModelConfigRepository repository;
    private final Set<String> supportedProviders = new HashSet<>();

    public LlmModelInitializer(AppProperties appProperties,
                               LlmModelConfigRepository repository,
                               List<LlmProviderClient> clients) {
        this.appProperties = appProperties;
        this.repository = repository;
        for (LlmProviderClient client : clients) {
            String provider = normalizeProvider(client.provider());
            if (provider != null) {
                this.supportedProviders.add(provider);
            }
        }
    }

    /**
     * 当模型配置表为空时执行初始化。
     */
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIfEmpty() {
        if (repository.count() > 0) {
            return;
        }

        String configuredDefaultProvider = normalizeProvider(appProperties.getLlm().getDefaultProvider());
        String configuredDefaultModel = normalizeModelId(appProperties.getLlm().getDefaultModel());

        LlmModelConfig firstSaved = null;
        LlmModelConfig defaultSaved = null;
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, AppProperties.Provider> entry : appProperties.getLlm().getProviders().entrySet()) {
            String provider = normalizeProvider(entry.getKey());
            if (provider == null) {
                continue;
            }
            if (!supportedProviders.contains(provider)) {
                continue;
            }
            AppProperties.Provider providerConfig = entry.getValue();
            if (providerConfig == null || providerConfig.getModels() == null) {
                continue;
            }
            for (AppProperties.Model model : providerConfig.getModels()) {
                if (model == null) {
                    continue;
                }
                String modelId = normalizeModelId(model.getId());
                if (modelId == null) {
                    continue;
                }
                String deduplicateKey = provider + "::" + modelId;
                if (!seen.add(deduplicateKey)) {
                    continue;
                }
                LlmModelConfig config = new LlmModelConfig();
                config.setProvider(provider);
                config.setModelId(modelId);
                config.setDisplayName(normalizeDisplayName(model.getDisplayName(), modelId));
                config.setBaseUrl(normalizeBaseUrl(providerConfig.getBaseUrl(), provider));
                config.setApiKey(normalizeApiKey(providerConfig.getApiKey()));
                config.setEnabled(true);
                config.setDefaultModel(false);
                LlmModelConfig saved = repository.save(config);
                if (firstSaved == null) {
                    firstSaved = saved;
                }
                if (provider.equals(configuredDefaultProvider) && modelId.equals(configuredDefaultModel)) {
                    defaultSaved = saved;
                }
            }
        }

        LlmModelConfig targetDefault = defaultSaved != null ? defaultSaved : firstSaved;
        if (targetDefault != null) {
            targetDefault.setDefaultModel(true);
            repository.save(targetDefault);
        }
    }

    /**
     * 规范化提供方标识。
     *
     * @param provider 原始提供方名称
     * @return 小写提供方标识；为空时返回 null
     */
    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化模型 ID。
     *
     * @param modelId 原始模型 ID
     * @return 去空白后的模型 ID；为空时返回 null
     */
    private String normalizeModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        return modelId.trim();
    }

    /**
     * 规范化模型展示名。
     *
     * @param displayName 原始展示名
     * @param fallback 默认回退值
     * @return 展示名
     */
    private String normalizeDisplayName(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return fallback;
        }
        return displayName.trim();
    }

    private String normalizeBaseUrl(String baseUrl, String provider) {
        if (baseUrl == null || baseUrl.isBlank()) {
            if ("siliconflow".equals(provider)) {
                return "https://api.siliconflow.cn/v1";
            }
            if ("ollama".equals(provider)) {
                return "http://localhost:11434";
            }
            return "http://localhost";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKey.trim();
    }
}
