package com.nstl.openplatform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.PlatformColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 栏目管理控制器
 */
@RestController
@RequestMapping("/admin/platform-column")
@RequiredArgsConstructor
public class PlatformColumnController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PlatformColumnService platformColumnService;
    private final ContentCategoryService contentCategoryService;
    private final ApplicationService applicationService;

    @GetMapping("/list")
    public Result<List<PlatformColumn>> list(@RequestParam(required = false) Long platformId,
                                             @RequestParam(required = false) Boolean publishedOnly) {
        var query = platformColumnService.query()
                .eq(platformId != null, "platform_id", platformId);
        if (Boolean.TRUE.equals(publishedOnly)) {
            query.eq("is_published", 1);
        }
        return Result.success(query.orderByAsc("sort").list());
    }

    @PostMapping("/add")
    @AuditLog(module = "分类与内容", businessType = "新增栏目", operationType = OperationType.INSERT)
    public Result<Boolean> add(@Valid @RequestBody PlatformColumn column) {
        column.setName(normalizeName(column.getName()));
        if (column.getName().isEmpty()) {
            return Result.error("请输入栏目名称");
        }
        if (column.getPlatformId() == null) {
            return Result.error("请选择所属平台");
        }
        long count = platformColumnService.query()
                .eq("name", column.getName())
                .eq("platform_id", column.getPlatformId())
                .count();
        if (count > 0) {
            return Result.error("栏目名称“" + column.getName() + "”已存在，请更换后再保存");
        }
        column.setIsPublished(0);
        return Result.success(platformColumnService.save(column));
    }

    @PostMapping("/update")
    @AuditLog(module = "分类与内容", businessType = "修改栏目", operationType = OperationType.UPDATE)
    public Result<Boolean> update(@Valid @RequestBody PlatformColumn column) {
        column.setName(normalizeName(column.getName()));
        if (column.getName().isEmpty()) {
            return Result.error("请输入栏目名称");
        }
        if (column.getPlatformId() == null) {
            return Result.error("请选择所属平台");
        }
        long count = platformColumnService.query()
                .eq("name", column.getName())
                .eq("platform_id", column.getPlatformId())
                .ne("id", column.getId())
                .count();
        if (count > 0) {
            return Result.error("栏目名称“" + column.getName() + "”已存在，请更换后再保存");
        }
        return Result.success(platformColumnService.updateById(column));
    }

    @DeleteMapping("/delete/{id}")
    @AuditLog(module = "分类与内容", businessType = "删除栏目", operationType = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        int referencedUserCount = countAssignedUsersByColumn(id);
        if (referencedUserCount > 0) {
            return Result.error("当前栏目已有 " + referencedUserCount + " 个用户在接口权限分配中配置，无法删除，请先调整权限分配");
        }
        long categoryCount = contentCategoryService.query().eq("column_id", id).count();
        if (categoryCount > 0) {
            return Result.error("当前栏目下存在分类，无法删除，请先删除分类后再删除栏目");
        }
        return Result.success(platformColumnService.removeById(id));
    }

    @PutMapping("/{id}/publish")
    @AuditLog(module = "分类与内容", businessType = "发布栏目", operationType = OperationType.UPDATE)
    public Result<Boolean> publish(@PathVariable Long id) {
        PlatformColumn column = platformColumnService.getById(id);
        if (column == null) {
            return Result.error("栏目不存在");
        }
        PlatformColumn update = new PlatformColumn();
        update.setId(id);
        update.setIsPublished(1);
        return Result.success(platformColumnService.updateById(update));
    }

    @PutMapping("/{id}/withdraw")
    @AuditLog(module = "分类与内容", businessType = "撤回栏目", operationType = OperationType.UPDATE)
    public Result<Boolean> withdraw(@PathVariable Long id) {
        PlatformColumn column = platformColumnService.getById(id);
        if (column == null) {
            return Result.error("栏目不存在");
        }
        PlatformColumn update = new PlatformColumn();
        update.setId(id);
        update.setIsPublished(2);
        return Result.success(platformColumnService.updateById(update));
    }

    @PutMapping("/batch/publish")
    @AuditLog(module = "分类与内容", businessType = "批量发布栏目", operationType = OperationType.UPDATE)
    public Result<Integer> batchPublish(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            PlatformColumn column = platformColumnService.getById(id);
            if (column != null) {
                PlatformColumn update = new PlatformColumn();
                update.setId(id);
                update.setIsPublished(1);
                if (platformColumnService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    @PutMapping("/batch/withdraw")
    @AuditLog(module = "分类与内容", businessType = "批量撤回栏目", operationType = OperationType.UPDATE)
    public Result<Integer> batchWithdraw(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            PlatformColumn column = platformColumnService.getById(id);
            if (column != null) {
                PlatformColumn update = new PlatformColumn();
                update.setId(id);
                update.setIsPublished(2);
                if (platformColumnService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private int countAssignedUsersByColumn(Long columnId) {
        List<ContentApplication> applications = applicationService.query()
                .select("id", "user_id", "column_id", "interface_configs")
                .eq("status", 1)
                .list();
        Set<Long> userIds = applications.stream()
                .filter(item -> Objects.equals(item.getColumnId(), columnId) || containsColumn(item.getInterfaceConfigs(), columnId))
                .map(ContentApplication::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userIds.size();
    }

    private boolean containsColumn(String interfaceConfigs, Long columnId) {
        if (interfaceConfigs == null || interfaceConfigs.isBlank()) {
            return false;
        }
        try {
            List<Map<String, Object>> configs = MAPPER.readValue(interfaceConfigs, new TypeReference<List<Map<String, Object>>>() {});
            return configs.stream().anyMatch(config -> Objects.equals(toLong(config.get("columnId")), columnId));
        } catch (Exception ignored) {
            return false;
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            return Long.parseLong((String) value);
        }
        return null;
    }
}
