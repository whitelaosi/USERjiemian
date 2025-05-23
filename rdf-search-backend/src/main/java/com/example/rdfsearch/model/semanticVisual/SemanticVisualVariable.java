package com.example.rdfsearch.model.semanticVisual;

import java.util.*;

/**
 * 语义视觉变量 - 基于PDF第3章的定义
 * 不同基础视觉变量在其变化空间中确定取值或状态的组合所引起的认知语义表征
 */
public class SemanticVisualVariable {
    private String id;
    private String name;
    private String category; // 空间/时间/外观
    private List<String> baseVariables; // 基础视觉变量列表
    private Map<String, Object> semanticMapping; // 语义映射
    private Map<String, Object> parameters; // 参数化表示

    // 一级语义维度（基于图3-3）
    public enum FirstLevelSemantic {
        SPATIAL("空间"),
        TEMPORAL("时间"),
        APPEARANCE("外观");

        private String label;

        FirstLevelSemantic(String label) {
            this.label = label;
        }
    }

    // 二级语义维度
    public enum SecondLevelSemantic {
        // 空间维度
        POSITION("位置"),
        SIZE("尺寸"),
        SHAPE("形状"),
        ORIENTATION("朝向"),
        ARRANGEMENT("排列"),

        // 时间维度
        MOMENT("时刻"),
        DURATION("持续时长"),
        FREQUENCY("频率"),
        ORDER("次序"),
        RATE_OF_CHANGE("变化率"),
        SYNCHRONIZATION("同步"),

        // 外观维度
        COLOR_HUE("色相"),
        INTENSITY("强度"),
        SATURATION("饱和度"),
        TEXTURE("纹理"),
        TRANSPARENCY("透明度");

        private String label;

        SecondLevelSemantic(String label) {
            this.label = label;
        }
    }

    // 视觉变量特性（基于PDF）
    public enum VisualProperty {
        SELECTIVE, // 选择性
        ASSOCIATIVE, // 关联性
        QUANTITATIVE, // 数量性
        ORDER, // 次序性
        GUIDANCE, // 视觉引导性
        STABILITY // 视觉稳定性
    }

    private Set<VisualProperty> properties;
    private Map<String, Double> propertyStrength; // 属性强度

    // 构造函数
    public SemanticVisualVariable() {
        this.baseVariables = new ArrayList<>();
        this.semanticMapping = new HashMap<>();
        this.parameters = new HashMap<>();
        this.properties = new HashSet<>();
        this.propertyStrength = new HashMap<>();
    }

    // 语义组合操作
    public SemanticVisualVariable combine(SemanticVisualVariable other) {
        SemanticVisualVariable combined = new SemanticVisualVariable();
        combined.setName(this.name + "_" + other.name);
        combined.baseVariables.addAll(this.baseVariables);
        combined.baseVariables.addAll(other.baseVariables);
        // 实现语义组合逻辑
        return combined;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getBaseVariables() {
        return baseVariables;
    }

    public void setBaseVariables(List<String> baseVariables) {
        this.baseVariables = baseVariables;
    }

    public Map<String, Object> getSemanticMapping() {
        return semanticMapping;
    }

    public void setSemanticMapping(Map<String, Object> semanticMapping) {
        this.semanticMapping = semanticMapping;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
