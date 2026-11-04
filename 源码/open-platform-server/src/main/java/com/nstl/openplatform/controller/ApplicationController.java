package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.PlatformColumnService;
import com.nstl.openplatform.service.PlatformService;
import com.nstl.openplatform.service.SysUserService;
import com.nstl.openplatform.util.DateTimeRangeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ApplicationController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApplicationService applicationService;
    private final PlatformService platformService;
    private final PlatformColumnService platformColumnService;
    private final ContentCategoryService contentCategoryService;
    private final SysUserService sysUserService;

    @GetMapping("/user/applications")
    public Result<List<ContentApplication>> getMyApplications() {
        Long userId = getUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        if (userId == null) {
            return Result.unauthorized();
        }
        List<ContentApplication> list = applicationService.query()
                .eq("user_id", userId)
                .orderByDesc("apply_time")
                .list();
        if (list != null) {
            list.forEach(this::enrichApplication);
        }
        return Result.success(list);
    }

    @GetMapping("/admin/applications")
    @AuditLog(module = "接口权限分配", businessType = "查询接口权限列表", operationType = OperationType.QUERY)
    public Result<Page<ContentApplication>> getApplicationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long platformId,
            @RequestParam(required = false) Long columnId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LambdaQueryWrapper<ContentApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ContentApplication::getStatus, status);
        }
        LocalDateTime queryStartTime = DateTimeRangeUtil.normalizeStartTime(startTime);
        LocalDateTime queryEndTime = DateTimeRangeUtil.normalizeEndTime(endTime);
        if (queryStartTime != null) {
            wrapper.ge(ContentApplication::getApplyTime, queryStartTime);
        }
        if (queryEndTime != null) {
            wrapper.le(ContentApplication::getApplyTime, queryEndTime);
        }
        wrapper.orderByDesc(ContentApplication::getApplyTime);

        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<ContentApplication> matched = applicationService.list(wrapper);
        matched.forEach(this::enrichApplication);
        matched = collapseByUser(matched).stream()
                .filter(item -> platformId == null || hasPlatform(item, platformId))
                .filter(item -> columnId == null || hasColumn(item, columnId))
                .filter(item -> !StringUtils.hasText(trimmedKeyword) || matchesKeyword(item, trimmedKeyword))
                .collect(Collectors.toList());

        Page<ContentApplication> result = new Page<>(page, size, matched.size());
        int fromIndex = Math.max((page - 1) * size, 0);
        if (fromIndex >= matched.size()) {
            result.setRecords(Collections.emptyList());
        } else {
            int toIndex = Math.min(fromIndex + size, matched.size());
            result.setRecords(new ArrayList<>(matched.subList(fromIndex, toIndex)));
        }
        return Result.success(result);
    }

    @PostMapping("/admin/applications/assign")
    @AuditLog(module = "接口权限分配", businessType = "新增接口权限分配", operationType = OperationType.INSERT)
    public Result<Long> assignApplication(@RequestBody ContentApplication application) {
        ValidationContext validation = validateAssignmentPayload(application);
        if (validation.error != null) {
            return Result.error(validation.error);
        }

        ContentApplication entity = findLatestActiveByUser(validation.user.getId());
        boolean create = entity == null;
        if (create) {
            entity = new ContentApplication();
            entity.setUserId(validation.user.getId());
            entity.setNstlUnionId(validation.user.getNstlUnionId());
            entity.setAppKey(applicationService.generateAppKey());
            entity.setAppSecret(applicationService.generateAppSecret());
            entity.setApplyTime(LocalDateTime.now());
        }

        applyValidatedAssignment(entity, application, validation);
        entity.setAuditTime(LocalDateTime.now());

        boolean success = create ? applicationService.save(entity) : applicationService.updateById(entity);
        if (success) {
            removeDuplicateActiveAssignments(validation.user.getId(), entity.getId());
            return Result.success(entity.getId());
        }
        return Result.error(create ? "新增授权失败" : "更新授权失败");
    }

    @GetMapping("/admin/applications/{id}")
    public Result<ContentApplication> getApplicationDetail(@PathVariable Long id) {
        ContentApplication application = applicationService.getById(id);
        if (application == null) {
            return Result.error("授权记录不存在");
        }
        enrichApplication(application);
        return Result.success(application);
    }

    @GetMapping("/user/application/check-auth")
    public Result<ContentApplication> checkAuth(@RequestParam Long categoryId) {
        Long userId = getUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<ContentApplication> applications = applicationService.query()
                .eq("user_id", userId)
                .eq("status", 1)
                .list();

        for (ContentApplication app : applications) {
            if (hasCategoryAccess(parseCategoryIds(app.getCategoryIds()), categoryId)) {
                ContentApplication result = new ContentApplication();
                result.setAppKey(app.getAppKey());
                result.setAppSecret(app.getAppSecret());
                return Result.success(result);
            }
        }
        return Result.error("未获得该接口授权");
    }

    @PutMapping("/admin/applications/{id}")
    @AuditLog(module = "接口权限分配", businessType = "更新接口权限分配", operationType = OperationType.UPDATE)
    public Result<Boolean> updateApplication(@PathVariable Long id, @RequestBody ContentApplication application) {
        ContentApplication existing = applicationService.getById(id);
        if (existing == null) {
            return Result.error("授权记录不存在");
        }

        if (application.getUserId() == null) {
            application.setUserId(existing.getUserId());
        }
        ValidationContext validation = validateAssignmentPayload(application);
        if (validation.error != null) {
            return Result.error(validation.error);
        }

        applyValidatedAssignment(existing, application, validation);
        existing.setAuditTime(LocalDateTime.now());
        if (!StringUtils.hasText(existing.getAppKey())) {
            existing.setAppKey(applicationService.generateAppKey());
            existing.setAppSecret(applicationService.generateAppSecret());
        }
        boolean success = applicationService.updateById(existing);
        if (success) {
            removeDuplicateActiveAssignments(existing.getUserId(), existing.getId());
        }
        return Result.success(success);
    }

    @DeleteMapping("/admin/applications/{id}")
    @AuditLog(module = "接口权限分配", businessType = "删除接口权限分配", operationType = OperationType.DELETE)
    public Result<Boolean> deleteApplication(@PathVariable Long id) {
        return Result.success(applicationService.removeById(id));
    }

    private void applyValidatedAssignment(ContentApplication entity, ContentApplication source, ValidationContext validation) {
        ScopeConfig firstScope = validation.scopeConfigs.get(0);
        entity.setUserId(validation.user.getId());
        entity.setNstlUnionId(validation.user.getNstlUnionId());
        entity.setPlatformId(firstScope.platformId);
        entity.setPlatformName(validation.platformNames.get(firstScope.platformId));
        entity.setColumnId(firstScope.columnId);
        entity.setColumnName(validation.columnNames.get(firstScope.columnId));
        entity.setCategoryIds(validation.categoryIdsJson);
        entity.setCategoryNames(validation.categoryNames);
        entity.setInterfaceConfigs(validation.interfaceConfigsJson);
        entity.setUnitName(defaultIfBlank(source.getUnitName(), validation.user.getUnitName()));
        entity.setDeptName(source.getDeptName());
        entity.setReason(defaultIfBlank(source.getReason(), "后台管理员直接授权"));
        entity.setStatus(1);
        if (entity.getApplyTime() == null) {
            entity.setApplyTime(LocalDateTime.now());
        }
    }

    private ContentApplication findLatestActiveByUser(Long userId) {
        return applicationService.query()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("apply_time")
                .last("limit 1")
                .one();
    }

    private void removeDuplicateActiveAssignments(Long userId, Long keepId) {
        List<Long> duplicateIds = applicationService.query()
                .select("id")
                .eq("user_id", userId)
                .eq("status", 1)
                .ne("id", keepId)
                .list()
                .stream()
                .map(ContentApplication::getId)
                .collect(Collectors.toList());
        if (!duplicateIds.isEmpty()) {
            applicationService.removeByIds(duplicateIds);
        }
    }

    private List<ContentApplication> collapseByUser(List<ContentApplication> applications) {
        Map<String, ContentApplication> map = new LinkedHashMap<>();
        for (ContentApplication application : applications) {
            String key = application.getUserId() == null ? "record-" + application.getId() : "user-" + application.getUserId();
            map.putIfAbsent(key, application);
        }
        return new ArrayList<>(map.values());
    }

    private void enrichApplication(ContentApplication application) {
        if (application == null) {
            return;
        }
        if (application.getUserId() != null) {
            SysUser user = sysUserService.getById(application.getUserId());
            if (user != null) {
                application.setUserName(user.getUserName());
                application.setNickname(user.getNickname());
                application.setBusinessUserId(user.getUserId());
                if (!StringUtils.hasText(application.getUnitName())) {
                    application.setUnitName(user.getUnitName());
                }
            }
        }

        List<Map<String, Object>> scopes = buildScopeConfigs(application);
        application.setScopeConfigs(scopes);
        application.setScopeSummary(scopes.stream()
                .map(scope -> String.valueOf(scope.get("path")))
                .collect(Collectors.joining("，")));

        String invalidReason = resolveInvalidReason(application, scopes);
        application.setInvalidReason(invalidReason);
        application.setValidAssignment(invalidReason == null);
    }

    private String resolveInvalidReason(ContentApplication application, List<Map<String, Object>> scopes) {
        if (scopes.isEmpty()) {
            return "未配置授权范围";
        }
        for (Map<String, Object> scope : scopes) {
            Long platformId = toLong(scope.get("platformId"));
            Long columnId = toLong(scope.get("columnId"));
            Long categoryId = toLong(scope.get("categoryId"));
            Platform platform = platformId == null ? null : platformService.getById(platformId);
            if (platform == null) {
                return "平台不存在";
            }
            if (!isPublished(platform.getIsPublished())) {
                return "平台未发布";
            }
            PlatformColumn column = columnId == null ? null : platformColumnService.getById(columnId);
            if (column == null) {
                return "栏目不存在";
            }
            if (!isPublished(column.getIsPublished())) {
                return "栏目未发布";
            }
            ContentCategory category = categoryId == null ? null : contentCategoryService.getById(categoryId);
            if (category == null) {
                return "分类不存在";
            }
            if (!isPublished(category.getIsPublished())) {
                return "分类未发布";
            }
        }
        return null;
    }

    private boolean matchesKeyword(ContentApplication item, String keyword) {
        return contains(item.getBusinessUserId(), keyword)
                || contains(item.getUserName(), keyword)
                || contains(item.getNickname(), keyword)
                || contains(item.getCategoryNames(), keyword)
                || contains(item.getScopeSummary(), keyword)
                || contains(item.getReason(), keyword);
    }

    private boolean hasPlatform(ContentApplication item, Long platformId) {
        return item.getScopeConfigs() != null && item.getScopeConfigs().stream()
                .anyMatch(scope -> Objects.equals(toLong(scope.get("platformId")), platformId));
    }

    private boolean hasColumn(ContentApplication item, Long columnId) {
        return item.getScopeConfigs() != null && item.getScopeConfigs().stream()
                .anyMatch(scope -> Objects.equals(toLong(scope.get("columnId")), columnId));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private ValidationContext validateAssignmentPayload(ContentApplication application) {
        ValidationContext context = new ValidationContext();
        if (application.getUserId() == null) {
            context.error = "用户ID不能为空";
            return context;
        }
        SysUser user = sysUserService.getById(application.getUserId());
        if (user == null) {
            context.error = "用户不存在";
            return context;
        }

        List<ScopeConfig> requestedScopes = parseRequestedScopes(application);
        if (requestedScopes.isEmpty()) {
            context.error = "请至少选择一个平台/栏目/一级分类授权范围";
            return context;
        }

        List<ScopeConfig> normalizedScopes = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Set<Long> allCategoryIds = new LinkedHashSet<>();
        List<String> categoryPaths = new ArrayList<>();
        Map<Long, String> platformNames = new LinkedHashMap<>();
        Map<Long, String> columnNames = new LinkedHashMap<>();

        for (ScopeConfig requested : requestedScopes) {
            Platform platform = requested.platformId == null ? null : platformService.getById(requested.platformId);
            if (platform == null) {
                context.error = "平台不存在";
                return context;
            }
            if (!isPublished(platform.getIsPublished())) {
                context.error = "未发布的平台不可分配";
                return context;
            }
            PlatformColumn column = requested.columnId == null ? null : platformColumnService.getById(requested.columnId);
            if (column == null) {
                context.error = "栏目不存在";
                return context;
            }
            if (!isPublished(column.getIsPublished())) {
                context.error = "未发布的栏目不可分配";
                return context;
            }
            if (!platform.getId().equals(column.getPlatformId())) {
                context.error = "栏目不属于当前平台";
                return context;
            }
            if (requested.categoryIds == null || requested.categoryIds.isEmpty()) {
                context.error = "请至少选择一个一级分类";
                return context;
            }

            platformNames.put(platform.getId(), platform.getName());
            columnNames.put(column.getId(), column.getName());
            List<Long> categoryIds = new ArrayList<>();
            for (Long categoryId : requested.categoryIds) {
                ContentCategory category = contentCategoryService.getById(categoryId);
                if (category == null) {
                    context.error = "分类不存在";
                    return context;
                }
                if (!column.getId().equals(category.getColumnId())) {
                    context.error = "分类不属于当前栏目";
                    return context;
                }
                if (!isPublished(category.getIsPublished())) {
                    context.error = "未发布的分类不可分配";
                    return context;
                }
                if (category.getParentId() != null && category.getParentId() != 0) {
                    context.error = "分类权限只能选择一级分类";
                    return context;
                }
                String dedupeKey = platform.getId() + ":" + column.getId() + ":" + category.getId();
                if (seen.add(dedupeKey)) {
                    categoryIds.add(category.getId());
                    allCategoryIds.add(category.getId());
                    categoryPaths.add(platform.getName() + "/" + column.getName() + "/" + category.getName());
                }
            }
            if (!categoryIds.isEmpty()) {
                ScopeConfig normalized = new ScopeConfig();
                normalized.platformId = platform.getId();
                normalized.columnId = column.getId();
                normalized.categoryIds = categoryIds;
                normalizedScopes.add(normalized);
            }
        }

        if (normalizedScopes.isEmpty()) {
            context.error = "请至少选择一个一级分类";
            return context;
        }

        context.user = user;
        context.scopeConfigs = normalizedScopes;
        context.categoryIdsJson = serialize(allCategoryIds);
        context.categoryNames = String.join(",", categoryPaths);
        context.interfaceConfigsJson = serialize(normalizedScopes);
        context.platformNames = platformNames;
        context.columnNames = columnNames;
        return context;
    }

    private List<ScopeConfig> parseRequestedScopes(ContentApplication application) {
        if (StringUtils.hasText(application.getInterfaceConfigs())) {
            try {
                List<ScopeConfig> scopes = MAPPER.readValue(application.getInterfaceConfigs(), new TypeReference<List<ScopeConfig>>() {});
                if (scopes != null) {
                    return scopes;
                }
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        }
        if (application.getPlatformId() == null || application.getColumnId() == null) {
            return Collections.emptyList();
        }
        ScopeConfig scope = new ScopeConfig();
        scope.platformId = application.getPlatformId();
        scope.columnId = application.getColumnId();
        scope.categoryIds = parseCategoryIds(application.getCategoryIds());
        return List.of(scope);
    }

    private List<Map<String, Object>> buildScopeConfigs(ContentApplication application) {
        List<ScopeConfig> scopes = parseStoredScopes(application);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScopeConfig scope : scopes) {
            Platform platform = scope.platformId == null ? null : platformService.getById(scope.platformId);
            PlatformColumn column = scope.columnId == null ? null : platformColumnService.getById(scope.columnId);
            for (Long categoryId : scope.categoryIds == null ? Collections.<Long>emptyList() : scope.categoryIds) {
                ContentCategory category = contentCategoryService.getById(categoryId);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("platformId", scope.platformId);
                row.put("platformName", platform == null ? "-" : platform.getName());
                row.put("columnId", scope.columnId);
                row.put("columnName", column == null ? "-" : column.getName());
                row.put("categoryId", categoryId);
                row.put("categoryName", category == null ? "分类 " + categoryId : category.getName());
                row.put("path", row.get("platformName") + "/" + row.get("columnName") + "/" + row.get("categoryName"));
                result.add(row);
            }
        }
        return result;
    }

    private List<ScopeConfig> parseStoredScopes(ContentApplication application) {
        if (StringUtils.hasText(application.getInterfaceConfigs())) {
            try {
                List<ScopeConfig> scopes = MAPPER.readValue(application.getInterfaceConfigs(), new TypeReference<List<ScopeConfig>>() {});
                if (scopes != null && !scopes.isEmpty()) {
                    return scopes;
                }
            } catch (Exception ignored) {
                // Fall back to legacy fields.
            }
        }
        ScopeConfig fallback = new ScopeConfig();
        fallback.platformId = application.getPlatformId();
        fallback.columnId = application.getColumnId();
        fallback.categoryIds = parseCategoryIds(application.getCategoryIds());
        if (fallback.platformId == null || fallback.columnId == null || fallback.categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(fallback);
    }

    private boolean hasCategoryAccess(List<Long> allowedCategoryIds, Long targetCategoryId) {
        if (targetCategoryId == null || allowedCategoryIds.isEmpty()) {
            return false;
        }
        if (allowedCategoryIds.contains(targetCategoryId)) {
            return true;
        }
        ContentCategory category = contentCategoryService.getById(targetCategoryId);
        while (category != null && category.getParentId() != null && category.getParentId() != 0) {
            if (allowedCategoryIds.contains(category.getParentId())) {
                return true;
            }
            category = contentCategoryService.getById(category.getParentId());
        }
        return false;
    }

    private List<Long> parseCategoryIds(String categoryIdsJson) {
        if (!StringUtils.hasText(categoryIdsJson)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(categoryIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String serialize(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("序列化授权范围失败", ex);
        }
    }

    private boolean isPublished(Integer isPublished) {
        return isPublished != null && isPublished == 1;
    }

    private String defaultIfBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Long.parseLong((String) value);
        }
        return null;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            Object userId = ((Map<?, ?>) details).get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            if (userId instanceof String && StringUtils.hasText((String) userId)) {
                try {
                    return Long.parseLong((String) userId);
                } catch (NumberFormatException ignored) {
                    // Fall through to principal lookup.
                }
            }
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SysUser) {
            return ((SysUser) principal).getId();
        }
        if (principal instanceof String) {
            SysUser user = sysUserService.lambdaQuery().eq(SysUser::getUserName, principal).one();
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }

    public static class ScopeConfig {
        public Long platformId;
        public Long columnId;
        public List<Long> categoryIds = new ArrayList<>();
    }

    private static class ValidationContext {
        private String error;
        private SysUser user;
        private List<ScopeConfig> scopeConfigs;
        private String categoryIdsJson;
        private String categoryNames;
        private String interfaceConfigsJson;
        private Map<Long, String> platformNames;
        private Map<Long, String> columnNames;
    }
}
