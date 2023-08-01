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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务组功能测试
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProcessTest3 {
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

    //1、部署流程定义及启动流程实例
    @Test
    public void deployProcess01() {
        // 流程部署
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("process/jiaban03.bpmn20.xml")
                .name("加班申请流程03")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jiaban03");
        System.out.println(processInstance.getId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void startUpProcess01() {
        Map<String, Object> variables = new HashMap<>();
        //创建流程实例,我们需要知道流程定义的key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jiaban03", variables);
        //输出实例的相关信息
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
    }

    //2、查询组任务
    @Test
    public void findGroupTaskList() {
        //查询组任务
        List<Task> list = taskService.createTaskQuery()
                .taskCandidateUser("tom01")//根据候选人查询
                .list();
        for (Task task : list) {
            System.out.println("----------------------------");
            System.out.println("流程实例id：" + task.getProcessInstanceId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    //3、拾取组任务
    @Test
    public void claimTask(){
        //拾取任务,即使该用户不是候选人也能拾取(建议拾取时校验是否有资格)
        //校验该用户有没有拾取任务的资格
        Task task = taskService.createTaskQuery()
                .taskCandidateUser("tom01")//根据候选人查询
                .singleResult();
        if(task!=null){
            //拾取任务
            taskService.claim(task.getId(), "tom01");
            System.out.println("任务拾取成功");
        }
    }

    //4、查询个人待办任务
    @Test
    public void findGroupPendingTaskList() {
        //任务负责人
        String assignee = "tom01";
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

    //5、办理个人任务
    @Test
    public void completGroupTask() {
        Task task = taskService.createTaskQuery()
                .taskAssignee("tom01")  //要查询的负责人
                .singleResult();//返回一条
        taskService.complete(task.getId());
    }

    //6、归还组任务，如果个人不想办理该组任务，可以归还组任务，归还后该用户不再是该任务的负责人
    @Test
    public void assigneeToGroupTask() {
        String taskId = "62398d87-1fd8-11ee-b4a2-1c8341c7c2f2";
        // 任务负责人
        String userId = "tom01";
        // 校验userId是否是taskId的负责人，如果是负责人才可以归还组任务
        Task task = taskService
                .createTaskQuery()
                .taskId(taskId)
                .taskAssignee(userId)
                .singleResult();
        if (task != null) {
            // 如果设置为null，归还组任务,该 任务没有负责人
            taskService.setAssignee(taskId, null);
        }
    }

    //7、任务交接，任务负责人将任务交给其它候选人办理该任务
    @Test
    public void assigneeToCandidateUser() {
        // 当前待办任务
        String taskId = "62398d87-1fd8-11ee-b4a2-1c8341c7c2f2";
        // 校验tom01是否是taskId的负责人，如果是负责人才可以归还组任务
        Task task = taskService
                .createTaskQuery()
                .taskId(taskId)
                .taskAssignee("tom01")
                .singleResult();
        if (task != null) {
            // 将此任务交给其它候选人tom02办理该 任务
            taskService.setAssignee(taskId, "tom02");
        }
    }
}
