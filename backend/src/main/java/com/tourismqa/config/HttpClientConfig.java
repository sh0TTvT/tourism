package com.tourismqa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端基础配置。
 * 使用场景：
 * 为调用外部服务（如地理编码与大模型网关）提供统一的 `RestClient.Builder`。
 * 核心职责：
 * 1. 配置连接超时与读取超时。
 * 2. 暴露可复用的客户端构建器 Bean。
 *
 * <p>框架作用：`@Configuration` 声明配置类；`@Bean` 默认单例作用域。</p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * 构建默认 HTTP 请求工厂。
     *
     * @return 已配置超时的请求工厂
     */
    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时用于控制 TCP 建连阶段的最长等待时间。
        factory.setConnectTimeout(10000);
        // 读取超时用于控制响应体回传阶段的最长等待时间。
        factory.setReadTimeout(40000);
        return factory;
    }

    /**
     * 构建默认 `RestClient` 构建器。
     *
     * @return 带超时配置的 RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder(SimpleClientHttpRequestFactory factory) {
        return RestClient.builder().requestFactory(factory);
    }
}
