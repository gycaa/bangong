package com.gyc.process.service;

import com.gyc.model.process.ProcessType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 审批类型 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-12
 */
public interface ProcessTypeService extends IService<ProcessType> {

    //获取全部审批分类及每个分类中的模板
    Object findProcessType();
}
