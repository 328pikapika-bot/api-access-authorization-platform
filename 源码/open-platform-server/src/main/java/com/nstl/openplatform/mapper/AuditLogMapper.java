package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 审计日志Mapper接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 分页查询审计日志（关联用户表获取昵称）
     */
    @Select({
            "SELECT al.id, al.user_id, al.nstl_union_id,",
            "COALESCE(NULLIF(u.user_name, ''), al.username) AS username,",
            "COALESCE(NULLIF(u.nickname, ''), NULLIF(u.user_name, ''), al.username) AS nickname,",
            "al.operation_type, al.module_name, al.business_type, al.request_url, al.request_method,",
            "al.request_params, al.response_result, al.operation_status, al.error_message,",
            "al.client_ip, al.cost_time, al.create_time",
            "FROM audit_log al LEFT JOIN sys_user u ON al.user_id = u.id ${ew.customSqlSegment}"
    })
    Page<AuditLog> selectAuditLogPage(Page<AuditLog> page, @Param(Constants.WRAPPER) Wrapper<AuditLog> wrapper);

    /**
     * 查询审计日志详情（关联用户表获取昵称）
     */
    @Select({
            "SELECT al.id, al.user_id, al.nstl_union_id,",
            "COALESCE(NULLIF(u.user_name, ''), al.username) AS username,",
            "COALESCE(NULLIF(u.nickname, ''), NULLIF(u.user_name, ''), al.username) AS nickname,",
            "al.operation_type, al.module_name, al.business_type, al.request_url, al.request_method,",
            "al.request_params, al.response_result, al.operation_status, al.error_message,",
            "al.client_ip, al.cost_time, al.create_time",
            "FROM audit_log al LEFT JOIN sys_user u ON al.user_id = u.id",
            "WHERE al.id = #{id} LIMIT 1"
    })
    AuditLog selectAuditLogDetail(@Param("id") Long id);
}
