package com.gyc.auth.controller;

import com.gyc.auth.service.SysMenuService;
import com.gyc.auth.service.SysUserService;
import com.gyc.common.config.exception.GuiguException;
import com.gyc.common.jwt.JwtHelper;
import com.gyc.common.result.Result;
import com.gyc.common.utils.MD5;
import com.gyc.model.system.SysUser;
import com.gyc.vo.system.LoginVo;
import com.gyc.vo.system.RouterVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 后台登录登出
 * </p>
 */
@Api(tags = "后台登录管理")
@RestController
@RequestMapping("/admin/system/index")
public class IndexController {
    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    //loginIn 登录
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("token","admin");
//        return Result.ok(map);
        //获取输入的用户名和密码
        String username = loginVo.getUsername();
        //根据用户名查询数据库
        SysUser sysUser = sysUserService.getByUsername(username);
        //用户信息是否存在
        if(null == sysUser) {
            throw new GuiguException(201,"用户不存在");
        }
        //判断密码是否正确
        //数据库存储密码(MD5加密)
        String password_db = sysUser.getPassword();
        //获取输入的密码并进行MD5加密
        String password_input = MD5.encrypt(loginVo.getPassword());
        if(!(password_db.equals(password_input))) {
            throw new GuiguException(201,"密码错误");
        }
        //判断用户是否被禁用: 1表示可用 , 0表示禁用
        if(sysUser.getStatus().intValue() == 0) {
            throw new GuiguException(201,"用户被禁用");
        }
        //使用jwt根据用户id和用户名称生成token字符串
        Map<String, Object> map = new HashMap<>();
        map.put("token", JwtHelper.createToken(sysUser.getId(), sysUser.getUsername()));
        //返回
        return Result.ok(map);
    }

    //Info  获取用户信息
    @GetMapping("info")
    public Result info(HttpServletRequest request) {
        //1、从请求头获取用户信息(就是获取请求头token字符串)
        String token = request.getHeader("token");
        //2、从token字符串获取用户id或者用户名称
        Long userId = JwtHelper.getUserId(token);
        //3、根据用户id查询数据库，把用户信息获取出来
        SysUser sysUser = sysUserService.getById(userId);
        //4、根据用户id获取可操作菜单列表
        //查询数据库，动态构建路由结构，进行显示
        List<RouterVo> routerList = sysMenuService.findUserMenuList(userId);
        //5、根据用户id获取用户可操作按钮列表
        List<String> permsList = sysMenuService.findUserPermsList(userId);
        //6、返回相应数据
        Map<String, Object> map = new HashMap<>();
        map.put("roles","[admin]");
        map.put("name",sysUser.getName());
        map.put("avatar","https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
        //返回用户可操作按钮
        map.put("buttons",permsList);
        //返回用户可操作菜单
        map.put("routers",routerList);
        return Result.ok(map);
    }

    //logOut  退出
    @PostMapping("logout")
    public Result logout(){
        return Result.ok();
    }
}
