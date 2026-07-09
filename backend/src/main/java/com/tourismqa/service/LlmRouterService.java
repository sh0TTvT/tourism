package com.tourismqa.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.exception.ApiException;

/**
 * 大模型路由服务。
 * 使用场景：
 * 在多提供方模型并存场景下，完成模型选择和客户端分发调用。
 * 核心职责：
 * 1. 根据偏好参数解析最终模型。
 * 2. 校验提供方可用性并执行请求转发。
 * 3. 将不可用场景转换为统一业务异常。
 *
 * <p>框架作用：`@Service` 声明服务 Bean，默认单例作用域。</p>
 */
@Service
public class LlmRouterService {

    private final ModelCatalogService modelCatalogService;
    private final LlmMetricsService llmMetricsService;
    private final Map<String, LlmProviderClient> clients = new HashMap<>();

    public LlmRouterService(ModelCatalogService modelCatalogService,
                            LlmMetricsService llmMetricsService,
                            List<LlmProviderClient> clients) {
        this.modelCatalogService = modelCatalogService;
        this.llmMetricsService = llmMetricsService;
        for (LlmProviderClient client : clients) {
            String provider = client.provider();
            if (provider == null || provider.isBlank()) {
                continue;
            }
            this.clients.put(provider.toLowerCase(Locale.ROOT), client);
        }
    }

    /**
     * 按偏好参数执行聊天请求。
     *
     * @param preferredProvider 偏好提供方，可为空
     * @param preferredModel 偏好模型，可为空
     * @param messages 消息序列
     * @param temperature 采样温度
     * @return 模型回复文本
     */
    public String chat(String preferredProvider,
                       String preferredModel,
                       List<LlmMessage> messages,
                       double temperature) {
        LlmModelConfig config = modelCatalogService.resolveModelConfig(preferredProvider, preferredModel);
        return chatResolved(config, messages, temperature);
    }

    /**
     * 按已解析模型执行聊天请求。
     *
     * @param provider 提供方标识
     * @param model 模型标识
     * @param messages 消息序列
     * @param temperature 采样温度
     * @return 模型回复文本
     * @throws ApiException 当提供方不可用或不存在时抛出
     */
    public String chatResolved(String provider,
                               String model,
                               List<LlmMessage> messages,
                               double temperature) {
        LlmModelConfig config = modelCatalogService.requireEnabledModel(provider, model);
        return chatResolved(config, messages, temperature);
    }

    /**
     * 按偏好参数执行流式聊天请求。
     *
     * @param preferredProvider 偏好提供方，可为空
     * @param preferredModel 偏好模型，可为空
     * @param messages 消息序列
     * @param temperature 采样温度
     * @param onDelta 增量文本回调
     */
    public void stream(String preferredProvider,
                       String preferredModel,
                       List<LlmMessage> messages,
                       double temperature,
                       Consumer<String> onDelta) {
        LlmModelConfig config = modelCatalogService.resolveModelConfig(preferredProvider, preferredModel);
        streamResolved(config, messages, temperature, onDelta);
    }

    /**
     * 按已解析模型执行流式聊天请求。
     *
     * @param provider 提供方标识
     * @param model 模型标识
     * @param messages 消息序列
     * @param temperature 采样温度
     * @param onDelta 增量文本回调
     */
    public void streamResolved(String provider,
                               String model,
                               List<LlmMessage> messages,
                               double temperature,
                               Consumer<String> onDelta) {
        LlmModelConfig config = modelCatalogService.requireEnabledModel(provider, model);
        streamResolved(config, messages, temperature, onDelta);
    }

    private String chatResolved(LlmModelConfig config,
                                List<LlmMessage> messages,
                                double temperature) {
        LlmProviderClient client = requireAvailableClient(config);
        long startNanos = System.nanoTime();
        boolean success = false;
        String errorMessage = null;
        try {
            String answer = client.chat(config, messages, temperature);
            success = true;
            return answer;
        } catch (RuntimeException ex) {
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            recordMetric(config, startNanos, success, errorMessage);
        }
    }

    private void streamResolved(LlmModelConfig config,
                                List<LlmMessage> messages,
                                double temperature,
                                Consumer<String> onDelta) {
        LlmProviderClient client = requireAvailableClient(config);
        long startNanos = System.nanoTime();
        boolean success = false;
        String errorMessage = null;
        try {
            client.streamChat(config, messages, temperature, onDelta);
            success = true;
        } catch (RuntimeException ex) {
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            recordMetric(config, startNanos, success, errorMessage);
        }
    }

    private void recordMetric(LlmModelConfig config, long startNanos, boolean success, String errorMessage) {
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        try {
            llmMetricsService.recordCall(config, latencyMs, success, errorMessage);
        } catch (RuntimeException ex) {
            // 指标写入不能影响主业务的大模型响应。
        }
    }

    private LlmProviderClient requireAvailableClient(LlmModelConfig config) {
        LlmProviderClient client = clients.get(config.getProvider().toLowerCase(Locale.ROOT));
        if (client == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "不支持的 provider: " + config.getProvider());
        }

        ModelCatalogService.Availability availability = modelCatalogService.availabilityOfModel(config);
        if (!availability.available()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "当前选择的大模型服务不可用: " + availability.reason()
            );
        }
        return client;
    }
}
