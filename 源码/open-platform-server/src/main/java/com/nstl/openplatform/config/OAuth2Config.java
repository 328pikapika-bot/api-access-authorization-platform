package com.nstl.openplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2单点登录配置
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sso")
public class OAuth2Config {

    /**
     * 是否启用SSO
     */
    private Boolean enabled = false;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端密钥
     */
    private String clientSecret;

    /**
     * 授权地址
     */
    private String authorizationUri;

    /**
     * 获取token地址
     */
    private String tokenUri;

    /**
     * 用户信息地址
     */
    private String userInfoUri;

    /**
     * 回调地址
     */
    private String redirectUri = "http://localhost:5173/callback";
}
