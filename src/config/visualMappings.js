// 任务类型与视觉组件的映射配置
const visualMappings = {
    // 搜索事件任务
    SearchEvent: {
      // 定义任务参数与UI组件的映射
      parameterMappings: {
        energyThreshold: {
          component: 'Slider',
          props: {
            min: 0,
            max: 10000,
            step: 100,
            defaultValue: 1000,
            label: '能量阈值 (J)'
          }
        },
        timeRange: {
          component: 'DateRangePicker',
          props: {
            format: 'YYYY-MM-DD',
            label: '时间范围'
          }
        },
        location: {
          component: 'AreaSelector',
          props: {
            label: '位置选择'
          }
        }
      },
      // 定义结果展示方式
      resultVisualization: {
        primary: 'EventTable',
        secondary: ['EnergyDistributionChart', 'LocationMap']
      }
    },
    
    // 风险分析任务
    RiskAnalysis: {
      parameterMappings: {
        areaId: {
          component: 'AreaDropdown',
          props: {
            label: '隧道区段'
          }
        },
        analysisFactors: {
          component: 'CheckboxGroup',
          props: {
            options: [
              { label: '能量释放', value: 'energy' },
              { label: '事件频率', value: 'frequency' },
              { label: '地质特性', value: 'geology' }
            ],
            defaultValue: ['energy', 'frequency'],
            label: '分析因素'
          }
        }
      },
      resultVisualization: {
        primary: 'RiskHeatmap',
        secondary: ['RiskFactorChart', 'TimelineView']
      }
    },
    
    // 历史对比任务
    HistoricalComparison: {
      parameterMappings: {
        periodOne: {
          component: 'DateRangePicker',
          props: {
            label: '第一时段'
          }
        },
        periodTwo: {
          component: 'DateRangePicker',
          props: {
            label: '第二时段'
          }
        },
        comparisonMetric: {
          component: 'RadioGroup',
          props: {
            options: [
              { label: '事件数量', value: 'count' },
              { label: '能量总和', value: 'totalEnergy' },
              { label: '最大能量', value: 'maxEnergy' }
            ],
            defaultValue: 'count',
            label: '对比指标'
          }
        }
      },
      resultVisualization: {
        primary: 'ComparisonBarChart',
        secondary: ['ChangePercentageTable', 'TrendLineChart']
      }
    }
  };
  
  export default visualMappings;