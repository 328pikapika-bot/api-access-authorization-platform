package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.MockoonService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mockoon 服务管理 Mapper
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface MockoonServiceMapper extends BaseMapper<MockoonService> {

    /**
     * 查询所有启用的服务（用于下拉选择）
     *
     * @return 服务列表
     */
    @Select("SELECT * FROM mockoon_service WHERE status = 1 ORDER BY sort ASC, id ASC")
    List<MockoonService> selectActiveList();
}
