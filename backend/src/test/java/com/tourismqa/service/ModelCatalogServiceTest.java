package com.tourismqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tourismqa.dto.AdminLlmModelSaveRequest;
import com.tourismqa.dto.AdminLlmModelSaveResponse;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.repository.LlmModelConfigRepository;

/**
 * 模型配置服务单元测试。
 * 使用场景：
 * 验证管理员保存模型配置时的可用性测试、自动禁用与 API Key 保留逻辑。
 */
class ModelCatalogServiceTest {

    private final Map<Long, LlmModelConfig> store = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    private LlmModelConfigRepository repository;
    private SiliconFlowClient siliconFlowClient;
    private OllamaClient ollamaClient;
    private ModelCatalogService modelCatalogService;

    @BeforeEach
    void setUp() {
        store.clear();
        idSequence.set(1);

        repository = mock(LlmModelConfigRepository.class);
        siliconFlowClient = mock(SiliconFlowClient.class);
        ollamaClient = mock(OllamaClient.class);

        when(siliconFlowClient.provider()).thenReturn("siliconflow");
        when(ollamaClient.provider()).thenReturn("ollama");
        when(ollamaClient.validateConfig(any())).thenReturn(new LlmProviderClient.Availability(true, null));

        when(repository.save(any(LlmModelConfig.class))).thenAnswer(invocation -> {
            LlmModelConfig entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
                if (entity.getCreatedAt() == null) {
                    entity.prePersist();
                }
            } else {
                entity.preUpdate();
            }
            store.put(entity.getId(), entity);
            return entity;
        });
        when(repository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(store.get(invocation.getArgument(0)))
        );
        when(repository.existsByProviderAndModelId(anyString(), anyString())).thenAnswer(invocation ->
                store.values().stream().anyMatch(item ->
                        item.getProvider().equals(invocation.getArgument(0))
                                && item.getModelId().equals(invocation.getArgument(1))
                )
        );
        when(repository.existsByProviderAndModelIdAndIdNot(anyString(), anyString(), anyLong())).thenAnswer(invocation ->
                store.values().stream().anyMatch(item ->
                        item.getProvider().equals(invocation.getArgument(0))
                                && item.getModelId().equals(invocation.getArgument(1))
                                && !item.getId().equals(invocation.getArgument(2))
                )
        );
        when(repository.findByDefaultModelTrue()).thenAnswer(invocation ->
                store.values().stream().filter(LlmModelConfig::isDefaultModel).toList()
        );
        when(repository.findFirstByDefaultModelTrueAndEnabledTrue()).thenAnswer(invocation ->
                store.values().stream().filter(item -> item.isDefaultModel() && item.isEnabled()).findFirst()
        );
        when(repository.findFirstByEnabledTrueOrderByProviderAscModelIdAsc()).thenAnswer(invocation ->
                store.values().stream()
                        .filter(LlmModelConfig::isEnabled)
                        .sorted(Comparator.comparing(LlmModelConfig::getProvider)
                                .thenComparing(LlmModelConfig::getModelId))
                        .findFirst()
        );
        when(repository.countByEnabledTrue()).thenAnswer(invocation ->
                store.values().stream().filter(LlmModelConfig::isEnabled).count()
        );

        modelCatalogService = new ModelCatalogService(repository, List.of(siliconFlowClient, ollamaClient));
    }

    @Test
    void createModelShouldAutoDisableWhenAvailabilityTestFails() {
        when(siliconFlowClient.validateConfig(any())).thenReturn(new LlmProviderClient.Availability(true, null));
        when(siliconFlowClient.testAvailability(any()))
                .thenReturn(new LlmProviderClient.Availability(false, "connect timed out"));

        AdminLlmModelSaveResponse response = modelCatalogService.createModel(new AdminLlmModelSaveRequest(
                "siliconflow",
                "deepseek-ai/DeepSeek-V3",
                "DeepSeek V3",
                "https://api.siliconflow.cn/v1",
                "sk-test-value",
                true,
                true
        ));

        assertThat(response.serviceAvailable()).isFalse();
        assertThat(response.autoDisabled()).isTrue();
        assertThat(response.message()).contains("自动将该模型配置为禁用状态");
        assertThat(response.model().enabled()).isFalse();
        assertThat(response.model().defaultModel()).isFalse();
        assertThat(response.model().lastCheckPassed()).isFalse();
        assertThat(response.model().lastCheckMessage()).contains("connect timed out");
        assertThat(store.values()).hasSize(1);
        assertThat(store.values().iterator().next().isEnabled()).isFalse();
    }

    @Test
    void createModelShouldRemainEnabledWhenAvailabilityTestPasses() {
        when(siliconFlowClient.validateConfig(any())).thenReturn(new LlmProviderClient.Availability(true, null));
        when(siliconFlowClient.testAvailability(any()))
                .thenReturn(new LlmProviderClient.Availability(true, "可用性测试通过"));

        AdminLlmModelSaveResponse response = modelCatalogService.createModel(new AdminLlmModelSaveRequest(
                "siliconflow",
                "Qwen/Qwen2.5-7B-Instruct",
                "Qwen2.5 7B",
                "https://api.siliconflow.cn/v1",
                "sk-test-value",
                true,
                true
        ));

        assertThat(response.serviceAvailable()).isTrue();
        assertThat(response.autoDisabled()).isFalse();
        assertThat(response.model().enabled()).isTrue();
        assertThat(response.model().defaultModel()).isTrue();
        assertThat(response.model().available()).isTrue();
        assertThat(response.model().lastCheckPassed()).isTrue();
    }

    @Test
    void updateModelShouldKeepExistingApiKeyWhenRequestLeavesItBlank() {
        when(siliconFlowClient.validateConfig(any())).thenReturn(new LlmProviderClient.Availability(true, null));
        when(siliconFlowClient.testAvailability(any()))
                .thenReturn(new LlmProviderClient.Availability(true, "可用性测试通过"));

        LlmModelConfig existing = seedModel(
                "siliconflow",
                "deepseek-ai/DeepSeek-V3",
                "旧模型",
                "https://api.siliconflow.cn/v1",
                "sk-existing-key",
                true,
                true
        );

        modelCatalogService.updateModel(existing.getId(), new AdminLlmModelSaveRequest(
                "siliconflow",
                "deepseek-ai/DeepSeek-V3",
                "新模型名称",
                "https://api.siliconflow.cn/v1",
                "",
                true,
                true
        ));

        LlmModelConfig saved = store.get(existing.getId());
        assertThat(saved.getApiKey()).isEqualTo("sk-existing-key");
        assertThat(saved.getDisplayName()).isEqualTo("新模型名称");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getLastCheckedAt()).isAfter(Instant.EPOCH);
    }

    private LlmModelConfig seedModel(String provider,
                                     String modelId,
                                     String displayName,
                                     String baseUrl,
                                     String apiKey,
                                     boolean enabled,
                                     boolean defaultModel) {
        LlmModelConfig entity = new LlmModelConfig();
        entity.setProvider(provider);
        entity.setModelId(modelId);
        entity.setDisplayName(displayName);
        entity.setBaseUrl(baseUrl);
        entity.setApiKey(apiKey);
        entity.setEnabled(enabled);
        entity.setDefaultModel(defaultModel);
        entity.setLastCheckedAt(Instant.now());
        entity.setLastCheckPassed(true);
        entity.setLastCheckMessage("已通过");
        repository.save(entity);
        return entity;
    }
}
