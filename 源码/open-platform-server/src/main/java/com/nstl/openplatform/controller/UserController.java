package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;
    private final ApplicationService applicationService;

    /**
     * 分页查询用户列表
     */
    @AuditLog(module = "权限管理", businessType = "用户查询", operationType = OperationType.QUERY, description = "查询用户列表")
    @GetMapping
    public Result<Page<SysUser>> getUserPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> params = new HashMap<>();
        putIfHasText(params, "userName", userName);
        putIfHasText(params, "email", email);
        putIfHasText(params, "userType", userType);
        putIfHasText(params, "startTime", startTime);
        putIfHasText(params, "endTime", endTime);

        Page<SysUser> page = sysUserService.getUserPage(current, size, params);
        return Result.success(page);
    }

    private void putIfHasText(Map<String, Object> params, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            params.put(key, value.trim());
        }
    }

    /**
     * 更新用户状态
     */
    @AuditLog(module = "权限管理", businessType = "用户状态更新", operationType = OperationType.UPDATE, description = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean success = sysUserService.updateStatus(id, status);
        return success ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    /**
     * 更新用户角色
     */
    @AuditLog(module = "权限管理", businessType = "用户角色更新", operationType = OperationType.UPDATE, description = "修改用户角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam Integer role) {
        boolean success = sysUserService.updateRole(id, role);
        return success ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @AuditLog(module = "权限管理", businessType = "用户删除", operationType = OperationType.DELETE, description = "删除系统用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id, Authentication authentication) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在或已删除");
        }

        Long currentUserId = getAuthenticatedUserId(authentication);
        if (id.equals(currentUserId)) {
            return Result.error("不能删除当前登录账号");
        }

        long assignmentCount = applicationService.count(
                new LambdaQueryWrapper<ContentApplication>().eq(ContentApplication::getUserId, id)
        );
        if (assignmentCount > 0) {
            return Result.error("该用户仍有接口权限分配，请先在“接口权限分配”中删除对应记录");
        }

        boolean success = sysUserService.removeById(id);
        return success ? Result.success("删除成功", true) : Result.error("删除失败");
    }

    /**
     * 新增用户
     */
    @AuditLog(module = "权限管理", businessType = "用户新增", operationType = OperationType.INSERT, description = "手动添加新用户")
    @PostMapping
    public Result<Void> addUser(@RequestBody SysUser user) {
        try {
            boolean success = sysUserService.addUser(user);
            return success ? Result.success("新增用户成功", null) : Result.error("新增用户失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增用户前校验联合ID并预览用户信息
     */
    @AuditLog(module = "权限管理", businessType = "用户新增预校验", operationType = OperationType.QUERY, description = "校验NSTL联合ID并预览用户信息")
    @GetMapping("/preview")
    public Result<SysUser> previewUser(@RequestParam String nstlUnionId,
                                       @RequestParam(required = false) Integer userRole,
                                       @RequestParam(required = false) String userType) {
        try {
            SysUser preparedUser = sysUserService.validateAndPrepareUser(nstlUnionId, userRole, userType);
            return Result.success(preparedUser);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Map<?, ?>)) {
            return null;
        }
        Object value = ((Map<?, ?>) details).get("userId");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
