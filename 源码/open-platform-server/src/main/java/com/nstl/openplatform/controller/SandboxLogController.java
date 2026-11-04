package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.SandboxLog;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.SandboxLogService;
import com.nstl.openplatform.service.SysUserService;
import com.nstl.openplatform.util.DateTimeRangeUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/logs/sandbox")
@RequiredArgsConstructor
public class SandboxLogController {

    private final SandboxLogService sandboxLogService;
    private final SysUserService sysUserService;

    @AuditLog(module = "沙箱请求日志", businessType = "查询", operationType = OperationType.QUERY)
    @GetMapping("/page")
    public Result<Page<SandboxLog>> getLogPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String interfaceName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Page<SandboxLog> logPage = new Page<>(page, size);
        LambdaQueryWrapper<SandboxLog> queryWrapper = new LambdaQueryWrapper<>();
        if (hasText(username)) {
            String trimmedUsername = username.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(SandboxLog::getUsername, trimmedUsername)
                    .or()
                    .like(SandboxLog::getNickname, trimmedUsername));
        }
        if (hasText(interfaceName)) {
            String trimmedInterfaceName = interfaceName.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(SandboxLog::getInterfaceName, trimmedInterfaceName)
                    .or()
                    .like(SandboxLog::getCategoryName, trimmedInterfaceName));
        }
        LocalDateTime queryStartTime = DateTimeRangeUtil.normalizeStartTime(startTime);
        LocalDateTime queryEndTime = DateTimeRangeUtil.normalizeEndTime(endTime);
        if (queryStartTime != null) {
            queryWrapper.ge(SandboxLog::getCreateTime, queryStartTime);
        }
        if (queryEndTime != null) {
            queryWrapper.le(SandboxLog::getCreateTime, queryEndTime);
        }
        queryWrapper.orderByDesc(SandboxLog::getCreateTime)
                .orderByDesc(SandboxLog::getId);

        Page<SandboxLog> result = sandboxLogService.page(logPage, queryWrapper);
        result.getRecords().forEach(this::enrichLog);
        return Result.success(result);
    }

    @AuditLog(module = "沙箱请求日志", businessType = "查看详情", operationType = OperationType.QUERY)
    @GetMapping("/{id}")
    public Result<SandboxLog> getDetail(@PathVariable Long id) {
        SandboxLog log = sandboxLogService.getById(id);
        if (log == null) {
            return Result.error("日志不存在");
        }
        enrichLog(log);
        return Result.success(log);
    }

    private void enrichLog(SandboxLog log) {
        if (log == null) {
            return;
        }
        SysUser user = null;
        if (log.getUserId() != null) {
            user = sysUserService.getById(log.getUserId());
        }
        if (user == null && hasText(log.getNstlUnionId())) {
            user = sysUserService.lambdaQuery()
                    .eq(SysUser::getNstlUnionId, log.getNstlUnionId().trim())
                    .one();
        }
        if (user == null) {
            return;
        }
        if (!hasText(log.getBusinessUserId())) {
            log.setBusinessUserId(user.getUserId());
        }
        if (!hasText(log.getUsername())) {
            log.setUsername(user.getUserName());
        }
        if (!hasText(log.getNickname())) {
            log.setNickname(user.getNickname());
        }
        if (!hasText(log.getNstlUnionId())) {
            log.setNstlUnionId(user.getNstlUnionId());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
