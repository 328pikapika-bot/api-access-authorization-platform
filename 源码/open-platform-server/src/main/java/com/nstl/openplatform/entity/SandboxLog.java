package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 沙箱请求日志实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("sandbox_log")
public class SandboxLog {
    
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

    @TableField(exist = false)
    private String businessUserId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 分类ID

     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 接口名称
     */
    private String interfaceName;

    /**
     * 所属平台ID
     */
    private Long platformId;

    /**
     * 所属平台名称
     */
    private String platformName;

    /**
     * 使用的凭证

     */
    private String appKey;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数 (Query Params)
     */
    private String requestParams;

    /**
     * 请求头
     */
    private String requestHeaders;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 返回内容
     */
    private String responseContent;
    
    /**
     * 状态码
     */
    private Integer statusCode;
    
    /**
     * 耗时(ms)
     */
    private Integer costTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
