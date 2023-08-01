package com.gyc.process.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gyc.auth.service.SysUserService;
import com.gyc.model.process.Process;
import com.gyc.model.process.ProcessRecord;
import com.gyc.model.process.ProcessTemplate;
import com.gyc.model.system.SysUser;
import com.gyc.process.mapper.ProcessMapper;
import com.gyc.process.service.ProcessRecordService;
import com.gyc.process.service.ProcessService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.process.service.ProcessTemplateService;
import com.gyc.security.custom.LoginUserInfoHelper;
import com.gyc.vo.process.ApprovalVo;
import com.gyc.vo.process.ProcessFormVo;
import com.gyc.vo.process.ProcessQueryVo;
import com.gyc.vo.process.ProcessVo;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-13
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProcessServiceImpl extends ServiceImpl<ProcessMapper, Process> implements ProcessService {

    @Autowired
    private ProcessMapper processMapper;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessTemplateService processTemplateService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessRecordService processRecordService;

    @Autowired
    private HistoryService historyService;

    //审批管理列表
    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        return page;
    }

    @Override
    public void deployByZip(String deployPath) {
            // 定义zip输入流
            InputStream inputStream = this
                    .getClass()
                    .getClassLoader()
                    .getResourceAsStream(deployPath);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            // 流程部署
            Deployment deployment = repositoryService.createDeployment()
                    .addZipInputStream(zipInputStream)
                    .deploy();
        }

    //启动流程实例
    @Override
    public void startUp(ProcessFormVo processFormVo) {
        //根据用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        //根据审批模板id查询模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());
        //保存提交审批信息到业务表，oa_process
        Process process = new Process();
        //把processFormVo复制到process对象里
        BeanUtils.copyProperties(processFormVo, process);
        String workNo = System.currentTimeMillis() + "";
        process.setProcessCode(workNo);
        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName() + "申请");
        process.setStatus(1);//审批中
        processMapper.insert(process);

        //绑定业务id
        String businessKey = String.valueOf(process.getId());
        //流程参数
        Map<String, Object> variables = new HashMap<>();
        //将表单数据放入流程实例中,流程参数form表单json数据，转换map集合
        JSONObject jsonObject = JSON.parseObject(process.getFormValues());
        JSONObject formData = jsonObject.getJSONObject("formData");
        Map<String, Object> map = new HashMap<>();
        //遍历foemData,封装map结合，循环转换
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        variables.put("data", map);
        //启动流程实例                                                               //流程定义key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processTemplate.getProcessDefinitionKey(), businessKey, variables);
        //业务表关联当前流程实例id
        String processInstanceId = processInstance.getId();
        process.setProcessInstanceId(processInstanceId);

        //计算下一个审批人，可能有多个（并行审批）
        List<Task> taskList = this.getCurrentTaskList(processInstanceId);
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assigneeList = new ArrayList<>();
            for(Task task : taskList) {
                SysUser user = sysUserService.getByUsername(task.getAssignee());
                assigneeList.add(user.getName());

                //推送消息给下一个审批人，后续完善
            }
            process.setDescription("等待" + StringUtils.join(assigneeList.toArray(), ",") + "审批");
        }
        //业务和流程关联，更新oa_process数据
        processMapper.updateById(process);

        //记录操作审批信息记录
        processRecordService.record(process.getId(), 1, "发起申请");
    }

    /**
     * 获取当前任务列表
     * @param processInstanceId
     * @return
     */
    private List<Task> getCurrentTaskList(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        return tasks;
    }

    //查询待处理列表
    @Override
    public IPage<ProcessVo> findPending(Page<Process> pageParam) {
        // 封装查询条件，根据当前登录的用户名称
        TaskQuery query = taskService.createTaskQuery().taskAssignee(LoginUserInfoHelper.getUsername()).orderByTaskCreateTime().desc();
        //调用方法分页条件查询，返回list集合，待办任务集合
        //listpage方法两个参数: 第一个参数：开始位置  第二个参数：每页显示记录数
        List<Task> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        long totalCount = query.count();
        //封装返回list集合数据，到 List<ProcessVo>中
        List<ProcessVo> processList = new ArrayList<>();
        // 根据流程的业务ID查询实体并关联
        for (Task item : list) {
            //从task获取流程实例id
            String processInstanceId = item.getProcessInstanceId();
            //根据流程实例id获取实例对象
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                continue;
            }
            // 从流程实例对象获取业务key
            String businessKey = processInstance.getBusinessKey();
            if (businessKey == null) {
                continue;
            }
            //根据业务key获取process对象
            Process process = this.getById(Long.parseLong(businessKey));
            ProcessVo processVo = new ProcessVo();
            //proces对象复制到processVo对象
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId(item.getId());
            //放到最终集合 processList
            processList.add(processVo);
        }
        //封装返回IPage对象                                      当前页           每页显示多少条记录      一共多少条记录
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    @Override
    public Map<String, Object> show(Long id) {
        //根据流程id获取流程信息process
        Process process = this.getById(id);
        //根据流程id获取流程记录信息
        List<ProcessRecord> processRecordList = processRecordService.list(new LambdaQueryWrapper<ProcessRecord>().eq(ProcessRecord::getProcessId, id));
        //根据模板id查询模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        //判断当前用户是否可以审批，可以看到信息详情的用户不一定都能审批，审批后也不能重复审批
        boolean isApprove = false;
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            for(Task task : taskList) {
                //判断任务审批人是否是当前用户                         当前用户
                if(task.getAssignee().equals(LoginUserInfoHelper.getUsername())) {
                    isApprove = true;
                }
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        map.put("isApprove", isApprove);
        return map;
    }

    @Override
    public void approve(ApprovalVo approvalVo) {
        //从approvalVo获取任务id，根据任务id获取流程变量
        Map<String, Object> variables1 = taskService.getVariables(approvalVo.getTaskId());
        for (Map.Entry<String, Object> entry : variables1.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
        }
        //判断审批状态值
        String taskId = approvalVo.getTaskId();
        if (approvalVo.getStatus() == 1) {
            //状态值=1，审批通过
            Map<String, Object> variables = new HashMap<String, Object>();
            taskService.complete(taskId, variables);
        } else {
            //审批驳回，流程结束
            this.endTask(taskId);
        }

        //记录审批相关过程信息 oa_process_record
        String description = approvalVo.getStatus().intValue() == 1 ? "已通过" : "驳回";
        processRecordService.record(approvalVo.getProcessId(), approvalVo.getStatus(), description);

        //查询下一个审批人
        Process process = this.getById(approvalVo.getProcessId());
        //查询任务
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assigneeList = new ArrayList<>();
            for(Task task : taskList) {
                SysUser sysUser = sysUserService.getByUsername(task.getAssignee());
                assigneeList.add(sysUser.getName());

                //推送消息给下一个审批人
            }
            //更新process流程信息
            process.setDescription("等待" + StringUtils.join(assigneeList.toArray(), ",") + "审批");
            process.setStatus(1);
        } else {
            if(approvalVo.getStatus().intValue() == 1) {
                process.setDescription("审批完成（同意）");
                process.setStatus(2);
            } else {
                process.setDescription("审批完成（拒绝）");
                process.setStatus(-1);
            }
        }
        //推送消息给申请人
        this.updateById(process);
    }

    private void endTask(String taskId) {
        //  根据任务id获取任务对象task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //获取流程定义模型 bpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        //获取结束流向结点
        List endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        // 并行任务可能为null
        if(CollectionUtils.isEmpty(endEventList)) {
            return;
        }
        //结束流向结点
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);
        //获取当前流向结点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        //设置相关内容
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);//当前结点
        newSequenceFlow.setTargetFlowElement(endFlowNode);//结束结点

        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());
    }

    //查询已处理任务
    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        // 封装查询条件。根据用户名查询
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                                                        .taskAssignee(LoginUserInfoHelper.getUsername())
                                                        .finished().orderByTaskCreateTime()
                                                        .desc();
        //调用方法条件分页查询，返回list集合
        //listPage方法两个参数：开始位置和每页显示记录数
        List<HistoricTaskInstance> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        //记录总数
        long totalCount = query.count();
        //遍历返回list集合，封装 List<ProcessVo>
        List<ProcessVo> processList = new ArrayList<>();
        for (HistoricTaskInstance item : list) {
            //获取流程实例id
            String processInstanceId = item.getProcessInstanceId();
            //根据流程实例id获取流程process信息
            Process process = this.getOne(new LambdaQueryWrapper<Process>().eq(Process::getProcessInstanceId, processInstanceId));
            //process转换为processVo
            ProcessVo processVo = new ProcessVo();
            //把process对象复制到processVo对象里
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId("0");
            //添加到 processList
            processList.add(processVo);
        }
        //IPage封装分页查询所有数据                               //当前页               //每页记录数     //记录总数
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    //查询已发起任务
    @Override
    public IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        //设置当前用户id
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        //查询
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        for (ProcessVo item : page.getRecords()) {
            item.setTaskId("0");
        }
        return page;
    }
}
