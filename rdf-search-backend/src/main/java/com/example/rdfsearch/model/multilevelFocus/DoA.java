package com.example.rdfsearch.model.multilevelFocus;

import java.util.*;

/**
 * 抽象程度（Degree of Abstraction） - 基于PDF第4章
 * 反映了场景对象可视化表达抽象程度的聚焦因子
 */
public class DoA {
    private String objectId;
    private double abstractionLevel; // [0,1]区间，0表示具体，1表示抽象
    private String abstractionMethod; // 抽象方法
    private Map<String, Object> visualParameters; // 视觉参数

    // 抽象类型
    public enum AbstractionType {
        GEOMETRIC("几何抽象"), // 简化几何形状
        SYMBOLIC("符号抽象"), // 使用符号表示
        TEXTUAL("文本抽象"), // 使用文本标签
        COLOR_BASED("颜色抽象"), // 基于颜色编码
        AGGREGATED("聚合抽象"); // 多个对象聚合

        private String description;

        AbstractionType(String description) {
            this.description = description;
        }
    }

    private AbstractionType abstractionType;

    public DoA(String objectId) {
        this.objectId = objectId;
        this.abstractionLevel = 0.0;
        this.visualParameters = new HashMap<>();
    }

    // 计算抽象程度
    public double calculateAbstraction(DoI doi, LoD lod) {
        // 基于兴趣度和细节层次计算抽象程度
        double interest = doi.getInterestValue();
        double detail = lod.getDetailLevel() / 3.0;

        // 低兴趣度或低细节层次导致高抽象
        this.abstractionLevel = 1.0 - (interest * detail);

        // 确定抽象类型
        determineAbstractionType();

        return this.abstractionLevel;
    }

    // 确定抽象类型
    private void determineAbstractionType() {
        if (abstractionLevel < 0.2) {
            abstractionType = AbstractionType.GEOMETRIC;
        } else if (abstractionLevel < 0.4) {
            abstractionType = AbstractionType.COLOR_BASED;
        } else if (abstractionLevel < 0.6) {
            abstractionType = AbstractionType.SYMBOLIC;
        } else if (abstractionLevel < 0.8) {
            abstractionType = AbstractionType.TEXTUAL;
        } else {
            abstractionType = AbstractionType.AGGREGATED;
        }
    }

    // 应用抽象到视觉参数
    public Map<String, Object> applyAbstraction() {
        Map<String, Object> visualMapping = new HashMap<>();

        switch (abstractionType) {
            case GEOMETRIC:
                visualMapping.put("geometry", "simplified");
                visualMapping.put("detail", "high");
                break;
            case SYMBOLIC:
                visualMapping.put("representation", "icon");
                visualMapping.put("size", "fixed");
                break;
            case TEXTUAL:
                visualMapping.put("display", "label");
                visualMapping.put("visibility", "text-only");
                break;
            case COLOR_BASED:
                visualMapping.put("encoding", "color");
                visualMapping.put("shape", "basic");
                break;
            case AGGREGATED:
                visualMapping.put("grouping", "clustered");
                visualMapping.put("representation", "aggregate");
                break;
        }

        return visualMapping;
    }

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public double getAbstractionLevel() {
        return abstractionLevel;
    }

    public void setAbstractionLevel(double abstractionLevel) {
        this.abstractionLevel = abstractionLevel;
    }

    public String getAbstractionMethod() {
        return abstractionMethod;
    }

    public void setAbstractionMethod(String abstractionMethod) {
        this.abstractionMethod = abstractionMethod;
    }

    public Map<String, Object> getVisualParameters() {
        return visualParameters;
    }

    public void setVisualParameters(Map<String, Object> visualParameters) {
        this.visualParameters = visualParameters;
    }

    public AbstractionType getAbstractionType() {
        return abstractionType;
    }

    public void setAbstractionType(AbstractionType abstractionType) {
        this.abstractionType = abstractionType;
    }
}