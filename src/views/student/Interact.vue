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
</script>

<template>
  <div class="student-interact-layout">
    <!-- 左侧主要区域 -->
    <div class="main-panel">
      <!-- 顶部统计概览 -->
      <div class="stats-section">
        <div class="stats-cards">
          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">总提问数</div>
                <div class="card-value">{{ interactStats.totalQuestions }}</div>
                <div class="card-desc">累计提问</div>
              </div>
              <div class="card-icon">
                <el-icon><ChatDotSquare /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">已回答</div>
                <div class="card-value">{{ interactStats.answeredQuestions }}</div>
                <div class="card-desc">获得解答</div>
              </div>
              <div class="card-icon">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">待回答</div>
                <div class="card-value">{{ interactStats.pendingQuestions }}</div>
                <div class="card-desc">等待中</div>
              </div>
              <div class="card-icon">
                <el-icon><Clock /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">平均评分</div>
                <div class="card-value">{{ interactStats.avgRating.toFixed(1) }}</div>
                <div class="card-desc">满意度</div>
              </div>
              <div class="card-icon">
                <el-icon><Star /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="stat-card">
            <div class="card-content">
              <div class="card-info">
                <div class="card-title">评分次数</div>
                <div class="card-value">{{ ratingStats.totalRatings }}</div>
                <div class="card-desc">已评价</div>
              </div>
              <div class="card-icon">
                <el-icon><Medal /></el-icon>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 提问区域 -->
      <div class="question-section">
        <el-card shadow="hover">
          <div class="section-header">
            <h3>
              <el-icon><ChatDotSquare /></el-icon>
              提问学习
            </h3>
            <div class="header-tips">
              <el-icon><InfoFilled /></el-icon>
              <span>详细描述问题，获得更好的回答</span>
            </div>
          </div>

          <el-form @submit.prevent="submitQuestion" class="question-form">
            <el-form-item label="问题类型：">
              <el-input
                v-model="newQuestion.type"
                placeholder="请输入问题类型，如：java、vue、database、frontend、other等"
                style="width: 400px"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="问题标题：">
              <el-input
                v-model="newQuestion.title"
                placeholder="请简要描述你的问题..."
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="详细描述：">
              <el-input
                v-model="newQuestion.content"
                type="textarea"
                :rows="4"
                placeholder="请详细描述你遇到的问题，包括具体的错误信息、代码片段等..."
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="submitQuestion"
                :loading="submitting"
                size="large"
              >
                <el-icon><Promotion /></el-icon>
                提交问题
              </el-button>
              <el-button @click="resetForm" size="large">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- 问题列表 -->
      <div class="questions-section">
        <el-card shadow="hover">
          <div class="section-header">
            <h3>
              <el-icon><List /></el-icon>
              我的问题
            </h3>
            <div class="header-actions">
              <el-radio-group v-model="filterStatus" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="pending">待回答</el-radio-button>
                <el-radio-button value="answered">已回答</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div v-if="filteredQuestions.length === 0" class="empty-state">
            <el-empty description="暂无问题数据" />
          </div>

          <div v-else class="questions-list">
            <div v-for="question in filteredQuestions" :key="question.id" class="question-item">
              <div class="question-header">
                <div class="question-info">
                  <div class="question-title-row">
                    <h4>{{ question.title }}</h4>
                    <div class="question-meta">
                              <el-tag :type="getTypeColor(question.type)" size="small">
          {{ question.type }}
        </el-tag>
                      <el-tag :type="getStatusType(question.status)" size="small">
                        {{ getStatusText(question.status) }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="question-time">
                    <el-icon><Clock /></el-icon>
                    <span>提问时间：{{ formatTime(question.createdTime) }}</span>
                    <span v-if="question.answeredTime" class="response-time">
                      · 响应时间：{{ getResponseTime(question.createdTime, question.answeredTime) }}
                    </span>
                  </div>
                </div>
                <div class="question-actions">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="viewQuestion(question)"
                  >
                    查看详情
                  </el-button>
                </div>
              </div>

              <div class="question-preview">
                <p class="question-desc">{{ question.content }}</p>
                
                <!-- 已回答的显示教师信息 -->
                <div v-if="question.status === 1" class="answer-preview">
                  <div class="teacher-info">
                    <el-icon><User /></el-icon>
                    <span>{{ question.teacherName }}</span>
                    <span v-if="question.rating > 0" class="rating-display">
                      <el-rate 
                        v-model="question.rating" 
                        disabled 
                        size="small"
                        show-score
                      />
                    </span>
                  </div>
                  <div class="answer-snippet">
                    {{ question.answer ? question.answer.substring(0, 100) + '...' : '查看完整回答' }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
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
        <el-descriptions :column="2" border>
          <el-descriptions-item label="问题类型">
            <el-tag :type="getTypeColor(currentQuestion.type)" size="small">
              {{ currentQuestion.type }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提问时间">
            {{ formatTime(currentQuestion.createdTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="回答状态">
            <el-tag :type="getStatusType(currentQuestion.status)" size="small">
              {{ getStatusText(currentQuestion.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="响应时间" v-if="currentQuestion.answeredTime">
            {{ getResponseTime(currentQuestion.createdTime, currentQuestion.answeredTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="question-content-section">
        <h4>问题描述：</h4>
        <div class="content-display">
          {{ currentQuestion.content }}
        </div>
      </div>

      <div v-if="currentQuestion.status === 1" class="answer-section">
        <h4>教师回答：</h4>
        <div class="teacher-info">
          <el-icon><User /></el-icon>
          <span>{{ currentQuestion.teacherName }}</span>
          <span class="answer-time">回答于 {{ formatTime(currentQuestion.answeredTime) }}</span>
        </div>
        <div class="answer-content">
          {{ currentQuestion.answer }}
        </div>

        <!-- 评价区域 -->
        <div class="rating-section">
          <h5>评价回答质量：</h5>
          <div class="rating-controls">
            <el-rate 
              v-model="ratingForm.rating" 
              :disabled="currentQuestion.rating && currentQuestion.rating > 0"
              size="large"
              show-text
              :texts="['很差', '较差', '一般', '较好', '很好']"
              @change="currentRating = ratingForm.rating"
            />
            

            
            <div v-if="!currentQuestion.rating || currentQuestion.rating === 0" class="rating-buttons">
              <el-button 
                type="primary" 
                size="small" 
                @click="submitRating"
                :disabled="ratingForm.rating === 0"
              >
                <el-icon><Check /></el-icon>
                确认评价
              </el-button>
              <el-button 
                size="small" 
                @click="resetRating"
              >
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </div>
            <div v-else class="rated-info">
              <span class="rated-text">
                <el-icon><CircleCheck /></el-icon>
                已评价 {{ currentQuestion.rating }} 分
              </span>

            </div>
          </div>
        </div>


      </div>

      <div v-else class="waiting-section">
        <el-result icon="clock" title="等待回答中" sub-title="教师会尽快为您解答问题">
        </el-result>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="showQuestionDialog = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>


</template>

<style scoped>
.student-interact-layout {
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

/* 统计卡片 */
.stats-section {
  margin-bottom: 24px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.stat-card {
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

/* 通用卡片样式 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-tips {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

/* 提问表单 */
.question-form {
  max-width: 800px;
}

.question-form .el-form-item {
  margin-bottom: 20px;
}

.question-form .el-form-item__label {
  font-weight: 600;
  color: #2c3e50;
}

/* 问题列表 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  padding: 20px;
  border: 1px solid #e8f4fd;
  border-radius: 12px;
  background: #f8fcff;
  transition: all 0.3s ease;
}

.question-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.question-info {
  flex: 1;
}

.question-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question-title-row h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}

.question-meta {
  display: flex;
  gap: 8px;
}

.question-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.response-time {
  color: #67c23a;
}

.question-preview {
  margin-top: 12px;
}

.question-desc {
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
}

.answer-preview {
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

/* 新增评分相关样式 */

.rating-reason {
  margin: 12px 0;
}

.rating-reason-input {
  width: 100%;
}

.rating-reason-display {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
}

.rated-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}



.teacher-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #2c3e50;
  font-weight: 600;
}

.rating-display {
  margin-left: auto;
}

.answer-snippet {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

/* 问题详情对话框 */
.question-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.question-info-panel {
  margin-bottom: 20px;
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

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .student-interact-layout {
    padding: 16px;
  }
  
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .question-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .question-title-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}
</style>
