<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAnswerApi, getStudentQuestionsApi, sendStudentAnswerApi, getStudentRatingsApi } from '@/api/teacher'

// 筛选和搜索
const filterType = ref('all')
const searchKeyword = ref('')

// 学生提问列表
const questions = ref([])

// 学生评分列表
const ratings = ref([])

const loadingMap = ref({}) // 控制每条问题的 AI 加载状态

// 计算属性
const filteredQuestions = computed(() => {
  let filtered = questions.value

  // 按状态筛选
  if (filterType.value === 'unanswered') {
    filtered = filtered.filter(q => !q.answered)
  } else if (filterType.value === 'answered') {
    filtered = filtered.filter(q => q.answered)
  }

  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(q => 
      q.name.toLowerCase().includes(keyword) || 
      q.question.toLowerCase().includes(keyword)
    )
  }

  return filtered
})

const unAnsweredCount = computed(() => questions.value.filter(q => !q.answered).length)
const answeredCount = computed(() => questions.value.filter(q => q.answered).length)
const responseRate = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})

// 评分统计
const averageRating = computed(() => {
  if (ratings.value.length === 0) return 0
  const total = ratings.value.reduce((sum, r) => sum + r.rating, 0)
  return (total / ratings.value.length).toFixed(1)
})

const positiveRatingCount = computed(() => ratings.value.filter(r => r.rating >= 4).length)
const neutralRatingCount = computed(() => ratings.value.filter(r => r.rating === 3).length)
const negativeRatingCount = computed(() => ratings.value.filter(r => r.rating <= 2).length)

// 获取评分星级显示
const getRatingStars = (rating) => {
  return '★'.repeat(rating) + '☆'.repeat(5 - rating)
}

// 获取评分颜色
const getRatingColor = (rating) => {
  if (rating >= 4) return '#67c23a'
  if (rating === 3) return '#e6a23c'
  return '#f56c6c'
}

// 获取评分文本
const getRatingText = (rating) => {
  if (rating >= 4) return '满意'
  if (rating === 3) return '一般'
  return '不满意'
}

// 加载学生评分列表
const loadRatings = async () => {
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return
    
    const result = await getStudentRatingsApi(teacherId)
    if (result.code === 1) {
      ratings.value = result.data.map(item => ({
        id: item.id,
        studentId: item.studentId,
        studentName: item.studentName,
        question: item.question,
        rating: item.rating,
        createTime: item.createdTime,
      }))
    }
  } catch (error) {
    console.error('加载评分列表失败：', error)
  }
}

// AI 生成回答
const generateAIAnswer = async (q) => {
  loadingMap.value[q.id] = true
  
  try {
    const result = await getAnswerApi({
      type: q.type,  // 使用问题的具体类型，如java、vue等
      question: q.question      // 学生的问题内容
    })
    
    if (result.code === 1 && result.data) {
      q.answer = result.data  // 直接使用返回的字符串
      ElMessage.success('AI回答生成成功，请检查后发送')
    } else {
      ElMessage.error(result.msg || '生成失败')
    }
  } catch (error) {
    ElMessage.error('AI生成回答失败，请重试')
  } finally {
    loadingMap.value[q.id] = false
  }
}

// 清空回答
const clearAnswer = (q) => {
  ElMessageBox.confirm('确定要清空当前回答内容吗？', '确认清空', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    q.answer = ''
    ElMessage.success('回答内容已清空')
  }).catch(() => {
    // 取消操作
  })
}

// 获取当前登录教师ID
const getCurrentTeacherId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 加载学生问题列表
const loadQuestions = async () => {
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return

    // 并发获取问答和评分
    const [questionsRes, ratingsRes] = await Promise.all([
      getStudentQuestionsApi(teacherId),
      getStudentRatingsApi(teacherId)
    ])

    let ratingsMap = {}
    if (ratingsRes.code === 1 && Array.isArray(ratingsRes.data)) {
      ratingsRes.data.forEach(r => {
        ratingsMap[r.id] = r
      })
    }

    if (questionsRes.code === 1 && Array.isArray(questionsRes.data)) {
      questions.value = questionsRes.data.map(item => {
        const ratingObj = ratingsMap[item.id]
        return {
          id: item.id,
          name: item.studentName,
          question: item.content || item.title || '',
          answer: item.answer || '',
          answered: item.status === 1,
          createTime: item.createdTime,
          title: item.title,
          type: item.type,
          // 合并评分
          rating: ratingObj ? Number(ratingObj.rating) : undefined,
          ratingTime: ratingObj ? ratingObj.createdTime : undefined
        }
      })
    }
  } catch (error) {
    console.error('加载问题列表或评分失败：', error)
  }
}




// 发送回答
const sendAnswer = async (q) => {
  if (!q.answer.trim()) {
    ElMessage.warning('请输入回答内容')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要发送回答给${q.name}吗？`, '确认发送', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      type: 'success',
    })

    const result = await sendStudentAnswerApi({
      questionId: q.id,
      teacherId: getCurrentTeacherId(),
      answer: q.answer
    })

    if (result.code === 1) {
      q.answered = true
      ElMessage.success(`回答已发送给${q.name}`)
    } else {
      ElMessage.error(result.msg || '发送失败')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发送失败，请重试')
         }
   }
 }

// 页面加载时获取数据
onMounted(() => {
  loadQuestions()
  loadRatings()
})
</script>


<template>
  <div class="interact-layout">
    <!-- 上方区域：操作栏和统计平分 -->
    <div class="top-section">
      <!-- 操作栏 -->
      <div class="control-panel">
        <el-card shadow="hover" class="panel-card">
          <div class="panel-header">
            <h3>
              <el-icon><ChatDotRound /></el-icon>
              学生互动问答
            </h3>
            <div class="header-stats">
              <el-tag type="info" size="small">待回答 {{ unAnsweredCount }}</el-tag>
              <el-tag type="success" size="small">已回答 {{ answeredCount }}</el-tag>
            </div>
          </div>
          
          <div class="panel-content">
            <div class="filter-section">
              <el-radio-group v-model="filterType" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="unanswered">待回答</el-radio-button>
                <el-radio-button value="answered">已回答</el-radio-button>
              </el-radio-group>
              
              <el-input
                v-model="searchKeyword"
                placeholder="搜索学生姓名或问题内容"
                prefix-icon="Search"
                size="small"
                style="width: 280px; margin-left: 16px"
                clearable
              />
            </div>
          </div>
        </el-card>
      </div>

      <!-- 统计面板 -->
      <div class="stats-panel">
        <el-card shadow="hover" class="panel-card">
          <div class="panel-header">
            <h3>
              <el-icon><DataAnalysis /></el-icon>
              问答统计
            </h3>
          </div>
          
          <div class="panel-content">
            <!-- 问答统计 -->
            <div class="stats-horizontal">
              <div class="stat-item">
                <div class="stat-value">{{ questions.length }}</div>
                <div class="stat-label">总问题数</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ unAnsweredCount }}</div>
                <div class="stat-label">待回答</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ answeredCount }}</div>
                <div class="stat-label">已回答</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ responseRate }}%</div>
                <div class="stat-label">回答率</div>
              </div>
            </div>

            <el-divider style="margin: 18px 0 12px 0;" />

            <!-- 评分统计优化 -->
            <div class="rating-stats-row">
              <div class="avg-rating-block">
                <span class="avg-value">{{ averageRating }}</span>
                <span class="avg-stars" :style="{ color: getRatingColor(Math.round(averageRating)) }">
                  {{ getRatingStars(Math.round(averageRating)) }}
                </span>
                <span class="avg-label">平均评分</span>
              </div>
              <div class="rating-bars-block">
                <div class="rating-bar">
                  <span>满意</span>
                  <el-progress :percentage="ratings.length > 0 ? Math.round((positiveRatingCount / ratings.length) * 100) : 0" :color="'#67c23a'" :stroke-width="8" />
                  <span>{{ positiveRatingCount }}</span>
                </div>
                <div class="rating-bar">
                  <span>一般</span>
                  <el-progress :percentage="ratings.length > 0 ? Math.round((neutralRatingCount / ratings.length) * 100) : 0" :color="'#e6a23c'" :stroke-width="8" />
                  <span>{{ neutralRatingCount }}</span>
                </div>
                <div class="rating-bar">
                  <span>不满意</span>
                  <el-progress :percentage="ratings.length > 0 ? Math.round((negativeRatingCount / ratings.length) * 100) : 0" :color="'#f56c6c'" :stroke-width="8" />
                  <span>{{ negativeRatingCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 下方区域：标签页切换 -->
    <div class="content-section">
      <el-card shadow="hover">
        <template v-if="filteredQuestions.length === 0">
          <div class="empty-hint">
            <el-icon><ChatDotRound /></el-icon>
            <p>暂无{{ filterType === 'all' ? '' : filterType === 'unanswered' ? '待回答的' : '已回答的' }}问题</p>
          </div>
        </template>
        <template v-else>
          <div v-for="q in filteredQuestions" :key="q.id" class="question-item">
            <div class="question-header">
              <div class="student-info">
                <div class="student-details">
                  <h4>{{ q.name }}</h4>
                  <span class="question-time">{{ q.createTime }}</span>
                </div>
              </div>
              <div class="question-status">
                <el-tag v-if="q.answered" type="success" size="small">
                  <el-icon><Check /></el-icon>
                  已回答
                </el-tag>
                <el-tag v-else type="warning" size="small">
                  <el-icon><Clock /></el-icon>
                  待回答
                </el-tag>
              </div>
            </div>
            <div class="question-content">
              <div class="question-text">
                <el-icon><QuestionFilled /></el-icon>
                <span>{{ q.question }}</span>
              </div>
            </div>
            <!-- 评分展示 -->
            <div class="question-rating" v-if="q.rating !== undefined && q.rating !== null">
              <span class="rating-stars" :style="{ color: getRatingColor(q.rating) }">
                {{ getRatingStars(q.rating) }}
              </span>
              <span class="rating-label">{{ getRatingText(q.rating) }}</span>
              <span class="rating-time">{{ q.ratingTime }}</span>
            </div>
            <div class="question-rating no-rating" v-else>
              暂无评分
            </div>
            <!-- 其余操作按钮等内容保持不变 -->
            <div class="answer-section">
              <el-form label-width="60px">
                <el-form-item label="回答">
                  <el-input
                    v-model="q.answer"
                    type="textarea"
                    :disabled="q.answered"
                    placeholder="请输入回答内容，或点击AI生成回答..."
                    :rows="4"
                    maxlength="2500"
                    show-word-limit
                  />
                </el-form-item>
              </el-form>
              <div class="action-buttons">
                <div class="left-actions">
                  <el-button
                    type="primary"
                    size="small"
                    @click="generateAIAnswer(q)"
                    :loading="loadingMap[q.id]"
                    :disabled="q.answered"
                  >
                    <el-icon><MagicStick /></el-icon>
                    AI生成回答
                  </el-button>
                  <el-button
                    type="info"
                    size="small"
                    @click="clearAnswer(q)"
                    :disabled="q.answered || !q.answer"
                    plain
                  >
                    <el-icon><RefreshLeft /></el-icon>
                    重新编辑
                  </el-button>
                </div>
                <div class="right-actions">
                  <el-button
                    type="success"
                    size="small"
                    @click="sendAnswer(q)"
                    :disabled="q.answered || !q.answer.trim()"
                  >
                    <el-icon><Promotion /></el-icon>
                    发送回答
                  </el-button>
                </div>
              </div>
            </div>
            <el-divider />
          </div>
        </template>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.interact-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.top-section {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
  height: 200px;
}

.control-panel {
  flex: 1;
}

.stats-panel {
  flex: 1;
}

/* 面板卡片统一样式 */
.panel-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-card :deep(.el-card__body) {
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
  flex-shrink: 0;
}

.panel-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-content {
  flex: 1;
  display: flex;
  align-items: center;
}

.content-section {
  flex: 1;
  overflow-y: auto;
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



.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.card-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-stats {
  display: flex;
  gap: 8px;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

/* 问题项样式 */
.question-item {
  margin-bottom: 24px;
  padding: 20px;
  border: 2px solid #e8f4fd;
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
  align-items: center;
  margin-bottom: 16px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-details h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.question-time {
  color: #999;
  font-size: 12px;
}

.question-content {
  margin-bottom: 20px;
}

.question-text {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  font-size: 15px;
  line-height: 1.6;
  color: #333;
}

.answer-section {
  margin-top: 16px;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e0e0e0;
}

.left-actions,
.right-actions {
  display: flex;
  gap: 8px;
}

/* 统计卡片 */
.stats-horizontal {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.stat-item {
  text-align: center;
  padding: 12px 8px;
  background: #f8f9fa;
  border-radius: 8px;
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 11px;
  color: #666;
}

/* 评分统计样式 */
.rating-stats {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.rating-overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avg-rating {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.avg-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.avg-stars {
  font-size: 20px;
  font-weight: bold;
}

.rating-breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rating-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.rating-bar span:first-child {
  width: 40px;
  color: #666;
}

.rating-bar span:last-child {
  width: 20px;
  text-align: right;
  color: #666;
}

.rating-bar :deep(.el-progress) {
  flex: 1;
}

.rating-bar :deep(.el-progress-bar__outer) {
  background-color: #f0f0f0;
}

/* 空状态 */
.empty-hint {
  color: #8c8c8c;
  padding: 80px 40px;
  text-align: center;
  font-size: 16px;
  background: #fafafa;
  border-radius: 12px;
  border: 2px dashed #d9d9d9;
}

.empty-hint .el-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #d0d0d0;
}

.empty-hint p {
  margin: 0;
  line-height: 1.6;
}

/* 标题样式 */
h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}



/* 按钮样式 */
.el-button {
  font-size: 14px !important;
  padding: 8px 16px !important;
  border-radius: 6px !important;
  font-weight: 500;
}

/* 表单样式 */
.el-form-item {
  margin-bottom: 16px;
}

.el-form-item :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.el-textarea :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.6;
  padding: 12px 15px;
}

/* 分割线 */
.el-divider {
  margin: 20px 0 !important;
  border-color: #e8e8e8 !important;
}

/* 标签页样式 */
.interact-tabs {
  margin-top: 16px;
}

.interact-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.interact-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  padding: 12px 24px;
}

/* 问题评分展示样式（简洁） */
.question-rating {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: #888;
  display: flex;
  align-items: center;
  gap: 8px;
}
.rating-stars {
  font-size: 16px;
  font-weight: bold;
}
.rating-label {
  font-size: 13px;
  color: #666;
}
.rating-comment {
  color: #409eff;
  font-style: italic;
}
.rating-time {
  font-size: 12px;
  color: #bbb;
  margin-left: 8px;
}
.no-rating {
  color: #ccc;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .interact-layout {
    gap: 16px;
    padding: 16px;
  }
  
  .top-section {
    gap: 16px;
  }
}

@media (max-width: 1200px) {
  .top-section {
    flex-direction: column;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .el-input {
    margin-left: 0 !important;
  }
}

@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    gap: 12px;
  }
  
  .left-actions,
  .right-actions {
    width: 100%;
    justify-content: center;
  }
  
  .stats-horizontal {
    flex-direction: column;
    gap: 8px;
  }
  
  .rating-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .rating-display {
    align-self: flex-end;
  }
}

/* 评分统计样式优化 */
.rating-stats-row {
  display: flex;
  align-items: flex-start;
  gap: 32px;
  margin-top: 8px;
}

.avg-rating-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
  margin-right: 24px;
}

.avg-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  line-height: 1;
}

.avg-stars {
  font-size: 22px;
  font-weight: bold;
  margin: 4px 0;
}

.avg-label {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}

.rating-bars-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 180px;
}

@media (max-width: 900px) {
  .rating-stats-row {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  .avg-rating-block {
    flex-direction: row;
    justify-content: flex-start;
    min-width: 0;
    margin-right: 0;
    gap: 12px;
  }
}
</style>
