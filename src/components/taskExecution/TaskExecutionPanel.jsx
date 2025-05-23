import React, { useState, useEffect } from 'react';
import { Card, Steps, Button, message, Spin } from 'antd';
import taskExecutionService from '../../services/taskExecutionService';
import visualMappings from '../../config/visualMappings';

// 动态导入步骤组件
import SearchParameters from './steps/SearchParameters';
import QueryExecution from './steps/QueryExecution';
import ResultsVisualization from './steps/ResultsVisualization';
import AreaSelection from './steps/AreaSelection';
import TimeRangeSelection from './steps/TimeRangeSelection';
import RiskAnalysisExecution from './steps/RiskAnalysisExecution';
import ReportGeneration from './steps/ReportGeneration';
import GenericExecution from './steps/GenericExecution';

const { Step } = Steps;

// 组件映射表
const ComponentMapping = {
  SearchParameters,
  QueryExecution,
  ResultsVisualization,
  AreaSelection,
  TimeRangeSelection,
  RiskAnalysisExecution,
  ReportGeneration,
  GenericExecution
};

const TaskExecutionPanel = ({ taskId }) => {
  const [taskState, setTaskState] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initializeTask = async () => {
      try {
        setLoading(true);
        const state = await taskExecutionService.startTask(taskId);
        setTaskState(state);
        setError(null);
      } catch (err) {
        setError(err.message || '初始化任务失败');
        message.error('启动任务失败：' + (err.message || '未知错误'));
      } finally {
        setLoading(false);
      }
    };

    if (taskId) {
      initializeTask();
    }
  }, [taskId]);

  const handleNext = async () => {
    try {
      const state = await taskExecutionService.nextStep();
      setTaskState(state);
      
      if (state.status === 'completed') {
        message.success('任务执行完成！');
      }
    } catch (err) {
      setError(err.message || '执行下一步失败');
      message.error('无法进入下一步：' + (err.message || '未知错误'));
    }
  };

  const handlePrevious = async () => {
    try {
      const state = await taskExecutionService.previousStep();
      setTaskState(state);
    } catch (err) {
      message.warning(err.message || '无法返回上一步');
    }
  };

  if (loading) {
    return <Spin tip="正在加载任务..." />;
  }

  if (error) {
    return <div className="error-message">错误: {error}</div>;
  }

  if (!taskState) {
    return <div>无任务数据</div>;
  }

  const { currentStep, steps, status } = taskState;
  const CurrentStepComponent = ComponentMapping[steps[currentStep]?.component];

  return (
    <Card title="任务执行" className="task-execution-panel">
      <Steps current={currentStep}>
        {steps.map(step => (
          <Step key={step.id} title={step.name} />
        ))}
      </Steps>

      <div className="step-content" style={{ margin: '24px 0' }}>
        {CurrentStepComponent ? (
          <CurrentStepComponent
            taskState={taskState}
            onChange={updatedState => setTaskState({ ...taskState, ...updatedState })}
          />
        ) : (
          <div>未找到对应的步骤组件</div>
        )}
      </div>

      <div className="steps-action">
        {currentStep > 0 && status === 'running' && (
          <Button style={{ marginRight: 8 }} onClick={handlePrevious}>
            上一步
          </Button>
        )}
        {status === 'running' && currentStep < steps.length - 1 && (
          <Button type="primary" onClick={handleNext}>
            下一步
          </Button>
        )}
        {status === 'running' && currentStep === steps.length - 1 && (
          <Button type="primary" onClick={handleNext}>
            完成
          </Button>
        )}
        {status === 'completed' && (
          <Button type="primary" onClick={() => window.location.reload()}>
            重新开始
          </Button>
        )}
      </div>
    </Card>
  );
};

export default TaskExecutionPanel;