package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 系统平台实体类
 */
@Data
@TableName("platform")
public class Platform {

    /**
     * 平台ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台名称
     */
    @NotBlank(message = "平台名称不能为空")
    @Size(max = 100, message = "平台名称长度不能超过100")
    private String name;

    /**
     * 描述
     */
    @Size(max = 5000, message = "描述长度不能超过5000")
    private String description;

    /**
     * 排序
     */
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;

    /**
     * 状态(0-禁用,1-启用)
     */
    private Integer status;

    /**
     * 发布状态(0-草稿,1-已发布,2-已撤回)
     */
    private Integer isPublished;

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
