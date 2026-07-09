package com.tourismqa.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tourismqa.repository.UserAccountRepository;

/**
 * Spring Security 用户详情加载服务。
 * 使用场景：
 * 在认证阶段按用户名加载用户实体并转换为 `UserDetails`。
 * 核心职责：
 * 1. 连接账户仓储查询用户数据。
 * 2. 将领域实体转换为安全主体 `UserPrincipal`。
 *
 * <p>框架作用：`@Service` 组件会被 `DaoAuthenticationProvider` 自动注入使用。</p>
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public AppUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * 按用户名加载用户详情。
     *
     * @param username 用户名
     * @return Spring Security 用户详情对象
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }
}
