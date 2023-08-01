package com.gyc.auth.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gyc.model.system.SysUser;

import java.util.Map;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-02
 */
public interface SysUserService extends IService<SysUser> {

    //更新状态
    void updateStatus(Long id, Integer status);

    SysUser getByUsername(String username);


    //获取当前用户基本信息
    Map<String, Object> getCurrentUser();
}
