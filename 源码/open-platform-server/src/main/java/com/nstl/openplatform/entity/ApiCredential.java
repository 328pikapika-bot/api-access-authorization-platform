package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口凭证分配实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("api_credential")
public class ApiCredential {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联申请ID
     */
    private Long applicationId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称（非数据库字段，仅用于显示）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 平台名称（非数据库字段，仅用于显示）
     */
    @TableField(exist = false)
    private String platformName;

    /**
     * Open ID
     */
    private String openId;

    /**
     * Client ID
     */
    private String clientId;

    /**
     * Token
     */
    private String token;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 说明
     */
    private String description;

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
