package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.InterfaceConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 接口配置 Mapper
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface InterfaceConfigMapper extends BaseMapper<InterfaceConfig> {

    /**
     * 根据分类ID查询接口配置（包含关联的Mockoon服务）
     *
     * @param categoryId 分类ID
     * @return 接口配置
     */
    @Select("SELECT ic.*, ms.name as mockoon_service_name, ms.service_url as mockoon_service_url " +
            "FROM interface_config ic " +
            "LEFT JOIN mockoon_service ms ON ic.mockoon_service_id = ms.id " +
            "WHERE ic.category_id = #{categoryId}")
    InterfaceConfig selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 查询接口配置及其关联的Mockoon服务
     *
     * @param id 配置ID
     * @return 接口配置
     */
    @Select("SELECT ic.*, ms.name as mockoon_service_name, ms.service_url as mockoon_service_url " +
            "FROM interface_config ic " +
            "LEFT JOIN mockoon_service ms ON ic.mockoon_service_id = ms.id " +
            "WHERE ic.id = #{id}")
    InterfaceConfig selectWithServiceById(@Param("id") Long id);
}
