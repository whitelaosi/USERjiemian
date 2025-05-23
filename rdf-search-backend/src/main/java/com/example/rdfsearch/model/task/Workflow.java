package com.example.rdfsearch.model.task;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流 - 任务链的容器
 */
public class Workflow {
    private String id;
    private String name;
    private String description;
    private Map<String, TaskNode> tasks;
    private String startTaskId;
    private String sourceTaskId; // 新增：源任务ID

    public Workflow() {
        this.tasks = new HashMap<>();
    }

    public Workflow(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.tasks = new HashMap<>();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, TaskNode> getTasks() {
        return tasks;
    }

    public void setTasks(Map<String, TaskNode> tasks) {
        this.tasks = tasks;
    }

    public String getStartTaskId() {
        return startTaskId;
    }

    public void setStartTaskId(String startTaskId) {
        this.startTaskId = startTaskId;
    }

    // 新增的getter和setter方法
    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public void addTask(TaskNode task) {
        if (this.tasks == null) {
            this.tasks = new HashMap<>();
        }

        this.tasks.put(task.getId(), task);

        // 如果是第一个添加的任务，设为起始任务
        if (this.tasks.size() == 1 && this.startTaskId == null) {
            this.startTaskId = task.getId();
        }
    }

    public TaskNode getTask(String taskId) {
        return this.tasks.get(taskId);
    }
}