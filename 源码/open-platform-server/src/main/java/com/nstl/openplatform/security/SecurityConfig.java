package com.nstl.openplatform.security;

import com.nstl.openplatform.service.SysUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;


/**
 * Spring Security配置
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(SysUserService sysUserService) {
        return new JwtAuthenticationFilter(sysUserService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // 允许匿名访问的接口
            .antMatchers("/auth/**").permitAll()
            .antMatchers("/api/frontend/my/**").authenticated()
            .antMatchers("/api/frontend/**").permitAll()
            .antMatchers("/sandbox/**").permitAll()  // 沙箱调试接口使用 X-API-KEY 认证，允许匿名访问
            .antMatchers("/admin/upload/image/view").permitAll()  // 图片查看接口允许匿名访问
            .antMatchers("/error").permitAll()

            // 对于管理员接口，除了过滤器认证，这里也显式要求 ADMIN 角色（可选，增强安全性）
            .antMatchers("/admin/**").hasRole("ADMIN")
            // 其他所有请求需要认证
            .anyRequest().authenticated()
            .and()
            .cors();


        // 添加 JWT 过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
