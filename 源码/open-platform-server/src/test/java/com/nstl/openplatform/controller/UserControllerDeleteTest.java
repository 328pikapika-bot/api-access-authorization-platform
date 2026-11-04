package com.nstl.openplatform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.ApplicationService;
import com.nstl.openplatform.service.SysUserService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserControllerDeleteTest {

    @Mock
    private SysUserService sysUserService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private Authentication authentication;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(sysUserService, applicationService);
    }

    @Test
    void userWithPermissionAssignmentsCannotBeDeleted() {
        SysUser user = new SysUser();
        user.setId(8L);
        when(sysUserService.getById(8L)).thenReturn(user);
        when(applicationService.count(any(Wrapper.class))).thenReturn(1L);

        Result<Boolean> result = controller.deleteUser(8L, null);

        assertEquals(500, result.getCode());
        assertEquals("该用户仍有接口权限分配，请先在“接口权限分配”中删除对应记录", result.getMessage());
        verify(sysUserService, never()).removeById(8L);
    }

    @Test
    void unreferencedUserIsLogicallyDeleted() {
        SysUser user = new SysUser();
        user.setId(8L);
        when(sysUserService.getById(8L)).thenReturn(user);
        when(applicationService.count(any(Wrapper.class))).thenReturn(0L);
        when(sysUserService.removeById(8L)).thenReturn(true);

        Result<Boolean> result = controller.deleteUser(8L, null);

        assertEquals(200, result.getCode());
        assertTrue(result.getData());
        verify(sysUserService).removeById(8L);
    }

    @Test
    void currentAdministratorCannotDeleteOwnAccount() {
        SysUser user = new SysUser();
        user.setId(8L);
        when(sysUserService.getById(8L)).thenReturn(user);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(Map.of("userId", 8L));

        Result<Boolean> result = controller.deleteUser(8L, authentication);

        assertEquals(500, result.getCode());
        assertEquals("不能删除当前登录账号", result.getMessage());
        verify(applicationService, never()).count(any(Wrapper.class));
        verify(sysUserService, never()).removeById(8L);
    }
}
