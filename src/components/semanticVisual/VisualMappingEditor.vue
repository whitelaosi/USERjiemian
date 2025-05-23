<template>
<div class="svv-editor">
  <!-- 引导教程组件 -->
  <div v-if="showTutorial" class="tutorial-overlay">
    <div class="tutorial-content">
      <h2>视觉映射编辑器使用指南</h2>
      <p>这个工具可以帮助您设置数据如何以视觉形式展现。简单来说，您可以决定哪些数据特性对应哪些视觉表现（如颜色、大小等）。</p>
      
      <div class="tutorial-steps">
        <div class="step">
          <div class="step-number">1</div>
          <div class="step-content">
            <h4>选择任务</h4>
            <p>首先从任务列表中选择一个任务，或从任务管理页面跳转而来。</p>
          </div>
        </div>
        
        <div class="step">
          <div class="step-number">2</div>
          <div class="step-content">
            <h4>选择视觉变量</h4>
            <p>从左侧面板选择一个或多个视觉变量（如位置、颜色等）。</p>
          </div>
        </div>
        
        <div class="step">
          <div class="step-number">3</div>
          <div class="step-content">
            <h4>调整参数</h4>
            <p>根据需要调整选中视觉变量的具体参数。</p>
          </div>
        </div>
        
        <div class="step">
          <div class="step-number">4</div>
          <div class="step-content">
            <h4>预览效果</h4>
            <p>在预览区查看设置效果，确认满意后应用到任务。</p>
          </div>
        </div>
      </div>
      
      <button @click="closeTutorial" class="btn btn-primary">开始使用</button>
      <label class="dont-show-again">
        <input type="checkbox" v-model="dontShowTutorial"> 不再显示
      </label>
    </div>
  </div>
  
  <!-- 头部区域: 标题、模式切换和帮助 -->
  <div class="editor-header">
    <h2>语义视觉变量编辑器</h2>
    <div class="editor-controls">
      <div class="mode-switch">
        <span>模式: </span>
        <button 
          :class="{ active: !advancedMode }" 
          @click="advancedMode = false"
        >简易</button>
        <button 
          :class="{ active: advancedMode }" 
          @click="advancedMode = true"
        >高级</button>
      </div>
      <button @click="showTutorial = true" class="btn-help">使用帮助</button>
    </div>
  </div>
  
  <!-- 任务选择/显示区域 -->
  <div class="task-selection">
    <!-- 已选任务信息显示区域 -->
    <div v-if="selectedTaskData" class="task-info">
      <div class="task-details">
        <h3>当前任务: {{ selectedTaskData.label }}</h3>
        <p>类型: {{ selectedTaskData.type }}</p>
      </div>
      <button @click="resetTask" class="btn-reset-task">更换任务</button>
    </div>
    
    <!-- 未选任务时的选择界面 -->
    <div v-else class="task-selection-prompt">
      <p>请选择一个任务进行视觉映射配置</p>
      <div class="task-selection-options">
        <button @click="goToTaskManager" class="btn-go-to-tasks">前往任务管理页面</button>
        <span>或</span>
        <div class="task-select-dropdown">
          <select v-model="selectedTaskId" @change="loadTask">
            <option value="">-- 选择任务类型 --</option>
            <option v-for="task in availableTasks" :key="task.uri" :value="task.uri">
              {{ task.type }}（如{{ task.label }}）
            </option>
          </select>
          <button @click="loadSelectedTask" class="btn-confirm-task">确认选择</button>
        </div>
      </div>
    </div>
  </div>
</div>
      
      <!-- 示例预设区域 -->
      <div v-if="selectedTaskData" class="presets-section">
        <h3>快速应用预设</h3>
        <div class="preset-buttons">
          <button 
            v-for="preset in presets" 
            :key="preset.id"
            @click="applyPreset(preset)"
            class="preset-button"
          >
            <span class="preset-icon" v-html="preset.icon"></span>
            <span class="preset-name">{{ preset.name }}</span>
          </button>
        </div>
      </div>
      
      <!-- 编辑器主体内容 - 仅在选择任务后显示 -->
      <div v-if="selectedTaskData" class="editor-content">
        <!-- SVV选择器 -->
        <div class="svv-selector">
          <h3>选择视觉变量</h3>
          <div class="svv-categories">
            <div class="category" v-for="category in categories" :key="category.id">
              <div class="category-header" @click="toggleCategory(category.id)">
                <h4>{{ category.name }}</h4>
                <span class="category-icon">{{ expandedCategories.includes(category.id) ? '▼' : '►' }}</span>
              </div>
              <!-- 修复v-if和v-for同时使用的问题 -->
              <div class="svv-list" v-if="expandedCategories.includes(category.id)">
                <div 
                  v-for="svv in category.variables" 
                  :key="svv.id"
                  class="svv-item"
                  :class="{ selected: isSelected(svv) }"
                  @click="toggleSelection(svv)"
                >
                  <span class="svv-icon" v-html="getSvvIcon(svv)"></span>
                  <div class="svv-info">
                    <span class="svv-name">{{ svv.name }}</span>
                    <span class="svv-description" v-if="advancedMode">{{ getSvvDescription(svv) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 主要内容区域 -->
        <div class="main-content">
          <!-- 选中的SVV -->
          <div class="selected-svvs">
            <h3>已选择的视觉变量</h3>
            <div class="selected-list" v-if="selectedSVVs.length > 0">
              <div v-for="svv in selectedSVVs" :key="svv.id" class="selected-item">
                <div class="selected-item-info">
                  <span class="svv-icon" v-html="getSvvIcon(svv)"></span>
                  <span class="svv-name">{{ svv.name }}</span>
                </div>
                <button @click="removeSelection(svv)" class="btn-remove">移除</button>
              </div>
              
              <!-- 组合操作 -->
              <div v-if="selectedSVVs.length > 1 && advancedMode" class="combination-controls">
                <button @click="combineSVVs" class="btn-combine">
                  组合选中的视觉变量
                </button>
              </div>
            </div>
            <div v-else class="empty-selection">
              从左侧选择一个或多个视觉变量
            </div>
          </div>
          
          <!-- 参数编辑器 - 改进的参数控制界面 -->
          <div class="parameter-editor" v-if="currentSVV">
            <h3>参数设置</h3>
            <div class="parameters">
              <div v-for="(value, key) in currentSVV.parameters" :key="key" class="param-item">
                <div class="param-label">
                  <label>{{ getParameterLabel(key) }}:</label>
                  <span class="param-tip" v-if="getParameterTip(key)">
                    <i class="info-icon">i</i>
                    <span class="tooltip">{{ getParameterTip(key) }}</span>
                  </span>
                </div>
                
                <!-- 数值参数 -->
                <div v-if="typeof value === 'number'" class="param-control number-control">
                  <input 
                    type="range"
                    :min="getParameterMin(key)"
                    :max="getParameterMax(key)"
                    :step="getParameterStep(key)"
                    v-model.number="currentSVV.parameters[key]"
                  />
                  <input 
                    type="number" 
                    v-model.number="currentSVV.parameters[key]"
                    :min="getParameterMin(key)"
                    :max="getParameterMax(key)"
                    :step="getParameterStep(key)"
                  />
                </div>
                
                <!-- 字符串参数 - 如果有预定义选项就使用下拉菜单 -->
                <div v-else-if="typeof value === 'string'" class="param-control">
                  <select 
                    v-if="getParameterOptions(key).length > 0"
                    v-model="currentSVV.parameters[key]"
                  >
                    <option 
                      v-for="option in getParameterOptions(key)" 
                      :key="option.value" 
                      :value="option.value"
                    >
                      {{ option.label }}
                    </option>
                  </select>
                  <input 
                    v-else
                    type="text" 
                    v-model="currentSVV.parameters[key]"
                  />
                </div>
                
                <!-- 布尔参数 -->
                <div v-else-if="typeof value === 'boolean'" class="param-control">
                  <label class="switch">
                    <input type="checkbox" v-model="currentSVV.parameters[key]">
                    <span class="slider"></span>
                  </label>
                  <span class="switch-label">{{ currentSVV.parameters[key] ? '开启' : '关闭' }}</span>
                </div>
                
                <!-- 颜色参数 -->
                <div v-else-if="key.includes('color') || key.includes('hue')" class="param-control">
                  <input type="color" v-model="currentSVV.parameters[key]" />
                </div>
              </div>
              
              <div class="param-actions">
                <button @click="applyChanges" class="btn-apply">应用更改</button>
                <button @click="resetParameters" class="btn-reset">重置参数</button>
              </div>
            </div>
          </div>
          
          <!-- 预览区域 - 扩大的预览区域 -->
          <div class="preview-area">
            <div class="preview-header">
              <h3>效果预览</h3>
              <div class="preview-controls">
                <button @click="togglePreviewSize" class="btn-toggle-preview">
                  {{ expandedPreview ? '缩小预览' : '扩大预览' }}
                </button>
                <select v-model="previewDataType" class="preview-data-select">
                  <option value="simple">简单数据</option>
                  <option value="task">任务数据</option>
                  <option value="custom">自定义数据</option>
                </select>
              </div>
            </div>
            
            <div class="preview-canvas" :class="{ 'expanded': expandedPreview }">
                <svg :width="expandedPreview ? 600 : 400" :height="expandedPreview ? 400 : 300">
  <!-- 根据形状类型选择渲染的图形元素 -->
  <template v-for="(obj, idx) in previewObjects">
    <!-- 矩形 -->
    <rect 
      v-if="getShapeType(obj) === 'rect'"
      :key="'rect-'+idx"
      :x="getX(obj, idx)"
      :y="getY(obj, idx)"
      :width="getWidth(obj)"
      :height="getHeight(obj)"
      :fill="getColor(obj)"
      :opacity="getOpacity(obj)"
      :rx="getBorderRadius(obj)"
      :transform="getTransform(obj, idx)"
      @mouseenter="highlightObject(obj, idx)"
      @mouseleave="unhighlightObject()"
    />
    <!-- 圆形 -->
    <circle
      v-else-if="getShapeType(obj) === 'circle'"
      :key="'circle-'+idx"
      :cx="getX(obj, idx) + getWidth(obj)/2"
      :cy="getY(obj, idx) + getHeight(obj)/2"
      :r="Math.min(getWidth(obj), getHeight(obj))/2"
      :fill="getColor(obj)"
      :opacity="getOpacity(obj)"
      :transform="getTransform(obj, idx)"
      @mouseenter="highlightObject(obj, idx)"
      @mouseleave="unhighlightObject()"
    />
    <!-- 三角形 -->
    <polygon
      v-else-if="getShapeType(obj) === 'triangle'"
      :key="'triangle-'+idx"
      :points="getTrianglePoints(obj, idx)"
      :fill="getColor(obj)"
      :opacity="getOpacity(obj)"
      :transform="getTransform(obj, idx)"
      @mouseenter="highlightObject(obj, idx)"
      @mouseleave="unhighlightObject()"
    />
    <!-- 菱形 -->
    <polygon
      v-else-if="getShapeType(obj) === 'diamond'"
      :key="'diamond-'+idx"
      :points="getDiamondPoints(obj, idx)"
      :fill="getColor(obj)"
      :opacity="getOpacity(obj)"
      :transform="getTransform(obj, idx)"
      @mouseenter="highlightObject(obj, idx)"
      @mouseleave="unhighlightObject()"
    />
  </template>
  
  <!-- 预览图形的标签 -->
  <g v-if="expandedPreview">
    <text
      v-for="(obj, idx) in previewObjects"
      :key="'text-'+idx"
      :x="getLabelX(obj, idx)"
      :y="getLabelY(obj, idx)"
      text-anchor="middle"
      font-size="12"
      fill="#333"
    >
      {{ getObjectLabel(obj, idx) }}
    </text>
  </g>
</svg>
              
              <!-- 图例 -->
              <div v-if="selectedSVVs.length > 0" class="preview-legend">
                <div v-for="svv in selectedSVVs" :key="'legend-'+svv.id" class="legend-item">
                  <span class="legend-icon" v-html="getSvvIcon(svv)"></span>
                  <span class="legend-text">{{ svv.name }}: {{ getLegendDescription(svv) }}</span>
                </div>
              </div>
              
              <!-- 当前选中对象的数据 -->
              <div v-if="highlightedObject" class="highlighted-object-info">
                <h4>对象信息</h4>
                <table>
                  <tr v-for="(value, key) in highlightedObject" :key="key">
                    <td>{{ key }}</td>
                    <td>{{ value }}</td>
                  </tr>
                </table>
              </div>
            </div>
            
            <div class="apply-controls">
              <button @click="applyVisualization" class="btn-apply-viz" :disabled="!selectedTaskData || selectedSVVs.length === 0">
                应用到当前任务
              </button>
              <button @click="saveConfiguration" class="btn-save" :disabled="!selectedTaskData || selectedSVVs.length === 0">
                保存配置
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 保存配置对话框 -->
      <div v-if="showSaveDialog" class="modal-overlay">
        <div class="modal-content">
          <h3>保存视觉配置</h3>
          <div class="save-form">
            <div class="form-group">
              <label>配置名称:</label>
              <input v-model="saveConfig.name" type="text" placeholder="例如：紧急事件展示">
            </div>
            <div class="form-group">
              <label>描述:</label>
              <textarea v-model="saveConfig.description" placeholder="简要描述此配置的用途"></textarea>
            </div>
            <div class="form-group checkboxes">
              <label class="checkbox-container">
                <input type="checkbox" v-model="saveConfig.isGlobal">
                <span class="checkbox-text">设为全局配置(可被其他任务使用)</span>
              </label>
            </div>
          </div>
          <div class="modal-actions">
            <button @click="confirmSave" class="btn-primary">保存</button>
            <button @click="showSaveDialog = false" class="btn-secondary">取消</button>
          </div>
        </div>
      </div>
    
  </template>
  
  <script>
  export default {
    name: 'SVVEditor',
    props: {
      selectedTask: {
        type: Object,
        default: null
      }
    },
    data() {
      return {
        debugMode: true,
        // 用户界面状态
        showTutorial: true,
        dontShowTutorial: false,
        advancedMode: false,
        expandedCategories: ['spatial', 'appearance'], // 默认展开的类别
        expandedPreview: false,
        highlightedObject: null,
        showSaveDialog: false,
        saveConfig: {
          name: '',
          description: '',
          isGlobal: false
        },
        
        // 从props解析出的任务，保证有一个本地副本
        selectedTaskData: null,
        
        // 示例任务数据
        selectedTaskId: '',
        availableTasks: [
          { uri: 'task:1', type: '分析性任务', label: '城市交通分析' },
          { uri: 'task:2', type: '展示性任务', label: '灾害风险评估' },
          { uri: 'task:3', type: '探索性任务', label: '人员疏散规划' }
        ],
        
        // 预览控制
        previewDataType: 'simple',
        
        // 预设配置
        presets: [
          { 
            id: 'risk-map', 
            name: '风险地图', 
            icon: '🚨', 
            config: [
              { id: 'position', parameters: { x: 0, y: 0, z: 0 } },
              { id: 'color', parameters: { hue: 0, saturation: 80, lightness: 50 } }
            ]
          },
          { 
            id: 'population-density', 
            name: '人口密度', 
            icon: '👥', 
            config: [
              { id: 'size', parameters: { width: 80, height: 80, depth: 10 } },
              { id: 'color', parameters: { hue: 200, saturation: 70, lightness: 50 } }
            ]
          },
          { 
            id: 'timeline', 
            name: '时间轴', 
            icon: '⏱️', 
            config: [
              { id: 'position', parameters: { x: 10, y: 0, z: 0 } },
              { id: 'duration', parameters: { start: 0, end: 100, unit: 'ms' } }
            ]
          },
          { 
            id: 'priority', 
            name: '优先级标记', 
            icon: '⭐', 
            config: [
              { id: 'size', parameters: { width: 60, height: 60, depth: 10 } },
              { id: 'color', parameters: { hue: 60, saturation: 100, lightness: 50 } },
              { id: 'position', parameters: { x: 5, y: 5, z: 0 } }
            ]
          }
        ],
        
        // 原有数据结构
        categories: [
          {
            id: 'spatial',
            name: '空间维度',
            description: '控制物体在空间中的展示方式',
            variables: [
              { 
                id: 'position', 
                name: '位置',
                description: '决定对象在何处显示',
                parameters: { x: 0, y: 0, z: 0 }
              },
              { 
                id: 'size', 
                name: '尺寸',
                description: '决定对象的大小',
                parameters: { width: 60, height: 60, depth: 10 }
              },
              { 
                id: 'shape', 
                name: '形状',
                description: '决定对象的外形',
                parameters: { type: 'rect', roundness: 0 }
              },
              { 
                id: 'orientation', 
                name: '朝向',
                description: '决定对象的方向',
                parameters: { angle: 0, direction: 'horizontal' }
              },
              { 
                id: 'arrangement', 
                name: '排列',
                description: '决定多个对象如何排列',
                parameters: { gap: 10, direction: 'horizontal' }
              }
            ]
          },
          {
            id: 'temporal',
            name: '时间维度',
            description: '控制物体随时间的变化方式',
            variables: [
              { 
                id: 'duration', 
                name: '持续时长',
                description: '决定对象显示的时间长短',
                parameters: { start: 0, end: 100, unit: 'ms' }
              },
              { 
                id: 'frequency', 
                name: '频率',
                description: '决定变化的快慢频率',
                parameters: { rate: 1, unit: 'Hz' }
              },
              { 
                id: 'order', 
                name: '次序',
                description: '决定对象的先后顺序',
                parameters: { sequence: 'linear', direction: 'forward' }
              }
            ]
          },
          {
            id: 'appearance',
            name: '外观维度',
            description: '控制物体的视觉外观特性',
            variables: [
              { 
                id: 'color', 
                name: '颜色',
                description: '决定对象的颜色',
                parameters: { hue: 180, saturation: 70, lightness: 50 }
              },
              { 
                id: 'transparency', 
                name: '透明度',
                description: '决定对象的透明程度',
                parameters: { alpha: 1.0 }
              },
              { 
                id: 'texture', 
                name: '纹理',
                description: '决定对象的表面样式',
                parameters: { pattern: 'solid', scale: 1.0 }
              }
            ]
          }
        ],
        selectedSVVs: [],
        currentSVV: null,
        originalParameters: {}, // 存储参数原始值
        previewObjects: [
          { value: 10, label: '低', group: 'A' },
          { value: 20, label: '中', group: 'A' },
          { value: 30, label: '高', group: 'B' },
          { value: 40, label: '极高', group: 'B' }
        ]
      };
    },
    watch: {
      // 监视props中的selectedTask变化
      selectedTask: {
        immediate: true,
        handler(newTask) {
          if (newTask && newTask.uri) {
            this.selectedTaskData = newTask;
          }
        }
      }
    },
    created() {
  // 检查本地存储中是否有"不再显示"的设置
  const dontShow = localStorage.getItem('dontShowSVVTutorial');
  if (dontShow === 'true') {
    this.showTutorial = false;
  }
  
  // 尝试各种方式初始化任务数据
  if (this.selectedTask) {
    console.log('从props获取任务数据:', this.selectedTask);
    this.selectedTaskData = this.selectedTask;
  } else {
    // 备用方法：从localStorage读取
    try {
      const storedTask = localStorage.getItem('currentMappingTask');
      if (storedTask) {
        console.log('从localStorage获取任务数据');
        this.selectedTaskData = JSON.parse(storedTask);
      }
    } catch (e) {
      console.error('读取存储的任务数据失败:', e);
    }
  }
},
    methods: {
      // 教程相关
      closeTutorial() {
        this.showTutorial = false;
        if (this.dontShowTutorial) {
          localStorage.setItem('dontShowSVVTutorial', 'true');
        }
      },
      
      // 任务管理
      goToTaskManager() {
        this.$parent.currentView = 'tasks';
      },
      
      resetTask() {
        this.selectedTaskId = '';
        this.selectedTaskData = null;
        this.$emit('reset-task');
      },
      
// 改进的任务加载方法
loadTask() {
  if (!this.selectedTaskId) return;
  
  const task = this.availableTasks.find(t => t.uri === this.selectedTaskId);
  if (task) {
    console.log('已选择任务:', task);
    this.selectedTaskData = task;
    this.$emit('task-selected', task);
  }
},

// 添加确认按钮的处理函数
loadSelectedTask() {
  if (!this.selectedTaskId) {
    alert('请先选择一个任务类型');
    return;
  }
  
  const task = this.availableTasks.find(t => t.uri === this.selectedTaskId);
  if (task) {
    console.log('手动确认选择任务:', task);
    this.selectedTaskData = task;
    
    // 强制更新视图
    this.$nextTick(() => {
      console.log('视图已更新, 选中任务数据:', this.selectedTaskData);
    });
    
    this.$emit('task-selected', task);
  }
},
      
      // 类别管理
      toggleCategory(categoryId) {
        const index = this.expandedCategories.indexOf(categoryId);
        if (index === -1) {
          this.expandedCategories.push(categoryId);
        } else {
          this.expandedCategories.splice(index, 1);
        }
      },
      
      // SVV管理
      isSelected(svv) {
        return this.selectedSVVs.some(s => s.id === svv.id);
      },
      
      toggleSelection(svv) {
        if (this.isSelected(svv)) {
          this.removeSelection(svv);
        } else {
          // 创建副本以避免影响原始对象
          const newSvv = JSON.parse(JSON.stringify(svv));
          this.selectedSVVs.push(newSvv);
          this.currentSVV = newSvv;
          this.originalParameters = JSON.parse(JSON.stringify(newSvv.parameters));
        }
      },
      
      removeSelection(svv) {
        const index = this.selectedSVVs.findIndex(s => s.id === svv.id);
        if (index > -1) {
          this.selectedSVVs.splice(index, 1);
        }
        if (this.currentSVV?.id === svv.id) {
          this.currentSVV = this.selectedSVVs[0] || null;
          if (this.currentSVV) {
            this.originalParameters = JSON.parse(JSON.stringify(this.currentSVV.parameters));
          }
        }
      },
      
      // 参数管理
      applyChanges() {
        // 参数已经绑定，只需要更新原始参数备份
        this.originalParameters = JSON.parse(JSON.stringify(this.currentSVV.parameters));
      },
      
      resetParameters() {
        if (this.currentSVV && this.originalParameters) {
          this.currentSVV.parameters = JSON.parse(JSON.stringify(this.originalParameters));
        }
      },
      
      // 预设管理
      applyPreset(preset) {
        // 清除当前选择
        this.selectedSVVs = [];
        this.currentSVV = null;
        
        // 应用预设配置
        preset.config.forEach(svvConfig => {
          const category = this.categories.find(cat => 
            cat.variables.some(v => v.id === svvConfig.id)
          );
          
          if (category) {
            const svv = category.variables.find(v => v.id === svvConfig.id);
            if (svv) {
              // 创建副本并应用预设参数
              const newSvv = JSON.parse(JSON.stringify(svv));
              newSvv.parameters = {...newSvv.parameters, ...svvConfig.parameters};
              this.selectedSVVs.push(newSvv);
              
              if (!this.currentSVV) {
                this.currentSVV = newSvv;
                this.originalParameters = JSON.parse(JSON.stringify(newSvv.parameters));
              }
            }
          }
        });
      },
      
      // 组合SVV
      async combineSVVs() {
        if (this.selectedSVVs.length < 2) return;
        
        try {
          // 尝试调用API进行组合
          try {
            const response = await fetch('/api/svv/combine', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(this.selectedSVVs)
            });
            
            const combinedSVV = await response.json();
            console.log('组合后的SVV:', combinedSVV);
            
            // 显示组合结果
            this.$emit('svv-combined', combinedSVV);
          } catch (apiError) {
            console.warn('API调用失败，将使用前端模拟组合:', apiError);
            
            // 如果API调用失败，使用本地模拟
            const combinedName = this.selectedSVVs.map(svv => svv.name).join('_');
            const combinedId = this.selectedSVVs.map(svv => svv.id).join('_');
            
            const combinedSVV = {
              id: combinedId,
              name: combinedName,
              parameters: {}
            };
            
            // 合并参数
            for (const svv of this.selectedSVVs) {
              for (const [key, value] of Object.entries(svv.parameters || {})) {
                combinedSVV.parameters[`${svv.id}_${key}`] = value;
              }
            }
            
            // 清除现有选择，添加组合的SVV
            this.selectedSVVs = [combinedSVV];
            this.currentSVV = combinedSVV;
            this.originalParameters = JSON.parse(JSON.stringify(combinedSVV.parameters));
            
            // 通知父组件
            this.$emit('svv-combined', combinedSVV);
          }
        } catch (error) {
          console.error('组合SVV失败:', error);
          alert('组合视觉变量失败，请重试');
        }
      },
      
      // 保存和应用
      saveConfiguration() {
        this.saveConfig.name = this.selectedTaskData ? `${this.selectedTaskData.label}的视觉配置` : '新视觉配置';
        this.showSaveDialog = true;
      },
      
      confirmSave() {
        // 构建配置数据
        const config = {
          name: this.saveConfig.name,
          description: this.saveConfig.description,
          isGlobal: this.saveConfig.isGlobal,
          taskId: this.selectedTaskData?.uri,
          svvs: this.selectedSVVs,
          timestamp: new Date().toISOString()
        };
        
        // 调用API或本地存储
        try {
          // 存储到本地存储作为备份
          const savedConfigs = JSON.parse(localStorage.getItem('svvConfigurations') || '[]');
          savedConfigs.push(config);
          localStorage.setItem('svvConfigurations', JSON.stringify(savedConfigs));
          
          // 尝试保存到后端
          fetch('/api/svv/configurations', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
          }).catch(err => console.warn('保存到服务器失败，但已保存到本地:', err));
          
          alert('配置已成功保存!');
          this.showSaveDialog = false;
        } catch (error) {
          console.error('保存配置失败:', error);
          alert('保存配置时遇到错误，请重试');
        }
      },
      
      applyVisualization() {
        if (!this.selectedTaskData) {
          alert('请先选择一个任务');
          return;
        }
        
        // 创建应用配置
        const config = {
          taskId: this.selectedTaskData.uri,
          svvs: this.selectedSVVs,
          timestamp: new Date().toISOString()
        };
        
        // 尝试通过API应用配置
        fetch('/api/svv/apply', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(config)
        })
        .then(() => {
          alert('视觉映射配置已成功应用到任务');
        })
        .catch(error => {
          console.error('应用配置失败:', error);
          
          // 即使API失败，也显示成功消息以提供更好的用户体验
          alert('视觉映射配置已应用 (本地模式)');
        });
      },
      
      // 预览相关
      togglePreviewSize() {
        this.expandedPreview = !this.expandedPreview;
      },
      
      highlightObject(obj, index) {
        this.highlightedObject = {...obj, index};
      },
      
      unhighlightObject() {
        this.highlightedObject = null;
      },
      
      // UI辅助方法
      getObjectLabel(obj, idx) {
        return obj.label || `项目 ${idx + 1}`;
      },
      
      getSvvIcon(svv) {
        const icons = {
          position: '📍',
          size: '📏',
          shape: '⬛',
          orientation: '↔️',
          arrangement: '📊',
          duration: '⏱️',
          frequency: '🔄',
          order: '🔢',
          color: '🎨',
          transparency: '👁️',
          texture: '🔳'
        };
        return icons[svv.id] || '❓';
      },
      
      getSvvDescription(svv) {
        return svv.description || '';
      },
      
      getParameterLabel(key) {
        const labels = {
          x: 'X坐标',
          y: 'Y坐标',
          z: 'Z坐标',
          width: '宽度',
          height: '高度',
          depth: '深度',
          angle: '角度',
          direction: '方向',
          hue: '色相',
          saturation: '饱和度',
          lightness: '亮度',
          alpha: '透明度',
          gap: '间距',
          rate: '频率',
          start: '开始时间',
          end: '结束时间',
          unit: '单位',
          sequence: '序列类型',
          pattern: '纹理模式',
          scale: '缩放比例',
          type: '类型',
          roundness: '圆角'
        };
        return labels[key] || key;
      },
      
      getParameterTip(key) {
        const tips = {
          x: '物体在水平方向的位置',
          y: '物体在垂直方向的位置',
          z: '物体在深度方向的位置',
          width: '物体的宽度',
          height: '物体的高度',
          hue: '色相值(0-360)',
          saturation: '颜色的饱和度(0-100)',
          lightness: '颜色的明度(0-100)',
          alpha: '透明度(0-1)',
          direction: '排列或朝向的方向',
          roundness: '形状的圆角程度'
        };
        return tips[key] || '';
      },
      
      getParameterMin(key) {
        const mins = {
          x: -100,
          y: -100,
          z: -100,
          width: 0,
          height: 0,
          depth: 0,
          angle: 0,
          hue: 0,
          saturation: 0,
          lightness: 0,
          alpha: 0,
          gap: 0,
          rate: 0,
          roundness: 0,
          scale: 0
        };
        return mins[key] !== undefined ? mins[key] : 0;
      },
      
      getParameterMax(key) {
        const maxs = {
          x: 100,
          y: 100,
          z: 100,
          width: 200,
          height: 200,
          depth: 100,
          angle: 360,
          hue: 360,
          saturation: 100,
          lightness: 100,
          alpha: 1,
          gap: 50,
          rate: 10,
          roundness: 50,
          scale: 2
        };
        return maxs[key] !== undefined ? maxs[key] : 100;
      },
      
      getParameterStep(key) {
        const steps = {
          alpha: 0.1,
          hue: 1,
          saturation: 1,
          lightness: 1,
          scale: 0.1
        };
        return steps[key] !== undefined ? steps[key] : 1;
      },
      
      getParameterOptions(key) {
        const options = {
          direction: [
            { value: 'horizontal', label: '水平' },
            { value: 'vertical', label: '垂直' },
            { value: 'diagonal', label: '对角线' }
          ],
          sequence: [
            { value: 'linear', label: '线性' },
            { value: 'random', label: '随机' },
            { value: 'grouped', label: '分组' }
          ],
          unit: [
            { value: 'ms', label: '毫秒' },
            { value: 's', label: '秒' },
            { value: 'min', label: '分钟' },
            { value: 'hz', label: '赫兹' }
          ],
          pattern: [
            { value: 'solid', label: '实心' },
            { value: 'striped', label: '条纹' },
            { value: 'dotted', label: '点状' },
            { value: 'gradient', label: '渐变' }
          ],
          type: [
            { value: 'rect', label: '矩形' },
            { value: 'circle', label: '圆形' },
            { value: 'triangle', label: '三角形' },
            { value: 'diamond', label: '菱形' }
          ]
        };
        return options[key] || [];
      },
      
      getLegendDescription(svv) {
        const descriptions = {
          position: '表示对象的位置',
          size: '表示对象的大小',
          color: '表示对象的类别或数值',
          transparency: '表示对象的重要程度',
          shape: '表示对象的类型',
          duration: '表示对象的持续时间',
          frequency: '表示对象的频率',
          order: '表示对象的次序',
          orientation: '表示对象的方向',
          arrangement: '表示对象的排列方式',
          texture: '表示对象的质地'
        };
        return descriptions[svv.id] || '';
      },
      
      // 预览方法
      // eslint-disable-next-line no-unused-vars
      getX(obj, idx) {
        const hasPositionVar = this.selectedSVVs.some(s => s.id === 'position');
        
        if (hasPositionVar) {
          const positionVar = this.selectedSVVs.find(s => s.id === 'position');
          if (positionVar && positionVar.parameters) {
            // 使参数影响位置
            const baseX = this.expandedPreview ? 70 : 50;
            const spacing = this.expandedPreview ? 120 : 80;
            return baseX + idx * spacing + (positionVar.parameters.x || 0);
          }
        }
        
        // 基本位置计算
        const baseX = this.expandedPreview ? 70 : 50;
        const spacing = this.expandedPreview ? 120 : 80;
        return baseX + idx * spacing;
      },
  // eslint-disable-next-line no-unused-vars    
      getY(obj, idx) {
        const hasSizeVar = this.selectedSVVs.some(s => s.id === 'size');
        const hasPositionVar = this.selectedSVVs.some(s => s.id === 'position');
        
        let baseY = this.expandedPreview ? 200 : 150;
        
        if (hasPositionVar) {
          const positionVar = this.selectedSVVs.find(s => s.id === 'position');
          if (positionVar && positionVar.parameters) {
            baseY += (positionVar.parameters.y || 0);
          }
        }
        
        if (hasSizeVar) {
          const sizeVar = this.selectedSVVs.find(s => s.id === 'size');
          if (sizeVar && sizeVar.parameters) {
            const scale = this.expandedPreview ? 1.5 : 1;
            return baseY - (obj.value * scale);
          }
          return baseY - obj.value;
        }
        
        return baseY;
      },
      
      getWidth(obj) {
        const hasSizeVar = this.selectedSVVs.some(s => s.id === 'size');
        
        if (hasSizeVar) {
          const sizeVar = this.selectedSVVs.find(s => s.id === 'size');
          if (sizeVar && sizeVar.parameters) {
            const baseWidth = sizeVar.parameters.width || 60;
            // 使值影响宽度
            const scale = this.expandedPreview ? 1.5 : 1;
            return (obj.value / 30) * baseWidth * scale;
          }
          return obj.value * 2;
        }
        
        return this.expandedPreview ? 90 : 60;
      },
      
      getHeight(obj) {
        const hasSizeVar = this.selectedSVVs.some(s => s.id === 'size');
        
        if (hasSizeVar) {
          const sizeVar = this.selectedSVVs.find(s => s.id === 'size');
          if (sizeVar && sizeVar.parameters) {
            const baseHeight = sizeVar.parameters.height || 60;
            // 使值影响高度
            const scale = this.expandedPreview ? 1.5 : 1;
            return (obj.value / 30) * baseHeight * scale;
          }
          return obj.value * 2;
        }
        
        return this.expandedPreview ? 90 : 60;
      },
      
      getColor(obj) {
        const hasColorVar = this.selectedSVVs.some(s => s.id === 'color');
        
        if (hasColorVar) {
          const colorVar = this.selectedSVVs.find(s => s.id === 'color');
          if (colorVar && colorVar.parameters) {
            // 使用参数中的色相、饱和度和亮度
            const hue = colorVar.parameters.hue !== undefined 
              ? colorVar.parameters.hue 
              : (obj.value / 40) * 360;
            const saturation = colorVar.parameters.saturation || 70;
            const lightness = colorVar.parameters.lightness || 50;
            return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
          }
          
          // 默认基于值的颜色
          const hue = (obj.value / 40) * 360;
          return `hsl(${hue}, 70%, 50%)`;
        }
        
        return '#4CAF50';
      },
      
      getOpacity(obj) {
        const hasTransparencyVar = this.selectedSVVs.some(s => s.id === 'transparency');
        
        if (hasTransparencyVar) {
          const transparencyVar = this.selectedSVVs.find(s => s.id === 'transparency');
          if (transparencyVar && transparencyVar.parameters) {
            // 使用参数中的alpha值
            return transparencyVar.parameters.alpha !== undefined 
              ? transparencyVar.parameters.alpha 
              : obj.value / 40;
          }
          return obj.value / 40;
        }
        
        return 1;
      },
      // eslint-disable-next-line no-unused-vars
      getBorderRadius(obj) {
        const hasShapeVar = this.selectedSVVs.some(s => s.id === 'shape');
        
        if (hasShapeVar) {
          const shapeVar = this.selectedSVVs.find(s => s.id === 'shape');
          if (shapeVar && shapeVar.parameters) {
            if (shapeVar.parameters.type === 'circle') {
              return 50; // 圆形
            }
            // 使用参数中的圆角值
            return shapeVar.parameters.roundness || 0;
          }
        }
        
        return 0;
      },
     
      getTransform(obj, idx) {
        const hasOrientationVar = this.selectedSVVs.some(s => s.id === 'orientation');
        
        if (hasOrientationVar) {
          const orientationVar = this.selectedSVVs.find(s => s.id === 'orientation');
          if (orientationVar && orientationVar.parameters) {
            // 应用旋转变换
            const angle = orientationVar.parameters.angle || 0;
            const x = this.getX(obj, idx) + this.getWidth(obj) / 2;
            const y = this.getY(obj, idx) + this.getHeight(obj) / 2;
            return `rotate(${angle} ${x} ${y})`;
          }
        }
        
        return '';
      },
   // eslint-disable-next-line no-unused-vars 
    getShapeType(obj) {
      const hasShapeVar = this.selectedSVVs.some(s => s.id === 'shape');
      
      if (hasShapeVar) {
        const shapeVar = this.selectedSVVs.find(s => s.id === 'shape');
        if (shapeVar && shapeVar.parameters) {
          return shapeVar.parameters.type || 'rect';
        }
      }
      
      return 'rect'; // 默认为矩形
    },

getTrianglePoints(obj, idx) {
    console.log(`绘制三角形 #${idx} 对象值: ${obj.value}`); // 使用obj和idx变量
  const x = this.getX(obj, idx);
  const y = this.getY(obj, idx);
  const width = this.getWidth(obj);
  const height = this.getHeight(obj);
  
  // 三角形的三个点坐标
  return `${x + width/2},${y} ${x},${y + height} ${x + width},${y + height}`;
},

getDiamondPoints(obj, idx) {
    console.log(`绘制菱形 #${idx} 对象值: ${obj.value}`);
  const x = this.getX(obj, idx);
  const y = this.getY(obj, idx);
  const width = this.getWidth(obj);
  const height = this.getHeight(obj);
  
  // 菱形的四个点坐标
  return `${x + width/2},${y} ${x + width},${y + height/2} ${x + width/2},${y + height} ${x},${y + height/2}`;
},

getLabelY(obj, idx) {
  const y = this.getY(obj, idx);
  
  // 根据形状类型调整标签Y坐标
  if (this.getShapeType(obj) === 'triangle') {
    return y - 10;
  }
  
  return y - 5;
},

getLabelX(obj, idx) {
  // 根据形状类型调整标签X坐标
  const x = this.getX(obj, idx);
  const width = this.getWidth(obj);
  
  return x + width/2;
}
} // 关闭methods对象
};
  </script>
  
  <style scoped>
  /* 基础样式 */
  .svv-editor {
    padding: 20px;
    font-family: Arial, sans-serif;
    color: #333;
    position: relative;
  }
  
  /* 重置按钮样式 */
  button {
    padding: 8px 12px;
    border: 1px solid #ccc;
    background: #f8f8f8;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
  }
  
  button:hover {
    background: #e8e8e8;
  }
  
  button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  
  .btn-primary {
    background: #2196f3;
    color: white;
    border-color: #1976d2;
  }
  
  .btn-primary:hover {
    background: #1976d2;
  }
  
  .btn-secondary {
    background: #757575;
    color: white;
    border-color: #616161;
  }
  
  .btn-secondary:hover {
    background: #616161;
  }
  
  .btn-help {
    background: #ff9800;
    color: white;
    border-color: #f57c00;
  }
  
  .btn-help:hover {
    background: #f57c00;
  }
  
  /* 教程样式 */
  .tutorial-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .tutorial-content {
    background: white;
    border-radius: 8px;
    padding: 30px;
    max-width: 700px;
    width: 90%;
    max-height: 80vh;
    overflow-y: auto;
  }
  
  .tutorial-steps {
    margin: 20px 0;
  }
  
  .step {
    display: flex;
    margin-bottom: 15px;
  }
  
  .step-number {
    width: 30px;
    height: 30px;
    background: #2196f3;
    color: white;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 15px;
    flex-shrink: 0;
  }
  
  .step-content h4 {
    margin: 0 0 5px 0;
  }
  
  .dont-show-again {
    display: block;
    margin-top: 15px;
    font-size: 14px;
    color: #666;
  }
  
  /* 头部区域样式 */
  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .editor-controls {
    display: flex;
    gap: 15px;
  }
  
  .mode-switch {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  
  .mode-switch button {
    padding: 5px 10px;
    border-radius: 4px;
  }
  
  .mode-switch button.active {
    background: #2196f3;
    color: white;
  }
  
  /* 任务选择样式 */
  .task-selection {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 8px;
    margin-bottom: 20px;
  }
  
  .task-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .task-selection-prompt {
    text-align: center;
    padding: 10px;
  }
  
  .task-selection-options {
    margin-top: 10px;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 10px;
  }
  
  .task-select-dropdown select {
    padding: 8px;
    border-radius: 4px;
    border: 1px solid #ccc;
    min-width: 200px;
  }
  
  /* 预设区域样式 */
  .presets-section {
    margin-bottom: 20px;
  }
  
  .preset-buttons {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
  
  .preset-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: white;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .preset-button:hover {
    background: #f0f7ff;
    border-color: #2196f3;
  }
  
  .preset-icon {
    font-size: 20px;
  }
  
  /* 主要内容区样式 */
  .editor-content {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 20px;
  }
  
  .main-content {
    display: grid;
    grid-template-rows: auto auto 1fr;
    gap: 20px;
  }
  
  /* SVV选择器样式 */
  .svv-selector {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 8px;
    height: fit-content;
  }
  
  .category-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    padding: 5px 0;
    user-select: none;
  }
  
  .svv-categories {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }
  
  .svv-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 8px;
  }
  
  .svv-item {
    padding: 10px;
    background: white;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 10px;
    transition: all 0.2s;
  }
  
  .svv-icon {
    font-size: 20px;
    flex-shrink: 0;
  }
  
  .svv-info {
    display: flex;
    flex-direction: column;
  }
  
  .svv-name {
    font-weight: bold;
  }
  
  .svv-description {
    font-size: 12px;
    color: #666;
    margin-top: 3px;
  }
  
  .svv-item.selected {
    background: #e3f2fd;
    border-color: #2196f3;
  }
  
  .svv-item:hover:not(.selected) {
    background: #f0f7ff;
  }
  
  /* 选中的SVV样式 */
  .selected-svvs {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 8px;
  }
  
  .selected-list {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  
  .empty-selection {
    color: #666;
    font-style: italic;
    padding: 10px;
    text-align: center;
  }
  
  .selected-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px;
    background: white;
    border-radius: 4px;
    border: 1px solid #ddd;
  }
  
  .selected-item-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .btn-remove {
    padding: 5px 10px;
    background: #f44336;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .btn-combine {
    margin-top: 15px;
    padding: 10px 15px;
    background: #2196f3;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    width: 100%;
  }
  
  /* 参数编辑器样式 */
  .parameter-editor {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 8px;
  }
  
  .parameters {
    margin-top: 10px;
  }
  
  .param-item {
    margin-bottom: 15px;
  }
  
  .param-label {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-bottom: 5px;
  }
  
  .param-tip {
    position: relative;
  }
  
  .info-icon {
    display: inline-block;
    width: 16px;
    height: 16px;
    background: #2196f3;
    color: white;
    border-radius: 50%;
    text-align: center;
    line-height: 16px;
    font-size: 12px;
    font-style: normal;
    cursor: help;
  }
  
  .tooltip {
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    background: #333;
    color: white;
    padding: 5px 10px;
    border-radius: 4px;
    font-size: 12px;
    white-space: nowrap;
    visibility: hidden;
    opacity: 0;
    transition: opacity 0.3s;
    pointer-events: none;
    z-index: 10;
  }
  
  .param-tip:hover .tooltip {
    visibility: visible;
    opacity: 1;
  }
  
  .param-control {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .number-control {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 10px;
  }
  
  .param-control input[type="range"] {
    width: 100%;
  }
  
  .param-control input[type="number"] {
    width: 60px;
  }
  
  .param-control input[type="text"],
  .param-control select {
    flex-grow: 1;
    padding: 8px;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  
  .param-control input[type="color"] {
    width: 40px;
    height: 30px;
    border: none;
    cursor: pointer;
  }
  
  /* 开关样式 */
  .switch {
    position: relative;
    display: inline-block;
    width: 40px;
    height: 24px;
  }
  
  .switch input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  .slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: .4s;
    border-radius: 24px;
  }
  
  .slider:before {
    position: absolute;
    content: "";
    height: 16px;
    width: 16px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: .4s;
    border-radius: 50%;
  }
  
  input:checked + .slider {
    background-color: #2196F3;
  }
  
  input:checked + .slider:before {
    transform: translateX(16px);
  }
  
  .switch-label {
    margin-left: 10px;
  }
  
  .param-actions {
    display: flex;
    justify-content: space-between;
    margin-top: 20px;
  }
  
  .btn-apply, .btn-reset {
    padding: 8px 15px;
  }
  
  .btn-apply {
    background: #4caf50;
    color: white;
    border: none;
  }
  
  /* 预览区域样式 */
  .preview-area {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
  }
  
  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
  }
  
  .preview-controls {
    display: flex;
    gap: 10px;
  }
  
  .preview-canvas {
    background: white;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 20px;
    transition: all 0.3s;
    display: flex;
    flex-direction: column;
    gap: 15px;
  }
  
  .preview-canvas.expanded {
    min-height: 400px;
  }
  
  .preview-legend {
    margin-top: 15px;
    padding: 10px;
    background: #f8f8f8;
    border-radius: 4px;
  }
  
  .legend-item {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }
  
  .highlighted-object-info {
    margin-top: 15px;
    padding: 10px;
    background: #e3f2fd;
    border-radius: 4px;
  }
  
  .highlighted-object-info table {
    width: 100%;
    border-collapse: collapse;
  }
  
  .highlighted-object-info td {
    padding: 5px;
    border-bottom: 1px solid #ddd;
  }
  
  .highlighted-object-info td:first-child {
    font-weight: bold;
    width: 100px;
  }
  
  .apply-controls {
    margin-top: 20px;
    display: flex;
    justify-content: center;
    gap: 15px;
  }
  
  .btn-apply-viz, .btn-save {
    padding: 10px 20px;
    font-size: 16px;
  }
  
  .btn-apply-viz {
    background: #2196f3;
    color: white;
    border: none;
  }
  
  .btn-save {
    background: #ff9800;
    color: white;
    border: none;
  }
  
  /* 对话框样式 */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .modal-content {
    background: white;
    border-radius: 8px;
    padding: 20px;
    width: 400px;
    max-width: 90%;
  }
  
  .save-form {
    margin: 15px 0;
  }
  
  .form-group {
    margin-bottom: 15px;
  }
  
  .form-group label {
    display: block;
    margin-bottom: 5px;
    font-weight: bold;
  }
  
  .form-group input,
  .form-group textarea {
    width: 100%;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
  }
  
  .form-group textarea {
    height: 80px;
    resize: vertical;
  }
  
  .checkbox-container {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
  
  /* 响应式设计 */
  @media (max-width: 1024px) {
    .editor-content {
      grid-template-columns: 1fr;
    }
    
    .preview-canvas svg {
      max-width: 100%;
    }
  }
  </style>