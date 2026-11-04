package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/admin/logs/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计日志
     */
    @AuditLog(module = "系统日志", businessType = "日志查询", operationType = OperationType.QUERY, description = "查询系统日志")
    @GetMapping
    public Result<Page<?>> getAuditLogPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer operationType,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) Integer operationStatus,
            @RequestParam(required = false) String clientIp,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        if (username != null && !username.isEmpty()) {
            params.put("username", username);
        }
        if (operationType != null) {
            params.put("operationType", operationType);
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            params.put("moduleName", moduleName);
        }
        if (operationStatus != null) {
            params.put("operationStatus", operationStatus);
        }
        if (clientIp != null && !clientIp.isEmpty()) {
            params.put("clientIp", clientIp);
        }
        if (hasText(startTime)) {
            params.put("startTime", startTime.trim());
        }
        if (hasText(endTime)) {
            params.put("endTime", endTime.trim());
        }

        Page<?> page = auditLogService.getAuditLogPage(current, size, params);
        return Result.success(page);
    }

    /**
     * 根据ID获取审计日志详情
     */
    @AuditLog(module = "系统日志", businessType = "日志详情查询", operationType = OperationType.QUERY, description = "查询系统日志详情")
    @GetMapping("/{id}")
    public Result<?> getAuditLogDetail(@PathVariable Long id) {
        var auditLog = auditLogService.getAuditLogDetail(id);
        if (auditLog == null) {
            return Result.error("日志不存在");
        }
        return Result.success(auditLog);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
