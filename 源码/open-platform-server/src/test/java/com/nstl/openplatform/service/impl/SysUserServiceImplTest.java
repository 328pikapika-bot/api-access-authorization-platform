package com.nstl.openplatform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.mapper.SysUserMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    private static final String USER_INFO_URL =
            "https://api.nstl.gov.cn/userManager/getUserInfoByUnionId";
    private static final String UNION_ID = "8k5c5gpy0qkcrej6dtikub62b0kr2qie";

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private RestTemplate restTemplate;

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl(sysUserMapper, restTemplate, USER_INFO_URL);
    }

    @Test
    void previewValidatesUnionIdAndBackfillsNstlProfile() {
        when(sysUserMapper.selectIncludingDeletedByUnionId(UNION_ID)).thenReturn(null);
        when(restTemplate.getForObject(any(URI.class), eq(Map.class))).thenReturn(successResponse());

        SysUser user = service.validateAndPrepareUser(" " + UNION_ID + " ", 1, "INTERNAL");

        assertEquals(UNION_ID, user.getNstlUnionId());
        assertEquals("ceshi_hyx", user.getUserId());
        assertEquals("侯月侠", user.getUserName());
        assertEquals("侯月侠", user.getNickname());
        assertEquals("18731655429", user.getPhone());
        assertEquals("921019712@qq.com", user.getEmail());
        assertEquals("中国科学技术信息研究所2", user.getUnitName());
        assertEquals("CN000000", user.getUserLibCode());
        assertEquals("中心站", user.getUserLibName());
        assertEquals("西部用户", user.getUserLibType());
        assertEquals("INTERNAL", user.getUserType());
        assertEquals(1, user.getUserRole());
        assertEquals(1, user.getStatus());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(Map.class));
        assertTrue(uriCaptor.getValue().toString().contains("nstlUnionId=" + UNION_ID));
    }

    @Test
    void missingUnionIdReturnsFriendlyMessage() {
        when(sysUserMapper.selectIncludingDeletedByUnionId("not-exists")).thenReturn(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 1);
        response.put("msg", "没有当前用户信息");
        response.put("count", 0);
        response.put("data", null);
        response.put("lcode", 1);
        when(restTemplate.getForObject(any(URI.class), eq(Map.class))).thenReturn(response);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.validateAndPrepareUser("not-exists", 0, "INTERNAL"));

        assertEquals("未查询到该NSTL联合ID对应的用户，请确认后重试", error.getMessage());
    }

    @Test
    void unavailableNstlUserCannotBeAdded() {
        when(sysUserMapper.selectIncludingDeletedByUnionId(UNION_ID)).thenReturn(null);
        Map<String, Object> response = successResponse();
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) response.get("data");
        profile.put("userStatus", "停用");
        when(restTemplate.getForObject(any(URI.class), eq(Map.class))).thenReturn(response);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.validateAndPrepareUser(UNION_ID, 0, "INTERNAL"));

        assertEquals("该NSTL用户当前状态为“停用”，无法添加", error.getMessage());
    }

    @Test
    void addPersistsVerifiedProfileInsteadOfPendingUser() {
        when(sysUserMapper.selectIncludingDeletedByUnionId(UNION_ID)).thenReturn(null);
        when(restTemplate.getForObject(any(URI.class), eq(Map.class))).thenReturn(successResponse());
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser request = new SysUser();
        request.setNstlUnionId(UNION_ID);
        request.setUserRole(0);
        request.setUserType("INTERNAL");

        assertTrue(service.addUser(request));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("ceshi_hyx", saved.getUserId());
        assertEquals("侯月侠", saved.getUserName());
        assertEquals("侯月侠", saved.getNickname());
    }

    @Test
    void activeUnionIdCannotBeAddedTwiceWithoutCallingNstlApi() {
        SysUser existing = new SysUser();
        existing.setDeleted(0);
        when(sysUserMapper.selectIncludingDeletedByUnionId("union-existing")).thenReturn(existing);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.validateAndPrepareUser("union-existing", 0, "INTERNAL"));

        assertEquals("该NSTL联合ID已在权限管理中存在，无需重复添加", error.getMessage());
        verifyNoInteractions(restTemplate);
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    private Map<String, Object> successResponse() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userStatus", "可用");
        profile.put("nstlUnionId", UNION_ID);
        profile.put("userName", "侯月侠");
        profile.put("userId", "ceshi_hyx");
        profile.put("userIdLogin", "test2025001");
        profile.put("mobilePhone", "18731655429");
        profile.put("email", "921019712@qq.com");
        profile.put("workUnitName", "中国科学技术信息研究所2");
        profile.put("userLibCode", "CN000000");
        profile.put("userLibName", "中心站");
        profile.put("userType", "西部用户");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", "");
        response.put("count", 0);
        response.put("data", profile);
        response.put("lcode", 1);
        return response;
    }
}
