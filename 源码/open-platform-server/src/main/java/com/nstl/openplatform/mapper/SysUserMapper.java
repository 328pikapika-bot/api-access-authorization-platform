package com.nstl.openplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nstl.openplatform.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE nstl_union_id = #{nstlUnionId} LIMIT 1")
    SysUser selectIncludingDeletedByUnionId(@Param("nstlUnionId") String nstlUnionId);

    @Update("UPDATE sys_user SET deleted = 0, status = #{status}, user_role = #{userRole}, user_type = #{userType}, update_time = NOW() WHERE id = #{id}")
    int restoreDeletedUser(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("userRole") Integer userRole,
            @Param("userType") String userType
    );
}
