package com.tourismqa.security;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 未认证访问入口处理器。
 * 使用场景：
 * 当请求访问受保护资源但未携带有效登录态时，统一输出 JSON 错误响应。
 * 核心职责：
 * 1. 返回 401 状态码。
 * 2. 根据接口类型生成差异化提示文案。
 * 3. 保持错误响应结构与全局异常处理器一致。
 *
 * <p>框架作用：作为 Spring Security `AuthenticationEntryPoint` 注入异常处理链。</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理未认证访问异常。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authException 认证异常
     * @throws IOException 当响应写入失败时抛出
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = request.getRequestURI().startsWith("/api/chat")
                ? "请先登录后再问答"
                : "请先登录后再操作";

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
