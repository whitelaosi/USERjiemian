
package com.example.rdfsearch.model.vizTask;

/**
 * 任务类型枚举 - 基于PDF定义的三种可视化任务类型
 */
public enum TaskType {
    /**
     * 展示型任务
     * 用于信息展示和描述性可视化
     */
    PRESENTATIONAL("展示型", "用于信息展示和描述性可视化"),

    /**
     * 分析型任务
     * 用于数据分析和比较
     */
    ANALYTICAL("分析型", "用于数据分析和比较"),

    /**
     * 探索型任务
     * 用于数据探索和发现
     */
    EXPLORATORY("探索型", "用于数据探索和发现");

    private String name;
    private String description;

    TaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据任务描述推断任务类型
     */
    public static TaskType inferFromDescription(String description) {
        if (description == null) {
            return ANALYTICAL; // 默认类型
        }

        String desc = description.toLowerCase();

        if (desc.contains("展示") || desc.contains("显示") || desc.contains("呈现")) {
            return PRESENTATIONAL;
        } else if (desc.contains("分析") || desc.contains("比较") || desc.contains("评估")) {
            return ANALYTICAL;
        } else if (desc.contains("探索") || desc.contains("发现") || desc.contains("寻找")) {
            return EXPLORATORY;
        }

        return ANALYTICAL; // 默认类型
    }
}