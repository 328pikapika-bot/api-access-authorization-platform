package com.nstl.openplatform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.PlatformColumnService;
import com.nstl.openplatform.service.PlatformService;
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
 * 系统平台管理控制器
 */
@RestController
@RequestMapping("/admin/platform")
@RequiredArgsConstructor
public class PlatformController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PlatformService platformService;
    private final PlatformColumnService platformColumnService;
    private final ApplicationService applicationService;

    @GetMapping("/list")
    public Result<List<Platform>> list(@RequestParam(required = false) Boolean publishedOnly) {
        var query = platformService.query();
        if (Boolean.TRUE.equals(publishedOnly)) {
            query.eq("is_published", 1);
        }
        return Result.success(query.orderByAsc("sort").list());
    }

    @PostMapping("/add")
    @AuditLog(module = "分类与内容", businessType = "新增平台", operationType = OperationType.INSERT)
    public Result<Boolean> add(@Valid @RequestBody Platform platform) {
        platform.setName(normalizeName(platform.getName()));
        if (platform.getName().isEmpty()) {
            return Result.error("请输入平台名称");
        }
        long count = platformService.query().eq("name", platform.getName()).count();
        if (count > 0) {
            return Result.error("平台名称“" + platform.getName() + "”已存在，请更换后再保存");
        }
        platform.setIsPublished(0);
        return Result.success(platformService.save(platform));
    }

    @PostMapping("/update")
    @AuditLog(module = "分类与内容", businessType = "修改平台", operationType = OperationType.UPDATE)
    public Result<Boolean> update(@Valid @RequestBody Platform platform) {
        platform.setName(normalizeName(platform.getName()));
        if (platform.getName().isEmpty()) {
            return Result.error("请输入平台名称");
        }
        long count = platformService.query()
                .eq("name", platform.getName())
                .ne("id", platform.getId())
                .count();
        if (count > 0) {
            return Result.error("平台名称“" + platform.getName() + "”已存在，请更换后再保存");
        }
        return Result.success(platformService.updateById(platform));
    }

    @DeleteMapping("/delete/{id}")
    @AuditLog(module = "分类与内容", businessType = "删除平台", operationType = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        int referencedUserCount = countAssignedUsersByPlatform(id);
        if (referencedUserCount > 0) {
            return Result.error("当前平台已有 " + referencedUserCount + " 个用户在接口权限分配中配置，无法删除，请先调整权限分配");
        }
        long columnCount = platformColumnService.query().eq("platform_id", id).count();
        if (columnCount > 0) {
            return Result.error("当前平台下存在栏目，无法删除，请先删除栏目后再删除平台");
        }
        return Result.success(platformService.removeById(id));
    }

    @PutMapping("/{id}/publish")
    @AuditLog(module = "分类与内容", businessType = "发布平台", operationType = OperationType.UPDATE)
    public Result<Boolean> publish(@PathVariable Long id) {
        Platform platform = platformService.getById(id);
        if (platform == null) {
            return Result.error("平台不存在");
        }
        Platform update = new Platform();
        update.setId(id);
        update.setIsPublished(1);
        return Result.success(platformService.updateById(update));
    }

    @PutMapping("/{id}/withdraw")
    @AuditLog(module = "分类与内容", businessType = "撤回平台", operationType = OperationType.UPDATE)
    public Result<Boolean> withdraw(@PathVariable Long id) {
        Platform platform = platformService.getById(id);
        if (platform == null) {
            return Result.error("平台不存在");
        }
        Platform update = new Platform();
        update.setId(id);
        update.setIsPublished(2);
        return Result.success(platformService.updateById(update));
    }

    @PutMapping("/batch/publish")
    @AuditLog(module = "分类与内容", businessType = "批量发布平台", operationType = OperationType.UPDATE)
    public Result<Integer> batchPublish(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            Platform platform = platformService.getById(id);
            if (platform != null) {
                Platform update = new Platform();
                update.setId(id);
                update.setIsPublished(1);
                if (platformService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    @PutMapping("/batch/withdraw")
    @AuditLog(module = "分类与内容", businessType = "批量撤回平台", operationType = OperationType.UPDATE)
    public Result<Integer> batchWithdraw(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(0);
        }
        int count = 0;
        for (Long id : ids) {
            Platform platform = platformService.getById(id);
            if (platform != null) {
                Platform update = new Platform();
                update.setId(id);
                update.setIsPublished(2);
                if (platformService.updateById(update)) {
                    count++;
                }
            }
        }
        return Result.success(count);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private int countAssignedUsersByPlatform(Long platformId) {
        List<ContentApplication> applications = applicationService.query()
                .select("id", "user_id", "platform_id", "interface_configs")
                .eq("status", 1)
                .list();
        Set<Long> userIds = applications.stream()
                .filter(item -> Objects.equals(item.getPlatformId(), platformId) || containsPlatform(item.getInterfaceConfigs(), platformId))
                .map(ContentApplication::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userIds.size();
    }

    private boolean containsPlatform(String interfaceConfigs, Long platformId) {
        if (interfaceConfigs == null || interfaceConfigs.isBlank()) {
            return false;
        }
        try {
            List<Map<String, Object>> configs = MAPPER.readValue(interfaceConfigs, new TypeReference<List<Map<String, Object>>>() {});
            return configs.stream().anyMatch(config -> Objects.equals(toLong(config.get("platformId")), platformId));
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
