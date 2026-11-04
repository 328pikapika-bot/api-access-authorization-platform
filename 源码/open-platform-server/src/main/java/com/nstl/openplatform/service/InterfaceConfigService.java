package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nstl.openplatform.entity.InterfaceConfig;

/**
 * 接口配置 Service 接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface InterfaceConfigService extends IService<InterfaceConfig> {

    /**
     * 根据分类ID获取接口配置
     *
     * @param categoryId 分类ID
     * @return 接口配置
     */
    InterfaceConfig getByCategoryId(Long categoryId);

    /**
     * 保存或更新接口配置
     *
     * @param config 接口配置
     * @return 是否成功
     */
    boolean saveOrUpdateConfig(InterfaceConfig config);
}
