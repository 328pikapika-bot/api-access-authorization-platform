package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口配置实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("interface_config")
public class InterfaceConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联分类ID
     */
    private Long categoryId;

    /**
     * 接口路径
     */
    private String apiPath;

    /**
     * 请求方法(GET/POST/PUT/DELETE)
     */
    private String apiMethod;

    /**
     * 默认参数(JSON)
     */
    private String defaultParams;

    /**
     * 默认请求头(JSON)
     */
    private String defaultHeaders;

    /**
     * 请求体模板
     */
    private String bodyTemplate;

    /**
     * 请求体类型(none, form-data, x-www-form-urlencoded, raw)
     */
    private String bodyType;

    /**
     * 表单数据(JSON)
     */
    private String bodyFormData;

    /**
     * Content-Type

     */
    private String contentType;

    /**
     * 关联Mockoon服务ID
     */
    private Long mockoonServiceId;

    /**
     * 接口说明
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
     * 关联的Mockoon服务（非数据库字段）
     */
    @TableField(exist = false)
    private MockoonService mockoonService;
}
