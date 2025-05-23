import React, { useState, useEffect } from 'react';
import { Form, Slider, DatePicker, Select, Button, Divider } from 'antd';
import visualMappings from '../../../config/visualMappings';
import taskService from '../../../services/taskService';

const { RangePicker } = DatePicker;
const { Option } = Select;

const SearchParameters = ({ taskState, onChange }) => {
  const [task, setTask] = useState(null);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTaskDetails = async () => {
      try {
        setLoading(true);
        const taskData = await taskService.getTaskById(taskState.taskId);
        setTask(taskData);
        
        // 预填表单
        if (taskData.parameters) {
          form.setFieldsValue(taskData.parameters);
        }
      } catch (error) {
        console.error('获取任务详情失败', error);
      } finally {
        setLoading(false);
      }
    };

    fetchTaskDetails();
  }, [taskState.taskId, form]);

  const handleValuesChange = (changedValues, allValues) => {
    // 更新任务参数
    if (task) {
      const updatedTask = {
        ...task,
        parameters: {
          ...task.parameters,
          ...allValues
        }
      };
      setTask(updatedTask);
    }
  };

  const handleSaveParameters = async () => {
    try {
      const values = await form.validateFields();
      
      if (task) {
        // 更新任务参数
        const updatedTask = {
          ...task,
          parameters: {
            ...task.parameters,
            ...values
          }
        };
        
        // 保存到服务器
        await taskService.updateTask(task.id, updatedTask);
        
        // 提示成功
        message.success('参数已保存');
      }
    } catch (error) {
      console.error('保存参数失败', error);
      message.error('保存参数失败: ' + (error.message || '未知错误'));
    }
  };

  if (loading || !task) {
    return <Spin tip="加载任务参数..." />;
  }

  // 获取当前任务类型的视觉映射配置
  const mappingConfig = visualMappings[task.type]?.parameterMappings || {};

  return (
    <div className="search-parameters">
      <h3>设置查询参数</h3>
      <Divider />
      
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
      >
        {Object.entries(mappingConfig).map(([paramKey, config]) => {
          const { component, props } = config;
          
          switch (component) {
            case 'Slider':
              return (
                <Form.Item 
                  key={paramKey} 
                  name={paramKey} 
                  label={props.label}
                >
                  <Slider
                    min={props.min}
                    max={props.max}
                    step={props.step}
                    defaultValue={props.defaultValue}
                  />
                </Form.Item>
              );
              
            case 'DateRangePicker':
              return (
                <Form.Item 
                  key={paramKey} 
                  name={paramKey} 
                  label={props.label}
                >
                  <RangePicker format={props.format} />
                </Form.Item>
              );
              
            case 'AreaDropdown':
              return (
                <Form.Item 
                  key={paramKey} 
                  name={paramKey} 
                  label={props.label}
                >
                  <Select placeholder="请选择区域">
                    <Option value="segment_1">区段1</Option>
                    <Option value="segment_2">区段2</Option>
                    <Option value="segment_3">区段3</Option>
                  </Select>
                </Form.Item>
              );
              
            default:
              return (
                <Form.Item 
                  key={paramKey} 
                  name={paramKey} 
                  label={paramKey}
                >
                  <Input placeholder={`请输入${paramKey}`} />
                </Form.Item>
              );
          }
        })}
        
        <Form.Item>
          <Button type="primary" onClick={handleSaveParameters}>
            保存参数
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

export default SearchParameters;