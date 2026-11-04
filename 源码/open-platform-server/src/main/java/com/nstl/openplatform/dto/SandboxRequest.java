package com.nstl.openplatform.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 沙箱调试请求 DTO
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
public class SandboxRequest {

    /**
     * 接口对应的分类ID（必填），用于验证用户是否已申请该接口
     */
    @NotNull(message = "接口分类ID不能为空")
    private Long categoryId;

    /**
     * 接口路径，如 /api/doc/search（可选，若不传则从接口配置中获取）
     */
    private String contentPath;

    /**
     * HTTP 请求方法：GET、POST、PUT、DELETE 等（可选，若不传则从接口配置中获取）
     */
    private String method;

    /**
     * 请求参数
     */
    private Map<String, Object> params;

    /**
     * 请求体数据（用于 POST/PUT 等）
     */
    private Object body;

    /**
     * 请求体类型
     */
    private String bodyType;

    /**
     * 表单数据
     */
    private Map<String, Object> bodyFormData;

    /**
     * 额外的请求头

     */
    private Map<String, String> headers;

    /**
     * 指定的 Mockoon 服务 ID（可选，主要用于管理员调试配置）
     */
    private Long mockoonServiceId;
}

