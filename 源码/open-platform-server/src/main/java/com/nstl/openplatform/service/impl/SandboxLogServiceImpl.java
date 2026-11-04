package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.SandboxLog;
import com.nstl.openplatform.mapper.SandboxLogMapper;
import com.nstl.openplatform.service.SandboxLogService;
import org.springframework.stereotype.Service;

/**
 * 沙箱请求日志服务实现类
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Service
public class SandboxLogServiceImpl extends ServiceImpl<SandboxLogMapper, SandboxLog> implements SandboxLogService {
}
