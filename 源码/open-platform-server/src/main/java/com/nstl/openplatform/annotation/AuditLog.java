package com.nstl.openplatform.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 业务类型
     */
    String businessType() default "";

    /**
     * 操作类型
     */
    OperationType operationType() default OperationType.OTHER;

    /**
     * 描述
     */
    String description() default "";
}
