package com.nstl.openplatform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.config.OAuth2Config;
import com.nstl.openplatform.dto.NstlSessionRequest;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.mapper.SysUserMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AuthControllerAuthorizationTest {

    private static final String LOGIN_CHECK_URL =
            "https://www.nstl.gov.cn/api/service/nstl/web/vilidate/isLogin";

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private RestTemplate restTemplate;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(new OAuth2Config(), sysUserMapper, restTemplate);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", "nstl-user-1");
        user.put("unionId", "union-1");
        user.put("phone", "1734981104");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("data", user);

        lenient().when(restTemplate.exchange(
                eq(LOGIN_CHECK_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    }

    @Test
    void portalRejectsUserMissingFromUnionIdAllowlist() {
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Result<Map<String, Object>> result = controller.createNstlSession(request("portal"), "NSTL_SESSION=test");

        assertEquals(500, result.getCode());
        assertEquals("当前账号未开通接口平台访问权限，请联系管理员", result.getMessage());
    }

    @Test
    void adminRejectsUserMissingFromUnionIdAllowlist() {
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Result<Map<String, Object>> result = controller.createNstlSession(request("admin"), "NSTL_SESSION=test");

        assertEquals(500, result.getCode());
        assertEquals("当前账号未开通后台管理员权限，请联系管理员", result.getMessage());
    }

    @Test
    void adminRejectsPreauthorizedRegularUser() {
        SysUser user = new SysUser();
        user.setStatus(1);
        user.setUserRole(0);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        Result<Map<String, Object>> result = controller.createNstlSession(request("admin"), "NSTL_SESSION=test");

        assertEquals(500, result.getCode());
        assertEquals("当前账号没有后台管理员权限", result.getMessage());
    }

    @Test
    void adminUsesTrustedMainSiteUserIdWhenIsLoginOmitsUnionId() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", "nstl-user-1");
        userInfo.put("mobilePhone", "17334981104");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("data", userInfo);
        when(restTemplate.exchange(
                eq(LOGIN_CHECK_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        SysUser authorizedUser = new SysUser();
        authorizedUser.setId(14L);
        authorizedUser.setUserId("nstl-user-1");
        authorizedUser.setUserName("王瑞");
        authorizedUser.setNickname("王瑞");
        authorizedUser.setNstlUnionId("union-1");
        authorizedUser.setUserRole(1);
        authorizedUser.setStatus(1);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(authorizedUser);

        Result<Map<String, Object>> result = controller.createNstlSession(request("admin"), "SESSION=test");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("union-1", authorizedUser.getNstlUnionId());
        assertEquals("王瑞", authorizedUser.getUserName());
        assertEquals("王瑞", authorizedUser.getNickname());
        assertEquals("17334981104", authorizedUser.getPhone());
    }

    @Test
    void localFallbackCreatesNstrAdminWhenUnifiedLoginIdentityIsUnavailable() {
        ReflectionTestUtils.setField(controller, "localFallbackEnabled", true);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        });

        Result<Map<String, Object>> result = controller.createNstlSession(request("admin"), null);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        SysUser user = (SysUser) result.getData().get("userInfo");
        assertEquals("NSTR 用户", user.getUserName());
        assertEquals("local-nstr-user", user.getNstlUnionId());
        assertEquals(1, user.getUserRole());
    }

    @Test
    void identityFreeRequestIsRejectedWhenLocalFallbackIsDisabled() {
        Result<Map<String, Object>> result = controller.createNstlSession(request("portal"), null);

        assertEquals(500, result.getCode());
    }

    private NstlSessionRequest request(String loginType) {
        NstlSessionRequest request = new NstlSessionRequest();
        request.setLoginType(loginType);
        return request;
    }
}
