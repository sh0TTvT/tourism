package com.tourismqa.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.AdminUserItemResponse;
import com.tourismqa.dto.AdminUserPublishedContentResponse;
import com.tourismqa.dto.AdminUserUpdateRequest;
import com.tourismqa.dto.UpdateUserRoleRequest;
import com.tourismqa.service.AdminUserService;

import jakarta.validation.Valid;

/**
 * 管理端用户管理控制器。
 * 使用场景：
 * 面向系统后台提供用户列表查询与角色调整能力。
 * 核心职责：
 * 1. 接收管理员操作请求并校验输入。
 * 2. 调用用户管理服务执行角色变更规则。
 * 3. 通过权限注解确保仅管理员访问。
 *
 * <p>框架作用：`@RestController` 与 `@PreAuthorize` 共同实现 REST 暴露与授权拦截。</p>
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 查询用户列表（后台管理视图）。
     *
     * @return 用户信息列表
     */
    @GetMapping
    public List<AdminUserItemResponse> listUsers() {
        return adminUserService.listUsers();
    }

    /**
     * 修改指定用户角色。
     *
     * @param userId 用户主键
     * @param request 角色更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{userId}/role")
    public AdminUserItemResponse updateUserRole(@PathVariable Long userId,
                                                @Valid @RequestBody UpdateUserRoleRequest request) {
        return adminUserService.updateUserRole(userId, request);
    }

    /**
     * 更新指定用户的后台可维护信息。
     *
     * @param userId 用户主键
     * @param request 用户更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{userId}")
    public AdminUserItemResponse updateUser(@PathVariable Long userId,
                                            @Valid @RequestBody AdminUserUpdateRequest request) {
        return adminUserService.updateUser(userId, request);
    }

    @GetMapping("/{userId}/published-content")
    public AdminUserPublishedContentResponse getPublishedContent(@PathVariable Long userId) {
        return adminUserService.getPublishedContent(userId);
    }
}
