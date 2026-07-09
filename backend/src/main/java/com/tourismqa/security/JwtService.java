package com.tourismqa.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.tourismqa.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 令牌服务。
 * 使用场景：
 * 在登录成功后签发访问令牌，并在请求鉴权阶段解析与校验令牌有效性。
 * 核心职责：
 * 1. 根据用户主体生成签名 JWT。
 * 2. 从令牌中提取用户名与声明信息。
 * 3. 校验令牌与当前用户的一致性及过期状态。
 *
 * <p>框架作用：`@Service` 声明安全基础服务 Bean。</p>
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(AppProperties appProperties) {
        String raw = appProperties.getSecurity().getJwtSecret();
        if (raw == null || raw.length() < 32) {
            throw new IllegalArgumentException("JWT secret 至少需要 32 个字符");
        }
        this.secretKey = Keys.hmacShaKeyFor(raw.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = appProperties.getSecurity().getJwtExpirationMinutes();
    }

    /**
     * 为指定用户主体生成 JWT。
     *
     * @param principal 用户安全主体
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getUserId())
                .claim("name", principal.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从令牌中提取用户名。
     *
     * @param token JWT 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 校验令牌与用户主体是否匹配且未过期。
     *
     * @param token JWT 令牌
     * @param principal 用户安全主体
     * @return 令牌有效时返回 true
     */
    public boolean isValidToken(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        String username = claims.getSubject();
        Date expiration = claims.getExpiration();
        return username.equals(principal.getUsername()) && expiration.after(new Date());
    }

    /**
     * 解析令牌中的声明负载。
     *
     * @param token JWT 令牌
     * @return 解析后的 Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
