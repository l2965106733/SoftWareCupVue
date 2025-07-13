<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getSystemOverviewApi,
  getUserActivityApi,
  getSystemUsageApi,
  getSystemHealthApi,
  getUserActivityTrendApi
} from '@/api/admin'
import dayjs from 'dayjs'

// 加载状态
const loading = ref({
  overview: false,
  activity: false,
  usage: false,
  health: false
})

// 系统概览数据
const systemOverview = ref({
  totalUsers: 0,
  totalStudents: 0,
  totalTeachers: 0,
  totalResources: 0,
  totalStorage: 0,
  systemUptime: 0,
  activeUsers: 0,
  newUsersToday: 0
})

// 用户活跃度数据
const userActivity = ref({
  todayActive: 0,
  todayNewUsers: 0,
  avgSessionTime: 0,
  totalLogins: 0,
  uniqueLogins: 0
})

// 系统使用情况
const systemUsage = ref({
  cpuUsage: 0,
  memoryUsage: 0,
  diskUsage: 0,
  networkTraffic: 0,
  databaseConnections: 0,
  apiRequests: 0
})

// 系统健康状态
const systemHealth = ref({
  status: 'healthy', // healthy, warning, critical
  responseTime: 0,
  errorRate: 0,
  availability: 99.8
})

// 用户活跃度趋势数据
const userActivityTrend = ref([])

// 工具函数
const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatUptime = (seconds) => {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}天 ${hours}小时 ${minutes}分钟`
}

const getHealthColor = (status) => {
  const colors = {
    'healthy': '#67c23a',
    'warning': '#e6a23c',
    'critical': '#f56c6c'
  }
  return colors[status] || '#909399'
}

const getHealthText = (status) => {
  const texts = {
    'healthy': '正常',
    'warning': '警告',
    'critical': '严重'
  }
  return texts[status] || '未知'
}

// 数据加载函数
const loadSystemOverview = async () => {
  loading.value.overview = true
  try {
    const result = await getSystemOverviewApi()
    if (result.code === 1) {
      systemOverview.value = result.data
      console.log('系统概览数据加载成功:', systemOverview.value)
    } else {
      ElMessage.error(result.msg || '获取系统概览失败')
    }
  } catch (error) {
    console.error('获取系统概览失败：', error)
    ElMessage.error('获取系统概览失败')
  } finally {
    loading.value.overview = false
  }
}

const loadUserActivity = async () => {
  loading.value.activity = true
  try {
    const result = await getUserActivityApi()
    if (result.code === 1) {
      userActivity.value = {
        todayActive: result.data?.todayActive || 0,
        todayNewUsers: result.data?.todayNewUsers || 0,
        avgSessionTime: result.data?.avgSessionTime || 0,
        totalLogins: result.data?.totalLogins || 0,
        uniqueLogins: result.data?.uniqueLogins || 0
      }
      console.log('用户活跃度数据加载成功:', userActivity.value)
    } else {
      ElMessage.error(result.msg || '获取用户活跃度失败')
    }
  } catch (error) {
    console.error('获取用户活跃度失败：', error)
    ElMessage.error('获取用户活跃度失败')
  } finally {
    loading.value.activity = false
  }
}

const loadSystemUsage = async () => {
  loading.value.usage = true
  try {
    const result = await getSystemUsageApi()
    if (result.code === 1) {
      systemUsage.value = {
        cpuUsage: result.data?.cpuUsage || 0,
        memoryUsage: result.data?.memoryUsage || 0,
        diskUsage: result.data?.diskUsage || 0,
        networkTraffic: result.data?.networkTraffic || 0,
        databaseConnections: result.data?.databaseConnections || 0,
        apiRequests: result.data?.apiRequests || 0
      }
      console.log('系统使用情况加载成功:', systemUsage.value)
    } else {
      ElMessage.error(result.msg || '获取系统使用情况失败')
    }
  } catch (error) {
    console.error('获取系统使用情况失败：', error)
    ElMessage.error('获取系统使用情况失败')
  } finally {
    loading.value.usage = false
  }
}

const loadSystemHealth = async () => {
  loading.value.health = true
  try {
    const result = await getSystemHealthApi()
    if (result.code === 1) {
      systemHealth.value = {
        status: result.data?.status || 'healthy',
        responseTime: result.data?.responseTime || 0,
        errorRate: result.data?.errorRate || 0,
        availability: result.data?.availability || 99.8
      }
      console.log('系统健康状态加载成功:', systemHealth.value)
    } else {
      ElMessage.error(result.msg || '获取系统健康状态失败')
    }
  } catch (error) {
    console.error('获取系统健康状态失败：', error)
    ElMessage.error('获取系统健康状态失败')
  } finally {
    loading.value.health = false
  }
}

const loadUserActivityTrend = async () => {
  try {
    const result = await getUserActivityTrendApi({
      startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
      granularity: 'day'
    })
    if (result.code === 1) {
      userActivityTrend.value = result.data || []
      console.log('用户活跃度趋势数据加载成功:', userActivityTrend.value)
    } else {
      console.warn('获取用户活跃度趋势失败:', result.msg)
      userActivityTrend.value = []
    }
  } catch (error) {
    console.error('获取用户活跃度趋势失败：', error)
    userActivityTrend.value = []
  }
}

// 加载所有数据
const loadAllData = async () => {
  console.log('🚀 开始加载系统总体统计数据...')
  
  // 并行加载所有数据
  await Promise.all([
    loadSystemOverview(),
    loadUserActivity(),
    loadSystemUsage(),
    loadSystemHealth(),
    loadUserActivityTrend()
  ])
}

// 刷新数据
const refreshData = () => {
  loadAllData()
  ElMessage.success('数据已刷新')
}

// ECharts 配置
const userActivityTrendOption = computed(() => {
  // 确保数据存在且格式正确
  const trendData = userActivityTrend.value || []
  const dates = trendData.map(item => item?.date || '').filter(Boolean)
  const counts = trendData.map(item => item?.activeUserCount || 0)
  
  // 如果没有数据，提供默认数据
  if (dates.length === 0) {
    const defaultDates = []
    const defaultCounts = []
    for (let i = 6; i >= 0; i--) {
      defaultDates.push(dayjs().subtract(i, 'day').format('MM-DD'))
      defaultCounts.push(0)
    }
    return {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: defaultDates,
        boundaryGap: false
      },
      yAxis: { type: 'value', name: '活跃用户数' },
      series: [{
        name: '活跃用户数',
        type: 'line',
        data: defaultCounts,
        smooth: true,
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#409eff' }
      }]
    }
  }
  
  return {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: { type: 'value', name: '活跃用户数' },
    series: [{
      name: '活跃用户数',
      type: 'line',
      data: counts,
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#409eff' }
    }]
  }
})

// 初始化
onMounted(() => {
  loadAllData()
})
</script>

<template>
  <div class="overall-stats-container">
    <!-- 页面标题和刷新按钮 -->
    <div class="page-header">
      <h2>📊 系统总体统计</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData" :loading="loading.overview">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 系统概览卡片 -->
    <div class="overview-section">
      <div class="overview-cards" v-loading="loading.overview">
        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-info">
              <div class="card-title">总用户数</div>
              <div class="card-value">{{ systemOverview.totalUsers }}</div>
              <div class="card-change positive">+{{ systemOverview.newUsersToday }} 今日新增</div>
            </div>
            <div class="card-icon">
              <el-icon><User /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-info">
              <div class="card-title">活跃用户</div>
              <div class="card-value">{{ systemOverview.activeUsers }}</div>
              <div class="card-change positive">{{ Math.round((systemOverview.activeUsers / systemOverview.totalUsers) * 100) }}% 活跃率</div>
            </div>
            <div class="card-icon">
              <el-icon><ChatLineRound /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-info">
              <div class="card-title">总存储量</div>
              <div class="card-value">{{ formatBytes(systemOverview.totalStorage || 0) }}</div>
              <div class="card-change positive">{{ systemOverview.totalResources }} 个资源</div>
            </div>
            <div class="card-icon">
              <el-icon><Folder /></el-icon>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-card">
          <div class="card-content">
            <div class="card-info">
              <div class="card-title">系统运行</div>
              <div class="card-value">{{ formatUptime(systemOverview.systemUptime || 0) }}</div>
              <div class="card-change positive">稳定运行中</div>
            </div>
            <div class="card-icon">
              <el-icon><Monitor /></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 系统健康状态 -->
    <div class="health-section">
      <el-card shadow="never" class="health-card">
        <div class="card-header">
          <h4>
            <el-icon><Warning /></el-icon>
            系统健康状态
          </h4>
          <el-tag 
            :type="systemHealth.status === 'healthy' ? 'success' : systemHealth.status === 'warning' ? 'warning' : 'danger'"
            size="large"
          >
            {{ getHealthText(systemHealth.status) }}
          </el-tag>
        </div>
        
        <div class="health-metrics" v-loading="loading.health">
          <div class="metric-item">
            <div class="metric-label">响应时间</div>
            <div class="metric-value">{{ systemHealth.responseTime }}ms</div>
            <el-progress 
              :percentage="Math.min((systemHealth.responseTime / 1000) * 100, 100)" 
              :color="systemHealth.responseTime < 500 ? '#67c23a' : systemHealth.responseTime < 1000 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
          
          <div class="metric-item">
            <div class="metric-label">错误率</div>
            <div class="metric-value">{{ systemHealth.errorRate }}%</div>
            <el-progress 
              :percentage="systemHealth.errorRate" 
              :color="systemHealth.errorRate < 1 ? '#67c23a' : systemHealth.errorRate < 5 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
          
          <div class="metric-item">
            <div class="metric-label">可用性</div>
            <div class="metric-value">{{ systemHealth.availability }}%</div>
            <el-progress 
              :percentage="systemHealth.availability" 
              :color="systemHealth.availability > 99.9 ? '#67c23a' : systemHealth.availability > 99 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 系统资源使用情况 -->
    <div class="usage-section">
      <el-card shadow="never" class="usage-card" v-loading="loading.usage">
        <div class="card-header">
          <h4>
            <el-icon><Monitor /></el-icon>
            系统资源使用
          </h4>
        </div>
        
        <div class="usage-metrics">
          <div class="usage-item">
            <div class="usage-label">CPU使用率</div>
            <div class="usage-value">{{ systemUsage.cpuUsage }}%</div>
            <el-progress 
              :percentage="systemUsage.cpuUsage" 
              :color="systemUsage.cpuUsage < 60 ? '#67c23a' : systemUsage.cpuUsage < 80 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
          
          <div class="usage-item">
            <div class="usage-label">内存使用率</div>
            <div class="usage-value">{{ systemUsage.memoryUsage }}%</div>
            <el-progress 
              :percentage="systemUsage.memoryUsage" 
              :color="systemUsage.memoryUsage < 70 ? '#67c23a' : systemUsage.memoryUsage < 85 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
          
          <div class="usage-item">
            <div class="usage-label">磁盘使用率</div>
            <div class="usage-value">{{ systemUsage.diskUsage }}%</div>
            <el-progress 
              :percentage="systemUsage.diskUsage" 
              :color="systemUsage.diskUsage < 80 ? '#67c23a' : systemUsage.diskUsage < 90 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
          
          <div class="usage-item">
            <div class="usage-label">数据库连接</div>
            <div class="usage-value">{{ systemUsage.databaseConnections }}</div>
            <el-progress 
              :percentage="(systemUsage.databaseConnections / 100) * 100" 
              :color="systemUsage.databaseConnections < 50 ? '#67c23a' : systemUsage.databaseConnections < 80 ? '#e6a23c' : '#f56c6c'"
            />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 用户活跃度统计 -->
    <div class="activity-section">
      <el-card shadow="never" class="activity-card" v-loading="loading.activity">
        <div class="card-header">
          <h4>
            <el-icon><DataAnalysis /></el-icon>
            用户活跃度分析
          </h4>
        </div>
        
        <div class="activity-stats">
          <div class="activity-item">
            <div class="activity-label">今日活跃用户</div>
            <div class="activity-value">{{ userActivity.todayActive }}</div>
          </div>
          
          <div class="activity-item">
            <div class="activity-label">今日新增用户</div>
            <div class="activity-value">{{ userActivity.todayNewUsers }}</div>
          </div>
          
          <div class="activity-item">
            <div class="activity-label">平均会话时长</div>
            <div class="activity-value">{{ formatUptime(userActivity.avgSessionTime) }}</div>
          </div>
          
          <div class="activity-item">
            <div class="activity-label">总登录次数</div>
            <div class="activity-value">{{ userActivity.totalLogins }}</div>
          </div>
          
          <div class="activity-item">
            <div class="activity-label">独立登录用户</div>
            <div class="activity-value">{{ userActivity.uniqueLogins }}</div>
          </div>
        </div>
        
        <!-- 用户活跃度趋势图 -->
        <div class="trend-chart-container">
          <div class="chart-title">用户活跃度趋势（近7天）</div>
          <vue-echarts :option="userActivityTrendOption" style="height: 300px;" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.overall-stats-container {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  color: white;
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 概览卡片 */
.overview-section {
  margin-bottom: 24px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.overview-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.overview-card :deep(.el-card__body) {
  padding: 20px;
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.card-value {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.card-change {
  font-size: 12px;
}

.card-change.positive {
  color: #67c23a;
}

.card-icon {
  font-size: 40px;
  color: #409eff;
  opacity: 0.3;
}

/* 系统健康状态 */
.health-section {
  margin-bottom: 24px;
}

.health-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.metric-item {
  text-align: center;
}

.metric-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.alerts-section h5 {
  margin-bottom: 12px;
  color: #2c3e50;
  font-size: 14px;
  font-weight: 600;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
}

.alert-item.warning {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.alert-item.critical {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.alert-time {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.7;
}

/* 系统资源使用 */
.usage-section {
  margin-bottom: 24px;
}

.usage-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.usage-metrics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.usage-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.usage-label {
  width: 100px;
  font-size: 14px;
  color: #666;
}

.usage-value {
  width: 80px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  text-align: right;
}



/* 用户活跃度 */
.activity-section {
  margin-bottom: 24px;
}

.activity-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.activity-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.activity-item {
  text-align: center;
}

.activity-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.activity-value {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

/* 趋势图样式 */
.trend-chart-container {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  text-align: center;
}



/* 响应式设计 */
@media (max-width: 1400px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .health-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
  

}

@media (max-width: 768px) {
  .overall-stats-container {
    padding: 16px;
  }
  
  .overview-cards {
    grid-template-columns: 1fr;
  }
  
  .health-metrics {
    grid-template-columns: 1fr;
  }
  
  .activity-stats {
    grid-template-columns: 1fr;
  }
  

  
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
}

/* 通用样式 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
}

.el-card :deep(.el-card__body) {
  padding: 24px;
}

.el-progress {
  flex: 1;
}
</style>