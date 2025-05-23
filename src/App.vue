<template>
  <div id="app">
    <header class="app-header">
      <h1>RDF数据查询系统</h1>
      <!-- 添加导航选项 -->
      <div class="nav-tabs">
        <button 
          :class="{ active: currentView === 'search' }" 
          @click="currentView = 'search'"
        >
          RDF搜索
        </button>
        <button 
          :class="{ active: currentView === 'tasks' }" 
          @click="currentView = 'tasks'"
        >
          任务管理
        </button>
        <button 
          :class="{ active: currentView === 'taskChain' }" 
          @click="currentView = 'taskChain'"
        >
          任务链执行器
        </button>
        <button 
          :class="{ active: currentView === 'visualMapping' }" 
          @click="currentView = 'visualMapping'"
        >
          视觉映射编辑器
        </button>
      </div>
    </header>
    
    <main>
      <!-- 根据当前视图切换组件 -->
      <RdfSearch v-if="currentView === 'search'" />
      <TaskManager 
  v-if="currentView === 'tasks'" 
  ref="taskManager"
  @task-selected="onTaskSelected"
  @navigate-to-visual-mapping="navigateToVisualMapping"
/>
      <TaskChainVisualizer v-if="currentView === 'taskChain'" />
      <VisualMappingEditor 
  v-if="currentView === 'visualMapping'" 
  ref="visualMappingEditor"
  :selected-task="selectedTask"
  @svv-combined="onSvvCombined"
/>
    </main>
    
    <footer class="app-footer">
      &copy; 2024 RDF查询系统
    </footer>
  </div>
</template>

<script>
import RdfSearch from './components/RdfSearch.vue'
import TaskManager from './components/TaskManager.vue'
import TaskChainVisualizer from './components/TaskChainVisualizer.vue'
import VisualMappingEditor from './components/semanticVisual/VisualMappingEditor.vue'

export default {
  name: 'App',
  components: {
    RdfSearch,
    TaskManager,
    TaskChainVisualizer,
    VisualMappingEditor
  },
  data() {
    return {
      currentView: 'search', // 默认显示搜索视图
      selectedTask: null
    }
  },
  methods: {
  onTaskSelected(task) {
    console.log('选中任务:', task);
    this.selectedTask = task;
    // 自动切换到视觉映射编辑器
    this.currentView = 'visualMapping';
  },
  
  navigateToVisualMapping(task) {
    console.log('导航到视觉映射编辑器，任务:', task);
    // 设置选中的任务
    this.selectedTask = task;
    // 切换视图到视觉映射编辑器
    this.currentView = 'visualMapping';
    
    // 可选：将任务数据存储到localStorage作为备份
    localStorage.setItem('currentMappingTask', JSON.stringify(task));
    
    // 如果视觉映射编辑器组件已经加载，可以直接调用其方法
    this.$nextTick(() => {
      if (this.$refs.visualMappingEditor) {
        // 通知视觉映射编辑器组件任务已更新
        console.log('通知视觉映射编辑器更新任务');
      }
    });
  },
  
  onSvvCombined(combinedSvv) {
    console.log('收到组合的SVV:', combinedSvv);
    // 这里可以处理组合的SVV，例如保存到任务中
    // 也可以显示一个成功消息
    alert('SVV组合成功: ' + combinedSvv.name);
  }
}
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: Arial, sans-serif;
  background-color: #f8f9fa;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background-color: #2c3e50;
  color: white;
  padding: 15px 0;
  text-align: center;
}

.nav-tabs {
  margin-top: 15px;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}

.nav-tabs button {
  background-color: #34495e;
  color: white;
  border: none;
  padding: 10px 20px;
  margin: 0 5px 5px 5px;
  cursor: pointer;
  border-radius: 4px 4px 0 0;
  transition: background-color 0.3s;
}

.nav-tabs button:hover {
  background-color: #3c5a76;
}

.nav-tabs button.active {
  background-color: #1abc9c;
}

main {
  flex-grow: 1;
  padding: 20px;
}

.app-footer {
  background-color: #2c3e50;
  color: white;
  text-align: center;
  padding: 10px 0;
  margin-top: 20px;
}
</style>