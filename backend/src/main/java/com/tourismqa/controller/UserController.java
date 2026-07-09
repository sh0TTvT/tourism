package com.tourismqa.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.ChangePasswordRequest;
import com.tourismqa.dto.UpdateUserPreferencesRequest;
import com.tourismqa.dto.UpdateUserProfileRequest;
import com.tourismqa.dto.UserProfileResponse;
import com.tourismqa.security.UserPrincipal;
import com.tourismqa.service.UserAccountService;

import jakarta.validation.Valid;

/**
 * 用户资料控制器。
 * 使用场景：
 * 为前端个人中心提供当前登录用户信息读取接口。
 * 核心职责：
 * 1. 从安全上下文提取认证主体。
 * 2. 调用认证服务返回标准化用户资料。
 *
 * <p>框架作用：`@RestController` 对响应对象执行 JSON 序列化。</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * 获取当前登录用户信息。
     *
     * @param principal 当前认证主体
     * @return 用户资料响应
     */
    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userAccountService.profile(principal);
    }

    @PutMapping("/me/profile")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        return userAccountService.updateProfile(principal, request);
    }

    @PutMapping("/me/preferences")
    public UserProfileResponse updatePreferences(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return userAccountService.updatePreferences(principal, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(principal, request);
    }
}
