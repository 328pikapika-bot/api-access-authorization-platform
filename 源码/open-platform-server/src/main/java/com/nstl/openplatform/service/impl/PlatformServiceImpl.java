package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.mapper.PlatformMapper;
import com.nstl.openplatform.service.PlatformService;
import org.springframework.stereotype.Service;

/**
 * 系统平台 服务实现类
 */
@Service
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {
}
