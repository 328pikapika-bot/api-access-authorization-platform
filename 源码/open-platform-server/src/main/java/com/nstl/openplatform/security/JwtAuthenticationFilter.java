package com.nstl.openplatform.security;

import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.SysUserService;
import com.nstl.openplatform.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;


import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SysUserService sysUserService;

    public JwtAuthenticationFilter(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && JwtUtil.validateToken(jwt)) {
                Long userId = JwtUtil.getUserIdFromToken(jwt);
                SysUser currentUser = userId == null ? null : sysUserService.getById(userId);

                if (currentUser != null && Integer.valueOf(1).equals(currentUser.getStatus())) {
                    String username = StringUtils.hasText(currentUser.getUserName())
                            ? currentUser.getUserName()
                            : JwtUtil.getUsernameFromToken(jwt);
                    String nickname = currentUser.getNickname();
                    Integer userRole = currentUser.getUserRole();
                    String nstlUnionId = currentUser.getNstlUnionId();
                    log.info("Token验证通过 - 用户名: {}, 角色值: {}", username, userRole);
                    java.util.List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                    if (Integer.valueOf(1).equals(userRole)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        log.info("用户 [{}] 已授予 ROLE_ADMIN 权限", username);
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        log.info("用户 [{}] 暂无管理员权限", username);
                    }

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    // 将 userId、username、nickname、nstlUnionId 存入 details 中，方便后续获取
                    java.util.Map<String, Object> details = new java.util.HashMap<>();
                    details.put("userId", userId);
                    details.put("username", username);
                    details.put("nickname", nickname);
                    details.put("nstlUnionId", nstlUnionId);
                    authentication.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else if (userId != null) {
                    log.warn("Token对应用户不存在、已删除或已禁用 - 用户ID: {}", userId);
                }

            }
        } catch (Exception ex) {
            log.error("无法在安全上下文中设置用户认证", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
