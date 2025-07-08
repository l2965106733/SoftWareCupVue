<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getHomeworkStatsApi,
  getTeacherOverviewApi,
  getStudentListApi,
  getResourceStatsApi,
  getInteractStatsApi,
  getStudentProgressApi,
  getScoreDistributionApi
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

// 图表选择器
const progressPeriod = ref('month')
const selectedSubject = ref('all')
const homeworkDateRange = ref([])

// 筛选器
const selectedClass = ref('all')
const timeRange = ref('month')
const scoreRange = ref([0, 100])
const showAnomalies = ref(false)

// 学生数据搜索和排序
const searchStudent = ref('')
const sortBy = ref('name')
const currentPage = ref(1)
const pageSize = ref(20)

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

  // 按成绩范围筛选
  filtered = filtered.filter(student => 
    student.avgScore >= scoreRange.value[0] && 
    student.avgScore <= scoreRange.value[1]
  )

  // 排序
  if (sortBy.value === 'score') {
    filtered = filtered.sort((a, b) => b.avgScore - a.avgScore)
  } else if (sortBy.value === 'progress') {
    filtered = filtered.sort((a, b) => b.progress - a.progress)
  } else {
    filtered = filtered.sort((a, b) => a.name.localeCompare(b.name))
  }

  return filtered
})

// 方法
const getProgressColor = (progress) => {
  if (progress >= 80) return '#67c23a'
  if (progress >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  return 'danger'
}

const formatTooltip = (val) => `${val}分`

// 计算成绩分布条形图宽度
const getDistributionWidth = (count) => {
  if (!homeworkStats.value.scoreDistribution) return 0
  const maxCount = Math.max(...Object.values(homeworkStats.value.scoreDistribution))
  return maxCount > 0 ? (count / maxCount) * 100 : 0
}

const applyFilters = () => {
  ElMessage.success('筛选条件已应用')
}

const viewStudentDetail = (student) => {
  ElMessage.info(`查看${student.name}的详细信息`)
}

const exportHomeworkData = () => {
  ElMessage.success('作业数据导出成功')
}

const exportStudentData = () => {
  ElMessage.success('学生数据导出成功')
}

const generateReport = (type) => {
  const reportTypes = {
    weekly: '周度学情报告',
    monthly: '月度分析报告',
    semester: '学期总结报告',
    custom: '自定义报告'
  }
  ElMessage.success(`开始生成${reportTypes[type]}`)
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
                <div class="card-title">平均分</div>
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
                <div class="card-title">活跃度</div>
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
              <div class="stat-value">{{ Math.round(homeworkStats.submitRate) }}%</div>
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
            <div class="homework-list">
              <div 
                v-for="homework in homeworkStats.recentHomework.slice(0, 5)" 
                :key="homework.id"
                class="homework-item"
              >
                <div class="homework-title">{{ homework.title }}</div>
                <div class="homework-stats">
                  <span>提交: {{ homework.submitted_count }}/{{ homework.total_students }}</span>
                  <span>已批改: {{ homework.graded_count }}</span>
                </div>
              </div>
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
              <div class="stat-label">平均评分</div>
            </div>
            
            <div class="stat-item">
              <div class="stat-value">{{ interactStats.avgResponseTime?.toFixed(1) || 0 }}h</div>
              <div class="stat-label">平均响应时间</div>
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
        <div class="charts-row">
          <!-- 学习进度趋势 -->
          <el-card shadow="hover" class="chart-card">
            <div class="chart-header">
              <h4>
                <el-icon><TrendCharts /></el-icon>
                学习进度趋势
              </h4>
              <el-radio-group v-model="progressPeriod" size="small">
                <el-radio-button value="week">本周</el-radio-button>
                <el-radio-button value="month">本月</el-radio-button>
                <el-radio-button value="semester">本学期</el-radio-button>
              </el-radio-group>
            </div>
            <div class="chart-container" id="progressChart">
              <div class="chart-placeholder">
                <el-icon><TrendCharts /></el-icon>
                <p>学习进度趋势图</p>
                <small>显示学生整体学习进度变化</small>
              </div>
            </div>
          </el-card>

          <!-- 知识点掌握情况 -->
          <el-card shadow="hover" class="chart-card">
            <div class="chart-header">
              <h4>
                <el-icon><PieChart /></el-icon>
                知识点掌握分布
              </h4>
              <el-select v-model="selectedSubject" size="small" style="width: 120px">
                <el-option label="全部" value="all" />
                <el-option label="Java" value="java" />
                <el-option label="Vue" value="vue" />
                <el-option label="数据库" value="database" />
              </el-select>
            </div>
            <div class="chart-container" id="knowledgeChart">
              <div class="chart-placeholder">
                <el-icon><PieChart /></el-icon>
                <p>知识点掌握分布图</p>
                <small>已掌握、部分掌握、未掌握</small>
              </div>
            </div>
          </el-card>
        </div>

        <div class="charts-row">
          <!-- 作业完成情况统计 -->
          <el-card shadow="hover" class="chart-card wide">
            <div class="chart-header">
              <h4>
                <el-icon><DataAnalysis /></el-icon>
                作业完成情况统计
              </h4>
              <div class="header-actions">
                <el-date-picker
                  v-model="homeworkDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="small"
                  style="width: 240px"
                />
                <el-button type="primary" size="small" @click="exportHomeworkData">
                  <el-icon><Download /></el-icon>
                  导出数据
                </el-button>
              </div>
            </div>
            <div class="chart-container large" id="homeworkChart">
              <div class="chart-placeholder">
                <el-icon><DataAnalysis /></el-icon>
                <p>作业完成情况柱状图</p>
                <small>按时间显示各次作业的完成情况</small>
              </div>
            </div>
          </el-card>
        </div>
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
              <el-select v-model="sortBy" size="small" style="width: 120px; margin-left: 12px">
                <el-option label="按姓名" value="name" />
                <el-option label="按成绩" value="score" />
                <el-option label="按进度" value="progress" />
              </el-select>
              <el-button type="success" size="small" @click="exportStudentData">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
            </div>
          </div>
          
          <el-table :data="filteredStudentData" stripe border>
            <el-table-column prop="name" label="学生姓名" width="120" />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column label="学习进度" width="150">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.progress" 
                  :color="getProgressColor(scope.row.progress)"
                  :stroke-width="8"
                />
              </template>
            </el-table-column>
            <el-table-column prop="avgScore" label="平均分" width="100">
              <template #default="scope">
                <el-tag :type="getScoreType(scope.row.avgScore)">
                  {{ scope.row.avgScore }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="homeworkCount" label="完成作业" width="100" />
            <el-table-column prop="totalHomework" label="总作业数" width="100" />
            <el-table-column label="完成率" width="100">
              <template #default="scope">
                {{ Math.round((scope.row.homeworkCount / scope.row.totalHomework) * 100) }}%
              </template>
            </el-table-column>
            <el-table-column prop="lastActive" label="最后活跃" width="140" />
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button type="primary" size="small" @click="viewStudentDetail(scope.row)">
                  详情
                </el-button>
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

    <!-- 右侧面板 -->
    <div class="side-panel">
      <!-- 快速筛选 -->
      <el-card shadow="hover">
        <h4>
          <el-icon><Filter /></el-icon>
          快速筛选
        </h4>
        <el-form label-width="70px">
          <el-form-item label="班级">
            <el-select v-model="selectedClass" size="small" style="width: 100%">
              <el-option label="全部班级" value="all" />
              <el-option label="计算机22-1" value="cs221" />
              <el-option label="计算机22-2" value="cs222" />
              <el-option label="软件22-1" value="se221" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="时间段">
            <el-select v-model="timeRange" size="small" style="width: 100%">
              <el-option label="最近一周" value="week" />
              <el-option label="最近一月" value="month" />
              <el-option label="本学期" value="semester" />
              <el-option label="自定义" value="custom" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="成绩范围">
            <el-slider
              v-model="scoreRange"
              range
              :min="0"
              :max="100"
              :format-tooltip="formatTooltip"
            />
          </el-form-item>
          
          <el-form-item label="只看异常">
            <el-switch v-model="showAnomalies" />
          </el-form-item>
        </el-form>
        
        <el-button type="primary" block @click="applyFilters">
          <el-icon><Search /></el-icon>
          应用筛选
        </el-button>
      </el-card>

      <!-- 数据报告 -->
      <el-card style="margin-top: 20px" shadow="hover">
        <h4>
          <el-icon><Document /></el-icon>
          数据报告
        </h4>
        <div class="report-list">
          <div class="report-item" @click="generateReport('weekly')">
            <el-icon><Calendar /></el-icon>
            <span>周度学情报告</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <div class="report-item" @click="generateReport('monthly')">
            <el-icon><DataAnalysis /></el-icon>
            <span>月度分析报告</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <div class="report-item" @click="generateReport('semester')">
            <el-icon><Reading /></el-icon>
            <span>学期总结报告</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <div class="report-item" @click="generateReport('custom')">
            <el-icon><Setting /></el-icon>
            <span>自定义报告</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.analysis-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.side-panel {
  width: 380px;
  flex-shrink: 0;
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

/* 图表区域 */
.charts-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.charts-row {
  display: flex;
  gap: 20px;
}

.chart-card {
  flex: 1;
}

.chart-card.wide {
  flex: 2;
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

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
  border: 2px dashed #e0e0e0;
}

.chart-container.large {
  height: 400px;
}

.chart-placeholder {
  text-align: center;
  color: #999;
}

.chart-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 12px;
  color: #ddd;
}

.chart-placeholder p {
  margin: 8px 0 4px 0;
  font-size: 16px;
  font-weight: 500;
}

.chart-placeholder small {
  font-size: 12px;
  color: #ccc;
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

/* 右侧面板样式 */
h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-form-item {
  margin-bottom: 16px;
}

.el-form-item :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

/* 报告列表 */
.report-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.report-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.report-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.report-item span {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.report-item .arrow {
  font-size: 12px;
  color: #999;
}



/* 响应式 */
@media (max-width: 1400px) {
  .analysis-layout {
    gap: 16px;
    padding: 16px;
  }
  
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .side-panel {
    width: 320px;
  }
}

@media (max-width: 1200px) {
  .analysis-layout {
    flex-direction: column;
  }
  
  .main-panel,
  .side-panel {
    width: 100%;
  }
  
  .charts-row {
    flex-direction: column;
  }
  
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