package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容信息实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("content_info")
public class ContentInfo {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 内容标题
     */
    private String title;
    
    /**
     * 内容分类
     */
    private String category;

    /**
     * 内容分类ID
     */
    private Long categoryId;

    /**
     * 所属平台ID
     */
    private Long platformId;

    /**
     * 所属栏目ID
     */
    private Long columnId;
    
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
