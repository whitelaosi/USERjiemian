package com.example.rdfsearch.service.multilevelFocus;

import com.example.rdfsearch.model.multilevelFocus.*;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import com.example.rdfsearch.model.vizTask.VisualizationTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 多层次聚焦服务 - 实现PDF第4章的核心逻辑
 */
@Service
public class MultilevelFocusService {

    @Autowired
    private TaskFocusService taskFocusService;

    @Autowired
    private DataFocusService dataFocusService;

    @Autowired
    private PerceptionFocusService perceptionFocusService;

    // 聚焦层次
    public enum FocusLevel {
        TASK_FOCUS("任务聚焦"),
        DATA_FOCUS("数据聚焦"),
        PERCEPTION_FOCUS("感知聚焦");

        private String label;

        FocusLevel(String label) {
            this.label = label;
        }
    }

    // 执行多层次聚焦
    public FocusContext executeFocus(
            VisualizationTask task,
            List<SpatioTemporalObject> objects) {

        FocusContext context = new FocusContext();
        context.setTask(task);
        context.setObjects(objects);

        // 1. 任务聚焦 - 计算DoI
        Map<String, DoI> doiMap = taskFocusService.calculateDoI(task, objects);
        context.setDoiMap(doiMap);

        // 2. 数据聚焦 - 计算LoD
        Map<String, LoD> lodMap = dataFocusService.calculateLoD(doiMap, context);
        context.setLodMap(lodMap);

        // 3. 感知聚焦 - 计算DoA
        Map<String, DoA> doaMap = perceptionFocusService.calculateDoA(doiMap, lodMap, context);
        context.setDoaMap(doaMap);

        // 4. 视觉负载平衡
        balanceVisualLoad(context);

        return context;
    }

    // 视觉负载平衡
    private void balanceVisualLoad(FocusContext context) {
        double totalVisualLoad = calculateTotalVisualLoad(context);
        double maxLoad = context.getMaxVisualLoad();

        if (totalVisualLoad > maxLoad) {
            // 需要减少视觉负载
            adjustFocusLevels(context, totalVisualLoad / maxLoad);
        }
    }

    // 计算总视觉负载
    private double calculateTotalVisualLoad(FocusContext context) {
        double totalLoad = 0.0;

        for (SpatioTemporalObject obj : context.getObjects()) {
            String objId = obj.getId();

            // 基于三个聚焦因子计算负载
            double doi = context.getDoiMap().get(objId).getInterestValue();
            double lod = context.getLodMap().get(objId).getDetailLevel() / 3.0;
            double doa = 1.0 - context.getDoaMap().get(objId).getAbstractionLevel();

            // 综合负载
            double objLoad = doi * lod * doa;
            totalLoad += objLoad;
        }

        return totalLoad;
    }

    // 调整聚焦层级
    private void adjustFocusLevels(FocusContext context, double adjustmentRatio) {
        // 优先调整低兴趣度对象
        List<String> sortedIds = new ArrayList<>(context.getDoiMap().keySet());
        sortedIds.sort((id1, id2) -> {
            double doi1 = context.getDoiMap().get(id1).getInterestValue();
            double doi2 = context.getDoiMap().get(id2).getInterestValue();
            return Double.compare(doi1, doi2);
        });

        // 从低兴趣度开始调整
        for (String objId : sortedIds) {
            LoD lod = context.getLodMap().get(objId);
            DoA doa = context.getDoaMap().get(objId);

            // 降低细节层次
            int currentLevel = lod.getDetailLevel();
            lod.setDetailLevel(Math.max(0, currentLevel - 1));

            // 提高抽象程度
            double currentAbstraction = doa.getAbstractionLevel();
            doa.setAbstractionLevel(Math.min(1.0, currentAbstraction + 0.2));

            // 检查是否达到目标负载
            if (calculateTotalVisualLoad(context) <= context.getMaxVisualLoad()) {
                break;
            }
        }
    }

    // 获取聚焦建议
    public Map<String, Object> getFocusSuggestions(FocusContext context) {
        Map<String, Object> suggestions = new HashMap<>();

        // 分析当前聚焦状态
        double avgDoI = calculateAverageDoI(context);
        double avgLoD = calculateAverageLoD(context);
        double avgDoA = calculateAverageDoA(context);

        suggestions.put("averageDoI", avgDoI);
        suggestions.put("averageLoD", avgLoD);
        suggestions.put("averageDoA", avgDoA);

        // 提供优化建议
        List<String> optimizations = new ArrayList<>();

        if (avgDoI < 0.3) {
            optimizations.add("整体兴趣度较低，建议调整任务定义或扩大搜索范围");
        }

        if (avgLoD > 2.5) {
            optimizations.add("细节层次过高，可能造成信息过载");
        }

        if (avgDoA < 0.3) {
            optimizations.add("抽象程度过低，建议增加抽象表达以突出重点");
        }

        suggestions.put("optimizations", optimizations);

        return suggestions;
    }

    // 辅助计算方法
    private double calculateAverageDoI(FocusContext context) {
        return context.getDoiMap().values().stream()
                .mapToDouble(DoI::getInterestValue)
                .average()
                .orElse(0.0);
    }

    private double calculateAverageLoD(FocusContext context) {
        return context.getLodMap().values().stream()
                .mapToDouble(lod -> lod.getDetailLevel() / 3.0)
                .average()
                .orElse(0.0);
    }

    private double calculateAverageDoA(FocusContext context) {
        return context.getDoaMap().values().stream()
                .mapToDouble(DoA::getAbstractionLevel)
                .average()
                .orElse(0.0);
    }
}
