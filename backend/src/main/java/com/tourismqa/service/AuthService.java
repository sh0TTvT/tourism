package com.tourismqa.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.AuthResponse;
import com.tourismqa.dto.LoginRequest;
import com.tourismqa.dto.RegisterRequest;
import com.tourismqa.dto.UserProfileResponse;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.exception.ApiException;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.JwtService;
import com.tourismqa.security.UserPrincipal;

/**
 * 用户认证与账户初始化服务。
 * 使用场景：
 * 为注册、登录与个人资料查询接口提供统一的认证业务逻辑。
 * 核心职责：
 * 1. 执行注册与登录校验并维护用户持久化数据。
 * 2. 生成 JWT 令牌与登录态返回结构。
 * 3. 按系统配置处理首个管理员账号的自动提升策略。
 * 设计说明：
 * 通过 `DatabaseConnectionService` 作为前置可用性守卫，避免数据库不可达时继续执行业务流程。
 *
 * <p>框架作用：`@Service` 声明领域服务组件，默认由 Spring 以单例 Bean 托管。</p>
 */
@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DatabaseConnectionService databaseConnectionService;
    private final AppProperties appProperties;
    private final UserAccountService userAccountService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DatabaseConnectionService databaseConnectionService,
                       AppProperties appProperties,
                       UserAccountService userAccountService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.databaseConnectionService = databaseConnectionService;
        this.appProperties = appProperties;
        this.userAccountService = userAccountService;
    }

    /**
     * 完成用户注册并返回认证结果。
     *
     * @param request 注册请求，包含用户名、可选邮箱、显示名与明文密码
     * @return 包含访问令牌和用户资料的认证响应
     */
    public AuthResponse register(RegisterRequest request) {
        databaseConnectionService.ensureAvailable();

        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        String displayName = request.displayName().trim();

        if (userAccountRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "用户名已存在");
        }
        if (email != null && userAccountRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "邮箱已注册");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(resolveInitialRole());
        UserAccount saved = userAccountRepository.save(user);

        return toAuthResponse(saved);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验账号口令并生成登录态。
     *
     * @param request 登录请求，账号可为用户名或邮箱
     * @return 包含访问令牌和用户资料的认证响应
     */
    public AuthResponse login(LoginRequest request) {
        databaseConnectionService.ensureAvailable();

        String account = request.account().trim();
        UserAccount user = userAccountRepository
                .findByUsernameOrEmail(account, account.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "账号未注册，请先注册"));

        if (user.isBanned()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "账号已被封禁");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "密码错误");
        }

        UserAccount effectiveUser = ensureAdminBootstrapOnLogin(user);
        return toAuthResponse(effectiveUser);
    }

    /**
     * 读取当前登录用户资料并映射为前端响应对象。
     *
     * @param principal 当前认证主体
     * @return 用户资料响应
     */
    public UserProfileResponse profile(UserPrincipal principal) {
        return userAccountService.profile(principal);
    }

    /**
     * 将用户实体封装为认证响应。
     *
     * @param user 已持久化的用户实体
     * @return 认证响应对象
     */
    private AuthResponse toAuthResponse(UserAccount user) {
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        UserProfileResponse profile = userAccountService.toProfileResponse(user);
        return new AuthResponse(token, profile);
    }

    /**
     * 解析新注册用户的初始角色。
     *
     * @return USER 或 ADMIN 角色
     */
    private UserRole resolveInitialRole() {
        boolean firstUserAdmin = appProperties.getSecurity().isFirstUserAdmin();
        boolean noAdmin = !userAccountRepository.existsByRoleAndBannedFalse(UserRole.ADMIN);
        return firstUserAdmin && noAdmin ? UserRole.ADMIN : UserRole.USER;
    }

    /**
     * 在开启“首用户管理员”策略时，确保系统存在管理员账号。
     *
     * @param user 当前登录用户
     * @return 原用户或已提升为管理员后的用户
     */
    private UserAccount ensureAdminBootstrapOnLogin(UserAccount user) {
        if (!appProperties.getSecurity().isFirstUserAdmin()) {
            return user;
        }
        if (user.getRole() == UserRole.ADMIN) {
            return user;
        }
        if (userAccountRepository.existsByRoleAndBannedFalse(UserRole.ADMIN)) {
            return user;
        }
        user.setRole(UserRole.ADMIN);
        return userAccountRepository.save(user);
    }
}
