package com.tourismqa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用启动集成测试。
 * 使用场景：
 * 在 CI 或本地构建阶段验证 Spring 上下文可正常启动。
 * 核心职责：
 * 1. 加载完整应用上下文。
 * 2. 快速暴露配置缺失或 Bean 装配冲突。
 */
@SpringBootTest
class TourismQaApplicationTests {

    /**
     * 验证 Spring Boot 上下文加载成功。
     */
    @Test
    void contextLoads() {
    }
}
