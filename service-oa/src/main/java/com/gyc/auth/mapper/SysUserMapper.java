package com.gyc.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.model.system.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-02
 */
@Repository
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

}
