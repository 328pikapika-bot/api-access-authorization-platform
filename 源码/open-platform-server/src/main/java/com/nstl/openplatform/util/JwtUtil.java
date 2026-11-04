package com.nstl.openplatform.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String SECRET = java.util.Objects.requireNonNull(System.getenv("OPEN_PLATFORM_JWT_SECRET"), "OPEN_PLATFORM_JWT_SECRET must be configured");
    private static final long EXPIRATION = 86400000; // 24小时

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成JWT token
     */
    public static String generateToken(String userId, String username, String nickname, Integer userRole) {
        return generateToken(userId, username, nickname, userRole, null);
    }

    /**
     * 生成JWT token（带nstlUnionId）
     */
    public static String generateToken(String userId, String username, String nickname, Integer userRole, String nstlUnionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);

        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("nickname", nickname)
                .claim("userRole", userRole);

        if (nstlUnionId != null) {
            builder.claim("nstlUnionId", nstlUnionId);
        }

        return builder
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(KEY)
                .compact();
    }

    /**
     * 从token中获取用户角色
     */
    public static Integer getUserRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object userRole = claims.get("userRole");
            if (userRole instanceof Number) {
                return ((Number) userRole).intValue();
            }
            return null;
        } catch (Exception e) {
            log.error("解析token中的userRole失败", e);
            return null;
        }
    }



    /**
     * 从token中获取用户昵称
     */
    public static String getNicknameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("nickname", String.class);
        } catch (Exception e) {
            log.error("解析token失败", e);
            return null;
        }
    }


    /**
     * 从token中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("解析token失败", e);
            return null;
        }
    }

    /**
     * 从token中获取用户名
     */
    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("解析token失败", e);
            return null;
        }
    }

    /**
     * 从token中获取NSTL联合ID
     */
    public static String getNstlUnionIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("nstlUnionId", String.class);
        } catch (Exception e) {
            log.error("解析token中的nstlUnionId失败", e);
            return null;
        }
    }

    /**
     * 验证token是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("验证token失败", e);
            return false;
        }
    }
}
