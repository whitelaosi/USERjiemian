package com.example.rdfsearch.controller;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rdfsearch.service.SparqlService;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private SparqlService sparqlService; // 使用您现有的SPARQL服务

    private static final String NS = "http://www.semanticweb.org/yoruh/ontologies/2024/9/untitled-ontology-11#";

    @PostMapping("/convert-to-rdf")
    public ResponseEntity<?> convertToRdf(@RequestBody Map<String, Object> tableData) {
        try {
            // 创建RDF模型
            Model model = ModelFactory.createDefaultModel();

            // 设置命名空间前缀
            model.setNsPrefix("geo", NS);

            // 获取表格数据
            List<Map<String, Object>> tables = (List<Map<String, Object>>) tableData.get("tables");

            // 处理每个表格
            for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                Map<String, Object> table = tables.get(tableIndex);

                String tableType = (String) table.get("tableType");
                List<String> header = (List<String>) table.get("header");
                List<List<String>> rows = (List<List<String>>) table.get("rows");

                // 为每个表格行创建RDF资源
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    List<String> row = rows.get(rowIndex);

                    // 创建资源URI
                    String resourceURI = NS + "table" + (tableIndex + 1) + "_row" + (rowIndex + 1);
                    Resource resource = model.createResource(resourceURI);

                    // 添加类型信息
                    String typeURI = NS + sanitizeForURI(tableType);
                    resource.addProperty(RDF.type, model.createResource(typeURI));

                    // 添加每列属性
                    for (int colIndex = 0; colIndex < header.size() && colIndex < row.size(); colIndex++) {
                        String colName = header.get(colIndex);
                        String value = row.get(colIndex);

                        // 跳过空值或占位符
                        if (value == null || value.equals("-")) {
                            continue;
                        }

                        // 创建属性
                        String propertyURI = NS + sanitizeForURI(colName);
                        Property property = model.createProperty(propertyURI);

                        // 添加属性值
                        resource.addProperty(property, value);
                    }
                }
            }

            // 将RDF模型转换为Turtle格式
            StringWriter writer = new StringWriter();
            model.write(writer, "TURTLE");
            String rdfContent = writer.toString();

            // 将RDF导入到Fuseki
            boolean success = importToFuseki(model);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "RDF数据已成功导入" : "RDF数据导入失败");
            response.put("rdfContent", rdfContent);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 将RDF模型导入到Fuseki
     */
    private boolean importToFuseki(Model model) {
        try {
            // 使用与SparqlService相同的数据集名称
            String datasetName = "mydata"; // 确保与SparqlService中的端点名称一致

            // 清除现有数据（可选）
            sparqlService.executeUpdate("CLEAR ALL");

            // 将模型写入Fuseki
            StringWriter writer = new StringWriter();
            model.write(writer, "N-TRIPLE");
            String tripleData = writer.toString();

            // 构建INSERT语句
            String insertQuery = "INSERT DATA { " + tripleData + " }";
            sparqlService.executeUpdate(insertQuery);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将字符串转换为适合URI的格式
     */
    private String sanitizeForURI(String input) {
        if (input == null) {
            return "Unknown";
        }

        // 移除空格、标点符号等，替换为下划线
        return input.replaceAll("[\\s\\p{Punct}]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}