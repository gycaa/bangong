package com.gyc.auth.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyc.auth.mapper.SysUserMapper;
import com.gyc.auth.service.SysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.model.system.SysUser;
import com.gyc.security.custom.LoginUserInfoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-02
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

//    @Autowired
//    private SysDeptService sysDeptService;
//
//    @Autowired
//    private SysPostService sysPostService;

    @Autowired
    private SysUserMapper sysUserMapper;

    //更新状态
    @Transactional
    @Override
    public void updateStatus(Long id, Integer status) {
        //根据id查询用户对象
        SysUser sysUser = this.getById(id);
        //设置修改状态值
        if(status.intValue() == 1) {
            sysUser.setStatus(status);
        } else {
            sysUser.setStatus(0);
        }
        //调用方法进行修改
        this.updateById(sysUser);
    }

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    //获取当前用户基本信息
    @Override
    public Map<String, Object> getCurrentUser() {
        //查询
        SysUser sysUser = sysUserMapper.selectById(LoginUserInfoHelper.getUserId());
        //SysDept sysDept = sysDeptService.getById(sysUser.getDeptId());
        //SysPost sysPost = sysPostService.getById(sysUser.getPostId());
        Map<String, Object> map = new HashMap<>();
        map.put("name", sysUser.getName());
        map.put("phone", sysUser.getPhone());
        //map.put("deptName", sysDept.getName());
        //map.put("postName", sysPost.getName());
        return map;
    }
}
