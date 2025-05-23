<template>
    <div class="task-execution-results">
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>正在加载执行结果...</p>
      </div>
      
      <div v-else class="results-container">
        <div class="results-header">
          <h2>{{ task.label || '任务' }} - 执行结果</h2>
          <button class="btn-close" @click="$emit('close')">关闭</button>
        </div>
        
        <!-- 执行摘要 -->
        <div class="execution-summary">
          <div class="summary-item">
            <span class="label">任务类型:</span>
            <span class="value">{{ getTaskTypeName() }}</span>
          </div>
          <div class="summary-item">
            <span class="label">结果总数:</span>
            <span class="value">{{ getResultsCount() }}</span>
          </div>
          <div class="summary-item">
            <span class="label">执行时间:</span>
            <span class="value">{{ getExecutionTime() }}</span>
          </div>
        </div>
        
        <!-- 基于任务类型的结果显示 -->
        <div class="results-content">
          <!-- SearchEvent结果 -->
          <div v-if="task.type === 'SearchEvent'" class="search-results">
            <h3>搜索结果</h3>
            <table v-if="hasData()">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>能量 (J)</th>
                  <th>时间</th>
                  <th>位置</th>
                </tr>
              </thead>
              <tbody>
  <tr v-for="(event, index) in getResults()" :key="index">
    <td>{{ event.ID }}</td>
    <td>{{ event['能量 (J)'] }}</td>
    <td>{{ event['时间'] }}</td>
    <td>{{ event['位置'] }}</td>
  </tr>
</tbody>
            </table>
            <div v-else class="no-data">未找到符合条件的数据</div>
          </div>
          
          <!-- RiskAnalysis结果 -->
          <div v-else-if="task.type === 'RiskAnalysis'" class="risk-results">
            <h3>风险分析结果</h3>
            <div v-if="hasData()" class="risk-summary">
              <div class="risk-level">
                <h4>风险等级: {{ getRiskLevel() }}</h4>
                <div class="risk-indicator" :class="getRiskLevelClass()"></div>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>区域</th>
                    <th>风险因素</th>
                    <th>评分</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in getRiskFactors()" :key="index">
                    <td>{{ item.area }}</td>
                    <td>{{ item.factor }}</td>
                    <td>{{ item.score }}</td>
                    <td>{{ item.status }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="no-data">未找到风险数据</div>
          </div>
          
          <!-- HistoricalComparison结果 -->
          <div v-else-if="task.type === 'HistoricalComparison'" class="comparison-results">
            <h3>历史对比结果</h3>
            <div v-if="hasData()" class="comparison-summary">
              <div class="period-comparison">
                <div class="period">
                  <h4>第一时间段</h4>
                  <p>{{ formatPeriod('periodOne') }}</p>
                  <p class="metric">{{ getMetricValue('periodOne') }}</p>
                </div>
                <div class="comparison-indicator">
                  <span class="arrow" :class="getComparisonDirection()"></span>
                  <span class="percentage">{{ getComparisonPercentage() }}</span>
                </div>
                <div class="period">
                  <h4>第二时间段</h4>
                  <p>{{ formatPeriod('periodTwo') }}</p>
                  <p class="metric">{{ getMetricValue('periodTwo') }}</p>
                </div>
              </div>
              <div class="comparison-details">
                <h4>对比指标: {{ getComparisonMetric() }}</h4>
                <table>
                  <thead>
                    <tr>
                      <th>属性</th>
                      <th>第一时间段</th>
                      <th>第二时间段</th>
                      <th>变化量</th>
                      <th>变化率</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, index) in getComparisonDetails()" :key="index">
                      <td>{{ item.property }}</td>
                      <td>{{ item.period1Value }}</td>
                      <td>{{ item.period2Value }}</td>
                      <td>{{ item.difference }}</td>
                      <td>{{ item.percentage }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else class="no-data">未找到对比数据</div>
          </div>
          
          <!-- 通用结果展示 -->
          <div v-else class="generic-results">
            <h3>执行结果</h3>
            <pre class="json-result">{{ JSON.stringify(results, null, 2) }}</pre>
          </div>
        </div>
        
        <!-- 导出和其他操作 -->
        <div class="results-actions">
          <button class="btn-export" @click="exportResults">导出结果</button>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { getTaskType } from '../../models/taskTypes';
  
  export default {
    name: 'TaskExecutionResults',
    props: {
      task: {
        type: Object,
        required: true
      },
      results: {
        type: Object,
        default: () => ({})
      },
      loading: {
        type: Boolean,
        default: false
      }
    },
    methods: {
  getTaskTypeName() {
    try {
      const taskType = getTaskType(this.task.type);
      return taskType ? taskType.label : (this.task.type || '未知类型');
    } catch (e) {
      console.error("获取任务类型名称错误:", e);
      return this.task.type || '未知类型';
    }
  },
  
  hasData() {
    if (!this.results) return false;
    
    // 基于任务类型检查是否有结果数据
    if (this.task.type === 'SearchEvent') {
      return Array.isArray(this.results.results) && this.results.results.length > 0;
    } else if (this.task.type === 'RiskAnalysis') {
      return this.results.riskFactors && this.results.riskFactors.length > 0;
    } else if (this.task.type === 'HistoricalComparison') {
      // 更新检查逻辑，确保有基本数据
      return this.results.periodOne && this.results.periodTwo;
    }
    
    return Object.keys(this.results).length > 0;
  },
  
  getResultsCount() {
    if (!this.results) return 0;
    
    if (this.task.type === 'SearchEvent') {
      return this.results.totalCount || 
            (Array.isArray(this.results.results) ? this.results.results.length : 0);
    } else if (this.task.type === 'RiskAnalysis') {
      return this.results.riskFactors ? this.results.riskFactors.length : 0;
    } else if (this.task.type === 'HistoricalComparison') {
      return 1; // 历史比较通常只有一个结果项
    }
    
    return 0;
  },
  
  getExecutionTime() {
    return this.results && this.results.executionTime ? this.results.executionTime : new Date().toLocaleString();
  },
  
  getResults() {
    if (this.task.type === 'SearchEvent') {
      return this.results && this.results.results ? this.results.results : [];
    }
    return [];
  },
  
  getEventId(event) {
    if (!event) return 'N/A';
    return event.id || (event.uri ? event.uri.split('/').pop() : 'N/A');
  },
  
  formatDateTime(dateTime) {
    if (!dateTime) return 'N/A';
    try {
      return new Date(dateTime).toLocaleString();
    } catch (e) {
      return dateTime;
    }
  },
  
  // RiskAnalysis相关方法
  getRiskLevel() {
    if (!this.results || !this.results.riskLevel) return '未知';
    return this.results.riskLevel;
  },
  
  getRiskLevelClass() {
    const level = this.getRiskLevel().toLowerCase();
    if (level.includes('高')) return 'high-risk';
    if (level.includes('中')) return 'medium-risk';
    if (level.includes('低')) return 'low-risk';
    return 'unknown-risk';
  },
  
  getRiskFactors() {
    return this.results && this.results.riskFactors ? this.results.riskFactors : [];
  },
  
  // HistoricalComparison相关方法
  formatPeriod(periodKey) {
    if (!this.results || !this.results[periodKey]) return 'N/A';
    
    // 处理字符串格式的期间，如 "2023-04-27/2023-04-28"
    const periodStr = this.results[periodKey];
    if (typeof periodStr === 'string' && periodStr.includes('/')) {
      const [start, end] = periodStr.split('/');
      return `${start} - ${end}`;
    }
    
    // 保留原有逻辑作为后备
    const period = this.results[periodKey];
    if (period && period.start && period.end) {
      return `${this.formatDateTime(period.start)} - ${this.formatDateTime(period.end)}`;
    }
    return 'N/A';
  },
  
  getMetricValue(periodKey) {
    // 处理实际API返回的periodOneValue和periodTwoValue
    const valueKey = periodKey + 'Value'; // 转换为 'periodOneValue' 或 'periodTwoValue'
    if (this.results && this.results[valueKey] !== undefined) {
      return this.results[valueKey];
    }
    
    // 保留原有逻辑作为后备
    if (!this.results || !this.results[periodKey]) return 'N/A';
    return this.results[periodKey].value || 'N/A';
  },
  
  // 添加新方法
  getComparisonDirection() {
    if (!this.results || this.results.changePercent === undefined) return 'equal';
    
    const changePercent = parseFloat(this.results.changePercent);
    if (isNaN(changePercent)) return 'equal';
    
    if (changePercent > 0) return 'up';
    if (changePercent < 0) return 'down';
    return 'equal';
  },
  
  getComparisonPercentage() {
    if (!this.results || this.results.changePercent === undefined) return '0%';
    
    const changePercent = parseFloat(this.results.changePercent);
    if (isNaN(changePercent)) return '0%';
    
    return `${Math.abs(changePercent).toFixed(2)}%`;
  },
  
  getComparisonMetric() {
    if (!this.results || !this.results.metric) return '未知';
    
    const metric = this.results.metric;
    switch (metric) {
      case 'count': return '事件数量';
      case 'totalEnergy': return '总能量';
      case 'maxEnergy': return '最大能量';
      case '能量阈值': return '能量阈值';
      default: return metric;
    }
  },
  
  getComparisonDetails() {
    if (!this.results) return [];
    
    // 如果API没有返回details，构建基本比较详情
    try {
      const periodOneValue = parseFloat(this.results.periodOneValue);
      const periodTwoValue = parseFloat(this.results.periodTwoValue);
      
      if (isNaN(periodOneValue) || isNaN(periodTwoValue)) {
        return [{
          property: this.getComparisonMetric(),
          period1Value: this.results.periodOneValue || 'N/A',
          period2Value: this.results.periodTwoValue || 'N/A',
          difference: 'N/A',
          percentage: this.results.changePercent !== undefined ? `${this.results.changePercent}%` : 'N/A'
        }];
      }
      
      const difference = periodTwoValue - periodOneValue;
      
      return [{
        property: this.getComparisonMetric(),
        period1Value: periodOneValue.toFixed(2),
        period2Value: periodTwoValue.toFixed(2),
        difference: difference.toFixed(2),
        percentage: `${(this.results.changePercent || 0).toFixed(2)}%`
      }];
    } catch (e) {
      console.error("构建比较详情出错:", e);
      return [];
    }
  },
  
  exportResults() {
    try {
      const dataStr = JSON.stringify(this.results, null, 2);
      const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
      
      const exportName = `${this.task.type || 'task'}_results_${new Date().getTime()}.json`;
      
      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportName);
      linkElement.click();
      
      this.$emit('message', '结果已导出', 'success');
    } catch (error) {
      console.error('导出结果失败:', error);
      this.$emit('message', '导出结果失败', 'error');
    }
  }
}
  }
  </script>
  
  <style scoped>
  .task-execution-results {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    padding: 20px;
    margin-top: 20px;
  }
  
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 200px;
  }
  
  .spinner {
    border: 4px solid #f3f3f3;
    border-top: 4px solid #3498db;
    border-radius: 50%;
    width: 30px;
    height: 30px;
    animation: spin 1s linear infinite;
    margin-bottom: 10px;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  .results-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
  }
  
  .results-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: #333;
  }
  
  .btn-close {
    background: none;
    border: none;
    color: #666;
    cursor: pointer;
    font-size: 1rem;
  }
  
  .execution-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
    margin-bottom: 20px;
    background: #f8f9fa;
    padding: 15px;
    border-radius: 5px;
  }
  
  .summary-item {
    display: flex;
    flex-direction: column;
  }
  
  .summary-item .label {
    font-size: 0.85rem;
    color: #666;
  }
  
  .summary-item .value {
    font-size: 1.1rem;
    font-weight: 500;
  }
  
  .results-content {
    margin-bottom: 20px;
  }
  
  table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
  }
  
  table th, table td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid #eee;
  }
  
  table th {
    background: #f3f4f6;
    font-weight: 500;
  }
  
  .no-data {
    padding: 20px;
    text-align: center;
    color: #666;
    font-style: italic;
    background: #f8f9fa;
    border-radius: 5px;
  }
  
  .risk-level {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
  }
  
  .risk-indicator {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    margin-left: 10px;
  }
  
  .high-risk {
    background-color: #f44336;
  }
  
  .medium-risk {
    background-color: #ff9800;
  }
  
  .low-risk {
    background-color: #4caf50;
  }
  
  .unknown-risk {
    background-color: #9e9e9e;
  }
  
  .period-comparison {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .period {
    flex: 1;
    padding: 15px;
    background: #f3f4f6;
    border-radius: 5px;
    text-align: center;
  }
  
  .period h4 {
    margin-top: 0;
  }
  
  .period .metric {
    font-size: 1.5rem;
    font-weight: bold;
  }
  
  .comparison-indicator {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0 20px;
  }
  
  .arrow {
    font-size: 2rem;
  }
  
  .arrow.up:before {
    content: '↑';
    color: #4caf50;
  }
  
  .arrow.down:before {
    content: '↓';
    color: #f44336;
  }
  
  .arrow.equal:before {
    content: '=';
    color: #9e9e9e;
  }
  
  .percentage {
    font-weight: bold;
  }
  
  .json-result {
    background: #f3f4f6;
    padding: 15px;
    border-radius: 5px;
    overflow-x: auto;
    font-family: monospace;
    white-space: pre-wrap;
  }
  
  .results-actions {
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #eee;
    padding-top: 15px;
  }
  
  .btn-export {
    background-color: #4caf50;
    color: white;
    border: none;
    padding: 8px 16px;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .btn-export:hover {
    background-color: #388e3c;
  }
  </style>