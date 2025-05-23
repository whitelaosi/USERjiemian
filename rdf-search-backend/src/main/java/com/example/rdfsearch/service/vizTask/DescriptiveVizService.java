package com.example.rdfsearch.service.vizTask;

import com.example.rdfsearch.model.vizTask.VisualizationTask;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 描述性可视化服务 - 处理展示型任务
 */
@Service
public class DescriptiveVizService {

    /**
     * 执行描述性可视化任务
     */
    public Map<String, Object> execute(VisualizationTask task, List<SpatioTemporalObject> objects) {
        Map<String, Object> result = new HashMap<>();

        // 生成展示型可视化配置
        Map<String, Object> vizConfig = new HashMap<>();
        vizConfig.put("type", "descriptive");
        vizConfig.put("layout", "hierarchical");

        // 处理对象数据
        List<Map<String, Object>> processedObjects = new ArrayList<>();
        for (SpatioTemporalObject obj : objects) {
            Map<String, Object> vizObject = new HashMap<>();
            vizObject.put("id", obj.getId());
            vizObject.put("label", obj.getLabel());
            vizObject.put("type", obj.getType());

            // 简单的视觉映射
            vizObject.put("color", getColorByType(obj.getType()));
            vizObject.put("size", 20);

            processedObjects.add(vizObject);
        }

        result.put("config", vizConfig);
        result.put("objects", processedObjects);
        result.put("status", "success");

        return result;
    }

    private String getColorByType(String type) {
        // 简单的类型-颜色映射
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("building", "#3498db");
        colorMap.put("area", "#2ecc71");
        colorMap.put("event", "#e74c3c");
        colorMap.put("person", "#f39c12");

        return colorMap.getOrDefault(type, "#95a5a6");
    }
}