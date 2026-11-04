package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nstl.openplatform.annotation.AuditLog;
import com.nstl.openplatform.annotation.OperationType;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.config.OAuth2Config;
import com.nstl.openplatform.dto.NstlSessionRequest;
import com.nstl.openplatform.dto.OAuth2TokenDTO;
import com.nstl.openplatform.dto.UserInfoDTO;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.mapper.SysUserMapper;
import com.nstl.openplatform.util.JwtUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String MAIN_SITE_LOGIN_CHECK_URL =
            "https://www.nstl.gov.cn/api/service/nstl/web/vilidate/isLogin";

    private static final Set<String> INVALID_USER_IDS = Set.of(
            "default",
            "unknown",
            "mock_user_id",
            "test_user",
            "test_account"
    );

    private static final String LOCAL_FALLBACK_USER_ID = "local-nstr-user";
    private static final String LOCAL_FALLBACK_USER_NAME = "NSTR 用户";

    private final OAuth2Config oauth2Config;
    private final SysUserMapper sysUserMapper;
    private final RestTemplate restTemplate;

    /**
     * Enabled only by the local start script. Test and production deployments
     * must continue to depend on the real NSTL unified-login identity.
     */
    @Value("${nstl.local-fallback.enabled:false}")
    private boolean localFallbackEnabled;

    @GetMapping("/sso/login")
    public Result<String> getLoginUrl() {
        if (!oauth2Config.getEnabled()) {
            return Result.error("SSO未启用");
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        String loginUrl = UriComponentsBuilder
                .fromHttpUrl(oauth2Config.getAuthorizationUri())
                .queryParam("client_id", oauth2Config.getClientId())
                .queryParam("redirect_uri", oauth2Config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        return Result.success(loginUrl);
    }

    @PostMapping("/sso/callback")
    @AuditLog(
            module = "用户认证",
            businessType = "SSO登录",
            operationType = OperationType.LOGIN,
            description = "SSO单点登录"
    )
    public Result<Map<String, Object>> ssoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(name = "login_type", required = false) String loginType
    ) {
        if (!oauth2Config.getEnabled()) {
            return Result.error("SSO未启用");
        }

        log.info("收到SSO回调请求 - code: {}, redirect_uri: {}, login_type: {}", code, redirect_uri, loginType);

        try {
            OAuth2TokenDTO tokenResponse = exchangeCodeForToken(code, redirect_uri);
            if (tokenResponse == null || isBlank(tokenResponse.getAccess_token())) {
                return Result.error("获取token失败，请检查 redirect_uri 是否与统一认证回调地址一致");
            }

            UserInfoDTO userInfo = fetchSsoUserInfo(tokenResponse.getAccess_token());
            if (userInfo == null || isBlank(userInfo.getUnionId())) {
                return Result.error("获取用户信息失败");
            }

            SysUser sysUser = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getNstlUnionId, userInfo.getUnionId())
                            .last("LIMIT 1")
            );

            boolean adminLogin = "admin".equalsIgnoreCase(trimToNull(loginType));
            if (adminLogin) {
                Result<Map<String, Object>> authorization = validateAuthorizedUser(sysUser, true);
                if (authorization != null) {
                    return authorization;
                }
                syncSsoUserProfile(sysUser, userInfo);
                sysUserMapper.updateById(sysUser);
                log.info("后台SSO用户登录成功，已同步本地账号资料 - unionId: {}, userId: {}",
                        userInfo.getUnionId(), userInfo.getUserName());
            } else if (sysUser == null) {
                sysUser = new SysUser();
                syncSsoUser(sysUser, userInfo);
                sysUserMapper.insert(sysUser);
                log.info("SSO用户首次登录，已自动创建本地账号 - unionId: {}, userId: {}",
                        userInfo.getUnionId(), userInfo.getUserName());
            } else {
                if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
                    return Result.error("当前账号已被禁用，请联系管理员");
                }
                syncSsoUser(sysUser, userInfo);
                sysUserMapper.updateById(sysUser);
                log.info("SSO用户登录成功，已同步本地账号 - unionId: {}, userId: {}",
                        userInfo.getUnionId(), userInfo.getUserName());
            }

            return Result.success(buildAuthResult(sysUser));
        } catch (Exception e) {
            log.error("SSO登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/nstl/session")
    @AuditLog(
            module = "用户认证",
            businessType = "NSTL统一认证登录",
            operationType = OperationType.LOGIN,
            description = "根据NSTL主站登录态建立本地会话"
    )
    public Result<Map<String, Object>> createNstlSession(
            @RequestBody(required = false) NstlSessionRequest request,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader
    ) {
        NstlSessionRequest sessionRequest = request == null ? new NstlSessionRequest() : request;
        boolean adminLogin = "admin".equalsIgnoreCase(trimToNull(sessionRequest.getLoginType()));

        try {
            NstlSessionUser sessionUser = resolveNstlSessionUser(cookieHeader, sessionRequest);
            if (!hasLookupIdentity(sessionUser)) {
                return Result.error("未通过NSTL主站登录态校验，且未获取到可用的本地兜底用户信息，请先完成统一认证登录");
            }

            SysUser sysUser = findLocalUser(sessionUser);
            if (sysUser == null && isLocalFallbackUser(sessionUser)) {
                sysUser = createLocalFallbackUser();
            }
            Result<Map<String, Object>> authorization = validateAuthorizedUser(sysUser, adminLogin);
            if (authorization != null) {
                return authorization;
            }

            if (isBlank(sessionUser.unionId) && isBlank(sysUser.getNstlUnionId())) {
                return Result.error("当前权限记录缺少NSTL联合ID，请联系管理员");
            }

            syncNstlSessionUserProfile(sysUser, sessionUser);
            sysUserMapper.updateById(sysUser);
            return Result.success(buildAuthResult(sysUser));
        } catch (Exception e) {
            log.error("建立NSTL本地会话失败", e);
            return Result.error("建立登录会话失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @AuditLog(
            module = "用户认证",
            businessType = "退出登录",
            operationType = OperationType.LOGOUT,
            description = "用户退出登录"
    )
    public Result<Void> logout(HttpServletResponse response) {
        expireCookie(response, "SESSION", null, true);
        expireCookie(response, "SESSION", "nstl.gov.cn", true);
        expireCookie(response, "CASTGC", "nstl.gov.cn", true);
        expireCookie(response, "nstl_rsp", "nstl.gov.cn", false);
        log.info("用户退出登录");
        return Result.success("退出成功", null);
    }

    private void expireCookie(HttpServletResponse response, String name, String domain, boolean httpOnly) {
        StringBuilder cookie = new StringBuilder(name)
                .append("=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure; SameSite=Lax");
        if (domain != null) {
            cookie.append("; Domain=").append(domain);
        }
        if (httpOnly) {
            cookie.append("; HttpOnly");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @GetMapping("/user/info")
    public Result<SysUser> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error("未登录");
        }

        try {
            String token = authorization.substring(7);
            Long userId = JwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error("登录已失效");
            }

            SysUser sysUser = sysUserMapper.selectById(userId);
            if (sysUser == null) {
                return Result.error("用户不存在");
            }

            sysUser.setDeleted(null);
            return Result.success(sysUser);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error("获取用户信息失败");
        }
    }

    private OAuth2TokenDTO exchangeCodeForToken(String code, String redirectUri) {
        try {
            String actualRedirectUri = !isBlank(redirectUri)
                    ? redirectUri
                    : oauth2Config.getRedirectUri();

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(oauth2Config.getTokenUri())
                    .queryParam("code", code)
                    .queryParam("client_id", oauth2Config.getClientId())
                    .queryParam("client_secret", oauth2Config.getClientSecret())
                    .queryParam("redirect_uri", actualRedirectUri)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            log.info("请求token URL: {}", uri);
            log.info("使用redirect_uri: {}", actualRedirectUri);

            String response = restTemplate.getForObject(uri, String.class);
            log.info("token响应: {}", response);

            if (isBlank(response)) {
                log.error("获取token失败: 响应为空");
                return null;
            }

            if (response.contains("error=")) {
                log.error("获取token失败: 服务端返回错误, 响应: {}", response);
                return null;
            }

            OAuth2TokenDTO tokenDTO = new OAuth2TokenDTO();
            String[] pairs = response.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                if ("access_token".equals(kv[0])) {
                    tokenDTO.setAccess_token(kv[1]);
                } else if ("expires".equals(kv[0])) {
                    try {
                        tokenDTO.setExpires(Integer.parseInt(kv[1]));
                    } catch (NumberFormatException e) {
                        log.warn("解析expires失败: {}", kv[1]);
                    }
                }
            }

            if (isBlank(tokenDTO.getAccess_token())) {
                log.error("获取token失败: 未找到access_token, 响应: {}", response);
                return null;
            }

            return tokenDTO;
        } catch (Exception e) {
            log.error("获取token失败", e);
            return null;
        }
    }

    private UserInfoDTO fetchSsoUserInfo(String accessToken) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(oauth2Config.getUserInfoUri())
                    .queryParam("access_token", accessToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            return restTemplate.getForObject(uri, UserInfoDTO.class);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return null;
        }
    }

    private NstlSessionUser resolveNstlSessionUser(String cookieHeader, NstlSessionRequest request) {
        NstlSessionUser remoteUser = fetchMainSiteSessionUser(cookieHeader);
        if (hasLookupIdentity(remoteUser)) {
            return remoteUser;
        }

        NstlSessionUser fallbackUser = normalizeFallbackRequestUser(request);
        if (hasLookupIdentity(fallbackUser)) {
            log.warn("NSTL主站登录态校验未返回有效用户信息，改用本地兜底身份建立会话 - userId: {}, unionId: {}, phone: {}, email: {}",
                    fallbackUser.userId, fallbackUser.unionId, fallbackUser.phone, fallbackUser.email);
            return fallbackUser;
        }

        if (localFallbackEnabled) {
            log.warn("NSTL unified-login identity is unavailable; using the local NSTR fallback account");
            return localFallbackUser();
        }

        return null;
    }

    private NstlSessionUser localFallbackUser() {
        return new NstlSessionUser(
                LOCAL_FALLBACK_USER_ID,
                LOCAL_FALLBACK_USER_NAME,
                null,
                LOCAL_FALLBACK_USER_ID,
                null,
                null,
                LOCAL_FALLBACK_USER_NAME,
                "本地开发环境"
        );
    }

    private boolean isLocalFallbackUser(NstlSessionUser sessionUser) {
        return localFallbackEnabled
                && sessionUser != null
                && LOCAL_FALLBACK_USER_ID.equals(sessionUser.userId)
                && LOCAL_FALLBACK_USER_ID.equals(sessionUser.unionId);
    }

    private SysUser createLocalFallbackUser() {
        SysUser fallbackUser = new SysUser();
        fallbackUser.setUserId(LOCAL_FALLBACK_USER_ID);
        fallbackUser.setNstlUnionId(LOCAL_FALLBACK_USER_ID);
        fallbackUser.setUserName(LOCAL_FALLBACK_USER_NAME);
        fallbackUser.setNickname(LOCAL_FALLBACK_USER_NAME);
        fallbackUser.setUnitName("本地开发环境");
        fallbackUser.setUserRole(1);
        fallbackUser.setStatus(1);
        fallbackUser.setDeleted(0);
        sysUserMapper.insert(fallbackUser);
        log.info("Created local NSTR fallback account - unionId: {}", LOCAL_FALLBACK_USER_ID);
        return fallbackUser;
    }

    private NstlSessionUser fetchMainSiteSessionUser(String cookieHeader) {
        if (isBlank(cookieHeader)) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.COOKIE, cookieHeader);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            headers.set("X-Requested-With", "XMLHttpRequest");

            ResponseEntity<Map> response = restTemplate.exchange(
                    MAIN_SITE_LOGIN_CHECK_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            NstlSessionUser sessionUser = normalizeMainSiteUser(response.getBody());
            if (sessionUser == null) {
                Map<?, ?> body = response.getBody();
                log.warn("NSTL主站登录态校验未返回有效用户 - code: {}, msg: {}",
                        body != null ? body.get("code") : null,
                        body != null ? body.get("msg") : null);
            }
            return sessionUser;
        } catch (Exception e) {
            log.warn("根据Cookie校验NSTL主站登录态失败: {}", e.getMessage());
            return null;
        }
    }

    private NstlSessionUser normalizeMainSiteUser(Map<?, ?> response) {
        if (response == null || !"0".equals(String.valueOf(response.get("code")))) {
            return null;
        }

        Map<?, ?> data = pickMap(response.get("data"));
        Map<?, ?> nestedUser = pickMap(data != null ? data.get("userInfo") : null);
        if (nestedUser == null) {
            nestedUser = pickMap(data != null ? data.get("user") : null);
        }
        if (nestedUser == null) {
            nestedUser = pickMap(response.get("userInfo"));
        }
        if (nestedUser == null) {
            nestedUser = pickMap(response.get("user"));
        }

        Map<?, ?> payload = nestedUser != null ? nestedUser : data;
        if (payload == null) {
            return null;
        }

        String phone = firstStringValue(payload, "phone", "mobile", "mobilePhone", "mobile_phone", "mobilephone", "tel");
        String userId = firstStringValue(payload, "userId", "user_id", "userid", "id", "uid", "loginId", "loginName", "account", "mobile", "phone");
        if (!isLookupUserId(userId) && isBlank(phone)) {
            return null;
        }

        String username = defaultIfBlank(
                firstStringValue(payload, "userName", "username", "name", "realName", "nickName", "displayName", "account", "loginName"),
                phone,
                userId
        );
        String email = firstStringValue(payload, "email", "mail");
        String unionId = firstStringValue(payload, "unionId", "unionid", "nstlUnionId", "nstl_union_id", "union_id");
        String ssoId = firstStringValue(payload, "ssoId", "sso_id", "ssoid");
        String nickname = defaultIfBlank(
                firstStringValue(payload, "nickname", "nickName", "realName", "displayName", "userName"),
                phone,
                username,
                userId
        );
        String unitName = firstStringValue(payload, "unitName", "unit", "orgName", "organizationName");

        return new NstlSessionUser(
                isLookupUserId(userId) ? userId : null,
                username,
                email,
                unionId,
                ssoId,
                phone,
                nickname,
                unitName
        );
    }

    private SysUser findLocalUser(NstlSessionUser sessionUser) {
        if (sessionUser == null) {
            return null;
        }

        if (!isBlank(sessionUser.unionId)) {
            SysUser userByUnionId = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getNstlUnionId, sessionUser.unionId)
                            .last("LIMIT 1")
            );
            if (userByUnionId != null) {
                return userByUnionId;
            }
        }

        if (isLookupUserId(sessionUser.userId)) {
            SysUser userByUserId = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUserId, sessionUser.userId)
                            .last("LIMIT 1")
            );
            if (userByUserId != null) {
                return userByUserId;
            }
        }

        if (!isBlank(sessionUser.phone)) {
            SysUser userByPhone = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getPhone, sessionUser.phone)
                            .last("LIMIT 1")
            );
            if (userByPhone != null) {
                return userByPhone;
            }
        }

        if (!isBlank(sessionUser.email)) {
            SysUser userByEmail = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, sessionUser.email)
                            .last("LIMIT 1")
            );
            if (userByEmail != null) {
                return userByEmail;
            }
        }

        return null;
    }

    private NstlSessionUser normalizeFallbackRequestUser(NstlSessionRequest request) {
        if (request == null) {
            return null;
        }

        String userId = normalizeLookupUserId(request.getUserId());
        String unionId = trimToNull(request.getNstlUnionId());
        String phone = trimToNull(request.getPhone());
        String email = trimToNull(request.getEmail());
        String username = defaultIfBlank(request.getUsername(), request.getNickname(), phone, userId, unionId, email);
        String nickname = defaultIfBlank(request.getNickname(), request.getUsername(), phone, userId, unionId, email);

        if (isBlank(userId) && isBlank(unionId) && isBlank(phone) && isBlank(email)) {
            return null;
        }

        return new NstlSessionUser(
                userId,
                username,
                email,
                unionId,
                trimToNull(request.getSsoId()),
                phone,
                nickname,
                trimToNull(request.getUnitName())
        );
    }

    private boolean hasLookupIdentity(NstlSessionUser sessionUser) {
        return sessionUser != null
                && (isLookupUserId(sessionUser.userId)
                || !isBlank(sessionUser.unionId)
                || !isBlank(sessionUser.phone)
                || !isBlank(sessionUser.email));
    }

    private Map<String, Object> buildAuthResult(SysUser sysUser) {
        String jwtToken = JwtUtil.generateToken(
                sysUser.getId().toString(),
                sysUser.getUserName(),
                sysUser.getNickname(),
                sysUser.getUserRole(),
                sysUser.getNstlUnionId()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtToken);
        result.put("userInfo", sysUser);
        result.put("expiresIn", 86400L);
        return result;
    }

    private Result<Map<String, Object>> validateAuthorizedUser(SysUser sysUser, boolean adminLogin) {
        if (sysUser == null) {
            return adminLogin
                    ? Result.error("当前账号未开通后台管理员权限，请联系管理员")
                    : Result.error("当前账号未开通接口平台访问权限，请联系管理员");
        }
        if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            return Result.error("当前账号已被禁用，请联系管理员");
        }
        if (adminLogin && (sysUser.getUserRole() == null || sysUser.getUserRole() != 1)) {
            return Result.error("当前账号没有后台管理员权限");
        }
        return null;
    }

    private void syncSsoUserProfile(SysUser sysUser, UserInfoDTO userInfo) {
        String phone = trimToNull(userInfo.getPhone());
        String ssoUserId = defaultIfBlank(userInfo.getUserName(), phone, userInfo.getSsoId(), userInfo.getUnionId());
        String nickname = defaultIfBlank(userInfo.getNickname(), phone, ssoUserId, "统一认证用户");

        sysUser.setSsoId(trimToNull(userInfo.getSsoId()));
        sysUser.setNstlUnionId(trimToNull(userInfo.getUnionId()));
        sysUser.setUserId(ssoUserId);
        sysUser.setUserName(defaultIfBlank(phone, ssoUserId, nickname));
        sysUser.setNickname(nickname);
        sysUser.setEmail(trimToNull(userInfo.getEmail()));
        sysUser.setPhone(phone);
        sysUser.setUnitName(trimToNull(userInfo.getUnitName()));
    }

    private void syncSsoUser(SysUser sysUser, UserInfoDTO userInfo) {
        syncSsoUserProfile(sysUser, userInfo);
        sysUser.setStatus(1);
        sysUser.setDeleted(0);
        if (sysUser.getUserRole() == null) {
            sysUser.setUserRole(0);
        }
    }

    private void syncNstlSessionUserProfile(SysUser sysUser, NstlSessionUser sessionUser) {
        sysUser.setSsoId(defaultIfBlank(sessionUser.ssoId, sysUser.getSsoId()));
        sysUser.setNstlUnionId(defaultIfBlank(sessionUser.unionId, sysUser.getNstlUnionId()));
        sysUser.setUserId(defaultIfBlank(normalizePersistentUserId(sessionUser.userId), sysUser.getUserId()));
        sysUser.setUserName(mergeSessionProfileValue(
                sysUser.getUserName(), sessionUser.username, sessionUser.phone, sessionUser.userId));
        sysUser.setNickname(mergeSessionProfileValue(
                sysUser.getNickname(), sessionUser.nickname, sessionUser.phone, sessionUser.userId));
        sysUser.setEmail(defaultIfBlank(sessionUser.email, sysUser.getEmail()));
        sysUser.setPhone(defaultIfBlank(sessionUser.phone, sysUser.getPhone()));
        sysUser.setUnitName(defaultIfBlank(sessionUser.unitName, sysUser.getUnitName()));
    }

    private String mergeSessionProfileValue(String storedValue, String sessionValue, String phone, String userId) {
        String stored = trimToNull(storedValue);
        String incoming = trimToNull(sessionValue);
        if (isDescriptiveProfileValue(incoming, phone, userId)) {
            return incoming;
        }
        if (isUsableStoredProfileValue(stored)) {
            return stored;
        }
        return defaultIfBlank(incoming, phone, normalizePersistentUserId(userId), "统一认证用户");
    }

    private boolean isDescriptiveProfileValue(String value, String phone, String userId) {
        if (isBlank(value) || value.equals(phone) || value.equals(userId)) {
            return false;
        }
        return !value.matches("\\d{6,}");
    }

    private boolean isUsableStoredProfileValue(String value) {
        if (isBlank(value)) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.startsWith("pending_")
                && !"待首次登录回填".equals(normalized)
                && !"统一认证用户".equals(normalized);
    }

    private String normalizePersistentUserId(String userId) {
        return isLookupUserId(userId) ? trimToNull(userId) : null;
    }

    private boolean isLookupUserId(String userId) {
        String normalized = trimToNull(userId);
        if (normalized == null) {
            return false;
        }

        String lower = normalized.toLowerCase();
        if (INVALID_USER_IDS.contains(lower)) {
            return false;
        }

        return !lower.startsWith("nstl_user_")
                && !lower.startsWith("mock_")
                && !lower.startsWith("fake_")
                && !lower.startsWith("test_");
    }

    private String normalizeLookupUserId(String userId) {
        return isLookupUserId(userId) ? trimToNull(userId) : null;
    }

    private Map<?, ?> pickMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
    }

    private String firstStringValue(Map<?, ?> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                value = findCaseInsensitiveValue(payload, key);
            }
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return ((String) value).trim();
            }
            if (value instanceof Number) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Object findCaseInsensitiveValue(Map<?, ?> payload, String key) {
        for (Map.Entry<?, ?> entry : payload.entrySet()) {
            Object entryKey = entry.getKey();
            if (entryKey != null && key.equalsIgnoreCase(String.valueOf(entryKey))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static final class NstlSessionUser {

        private final String userId;
        private final String username;
        private final String email;
        private final String unionId;
        private final String ssoId;
        private final String phone;
        private final String nickname;
        private final String unitName;

        private NstlSessionUser(
                String userId,
                String username,
                String email,
                String unionId,
                String ssoId,
                String phone,
                String nickname,
                String unitName
        ) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.unionId = unionId;
            this.ssoId = ssoId;
            this.phone = phone;
            this.nickname = nickname;
            this.unitName = unitName;
        }
    }
}
