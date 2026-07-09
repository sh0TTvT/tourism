package com.tourismqa.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tourismqa.security.JwtAuthenticationFilter;
import com.tourismqa.security.RestAccessDeniedHandler;
import com.tourismqa.security.RestAuthenticationEntryPoint;

/**
 * Spring Security 核心配置。
 * 使用场景：
 * 该配置在应用启动时由 Spring 容器加载，用于定义无状态 JWT 鉴权链路与接口访问策略。
 * 核心职责：
 * 1. 注册 {@link SecurityFilterChain} 并声明公开/受限接口。
 * 2. 注入 DAO 鉴权提供器与密码编码器。
 * 3. 暴露 {@link AuthenticationManager} 供登录流程执行认证。
 * 设计说明：
 * 采用 `SessionCreationPolicy.STATELESS`，确保服务端不维护会话状态，认证信息完全由 JWT 承载。
 *
 * <p>框架作用：`@Configuration` 声明配置类；`@Bean` 默认单例作用域，由 Spring 容器托管生命周期。</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 构建应用的安全过滤器链。
     *
     * @param http Spring Security HTTP 配置入口
     * @param jwtFilter JWT 解析与鉴权过滤器
     * @param authProvider 基于用户库的 DAO 鉴权提供器
     * @param restAuthenticationEntryPoint 未认证访问处理器
     * @param restAccessDeniedHandler 无权限访问处理器
     * @return 已构建完成的过滤器链实例
     * @throws Exception 当安全链构建失败时抛出
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   DaoAuthenticationProvider authProvider,
                                                   RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        // 前端静态资源与认证接口允许匿名访问。
                        .requestMatchers("/", "/index.html", "/app.js", "/app.css", "/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/models").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/weather/**").permitAll()
                        // 管理端与知识图谱写操作仅管理员可访问。
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/kg/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 提供密码哈希编码器。
     *
     * @return BCrypt 编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 组装 DAO 认证提供器。
     *
     * @param userDetailsService 用户详情加载服务
     * @param passwordEncoder 密码编码器
     * @return 配置完成的 DAO 认证提供器
     */
    @Bean
    public DaoAuthenticationProvider authProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 暴露认证管理器以供登录流程调用。
     *
     * @param config Spring Security 认证配置
     * @return 认证管理器
     * @throws Exception 当无法获取认证管理器时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
