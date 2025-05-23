
package com.example.rdfsearch.model.vizTask;

import java.util.*;

/**
 * 可视化任务 - 基于PDF第2章定义的三层次可视化任务
 */
public class VisualizationTask {
    private String id;
    private String name;
    private String sourceTaskId; // 源任务ID
    private TaskType taskType;
    private TaskContext context;
    private List<String> targetObjects;
    private Map<String, Object> parameters;
    private String state; // 任务状态
    private Date createTime;
    private Date updateTime;

    public VisualizationTask() {
        this.targetObjects = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.state = "CREATED";
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    // 任务执行相关方法
    public boolean isValid() {
        return id != null && name != null && taskType != null;
    }

    public void updateState(String newState) {
        this.state = newState;
        this.updateTime = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskContext getContext() {
        return context;
    }

    public void setContext(TaskContext context) {
        this.context = context;
    }

    public List<String> getTargetObjects() {
        return targetObjects;
    }

    public void setTargetObjects(List<String> targetObjects) {
        this.targetObjects = targetObjects;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}