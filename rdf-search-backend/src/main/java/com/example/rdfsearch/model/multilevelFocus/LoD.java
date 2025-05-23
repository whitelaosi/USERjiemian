package com.example.rdfsearch.model.multilevelFocus;

import java.util.*;

/**
 * 细节层次（Level of Detail） - 基于PDF第4章
 * 反映了场景对象内容信息详略程度的聚焦因子
 */
public class LoD {
    private String objectId;
    private int detailLevel; // 0-3的整数，3为最高细节
    private String simplificationMethod; // 简化方法
    private Map<String, Object> detailParameters; // 细节参数

    // 细节层次定义
    public enum DetailLevel {
        LEVEL_0(0, "最简略"), // 只显示存在性
        LEVEL_1(1, "简略"), // 基本形状和主要属性
        LEVEL_2(2, "一般"), // 详细形状和多数属性
        LEVEL_3(3, "详细"); // 完整细节和所有属性

        private int level;
        private String description;

        DetailLevel(int level, String description) {
            this.level = level;
            this.description = description;
        }
    }

    public LoD(String objectId) {
        this.objectId = objectId;
        this.detailLevel = 0;
        this.detailParameters = new HashMap<>();
    }

    // 根据DoI计算LoD
    public int calculateLoD(DoI doi, Map<String, Object> context) {
        double interest = doi.getInterestValue();

        // 基于兴趣度映射到细节层次
        if (interest > 0.75) {
            this.detailLevel = 3;
        } else if (interest > 0.5) {
            this.detailLevel = 2;
        } else if (interest > 0.25) {
            this.detailLevel = 1;
        } else {
            this.detailLevel = 0;
        }

        // 根据上下文调整
        adjustByContext(context);

        return this.detailLevel;
    }

    // 根据上下文调整细节层次
    private void adjustByContext(Map<String, Object> context) {
        // 考虑空间尺度
        Object spaceScale = context.get("spaceScale");
        if (spaceScale != null && spaceScale.equals("MACRO_SCALE")) {
            this.detailLevel = Math.min(this.detailLevel, 1);
        }

        // 考虑显示设备
        Object displayType = context.get("displayType");
        if (displayType != null && displayType.equals("MOBILE")) {
            this.detailLevel = Math.min(this.detailLevel, 2);
        }
    }

    // 生成细节参数
    public Map<String, Object> generateDetailParameters() {
        Map<String, Object> params = new HashMap<>();

        switch (detailLevel) {
            case 0:
                params.put("geometry", "point");
                params.put("attributes", Arrays.asList("id", "type"));
                params.put("texture", false);
                break;
            case 1:
                params.put("geometry", "simplifiedMesh");
                params.put("attributes", Arrays.asList("id", "type", "name", "status"));
                params.put("texture", false);
                break;
            case 2:
                params.put("geometry", "detailedMesh");
                params.put("attributes", Arrays.asList("id", "type", "name", "status", "properties"));
                params.put("texture", true);
                params.put("textureResolution", "medium");
                break;
            case 3:
                params.put("geometry", "fullMesh");
                params.put("attributes", "all");
                params.put("texture", true);
                params.put("textureResolution", "high");
                params.put("animation", true);
                break;
        }

        this.detailParameters = params;
        return params;
    }

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public int getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(int detailLevel) {
        this.detailLevel = detailLevel;
    }

    public String getSimplificationMethod() {
        return simplificationMethod;
    }

    public void setSimplificationMethod(String simplificationMethod) {
        this.simplificationMethod = simplificationMethod;
    }

    public Map<String, Object> getDetailParameters() {
        return detailParameters;
    }

    public void setDetailParameters(Map<String, Object> detailParameters) {
        this.detailParameters = detailParameters;
    }
}