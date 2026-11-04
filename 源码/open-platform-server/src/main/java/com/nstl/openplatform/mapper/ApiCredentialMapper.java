package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.ApiCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口凭证分配 Mapper 接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface ApiCredentialMapper extends BaseMapper<ApiCredential> {
}
