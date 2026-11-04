package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstl.openplatform.dto.SandboxRequest;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.InterfaceConfig;
import com.nstl.openplatform.entity.MockoonService;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.entity.SandboxLog;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.mapper.ContentApplicationMapper;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.InterfaceConfigService;
import com.nstl.openplatform.service.MockoonServiceService;
import com.nstl.openplatform.service.PlatformColumnService;
import com.nstl.openplatform.service.PlatformService;
import com.nstl.openplatform.service.SandboxLogService;
import com.nstl.openplatform.service.SandboxService;
import com.nstl.openplatform.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxServiceImpl implements SandboxService {

    private final ContentApplicationMapper contentApplicationMapper;
    private final InterfaceConfigService interfaceConfigService;
    private final SysUserService sysUserService;
    private final ContentCategoryService contentCategoryService;
    private final PlatformService platformService;
    private final PlatformColumnService platformColumnService;
    private final SandboxLogService sandboxLogService;
    private final MockoonServiceService mockoonServiceService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sandbox.mockoon-url:http://localhost:8082}")
    private String mockoonUrl;

    @PostConstruct
    public void init() {
        log.info("沙箱服务初始化完成，Mockoon 地址: {}", mockoonUrl);
    }

    @Override
    public Map<String, Object> executeDebug(String appKey, SandboxRequest request) {
        long startTime = System.currentTimeMillis();
        SandboxRequest safeRequest = request != null ? request : new SandboxRequest();
        Map<String, Object> response = new HashMap<>();

        try {
            if (appKey == null || appKey.trim().isEmpty()) {
                response.put("code", 401);
                response.put("message", "API Key 不能为空");
                recordFailedLog(null, appKey, safeRequest, 401, response, startTime);
                return response;
            }

            ContentApplication application = contentApplicationMapper.selectByAppKey(appKey);
            if (application == null) {
                response.put("code", 401);
                response.put("message", "无效的 API Key");
                recordFailedLog(null, appKey, safeRequest, 401, response, startTime);
                return response;
            }

            Long categoryId = safeRequest.getCategoryId();
            if (categoryId == null) {
                response.put("code", 400);
                response.put("message", "接口分类ID不能为空");
                recordFailedLog(application.getUserId(), appKey, safeRequest, 400, response, startTime);
                return response;
            }

            boolean hasAccess = false;
            String rawCatIds = application.getCategoryIds();
            if (rawCatIds != null && !rawCatIds.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Integer> catIds = objectMapper.readValue(rawCatIds, List.class);
                    hasAccess = checkCategoryAccess(catIds, categoryId);
                } catch (JsonProcessingException e) {
                    log.warn("解析 categoryIds 失败: {}", rawCatIds);
                }
            }

            if (!hasAccess) {
                response.put("code", 403);
                response.put("message", "您尚未获得该接口或其上级分类授权，请联系管理员分配权限");
                recordFailedLog(application.getUserId(), appKey, safeRequest, 403, response, startTime);
                return response;
            }

            InterfaceConfig config = interfaceConfigService.getByCategoryId(categoryId);
            return doForward(safeRequest, config, appKey, application.getUserId(), startTime);
        } catch (Exception e) {
            log.error(
                    "沙箱请求处理失败 - categoryId: {}, path: {}, method: {}, error: {}",
                    safeRequest.getCategoryId(),
                    safeRequest.getContentPath(),
                    safeRequest.getMethod(),
                    e.getMessage(),
                    e
            );
            response.put("code", 500);
            response.put("message", "沙箱服务暂时不可用 " + e.getMessage());
            recordFailedLog(null, appKey, safeRequest, 500, response, startTime);
            return response;
        }
    }

    @Override
    public Map<String, Object> executeDebugForAdmin(SandboxRequest request) {
        long startTime = System.currentTimeMillis();
        SandboxRequest safeRequest = request != null ? request : new SandboxRequest();
        InterfaceConfig config = interfaceConfigService.getByCategoryId(safeRequest.getCategoryId());

        Long adminUserId = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                adminUserId = (Long) details.get("userId");
            }
        } catch (Exception e) {
            log.warn("获取管理员用户信息失败", e);
        }

        try {
            return doForward(safeRequest, config, "ADMIN", adminUserId, startTime);
        } catch (Exception e) {
            log.error("管理员沙箱请求处理失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "服务调试失败: " + e.getMessage());
            recordFailedLog(adminUserId, "ADMIN", safeRequest, 500, response, startTime);
            return response;
        }
    }

    private Map<String, Object> doForward(SandboxRequest request, InterfaceConfig config, String appKey, Long userId, long startTime) throws Exception {
        Map<String, Object> response = new HashMap<>();

        String apiPath = request.getContentPath();
        if (apiPath == null || apiPath.isEmpty()) {
            if (config != null && config.getApiPath() != null) {
                apiPath = config.getApiPath();
            } else {
                response.put("code", 400);
                response.put("message", "接口地址未配置");
                recordFailedLog(userId, appKey, request, 400, response, startTime);
                return response;
            }
        }

        String method = request.getMethod();
        if (method == null || method.trim().isEmpty()) {
            method = (config != null && config.getApiMethod() != null) ? config.getApiMethod() : "GET";
        }

        boolean absoluteApiUrl = isAbsoluteHttpUrl(apiPath);
        String baseUrl = mockoonUrl;
        if (!absoluteApiUrl && (baseUrl == null || baseUrl.isEmpty())) {
            response.put("code", 500);
            response.put("message", "Mockoon 服务地址未配置");
            recordFailedLog(userId, appKey, request, 500, response, startTime);
            return response;
        }

        Long serviceId = request.getMockoonServiceId();
        if (serviceId == null && config != null) {
            serviceId = config.getMockoonServiceId();
        }
        if (!absoluteApiUrl && serviceId != null) {
            MockoonService service = mockoonServiceService.getById(serviceId);
            if (service != null && service.getServiceUrl() != null && !service.getServiceUrl().isEmpty()) {
                baseUrl = service.getServiceUrl();
            }
        }

        String targetUrl = buildTargetUrl(baseUrl, apiPath, request.getParams());
        log.debug("沙箱请求转发到 {}", targetUrl);

        HttpHeaders headers = new HttpHeaders();
        Object bodyObj = request.getBody();
        String bodyType = request.getBodyType();
        if (bodyType == null && config != null) {
            bodyType = config.getBodyType();
        }

        if (bodyType != null && !"none".equals(bodyType)) {
            if ("form-data".equals(bodyType)) {
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                if (request.getBodyFormData() != null && !request.getBodyFormData().isEmpty()) {
                    bodyObj = request.getBodyFormData();
                }
            } else if ("x-www-form-urlencoded".equals(bodyType)) {
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                if (request.getBodyFormData() != null && !request.getBodyFormData().isEmpty()) {
                    org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
                    request.getBodyFormData().forEach((key, value) -> map.add(key, value != null ? value.toString() : ""));
                    bodyObj = map;
                }
            } else if ("raw".equals(bodyType)) {
                if (config != null && config.getContentType() != null) {
                    headers.setContentType(MediaType.valueOf(config.getContentType()));
                } else {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
            }
        } else if (config != null && config.getContentType() != null) {
            headers.setContentType(MediaType.valueOf(config.getContentType()));
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        if (request.getHeaders() != null) {
            request.getHeaders().forEach(headers::add);
        }

        HttpEntity<Object> httpEntity = new HttpEntity<>(bodyObj, headers);
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

        String responseData = null;
        int statusCode = 200;
        Map<String, String> responseHeaders = new HashMap<>();

        try {
            java.net.URI uri = java.net.URI.create(targetUrl);
            ResponseEntity<String> mockoonResponse = restTemplate.exchange(uri, httpMethod, httpEntity, String.class);
            responseData = mockoonResponse.getBody();
            statusCode = mockoonResponse.getStatusCode().value();
            mockoonResponse.getHeaders().forEach((headerName, values) -> {
                if (!values.isEmpty()) {
                    responseHeaders.put(headerName, values.get(0));
                }
            });
        } catch (RestClientResponseException e) {
            statusCode = e.getRawStatusCode();
            responseData = e.getResponseBodyAsString();
            if (e.getResponseHeaders() != null) {
                e.getResponseHeaders().forEach((headerName, values) -> {
                    if (!values.isEmpty()) {
                        responseHeaders.put(headerName, values.get(0));
                    }
                });
            }
            log.warn("Mockoon 返回错误状态 {}, 响应: {}", statusCode, responseData);
        }

        Object body = responseData;
        if (responseData != null) {
            try {
                body = objectMapper.readValue(responseData, Object.class);
            } catch (JsonProcessingException ignored) {
            }
        }

        response.put("httpStatus", statusCode);
        response.put("body", body);
        response.put("headers", responseHeaders);

        long costTime = System.currentTimeMillis() - startTime;
        request.setMethod(method);
        recordLog(userId, appKey, request, targetUrl, responseData, statusCode, costTime);
        return response;
    }

    private void recordFailedLog(Long userId, String appKey, SandboxRequest request, int statusCode, Map<String, Object> response, long startTime) {
        String responseData;
        try {
            responseData = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            responseData = String.valueOf(response);
        }
        long costTime = System.currentTimeMillis() - startTime;
        recordLog(userId, appKey, request, null, responseData, statusCode, costTime);
    }

    private boolean checkCategoryAccess(List<Integer> allowedCatIds, Long categoryId) {
        if (categoryId == null || allowedCatIds == null) {
            return false;
        }
        if (allowedCatIds.contains(categoryId.intValue())) {
            return true;
        }
        ContentCategory category = contentCategoryService.getById(categoryId);
        if (category != null && category.getParentId() != null && category.getParentId() != 0) {
            return checkCategoryAccess(allowedCatIds, category.getParentId());
        }
        return false;
    }

    static String buildTargetUrl(String baseUrl, String apiPath, Map<String, Object> params) {
        String originalApiPath = apiPath.trim();
        String targetUrl;
        if (isAbsoluteHttpUrl(originalApiPath)) {
            targetUrl = originalApiPath;
        } else {
            String path = originalApiPath.startsWith("/") ? originalApiPath : "/" + originalApiPath;
            String finalBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            targetUrl = finalBaseUrl + path;
        }

        if (targetUrl.contains("?") || params == null || params.isEmpty()) {
            return targetUrl;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(targetUrl);
        if (!params.isEmpty()) {
            params.forEach((paramKey, value) -> {
                if (paramKey != null && value != null) {
                    builder.queryParam(paramKey, value.toString());
                }
            });
        }
        return builder.toUriString();
    }

    private static boolean isAbsoluteHttpUrl(String value) {
        return value != null && value.matches("(?i)^https?://.*");
    }

    @Async
    protected void recordLog(Long userId, String appKey, SandboxRequest request, String targetUrl, String responseData, int statusCode, long costTime) {
        try {
            SandboxLog sandboxLog = new SandboxLog();
            Long resolvedUserId = userId;
            ContentApplication application = null;
            if (resolvedUserId == null && appKey != null && !appKey.trim().isEmpty() && !"ADMIN".equalsIgnoreCase(appKey)) {
                application = contentApplicationMapper.selectByAppKey(appKey);
                if (application != null) {
                    resolvedUserId = application.getUserId();
                }
            }

            sandboxLog.setUserId(resolvedUserId);
            sandboxLog.setAppKey(appKey);
            sandboxLog.setCategoryId(request.getCategoryId());
            sandboxLog.setRequestMethod(request.getMethod());
            sandboxLog.setStatusCode(statusCode);
            sandboxLog.setCostTime((int) costTime);

            if (resolvedUserId != null) {
                SysUser user = sysUserService.getById(resolvedUserId);
                if (user != null) {
                    sandboxLog.setBusinessUserId(user.getUserId());
                    sandboxLog.setUsername(user.getUserName());
                    sandboxLog.setNickname(user.getNickname());
                    sandboxLog.setNstlUnionId(user.getNstlUnionId());
                } else if (application != null) {
                    sandboxLog.setNstlUnionId(application.getNstlUnionId());
                }
            } else if (application != null) {
                sandboxLog.setNstlUnionId(application.getNstlUnionId());
            }

            if ((sandboxLog.getUsername() == null || sandboxLog.getNickname() == null || sandboxLog.getBusinessUserId() == null)
                    && sandboxLog.getNstlUnionId() != null && !sandboxLog.getNstlUnionId().trim().isEmpty()) {
                SysUser fallbackUser = sysUserService.lambdaQuery()
                        .eq(SysUser::getNstlUnionId, sandboxLog.getNstlUnionId().trim())
                        .one();
                if (fallbackUser != null) {
                    if (sandboxLog.getUserId() == null) {
                        sandboxLog.setUserId(fallbackUser.getId());
                    }
                    if (sandboxLog.getBusinessUserId() == null) {
                        sandboxLog.setBusinessUserId(fallbackUser.getUserId());
                    }
                    if (sandboxLog.getUsername() == null) {
                        sandboxLog.setUsername(fallbackUser.getUserName());
                    }
                    if (sandboxLog.getNickname() == null) {
                        sandboxLog.setNickname(fallbackUser.getNickname());
                    }
                }
            }

            if (request.getCategoryId() != null) {
                ContentCategory category = contentCategoryService.getById(request.getCategoryId());
                if (category != null) {
                    sandboxLog.setCategoryName(category.getName());
                    sandboxLog.setInterfaceName(category.getName());
                    if (category.getColumnId() != null) {
                        PlatformColumn column = platformColumnService.getById(category.getColumnId());
                        if (column != null && column.getPlatformId() != null) {
                            Platform platform = platformService.getById(column.getPlatformId());
                            if (platform != null) {
                                sandboxLog.setPlatformId(platform.getId());
                                sandboxLog.setPlatformName(platform.getName());
                            }
                        }
                    }
                }
            }

            sandboxLog.setRequestUrl(targetUrl);

            if (request.getParams() != null && !request.getParams().isEmpty()) {
                sandboxLog.setRequestParams(objectMapper.writeValueAsString(request.getParams()));
            }
            if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
                sandboxLog.setRequestHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            }
            if (request.getBody() != null) {
                String body = request.getBody() instanceof String
                        ? (String) request.getBody()
                        : objectMapper.writeValueAsString(request.getBody());
                if (body.length() > 5000) {
                    body = body.substring(0, 5000) + "... [truncated]";
                }
                sandboxLog.setRequestBody(body);
            }

            if (responseData != null && responseData.length() > 10000) {
                responseData = responseData.substring(0, 10000) + "... [truncated]";
            }
            sandboxLog.setResponseContent(responseData);

            sandboxLogService.save(sandboxLog);
            log.debug("沙箱调用日志已记录，用户ID: {}, 耗时: {}ms", resolvedUserId, costTime);
        } catch (Exception e) {
            log.error("记录沙箱调用日志失败", e);
        }
    }
}
