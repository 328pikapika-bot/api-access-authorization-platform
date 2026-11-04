package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.ContentLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容访问日志Mapper
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface ContentLogMapper extends BaseMapper<ContentLog> {
}
