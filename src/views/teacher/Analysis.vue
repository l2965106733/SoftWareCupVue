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
                <div class="card-value"><el-icon><User /></el-icon>  {{ overviewData.totalStudents }}</div>
              </div>
    
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">作业完成率</div>
                <div class="card-value"><el-icon><Document /></el-icon>  {{ overviewData.homeworkRate }}%</div>
              </div>
              
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">平均得分率</div>
                <div class="card-value"><el-icon><TrendCharts /></el-icon>  {{ overviewData.avgScore * 100}}%</div>
              </div>
      
            </div>
          </el-card>

          <el-card shadow="hover" class="overview-card" v-loading="loading.overview">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">学生活跃度</div>
                <div class="card-value"><el-icon><ChatLineRound /></el-icon>  {{ overviewData.activeRate }}%</div>
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
              <!-- <div class="stat-value">{{ resourceStats.viewCount }}</div> -->
              <div class="stat-value">1</div>
              <div class="stat-label">总浏览量</div>
            </div>
            
            <div class="stat-item">
              <!-- <div class="stat-value">{{ resourceStats.downloadCount }}</div> -->
              <div class="stat-value">1</div>
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
          
          <el-table   style="border-radius: 10px; margin-bottom: 10px;"
          :data="filteredStudentData" stripe border default-sort="{prop: 'avgScore', order: 'descending'}" @sort-change="handleSortChange">
            <el-table-column prop="name" label="姓名" width="125" sortable/>
            <el-table-column prop="studentId" label="学号" width="125" />

            <!-- <el-table-column label="学习进度" width="150" sortable prop="progress">
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
            </el-table-column> -->

            <el-table-column prop="viewCount" label="资源查看数" width="119" sortable :cell-class-name="() => 'table-cell'">
              <template #default="scope">
                <div class="view-count-cell">
                  <el-icon><View /></el-icon>
                  <span>{{ scope.row.viewCount || 0 }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="homeworkCount" label="完成作业" width="110" sortable :cell-class-name="() => 'table-cell'"/>
            <el-table-column prop="totalHomework" label="总作业数" width="120" />
            <el-table-column label="完成率" width="95" sortable :sort-by="(row) => (row.homeworkCount / row.totalHomework)" :cell-class-name="() => 'table-cell'">
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
            <!-- <el-table-column prop="lastActive" label="最后活跃" width="140" /> -->
            <el-table-column prop="questionCount" label="提问数" width="92" sortable>
              <template #default="scope">
                <div class="question-count-cell">
                  <el-icon><ChatLineSquare /></el-icon>
                  <span>{{ scope.row.questionCount || 0 }} </span>
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
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');
.analysis-layout {
    min-height: 100%;
    animation: page-fade-in 0.8s ease-out;
    background: rgba(255, 255, 255, 0.05);
    padding: clamp(24px, 4vw, 48px);
    border-radius: 24px;
    color: #fff;
}

@keyframes page-fade-in {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

.section-title, h4 {
    font-size: clamp(20px, 3vw, 24px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 clamp(16px, 3vw, 24px) 0;
    display: flex;
    align-items: center;
    gap: clamp(8px, 2vw, 12px);
    animation: section-fade-in 0.8s ease-out;
}

@keyframes section-fade-in {
    0% { opacity: 0; transform: translateX(-20px); }
    100% { opacity: 1; transform: translateX(0); }
}

.card, .el-card, .overview-cards > div, .stats-grid > div, .resource-item, .question-item {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);
    padding: clamp(20px, 4vw, 32px);
    transition: all 0.3s ease;
    color: #fff;
}

.card:hover, .el-card:hover, .overview-cards > div:hover, .stats-grid > div:hover, .resource-item:hover, .question-item:hover {
    background: rgba(255, 255, 255, 0.15);
    box-shadow: 0 16px 48px rgba(0,0,0,0.18);
}

.table-cell {
  color: black; /* Element Plus 蓝色 */
  font-weight: 500;
}

.el-button, .btn, button {
    background: rgba(255,255,255,0.2) !important;
    border: 1px solid rgba(255,255,255,0.3) !important;
    border-radius: 12px !important;
    color: #fff !important;
    font-size: 14px !important;
    font-weight: 500;
    padding: 12px 24px !important;
    transition: all 0.3s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}
.el-button:hover, .btn:hover, button:hover {
    background: rgba(255,255,255,0.3) !important;
    transform: translateY(-2px);
}

.icon, .fa, .fas, .far, .fal, .fab {
    color: #fff !important;
    font-size: 20px !important;
}

.stats-grid, .overview-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: clamp(16px, 3vw, 20px);
}

.resource-list, .question-list {
    background: rgba(255,255,255,0.1);
    border-radius: 20px;
    padding: clamp(16px, 3vw, 24px);
    color: #fff;
}
.table-section {
  /* display: flex; */
  background: rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: clamp(16px, 3vw, 24px);
  color: #fff;
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
  background: rgba(255,255,255,0.08) !important;
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
  background: rgba(255,255,255,0.12) !important;
  border-radius: 20px !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10) !important;
  color: #fff !important;
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
  font-size:17px;
  color: #e0e0e0;
  margin-bottom: 8px;
}

.card-value {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.card-change {
  font-size: 16px;
}

.card-change.positive {
  color: #67c23a;
}

.card-change.negative {
  color: #f56c6c;
}

.card-icon {
  font-size: 40px;
  color: #fff;
  opacity: 0.3;
}

/* 分析区块 */
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
  color: #fff;
}

/* 成绩分布条 */
.score-distribution {
  margin-top: 20px;
  margin-bottom: 24px;
}

.score-distribution h5 {
  margin-bottom: 16px;
  color: #fff;
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
  color: #e0e0e0;
  text-align: right;
}


.range-count {
  width: 50px;
  font-size: 14px;
  color: #e0e0e0;
  text-align: center;
}

.recent-homework h5 {
  margin-bottom: 16px;
  color: #fff;
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
  background: rgba(255,255,255,0.08);
  border-radius: 8px;
  transition: all 0.3s ease;
  color: #fff;
}

.homework-item:hover {
  background: rgba(255,255,255,0.15);
}

.homework-title {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.homework-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #e0e0e0;
}

.empty-placeholder {
  text-align: center;
  padding: 20px;
  background: rgba(255,255,255,0.08);
  border-radius: 8px;
  border: 1px dashed #e0e0e0;
  color: #fff;
}

.el-table th,
.el-table td {
  padding: 12px 16px !important; 
  white-space: nowrap;
}

.el-table .cell {
  overflow: hidden;
  text-overflow: ellipsis;
} 

.resource-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #fff; /* 玻璃背景下字体亮色更清晰 */
}

.name-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: black;
}

.download-count {
  color: #67c23a;
  font-weight: 600;
}







/* 学生详细数据表格样式 */
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: #e0e0e0;
  font-weight: 500;
  min-width: 40px;
}

.view-count-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: gray;
}

.question-count-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: gray;
}

.completion-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: black;
}

.completion-text {
  font-size: 12px;
  color: gray;
  font-weight: 500;
  min-width: 40px;
}

/* 成绩分布自定义进度条样式 */
.range-bar {
  flex: 1;
  height: 16px;
  background: #e6f9ed; /* 极淡青绿底色 */
  border-radius: 8px;
  overflow: hidden;
  margin: 0 8px;
  position: relative;
}
.range-fill {
  height: 100%;
  background: linear-gradient(90deg, #5ad8a6 0%, #36cfc9 100%); /* 青绿渐变 */
  border-radius: 8px 0 0 8px;
  transition: width 0.4s cubic-bezier(.4,0,.2,1);
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
  color: #fff;
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
  background: rgba(255,255,255,0.08);
  border-radius: 6px;
  border-left: 3px solid #409eff;
  color: #fff;
}

.resource-name,
.question-title {
  font-size: 14px;
  color: #fff;
  margin-bottom: 4px;
  font-weight: 500;
}

.resource-stats,
.question-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #e0e0e0;
}

/* 互动统计专区 */
.interact-stats-section {
  margin-top: 20px;
}

/* 表格区域 */
/* .table-section {
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
  color: #fff;
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
} */

/* 表格头部容器样式玻璃化 */
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* 标题文字样式 */
.table-header h4 {
  margin: 0;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 动作按钮区域 */
/* .table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
} */
.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
/* 分页区样式统一玻璃风 */
/* .table-pagination {
  margin-top: 20px;
  text-align: right;
} */
.table-pagination {
  margin-top: 20px;
  margin-left: 150px;
  text-align: right;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: inline-block;
}

/* 深度覆盖分页组件按钮 */
.table-pagination :deep(.el-pager li),
.table-pagination :deep(.el-pagination__sizes),
.table-pagination :deep(.el-pagination__jump),
.table-pagination :deep(button) {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 6px;
  color: #fff;
  margin: 2 2px;
  margin-right: 5px;
  transition: all 0.2s ease;
}

/* hover 效果更亮 */
.table-pagination :deep(.el-pager li:hover),
.table-pagination :deep(.el-pagination__sizes:hover),
.table-pagination :deep(.el-pagination__jump:hover),
.table-pagination :deep(button:hover) {
  background: rgba(255, 255, 255, 0.3);
}



/* 通用标题样式 */
h4 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .overview-cards {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 600px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
  .analysis-layout {
    padding: 12px;
    border-radius: 12px;
  }
}
</style> 