package com.example.rdfsearch.controller;

import com.example.rdfsearch.model.Task;
import com.example.rdfsearch.model.task.Workflow;
import com.example.rdfsearch.service.TaskChainService;
import com.example.rdfsearch.service.TaskExecutionService;
import com.example.rdfsearch.service.TaskResultProcessor;
import com.example.rdfsearch.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = { "http://localhost:8080" }, allowedHeaders = "*", methods = { RequestMethod.GET,
        RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS }, maxAge = 3600)

public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskResultProcessor taskResultProcessor;

    /**
     * 获取所有任务
     */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            List<Task> tasks = taskService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("获取所有任务失败: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 通过URI获取任务（使用URL编码的URI）
     */
    @GetMapping("/{encodedUri}")
    public ResponseEntity<?> getTaskByUri(@PathVariable String encodedUri, HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            String uri = URLDecoder.decode(encodedUri, "UTF-8");
            Task task = taskService.getTaskByUri(uri);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(task);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL解码失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("获取任务失败: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建新任务
     */
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task, HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            logger.info("接收到创建任务请求: {}", task);

            // 确保新任务没有URI，让系统自动生成
            task.setUri(null);

            Task createdTask = taskService.saveTask(task);
            logger.info("任务创建成功: URI={}", createdTask.getUri());

            return ResponseEntity.ok(createdTask);
        } catch (Exception e) {
            logger.error("创建任务失败: " + e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 提供替代的删除方法（使用POST方法）
     */
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTaskByPost(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        System.out.println("收到POST删除请求: " + request.getRequestURI());
        System.out.println("请求体内容: " + payload);

        try {
            String uri = payload.get("uri");
            System.out.println("URI值: " + uri);

            if (uri == null || uri.isEmpty()) {
                System.out.println("URI为空");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing uri parameter"));
            }

            System.out.println("需要删除的任务URI: " + uri);

            Task task = taskService.getTaskByUri(uri);
            if (task == null) {
                System.out.println("找不到要删除的任务: " + uri);
                return ResponseEntity.notFound().build();
            }

            System.out.println("开始删除任务: " + uri);
            taskService.deleteTask(uri);
            System.out.println("任务删除成功");

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("删除任务失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新任务（使用URL编码的URI）
     */
    @PutMapping("/{encodedUri}")
    public ResponseEntity<?> updateTask(@PathVariable String encodedUri, @RequestBody Task task,
            HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            String uri = URLDecoder.decode(encodedUri, "UTF-8");
            if (taskService.getTaskByUri(uri) == null) {
                return ResponseEntity.notFound().build();
            }

            task.setUri(uri); // 确保URI一致
            Task updatedTask = taskService.saveTask(task);
            return ResponseEntity.ok(updatedTask);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL解码失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("更新任务失败: " + e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 删除任务（使用URL编码的URI）
     */
    @DeleteMapping("/{encodedUri}")
    public ResponseEntity<?> deleteTask(@PathVariable String encodedUri, HttpServletRequest request) {
        System.out.println("收到删除请求 - 原始编码URI: " + encodedUri);
        try {
            // 使用StandardCharsets.UTF_8
            String uri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
            System.out.println("解码后URI: " + uri);

            // 查找任务
            Task task = taskService.getTaskByUri(uri);
            if (task == null) {
                System.out.println("找不到任务: " + uri);
                return ResponseEntity.notFound().build();
            }

            System.out.println("开始删除任务: " + uri);
            taskService.deleteTask(uri);
            System.out.println("任务删除成功");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("删除失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 执行任务（使用URL编码的URI）
     */
    @GetMapping("/{encodedUri}/execute")
    public ResponseEntity<?> executeTask(@PathVariable String encodedUri, HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            String uri = URLDecoder.decode(encodedUri, "UTF-8");
            logger.info("收到执行任务请求，URI: {}", uri);

            // 1. 获取任务 - 强制从数据库重新加载最新数据
            Task task = taskService.getTaskByUri(uri);
            if (task == null) {
                logger.error("未找到任务，URI: {}", uri);
                return ResponseEntity.notFound().build();
            }

            // 打印详细的任务信息用于调试
            logger.info("找到任务: URI={}, Type={}, 参数: {}", task.getUri(), task.getType(), task.getParameters());

            // 如果是历史比较任务，特别记录日期参数
            if ("HistoricalComparison".equals(task.getType()) && task.getParameters() != null) {
                String periodOne = task.getParameters().containsKey("periodOne")
                        ? task.getParameters().get("periodOne").toString()
                        : "未指定";
                String periodTwo = task.getParameters().containsKey("periodTwo")
                        ? task.getParameters().get("periodTwo").toString()
                        : "未指定";
                logger.info("历史比较任务: 周期1={}, 周期2={}", periodOne, periodTwo);
            }

            // 2. 执行任务
            Map<String, Object> executionResult = taskExecutionService.execute(task);

            // 3. 处理结果
            Map<String, Object> processedResult = taskResultProcessor.processTaskResult(
                    task.getType(), executionResult);

            // 4. 返回处理后的结果
            return ResponseEntity.ok(processedResult);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL解码失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("执行任务失败: URI={}, 错误={}", encodedUri, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取可用的任务类型
     */
    @GetMapping("/types")
    public ResponseEntity<?> getTaskTypes(HttpServletRequest request) {
        System.out.println("收到请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            // 定义支持的任务类型及其描述
            Map<String, Object> taskTypes = new HashMap<>();

            Map<String, Object> searchEventType = new HashMap<>();
            searchEventType.put("name", "SearchEvent");
            searchEventType.put("description", "搜索微震事件");
            searchEventType.put("parameterSchema", getSearchEventParameterSchema());

            Map<String, Object> riskAnalysisType = new HashMap<>();
            riskAnalysisType.put("name", "RiskAnalysis");
            riskAnalysisType.put("description", "区域风险分析");
            riskAnalysisType.put("parameterSchema", getRiskAnalysisParameterSchema());

            Map<String, Object> historicalComparisonType = new HashMap<>();
            historicalComparisonType.put("name", "HistoricalComparison");
            historicalComparisonType.put("description", "历史数据对比");
            historicalComparisonType.put("parameterSchema", getHistoricalComparisonParameterSchema());

            taskTypes.put("SearchEvent", searchEventType);
            taskTypes.put("RiskAnalysis", riskAnalysisType);
            taskTypes.put("HistoricalComparison", historicalComparisonType);

            return ResponseEntity.ok(taskTypes);
        } catch (Exception e) {
            logger.error("获取任务类型失败: " + e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 参数模式方法保持不变...
    private Map<String, Object> getSearchEventParameterSchema() {
        Map<String, Object> schema = new HashMap<>();

        Map<String, Object> energyThreshold = new HashMap<>();
        energyThreshold.put("type", "number");
        energyThreshold.put("description", "能量阈值 (J)");
        energyThreshold.put("default", 1000);
        energyThreshold.put("min", 0);
        energyThreshold.put("max", 10000);

        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("type", "dateRange");
        timeRange.put("description", "时间范围");

        Map<String, Object> location = new HashMap<>();
        location.put("type", "string");
        location.put("description", "位置筛选");

        schema.put("energyThreshold", energyThreshold);
        schema.put("timeRange", timeRange);
        schema.put("location", location);

        return schema;
    }

    private Map<String, Object> getRiskAnalysisParameterSchema() {
        Map<String, Object> schema = new HashMap<>();

        Map<String, Object> areaId = new HashMap<>();
        areaId.put("type", "string");
        areaId.put("description", "隧道区段ID");

        Map<String, Object> analysisFactors = new HashMap<>();
        analysisFactors.put("type", "array");
        analysisFactors.put("description", "分析因素");
        analysisFactors.put("items", new String[] { "energy", "frequency", "geology" });
        analysisFactors.put("default", new String[] { "energy", "frequency" });

        schema.put("areaId", areaId);
        schema.put("analysisFactors", analysisFactors);

        return schema;
    }

    private Map<String, Object> getHistoricalComparisonParameterSchema() {
        Map<String, Object> schema = new HashMap<>();

        Map<String, Object> periodOne = new HashMap<>();
        periodOne.put("type", "dateRange");
        periodOne.put("description", "第一时间段");

        Map<String, Object> periodTwo = new HashMap<>();
        periodTwo.put("type", "dateRange");
        periodTwo.put("description", "第二时间段");

        Map<String, Object> comparisonMetric = new HashMap<>();
        comparisonMetric.put("type", "string");
        comparisonMetric.put("description", "对比指标");
        comparisonMetric.put("options", new String[] { "count", "totalEnergy", "maxEnergy" });
        comparisonMetric.put("default", "count");

        schema.put("periodOne", periodOne);
        schema.put("periodTwo", periodTwo);
        schema.put("comparisonMetric", comparisonMetric);

        return schema;
    }

    /**
     * 通过ID执行任务（简化版）
     */
    @GetMapping("/execute-by-id/{taskId}")
    public ResponseEntity<?> executeTaskById(@PathVariable String taskId) {
        try {
            String uri = "http://example.com/task/" + taskId;
            logger.info("通过ID执行任务，ID: {}, URI: {}", taskId, uri);

            // 1. 强制重新从数据库获取最新任务数据
            Task task = null;

            // 尝试多次获取，确保数据是最新的
            for (int i = 0; i < 2; i++) {
                task = taskService.getTaskByUri(uri);
                if (task != null) {
                    break;
                }
                // 短暂延迟后重试
                Thread.sleep(100);
            }

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            // 打印详细的任务信息用于调试
            logger.info("获取到任务: ID={}, Type={}, 参数: {}", taskId, task.getType(), task.getParameters());

            // 如果是历史比较任务，特别记录日期参数
            if ("HistoricalComparison".equals(task.getType()) && task.getParameters() != null) {
                String periodOne = task.getParameters().containsKey("periodOne")
                        ? task.getParameters().get("periodOne").toString()
                        : "未指定";
                String periodTwo = task.getParameters().containsKey("periodTwo")
                        ? task.getParameters().get("periodTwo").toString()
                        : "未指定";
                logger.info("历史比较任务: 周期1={}, 周期2={}", periodOne, periodTwo);
            }

            // 2. 执行任务
            Map<String, Object> executionResult = taskExecutionService.execute(task);

            // 3. 处理结果
            Map<String, Object> processedResult = taskResultProcessor.processTaskResult(
                    task.getType(), executionResult);

            // 4. 返回处理后的结果
            return ResponseEntity.ok(processedResult);
        } catch (Exception e) {
            logger.error("执行任务失败: ID={}, 错误={}", taskId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 通过ID更新任务（简化版）
     */
    @PutMapping("/update-by-id/{taskId}")
    public ResponseEntity<?> updateTaskById(@PathVariable String taskId, @RequestBody Task task,
            HttpServletRequest request) {
        try {
            String uri = "http://example.com/task/" + taskId;
            logger.info("通过ID更新任务，ID: {}, URI: {}", taskId, uri);

            // 检查任务是否存在
            if (taskService.getTaskByUri(uri) == null) {
                return ResponseEntity.notFound().build();
            }

            // 确保URI一致
            task.setUri(uri);

            // 保存更新后的任务
            Task updatedTask = taskService.saveTask(task);
            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            logger.error("更新任务失败: ID={}, 错误={}", taskId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 通过ID获取任务（简化版）
     */
    @GetMapping("/get-by-id/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable String taskId) {
        try {
            String uri = "http://example.com/task/" + taskId;
            logger.info("通过ID获取任务，ID: {}, URI: {}", taskId, uri);

            // 获取任务
            Task task = taskService.getTaskByUri(uri);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(task);
        } catch (Exception e) {
            logger.error("获取任务失败: ID={}, 错误={}", taskId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Autowired
    private TaskChainService taskChainService;

    /**
     * 获取可用工作流列表
     */
    @GetMapping("/workflows")
    public ResponseEntity<?> getAvailableWorkflows() {
        try {
            // 获取可用工作流列表
            Map<String, Object> response = new HashMap<>();

            // 示例数据 - 实际应从taskChainService获取
            List<Map<String, Object>> workflows = new ArrayList<>();

            Map<String, Object> workflow1 = new HashMap<>();
            workflow1.put("id", "workflow1");
            workflow1.put("name", "能量阈值分析工作流");
            workflow1.put("description", "分析不同时间段的能量阈值变化并提供相应的分析和建议");
            workflows.add(workflow1);

            Map<String, Object> workflow2 = new HashMap<>();
            workflow2.put("id", "workflow2");
            workflow2.put("name", "多震点分析工作流");
            workflow2.put("description", "分析多个震点的数据并生成综合报告");
            workflows.add(workflow2);

            response.put("workflows", workflows);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取工作流列表失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<?> getWorkflowDetails(@PathVariable String workflowId) {
        try {
            // 获取工作流详情
            Workflow workflow = taskChainService.getWorkflowDefinition(workflowId);

            if (workflow == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            logger.error("获取工作流详情失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 执行工作流
     */
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<?> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> parameters) {
        try {
            // 如果没有提供参数，使用空Map
            if (parameters == null) {
                parameters = new HashMap<>();
            }

            // 执行工作流
            Map<String, Object> result = taskChainService.executeWorkflow(workflowId, parameters);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("执行工作流失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取工作流执行状态
     */
    @GetMapping("/workflows/{workflowId}/status")
    public ResponseEntity<?> getWorkflowStatus(@PathVariable String workflowId) {
        try {
            // 获取工作流执行状态
            // 实际应从taskChainService获取
            Map<String, Object> status = new HashMap<>();
            status.put("workflowId", workflowId);
            status.put("status", "completed");
            status.put("executionPath", Arrays.asList("task1:outcome1_1", "task2:outcome2_1"));

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取工作流状态失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 使用请求体中的任务URI更新任务
     */
    @PutMapping
    public ResponseEntity<?> updateTaskWithoutUri(@RequestBody Task task, HttpServletRequest request) {
        System.out.println("收到直接更新请求: " + request.getMethod() + " " + request.getRequestURI());
        try {
            if (task.getUri() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "任务URI不能为空"));
            }

            // 添加timeRange的特别监控
            if (task.getParameters() != null && task.getParameters().containsKey("timeRange")) {
                System.out.println("更新前timeRange参数值: " + task.getParameters().get("timeRange"));
            } else {
                System.out.println("更新前timeRange参数不存在!");
            }

            if (taskService.getTaskByUri(task.getUri()) == null) {
                return ResponseEntity.notFound().build();
            }

            Task updatedTask = taskService.saveTask(task);

            // 验证保存后的参数
            if (updatedTask.getParameters() != null && updatedTask.getParameters().containsKey("timeRange")) {
                System.out.println("更新后返回的timeRange参数值: " + updatedTask.getParameters().get("timeRange"));
            } else {
                System.out.println("更新后返回的task中timeRange参数不存在!");
            }

            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            logger.error("更新任务失败: " + e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/workflows/{workflowId}/reset")
    public ResponseEntity<Map<String, Object>> resetWorkflow(@PathVariable String workflowId) {
        taskChainService.resetWorkflowContext(workflowId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工作流已重置");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务节点的所有可能选项，用于用户交互选择
     */
    @GetMapping("/workflows/{workflowId}/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskOptions(
            @PathVariable String workflowId,
            @PathVariable String taskId) {
        Map<String, Object> options = taskChainService.getTaskOptions(workflowId, taskId);
        return ResponseEntity.ok(options);
    }

    /**
     * 用户选择任务结果，前进到下一个任务
     */
    @PostMapping("/workflows/{workflowId}/tasks/{taskId}/select")
    public ResponseEntity<Map<String, Object>> selectOutcome(
            @PathVariable String workflowId,
            @PathVariable String taskId,
            @RequestBody Map<String, String> request) {
        String outcomeId = request.get("outcomeId");
        String contradictionReason = request.get("contradictionReason"); // 获取偏差原因

        Map<String, Object> result = taskChainService.selectOutcome(workflowId, taskId, outcomeId, contradictionReason);
        return ResponseEntity.ok(result);
    }

    /**
     * 设置工作流的源任务
     */
    @PostMapping("/workflows/{workflowId}/set-source")
    public ResponseEntity<?> setWorkflowSource(
            @PathVariable String workflowId,
            @RequestBody Map<String, String> request) {
        try {
            String sourceTaskId = request.get("sourceTaskId");

            // 调用已有的服务方法设置源任务
            taskChainService.setWorkflowSource(workflowId, sourceTaskId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已设置工作流源任务");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("设置工作流源任务失败: " + e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}