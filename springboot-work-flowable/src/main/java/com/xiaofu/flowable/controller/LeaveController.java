package com.xiaofu.flowable.controller;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping(value = "leave")
public class LeaveController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    @RequestMapping(value = "startLeaveProcess1")
    @ResponseBody
    public String runProcess(String staffId){
        Map<String, Object> map = new HashMap<>(2);
        map.put("processType", "ApplyOffShop");
        map.put("sponsor", "HD001");
        map.put("sponsorName", "Ayesha");
        map.put("sponsorType", "领导");
        map.put("deptId", "M-G");
//        map.put("approved", "3");
        Authentication.setAuthenticatedUserId("zhangsan");
        // 发起流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ApplyOffShop", map);
        System.out.println(processInstance.getId());
        return processInstance.getId();
    }
    @RequestMapping(value = "misty1")
    @ResponseBody
    public void completeTask(String sdf ){
        Task task = taskService.createTaskQuery()
                //.processInstanceId("2501")
                .processDefinitionId(sdf)
                .taskAssignee("lisi")
                .singleResult();
        Map<String, Object> variables = new HashMap<>(2);

        variables.put("approved", false);
        if(task != null){
            // 完成任务
            taskService.complete(task.getId(),variables);
            System.out.println("完成Task");
        }
    }
    @RequestMapping(value = "misty")
    @ResponseBody
    public void misty(boolean staffId){
        System.out.println("===");
        System.out.println("000");
        System.out.println("999");
        System.out.println("888");
        System.out.println("777");

    }
    /**
     * @author xiaofu
     * @description 启动流程
     * @date 2020/8/26 17:36
     */
    @RequestMapping(value = "startLeaveProcess")
    @ResponseBody
    public String startLeaveProcess(String staffId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("ApplyOffShopTask", staffId);
//        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ApplyOffShop", map);
        StringBuilder sb = new StringBuilder();
//        sb.append("创建请假流程 processId：" + processInstance.getId());
        List<Task> tasks = taskService.createTaskQuery().taskAssignee("HD001").orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            sb.append("任务taskId:" + task.getId());
        }
        return sb.toString();
    }

    /**
     * @param taskId
     * @author xinzhifu
     * @description 批准
     * @date 2020/8/27 14:30
     */
    @RequestMapping(value = "applyTask")
    @ResponseBody
    public String applyTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "通过");
        taskService.complete(taskId, map);
        return "申请审核通过~";
    }

    /**
     * @param taskId
     * @author xinzhifu
     * @description 驳回
     * @date 2020/8/27 14:30
     */
    @ResponseBody
    @RequestMapping(value = "rejectTask")
    public String rejectTask(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "驳回");
        taskService.complete(taskId, map);
        return "申请审核驳回~";
    }


    /**
     * @author xiaofu
     * @description 生成流程图
     * @date 2020/8/27 14:29
     */
    @RequestMapping(value = "createProcessDiagramPic")
    public void createProcessDiagramPic(HttpServletResponse httpServletResponse, String processId) throws Exception {

        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        /**
         * 生成流程图
         */
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0,false);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
