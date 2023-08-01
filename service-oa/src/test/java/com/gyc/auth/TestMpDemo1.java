package com.gyc.auth;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gyc.auth.mapper.SysRoleMapper;
import com.gyc.model.system.SysRole;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class TestMpDemo1 {

    //注入
    @Autowired
    private SysRoleMapper mapper;

    //查询所有记录
    @Test
    public void getAll(){
        List<SysRole> users = mapper.selectList(null);
        users.forEach(System.out::println);
    }

    //添加操作
    @Test
    public void testInsert(){
        SysRole sysRole = new SysRole();
        sysRole.setRoleName("角色管理员");
        sysRole.setRoleCode("role");
        sysRole.setDescription("角色管理员");

        int result = mapper.insert(sysRole);
        System.out.println(result); //影响的行数
        System.out.println(sysRole); //id自动回填
    }

    //修改操作
    @Test
    public void testUpdateById(){
       //根据id查询
        SysRole sysRole = mapper.selectById(9);
       //设置修改值
        sysRole.setRoleName("角色管理员1");

        //调用方法修改
        int result = mapper.updateById(sysRole);
        System.out.println(result);
    }

    //根据id删除
    @Test
    public void testDeleteById(){
        int result = mapper.deleteById(9);
        System.out.println(result);
    }

    //批量删除
    @Test
    public void testDeleteBatchIds() {
        int result = mapper.deleteBatchIds(Arrays.asList(1, 2));
        System.out.println(result);
    }

    //条件查询
    @Test
    public void testQuery1(){
        //创建QueryWrapper对象，调用方法封装条件
        QueryWrapper<SysRole> wrapper = new QueryWrapper<>();
        wrapper.eq("role_name", "系统管理员");
        //调用mp方法实现查询操作
        List<SysRole> list = mapper.selectList(wrapper);
        System.out.println(list);
    }

    //条件查询
    @Test
    public void testQuery2(){
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getRoleCode, "COMMON");
        List<SysRole> users = mapper.selectList(queryWrapper);
        System.out.println(users);


    }
}
