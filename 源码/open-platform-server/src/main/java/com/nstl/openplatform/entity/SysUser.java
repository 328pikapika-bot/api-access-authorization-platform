package com.nstl.openplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
@TableName("sys_user")
public class SysUser {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 账号
     */
    private String userName;
    
    /**
     * 姓名
     */
    private String nickname;
    
    /**
     * 注册站点编码
     */
    private String userLibCode;
    
    /**
     * 注册站点名称
     */
    private String userLibName;
    
    /**
     * 用户站点类型
     */
    private String userLibType;
    
    /**
     * 用户类型
     */
    private String userType;
    
    /**
     * 用户角色(0-普通用户,1-管理员)
     */
    private Integer userRole;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 统一认证唯一标识
     */
    private String ssoId;

    /**
     * NSTL统一认证联合ID (unionid)
     */
    private String nstlUnionId;

    /**
     * NSTL统一认证用户ID (userid)
     */
    private String userId;

    /**
     * 所属单位
     */
    private String unitName;
    
    /**
     * 状态(1-正常,0-禁用)
     */
    private Integer status;
    
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
