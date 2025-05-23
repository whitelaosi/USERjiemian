<template>
  <div class="task-form">
    <h2>{{ isNew ? '创建任务' : '编辑任务' }}</h2>
    
    <div class="form-group">
      <label for="task-type">任务类型</label>
      <select 
        id="task-type" 
        v-model="formData.type" 
        @change="handleTypeChange"
        :disabled="!isNew"
      >
        <option value="">-- 选择任务类型 --</option>
        <option 
          v-for="type in taskTypes" 
          :key="type.id" 
          :value="type.id"
        >
          {{ type.label }}
        </option>
      </select>
      <div class="error-message" v-if="errors.type">{{ errors.type }}</div>
    </div>
    
    <div class="form-group">
      <label for="task-label">任务名称</label>
      <input 
        id="task-label" 
        type="text" 
        v-model="formData.label"
        placeholder="给任务一个描述性的名称"
      />
      <div class="error-message" v-if="errors.label">{{ errors.label }}</div>
    </div>
    
    <div class="form-group">
      <label for="task-description">任务描述</label>
      <textarea 
        id="task-description" 
        v-model="formData.description"
        placeholder="描述任务的目的和内容"
        rows="3"
      ></textarea>
    </div>
    
    <!-- 动态属性字段 -->
    <div v-if="currentTypeProperties.length > 0">
      <h3>任务属性</h3>
      
      <div 
        v-for="prop in currentTypeProperties" 
        :key="prop.id" 
        class="form-group"
      >
        <label :for="`task-prop-${prop.id}`">{{ prop.label }}</label>
        
        <!-- 文本输入 -->
        <input 
          v-if="prop.type === 'text'" 
          :id="`task-prop-${prop.id}`"
          type="text" 
          v-model="formData.properties[prop.id]"
        />
        
        <!-- 数字输入 -->
        <input 
          v-else-if="prop.type === 'number'" 
          :id="`task-prop-${prop.id}`"
          type="number" 
          v-model.number="formData.properties[prop.id]"
        />
        
        <!-- 日期选择 -->
        <input 
          v-else-if="prop.type === 'date'" 
          :id="`task-prop-${prop.id}`"
          type="date" 
          v-model="formData.properties[prop.id]"
        />
        

<!-- 日期范围选择 -->
<div 
  v-else-if="prop.type === 'dateRange'" 
  class="date-range"
>
  <div class="date-range-inputs">
    <input 
      :id="`task-prop-${prop.id}-start`" 
      type="date" 
      v-model="formData.properties[`${prop.id}Start`]" 
      placeholder="开始日期"
    />
    <span class="date-separator">至</span>
    <input 
      :id="`task-prop-${prop.id}-end`" 
      type="date" 
      v-model="formData.properties[`${prop.id}End`]" 
      placeholder="结束日期"
    />
  </div>
</div>
        
        <!-- 下拉选择 -->
        <select 
          v-else-if="prop.type === 'select'" 
          :id="`task-prop-${prop.id}`"
          v-model="formData.properties[prop.id]"
        >
          <option value="">-- 请选择 --</option>
          <option 
            v-for="option in prop.options" 
            :key="option.value || option"
            :value="option.value || option"
          >
            {{ option.label || option }}
          </option>
        </select>
        
        <!-- 实体选择（简化实现） -->
        <input 
          v-else-if="prop.type === 'entity'" 
          :id="`task-prop-${prop.id}`"
          type="text" 
          v-model="formData.properties[prop.id]"
          placeholder="输入实体URI"
        />
        
        <!-- 默认文本输入 -->
        <input 
          v-else
          :id="`task-prop-${prop.id}`"
          type="text" 
          v-model="formData.properties[prop.id]"
        />
        
        <div class="error-message" v-if="errors[`prop_${prop.id}`]">
          {{ errors[`prop_${prop.id}`] }}
        </div>
      </div>
    </div>
    
    <div class="form-actions">
      <button class="btn-save" @click="saveTask">保存</button>
      <button class="btn-cancel" @click="cancel">取消</button>
    </div>
  </div>
</template>

<script>
import { getTaskType, getAllTaskTypes } from '../models/taskTypes';
import taskService from '../services/taskService';
export default {
  name: 'TaskForm',
  props: {
    task: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      taskTypes: [],
      formData: {
        uri: '',
        type: '',
        label: '',
        description: '',
        properties: {}
      },
      errors: {}
    };
  },
  computed: {
  isNew() {
    return !this.formData.uri;
  },
  currentTypeProperties() {
    if (!this.formData.type) return [];
    
    // 尝试从API获取的类型定义中获取属性
    const typeFromApi = this.taskTypes.find(t => t.id === this.formData.type);
    if (typeFromApi && typeFromApi.parameterSchema) {
      return Object.entries(typeFromApi.parameterSchema).map(([id, schema]) => ({
        id,
        label: schema.description || id,
        type: schema.type || 'text',
        options: schema.options || [],
        required: schema.required || false,
        default: schema.default
      }));
    }
    
    // 后备：从本地定义获取
    const taskType = getTaskType(this.formData.type);
    return taskType ? taskType.properties : [];
  }
},
  created() {
    this.loadTaskTypes();
    this.initForm();
  },
  methods: {
    async loadTaskTypes() {
  try {
    // 从API获取任务类型
    const typesData = await taskService.getTaskTypes();
    
    // 将API返回的数据格式转换为组件所需格式，保留完整的参数模式
    this.taskTypes = Object.entries(typesData).map(([id, type]) => ({
      id: id,
      label: type.name || id,
      description: type.description || '',
      parameterSchema: type.parameterSchema || {}
    }));
    
    console.log("从API加载的任务类型:", this.taskTypes);
  } catch (error) {
    console.error("加载任务类型失败:", error);
    // 使用本地备份
    this.taskTypes = getAllTaskTypes();
    console.log("使用本地任务类型备份");
  }
},
    
initForm() {
  console.log("初始化表单，接收到的任务数据:", this.task);
  
  if (this.task && this.task.uri) {
    // 检查localStorage中是否有完整数据
    const localStorageKey = `complete_task_${this.task.uri}`;
    const localData = localStorage.getItem(localStorageKey);
    
    let taskToUse = this.task;
    if (localData) {
      // 使用localStorage中的完整数据
      try {
        const parsedData = JSON.parse(localData);
        console.log("使用本地存储的完整任务数据:", parsedData);
        taskToUse = parsedData;
      } catch (e) {
        console.error("解析本地存储数据失败:", e);
      }
    }
    
    // 编辑现有任务，使用可能来自localStorage的增强数据
    const properties = {};
    
    // 处理来自parameters或properties的参数
    const paramsSource = taskToUse.parameters || taskToUse.properties || {};
    
    // 处理所有键值对，特别处理日期范围
    Object.entries(paramsSource).forEach(([key, value]) => {
      if (typeof value === 'string' && value.includes('/')) {
        const [start, end] = value.split('/');
        properties[`${key}Start`] = start;
        properties[`${key}End`] = end;
      } else {
        properties[key] = value;
      }
    });
    
    this.formData = {
      uri: taskToUse.uri,
      type: taskToUse.type || '',
      label: taskToUse.label || '',
      description: taskToUse.description || '',
      properties: properties
    };
    
    console.log("初始化的表单数据:", this.formData);
  } else {
    // 创建新任务，不变
  }
},
    
    handleTypeChange() {
      // 切换任务类型时重置属性
      this.formData.properties = {};
      this.errors = {};
    },
    
    validateForm() {
  this.errors = {};
  let isValid = true;
  
  // 验证基本字段
  if (!this.formData.type) {
    this.errors.type = '请选择任务类型';
    isValid = false;
  }
  
  if (!this.formData.label) {
    this.errors.label = '请输入任务名称';
    isValid = false;
  }
  
  // 验证特定类型的属性
  for (const prop of this.currentTypeProperties) {
    if (prop.required) {
      if (prop.type === 'dateRange') {
        // 检查日期范围的开始和结束
        if (!this.formData.properties[`${prop.id}Start`] || !this.formData.properties[`${prop.id}End`]) {
          this.errors[`prop_${prop.id}`] = `请选择完整的${prop.label}`;
          isValid = false;
        }
      } else if (!this.formData.properties[prop.id]) {
        this.errors[`prop_${prop.id}`] = `请输入${prop.label}`;
        isValid = false;
      }
    }
  }
  
  return isValid;
},
    
saveTask() {
  if (!this.validateForm()) {
    return;
  }
  
  // 处理日期范围和其他参数
  const parameters = {};
  
  // 遍历所有属性处理
  for (const prop of this.currentTypeProperties) {
    if (prop.type === 'dateRange') {
      // 合并日期范围为一个字符串
      const startDate = this.formData.properties[`${prop.id}Start`];
      const endDate = this.formData.properties[`${prop.id}End`];
      if (startDate && endDate) {
        parameters[prop.id] = `${startDate}/${endDate}`;
      }
    } else {
      // 其他属性直接复制
      parameters[prop.id] = this.formData.properties[prop.id];
    }
  }
  
  // 构建任务数据
  const taskData = { 
    ...this.formData,
    parameters: parameters,
    // 不要包含properties属性，避免混淆
    properties: undefined 
  };
  
  if (!taskData.uri) {
    delete taskData.uri;
  }
  
  console.log("保存任务数据:", taskData);
  
  // 发出保存事件
  this.$emit('save', taskData);
  
  // 保存后自动尝试获取最新数据 (新增部分)
  if (taskData.uri) {
    setTimeout(() => {
      // 已知任务URI，尝试刷新数据
      taskService.getTaskByUri(taskData.uri)
        .then(updatedTask => {
          console.log("保存后自动刷新获取的任务数据:", updatedTask);
          
          // 保存完整数据到localStorage以备后用
          const localStorageKey = `complete_task_${updatedTask.uri}`;
          localStorage.setItem(localStorageKey, JSON.stringify(updatedTask));
          
          // 重新初始化表单
          this.initForm();
        })
        .catch(error => {
          console.error("保存后自动刷新任务数据失败:", error);
        });
    }, 500); // 短暂延迟以等待保存完成
  }
},

cancel() {
    console.log('TaskForm: 取消按钮被点击');
    this.$emit('cancel');
  },
  refreshTask() {
  // 检查是否有URI
  if (!this.formData.uri) {
    console.warn("无法刷新：没有任务URI");
    return;
  }
  
  // 从服务器获取最新任务数据
  taskService.getTaskByUri(this.formData.uri)
    .then(updatedTask => {
      console.log("刷新得到的任务数据:", updatedTask);
      
      // 保存完整数据到localStorage以备后用
      const localStorageKey = `complete_task_${updatedTask.uri}`;
      localStorage.setItem(localStorageKey, JSON.stringify(updatedTask));
      
      // 正确的方式：发送事件而不是直接修改prop
      this.$emit('task-updated', updatedTask);
      
      // 重新初始化表单（使用当前formData.uri进行初始化）
      this.initForm();
      
      // 通知用户
      alert('任务数据已刷新');
    })
    .catch(error => {
      console.error("刷新任务数据失败:", error);
      alert('刷新任务数据失败: ' + (error.message || '未知错误'));
    });
}
  }
}
</script>

<style scoped>
.task-form {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 20px;
  color: #333;
}

h3 {
  margin-top: 16px;
  margin-bottom: 12px;
  font-size: 16px;
  color: #555;
}

.form-group {
  margin-bottom: 16px;
}

label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #555;
}

input, select, textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

select {
  height: 36px;
}

textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-left: 8px;
}

.btn-save {
  background-color: #1976d2;
  color: white;
}

.btn-save:hover {
  background-color: #1565c0;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #333;
}

.btn-cancel:hover {
  background-color: #e0e0e0;
}

.error-message {
  color: #d32f2f;
  font-size: 12px;
  margin-top: 4px;
}

.date-range {
  width: 100%;
}

.date-range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  flex-shrink: 0;
  color: #666;
}

.date-range input {
  flex: 1;
}
</style>