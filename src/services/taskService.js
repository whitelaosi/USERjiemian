// 添加详细请求日志记录
axios.interceptors.request.use(request => {
  console.log('API详细请求:', request.method, request.url, JSON.stringify(request.data));
  return request;
});

axios.interceptors.response.use(response => {
  console.log('API详细响应:', response.status, response.config.url, 
    response.data ? (typeof response.data === 'object' ? '数据对象' : response.data) : '无数据');
  return response;
}, error => {
  console.error('API详细错误:', error);
  return Promise.reject(error);
});
import axios from 'axios';

// 使用完全明确的URL，避免任何差异
const API_URL = '/api/tasks';
const TASK_PREFIX = 'http://example.com/task/';

// 添加请求拦截器 - 在每个请求前记录详情
axios.interceptors.request.use(request => {
  console.log('API请求:', request.method, request.url);
  return request;
});

// 添加响应拦截器 - 在每个响应后记录结果
axios.interceptors.response.use(response => {
  console.log('API响应:', response.status, response.config.url, Array.isArray(response.data) ? `[数组长度: ${response.data.length}]` : response.data);
  return response;
}, error => {
  console.error('API错误:', error);
  return Promise.reject(error);
});

// // 工具函数：确保URI是完整的，并正确编码
// function ensureFullUriAndEncode(uri) {
//   if (!uri) return '';
  
//   // 如果已经是完整URI，直接编码
//   if (uri.startsWith('http://') || uri.startsWith('https://')) {
//     return encodeURIComponent(uri);
//   }
  
//   // 否则，添加前缀后编码
//   return encodeURIComponent(TASK_PREFIX + uri);
// }

class TaskService {
  /**
   * 获取所有任务
   */
  async getAllTasks() {
    try {
      console.log('调用getAllTasks...');
      const response = await axios.get(API_URL, {
        headers: {
          'Cache-Control': 'no-cache',
          'Pragma': 'no-cache',
          'Content-Type': 'application/json'
        },
        withCredentials: false  // 尝试关闭credentials
      });
      console.log('获取任务数据成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('获取任务列表失败:', error);
      throw error;
    }
  }
  
/**
 * 获取单个任务 - 使用简化的ID方式，处理编码URI问题
 */
async getTaskByUri(uri) {
  try {
    // 首先检查URI是否已经编码，如果是则解码
    let decodedUri = uri;
    if (uri.includes('%')) {
      decodedUri = decodeURIComponent(uri);
    }
    
    // 从解码后的URI中提取ID
    const taskId = decodedUri.split('/').pop();
    console.log('获取任务，使用ID:', taskId);
    
    // 使用新的简化端点和正确的ID
    const response = await axios.get(`${API_URL}/get-by-id/${taskId}`);
    return response.data;
  } catch (error) {
    console.error(`获取任务失败:`, error);
    throw error;
  }
}
  
  /**
   * 创建新任务
   */
  async createTask(task) {
    try {
      const response = await axios.post(API_URL, task);
      return response.data;
    } catch (error) {
      console.error('创建任务失败:', error);
      throw error;
    }
  }
  
/**
 * 更新任务 - 混合方案（本地存储 + API调用尝试）
 */
async updateTask(uri, task) {
  try {
    // 1. 处理URI和任务数据
    let decodedUri = uri;
    if (uri.includes('%')) {
      decodedUri = decodeURIComponent(uri);
    }
    
    const taskId = decodedUri.split('/').pop();
    console.log('更新任务，使用ID:', taskId);
    
    // 克隆任务并确保参数正确
    const taskToUpdate = { ...task };
    taskToUpdate.uri = `http://example.com/task/${taskId}`;
    
    // 确保parameters属性存在
    if (!taskToUpdate.parameters) {
      taskToUpdate.parameters = {};
    }
    
    // 合并properties到parameters
    if (taskToUpdate.properties && Object.keys(taskToUpdate.properties).length > 0) {
      taskToUpdate.parameters = { ...taskToUpdate.properties, ...taskToUpdate.parameters };
      delete taskToUpdate.properties;
    }
    
    console.log("准备更新的完整数据:", taskToUpdate);
    
    // 2. 更新本地存储（优先确保UI体验）
    let localTasks = localStorage.getItem('localTasks');
    if (localTasks) {
      let tasks = JSON.parse(localTasks);
      const index = tasks.findIndex(t => t.uri === taskToUpdate.uri);
      if (index !== -1) {
        tasks[index] = taskToUpdate;
        localStorage.setItem('localTasks', JSON.stringify(tasks));
        
        // 单独保存完整版本，用于表单编辑
        localStorage.setItem(`complete_task_${taskToUpdate.uri}`, JSON.stringify(taskToUpdate));
        console.log("已更新本地任务数据");
      }
    }
    
    // 3. 尝试多种API调用方式
    try {
      // 首先尝试简化ID方式
      const response = await axios.put(`${API_URL}/update-by-id/${taskId}`, taskToUpdate);
      console.log('使用update-by-id端点更新成功:', response.data);
      return response.data;
    } catch (error1) {
      console.warn('update-by-id端点失败，尝试原始方法:', error1.message);
      
      try {
        // 再尝试标准方式
        const encodedUri = encodeURIComponent(taskToUpdate.uri);
        const response = await axios.put(`${API_URL}/${encodedUri}`, taskToUpdate);
        console.log('使用标准端点更新成功:', response.data);
        return response.data;
      } catch (error2) {
        console.warn('标准端点也失败，使用本地存储版本:', error2.message);
        return taskToUpdate; // 返回本地版本确保UI体验
      }
    }
  } catch (error) {
    console.error(`更新任务失败:`, error);
    throw error;
  }
}
  /**
   * 删除任务（使用编码的URI）
   */
  async deleteTask(uri) {
    try {
      // 简化编码逻辑，避免双重编码
      let encodedUri;
      if (uri.includes('http://') || uri.includes('https://')) {
        // 如果已经是完整URI，只做一次编码
        encodedUri = encodeURIComponent(uri);
      } else {
        // 否则添加前缀再编码
        encodedUri = encodeURIComponent(TASK_PREFIX + uri);
      }
      
      console.log('删除任务，原始URI:', uri);
      console.log('编码后URI:', encodedUri);
      console.log('完整请求URL:', `${API_URL}/${encodedUri}`);
      
      await axios.delete(`${API_URL}/${encodedUri}`);
      return true;
    } catch (error) {
      console.error(`删除任务失败:`, error);
      throw error;
    }
  }
  
/**
 * 执行任务 - 简化处理任务ID
 */
async executeTask(uri) {
  try {
    // 1. 只提取任务ID（URI最后一部分）
    const taskId = uri.split('/').pop();
    console.log('执行任务，使用任务ID:', taskId);
    
    // 2. 使用标准格式重建完整URI
    const standardUri = `http://example.com/task/${taskId}`;
    console.log('标准化URI:', standardUri);
    
    // 3. 编码URI
    const encodedUri = encodeURIComponent(standardUri);
    console.log('编码后URI:', encodedUri);
    
    // 4. 发送请求
    const response = await axios.get(`${API_URL}/${encodedUri}/execute`);
    return response.data;
  } catch (error) {
    console.error(`执行任务失败:`, error);
    throw error;
  }
}
  
  /**
   * 获取可用的任务类型
   */
  async getTaskTypes() {
    try {
      const response = await axios.get(`${API_URL}/types`);
      return response.data;
    } catch (error) {
      console.error('获取任务类型失败:', error);
      throw error;
    }
  }
  /**
 * 使用POST方法删除任务（替代方案）
 */
  async deleteTaskViaPost(uri) {
    try {
      console.log('使用POST方法删除任务，URI:', uri);
      
      // 注意这里的URL路径
      const response = await axios.post(`${API_URL}/delete`, { uri: uri });
      
      console.log('删除成功响应:', response.data);
      return true;
    } catch (error) {
      console.error(`删除任务失败:`, error);
      throw error;
    }
  }
/**
 * 通过ID执行任务（简化版）
 */
async executeTaskById(uri) {
  try {
    // 提取ID部分
    const taskId = uri.split('/').pop();
    console.log('通过ID执行任务:', taskId);
    
    // 使用相对路径，依赖Vue代理
    const response = await axios.get(`${API_URL}/execute-by-id/${taskId}`);
    
    return response.data;
  } catch (error) {
    console.error(`执行任务失败:`, error);
    throw error;
  }
}

}

export default new TaskService();