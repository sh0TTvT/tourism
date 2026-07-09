package com.tourismqa.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.AuthResponse;
import com.tourismqa.dto.LoginRequest;
import com.tourismqa.dto.RegisterRequest;
import com.tourismqa.service.AuthService;

import jakarta.validation.Valid;

/**
 * 认证接口控制器。
 * 使用场景：
 * 为注册与登录页面提供账号创建和令牌签发接口。
 * 核心职责：
 * 1. 执行请求体验证并转发至认证服务。
 * 2. 统一返回认证响应结构供前端保存登录态。
 * 3. 以明确的 HTTP 状态语义表达注册成功结果。
 *
 * <p>框架作用：`@RestController` 将返回对象序列化为 JSON，Bean 默认单例。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户账号。
     *
     * @param request 注册请求体
     * @return 注册后的认证响应
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 执行用户登录并签发访问令牌。
     *
     * @param request 登录请求体
     * @return 登录认证响应
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
