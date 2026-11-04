package com.nstl.openplatform.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.mapper.ContentApplicationMapper;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 申请服务实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl extends ServiceImpl<ContentApplicationMapper, ContentApplication>
        implements ApplicationService {

    private final MinioService minioService;

    /**
     * 将多个申请文件上传到 MinIO，返回 JSON 格式的对象 key 列表。
     * 单文件大小由 Spring Multipart 配置限制（≤10MB），此处额外校验数量（≤10个）。
     */
    @Override
    public String saveApplicationFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return null;
        }
        if (files.length > 10) {
            throw new IllegalArgumentException("最多只能上传 10 个文件");
        }

        List<String> objectKeys = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String key = minioService.upload(file);
            if (key != null) {
                objectKeys.add(key);
            }
        }

        return objectKeys.isEmpty() ? null : JSON.toJSONString(objectKeys);
    }

    @Override
    public String generateAppKey() {
        return "AK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Override
    public String generateAppSecret() {
        return "SK" + UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
    }
}
