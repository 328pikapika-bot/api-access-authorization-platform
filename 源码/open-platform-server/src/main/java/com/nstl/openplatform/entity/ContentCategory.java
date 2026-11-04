package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容分类实体类
 */
@Data
@TableName("content_category")
public class ContentCategory {
    
    /**
     * 分类ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String name;

    /**
     * 所属栏目ID
     */
    private Long columnId;

    /**
     * 父分类ID
     */

    private Long parentId;

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
     * 内容标题
     */
    private String title;

    /**
     * Markdown格式文档内容
     */
    private String docContent;

    /**
     * 发布状态(0-草稿,1-已发布,2-已撤回)
     */
    private Integer isPublished;

    /**
     * 权限状态(0-关闭,1-开放)
     */
    private Integer isOpen;

    /**
     * 类型(content-内容, api-接口)
     */
    private String type;

    /**
     * 是否可以申请(0-不允许申请, 1-允许申请)
     */
    private Integer canApply;

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
     * 子分类列表 (非数据库字段)
     */
    @TableField(exist = false)
    private List<ContentCategory> children;
}
