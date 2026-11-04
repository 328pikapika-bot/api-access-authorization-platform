package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.SysUser;
import com.nstl.openplatform.mapper.SysUserMapper;
import com.nstl.openplatform.service.SysUserService;
import com.nstl.openplatform.util.DateTimeRangeUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 用户服务实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final RestTemplate restTemplate;
    private final String userInfoByUnionIdUrl;

    public SysUserServiceImpl(SysUserMapper sysUserMapper,
                              RestTemplate restTemplate,
                              @Value("${nstl.user-info-by-union-id-url:https://api.nstl.gov.cn/userManager/getUserInfoByUnionId}")
                              String userInfoByUnionIdUrl) {
        this.sysUserMapper = sysUserMapper;
        this.restTemplate = restTemplate;
        this.userInfoByUnionIdUrl = userInfoByUnionIdUrl;
    }

    @Override
    public Page<SysUser> getUserPage(Integer current, Integer size, Map<String, Object> params) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (params.containsKey("userName")) {
            queryWrapper.like(SysUser::getUserName, params.get("userName"));
        }
        if (params.containsKey("email")) {
            queryWrapper.like(SysUser::getEmail, params.get("email"));
        }
        if (params.containsKey("userType")) {
            queryWrapper.eq(SysUser::getUserType, params.get("userType"));
        }
        if (params.containsKey("startTime")) {
            LocalDateTime startTime = DateTimeRangeUtil.normalizeStartTime(params.get("startTime"));
            if (startTime != null) {
                queryWrapper.ge(SysUser::getCreateTime, startTime);
            }
        }
        if (params.containsKey("endTime")) {
            LocalDateTime endTime = DateTimeRangeUtil.normalizeEndTime(params.get("endTime"));
            if (endTime != null) {
                queryWrapper.le(SysUser::getCreateTime, endTime);
            }
        }

        queryWrapper.orderByDesc(SysUser::getCreateTime);
        return this.page(page, queryWrapper);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        return this.updateById(user);
    }

    @Override
    public boolean updateRole(Long id, Integer role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUserRole(role);
        return this.updateById(user);
    }

    @Override
    @Transactional
    public boolean addUser(SysUser user) {
        SysUser preparedUser = validateAndPrepareUser(user.getNstlUnionId(), user.getUserRole(), user.getUserType());
        if (preparedUser.getId() != null) {
            boolean restored = sysUserMapper.restoreDeletedUser(
                    preparedUser.getId(),
                    preparedUser.getStatus(),
                    preparedUser.getUserRole(),
                    preparedUser.getUserType()
            ) > 0;
            return restored && sysUserMapper.updateById(preparedUser) > 0;
        }
        return sysUserMapper.insert(preparedUser) > 0;
    }

    @Override
    public SysUser validateAndPrepareUser(String nstlUnionId, Integer userRole, String userType) {
        String normalizedUnionId = trimToNull(nstlUnionId);
        if (!StringUtils.hasText(normalizedUnionId)) {
            throw new RuntimeException("NSTL联合ID不能为空");
        }

        SysUser existingUser = sysUserMapper.selectIncludingDeletedByUnionId(normalizedUnionId);
        if (existingUser != null && (existingUser.getDeleted() == null || existingUser.getDeleted() == 0)) {
            throw new RuntimeException("该NSTL联合ID已在权限管理中存在，无需重复添加");
        }

        Map<String, Object> nstlProfile = fetchNstlUserProfile(normalizedUnionId);
        SysUser newUser = mapNstlProfile(nstlProfile, normalizedUnionId);
        if (existingUser != null) {
            newUser.setId(existingUser.getDeleted() != null && existingUser.getDeleted() == 1 ? existingUser.getId() : null);
            newUser.setSsoId(defaultIfBlank(newUser.getSsoId(), existingUser.getSsoId()));
        }
        newUser.setUserType(StringUtils.hasText(userType)
                ? userType.trim()
                : defaultIfBlank(existingUser != null ? existingUser.getUserType() : null, "INTERNAL"));
        newUser.setStatus(1);
        newUser.setDeleted(0);
        newUser.setUserRole(userRole != null ? userRole : 0);
        return newUser;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchNstlUserProfile(String nstlUnionId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(userInfoByUnionIdUrl)
                .queryParam("nstlUnionId", nstlUnionId)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        final Map<String, Object> response;
        try {
            response = restTemplate.getForObject(uri, Map.class);
        } catch (RestClientException exception) {
            throw new RuntimeException("NSTL用户信息服务暂时不可用，请稍后重试");
        }

        if (response == null) {
            throw new RuntimeException("NSTL用户信息服务返回异常，请稍后重试");
        }
        if (toInteger(response.get("code")) != 0 || !(response.get("data") instanceof Map<?, ?>)) {
            String message = trimToNull(stringValue(response.get("msg")));
            if (message != null && message.contains("没有当前用户信息")) {
                throw new RuntimeException("未查询到该NSTL联合ID对应的用户，请确认后重试");
            }
            throw new RuntimeException(defaultIfBlank(message, "NSTL联合ID校验失败，请稍后重试"));
        }
        return (Map<String, Object>) response.get("data");
    }

    private SysUser mapNstlProfile(Map<String, Object> profile, String requestedUnionId) {
        String returnedUnionId = trimToNull(stringValue(profile.get("nstlUnionId")));
        if (!requestedUnionId.equals(returnedUnionId)) {
            throw new RuntimeException("NSTL用户信息校验失败，返回的联合ID不一致");
        }

        String userStatus = trimToNull(stringValue(profile.get("userStatus")));
        if (userStatus != null && !"可用".equals(userStatus)) {
            throw new RuntimeException("该NSTL用户当前状态为“" + userStatus + "”，无法添加");
        }

        String userId = trimToNull(stringValue(profile.get("userId")));
        String displayName = trimToNull(stringValue(profile.get("userName")));
        String userName = defaultIfBlank(
                displayName,
                stringValue(profile.get("userIdLogin")),
                userId
        );
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(userName)) {
            throw new RuntimeException("NSTL用户信息不完整，缺少用户ID或用户名");
        }

        SysUser user = new SysUser();
        user.setNstlUnionId(returnedUnionId);
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickname(defaultIfBlank(displayName, userName));
        user.setEmail(trimToNull(stringValue(profile.get("email"))));
        user.setPhone(trimToNull(stringValue(profile.get("mobilePhone"))));
        user.setUnitName(defaultIfBlank(
                stringValue(profile.get("workUnitName")),
                stringValue(profile.get("address"))
        ));
        user.setUserLibCode(trimToNull(stringValue(profile.get("userLibCode"))));
        user.setUserLibName(trimToNull(stringValue(profile.get("userLibName"))));
        user.setUserLibType(trimToNull(stringValue(profile.get("userType"))));
        return user;
    }

    private int toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
