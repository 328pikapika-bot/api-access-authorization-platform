package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.mapper.PlatformColumnMapper;
import com.nstl.openplatform.service.PlatformColumnService;
import org.springframework.stereotype.Service;

/**
 * 栏目 服务实现类
 */
@Service
public class PlatformColumnServiceImpl extends ServiceImpl<PlatformColumnMapper, PlatformColumn> implements PlatformColumnService {
}
