package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("audit_log")
public class AuditLog {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * NSTL统一认证联合ID
     */
    private String nstlUnionId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称（不持久化，通过关联查询或填充展示）
     */
    @TableField(exist = false)
    private String nickname;


    /**
     * 操作类型(1-查询,2-新增,3-修改,4-删除,5-导出,6-登录,7-登出,8-其他)
     */
    private Integer operationType;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 业务类型(如:接口发布,权限分配等)
     */
    private String businessType;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法(GET/POST/PUT/DELETE)
     */
    private String requestMethod;

    /**
     * 请求参数(JSON格式)
     */
    private String requestParams;

    /**
     * 响应结果(JSON格式)
     */
    private String responseResult;

    /**
     * 操作状态(1-成功,0-失败)
     */
    private Integer operationStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 执行耗时(毫秒)
     */
    private Long costTime;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
