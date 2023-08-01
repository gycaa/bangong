package com.gyc.auth.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.model.system.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 菜单表 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-04
 */
@Repository
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    //多表关联查询：用户角色关系表、角色菜单关系表、菜单表
    List<SysMenu> findListByUserId(@Param("userId") Long userId);
}
