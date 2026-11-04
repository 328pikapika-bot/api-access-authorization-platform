package com.nstl.openplatform.service;

import com.nstl.openplatform.dto.SandboxRequest;

import java.util.Map;

/**
 * 沙箱服务接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface SandboxService {

    /**
     * 执行沙箱调试请求
     *
     * @param appKey  开发者 API Key
     * @param request 沙箱请求参数
     * @return Mock 响应数据
     */
    Map<String, Object> executeDebug(String appKey, SandboxRequest request);

    /**
     * 管理员执行沙箱调试请求（无需 API Key）
     *
     * @param request 沙箱请求参数
     * @return Mock 响应数据
     */
    Map<String, Object> executeDebugForAdmin(SandboxRequest request);
}

