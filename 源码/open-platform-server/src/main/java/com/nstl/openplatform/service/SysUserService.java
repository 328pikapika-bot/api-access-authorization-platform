package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nstl.openplatform.entity.SysUser;
import java.util.Map;

/**
 * 用户服务接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface SysUserService extends IService<SysUser> {
    
    /**
     * 分页查询用户列表
     */
    Page<SysUser> getUserPage(Integer current, Integer size, Map<String, Object> params);
    
    /**
     * 更新用户状态
     */
    boolean updateStatus(Long id, Integer status);
    
    /**
     * 更新用户角色
     */
    boolean updateRole(Long id, Integer role);

    /**
     * 新增用户（手动添加）
     * @param user 用户信息
     * @return true-成功, false-失败
     */
    boolean addUser(SysUser user);

    /**
     * 校验联合ID并返回当前可用用户信息
     */
    SysUser validateAndPrepareUser(String nstlUnionId, Integer userRole, String userType);
}
