package com.nstl.openplatform.controller;

import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 *
 * <p>提供通用文件上传接口，支持图片等文件上传到 MinIO，图片访问通过Java层转发。</p>
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final MinioService minioService;

    /**
     * 上传图片文件
     *
     * @param file 图片文件
     * @return 包含图片访问路径的结果（通过Java层转发访问）
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只允许上传图片文件");
        }

        // 校验文件大小（最大 10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("图片大小不能超过 10MB");
        }

        try {
            // 上传图片到 MinIO（使用专门的图片上传方法）
            String objectKey = minioService.uploadImage(file);
            if (objectKey == null) {
                return Result.error("文件上传失败");
            }

            // 使用 Base64 编码 objectKey，避免特殊字符问题
            String encodedKey = Base64.getUrlEncoder().encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
            // 返回通过Java层转发的访问路径（使用查询参数方式）
            String fileUrl = "/api/v1/service/open_platform/admin/upload/image/view?key=" + encodedKey;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("objectKey", objectKey);

            log.info("图片上传成功: {}", objectKey);
            return Result.success(result);
        } catch (Exception e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过Java层转发获取图片（不直接暴露MinIO）
     *
     * @param key Base64编码后的图片对象key
     * @param response HTTP响应
     */
    @GetMapping("/image/view")
    public void getImage(@RequestParam("key") String key, HttpServletResponse response) {
        try {
            // Base64 解码获取 objectKey
            String objectKey = new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
            log.debug("获取图片，objectKey: {}", objectKey);

            // 获取文件流
            InputStream inputStream = minioService.getFileStream(objectKey);
            if (inputStream == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 获取Content-Type
            String contentType = minioService.getContentType(objectKey);
            if (contentType != null) {
                response.setContentType(contentType);
            } else {
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            }

            // 写入响应流
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            inputStream.close();
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("获取图片失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
