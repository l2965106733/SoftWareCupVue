<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getHomeworkListApi, 
  getHomeworkDetailApi, 
  saveHomeworkDraftApi, 
  submitHomeworkApi, 
  getHomeworkStatsApi 
} from '@/api/student'

// 过滤状态
const filterStatus = ref('all')

// 统计数据
const homeworkStats = ref({
  totalHomework: 0,
  completedHomework: 0,
  pendingHomework: 0,
  averageScore: 0,
  overallProgress: 0
})

// 作业列表
const homeworkList = ref([])

// 对话框
const showHomeworkDialog = ref(false)
const currentHomework = ref(null)
const currentAnswers = ref({}) // 存储当前作业的答案

// 获取当前登录学生ID
const getCurrentStudentId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 计算属性
const completionRate = computed(() => 
  homeworkStats.value.totalHomework > 0 
    ? Math.round((homeworkStats.value.completedHomework / homeworkStats.value.totalHomework) * 100) 
    : 0
)

const filteredHomework = computed(() => {
  if (filterStatus.value === 'all') return homeworkList.value
  return homeworkList.value.filter(hw => {
    switch (filterStatus.value) {
      case 'pending':
        return hw.status === 0  // 草稿状态
      case 'submitted':
        return hw.status === 1  // 已提交
      case 'graded':
        return hw.status === 2  // 已批改
      default:
        return true
    }
  })
})

// 状态相关方法
const getStatusType = (status) => {
  const types = {
    0: 'warning',   // 未提交(草稿)
    1: 'primary',   // 已提交
    2: 'success'    // 已批改
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '草稿',      // 未提交
    1: '已提交',    // 已提交
    2: '已批改'     // 已批改
  }
  return texts[status] || '未知'
}

const getTimeLeftClass = (deadline) => {
  const now = new Date()
  const deadlineDate = new Date(deadline)
  const timeDiff = deadlineDate - now
  
  if (timeDiff < 0) return 'overdue'
  if (timeDiff < 24 * 60 * 60 * 1000) return 'urgent' // 24小时内
  return 'normal'
}

const formatTimeLeft = (deadline) => {
  if (!deadline) return '未设置'
  
  const now = new Date()
  const deadlineDate = new Date(deadline)
  
  // 检查日期是否有效
  if (isNaN(deadlineDate.getTime())) return '无效日期'
  
  const timeDiff = deadlineDate - now
  
  if (timeDiff < 0) return '已逾期'
  
  const days = Math.floor(timeDiff / (24 * 60 * 60 * 1000))
  const hours = Math.floor((timeDiff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000))
  
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时`
  
  const minutes = Math.floor((timeDiff % (60 * 60 * 1000)) / (60 * 1000))
  return `${minutes}分钟`
}

const formatDeadline = (deadline) => {
  if (!deadline) return '未设置'
  
  const date = new Date(deadline)
  if (isNaN(date.getTime())) return '无效日期'
  
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getScoreClass = (score, totalScore) => {
  if (!score || !totalScore) return 'no-score'
  const percentage = (score / totalScore) * 100
  if (percentage >= 90) return 'excellent'
  if (percentage >= 80) return 'good'
  if (percentage >= 70) return 'average'
  return 'poor'
}

const getQuestionTypeColor = (type) => {
  const colors = {
    choice: 'primary',
    short: 'success', 
    code: 'warning'
  }
  return colors[type] || 'info'
}

const getQuestionTypeName = (type) => {
  const names = {
    choice: '选择题',
    short: '简答题',
    code: '编程题'
  }
  return names[type] || '未知'
}

const getAnswerPlaceholder = (type) => {
  const placeholders = {
    choice: '请输入选择的答案，如：A 或 B...',
    short: '请输入答案...',
    code: '请输入代码...'
  }
  return placeholders[type] || '请输入答案...'
}

// 加载作业列表
const loadHomeworkList = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getHomeworkListApi(studentId)
    if (result.code === 1 && result.data && Array.isArray(result.data)) {
      homeworkList.value = result.data.map(item => ({
        id: item.student_homework_id,               // 学生作业记录ID
        homeworkId: item.homework_id,               // 真正的作业ID
        title: item.homework_title || '未命名作业',
        description: item.remark || '暂无描述',
        deadline: item.homework_end_time,           // 截止时间
        status: item.status !== undefined ? item.status : 0,  // 默认为0（草稿状态）
        score: item.total_score || 0,                     // 学生得分
        totalScore: item.homework_total_score || 0, // 作业总分
        feedback: item.feedback || '',
        teacherName: item.teacherName || item.teacher_name || '未知教师',
        createdTime: item.created_time || item.homework_start_time, // 创建时间
        timeLeft: formatTimeLeft(item.homework_end_time) // 剩余时间计算
      }))
    } else {
      console.warn('作业数据格式不正确：', result)
      homeworkList.value = []
    }
  } catch (error) {
    console.error('加载作业列表失败：', error)
    ElMessage.error('加载作业列表失败，请刷新页面重试')
  }
}

// 加载作业统计
const loadHomeworkStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getHomeworkStatsApi(studentId)
    if (result.code === 1) {
      const data = result.data || {}
      
      // 从学生角度：已完成 = 已提交，待完成 = 未提交
      const totalHomework = data.totalHomework || 0
      const submittedHomework = data.submittedHomework || 0
      const gradedHomework = data.gradedHomework || 0
      
      homeworkStats.value = {
        totalHomework: totalHomework,
        completedHomework: submittedHomework,                 // 已完成 = 已提交作业数
        pendingHomework: Math.max(0, totalHomework - submittedHomework), // 待完成 = 总数 - 已提交
        averageScore: data.avgScore || 0,                     // 平均分
        overallProgress: totalHomework > 0 ? Math.round((submittedHomework / totalHomework) * 100) : 0 // 整体进度基于提交率
      }
      
      console.log('作业统计数据:', homeworkStats.value) // 调试日志
    }
  } catch (error) {
    console.error('加载作业统计失败：', error)
  }
}

// 开始作业
const startHomework = async (homework) => {
  try {
    const result = await getHomeworkDetailApi(homework.homeworkId)  // 使用真正的作业ID
    if (result.code === 1) {
      currentHomework.value = {
        ...homework,
        questions: result.data || []  // 直接使用返回的数组
      }
      
      // 初始化答案对象
      currentAnswers.value = {}
      currentHomework.value.questions.forEach(question => {
        currentAnswers.value[question.id] = question.studentAnswer || ''
      })
      
      showHomeworkDialog.value = true
    }
  } catch (error) {
    ElMessage.error('加载作业详情失败')
  }
}

// 查看作业（已提交/已批改）
const viewHomework = (homework) => {
  startHomework(homework)
}

// 保存草稿
const saveDraft = async () => {
  try {
    const studentId = getCurrentStudentId()
    const draftData = {
      id: currentHomework.value.id,                    // 学生作业记录ID
      homeworkId: currentHomework.value.homeworkId,    // 作业ID
      studentId: studentId,                            // 学生ID
      status: 0,                                       // 0:未提交(草稿状态)
      answers: currentAnswers.value                    // 学生答案
    }
    
    const result = await saveHomeworkDraftApi(draftData)
    if (result.code === 1) {
      ElMessage.success('草稿保存成功')
      showHomeworkDialog.value = false
      loadHomeworkList() // 重新加载列表
    } else {
      ElMessage.error(result.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存草稿失败')
  }
}

// 提交作业
const submitHomework = () => {
  // 检查是否所有题目都已作答
  const unanswered = currentHomework.value.questions.filter(q => 
    !currentAnswers.value[q.id]?.trim()
  )
  
  if (unanswered.length > 0) {
    ElMessageBox.confirm(`还有${unanswered.length}道题未作答，确定要提交吗？`, '确认提交', {
      confirmButtonText: '确定提交',
      cancelButtonText: '继续作答',
      type: 'warning',
    }).then(() => {
      doSubmit()
    }).catch(() => {
      // 取消提交
    })
  } else {
    doSubmit()
  }
}

const doSubmit = async () => {
  try {
    const studentId = getCurrentStudentId()
    const submitData = {
      id: currentHomework.value.id,                    // 学生作业记录ID
      homeworkId: currentHomework.value.homeworkId,    // 作业ID
      studentId: studentId,                            // 学生ID
      status: 1,                                       // 1:已提交
      answers: currentAnswers.value                    // 学生答案
    }
    
    const result = await submitHomeworkApi(submitData)
    if (result.code === 1) {
      ElMessage.success('作业提交成功，等待教师批改')
      showHomeworkDialog.value = false
      loadHomeworkList() // 重新加载列表
      loadHomeworkStats() // 重新加载统计
    } else {
      ElMessage.error(result.msg || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交作业失败')
  }
}

// 初始化
onMounted(() => {
  console.log('学生作业模块初始化')
  loadHomeworkList()
  loadHomeworkStats()
})
</script>

<template>
  <div class="student-homework-layout">
    <!-- 左侧主要区域 -->
    <div class="main-panel">
      <!-- 顶部状态概览 -->
      <div class="status-section">
        <div class="status-cards">
          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">总作业数</div>
                <div class="card-value">{{ homeworkStats.totalHomework }}</div>
                <div class="card-desc">本学期发布</div>
              </div>
              <div class="card-icon">
                <el-icon><Document /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">已完成</div>
                <div class="card-value">{{ homeworkStats.completedHomework }}</div>
                <div class="card-desc">完成率 {{ completionRate }}%</div>
              </div>
              <div class="card-icon">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">待完成</div>
                <div class="card-value">{{ homeworkStats.pendingHomework }}</div>
                <div class="card-desc">需要关注</div>
              </div>
              <div class="card-icon">
                <el-icon><Clock /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">平均得分率</div>
                <div class="card-value">{{ homeworkStats.averageScore.toFixed(2) }}</div>
                <div class="card-desc">学习进度</div>
              </div>
              <div class="card-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 筛选和搜索 -->
      <div class="filter-section">
        <el-card shadow="hover">
          <div class="filter-header">
            <h3>
              <el-icon><List /></el-icon>
              作业列表
            </h3>
            <div class="filter-controls">
              <el-radio-group v-model="filterStatus" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="pending">待完成</el-radio-button>
                <el-radio-button value="submitted">已提交</el-radio-button>
                <el-radio-button value="graded">已批改</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 作业列表 -->
      <div class="homework-section">
        <div v-if="filteredHomework.length === 0" class="empty-state">
          <el-empty description="暂无作业数据" />
        </div>
        
        <div v-else class="homework-list">
          <el-card 
            v-for="homework in filteredHomework" 
            :key="homework.id" 
            shadow="hover" 
            class="homework-item"
          >
            <div class="homework-header">
              <div class="homework-info">
                <h4>{{ homework.title }}</h4>
                <p class="homework-desc">作业备注：{{ homework.description }}</p>
                <div class="homework-meta">
                  <span class="teacher">发布教师：{{ homework.teacherName }}</span>
                  <span class="deadline">截止时间：{{ formatDeadline(homework.deadline) }}</span>
                  <span 
                    class="time-left" 
                    :class="getTimeLeftClass(homework.deadline)"
                  >
                    剩余：{{ homework.timeLeft }}
                  </span>
                </div>
              </div>
              
              <div class="homework-status">
                <el-tag 
                  :type="getStatusType(homework.status)" 
                  size="large"
                >
                  {{ getStatusText(homework.status) }}
                </el-tag>
                
                <div v-if="homework.status === 2" class="score-display">
                  <span 
                    class="score" 
                    :class="getScoreClass(homework.score, homework.totalScore)"
                  >
                    {{ homework.score }}/{{ homework.totalScore }}
                  </span>
                </div>
              </div>
            </div>
            
            <div class="homework-actions">
              <el-button 
                v-if="homework.status === 0" 
                type="primary" 
                @click="startHomework(homework)"
              >
                <el-icon><Edit /></el-icon>
                开始作业
              </el-button>
              
              <el-button 
                v-if="homework.status === 1 || homework.status === 2" 
                type="info" 
                @click="viewHomework(homework)"
              >
                <el-icon><View /></el-icon>
                查看详情
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 作业详情对话框 -->
    <el-dialog 
      v-model="showHomeworkDialog" 
      :title="currentHomework?.title" 
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentHomework" class="homework-detail">
        <div class="homework-info-panel">
          <p><strong>作业描述：</strong>{{ currentHomework.description }}</p>
          <p><strong>截止时间：</strong>{{ formatDeadline(currentHomework.deadline) }}</p>
          <p><strong>总分：</strong>{{ currentHomework.totalScore }}分</p>
          <p v-if="currentHomework.feedback"><strong>教师反馈：</strong>{{ currentHomework.feedback }}</p>
        </div>
        
        <div class="questions-container">
          <div 
            v-for="(question, index) in currentHomework.questions" 
            :key="question.id" 
            class="question-item"
          >
            <div class="question-header">
              <span class="question-number">第{{ index + 1 }}题</span>
              <el-tag 
                :type="getQuestionTypeColor(question.type)" 
                size="small"
              >
                {{ getQuestionTypeName(question.type) }}
              </el-tag>
              <span class="question-score">{{ question.score || question.totalScore }}分</span>
            </div>
            
            <div class="question-content">
              <p>{{ question.content }}</p>
            </div>
            
            <div class="answer-section">
              <div class="text-answer">
                <el-input
                  v-model="currentAnswers[question.id]"
                  type="textarea"
                  :rows="question.type === 'code' ? 8 : (question.type === 'choice' ? 2 : 4)"
                  :placeholder="getAnswerPlaceholder(question.type)"
                  :disabled="currentHomework.status === 1 || currentHomework.status === 2"
                />
              </div>
              

              
              <!-- 显示得分（已批改） -->
              <div v-if="currentHomework.status === 2 && question.score !== undefined" class="question-score-display">
                <div class="question-answer" style="margin-top: 8px;">
                <strong>标准答案：</strong>
                <div style="white-space: pre-wrap; color: #409eff;">{{ question.answer || '暂无' }}</div>
              </div>
              <div class="question-explain" style="margin-top: 8px;">
                <strong>解析：</strong>
                <div style="white-space: pre-wrap; color: #666;">{{ question.explain || '暂无' }}</div>
              </div>

                <span class="score-label">得分：</span>
                <span 
                  class="score-value" 
                  :class="getScoreClass(question.score, question.totalScore)"
                >
                  {{ question.score }}/{{ question.totalScore }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showHomeworkDialog = false">关闭</el-button>
          
          <el-button 
            v-if="currentHomework?.status === 0" 
            type="info" 
            @click="saveDraft"
          >
            保存草稿
          </el-button>
          
          <el-button 
            v-if="currentHomework?.status === 0" 
            type="primary" 
            @click="submitHomework"
          >
            提交作业
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-homework-layout {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.main-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 状态卡片 */
.status-section {
  margin-bottom: 24px;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.status-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
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

.card-desc {
  font-size: 12px;
  color: #999;
}

.card-icon {
  font-size: 32px;
  color: #409eff;
  opacity: 0.8;
}

/* 筛选区域 */
.filter-section .el-card {
  border-radius: 12px !important;
  border: none !important;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 作业列表 */
.homework-section {
  flex: 1;
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.homework-item {
  border-radius: 12px !important;
  border: none !important;
  transition: all 0.3s ease;
}

.homework-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15) !important;
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.homework-info {
  flex: 1;
}

.homework-info h4 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.homework-desc {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.homework-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.homework-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.score-display .score {
  font-size: 16px;
  font-weight: 600;
}

.score.excellent { color: #67c23a; }
.score.good { color: #409eff; }
.score.average { color: #e6a23c; }
.score.poor { color: #f56c6c; }

.time-left.urgent { color: #f56c6c; font-weight: 600; }
.time-left.overdue { color: #f56c6c; font-weight: 600; }
.time-left.normal { color: #409eff; }

.homework-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 作业详情对话框 */
.homework-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.homework-info-panel {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.homework-info-panel p {
  margin: 8px 0;
  line-height: 1.6;
}

.questions-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.question-item {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  background: #fafafa;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.question-number {
  font-weight: 600;
  color: #2c3e50;
}

.question-score {
  margin-left: auto;
  color: #666;
  font-size: 14px;
}

.question-content {
  margin-bottom: 16px;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}



.submitted-answer {
  margin-top: 16px;
  padding: 12px;
  background: #e8f4fd;
  border-radius: 6px;
}

.submitted-answer h5 {
  margin: 0 0 8px 0;
  color: #409eff;
}

.answer-content {
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
}

.question-score-display {
  margin-top: 12px;
  text-align: right;
}

.score-label {
  color: #666;
  font-size: 14px;
}

.score-value {
  font-size: 16px;
  font-weight: 600;
  margin-left: 8px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .student-homework-layout {
    padding: 16px;
  }
  
  .status-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filter-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .homework-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .homework-actions {
    justify-content: center;
  }
}
</style>
