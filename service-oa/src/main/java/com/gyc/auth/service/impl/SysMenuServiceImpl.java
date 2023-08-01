package com.gyc.auth.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.gyc.auth.mapper.SysMenuMapper;
import com.gyc.auth.mapper.SysRoleMenuMapper;
import com.gyc.auth.service.SysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.auth.utils.MenuHelper;
import com.gyc.common.config.exception.GuiguException;
import com.gyc.model.system.SysMenu;
import com.gyc.model.system.SysRoleMenu;
import com.gyc.vo.system.AssginMenuVo;
import com.gyc.vo.system.MetaVo;
import com.gyc.vo.system.RouterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-04
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Override
    public List<SysMenu> findNodes() {
        //查询所有菜单数据
        List<SysMenu> sysMenuList = baseMapper.selectList(null);

        //构建树形数据
        List<SysMenu> result = MenuHelper.buildTree(sysMenuList);
        return result;
    }

    //查询所有菜单和角色分配的菜单
    @Override
    public List<SysMenu> findSysMenuByRoleId(Long roleId) {
        //查询所有菜单 添加条件 status=1
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus,1);
        List<SysMenu> allSysMenuList = baseMapper.selectList(wrapper);
        // 根据角色id roleId查询 角色菜单关系表里面角色id对应所有的菜单id
        List<SysRoleMenu> sysRoleMenuList = sysRoleMenuMapper.selectList
                (new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        // 根据获取菜单id,获取对应菜单对象
        List<Long> menuIdList = sysRoleMenuList.stream().map(e -> e.getMenuId()).collect(Collectors.toList());
        // 拿着菜单id和所有菜单集合里面的id比较，相同就封装
        allSysMenuList.forEach(permission -> {
            if (menuIdList.contains(permission.getId())) {
                permission.setSelect(true);
            } else {
                permission.setSelect(false);
            }
        });
        //返回规定树形显示格式菜单列表
        List<SysMenu> sysMenuList = MenuHelper.buildTree(allSysMenuList);
        return sysMenuList;
    }

    //角色分配菜单
    @Transactional
    @Override
    public void doAssign(AssginMenuVo assginMenuVo) {
        //根据角色id删除菜单角色表 分配数据
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, assginMenuVo.getRoleId());
        sysRoleMenuMapper.delete(wrapper);
        //从参数里获取角色新分配菜单id列表
        //进行遍历，把每个id数据添加菜单角色表
        for (Long menuId : assginMenuVo.getMenuIdList()) {
            if (StringUtils.isEmpty(menuId)) continue;
            SysRoleMenu rolePermission = new SysRoleMenu();
            rolePermission.setRoleId(assginMenuVo.getRoleId());
            rolePermission.setMenuId(menuId);
            sysRoleMenuMapper.insert(rolePermission);
        }
    }

    /**
     * 根据用户id获取用户可操作菜单列表
     * @param userId
     * @return
     */
    @Override
    public List<RouterVo> findUserMenuList(Long userId) {
        //1、判断当前用户是否是管理员,userId=1 就是管理员,超级管理员admin账号id为：1
        //1.1、如果是管理员，查询所有菜单列表
        List<SysMenu> sysMenuList = null;
        if (userId.longValue() == 1) {
            //查询所有菜单列表
            sysMenuList = this.list(new LambdaQueryWrapper<SysMenu>()
                               .eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSortValue));//orderByAsc：升序排序
        } else {
            //1.2、如果不是管理员，根据userId查询可操作菜单列表
            //多表关联查询：用户角色关系表、角色菜单关系表、菜单表
            sysMenuList = sysMenuMapper.findListByUserId(userId);
        }
        //2、把查询出来的数据列表构建成框架要求的路由数据结构
        //使用菜单操作工具类构建树形结构
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        //构建成框架要求的路由结构
        List<RouterVo> routerVoList = this.buildMenus(sysMenuTreeList);
        return routerVoList;

    }

    /**
     * 根据菜单构建路由(构建成框架要求的路由结构)
     * @param menus
     * @return
     */
    private List<RouterVo> buildMenus(List<SysMenu> menus) {
        //创建list结合，存储最终数据
        List<RouterVo> routers = new LinkedList<RouterVo>();
        //menus遍历
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden(false);
            router.setAlwaysShow(false);
            router.setPath(getRouterPath(menu));
            router.setComponent(menu.getComponent());
            router.setMeta(new MetaVo(menu.getName(), menu.getIcon()));
            //下一层数据部分
            List<SysMenu> children = menu.getChildren();
            //如果当前是菜单，需将按钮对应的路由加载出来，如：“角色授权”按钮对应的路由在“系统管理”下面
            if(menu.getType().intValue() == 1) {
                //加载下面隐藏路由
                List<SysMenu> hiddenMenuList = children.stream()
                                                       .filter(item -> !StringUtils.isEmpty(item.getComponent()))
                                                       .collect(Collectors.toList());
                for (SysMenu hiddenMenu : hiddenMenuList) {
                    RouterVo hiddenRouter = new RouterVo();
                    //true表示为隐藏路由
                    hiddenRouter.setHidden(true);
                    hiddenRouter.setAlwaysShow(false);
                    hiddenRouter.setPath(getRouterPath(hiddenMenu));
                    hiddenRouter.setComponent(hiddenMenu.getComponent());
                    hiddenRouter.setMeta(new MetaVo(hiddenMenu.getName(), hiddenMenu.getIcon()));
                    routers.add(hiddenRouter);
                }
            } else {
                if (!CollectionUtils.isEmpty(children)) {
                    if(children.size() > 0) {
                        router.setAlwaysShow(true);
                    }
                    //递归
                    router.setChildren(buildMenus(children));
                }
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = "/" + menu.getPath();
        if(menu.getParentId().intValue() != 0) {
            routerPath = menu.getPath();
        }
        return routerPath;
    }

    /**
     * 根据用户id获取用户可操作按钮列表
     * @param userId
     * @return
     */
    @Override
    public List<String> findUserPermsList(Long userId) {
        //判断是否是管理员，如果是，查询所有按钮列表，超级管理员admin账号id为：1
        List<SysMenu> sysMenuList = null;
        if (userId.longValue() == 1) {
            sysMenuList = this.list(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1));
        } else {
            //如果不是管理员，根据userId查询可操作按钮列表
            //多表关联查询
            sysMenuList = sysMenuMapper.findListByUserId(userId);
        }
        //从查询出来的数据里面，获取可操作按钮值的list集合，返回
        List<String> permsList = sysMenuList.stream().filter(item -> item.getType() == 2)
                                                     .map(item -> item.getPerms())
                                                     .collect(Collectors.toList());
        return permsList;
    }

    //删除菜单
    @Override
    public boolean removeById(Serializable id) {
        //判断当前菜单是否有下一层菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0){
            throw new GuiguException(201,"菜单不能删除");
        }
        sysMenuMapper.deleteById(id);
        return false;
    }
}
