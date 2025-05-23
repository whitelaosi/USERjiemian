package com.example.rdfsearch.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.io.Serializable;

public class Task implements Serializable {
    private String uri;
    private String type;
    private String label;
    private String description = ""; // 默认为空字符串而非null
    private Date createdAt;
    private Date updatedAt;
    private String priority;
    private String status;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, Object> parameters = new HashMap<>();

    public Task() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.parameters = new HashMap<>();
    }

    // 主要的getter和setter
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 日期处理
    public String getCreatedAt() {
        return createdAt != null ? createdAt.toString() : null;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = new Date();
    }

    public String getUpdatedAt() {
        return updatedAt != null ? updatedAt.toString() : null;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = new Date();
    }

    @Override
    public String toString() {
        return "Task{" +
                "uri='" + uri + '\'' +
                ", type='" + type + '\'' +
                ", label='" + label + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}