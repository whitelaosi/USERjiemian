package com.example.rdfsearch.model.multilevelFocus;

import java.util.*;

/**
 * 感兴趣程度（Degree of Interest） - 基于PDF第4章
 * 反映了可视化任务对场景对象及其相关属性信息关注程度的聚焦因子
 */
public class DoI {
    private String objectId;
    private double interestValue; // [0,1]区间
    private Map<String, Double> attributeInterest; // 不同属性的兴趣度
    private String calculationMethod; // 计算方法
    private Map<String, Object> context; // 上下文信息

    // DoI传播机制
    private Map<String, Double> propagationWeights; // 传播权重
    private double globalWeight; // 全局权重
    private double localWeight; // 局部权重

    public DoI(String objectId) {
        this.objectId = objectId;
        this.interestValue = 0.0;
        this.attributeInterest = new HashMap<>();
        this.context = new HashMap<>();
        this.propagationWeights = new HashMap<>();
    }

    // 计算DoI值（基于条件概率）
    public double calculateDoI(FocusContext context) {
        // 实现基于上下文的DoI计算
        double baseValue = 0.5; // 基础值

        // 根据任务相关性调整
        double taskRelevance = getTaskRelevance(context);

        // 根据空间距离调整
        double spatialDistance = getSpatialDistance(context);

        // 根据语义相关性调整
        double semanticRelevance = getSemanticRelevance(context);

        // 综合计算
        this.interestValue = baseValue * taskRelevance * (1.0 / (1.0 + spatialDistance)) * semanticRelevance;

        return this.interestValue;
    }

    // DoI传播
    public void propagate(Map<String, DoI> neighbors) {
        for (Map.Entry<String, DoI> entry : neighbors.entrySet()) {
            String neighborId = entry.getKey();
            DoI neighbor = entry.getValue();

            double weight = propagationWeights.getOrDefault(neighborId, 0.5);
            double propagatedValue = this.interestValue * weight;

            // 更新邻居的DoI值
            neighbor.updateFromPropagation(propagatedValue);
        }
    }

    // 从传播更新
    public void updateFromPropagation(double propagatedValue) {
        // 使用衰减函数
        this.interestValue = Math.max(this.interestValue, propagatedValue * 0.8);
    }

    // 辅助方法
    private double getTaskRelevance(FocusContext context) {
        // 实现任务相关性计算
        return 1.0;
    }

    private double getSpatialDistance(FocusContext context) {
        // 实现空间距离计算
        return 0.0;
    }

    private double getSemanticRelevance(FocusContext context) {
        // 实现语义相关性计算
        return 1.0;
    }

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public double getInterestValue() {
        return interestValue;
    }

    public void setInterestValue(double interestValue) {
        this.interestValue = interestValue;
    }

    public Map<String, Double> getAttributeInterest() {
        return attributeInterest;
    }

    public void setAttributeInterest(Map<String, Double> attributeInterest) {
        this.attributeInterest = attributeInterest;
    }
}
