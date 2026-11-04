package com.nstl.openplatform.dto;

import lombok.Data;

/**
 * OAuth2 Token响应DTO（NSTL格式）
 *
 * NSTL返回格式: access_token=TGT-201-xxx&expires=28389
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
public class OAuth2TokenDTO {

    /**
     * 访问令牌
     */
    private String access_token;

    /**
     * 过期时间(秒)
     */
    private Integer expires;
}
