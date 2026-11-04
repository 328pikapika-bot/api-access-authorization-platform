package com.nstl.sandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 沙箱服务启动类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@SpringBootApplication
public class SandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxApplication.class, args);
        System.out.println("========================================");
        System.out.println("NSTL沙箱服务启动成功");
        System.out.println("========================================");
    }
}
