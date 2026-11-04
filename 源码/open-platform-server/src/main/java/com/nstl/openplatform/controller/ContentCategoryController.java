package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ApiCredential;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.IApiCredentialService;
import com.nstl.openplatform.service.InterfaceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class ContentCategoryController {

    private static final int DEFAULT_PARENT_ID = 0;
    private static final int DEFAULT_SORT = 0;
    private static final int MAX_TEXT_LENGTH = 255;

    private final ContentCategoryService contentCategoryService;
    private final InterfaceConfigService interfaceConfigService;
    private final ApplicationService applicationService;
    private final IApiCredentialService apiCredentialService;
    private final ObjectMapper objectMapper;

    @GetMapping("/tree")
    public Result<List<ContentCategory>> getCategoryTree(@RequestParam(required = false) Long columnId,
                                                         @RequestParam(required = false) Boolean publishedOnly,
                                                         @RequestParam(required = false) Boolean firstLevelOnly) {
        List<ContentCategory> tree = contentCategoryService.getCategoryTree(columnId, publishedOnly);
        if (Boolean.TRUE.equals(firstLevelOnly) && tree != null) {
            tree.forEach(item -> item.setChildren(Collections.emptyList()));
        }
        return Result.success(tree);
    }

    @GetMapping("/{id}")
    public Result<ContentCategory> getDetail(@PathVariable Long id) {
        return Result.success(contentCategoryService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<ContentCategory>> list(@RequestParam(required = false) Long columnId,
                                              @RequestParam(required = false) Boolean publishedOnly) {
        var query = contentCategoryService.query().eq(columnId != null, "column_id", columnId);
        if (Boolean.TRUE.equals(publishedOnly)) {
            query.eq("is_published", 1);
        }
        return Result.success(query.list());
    }

    @PostMapping("/add")
    @AuditLog(module = "分类与内容", businessType = "新增分类", operationType = OperationType.INSERT)
    public Result<Boolean> add(@Valid @RequestBody ContentCategory contentCategory) {
        normalizeCategoryPayload(contentCategory, true);
        Result<Boolean> validationResult = validateCategoryPayload(contentCategory);
        if (validationResult != null) {
            return validationResult;
        }
        if (hasDuplicateName(contentCategory, false)) {
            return duplicateNameError(contentCategory);
        }
        return Result.success(contentCategoryService.save(contentCategory));
    }

    @PostMapping("/update")
    @AuditLog(module = "分类与内容", businessType = "修改分类", operationType = OperationType.UPDATE)
    public Result<Boolean> update(@Valid @RequestBody ContentCategory contentCategory) {
        normalizeCategoryPayload(contentCategory, false);
        Result<Boolean> validationResult = validateCategoryPayload(contentCategory);
        if (validationResult != null) {
            return validationResult;
        }
        if (contentCategory.getId() == null || contentCategoryService.getById(contentCategory.getId()) == null) {
            return Result.error("分类不存在");
        }
        if (hasDuplicateName(contentCategory, true)) {
            return duplicateNameError(contentCategory);
        }
        return Result.success(contentCategoryService.updateById(contentCategory));
    }

    @DeleteMapping("/delete/{id}")
    @AuditLog(module = "分类与内容", businessType = "删除分类", operationType = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        ContentCategory category = contentCategoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }

        List<ContentCategory> children = contentCategoryService.query().eq("parent_id", id).list();
        if (children != null && !children.isEmpty()) {
            return Result.error("存在子分类，无法删除");
        }

        if (interfaceConfigService.getByCategoryId(id) != null) {
            return Result.error("该分类已配置接口，请先删除接口配置");
        }

        long credentialCount = apiCredentialService.count(
                new LambdaQueryWrapper<ApiCredential>().eq(ApiCredential::getCategoryId, id)
        );
        if (credentialCount > 0) {
            return Result.error("该分类已被凭证分配引用，无法删除");
        }

        if (isReferencedByApplication(id)) {
            return Result.error("该分类已被授权记录引用，无法删除");
        }

        return Result.success(contentCategoryService.removeById(id));
    }

    @PostMapping("/{id}/content")
    @AuditLog(module = "分类与内容", businessType = "更新内容", operationType = OperationType.UPDATE)
    public Result<Boolean> updateContent(@PathVariable Long id, @RequestBody ContentCategory contentCategory) {
        ContentCategory existing = contentCategoryService.getById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }

        String title = trimToNull(contentCategory.getTitle());
        if (title != null && title.length() > MAX_TEXT_LENGTH) {
            return Result.error("内容标题长度不能超过255个字符");
        }

        Integer isOpen = normalizeIsOpen(contentCategory.getIsOpen(), existing.getIsOpen());

        ContentCategory update = new ContentCategory();
        update.setId(id);
        update.setTitle(title);
        update.setDocContent(contentCategory.getDocContent());
        update.setIsOpen(isOpen);
        update.setType(resolveTypeByIsOpen(isOpen));
        return Result.success(contentCategoryService.updateById(update));
    }

    @PutMapping("/{id}/publish")
    @AuditLog(module = "分类与内容", businessType = "发布内容", operationType = OperationType.UPDATE)
    public Result<Boolean> publish(@PathVariable Long id) {
        ContentCategory category = contentCategoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        ContentCategory update = new ContentCategory();
        update.setId(id);
        update.setIsPublished(1);
        return Result.success(contentCategoryService.updateById(update));
    }

    @PutMapping("/{id}/withdraw")
    @AuditLog(module = "分类与内容", businessType = "撤回内容", operationType = OperationType.UPDATE)
    public Result<Boolean> withdraw(@PathVariable Long id) {
        ContentCategory category = contentCategoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        ContentCategory update = new ContentCategory();
        update.setId(id);
        update.setIsPublished(2);
        return Result.success(contentCategoryService.updateById(update));
    }

    @PutMapping("/{id}/open/{isOpen}")
    @AuditLog(module = "分类与内容", businessType = "更新接口状态", operationType = OperationType.UPDATE)
    public Result<Boolean> updateOpenStatus(@PathVariable Long id, @PathVariable Integer isOpen) {
        ContentCategory existing = contentCategoryService.getById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }

        Integer normalizedIsOpen = normalizeIsOpen(isOpen, existing.getIsOpen());
        LambdaUpdateWrapper<ContentCategory> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ContentCategory::getId, id)
                .set(ContentCategory::getIsOpen, normalizedIsOpen)
                .set(ContentCategory::getType, resolveTypeByIsOpen(normalizedIsOpen));

        boolean success = contentCategoryService.update(updateWrapper);
        if (!success) {
            return Result.error("更新失败，请稍后重试");
        }
        return Result.success(true);
    }

    @PutMapping("/batch/publish")
    @AuditLog(module = "分类与内容", businessType = "批量发布分类", operationType = OperationType.UPDATE)
    public Result<Integer> batchPublish(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            ContentCategory category = contentCategoryService.getById(id);
            if (category != null) {
                ContentCategory update = new ContentCategory();
                update.setId(id);
                update.setIsPublished(1);
                if (contentCategoryService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    @PutMapping("/batch/withdraw")
    @AuditLog(module = "分类与内容", businessType = "批量撤回分类", operationType = OperationType.UPDATE)
    public Result<Integer> batchWithdraw(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            ContentCategory category = contentCategoryService.getById(id);
            if (category != null) {
                ContentCategory update = new ContentCategory();
                update.setId(id);
                update.setIsPublished(2);
                if (contentCategoryService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    private void normalizeCategoryPayload(ContentCategory contentCategory, boolean isNew) {
        contentCategory.setName(trimToNull(contentCategory.getName()));
        contentCategory.setTitle(trimToNull(contentCategory.getTitle()));
        contentCategory.setParentId(contentCategory.getParentId() == null ? (long) DEFAULT_PARENT_ID : contentCategory.getParentId());
        contentCategory.setSort(contentCategory.getSort() == null ? DEFAULT_SORT : contentCategory.getSort());

        Integer isOpen = contentCategory.getIsOpen();
        if (isOpen == null && StringUtils.hasText(contentCategory.getType())) {
            isOpen = "api".equalsIgnoreCase(contentCategory.getType().trim()) ? 1 : 0;
        }
        if (isOpen == null) {
            isOpen = isNew ? 0 : 0;
        }
        contentCategory.setIsOpen(isOpen);
        contentCategory.setType(resolveTypeByIsOpen(isOpen));

        if (contentCategory.getCanApply() == null) {
            contentCategory.setCanApply(1);
        }
        if (contentCategory.getStatus() == null) {
            contentCategory.setStatus(1);
        }
        if (isNew && contentCategory.getIsPublished() == null) {
            contentCategory.setIsPublished(0);
        }
    }

    private Result<Boolean> validateCategoryPayload(ContentCategory contentCategory) {
        if (!StringUtils.hasText(contentCategory.getName())) {
            return Result.error("分类名称不能为空");
        }
        if (contentCategory.getName().length() > 100) {
            return Result.error("分类名称长度不能超过100个字符");
        }
        if (contentCategory.getColumnId() == null) {
            return Result.error("所属栏目不能为空");
        }
        if (contentCategory.getSort() == null || contentCategory.getSort() < 0 || contentCategory.getSort() > 9999) {
            return Result.error("排序值必须为0到9999之间的整数");
        }
        if (contentCategory.getTitle() != null && contentCategory.getTitle().length() > MAX_TEXT_LENGTH) {
            return Result.error("内容标题长度不能超过255个字符");
        }
        return null;
    }

    private boolean hasDuplicateName(ContentCategory contentCategory, boolean excludeSelf) {
        // Category type is intentionally not part of this query: API and content
        // categories share one namespace within the same column and parent.
        var query = contentCategoryService.query()
                .eq("name", contentCategory.getName())
                .eq("column_id", contentCategory.getColumnId())
                .eq("parent_id", contentCategory.getParentId() != null ? contentCategory.getParentId() : DEFAULT_PARENT_ID);
        if (excludeSelf && contentCategory.getId() != null) {
            query.ne("id", contentCategory.getId());
        }
        return query.count() > 0;
    }

    private Result<Boolean> duplicateNameError(ContentCategory contentCategory) {
        return Result.error("当前层级已存在分类“" + contentCategory.getName()
                + "”，接口分类和内容分类不能同名，请更换后再保存");
    }

    private boolean isReferencedByApplication(Long categoryId) {
        List<ContentApplication> applications = applicationService.list(new LambdaQueryWrapper<ContentApplication>()
                .select(ContentApplication::getId, ContentApplication::getCategoryIds, ContentApplication::getInterfaceConfigs));
        for (ContentApplication application : applications) {
            List<Long> categoryIds = parseCategoryIds(application.getCategoryIds());
            if (categoryIds.contains(categoryId) || interfaceConfigsContainCategory(application.getInterfaceConfigs(), categoryId)) {
                return true;
            }
        }
        return false;
    }

    private boolean interfaceConfigsContainCategory(String interfaceConfigsJson, Long categoryId) {
        if (!StringUtils.hasText(interfaceConfigsJson)) {
            return false;
        }
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(interfaceConfigsJson, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> config : configs) {
                Object value = config.get("categoryIds");
                if (value instanceof List<?>) {
                    for (Object item : (List<?>) value) {
                        Long parsed = parseLong(item);
                        if (categoryId.equals(parsed)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<Long> parseCategoryIds(String categoryIdsJson) {
        if (!StringUtils.hasText(categoryIdsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(categoryIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private Integer normalizeIsOpen(Integer isOpen, Integer defaultValue) {
        if (isOpen != null) {
            return isOpen == 1 ? 1 : 0;
        }
        return defaultValue != null && defaultValue == 1 ? 1 : 0;
    }

    private String resolveTypeByIsOpen(Integer isOpen) {
        return isOpen != null && isOpen == 1 ? "api" : "content";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
