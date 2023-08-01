package com.gyc.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.model.system.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

}
