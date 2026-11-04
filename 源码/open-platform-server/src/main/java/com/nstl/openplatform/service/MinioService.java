package com.nstl.openplatform.service;

import com.nstl.openplatform.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 *
 * <p>负责初始化存储桶、上传文件并返回对象路径（object key）。</p>
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 启动时确保存储桶存在，不存在则自动创建
     */
    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
                log.info("MinIO bucket '{}' created.", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO bucket init failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file MultipartFile 文件对象
     * @return 对象在 MinIO 中的 key（形如 applications/2026/04/uuid.pdf）
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = "applications/" + datePath + "/"
                    + UUID.randomUUID().toString().replace("-", "") + ext;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded to MinIO: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("MinIO upload failed for file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("文件上传到 MinIO 失败", e);
        }
    }

    /**
     * 上传图片到 MinIO（专门用于Markdown编辑器的图片上传）
     *
     * @param file MultipartFile 图片文件对象
     * @return 对象在 MinIO 中的 key（形如 images/2026/04/uuid.jpg）
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            // 图片存储在 images/ 路径下
            String objectKey = "images/" + datePath + "/"
                    + UUID.randomUUID().toString().replace("-", "") + ext;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded image to MinIO: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("MinIO image upload failed for file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("图片上传到 MinIO 失败", e);
        }
    }

    /**
     * 生成文件预签名访问 URL（默认有效期 7 天）
     *
     * @param objectKey MinIO 对象 key
     * @return 临时访问 URL
     */
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(7 * 24 * 3600) // 7天
                            .build()
            );
        } catch (Exception e) {
            log.error("Get presigned URL failed for '{}': {}", objectKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取文件输入流
     *
     * @param objectKey MinIO 对象 key
     * @return InputStream
     */
    public InputStream getFileStream(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Get file stream failed for '{}': {}", objectKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取文件 Content-Type
     *
     * @param objectKey MinIO 对象 key
     * @return Content-Type
     */
    public String getContentType(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return stat.contentType();
        } catch (Exception e) {
            log.error("Get content type failed for '{}': {}", objectKey, e.getMessage(), e);
            return null;
        }
    }
}
