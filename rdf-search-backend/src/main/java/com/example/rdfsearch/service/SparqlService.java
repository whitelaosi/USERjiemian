package com.example.rdfsearch.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.rdfsearch.model.RdfInstance;
import com.example.rdfsearch.model.TaskHistory;
import com.example.rdfsearch.repository.TaskHistoryRepository;

@Service
public class SparqlService {
    // 注入TaskHistoryRepository
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    // 从配置文件注入Fuseki端点
    @Value("${fuseki.query.endpoint:http://localhost:3030/mydata/query}")
    private String queryEndpoint;

    @Value("${fuseki.update.endpoint:http://localhost:3030/mydata/update}")
    private String updateEndpoint;

    /**
     * 执行SPARQL更新操作
     */
    public void executeUpdate(String updateQueryString) {
        try {
            // 创建更新操作
            UpdateRequest updateRequest = UpdateFactory.create(updateQueryString);

            // 执行更新操作
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    updateRequest, updateEndpoint);
            processor.execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPARQL更新失败: " + e.getMessage());
        }
    }

    /**
     * 简化的更新方法，供TaskService使用
     */
    public void update(String updateQueryString) {
        try {
            System.out.println("执行SPARQL更新: " + updateQueryString);

            // 创建并执行更新请求
            UpdateRequest updateRequest = UpdateFactory.create(updateQueryString);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    updateRequest, updateEndpoint);
            processor.execute();

            System.out.println("更新成功完成");
        } catch (Exception e) {
            System.err.println("SPARQL更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行SPARQL查询并返回通用结果列表
     * 这个方法是为了支持TaskService而添加的
     */
    public List<Map<String, String>> query(String queryString) {
        List<Map<String, String>> results = new ArrayList<>();

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(queryEndpoint, query)) {
                ResultSet resultSet = qexec.execSelect();

                while (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();
                    Map<String, String> row = new HashMap<>();

                    solution.varNames().forEachRemaining(varName -> {
                        RDFNode node = solution.get(varName);
                        if (node != null) {
                            row.put(varName, node.toString());
                        }
                    });

                    results.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPARQL查询失败: " + e.getMessage());
        }

        return results;
    }

    /**
     * 执行SPARQL查询并返回通用结果列表
     * 为TaskExecutor服务的方法
     */
    public List<Map<String, Object>> executeQuery(String queryString) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(queryEndpoint, query)) {
                ResultSet resultSet = qexec.execSelect();

                while (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();
                    Map<String, Object> row = new HashMap<>();

                    solution.varNames().forEachRemaining(varName -> {
                        RDFNode node = solution.get(varName);
                        if (node != null) {
                            if (node.isLiteral()) {
                                // 根据字面量类型转换为适当的Java类型
                                try {
                                    if (node.asLiteral().getDatatype() != null) {
                                        String datatypeURI = node.asLiteral().getDatatype().getURI();
                                        if (datatypeURI.contains("integer")) {
                                            row.put(varName, node.asLiteral().getInt());
                                        } else if (datatypeURI.contains("float") || datatypeURI.contains("double")) {
                                            row.put(varName, node.asLiteral().getFloat());
                                        } else if (datatypeURI.contains("boolean")) {
                                            row.put(varName, node.asLiteral().getBoolean());
                                        } else if (datatypeURI.contains("dateTime")) {
                                            row.put(varName, node.asLiteral().getString());
                                        } else {
                                            row.put(varName, node.asLiteral().getString());
                                        }
                                    } else {
                                        row.put(varName, node.asLiteral().getString());
                                    }
                                } catch (Exception e) {
                                    // 转换失败时使用字符串
                                    row.put(varName, node.asLiteral().getString());
                                }
                            } else {
                                // 资源节点
                                row.put(varName, node.toString());
                            }
                        }
                    });

                    results.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPARQL查询失败: " + e.getMessage());
        }

        return results;
    }

    /**
     * 基本长度搜索
     */
    public List<RdfInstance> searchByLength(int minLength, int maxLength) {
        List<RdfInstance> results = new ArrayList<>();

        // 构建SPARQL查询 - 查询所有属性
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX ont: <http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "SELECT ?uri ?length ?microseismicEventCount ?energyRelease " +
                "?cumulativeApparentVolume ?dailyEventRate ?dailyEnergyRelease " +
                "?dailyApparentVolumeRate ?serialNumber ?mileage ?startMileage " +
                "?endMileage ?description ?riskType WHERE {\n" +
                "  ?uri ont:length ?length .\n" +
                "  FILTER (?length >= " + minLength + " && ?length <= " + maxLength + ")\n" +
                "  OPTIONAL { ?uri ont:microseismicEventCount ?microseismicEventCount }\n" +
                "  OPTIONAL { ?uri ont:energyRelease ?energyRelease }\n" +
                "  OPTIONAL { ?uri ont:cumulativeApparentVolume ?cumulativeApparentVolume }\n" +
                "  OPTIONAL { ?uri ont:dailyEventRate ?dailyEventRate }\n" +
                "  OPTIONAL { ?uri ont:dailyEnergyRelease ?dailyEnergyRelease }\n" +
                "  OPTIONAL { ?uri ont:dailyApparentVolumeRate ?dailyApparentVolumeRate }\n" +
                "  OPTIONAL { ?uri ont:serialNumber ?serialNumber }\n" +
                "  OPTIONAL { ?uri ont:mileage ?mileage }\n" +
                "  OPTIONAL { ?uri ont:startMileage ?startMileage }\n" +
                "  OPTIONAL { ?uri ont:endMileage ?endMileage }\n" +
                "  OPTIONAL { ?uri ont:description ?description }\n" +
                "  OPTIONAL { ?uri ont:riskType ?riskType }\n" +
                "} ORDER BY ?length LIMIT 100";

        // 执行查询并处理结果
        executeQuery(queryString, results);

        return results;
    }

    /**
     * 高级多属性搜索
     */
    public List<RdfInstance> advancedSearch(
            Float lengthMin, Float lengthMax,
            Integer microseismicEventCountMin, Integer microseismicEventCountMax,
            Float energyReleaseMin, Float energyReleaseMax,
            Float dailyEnergyReleaseMin, Float dailyEnergyReleaseMax,
            List<String> riskTypes) {

        List<RdfInstance> results = new ArrayList<>();

        // 构建SPARQL查询
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        queryBuilder.append("PREFIX ont: <http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#>\n");
        queryBuilder.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        queryBuilder.append("SELECT ?uri ?length ?microseismicEventCount ?energyRelease ");
        queryBuilder.append("?cumulativeApparentVolume ?dailyEventRate ?dailyEnergyRelease ");
        queryBuilder.append("?dailyApparentVolumeRate ?serialNumber ?mileage ?startMileage ");
        queryBuilder.append("?endMileage ?description ?riskType WHERE {\n");

        // 添加搜索过滤条件
        List<String> filters = new ArrayList<>();

        // 长度过滤器
        if (lengthMin != null || lengthMax != null) {
            queryBuilder.append("  ?uri ont:length ?length .\n");
            if (lengthMin != null && lengthMax != null) {
                filters.add("?length >= " + lengthMin + " && ?length <= " + lengthMax);
            } else if (lengthMin != null) {
                filters.add("?length >= " + lengthMin);
            } else if (lengthMax != null) {
                filters.add("?length <= " + lengthMax);
            }
        } else {
            queryBuilder.append("  OPTIONAL { ?uri ont:length ?length }\n");
        }

        // 微震事件数过滤器
        if (microseismicEventCountMin != null || microseismicEventCountMax != null) {
            queryBuilder.append("  ?uri ont:microseismicEventCount ?microseismicEventCount .\n");
            if (microseismicEventCountMin != null && microseismicEventCountMax != null) {
                filters.add("?microseismicEventCount >= " + microseismicEventCountMin +
                        " && ?microseismicEventCount <= " + microseismicEventCountMax);
            } else if (microseismicEventCountMin != null) {
                filters.add("?microseismicEventCount >= " + microseismicEventCountMin);
            } else if (microseismicEventCountMax != null) {
                filters.add("?microseismicEventCount <= " + microseismicEventCountMax);
            }
        } else {
            queryBuilder.append("  OPTIONAL { ?uri ont:microseismicEventCount ?microseismicEventCount }\n");
        }

        // 能量释放过滤器
        if (energyReleaseMin != null || energyReleaseMax != null) {
            queryBuilder.append("  ?uri ont:energyRelease ?energyRelease .\n");
            if (energyReleaseMin != null && energyReleaseMax != null) {
                filters.add("?energyRelease >= " + energyReleaseMin +
                        " && ?energyRelease <= " + energyReleaseMax);
            } else if (energyReleaseMin != null) {
                filters.add("?energyRelease >= " + energyReleaseMin);
            } else if (energyReleaseMax != null) {
                filters.add("?energyRelease <= " + energyReleaseMax);
            }
        } else {
            queryBuilder.append("  OPTIONAL { ?uri ont:energyRelease ?energyRelease }\n");
        }

        // 日能量释放过滤器
        if (dailyEnergyReleaseMin != null || dailyEnergyReleaseMax != null) {
            queryBuilder.append("  ?uri ont:dailyEnergyRelease ?dailyEnergyRelease .\n");
            if (dailyEnergyReleaseMin != null && dailyEnergyReleaseMax != null) {
                filters.add("?dailyEnergyRelease >= " + dailyEnergyReleaseMin +
                        " && ?dailyEnergyRelease <= " + dailyEnergyReleaseMax);
            } else if (dailyEnergyReleaseMin != null) {
                filters.add("?dailyEnergyRelease >= " + dailyEnergyReleaseMin);
            } else if (dailyEnergyReleaseMax != null) {
                filters.add("?dailyEnergyRelease <= " + dailyEnergyReleaseMax);
            }
        } else {
            queryBuilder.append("  OPTIONAL { ?uri ont:dailyEnergyRelease ?dailyEnergyRelease }\n");
        }

        // 风险类型过滤器
        if (riskTypes != null && !riskTypes.isEmpty()) {
            queryBuilder.append("  ?uri ont:riskType ?riskType .\n");

            // 创建风险类型的FILTER（IN）表达式
            StringBuilder riskFilterBuilder = new StringBuilder("?riskType IN (");
            for (int i = 0; i < riskTypes.size(); i++) {
                if (i > 0) {
                    riskFilterBuilder.append(", ");
                }
                riskFilterBuilder.append("\"").append(riskTypes.get(i)).append("\"");
            }
            riskFilterBuilder.append(")");
            filters.add(riskFilterBuilder.toString());
        } else {
            queryBuilder.append("  OPTIONAL { ?uri ont:riskType ?riskType }\n");
        }

        // 添加其他可选属性
        queryBuilder.append("  OPTIONAL { ?uri ont:cumulativeApparentVolume ?cumulativeApparentVolume }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:dailyEventRate ?dailyEventRate }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:dailyApparentVolumeRate ?dailyApparentVolumeRate }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:serialNumber ?serialNumber }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:mileage ?mileage }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:startMileage ?startMileage }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:endMileage ?endMileage }\n");
        queryBuilder.append("  OPTIONAL { ?uri ont:description ?description }\n");

        // 添加所有过滤器
        if (!filters.isEmpty()) {
            queryBuilder.append("  FILTER (");
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    queryBuilder.append(" && ");
                }
                queryBuilder.append("(").append(filters.get(i)).append(")");
            }
            queryBuilder.append(")\n");
        }

        // 完成查询
        queryBuilder.append("} ORDER BY ?uri LIMIT 100");

        // 执行查询并处理结果
        executeQuery(queryBuilder.toString(), results);

        return results;
    }

    /**
     * 根据任务个体查询对应微震数据
     * 修改：添加userId参数，用于记录查询历史
     */
    public Map<String, Object> searchByTask(String taskUri, String userId) {
        List<RdfInstance> results = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        Map<String, String> taskInfo = new HashMap<>();

        // 首先查询任务本身的信息
        String taskInfoQuery = "PREFIX ont: <http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#>\n"
                +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?targetEntity ?startDate ?endDate ?label WHERE {\n" +
                "  <" + taskUri + "> ont:hasTargetEntity ?targetEntity .\n" +
                "  <" + taskUri + "> ont:hasStartDate ?startDate .\n" +
                "  <" + taskUri + "> ont:hasEndDate ?endDate .\n" +
                "  OPTIONAL { <" + taskUri + "> rdfs:label ?label }\n" +
                "}";

        try {
            Query query = QueryFactory.create(taskInfoQuery);
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(queryEndpoint, query)) {
                ResultSet resultSet = qexec.execSelect();
                if (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();

                    if (solution.get("targetEntity") != null) {
                        taskInfo.put("targetEntity", solution.get("targetEntity").toString());
                    }
                    if (solution.get("startDate") != null) {
                        taskInfo.put("startDate", solution.get("startDate").toString());
                    }
                    if (solution.get("endDate") != null) {
                        taskInfo.put("endDate", solution.get("endDate").toString());
                    }
                    if (solution.get("label") != null) {
                        taskInfo.put("label", solution.get("label").toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("任务信息查询失败: " + e.getMessage());
        }

        // 如果找不到任务信息，返回空结果
        if (taskInfo.isEmpty()) {
            response.put("taskInfo", taskInfo);
            response.put("results", results);
            return response;
        }

        // 根据目标实体查询数据
        String targetEntity = taskInfo.get("targetEntity");
        if (targetEntity != null) {
            String dataQuery = "PREFIX ont: <http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#>\n"
                    +
                    "SELECT ?uri ?length ?microseismicEventCount ?energyRelease " +
                    "?cumulativeApparentVolume ?dailyEnergyRelease ?dailyApparentVolumeRate " +
                    "?serialNumber ?mileage ?startMileage ?endMileage ?description ?riskType WHERE {\n" +
                    "  BIND(<" + targetEntity + "> AS ?uri)\n" +
                    "  OPTIONAL { ?uri ont:length ?length }\n" +
                    "  OPTIONAL { ?uri ont:microseismicEventCount ?microseismicEventCount }\n" +
                    "  OPTIONAL { ?uri ont:energyRelease ?energyRelease }\n" +
                    "  OPTIONAL { ?uri ont:cumulativeApparentVolume ?cumulativeApparentVolume }\n" +
                    "  OPTIONAL { ?uri ont:dailyEnergyRelease ?dailyEnergyRelease }\n" +
                    "  OPTIONAL { ?uri ont:dailyApparentVolumeRate ?dailyApparentVolumeRate }\n" +
                    "  OPTIONAL { ?uri ont:serialNumber ?serialNumber }\n" +
                    "  OPTIONAL { ?uri ont:mileage ?mileage }\n" +
                    "  OPTIONAL { ?uri ont:startMileage ?startMileage }\n" +
                    "  OPTIONAL { ?uri ont:endMileage ?endMileage }\n" +
                    "  OPTIONAL { ?uri ont:description ?description }\n" +
                    "  OPTIONAL { ?uri ont:riskType ?riskType }\n" +
                    "}";

            executeQuery(dataQuery, results);
        }

        response.put("taskInfo", taskInfo);
        response.put("results", results);

        // 记录查询历史 (新增代码)
        if (userId != null && !userId.isEmpty()) { // 匿名用户可能没有userId
            try {
                TaskHistory history = new TaskHistory();
                history.setTaskUri(taskUri);
                history.setUserId(userId);
                history.setQueryTime(new Date());
                // 可选：保存简要结果摘要
                String resultSummary = results.size() > 0
                        ? "找到" + results.size() + "条结果，目标:" + taskInfo.get("targetEntity")
                        : "未找到结果";
                history.setQueryResult(resultSummary);
                taskHistoryRepository.save(history);
                System.out.println("已记录查询历史: 用户=" + userId + ", 任务=" + taskUri);
            } catch (Exception e) {
                // 记录历史失败不应影响主要功能
                System.err.println("记录查询历史失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return response;
    }

    /**
     * 执行SPARQL查询并处理结果
     */
    private void executeQuery(String queryString, List<RdfInstance> results) {
        try {
            // 创建查询
            Query query = QueryFactory.create(queryString);

            // 执行查询
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(queryEndpoint, query)) {
                ResultSet resultSet = qexec.execSelect();

                // 处理结果
                while (resultSet.hasNext()) {
                    QuerySolution solution = resultSet.nextSolution();
                    RdfInstance instance = new RdfInstance();

                    // 设置URI
                    RDFNode uriNode = solution.get("uri");
                    if (uriNode != null) {
                        instance.setUri(uriNode.toString());
                    }

                    // 设置length
                    RDFNode lengthNode = solution.get("length");
                    if (lengthNode != null) {
                        instance.setLength(lengthNode.asLiteral().getFloat());
                    }

                    // 设置microseismicEventCount
                    RDFNode microseismicEventCountNode = solution.get("microseismicEventCount");
                    if (microseismicEventCountNode != null) {
                        instance.setMicroseismicEventCount(microseismicEventCountNode.asLiteral().getInt());
                    }

                    // 设置energyRelease
                    RDFNode energyReleaseNode = solution.get("energyRelease");
                    if (energyReleaseNode != null) {
                        instance.setEnergyRelease(energyReleaseNode.asLiteral().getFloat());
                    }

                    // 设置cumulativeApparentVolume
                    RDFNode cumulativeApparentVolumeNode = solution.get("cumulativeApparentVolume");
                    if (cumulativeApparentVolumeNode != null) {
                        instance.setCumulativeApparentVolume(cumulativeApparentVolumeNode.asLiteral().getFloat());
                    }

                    // 设置dailyEventRate
                    RDFNode dailyEventRateNode = solution.get("dailyEventRate");
                    if (dailyEventRateNode != null) {
                        instance.setDailyEventRate(dailyEventRateNode.asLiteral().getString());
                    }

                    // 设置dailyEnergyRelease
                    RDFNode dailyEnergyReleaseNode = solution.get("dailyEnergyRelease");
                    if (dailyEnergyReleaseNode != null) {
                        instance.setDailyEnergyRelease(dailyEnergyReleaseNode.asLiteral().getFloat());
                    }

                    // 设置dailyApparentVolumeRate
                    RDFNode dailyApparentVolumeRateNode = solution.get("dailyApparentVolumeRate");
                    if (dailyApparentVolumeRateNode != null) {
                        instance.setDailyApparentVolumeRate(dailyApparentVolumeRateNode.asLiteral().getFloat());
                    }

                    // 设置serialNumber
                    RDFNode serialNumberNode = solution.get("serialNumber");
                    if (serialNumberNode != null) {
                        instance.setSerialNumber(serialNumberNode.asLiteral().getInt());
                    }

                    // 设置mileage
                    RDFNode mileageNode = solution.get("mileage");
                    if (mileageNode != null) {
                        instance.setMileage(mileageNode.asLiteral().getString());
                    }

                    // 设置startMileage
                    RDFNode startMileageNode = solution.get("startMileage");
                    if (startMileageNode != null) {
                        instance.setStartMileage(startMileageNode.asLiteral().getString());
                    }

                    // 设置endMileage
                    RDFNode endMileageNode = solution.get("endMileage");
                    if (endMileageNode != null) {
                        instance.setEndMileage(endMileageNode.asLiteral().getString());
                    }

                    // 设置description
                    RDFNode descriptionNode = solution.get("description");
                    if (descriptionNode != null) {
                        instance.setDescription(descriptionNode.asLiteral().getString());
                    }

                    // 设置riskType
                    RDFNode riskTypeNode = solution.get("riskType");
                    if (riskTypeNode != null) {
                        instance.setRiskType(riskTypeNode.asLiteral().getString());
                    }

                    results.add(instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPARQL查询失败: " + e.getMessage());
        }
    }
}