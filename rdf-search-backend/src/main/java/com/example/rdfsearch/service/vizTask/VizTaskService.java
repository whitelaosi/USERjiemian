package com.example.rdfsearch.service.vizTask;

import com.example.rdfsearch.model.vizTask.*;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import com.example.rdfsearch.model.Task;
import com.example.rdfsearch.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 可视化任务服务 - 实现PDF第2章定义的三层次可视化任务
 */
@Service
public class VizTaskService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private DescriptiveVizService descriptiveVizService;

    @Autowired
    private AnalyticalVizService analyticalVizService;

    @Autowired
    private ExploratoryVizService exploratoryVizService;

    // 创建可视化任务
    public VisualizationTask createVizTask(Task sourceTask) {
        VisualizationTask vizTask = new VisualizationTask();

        // 基础信息
        vizTask.setId(UUID.randomUUID().toString());
        vizTask.setSourceTaskId(sourceTask.getUri());
        vizTask.setName(sourceTask.getLabel() + "_可视化");

        // 根据任务类型确定可视化任务类型
        TaskType taskType = determineTaskType(sourceTask);
        vizTask.setTaskType(taskType);

        // 设置任务参数
        TaskContext context = buildTaskContext(sourceTask);
        vizTask.setContext(context);

        // 设置目标对象
        List<String> targetObjects = extractTargetObjects(sourceTask);
        vizTask.setTargetObjects(targetObjects);

        return vizTask;
    }

    // 执行可视化任务
    public Map<String, Object> executeVizTask(VisualizationTask vizTask,
            List<SpatioTemporalObject> objects) {

        Map<String, Object> result = new HashMap<>();

        switch (vizTask.getTaskType()) {
            case PRESENTATIONAL:
                result = descriptiveVizService.execute(vizTask, objects);
                break;
            case ANALYTICAL:
                result = analyticalVizService.execute(vizTask, objects);
                break;
            case EXPLORATORY:
                result = exploratoryVizService.execute(vizTask, objects);
                break;
            default:
                throw new IllegalArgumentException("未知的任务类型: " + vizTask.getTaskType());
        }

        result.put("taskId", vizTask.getId());
        result.put("taskType", vizTask.getTaskType());

        return result;
    }

    // 确定任务类型
    private TaskType determineTaskType(Task task) {
        String taskTypeStr = task.getType();

        // 根据任务类型字符串判断
        if (taskTypeStr.contains("比较") || taskTypeStr.contains("展示")) {
            return TaskType.PRESENTATIONAL;
        } else if (taskTypeStr.contains("分析") || taskTypeStr.contains("预测")) {
            return TaskType.ANALYTICAL;
        } else if (taskTypeStr.contains("探索") || taskTypeStr.contains("发现")) {
            return TaskType.EXPLORATORY;
        }

        // 默认为分析型
        return TaskType.ANALYTICAL;
    }

    // 构建任务上下文
    private TaskContext buildTaskContext(Task task) {
        TaskContext context = new TaskContext();

        // 从任务参数中提取上下文信息
        Map<String, Object> params = task.getParameters();

        // 时间范围
        Object timeRanges = params.get("timeRanges");
        if (timeRanges != null) {
            context.setTimeRange(timeRanges.toString());
        }

        // 空间范围
        Object spatialRange = params.get("spatialRange");
        if (spatialRange != null) {
            context.setSpatialRange(spatialRange.toString());
        }

        // 属性列表
        Object attributes = params.get("attributes");
        if (attributes instanceof List) {
            context.setAttributes((List<String>) attributes);
        }

        // 用户信息
        context.setUserId("default_user"); // 可以从会话中获取

        return context;
    }

    // 提取目标对象
    private List<String> extractTargetObjects(Task task) {
        List<String> objects = new ArrayList<>();

        // 从任务参数中提取对象标识
        Map<String, Object> params = task.getParameters();

        Object targetObjects = params.get("targetObjects");
        if (targetObjects instanceof List) {
            objects.addAll((List<String>) targetObjects);
        }

        // 如果没有指定，则使用任务URI作为默认对象
        if (objects.isEmpty()) {
            objects.add(task.getUri());
        }

        return objects;
    }

    // 任务状态管理
    public void updateTaskState(String taskId, String state) {
        // 更新任务状态
        // 可以保存到数据库或缓存中
    }

    // 获取任务执行历史
    public List<Map<String, Object>> getTaskHistory(String taskId) {
        // 返回任务执行历史
        return new ArrayList<>();
    }
}
