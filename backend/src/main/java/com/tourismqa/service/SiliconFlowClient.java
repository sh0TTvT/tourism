package com.tourismqa.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.exception.ApiException;

/**
 * SiliconFlow 提供方客户端实现。
 * 使用场景：
 * 对接 SiliconFlow OpenAI 兼容接口，向云端模型发送聊天补全请求。
 * 核心职责：
 * 1. 校验 API Key 与提供方启用状态。
 * 2. 组装标准 `chat/completions` 请求体。
 * 3. 解析并校验响应结构后返回内容。
 *
 * <p>框架作用：`@Component` 注册客户端实例，供路由服务按名称发现。</p>
 */
@Component
public class SiliconFlowClient implements LlmProviderClient {

    private static final Logger log = LoggerFactory.getLogger(SiliconFlowClient.class);

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SiliconFlowClient(RestClient.Builder builder,
                             ObjectMapper objectMapper) {
        this.restClientBuilder = builder;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 提供方标识。
     *
     * @return 固定值 `siliconflow`
     */
    @Override
    public String provider() {
        return "siliconflow";
    }

    /**
     * 判断 SiliconFlow 客户端是否可调用。
     *
     * @return 配置完整且启用时返回 true
     */
    @Override
    public Availability validateConfig(LlmModelConfig config) {
        if (config == null) {
            return new Availability(false, "模型配置不存在");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            return new Availability(false, "SiliconFlow Base URL 不能为空");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return new Availability(false, "SiliconFlow API Key 不能为空");
        }
        return new Availability(true, null);
    }

    /**
     * 调用 SiliconFlow 聊天补全接口。
     *
     * @param model 模型 ID
     * @param messages 上下文消息
     * @param temperature 采样温度
     * @return 模型回复文本
     */
    @Override
    public String chat(LlmModelConfig config, List<LlmMessage> messages, double temperature) {
        Availability validation = validateConfig(config);
        if (!validation.available()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), validation.reason());
        }

        log.debug("开始非流式调用: model={}, temperature={}, messageCount={}",
                config.getModelId(), temperature, messages.size());

        try {
            String body = objectMapper.writeValueAsString(
                    buildRequestBody(config.getModelId(), messages, temperature, false)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 400) {
                log.error("SiliconFlow API返回错误: statusCode={}, error={}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 调用失败: " + response.body());
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 返回空响应");
            }

            Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);

            if (!(resp.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 返回空响应");
            }

            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> firstMap)
                    || !(firstMap.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)
                    || content.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 响应格式异常");
            }

            log.debug("非流式调用成功: contentLength={}", content.length());
            return content.trim();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("非流式调用失败: error={}", ex.getMessage(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 调用失败: " + ex.getMessage());
        }
    }

    @Override
    public void streamChat(LlmModelConfig config,
                           List<LlmMessage> messages,
                           double temperature,
                           Consumer<String> onDelta) {
        Availability validation = validateConfig(config);
        if (!validation.available()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), validation.reason());
        }

        log.debug("开始流式调用: model={}, temperature={}, messageCount={}",
                config.getModelId(), temperature, messages.size());
        long startTime = System.currentTimeMillis();
        int deltaCount = 0;

        try {
            String body = objectMapper.writeValueAsString(
                    buildRequestBody(config.getModelId(), messages, temperature, true)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofMinutes(10))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = readErrorBody(response.body());
                log.error("SiliconFlow API返回错误: statusCode={}, error={}", response.statusCode(), errorBody);
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 调用失败: " + errorBody);
            }

            log.debug("已建立SSE连接，开始读取流");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("data:")) {
                        continue;
                    }

                    String payload = trimmed.substring(5).trim();
                    if (payload.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(payload)) {
                        log.debug("收到[DONE]标记，流式调用正常结束");
                        break;
                    }

                    try {
                        JsonNode node = objectMapper.readTree(payload);
                        JsonNode content = node.path("choices").path(0).path("delta").path("content");
                        if (content.isTextual() && !content.asText().isEmpty()) {
                            deltaCount++;
                            if (deltaCount % 10 == 0) {
                                log.debug("已接收 {} 个delta，耗时 {}ms", deltaCount, System.currentTimeMillis() - startTime);
                            }
                            onDelta.accept(content.asText());
                            continue;
                        }
                        if (content.isArray()) {
                            for (JsonNode item : content) {
                                JsonNode text = item.path("text");
                                if (text.isTextual() && !text.asText().isEmpty()) {
                                    deltaCount++;
                                    if (deltaCount % 10 == 0) {
                                        log.debug("已接收 {} 个delta，耗时 {}ms", deltaCount, System.currentTimeMillis() - startTime);
                                    }
                                    onDelta.accept(text.asText());
                                }
                            }
                        }
                    } catch (java.io.UncheckedIOException ex) {
                        // 客户端已断开连接，停止读取 SSE 流
                        log.debug("客户端断开连接，停止读取SSE流: deltaCount={}, 耗时={}ms",
                                deltaCount, System.currentTimeMillis() - startTime);
                        throw ex;
                    }
                }
            }

            log.debug("流式调用完成: deltaCount={}, 总耗时={}ms", deltaCount, System.currentTimeMillis() - startTime);
        } catch (ApiException ex) {
            log.error("流式调用业务异常: deltaCount={}, 耗时={}ms, error={}",
                    deltaCount, System.currentTimeMillis() - startTime, ex.getMessage());
            throw ex;
        } catch (java.io.UncheckedIOException ex) {
            // 客户端断开连接是正常情况（用户刷新页面、网络中断等），不应作为错误处理
            log.debug("客户端断开连接（正常情况）: deltaCount={}, 耗时={}ms",
                    deltaCount, System.currentTimeMillis() - startTime);
            throw ex;
        } catch (InterruptedException ex) {
            // 线程被中断 - 通常是因为 Spring 检测到客户端断开连接并中断了异步线程
            // 这是正常情况，不应作为错误处理
            log.debug("线程被中断（客户端断开）: deltaCount={}, 耗时={}ms",
                    deltaCount, System.currentTimeMillis() - startTime);
            Thread.currentThread().interrupt();
            throw new java.io.UncheckedIOException(new java.io.IOException("Thread interrupted (client disconnected)"));
        } catch (Exception ex) {
            log.error("流式调用失败: deltaCount={}, 耗时={}ms, error={}",
                    deltaCount, System.currentTimeMillis() - startTime, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "SiliconFlow 流式调用失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 List<LlmMessage> messages,
                                                 double temperature,
                                                 boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("stream", stream);
        body.put("messages", messages.stream().map(msg -> Map.of(
                "role", msg.role(),
                "content", msg.content()
        )).toList());
        return body;
    }

    private String readErrorBody(InputStream body) throws IOException {
        String text = new String(body.readAllBytes(), StandardCharsets.UTF_8).trim();
        return text.isBlank() ? "响应异常" : text;
    }
}
