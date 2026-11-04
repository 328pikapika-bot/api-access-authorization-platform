package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.entity.AuditLog;

import java.util.Map;

/**
 * 审计日志服务接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface AuditLogService {

    /**
     * 分页查询审计日志
     *
     * @param current  当前页
     * @param size     每页大小
     * @param params   查询参数
     * @return 分页结果
     */
    Page<AuditLog> getAuditLogPage(Integer current, Integer size, Map<String, Object> params);

    /**
     * 根据ID查询审计日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    AuditLog getAuditLogDetail(Long id);
}
