package com.example.rdfsearch.controller.multilevelFocus;

import com.example.rdfsearch.model.multilevelFocus.*;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import com.example.rdfsearch.model.vizTask.VisualizationTask;
import com.example.rdfsearch.model.vizTask.TaskType; // 添加这个导入
import com.example.rdfsearch.service.multilevelFocus.MultilevelFocusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 聚焦控制器
 */
@RestController
@RequestMapping("/api/focus")
@CrossOrigin
public class FocusController {

    @Autowired
    private MultilevelFocusService multilevelFocusService;

    /**
     * 应用多层次聚焦
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyFocus(@RequestBody Map<String, Object> request) {
        try {
            // 提取请求数据
            VisualizationTask vizTask = convertToVizTask(request.get("vizTask"));
            List<SpatioTemporalObject> objects = convertToObjects(request.get("objects"));

            // 执行多层次聚焦
            FocusContext focusContext = multilevelFocusService.executeFocus(vizTask, objects);

            // 获取聚焦建议
            Map<String, Object> suggestions = multilevelFocusService.getFocusSuggestions(focusContext);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("doiMap", convertDoIMap(focusContext.getDoiMap()));
            response.put("lodMap", convertLoDMap(focusContext.getLodMap()));
            response.put("doaMap", convertDoAMap(focusContext.getDoaMap()));
            response.put("suggestions", suggestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取聚焦建议
     */
    @PostMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(@RequestBody FocusContext context) {
        try {
            Map<String, Object> suggestions = multilevelFocusService.getFocusSuggestions(context);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("suggestions", suggestions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 辅助转换方法
    private VisualizationTask convertToVizTask(Object taskData) {
        if (taskData instanceof VisualizationTask) {
            return (VisualizationTask) taskData;
        }

        // 从Map转换
        Map<String, Object> taskMap = (Map<String, Object>) taskData;
        VisualizationTask vizTask = new VisualizationTask();

        vizTask.setId((String) taskMap.get("id"));
        vizTask.setName((String) taskMap.get("name"));
        vizTask.setSourceTaskId((String) taskMap.get("sourceTaskId"));

        // 转换任务类型
        String taskTypeStr = (String) taskMap.get("taskType");
        if (taskTypeStr != null) {
            vizTask.setTaskType(TaskType.valueOf(taskTypeStr));
        }

        return vizTask;
    }

    private List<SpatioTemporalObject> convertToObjects(Object objectsData) {
        List<SpatioTemporalObject> objects = new ArrayList<>();

        if (objectsData instanceof List) {
            List<Map<String, Object>> objectsList = (List<Map<String, Object>>) objectsData;

            for (Map<String, Object> objMap : objectsList) {
                SpatioTemporalObject obj = new SpatioTemporalObject();
                obj.setId((String) objMap.get("id"));
                obj.setLabel((String) objMap.get("label"));
                obj.setType((String) objMap.get("type"));

                Map<String, Object> attrs = (Map<String, Object>) objMap.get("attributes");
                if (attrs != null) {
                    obj.setAttributes(attrs);
                }

                objects.add(obj);
            }
        }

        return objects;
    }

    private Map<String, Map<String, Object>> convertDoIMap(Map<String, DoI> doiMap) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<String, DoI> entry : doiMap.entrySet()) {
            Map<String, Object> doiData = new HashMap<>();
            DoI doi = entry.getValue();

            doiData.put("interestValue", doi.getInterestValue());
            doiData.put("attributeInterest", doi.getAttributeInterest());

            result.put(entry.getKey(), doiData);
        }

        return result;
    }

    private Map<String, Map<String, Object>> convertLoDMap(Map<String, LoD> lodMap) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<String, LoD> entry : lodMap.entrySet()) {
            Map<String, Object> lodData = new HashMap<>();
            LoD lod = entry.getValue();

            lodData.put("detailLevel", lod.getDetailLevel());
            lodData.put("detailParameters", lod.getDetailParameters());

            result.put(entry.getKey(), lodData);
        }

        return result;
    }

    private Map<String, Map<String, Object>> convertDoAMap(Map<String, DoA> doaMap) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<String, DoA> entry : doaMap.entrySet()) {
            Map<String, Object> doaData = new HashMap<>();
            DoA doa = entry.getValue();

            doaData.put("abstractionLevel", doa.getAbstractionLevel());
            doaData.put("abstractionType", doa.getAbstractionType());
            doaData.put("visualParameters", doa.getVisualParameters());

            result.put(entry.getKey(), doaData);
        }

        return result;
    }
}