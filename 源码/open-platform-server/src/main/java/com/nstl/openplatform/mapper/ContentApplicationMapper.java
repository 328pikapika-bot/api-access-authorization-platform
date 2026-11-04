package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.ContentApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 内容申请记录Mapper
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface ContentApplicationMapper extends BaseMapper<ContentApplication> {

    /**
     * 根据 AppKey 查询申请记录
     *
     * @param appKey API Key
     * @return 申请记录，未找到返回 null
     */
    @Select("SELECT * FROM content_application WHERE app_key = #{appKey} AND status = 1 AND deleted = 0")
    ContentApplication selectByAppKey(@Param("appKey") String appKey);
}
