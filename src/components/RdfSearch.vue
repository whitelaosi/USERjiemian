<template>
  <div class="editor-link">
  <a href="/table-editor.html" target="_blank" class="btn btn-outline-primary">
    表格编辑器
  </a>
</div>
  <div class="search-container">
    <h2>RDF实例多属性查询</h2>
    
    <div class="search-panel">
      <div class="search-tabs">
        <button 
          v-for="(tab, index) in searchTabs" 
          :key="index"
          :class="['tab-button', { active: activeTab === tab.id }]"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </button>
      </div>
      
      <!-- 长度搜索 -->
      <div v-if="activeTab === 'length'" class="search-form">
        <div class="form-group">
          <label for="minLength">最小长度:</label>
          <input 
            type="number" 
            id="minLength" 
            v-model="searchParams.length.min" 
            min="0" 
            max="1000"
          />
        </div>
        
        <div class="form-group">
          <label for="maxLength">最大长度:</label>
          <input 
            type="number" 
            id="maxLength" 
            v-model="searchParams.length.max" 
            min="0" 
            max="1000"
          />
        </div>
      </div>
      
      <!-- 微震事件数搜索 -->
      <div v-if="activeTab === 'microseismicEventCount'" class="search-form">
        <div class="form-group">
          <label for="minEventCount">最小事件数:</label>
          <input 
            type="number" 
            id="minEventCount" 
            v-model="searchParams.microseismicEventCount.min" 
            min="0" 
            max="1000"
          />
        </div>
        
        <div class="form-group">
          <label for="maxEventCount">最大事件数:</label>
          <input 
            type="number" 
            id="maxEventCount" 
            v-model="searchParams.microseismicEventCount.max" 
            min="0" 
            max="1000"
          />
        </div>
      </div>
      
      <!-- 能量释放搜索 -->
      <div v-if="activeTab === 'energyRelease'" class="search-form">
        <div class="form-group">
          <label for="minEnergyRelease">最小能量释放:</label>
          <input 
            type="number" 
            id="minEnergyRelease" 
            v-model="searchParams.energyRelease.min" 
            min="0" 
            step="0.01"
          />
        </div>
        
        <div class="form-group">
          <label for="maxEnergyRelease">最大能量释放:</label>
          <input 
            type="number" 
            id="maxEnergyRelease" 
            v-model="searchParams.energyRelease.max" 
            min="0" 
            step="0.01"
          />
        </div>
      </div>
      
      <!-- 日能量释放搜索 -->
      <div v-if="activeTab === 'dailyEnergyRelease'" class="search-form">
        <div class="form-group">
          <label for="minDailyEnergyRelease">最小日能量释放:</label>
          <input 
            type="number" 
            id="minDailyEnergyRelease" 
            v-model="searchParams.dailyEnergyRelease.min" 
            min="0" 
            step="0.01"
          />
        </div>
        
        <div class="form-group">
          <label for="maxDailyEnergyRelease">最大日能量释放:</label>
          <input 
            type="number" 
            id="maxDailyEnergyRelease" 
            v-model="searchParams.dailyEnergyRelease.max" 
            min="0" 
            step="0.01"
          />
        </div>
      </div>
      
      <!-- 风险类型搜索 -->
      <div v-if="activeTab === 'riskType'" class="search-form">
        <div class="form-group checkbox-group">
          <label class="checkbox-label">
            <input type="checkbox" v-model="searchParams.riskType.green" />
            <span class="risk-badge green">低风险</span>
          </label>
          
          <label class="checkbox-label">
            <input type="checkbox" v-model="searchParams.riskType.yellow" />
            <span class="risk-badge yellow">中等风险</span>
          </label>
          
          <label class="checkbox-label">
            <input type="checkbox" v-model="searchParams.riskType.red" />
            <span class="risk-badge red">高风险</span>
          </label>
        </div>
      </div>
      
      <!-- 任务查询 (新增) -->
      <div v-if="activeTab === 'taskUri'" class="search-form">
        <div class="form-group task-uri-input">
          <label for="taskUriInput">任务 URI:</label>
          <input 
            type="text" 
            id="taskUriInput" 
            v-model="searchParams.taskUri" 
            placeholder="如 http://...#Task_001" 
            class="task-input"
          />
        </div>
        <div class="task-search-info">
          <p class="info-text">输入任务URI可自动提取目标段和时间范围进行查询</p>
        </div>
      </div>
      
      <div class="search-actions">
        <div class="active-filters" v-if="hasActiveFilters">
          <span class="filter-title">活动过滤器:</span>
          <div class="filter-badges">
            <span 
              v-for="(filter, index) in activeFilters" 
              :key="index" 
              class="filter-badge"
            >
              {{ filter.label }}: {{ filter.value }}
              <button class="remove-filter" @click="removeFilter(filter.id)">×</button>
            </span>
          </div>
        </div>
        
        <div class="buttons">
          <button v-if="activeTab !== 'taskUri'" @click="addCurrentFilter" class="add-filter-button" :disabled="!canAddCurrentFilter">
            添加当前过滤器
          </button>
          <button v-if="activeTab !== 'taskUri'" @click="searchData" class="search-button" :disabled="!hasActiveFilters">搜索</button>
          <button v-if="activeTab === 'taskUri'" @click="searchByTaskUri" class="search-button" :disabled="!searchParams.taskUri">根据任务查询</button>
          <button @click="resetFilters" class="reset-button">重置</button>
        </div>
      </div>
    </div>
    
    <div v-if="loading" class="loading">
      正在加载...
    </div>
    
    <div v-else-if="error" class="error">
      {{ error }}
    </div>
    
    <div v-else-if="results.length > 0" class="results">
      <h3>查询结果 ({{ results.length }})</h3>
      <div v-if="activeTab === 'taskUri' && searchedTask" class="task-info-panel">
        <h4>任务信息</h4>
        <div class="task-details">
          <div class="task-detail-item">
            <span class="detail-label">目标段:</span>
            <span class="detail-value">{{ searchedTask.targetEntity || '未指定' }}</span>
          </div>
          <div class="task-detail-item">
            <span class="detail-label">开始日期:</span>
            <span class="detail-value">{{ searchedTask.startDate || '未指定' }}</span>
          </div>
          <div class="task-detail-item">
            <span class="detail-label">结束日期:</span>
            <span class="detail-value">{{ searchedTask.endDate || '未指定' }}</span>
          </div>
        </div>
      </div>
      <div class="instance-cards">
        <div v-for="(result, index) in results" :key="index" class="instance-card">
          <div class="card-header">
            <h4>{{ result.name }}</h4>
            <div class="match-badges">
              <span 
                v-for="(match, idx) in getMatchingFilters(result)" 
                :key="idx" 
                class="match-badge"
                :class="getMatchClass(match.type)"
              >
                {{ match.label }}
              </span>
            </div>
          </div>
          
          <div class="property-grid">
            <div class="property" v-if="result.length !== null">
              <div class="property-label">长度:</div>
              <div class="property-value" :class="{ 'highlight': isValueHighlighted(result.length, 'length') }">
                {{ result.length }}
              </div>
            </div>
            
            <div class="property" v-if="result.microseismicEventCount !== null">
              <div class="property-label">微震事件数:</div>
              <div class="property-value" :class="{ 'highlight': isValueHighlighted(result.microseismicEventCount, 'microseismicEventCount') }">
                {{ result.microseismicEventCount }}
              </div>
            </div>
            
            <div class="property" v-if="result.energyRelease !== null">
              <div class="property-label">能量释放:</div>
              <div class="property-value" :class="{ 'highlight': isValueHighlighted(result.energyRelease, 'energyRelease') }">
                {{ result.energyRelease }}
              </div>
            </div>
            
            <div class="property" v-if="result.cumulativeApparentVolume !== null">
              <div class="property-label">累计表观体积:</div>
              <div class="property-value">{{ result.cumulativeApparentVolume }}</div>
            </div>
            
            <div class="property" v-if="result.dailyEnergyRelease !== null">
              <div class="property-label">日能量释放:</div>
              <div class="property-value" :class="{ 'highlight': isValueHighlighted(result.dailyEnergyRelease, 'dailyEnergyRelease') }">
                {{ result.dailyEnergyRelease }}
              </div>
            </div>
            
            <div class="property" v-if="result.dailyApparentVolumeRate !== null">
              <div class="property-label">日表观体积率:</div>
              <div class="property-value">{{ result.dailyApparentVolumeRate }}</div>
            </div>
            
            <div class="property" v-if="result.serialNumber !== null">
              <div class="property-label">序列号:</div>
              <div class="property-value">{{ result.serialNumber }}</div>
            </div>
          </div>
          
          <div class="mileage-info" v-if="result.mileage">
            <div class="property-label">里程:</div>
            <div class="property-value">{{ result.mileage }}</div>
          </div>
          
          <div class="mileage-details">
            <div class="property half" v-if="result.startMileage">
              <div class="property-label">起始里程:</div>
              <div class="property-value">{{ result.startMileage }}</div>
            </div>
            
            <div class="property half" v-if="result.endMileage">
              <div class="property-label">结束里程:</div>
              <div class="property-value">{{ result.endMileage }}</div>
            </div>
          </div>
          
          <div class="description" v-if="result.description">
            <div class="property-label">描述:</div>
            <div class="property-value">{{ result.description }}</div>
          </div>
          
          <div class="risk-type" v-if="result.riskType" :class="result.riskType">
            <div class="property-label">风险类型:</div>
            <div class="property-value" :class="{ 'highlight': isRiskTypeHighlighted(result.riskType) }">
              {{ formatRiskType(result.riskType) }}
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="searched" class="no-results">
      未找到结果
    </div>
  </div>
</template>

<script>
export default {
  name: 'RdfSearch',
  data() {
    return {
      activeTab: 'length',
      searchTabs: [
        { id: 'length', name: '长度' },
        { id: 'microseismicEventCount', name: '微震事件数' },
        { id: 'energyRelease', name: '能量释放' },
        { id: 'dailyEnergyRelease', name: '日能量释放' },
        { id: 'riskType', name: '风险类型' },
        { id: 'taskUri', name: '任务查询' } // 新增的任务查询tab
      ],
      searchParams: {
        length: { min: 0, max: 50 },
        microseismicEventCount: { min: 0, max: 100 },
        energyRelease: { min: 0, max: 10 },
        dailyEnergyRelease: { min: 0, max: 10 },
        riskType: { green: false, yellow: false, red: false },
        taskUri: '' // 新增字段
      },
      activeFilters: [], // 存储激活的过滤器
      results: [],
      loading: false,
      error: null,
      searched: false,
      searchedTask: null // 存储当前查询的任务信息
    }
  },
  computed: {
    hasActiveFilters() {
      return this.activeFilters.length > 0;
    },
    canAddCurrentFilter() {
      switch(this.activeTab) {
        case 'length':
          return this.searchParams.length.min <= this.searchParams.length.max;
        case 'microseismicEventCount':
          return this.searchParams.microseismicEventCount.min <= this.searchParams.microseismicEventCount.max;
        case 'energyRelease':
          return this.searchParams.energyRelease.min <= this.searchParams.energyRelease.max;
        case 'dailyEnergyRelease':
          return this.searchParams.dailyEnergyRelease.min <= this.searchParams.dailyEnergyRelease.max;
        case 'riskType':
          return this.searchParams.riskType.green || 
                 this.searchParams.riskType.yellow || 
                 this.searchParams.riskType.red;
        default:
          return false;
      }
    }
  },
  methods: {
    addCurrentFilter() {
      // 根据当前选中的标签添加过滤器
      switch(this.activeTab) {
        case 'length':
          // 检查是否已存在相同的过滤器
          if (!this.filterExists('length')) {
            this.activeFilters.push({
              id: 'length',
              type: 'range',
              label: '长度',
              value: `${this.searchParams.length.min} - ${this.searchParams.length.max}`,
              min: this.searchParams.length.min,
              max: this.searchParams.length.max
            });
          }
          break;
        case 'microseismicEventCount':
          if (!this.filterExists('microseismicEventCount')) {
            this.activeFilters.push({
              id: 'microseismicEventCount',
              type: 'range',
              label: '微震事件数',
              value: `${this.searchParams.microseismicEventCount.min} - ${this.searchParams.microseismicEventCount.max}`,
              min: this.searchParams.microseismicEventCount.min,
              max: this.searchParams.microseismicEventCount.max
            });
          }
          break;
        case 'energyRelease':
          if (!this.filterExists('energyRelease')) {
            this.activeFilters.push({
              id: 'energyRelease',
              type: 'range',
              label: '能量释放',
              value: `${this.searchParams.energyRelease.min} - ${this.searchParams.energyRelease.max}`,
              min: this.searchParams.energyRelease.min,
              max: this.searchParams.energyRelease.max
            });
          }
          break;
        case 'dailyEnergyRelease':
          if (!this.filterExists('dailyEnergyRelease')) {
            this.activeFilters.push({
              id: 'dailyEnergyRelease',
              type: 'range',
              label: '日能量释放',
              value: `${this.searchParams.dailyEnergyRelease.min} - ${this.searchParams.dailyEnergyRelease.max}`,
              min: this.searchParams.dailyEnergyRelease.min,
              max: this.searchParams.dailyEnergyRelease.max
            });
          }
          break;
        case 'riskType':
          if (!this.filterExists('riskType')) {
            const selectedRiskTypes = [];
            if (this.searchParams.riskType.green) selectedRiskTypes.push('低风险');
            if (this.searchParams.riskType.yellow) selectedRiskTypes.push('中等风险');
            if (this.searchParams.riskType.red) selectedRiskTypes.push('高风险');
            
            if (selectedRiskTypes.length > 0) {
              this.activeFilters.push({
                id: 'riskType',
                type: 'enum',
                label: '风险类型',
                value: selectedRiskTypes.join(', '),
                selected: { ...this.searchParams.riskType }
              });
            }
          }
          break;
      }
    },
    
    filterExists(id) {
      return this.activeFilters.some(filter => filter.id === id);
    },
    
    removeFilter(id) {
      const index = this.activeFilters.findIndex(filter => filter.id === id);
      if (index !== -1) {
        this.activeFilters.splice(index, 1);
      }
    },
    
    resetFilters() {
      this.activeFilters = [];
      this.searchParams = {
        length: { min: 0, max: 50 },
        microseismicEventCount: { min: 0, max: 100 },
        energyRelease: { min: 0, max: 10 },
        dailyEnergyRelease: { min: 0, max: 10 },
        riskType: { green: false, yellow: false, red: false },
        taskUri: '' // 重置任务URI
      };
      this.results = [];
      this.searched = false;
      this.searchedTask = null; // 重置任务信息
    },
    
    async searchData() {
      if (!this.hasActiveFilters) return;
      
      this.loading = true;
      this.error = null;
      this.results = [];
      this.searched = true;
      this.searchedTask = null; // 清除任务信息
      
      try {
        // 构建查询参数
        const queryParams = new URLSearchParams();
        
        // 添加所有激活的过滤器
        this.activeFilters.forEach(filter => {
          if (filter.type === 'range') {
            queryParams.append(`${filter.id}Min`, filter.min);
            queryParams.append(`${filter.id}Max`, filter.max);
          } else if (filter.type === 'enum' && filter.id === 'riskType') {
            // 添加风险类型过滤器
            if (filter.selected.green) queryParams.append('riskType', 'green');
            if (filter.selected.yellow) queryParams.append('riskType', 'yellow');
            if (filter.selected.red) queryParams.append('riskType', 'red');
          }
        });
        
        // 使用后端API
        const response = await fetch(`http://localhost:8081/api/advancedSearch?${queryParams.toString()}`);
        
        if (!response.ok) {
          throw new Error(`错误: ${response.status}`);
        }
        
        const data = await response.json();
        this.results = data;
        
        console.log('从后端接收到的数据:', data); // 调试用
      } catch (err) {
        this.error = `查询失败: ${err.message}`;
        console.error('API错误:', err);
      } finally {
        this.loading = false;
      }
    },
    
    // 根据任务URI查询的新方法
    async searchByTaskUri() {
  if (!this.searchParams.taskUri) return;
  
  this.loading = true;
  this.error = null;
  this.results = [];
  this.searched = true;
  this.searchedTask = null;
  
  try {
    // 添加userId参数
    const response = await fetch(`http://localhost:8081/api/searchByTask?taskUri=${encodeURIComponent(this.searchParams.taskUri)}&userId=anonymous`);
    
    if (!response.ok) {
      throw new Error(`错误: ${response.status}`);
    }
    
    const data = await response.json();
    
    // 其余代码保持不变
    if (data.taskInfo) {
      this.searchedTask = {
        targetEntity: data.taskInfo.targetEntity,
        startDate: data.taskInfo.startDate,
        endDate: data.taskInfo.endDate
      };
      this.results = data.results || [];
    } else {
      this.results = data;
    }
    
    console.log('任务查询结果:', data);
  } catch (err) {
    this.error = `任务查询失败: ${err.message}`;
    console.error('任务API错误:', err);
  } finally {
    this.loading = false;
  }
},
    
    formatRiskType(riskType) {
      const riskMap = {
        'green': '低风险',
        'yellow': '中等风险',
        'red': '高风险'
      };
      
      return riskMap[riskType] || riskType;
    },
    
    isValueHighlighted(value, propertyName) {
      // 检查值是否匹配某个过滤器
      const filter = this.activeFilters.find(f => f.id === propertyName);
      if (filter && filter.type === 'range') {
        return value >= filter.min && value <= filter.max;
      }
      return false;
    },
    
    isRiskTypeHighlighted(riskType) {
      const filter = this.activeFilters.find(f => f.id === 'riskType');
      if (filter && filter.type === 'enum') {
        return filter.selected[riskType] === true;
      }
      return false;
    },
    
    getMatchingFilters(result) {
      // 返回匹配的过滤器列表，用于显示标记
      const matches = [];
      
      this.activeFilters.forEach(filter => {
        if (filter.type === 'range') {
          const value = result[filter.id];
          if (value !== null && value >= filter.min && value <= filter.max) {
            matches.push({
              type: filter.id,
              label: filter.label
            });
          }
        } else if (filter.type === 'enum' && filter.id === 'riskType') {
          if (filter.selected[result.riskType] === true) {
            matches.push({
              type: 'riskType',
              label: this.formatRiskType(result.riskType)
            });
          }
        }
      });
      
      return matches;
    },
    
    getMatchClass(type) {
      // 为不同类型的匹配返回不同的样式类
      const classMap = {
        'length': 'length-match',
        'microseismicEventCount': 'event-match',
        'energyRelease': 'energy-match',
        'dailyEnergyRelease': 'daily-energy-match',
        'riskType': 'risk-match'
      };
      
      return classMap[type] || '';
    }
  }
}
</script>

<style scoped>
.search-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  max-width: 1000px;
  margin: 0 auto;
}

.search-panel {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
}

.search-tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9;
  overflow-x: auto;
}

.tab-button {
  padding: 12px 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  color: #666;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-button:hover {
  background-color: #f0f0f0;
}

.tab-button.active {
  background-color: #fff;
  color: #2c3e50;
  border-bottom: 2px solid #2c3e50;
}

.search-form {
  padding: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.search-actions {
  padding: 15px 20px;
  background-color: #f9f9f9;
  border-top: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.active-filters {
  margin-bottom: 15px;
}

.filter-title {
  font-weight: bold;
  margin-right: 10px;
  color: #666;
  font-size: 0.9em;
}

.filter-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.filter-badge {
  display: inline-flex;
  align-items: center;
  background-color: #e8f4fd;
  color: #0066cc;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.85em;
}

.remove-filter {
  background: none;
  border: none;
  color: #0066cc;
  cursor: pointer;
  margin-left: 5px;
  font-weight: bold;
  font-size: 1.1em;
}

.buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.add-filter-button, .search-button, .reset-button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.add-filter-button {
  background-color: #e8f4fd;
  color: #0066cc;
}

.add-filter-button:hover:not(:disabled) {
  background-color: #d0e8fc;
}

.search-button {
  background-color: #2c3e50;
  color: white;
}

.search-button:hover:not(:disabled) {
  background-color: #1a2530;
}

.reset-button {
  background-color: #f2f2f2;
  color: #666;
}

.reset-button:hover {
  background-color: #e6e6e6;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-group {
  margin-right: 20px;
  margin-bottom: 10px;
}

.checkbox-group {
  display: flex;
  gap: 15px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.risk-badge {
  padding: 4px 10px;
  border-radius: 4px;
  margin-left: 5px;
  font-size: 0.85em;
}

.risk-badge.green {
  background-color: rgba(76, 175, 80, 0.2);
  color: #2e7d32;
}

.risk-badge.yellow {
  background-color: rgba(255, 193, 7, 0.2);
  color: #f57f17;
}

.risk-badge.red {
  background-color: rgba(244, 67, 54, 0.2);
  color: #d32f2f;
}

label {
  font-weight: 500;
  margin-right: 10px;
}

input[type="number"] {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 80px;
}

/* 任务URI输入样式 */
.task-uri-input {
  width: 100%;
  margin-bottom: 15px;
}

.task-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.task-search-info {
  width: 100%;
  margin-top: 10px;
}

.info-text {
  color: #666;
  font-size: 0.9em;
  font-style: italic;
}

/* 任务信息面板 */
.task-info-panel {
  background-color: #f0f7ff;
  border: 1px solid #d0e3ff;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 20px;
}

.task-details {
  display: flex;
  flex-direction: column;  /* 改为垂直布局 */
  gap: 12px;
  margin-top: 10px;
}

.task-detail-item {
  display: grid;
  grid-template-columns: 120px 1fr;  /* 固定标签宽度，内容自适应 */
  align-items: flex-start;
}

.detail-label {
  font-weight: 500;
  color: #555;
  white-space: nowrap;
}

.detail-value {
  color: #0066cc;
  font-weight: 500;
  word-break: break-all;  /* 允许长URL在任何位置换行 */
  overflow-wrap: break-word;
} 

.instance-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.instance-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  background-color: #f9f9f9;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.card-header {
  margin-bottom: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.card-header h4 {
  margin-top: 0;
  margin-bottom: 8px;
  font-size: 1.1em;
}

.match-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.match-badge {
  font-size: 0.75em;
  padding: 2px 8px;
  border-radius: 12px;
  background-color: #f2f2f2;
  color: #666;
}

.length-match {
  background-color: #e3f2fd;
  color: #1565c0;
}

.event-match {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.energy-match {
  background-color: #fff3e0;
  color: #e65100;
}

.daily-energy-match {
  background-color: #ede7f6;
  color: #4527a0;
}

.risk-match.green {
  background-color: rgba(76, 175, 80, 0.2);
  color: #2e7d32;
}

.risk-match.yellow {
  background-color: rgba(255, 193, 7, 0.2);
  color: #f57f17;
}

.risk-match.red {
  background-color: rgba(244, 67, 54, 0.2);
  color: #d32f2f;
}

.property-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.property {
  margin-bottom: 8px;
}

.property.half {
  width: 50%;
  display: inline-block;
}

.property-label {
  font-weight: 500;
  font-size: 0.9em;
  color: #666;
  margin-bottom: 2px;
}

.property-value {
  font-size: 1em;
}

.property-value.highlight {
  font-weight: bold;
  color: #1565c0;
  background-color: rgba(21, 101, 192, 0.08);
  padding: 2px 6px;
  border-radius: 3px;
  display: inline-block;
}

.mileage-info, .mileage-details, .description {
  margin-bottom: 15px;
  padding-top: 10px;
  border-top: 1px dashed #eee;
}

.mileage-details {
  display: flex;
  justify-content: space-between;
}

.description .property-value {
  white-space: pre-line;
  font-size: 0.9em;
  line-height: 1.4;
  margin-top: 5px;
}

.risk-type {
  padding: 8px;
  border-radius: 4px;
  margin-top: 10px;
}

.risk-type.green {
  background-color: rgba(76, 175, 80, 0.1);
}

.risk-type.yellow {
  background-color: rgba(255, 193, 7, 0.1);
}

.risk-type.red {
  background-color: rgba(244, 67, 54, 0.1);
}

.editor-link {
  margin: 20px 0;
  text-align: right;
}

.loading {
  text-align: center;
  padding: 30px;
  color: #666;
  font-style: italic;
}

.error {
  background-color: #ffebee;
  border: 1px solid #ffcdd2;
  border-radius: 4px;
  padding: 15px;
  color: #c62828;
  margin: 20px 0;
}

.no-results {
  text-align: center;
  padding: 30px;
  color: #666;
  background-color: #f5f5f5;
  border-radius: 4px;
  border: 1px dashed #ddd;
  margin: 20px 0;
}
</style>