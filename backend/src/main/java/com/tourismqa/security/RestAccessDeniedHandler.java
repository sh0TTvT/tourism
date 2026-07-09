package com.tourismqa.security;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 无权限访问处理器。
 * 使用场景：
 * 当已登录用户访问超出权限范围的资源时，返回统一 403 JSON 响应。
 * 核心职责：
 * 1. 输出标准化权限拒绝响应体。
 * 2. 与全局错误协议保持一致的字段结构。
 *
 * <p>框架作用：作为 Spring Security `AccessDeniedHandler` 参与授权异常处理。</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理授权失败异常。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param accessDeniedException 权限拒绝异常
     * @throws IOException 当响应写入失败时抛出
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("message", "无权限访问该资源，仅管理员可操作");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
