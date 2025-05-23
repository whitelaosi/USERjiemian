
package com.example.rdfsearch.service.multilevelFocus;

import com.example.rdfsearch.model.multilevelFocus.*;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import com.example.rdfsearch.model.vizTask.TaskContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据聚焦服务 - 计算细节层次(LoD)
 */
@Service
public class DataFocusService {

    /**
     * 计算细节层次
     * 
     * @param doiMap  兴趣度映射
     * @param context 聚焦上下文
     * @return 细节层次映射
     */
    public Map<String, LoD> calculateLoD(Map<String, DoI> doiMap, FocusContext context) {
        Map<String, LoD> lodMap = new HashMap<>();

        // 构建计算上下文
        Map<String, Object> calcContext = buildCalculationContext(context);

        // 为每个对象计算LoD
        for (Map.Entry<String, DoI> entry : doiMap.entrySet()) {
            String objectId = entry.getKey();
            DoI doi = entry.getValue();

            LoD lod = new LoD(objectId);

            // 基于DoI计算LoD
            int detailLevel = lod.calculateLoD(doi, calcContext);

            // 生成细节参数
            lod.generateDetailParameters();

            lodMap.put(objectId, lod);
        }

        // 进行细节层次平衡
        balanceDetailLevels(lodMap, context);

        return lodMap;
    }

    /**
     * 构建计算上下文
     */
    private Map<String, Object> buildCalculationContext(FocusContext context) {
        Map<String, Object> calcContext = new HashMap<>();

        // 从任务上下文中提取信息
        if (context.getTask() != null && context.getTask().getContext() != null) {
            TaskContext taskContext = context.getTask().getContext();

            // 空间范围
            if (taskContext.getSpatialRange() != null) {
                calcContext.put("spatialRange", taskContext.getSpatialRange());
                // 推断空间尺度
                String scale = inferSpaceScale(taskContext.getSpatialRange());
                calcContext.put("spaceScale", scale);
            }

            // 用户环境信息
            Object displayType = taskContext.getEnvironmentInfo("displayType");
            if (displayType != null) {
                calcContext.put("displayType", displayType);
            }
        }

        return calcContext;
    }

    /**
     * 推断空间尺度
     */
    private String inferSpaceScale(String spatialRange) {
        if (spatialRange.contains("城市") || spatialRange.contains("市")) {
            return "MACRO_SCALE";
        } else if (spatialRange.contains("区") || spatialRange.contains("县")) {
            return "MESO_SCALE";
        } else if (spatialRange.contains("建筑") || spatialRange.contains("楼")) {
            return "MICRO_SCALE";
        }
        return "MESO_SCALE"; // 默认中观尺度
    }

    /**
     * 平衡细节层次
     * 防止过多高细节对象造成视觉负载过重
     */
    private void balanceDetailLevels(Map<String, LoD> lodMap, FocusContext context) {
        // 统计各层次的对象数量
        int[] levelCounts = new int[4];
        for (LoD lod : lodMap.values()) {
            levelCounts[lod.getDetailLevel()]++;
        }

        // 如果高细节对象过多，进行调整
        int maxHighDetail = context.getObjects().size() / 3; // 最多1/3的对象可以是高细节

        if (levelCounts[3] > maxHighDetail) {
            // 将一些次重要的对象降低细节层次
            List<Map.Entry<String, LoD>> sortedEntries = new ArrayList<>(lodMap.entrySet());

            // 按DoI排序
            sortedEntries.sort((e1, e2) -> {
                DoI doi1 = context.getDoiMap().get(e1.getKey());
                DoI doi2 = context.getDoiMap().get(e2.getKey());
                return Double.compare(doi1.getInterestValue(), doi2.getInterestValue());
            });

            // 降低低兴趣度对象的细节层次
            int adjusted = 0;
            for (Map.Entry<String, LoD> entry : sortedEntries) {
                if (adjusted >= levelCounts[3] - maxHighDetail) {
                    break;
                }

                LoD lod = entry.getValue();
                if (lod.getDetailLevel() == 3) {
                    lod.setDetailLevel(2);
                    adjusted++;
                }
            }
        }
    }
}