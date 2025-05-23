<!-- D:\USERjiemian\rdf-search-app\src\components\TaskListItem.vue -->
<template>
  <div class="task-list-item" :class="{ 'active': isActive }">
    <!-- 添加调试信息，帮助排查 -->
    <pre v-if="showDebug" class="debug-info">{{ JSON.stringify(task, null, 2) }}</pre>
    
    <div class="task-icon">
      <i :class="'icon-' + getTaskTypeIcon()"></i>
    </div>
    <div class="task-content">
      <div class="task-header">
        <h3 class="task-title">{{ task.label || '未命名任务' }}</h3>
        <span class="task-type">{{ getTaskTypeName() }}</span>
      </div>
      <div class="task-description" v-if="task.description">
        {{ task.description }}
      </div>
      <div class="task-properties" v-if="task.properties">
        <div v-for="(value, key) in task.properties" :key="key" class="task-property">
          <span class="property-name">{{ getPropertyLabel(key) }}:</span>
          <span class="property-value">{{ formatPropertyValue(key, value) }}</span>
        </div>
      </div>
    </div>
    <div class="task-actions">
      <button class="btn-execute" @click.stop="executeTask">执行</button>
      <button class="btn-edit" @click.stop="$emit('edit', task)">编辑</button>
      <button class="btn-delete" @click.stop="confirmDelete">删除</button>
      <button class="btn-visual-mapping" @click.stop="$emit('visual-mapping', task)">视觉映射</button>
    </div>
  </div>
</template>

<script>
import { getTaskType } from '../models/taskTypes';
import taskService from '../services/taskService';

export default {
  name: 'TaskListItem',
  props: {
    task: {
      type: Object,
      required: true
    },
    isActive: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showDebug: false // 设为true可以显示调试信息
    }
  },
  created() {
    // 在创建时输出任务信息到控制台，帮助调试
    console.log("TaskListItem创建，任务数据:", this.task);
    console.log("任务类型:", this.task.type);
    console.log("从taskTypes获取的信息:", getTaskType(this.task.type));
  },
  methods: {
    getTaskTypeIcon() {
      try {
        const taskType = getTaskType(this.task.type);
        return taskType ? taskType.icon : 'default';
      } catch (e) {
        console.error("获取任务图标错误:", e);
        return 'default';
      }
    },
    
    getTaskTypeName() {
      try {
        const taskType = getTaskType(this.task.type);
        return taskType ? taskType.label : (this.task.type || '未知类型');
      } catch (e) {
        console.error("获取任务类型名称错误:", e);
        return this.task.type || '未知类型';
      }
    },
    
    getPropertyLabel(propertyId) {
      try {
        const taskType = getTaskType(this.task.type);
        if (!taskType) return propertyId;
        
        const property = taskType.properties.find(p => p.id === propertyId);
        return property ? property.label : propertyId;
      } catch (e) {
        console.error("获取属性标签错误:", e);
        return propertyId;
      }
    },
    
    formatPropertyValue(propertyId, value) {
      if (value === null || value === undefined) return '';
      
      try {
        const taskType = getTaskType(this.task.type);
        if (!taskType) return value;
        
        const property = taskType.properties.find(p => p.id === propertyId);
        if (!property) return value;
        
        // 根据属性类型格式化值
        if (property.type === 'date') {
          return new Date(value).toLocaleDateString();
        } else if (property.type === 'select') {
          // 查找选项标签
          const option = property.options?.find(o => o.value === value || o === value);
          return option ? (option.label || option) : value;
        }
        
        return value;
      } catch (e) {
        console.error("格式化属性值错误:", e);
        return value;
      }
    },
    
    async executeTask() {
    try {
      this.loading = true;
      
      // 关键修改：执行前先获取最新的任务数据
      const freshTask = await taskService.getTaskByUri(this.task.uri);
      console.log("执行前获取的最新任务数据:", freshTask);
      
      // 使用最新的任务数据执行
      const results = await taskService.executeTaskById(freshTask.uri.split('/').pop());
      
      this.loading = false;
      
      // 发出执行成功事件，同时传递最新的任务数据
      this.$emit('execute', { 
        task: freshTask, // 使用刚获取的最新任务对象
        results: results 
      });
    } catch (error) {
      this.loading = false;
      console.error("执行任务失败:", error);
      this.$emit('error', "执行任务失败: " + error.message);
    }
  },
    
// 在TaskListItem.vue中
async deleteTask() {
  try {
    // 直接从localStorage删除
    const localTasks = localStorage.getItem('localTasks');
    if (localTasks) {
      let tasks = JSON.parse(localTasks);
      tasks = tasks.filter(t => t.uri !== this.task.uri);
      localStorage.setItem('localTasks', JSON.stringify(tasks));
      
      // 通知父组件更新列表
      this.$emit('delete-success', this.task.uri);
    }
    
    // 尝试后端删除，但不阻塞UI
    try {
      await taskService.deleteTaskViaPost(this.task.uri);
    } catch (error) {
      console.warn("后端删除失败，但本地已删除", error);
    }
    
    this.$emit('error', '任务已删除', 'success');
  } catch (error) {
    this.$emit('error', '删除任务失败', 'error');
    console.error('删除失败:', error);
  }
},
    
    getTaskId() {
      // 从URI中提取ID
      const uri = this.task.uri;
      if (!uri) {
        console.error("任务没有URI:", this.task);
        return '';
      }
      
      try {
        const parts = uri.split('/');
        return parts[parts.length - 1];
      } catch (e) {
        console.error("解析任务ID错误:", e);
        return '';
      }
    }
  }
}
</script>

<style scoped>
.task-list-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-wrap: wrap; /* 允许内容换行 */
}

.debug-info {
  width: 100%;
  background: #f0f0f0;
  padding: 10px;
  border: 1px dashed #999;
  font-family: monospace;
  font-size: 12px;
  color: #333;
  white-space: pre-wrap;
  overflow-x: auto;
  margin-bottom: 10px;
}

.task-list-item:hover {
  background-color: #f5f5f5;
}

.task-list-item.active {
  background-color: #e3f2fd;
}

.task-icon {
  flex: 0 0 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #1976d2;
}

.task-content {
  flex: 1;
  margin: 0 16px;
  min-width: 200px; /* 确保内容区域有最小宽度 */
}

.task-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap; /* 允许在小屏幕上换行 */
}

.task-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  margin-right: 8px;
}

.task-type {
  margin-top: 4px;
  padding: 2px 8px;
  font-size: 12px;
  color: white;
  background-color: #1976d2;
  border-radius: 4px;
}

.task-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.task-properties {
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
}

.task-property {
  margin-right: 16px;
  margin-bottom: 4px;
}

.property-name {
  font-weight: 500;
  color: #666;
}

.task-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.task-actions button {
  margin: 4px 0;
  padding: 4px 12px;
  background-color: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
}

.task-actions button {
  margin: 4px 0;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  color: #333; /* 默认深色文字 */
}

.btn-execute {
  background-color: #4caf50;
  color: #ffffff; /* 明确设置白色文字 */
  border: 1px solid #388e3c;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5); /* 添加文字阴影增加对比度 */
}

.btn-edit {
  background-color: #ffc107;
  color: #333333; /* 深色文字 */
  border: 1px solid #ffa000;
}

.btn-delete {
  background-color: #f44336;
  color: #ffffff; /* 明确设置白色文字 */
  border: 1px solid #d32f2f;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5); /* 添加文字阴影增加对比度 */
}

.btn-visual-mapping {
  background-color: #9c27b0;
  color: #ffffff; /* 明确设置白色文字 */
  border: 1px solid #7b1fa2;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5); /* 添加文字阴影增加对比度 */
}
</style>