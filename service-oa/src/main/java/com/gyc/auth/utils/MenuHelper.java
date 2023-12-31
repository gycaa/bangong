package com.gyc.auth.utils;

import com.gyc.model.system.SysMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 根据菜单数据构建菜单数据
 * </p>
 *
 */
public class MenuHelper {
    /**
     * 使用递归方法建菜单
     * @param sysMenuList
     * @return
     */
    public static List<SysMenu> buildTree(List<SysMenu> sysMenuList) {
        //创建list集合，用于最终数据
        List<SysMenu> trees = new ArrayList<>();
        //把所有菜单数据进行遍历
        for (SysMenu sysMenu : sysMenuList) {
            //递归入口进入
            //parentId=0 是入口
            if (sysMenu.getParentId().longValue() == 0) {
                trees.add(findChildren(sysMenu,sysMenuList));
            }
        }
        return trees;
    }

    /**
     * 递归查找子节点
     * @param treeNodes
     * @return
     */
    public static SysMenu findChildren(SysMenu sysMenu, List<SysMenu> treeNodes) {
        sysMenu.setChildren(new ArrayList<SysMenu>());
        //遍历所有菜单数据，判断id和parentId的关系
        for (SysMenu it : treeNodes) {
             if (sysMenu.getId().longValue() == it.getParentId().longValue()){
                 if (sysMenu.getChildren() == null){
                     sysMenu.setChildren(new ArrayList<>());
                 }
                 sysMenu.getChildren().add(findChildren(it,treeNodes));
             }
        }
        return sysMenu;
    }
}
