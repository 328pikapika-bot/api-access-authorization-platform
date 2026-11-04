package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.InterfaceConfig;
import com.nstl.openplatform.mapper.InterfaceConfigMapper;
import com.nstl.openplatform.service.InterfaceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 接口配置 Service 实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class InterfaceConfigServiceImpl extends ServiceImpl<InterfaceConfigMapper, InterfaceConfig> implements InterfaceConfigService {

    private final InterfaceConfigMapper interfaceConfigMapper;

    @Override
    public InterfaceConfig getByCategoryId(Long categoryId) {
        return interfaceConfigMapper.selectByCategoryId(categoryId);
    }

    @Override
    public boolean saveOrUpdateConfig(InterfaceConfig config) {
        // 检查是否已存在
        InterfaceConfig existing = getByCategoryId(config.getCategoryId());
        if (existing != null) {
            config.setId(existing.getId());
            return updateById(config);
        }
        return save(config);
    }
}
