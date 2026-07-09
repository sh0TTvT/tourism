package com.tourismqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 旅游问答系统启动入口。
 * 使用场景：
 * 作为 Spring Boot 应用主类，负责触发组件扫描与自动配置加载。
 * 核心职责：
 * 1. 启动内嵌 Web 容器。
 * 2. 激活配置属性绑定扫描。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TourismQaApplication {

    /**
     * 应用主入口方法。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TourismQaApplication.class, args);
    }
}
