<template>
    <div class="task-chain-container">
      <h2 class="title">任务链执行器</h2>
      <!-- 添加任务选择器 -->
      <div class="task-selector" v-if="!selectedWorkflow">
  <h3>选择要执行的任务</h3>
  <div class="task-list">
    <div 
      v-for="task in availableTasks" 
      :key="task.uri"
      class="task-item"
      :class="{ active: selectedTask?.uri === task.uri }"
      @click="selectTask(task)"
    >
      <div class="task-name">{{ task.label }}</div>
      <div class="task-type">{{ task.type }}</div>
      <div class="task-description">{{ task.description }}</div>
    </div>
  </div>
  <button 
  v-if="selectedTask" 
  @click="startWorkflowForTask"
  class="btn-primary"
>
  为选中任务启动工作流
</button>
<button 
      v-if="selectedTask" 
      @click="selectedTask = null" 
      class="btn-secondary"
    >
      取消选择
    </button>
</div>
      <!-- 工作流选择 -->
      <div class="workflow-selector" v-if="selectedTask && workflows.length > 0">
        <h3>选择工作流</h3>
        <div class="workflow-list">
          <div 
            v-for="workflow in workflows" 
            :key="workflow.id" 
            class="workflow-item"
            :class="{ active: selectedWorkflow === workflow.id }"
            @click="selectWorkflow(workflow.id)"
          >
            <div class="workflow-name">{{ workflow.name }}</div>
            <div class="workflow-description">{{ workflow.description }}</div>
          </div>
        </div>
      </div>
      
      <!-- 工作流详情 -->
      <div v-if="workflowDetails" class="workflow-details">
        <h3>工作流: {{ workflowDetails.name }}</h3>
        
        <!-- 添加工作流来源信息 -->
        <div class="workflow-source">
          <span class="source-label">来源任务:</span>
          <span class="source-value">{{ workflowSource.taskName || '未指定' }}</span>
          <span class="source-id">(#{{ workflowSource.taskId || 'N/A' }})</span>
        </div>
        
        <p>{{ workflowDetails.description }}</p>
        
        <div class="controls">
            <button @click="backToTaskSelection" class="btn-primary">
      ← 选择其他任务
    </button>
          <button @click="resetWorkflow" class="btn-secondary">重置</button>
        </div>
        
        <!-- 执行路径 -->
        <div v-if="executionPath.length > 0" class="execution-path">
          <h4>执行路径:</h4>
          <div class="path-steps">
            <div v-for="(step, index) in executionPath" :key="index" class="path-step">
              <span class="node-name">{{ getNodeName(step.nodeId) }}</span>
              <span class="arrow">→</span>
              <span class="outcome-name">{{ step.outcomeName }}</span>
              <span v-if="index < executionPath.length - 1" class="connector">→</span>
            </div>
          </div>
        </div>
        
        <!-- 当前任务执行部分 -->
        <div class="task-execution" v-if="currentNodeId">
          <!-- 添加决策偏差提示 -->
          <div v-if="currentTaskHasDeviation" class="deviation-alert">
            <h4>上一步决策与数据分析结果存在差异</h4>
            <div class="deviation-details">
              <p><strong>任务:</strong> {{ deviationInfo.taskName }}</p>
              <p><strong>系统建议:</strong> {{ deviationInfo.recommendedChoice }}</p>
              <p><strong>实际选择:</strong> {{ deviationInfo.actualChoice }}</p>
              <p><strong>选择理由:</strong> {{ deviationInfo.reason }}</p>
            </div>
            <div class="deviation-warning">
              <i class="warning-icon"></i>
              <p>注意：当前分析是基于与数据不完全吻合的选择进行的，请谨慎评估结果。</p>
            </div>
          </div>
  
          <h3>当前任务: {{ getCurrentNodeName() }}</h3>
          <p>{{ getCurrentNodeDescription() }}</p>
          
          <!-- 显示任务输入参数 -->
          <div v-if="currentTask && Object.keys(currentTask.inputParameters).length > 0" class="task-parameters">
            <h4>任务参数:</h4>
            <div v-for="(value, key) in currentTask.inputParameters" :key="key" class="parameter">
              <span class="param-name">{{ key }}:</span>
              <span class="param-value">{{ formatParameterValue(value) }}</span>
            </div>
          </div>
          
          <!-- 显示分析数据 - 改进数据可视化 -->
          <div v-if="analysisData" class="analysis-data">
            <h4>分析数据:</h4>
            
            <!-- 条件判断：如果是多属性比较任务 -->
            <div v-if="currentTask && currentTask.type === 'MultiAttributeComparison'" class="comparison-visualization">
              <div class="comparison-summary">
                <!-- 关键指标突出显示 -->
                <div v-if="getChangeRate('energyRelease')" class="key-metric">
                  <div class="metric-label">能量变化率:</div>
                  <div class="metric-value" :class="getChangeRateClass('energyRelease')">
                    {{ formatChangeRate(getChangeRate('energyRelease')) }}
                  </div>
                </div>
                
                <!-- 添加更多关键指标... -->
              </div>
              
              <!-- 对比数据可视化 -->
              <div class="comparison-chart">

                <div v-if="!hasComparisonData()" class="no-data">无可视化数据</div>
              </div>
              
              <!-- 仍然提供查看原始数据的选项 -->
              <div class="raw-data-toggle">
                <button @click="toggleRawData" class="btn-toggle">
                  {{ showRawData ? '隐藏' : '查看' }}原始数据
                </button>
                <pre v-if="showRawData">{{ JSON.stringify(analysisData, null, 2) }}</pre>
              </div>
            </div>
            
            <!-- 其他类型任务的可视化... -->
            <div v-else>
              <!-- 检查是否存在矛盾警告 -->
              <div v-if="analysisData.detailedAnalysis && analysisData.detailedAnalysis.contradictionWarning" 
                   class="contradiction-alert">
                <i class="warning-icon"></i>
                <div class="contradiction-message">
                  {{ analysisData.detailedAnalysis.contradictionWarning }}
                </div>
              </div>
              
              <div v-if="analysisData.detailedAnalysis && analysisData.detailedAnalysis.userJustification" 
                   class="user-justification">
                <strong>您的选择理由:</strong> {{ analysisData.detailedAnalysis.userJustification }}
              </div>
  
              <pre>{{ JSON.stringify(analysisData, null, 2) }}</pre>
            </div>
          </div>
          
          <!-- 用户选择部分 - 改进选项展示 -->
          <div class="outcome-selection">
            <h4>请选择下一步操作:</h4>
            
            <!-- 当有推荐选项时显示提示 -->
            <div v-if="recommendedOutcome" class="recommendation-banner">
              <div class="recommendation-icon">
                <i class="icon-recommendation"></i>
              </div>
              <div class="recommendation-text">
                系统推荐: <strong>{{ recommendedOutcome.name }}</strong>
                <div class="recommendation-reason">
                  根据数据分析，{{ getRecommendationReason() }}
                </div>
              </div>
            </div>
            
            <div v-for="outcome in possibleOutcomes" :key="outcome.id" class="outcome-option">
              <button 
                @click="confirmOutcomeSelection(outcome)" 
                class="outcome-button"
                :class="{
                  'recommended': isRecommendedOutcome(outcome),
                  'contradicts-data': contradictsData(outcome)
                }"
              >
                {{ outcome.name }}
                
                <!-- 添加数据支持度指示器 -->
                <span v-if="isRecommendedOutcome(outcome)" class="data-support high">
                  (数据强支持)
                </span>
                <span v-else-if="contradictsData(outcome)" class="data-support low">
                  (与数据不符)
                </span>
                <span v-else class="data-support medium">
                  (数据部分支持)
                </span>
                
                <span v-if="outcome.description" class="outcome-description">
                  ({{ outcome.description }})
                </span>
              </button>
            </div>
          </div>
          
          <!-- 改进的选择确认对话框 -->
          <div v-if="showConfirmDialog" class="confirmation-dialog">
            <div class="dialog-content">
              <h3>确认与数据矛盾的选择</h3>
              <div class="contradiction-warning">
                <i class="warning-icon"></i>
                <div class="warning-message">
                  <strong>警告：您的选择与数据分析结果矛盾</strong>
                  <p>实际数据: {{ getDataContradiction() }}</p>
                  <p>您的选择: "{{ pendingOutcome.name }}"</p>
                </div>
              </div>
              
              <div class="reason-input">
                <label for="contradiction-reason">
                  请说明为什么您认为此选择合适（必填）：
                </label>
                <textarea 
                  id="contradiction-reason" 
                  v-model="contradictionReason" 
                  rows="3" 
                  placeholder="例如：基于专业判断，我认为..."
                  :class="{ 'error': showReasonError }"
                ></textarea>
                <span v-if="showReasonError" class="reason-error">请提供选择理由</span>
              </div>
              
              <div class="dialog-actions">
                <button @click="confirmContradictingSelection" class="btn-confirm">确认选择</button>
                <button @click="cancelSelection" class="btn-cancel">返回修改</button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 无任务时的开始按钮 -->
        <div v-else class="start-workflow">
          <button @click="startWorkflow" class="btn-primary">开始工作流</button>
        </div>
        
        <!-- 任务节点列表 -->
        <div class="task-nodes">
          <div 
            v-for="node in getOrderedNodes()" 
            :key="node.id" 
            class="task-node"
            :class="{
              'current': currentNodeId === node.id,
              'executed': isNodeExecuted(node.id),
              'expanded': expandedNodes[node.id]
            }"
          >
            <div class="node-header" @click="toggleNodeExpansion(node.id)">
              <div class="node-title">
                <span v-if="isNodeExecuted(node.id)" class="node-status executed">✓</span>
                <span v-else-if="currentNodeId === node.id" class="node-status current">●</span>
                <span v-else class="node-status">○</span>
                {{ node.name }}
              </div>
              <div class="node-type">{{ node.type }}</div>
            </div>
            
            <div v-if="expandedNodes[node.id]" class="node-details">
              <div class="node-description">{{ node.description }}</div>
              
              <!-- 任务参数 -->
              <div v-if="Object.keys(node.inputParameters).length > 0" class="node-parameters">
                <h5>参数:</h5>
                <div v-for="(value, key) in node.inputParameters" :key="key" class="parameter">
                  <span class="param-name">{{ key }}:</span>
                  <span class="param-value">{{ formatParameterValue(value) }}</span>
                </div>
              </div>
              
              <!-- 可能的结果 -->
              <div class="node-outcomes">
                <h5>可能的结果:</h5>
                <div 
                  v-for="outcome in node.possibleOutcomes" 
                  :key="outcome.id" 
                  class="outcome"
                  :class="{ selected: isOutcomeSelected(node.id, outcome.id) }"
                >
                  <span class="outcome-name">{{ outcome.name }}</span>
                  <span v-if="outcome.description" class="outcome-description">
                    ({{ outcome.description }})
                  </span>
                  <span v-if="outcome.nextTaskId" class="outcome-next">
                    → {{ getNodeName(outcome.nextTaskId) }}
                  </span>
                </div>
              </div>
              
              <!-- 结果数据 -->
              <div v-if="nodeResults[node.id]" class="node-results">
                <h5>执行结果:</h5>
                <pre class="results-data">{{ JSON.stringify(nodeResults[node.id], null, 2) }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'TaskChainVisualizer',
    data() {
      return {
        workflows: [],
        // 新增任务相关状态
availableTasks: [],
selectedTask: null,
        selectedWorkflow: null,
        workflowDetails: null,
        currentNodeId: null,
        executionPath: [],
        nodeResults: {},
        expandedNodes: {},
        isExecuting: false,
        
        // 新增状态，用于存储当前任务和可选项
        currentTask: null,
        possibleOutcomes: [],
        analysisData: null,
        
        // 工作流来源信息
        workflowSource: {
          taskId: null,
          taskName: null
        },
        
        // 数据可视化相关
        showRawData: false,
        chartOptions: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              beginAtZero: true
            }
          }
        },
        
        // 推荐系统相关
        recommendedOutcome: null,
        
        // 确认对话框相关
        showConfirmDialog: false,
        confirmationDialogType: 'normal', // 'normal' 或 'contradiction'
        pendingOutcome: null,
        contradictionReason: '',
        showReasonError: false,
        currentTaskHasDeviation: false,
        deviationInfo: null
      };
    },
    computed: {
      canExecuteNext() {
        return this.currentNodeId !== null;
      }
    },
    created() {
        this.loadAvailableTasks(); // 加载可用任务
  // 不再直接加载工作流
  // this.loadWorkflows();
    },
    methods: {
        backToTaskSelection() {
  // 清除当前选择的工作流和任务
  this.selectedWorkflow = null;
  this.selectedTask = null;
  this.workflowDetails = null;
  this.workflows = [];
  
  // 重置工作流状态
  this.resetState();
  
  // 重新加载任务列表（如果需要）
  this.loadAvailableTasks();
},
        async loadAvailableTasks() {
  try {
    const response = await fetch('/api/tasks');
    const tasks = await response.json();
    this.availableTasks = tasks;
  } catch (error) {
    console.error('加载任务列表失败:', error);
  }
},

selectTask(task) {
  this.selectedTask = task;
},

async startWorkflowForTask() {
  if (!this.selectedTask) return;
  
  try {
    // 1. 加载可用的工作流
    await this.loadWorkflows();
    
    // 2. 如果有工作流，设置第一个工作流的源任务
    if (this.workflows.length > 0) {
      const workflowId = this.workflows[0].id;
      
      // 获取任务ID (从URI中提取)
      const taskId = this.selectedTask.uri.split('/').pop();
      
      // 调用后端API设置工作流的源任务
      await fetch(`/api/tasks/workflows/${workflowId}/set-source`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sourceTaskId: taskId })
      });
      
      // 3. 自动选择工作流
      this.selectWorkflow(workflowId);
    }
  } catch (error) {
    console.error('启动工作流失败:', error);
  }
},
      async loadWorkflows() {
        try {
          const response = await fetch('/api/tasks/workflows');
          const data = await response.json();
          this.workflows = data.workflows;
          
          if (this.workflows.length > 0) {
            this.selectWorkflow(this.workflows[0].id);
          }
        } catch (error) {
          console.error('加载工作流失败:', error);
          
          // 使用模拟数据
          this.workflows = [
            {
              id: 'workflow1',
              name: '能量阈值分析工作流',
              description: '分析不同时间段的能量阈值变化并提供相应的分析和建议'
            },
            {
              id: 'workflow2',
              name: '多震点分析工作流',
              description: '分析多个震点的数据并生成综合报告'
            }
          ];
          
          if (this.workflows.length > 0) {
            this.selectWorkflow(this.workflows[0].id);
          }
        }
      },
      
      async selectWorkflow(workflowId) {
        this.selectedWorkflow = workflowId;
        this.resetState();
        
        try {
          const response = await fetch(`/api/tasks/workflows/${workflowId}`);
          this.workflowDetails = await response.json();
          
          // 获取工作流来源任务信息
          if (this.workflowDetails.sourceTaskId) {
            try {
              const sourceTask = await fetch(`/api/tasks/get-by-id/${this.workflowDetails.sourceTaskId}`);
              if (sourceTask.ok) {
                const sourceTaskData = await sourceTask.json();
                this.workflowSource = {
                  taskId: this.workflowDetails.sourceTaskId,
                  taskName: sourceTaskData.label || '未命名任务'
                };
                console.log("工作流来源任务信息:", this.workflowSource);
              } else {
                console.error("获取源任务失败:", await sourceTask.text());
                this.workflowSource = { taskId: this.workflowDetails.sourceTaskId, taskName: "无法加载任务信息" };
              }
            } catch (error) {
              console.error("获取源任务信息时出错:", error);
              this.workflowSource = { taskId: this.workflowDetails.sourceTaskId, taskName: "加载失败" };
            }
          } else {
            // 没有源任务时的处理
            this.workflowSource = { taskId: null, taskName: "未指定源任务" };
          }
          
          // 初始化展开节点状态 - 处理tasks可能是对象而非数组的情况
          if (Array.isArray(this.workflowDetails.tasks)) {
            // 如果是数组
            this.workflowDetails.tasks.forEach(node => {
              this.expandedNodes[node.id] = false;
            });
          } else {
            // 如果是对象(键值对形式)
            Object.values(this.workflowDetails.tasks).forEach(node => {
              this.expandedNodes[node.id] = false;
            });
          }
        } catch (error) {
          console.error('加载工作流详情失败:', error);
          
          // 使用模拟数据
          this.workflowDetails = {
            id: workflowId,
            name: this.workflows.find(w => w.id === workflowId).name,
            description: this.workflows.find(w => w.id === workflowId).description,
            startTaskId: 'task1',
            tasks: {
              task1: {
                id: 'task1',
                name: '能量数据比较',
                type: 'MultiAttributeComparison',
                description: '对比两个时间段的能量阈值和微震事件数',
                inputParameters: {
                  timeRanges: ['2023-05-04/2023-05-05', '2023-05-03/2023-05-04'],
                  attributes: ['energyRelease', 'microseismicEventCount'],
                  aggregationMethod: 'AVERAGE'
                },
                possibleOutcomes: [
                  { id: 'outcome1_1', name: '能量增加', nextTaskId: 'task2', description: '能量值增加10%以上' },
                  { id: 'outcome1_2', name: '能量减少', nextTaskId: 'task3', description: '能量值减少10%以上' },
                  { id: 'outcome1_3', name: '能量稳定', nextTaskId: 'task4', description: '能量值变化在±10%以内' }
                ]
              },
              task2: {
                id: 'task2',
                name: '能量增加分析',
                type: 'DataAnalysis',
                description: '分析能量增加的原因和潜在影响',
                inputParameters: {},
                possibleOutcomes: [
                  { id: 'outcome2_1', name: '能量增加显著', nextTaskId: 'task5', description: '能量增加超过30%' },
                  { id: 'outcome2_2', name: '能量增加轻微', nextTaskId: null, description: '能量增加不超过30%' }
                ]
              },
              task3: {
                id: 'task3',
                name: '能量减少分析',
                type: 'DataAnalysis',
                description: '分析能量减少的原因和影响',
                inputParameters: {},
                possibleOutcomes: [
                  { id: 'outcome3_1', name: '能量减少显著', nextTaskId: 'task6', description: '能量减少超过30%' },
                  { id: 'outcome3_2', name: '能量减少轻微', nextTaskId: null, description: '能量减少不超过30%' }
                ]
              },
              task4: {
                id: 'task4',
                name: '能量稳定分析',
                type: 'DataAnalysis',
                description: '分析能量稳定的情况',
                inputParameters: {},
                possibleOutcomes: [
                  { id: 'outcome4_1', name: '能量稳定', nextTaskId: null, description: '能量保持稳定' }
                ]
              }
              // 添加更多模拟任务...
            }
          };
          
          // 初始化展开节点状态
          Object.keys(this.workflowDetails.tasks).forEach(nodeId => {
            this.expandedNodes[nodeId] = false;
          });
        }
      },
      
      resetState() {
        this.currentNodeId = null;
        this.executionPath = [];
        this.nodeResults = {};
        this.expandedNodes = {};
        this.currentTask = null;
        this.possibleOutcomes = [];
        this.analysisData = null;
        this.recommendedOutcome = null;
        this.showConfirmDialog = false;
        this.pendingOutcome = null;
        this.contradictionReason = '';
        this.showReasonError = false;
        this.currentTaskHasDeviation = false;
        this.deviationInfo = null;
        console.log('重置状态 - 完成');
      },
      
      // 启动工作流
      async startWorkflow() {
        // 重置工作流
        await fetch(`/api/tasks/workflows/${this.selectedWorkflow}/reset`, {
          method: 'POST'
        });
        
        // 获取工作流起始任务
        const workflow = await fetch(`/api/tasks/workflows/${this.selectedWorkflow}`).then(res => res.json());
        this.loadTask(workflow.startTaskId);
      },
      
      // 加载任务
      async loadTask(taskId) {
        try {
          this.currentNodeId = taskId;
          const response = await fetch(`/api/tasks/workflows/${this.selectedWorkflow}/tasks/${taskId}`);
          const taskData = await response.json();
          
          this.currentTask = {
            id: taskData.taskId,
            name: taskData.taskName,
            description: taskData.taskDescription,
            type: taskData.taskType,
            inputParameters: taskData.inputParameters
          };
          
          this.possibleOutcomes = taskData.possibleOutcomes;
          this.analysisData = taskData.analysisData;
          
          // 设置推荐选项
          this.setRecommendedOutcome(taskData.recommendation);
          
          // 展开当前节点
          this.expandedNodes[taskId] = true;
          
          // 检查是否存在前一个任务的决策偏差
          if (this.executionPath.length > 0) {
            const lastStep = this.executionPath[this.executionPath.length - 1];
            if (lastStep.hasDeviation) {
              // 在当前任务顶部显示决策偏差提示
              this.currentTaskHasDeviation = true;
              this.deviationInfo = {
                taskName: lastStep.nodeName,
                actualChoice: lastStep.outcomeName,
                recommendedChoice: lastStep.recommendedOutcomeName,
                reason: lastStep.deviationReason || '未提供理由'
              };
            } else {
              this.currentTaskHasDeviation = false;
            }
          }
        } catch (error) {
          console.error('加载任务失败:', error);
        }
      },
      
      // 设置推荐选项
      setRecommendedOutcome(recommendation) {
        if (recommendation) {
          // 使用后端提供的推荐
          this.recommendedOutcome = this.possibleOutcomes.find(o => o.id === recommendation.outcomeId);
        } else if (this.currentTask && this.currentTask.type === 'MultiAttributeComparison' && 
            this.analysisData && this.analysisData.analysisResults) {
          
          // 获取能量变化率
          const energyChange = this.getChangeRate('energyRelease');
          
          if (energyChange !== null) {
            // 基于能量变化率推荐选项
            if (energyChange > 10) {
              // 查找"能量增加"相关选项
              this.recommendedOutcome = this.possibleOutcomes.find(o => 
                o.name.includes('能量增加') || o.description.includes('能量增加'));
            } else if (energyChange < -10) {
              // 查找"能量减少"相关选项
              this.recommendedOutcome = this.possibleOutcomes.find(o => 
                o.name.includes('能量减少') || o.description.includes('能量减少'));
            } else {
              // 查找"能量稳定"相关选项
              this.recommendedOutcome = this.possibleOutcomes.find(o => 
                o.name.includes('能量稳定') || o.description.includes('能量稳定') || 
                o.name.includes('维持不变') || o.description.includes('维持不变'));
            }
          }
        } else {
          this.recommendedOutcome = null;
        }
      },
      
      // 判断是否为推荐选项
      isRecommendedOutcome(outcome) {
        return this.recommendedOutcome && this.recommendedOutcome.id === outcome.id;
      },
      
      // 判断是否与数据矛盾
      contradictsData(outcome) {
        if (!this.currentTask || !this.analysisData || !this.analysisData.analysisResults) {
          return false;
        }
        
        // 检查是否与数据明显矛盾
        if (this.currentTask.type === 'MultiAttributeComparison') {
          const energyChange = this.getChangeRate('energyRelease');
          
          if (energyChange !== null) {
            if (energyChange > 10 && (outcome.name.includes('能量减少') || 
                (outcome.description && outcome.description.includes('能量减少')))) {
              return true;
            }
            if (energyChange < -10 && (outcome.name.includes('能量增加') || 
                (outcome.description && outcome.description.includes('能量增加')))) {
              return true;
            }
          }
        }
        
        return false;
      },
      
      // 获取推荐理由
      getRecommendationReason() {
        if (!this.recommendedOutcome || !this.analysisData || !this.analysisData.analysisResults) {
          return '无法提供推荐原因';
        }
        
        // 针对不同任务类型提供推荐理由
        if (this.currentTask.type === 'MultiAttributeComparison') {
          const energyChange = this.getChangeRate('energyRelease');
          
          if (energyChange !== null) {
            if (energyChange > 10) {
              return `能量释放增加了${energyChange.toFixed(2)}%，明显高于10%的变化阈值`;
            } else if (energyChange < -10) {
              return `能量释放减少了${Math.abs(energyChange).toFixed(2)}%，明显低于-10%的变化阈值`;
            } else {
              return `能量释放变化为${energyChange.toFixed(2)}%，在±10%的稳定范围内`;
            }
          }
        }
        
        return '基于当前数据分析结果';
      },
      
      // 获取数据矛盾描述
      getDataContradiction() {
        if (!this.pendingOutcome || !this.analysisData || !this.analysisData.analysisResults) {
          return '与数据不符';
        }
        
        // 针对不同任务类型描述矛盾点
        if (this.currentTask.type === 'MultiAttributeComparison') {
          const energyChange = this.getChangeRate('energyRelease');
          
          if (energyChange !== null) {
            if (energyChange > 10 && this.pendingOutcome.name.includes('能量减少')) {
              return `能量释放实际增加了${energyChange.toFixed(2)}%`;
            } else if (energyChange < -10 && this.pendingOutcome.name.includes('能量增加')) {
              return `能量释放实际减少了${Math.abs(energyChange).toFixed(2)}%`;
            }
          }
        }
        
        return '与当前数据分析结果不符';
      },
      
      // 选择确认流程
      confirmOutcomeSelection(outcome) {
        this.pendingOutcome = outcome;
        this.showReasonError = false;
        
        // 检查是否需要确认
        if (this.contradictsData(outcome)) {
          this.showConfirmDialog = true;
          this.confirmationDialogType = 'contradiction';
          this.contradictionReason = '';
        } else {
          // 直接选择
          this.selectOutcome(outcome.id);
        }
      },
      
      // 确认矛盾选择
      confirmContradictingSelection() {
        if (!this.contradictionReason.trim()) {
          this.showReasonError = true;
          return;
        }
        
        this.showReasonError = false;
        this.selectOutcome(this.pendingOutcome.id, this.contradictionReason);
        this.showConfirmDialog = false;
        this.pendingOutcome = null;
        this.contradictionReason = '';
      },
      
      // 取消选择
      cancelSelection() {
        this.showConfirmDialog = false;
        this.pendingOutcome = null;
        this.contradictionReason = '';
        this.showReasonError = false;
      },
      
      // 确认选择
      confirmSelection(confirmed) {
        if (confirmed && this.pendingOutcome) {
          // 验证是否提供了理由（对于矛盾选择）
          if (this.confirmationDialogType === 'contradiction' && !this.contradictionReason.trim()) {
            this.showReasonError = true;
            return;
          }
          
          // 执行选择，附带理由
          this.selectOutcome(this.pendingOutcome.id, this.contradictionReason);
        }
        
        // 重置对话框状态
        this.showConfirmDialog = false;
        this.pendingOutcome = null;
        this.contradictionReason = '';
        this.showReasonError = false;
      },
      
      // 选择结果
      async selectOutcome(outcomeId, reason = '') {
        try {
          const requestBody = { 
            outcomeId: outcomeId 
          };
          
          // 如果有理由，添加到请求中
          if (reason) {
            requestBody.contradictionReason = reason;
          }
          
          const response = await fetch(`/api/tasks/workflows/${this.selectedWorkflow}/tasks/${this.currentNodeId}/select`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
          });
          
          const result = await response.json();
          
          // 更新执行路径，增加偏差信息
          this.executionPath.push({
            nodeId: result.taskId,
            outcomeId: result.selectedOutcome,
            outcomeName: result.outcomeName,
            hasDeviation: result.deviation ? true : false,
            recommendedOutcomeId: result.deviation ? result.deviation.recommendedOutcomeId : null,
            recommendedOutcomeName: result.deviation ? result.deviation.recommendedOutcomeName : null,
            deviationReason: result.deviation ? result.deviation.reason : null,
            nodeName: this.getNodeName(result.taskId)
          });
          
          // 保存结果数据
          if (result.outcomeData) {
            this.nodeResults[result.taskId] = result.outcomeData;
          }
          
          // 如果有下一个任务，加载它
          if (result.nextTaskId) {
            this.loadTask(result.nextTaskId);
          } else {
            // 工作流结束
            this.currentNodeId = null;
            this.currentTask = null;
            this.possibleOutcomes = [];
            this.analysisData = null;
            
            // 显示完成消息
            alert("工作流执行完成!");
          }
        } catch (error) {
          console.error('选择结果失败:', error);
        }
      },
      
      async resetWorkflow() {
  console.log('重置工作流 - 开始');
  console.log('选中的工作流ID:', this.selectedWorkflow);
  
  if (!this.selectedWorkflow) {
    console.error('没有选中的工作流');
    alert('请先选择一个工作流');
    return;
  }
  
  try {
    const response = await fetch(`/api/tasks/workflows/${this.selectedWorkflow}/reset`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    console.log('重置请求响应状态:', response.status);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const data = await response.json();
    console.log('重置响应数据:', data);
    
    // 重置前端状态
    this.resetState();
    
    // 通知用户
    alert('工作流已重置');
    
  } catch (error) {
    console.error('重置工作流失败:', error);
    alert('重置失败: ' + error.message);
  }
},
      
      toggleNodeExpansion(nodeId) {
        this.expandedNodes[nodeId] = !this.expandedNodes[nodeId];
      },
      
      getNodeName(nodeId) {
        if (!nodeId || !this.workflowDetails || !this.workflowDetails.tasks[nodeId]) {
          return nodeId;
        }
        return this.workflowDetails.tasks[nodeId].name;
      },
      
      getCurrentNodeName() {
        return this.currentTask ? this.currentTask.name : '';
      },
      
      getCurrentNodeDescription() {
        return this.currentTask ? this.currentTask.description : '';
      },
      
      isNodeExecuted(nodeId) {
        return this.executionPath.some(step => step.nodeId === nodeId);
      },
      
      isOutcomeSelected(nodeId, outcomeId) {
        return this.executionPath.some(step => step.nodeId === nodeId && step.outcomeId === outcomeId);
      },
      
      getOrderedNodes() {
        if (!this.workflowDetails || !this.workflowDetails.tasks) return [];
        
        const nodes = Object.values(this.workflowDetails.tasks);
        
        // 尝试按执行顺序排序
        const nodeOrder = {};
        let order = 0;
        
        // 先将起始节点放在第一位
        if (this.workflowDetails.startTaskId) {
          nodeOrder[this.workflowDetails.startTaskId] = order++;
        }
        
        // 然后按照执行路径的顺序
        this.executionPath.forEach(step => {
          if (nodeOrder[step.nodeId] === undefined) {
            nodeOrder[step.nodeId] = order++;
          }
          
          // 查找下一个节点
          const node = this.workflowDetails.tasks[step.nodeId];
          if (node) {
            const outcome = node.possibleOutcomes.find(o => o.id === step.outcomeId);
            if (outcome && outcome.nextTaskId && nodeOrder[outcome.nextTaskId] === undefined) {
              nodeOrder[outcome.nextTaskId] = order++;
            }
          }
        });
        
        // 将剩余的节点按ID排序
        nodes.forEach(node => {
          if (nodeOrder[node.id] === undefined) {
            nodeOrder[node.id] = order++;
          }
        });
        
        // 按序号排序
        return nodes.sort((a, b) => nodeOrder[a.id] - nodeOrder[b.id]);
      },
      
      formatParameterValue(value) {
        if (Array.isArray(value)) {
          return value.join(', ');
        }
        
        if (typeof value === 'object' && value !== null) {
          return JSON.stringify(value);
        }
        
        return value;
      },
      
      // 数据可视化相关方法
      toggleRawData() {
        this.showRawData = !this.showRawData;
      },
      
      hasComparisonData() {
        return this.analysisData && 
               this.analysisData.comparisonResults && 
               Object.keys(this.analysisData.comparisonResults).length > 0;
      },
      
      getChangeRate(attribute) {
        if (this.analysisData && 
            this.analysisData.analysisResults && 
            this.analysisData.analysisResults.changeRates) {
          return this.analysisData.analysisResults.changeRates[attribute];
        }
        return null;
      },
      
      getChangeRateClass(attribute) {
        const rate = this.getChangeRate(attribute);
        if (rate === null) return '';
        if (rate > 10) return 'positive-change';
        if (rate < -10) return 'negative-change';
        return 'neutral-change';
      },
      
      formatChangeRate(rate) {
        if (rate === null) return 'N/A';
        const sign = rate > 0 ? '+' : '';
        return `${sign}${rate.toFixed(2)}%`;
      },
      
      prepareComparisonChartData() {
        if (!this.hasComparisonData()) return null;
        
        const results = this.analysisData.comparisonResults;
        const periods = Object.keys(results);
        const datasets = [];
        
        // 提取所有属性
        const attributes = new Set();
        periods.forEach(period => {
          Object.keys(results[period]).forEach(attr => attributes.add(attr));
        });
        
        // 为每个属性创建数据集
        Array.from(attributes).forEach(attribute => {
          const data = periods.map(period => {
            return results[period][attribute] || 0;
          });
          
          datasets.push({
            label: this.getAttributeLabel(attribute),
            data: data,
            backgroundColor: this.getAttributeColor(attribute)
          });
        });
        
        return {
          labels: periods.map(this.formatPeriodLabel),
          datasets: datasets
        };
      },
      
      getAttributeLabel(attribute) {
        const labels = {
          'energyRelease': '能量释放',
          'microseismicEventCount': '微震事件数',
          // 添加更多属性标签...
        };
        return labels[attribute] || attribute;
      },
      
      getAttributeColor(attribute) {
        const colors = {
          'energyRelease': 'rgba(255, 99, 132, 0.7)',
          'microseismicEventCount': 'rgba(54, 162, 235, 0.7)',
          // 添加更多属性颜色...
        };
        return colors[attribute] || 'rgba(128, 128, 128, 0.7)';
      },
      
      formatPeriodLabel(period) {
        // 例如: "2023-05-04/2023-05-05" -> "5月4日-5日"
        if (period.includes('/')) {
          const [start, end] = period.split('/');
          const startDate = new Date(start);
          const endDate = new Date(end);
          return `${startDate.getMonth()+1}月${startDate.getDate()}日-${endDate.getDate()}日`;
        }
        return period;
      }
    }
  };
  </script>
  
  <style scoped>
  .task-chain-container {
    max-width: 1000px;
    margin: 0 auto;
    padding: 20px;
  }
  
  .title {
    font-size: 24px;
    margin-bottom: 20px;
    color: #333;
  }
  
  .workflow-selector {
    margin-bottom: 30px;
  }
  
  .workflow-list {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .workflow-item {
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 15px;
    flex: 1;
    min-width: 250px;
    cursor: pointer;
    transition: all 0.3s;
  }
  
  .workflow-item:hover {
    border-color: #007bff;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  }
  
  .workflow-item.active {
    border-color: #007bff;
    background-color: #f8f9fa;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  }
  
  .workflow-name {
    font-weight: bold;
    font-size: 16px;
    margin-bottom: 5px;
  }
  
  .workflow-description {
    font-size: 14px;
    color: #666;
  }
  
  /* 新增工作流来源样式 */
  .workflow-source {
    background-color: #f8f9fa;
    padding: 8px 12px;
    border-radius: 4px;
    margin-bottom: 15px;
    font-size: 14px;
  }
  
  .source-label {
    color: #6c757d;
    margin-right: 5px;
  }
  
  .source-value {
    font-weight: 500;
  }
  
  .source-id {
    color: #6c757d;
    margin-left: 5px;
  }
  
  .controls {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }
  
  .btn-primary, .btn-secondary {
    padding: 8px 16px;
    border-radius: 4px;
    border: none;
    cursor: pointer;
    font-size: 14px;
  }
  
  .btn-primary {
    background-color: #007bff;
    color: white;
  }
  
  .btn-primary:hover {
    background-color: #0069d9;
  }
  
  .btn-primary:disabled {
    background-color: #b3d7ff;
    cursor: not-allowed;
  }
  
  .btn-secondary {
    background-color: #6c757d;
    color: white;
  }
  
  .btn-secondary:hover {
    background-color: #5a6268;
  }
  
  .execution-path {
    margin-bottom: 20px;
    padding: 15px;
    background-color: #f8f9fa;
    border-radius: 8px;
  }
  
  .path-steps {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }
  
  .path-step {
    display: flex;
    align-items: center;
  }
  
  .node-name {
    font-weight: bold;
    color: #007bff;
  }
  
  .arrow {
    margin: 0 5px;
    color: #666;
  }
  
  .outcome-name {
    color: #28a745;
    font-weight: bold;
  }
  
  .connector {
    margin: 0 5px;
    color: #666;
  }
  
  .task-nodes {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }
  
  .task-node {
    border: 1px solid #ddd;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;
  }
  
  .task-node.current {
    border-color: #007bff;
    box-shadow: 0 0 0 1px #007bff;
  }
  
  .task-node.executed {
    border-color: #28a745;
  }
  
  .node-header {
    padding: 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: #f8f9fa;
    cursor: pointer;
  }
  
  .task-node.current .node-header {
    background-color: #e6f2ff;
  }
  
  .task-node.executed .node-header {
    background-color: #e6f7ee;
  }
  
  .node-title {
    font-weight: bold;
    display: flex;
    align-items: center;
  }
  
  .node-status {
    margin-right: 8px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
  }
  
  .node-status.executed {
    color: #28a745;
  }
  
  .node-status.current {
    color: #007bff;
  }
  
  .node-type {
    font-size: 12px;
    color: #666;
    padding: 3px 8px;
    background-color: #e9ecef;
    border-radius: 4px;
  }
  
  .node-details {
    padding: 15px;
    border-top: 1px solid #ddd;
  }
  
  .node-description {
    margin-bottom: 15px;
    color: #666;
  }
  
  .node-parameters, .node-outcomes, .node-results {
    margin-bottom: 15px;
  }
  
  .parameter {
    margin-bottom: 5px;
    font-size: 14px;
  }
  
  .param-name {
    font-weight: bold;
    color: #495057;
  }
  
  .param-value {
    color: #212529;
    word-break: break-word;
  }
  
  .outcome {
    margin-bottom: 8px;
    padding: 5px 10px;
    background-color: #f8f9fa;
    border-radius: 4px;
    font-size: 14px;
  }
  
  .outcome.selected {
    background-color: #e6f7ee;
    border-left: 3px solid #28a745;
  }
  
  .outcome-description {
    color: #666;
    font-size: 12px;
  }
  
  .outcome-next {
    margin-left: 5px;
    color: #007bff;
    font-size: 12px;
  }
  
  .results-data {
    background-color: #f8f9fa;
    padding: 10px;
    border-radius: 4px;
    font-size: 12px;
    overflow-x: auto;
    white-space: pre-wrap;
  }
  
  .task-execution {
    margin: 20px 0;
    padding: 15px;
    border: 1px solid #ddd;
    border-radius: 8px;
    background-color: #f8f9fa;
  }
  
  .start-workflow {
    margin: 20px 0;
    text-align: center;
  }
  
  .task-parameters {
    margin: 15px 0;
  }
  
  /* 数据可视化相关样式 */
  .comparison-visualization {
    background-color: #f8f9fa;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 20px;
  }
  
  .comparison-summary {
    display: flex;
    margin-bottom: 15px;
  }
  
  .key-metric {
    flex: 1;
    text-align: center;
    padding: 10px;
    border-radius: 5px;
    background-color: #e9ecef;
  }
  
  .metric-label {
    font-size: 14px;
    color: #495057;
    margin-bottom: 5px;
  }
  
  .metric-value {
    font-size: 24px;
    font-weight: bold;
  }
  
  .positive-change {
    color: #28a745;
  }
  
  .negative-change {
    color: #dc3545;
  }
  
  .neutral-change {
    color: #6c757d;
  }
  
  .comparison-chart {
    height: 250px;
    margin-bottom: 15px;
  }
  
  .raw-data-toggle {
    text-align: right;
  }
  
  .btn-toggle {
    background: none;
    border: none;
    color: #007bff;
    text-decoration: underline;
    cursor: pointer;
  }
  
  /* 选项与推荐相关样式 */
  .recommendation-banner {
    display: flex;
    align-items: center;
    background-color: #e6f7ff;
    border: 1px solid #91d5ff;
    border-radius: 6px;
    padding: 10px 15px;
    margin-bottom: 15px;
  }
  
  .recommendation-icon {
    font-size: 22px;
    color: #1890ff;
    margin-right: 10px;
  }
  
  .recommendation-text {
    flex: 1;
  }
  
  .recommendation-reason {
    font-size: 13px;
    color: #666;
    margin-top: 5px;
  }
  
  .outcome-selection {
    margin-top: 20px;
  }
  
  .outcome-option {
    margin-bottom: 10px;
  }
  
  .outcome-button {
    position: relative;
    padding: 12px 15px;
    text-align: left;
    width: 100%;
    border: 1px solid #d9d9d9;
    border-radius: 6px;
    background-color: white;
    cursor: pointer;
  }
  
  .outcome-button.recommended {
    border: 2px solid #52c41a;
    background-color: #f6ffed;
  }
  
  .outcome-button.contradicts-data {
    border: 2px solid #faad14;
    background-color: #fffbe6;
  }
  
  .data-support {
    position: absolute;
    top: 8px;
    right: 8px;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 10px;
  }
  
  .data-support.high {
    background-color: #f6ffed;
    color: #52c41a;
    border: 1px solid #b7eb8f;
  }
  
  .data-support.medium {
    background-color: #e6f7ff;
    color: #1890ff;
    border: 1px solid #91d5ff;
  }
  
  .data-support.low {
    background-color: #fffbe6;
    color: #faad14;
    border: 1px solid #ffe58f;
  }
  
  /* 确认对话框样式 */
  .confirmation-dialog {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }
  
  .dialog-content {
    background-color: white;
    border-radius: 8px;
    padding: 20px;
    width: 80%;
    max-width: 500px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  
  .dialog-content h3 {
    margin-top: 0;
    margin-bottom: 15px;
  }
  
  .reason-input {
    margin: 15px 0;
  }
  
  .reason-input label {
    display: block;
    margin-bottom: 5px;
  }
  
  .reason-input textarea {
    width: 100%;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 8px;
  }
  
  .dialog-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
  
  .dialog-actions button {
    margin-left: 10px;
    padding: 8px 16px;
    border-radius: 4px;
    border: 1px solid #d9d9d9;
    background: white;
    cursor: pointer;
  }
  
  .btn-confirm {
    background-color: #1890ff !important;
    color: white;
    border-color: #1890ff !important;
  }
  
  .btn-cancel {
    color: #666;
  }
  
  .no-data {
    text-align: center;
    padding: 20px;
    color: #999;
    font-style: italic;
  }
  
  /* 偏差警告相关样式 */
  .deviation-alert {
    background-color: #fff8e6;
    border: 1px solid #ffe39e;
    border-left: 4px solid #ffb400;
    padding: 15px;
    margin-bottom: 20px;
    border-radius: 4px;
  }
  
  .deviation-details {
    margin: 10px 0;
    background-color: #fffcf2;
    padding: 12px;
    border-radius: 4px;
  }
  
  .deviation-warning {
    display: flex;
    align-items: center;
    background-color: #fffbf0;
    padding: 10px;
    border-radius: 4px;
    margin-top: 10px;
    border: 1px dashed #ffcb47;
  }
  
  .warning-icon {
    display: inline-block;
    width: 24px;
    height: 24px;
    margin-right: 10px;
    background-color: #ffb400;
    border-radius: 50%;
    position: relative;
  }
  
  .warning-icon:before {
    content: "!";
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: white;
    font-weight: bold;
  }
  
  .contradiction-warning {
    background-color: #fff1f0;
    border: 1px solid #ffccc7;
    padding: 15px;
    margin-bottom: 15px;
    border-radius: 4px;
    display: flex;
    align-items: flex-start;
  }
  
  .contradiction-warning .warning-icon {
    background-color: #f5222d;
  }
  
  .warning-message {
    flex: 1;
  }
  
  .reason-input textarea.error {
    border-color: #f5222d;
    background-color: #fff2f0;
  }
  
  .reason-error {
    color: #f5222d;
    font-size: 12px;
    margin-top: 4px;
    display: block;
  }
  
  .contradiction-alert {
    background-color: #fff1f0;
    border: 1px solid #ffccc7;
    border-left: 4px solid #f5222d;
    padding: 12px;
    margin-bottom: 15px;
    border-radius: 4px;
    display: flex;
    align-items: flex-start;
  }
  
  .contradiction-message {
    flex: 1;
    color: #cf1322;
    font-weight: 500;
  }
  
  .user-justification {
    background-color: #f8f9fa;
    border: 1px solid #e9ecef;
    border-left: 3px solid #6c757d;
    padding: 10px 15px;
    margin-bottom: 15px;
    font-style: italic;
    color: #495057;
  }
  .task-selector {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.task-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.task-item {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.task-item:hover {
  border-color: #007bff;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.task-item.active {
  border-color: #007bff;
  background-color: #e6f7ff;
}

.task-name {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 5px;
}

.task-type {
  display: inline-block;
  padding: 3px 8px;
  background-color: #e9ecef;
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}

.task-description {
  font-size: 14px;
  color: #666;
}
  </style>