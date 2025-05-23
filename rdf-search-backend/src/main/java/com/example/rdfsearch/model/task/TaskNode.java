package com.example.rdfsearch.model.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务节点 - 工作流中的基本单元
 */
public class TaskNode {
    private String id;
    private String name;
    private String description;
    private String type;
    private Map<String, Object> inputParameters;
    private List<TaskOutcome> possibleOutcomes;

    public TaskNode() {
        this.inputParameters = new HashMap<>();
        this.possibleOutcomes = new ArrayList<>();
    }

    public TaskNode(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = "";
        this.inputParameters = new HashMap<>();
        this.possibleOutcomes = new ArrayList<>();
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(Map<String, Object> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public List<TaskOutcome> getPossibleOutcomes() {
        return possibleOutcomes;
    }

    public void setPossibleOutcomes(List<TaskOutcome> possibleOutcomes) {
        this.possibleOutcomes = possibleOutcomes;
    }

    public void addOutcome(TaskOutcome outcome) {
        if (this.possibleOutcomes == null) {
            this.possibleOutcomes = new ArrayList<>();
        }
        this.possibleOutcomes.add(outcome);
    }
}