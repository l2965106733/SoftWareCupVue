<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getHomeworkStatsApi,
  getTeacherOverviewApi,
  getStudentListApi,
  getResourceStatsApi,
  getInteractStatsApi,
} from '@/api/teacher'

// 概览数据 - 从API获取真实数据
const overviewData = ref({
  totalStudents: 0,
  newStudentsWeek: 0,
  homeworkRate: 0,
  homeworkTrend: 0,
  avgScore: 0,
  scoreTrend: 0,
  activeRate: 0,
  activeIncrease: 0
})

// 加载状态
const loading = ref({
  overview: false,
  homework: false,
  students: false,
  resources: false,
  interact: false
})

// 作业统计数据
const homeworkStats = ref({
  totalHomework: 0,
  publishedHomework: 0,
  gradedHomework: 0,
  pendingGrade: 0,
  submitRate: 0,
  recentHomework: [],
  scoreDistribution: {}
})






// 学生数据搜索和排序
const searchStudent = ref('')
const sortBy = ref('name')
const currentPage = ref(1)
const pageSize = ref(20)

// const sortOrder = ref('descending') // 降序
const sortOrder = ref('ascending') // 升序

const handleSortChange = ({ prop, order }) => {
  sortBy.value = prop
  sortOrder.value = order
}

// 学生数据 - 从API获取
const studentData = ref([])

// 资源统计数据
const resourceStats = ref({
  totalResources: 0,
  weeklyUploads: 0,
  viewCount: 0,
  downloadCount: 0,
  popularResources: []
})

// 互动统计数据
const interactStats = ref({
  totalQuestions: 0,
  answeredQuestions: 0,
  pendingQuestions: 0,
  avgRating: 0,
  avgResponseTime: 0,
  recentQuestions: []
})

// 计算属性
const filteredStudentData = computed(() => {
  let filtered = studentData.value

  // 按姓名搜索
  if (searchStudent.value) {
    filtered = filtered.filter(student => 
      student.name.includes(searchStudent.value) ||
      student.studentId.includes(searchStudent.value)
    )
  }

  if (sortBy.value === 'viewCount') {
    filtered.sort((a, b) =>
      sortOrder.value === 'ascending'
        ? (a.viewCount || 0) - (b.viewCount || 0)
        : (b.viewCount || 0) - (a.viewCount || 0)
    )
  } else if (sortBy.value === 'progress') {
    filtered.sort((a, b) =>
      sortOrder.value === 'ascending'
        ? (a.progress || 0) - (b.progress || 0)
        : (b.progress || 0) - (a.progress || 0)
    )
  } else if (sortBy.value === 'questionCount') {
    filtered.sort((a, b) =>
      sortOrder.value === 'ascending'
        ? (a.questionCount || 0) - (b.questionCount || 0)
        : (b.questionCount || 0) - (a.questionCount || 0)
    )
  } else if (sortBy.value === 'name') {
    filtered.sort((a, b) =>
      sortOrder.value === 'ascending'
        ? (a.name || 0) - (b.name || 0)
        : (b.name || 0) - (a.name || 0)
    )
  } else {
    filtered.sort((a, b) =>
      sortOrder.value === 'ascending'
        ? (a.name || '').localeCompare(b.name || '')
        : (b.name || '').localeCompare(a.name || '')
    )
  }
  return filtered
})

// 方法
const getProgressColor = (progress) => {
  if (progress >= 80) return '#67c23a'
  if (progress >= 60) return '#e6a23c'
  return '#f56c6c'
}



// 计算成绩分布条形图宽度
const getDistributionWidth = (count) => {
  if (!homeworkStats.value.scoreDistribution) return 0
  const maxCount = Math.max(...Object.values(homeworkStats.value.scoreDistribution))
  return maxCount > 0 ? (count / maxCount) * 100 : 0
}



const handleSizeChange = (val) => {
  pageSize.value = val
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 获取当前登录教师ID
const getCurrentTeacherId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 加载概览数据
const loadOverviewData = async () => {
  loading.value.overview = true
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) {
      ElMessage.warning('请先登录')
      return
    }
    
    const result = await getTeacherOverviewApi(teacherId)
    if (result.code === 1) {
      overviewData.value = result.data
    } else {
      ElMessage.error(result.msg || '获取概览数据失败')
    }
  } catch (error) {
    console.error('获取概览数据失败:', error)
    ElMessage.error('获取概览数据失败')
  } finally {
    loading.value.overview = false
  }
}

// 获取作业统计数据
const loadHomeworkStats = async () => {
  loading.value.homework = true
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return
    
    const result = await getHomeworkStatsApi(teacherId)
    if (result.code === 1) {
      homeworkStats.value = result.data
      console.log('作业统计数据加载成功:', homeworkStats.value)
    } else {
      ElMessage.error(result.msg || '获取作业统计失败')
    }
  } catch (error) {
    console.error('获取作业统计失败:', error)
    ElMessage.error('获取作业统计失败')
  } finally {
    loading.value.homework = false
  }
}

// 加载学生列表数据
const loadStudentData = async () => {
  loading.value.students = true
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return
    
    const result = await getStudentListApi(teacherId)
    if (result.code === 1) {
      studentData.value = result.data || []
      console.log('学生数据加载成功:', studentData.value)
    } else {
      ElMessage.error(result.msg || '获取学生数据失败')
  }
  } catch (error) {
    console.error('获取学生数据失败:', error)
    ElMessage.error('获取学生数据失败')
  } finally {
    loading.value.students = false
  }
}

// 加载资源统计数据
const loadResourceStats = async () => {
  loading.value.resources = true
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return
    
    const result = await getResourceStatsApi(teacherId)
    if (result.code === 1) {
      resourceStats.value = result.data
      console.log('资源统计数据加载成功:', resourceStats.value)
    } else {
      ElMessage.error(result.msg || '获取资源统计失败')
    }
  } catch (error) {
    console.error('获取资源统计失败:', error)
    ElMessage.error('获取资源统计失败')
  } finally {
    loading.value.resources = false
  }
}

// 加载互动统计数据
const loadInteractStats = async () => {
  loading.value.interact = true
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return
    
    const result = await getInteractStatsApi(teacherId)
    if (result.code === 1) {
      interactStats.value = result.data
      console.log('互动统计数据加载成功:', interactStats.value)
    } else {
      ElMessage.error(result.msg || '获取互动统计失败')
    }
  } catch (error) {
    console.error('获取互动统计失败:', error)
    ElMessage.error('获取互动统计失败')
  } finally {
    loading.value.interact = false
  }
}



// 加载所有数据
const loadAllData = async () => {
  const teacherId = getCurrentTeacherId()
  if (!teacherId) {
    ElMessage.warning('请先登录')
    return
  }
  
  console.log('开始加载教师数据分析，教师ID:', teacherId)
  
  // 并行加载所有数据
  await Promise.all([
    loadOverviewData(),
    loadHomeworkStats(),
    loadStudentData(),
    loadResourceStats(),
    loadInteractStats()
  ])
}

// 初始化
onMounted(() => {
  // 加载所有统计数据
  loadAllData()
  
  // 这里可以初始化图表
  console.log('初始化数据分析图表')
})
</script>


<template>
  <div class="analysis-layout">
    <!-- 左侧主要区域 -->
    <div class="main-panel">
      <!-- 顶部概览卡片 -->
      <div class="overview-section">
        <div class="overview-cards">
          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">总学生数</div>
                <div class="card-value">{{ overviewData.totalStudents }}</div>
                <div class="card-change positive">+{{ overviewData.newStudentsWeek }} 本周新增</div>
              </div>
              <div class="card-icon">
                <el-icon><User /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">作业完成率</div>
                <div class="card-value">{{ overviewData.homeworkRate }}%</div>
                <div class="card-change" :class="overviewData.homeworkTrend > 0 ? 'positive' : 'negative'">
                  {{ overviewData.homeworkTrend > 0 ? '+' : '' }}{{ overviewData.homeworkTrend }}% 较上周
                </div>
              </div>
              <div class="card-icon">
                <el-icon><Document /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">平均得分率</div>
                <div class="card-value">{{ overviewData.avgScore }}</div>
                <div class="card-change" :class="overviewData.scoreTrend > 0 ? 'positive' : 'negative'">
                  {{ overviewData.scoreTrend > 0 ? '+' : '' }}{{ overviewData.scoreTrend }} 较上次
                </div>
              </div>
              <div class="card-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">学生活跃度</div>
                <div class="card-value">{{ overviewData.activeRate }}%</div>
                <div class="card-change positive">+{{ overviewData.activeIncrease }}% 本周</div>
              </div>
              <div class="card-icon">
                <el-icon><ChatLineRound /></el-icon>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 作业统计专区 -->
      <div class="homework-stats-section">
        <el-card shadow="hover" v-loading="loading.homework">
          <div class="card-header">
            <h4>
              <el-icon><DataAnalysis /></el-icon>
              作业统计概览
            </h4>
          </div>
          
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ homeworkStats.totalHomework }}</div>
              <div class="stat-label">总作业数</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ homeworkStats.publishedHomework }}</div>
              <div class="stat-label">已发布</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ homeworkStats.gradedHomework }}</div>
              <div class="stat-label">已批改</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ homeworkStats.pendingGrade }}</div>
              <div class="stat-label">待批改</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ (() => {
                let rate = homeworkStats.submitRate || 0;
                if (rate <= 1) rate = rate * 100;  // 小数转百分比
                if (rate > 100) rate = 100;  // 限制最大值
                if (rate < 0) rate = 0;  // 限制最小值
                return Math.round(rate);
              })() }}%</div>
              <div class="stat-label">平均提交率</div>
            </div>
          </div>

          <!-- 成绩分布 -->
          <div class="score-distribution">
            <h5>成绩分布</h5>
            <div class="distribution-bars">
              <div 
                v-for="(count, range) in homeworkStats.scoreDistribution" 
                :key="range"
                class="distribution-item"
              >
                <div class="range-label">{{ range }}</div>
                <div class="range-bar">
                  <div 
                    class="range-fill" 
                    :style="{ width: getDistributionWidth(count) + '%' }"
                  ></div>
                </div>
                <div class="range-count">{{ count }}人</div>
              </div>
            </div>
          </div>

          <!-- 最近作业 -->
          <div class="recent-homework">
            <h5>最近作业</h5>
            <div class="homework-list" v-if="homeworkStats.recentHomework && homeworkStats.recentHomework.length > 0">
              <div 
                v-for="homework in homeworkStats.recentHomework.slice(0, 5)" 
                :key="homework.id"
                class="homework-item"
              >
                <div class="homework-title">{{ homework.title }}</div>
                <div class="homework-stats">
                  <span>提交: {{ homework.submitted_count || homework.submittedCount || 0 }}/{{ homework.total_students || homework.totalStudents || 0 }}</span>
                  <span>已批改: {{ homework.graded_count || homework.gradedCount || 0 }}</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-placeholder">
              <span style="color: #999; font-size: 14px;">暂无最近作业数据</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 资源统计专区 -->
      <div class="resource-stats-section">
        <el-card shadow="hover" v-loading="loading.resources">
          <div class="card-header">
            <h4>
              <el-icon><Folder /></el-icon>
              资源统计概览
            </h4>
          </div>
          
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ resourceStats.totalResources }}</div>
              <div class="stat-label">总资源数</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ resourceStats.weeklyUploads }}</div>
              <div class="stat-label">本周上传</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ resourceStats.viewCount }}</div>
              <div class="stat-label">总浏览量</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ resourceStats.downloadCount }}</div>
              <div class="stat-label">总下载量</div>
            </div>
          </div>

          <!-- 热门资源 -->
          <div class="popular-resources" v-if="resourceStats.popularResources?.length > 0">
            <h5>热门资源</h5>
            <div class="resource-list">
              <div 
                v-for="resource in resourceStats.popularResources.slice(0, 5)" 
                :key="resource.id"
                class="resource-item"
              >
                <div class="resource-name">{{ resource.fileName }}</div>
                <div class="resource-stats">
                  <span>浏览: {{ resource.viewCount }}</span>
                  <span>下载: {{ resource.downloadCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 互动统计专区 -->
      <div class="interact-stats-section">
        <el-card shadow="hover" v-loading="loading.interact">
          <div class="card-header">
            <h4>
              <el-icon><ChatLineSquare /></el-icon>
              师生互动统计
            </h4>
          </div>
          
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.totalQuestions }}</div>
              <div class="stat-label">总提问数</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.answeredQuestions }}</div>
              <div class="stat-label">已回答</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.pendingQuestions }}</div>
              <div class="stat-label">待回答</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.avgRating?.toFixed(1) || 0 }}</div>
              <div class="stat-label">平均互动评分</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.avgResponseTime?.toFixed(1) || 0 }}h</div>
              <div class="stat-label">平均回答时间</div>
            </div>
          </div>

          <!-- 最近问题 -->
          <div class="recent-questions" v-if="interactStats.recentQuestions?.length > 0">
            <h5>最近问题</h5>
            <div class="question-list">
              <div 
                v-for="question in interactStats.recentQuestions.slice(0, 3)" 
                :key="question.id"
                class="question-item"
              >
                <div class="question-title">{{ question.title }}</div>
                <div class="question-info">
                  <span>{{ question.studentName }}</span>
                  <span>{{ question.createdTime }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 图表分析区域 -->
      <div class="charts-section">



      </div>

      <!-- 详细数据表格 -->
      <div class="table-section">
        <el-card shadow="hover" v-loading="loading.students">
          <div class="table-header">
            <h4>
              <el-icon><List /></el-icon>
              学生详细数据
            </h4>
            <div class="table-actions">
              <el-input
                v-model="searchStudent"
                placeholder="搜索学生姓名"
                prefix-icon="Search"
                size="small"
                style="width: 200px"
                clearable
              />
              <el-select v-model="sortBy" size="small" style="width: 140px; margin-left: 12px">
                <el-option label="按姓名" value="name" />
                <el-option label="按查看次数" value="viewCount" />
                <el-option label="按进度" value="progress" />
                <el-option label="按提问数" value="questionCount" />
              </el-select>
            </div>
          </div>
          
          <el-table :data="filteredStudentData" stripe border default-sort="{prop: 'avgScore', order: 'descending'}" @sort-change="handleSortChange">
            <el-table-column prop="name" label="学生姓名" width="120" sortable />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column label="学习进度" width="150" sortable prop="progress">
              <template #default="scope">
                <div class="progress-cell">
                <el-progress 
                  :percentage="scope.row.progress" 
                  :color="getProgressColor(scope.row.progress)"
                  :stroke-width="8"
                    :show-text="false"
                />
                  <span class="progress-text">{{ scope.row.progress }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="资源查看次数" width="120" sortable>
              <template #default="scope">
                <div class="view-count-cell">
                  <el-icon><View /></el-icon>
                  <span>{{ scope.row.viewCount || 0 }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="homeworkCount" label="完成作业" width="100" sortable />
            <el-table-column prop="totalHomework" label="总作业数" width="100" />
            <el-table-column label="完成率" width="120" sortable :sort-by="(row) => (row.homeworkCount / row.totalHomework)">
              <template #default="scope">
                <div class="completion-cell">
                  <el-progress 
                    :percentage="Math.round((scope.row.homeworkCount / scope.row.totalHomework) * 100)" 
                    :stroke-width="6"
                    :show-text="false"
                    :color="scope.row.homeworkCount / scope.row.totalHomework >= 0.8 ? '#67c23a' : scope.row.homeworkCount / scope.row.totalHomework >= 0.6 ? '#e6a23c' : '#f56c6c'"
                  />
                  <span class="completion-text">{{ Math.round((scope.row.homeworkCount / scope.row.totalHomework) * 100) }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="lastActive" label="最后活跃" width="140" />
            <el-table-column prop="questionCount" label="提问数" width="100" sortable>
              <template #default="scope">
                <div class="question-count-cell">
                  <el-icon><ChatLineSquare /></el-icon>
                  <span>{{ scope.row.questionCount || 0 }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="studentData.length"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    
  </div>
</template>

<style scoped>
.analysis-layout {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.main-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

/* 卡片样式 */
.el-card {
  border-radius: 12px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
  border: none !important;
}

.el-card :deep(.el-card__body) {
  padding: 24px;
}

/* 概览卡片 */
.overview-section {
  flex-shrink: 0;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.overview-card {
  background: white;
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

.card-change.negative {
  color: #f56c6c;
}

.card-icon {
  font-size: 40px;
  color: #409eff;
  opacity: 0.3;
}

/* 分析区域 */
.charts-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}





.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}





/* 作业统计专区 */
.homework-stats-section {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: #e9ecef;
  transform: translateY(-2px);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.score-distribution {
  margin-bottom: 24px;
}

.score-distribution h5 {
  margin-bottom: 16px;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.distribution-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.range-label {
  width: 100px;
  font-size: 14px;
  color: #333;
  text-align: right;
}

.range-bar {
  flex: 1;
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.range-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.range-count {
  width: 50px;
  font-size: 14px;
  color: #666;
  text-align: center;
}

.recent-homework h5 {
  margin-bottom: 16px;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.homework-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.homework-item:hover {
  background: #e9ecef;
}

.homework-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.homework-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #666;
}

.empty-placeholder {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #e0e0e0;
}





/* 学生详细数据表格样式 */
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  min-width: 40px;
}

.view-count-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
}

.question-count-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
}

.completion-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.completion-text {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  min-width: 40px;
}

/* 资源统计专区 */
.resource-stats-section {
  margin-top: 20px;
}

.popular-resources,
.recent-questions {
  margin-top: 20px;
}

.popular-resources h5,
.recent-questions h5 {
  font-size: 14px;
  color: #2c3e50;
  margin-bottom: 12px;
  font-weight: 600;
}

.resource-list,
.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-item,
.question-item {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.resource-name,
.question-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  font-weight: 500;
}

.resource-stats,
.question-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
}

/* 互动统计专区 */
.interact-stats-section {
  margin-top: 20px;
}

/* 表格区域 */
.table-section {
  flex: 1;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-pagination {
  margin-top: 20px;
  text-align: right;
}

/* 通用标题样式 */
h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}





/* 响应式 */
@media (max-width: 1400px) {
  .analysis-layout {
    padding: 16px;
  }
  
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .table-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .table-actions {
    flex-wrap: wrap;
  }
}
</style> 