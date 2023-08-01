package com.gyc.process.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gyc.model.process.Process;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gyc.vo.process.ApprovalVo;
import com.gyc.vo.process.ProcessFormVo;
import com.gyc.vo.process.ProcessQueryVo;
import com.gyc.vo.process.ProcessVo;

import java.util.Map;

/**
 * <p>
 * 审批类型 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-13
 */
public interface ProcessService extends IService<Process> {

    //审批管理列表
    IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo);

    //部署流程定义
    void deployByZip(String deployPath);

    //启动流程
    void startUp(ProcessFormVo processFormVo);

    //查询待处理任务列表
    IPage<ProcessVo> findPending(Page<Process> pageParam);

    //获取审批详情
    Map<String, Object> show(Long id);

    //任务审批
    void approve(ApprovalVo approvalVo);

    //查询已处理任务
    IPage<ProcessVo> findProcessed(Page<Process> pageParam);

    //查询已发起任务
    IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam);
}
