package com.example.rdfsearch.service.semanticVisual;

import com.example.rdfsearch.model.semanticVisual.*;
import com.example.rdfsearch.model.vizTask.VisualizationTask;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 语义视觉变量服务 - 实现PDF第3章的核心逻辑
 */
@Service
public class SVVService {

    private Map<String, SemanticVisualVariable> svvRepository;

    public SVVService() {
        this.svvRepository = new HashMap<>();
        initializeBaseSVVs();
    }

    // 初始化基础语义视觉变量
    private void initializeBaseSVVs() {
        // 空间维度变量
        createSpatialSVV("position", "位置", Arrays.asList("x", "y", "z"));
        createSpatialSVV("size", "尺寸", Arrays.asList("width", "height", "depth"));
        createSpatialSVV("shape", "形状", Arrays.asList("geometry", "topology"));

        // 时间维度变量
        createTemporalSVV("duration", "持续时长", Arrays.asList("startTime", "endTime"));
        createTemporalSVV("frequency", "频率", Arrays.asList("interval", "count"));

        // 外观维度变量
        createAppearanceSVV("color", "颜色", Arrays.asList("hue", "saturation", "intensity"));
        createAppearanceSVV("transparency", "透明度", Arrays.asList("alpha"));
    }

    // 创建空间维度SVV
    private void createSpatialSVV(String id, String name, List<String> baseVars) {
        SemanticVisualVariable svv = new SemanticVisualVariable();
        svv.setId(id);
        svv.setName(name);
        svv.setCategory("SPATIAL");
        svv.setBaseVariables(baseVars);
        svvRepository.put(id, svv);
    }

    // 创建时间维度SVV
    private void createTemporalSVV(String id, String name, List<String> baseVars) {
        SemanticVisualVariable svv = new SemanticVisualVariable();
        svv.setId(id);
        svv.setName(name);
        svv.setCategory("TEMPORAL");
        svv.setBaseVariables(baseVars);
        svvRepository.put(id, svv);
    }

    // 创建外观维度SVV
    private void createAppearanceSVV(String id, String name, List<String> baseVars) {
        SemanticVisualVariable svv = new SemanticVisualVariable();
        svv.setId(id);
        svv.setName(name);
        svv.setCategory("APPEARANCE");
        svv.setBaseVariables(baseVars);
        svvRepository.put(id, svv);
    }

    // 根据任务选择合适的SVV
    public List<SemanticVisualVariable> selectSVVsForTask(VisualizationTask task) {
        List<SemanticVisualVariable> selectedSVVs = new ArrayList<>();

        // 根据任务类型选择
        switch (task.getTaskType()) {
            case PRESENTATIONAL:
                // 展示性任务：强调外观
                selectedSVVs.addAll(getAppearanceSVVs());
                selectedSVVs.add(svvRepository.get("position"));
                break;
            case ANALYTICAL:
                // 分析性任务：强调数值映射
                selectedSVVs.add(svvRepository.get("size"));
                selectedSVVs.add(svvRepository.get("color"));
                selectedSVVs.add(svvRepository.get("position"));
                break;
            case EXPLORATORY:
                // 探索性任务：强调交互和动态
                selectedSVVs.addAll(getTemporalSVVs());
                selectedSVVs.addAll(getSpatialSVVs());
                break;
        }

        return selectedSVVs;
    }

    // 语义组合操作
    public SemanticVisualVariable combineSVVs(List<SemanticVisualVariable> svvs) {
        if (svvs.isEmpty())
            return null;

        SemanticVisualVariable combined = svvs.get(0);
        for (int i = 1; i < svvs.size(); i++) {
            combined = combined.combine(svvs.get(i));
        }

        return combined;
    }

    // 根据影响因素调整SVV参数
    public void adjustSVVParameters(SemanticVisualVariable svv, SVVInfluencingFactors.SpaceScale spaceScale) {
        Map<String, Object> params = svv.getParameters();

        // 根据空间尺度调整
        if (spaceScale == SVVInfluencingFactors.SpaceScale.MACRO_SCALE) {
            params.put("simplificationLevel", 0.8);
        } else if (spaceScale == SVVInfluencingFactors.SpaceScale.MICRO_SCALE) {
            params.put("detailLevel", 0.9);
        }

        svv.setParameters(params);
    }

    // 获取不同类别的SVV
    private List<SemanticVisualVariable> getSpatialSVVs() {
        return filterByCategory("SPATIAL");
    }

    private List<SemanticVisualVariable> getTemporalSVVs() {
        return filterByCategory("TEMPORAL");
    }

    private List<SemanticVisualVariable> getAppearanceSVVs() {
        return filterByCategory("APPEARANCE");
    }

    private List<SemanticVisualVariable> filterByCategory(String category) {
        List<SemanticVisualVariable> filtered = new ArrayList<>();
        for (SemanticVisualVariable svv : svvRepository.values()) {
            if (svv.getCategory().equals(category)) {
                filtered.add(svv);
            }
        }
        return filtered;
    }
}
