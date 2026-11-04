package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 栏目实体类
 */
@Data
@TableName("platform_column")
public class PlatformColumn {

    /**
     * 栏目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属平台ID
     */
    private Long platformId;

    /**
     * 栏目名称
     */
    @NotBlank(message = "栏目名称不能为空")
    @Size(max = 100, message = "栏目名称长度不能超过100")
    private String name;

    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500")
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
