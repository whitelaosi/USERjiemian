
package com.example.rdfsearch.service.vizTask;

import com.example.rdfsearch.model.vizTask.VisualizationTask;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 探索型可视化服务 - 处理探索型任务
 */
@Service
public class ExploratoryVizService {

    /**
     * 执行探索型可视化任务
     */
    public Map<String, Object> execute(VisualizationTask task, List<SpatioTemporalObject> objects) {
        Map<String, Object> result = new HashMap<>();

        // 生成探索型可视化配置
        Map<String, Object> vizConfig = new HashMap<>();
        vizConfig.put("type", "exploratory");
        vizConfig.put("interactive", true);
        vizConfig.put("features", Arrays.asList("zoom", "pan", "filter", "highlight"));

        // 创建探索界面数据
        Map<String, Object> explorationData = createExplorationData(objects);

        // 生成交互配置
        Map<String, Object> interactionConfig = createInteractionConfig();

        result.put("config", vizConfig);
        result.put("explorationData", explorationData);
        result.put("interactionConfig", interactionConfig);
        result.put("status", "success");

        return result;
    }

    private Map<String, Object> createExplorationData(List<SpatioTemporalObject> objects) {
        Map<String, Object> explorationData = new HashMap<>();

        // 创建节点数据
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SpatioTemporalObject obj : objects) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", obj.getId());
            node.put("label", obj.getLabel());
            node.put("type", obj.getType());
            node.put("attributes", obj.getAttributes());

            // 添加探索相关的元数据
            node.put("expandable", !obj.getChildren().isEmpty());
            node.put("selectable", true);

            nodes.add(node);
        }

        // 创建边数据（关系）
        List<Map<String, Object>> edges = new ArrayList<>();
        // 这里可以根据对象间的关系创建边

        explorationData.put("nodes", nodes);
        explorationData.put("edges", edges);

        return explorationData;
    }

    private Map<String, Object> createInteractionConfig() {
        Map<String, Object> config = new HashMap<>();

        // 定义交互行为
        config.put("onNodeClick", "showDetails");
        config.put("onNodeHover", "highlight");
        config.put("onNodeDoubleClick", "expand");

        // 定义过滤器
        List<Map<String, Object>> filters = new ArrayList<>();
        Map<String, Object> typeFilter = new HashMap<>();
        typeFilter.put("field", "type");
        typeFilter.put("type", "select");
        typeFilter.put("options", Arrays.asList("building", "area", "event", "person"));
        filters.add(typeFilter);

        config.put("filters", filters);

        return config;
    }
}