package com.example.rdfsearch.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rdfsearch.model.RdfInstance;
import com.example.rdfsearch.service.SparqlService;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8080", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS
}, allowCredentials = "true")
public class RdfController {

    @Autowired
    private SparqlService sparqlService;

    /**
     * 基本长度搜索端点
     */
    @GetMapping("/search")
    public List<RdfInstance> searchByLength(
            @RequestParam(defaultValue = "0") int minLength,
            @RequestParam(defaultValue = "100") int maxLength) {

        return sparqlService.searchByLength(minLength, maxLength);
    }

    /**
     * 高级多属性搜索端点
     */
    @GetMapping("/advancedSearch")
    public List<RdfInstance> advancedSearch(
            // 长度参数
            @RequestParam(required = false) Float lengthMin,
            @RequestParam(required = false) Float lengthMax,

            // 微震事件数参数
            @RequestParam(required = false) Integer microseismicEventCountMin,
            @RequestParam(required = false) Integer microseismicEventCountMax,

            // 能量释放参数
            @RequestParam(required = false) Float energyReleaseMin,
            @RequestParam(required = false) Float energyReleaseMax,

            // 日能量释放参数
            @RequestParam(required = false) Float dailyEnergyReleaseMin,
            @RequestParam(required = false) Float dailyEnergyReleaseMax,

            // 风险类型参数（可以有多个）
            @RequestParam(required = false) List<String> riskType) {

        return sparqlService.advancedSearch(
                lengthMin, lengthMax,
                microseismicEventCountMin, microseismicEventCountMax,
                energyReleaseMin, energyReleaseMax,
                dailyEnergyReleaseMin, dailyEnergyReleaseMax,
                riskType);
    }

    /**
     * 根据任务个体执行搜索
     */
    // 新版本
    @GetMapping("/searchByTask")
    public ResponseEntity<?> searchByTask(
            @RequestParam String taskUri,
            @RequestParam(required = false) String userId) {
        Map<String, Object> response = sparqlService.searchByTask(taskUri, userId);
        return ResponseEntity.ok(response);
    }
}