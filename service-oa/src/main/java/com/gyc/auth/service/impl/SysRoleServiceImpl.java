package com.gyc.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.auth.mapper.SysRoleMapper;
import com.gyc.auth.mapper.SysUserRoleMapper;
import com.gyc.auth.service.SysRoleService;
import com.gyc.model.system.SysRole;
import com.gyc.model.system.SysUserRole;
import com.gyc.vo.system.AssginRoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    // 查询所有角色和当前用户所属角色
    @Override
    public Map<String, Object> findRoleByAdminId(Long userId) {
        //查询所有的角色，返回list集合
        List<SysRole> allRolesList = this.list();

        //根据userid查询 角色用户关系表，查询userid 对应拥有的角色id
        List<SysUserRole> existUserRoleList = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId).select(SysUserRole::getRoleId));
        //从查询出来的用户id对应角色的list集合，获取所有角色id
        List<Long> existRoleIdList = existUserRoleList.stream().map(c->c.getRoleId()).collect(Collectors.toList());

        //对角色进行分类，根据查询所有角色id，找到对应角色信息
        //根据角色id，到所有的角色的list集合进行比较
        List<SysRole> assginRoleList = new ArrayList<>();
        for (SysRole role : allRolesList) {
            //比较，相同就取对象
            if(existRoleIdList.contains(role.getId())) {
                assginRoleList.add(role);
            }
        }

        //把得到的两部分数据封装map集合，返回
        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assginRoleList);
        roleMap.put("allRolesList", allRolesList);
        return roleMap;
    }

    //为用户分配角色
    @Transactional
    @Override
    public void doAssign(AssginRoleVo assginRoleVo) {
        //把用户之前分配角色数据删除，用户角色关系表里面，根据userid删除
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, assginRoleVo.getUserId()));
        //重新进行分配
        for(Long roleId : assginRoleVo.getRoleIdList()) {
            if(StringUtils.isEmpty(roleId)) continue;
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(assginRoleVo.getUserId());
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }
}
