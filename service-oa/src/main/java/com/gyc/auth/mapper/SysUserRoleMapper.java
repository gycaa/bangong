package com.gyc.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.model.system.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 用户角色 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-02
 */
@Repository
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

}
