package com.example.rdfsearch.controller.semanticVisual;

import com.example.rdfsearch.model.semanticVisual.SemanticVisualVariable;
import com.example.rdfsearch.model.vizTask.VisualizationTask;
import com.example.rdfsearch.service.semanticVisual.SVVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 语义视觉变量控制器
 */
@RestController
@RequestMapping("/api/svv")
@CrossOrigin
public class SVVController {

    @Autowired
    private SVVService svvService;

    // 获取任务的推荐SVV
    @PostMapping("/recommend")
    public ResponseEntity<List<SemanticVisualVariable>> recommendSVVs(
            @RequestBody VisualizationTask task) {

        List<SemanticVisualVariable> recommendedSVVs = svvService.selectSVVsForTask(task);
        return ResponseEntity.ok(recommendedSVVs);
    }

    // 组合SVV
    @PostMapping("/combine")
    public ResponseEntity<SemanticVisualVariable> combineSVVs(
            @RequestBody List<SemanticVisualVariable> svvs) {

        SemanticVisualVariable combined = svvService.combineSVVs(svvs);
        return ResponseEntity.ok(combined);
    }

    // 获取SVV参数化表示
    @GetMapping("/{svvId}/parameters")
    public ResponseEntity<Map<String, Object>> getSVVParameters(@PathVariable String svvId) {
        // 实现获取SVV参数的逻辑
        return ResponseEntity.ok(Map.of());
    }

    // 应用SVV到对象
    @PostMapping("/apply")
    public ResponseEntity<?> applySVV(
            @RequestParam String objectId,
            @RequestBody SemanticVisualVariable svv) {

        // 实现应用SVV的逻辑
        return ResponseEntity.ok("SVV applied successfully");
    }
}
