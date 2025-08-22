<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getHomeworkListApi, 
  getHomeworkDetailApi, 
  saveHomeworkDraftApi, 
  submitHomeworkApi, 
  getHomeworkStatsApi,
  getAnalysisApi
} from '@/api/student'
import Analysis from '../teacher/Analysis.vue'

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

// 作业统计数据
const homeworkStatsData = ref([
  { label: '总作业数', value: '0', icon: 'fas fa-file-alt', color: '#667eea' },
  { label: '已完成', value: '0', icon: 'fas fa-check-circle', color: '#f5576c' },
  { label: '待完成', value: '0', icon: 'fas fa-clock', color: '#4facfe' },
  { label: '平均得分', value: '0', icon: 'fas fa-chart-line', color: '#26d0ce' }
])

// 筛选选项
const filterOptions = ref([
  { label: '全部', value: 'all', icon: 'fas fa-list' },
  { label: '待完成', value: 'pending', icon: 'fas fa-clock' },
  { label: '已提交', value: 'submitted', icon: 'fas fa-paper-plane' },
  { label: '已批改', value: 'graded', icon: 'fas fa-check-double' }
])

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

const getStatusClass = (status) => {
  const classes = {
    0: 'status-warning',   // 未提交(草稿)
    1: 'status-primary',   // 已提交
    2: 'status-success'    // 已批改
  }
  return classes[status] || 'status-info'
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
      
      // 更新作业统计数据
      homeworkStatsData.value = [
        { label: '总作业数', value: homeworkStats.value.totalHomework.toString(), icon: 'fas fa-file-alt', color: '#667eea' },
        { label: '已完成', value: homeworkStats.value.completedHomework.toString(), icon: 'fas fa-check-circle', color: '#f5576c' },
        { label: '待完成', value: homeworkStats.value.pendingHomework.toString(), icon: 'fas fa-clock', color: '#4facfe' },
        { label: '平均得分率', value: (homeworkStats.value.averageScore.toFixed(2)*100).toString() + "%", icon: 'fas fa-chart-line', color: '#26d0ce' }
      ]
      
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

const analysisMap = ref({})

const fetchQuestionAnalysis = async (question) => {
  try {
    const res = await getAnalysisApi({
      content: question.content,
      answer: question.answer,
      knowledge: question.knowledge
    })

    analysisMap.value[question.id] = res.data || '暂无分析结果'
  } catch (e) {
    analysisMap.value[question.id] = '诊断失败'
  }
}

</script>

<template>
  <div class="student-container">
    <!-- 页面标题 -->
    <div class="student-section">
      <h1 class="student-title large">
        <i class="fas fa-tasks"></i>
        作业模块
      </h1>
      <p class="student-text secondary">完成课程作业，提升学习效果</p>
    </div>

    <!-- 作业统计区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-chart-bar"></i>
        作业统计
      </h2>
      <div class="student-grid three-columns">
        <div class="student-card stat-card" v-for="(stat, index) in homeworkStatsData" :key="index">
          <div class="stat-icon" :style="{ color: stat.color }">
            <i :class="stat.icon"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="student-section">
      <div class="student-card filter-card">
        <div class="filter-header">
          <h3 class="student-title small">
            <i class="fas fa-filter"></i>
            作业筛选
          </h3>
          <div class="filter-controls">
            <button 
              v-for="filter in filterOptions" 
              :key="filter.value"
              class="filter-btn"
              :class="{ active: filterStatus === filter.value }"
              @click="filterStatus = filter.value"
            >
              <i :class="filter.icon"></i>
              {{ filter.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 作业列表区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-list"></i>
        作业列表
      </h2>
      
      <div v-if="filteredHomework.length === 0" class="student-card empty-card">
        <div class="empty-content">
          <i class="fas fa-inbox"></i>
          <p class="student-text secondary">暂无作业数据</p>
        </div>
      </div>
      
      <div v-else class="student-grid auto-fit">
        <div 
          v-for="homework in filteredHomework" 
          :key="homework.id" 
          class="student-card homework-card"
        >
          <div class="homework-header">
            <div class="homework-title">{{ homework.title }}</div>
            <div class="homework-status">
              <span class="status-badge" :class="getStatusClass(homework.status)">
                {{ getStatusText(homework.status) }}
              </span>
            </div>
          </div>
          
          <div class="homework-content">
            <p class="homework-desc">{{ homework.description }}</p>
            <div class="homework-meta">
              <div class="meta-item">
                <i class="fas fa-user"></i>
                <span>{{ homework.teacherName }}</span>
              </div>
              <div class="meta-item">
                <i class="fas fa-calendar"></i>
                <span>{{ formatDeadline(homework.deadline) }}</span>
              </div>
              <div class="meta-item">
                <i class="fas fa-clock"></i>
                <span :class="getTimeLeftClass(homework.deadline)">
                  {{ homework.timeLeft }}
                </span>
              </div>
            </div>
            
            <div v-if="homework.status === 2" class="score-section">
              <div class="score-display">
                <span class="score-label">得分:</span>
                <span class="score-value" :class="getScoreClass(homework.score, homework.totalScore)">
                  {{ homework.score }}/{{ homework.totalScore }}
                </span>
                <span v-if="homework.totalScore > 0" class="score-rate" :class="getScoreClass(homework.score, homework.totalScore)">
                  得分率：{{ Math.round((homework.score / homework.totalScore) * 100) }}%
                </span>
              </div>
            </div>
            <div v-else-if="homework.status === 1" class="score-section">
              <div class="score-display">
                <span class="score-label">状态:</span>
                <span class="score-value">暂未批改</span>
              </div>
            </div>
            <div v-else-if="homework.status === 0" class="score-section">
              <div class="score-display">
                <span class="score-label">状态:</span>
                <span class="score-value">暂未提交</span>
              </div>
            </div>
          </div>
          
          <div class="homework-actions">
            <button 
              v-if="homework.status === 0" 
              class="student-button"
              @click="startHomework(homework)"
              :disabled="dayjs().isAfter(dayjs(homework.homework_end_time))"
            >
              <i class="fas fa-edit"></i>
              开始作业
            </button>
            
            <button 
              v-if="homework.status === 1 || homework.status === 2" 
              class="student-button secondary"
              @click="viewHomework(homework)"
            >
              <i class="fas fa-eye"></i>
              查看详情
            </button>
          </div>
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
              
    

              <div v-if="currentHomework.status === 2 && question.score !== undefined" class="question-score-display">
                
                <!-- <div v-if="!analysisMap[question.id]">
                  <span :data-id="question.id" v-once>
                    {{ fetchQuestionAnalysis(question) }}
                  </span>
                </div>   需要吗？？-->
                
                <!-- <div class="question-section question-explain">
                  <strong>错误诊断：</strong>
                  <div>{{ analysisMap[question.id] || '诊断中...' }}</div>
                </div> -->
                
                <div class="question-section question-answer">
                <strong>标准答案：</strong>
                <div style="color: #409eff;">{{ question.answer || '暂无' }}</div>
              </div>

                <div class="question-section question-analysis">
                  <strong>解析：</strong>
                  <div>{{ question.explain || '暂无' }}</div>
                </div>
              
              
              </div>

              
              <!-- <div v-if="currentHomework.status === 2" class="question-score-display">
                <span class="score-label">得分：</span>
                <span class="score-value" :class="getScoreClass(question.score, question.totalScore)">
                  {{ question.score || 0 }}/{{ question.totalScore }}
                </span>
                <span v-if="question.totalScore > 0" class="score-rate" :class="getScoreClass(question.score, question.totalScore)">
                  得分率：{{ Math.round(((question.score || 0) / question.totalScore) * 100) }}%
                </span>
              </div> -->
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showHomeworkDialog = false">关闭</el-button>
          <el-button 
            v-if="currentHomework?.status === 0"
            type="primary" 
            @click="saveDraft"
          >
            保存草稿
          </el-button>
          <el-button 
            v-if="currentHomework?.status === 0"
            type="success" 
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

.question-section {
  margin-top: 8px;
  padding: 12px;
  border-radius: 6px;
  background-color: #f9f9f9; /* 默认背景 */
}

.question-explain {
  background-color: #fdf6ec; /* 淡橘黄 - 用于“错误诊断” */
  border-left: 4px solid #f4a261;
}

.question-answer {
  background-color: #ecf5ff; /* 淡蓝色 - 用于“标准答案” */
  border-left: 4px solid #409eff;
}

.question-analysis {
  background-color: #f0f9eb; /* 淡绿色 - 用于“解析” */
  border-left: 4px solid #67c23a;
}

.question-section strong {
  display: block;
  margin-bottom: 4px;
  font-weight: bold;
  color: #333;
}

.question-section div {
  white-space: pre-wrap;
  color: #666;
}


/* 统计卡片样式 */
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  animation: student-scale-in 0.8s cubic-bezier(.4, 0, .2, 1);
  animation-delay: calc(var(--index, 0) * 0.1s);
  animation-fill-mode: both;
}

.stat-icon {
  font-size: clamp(20px, 3vw, 28px);
  margin-right: 12px;
  animation: icon-pulse 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: clamp(18px, 3vw, 24px);
  font-weight: 700;
  color: var(--student-text);
  margin-bottom: 4px;
  line-height: 1.2;
}

.stat-label {
  font-size: clamp(11px, 2vw, 14px);
  color: var(--student-text-secondary);
  line-height: 1.3;
}

/* 筛选卡片样式 */
.filter-card {
  padding: 24px;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  background: var(--student-glass);
  border: 1px solid var(--student-glass-border);
  color: var(--student-text-secondary);
  padding: 8px 16px;
  border-radius: var(--student-border-radius-small);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--student-animation);
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-btn:hover {
  background: var(--student-card-hover);
  color: var(--student-text);
}

.filter-btn.active {
  background: var(--gradient-primary);
  color: var(--student-text);
  border-color: rgba(255, 255, 255, 0.3);
}

/* 作业卡片样式 */
.homework-card {
  padding: 20px;
  animation: student-slide-up 0.8s cubic-bezier(.4, 0, .2, 1);
  animation-delay: calc(var(--index, 0) * 0.1s);
  animation-fill-mode: both;
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.homework-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.homework-status {
  flex-shrink: 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: var(--student-text);
}

.status-warning {
  background:rgb(229, 202, 27);
}

.status-primary {
  background:rgb(238, 134, 64);
}

.status-success {
  background:rgb(45, 218, 91);
}

.status-info {
  background: #444;
}

.homework-content {
  margin-bottom: 16px;
}

.homework-desc {
  color: var(--student-text-secondary);
  margin-bottom: 12px;
  line-height: 1.5;
}

.homework-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--student-text-secondary);
}

.meta-item i {
  width: 12px;
  text-align: center;
  flex-shrink: 0;
}

.time-left.urgent {
  color: #ff6b6b;
  font-weight: 600;
}

.time-left.overdue {
  color: #ff6b6b;
  font-weight: 600;
}

.time-left.normal {
  color: var(--student-text-secondary);
}

.score-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--student-glass-border);
}

.score-display {
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 统一得分相关字体样式 */
.score-label,
.score-value,
.score-rate {
  font-size: 15px !important;
  font-weight: bold !important;
  color: #fff !important;
  font-family: 'Microsoft YaHei', 'Arial', 'PingFang SC', 'Hiragino Sans GB', 'Heiti SC', 'sans-serif' !important;
  margin: 0 !important;
  letter-spacing: 0.5px;
}

.score-value,
.score-rate {
  margin-left: 8px !important;
}

.homework-actions {
  display: flex;
  gap: 8px;
}

/* 空状态卡片 */
.empty-card {
  padding: 60px 20px;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-content i {
  font-size: 48px;
  color: var(--student-text-muted);
}

/* 作业详情对话框样式 */
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
  text-align: left;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-controls {
    justify-content: center;
  }
  
  .homework-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .homework-actions {
    flex-direction: column;
  }
  
  .homework-meta {
    flex-direction: column;
  }
}

.student-title.medium {
  margin-bottom: 20px;
}
</style>
