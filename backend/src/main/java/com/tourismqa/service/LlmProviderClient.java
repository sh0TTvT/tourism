package com.tourismqa.service;

import java.util.List;
import java.util.function.Consumer;

import com.tourismqa.entity.LlmModelConfig;

/**
 * 大模型提供方客户端抽象。
 * 使用场景：
 * 屏蔽不同厂商 API 差异，为路由服务提供统一调用协议。
 */
public interface LlmProviderClient {

    /**
     * 返回提供方唯一标识。
     *
     * @return 提供方标识
     */
    String provider();

    /**
     * 校验当前模型配置是否满足 provider 的基本调用条件。
     *
     * @param config 模型配置
     * @return 校验结果
     */
    Availability validateConfig(LlmModelConfig config);

    /**
     * 调用提供方聊天补全接口。
     *
     * @param config 模型配置
     * @param messages 对话消息序列
     * @param temperature 采样温度
     * @return 模型输出文本
     */
    String chat(LlmModelConfig config, List<LlmMessage> messages, double temperature);

    /**
     * 以增量方式调用提供方聊天补全接口。
     *
     * @param config 模型配置
     * @param messages 对话消息序列
     * @param temperature 采样温度
     * @param onDelta 每次收到文本增量时的回调
     */
    default void streamChat(LlmModelConfig config,
                            List<LlmMessage> messages,
                            double temperature,
                            Consumer<String> onDelta) {
        String answer = chat(config, messages, temperature);
        if (answer != null && !answer.isBlank()) {
            onDelta.accept(answer);
        }
    }

    /**
     * 使用轻量对话请求测试当前配置可用性。
     *
     * @param config 模型配置
     * @return 测试结果
     */
    default Availability testAvailability(LlmModelConfig config) {
        Availability validation = validateConfig(config);
        if (!validation.available()) {
            return validation;
        }

        try {
            String answer = chat(
                    config,
                    List.of(
                            new LlmMessage("system", "Reply with OK only."),
                            new LlmMessage("user", "ping")
                    ),
                    0
            );
            if (answer == null || answer.isBlank()) {
                return new Availability(false, "模型返回空响应");
            }
            return new Availability(true, "可用性测试通过");
        } catch (Exception ex) {
            return new Availability(false, ex.getMessage());
        }
    }

    /**
     * 提供方配置校验结果。
     *
     * @param available 是否通过
     * @param reason 未通过原因
     */
    record Availability(boolean available, String reason) {
    }
}
