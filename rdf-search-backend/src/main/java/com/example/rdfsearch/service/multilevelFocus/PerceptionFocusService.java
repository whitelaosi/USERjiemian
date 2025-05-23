
package com.example.rdfsearch.service.multilevelFocus;

import com.example.rdfsearch.model.multilevelFocus.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 感知聚焦服务 - 计算抽象程度(DoA)
 */
@Service
public class PerceptionFocusService {

    /**
     * 计算抽象程度
     * 
     * @param doiMap  兴趣度映射
     * @param lodMap  细节层次映射
     * @param context 聚焦上下文
     * @return 抽象程度映射
     */
    public Map<String, DoA> calculateDoA(Map<String, DoI> doiMap,
            Map<String, LoD> lodMap,
            FocusContext context) {
        Map<String, DoA> doaMap = new HashMap<>();

        // 为每个对象计算DoA
        for (String objectId : doiMap.keySet()) {
            DoI doi = doiMap.get(objectId);
            LoD lod = lodMap.get(objectId);

            DoA doa = new DoA(objectId);

            // 基于DoI和LoD计算抽象程度
            double abstractionLevel = doa.calculateAbstraction(doi, lod);

            // 应用抽象到视觉参数
            Map<String, Object> visualParams = doa.applyAbstraction();
            doa.setVisualParameters(visualParams);

            doaMap.put(objectId, doa);
        }

        // 进行感知平衡
        balancePerception(doaMap, context);

        return doaMap;
    }

    /**
     * 感知平衡
     * 确保整体可视化的感知舒适度
     */
    private void balancePerception(Map<String, DoA> doaMap, FocusContext context) {
        // 统计不同抽象类型的数量
        Map<DoA.AbstractionType, Integer> typeCounts = new HashMap<>();
        for (DoA doa : doaMap.values()) {
            DoA.AbstractionType type = doa.getAbstractionType();
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        // 避免过多的文本抽象或符号抽象
        int totalObjects = doaMap.size();
        int maxTextAbstraction = totalObjects / 4; // 最多1/4使用文本抽象
        int maxSymbolAbstraction = totalObjects / 3; // 最多1/3使用符号抽象

        // 如果文本抽象过多，调整为符号抽象
        if (typeCounts.getOrDefault(DoA.AbstractionType.TEXTUAL, 0) > maxTextAbstraction) {
            adjustAbstractionTypes(doaMap, DoA.AbstractionType.TEXTUAL,
                    DoA.AbstractionType.SYMBOLIC,
                    typeCounts.get(DoA.AbstractionType.TEXTUAL) - maxTextAbstraction);
        }

        // 如果符号抽象过多，调整为颜色抽象
        if (typeCounts.getOrDefault(DoA.AbstractionType.SYMBOLIC, 0) > maxSymbolAbstraction) {
            adjustAbstractionTypes(doaMap, DoA.AbstractionType.SYMBOLIC,
                    DoA.AbstractionType.COLOR_BASED,
                    typeCounts.get(DoA.AbstractionType.SYMBOLIC) - maxSymbolAbstraction);
        }
    }

    /**
     * 调整抽象类型
     */
    private void adjustAbstractionTypes(Map<String, DoA> doaMap,
            DoA.AbstractionType fromType,
            DoA.AbstractionType toType,
            int adjustCount) {
        int adjusted = 0;

        for (DoA doa : doaMap.values()) {
            if (adjusted >= adjustCount) {
                break;
            }

            if (doa.getAbstractionType() == fromType) {
                doa.setAbstractionType(toType);
                // 重新应用抽象
                Map<String, Object> newParams = doa.applyAbstraction();
                doa.setVisualParameters(newParams);
                adjusted++;
            }
        }
    }

    /**
     * 获取视觉化建议
     */
    public Map<String, Object> getVisualizationSuggestions(Map<String, DoA> doaMap) {
        Map<String, Object> suggestions = new HashMap<>();

        // 统计抽象类型分布
        Map<DoA.AbstractionType, Integer> distribution = new HashMap<>();
        double avgAbstractionLevel = 0.0;

        for (DoA doa : doaMap.values()) {
            DoA.AbstractionType type = doa.getAbstractionType();
            distribution.put(type, distribution.getOrDefault(type, 0) + 1);
            avgAbstractionLevel += doa.getAbstractionLevel();
        }

        avgAbstractionLevel /= doaMap.size();

        suggestions.put("abstractionDistribution", distribution);
        suggestions.put("averageAbstractionLevel", avgAbstractionLevel);

        // 生成建议
        List<String> visualSuggestions = new ArrayList<>();

        if (avgAbstractionLevel > 0.7) {
            visualSuggestions.add("整体抽象程度较高，可考虑增加细节展示");
        }

        if (distribution.getOrDefault(DoA.AbstractionType.AGGREGATED, 0) > doaMap.size() / 2) {
            visualSuggestions.add("聚合对象过多，可考虑展开部分重要聚合");
        }

        suggestions.put("suggestions", visualSuggestions);

        return suggestions;
    }
}