package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionScopeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApplicationService applicationService;
    private final ContentCategoryService contentCategoryService;
    private final PlatformColumnService platformColumnService;
    private final PlatformService platformService;

    public Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?>) {
            return toLong(((Map<?, ?>) details).get("userId"));
        }
        if (authentication.getPrincipal() instanceof SysUser) {
            return ((SysUser) authentication.getPrincipal()).getId();
        }
        return null;
    }

    public PermissionScope getPermissionScope(Long userId) {
        if (userId == null) {
            return PermissionScope.empty();
        }

        List<ContentApplication> applications = applicationService.list(
                new LambdaQueryWrapper<ContentApplication>()
                        .eq(ContentApplication::getUserId, userId)
                        .eq(ContentApplication::getStatus, 1)
        );
        Set<Long> requestedCategoryIds = collectCategoryIds(applications);
        if (requestedCategoryIds.isEmpty()) {
            return PermissionScope.empty();
        }

        List<ContentCategory> categories = contentCategoryService.listByIds(requestedCategoryIds);
        Set<Long> requestedColumnIds = categories.stream()
                .filter(this::isAvailableCategory)
                .map(ContentCategory::getColumnId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedColumnIds.isEmpty()) {
            return PermissionScope.empty();
        }

        List<PlatformColumn> columns = platformColumnService.listByIds(requestedColumnIds);
        Set<Long> requestedPlatformIds = columns.stream()
                .filter(this::isAvailableColumn)
                .map(PlatformColumn::getPlatformId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedPlatformIds.isEmpty()) {
            return PermissionScope.empty();
        }

        Set<Long> platformIds = platformService.listByIds(requestedPlatformIds).stream()
                .filter(this::isAvailablePlatform)
                .map(Platform::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> columnIds = columns.stream()
                .filter(this::isAvailableColumn)
                .filter(column -> platformIds.contains(column.getPlatformId()))
                .map(PlatformColumn::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> categoryIds = categories.stream()
                .filter(this::isAvailableCategory)
                .filter(category -> columnIds.contains(category.getColumnId()))
                .map(ContentCategory::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PermissionScope(platformIds, columnIds, categoryIds);
    }

    public boolean canAccessCategory(PermissionScope scope, Long categoryId) {
        if (scope == null || categoryId == null || scope.getCategoryIds().isEmpty()) {
            return false;
        }

        Set<Long> visited = new LinkedHashSet<>();
        ContentCategory category = contentCategoryService.getById(categoryId);
        while (category != null && visited.add(category.getId())) {
            if (!isAvailableCategory(category)) {
                return false;
            }
            if (scope.getCategoryIds().contains(category.getId())) {
                return true;
            }
            Long parentId = category.getParentId();
            if (parentId == null || parentId == 0) {
                return false;
            }
            category = contentCategoryService.getById(parentId);
        }
        return false;
    }

    public List<ContentCategory> filterAuthorizedTree(List<ContentCategory> categories, Set<Long> authorizedCategoryIds) {
        if (categories == null || categories.isEmpty() || authorizedCategoryIds == null || authorizedCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return filterAuthorizedTree(categories, authorizedCategoryIds, false);
    }

    private List<ContentCategory> filterAuthorizedTree(List<ContentCategory> categories,
                                                       Set<Long> authorizedCategoryIds,
                                                       boolean ancestorAuthorized) {
        List<ContentCategory> result = new ArrayList<>();
        for (ContentCategory category : categories) {
            if (!isAvailableCategory(category)) {
                continue;
            }
            boolean branchAuthorized = ancestorAuthorized || authorizedCategoryIds.contains(category.getId());
            List<ContentCategory> children = filterAuthorizedTree(
                    category.getChildren() == null ? Collections.emptyList() : category.getChildren(),
                    authorizedCategoryIds,
                    branchAuthorized
            );
            if (branchAuthorized || !children.isEmpty()) {
                category.setChildren(children.isEmpty() ? null : children);
                result.add(category);
            }
        }
        return result;
    }

    private Set<Long> collectCategoryIds(List<ContentApplication> applications) {
        Set<Long> categoryIds = new LinkedHashSet<>();
        if (applications == null) {
            return categoryIds;
        }
        for (ContentApplication application : applications) {
            parseCategoryIds(application.getCategoryIds(), categoryIds);
            parseScopeCategoryIds(application.getInterfaceConfigs(), categoryIds);
        }
        return categoryIds;
    }

    private void parseCategoryIds(String json, Set<Long> target) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            List<Object> values = MAPPER.readValue(json, new TypeReference<List<Object>>() {});
            for (Object value : values) {
                Long id = toLong(value);
                if (id != null) {
                    target.add(id);
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed legacy data and continue with interfaceConfigs.
        }
    }

    private void parseScopeCategoryIds(String json, Set<Long> target) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            List<Map<String, Object>> scopes = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> scope : scopes) {
                Object values = scope.get("categoryIds");
                if (values instanceof List<?>) {
                    for (Object value : (List<?>) values) {
                        Long id = toLong(value);
                        if (id != null) {
                            target.add(id);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Malformed historical scopes do not grant access.
        }
    }

    private boolean isAvailableCategory(ContentCategory category) {
        return category != null
                && category.getId() != null
                && Integer.valueOf(1).equals(category.getStatus())
                && Integer.valueOf(1).equals(category.getIsPublished());
    }

    private boolean isAvailableColumn(PlatformColumn column) {
        return column != null
                && column.getId() != null
                && Integer.valueOf(1).equals(column.getStatus())
                && Integer.valueOf(1).equals(column.getIsPublished());
    }

    private boolean isAvailablePlatform(Platform platform) {
        return platform != null
                && platform.getId() != null
                && Integer.valueOf(1).equals(platform.getStatus())
                && Integer.valueOf(1).equals(platform.getIsPublished());
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static final class PermissionScope {
        private final Set<Long> platformIds;
        private final Set<Long> columnIds;
        private final Set<Long> categoryIds;

        private PermissionScope(Set<Long> platformIds, Set<Long> columnIds, Set<Long> categoryIds) {
            this.platformIds = Collections.unmodifiableSet(new LinkedHashSet<>(platformIds));
            this.columnIds = Collections.unmodifiableSet(new LinkedHashSet<>(columnIds));
            this.categoryIds = Collections.unmodifiableSet(new LinkedHashSet<>(categoryIds));
        }

        public static PermissionScope empty() {
            return new PermissionScope(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }

        public Set<Long> getPlatformIds() {
            return platformIds;
        }

        public Set<Long> getColumnIds() {
            return columnIds;
        }

        public Set<Long> getCategoryIds() {
            return categoryIds;
        }
    }
}
