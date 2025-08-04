<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  submitQuestionApi, 
  getMyQuestionsApi, 
  getQuestionDetailApi, 
  getInteractStatsApi,
  getTeacherIdApi,
  submitRatingApi,
  getRatingApi,
  getRatingStatsApi
} from '@/api/student'

// 过滤状态
const filterStatus = ref('all')

// 统计数据
const interactStats = ref({
  totalQuestions: 0,
  answeredQuestions: 0,
  pendingQuestions: 0,
  avgRating: 0,
  avgResponseTime: 0
})

// 互动统计数据
const interactStatsData = ref([
  { label: '总提问数', value: '0', icon: 'fas fa-comments', color: '#667eea' },
  { label: '已回答', value: '0', icon: 'fas fa-check-circle', color: '#f5576c' },
  { label: '待回答', value: '0', icon: 'fas fa-clock', color: '#4facfe' },
  { label: '平均评分', value: '0', icon: 'fas fa-star', color: '#26d0ce' },
  { label: '评分次数', value: '0', icon: 'fas fa-medal', color: '#ffd700' }
])

// 筛选选项
const filterOptions = ref([
  { label: '全部', value: 'all', icon: 'fas fa-list' },
  { label: '待回答', value: 'pending', icon: 'fas fa-clock' },
  { label: '已回答', value: 'answered', icon: 'fas fa-check' }
])

// 评分统计数据
const ratingStats = ref({
  totalRatings: 0,
  avgRating: 0,
  ratingDistribution: {
    1: 0, 2: 0, 3: 0, 4: 0, 5: 0
  }
})



// 表单数据
const newQuestion = ref({
  type: '',
  title: '',
  content: ''
})

// 评分表单数据
const ratingForm = ref({
  rating: 0
})

// 提交状态
const submitting = ref(false)

// 问题列表
const questionList = ref([])

// 对话框
const showQuestionDialog = ref(false)
const currentQuestion = ref(null)
const currentRating = ref(0)

// 缓存分配的教师ID
const assignedTeacherId = ref(null)

// 获取当前登录学生ID
const getCurrentStudentId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 计算属性
const filteredQuestions = computed(() => {
  if (filterStatus.value === 'all') return questionList.value
  return questionList.value.filter(q => {
    switch (filterStatus.value) {
      case 'pending':
        return q.status === 0
      case 'answered':
        return q.status === 1
      default:
        return true
    }
  })
})

// 状态相关方法
const getTypeColor = (type) => {
  // 根据类型名称动态分配颜色
  if (!type) return 'info'
  
  const typeLower = type.toLowerCase()
  
  // 根据类型名称特征分配颜色
  if (typeLower.includes('java') || typeLower.includes('编程')) return 'warning'
  if (typeLower.includes('vue') || typeLower.includes('前端')) return 'success'
  if (typeLower.includes('database') || typeLower.includes('数据库')) return 'primary'
  if (typeLower.includes('spring') || typeLower.includes('框架')) return 'danger'
  if (typeLower.includes('html') || typeLower.includes('css')) return 'info'
  
  // 默认颜色
  return 'info'
}




const getStatusType = (status) => {
  return status === 1 ? 'success' : 'warning'
}

const getStatusClass = (status) => {
  return status === 1 ? 'status-success' : 'status-warning'
}

const getStatusText = (status) => {
  return status === 1 ? '已回答' : '待回答'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN')
}

const getResponseTime = (createdTime, answeredTime) => {
  if (!answeredTime) return '待回答'
  const created = new Date(createdTime)
  const answered = new Date(answeredTime)
  const diffHours = Math.round((answered - created) / (1000 * 60 * 60))
  if (diffHours < 1) return '1小时内'
  if (diffHours < 24) return `${diffHours}小时`
  return `${Math.round(diffHours / 24)}天`
}

// 加载问题列表
const loadQuestions = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getMyQuestionsApi(studentId)
    if (result.code === 1) {
      questionList.value = result.data.map(item => ({
        id: item.id,
        type: item.type || 'other',
        title: item.title,
        content: item.content,
        status: item.status, // 0-待回答, 1-已回答
        createdTime: item.createdTime,
        answeredTime: item.answeredTime,
        answer: item.answer || '',
        teacherName: item.teacherName || '',
        rating: item.rating || 0
      }))
    }
  } catch (error) {
    console.error('加载问题列表失败：', error)
  }
}

// 加载统计数据
const loadInteractStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getInteractStatsApi(studentId)
    if (result.code === 1) {
      interactStats.value = {
        totalQuestions: result.data.totalQuestions || 0,
        answeredQuestions: result.data.answeredQuestions || 0,
        pendingQuestions: result.data.pendingQuestions || 0,
        avgRating: result.data.avgRating || 0,
        avgResponseTime: result.data.avgResponseTime || 0
      }
      
      // 更新互动统计数据
      interactStatsData.value = [
        { label: '总提问数', value: interactStats.value.totalQuestions.toString(), icon: 'fas fa-comments', color: '#667eea' },
        { label: '已回答', value: interactStats.value.answeredQuestions.toString(), icon: 'fas fa-check-circle', color: '#f5576c' },
        { label: '待回答', value: interactStats.value.pendingQuestions.toString(), icon: 'fas fa-clock', color: '#4facfe' },
        { label: '平均评分', value: interactStats.value.avgRating.toFixed(1), icon: 'fas fa-star', color: '#26d0ce' },
        { label: '评分次数', value: ratingStats.value.totalRatings.toString(), icon: 'fas fa-medal', color: '#ffd700' }
      ]
    }
  } catch (error) {
    console.error('加载统计数据失败：', error)
  }
}

// 提交问题
const submitQuestion = async () => {
  if (!newQuestion.value.title.trim()) {
    ElMessage.warning('请填写问题标题')
    return
  }
  if (!newQuestion.value.content.trim()) {
    ElMessage.warning('请详细描述问题')
    return
  }
  if (!newQuestion.value.type) {
    ElMessage.warning('请选择问题类型')
    return
  }

  submitting.value = true
  
  try {
    const studentId = getCurrentStudentId()
    const teacherId = await getAssignedTeacherId() // 异步获取教师ID
    
    if (!teacherId) {
      submitting.value = false
      return // 如果获取不到教师ID，直接返回（错误信息已在getAssignedTeacherId中显示）
    }
    
    const questionData = {
      studentId: studentId,
      teacherId: teacherId,
      title: newQuestion.value.title,
      content: newQuestion.value.content,
      type: newQuestion.value.type
    }
    
    const result = await submitQuestionApi(questionData)
    if (result.code === 1) {
      ElMessage.success('问题提交成功，教师会尽快回答')
      resetForm()
      loadQuestions() // 重新加载问题列表
      loadInteractStats() // 重新加载统计数据
    } else {
      ElMessage.error(result.msg || '提交失败')
    }
    
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 获取分配的教师ID（从学生-教师关系表获取）
const getAssignedTeacherId = async () => {
  // 如果已经缓存了教师ID，直接返回
  if (assignedTeacherId.value) {
    return assignedTeacherId.value
  }
  
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) {
      ElMessage.error('获取学生信息失败')
      return null
    }
    
    const result = await getTeacherIdApi(studentId)
    if (result.code === 1 && result.data) {
      assignedTeacherId.value = result.data // 缓存教师ID
      return result.data
    } else {
      ElMessage.error('未找到分配的教师，请联系管理员')
      return null
    }
  } catch (error) {
    console.error('获取分配教师失败：', error)
    ElMessage.error('获取分配教师失败，请重试')
    return null
  }
}

// 重置表单
const resetForm = () => {
  newQuestion.value = {
    type: '',
    title: '',
    content: ''
  }
}

// 查看问题详情
const viewQuestion = async (question) => {
  try {
    const result = await getQuestionDetailApi(question.id)
    if (result.code === 1) {
      currentQuestion.value = {
        ...question,
        ...result.data
      }
      
      // 加载该问题的评分信息
      const ratingData = await loadQuestionRating(question.id)
      if (ratingData) {
        currentQuestion.value.rating = ratingData
        ratingForm.value.rating = ratingData
      } else {
        ratingForm.value.rating = 0
      }
      
      currentRating.value = ratingForm.value.rating
      showQuestionDialog.value = true
    }
  } catch (error) {
    ElMessage.error('加载问题详情失败')
  }
}

// 使用新评分系统提交评分
const submitRating = async () => {
  if (ratingForm.value.rating === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  
  try {
    const result = await submitRatingApi(currentQuestion.value.id, ratingForm.value.rating)
    if (result.code === 1) {
      ElMessage.success('评价提交成功，感谢您的反馈')
      // 立即更新当前问题的评分状态
      currentQuestion.value = {
        ...currentQuestion.value,
        rating: ratingForm.value.rating
      }
      // 更新问题列表中对应的问题
      const questionIndex = questionList.value.findIndex(q => q.id === currentQuestion.value.id)
      if (questionIndex !== -1) {
        questionList.value[questionIndex].rating = ratingForm.value.rating
      }
      // 重置评分表单
      ratingForm.value = {
        rating: 0
      }
      currentRating.value = ratingForm.value.rating
      // 重新加载数据
      loadQuestions()
      loadInteractStats()
      loadRatingStats()
    } else {
      ElMessage.error(result.msg || '评价失败')
    }
  } catch (error) {
    ElMessage.error('评价失败，请重试')
  }
}

// 加载评分统计数据
const loadRatingStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getRatingStatsApi(studentId)
    if (result.code === 1) {
      ratingStats.value = result.data
    }
  } catch (error) {
    console.error('加载评分统计失败：', error)
  }
}



// 获取特定问题的评分
const loadQuestionRating = async (questionId) => {
  try {
    const result = await getRatingApi(questionId)
    if (result.code === 1 && result.data) {
      return result.data
    }
    return null
  } catch (error) {
    console.error('获取问题评分失败：', error)
    return null
  }
}

// 重置评分
const resetRating = () => {
  ElMessageBox.confirm(
    '确定要重置评分吗？这会清除您的当前评分。',
    '确认重置',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    // 重置评分表单
    ratingForm.value = {
      rating: 0
    }
    currentRating.value = 0
    // 同时重置当前问题的评分状态（仅在本地，不提交到服务器）
    if (currentQuestion.value) {
      currentQuestion.value = {
        ...currentQuestion.value,
        rating: 0
      }
    }
    ElMessage.success('评分已重置，您可以重新选择评分')
  }).catch(() => {
    // 用户取消操作
  })
}

// 复制代码
const copyCode = (code) => {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('代码已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 初始化
onMounted(() => {
  console.log('学生互动模块初始化')
  loadQuestions()
  loadInteractStats()
  loadRatingStats()
})

const hoverRating = ref(0)
</script>

<template>
  <div class="student-container">
    <!-- 页面标题 -->
    <div class="student-section">
      <h1 class="student-title large">
        <i class="fas fa-comments"></i>
        互动模块
      </h1>
      <p class="student-text secondary">与教师交流，解决学习问题</p>
    </div>

    <!-- 互动统计区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-chart-bar"></i>
        互动统计
      </h2>
      <div class="student-grid three-columns">
        <div class="student-card stat-card" v-for="(stat, index) in interactStatsData" :key="index">
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

    <!-- 提问区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-question-circle"></i>
        提问学习
      </h2>
      <div class="student-card question-form-card">
        <div class="form-header">
          <div class="form-title">
            <i class="fas fa-edit"></i>
            提交新问题
          </div>
          <div class="form-tips">
            <i class="fas fa-info-circle"></i>
            详细描述问题，获得更好的回答
          </div>
        </div>
        
        <form @submit.prevent="submitQuestion" class="question-form">
          <div class="form-group">
            <label class="form-label">问题类型</label>
            <input
              v-model="newQuestion.type"
              type="text"
              class="student-input"
              placeholder="请输入问题类型(简短名词)，如：java、vue、database、frontend、other等"
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label class="form-label">问题标题</label>
            <input
              v-model="newQuestion.title"
              type="text"
              class="student-input"
              placeholder="请简要描述你的问题..."
              maxlength="100"
            />
          </div>

          <div class="form-group">
            <label class="form-label">详细描述</label>
            <textarea
              v-model="newQuestion.content"
              class="student-input"
              rows="4"
              placeholder="请详细描述你遇到的问题，包括具体的错误信息、代码片段等..."
              maxlength="2500"
            ></textarea>
          </div>

          <div class="form-actions">
            <button
              type="submit"
              class="student-button"
              :disabled="submitting"
            >
              <i class="fas fa-paper-plane"></i>
              {{ submitting ? '提交中...' : '提交问题' }}
            </button>
            <button
              type="button"
              class="student-button secondary"
              @click="resetForm"
            >
              <i class="fas fa-redo"></i>
              重置
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 问题列表区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-list"></i>
        我的问题
      </h2>
      
      <!-- 筛选区域 -->
      <div class="student-card filter-card">
        <div class="filter-header">
          <div class="filter-title">
            <i class="fas fa-filter"></i>
            问题筛选
          </div>
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
      
      <div v-if="filteredQuestions.length === 0" class="student-card empty-card">
        <div class="empty-content">
          <i class="fas fa-inbox"></i>
          <p class="student-text secondary">暂无问题数据</p>
        </div>
      </div>
      
      <div v-else class="student-grid auto-fit">
        <div 
          v-for="question in filteredQuestions" 
          :key="question.id" 
          class="student-card question-card"
        >
          <div class="question-header">
            <div class="question-title">{{ question.title }}</div>
            <div class="question-status">
              <span class="status-badge" :class="getStatusClass(question.status)">
                {{ getStatusText(question.status) }}
              </span>
            </div>
          </div>
          
          <div class="question-content">
            <div class="question-meta">
              <div class="meta-item">
                <i class="fas fa-tag"></i>
                <span>{{ question.type }}</span>
              </div>
              <div class="meta-item">
                <i class="fas fa-calendar"></i>
                <span>{{ formatTime(question.createdTime) }}</span>
              </div>
              <div v-if="question.answeredTime" class="meta-item">
                <i class="fas fa-clock"></i>
                <span class="response-time">{{ getResponseTime(question.createdTime, question.answeredTime) }}</span>
              </div>
            </div>
            
            <p class="question-desc">{{ question.content }}</p>
          
          </div>
          
          <div class="question-actions">
            <button 
              class="student-button"
              @click="viewQuestion(question)"
            >
              <i class="fas fa-eye"></i>
              查看详情
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 问题详情对话框 -->
    <el-dialog 
      v-model="showQuestionDialog" 
      :title="currentQuestion?.title" 
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentQuestion" class="question-detail">
        <div class="question-info-panel">
          <p><strong>问题类型：</strong>{{ currentQuestion.type }}</p>
          <p><strong>提问时间：</strong>{{ formatTime(currentQuestion.createdTime) }}</p>
          <p v-if="currentQuestion.answeredTime"><strong>回答时间：</strong>{{ formatTime(currentQuestion.answeredTime) }}</p>
        </div>
        
        <div class="question-content-section">
          <h4>问题内容</h4>
          <div class="content-display">{{ currentQuestion.content }}</div>
        </div>
        
        <div v-if="currentQuestion.status === 1" class="answer-section">
          <h4>教师回答</h4>
          <div class="answer-section">
            <div class="teacher-info">
              <i class="fas fa-user-tie"></i>
              <span>{{ currentQuestion.teacherName }}</span>
              <span class="answer-time">{{ formatTime(currentQuestion.answeredTime) }}</span>
            </div>
            <div class="answer-content">{{ currentQuestion.answer }}</div>
          </div>
        </div>
        
        <div v-if="currentQuestion.status === 1" class="rating-section">
          <h5>评价回答质量</h5>
          <div class="rating-controls">

            <div v-if="currentRating === 0" class="rating-buttons">
            <button 
              v-for="rating in 5" 
              :key="rating"
              class="rating-btn"
              @click="currentRating = rating; ratingForm.rating = rating"
              @mouseover="hoverRating = rating"
              @mouseleave="hoverRating = 0"
            >
              <i 
                class="fas fa-star"
                :style="{ color: rating <= hoverRating ? '#f7ba2a' : '#ccc' }"
              ></i>
            </button>
          </div>

            <div v-else class="rated-text">
              <i class="fas fa-star"></i>
              已评价：{{ currentRating }} 星
            </div>
          </div>
        </div>
        
        <div v-else class="waiting-section">
          <i class="fas fa-clock"></i>
          <p>等待教师回答中...</p>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showQuestionDialog = false">关闭</el-button>
          <el-button 
            type="primary" 
            @click="submitRating"
          >
            提交评价
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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

/* 提问表单卡片样式 */
.question-form-card {
  padding: 24px;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--student-glass-border);
  flex-wrap: wrap;
  gap: 16px;
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-tips {
  font-size: 14px;
  color: var(--student-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.question-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--student-text);
}

.student-input {
  background: var(--student-glass);
  border: 1px solid var(--student-glass-border);
  border-radius: var(--student-border-radius-small);
  color: var(--student-text);
  padding: 12px 16px;
  font-size: 14px;
  transition: all var(--student-animation);
  backdrop-filter: blur(10px);
  resize: vertical;
}

.student-input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.student-input::placeholder {
  color: var(--student-text-muted);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 筛选卡片样式 */
.filter-card {
  padding: 20px;
  margin-bottom: 20px;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--student-text);
  display: flex;
  align-items: center;
  gap: 8px;
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

/* 问题卡片样式 */
.question-card {
  padding: 20px;
  animation: student-slide-up 0.8s cubic-bezier(.4, 0, .2, 1);
  animation-delay: calc(var(--index, 0) * 0.1s);
  animation-fill-mode: both;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.question-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.question-status {
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
  background: rgb(222, 105, 42);
}

.status-success {
  background: rgb(33, 220, 89);
}

.question-content {
  margin-bottom: 16px;
}

.question-meta {
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

.response-time {
  color: var(--student-text);
  font-weight: 500;
}

.question-desc {
  color: var(--student-text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
}

.answer-preview {
  padding: 12px;
  background: var(--student-glass);
  border-radius: var(--student-border-radius-small);
  border: 1px solid var(--student-glass-border);
}

.teacher-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--student-text);
  font-weight: 600;
}

.answer-snippet {
  color: var(--student-text-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.question-actions {
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

/* 问题详情对话框样式 */
.question-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.question-info-panel {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.question-info-panel p {
  margin: 8px 0;
  line-height: 1.6;
}

.question-content-section {
  margin-bottom: 20px;
}

.question-content-section h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
}

.content-display {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  line-height: 1.6;
  white-space: pre-wrap;
}

.answer-section {
  margin-bottom: 20px;
}

.answer-section h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
}

.answer-section .teacher-info {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.answer-time {
  color: #999;
  font-size: 12px;
  font-weight: normal;
  margin-left: auto;
}

.answer-content {
  padding: 16px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 16px;
}

.rating-section h5 {
  margin: 0 0 12px 0;
  color: #2c3e50;
}

.rating-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rating-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.rating-btn {
  background: var(--student-glass);
  border: 1px solid var(--student-glass-border);
  color: var(--student-text-secondary);
  padding: 8px 12px;
  border-radius: var(--student-border-radius-small);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--student-animation);
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-btn:hover {
  background: var(--student-card-hover);
  color: var(--student-text);
}

.rated-text {
  color: #67c23a;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.waiting-section {
  text-align: center;
  padding: 40px 20px;
}

.waiting-section i {
  font-size: 48px;
  color: var(--student-text-muted);
  margin-bottom: 16px;
}

.waiting-section p {
  color: var(--student-text-secondary);
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-controls {
    justify-content: center;
  }
  
  .question-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .question-actions {
    flex-direction: column;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .rating-buttons {
    flex-wrap: wrap;
    justify-content: center;
  }
}

.student-title.medium {
  margin-bottom: 20px;
}

.student-grid.three-columns {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  margin-bottom: 32px;
}

.student-grid.three-columns .stat-card {
  flex: 1 1 0;
  min-width: 160px;
  max-width: 220px;
}

@media (max-width: 900px) {
  .student-grid.three-columns {
    flex-wrap: wrap;
    gap: 12px;
  }
  .student-grid.three-columns .stat-card {
    min-width: 140px;
    max-width: 100%;
  }
}

</style>
