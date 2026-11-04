package com.nstl.openplatform.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.service.SysUserService;
import com.nstl.openplatform.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private SysUserService sysUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletedOrMissingUserCannotReuseExistingToken() throws Exception {
        String token = JwtUtil.generateToken("8", "old-admin", "旧管理员", 1, "union-8");
        when(sysUserService.getById(8L)).thenReturn(null);

        executeFilter(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void currentDatabaseRoleOverridesRoleStoredInToken() throws Exception {
        String token = JwtUtil.generateToken("8", "old-admin", "旧管理员", 1, "union-8");
        SysUser currentUser = new SysUser();
        currentUser.setId(8L);
        currentUser.setUserName("regular-user");
        currentUser.setNickname("普通用户");
        currentUser.setNstlUnionId("union-8");
        currentUser.setUserRole(0);
        currentUser.setStatus(1);
        when(sysUserService.getById(8L)).thenReturn(currentUser);

        executeFilter(token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    private void executeFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(sysUserService).doFilter(request, response, new MockFilterChain());
    }
}
