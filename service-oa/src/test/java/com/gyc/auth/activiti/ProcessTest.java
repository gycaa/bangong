package com.gyc.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProcessTest {

    //  RepositoryService
    //  Activiti 的资源管理类，该服务负责部署流程定义，管理流程资源。在使用 Activiti 时，一开始需要先完成流程部署，即将使用建模工具设计的业务流程图通过 RepositoryService 进行部署
    @Autowired
    private RepositoryService repositoryService;

    //RuntimeService
    //Activiti 的流程运行管理类，用于开始一个新的流程实例，获取关于流程执行的相关信息。流程定义用于确定一个流程中的结构和各个节点间行为，而流程实例则是对应的流程定义的一个执行，可以理解为 Java 中类和对象的关系
    @Autowired
    private RuntimeService runtimeService;

    //TaskService
    //Activiti 的任务管理类，用于处理业务运行中的各种任务，例如查询分给用户或组的任务、创建新的任务、分配任务、确定和完成一个任务
    @Autowired
    private TaskService taskService;

    //HistoryService
    //Activiti 的历史管理类，可以查询历史信息。执行流程时，引擎会保存很多数据，比如流程实例启动时间、任务的参与者、完成任务的时间、每个流程实例的执行路径等等。这个服务主要通过查询功能来获得这些数据
    @Autowired
    private HistoryService historyService;

    //单个文件部署
    @Test
    public void deployProcess() {
        // 流程部署
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("process/qingjia.bpmn20.xml")
                .addClasspathResource("process/qingjia.png")
                .name("请假申请流程")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    //启动流程实例
    @Test
    public void startUpProcess() {
        //创建流程实例,我们需要知道流程定义的key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("qingjia");
        //输出实例的相关信息
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
        System.out.println("当前活动Id：" + processInstance.getActivityId());
    }

    /**
     * 查询当前个人待执行的任务
     */
    @Test
    public void findPendingTaskList() {
        //任务负责人
        String assignee = "zhangsan";
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(assignee)//只查询该任务负责人的任务
                .list();

        for (Task task : list) {
            System.out.println("流程实例id：" + task.getProcessInstanceId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    //处理当前任务
    @Test
    public void completTask(){
        Task task = taskService.createTaskQuery()
                .taskAssignee("zhangsan")  //要查询的负责人
                .singleResult();//返回一条

        //完成任务,参数：任务id
        taskService.complete(task.getId());
    }

    //查询已处理历史任务
    @Test
    public void findProcessedTaskList() {
        //张三已处理过的历史任务
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                                                        .taskAssignee("zhangsan")
                                                        .finished().list();

        for (HistoricTaskInstance historicTaskInstance : list) {
            System.out.println("流程实例id：" + historicTaskInstance.getProcessInstanceId());
            System.out.println("任务id：" + historicTaskInstance.getId());
            System.out.println("任务负责人：" + historicTaskInstance.getAssignee());
            System.out.println("任务名称：" + historicTaskInstance.getName());
        }
    }

    /**
     * 查询流程定义
     */
    @Test
    public void findProcessDefinitionList(){
        List<ProcessDefinition> definitionList = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        //输出流程定义信息
        for (ProcessDefinition processDefinition : definitionList) {
            System.out.println("流程定义 id="+processDefinition.getId());
            System.out.println("流程定义 name="+processDefinition.getName());
            System.out.println("流程定义 key="+processDefinition.getKey());
            System.out.println("流程定义 Version="+processDefinition.getVersion());
            System.out.println("流程部署ID ="+processDefinition.getDeploymentId());
        }
    }

    /**
     * 删除流程定义
     */
    public void deleteDeployment() {
        //部署id
        String deploymentId = "82e3bc6b-81da-11ed-8e03-7c57581a7819";
        //删除流程定义，如果该流程定义已有流程实例启动则删除时出错
        repositoryService.deleteDeployment(deploymentId);
        //设置true 级联删除流程定义，即使该流程有流程实例启动也可以删除，设置为false非级别删除方式
        //repositoryService.deleteDeployment(deploymentId, true);
    }

    /**
     * 启动流程实例，添加 businessKey
     */
    @Test
    public void startUpProcessAddBusinessKey(){
        String businessKey = "1";
        // 启动流程实例，指定业务标识businessKey，也就是请假申请单id
        ProcessInstance processInstance = runtimeService.
                startProcessInstanceByKey("qingjia",businessKey);
        // 输出
        System.out.println("业务id:"+processInstance.getBusinessKey());
    }

    //全部流程实例挂起/激活
    @Test
    public void suspendProcessInstance() {
        //获取流程定义的对象
        ProcessDefinition qingjia = repositoryService.createProcessDefinitionQuery().processDefinitionKey("qingjia").singleResult();
        // 获取到当前流程定义是否为暂停状态，suspended方法为true是暂停的，suspended方法为false是运行的
        boolean suspended = qingjia.isSuspended();
        if (suspended) {
            // 暂停挂起状态,激活
            // 参数1:流程定义的id  参数2:是否激活  参数3:时间点
            repositoryService.activateProcessDefinitionById(qingjia.getId(), true, null);
            System.out.println("流程定义:" + qingjia.getId() + "激活");
        } else {
            //激活状态，挂起
            repositoryService.suspendProcessDefinitionById(qingjia.getId(), true, null);
            System.out.println("流程定义:" + qingjia.getId() + "挂起");
        }
    }

    //单个流程实例挂起/激活
    @Test
    public void SingleSuspendProcessInstance() {
        //
        String processInstanceId = "fb58f0c9-1f3d-11ee-9084-1c8341c7c2f2";
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        //获取到当前流程定义是否为暂停状态   suspended方法为true代表为暂停   false就是运行的
        boolean suspended = processInstance.isSuspended();
        if (suspended) {
            //挂起状态，激活
            runtimeService.activateProcessInstanceById(processInstanceId);
            System.out.println("流程实例:" + processInstanceId + "激活");
        } else {
            //激活状态，挂起
            runtimeService.suspendProcessInstanceById(processInstanceId);
            System.out.println("流程实例:" + processInstanceId + "挂起");
        }
    }
}
