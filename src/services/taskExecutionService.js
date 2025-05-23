import taskService from './taskService';

class TaskExecutionService {
  // 存储当前执行中的任务状态
  currentTaskState = {
    taskId: null,
    currentStep: 0,
    steps: [],
    results: null,
    status: 'idle' // idle, running, completed, error
  };

  // 开始执行任务
  async startTask(taskId) {
    try {
      // 1. 获取任务详情
      const task = await taskService.getTaskById(taskId);
      
      // 2. 解析任务步骤
      const steps = this.parseTaskSteps(task);
      
      // 3. 初始化任务状态
      this.currentTaskState = {
        taskId,
        currentStep: 0,
        steps,
        results: null,
        status: 'running'
      };
      
      // 4. 执行第一步
      return this.executeCurrentStep();
    } catch (error) {
      console.error('启动任务执行失败:', error);
      this.currentTaskState.status = 'error';
      throw error;
    }
  }

  // 解析任务步骤
  parseTaskSteps(task) {
    // 根据任务类型生成步骤
    // 这只是一个示例，实际需要根据你的任务类型进行定制
    switch(task.type) {
      case 'SearchEvent':
        return [
          { id: 'parameters', name: '设置参数', component: 'SearchParameters' },
          { id: 'execution', name: '执行查询', component: 'QueryExecution' },
          { id: 'results', name: '查看结果', component: 'ResultsVisualization' }
        ];
      case 'RiskAnalysis':
        return [
          { id: 'selectArea', name: '选择区域', component: 'AreaSelection' },
          { id: 'timeRange', name: '设置时间范围', component: 'TimeRangeSelection' },
          { id: 'analysis', name: '风险分析', component: 'RiskAnalysisExecution' },
          { id: 'report', name: '生成报告', component: 'ReportGeneration' }
        ];
      default:
        return [
          { id: 'default', name: '执行任务', component: 'GenericExecution' }
        ];
    }
  }

  // 执行当前步骤
  async executeCurrentStep() {
    const { taskId, currentStep, steps } = this.currentTaskState;
    
    if (currentStep >= steps.length) {
      this.currentTaskState.status = 'completed';
      return this.currentTaskState;
    }
    
    const step = steps[currentStep];
    
    try {
      // 这里可以根据步骤类型执行不同的逻辑
      // 例如，如果是最后一步，需要调用实际的执行API
      if (currentStep === steps.length - 1) {
        const results = await taskService.executeTask(taskId);
        this.currentTaskState.results = results;
      }
      
      return this.currentTaskState;
    } catch (error) {
      console.error(`执行步骤 ${step.id} 失败:`, error);
      this.currentTaskState.status = 'error';
      throw error;
    }
  }

  // 进入下一步
  nextStep() {
    if (this.currentTaskState.status !== 'running') {
      throw new Error('任务未处于运行状态');
    }
    
    this.currentTaskState.currentStep += 1;
    return this.executeCurrentStep();
  }

  // 返回上一步
  previousStep() {
    if (this.currentTaskState.status !== 'running' || this.currentTaskState.currentStep <= 0) {
      throw new Error('无法返回上一步');
    }
    
    this.currentTaskState.currentStep -= 1;
    return this.currentTaskState;
  }

  // 获取当前任务状态
  getTaskState() {
    return this.currentTaskState;
  }
}

export default new TaskExecutionService();