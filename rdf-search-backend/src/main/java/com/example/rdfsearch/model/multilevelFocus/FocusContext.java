package com.example.rdfsearch.model.multilevelFocus;

import com.example.rdfsearch.model.vizTask.VisualizationTask;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import java.util.*;

/**
 * 聚焦上下文 - 包含聚焦相关的所有信息
 */
public class FocusContext {
    private VisualizationTask task;
    private List<SpatioTemporalObject> objects;
    private Map<String, DoI> doiMap; // 兴趣度映射
    private Map<String, LoD> lodMap; // 细节层次映射
    private Map<String, DoA> doaMap; // 抽象程度映射
    private double maxVisualLoad = 100.0; // 最大视觉负载

    public FocusContext() {
        this.objects = new ArrayList<>();
        this.doiMap = new HashMap<>();
        this.lodMap = new HashMap<>();
        this.doaMap = new HashMap<>();
    }

    // Getters and Setters
    public VisualizationTask getTask() {
        return task;
    }

    public void setTask(VisualizationTask task) {
        this.task = task;
    }

    public List<SpatioTemporalObject> getObjects() {
        return objects;
    }

    public void setObjects(List<SpatioTemporalObject> objects) {
        this.objects = objects;
    }

    public Map<String, DoI> getDoiMap() {
        return doiMap;
    }

    public void setDoiMap(Map<String, DoI> doiMap) {
        this.doiMap = doiMap;
    }

    public Map<String, LoD> getLodMap() {
        return lodMap;
    }

    public void setLodMap(Map<String, LoD> lodMap) {
        this.lodMap = lodMap;
    }

    public Map<String, DoA> getDoaMap() {
        return doaMap;
    }

    public void setDoaMap(Map<String, DoA> doaMap) {
        this.doaMap = doaMap;
    }

    public double getMaxVisualLoad() {
        return maxVisualLoad;
    }

    public void setMaxVisualLoad(double maxVisualLoad) {
        this.maxVisualLoad = maxVisualLoad;
    }
}
