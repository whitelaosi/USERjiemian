
package com.example.rdfsearch.service.multilevelFocus;

import com.example.rdfsearch.model.multilevelFocus.DoI;
import com.example.rdfsearch.model.multilevelFocus.FocusContext;
import com.example.rdfsearch.model.spatioTemporal.SpatioTemporalObject;
import com.example.rdfsearch.model.vizTask.VisualizationTask;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 任务聚焦服务 - 计算兴趣度(DoI)
 */
@Service
public class TaskFocusService {

    /**
     * 计算兴趣度
     * 
     * @param task    可视化任务
     * @param objects 时空对象列表
     * @return 兴趣度映射
     */
    public Map<String, DoI> calculateDoI(VisualizationTask task, List<SpatioTemporalObject> objects) {
        Map<String, DoI> doiMap = new HashMap<>();

        // 创建聚焦上下文
        FocusContext context = new FocusContext();
        context.setTask(task);
        context.setObjects(objects);

        // 为每个对象计算DoI
        for (SpatioTemporalObject obj : objects) {
            DoI doi = new DoI(obj.getId());

            // 计算兴趣度
            double interest = calculateObjectInterest(obj, task, context);
            doi.setInterestValue(interest);

            // 计算属性兴趣度
            Map<String, Double> attrInterest = calculateAttributeInterest(obj, task);
            doi.setAttributeInterest(attrInterest);

            doiMap.put(obj.getId(), doi);
        }

        // 传播兴趣度
        propagateInterest(doiMap);

        return doiMap;
    }

    private double calculateObjectInterest(SpatioTemporalObject obj, VisualizationTask task, FocusContext context) {
        double interest = 0.5; // 基础兴趣度

        // 1. 根据任务目标对象列表调整
        if (task.getTargetObjects().contains(obj.getId())) {
            interest += 0.3;
        }

        // 2. 根据对象类型调整
        if (task.getContext() != null) {
            List<String> targetTypes = (List<String>) task.getContext().getAdditionalInfo().get("targetTypes");
            if (targetTypes != null && targetTypes.contains(obj.getType())) {
                interest += 0.2;
            }
        }

        // 3. 根据空间距离调整（距离焦点越近兴趣度越高）
        // 这里简化处理，实际应根据空间坐标计算

        // 4. 根据时间相关性调整
        // 这里简化处理，实际应根据时间戳计算

        return Math.min(1.0, interest);
    }

    private Map<String, Double> calculateAttributeInterest(SpatioTemporalObject obj, VisualizationTask task) {
        Map<String, Double> attrInterest = new HashMap<>();

        // 获取任务关注的属性
        List<String> targetAttributes = task.getContext().getAttributes();

        // 为每个属性计算兴趣度
        for (Map.Entry<String, Object> attr : obj.getAttributes().entrySet()) {
            String attrName = attr.getKey();
            double interest = 0.3; // 基础值

            if (targetAttributes.contains(attrName)) {
                interest = 0.8;
            }

            attrInterest.put(attrName, interest);
        }

        return attrInterest;
    }

    private void propagateInterest(Map<String, DoI> doiMap) {
        // 简化的兴趣度传播
        // 实际应该基于对象间的关系进行传播

        for (DoI doi : doiMap.values()) {
            // 这里可以实现基于关系的传播逻辑
        }
    }
}