package com.nstl.openplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NSTL接口平台启动类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class OpenPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenPlatformApplication.class, args);
        System.out.println("========================================");
        System.out.println("NSTL接口平台启动成功");
        System.out.println("Swagger文档地址: http://localhost:17300/doc.html");
        System.out.println("========================================");
    }
}
