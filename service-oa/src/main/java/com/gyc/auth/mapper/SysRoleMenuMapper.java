package com.gyc.auth.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.model.system.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 角色菜单 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-04
 */
@Repository
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

}
