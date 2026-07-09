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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.entity.LlmModelConfig;
import com.tourismqa.exception.ApiException;

/**
 * Ollama 提供方客户端实现。
 * 使用场景：
 * 在本地或私有部署场景下调用 Ollama `chat` 接口完成模型推理。
 * 核心职责：
 * 1. 判断 Ollama 配置可用性。
 * 2. 组装请求并调用 `/api/chat`。
 * 3. 校验响应结构并返回文本结果。
 *
 * <p>框架作用：`@Component` 注册为可注入组件，被路由服务按提供方标识选择。</p>
 */
@Component
public class OllamaClient implements LlmProviderClient {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaClient(RestClient.Builder builder,
                        ObjectMapper objectMapper) {
        this.restClientBuilder = builder;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 提供方标识。
     *
     * @return 固定值 `ollama`
     */
    @Override
    public String provider() {
        return "ollama";
    }

    /**
     * 判断 Ollama 客户端是否可调用。
     *
     * @return 已配置且启用时返回 true
     */
    @Override
    public Availability validateConfig(LlmModelConfig config) {
        if (config == null) {
            return new Availability(false, "模型配置不存在");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            return new Availability(false, "Ollama Base URL 不能为空");
        }
        return new Availability(true, null);
    }

    /**
     * 调用 Ollama 聊天接口。
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

        Map<String, Object> body = buildRequestBody(config.getModelId(), messages, temperature, false);
        RestClient restClient = restClientBuilder.baseUrl(config.getBaseUrl()).build();

        try {
            Map<String, Object> resp = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (resp == null || !(resp.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)
                    || content.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "Ollama 响应格式异常");
            }

            return content.trim();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "Ollama 调用失败: " + ex.getMessage());
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

        try {
            String body = objectMapper.writeValueAsString(
                    buildRequestBody(config.getModelId(), messages, temperature, true)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/chat"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "Ollama 调用失败: " + readErrorBody(response.body()));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    try {
                        JsonNode node = objectMapper.readTree(trimmed);
                        JsonNode content = node.path("message").path("content");
                        if (content.isTextual() && !content.asText().isEmpty()) {
                            onDelta.accept(content.asText());
                        }
                        if (node.path("done").asBoolean(false)) {
                            break;
                        }
                    } catch (java.io.UncheckedIOException ex) {
                        // 客户端已断开连接，停止读取流
                        throw ex;
                    }
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (java.io.UncheckedIOException ex) {
            // 客户端断开连接是正常情况（用户刷新页面、网络中断等），不应作为错误处理
            throw ex;
        } catch (InterruptedException ex) {
            // 线程被中断 - 通常是因为 Spring 检测到客户端断开连接并中断了异步线程
            // 这是正常情况，不应作为错误处理
            Thread.currentThread().interrupt();
            throw new java.io.UncheckedIOException(new java.io.IOException("Thread interrupted (client disconnected)"));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY.value(), "Ollama 流式调用失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 List<LlmMessage> messages,
                                                 double temperature,
                                                 boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("messages", messages.stream().map(msg -> Map.of(
                "role", msg.role(),
                "content", msg.content()
        )).toList());
        body.put("options", Map.of("temperature", temperature));
        return body;
    }

    private String readErrorBody(InputStream body) throws IOException {
        String text = new String(body.readAllBytes(), StandardCharsets.UTF_8).trim();
        return text.isBlank() ? "响应异常" : text;
    }
}
