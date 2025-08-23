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
  if (rating >= 4) return '#5ad8a6'
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

const changeFormat =  (markdown) =>{
  if (!markdown) return "";

  return markdown
    // 去掉加粗、斜体、代码块等标记
    .replace(/\*\*(.*?)\*\*/g, "$1")
    .replace(/\*(.*?)\*/g, "$1")
    .replace(/`(.*?)`/g, "$1")
    .replace(/#+\s?(.*)/g, "$1") // 去掉标题符号 #

    // 去掉列表符号（有序/无序）
    .replace(/^\s*[-+*]\s+/gm, "")
    .replace(/^\s*\d+\.\s+/gm, "")

    // 把多个换行替换成一个空格
    .replace(/\n+/g, " ")

    // 多余空格压缩
    .replace(/\s{2,}/g, " ")
    .trim();
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
      
      q.answer = changeFormat(result.data)  // 直接使用返回的字符串
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
  <div class="interact-layout vertical-layout">
    <div class="vertical-blocks">
      <!-- 统计区块 -->
      <el-card class="stats-section" shadow="never">
        <h3 class="section-title"><i class="fas fa-chart-bar"></i> 问答统计</h3>
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
        <div class="rating-stats-row">
          <div class="avg-rating-block">
            <span class="avg-value" style="color: white;">{{ averageRating }}</span>
            <span class="avg-stars" :style="{ color: getRatingColor(Math.round(averageRating)) }">
              {{ getRatingStars(Math.round(averageRating)) }}
            </span>
            <span class="avg-label" >平均评分</span>
          </div>
          <div class="rating-bars-block">
            <div class="rating-bar">
              <span style="color:white">满意</span>
              <el-progress
                class="satisfied-progress"
                :percentage="ratings.length > 0 ? Math.round((positiveRatingCount / ratings.length) * 100) : 0"
                :color="'#67c23a'"
                :stroke-width="8"
              />
            </div>
            <div class="rating-bar">
              <span style="color:white">一般</span>
              <el-progress
                class="neutral-progress"
                :percentage="ratings.length > 0 ? Math.round((neutralRatingCount / ratings.length) * 100) : 0"
                :color="'#e6a23c'"
                :stroke-width="8"
              />
            </div>
            <div class="rating-bar">
              <span style="color:white">不满意</span>
              <el-progress
                class="unsatisfied-progress"
                :percentage="ratings.length > 0 ? Math.round((negativeRatingCount / ratings.length) * 100) : 0"
                :color="'#f56c6c'"
                :stroke-width="8"
              />
            </div>
          </div>
        </div>
      </el-card>
      <!-- 问题列表区块 -->
      <el-card class="question-section" shadow="never">
        <h3 class="section-title"><i class="fas fa-comments"></i> 学生互动问答</h3>
        
        <div class="top-toolbar">
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

        <div class="question-list">
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
              <div class="answer-section">

                <el-form label-width="60px">
                  <el-form-item label="回答">
                    <el-input
                      v-model="q.answer"
                      type="textarea"
                      :disabled="q.answered"
                     
                      placeholder="请输入回答内容，或点击AI生成回答..."
                      :rows="10"
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
                      AI生成
                    </el-button>
                    <el-button
                      type="warning"
                      size="small"
                      @click="clearAnswer(q)"
                      :disabled="q.answered"
                    >
                      <el-icon><Delete /></el-icon>
                      清空
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
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');
.interact-layout {
    min-height: 100%;
    animation: page-fade-in 0.8s ease-out;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    padding: clamp(24px, 4vw, 48px);
    border-radius: 24px;
    color: #fff;
}

@keyframes page-fade-in {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

.section-title, h3, h4 {
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

.card, .el-card, .question-item, .rating-stats-row, .avg-rating-block, .rating-bars-block {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);
    padding: clamp(20px, 4vw, 32px);
    transition: all 0.3s ease;
    color: #fff;
}

.card:hover, .el-card:hover, .question-item:hover {
    box-shadow: 0 8px 32px rgba(0,0,0,0.10);
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

.top-toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
/* 单选按钮组玻璃风 */
.top-toolbar :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(6px);
  border-radius: 6px;
  padding: 6px 12px;
  transition: all 0.2s ease;
  margin-right: 10px;
}

.top-toolbar :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.3);
}

.top-toolbar :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.4);
  color: #000;
  font-weight: 600;
}

/* 搜索输入框玻璃风 */
.top-toolbar :deep(.el-input) {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  backdrop-filter: blur(8px);
  color: #fff;
}

.top-toolbar :deep(.el-input__inner) {
  background: transparent !important;
  color: #fff;
}

.top-toolbar :deep(.el-input__prefix),
.top-toolbar :deep(.el-input__suffix) {
  color: #fff;
}

:deep(.el-textarea__inner::placeholder) {
  color: white !important; /* 改成你想要的颜色 */
  opacity: 1;
}

/* 修改字数统计样式 */
:deep(.el-input__count) {
  color: white !important;
  font-size: 13px;
  font-weight: 500;
  background-color: transparent;
}


.vertical-blocks {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.question-item {
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  transition: all 0.3s ease;
}

/* .question-item:hover { */
  /* border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  background: rgba(255,255,255,0.15); */
/* } */

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
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.question-time {
  color: #e0e0e0;
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
  background: rgba(255,255,255,0.12);
  border-radius: 8px;
  border-left: 4px solid #409eff;
  font-size: 15px;
  line-height: 1.6;
  color: #fff;
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
  border-top: 1px solid rgba(255,255,255,0.12);
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
  background: rgba(255,255,255,0.08);
  border-radius: 8px;
  flex: 1;
  min-width: 0;
  color: #fff;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 11px;
  color: #e0e0e0;
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
  color: yellow;
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
  color: #e0e0e0;
}

.rating-bar span:last-child {
  width: 20px;
  text-align: right;
  color: #e0e0e0;
}

.rating-bar :deep(.el-progress) {
  flex: 1;
}

.rating-bar :deep(.el-progress-bar__outer) {
  background-color: #2c3e50;
}

/* 空状态 */
.empty-hint {
  color: #e0e0e0;
  padding: 80px 40px;
  text-align: center;
  font-size: 16px;
  background: rgba(255,255,255,0.08);
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
  border-radius: 12px !important;
  font-weight: 500;
  color: #fff !important;
  background: rgba(255,255,255,0.2) !important;
  border: 1px solid rgba(255,255,255,0.3) !important;
  transition: all 0.3s ease;
}
.el-button:hover {
  background: rgba(255,255,255,0.3) !important;
  transform: translateY(-2px);
}

/* 表单样式 */
.el-form-item {
  margin-bottom: 16px;
}

.el-form-item :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.el-textarea :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.6;
  padding: 12px 15px;
  color: #fff;
  background: rgba(255,255,255,0.08);
  border-radius: 8px;
  border: none;
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
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
}
.rating-stars {
  font-size: 18px;
  font-weight: bold;
}
.rating-label {
  font-size: 13px;
  color: #e0e0e0;
}
.rating-comment {
  color: #409eff;
  font-style: italic;
}
.rating-time {
  font-size: 12px;
  color: #e0e0e0;
}
.question-rating.no-rating {
  color: #e0e0e0;
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
  font-size: 15px;
  color: white;
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

:deep(.satisfied-progress .el-progress-bar__outer) {
  background-color: #fff !important;
}
:deep(.neutral-progress .el-progress-bar__outer) {
  background-color: #fff !important;
}
:deep(.unsatisfied-progress .el-progress-bar__outer) {
  background-color: #fff !important;
}
</style>
