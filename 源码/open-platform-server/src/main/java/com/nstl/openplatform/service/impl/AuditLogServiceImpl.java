package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.entity.AuditLog;
import com.nstl.openplatform.mapper.AuditLogMapper;
import com.nstl.openplatform.service.AuditLogService;
import com.nstl.openplatform.util.DateTimeRangeUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 审计日志服务实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public Page<AuditLog> getAuditLogPage(Integer current, Integer size, Map<String, Object> params) {
        Page<AuditLog> page = new Page<>(current, size);
        QueryWrapper<AuditLog> queryWrapper = new QueryWrapper<>();

        // 按用户ID查询
        if (params.containsKey("userId")) {
            queryWrapper.eq("al.user_id", params.get("userId"));
        }

        // 按用户名或昵称查询（模糊匹配）
        if (params.containsKey("username")) {
            String keyword = String.valueOf(params.get("username")).trim();
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                        .like("u.user_name", keyword)
                        .or()
                        .like("u.nickname", keyword)
                        .or()
                        .like("al.username", keyword));
            }
        }

        // 按操作类型查询
        if (params.containsKey("operationType")) {
            queryWrapper.eq("al.operation_type", params.get("operationType"));
        }

        // 按模块名称查询（模糊匹配）
        if (params.containsKey("moduleName")) {
            queryWrapper.like("al.module_name", params.get("moduleName"));
        }

        // 按操作状态查询
        if (params.containsKey("operationStatus")) {
            queryWrapper.eq("al.operation_status", params.get("operationStatus"));
        }

        // 按时间范围查询
        if (params.containsKey("startTime")) {
            LocalDateTime startTime = DateTimeRangeUtil.normalizeStartTime(params.get("startTime"));
            if (startTime != null) {
                queryWrapper.ge("al.create_time", startTime);
            }
        }
        if (params.containsKey("endTime")) {
            LocalDateTime endTime = DateTimeRangeUtil.normalizeEndTime(params.get("endTime"));
            if (endTime != null) {
                queryWrapper.le("al.create_time", endTime);
            }
        }

        // 按客户端IP查询
        if (params.containsKey("clientIp")) {
            queryWrapper.eq("al.client_ip", params.get("clientIp"));
        }

        // 按创建时间倒序排序
        queryWrapper.orderByDesc("al.create_time");

        // 使用自定义联表查询方法
        return auditLogMapper.selectAuditLogPage(page, queryWrapper);
    }

    @Override
    public AuditLog getAuditLogDetail(Long id) {
        return auditLogMapper.selectAuditLogDetail(id);
    }

}
