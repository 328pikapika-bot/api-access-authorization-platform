package com.nstl.openplatform.controller;

import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.InterfaceConfig;
import com.nstl.openplatform.service.InterfaceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 接口配置控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@RestController
@RequestMapping("/admin/interface-config")
@RequiredArgsConstructor
public class InterfaceConfigController {

    private final InterfaceConfigService interfaceConfigService;

    /**
     * 根据分类ID获取接口配置
     */
    @GetMapping("/by-category/{categoryId}")
    public Result<InterfaceConfig> getByCategoryId(@PathVariable Long categoryId) {
        InterfaceConfig config = interfaceConfigService.getByCategoryId(categoryId);
        if (config == null) {
            return Result.success(null);
        }
        return Result.success(config);
    }

    /**
     * 保存或更新接口配置
     */
    @PostMapping("/save")
    public Result<Void> saveOrUpdate(@RequestBody InterfaceConfig config) {
        boolean success = interfaceConfigService.saveOrUpdateConfig(config);
        return success ? Result.success() : Result.error("保存失败");
    }

    /**
     * 根据ID获取接口配置详情
     */
    @GetMapping("/{id}")
    public Result<InterfaceConfig> getById(@PathVariable Long id) {
        InterfaceConfig config = interfaceConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.success(config);
    }

    /**
     * 删除接口配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = interfaceConfigService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 根据分类ID删除配置
     */
    @DeleteMapping("/by-category/{categoryId}")
    public Result<Void> deleteByCategoryId(@PathVariable Long categoryId) {
        InterfaceConfig config = interfaceConfigService.getByCategoryId(categoryId);
        if (config != null) {
            boolean success = interfaceConfigService.removeById(config.getId());
            return success ? Result.success() : Result.error("删除失败");
        }
        return Result.success();
    }
}
