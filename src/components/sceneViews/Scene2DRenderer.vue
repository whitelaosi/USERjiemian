<template>
    <div class="graph-container">
      <div class="graph-controls">
        <button @click="resetView" class="btn-reset">重置视图</button>
        <button @click="toggleLayout" class="btn-layout">切换布局</button>
        <div class="zoom-controls">
          <button @click="zoomIn">+</button>
          <button @click="zoomOut">-</button>
        </div>
      </div>
      
      <!-- SVG画布 -->
      <svg 
        ref="svgCanvas"
        class="graph-svg"
        :width="canvasWidth" 
        :height="canvasHeight"
        @mousedown="startPan"
        @mousemove="pan"
        @mouseup="endPan"
        @wheel="zoom"
      >
        <!-- 定义箭头标记 -->
        <defs>
          <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                  refX="9" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#666" />
          </marker>
        </defs>
        
        <!-- 变换组 -->
        <g :transform="transformString">
          <!-- 边/关系线 -->
          <g class="edges">
            <line
              v-for="edge in graphData.edges"
              :key="edge.id"
              :x1="getNodePosition(edge.source).x"
              :y1="getNodePosition(edge.source).y"
              :x2="getNodePosition(edge.target).x"
              :y2="getNodePosition(edge.target).y"
              :class="['edge', getEdgeClass(edge)]"
              :stroke-width="getEdgeWidth(edge)"
              marker-end="url(#arrowhead)"
            />
          </g>
          
          <!-- 节点 -->
          <g class="nodes">
            <g
              v-for="node in graphData.nodes"
              :key="node.id"
              :transform="`translate(${node.x}, ${node.y})`"
              :class="['node', getNodeClass(node)]"
              @click="onNodeClick(node)"
              @mouseenter="onNodeHover(node)"
              @mouseleave="onNodeLeave(node)"
            >
              <!-- 节点圆形 -->
              <circle
                :r="getNodeRadius(node)"
                :fill="getNodeColor(node)"
                :stroke="getNodeStroke(node)"
                stroke-width="2"
              />
              
              <!-- 节点标签 -->
              <text
                :dy="getNodeRadius(node) + 15"
                text-anchor="middle"
                class="node-label"
                :class="{ 'highlighted': node.highlighted }"
              >
                {{ getNodeLabel(node) }}
              </text>
              
              <!-- 节点详细信息（悬停时显示） -->
              <g v-if="node.showDetails" class="node-details">
                <rect x="-50" y="-30" width="100" height="20" 
                      fill="rgba(0,0,0,0.8)" rx="3"/>
                <text x="0" :y="-15" text-anchor="middle" 
                      fill="white" font-size="12">
                  能量: {{ node.properties?.energyRelease || 'N/A' }}
                </text>
              </g>
            </g>
          </g>
        </g>
      </svg>
      
      <!-- 推荐面板 -->
      <div v-if="selectedNode && recommendations.length > 0" 
           class="recommendation-panel">
        <h3>基于距离分级的推荐</h3>
        <div class="recommendations">
          <div
            v-for="rec in recommendations"
            :key="rec.entity.id"
            :class="['rec-item', `level-${rec.distance_level.toLowerCase()}`]"
            @click="highlightRecommendation(rec)"
          >
            <div class="rec-header">
              <span class="rec-name">{{ rec.entity.name }}</span>
              <span class="rec-distance">{{ rec.distance.toFixed(1) }}m</span>
            </div>
            <div class="rec-level">{{ rec.distance_level }}</div>
            <div class="rec-score">关联度: {{ (rec.relevance_score * 100).toFixed(1) }}%</div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'Scene2DRenderer',
    props: {
      graphData: {
        type: Object,
        default: () => ({ nodes: [], edges: [] })
      }
    },
    data() {
      return {
        canvasWidth: 1200,
        canvasHeight: 800,
        transform: { x: 0, y: 0, scale: 1 },
        isPanning: false,
        lastPanPoint: { x: 0, y: 0 },
        selectedNode: null,
        recommendations: [],
        layout: 'force' // 'force', 'circular', 'hierarchical'
      }
    },
    computed: {
      transformString() {
        return `translate(${this.transform.x}, ${this.transform.y}) scale(${this.transform.scale})`
      }
    },
    methods: {
      // 节点点击处理
      async onNodeClick(node) {
        console.log('节点被点击:', node)
        this.selectedNode = node
        
        // 高亮选中节点
        this.highlightNode(node)
        
        // 调用推荐API
        await this.fetchRecommendations(node)
        
        // 发出事件给父组件
        this.$emit('node-selected', node)
      },
      
      // 获取推荐
      async fetchRecommendations(node) {
        try {
          const response = await fetch('/api/knowledge-graph/recommend', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              clickedEntityId: node.id,
              userId: 'anonymous'
            })
          })
          
          if (response.ok) {
            const data = await response.json()
            this.recommendations = data.recommendations || []
            
            // 在图上高亮推荐的节点
            this.highlightRecommendedNodes()
          }
        } catch (error) {
          console.error('获取推荐失败:', error)
        }
      },
      
      // 高亮推荐节点
      highlightRecommendedNodes() {
        // 重置所有节点状态
        this.graphData.nodes.forEach(node => {
          node.highlighted = false
          node.recommendationLevel = null
        })
        
        // 高亮推荐节点
        this.recommendations.forEach(rec => {
          const node = this.graphData.nodes.find(n => n.id === rec.entity.id)
          if (node) {
            node.highlighted = true
            node.recommendationLevel = rec.distance_level
          }
        })
      },
      
      // 节点样式方法
      getNodeRadius(node) {
        if (node.id === this.selectedNode?.id) return 12
        if (node.highlighted) return 10
        return 8
      },
      
      getNodeColor(node) {
        if (node.id === this.selectedNode?.id) return '#ff4444'
        
        if (node.recommendationLevel) {
          const colors = {
            'IMMEDIATE': '#ff0000',
            'NEARBY': '#ff6600', 
            'RELATED': '#ffaa00',
            'DISTANT': '#ffdd00',
            'REMOTE': '#888888'
          }
          return colors[node.recommendationLevel] || '#4CAF50'
        }
        
        // 根据风险类型着色
        const riskColors = {
          'green': '#4CAF50',
          'yellow': '#FFC107', 
          'red': '#F44336'
        }
        return riskColors[node.properties?.riskType] || '#2196F3'
      },
      
      getNodePosition(nodeId) {
        const node = this.graphData.nodes.find(n => n.id === nodeId)
        return node ? { x: node.x, y: node.y } : { x: 0, y: 0 }
      },
      
      getNodeLabel(node) {
        return node.name || node.label || `节点${node.id}`
      }
      
      // ... 其他方法继续
    }
  }
  </script>
  
  <style scoped>
  .graph-container {
    position: relative;
    width: 100%;
    height: 100vh;
    background: #f5f5f5;
  }
  
  .graph-svg {
    border: 1px solid #ddd;
    background: white;
    cursor: grab;
  }
  
  .graph-svg:active {
    cursor: grabbing;
  }
  
  .node {
    cursor: pointer;
    transition: all 0.2s ease;
  }
  
  .node:hover circle {
    stroke-width: 3px;
  }
  
  .edge {
    stroke: #666;
    stroke-opacity: 0.6;
  }
  
  .level-immediate { border-left: 4px solid #ff0000; }
  .level-nearby { border-left: 4px solid #ff6600; }
  .level-related { border-left: 4px solid #ffaa00; }
  .level-distant { border-left: 4px solid #ffdd00; }
  .level-remote { border-left: 4px solid #888888; }
  
  .recommendation-panel {
    position: absolute;
    top: 60px;
    right: 20px;
    width: 300px;
    background: white;
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 15px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    max-height: 400px;
    overflow-y: auto;
  }
  </style> 
