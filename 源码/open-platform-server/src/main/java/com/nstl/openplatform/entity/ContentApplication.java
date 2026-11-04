package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 接口权限分配记录实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("content_application")
public class ContentApplication {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 授权用户ID
     */
    private Long userId;

    /**
     * NSTL统一认证联合ID
     */
    private String nstlUnionId;

    /**
     * 授权用户账号
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 授权用户姓名
     */
    @TableField(exist = false)
    private String nickname;

    /**
     * NSTL统一认证用户ID（sys_user.user_id）
     */
    @TableField(exist = false)
    private String businessUserId;

    /**
     * 当前分配记录是否仍然有效
     */
    @TableField(exist = false)
    private Boolean validAssignment;

    /**
     * 当前分配记录失效原因
     */
    @TableField(exist = false)
    private String invalidReason;

    /**
     * 授权范围明细（平台/栏目/一级分类），用于前端展示和回显。
     */
    @TableField(exist = false)
    private List<Map<String, Object>> scopeConfigs;

    /**
     * 授权范围摘要。
     */
    @TableField(exist = false)
    private String scopeSummary;
    
    /**
     * 授权内容ID（已废弃，使用category_ids）
     */
    @Deprecated
    @TableField(insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
    private Long contentId;

    /**
     * 平台ID
     */
    private Long platformId;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 栏目ID
     */
    private Long columnId;

    /**
     * 栏目名称
     */
    private String columnName;

    /**
     * 授权分类ID列表（JSON数组）
     */
    private String categoryIds;

    /**
     * 授权分类名称列表（逗号分隔）
     */
    private String categoryNames;

    /**
     * 授权接口配置列表（JSON数组）
     */
    private String interfaceConfigs;

    /**
     * 授权用户单位
     */
    private String unitName;
    
    /**
     * 授权用户部门
     */
    private String deptName;
    
    /**
     * 分配说明
     */
    private String reason;
    
    /**
     * 历史申请文件路径
     */
    private String filePath;
    
    /**
     * 分配状态(0-未生效,1-已生效,2-已停用)
     */
    private Integer status;
    
    /**
     * 历史驳回原因
     */
    private String rejectReason;
    
    /**
     * 发放的Key
     */
    private String appKey;
    
    /**
     * 发放的Secret(加密存储)
     */
    private String appSecret;
    
    /**
     * 分配时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime applyTime;
    
    /**
     * 生效时间
     */
    private LocalDateTime auditTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;
}
