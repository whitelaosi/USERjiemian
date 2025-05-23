package com.example.rdfsearch.service;

import com.example.rdfsearch.model.task.TaskNode;
import com.example.rdfsearch.model.task.TaskOutcome;
import com.example.rdfsearch.model.task.Workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务链执行服务 - 处理树状任务执行和多属性比较
 */
@Service
public class TaskChainService {

    @Autowired
    private SparqlService sparqlService;

    @Autowired
    private TaskService taskService;

    // 缓存执行上下文，保存执行状态
    private Map<String, ExecutionContext> executionContexts = new ConcurrentHashMap<>();

    // 工作流定义缓存
    private Map<String, Workflow> workflowCache = new ConcurrentHashMap<>();

    // 支持的聚合方法
    private enum AggregationMethod {
        AVERAGE, MAX, MIN, SUM, LATEST, FIRST
    }

    /**
     * 获取任务节点的所有可能选项，用于用户交互选择
     */
    public Map<String, Object> getTaskOptions(String workflowId, String taskId) {
        // 获取工作流定义
        Workflow workflow = getWorkflowDefinition(workflowId);

        // 获取任务节点
        TaskNode task = workflow.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("找不到任务: " + taskId);
        }

        // 获取或创建执行上下文
        ExecutionContext context = executionContexts.computeIfAbsent(
                workflowId, id -> new ExecutionContext(id));

        // 返回任务信息和可选的结果
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskName", task.getName());
        result.put("taskDescription", task.getDescription());
        result.put("taskType", task.getType());
        result.put("inputParameters", task.getInputParameters());

        // 如果是第一次访问任务，执行任务分析逻辑获取数据
        if (!context.isTaskExecuted(taskId)) {
            executeTaskAnalysis(task, context);
        }

        // 获取任务的分析结果
        Map<String, Object> analysisData = new HashMap<>();
        if (task.getType().equals("MultiAttributeComparison")) {
            analysisData.put("comparisonResults", context.getVariable("comparisonResults"));
            analysisData.put("analysisResults", context.getVariable("analysisResults"));
        } else if (task.getType().equals("DataAnalysis")) {
            analysisData.put("detailedAnalysis", context.getVariable("detailedAnalysis"));
        } else if (task.getType().equals("DeepAnalysis")) {
            analysisData.put("deepAnalysisResults", context.getVariable("deepAnalysisResults"));
        }
        result.put("analysisData", analysisData);

        // 返回可能的选择（结果）
        List<Map<String, Object>> possibleOutcomes = new ArrayList<>();
        for (TaskOutcome outcome : task.getPossibleOutcomes()) {
            Map<String, Object> outcomeMap = new HashMap<>();
            outcomeMap.put("id", outcome.getId());
            outcomeMap.put("name", outcome.getName());
            outcomeMap.put("description", outcome.getDescription());
            outcomeMap.put("nextTaskId", outcome.getNextTaskId());
            possibleOutcomes.add(outcomeMap);
        }
        result.put("possibleOutcomes", possibleOutcomes);
        // 添加推荐结果信息
        String recommendedOutcomeId = getRecommendedOutcome(task, context);
        if (recommendedOutcomeId != null) {
            TaskOutcome recommendedOutcome = task.getPossibleOutcomes().stream()
                    .filter(o -> o.getId().equals(recommendedOutcomeId))
                    .findFirst().orElse(null);

            if (recommendedOutcome != null) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("outcomeId", recommendedOutcomeId);
                recommendation.put("name", recommendedOutcome.getName());
                recommendation.put("reason", getRecommendationReason(task, context));

                result.put("recommendation", recommendation);
            }
        }
        return result;
    }

    /**
     * 选择任务结果，前进到下一个任务
     */
    public Map<String, Object> selectOutcome(String workflowId, String taskId, String outcomeId,
            String contradictionReason) {
        // 获取工作流定义
        Workflow workflow = getWorkflowDefinition(workflowId);

        // 获取任务节点
        TaskNode task = workflow.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("找不到任务: " + taskId);
        }

        // 获取选择的结果
        TaskOutcome selectedOutcome = task.getPossibleOutcomes().stream()
                .filter(o -> o.getId().equals(outcomeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("找不到结果: " + outcomeId));

        // 获取执行上下文
        ExecutionContext context = executionContexts.computeIfAbsent(
                workflowId, id -> new ExecutionContext(id));

        // 获取推荐的结果
        String recommendedOutcomeId = getRecommendedOutcome(task, context);

        // 检查是否与推荐结果不符
        boolean isDeviated = recommendedOutcomeId != null && !recommendedOutcomeId.equals(outcomeId);

        // 如果存在偏差，记录
        if (isDeviated) {
            context.recordDecisionDeviation(taskId, recommendedOutcomeId, outcomeId);

            // 记录用户提供的原因
            if (contradictionReason != null && !contradictionReason.isEmpty()) {
                context.recordDeviationReason(taskId, contradictionReason);
            }
        }

        // 记录用户选择
        context.addHistory(taskId, outcomeId);

        // 执行任务处理逻辑
        executeTaskByType(task, selectedOutcome, context);

        // 标记任务已执行
        context.markTaskExecuted(taskId);

        // 使用getOutcomeData获取结果数据
        Map<String, Object> outcomeData = getOutcomeData(task, selectedOutcome, context);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("workflowId", workflowId);
        result.put("taskId", taskId);
        result.put("selectedOutcome", outcomeId);
        result.put("outcomeName", selectedOutcome.getName());
        result.put("nextTaskId", selectedOutcome.getNextTaskId());
        result.put("executionPath", context.getExecutionPath());
        result.put("completed", selectedOutcome.getNextTaskId() == null);
        result.put("outcomeData", outcomeData);

        // 添加偏差信息到结果
        if (isDeviated) {
            Map<String, Object> deviationInfo = new HashMap<>();
            deviationInfo.put("recommendedOutcomeId", recommendedOutcomeId);

            TaskOutcome recommendedOutcome = task.getPossibleOutcomes().stream()
                    .filter(o -> o.getId().equals(recommendedOutcomeId))
                    .findFirst().orElse(null);

            if (recommendedOutcome != null) {
                deviationInfo.put("recommendedOutcomeName", recommendedOutcome.getName());
            }

            deviationInfo.put("reason", context.getDeviationReason(taskId));
            result.put("deviation", deviationInfo);
        }

        return result;
    }

    /**
     * 获取结果数据
     */
    private Map<String, Object> getOutcomeData(TaskNode task, TaskOutcome outcome, ExecutionContext context) {
        Map<String, Object> data = new HashMap<>();

        switch (task.getType()) {
            case "MultiAttributeComparison":
                data.put("comparisonResults", context.getVariable("comparisonResults"));
                data.put("analysisResults", context.getVariable("analysisResults"));
                break;
            case "DataAnalysis":
                data.put("detailedAnalysis", context.getVariable("detailedAnalysis"));
                break;
            case "DeepAnalysis":
                data.put("deepAnalysisResults", context.getVariable("deepAnalysisResults"));
                break;
        }

        return data;
    }

    /**
     * 执行任务分析逻辑
     */
    private void executeTaskAnalysis(TaskNode task, ExecutionContext context) {
        switch (task.getType()) {
            case "MultiAttributeComparison":
                executeMultiAttributeComparison(task, context);
                break;
            case "DataAnalysis":
                executeDataAnalysis(task, context);
                break;
            case "DeepAnalysis":
                executeDeepAnalysis(task, context);
                break;
            case "CustomScript":
                executeCustomScript(task, context);
                break;
        }
    }

    /**
     * 执行工作流
     * 
     * @param workflowId        工作流ID
     * @param initialParameters 初始参数
     * @return 执行结果
     */
    public Map<String, Object> executeWorkflow(String workflowId, Map<String, Object> initialParameters) {
        // 每次执行工作流时，创建一个全新的执行上下文，而不是复用旧的
        ExecutionContext context = new ExecutionContext(workflowId);
        executionContexts.put(workflowId, context);

        // 2. 设置初始参数
        initialParameters.forEach((key, value) -> context.setVariable(key, value));

        // 3. 获取工作流定义
        Workflow workflow = getWorkflowDefinition(workflowId);

        // 4. 执行工作流中的第一个任务
        String currentTaskId = workflow.getStartTaskId();
        TaskNode currentTask = null;
        TaskOutcome outcome = null;

        // 添加循环检测
        Set<String> visitedStates = new HashSet<>();

        // 5. 循环执行任务，直到工作流结束
        while (currentTaskId != null) {
            currentTask = workflow.getTask(currentTaskId);

            if (currentTask == null) {
                throw new RuntimeException("找不到任务: " + currentTaskId);
            }

            // 检测循环
            String currentState = currentTaskId;
            if (visitedStates.contains(currentState)) {
                System.out.println("检测到循环执行，终止工作流: " + currentState);
                break;
            }
            visitedStates.add(currentState);

            // 执行当前任务
            outcome = executeTask(currentTask, context);

            // 记录执行历史
            context.addHistory(currentTaskId, outcome.getId());
            context.markTaskExecuted(currentTaskId);

            // 获取下一个任务ID
            currentTaskId = outcome.getNextTaskId();
        }

        // 6. 返回最终结果
        Map<String, Object> result = new HashMap<>();
        result.put("workflowId", workflowId);
        result.put("completed", true);
        result.put("executionPath", context.getExecutionPath());

        // 如果有最终结果，添加到返回值
        if (outcome != null) {
            result.put("finalOutcome", outcome.getName());
            result.put("finalResult", outcome.getResultData());
        }

        return result;
    }

    /**
     * 重置工作流执行上下文
     */
    public void resetWorkflowContext(String workflowId) {
        // 从上下文缓存中移除，强制下次执行时创建新的上下文
        executionContexts.remove(workflowId);
    }

    /**
     * 获取工作流定义
     */
    public Workflow getWorkflowDefinition(String workflowId) {
        // 先从缓存中获取
        Workflow cachedWorkflow = workflowCache.get(workflowId);
        if (cachedWorkflow != null) {
            return cachedWorkflow;
        }

        // 这里应该从数据库或配置加载工作流
        // 暂时返回一个示例工作流
        Workflow workflow = createSampleWorkflow(workflowId);

        // 保存到缓存
        workflowCache.put(workflowId, workflow);

        return workflow;
    }

    /**
     * 设置工作流的源任务
     */
    public void setWorkflowSource(String workflowId, String sourceTaskId) {
        Workflow workflow = getWorkflowDefinition(workflowId);
        if (workflow != null) {
            workflow.setSourceTaskId(sourceTaskId);
            workflowCache.put(workflowId, workflow);
        } else {
            throw new RuntimeException("找不到工作流: " + workflowId);
        }
    }

    /**
     * 创建示例工作流 - 能量阈值分析
     */
    private Workflow createSampleWorkflow(String workflowId) {
        Workflow workflow = new Workflow(workflowId, "能量分析工作流");
        workflow.setDescription("分析不同时间段的能量阈值变化并提供相应的分析和建议");

        // 初始化源任务ID为空
        workflow.setSourceTaskId(null);

        // 创建任务1：多属性比较
        TaskNode task1 = new TaskNode("task1", "能量数据比较", "MultiAttributeComparison");
        task1.setDescription("对比两个时间段的能量阈值和微震事件数");

        Map<String, Object> params1 = new HashMap<>();
        params1.put("timeRanges", Arrays.asList("2023-05-04/2023-05-05", "2023-05-03/2023-05-04"));
        params1.put("attributes", Arrays.asList("energyRelease", "microseismicEventCount"));
        params1.put("aggregationMethod", "AVERAGE");
        task1.setInputParameters(params1);

        // 定义任务1的可能结果
        TaskOutcome outcome1_1 = new TaskOutcome("outcome1_1", "能量增加", "task2");
        outcome1_1.setDescription("能量值增加10%以上");
        outcome1_1.setCondition("energyChange > 10");

        TaskOutcome outcome1_2 = new TaskOutcome("outcome1_2", "能量减少", "task3");
        outcome1_2.setDescription("能量值减少10%以上");
        outcome1_2.setCondition("energyChange < -10");

        TaskOutcome outcome1_3 = new TaskOutcome("outcome1_3", "能量稳定", "task4");
        outcome1_3.setDescription("能量值变化在±10%以内");
        outcome1_3.setCondition("true");

        task1.addOutcome(outcome1_1);
        task1.addOutcome(outcome1_2);
        task1.addOutcome(outcome1_3);

        // 创建任务2：数据分析 - 能量增加
        TaskNode task2 = new TaskNode("task2", "能量增加分析", "DataAnalysis");
        task2.setDescription("分析能量增加的原因和潜在影响");

        TaskOutcome outcome2_1 = new TaskOutcome("outcome2_1", "能量增加显著", "task5");
        outcome2_1.setDescription("能量增加超过30%");
        outcome2_1.setCondition("energyChange > 30");

        TaskOutcome outcome2_2 = new TaskOutcome("outcome2_2", "能量增加轻微", null);
        outcome2_2.setDescription("能量增加不超过30%");
        outcome2_2.setCondition("true");

        task2.addOutcome(outcome2_1);
        task2.addOutcome(outcome2_2);

        // 创建任务3：数据分析 - 能量减少
        TaskNode task3 = new TaskNode("task3", "能量减少分析", "DataAnalysis");
        task3.setDescription("分析能量减少的原因和影响");

        TaskOutcome outcome3_1 = new TaskOutcome("outcome3_1", "能量减少显著", "task6");
        outcome3_1.setDescription("能量减少超过30%");
        outcome3_1.setCondition("energyChange < -30");

        TaskOutcome outcome3_2 = new TaskOutcome("outcome3_2", "能量减少轻微", null);
        outcome3_2.setDescription("能量减少不超过30%");
        outcome3_2.setCondition("true");

        task3.addOutcome(outcome3_1);
        task3.addOutcome(outcome3_2);

        // 创建任务4：数据分析 - 能量稳定
        TaskNode task4 = new TaskNode("task4", "能量稳定分析", "DataAnalysis");
        task4.setDescription("分析能量稳定的情况");

        TaskOutcome outcome4_1 = new TaskOutcome("outcome4_1", "能量稳定", null);
        outcome4_1.setDescription("能量保持稳定");
        outcome4_1.setCondition("true");

        task4.addOutcome(outcome4_1);

        // 创建任务5：深度分析 - 能量增加显著
        TaskNode task5 = new TaskNode("task5", "能量增加显著分析", "DeepAnalysis");
        task5.setDescription("对能量显著增加情况进行深入分析");

        TaskOutcome outcome5_1 = new TaskOutcome("outcome5_1", "需要干预", null);
        outcome5_1.setDescription("风险评级为高");
        outcome5_1.setCondition("true");

        TaskOutcome outcome5_2 = new TaskOutcome("outcome5_2", "持续监测", null);
        outcome5_2.setDescription("风险评级为中");
        outcome5_2.setCondition("false");

        task5.addOutcome(outcome5_1);
        task5.addOutcome(outcome5_2);

        // 创建任务6：深度分析 - 能量减少显著
        TaskNode task6 = new TaskNode("task6", "能量减少显著分析", "DeepAnalysis");
        task6.setDescription("对能量显著减少情况进行深入分析");

        TaskOutcome outcome6_1 = new TaskOutcome("outcome6_1", "分析完成", null);
        outcome6_1.setDescription("分析完成");
        outcome6_1.setCondition("true");

        task6.addOutcome(outcome6_1);

        // 将任务添加到工作流
        workflow.addTask(task1);
        workflow.addTask(task2);
        workflow.addTask(task3);
        workflow.addTask(task4);
        workflow.addTask(task5);
        workflow.addTask(task6);

        return workflow;
    }

    /**
     * 执行上下文类
     */
    class ExecutionContext {
        private String id;
        private Map<String, Object> variables;
        private List<ExecutionHistoryEntry> history;
        private Set<String> executedTasks;
        private Map<String, String> decisionDeviations;
        private Map<String, String> deviationReasons;

        public ExecutionContext(String id) {
            this.id = id;
            this.variables = new HashMap<>();
            this.history = new ArrayList<>();
            this.executedTasks = new HashSet<>();
            this.decisionDeviations = new HashMap<>();
            this.deviationReasons = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public void setVariable(String name, Object value) {
            variables.put(name, value);
        }

        public Object getVariable(String name) {
            return variables.get(name);
        }

        public void addHistory(String taskId, String outcomeId) {
            history.add(new ExecutionHistoryEntry(taskId, outcomeId, new Date()));
        }

        public List<String> getExecutionPath() {
            return history.stream()
                    .map(entry -> entry.getTaskId() + ":" + entry.getOutcomeId())
                    .collect(Collectors.toList());
        }

        public void markTaskExecuted(String taskId) {
            executedTasks.add(taskId);
        }

        public boolean isTaskExecuted(String taskId) {
            return executedTasks.contains(taskId);
        }

        /**
         * 记录决策偏差
         */
        public void recordDecisionDeviation(String taskId, String recommendedOutcomeId, String actualOutcomeId) {
            decisionDeviations.put(taskId, recommendedOutcomeId);
        }

        /**
         * 记录偏差原因
         */
        public void recordDeviationReason(String taskId, String reason) {
            if (reason != null && !reason.trim().isEmpty()) {
                deviationReasons.put(taskId, reason);
            }
        }

        /**
         * 获取推荐的结果ID
         */
        public String getRecommendedOutcome(String taskId) {
            return decisionDeviations.get(taskId);
        }

        /**
         * 获取偏差原因
         */
        public String getDeviationReason(String taskId) {
            return deviationReasons.get(taskId);
        }

        /**
         * 检查任务是否有决策偏差
         */
        public boolean hasDecisionDeviation(String taskId) {
            return decisionDeviations.containsKey(taskId);
        }

        /**
         * 获取所有决策偏差
         */
        public Map<String, Object> getDecisionDeviationsInfo() {
            Map<String, Object> info = new HashMap<>();
            info.put("deviations", new HashMap<>(decisionDeviations));
            info.put("reasons", new HashMap<>(deviationReasons));
            return info;
        }
    }

    /**
     * 执行历史条目
     */
    class ExecutionHistoryEntry {
        private String taskId;
        private String outcomeId;
        private Date timestamp;

        public ExecutionHistoryEntry(String taskId, String outcomeId, Date timestamp) {
            this.taskId = taskId;
            this.outcomeId = outcomeId;
            this.timestamp = timestamp;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getOutcomeId() {
            return outcomeId;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 执行单个任务
     * 
     * @param task    任务
     * @param context 执行上下文
     * @return 任务执行结果
     */
    private TaskOutcome executeTask(TaskNode task, ExecutionContext context) {
        System.out.println("执行任务: " + task.getName() + " (ID: " + task.getId() + ")");

        // 根据任务类型执行不同的逻辑
        switch (task.getType()) {
            case "MultiAttributeComparison":
                return executeMultiAttributeComparison(task, context);
            case "DataAnalysis":
                return executeDataAnalysis(task, context);
            case "DeepAnalysis":
                return executeDeepAnalysis(task, context);
            case "CustomScript":
                return executeCustomScript(task, context);
            default:
                throw new RuntimeException("不支持的任务类型: " + task.getType());
        }
    }

    /**
     * 执行多属性比较任务
     */
    private TaskOutcome executeMultiAttributeComparison(TaskNode task, ExecutionContext context) {
        try {
            // 1. 获取任务参数
            List<String> timeRanges = getListParam(task, "timeRanges");
            List<String> attributes = getListParam(task, "attributes");
            String aggregationMethodStr = getStringParam(task, "aggregationMethod", "AVERAGE");
            AggregationMethod aggregationMethod = AggregationMethod.valueOf(aggregationMethodStr.toUpperCase());

            // 2. 执行比较
            Map<String, Map<String, Object>> comparisonResults = new HashMap<>();

            // 对每个时间范围进行分析
            for (String timeRange : timeRanges) {
                String[] parts = timeRange.split("/");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("时间范围格式错误: " + timeRange);
                }

                String startDate = parts[0];
                String endDate = parts[1];

                Map<String, Object> rangeResults = new HashMap<>();

                // 对每个属性进行分析
                for (String attribute : attributes) {
                    // 查询数据
                    List<Map<String, Object>> dataPoints = queryDataPoints(startDate, endDate, attribute);

                    // 根据聚合方法计算结果
                    Object value = aggregateValues(dataPoints, attribute, aggregationMethod);

                    // 存储结果
                    rangeResults.put(attribute, value);
                }

                comparisonResults.put(timeRange, rangeResults);
            }

            // 3. 将结果添加到上下文
            context.setVariable("comparisonResults", comparisonResults);

            // 4. 创建结果分析数据，包括增长率、变化趋势等
            Map<String, Object> analysisResults = new HashMap<>();

            // 如果有两个时间段，计算变化率
            if (timeRanges.size() >= 2) {
                Map<String, Object> changeRates = new HashMap<>();
                Map<String, String> trends = new HashMap<>();

                String firstRange = timeRanges.get(0);
                String secondRange = timeRanges.get(1);

                Map<String, Object> firstResults = comparisonResults.get(firstRange);
                Map<String, Object> secondResults = comparisonResults.get(secondRange);

                for (String attribute : attributes) {
                    if (firstResults.containsKey(attribute) && secondResults.containsKey(attribute)) {
                        double firstValue = convertToDouble(firstResults.get(attribute));
                        double secondValue = convertToDouble(secondResults.get(attribute));

                        // 计算变化率
                        double changeRate = 0;
                        if (firstValue != 0) {
                            changeRate = (secondValue - firstValue) / firstValue * 100;
                        }

                        // 确定趋势
                        String trend;
                        if (Math.abs(changeRate) < 5) {
                            trend = "持平";
                        } else if (changeRate > 0) {
                            trend = "上升";
                        } else {
                            trend = "下降";
                        }

                        changeRates.put(attribute, changeRate);
                        trends.put(attribute, trend);

                        // 特别保存能量变化率，方便条件评估
                        if (attribute.equals("energyRelease")) {
                            context.setVariable("energyChange", changeRate);
                        }
                    }
                }

                analysisResults.put("changeRates", changeRates);
                analysisResults.put("trends", trends);
            }

            context.setVariable("analysisResults", analysisResults);

            // 5. 确定适当的执行结果
            for (TaskOutcome outcome : task.getPossibleOutcomes()) {
                String condition = outcome.getCondition();
                if (evaluateCondition(condition, context)) {
                    // 设置结果数据
                    outcome.getResultData().put("comparisonResults", comparisonResults);
                    outcome.getResultData().put("analysisResults", analysisResults);
                    return outcome;
                }
            }

            // 默认返回第一个结果
            TaskOutcome defaultOutcome = task.getPossibleOutcomes().get(0);
            defaultOutcome.getResultData().put("comparisonResults", comparisonResults);
            defaultOutcome.getResultData().put("analysisResults", analysisResults);
            return defaultOutcome;

        } catch (Exception e) {
            System.err.println("执行多属性比较任务失败: " + e.getMessage());
            e.printStackTrace();

            // 如果有错误结果，返回错误结果
            for (TaskOutcome outcome : task.getPossibleOutcomes()) {
                if ("error".equals(outcome.getId())) {
                    outcome.getResultData().put("error", e.getMessage());
                    return outcome;
                }
            }

            // 否则创建一个错误结果
            TaskOutcome errorOutcome = new TaskOutcome("error", "执行错误", null);
            errorOutcome.getResultData().put("error", e.getMessage());
            return errorOutcome;
        }
    }

    /**
     * 执行数据分析任务
     */
    @SuppressWarnings("unchecked")
    private TaskOutcome executeDataAnalysis(TaskNode task, ExecutionContext context) {
        try {
            // 获取比较结果
            Map<String, Map<String, Object>> comparisonResults = (Map<String, Map<String, Object>>) context
                    .getVariable("comparisonResults");

            Map<String, Object> analysisResults = (Map<String, Object>) context.getVariable("analysisResults");

            // 进行更深入的分析
            Map<String, Object> detailedAnalysis = new HashMap<>();

            // 例如，分析能量值是否超过阈值
            Map<String, Object> thresholdAnalysis = new HashMap<>();

            for (Map.Entry<String, Map<String, Object>> entry : comparisonResults.entrySet()) {
                String timeRange = entry.getKey();
                Map<String, Object> values = entry.getValue();

                Map<String, Object> rangeAnalysis = new HashMap<>();

                for (Map.Entry<String, Object> valueEntry : values.entrySet()) {
                    String attribute = valueEntry.getKey();
                    double value = convertToDouble(valueEntry.getValue());

                    Map<String, Object> attributeAnalysis = new HashMap<>();
                    attributeAnalysis.put("value", value);

                    String status;
                    if (attribute.equals("energyRelease") || attribute.equals("能量阈值")) {
                        if (value > 4.5) {
                            status = "高";
                        } else if (value > 3.0) {
                            status = "中";
                        } else {
                            status = "低";
                        }
                    } else if (attribute.equals("microseismicEventCount")) {
                        if (value > 10) {
                            status = "高";
                        } else if (value > 5) {
                            status = "中";
                        } else {
                            status = "低";
                        }
                    } else {
                        status = "正常";
                    }

                    attributeAnalysis.put("status", status);
                    rangeAnalysis.put(attribute, attributeAnalysis);
                }

                thresholdAnalysis.put(timeRange, rangeAnalysis);
            }

            detailedAnalysis.put("thresholdAnalysis", thresholdAnalysis);

            // 生成分析建议
            String recommendation = generateRecommendation(comparisonResults, analysisResults);
            detailedAnalysis.put("recommendation", recommendation);

            // 将结果添加到上下文
            context.setVariable("detailedAnalysis", detailedAnalysis);

            // 确定适当的执行结果
            for (TaskOutcome outcome : task.getPossibleOutcomes()) {
                String condition = outcome.getCondition();
                if (evaluateCondition(condition, context)) {
                    // 设置结果数据
                    outcome.getResultData().put("detailedAnalysis", detailedAnalysis);
                    return outcome;
                }
            }

            // 默认返回第一个结果
            TaskOutcome defaultOutcome = task.getPossibleOutcomes().get(0);
            defaultOutcome.getResultData().put("detailedAnalysis", detailedAnalysis);
            return defaultOutcome;

        } catch (Exception e) {
            System.err.println("执行数据分析任务失败: " + e.getMessage());
            e.printStackTrace();

            // 返回错误结果
            TaskOutcome errorOutcome = new TaskOutcome("error", "执行错误", null);
            errorOutcome.getResultData().put("error", e.getMessage());
            return errorOutcome;
        }
    }

    /**
     * 执行深度分析任务
     */
    @SuppressWarnings("unchecked")
    private TaskOutcome executeDeepAnalysis(TaskNode task, ExecutionContext context) {
        try {
            // 获取前面任务的分析结果
            Map<String, Object> detailedAnalysis = (Map<String, Object>) context.getVariable("detailedAnalysis");

            // 进行进一步的分析...
            Map<String, Object> deepAnalysisResults = new HashMap<>();

            // 这里可以实现更复杂的分析逻辑
            deepAnalysisResults.put("severity", calculateSeverity(detailedAnalysis));
            deepAnalysisResults.put("actionRequired", determineAction(detailedAnalysis));
            deepAnalysisResults.put("priority", determinePriority(detailedAnalysis));

            // 添加到上下文
            context.setVariable("deepAnalysisResults", deepAnalysisResults);

            // 确定适当的执行结果
            for (TaskOutcome outcome : task.getPossibleOutcomes()) {
                String condition = outcome.getCondition();
                if (evaluateCondition(condition, context)) {
                    outcome.getResultData().put("deepAnalysisResults", deepAnalysisResults);
                    return outcome;
                }
            }

            // 默认返回第一个结果
            TaskOutcome defaultOutcome = task.getPossibleOutcomes().get(0);
            defaultOutcome.getResultData().put("deepAnalysisResults", deepAnalysisResults);
            return defaultOutcome;

        } catch (Exception e) {
            System.err.println("执行深度分析任务失败: " + e.getMessage());
            e.printStackTrace();

            TaskOutcome errorOutcome = new TaskOutcome("error", "执行错误", null);
            errorOutcome.getResultData().put("error", e.getMessage());
            return errorOutcome;
        }
    }

    /**
     * 执行自定义脚本任务
     */
    private TaskOutcome executeCustomScript(TaskNode task, ExecutionContext context) {
        try {
            // 获取脚本
            String script = getStringParam(task, "script", "");

            // 执行脚本（这里是示例，实际实现需要根据具体需求）
            Map<String, Object> scriptResult = evaluateScript(script, context);

            // 添加到上下文
            context.setVariable("scriptResult", scriptResult);

            // 确定适当的执行结果
            for (TaskOutcome outcome : task.getPossibleOutcomes()) {
                String condition = outcome.getCondition();
                if (evaluateCondition(condition, context)) {
                    outcome.getResultData().put("scriptResult", scriptResult);
                    return outcome;
                }
            }

            // 默认返回第一个结果
            TaskOutcome defaultOutcome = task.getPossibleOutcomes().get(0);
            defaultOutcome.getResultData().put("scriptResult", scriptResult);
            return defaultOutcome;

        } catch (Exception e) {
            System.err.println("执行自定义脚本任务失败: " + e.getMessage());
            e.printStackTrace();

            TaskOutcome errorOutcome = new TaskOutcome("error", "执行错误", null);
            errorOutcome.getResultData().put("error", e.getMessage());
            return errorOutcome;
        }
    }

    /**
     * 查询数据点
     */
    private List<Map<String, Object>> queryDataPoints(String startDate, String endDate, String attribute) {
        // 构建SPARQL查询
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        queryBuilder.append("PREFIX ont: <http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#>\n");
        queryBuilder.append("SELECT ?event ?").append(attribute).append(" ?description WHERE {\n");
        queryBuilder.append("  ?event rdf:type ont:RockBurst .\n");
        queryBuilder.append("  ?event ont:").append(attribute).append(" ?").append(attribute).append(" .\n");
        queryBuilder.append("  ?event ont:description ?description .\n");

        // 添加日期过滤
        queryBuilder.append(buildExactDateRangeFilter(startDate, endDate));

        queryBuilder.append("} ORDER BY ?description");

        System.out.println("查询数据点: " + queryBuilder.toString());

        // 执行查询
        return sparqlService.executeQuery(queryBuilder.toString());
    }

    /**
     * 构建精确的日期范围过滤条件
     */
    private String buildExactDateRangeFilter(String startDate, String endDate) {
        try {
            // 解析开始日期
            String[] startParts = startDate.split("-");
            String startYear = startParts[0];
            String startMonth = startParts[1];
            String startDay = startParts[2];

            // 解析结束日期
            String[] endParts = endDate.split("-");
            String endYear = endParts[0];
            String endMonth = endParts[1];
            String endDay = endParts[2];

            // 构建精确的日期范围匹配
            StringBuilder filterBuilder = new StringBuilder();

            String exactPattern = "从" + startYear + "年" + startMonth + "月" + startDay + "日.*至" +
                    endYear + "年" + endMonth + "月" + endDay + "日";

            filterBuilder.append("  FILTER(REGEX(STR(?description), \"" + exactPattern + "\", \"i\"))\n");

            return filterBuilder.toString();
        } catch (Exception e) {
            System.err.println("日期格式转换错误: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 根据聚合方法聚合值
     */
    private Object aggregateValues(List<Map<String, Object>> dataPoints, String attribute, AggregationMethod method) {
        // 提取所有值
        List<Object> values = dataPoints.stream()
                .map(point -> point.get(attribute))
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return 0.0;
        }

        // 转换为数值
        List<Double> numericValues = values.stream()
                .map(this::convertToDouble)
                .collect(Collectors.toList());

        // 根据聚合方法计算
        switch (method) {
            case AVERAGE:
                return numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX:
                return numericValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case MIN:
                return numericValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case SUM:
                return numericValues.stream().mapToDouble(Double::doubleValue).sum();
            case LATEST:
                return values.get(values.size() - 1);
            case FIRST:
                return values.get(0);
            default:
                return numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }

    /**
     * 将对象转换为double
     */
    private double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, ExecutionContext context) {
        // 简单的条件评估实现
        // 在实际实现中，可以使用JavaScript引擎或表达式引擎

        // 如果条件为空或"true"，返回true
        if (condition == null || condition.isEmpty() || "true".equals(condition)) {
            return true;
        }

        // 这里是一个非常简化的条件评估示例
        if (condition.startsWith("energyChange > ")) {
            double threshold = Double.parseDouble(condition.substring("energyChange > ".length()));
            double energyChange = (double) context.getVariable("energyChange");
            return energyChange > threshold;
        } else if (condition.startsWith("energyChange < ")) {
            double threshold = Double.parseDouble(condition.substring("energyChange < ".length()));
            double energyChange = (double) context.getVariable("energyChange");
            return energyChange < threshold;
        }

        // 默认返回false
        return false;
    }

    /**
     * 生成分析建议
     */
    @SuppressWarnings("unchecked")
    private String generateRecommendation(Map<String, Map<String, Object>> comparisonResults,
            Map<String, Object> analysisResults) {
        StringBuilder recommendation = new StringBuilder();

        try {
            // 获取趋势
            Map<String, String> trends = (Map<String, String>) analysisResults.get("trends");

            if (trends != null) {
                // 能量释放趋势
                if (trends.containsKey("energyRelease")) {
                    String energyTrend = trends.get("energyRelease");
                    Map<String, Object> changeRates = (Map<String, Object>) analysisResults.get("changeRates");
                    double energyChangeRate = convertToDouble(changeRates.get("energyRelease"));

                    if ("上升".equals(energyTrend)) {
                        if (energyChangeRate > 30) {
                            recommendation.append("能量释放显著增加(").append(String.format("%.1f", energyChangeRate))
                                    .append("%)，建议进行详细检查并加强监测。\n");
                        } else {
                            recommendation.append("能量释放有所增加(").append(String.format("%.1f", energyChangeRate))
                                    .append("%)，建议关注变化趋势。\n");
                        }
                    } else if ("下降".equals(energyTrend)) {
                        recommendation.append("能量释放呈下降趋势(").append(String.format("%.1f", Math.abs(energyChangeRate)))
                                .append("%)，情况趋于稳定。\n");
                    } else {
                        recommendation.append("能量释放基本稳定，无明显变化。\n");
                    }
                }

                // 微震事件数趋势
                if (trends.containsKey("microseismicEventCount")) {
                    String countTrend = trends.get("microseismicEventCount");
                    Map<String, Object> changeRates = (Map<String, Object>) analysisResults.get("changeRates");
                    double countChangeRate = convertToDouble(changeRates.get("microseismicEventCount"));

                    if ("上升".equals(countTrend)) {
                        if (countChangeRate > 30) {
                            recommendation.append("微震事件数量显著增加(").append(String.format("%.1f", countChangeRate))
                                    .append("%)，建议提高预警级别并进行隐患排查。\n");
                        } else {
                            recommendation.append("微震事件数量有所增加(").append(String.format("%.1f", countChangeRate))
                                    .append("%)，建议保持监测。\n");
                        }
                    } else if ("下降".equals(countTrend)) {
                        recommendation.append("微震事件数量减少(").append(String.format("%.1f", Math.abs(countChangeRate)))
                                .append("%)，情况有所改善。\n");
                    } else {
                        recommendation.append("微震事件数量保持稳定。\n");
                    }
                }
            } else {
                recommendation.append("暂无足够数据生成建议。请确保提供足够的历史数据进行比较分析。");
            }
        } catch (Exception e) {
            recommendation.append("生成建议时发生错误: ").append(e.getMessage());
        }

        return recommendation.toString();
    }

    /**
     * 计算事件严重性
     */
    private String calculateSeverity(Map<String, Object> analysis) {
        // 实现严重性计算逻辑
        return "中等";
    }

    /**
     * 确定需要采取的行动
     */
    private String determineAction(Map<String, Object> analysis) {
        // 实现行动确定逻辑
        return "增加监测频率";
    }

    /**
     * 确定优先级
     */
    private int determinePriority(Map<String, Object> analysis) {
        // 实现优先级确定逻辑
        return 2;
    }

    /**
     * 执行脚本
     */
    private Map<String, Object> evaluateScript(String script, ExecutionContext context) {
        // 简单实现 - 实际项目中可能需要更安全的脚本执行引擎
        Map<String, Object> result = new HashMap<>();
        result.put("executed", true);
        result.put("message", "脚本执行成功");
        return result;
    }

    /**
     * 获取字符串参数
     */
    private String getStringParam(TaskNode task, String paramName, String defaultValue) {
        Object value = task.getInputParameters().get(paramName);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取列表参数
     */
    @SuppressWarnings("unchecked")
    private List<String> getListParam(TaskNode task, String paramName) {
        Object value = task.getInputParameters().get(paramName);

        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof List) {
            return (List<String>) value;
        }

        if (value instanceof String) {
            return Arrays.asList(((String) value).split(","));
        }

        return new ArrayList<>();
    }

    /**
     * 根据任务类型执行相应逻辑
     * 此方法在selectOutcome中被调用，用于处理用户选择的结果
     */
    private void executeTaskByType(TaskNode task, TaskOutcome outcome, ExecutionContext context) {
        switch (task.getType()) {
            case "MultiAttributeComparison":
                // 如果任务未执行过，则执行任务
                if (!context.isTaskExecuted(task.getId())) {
                    // 这里我们不需要获取返回值，因为我们只是想确保任务被执行
                    // 结果会被存储在context中
                    executeMultiAttributeComparison(task, context);
                }
                break;

            case "DataAnalysis":
                if (!context.isTaskExecuted(task.getId())) {
                    executeDataAnalysis(task, context);
                }
                break;

            case "DeepAnalysis":
                if (!context.isTaskExecuted(task.getId())) {
                    executeDeepAnalysis(task, context);
                }
                break;

            case "CustomScript":
                if (!context.isTaskExecuted(task.getId())) {
                    executeCustomScript(task, context);
                }
                break;

            default:
                System.out.println("未知任务类型: " + task.getType() + "，不执行额外处理");
                break;
        }
    }

    /**
     * 获取推荐的结果ID
     * 这是一个全新的方法，需要完整添加到类中
     */
    private String getRecommendedOutcome(TaskNode task, ExecutionContext context) {
        if (task.getType().equals("MultiAttributeComparison")) {
            // 获取能量变化率
            Object energyChangeObj = context.getVariable("energyChange");
            if (energyChangeObj instanceof Double) {
                double energyChange = (Double) energyChangeObj;

                // 基于能量变化率推荐选项
                List<TaskOutcome> outcomes = task.getPossibleOutcomes();
                for (TaskOutcome outcome : outcomes) {
                    if (energyChange > 10 && (outcome.getName().contains("能量增加") ||
                            (outcome.getDescription() != null && outcome.getDescription().contains("能量增加")))) {
                        return outcome.getId();
                    } else if (energyChange < -10 && (outcome.getName().contains("能量减少") ||
                            (outcome.getDescription() != null && outcome.getDescription().contains("能量减少")))) {
                        return outcome.getId();
                    } else if (Math.abs(energyChange) <= 10 && (outcome.getName().contains("能量稳定") ||
                            (outcome.getDescription() != null && outcome.getDescription().contains("能量稳定")))) {
                        return outcome.getId();
                    }
                }
            }
        }

        // 其他类型的任务或无法确定推荐
        return null;
    }

    /**
     * 获取推荐理由
     * 这是一个全新的方法，需要完整添加到类中
     */
    private String getRecommendationReason(TaskNode task, ExecutionContext context) {
        if (task.getType().equals("MultiAttributeComparison")) {
            Object energyChangeObj = context.getVariable("energyChange");
            if (energyChangeObj instanceof Double) {
                double energyChange = (Double) energyChangeObj;

                if (energyChange > 10) {
                    return String.format("能量释放增加了%.2f%%，明显高于10%%的变化阈值", energyChange);
                } else if (energyChange < -10) {
                    return String.format("能量释放减少了%.2f%%，明显低于-10%%的变化阈值", Math.abs(energyChange));
                } else {
                    return String.format("能量释放变化为%.2f%%，在±10%%的稳定范围内", energyChange);
                }
            }
        }

        return "基于当前数据分析结果";
    }
}