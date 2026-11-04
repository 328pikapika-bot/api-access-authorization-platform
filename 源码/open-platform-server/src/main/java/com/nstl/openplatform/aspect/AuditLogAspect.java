package com.nstl.openplatform.aspect;

import com.alibaba.fastjson2.JSON;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.mapper.AuditLogMapper;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 审计日志切面
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("applicationTaskExecutor")
    private Executor asyncExecutor;

    /**
     * 环绕通知，拦截带有 @AuditLog 注解的方法
     */
    @Around("@annotation(com.nstl.openplatform.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 异步记录审计日志
            long costTime = System.currentTimeMillis() - startTime;
            com.nstl.openplatform.entity.AuditLog auditLogEntity = buildAuditLog(joinPoint, result, exception, costTime);

            if (asyncExecutor != null) {
                // 使用配置的线程池异步执行
                CompletableFuture.runAsync(() -> saveAuditLog(auditLogEntity), asyncExecutor);
            } else {
                // 使用默认异步方法
                saveAuditLogAsync(auditLogEntity);
            }
        }
    }

    /**
     * 构建审计日志对象
     */
    private com.nstl.openplatform.entity.AuditLog buildAuditLog(ProceedingJoinPoint joinPoint, Object result, Exception exception, long costTime) {
        com.nstl.openplatform.entity.AuditLog auditLog = new com.nstl.openplatform.entity.AuditLog();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解信息
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);
        if (auditLogAnnotation != null) {
            auditLog.setModuleName(auditLogAnnotation.module());
            auditLog.setBusinessType(auditLogAnnotation.businessType());
            auditLog.setOperationType(auditLogAnnotation.operationType().getCode());
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setRequestUrl(request.getRequestURI());
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setClientIp(getClientIp(request));

            // 从安全上下文中获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                Object details = authentication.getDetails();
                if (details instanceof java.util.Map) {
                    java.util.Map<String, Object> detailsMap = (java.util.Map<String, Object>) details;
                    Object userId = detailsMap.get("userId");
                    if (userId instanceof Number) {
                        auditLog.setUserId(((Number) userId).longValue());
                    }
                    String username = asString(detailsMap.get("username"));
                    auditLog.setUsername(username != null ? username : authentication.getName());
                    auditLog.setNickname(asString(detailsMap.get("nickname")));
                    auditLog.setNstlUnionId(asString(detailsMap.get("nstlUnionId")));
                } else {
                    auditLog.setUsername(authentication.getName());
                }
            } else {
                auditLog.setUsername("unknown");
            }

            // SSO 登录场景兜底：当 SecurityContext 中无用户信息时，尝试从返回结果中提取
            if (auditLog.getUserId() == null && result != null) {
                tryExtractUserFromResult(result, auditLog);
            }

        }




        // 记录请求参数
        Object[] args = joinPoint.getArgs();
        try {
            if (args != null && args.length > 0) {
                // 过滤掉不需要记录的参数类型（如 HttpServletRequest、HttpServletResponse）
                String params = filterParams(args);
                if (params.length() <= 2000) {
                    auditLog.setRequestParams(params);
                }
            }
        } catch (Exception e) {
            log.error("解析请求参数失败", e);
        }

        // 记录响应结果
        try {
            if (result != null) {
                String resultStr = JSON.toJSONString(result);
                if (resultStr.length() <= 2000) {
                    auditLog.setResponseResult(resultStr);
                }
            }
        } catch (Exception e) {
            log.error("序列化响应结果失败", e);
        }

        // 记录操作状态和错误信息
        if (exception != null) {
            auditLog.setOperationStatus(0);
            String errorMsg = exception.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            auditLog.setErrorMessage(errorMsg);
        } else {
            auditLog.setOperationStatus(1);
        }

        auditLog.setCostTime(costTime);

        return auditLog;
    }

    /**
     * 过滤不需要记录的参数
     */
    private String filterParams(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 过滤掉 Servlet 相关对象
            String className = arg.getClass().getName();
            if (className.contains("javax.servlet") || className.contains("org.springframework")) {
                continue;
            }
            sb.append(JSON.toJSONString(arg)).append("; ");
        }
        return sb.toString();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 从方法返回结果中提取用户信息（用于SSO登录等尚未认证的场景）
     */
    private void tryExtractUserFromResult(Object result, com.nstl.openplatform.entity.AuditLog auditLog) {
        try {
            // result 结构: Result<Map<String, Object>>, 其中 data.userInfo 是 SysUser
            if (!(result instanceof com.nstl.openplatform.common.Result)) {
                return;
            }
            Object data = ((com.nstl.openplatform.common.Result<?>) result).getData();
            if (!(data instanceof java.util.Map)) {
                return;
            }
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
            Object userInfo = dataMap.get("userInfo");
            if (userInfo instanceof com.nstl.openplatform.entity.SysUser) {
                com.nstl.openplatform.entity.SysUser sysUser = (com.nstl.openplatform.entity.SysUser) userInfo;
                auditLog.setUserId(sysUser.getId());
                auditLog.setUsername(sysUser.getUserName());
                auditLog.setNickname(sysUser.getNickname());
                auditLog.setNstlUnionId(sysUser.getNstlUnionId());
            }
        } catch (Exception e) {
            log.debug("从返回结果提取用户信息失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 异步保存审计日志
     */
    @Async
    public void saveAuditLogAsync(com.nstl.openplatform.entity.AuditLog auditLog) {
        try {
            saveAuditLog(auditLog);
        } catch (Exception e) {
            log.error("异步保存审计日志失败", e);
        }
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(com.nstl.openplatform.entity.AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
            log.debug("审计日志记录成功: {}", auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }
}
