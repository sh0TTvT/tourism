package com.tourismqa.service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourismqa.dto.AdminLlmModelItemResponse;
import com.tourismqa.dto.AdminLlmModelSaveRequest;
import com.tourismqa.dto.AdminLlmModelSaveResponse;
import com.tourismqa.dto.ModelInfoResponse;
import com.tourismqa.dto.ModelsResponse;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.LlmModelConfigRepository;

/**
 * 模型目录与配置管理服务。
 * 使用场景：
 * 同时支撑前台模型查询与后台模型配置管理，统一封装默认模型、可用性测试与运行时选择规则。
 */
@Service
public class ModelCatalogService {

    private final LlmModelConfigRepository repository;
    private final Map<String, LlmProviderClient> clients = new HashMap<>();

    public ModelCatalogService(LlmModelConfigRepository repository, List<LlmProviderClient> clients) {
        this.repository = repository;
        for (LlmProviderClient client : clients) {
            String provider = client.provider();
            if (provider == null || provider.isBlank()) {
                continue;
            }
            this.clients.put(provider.toLowerCase(Locale.ROOT), client);
        }
    }

    /**
     * 查询前端可见模型列表。
     *
     * @return 模型目录响应
     */
    @Transactional(readOnly = true)
    public ModelsResponse listModels() {
        List<LlmModelConfig> enabledModels = repository.findByEnabledTrueOrderByProviderAscModelIdAsc();
        if (enabledModels.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "未配置可用大模型，请联系管理员");
        }

        LlmModelConfig defaultModel = resolveDefaultEnabledModel(enabledModels);
        List<ModelInfoResponse> models = new ArrayList<>();
        for (LlmModelConfig modelConfig : enabledModels) {
            Availability availability = availabilityOfModel(modelConfig);
            models.add(new ModelInfoResponse(
                    modelConfig.getProvider(),
                    modelConfig.getModelId(),
                    modelConfig.getDisplayName(),
                    availability.available(),
                    availability.reason()
            ));
        }

        return new ModelsResponse(
                defaultModel.getProvider(),
                defaultModel.getModelId(),
                models
        );
    }

    /**
     * 查询后台模型配置列表。
     *
     * @return 管理端模型列表
     */
    @Transactional(readOnly = true)
    public List<AdminLlmModelItemResponse> listModelsForAdmin() {
        return repository.findAllByOrderByProviderAscModelIdAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /**
     * 新建模型配置。
     *
     * @param request 模型保存请求
     * @return 新建后的模型配置与测试结果
     */
    @Transactional
    public AdminLlmModelSaveResponse createModel(AdminLlmModelSaveRequest request) {
        String provider = normalizeProvider(request.provider());
        String modelId = normalizeModelId(request.modelId());

        validateProviderSupported(provider);
        if (repository.existsByProviderAndModelId(provider, modelId)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "模型已存在: " + provider + "/" + modelId);
        }

        LlmModelConfig entity = new LlmModelConfig();
        entity.setProvider(provider);
        applyRequest(entity, request, null);
        return persistWithAvailabilityCheck(entity, Boolean.TRUE.equals(request.enabled()), Boolean.TRUE.equals(request.defaultModel()));
    }

    /**
     * 更新模型配置。
     *
     * @param id 模型配置主键
     * @param request 模型保存请求
     * @return 更新后的模型配置与测试结果
     */
    @Transactional
    public AdminLlmModelSaveResponse updateModel(Long id, AdminLlmModelSaveRequest request) {
        LlmModelConfig entity = requireById(id);
        String provider = normalizeProvider(request.provider());
        String modelId = normalizeModelId(request.modelId());

        validateProviderSupported(provider);
        if (repository.existsByProviderAndModelIdAndIdNot(provider, modelId, id)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "模型已存在: " + provider + "/" + modelId);
        }
        if (entity.isEnabled() && !Boolean.TRUE.equals(request.enabled()) && repository.countByEnabledTrue() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "至少保留一个启用模型");
        }

        applyRequest(entity, request, entity);
        return persistWithAvailabilityCheck(entity, Boolean.TRUE.equals(request.enabled()), Boolean.TRUE.equals(request.defaultModel()));
    }

    /**
     * 设置默认模型。
     *
     * @param id 模型配置主键
     * @return 更新后的模型配置
     */
    @Transactional
    public AdminLlmModelItemResponse setDefaultModel(Long id) {
        LlmModelConfig model = requireById(id);
        if (!model.isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "禁用模型不能设为默认");
        }

        Availability availability = availabilityOfModel(model);
        if (!availability.available()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "当前模型不可设为默认: " + availability.reason());
        }

        switchDefault(model);
        return toAdminResponse(requireById(id));
    }

    /**
     * 删除指定模型配置。
     *
     * @param id 模型配置主键
     */
    @Transactional
    public void deleteModel(Long id) {
        LlmModelConfig entity = requireById(id);
        if (entity.isEnabled() && repository.countByEnabledTrue() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "至少保留一个启用模型");
        }
        repository.delete(entity);
        ensureDefaultExistsIfMissing();
    }

    /**
     * 解析本次请求应使用的模型配置。
     *
     * @param preferredProvider 偏好提供方
     * @param preferredModel 偏好模型
     * @return 可运行的模型配置
     */
    @Transactional(readOnly = true)
    public LlmModelConfig resolveModelConfig(String preferredProvider, String preferredModel) {
        String provider = normalizeNullableProvider(preferredProvider);
        String modelId = normalizeNullableModelId(preferredModel);

        if (provider != null && modelId != null) {
            LlmModelConfig model = repository.findByProviderAndModelIdAndEnabledTrue(provider, modelId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "模型不可用: " + provider + "/" + modelId));
            validateModelForRuntime(model);
            return model;
        }

        if (provider != null) {
            List<LlmModelConfig> models = repository.findByProviderAndEnabledTrueOrderByDefaultModelDescModelIdAsc(provider);
            if (models.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "provider 未配置可用模型: " + provider);
            }
            LlmModelConfig model = models.get(0);
            validateModelForRuntime(model);
            return model;
        }

        if (modelId != null) {
            List<LlmModelConfig> models = repository.findByModelIdAndEnabledTrueOrderByDefaultModelDescProviderAsc(modelId);
            if (models.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "模型不可用: " + modelId);
            }
            LlmModelConfig selected = models.get(0);
            if (models.size() > 1 && !selected.isDefaultModel()) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "模型ID存在多个 provider，请同时指定 provider");
            }
            validateModelForRuntime(selected);
            return selected;
        }

        LlmModelConfig selected = requireDefaultEnabledModel();
        validateModelForRuntime(selected);
        return selected;
    }

    /**
     * 解析本次请求应使用的提供方与模型。
     *
     * @param preferredProvider 偏好提供方
     * @param preferredModel 偏好模型
     * @return 运行时模型选择结果
     */
    @Transactional(readOnly = true)
    public LlmModelSelection resolveModelSelection(String preferredProvider, String preferredModel) {
        LlmModelConfig config = resolveModelConfig(preferredProvider, preferredModel);
        return new LlmModelSelection(config.getProvider(), config.getModelId());
    }

    /**
     * 根据 provider 和 modelId 查询模型配置。
     *
     * @param provider provider
     * @param modelId modelId
     * @return 模型配置
     */
    @Transactional(readOnly = true)
    public LlmModelConfig requireEnabledModel(String provider, String modelId) {
        LlmModelConfig config = repository.findByProviderAndModelIdAndEnabledTrue(
                        normalizeProvider(provider),
                        normalizeModelId(modelId)
                )
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "模型不可用: " + provider + "/" + modelId));
        validateModelForRuntime(config);
        return config;
    }

    /**
     * 查询模型配置可用性状态。
     *
     * @param config 模型配置
     * @return 可用性结果
     */
    @Transactional(readOnly = true)
    public Availability availabilityOfModel(LlmModelConfig config) {
        if (config == null) {
            return new Availability(false, "模型配置不存在");
        }
        if (!config.isEnabled()) {
            if (Boolean.FALSE.equals(config.getLastCheckPassed())) {
                return new Availability(false, safeReason(config.getLastCheckMessage(), "服务不可用"));
            }
            return new Availability(false, "model disabled");
        }

        LlmProviderClient client = clients.get(config.getProvider());
        if (client == null) {
            return new Availability(false, "provider client not registered");
        }

        LlmProviderClient.Availability validation = client.validateConfig(config);
        if (!validation.available()) {
            return new Availability(false, validation.reason());
        }
        if (Boolean.FALSE.equals(config.getLastCheckPassed())) {
            return new Availability(false, safeReason(config.getLastCheckMessage(), "最近一次可用性测试失败"));
        }
        return new Availability(true, null);
    }

    private AdminLlmModelSaveResponse persistWithAvailabilityCheck(LlmModelConfig entity,
                                                                  boolean requestedEnabled,
                                                                  boolean requestedDefault) {
        if (requestedDefault && !requestedEnabled) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "默认模型必须是启用状态");
        }

        Availability testResult = testModelAvailability(entity);
        entity.setLastCheckedAt(Instant.now());
        entity.setLastCheckPassed(testResult.available());
        entity.setLastCheckMessage(safeReason(testResult.reason(), testResult.available() ? "可用性测试通过" : "服务不可用"));

        boolean autoDisabled = requestedEnabled && !testResult.available();
        entity.setEnabled(requestedEnabled && testResult.available());
        if (!entity.isEnabled()) {
            entity.setDefaultModel(false);
        }

        LlmModelConfig saved = repository.save(entity);
        if (saved.isEnabled() && (requestedDefault
                || repository.findFirstByDefaultModelTrueAndEnabledTrue().isEmpty())) {
            switchDefault(saved);
        } else {
            ensureDefaultExistsIfMissing();
        }

        AdminLlmModelItemResponse response = toAdminResponse(requireById(saved.getId()));
        String message = buildSaveMessage(requestedEnabled, testResult, autoDisabled);
        return new AdminLlmModelSaveResponse(response, testResult.available(), autoDisabled, message);
    }

    private String buildSaveMessage(boolean requestedEnabled, Availability testResult, boolean autoDisabled) {
        if (testResult.available()) {
            if (requestedEnabled) {
                return "模型配置已保存并通过可用性测试";
            }
            return "模型配置已保存，连通性测试通过，当前仍保持禁用状态";
        }
        if (autoDisabled) {
            return "服务不可用，系统已自动将该模型配置为禁用状态: " + safeReason(testResult.reason(), "服务不可用");
        }
        return "模型配置已保存，但服务当前不可用: " + safeReason(testResult.reason(), "服务不可用");
    }

    private Availability testModelAvailability(LlmModelConfig entity) {
        LlmProviderClient client = clients.get(entity.getProvider());
        if (client == null) {
            return new Availability(false, "provider client not registered");
        }
        LlmProviderClient.Availability result = client.testAvailability(entity);
        return new Availability(result.available(), safeReason(result.reason(), result.available() ? null : "服务不可用"));
    }

    private AdminLlmModelItemResponse toAdminResponse(LlmModelConfig config) {
        Availability availability = availabilityOfModel(config);
        return new AdminLlmModelItemResponse(
                config.getId(),
                config.getProvider(),
                config.getModelId(),
                config.getDisplayName(),
                config.getBaseUrl(),
                config.getApiKey() != null && !config.getApiKey().isBlank(),
                maskSecret(config.getApiKey()),
                config.isEnabled(),
                config.isDefaultModel(),
                availability.available(),
                availability.reason(),
                config.getLastCheckedAt(),
                config.getLastCheckPassed(),
                config.getLastCheckMessage(),
                config.getTotalCallCount(),
                config.getSuccessfulCallCount(),
                config.getFailedCallCount(),
                config.getAverageLatencyMs(),
                config.getLastLatencyMs(),
                config.getLastCalledAt(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private void ensureDefaultExistsIfMissing() {
        if (repository.countByEnabledTrue() == 0) {
            return;
        }
        if (repository.findFirstByDefaultModelTrueAndEnabledTrue().isPresent()) {
            return;
        }
        LlmModelConfig fallback = repository.findFirstByEnabledTrueOrderByProviderAscModelIdAsc()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(), "至少保留一个启用模型"));
        switchDefault(fallback);
    }

    private void switchDefault(LlmModelConfig target) {
        if (!target.isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "默认模型必须是启用状态");
        }
        List<LlmModelConfig> defaults = repository.findByDefaultModelTrue();
        for (LlmModelConfig current : defaults) {
            if (!current.getId().equals(target.getId())) {
                current.setDefaultModel(false);
                repository.save(current);
            }
        }
        if (!target.isDefaultModel()) {
            target.setDefaultModel(true);
            repository.save(target);
        }
    }

    private LlmModelConfig requireDefaultEnabledModel() {
        return repository.findFirstByDefaultModelTrueAndEnabledTrue()
                .or(() -> repository.findFirstByEnabledTrueOrderByProviderAscModelIdAsc())
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "未配置可用大模型，请联系管理员"));
    }

    private LlmModelConfig resolveDefaultEnabledModel(List<LlmModelConfig> enabledModels) {
        if (enabledModels.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "未配置可用大模型，请联系管理员");
        }
        for (LlmModelConfig model : enabledModels) {
            if (model.isDefaultModel()) {
                return model;
            }
        }
        return enabledModels.get(0);
    }

    private LlmModelConfig requireById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "模型不存在"));
    }

    private void validateProviderSupported(String provider) {
        if (!clients.containsKey(provider)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "不支持的 provider: " + provider);
        }
    }

    private void validateModelForRuntime(LlmModelConfig config) {
        Availability availability = availabilityOfModel(config);
        if (!availability.available()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "当前选择的大模型服务不可用: " + availability.reason()
            );
        }
    }

    private void applyRequest(LlmModelConfig entity, AdminLlmModelSaveRequest request, LlmModelConfig existing) {
        String provider = normalizeProvider(request.provider());
        String modelId = normalizeModelId(request.modelId());
        String displayName = normalizeDisplayName(request.displayName());
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        String apiKey = resolveApiKey(provider, request.apiKey(), existing);

        entity.setProvider(provider);
        entity.setModelId(modelId);
        entity.setDisplayName(displayName);
        entity.setBaseUrl(baseUrl);
        entity.setApiKey(apiKey);
    }

    private String resolveApiKey(String provider, String rawApiKey, LlmModelConfig existing) {
        String normalized = normalizeNullableApiKey(rawApiKey);
        if ("siliconflow".equals(provider)) {
            if (normalized != null) {
                return normalized;
            }
            if (existing != null
                    && "siliconflow".equals(existing.getProvider())
                    && existing.getApiKey() != null
                    && !existing.getApiKey().isBlank()) {
                return existing.getApiKey();
            }
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "SiliconFlow API Key 不能为空");
        }
        return normalized;
    }

    private String normalizeProvider(String provider) {
        String normalized = normalizeNullableProvider(provider);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "provider 不能为空");
        }
        return normalized;
    }

    private String normalizeModelId(String modelId) {
        String normalized = normalizeNullableModelId(modelId);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "模型ID不能为空");
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "显示名称不能为空");
        }
        return displayName.trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Base URL 不能为空");
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("只支持 http/https 协议");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("缺少有效主机名");
            }
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Base URL 格式错误: " + ex.getMessage());
        }
        return normalized;
    }

    private String normalizeNullableProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullableModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        return modelId.trim();
    }

    private String normalizeNullableApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return apiKey.trim();
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        String value = secret.trim();
        if (value.length() <= 8) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String safeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        return reason;
    }

    /**
     * 提供方可用性值对象。
     *
     * @param available 是否可用
     * @param reason 不可用原因
     */
    public record Availability(boolean available, String reason) {
    }
}
