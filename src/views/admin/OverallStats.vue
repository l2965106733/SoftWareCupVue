<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getSystemOverviewApi,
  getUserActivityApi,
  getUserActivityTrendApi,
  getTopKnowledgeScoreApi,
  getKnowledgeDistributionApi
} from '@/api/admin'
import dayjs from 'dayjs'


const barOption = ref({})
const pieOption = ref({})
const pieTitle = ref('知识点掌握分布')

const loadBarChart = async () => {
  const res = await getTopKnowledgeScoreApi();
  const rawData = res.data || []

  const data = rawData.filter(item => item.knowledge)

  const colors = ['#5470C6', '#91CC75', '#EE6666']

barOption.value = {
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: '{b}: {c}%'
  },
  grid: {
    left: 10, // 纵坐标
    top: 10,
    bottom: 10,
    right: 10,
    containLabel: true
  },
  xAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%' }
  },
  yAxis: {
    type: 'category',
    data: data.map(item => item.knowledge),
    inverse: true,
    axisLabel: {
      overflow: 'ellipsis', 
      width: 150  
    }
  },
  series: [{
    type: 'bar',
    name: '得分率',
    data: data.map(item => item.scoreRate), 
    label: {
      show: true,
      position: 'right',
      formatter: '{c}%'
    },
    itemStyle: {
      color: function (params) {
        return colors[params.dataIndex % colors.length]
      }
    }
  }]
}
  if (data.length > 0) {
    loadPieChart(data[0].knowledge)
  }
}


const loadPieChart = async (knowledgeName) => {
  const res = await getKnowledgeDistributionApi({
    knowledgeName: knowledgeName
  })
  const dist = res.data // 格式：[{ label: '掌握好', count: 15 }, ...]

  pieTitle.value = `${knowledgeName} 知识点情况掌握分布`
  pieOption.value = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    legend: {
      bottom: 0,
      data: dist.map(item => item.label)
    },
    series: [{
      type: 'pie',
      radius: '60%',
      data: dist.map(item => ({
        name: item.label,
        value: item.count
      })),
      label: {
        formatter: '{b}: {c}人 ({d}%)'
      }
    }]
  }
}

const onBarClick = (params) => {
  if (params?.name) {
    loadPieChart(params.name)
  }
}


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
  return `${days}天 ${hours}小时`
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
  setTimeout(() => {
    loadBarChart()
  }, 100)
})
</script>

<template>
  <div class="overall-stats-container">
    <!-- 页面标题和刷新按钮 -->
    <div class="page-header">
      <h2> <i class="fas fa-chart-pie nav-icon"></i> 系统总体统计</h2>
      <!-- <div class="header-actions">
        <el-button type="primary" @click="refreshData" :loading="loading.overview">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button> -->
      <!-- </div> -->
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
              <div class="card-value" style="font-size: 22px;">{{ formatUptime(systemOverview.systemUptime || 0) }}</div>
              <div class="card-change positive">稳定运行中</div>
            </div>
            <div class="card-icon">
              <el-icon><Monitor /></el-icon>
            </div>
          </div>
        </el-card>
      </div>
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
            <div class="activity-value">{{ userActivity.uniqueLogins }}</div>
          </div>
          
          <div class="activity-item">
            <div class="activity-label">今日新增用户</div>
            <div class="activity-value">{{ userActivity.todayNewUsers }}</div>
          </div>
          
          
          <div class="activity-item">
            <div class="activity-label">总登录次数</div>
            <div class="activity-value">4</div>
          </div>
          

        </div>
        
        <!-- 用户活跃度趋势图 -->
        <div class="trend-chart-container">
          <div class="chart-title">用户活跃度趋势（近7天）</div>
          <vue-echarts :option="userActivityTrendOption" style="height: 300px;" />
        </div>
        


      </el-card>


      <el-card shadow="never" class="activity-card" v-loading="loading.activity">
        <div class="card-header">
          <h4>
            <el-icon><DataAnalysis /></el-icon>
            知识点掌握分析
          </h4>
        </div>
        <div class="charts-container">
          <!-- 条形图区域 -->
          <div class="chart-box">
            <div class="chart-title">知识点得分率 Top5</div>
            <vue-echarts :option="barOption" style="height: 300px;" @click="onBarClick"/>
          </div>

        </div>
        
        <div class="charts-container">
          <!-- 饼图区域 -->
          <div class="chart-box">
            <div class="chart-title">{{ pieTitle }}</div>
            <vue-echarts :option="pieOption" style="height: 300px"/>
          </div>
        </div>
      </el-card>


    </div>
  </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.overall-stats-container {
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  min-height: 100vh;
  animation: admin-page-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
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
  animation: admin-title-glow 3s ease-in-out infinite alternate;
}

@keyframes section-fade-in {
  0% { opacity: 0; transform: translateX(-20px); }
  100% { opacity: 1; transform: translateX(0); }
}

@keyframes card-slide-up {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes stat-fade-in {
  0% { opacity: 0; transform: scale(0.9); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes activities-fade-in {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

@keyframes chart-fade-in {
  0% { opacity: 0; transform: translateX(-20px); }
  100% { opacity: 1; transform: translateX(0); }
}

/* 概览卡片 */
.overview-section {
  margin-bottom: 24px;
  animation: section-fade-in 0.8s ease-out 0.2s both;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* 玻璃卡片核心 */
.overview-card {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  transition: transform 0.25s ease;
  animation: card-slide-up 0.8s ease-out;
}

@keyframes card-slide-up {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.overview-card:hover {
  transform: translateY(-4px);
}

.overview-card :deep(.el-card__body) {
  padding: 20px;
}

/* 布局 */
.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 信息文本区域 */
.card-info {
  flex: 1;
}

.card-title {
  font-size: 13px;
  color: #ffffffcc; /* 半透明白 */
  margin-bottom: 6px;
}

.card-value {
  font-size: 26px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
}

.card-change {
  font-size: 12px;
  color: #ffffffb0;
}

.card-change.positive {
  color: #67c23a;
  font-size: 18px;
}
.card-change.negative {
  color: #f56c6c;
}

/* 图标美化 */
.card-icon {
  font-size: 36px;
  color: #ffffff80;
  opacity: 0.8;
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 用户活跃度 */
.activity-section {
  margin-bottom: 24px;
  animation: section-fade-in 0.8s ease-out 0.4s both;
}

/* 卡片容器改为玻璃风 */
.activity-card {
  margin-top: 25px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  transition: transform 0.25s ease;
  color: #fff;
  animation: card-slide-up 0.8s ease-out 0.6s both;
}

.activity-card:hover {
  transform: translateY(-4px);
}

/* 统计项网格布局保持 */
.activity-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

/* 每项居中显示 */
.activity-item {
  text-align: center;
  animation: stat-fade-in 0.8s ease-out 0.8s both;
}

@keyframes stat-fade-in {
  0% { opacity: 0; transform: scale(0.9); }
  100% { opacity: 1; transform: scale(1); }
}

/* 标签文字改为浅白色 */
.activity-label {
  font-size: 14px;
  color: #ffffffcc;
  margin-bottom: 8px;
}

/* 数值更亮白字 */
.activity-value {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
}
.card-header {
  font-size: 20px;
  padding: 10px 15px;         /* 原可能是 12px */
  min-height: 60px;           /* 强制拉高容器高度 */
  display: flex;
  align-items: left;
  justify-content: space-between;
}

/* 趋势图样式 */
.trend-chart-container {
  margin-top: 20px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.12);  /* 更柔和透明 */
  backdrop-filter: blur(14px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  animation: activities-fade-in 0.8s ease-out 1s both;
}

@keyframes activities-fade-in {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  text-align: center;
}

.charts-container {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  animation: chart-fade-in 0.8s ease-out 1.2s both;
}

@keyframes chart-fade-in {
  0% { opacity: 0; transform: translateX(-20px); }
  100% { opacity: 1; transform: translateX(0); }
}

.chart-box {
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .overview-cards {
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

/* 表格行动画（如果有表格的话） */
.el-table :deep(.el-table__body tr) {
  animation: row-fade-in 0.6s ease-out;
}

@keyframes row-fade-in {
  0% { opacity: 0; transform: translateX(-10px); }
  100% { opacity: 1; transform: translateX(0); }
}
</style>