package com.example.rdfsearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.example.rdfsearch.model.Task;
import com.example.rdfsearch.model.TaskHistory;
import com.example.rdfsearch.repository.TaskHistoryRepository;
import com.example.rdfsearch.util.QueryTemplateUtil;

import java.util.*;
import jakarta.annotation.PostConstruct;

@Service
public class TaskService {

    @Autowired
    private SparqlService sparqlService;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    private static final String TASK_PREFIX = "http://example.com/task/";
    private static final String TASK_TYPE_PREFIX = "http://example.com/taskType/";

    private final Map<String, Task> taskMap = new HashMap<>();

    @PostConstruct
    public void init() {
        System.out.println("TaskService 初始化 - 同步RDF存储中的任务到内存");
        try {
            syncTasks();
        } catch (Exception e) {
            System.err.println("初始化同步任务失败: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanup() {
        System.out.println("开始定时清理任务");
        cleanupOldTasks(24);
    }

    public List<Task> findAll() {
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX task: <http://example.com/task/>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "SELECT ?task ?type ?label ?p ?o WHERE { " +
                "?task rdf:type ?type . " +
                "?task rdfs:label ?label . " +
                "?task ?p ?o . " +
                "FILTER(STRSTARTS(STR(?type), '" + TASK_TYPE_PREFIX + "')) " +
                "}";

        System.out.println("执行查询任务列表: " + query);
        List<Map<String, String>> results = sparqlService.query(query);
        System.out.println("查询结果数量: " + results.size());

        List<Task> tasks = convertResultsToTasks(results);
        System.out.println("转换后的任务数量: " + tasks.size());
        return tasks;
    }

    public Task findByUri(String uri) {
        System.out.println("findByUri 被调用，URI: " + uri);

        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX task: <http://example.com/task/>\n" +
                "SELECT ?type ?label ?p ?o WHERE { " +
                "<" + uri + "> rdf:type ?type . " +
                "<" + uri + "> rdfs:label ?label . " +
                "<" + uri + "> ?p ?o . " +
                "}";

        System.out.println("查询单个任务: " + query);
        List<Map<String, String>> results = sparqlService.query(query);
        System.out.println("查询结果数量: " + results.size());

        if (results.isEmpty()) {
            Task taskFromMap = taskMap.get(uri);
            if (taskFromMap != null) {
                return taskFromMap;
            }
            return null;
        }

        Task task = convertResultToTask(uri, results);
        if (task != null) {
            taskMap.put(uri, task);
        }

        return task;
    }

    public List<Task> getAllTasks() {
        List<Task> tasksFromRdf = findAll();
        Map<String, Task> taskMap = new HashMap<>();

        for (Task task : tasksFromRdf) {
            if (task.getUri() != null) {
                taskMap.put(task.getUri(), task);
            }
        }

        for (Task task : this.taskMap.values()) {
            if (task.getUri() != null) {
                taskMap.put(task.getUri(), task);
            }
        }

        List<Task> allTasks = new ArrayList<>(taskMap.values());
        System.out.println("返回任务总数: " + allTasks.size());

        return allTasks;
    }

    /**
     * 通过URI获取任务 - 改进版本，确保获取最新数据
     */
    public Task getTaskByUri(String uri) {
        System.out.println("查找任务 - URI: " + uri);

        // 首先尝试直接从RDF获取最新数据，跳过缓存
        Task taskFromRdf = findByUri(uri);
        if (taskFromRdf != null) {
            System.out.println("直接从RDF获取最新任务数据: " + taskFromRdf.getUri());

            // 重要：更新内存缓存
            taskMap.put(uri, taskFromRdf);

            // 同时保存完整版本到localStorage，方便前端使用
            // (此处仅为表示概念，后端不应操作localStorage)

            return taskFromRdf;
        }

        // 如果从RDF获取失败，尝试从内存缓存获取
        Task taskFromCache = taskMap.get(uri);
        if (taskFromCache != null) {
            System.out.println("从内存缓存中找到任务: " + taskFromCache.getUri());
            return taskFromCache;
        }

        System.out.println("未找到任务 - URI: " + uri);
        return null;
    }

    /**
     * 保存任务 - 改进版本，确保参数正确保存
     */
    public Task saveTask(Task task) {
        // 添加详细日志
        System.out.println("\n==== 开始保存任务 ====");
        System.out.println("任务URI: " + task.getUri());
        System.out.println("任务类型: " + task.getType());
        System.out.println("参数详情: " + task.getParameters());

        // 如果task.parameters为null，初始化它
        if (task.getParameters() == null) {
            task.setParameters(new HashMap<>());
        }

        // 原有代码保持不变
        if (task.getUri() == null || task.getUri().isEmpty()) {
            String uniquePart = UUID.randomUUID().toString();
            task.setUri(TASK_PREFIX + uniquePart);
        }

        if (task.getCreatedAt() == null) {
            task.setCreatedAt(new Date().toString());
        }
        task.setUpdatedAt(new Date().toString());

        System.out.println("准备保存任务: URI=" + task.getUri());

        // 确保参数不会丢失 - 特别关注日期范围参数
        if (task.getType().equals("HistoricalComparison")) {
            // 检查并记录关键参数
            if (task.getParameters().containsKey("periodOne")) {
                System.out.println("保存前周期1: " + task.getParameters().get("periodOne"));
            } else {
                System.out.println("警告: 任务缺少periodOne参数!");
            }

            if (task.getParameters().containsKey("periodTwo")) {
                System.out.println("保存前周期2: " + task.getParameters().get("periodTwo"));
            } else {
                System.out.println("警告: 任务缺少periodTwo参数!");
            }
        }

        // 保存到内存缓存
        taskMap.put(task.getUri(), task);
        System.out.println("保存到内存的任务参数: " + task.getParameters());

        try {
            List<String> triples = buildTaskTriples(task);
            System.out.println("生成的三元组数量: " + triples.size());
            System.out.println("生成的三元组内容预览: " + String.join("\n", triples.subList(0, Math.min(5, triples.size()))));

            // 先删除旧数据
            if (findByUri(task.getUri()) != null) {
                String deleteQuery = "DELETE WHERE { <" + task.getUri() + "> ?p ?o }";
                sparqlService.update(deleteQuery);
            }

            // 插入新数据
            String insertQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX task: <http://example.com/task/>\n" +
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "INSERT DATA { " + String.join(" ", triples) + " }";
            sparqlService.update(insertQuery);

            // 保存历史记录
            try {
                TaskHistory history = new TaskHistory();
                history.setTaskUri(task.getUri());
                history.setTaskType(task.getType());
                history.setTaskLabel(task.getLabel());
                history.setTimestamp(new Date());
                taskHistoryRepository.save(history);
            } catch (Exception e) {
                System.err.println("保存任务历史时出错: " + e.getMessage());
            }

            System.out.println("任务保存成功 - URI: " + task.getUri() + ", 内存中任务数量: " + taskMap.size());
            System.out.println("保存后最终的任务参数: " + task.getParameters());

            // 特别确认参数是否成功保存
            if (task.getType().equals("HistoricalComparison")) {
                if (task.getParameters().containsKey("periodOne")) {
                    System.out.println("保存后确认周期1: " + task.getParameters().get("periodOne"));
                }
                if (task.getParameters().containsKey("periodTwo")) {
                    System.out.println("保存后确认周期2: " + task.getParameters().get("periodTwo"));
                }
            }

            System.out.println("==== 保存任务结束 ====\n");
        } catch (Exception e) {
            System.err.println("保存到RDF存储时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return task;
    }

    public List<Map<String, String>> executeTaskQuery(String taskUri) {
        Task task = getTaskByUri(taskUri);
        if (task == null) {
            throw new IllegalArgumentException("找不到任务: " + taskUri);
        }

        String queryTemplate = getQueryTemplateForTaskType(task.getType());
        if (queryTemplate == null || queryTemplate.isEmpty()) {
            throw new IllegalArgumentException("找不到任务类型的查询模板: " + task.getType());
        }

        String query = QueryTemplateUtil.applyTemplate(queryTemplate, task.getProperties());
        System.out.println("执行任务查询: " + query);

        return sparqlService.query(query);
    }

    public void deleteTask(String uri) {
        System.out.println("========================");
        System.out.println("尝试删除任务，URI: " + uri);

        String deleteQuery = "DELETE WHERE { <" + uri + "> ?p ?o }";
        System.out.println("删除查询: " + deleteQuery);
        sparqlService.update(deleteQuery);

        taskMap.remove(uri);

        System.out.println("========================");
    }

    public void syncTasks() {
        List<Task> rdfTasks = findAll();
        for (Task task : rdfTasks) {
            if (task.getUri() != null) {
                taskMap.put(task.getUri(), task);
            }
        }
        System.out.println("同步完成，内存中任务数量: " + taskMap.size());
    }

    public void clearMemoryTasks() {
        taskMap.clear();
        System.out.println("已清除内存中的所有任务");
    }

    public void cleanupOldTasks(long ageThresholdInHours) {
        long now = System.currentTimeMillis();
        long threshold = now - (ageThresholdInHours * 60 * 60 * 1000);

        int removed = 0;
        Iterator<Map.Entry<String, Task>> iterator = taskMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Task> entry = iterator.next();
            Task task = entry.getValue();

            try {
                Date updatedDate = new Date(task.getUpdatedAt());
                if (updatedDate.getTime() < threshold) {
                    iterator.remove();
                    removed++;
                }
            } catch (Exception e) {
                System.err.println("清理任务时出错: " + e.getMessage());
            }
        }

        System.out.println("清理完成，删除了 " + removed + " 个旧任务");
    }

    private List<String> buildTaskTriples(Task task) {
        List<String> triples = new ArrayList<>();

        triples.add("<" + task.getUri() + "> rdf:type <" + TASK_TYPE_PREFIX + task.getType() + "> .");
        triples.add("<" + task.getUri() + "> rdfs:label \"" + escapeString(task.getLabel()) + "\" .");

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            triples.add("<" + task.getUri() + "> rdfs:comment \"" + escapeString(task.getDescription()) + "\" .");
        }

        String createdAtStr = task.getCreatedAt();
        String updatedAtStr = task.getUpdatedAt();

        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            triples.add("<" + task.getUri() + "> task:createdAt \"" + createdAtStr + "\"^^xsd:string .");
        }

        if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
            triples.add("<" + task.getUri() + "> task:updatedAt \"" + updatedAtStr + "\"^^xsd:string .");
        }

        // 处理普通属性
        for (Map.Entry<String, Object> entry : task.getProperties().entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();

            if (propertyValue != null) {
                String formattedValue = formatRdfValue(propertyValue);
                triples.add("<" + task.getUri() + "> task:" + propertyName + " " + formattedValue + " .");
            }
        }

        // 处理参数 (parameters)
        if (task.getParameters() != null && !task.getParameters().isEmpty()) {
            System.out.println("处理任务参数: " + task.getParameters().size() + "个");

            // 添加标记表示此任务有参数
            triples.add("<" + task.getUri() + "> task:hasParameters \"true\"^^xsd:boolean .");

            // 特别关注timeRange参数
            if (task.getParameters().containsKey("timeRange")) {
                System.out.println("==== 特别监控: 发现timeRange参数 ====");
                System.out.println("timeRange值: " + task.getParameters().get("timeRange"));
                System.out.println("timeRange类型: " + (task.getParameters().get("timeRange") != null
                        ? task.getParameters().get("timeRange").getClass().getName()
                        : "null"));
            }

            for (Map.Entry<String, Object> entry : task.getParameters().entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();

                if (paramValue != null) {
                    // 特别处理timeRange参数的日志
                    if ("timeRange".equals(paramName)) {
                        System.out.println("正在添加timeRange参数三元组，值为: " + paramValue);
                    }

                    String formattedValue = formatRdfValue(paramValue);
                    // 使用特殊前缀区分参数
                    String triple = "<" + task.getUri() + "> task:param_" + paramName + " " + formattedValue + " .";
                    triples.add(triple);
                    System.out.println("添加参数三元组: " + triple);
                } else if ("timeRange".equals(paramName)) {
                    // timeRange为null的特殊处理
                    System.out.println("警告: timeRange参数值为null!");
                }
            }
        } else {
            System.out.println("任务没有参数或参数为空");
        }

        return triples;
    }

    private String formatRdfValue(Object value) {
        if (value == null)
            return "\"\"";

        // 特别处理可能是日期范围的字符串
        if (value instanceof String) {
            String stringValue = (String) value;

            // 检查是否是日期范围格式 (例如 2023-04-27/2023-04-28)
            if (stringValue.contains("/") && stringValue.matches(".*\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}.*")) {
                System.out.println("检测到日期范围格式: " + stringValue);
                return "\"" + escapeString(stringValue) + "\"^^xsd:string";
            }

            if (stringValue.startsWith("http://") || stringValue.startsWith("https://")) {
                return "<" + stringValue + ">";
            } else {
                return "\"" + escapeString(stringValue) + "\"";
            }
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Date) {
            return "\"" + value.toString() + "\"^^xsd:string";
        } else {
            return "\"" + escapeString(value.toString()) + "\"";
        }
    }

    private String escapeString(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<Task> convertResultsToTasks(List<Map<String, String>> results) {
        Map<String, List<Map<String, String>>> groupedResults = new HashMap<>();

        for (Map<String, String> result : results) {
            String taskUri = result.get("task");
            if (taskUri != null) {
                groupedResults.computeIfAbsent(taskUri, k -> new ArrayList<>()).add(result);
            }
        }

        System.out.println("分组后的任务URI数量: " + groupedResults.size());

        List<Task> tasks = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : groupedResults.entrySet()) {
            String taskUri = entry.getKey();
            List<Map<String, String>> taskResults = entry.getValue();
            Task task = convertResultToTask(taskUri, taskResults);
            tasks.add(task);
            System.out.println("转换任务: " + taskUri + " -> " + task.getLabel());
        }

        return tasks;
    }

    private Task convertResultToTask(String taskUri, List<Map<String, String>> results) {
        Task task = new Task();
        task.setUri(taskUri);

        System.out.println("开始从查询结果转换任务: " + taskUri);
        System.out.println("查询结果行数: " + results.size());

        for (Map<String, String> result : results) {
            if (task.getType() == null && result.containsKey("type")) {
                String fullType = result.get("type");
                task.setType(getTypeFromUri(fullType));
                System.out.println("设置任务类型: " + task.getType());
            }

            if (task.getLabel() == null && result.containsKey("label")) {
                task.setLabel(result.get("label"));
                System.out.println("设置任务标签: " + task.getLabel());
            }

            if (result.containsKey("p") && result.containsKey("o")) {
                String property = result.get("p");
                String value = result.get("o");

                System.out.println("处理属性: " + property + " = " + value);

                // 修改: 处理完整URI形式的属性
                if (property.contains("task:") || property.contains("/task/")) {
                    String propName;
                    if (property.contains("task:")) {
                        propName = property.substring(property.indexOf("task:") + "task:".length());
                    } else {
                        propName = property.substring(property.lastIndexOf("/") + 1);
                    }

                    System.out.println("提取的属性名: " + propName);

                    if (propName.equals("description") || propName.equals("comment")) {
                        task.setDescription(value);
                        System.out.println("设置描述: " + value);
                    } else if (propName.equals("createdAt")) {
                        task.setCreatedAt(value);
                    } else if (propName.equals("updatedAt")) {
                        task.setUpdatedAt(value);
                    } else if (propName.startsWith("param_")) {
                        // 处理参数
                        String paramName = propName.substring("param_".length());
                        Object parsedValue = parseValue(value);
                        task.getParameters().put(paramName, parsedValue);
                        System.out.println("从RDF加载参数: " + paramName + " = " + parsedValue);
                    } else {
                        task.addProperty(propName, parseValue(value));
                        System.out.println("添加属性: " + propName + " = " + parseValue(value));
                    }
                }
            }
        }

        // 额外检查加载后的timeRange参数
        if (task.getParameters().containsKey("timeRange")) {
            System.out.println("从RDF加载后发现timeRange参数: " + task.getParameters().get("timeRange"));
        } else {
            System.out.println("从RDF加载后未找到timeRange参数!");

            // 查找可能命名不同的参数
            for (Map.Entry<String, Object> entry : task.getParameters().entrySet()) {
                if (entry.getKey().toLowerCase().contains("time") ||
                        entry.getKey().toLowerCase().contains("date") ||
                        entry.getKey().toLowerCase().contains("range")) {
                    System.out.println("发现可能相关的参数: " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }

        System.out.println("从RDF加载的任务参数: " + task.getParameters());
        return task;
    }

    private Object parseValue(String value) {
        if (value == null)
            return null;

        // 处理RDF类型化字面量，如 "1"^^http://www.w3.org/2001/XMLSchema#integer
        if (value.contains("^^")) {
            String[] parts = value.split("\\^\\^", 2);
            String actualValue = parts[0].trim();
            // 去掉可能的引号
            if (actualValue.startsWith("\"") && actualValue.endsWith("\"")) {
                actualValue = actualValue.substring(1, actualValue.length() - 1);
            }

            // 获取数据类型
            String dataType = parts.length > 1 ? parts[1].trim() : "";

            System.out.println("解析类型化值: " + actualValue + " (类型: " + dataType + ")");

            // 根据数据类型解析
            if (dataType.contains("integer")) {
                try {
                    return Integer.parseInt(actualValue);
                } catch (NumberFormatException e) {
                    System.out.println("无法解析为整数: " + actualValue);
                    return actualValue;
                }
            } else if (dataType.contains("double") || dataType.contains("float")) {
                try {
                    return Double.parseDouble(actualValue);
                } catch (NumberFormatException e) {
                    return actualValue;
                }
            } else if (dataType.contains("boolean")) {
                return Boolean.parseBoolean(actualValue);
            } else {
                // 默认返回字符串值
                return actualValue;
            }
        }

        // 日期范围特殊处理
        if (value.contains("/") && value.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
            return value;
        }

        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回原始字符串
            return value;
        }
    }

    private String getTypeFromUri(String uri) {
        if (uri.startsWith(TASK_TYPE_PREFIX)) {
            return uri.substring(TASK_TYPE_PREFIX.length());
        }
        return uri;
    }

    private String getQueryTemplateForTaskType(String taskType) {
        Map<String, String> templates = new HashMap<>();
        templates.put("SearchTask",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT ?s ?p ?o WHERE { ?s ?p ?o . ?s rdfs:label ?label . FILTER(contains(lcase(?label), {{keywords}})) }");
        templates.put("MonitoringTask",
                "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
                        "SELECT ?change ?time WHERE { {{entity}} ?change ?value . ?change dcterms:date ?time . }");
        templates.put("RiskAnalysisTask",
                "PREFIX risk: <http://example.com/risk/>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT ?risk ?level ?description WHERE { ?risk a risk:Risk . ?risk risk:level ?level . ?risk rdfs:label ?description . FILTER(?level >= {{minLevel}}) }");

        return templates.getOrDefault(taskType, "");
    }
}