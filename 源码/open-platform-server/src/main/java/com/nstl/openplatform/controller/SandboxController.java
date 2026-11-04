package com.nstl.openplatform.controller;

import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.dto.SandboxRequest;
import com.nstl.openplatform.service.SandboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxService sandboxService;

    @PostMapping("/debug")
    public ResponseEntity<Result<Object>> debug(
            HttpServletRequest request,
            @RequestBody(required = false) SandboxRequest sandboxReq) {

        String apiKey = extractApiKey(request);
        SandboxRequest safeReq = sandboxReq != null ? sandboxReq : new SandboxRequest();
        log.info("沙箱调试请求 - 分类ID: {}, 路径: {}, 方法: {}, API Key: {}...",
                safeReq.getCategoryId(),
                safeReq.getContentPath(),
                safeReq.getMethod(),
                apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);

        Map<String, Object> response = sandboxService.executeDebug(apiKey, safeReq);
        return buildSandboxResponse(response);
    }

    @GetMapping("/debug")
    public ResponseEntity<Result<Object>> debugGet(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "contentPath", required = false) String contentPath,
            HttpServletRequest request) {

        String apiKey = extractApiKey(request);

        SandboxRequest sandboxReq = new SandboxRequest();
        sandboxReq.setCategoryId(categoryId);
        sandboxReq.setContentPath(contentPath);
        sandboxReq.setMethod("GET");

        Map<String, Object> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (!"categoryId".equals(name) && !"contentPath".equals(name)) {
                params.put(name, request.getParameter(name));
            }
        }
        sandboxReq.setParams(params);

        log.info("沙箱 GET 调试请求 - 分类ID: {}, 路径: {}, API Key: {}...",
                categoryId,
                contentPath,
                apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);

        Map<String, Object> response = sandboxService.executeDebug(apiKey, sandboxReq);
        return buildSandboxResponse(response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = request.getHeader("x-api-key");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        return apiKey.trim();
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("沙箱服务运行正常");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/debug")
    public ResponseEntity<Result<Object>> adminDebug(@RequestBody(required = false) SandboxRequest sandboxReq) {
        SandboxRequest safeReq = sandboxReq != null ? sandboxReq : new SandboxRequest();
        log.info("管理员沙箱调试请求 - 分类ID: {}, 路径: {}, 方法: {}",
                safeReq.getCategoryId(),
                safeReq.getContentPath(),
                safeReq.getMethod());

        Map<String, Object> response = sandboxService.executeDebugForAdmin(safeReq);
        return buildSandboxResponse(response);
    }

    private ResponseEntity<Result<Object>> buildSandboxResponse(Map<String, Object> response) {
        Integer code = (Integer) response.get("code");
        if (code != null && (code == 400 || code == 401 || code == 403 || code == 500)) {
            String message = (String) response.get("message");
            return ResponseEntity
                    .status(resolveHttpStatus(code))
                    .body(Result.error(code, message != null ? message : "系统处理失败"));
        }

        Integer httpStatus = (Integer) response.get("httpStatus");
        return ResponseEntity
                .status(resolveHttpStatus(httpStatus))
                .body(Result.success(response));
    }

    private HttpStatus resolveHttpStatus(Integer statusCode) {
        if (statusCode == null) {
            return HttpStatus.OK;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (IllegalArgumentException ex) {
            return HttpStatus.OK;
        }
    }
}
