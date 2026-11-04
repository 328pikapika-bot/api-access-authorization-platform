package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ApiCredential;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.IApiCredentialService;
import com.nstl.openplatform.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ApiCredentialController {

    private final IApiCredentialService apiCredentialService;
    private final ApplicationService applicationService;
    private final SysUserService sysUserService;

    @PostMapping("/admin/credentials")
    @AuditLog(module = "接口权限分配", businessType = "接口凭证分配", operationType = OperationType.UPDATE)
    public Result<Void> saveApiCredentials(
            @RequestParam Long applicationId,
            @RequestBody List<ApiCredential> credentials) {
        try {
            if (credentials != null && !credentials.isEmpty()) {
                for (ApiCredential credential : credentials) {
                    credential.setApplicationId(applicationId);
                    LambdaQueryWrapper<ApiCredential> queryWrapper = new LambdaQueryWrapper<ApiCredential>()
                            .eq(ApiCredential::getApplicationId, applicationId)
                            .eq(ApiCredential::getCategoryId, credential.getCategoryId());
                    ApiCredential existing = apiCredentialService.getOne(queryWrapper);
                    if (existing != null) {
                        credential.setId(existing.getId());
                        apiCredentialService.updateById(credential);
                    } else {
                        apiCredentialService.save(credential);
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            log.error("保存接口凭证失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/admin/credentials")
    public Result<List<ApiCredential>> getApiCredentials(@RequestParam Long applicationId) {
        try {
            List<ApiCredential> credentials = apiCredentialService.list(
                    new LambdaQueryWrapper<ApiCredential>()
                            .eq(ApiCredential::getApplicationId, applicationId)
                            .orderByAsc(ApiCredential::getCategoryId)
            );
            return Result.success(credentials);
        } catch (Exception e) {
            log.error("获取接口凭证失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/credentials/{applicationId}")
    public Result<List<ApiCredential>> getMyCredentials(@PathVariable Long applicationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Result.error("请先登录");
            }
            Object principal = authentication.getPrincipal();
            Long userId = null;
            if (principal instanceof SysUser) {
                userId = ((SysUser) principal).getId();
            } else if (principal instanceof String) {
                String username = (String) principal;
                SysUser user = sysUserService.lambdaQuery()
                        .eq(SysUser::getUserName, username)
                        .one();
                if (user != null) {
                    userId = user.getId();
                }
            }
            if (userId == null) {
                return Result.error("无法获取用户信息");
            }
            ContentApplication app = applicationService.getById(applicationId);
            if (app == null || !userId.equals(app.getUserId())) {
                return Result.error("无权查看此凭证");
            }
            List<ApiCredential> credentials = apiCredentialService.list(
                    new LambdaQueryWrapper<ApiCredential>()
                            .eq(ApiCredential::getApplicationId, applicationId)
                            .orderByAsc(ApiCredential::getCategoryId)
            );
            return Result.success(credentials);
        } catch (Exception e) {
            log.error("获取接口凭证失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
