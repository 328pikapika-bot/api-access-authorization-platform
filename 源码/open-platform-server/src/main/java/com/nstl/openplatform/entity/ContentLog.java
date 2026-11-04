package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容访问日志实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("content_log")
public class ContentLog {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 调用者ID
     */
    private Long userId;

    /**
     * NSTL统一认证联合ID
     */
    private String nstlUnionId;

    /**
     * 访问路径
     */
    @TableField("content_path")
    private String contentPath;
    
    /**
     * 请求参数(JSON)
     */
    private String requestParams;
    
    /**
     * 返回数据(JSON)
     */
    private String responseData;
    
    /**
     * HTTP状态码
     */
    private Integer statusCode;
    
    /**
     * 耗时(ms)
     */
    private Integer costTime;
    
    /**
     * 调用时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
