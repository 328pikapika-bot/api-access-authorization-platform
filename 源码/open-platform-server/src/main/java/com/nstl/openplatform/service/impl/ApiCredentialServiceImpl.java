package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.ApiCredential;
import com.nstl.openplatform.mapper.ApiCredentialMapper;
import com.nstl.openplatform.service.IApiCredentialService;
import org.springframework.stereotype.Service;

/**
 * 接口凭证分配 Service 实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
public class ApiCredentialServiceImpl extends ServiceImpl<ApiCredentialMapper, ApiCredential> implements IApiCredentialService {
}
