package com.gyc.process.service;

import com.gyc.model.process.ProcessRecord;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 审批记录 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-13
 */
public interface ProcessRecordService extends IService<ProcessRecord> {

    void record(Long processId, Integer status, String description);
}
