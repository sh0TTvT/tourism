package com.tourismqa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 * 核心职责：
 * 1. 配置异步请求超时时间，支持长时间的流式响应。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时为10分钟（600000ms），支持长时间的流式对话
        configurer.setDefaultTimeout(600000L);
    }
}
