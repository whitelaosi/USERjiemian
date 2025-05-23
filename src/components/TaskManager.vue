<template>
  <div class="task-manager">
    <div class="task-header">
      <h1>任务管理</h1>
      <div class="task-controls">
        <button class="btn-create" @click="showCreateForm">创建新任务</button>
        <button class="btn-refresh" @click="forcedLoadTasks">刷新列表</button>
        <button v-if="debugMode" class="btn-debug" @click="toggleDebug">{{ showDebugInfo ? '隐藏调试' : '显示调试' }}</button>
      </div>
    </div>
    
    <!-- 调试信息区域 -->
    <div v-if="debugMode && showDebugInfo" class="debug-panel">
      <h3>调试信息</h3>
      <p><strong>API URL:</strong> {{ API_URL }}</p>
      <p><strong>任务数量:</strong> {{ tasks ? tasks.length : 'undefined' }}</p>
      <p><strong>加载状态:</strong> {{ loading ? '加载中' : '已完成' }}</p>
      <p><strong>加载尝试次数:</strong> {{ loadAttempts }}</p>
      <p><strong>API响应:</strong> 
        <button @click="testDirectAPI">直接测试API</button>
        <button @click="testTaskTypes">测试任务类型API</button>
        <!-- 在debug-panel中添加 -->
<button @click="verifyTaskParameters" class="btn-debug">验证参数存储</button>
      </p>
      <div v-if="apiTestResult">
        <pre>{{ apiTestResult }}</pre>
      </div>
      <div v-if="tasks && tasks.length > 0">
        <h4>任务数据:</h4>
        <pre>{{ JSON.stringify(tasks[0], null, 2) }}</pre>
      </div>
    </div>
    
    <div class="task-container">
      <!-- 任务列表部分 -->
      <div class="task-list" v-if="!showForm">
        <div v-if="loading" class="loading">
          加载中...
        </div>
        
        <div v-else-if="!tasks || tasks.length === 0" class="empty-state">
          暂无任务，点击"创建新任务"按钮添加第一个任务。
        </div>
        
        <template v-else>
          <div class="debug-raw-data">
            <h3>API返回的原始数据:</h3>
            <p class="task-count">数据类型: {{ typeof tasks }}, 是否数组: {{ Array.isArray(tasks) }}, 长度: {{ Array.isArray(tasks) ? tasks.length : 'N/A' }}</p>
            <pre>{{ JSON.stringify(tasks, null, 2) }}</pre>
            
            <!-- 如果有任务数据，还是显示正常的任务列表 -->
            <div v-if="Array.isArray(tasks) && tasks.length > 0">
              <h3>任务列表:</h3>
              <task-list-item
  v-for="task in tasks"
  :key="task.uri"
  :task="task"
  :is-active="activeTask && activeTask.uri === task.uri"
  @click="selectTask(task)"
  @edit="editTask"
  @delete="deleteTask"
  @execute="handleTaskExecution"
  @visual-mapping="goToVisualMapping"
  @error="showError"
/>
            </div>
          </div>
        </template>
      </div>
      
      <!-- 任务表单部分 -->
      <task-form
  v-if="showForm"
  :task="currentTask"
  @save="saveTask"
  @task-updated="updateTask"
  @cancel="cancelForm"
/>
      
      <!-- 任务执行结果部分 -->
      <task-execution-results
  v-if="executingTask && executionResults"
  :task="executingTask"
  :results="executionResults"
  :loading="executionLoading"
  @close="clearResults"
  @message="showError"
/>
      <div class="task-results" v-if="taskResults && taskResults.length > 0">
        <h2>执行结果</h2>
        <button class="btn-close-results" @click="clearResults">关闭</button>
        
        <div class="results-content">
          <table>
            <thead>
              <tr>
                <th v-for="(_, key) in taskResults[0]" :key="key">{{ key }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(result, index) in taskResults" :key="index">
                <td v-for="(value, key) in result" :key="key">{{ value }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <button @click="testDeleteAPI">测试删除API</button>
    <!-- 错误消息提示 -->
    <div class="error-toast" v-if="errorMessage" :class="{'success': errorType === 'success'}">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script>
import TaskListItem from './TaskListItem.vue';
import TaskForm from './TaskForm.vue';
import taskService from '../services/taskService';
// 如果文件在子目录中
import TaskExecutionResults from './taskExecution/TaskExecutionResults.vue';

export default {
  name: 'TaskManager',
  components: {
    TaskListItem,
    TaskForm,
    TaskExecutionResults
  },
  data() {
    return {
      tasks: [],
      activeTask: null,
      currentTask: null,
      showForm: false,
      loading: true,
      taskResults: null,
      errorMessage: null,
      errorType: 'error',
      errorTimeout: null,
      debugMode: true,
      showDebugInfo: false,
      loadAttempts: 0,
      API_URL: '/api/tasks',
      apiTestResult: null,
      executingTask: null,    // 当前执行中的任务
executionResults: null, // 任务执行结果
executionLoading: false, // 执行加载状态
    };
  },
  created() {
    console.log("TaskManager组件已创建");
    this.loadTasks();
  },
  methods: {
    async loadTasks() {
      this.loading = true;
      try {
        const tasksData = await taskService.getAllTasks();
        console.log("API返回的任务数据:", tasksData);
        
        if (Array.isArray(tasksData) && tasksData.length === 0) {
          const localTasks = localStorage.getItem('localTasks');
          if (localTasks) {
            this.tasks = JSON.parse(localTasks);
            console.log("从本地存储加载任务:", this.tasks.length);
          } else {
            this.tasks = [];
          }
        } else {
          this.tasks = tasksData;
          localStorage.setItem('localTasks', JSON.stringify(tasksData));
        }
      } catch (error) {
        this.tasks = [];
      } finally {
        this.loading = false;
      }
    },
    
    forcedLoadTasks() {
      this.showError('正在刷新任务列表...', 'success');
      this.loadTasks();
    },
    
    async testDirectAPI() {
  try {
    // 使用相对路径而不是绝对URL
    const exactUrl = '/api/tasks';
    console.log("请求URL:", exactUrl);
    
    const response = await fetch(exactUrl, {
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    });
        
        const responseText = await response.text();
        console.log("原始响应文本:", responseText);
        
        let data;
        try {
          data = JSON.parse(responseText);
        } catch (e) {
          this.apiTestResult = `解析错误: ${e.message}\n原始响应: ${responseText}`;
          return;
        }
        
        this.apiTestResult = JSON.stringify(data, null, 2);
        this.showError(`API测试成功: 获取到${Array.isArray(data) ? data.length : 0}条任务`, 'success');
      } catch (error) {
        this.apiTestResult = `错误: ${error.message}`;
        this.showError('API测试失败', 'error');
      }
    },
    
    async testTaskTypes() {
      try {
        const response = await fetch(`${this.API_URL}/types`);
        const data = await response.json();
        this.apiTestResult = JSON.stringify(data, null, 2);
        this.showError('任务类型API测试成功', 'success');
      } catch (error) {
        this.apiTestResult = `错误: ${error.message}`;
        this.showError('任务类型API测试失败', 'error');
      }
    },
    
    toggleDebug() {
      this.showDebugInfo = !this.showDebugInfo;
    },
    
    selectTask(task) {
      this.activeTask = task;
      this.$emit('task-selected', task);
    },
    
    showCreateForm() {
      this.currentTask = null;
      this.showForm = true;
    },
    
    editTask(task) {
      this.currentTask = { ...task };
      this.showForm = true;
    },
    
    cancelForm() {
      this.showForm = false;
      this.currentTask = null;
    },
    
    async saveTask(taskData) {
      try {
        let savedTask;
        
        if (taskData.uri) {
          // 更新任务时，使用编码的URI
          const encodedUri = encodeURIComponent(taskData.uri);
          console.log("更新任务:", encodedUri, taskData);
          
          savedTask = await taskService.updateTask(encodedUri, taskData);
          
          const localTasks = localStorage.getItem('localTasks');
          if (localTasks) {
            let tasks = JSON.parse(localTasks);
            const index = tasks.findIndex(t => t.uri === taskData.uri);
            if (index !== -1) {
              tasks[index] = savedTask;
            } else {
              tasks.push(savedTask);
            }
            localStorage.setItem('localTasks', JSON.stringify(tasks));
          }
          
          this.showError('任务更新成功', 'success');
        } else {
          // 创建新任务时，确保不包含URI
          const newTaskData = { ...taskData };
          delete newTaskData.uri;
          
          console.log("创建新任务:", newTaskData);
          savedTask = await taskService.createTask(newTaskData);
          
          const localTasks = localStorage.getItem('localTasks');
          let tasks = localTasks ? JSON.parse(localTasks) : [];
          tasks.push(savedTask);
          localStorage.setItem('localTasks', JSON.stringify(tasks));
          
          this.showError('任务创建成功', 'success');
        }
        
        console.log("保存结果:", savedTask);
        
        this.showForm = false;
        
        const localTasks = localStorage.getItem('localTasks');
        if (localTasks) {
          this.tasks = JSON.parse(localTasks);
          console.log("从本地存储加载任务:", this.tasks.length);
          
          if (savedTask) {
            const task = this.tasks.find(t => t.uri === savedTask.uri);
            if (task) {
              this.selectTask(task);
            }
          }
        }
        
        setTimeout(async () => {
          try {
            const tasksData = await taskService.getAllTasks();
            if (Array.isArray(tasksData) && tasksData.length > 0) {
              this.tasks = tasksData;
              localStorage.setItem('localTasks', JSON.stringify(tasksData));
            }
          } catch (error) {
            console.error("后台刷新任务失败:", error);
          }
        }, 1000);
      } catch (error) {
        this.showError('保存任务失败，请稍后重试');
        console.error('保存任务失败:', error);
      }
    },
    
    async deleteTask(task) {
  try {
    // 无论后端结果如何，都先从本地删除
    const localTasks = localStorage.getItem('localTasks');
    if (localTasks) {
      let tasks = JSON.parse(localTasks);
      tasks = tasks.filter(t => t.uri !== task.uri);
      localStorage.setItem('localTasks', JSON.stringify(tasks));
      this.tasks = tasks;
    }
    
    // 然后尝试从后端删除
    try {
      await taskService.deleteTaskViaPost(task.uri);
      this.showError('任务已成功删除', 'success');
    } catch (error) {
      // 即使后端删除失败，前端显示也已更新
      console.warn("后端删除失败，但本地删除成功", error);
      this.showError('任务已从本地删除', 'success');
    }
  } catch (error) {
    this.showError('删除任务失败', 'error');
    console.error('删除失败:', error);
  }
},
deleteSuccess(deletedTaskUri) {
    this.tasks = this.tasks.filter(t => t.uri !== deletedTaskUri);
  },
    // 在TaskManager.vue中添加
async testDeleteAPI() {
  try {
    const taskToDelete = this.tasks[0]; // 获取第一个任务
    if (!taskToDelete) {
      this.apiTestResult = "没有任务可删除";
      return;
    }
    
    console.log("测试删除任务:", taskToDelete.uri);
    const response = await fetch(`${this.API_URL}/delete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ uri: taskToDelete.uri })
    });
    
    const text = await response.text();
    this.apiTestResult = `状态: ${response.status}, 响应: ${text}`;
    this.showError(`删除API测试: ${response.ok ? '成功' : '失败'}`, response.ok ? 'success' : 'error');
  } catch (error) {
    this.apiTestResult = `错误: ${error.message}`;
    this.showError('删除API测试失败', 'error');
  }
},
// 在methods中添加
async verifyTaskParameters() {
  try {
    if (!this.activeTask) {
      this.showError('请先选择一个任务');
      return;
    }
    
    // 直接从后端获取最新数据
    const freshTask = await taskService.getTaskByUri(this.activeTask.uri);
    
    // 显示对比信息
    const localParams = this.activeTask.parameters ? Object.keys(this.activeTask.parameters).length : 0;
    const backendParams = freshTask.parameters ? Object.keys(freshTask.parameters).length : 0;
    
    let message = `参数验证结果:\n`;
    message += `- 前端显示参数数量: ${localParams}\n`;
    message += `- 后端存储参数数量: ${backendParams}\n`;
    
    if (backendParams > 0) {
      message += `\n后端存储的参数:\n`;
      Object.entries(freshTask.parameters).forEach(([key, value]) => {
        message += `- ${key}: ${value}\n`;
      });
      this.showError('后端参数存储成功! 前端补偿方案可以移除', 'success');
    } else {
      message += `\n后端参数存储失败，需要继续保留前端补偿方案`;
      this.showError('后端参数存储失败', 'error');
    }
    
    // 在console中显示详细信息
    console.log(message);
    this.apiTestResult = message;
  } catch (error) {
    this.showError('验证参数存储失败');
    console.error('验证参数失败:', error);
  }
},
handleTaskExecution(data) {
  // 支持旧的显示方法
  this.taskResults = data.results;
  
  // 支持新的显示组件
  this.executingTask = data.task;
  this.executionResults = data.results;
  this.executionLoading = false;
  
  this.$emit('task-executed', {
    task: data.task,
    results: data.results
  });
},
    
clearResults() {
  // 清除旧的结果
  this.taskResults = null;
  
  // 清除新的结果
  this.executingTask = null;
  this.executionResults = null;
  this.executionLoading = false;
},
    
    showError(message, type = 'error') {
      if (this.errorTimeout) {
        clearTimeout(this.errorTimeout);
      }
      
      this.errorMessage = message;
      this.errorType = type;
      
      this.errorTimeout = setTimeout(() => {
        this.errorMessage = null;
      }, 3000);
    },
    updateTask(updatedTask) {
  console.log("接收到更新后的任务:", updatedTask);
  this.currentTask = updatedTask;
  
  // 如果在任务列表中存在该任务，也更新列表中的任务
  if (this.tasks && this.tasks.length > 0) {
    const index = this.tasks.findIndex(t => t.uri === updatedTask.uri);
    if (index !== -1) {
      this.tasks[index] = updatedTask;
    }
  }
  
  this.showError('任务数据已刷新', 'success');
},
goToVisualMapping(task) {
  console.log('跳转到视觉映射编辑器，任务:', task);
  
  // 设置当前活动任务
  this.activeTask = task;
  
  // 方法1：如果使用父组件的currentView属性控制视图
  this.$parent.currentView = 'visualMapping';
  
  // 明确发送带有任务数据的事件
  this.$emit('navigate-to-visual-mapping', task);
  
  // 存储任务数据到localStorage，作为备份传递方式
  localStorage.setItem('currentMappingTask', JSON.stringify(task));
}
  }
}
</script>

<style scoped>
/* 样式保持不变 */
.task-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.task-controls {
  display: flex;
  gap: 10px;
}

h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.btn-create {
  padding: 8px 16px;
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-refresh {
  padding: 8px 16px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-debug {
  padding: 8px 16px;
  background-color: #ff9800;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.debug-panel {
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  padding: 15px;
  margin: 10px;
  border-radius: 5px;
  font-family: monospace;
  overflow-x: auto;
}

.debug-panel pre {
  background-color: #eee;
  padding: 10px;
  border-radius: 3px;
  white-space: pre-wrap;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}

.debug-panel button {
  margin-right: 10px;
  padding: 4px 8px;
  background-color: #333;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.task-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}

.task-count {
  margin: 10px 0;
  font-size: 14px;
  color: #666;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.loading, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #666;
  font-size: 16px;
}

.task-results {
  position: relative;
  margin-top: 20px;
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.task-results h2 {
  margin-top: 0;
  margin-bottom: 16px;
}

.btn-close-results {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.results-content {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background-color: #eeeeee;
  font-weight: 500;
}

tr:hover {
  background-color: #f0f0f0;
}

.error-toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  background-color: #f44336;
  color: white;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.error-toast.success {
  background-color: #4caf50;
}
</style>