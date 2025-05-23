
package com.example.rdfsearch.model.vizTask;

import java.util.*;

/**
 * 任务上下文 - 包含任务执行所需的上下文信息
 */
public class TaskContext {
    private String userId;
    private String timeRange; // 时间范围
    private String spatialRange; // 空间范围
    private List<String> attributes; // 关注的属性列表
    private Map<String, Object> userPreferences; // 用户偏好
    private Map<String, Object> environmentInfo; // 环境信息
    private Map<String, Object> additionalInfo; // 额外信息

    public TaskContext() {
        this.attributes = new ArrayList<>();
        this.userPreferences = new HashMap<>();
        this.environmentInfo = new HashMap<>();
        this.additionalInfo = new HashMap<>();
    }

    // 辅助方法
    public void addAttribute(String attribute) {
        if (!attributes.contains(attribute)) {
            attributes.add(attribute);
        }
    }

    public void setUserPreference(String key, Object value) {
        userPreferences.put(key, value);
    }

    public Object getUserPreference(String key) {
        return userPreferences.get(key);
    }

    public void setEnvironmentInfo(String key, Object value) {
        environmentInfo.put(key, value);
    }

    public Object getEnvironmentInfo(String key) {
        return environmentInfo.get(key);
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getSpatialRange() {
        return spatialRange;
    }

    public void setSpatialRange(String spatialRange) {
        this.spatialRange = spatialRange;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(Map<String, Object> userPreferences) {
        this.userPreferences = userPreferences;
    }

    public Map<String, Object> getEnvironmentInfo() {
        return environmentInfo;
    }

    public void setEnvironmentInfo(Map<String, Object> environmentInfo) {
        this.environmentInfo = environmentInfo;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}