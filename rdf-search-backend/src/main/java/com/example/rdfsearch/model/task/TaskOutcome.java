package com.example.rdfsearch.model.task;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务执行结果
 */
public class TaskOutcome {
    private String id;
    private String name;
    private String description;
    private String nextTaskId;
    private String condition;
    private Map<String, Object> resultData;

    public TaskOutcome() {
        this.resultData = new HashMap<>();
        this.condition = "true"; // 默认条件为真
    }

    public TaskOutcome(String id, String name, String nextTaskId) {
        this.id = id;
        this.name = name;
        this.nextTaskId = nextTaskId;
        this.description = "";
        this.condition = "true"; // 默认条件为真
        this.resultData = new HashMap<>();
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

    public String getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(String nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }
}