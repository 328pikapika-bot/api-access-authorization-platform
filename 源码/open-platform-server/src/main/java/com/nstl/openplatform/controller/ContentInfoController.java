package com.nstl.openplatform.controller;

import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.service.ContentInfoService;
import com.nstl.openplatform.service.UserPermissionScopeService;
import com.nstl.openplatform.service.UserPermissionScopeService.PermissionScope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容信息控制器（从content_category表读取）
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class ContentInfoController {

    private final ContentInfoService contentInfoService;
    private final UserPermissionScopeService userPermissionScopeService;

    @AuditLog(module = "分类与内容", businessType = "内容列表查询", operationType = OperationType.QUERY, description = "获取已发布的内容列表")
    @GetMapping("/list")
    public Result<List<ContentCategory>> getContentList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        Result<List<ContentCategory>> permissionError = validateCategoryAccess(categoryId, authentication);
        if (permissionError != null) {
            return permissionError;
        }
        List<ContentCategory> contentList = contentInfoService.getPublishedContents(categoryId, category);
        return Result.success(contentList);
    }

    @AuditLog(module = "分类与内容", businessType = "内容详情查询", operationType = OperationType.QUERY, description = "获取内容详细信息")
    @GetMapping("/{id}")
    public Result<ContentCategory> getContentDetail(@PathVariable Long id, Authentication authentication) {
        Result<ContentCategory> permissionError = validateCategoryAccess(id, authentication);
        if (permissionError != null) {
            return permissionError;
        }
        ContentCategory content = contentInfoService.getContentDetail(id);
        if (content == null) {
            return Result.error("内容不存在或未发布");
        }
        return Result.success(content);
    }

    @AuditLog(module = "分类与内容", businessType = "内容搜索", operationType = OperationType.QUERY, description = "搜索内容")
    @GetMapping("/search")
    public Result<List<ContentCategory>> searchContents(@RequestParam String keyword) {
        // TODO: 实现搜索逻辑
        return Result.<List<ContentCategory>>success("搜索功能待实现", null);
    }

    private <T> Result<T> validateCategoryAccess(Long categoryId, Authentication authentication) {
        if (categoryId == null) {
            return Result.error(403, "必须指定已授权的分类");
        }
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (!userPermissionScopeService.canAccessCategory(scope, categoryId)) {
            return Result.error(403, "当前账号未分配该分类访问权限");
        }
        return null;
    }
}
