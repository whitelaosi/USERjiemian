// D:\USERjiemian\rdf-search-app\src\models\taskTypes.js
export const taskTypes = [
    {
      id: "SearchTask",
      label: "搜索任务",
      icon: "search",
      properties: [
        { id: "keywords", label: "关键词", type: "text", required: true },
        { id: "startDate", label: "开始日期", type: "date" }
      ],
      queryTemplate: "SELECT ?s ?p ?o WHERE { ?s ?p ?o . ?s rdfs:label ?label . FILTER(contains(lcase(?label), '{{keywords}}')) }"
    },
    {
      id: "MonitoringTask",
      label: "监控任务",
      icon: "monitor",
      properties: [
        { id: "entity", label: "监控实体", type: "entity", required: true },
        { 
          id: "frequency", 
          label: "监控频率", 
          type: "select", 
          options: [
            { value: "hourly", label: "每小时" },
            { value: "daily", label: "每天" },
            { value: "weekly", label: "每周" }
          ],
          required: true 
        }
      ],
      queryTemplate: "SELECT ?change ?time WHERE { {{entity}} ?change ?value . ?change dcterms:date ?time . }"
    },
    {
      id: "RiskAnalysisTask",
      label: "风险分析任务",
      icon: "assessment",
      properties: [
        { id: "minLevel", label: "最小风险等级", type: "number", required: true },
        { 
          id: "category", 
          label: "风险类别", 
          type: "select", 
          options: [
            { value: "financial", label: "财务风险" },
            { value: "operational", label: "运营风险" },
            { value: "strategic", label: "战略风险" },
            { value: "compliance", label: "合规风险" }
          ]
        }
      ],
      queryTemplate: "SELECT ?risk ?level ?description WHERE { ?risk a risk:Risk . ?risk risk:level ?level . ?risk rdfs:label ?description . FILTER(?level >= {{minLevel}}) }"
    }
  ];
  
  // 获取任务类型信息的工具函数
  export function getTaskType(typeId) {
    return taskTypes.find(type => type.id === typeId);
  }
  
  export function getAllTaskTypes() {
    return taskTypes;
  }
  
  export function getTaskTypeProperties(typeId) {
    const type = getTaskType(typeId);
    return type ? type.properties : [];
  }
  
  export function getQueryTemplate(typeId) {
    const type = getTaskType(typeId);
    return type ? type.queryTemplate : "";
  }