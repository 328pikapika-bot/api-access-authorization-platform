package com.nstl.openplatform.controller;

import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.ContentInfoService;
import com.nstl.openplatform.service.PlatformColumnService;
import com.nstl.openplatform.service.PlatformService;
import com.nstl.openplatform.service.UserPermissionScopeService;
import com.nstl.openplatform.service.UserPermissionScopeService.PermissionScope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 前端公开接口控制器
 * 只提供已发布状态的查询接口，不暴露所有数据
 */
@RestController
@RequestMapping("/api/frontend")
@RequiredArgsConstructor
public class FrontendController {

    private final PlatformService platformService;
    private final PlatformColumnService platformColumnService;
    private final ContentCategoryService contentCategoryService;
    private final ContentInfoService contentInfoService;
    private final UserPermissionScopeService userPermissionScopeService;

    /**
     * 获取所有已发布的平台列表
     * 前端首页平台下拉菜单使用
     */
    @GetMapping("/platforms")
    public Result<List<Platform>> getPublishedPlatforms() {
        List<Platform> platforms = platformService.query()
                .eq("is_published", 1)
                .orderByAsc("sort")
                .list();
        return Result.success(platforms);
    }

    @GetMapping("/my/platforms")
    public Result<List<Platform>> getMyPublishedPlatforms(Authentication authentication) {
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (scope.getPlatformIds().isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Platform> platforms = platformService.query()
                .in("id", scope.getPlatformIds())
                .eq("status", 1)
                .eq("is_published", 1)
                .orderByAsc("sort")
                .list();
        return Result.success(platforms);
    }

    /**
     * 根据平台ID获取已发布的栏目列表
     * 前端页面左侧栏目树使用
     */
    @GetMapping("/platforms/{platformId}/columns")
    public Result<List<PlatformColumn>> getPublishedColumns(@PathVariable Long platformId) {
        List<PlatformColumn> columns = platformColumnService.query()
                .eq("platform_id", platformId)
                .eq("is_published", 1)
                .orderByAsc("sort")
                .list();
        return Result.success(columns);
    }

    @GetMapping("/my/platforms/{platformId}/columns")
    public Result<List<PlatformColumn>> getMyPublishedColumns(@PathVariable Long platformId,
                                                               Authentication authentication) {
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (!scope.getPlatformIds().contains(platformId) || scope.getColumnIds().isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<PlatformColumn> columns = platformColumnService.query()
                .eq("platform_id", platformId)
                .in("id", scope.getColumnIds())
                .eq("status", 1)
                .eq("is_published", 1)
                .orderByAsc("sort")
                .list();
        return Result.success(columns);
    }

    /**
     * 根据栏目ID获取已发布的分类树
     * 前端页面内容分类使用
     */
    @GetMapping("/columns/{columnId}/categories")
    public Result<List<ContentCategory>> getPublishedCategories(@PathVariable Long columnId) {
        List<ContentCategory> categories = contentCategoryService.getCategoryTree(columnId, true);
        return Result.success(categories);
    }

    @GetMapping("/my/columns/{columnId}/categories")
    public Result<List<ContentCategory>> getMyPublishedCategories(@PathVariable Long columnId,
                                                                   Authentication authentication) {
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (!scope.getColumnIds().contains(columnId)) {
            return Result.success(Collections.emptyList());
        }
        List<ContentCategory> categories = contentCategoryService.getCategoryTree(columnId, true);
        return Result.success(userPermissionScopeService.filterAuthorizedTree(categories, scope.getCategoryIds()));
    }

    /**
     * 递归过滤，只保留已发布的分类
     */
    private void filterPublishedCategories(List<ContentCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        categories.removeIf(category -> category.getIsPublished() == null || category.getIsPublished() != 1);
        for (ContentCategory category : categories) {
            if (category.getChildren() != null && !category.getChildren().isEmpty()) {
                filterPublishedCategories(category.getChildren());
            }
        }
    }

    /**
     * 获取已发布分类的详细内容
     * 前端页面内容详情使用
     */
    @GetMapping("/categories/{id}/content")
    public Result<ContentCategory> getPublishedCategoryContent(@PathVariable Long id,
                                                                Authentication authentication) {
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (!userPermissionScopeService.canAccessCategory(scope, id)) {
            return Result.error(403, "当前账号未分配该分类访问权限");
        }
        ContentCategory category = contentCategoryService.query()
                .eq("id", id)
                .eq("is_published", 1)
                .one();
        if (category == null) {
            return Result.error("内容不存在或未发布");
        }
        return Result.success(category);
    }

    @GetMapping("/my/categories/{id}/contents")
    public Result<List<ContentCategory>> getMyPublishedCategoryContents(@PathVariable Long id,
                                                                         Authentication authentication) {
        Long userId = userPermissionScopeService.getAuthenticatedUserId(authentication);
        if (userId == null) {
            return Result.unauthorized();
        }
        PermissionScope scope = userPermissionScopeService.getPermissionScope(userId);
        if (!userPermissionScopeService.canAccessCategory(scope, id)) {
            return Result.error(403, "当前账号未分配该分类访问权限");
        }
        return Result.success(contentInfoService.getPublishedContents(id, null));
    }
}
