package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.MockoonService;
import com.nstl.openplatform.service.MockoonServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mockoon 服务管理控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/admin/mockoon-service")
@RequiredArgsConstructor
public class MockoonServiceController {

    private final MockoonServiceService mockoonServiceService;

    /**
     * 分页查询服务列表
     */
    @AuditLog(module = "沙箱服务管理", businessType = "服务查询", operationType = OperationType.QUERY, description = "查询沙箱服务列表")
    @GetMapping("/list")
    public Result<Page<MockoonService>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        Page<MockoonService> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MockoonService> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            wrapper.like(MockoonService::getName, trimmedKeyword)
                    .or()
                    .like(MockoonService::getDescription, trimmedKeyword);
        }

        wrapper.orderByAsc(MockoonService::getSort).orderByDesc(MockoonService::getCreateTime);
        Page<MockoonService> result = mockoonServiceService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的服务（下拉选择用）
     */
    @AuditLog(module = "沙箱服务管理", businessType = "启用服务查询", operationType = OperationType.QUERY, description = "查询启用中的沙箱服务")
    @GetMapping("/active-list")
    public Result<List<MockoonService>> getActiveList() {
        List<MockoonService> list = mockoonServiceService.getActiveList();
        return Result.success(list);
    }

    /**
     * 根据ID获取服务详情
     */
    @AuditLog(module = "沙箱服务管理", businessType = "服务详情查询", operationType = OperationType.QUERY, description = "查询沙箱服务详情")
    @GetMapping("/{id}")
    public Result<MockoonService> getById(@PathVariable Long id) {
        MockoonService service = mockoonServiceService.getById(id);
        if (service == null) {
            return Result.error("服务不存在");
        }
        return Result.success(service);
    }

    /**
     * 新增服务
     */
    @AuditLog(module = "沙箱服务管理", businessType = "新增服务", operationType = OperationType.INSERT, description = "新增沙箱服务")
    @PostMapping
    public Result<Void> save(@RequestBody MockoonService service) {
        try {
            boolean success = mockoonServiceService.createService(service);
            return success ? Result.success("保存成功", null) : Result.error("保存失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error("保存失败，请稍后重试");
        }
    }

    /**
     * 更新服务
     */
    @AuditLog(module = "沙箱服务管理", businessType = "编辑服务", operationType = OperationType.UPDATE, description = "编辑沙箱服务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MockoonService service) {
        service.setId(id);
        try {
            boolean success = mockoonServiceService.updateService(service);
            return success ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败，请稍后重试");
        }
    }

    /**
     * 删除服务
     */
    @AuditLog(module = "沙箱服务管理", businessType = "删除服务", operationType = OperationType.DELETE, description = "删除沙箱服务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (mockoonServiceService.getById(id) == null) {
            return Result.error("服务不存在");
        }
        boolean success = mockoonServiceService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestParam List<Long> ids) {
        boolean success = mockoonServiceService.removeByIds(ids);
        return success ? Result.success() : Result.error("批量删除失败");
    }
}
