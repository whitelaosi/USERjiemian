
package com.example.rdfsearch.model.spatioTemporal;

/**
 * 对象关系
 */
public class ObjectRelationship {
    private String id;
    private String sourceId; // 源对象ID
    private String targetId; // 目标对象ID
    private String relationshipType; // 关系类型
    private String relationshipName; // 关系名称
    private double strength; // 关系强度

    public ObjectRelationship() {
    }

    public ObjectRelationship(String sourceId, String targetId, String relationshipType) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.relationshipType = relationshipType;
        this.strength = 1.0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }
}