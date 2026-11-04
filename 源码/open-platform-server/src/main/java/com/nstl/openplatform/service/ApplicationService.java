package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nstl.openplatform.entity.ContentApplication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 申请服务接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface ApplicationService extends IService<ContentApplication> {

    /**
     * 将多个申请文件上传到 MinIO，返回 JSON 格式的对象 key 列表
     *
     * @param files 上传的文件数组（最多 10 个，每个 ≤ 10MB）
     * @return JSON 数组字符串，如 ["applications/2026/04/xxx.pdf","applications/2026/04/yyy.jpg"]
     */
    String saveApplicationFiles(MultipartFile[] files);

    /**
     * 生成 AppKey
     */
    String generateAppKey();

    /**
     * 生成 AppSecret
     */
    String generateAppSecret();
}

