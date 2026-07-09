package com.tourismqa.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;

/**
 * 登录用户安全主体。
 * 使用场景：
 * 作为 Spring Security 上下文中的认证主体，向业务层暴露用户标识与权限信息。
 * 核心职责：
 * 1. 适配 `UserAccount` 到 `UserDetails`。
 * 2. 提供用户 ID、角色、邮箱等业务常用属性访问器。
 * 3. 输出标准化角色权限集合。
 */
public class UserPrincipal implements UserDetails {

    private final UserAccount user;

    public UserPrincipal(UserAccount user) {
        this.user = user;
    }

    /**
     * 获取用户主键。
     *
     * @return 用户主键
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * 获取显示名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return user.getDisplayName();
    }

    /**
     * 获取邮箱地址。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * 获取角色枚举。
     *
     * @return 用户角色
     */
    public UserRole getRole() {
        return user.getRole();
    }

    /**
     * 判断当前主体是否为管理员。
     *
     * @return 管理员返回 true
     */
    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }

    public boolean isBanned() {
        return user.isBanned();
    }

    public String getBanReason() {
        return user.getBanReason();
    }

    /**
     * 生成 Spring Security 权限集合。
     *
     * @return 权限列表
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /**
     * 获取密码哈希。
     *
     * @return 密码哈希字符串
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * 获取登录用户名。
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 账户是否未过期。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isAccountNonLocked() {
        return !user.isBanned();
    }

    /**
     * 凭据是否未过期。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否可用。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isEnabled() {
        return !user.isBanned();
    }
}
