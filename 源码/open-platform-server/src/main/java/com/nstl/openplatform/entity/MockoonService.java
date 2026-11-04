package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mockoon 服务管理实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("mockoon_service")
public class MockoonService {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 服务地址
     */
    private String serviceUrl;

    /**
     * 服务说明
     */
    private String description;

    /**
     * 状态(0-禁用,1-启用)
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

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
}
