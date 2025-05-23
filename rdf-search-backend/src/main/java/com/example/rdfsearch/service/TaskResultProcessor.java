package com.example.rdfsearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务结果处理服务，负责格式化和转换执行结果
 */
@Service
public class TaskResultProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TaskResultProcessor.class);

    /**
     * 处理搜索事件任务的结果
     */
    public Map<String, Object> processSearchEventResult(Map<String, Object> rawResult) {
        Map<String, Object> processedResult = new HashMap<>(rawResult);

        try {
            // 从"results"键获取结果列表 (而不是之前的"events")
            if (rawResult.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> events = (List<Map<String, Object>>) rawResult.get("results");

                // 处理能量值，适配新的"能量 (J)"字段名
                events.forEach(event -> {
                    if (event.containsKey("能量 (J)")) {
                        Object energyObj = event.get("能量 (J)");
                        double energy;
                        if (energyObj instanceof Number) {
                            energy = ((Number) energyObj).doubleValue();
                        } else {
                            try {
                                energy = Double.parseDouble(energyObj.toString());
                            } catch (NumberFormatException e) {
                                energy = 0.0;
                            }
                        }

                        // 添加分类
                        String category;
                        if (energy < 100)
                            category = "微弱";
                        else if (energy < 1000)
                            category = "轻微";
                        else if (energy < 5000)
                            category = "中等";
                        else if (energy < 10000)
                            category = "强烈";
                        else
                            category = "极强";

                        event.put("category", category);
                    }
                });

                // 计算能量分布统计
                Map<String, Integer> energyDistribution = new HashMap<>();
                energyDistribution.put("微弱", 0);
                energyDistribution.put("轻微", 0);
                energyDistribution.put("中等", 0);
                energyDistribution.put("强烈", 0);
                energyDistribution.put("极强", 0);

                events.forEach(event -> {
                    if (event.containsKey("category")) {
                        String category = event.get("category").toString();
                        energyDistribution.put(category, energyDistribution.getOrDefault(category, 0) + 1);
                    }
                });

                processedResult.put("energyDistribution", energyDistribution);
            }
        } catch (Exception e) {
            logger.error("处理搜索事件结果时出错: " + e.getMessage(), e);
            processedResult.put("processingError", e.getMessage());
        }

        return processedResult;
    }

    /**
     * 处理风险分析任务的结果
     */
    public Map<String, Object> processRiskAnalysisResult(Map<String, Object> rawResult) {
        Map<String, Object> processedResult = new HashMap<>(rawResult);

        try {
            // 生成风险因素贡献表
            List<Map<String, Object>> riskFactors = new ArrayList<>();
            double riskScore = rawResult.containsKey("riskScore") ? ((Number) rawResult.get("riskScore")).doubleValue()
                    : 0;

            if (rawResult.containsKey("analysisFactors")) {
                @SuppressWarnings("unchecked")
                List<String> factors = (List<String>) rawResult.get("analysisFactors");

                if (factors.contains("frequency")) {
                    int eventCount = rawResult.containsKey("eventCount")
                            ? ((Number) rawResult.get("eventCount")).intValue()
                            : 0;
                    double contribution = Math.min(eventCount / 10.0, 50) / riskScore * 100;

                    Map<String, Object> factor = new HashMap<>();
                    factor.put("name", "事件频率");
                    factor.put("value", eventCount);
                    factor.put("contribution", contribution);
                    riskFactors.add(factor);
                }

                if (factors.contains("energy") && rawResult.containsKey("energyStats")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> energyStats = (Map<String, Object>) rawResult.get("energyStats");

                    if (energyStats.containsKey("totalEnergy")) {
                        double totalEnergy = ((Number) energyStats.get("totalEnergy")).doubleValue();
                        double contribution = Math.min(totalEnergy / 1000.0, 50) / riskScore * 100;

                        Map<String, Object> factor = new HashMap<>();
                        factor.put("name", "能量释放");
                        factor.put("value", totalEnergy);
                        factor.put("contribution", contribution);
                        riskFactors.add(factor);
                    }
                }

                if (factors.contains("geology")) {
                    // 假设地质因素的贡献固定
                    Map<String, Object> factor = new HashMap<>();
                    factor.put("name", "地质特性");
                    factor.put("value", "复杂");
                    factor.put("contribution", 20.0);
                    riskFactors.add(factor);
                }
            }

            processedResult.put("riskFactors", riskFactors);

            // 添加风险建议
            String riskLevel = rawResult.containsKey("riskLevel") ? rawResult.get("riskLevel").toString() : "未知";

            String recommendation;
            switch (riskLevel) {
                case "低":
                    recommendation = "定期监测，无需特别措施。";
                    break;
                case "中":
                    recommendation = "增加监测频率，注意变化趋势。";
                    break;
                case "高":
                    recommendation = "加强监测，准备应急预案，考虑部分区域加固。";
                    break;
                case "严重":
                    recommendation = "立即采取应急措施，考虑人员撤离，全面加固。";
                    break;
                default:
                    recommendation = "建议进行详细评估后再决定措施。";
            }

            processedResult.put("recommendation", recommendation);

        } catch (Exception e) {
            logger.error("处理风险分析结果时出错: " + e.getMessage(), e);
            processedResult.put("processingError", e.getMessage());
        }

        return processedResult;
    }

    /**
     * 处理历史对比任务的结果
     */
    public Map<String, Object> processHistoricalComparisonResult(Map<String, Object> rawResult) {
        Map<String, Object> processedResult = new HashMap<>(rawResult);

        try {
            // 生成时间序列数据
            if (rawResult.containsKey("periodOneValue") && rawResult.containsKey("periodTwoValue")) {
                List<Map<String, Object>> timeSeriesData = new ArrayList<>();

                Map<String, Object> point1 = new HashMap<>();
                point1.put("period", rawResult.get("periodOne"));
                point1.put("value", rawResult.get("periodOneValue"));
                timeSeriesData.add(point1);

                Map<String, Object> point2 = new HashMap<>();
                point2.put("period", rawResult.get("periodTwo"));
                point2.put("value", rawResult.get("periodTwoValue"));
                timeSeriesData.add(point2);

                processedResult.put("timeSeriesData", timeSeriesData);
            }

            // 生成变化分析
            double changePercent = rawResult.containsKey("changePercent")
                    ? ((Number) rawResult.get("changePercent")).doubleValue()
                    : 0;

            // 优化变化分析逻辑
            String changeAnalysis;
            String metricName = rawResult.containsKey("metric") ? rawResult.get("metric").toString() : "指标";

            if (Math.abs(changePercent) < 5) {
                changeAnalysis = metricName + "变化不明显，保持稳定状态。";
            } else if (changePercent > 0) {
                if (changePercent > 50) {
                    changeAnalysis = metricName + "显著增加(" + String.format("%.1f", changePercent) + "%)，需要特别关注并分析原因。";
                } else {
                    changeAnalysis = metricName + "有所增加(" + String.format("%.1f", changePercent) + "%)，建议继续观察变化趋势。";
                }
            } else {
                if (changePercent < -50) {
                    changeAnalysis = metricName + "显著减少(" + String.format("%.1f", Math.abs(changePercent))
                            + "%)，可能表明安全措施有效或活动减弱。";
                } else {
                    changeAnalysis = metricName + "有所减少(" + String.format("%.1f", Math.abs(changePercent))
                            + "%)，建议持续监测确认趋势。";
                }
            }

            processedResult.put("changeAnalysis", changeAnalysis);

        } catch (Exception e) {
            logger.error("处理历史对比结果时出错: " + e.getMessage(), e);
            processedResult.put("processingError", e.getMessage());
        }

        return processedResult;
    }

    /**
     * 根据任务类型处理结果
     */
    public Map<String, Object> processTaskResult(String taskType, Map<String, Object> rawResult) {
        switch (taskType) {
            case "SearchEvent":
                return processSearchEventResult(rawResult);
            case "RiskAnalysis":
                return processRiskAnalysisResult(rawResult);
            case "HistoricalComparison":
                return processHistoricalComparisonResult(rawResult);
            default:
                return rawResult;
        }
    }
}