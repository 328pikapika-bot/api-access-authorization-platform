package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.SandboxLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 沙箱请求日志 Mapper 接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface SandboxLogMapper extends BaseMapper<SandboxLog> {
}
