package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.MockoonService;
import com.nstl.openplatform.mapper.MockoonServiceMapper;
import com.nstl.openplatform.service.MockoonServiceService;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Mockoon 服务管理 Service 实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MockoonServiceServiceImpl extends ServiceImpl<MockoonServiceMapper, MockoonService> implements MockoonServiceService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 255;
    private static final int MAX_SORT = 9999;

    private final MockoonServiceMapper mockoonServiceMapper;

    @Override
    public List<MockoonService> getActiveList() {
        return mockoonServiceMapper.selectActiveList();
    }

    @Override
    public MockoonService validateAndNormalize(MockoonService service, boolean update) {
        if (service == null) {
            throw new IllegalArgumentException("服务信息不能为空");
        }

        service.setName(trimToNull(service.getName()));
        service.setServiceUrl(trimToNull(service.getServiceUrl()));
        service.setDescription(trimToNull(service.getDescription()));
        if (service.getStatus() == null) {
            service.setStatus(1);
        }
        if (service.getSort() == null) {
            service.setSort(0);
        }

        if (!StringUtils.hasText(service.getName())) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
        if (service.getName().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("服务名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(service.getServiceUrl())) {
            throw new IllegalArgumentException("服务地址不能为空");
        }
        if (service.getServiceUrl().length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("服务地址长度不能超过255个字符");
        }
        validateUrl(service.getServiceUrl());
        if (service.getDescription() != null && service.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("服务说明长度不能超过500个字符");
        }
        if (service.getSort() < 0 || service.getSort() > MAX_SORT) {
            throw new IllegalArgumentException("排序值必须为0到9999之间的整数");
        }
        if (service.getStatus() != 0 && service.getStatus() != 1) {
            throw new IllegalArgumentException("状态值不合法");
        }

        LambdaQueryWrapper<MockoonService> nameWrapper = new LambdaQueryWrapper<MockoonService>()
                .eq(MockoonService::getName, service.getName());
        LambdaQueryWrapper<MockoonService> urlWrapper = new LambdaQueryWrapper<MockoonService>()
                .eq(MockoonService::getServiceUrl, service.getServiceUrl());

        if (update && service.getId() != null) {
            nameWrapper.ne(MockoonService::getId, service.getId());
            urlWrapper.ne(MockoonService::getId, service.getId());
        }

        if (this.count(nameWrapper) > 0) {
            throw new IllegalArgumentException("服务名称已存在，请更换后重试");
        }
        if (this.count(urlWrapper) > 0) {
            throw new IllegalArgumentException("服务地址已存在，请更换后重试");
        }

        return service;
    }

    @Override
    public boolean createService(MockoonService service) {
        validateAndNormalize(service, false);
        return this.save(service);
    }

    @Override
    public boolean updateService(MockoonService service) {
        if (service == null || service.getId() == null) {
            throw new IllegalArgumentException("服务ID不能为空");
        }
        MockoonService existing = this.getById(service.getId());
        if (existing == null) {
            throw new IllegalArgumentException("服务不存在");
        }

        existing.setName(service.getName());
        existing.setServiceUrl(service.getServiceUrl());
        existing.setDescription(service.getDescription());
        existing.setStatus(service.getStatus());
        existing.setSort(service.getSort());
        validateAndNormalize(existing, true);
        return this.updateById(existing);
    }

    private void validateUrl(String url) {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new IllegalArgumentException("服务地址格式不正确，需要以 http:// 或 https:// 开头");
        }
        try {
            URI uri = new URI(url);
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("服务地址格式不正确，请检查主机名");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("服务地址格式不正确，请检查输入内容");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
