<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuestionApi, saveQuestionApi, publishHomeworkApi, getHomeworkListApi, getStudentSubmissionsApi, gradeHomeworkApi, getHomeworkDetailWithAnswerApi, getHomeworkDetailApi } from '@/api/teacher'
import dayjs from 'dayjs';

const formatDate = (row, column, cellValue) => {
  return dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss');
};

const statusMap = {
  0: { text: '未提交', type: 'info' },
  1: { text: '已提交', type: 'warning' },
  2: { text: '已批改', type: 'success' }
};

const questions = ref([])
const tempIdCounter = ref(-1) // 临时ID计数器，从-1开始递减

const homeworkTitle = ref('')
const homeworkRemark = ref('')
const timeRange = ref([])

// AI对话框相关
const showAIDialogVisible = ref(false)
const isGenerating = ref(false)
const aiFormRef = ref()

// 保存题目按钮状态
const isSaving = ref(false)
const hasSavedInCurrentSession = ref(false)

// 计算保存按钮是否应该禁用
const isSaveButtonDisabled = computed(() => {
  const hasUnsavedQuestions = questions.value.filter(q => q.id < 0).length > 0
  return questions.value.length === 0 || isSaving.value || (hasSavedInCurrentSession.value && !hasUnsavedQuestions)
})

// AI表单数据
const aiFormData = ref({
  knowledge: '',
  type: '',
  count: 3,
  remark: ''
})

// 显示AI对话框
const showAIDialog = () => {
  showAIDialogVisible.value = true
}

// 关闭AI对话框
const handleCloseAIDialog = () => {
  if (isGenerating.value) {
    ElMessage.warning('正在生成题目，请稍候...')
    return false
  }
  showAIDialogVisible.value = false
  // 重置表单
  aiFormData.value = {
    knowledge: '',
    type: '',
    count: 3,
    remark: ''
  }
}

// AI生成题目
const handleAIGenerate = async () => {
  // 验证表单
  const valid = await aiFormRef.value.validate().catch(() => false)
  if (!valid) return

  isGenerating.value = true

  try {
    ElMessage.info('AI正在分析知识点并生成题目...')

    const result = await getQuestionApi(aiFormData.value)
    if (result.code === 1 && Array.isArray(result.data)) {
      // AI生成的题目给临时ID，并自动设置分值
      questions.value = result.data.map(q => {
        const { id, ...questionWithoutId } = q

        // 根据题型自动设置分值（与人工出题逻辑一致）
        const defaultScores = {
          choice: 5,
          short: 15,
          code: 25
        }
        const score = defaultScores[q.type] || 10

        return {
          ...questionWithoutId,
          score: score,  // 自动设置分值
          id: tempIdCounter.value-- // 临时ID，负数
        }
      })
      // 重置保存状态，允许重新保存
      hasSavedInCurrentSession.value = false
      ElMessage.success(`AI成功生成了${aiFormData.value.count}道题目！`)
    } else {
      ElMessage.error(result.msg || '生成失败')
    }

    showAIDialogVisible.value = false

    // 重置表单
    aiFormData.value = {
      knowledge: '',
      type: '',
      count: 3,
      remark: ''
    }

  } catch (error) {
    ElMessage.error('AI生成题目失败，请重试')
  } finally {
    isGenerating.value = false
  }
}

// 人工出题
const handleManualAdd = () => {
  const newQuestion = {
    id: tempIdCounter.value--, // 临时ID，负数
    content: '',
    knowledge: '',
    answer: '',
    explain: '',
    type: 'choice',
    score: 5
    // 临时ID为负数，保存后会获得真实的正数数据库ID
  }

  questions.value.push(newQuestion)
  // 重置保存状态，允许重新保存
  hasSavedInCurrentSession.value = false
  ElMessage.success('已添加新题目，请填写内容')
}

// 监听题型变化，自动调整分值
const handleTypeChange = (question) => {
  const defaultScores = {
    choice: 5,
    short: 15,
    code: 25
  }
  question.score = defaultScores[question.type] || 10
}

// 获取答案输入框的提示文字
const getAnswerPlaceholder = (type) => {
  const placeholders = {
    choice: '请输入选择题答案，格式如：A. 选项内容\nB. 选项内容\nC. 选项内容\nD. 选项内容\n正确答案：A',
    short: '请输入简答题的参考答案',
    code: '请输入编程题的参考代码和解答说明'
  }
  return placeholders[type] || '请输入答案内容'
}

// 获取答案输入框的行数
const getAnswerRows = (type) => {
  const rows = {
    choice: 6,
    short: 4,
    code: 8
  }
  return rows[type] || 4
}

// 删除题目
const removeQuestion = (id) => {
  ElMessageBox.confirm('确定要删除这道题目吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    questions.value = questions.value.filter(q => q.id !== id)
    ElMessage.success('题目已删除')
  }).catch(() => {
    // 取消删除
  })
}

// 保存题目到数据库
const saveQuestions = async () => {
  if (questions.value.length === 0) {
    ElMessage.warning('没有题目可保存')
    return
  }

  // 验证题目完整性
  const incompleteQuestions = questions.value.filter(q => !q.content.trim())
  if (incompleteQuestions.length > 0) {
    ElMessage.warning('请完善所有题目的内容')
    return
  }

  // 过滤出未保存的题目（临时ID为负数）
  const unsavedQuestions = questions.value.filter(q => q.id < 0)

  if (unsavedQuestions.length === 0) {
    ElMessage.info('所有题目已保存，无需重复保存')
    return
  }

  // 设置保存状态
  isSaving.value = true

  try {
    ElMessage.info('正在保存题目...')
    // 这里调用后端API保存题目

    console.log('准备保存的题目:', unsavedQuestions)

    const result = await saveQuestionApi(unsavedQuestions)
    console.log('后端返回的完整结果:', result)

    if (result.code === 1) {
      // 保存成功后，用真实ID替换临时ID
      console.log('保存成功，返回的数据类型:', typeof result.data)
      console.log('返回的数据内容:', result.data)
      console.log('是否为数组:', Array.isArray(result.data))

      if (result.data) {
        if (Array.isArray(result.data)) {
          // 如果返回的是数组
          console.log('处理数组数据，长度:', result.data.length)
          result.data.forEach((savedQuestion, index) => {
            console.log(`处理第${index + 1}个保存的题目:`, savedQuestion)
            const tempId = unsavedQuestions[index].id
            const questionIndex = questions.value.findIndex(q => q.id === tempId)
            if (questionIndex !== -1) {
              console.log(`替换题目: 临时ID ${tempId} → 真实ID ${savedQuestion.id}`)
              questions.value[questionIndex] = savedQuestion
            }
          })
        } else {
          // 如果返回的不是数组，可能是单个对象或其他结构
          console.log('返回的不是数组，尝试其他处理方式')
          console.log('数据结构:', result.data)

          // 检查是否有 questions 字段
          if (result.data.questions && Array.isArray(result.data.questions)) {
            console.log('找到 questions 字段，处理数据')
            result.data.questions.forEach((savedQuestion, index) => {
              const tempId = unsavedQuestions[index].id
              const questionIndex = questions.value.findIndex(q => q.id === tempId)
              if (questionIndex !== -1) {
                console.log(`替换题目: 临时ID ${tempId} → 真实ID ${savedQuestion.id}`)
                questions.value[questionIndex] = savedQuestion
              }
            })
          } else {
            console.log('无法识别的数据结构，请检查后端返回格式')
          }
        }

        console.log('更新后的questions:', questions.value)

        // 检查每个题目的ID情况
        questions.value.forEach((q, index) => {
          console.log(`题目${index + 1}: id=${q.id}, 类型=${typeof q.id}, 是否已保存=${q.id > 0}`)
        })

        // 检查发布按钮状态
        const hasUnsaved = questions.value.some(q => !q.id || q.id <= 0)
        console.log('发布按钮禁用状态:', hasUnsaved)
        console.log('是否可以发布:', !hasUnsaved && homeworkTitle.value)
      }
      ElMessage.success('题目保存成功！')
      // 标记本次会话已保存
      hasSavedInCurrentSession.value = true
    } else {
      ElMessage.error(result.msg || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  } finally {
    // 重置保存状态
    isSaving.value = false
  }
}

// 清空题目
const clearQuestions = () => {
  ElMessageBox.confirm('确定要清空所有题目吗？', '确认清空', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    questions.value = []
    // 重置保存状态
    hasSavedInCurrentSession.value = false
    ElMessage.success('已清空所有题目')
  }).catch(() => {
    // 取消清空
  })
}

// 在script中添加清空题目内容的方法
const clearQuestionContent = (q) => {
  q.content = ''
  q.knowledge = ''
  q.answer = ''
  q.explain = ''
  q.score = 5
  hasSavedInCurrentSession.value = false
}

// 监听题目内容变化，允许再次保存
const onQuestionFieldChange = () => {
  hasSavedInCurrentSession.value = false
}

// 判断题目是否已保存
const isQuestionSaved = (q) => q.id > 0

// 获取当前登录老师ID
const getCurrentTeacherId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 发布作业
const publishHomework = async () => {
  if (!homeworkTitle.value) {
    ElMessage.warning('请输入作业标题')
    return
  }

  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择作业起止时间')
    return
  }

  if (questions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }

  try {
    // 检查题目是否已保存（ID为正数表示已保存）
    const unsavedQuestions = questions.value.filter(q => !q.id || q.id <= 0)
    if (unsavedQuestions.length > 0) {
      ElMessage.warning('请先保存题目，再发布作业')
      return
    }

    ElMessage.info('正在发布作业...')

    // 使用已保存题目的真实ID
    const questionIds = questions.value.map(q => q.id)
    const totalScore = questions.value.reduce((sum, q) => sum + q.score, 0)

    const result = await publishHomeworkApi({
      remark: homeworkRemark.value,
      title: homeworkTitle.value,
      teacherId: getCurrentTeacherId(),
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
      questionIds: questionIds,  // 使用真实的题目ID
      totalScore: totalScore
    })

    if (result.code === 1) {
      ElMessage.success(`作业发布成功！已发布给${result.data}名学生`)

      // 重新加载历史记录
      await loadHomeworkHistory()

      // 重置表单
      homeworkTitle.value = ''
      homeworkRemark.value = ''
      timeRange.value = []
      questions.value = []
      // 重置保存状态
      hasSavedInCurrentSession.value = false
    } else {
      ElMessage.error(result.msg || '发布失败')
    }

  } catch (error) {
    ElMessage.error('发布失败：' + (error.message || '请重试'))
  }
}

// 获取题型名称
const getTypeName = (type) => {
  const typeMap = {
    choice: '选择题',
    short: '简答题',
    code: '编程题'
  }
  return typeMap[type] || '未知题型'
}

// 获取题型颜色
const getTypeColor = (type) => {
  const colorMap = {
    choice: 'primary',
    short: 'success',
    code: 'warning'
  }
  return colorMap[type] || 'info'
}

// 历史记录
const history = ref([])

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


// 加载作业历史记录
const loadHomeworkHistory = async () => {
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return

    const result = await getHomeworkListApi(teacherId)
    if (result.code === 1) {
      history.value = result.data.map(item => ({
        id: item.id,
        title: item.title,
        publishTime: item.createdTime,
        status: item.status,
        timeLeft: formatTimeLeft(item.endTime)
      }))
    }
  } catch (error) {
    console.error('加载作业历史失败：', error)
  }
}

// 页面加载时获取历史记录
onMounted(() => {
  loadHomeworkHistory()
})

// 查看作业详情
const detailDialogVisible = ref(false)
const currentHomework = ref({})
const homeworkQuestions = ref([])
const studentSubmissions = ref([])

const viewDetail = async (row) => {
  try {
    currentHomework.value = row
    detailDialogVisible.value = true

    // 调用API获取作业详细信息（只查题目和标准答案）
    const [detailResult, submissionsResult] = await Promise.all([
      getHomeworkDetailApi(row.id),
      getStudentSubmissionsApi(row.id)
    ])

    if (detailResult.code === 1) {
      homeworkQuestions.value = detailResult.data || []
    } else {
      ElMessage.error('获取作业题目失败：' + detailResult.msg)
      homeworkQuestions.value = []
    }

    if (submissionsResult.code === 1) {
      studentSubmissions.value = submissionsResult.data || []
    } else {
      ElMessage.error('获取提交统计失败：' + submissionsResult.msg)
      studentSubmissions.value = []
    }

  } catch (error) {
    console.error('获取作业详情失败：', error)
    ElMessage.error('获取作业详情失败，请重试')

    // 如果接口失败，提供空数据避免页面报错
    homeworkQuestions.value = []
    studentSubmissions.value = []
  }
}

// 状态转换工具函数
const getStatusText = (status) => {
  const statusMap = {
    0: '未提交',
    1: '已提交',
    2: '已批改'
  }
  return statusMap[status] || '未提交'
}

const getStatusTagType = (status) => {
  const typeMap = {
    0: 'info',      // 未提交 - 灰色
    1: 'warning',   // 已提交 - 橙色
    2: 'success'    // 已批改 - 绿色
  }
  return typeMap[status] || 'info'
}

// 批改相关
const gradeDialogVisible = ref(false)
const currentSubmission = ref({})
const currentGradeQuestions = ref([])
const gradeScores = ref({})
const gradeFeedback = ref('')

// 批改作业
const gradeHomework = async (submission) => {
  try {
    currentSubmission.value = submission
    gradeDialogVisible.value = true

    // 获取作业详情和学生答案（新接口）
    const detailResult = await getHomeworkDetailWithAnswerApi(submission.homeworkId, submission.studentId)
    if (detailResult.code === 1) {
      currentGradeQuestions.value = detailResult.data || []
      // 初始化评分对象，绑定 score 字段
      gradeScores.value = {}
      currentGradeQuestions.value.forEach(question => {
        // 兼容后端返回的 score（实际得分）和 questionScore（题目分值）
        gradeScores.value[question.id] = question.score || 0
      })
      gradeFeedback.value = submission.feedback || ''
    } else {
      ElMessage.error('获取作业详情失败')
    }
  } catch (error) {
    ElMessage.error('加载批改页面失败')
  }
}

// 提交批改结果
const submitGrade = async () => {
  try {
    // 计算总分
    const totalScore = Object.values(gradeScores.value).reduce((sum, score) => sum + (parseInt(score) || 0), 0)

    const gradeData = {
      homeworkId: currentHomework.value.id,  // 使用作业ID，不是提交记录ID
      submissionId: currentSubmission.value.id,  // 添加提交记录ID
      studentId: currentSubmission.value.studentId,  // 添加学生ID
      scores: gradeScores.value,
      totalScore: totalScore,
      feedback: gradeFeedback.value
    }

    const result = await gradeHomeworkApi(gradeData)
    if (result.code === 1) {
      ElMessage.success('批改完成！')
      gradeDialogVisible.value = false

      // 重新加载提交列表
      const submissionsResult = await getStudentSubmissionsApi(currentHomework.value.id)
      if (submissionsResult.code === 1) {
        studentSubmissions.value = submissionsResult.data || []
      }
    } else {
      ElMessage.error(result.msg || '批改失败')
    }
  } catch (error) {
    ElMessage.error('提交批改失败')
  }
}

// 获取得分百分比样式
const getScoreClass = (score, totalScore) => {
  if (!score || !totalScore) return ''
  const percentage = (score / totalScore) * 100
  if (percentage >= 90) return 'excellent'
  if (percentage >= 80) return 'good'
  if (percentage >= 70) return 'average'
  return 'poor'
}

const getTypeTextColor = (type) => {
  switch (type) {
    case 'choice':
      return '#FFFFFF';
    case 'short':
      return '#FFFFFF'; // 橙色
    case 'code':
      return '#FFFFFF'; // 紫色
    default:
      return '#FFFFFF'; // 默认灰
  }
}

// 保存题目前弹出确认提示
const confirmAndSaveQuestions = async () => {
  try {
    await ElMessageBox.confirm('保存后将无法修改该题目内容，是否确认保存？', '确认保存', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await saveQuestions()
  } catch (e) {
    // 用户取消
  }
}
</script>

<template>
  <div class="vertical-layout practise-layout">
    <!-- 题目编辑区域 -->
    <div class="editor-section">
      <el-card shadow="hover" class="temp-el-card">
        <div class="control-header">
          <h3 class="section-title">
            <el-icon style="margin-right:8px;"><EditPen /></el-icon>
            题目编辑区
          </h3>
        </div>
        <div class="empty-hint">
          <el-button type="primary" @click="showAIDialog" size="large" style="color: white">
            <el-icon>
              <MagicStick />
            </el-icon>
            AI 生成题目
          </el-button>

          <el-button type="danger" @click="handleManualAdd" size="large" style="color: white">
            <el-icon>
              <EditPen />
            </el-icon>
            人工出题
          </el-button>
        </div>

        <div v-for="(q, index) in questions" :key="q.id" class="question-block">
          <div class="question-header">
            <h4>题目 {{ index + 1 }}</h4>
            <div>
              <el-tag :type="getTypeColor(q.type)" size="small" :style="{ color: getTypeTextColor(q.type) }">{{
                getTypeName(q.type) }}</el-tag>
              <el-tag v-if="q.id && q.id > 0" size="small"
                style="margin-left: 8px; color:white">已保存</el-tag>
              <el-tag v-else type="warning" size="small" style="margin-left: 8px; color:white">未保存</el-tag>
            </div>
          </div>

          <el-form label-width="60px">
            <el-form-item label="题型">
              <el-select v-model="q.type" placeholder="请选择题型" style="width: 200px" @change="(val) => { handleTypeChange(q); onQuestionFieldChange(); }" :disabled="isQuestionSaved(q)">
                <el-option label="选择题" value="choice">
                  <span>选择题</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">5分</span>
                </el-option>
                <el-option label="简答题" value="short">
                  <span>简答题</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">15分</span>
                </el-option>
                <el-option label="编程题" value="code">
                  <span>编程题</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">25分</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="题干">
              <el-input type="textarea" v-model="q.content" placeholder="请输入题目内容" :rows="8" @input="onQuestionFieldChange" :disabled="isQuestionSaved(q)" />
            </el-form-item>

            <el-form-item label="知识点">
              <el-input type="textarea" v-model="q.knowledge" placeholder="请输入涉及知识点" :rows="1" @input="onQuestionFieldChange" :disabled="isQuestionSaved(q)" />
            </el-form-item>

            <el-form-item label="答案">
              <el-input type="textarea" v-model="q.answer" :placeholder="getAnswerPlaceholder(q.type)"
                :rows="getAnswerRows(q.type)" @input="onQuestionFieldChange" :disabled="isQuestionSaved(q)" />
            </el-form-item>

            <el-form-item label="解析">
              <el-input type="textarea" v-model="q.explain" placeholder="请输入解析说明" :rows="2" @input="onQuestionFieldChange" :disabled="isQuestionSaved(q)" />
            </el-form-item>

            <el-form-item label="分值">
              <el-input-number v-model="q.score" :min="1" :max="100" placeholder="分值" style="width: 120px" @change="onQuestionFieldChange" :disabled="isQuestionSaved(q)" />
            </el-form-item>
          </el-form>

          <div class="question-actions right-actions">
            <el-button type="warning" size="small" @click="clearQuestionContent(q)" :disabled="isQuestionSaved(q)">
              <el-icon><Delete /></el-icon>
              清空
            </el-button>
            <el-button type="danger" size="small" @click="removeQuestion(q.id)">
              <el-icon><Close /></el-icon>
              删除
            </el-button>
            <el-button
              :type="hasSavedInCurrentSession && questions.filter(q => q.id < 0).length === 0 ? 'info' : 'success'"
              size="small" @click="confirmAndSaveQuestions" :disabled="isQuestionSaved(q) || isSaveButtonDisabled" :loading="isSaving">
              <el-icon><Check /></el-icon>
              {{
                isSaving ? '保存中...' :
                  isQuestionSaved(q) ? '已保存' : '保存题目'}}
            </el-button>
          </div>
        </div>

        <div class="header-actions" style="margin-top: 16px;">
          
        </div>
      </el-card>
    </div>

    <!-- 右侧发布与预览 -->
    <div class="side-panel">
      <el-card shadow="hover" >
        <div class="control-header">
          <h3 class="section-title">
            <el-icon style="margin-right:8px;"><Setting /></el-icon>
            作业设置
          </h3>
        </div>
        <el-form label-width="80px" >
          <el-form-item label="作业标题">
            <el-input v-model="homeworkTitle" placeholder="请输入作业标题" />
          </el-form-item>

          <el-form-item label="作业备注">
            <el-input v-model="homeworkRemark" placeholder="请输入作业备注(默认无)" />
          </el-form-item>

          <el-form-item label="起止时间">
            <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始"
              end-placeholder="截止" style="width: 100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>

          <el-form-item label="题目总数">
            <el-tag style="color : white" size="large" >{{ questions.length }} 道题目</el-tag>
          </el-form-item>
        </el-form>

        <el-button type="success" block @click="publishHomework" style=""
          :disabled="questions.length === 0 || !homeworkTitle || questions.some(q => !q.id || q.id <= 0)">
          <el-icon>
            <Upload />
          </el-icon>
          发布作业
        </el-button>
        <div v-if="questions.length > 0 && questions.some(q => !q.id || q.id <= 0)" class="form-tips"
          style="margin-top: 8px; text-align: center;">
          💡 请先保存题目再发布作业
        </div>

        <!-- 调试信息
        <div class="debug-info" style="margin-top: 10px; font-size: 12px; color: #666; border: 1px solid #eee; padding: 10px; border-radius: 4px;">
          <p><strong>调试信息：</strong></p>
          <p>题目数量: {{ questions.length }}</p>
          <p>作业标题: {{ homeworkTitle || '未填写' }}</p>
          <p>时间范围: {{ timeRange && timeRange.length === 2 ? '已选择' : '未选择' }}</p>
          <p>保存状态: {{ hasSavedInCurrentSession ? '✅已保存' : '❌未保存' }}</p>
          <p>保存按钮状态: {{ 
            isSaving ? '🔄保存中' : 
            questions.filter(q => q.id < 0).length === 0 && hasSavedInCurrentSession ? '🔒已禁用' : 
            '✅可用'
          }}</p>
          <p v-if="questions.length > 0">题目保存状态:</p>
          <ul v-if="questions.length > 0" style="margin: 0; padding-left: 20px;">
            <li v-for="(q, index) in questions" :key="q.id">
              题目{{ index + 1 }}: ID={{ q.id }} {{ q.id > 0 ? '✅已保存' : '❌未保存' }}
            </li>
          </ul>
          <p style="margin-top: 8px;"><strong>发布状态: {{ 
            questions.length === 0 ? '❌没有题目' : 
            !homeworkTitle ? '❌未填写标题' : 
            questions.some(q => !q.id || q.id <= 0) ? '❌有题目未保存' : 
            '✅可以发布'
          }}</strong></p>
        </div> -->
      </el-card>

      <!-- 预览区域 -->
      <!-- <el-card style="margin-top: 20px" shadow="hover" v-if="questions.length > 0">
        <div class="control-header">
          <h3>题目预览</h3>
        </div>
        <div class="preview-container">
          <div class="list-preview">
            <div v-for="(q, index) in questions" :key="q.id" class="preview-item">
              <span class="item-num">{{ index + 1 }}.</span>
              <span class="item-content">{{ q.content || '题目内容' }}</span>
              <el-tag :type="getTypeColor(q.type)" size="small">{{ getTypeName(q.type) }}</el-tag>
              <span class="item-score">({{ q.score }}分)</span>
            </div>
          </div>
        </div>
      </el-card> -->

      <!-- 发布记录区块 -->
      <el-card shadow="never">
        <h3 class="section-title" style="color:white"><el-icon>
            <List />
          </el-icon> 发布记录</h3>
        <el-table :data="history" stripe :row-key="row => row.id">
          <el-table-column prop="title" label="作业名称" width="150px" align="center" header-align="center"
            show-overflow-tooltip />

          <el-table-column prop="publishTime" label="发布时间" :formatter="formatDate" width="200px" align="center"
            header-align="center" show-overflow-tooltip />

          <el-table-column prop="endTime" label="截止时间" :formatter="formatDate" width="200px" align="center"
            header-align="center" show-overflow-tooltip />

          <el-table-column prop="timeLeft" label="剩余时间" width="150px" align="center" header-align="center">
            <template #default="scope">
              <!-- <el-tag :type="scope.row.timeLeft === '已逾期' ? 'danger' : 'info'" size="small"
                style="font-size: 13px; font-weight: 500; padding: 0 8px;">
              </el-tag> -->
              {{ scope.row.timeLeft }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100px" align="center" header-align="center">
            <template #default="scope" class >
              <div class="center-button-wrapper">
              <el-button type="primary" size="small" text @click="viewDetail(scope.row)"  class="action-btn"
                style="font-size: 13px; font-weight: 500;">
                查看
              </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>


      </el-card>
    </div>

    <!-- AI生成题目对话框 -->
    <el-dialog v-model="showAIDialogVisible" title="AI 生成题目" width="600px" :before-close="handleCloseAIDialog">
      <div class="ai-dialog-content">
        <el-form label-width="80px" :model="aiFormData" ref="aiFormRef">
          <el-form-item label="知识点" prop="knowledge" :rules="[{ required: true, message: '请输入知识点', trigger: 'blur' }]">
            <el-input v-model="aiFormData.knowledge" placeholder="请输入知识点，如：JAVA面向对象编程" type="textarea" :rows="3" />
            <div class="form-tips">
              💡 提示：请详细描述知识点内容，AI将根据此内容生成相关题目
            </div>
          </el-form-item>

          <el-form-item label="题型" prop="type" :rules="[{ required: true, message: '请选择题型', trigger: 'change' }]">
            <el-select v-model="aiFormData.type" placeholder="请选择题型" style="width: 100%">
              <el-option label="选择题" value="choice">
                <span>选择题</span>
                <span style="float: right; color: #8492a6; font-size: 13px">快速判断基础知识</span>
              </el-option>
              <el-option label="简答题" value="short">
                <span>简答题</span>
                <span style="float: right; color: #8492a6; font-size: 13px">考察理解和表达能力</span>
              </el-option>
              <el-option label="编程题" value="code">
                <span>编程题</span>
                <span style="float: right; color: #8492a6; font-size: 13px">实际编程能力测试</span>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="题目数量">
            <el-input-number v-model="aiFormData.count" :min="1" :max="10" placeholder="题目数量" style="width: 200px" />
            <div class="form-tips">
              建议：选择题 3-5道，简答题 2-3道，编程题 1-2道
            </div>
          </el-form-item>

          <el-form-item label="额外要求">
            <el-input v-model="aiFormData.remark" placeholder="可选：特殊要求或注意事项" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCloseAIDialog">取消</el-button>
          <el-button type="primary" @click="handleAIGenerate" :loading="isGenerating">
            <el-icon>
              <MagicStick />
            </el-icon>
            {{ isGenerating ? 'AI生成中...' : '开始生成' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 作业详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="`作业详情 - ${currentHomework.title}`" width="80%" top="5vh">
      <el-tabs type="border-card">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作业标题">{{ currentHomework.title }}</el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ formatDate(currentHomework.publishTime) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMap[currentHomework.status]?.type || 'default'" style="color:white">
                {{ statusMap[currentHomework.status]?.text || '未知状态' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总分">{{homeworkQuestions.reduce((sum, q) => sum + q.score, 0)
            }}分</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- 题目列表 -->
        <el-tab-pane label="题目内容">
          <div v-for="(question, index) in homeworkQuestions" :key="question.id || `detail-${index}`">
            <el-card shadow="hover" style="margin-bottom: 15px">
              <div class="question-header">
                <h4>第{{ index + 1 }}题</h4>
                <div>
                  <el-tag :type="info" style="color:white" size="small">{{ getTypeName(question.type) }}</el-tag>
                  <el-tag type="info" size="small"  style="margin-left: 8px; color:white">{{ question.score }}分</el-tag>
                </div>
              </div>
              <div class="question-content">
                <p>{{ question.content }}</p>
              </div>

            </el-card>
          </div>
        </el-tab-pane>

        <!-- 学生提交情况 -->
        <el-tab-pane label="提交统计">
          <el-row :gutter="16" style="margin-bottom: 20px">
            <el-col :span="6">
              <el-statistic title="总人数" :value="studentSubmissions.length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="已提交"
                :value="studentSubmissions.filter(s => s.status === 1 || s.status === 2).length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="未提交" :value="studentSubmissions.filter(s => s.status === 0).length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="提交率"
                :value="Math.round(studentSubmissions.filter(s => s.status === 1 || s.status === 2).length / studentSubmissions.length * 100)"
                suffix="%" />
            </el-col>
          </el-row>

          <el-table :data="studentSubmissions" border stripe :row-key="row => row.id || row.studentId">
            <el-table-column prop="studentName" label="学生姓名" />
            <el-table-column prop="status" label="提交状态">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.status)" style="color:white">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="得分">
              <template #default="scope">
                <span v-if="scope.row.status >= 1">{{ scope.row.totalScore }}分</span>
                <span v-else style="color: #999">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" />
            <el-table-column prop="feedback" label="教师反馈" width="200">
              <template #default="scope">
                <span v-if="scope.row.status === 2 && scope.row.feedback" class="feedback-text">
                  {{ scope.row.feedback }}
                </span>
                <span v-else-if="scope.row.status === 1" style="color: #999">待批改</span>
                <span v-else style="color: #999">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status === 1" type="primary" size="small" @click="gradeHomework(scope.row)">
                  批改
                </el-button>
                <el-button v-else-if="scope.row.status === 2" type="danger" size="small"
                  @click="gradeHomework(scope.row)">
                  查看
                </el-button>
                <span v-else style="color: #999">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 批改作业对话框 -->
    <el-dialog v-model="gradeDialogVisible" :title="`批改作业 - ${currentSubmission.studentName}`" width="80%"
      :close-on-click-modal="false">
      <div v-if="currentGradeQuestions.length > 0" class="grade-container">
        <div class="student-info">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="学生姓名">{{ currentSubmission.studentName }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ currentSubmission.submitTime }}</el-descriptions-item>
            <el-descriptions-item label="当前状态">
              <el-tag :type="getStatusTagType(currentSubmission.status)" style="color:white">
                {{ getStatusText(currentSubmission.status) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="questions-grade">
          <div v-for="(question, index) in currentGradeQuestions" :key="question.id || `grade-${index}`"
            class="grade-question-item">
            <el-card shadow="hover" style="margin-bottom: 20px">
              <div class="question-header">
                <h4>第{{ index + 1 }}题</h4>
                <div>
                  <el-tag :type="getTypeColor(question.type)" size="small" style="color:white">
                    {{ getTypeName(question.type) }}
                  </el-tag>
                  <el-tag type="info" size="small" style="margin-left: 8px; color:white">
                    满分{{ question.questionScore }}分
                  </el-tag>
                </div>
              </div>

              <div class="question-content">
                <h5>题目内容：</h5>
                <p>{{ question.content }}</p>
              </div>

              <div class="question-answer" style="margin-top: 8px;">
                <strong>标准答案：</strong>
                <div style="white-space: pre-wrap;">{{ question.trueAnswer || '暂无' }}</div>
              </div>

              <div class="student-answer-section">
                <h5>学生答案：</h5>
                <div class="answer-display">
                  {{ question.answer || '未作答' }}
                </div>
              </div>

              <div class="score-input-section">
                <el-form-item :label="`得分（满分${question.questionScore}分）：`">
                  <el-input-number v-model="gradeScores[question.id]" :min="0" :max="question.questionScore"
                    :precision="0" size="large" style="width: 200px" />
                  <span v-if="gradeScores[question.id] !== undefined"
                    :class="getScoreClass(gradeScores[question.id], question.questionScore)"
                    style="margin-left: 12px; font-weight: 600;">
                    {{ Math.round((gradeScores[question.id] / question.questionScore) * 100) }}%
                  </span>
                </el-form-item>
              </div>
            </el-card>
          </div>
        </div>

        <div class="feedback-section">
          <el-card shadow="hover">
            <h4 style="color:grey">教师反馈：</h4>
            <el-input v-model="gradeFeedback" type="textarea" :rows="4" placeholder="请输入对学生作业的整体评价和建议..." />
          </el-card>
        </div>

        <div class="grade-summary">
          <el-card shadow="hover" style="background: rgba(255,255,255,0.08); backdrop-filter: blur(10px);">
            <div class="summary-content">
              <div class="total-score">
                <span class="label">总得分：</span>
                <span class="score-value">
                  {{Object.values(gradeScores).reduce((sum, score) => sum + (parseInt(score) || 0), 0)}}
                </span>
                <span class="total-possible">
                  / {{currentGradeQuestions.reduce((sum, q) => sum + (q.questionScore || 0), 0)}}
                </span>
              </div>
              <div class="percentage">
                <span :class="getScoreClass(
                  Object.values(gradeScores).reduce((sum, score) => sum + (parseInt(score) || 0), 0),
                  currentGradeQuestions.reduce((sum, q) => sum + (q.questionScore || 0), 0)
                )">
                  {{
                    (() => {
                      const total = Object.values(gradeScores).reduce((sum, score) => sum + (parseInt(score) || 0), 0)
                      const max = currentGradeQuestions.reduce((sum, q) => sum + (q.questionScore || 0), 0)
                      return max > 0 ? Math.round((total / max) * 100) : 0
                    })()
                  }}%
                </span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="gradeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitGrade">
            <el-icon>
              <Check />
            </el-icon>
            提交批改
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>


<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.vertical-layout.practise-layout {
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(18px);
  border-radius: 24px;
  padding: clamp(24px, 4vw, 48px);
  color: #fff;
  animation: page-fade-in 0.8s cubic-bezier(.4,0,.2,1);
}

@keyframes page-fade-in {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.section-title, .control-header h3, h4 {
  font-size: clamp(20px, 3vw, 24px);
  font-weight: 600;
  color: #fff;
  margin: 0 0 clamp(16px, 3vw, 24px) 0;
  display: flex;
  align-items: center;
  gap: clamp(8px, 2vw, 12px);
  animation: section-fade-in 0.8s cubic-bezier(.4,0,.2,1);
}

@keyframes section-fade-in {
  0% { opacity: 0; transform: translateX(-20px); }
  100% { opacity: 1; transform: translateX(0); }
}

.el-card, .temp-el-card {
  background: rgba(255,255,255,0.12) !important;
  border-radius: 20px !important;
  box-shadow: 0 12px 40px rgba(0,0,0,0.16) !important;
  border: none !important;
  transition: all 0.3s ease;
  margin-bottom: 32px;
}

.el-card:hover {
  background: rgba(255,255,255,0.18) !important;
  box-shadow: 0 16px 48px rgba(0,0,0,0.18) !important;
}

.el-button, .btn, button {
  border-radius: 12px !important;
  font-size: 14px !important;
  font-weight: 500;
  padding: 12px 24px !important;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.el-button:hover {
  background: rgba(255,255,255,0.3) !important;
  transform: translateY(-2px);
}

.el-input, .el-date-editor, .el-textarea {
  background: rgba(255,255,255,0.15) !important;
  border: 1px solid rgba(255,255,255,0.25) !important;
  border-radius: 8px !important;
}
.el-input__inner, .el-textarea__inner {
  background: transparent !important;
}

.el-tag {
  background: rgba(161,140,209,0.7) !important;
  border: none !important;
  border-radius: 8px !important;
  font-size: 13px !important;
  padding: 2px 10px !important;
}

.header-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.header-actions .el-button {
  min-width: 110px;
  padding: 10px 20px;
  font-size: 15px;
}

.question-block {
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  padding: 24px 24px 16px 24px;
  margin-bottom: 28px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  color: #fff;
  transition: all 0.3s ease;
}
.question-block:hover {
  background: rgba(255,255,255,0.18);
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.question-header h4 {
  font-size: 16px;
  font-weight: bold;
  margin: 0;
}

.empty-hint {
  color: #e0e0e0;
  padding: 60px 30px;
  text-align: center;
  font-size: 16px;
  background: rgba(255,255,255,0.08);
  border-radius: 12px;
  border: 2px dashed #d9d9d9;
  margin-bottom: 24px;
}

.el-table th,
.el-table td {
  padding: 12px 16px !important; 
  white-space: nowrap;
}

.form-tips {
  font-size: 13px;
  color: #e0e0e0;
  margin-top: 8px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.right-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.saved-tip {
  color:rgb(255, 255, 255);
  font-size: 14px;
  margin-top: 8px;
  text-align: right;
}

@media (max-width: 900px) {
  .vertical-layout.practise-layout {
    padding: 12px;
  }
  .el-card, .temp-el-card {
    padding: 8px;
  }
  .side-panel {
    gap: 16px;
  }
  .question-block {
    padding: 12px 8px 8px 8px;
    margin-bottom: 16px;
  }
}
</style>