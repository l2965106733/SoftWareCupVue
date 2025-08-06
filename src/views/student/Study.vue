<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getCoursewareListApi, getStudyStatsApi, recordStudyBehaviorApi, getQuestionApi, getStudyRecordsApi } from '@/api/student'

// AI对话框相关
const showAIDialogVisible = ref(false)
const isGenerating = ref(false)
const aiFormRef = ref()
const questions = ref([])

const tempIdCounter = ref(-1)

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
      
      questions.value = result.data.map(q => ({
      ...q,
      id: tempIdCounter.value-- // 每次生成一个唯一负数 ID
      }))
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

// 课件数据
const coursewareList = ref([])

// 学习时长跟踪相关
const studyTimer = ref({})          // 存储每个资源的计时器
const studyStartTime = ref({})      // 存储学习开始时间
const studySessionTime = ref({})    // 存储本次学习会话时间
const activeStudyResources = ref(new Set()) // 当前正在学习的资源
const pausedStudyResources = ref(new Set()) // 暂停的学习资源
const realTimeStudyTime = ref({})   // 实时学习时长显示


// 统计数据
const totalCourseware = ref(0)
const studiedCourseware = ref(0)
const aiQuestions = ref(0)
const totalStudyTime = ref(0)      // 总学习时长（分钟）
const todayStudyTime = ref(0)      // 今日学习时长（分钟）

// 学习统计数据
const studyStats = ref([
    { label: '课件总数', value: '0', icon: 'fas fa-folder', color: '#667eea' },
    { label: '已学习', value: '0', icon: 'fas fa-check-circle', color: '#f5576c' },
    // { label: 'AI提问数', value: '0', icon: 'fas fa-robot', color: '#4facfe' },
    { label: '总学习时长', value: '0分钟', icon: 'fas fa-clock', color: '#26d0ce' },
    { label: '今日学习', value: '0分钟', icon: 'fas fa-calendar-day', color: '#ffd700' }
])

// 学习统计详情对话框
const showStatsDialog = ref(false)
const todayStudyResources = ref(0)
const todayAiQuestions = ref(0)
const resourceProgress = ref([])
const knowledgeStats = ref([])

// 学习时长趋势图数据
import { getStudyTimeTrendApi } from '@/api/student'
const trendOption = ref({})
const trendChartVisible = ref(false)

// 显示学习统计详情
const showStudyStatsDetail = async () => {
  showStatsDialog.value = true
  await loadDetailedStats()
  // 不要在这里调用loadTrendData
}

const onStatsDialogOpened = () => {
  trendChartVisible.value = false
  nextTick(() => {
    trendChartVisible.value = true
    loadTrendData()
  })
}

// 加载详细统计数据
const loadDetailedStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return

    // 加载今日统计数据
    const todayResult = await getStudyStatsApi(studentId, { period: 'today' })
    if (todayResult.code === 1) {
      todayStudyResources.value = todayResult.data.studyResources || 0
      todayAiQuestions.value = todayResult.data.aiQuestions || 0
    }

    // 加载学习进度数据
    const progressResult = await getStudyRecordsApi(studentId, { type: 'progress' })
    if (progressResult.code === 1) {
      resourceProgress.value = progressResult.data.map(item => ({
        id: item.resourceId,
        name: item.resourceName,
        studyTime: Math.floor((item.studyDuration || 0) / 60),
        lastStudyTime: item.lastStudyTime || '暂无'
      }))
    }

    // 加载知识点掌握数据
    const knowledgeResult = await getStudyStatsApi(studentId, { type: 'knowledge' })
    if (knowledgeResult.code === 1) {
      knowledgeStats.value = knowledgeResult.data.knowledgeStats || []
    }

    console.log('详细统计数据加载成功')
  } catch (error) {
    console.error('加载详细统计数据失败:', error)
  }
}

// 学习时长跟踪方法
const startStudyTimer = (resourceId) => {
  const now = Date.now()
  studyStartTime.value[resourceId] = now
  studySessionTime.value[resourceId] = 0
  activeStudyResources.value.add(resourceId)

  // 每30秒记录一次学习进度
  studyTimer.value[resourceId] = setInterval(() => {
    updateStudyProgress(resourceId)
  }, 30000) // 30秒间隔

  console.log(`开始学习资源 ${resourceId}，计时器已启动`)
}

const stopStudyTimer = async (resourceId) => {
  if (studyTimer.value[resourceId]) {
    clearInterval(studyTimer.value[resourceId])
    delete studyTimer.value[resourceId]
  }

  activeStudyResources.value.delete(resourceId)

  // 记录最终的学习时长
  await updateStudyProgress(resourceId, true)

  // 清理相关数据
  delete studyStartTime.value[resourceId]
  delete studySessionTime.value[resourceId]

  console.log(`停止学习资源 ${resourceId}，数据已保存`)
}

const updateStudyProgress = async (resourceId, isEnd = false, action = 'update') => {
  const studentId = getCurrentStudentId()
  if (!studentId || !studyStartTime.value[resourceId]) return

  const currentTime = Date.now()
  const sessionDuration = Math.floor((currentTime - studyStartTime.value[resourceId]) / 1000)

  try {
    await recordStudyBehaviorApi({
      studentId: studentId,
      resourceId: resourceId,
      action: isEnd ? 'end_study' : 'update_progress',
      sessionDuration: sessionDuration,
      studyStatus: isEnd ? 2 : 1, // 1-学习中, 2-本次学习结束
      timestamp: new Date().toISOString()
    })

    // 如果不是结束，重置开始时间用于下一个间隔
    if (!isEnd) {
      studyStartTime.value[resourceId] = Date.now()
    }

    console.log(`更新学习进度: 资源${resourceId}, 时长${sessionDuration}秒, 结束:${isEnd}, 动作:${action}`)
  } catch (error) {
    console.error('更新学习进度失败:', error)
  }
}

// 停止所有学习计时器
const stopAllStudyTimers = async () => {
  const promises = Array.from(activeStudyResources.value).map(resourceId =>
    stopStudyTimer(resourceId)
  )
  await Promise.all(promises)
}

// 获取资源名称
const getResourceName = (resourceId) => {
  const resource = coursewareList.value.find(item => item.id === resourceId)
  return resource ? resource.title : `资源 ${resourceId}`
}

// 格式化实时学习时长
const formatRealTimeStudyTime = (resourceId) => {
  const time = realTimeStudyTime.value[resourceId] || 0
  return formatStudyTime(time)
}

// 暂停学习计时器
const pauseStudyTimer = async (resourceId) => {
  if (studyTimer.value[resourceId]) {
    clearInterval(studyTimer.value[resourceId])
    delete studyTimer.value[resourceId]
    pausedStudyResources.value.add(resourceId)

    // 记录暂停
    await updateStudyProgress(resourceId, false, 'pause')

    ElMessage.info(`已暂停学习: ${getResourceName(resourceId)}`)
  }
}


// 更新实时学习时长显示
const updateRealTimeDisplay = () => {
  const now = Date.now()
  activeStudyResources.value.forEach(resourceId => {
    if (studyStartTime.value[resourceId] && !pausedStudyResources.value.has(resourceId)) {
      const elapsed = Math.floor((now - studyStartTime.value[resourceId]) / 1000)
      realTimeStudyTime.value[resourceId] = elapsed
    }
  })
}

// 开始实时时长显示更新
const startRealTimeDisplay = () => {
  setInterval(updateRealTimeDisplay, 1000) // 每秒更新一次
}

// 增强的课件预览方法
const handlePreview = async (courseware) => {
  if (!courseware.url) return ElMessage.warning('文件未就绪')

  const studentId = getCurrentStudentId()
  const resourceId = courseware.id

  try {
    // 记录开始学习
    await recordStudyBehaviorApi({
      studentId: studentId,
      resourceId: resourceId,
      action: 'start_study',
      studyStatus: 1, // 开始学习
      timestamp: new Date().toISOString()
    })

    // 开始计时
    startStudyTimer(resourceId)

    // 更新本地统计
    await loadStudyStats()

    ElMessage.success(`开始学习: ${courseware.title}`)
  } catch (error) {
    console.error('记录学习开始失败:', error)
  }

  const url = courseware.url.toLowerCase()

  if (
    url.endsWith('.doc') || url.endsWith('.docx') ||
    url.endsWith('.ppt') || url.endsWith('.pptx') ||
    url.endsWith('.xls') || url.endsWith('.xlsx')
  ) {
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(url)}`

    // 监听窗口关闭事件
    const studyWindow = window.open(officeUrl, '_blank')
    monitorStudyWindow(studyWindow, resourceId)
  } else if (
    url.endsWith('.pdf')
  ) {
    const pdfUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(url)}&embedded=true`
    const studyWindow = window.open(pdfUrl, '_blank')
    monitorStudyWindow(studyWindow, resourceId)
  }
}

// 监听学习窗口
const monitorStudyWindow = (studyWindow, resourceId) => {
  if (!studyWindow) return

  // 定期检查窗口是否关闭
  const checkWindowClosed = setInterval(() => {
    if (studyWindow.closed) {
      clearInterval(checkWindowClosed)
      stopStudyTimer(resourceId)
      ElMessage.info('学习窗口已关闭，学习时长已记录')
    }
  }, 1000)

  // 5分钟后自动停止检查（防止内存泄漏）
  setTimeout(() => {
    clearInterval(checkWindowClosed)
  }, 5 * 60 * 1000)
}

const downloadCourseware = async (courseware) => {
  if (!courseware.url) {
    ElMessage.warning('文件链接无效，无法下载')
    return
  }

  const studentId = getCurrentStudentId()
  if (!studentId) {
    ElMessage.error('用户信息无效')
    return
  }

  try {
    // 记录下载行为到后端
    await recordStudyBehaviorApi({
      studentId: studentId,
      resourceId: courseware.id,
      action: 'download',
      studyStatus: 3, // 3-下载行为
      timestamp: new Date().toISOString()
    })

    // 创建下载链接
    const link = document.createElement('a')
    link.href = courseware.url
    link.download = courseware.title || '课件文件'

    // 处理跨域下载
    if (courseware.url.startsWith('http') && !courseware.url.includes(window.location.hostname)) {
      // 跨域文件，在新窗口打开
      window.open(courseware.url, '_blank')
      ElMessage.success(`开始下载：${courseware.title}`)
    } else {
      // 同域文件，直接下载
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      ElMessage.success(`开始下载：${courseware.title}`)
    }

    // 更新本地统计
    await loadStudyStats()

  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请重试')
  }
}


const formatFileSize = (bytes) => {
  // 处理空值和无效值
  if (!bytes || bytes === null || bytes === undefined || isNaN(bytes) || bytes <= 0) {
    return '未知大小'
  }

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}


// 格式化学习时长显示
const formatStudyTime = (seconds) => {
  if (seconds < 60) {
    return `${seconds}秒`
  } else if (seconds < 3600) {
    return `${Math.floor(seconds / 60)}分钟`
  } else {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${hours}小时${minutes}分钟`
  }
}

// 获取当前登录学生ID
const getCurrentStudentId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
}

// 加载课件列表
const loadCoursewareList = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return

    const result = await getCoursewareListApi(studentId)
    if (result.code === 1) {
      coursewareList.value = result.data.map(item => ({
        id: item.id || 0,
        title: item.resource_name || item.resourceName || '未命名课件',
        type: item.resource_type || item.resourceType || 'file',
        teacher: item.name || '未知教师',
        uploadTime: item.upload_time || item.uploadTime || '未知时间',
        size: formatFileSize(item.file_size || item.fileSize),
        url: item.resource_url || item.resourceUrl || '',

        lastStudyTime: item.last_study_time || item.lastStudyTime || '暂无'       // 最后学习时间
      }))

      console.log('课件列表加载成功:', coursewareList.value) // 调试日志
    } else {
      console.log('课件列表API响应:', result)
      ElMessage.warning('暂无课件数据')
    }
  } catch (error) {
    console.error('加载课件列表失败:', error)
    ElMessage.error('加载课件列表失败，请刷新重试')
  }
}

// 加载学习统计数据
const loadStudyStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return

    const result = await getStudyStatsApi(studentId)
    if (result.code === 1) {
      const data = result.data
      studyStats.value[0].value = String(data.totalCourseware || 1)
      studyStats.value[1].value = String(data.studiedCourseware || 0)
      studyStats.value[2].value = String(data.aiQuestions || 1)
      studyStats.value[3].value = `${Math.floor((data.totalStudyTime || 0) / 60)}分钟`
      studyStats.value[4].value = `${Math.floor((data.todayStudyTime || 0) / 60)}分钟`
      // totalCourseware.value = data.totalCourseware || 0
      // studiedCourseware.value = data.studiedCourseware || 0
      // aiQuestions.value = data.aiQuestions || 0
      // totalStudyTime.value = Math.floor((data.totalStudyTime || 0) / 60) // 转换为分钟
      // todayStudyTime.value = Math.floor((data.todayStudyTime || 0) / 60) // 转换为分钟
    }
  } catch (error) {
    console.error('加载学习统计失败:', error)
  }
}

// 加载学习时长趋势数据
const loadTrendData = async () => {
  const studentId = getCurrentStudentId()
  const res = await getStudyTimeTrendApi(studentId, { days: 7 })
  if (res.code === 1) {
    trendOption.value = {
      title: { text: '近7天学习时长趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: res.data.map(item => item.date)
      },
      yAxis: {
        type: 'value',
        name: '学习时长（分钟）'
      },
      series: [
        {
          name: '学习时长',
          type: 'line',
          data: res.data.map(item => item.studyMinutes),
          smooth: true,
          areaStyle: {}
        }
      ]
    }
  }
}

// 页面可见性变化监听
const handleVisibilityChange = () => {
  if (document.hidden) {
    // 页面隐藏时暂停所有计时器
    Object.keys(studyTimer.value).forEach(resourceId => {
      if (studyTimer.value[resourceId]) {
        clearInterval(studyTimer.value[resourceId])
        updateStudyProgress(parseInt(resourceId))
      }
    })
  } else {
    // 页面显示时恢复计时器
    activeStudyResources.value.forEach(resourceId => {
      if (!studyTimer.value[resourceId]) {
        studyStartTime.value[resourceId] = Date.now()
        studyTimer.value[resourceId] = setInterval(() => {
          updateStudyProgress(resourceId)
        }, 30000)
      }
    })
  }
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

// 获取题型名称
const getTypeName = (type) => {
  const typeMap = {
    choice: '选择题',
    short: '简答题',
    code: '编程题'
  }
  return typeMap[type] || '未知题型'
}

const removeQuestion = (id) => {
  questions.value = questions.value.filter(q => q.id !== id)
}

const clearQuestions = () => {
  questions.value = []
  tempIdCounter.value = -1 // 可选：重置 ID 起点
}

// 初始化
onMounted(() => {
  console.log('学生学习模块初始化')
  loadCoursewareList()
  loadStudyStats()
  // 启动实时学习时长显示
  startRealTimeDisplay()
  // 监听页面可见性变化
  document.addEventListener('visibilitychange', handleVisibilityChange)
  // 监听页面刷新和关闭
  window.addEventListener('beforeunload', stopAllStudyTimers)
})

// 清理
onBeforeUnmount(() => {
  console.log('学习模块清理...')
  stopAllStudyTimers()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', stopAllStudyTimers)
})


const getParsedQuestion = (content) => {
  const [questionText, ...options] = content.split(/(?=A\.)/); // 从 A. 开始截断
  return {
    text: questionText.trim(),
    options: options.join('').trim().split(/(?=[A-D]\.)/),
  };
}
// watch(
//   () => q.content,
//   (newContent) => {
//     if (q.type === 'choice' && newContent) {
//       const parsed = getParsedQuestion(newContent);
//       q.text = parsed.text;
//       q.options = parsed.options;
//     }
//   },
//   { immediate: true }
// );
</script>

<template>
  <div class="student-container">
    <!-- 页面标题 -->
    <div class="student-section">
      <h1 class="student-title large">
        <i class="fas fa-book"></i>
        学习模块
      </h1>
      <p class="student-text secondary">探索丰富的学习资源</p>
    </div>

    <!-- 学习统计区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-chart-bar"></i>
        学习统计
      </h2>
      <div class="student-grid three-columns stat-row">
        <div class="student-card stat-card" v-for="(stat, index) in studyStats" :key="index">
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

    <!-- 学习计时器区域 -->
    <div class="student-section" v-if="activeStudyResources.size > 0">
      <h2 class="student-title medium">
        <i class="fas fa-clock"></i>
        正在学习
      </h2>
      <div class="student-card timer-card">
        <div class="timer-header">
          <div class="timer-title">
            <i class="fas fa-play-circle"></i>
            学习计时器
          </div>
          <button class="student-button danger" @click="stopAllStudyTimers">
            <i class="fas fa-stop"></i>
            全部停止
          </button>
        </div>
        <div class="timer-list">
          <div v-for="resourceId in Array.from(activeStudyResources)" :key="resourceId" class="timer-item">
            <div class="timer-info">
              <div class="resource-title">{{ getResourceName(resourceId) }}</div>
              <div class="timer-display">{{ formatRealTimeStudyTime(resourceId) }}</div>
            </div>
            <div class="timer-actions">
              <button class="student-button warning" @click="pauseStudyTimer(resourceId)">
                <i class="fas fa-pause"></i>
                暂停
              </button>
              <button class="student-button danger" @click="stopStudyTimer(resourceId)">
                <i class="fas fa-stop"></i>
                停止
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 课件学习区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-folder-open"></i>
        课程课件
      </h2>
      <div class="student-grid auto-fit">
        <div
          v-for="courseware in coursewareList"
          :key="courseware.id"
          class="student-card courseware-card"
        >
          <div class="courseware-header">
            <div class="file-icon">
              <i v-if="courseware.type === 'pdf'" class="fas fa-file-pdf"></i>
              <i v-else-if="courseware.type === 'ppt'" class="fas fa-file-powerpoint"></i>
              <i v-else-if="courseware.type === 'video'" class="fas fa-file-video"></i>
              <i v-else class="fas fa-file"></i>
            </div>
            <div class="courseware-title">{{ courseware.title }}</div>
          </div>
          <div class="courseware-meta">
            <div class="meta-item">
              <i class="fas fa-user"></i>
              <span>{{ courseware.teacher }}</span>
            </div>
            <div class="meta-item">
              <i class="fas fa-calendar"></i>
              <span>{{ courseware.uploadTime }}</span>
            </div>
            <div class="meta-item">
              <i class="fas fa-weight-hanging"></i>
              <span>{{ courseware.size }}</span>
            </div>
          </div>
          <div class="courseware-actions">
            <button class="student-button" @click.stop="handlePreview(courseware)">
              <i class="fas fa-eye"></i>
              预览
            </button>
            <button class="student-button secondary" @click.stop="downloadCourseware(courseware)">
              <i class="fas fa-download"></i>
              下载
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- AI题目生成区域 -->
    <div class="student-section">
      <h2 class="student-title medium">
        <i class="fas fa-robot"></i>
        AI题目生成
      </h2>
      <div class="student-card ai-card">
        <div class="ai-header">
          <div class="ai-title">
            <i class="fas fa-magic"></i>
            智能题目生成
          </div>
          <button class="student-button" @click="showAIDialog">
            <i class="fas fa-plus"></i>
            生成题目
          </button>
        </div>
        <div class="ai-content">
          <p class="student-text secondary">基于学习内容，AI可以为您生成个性化的练习题</p>
        </div>
      </div>



      <div class="question-display-wrapper">
        <div
          class="question-display-card"
          v-for="(q, index) in questions"
          :key="q.id"
        >
          <div class="question-top">
            <h4 class="question-title">题目 {{ index + 1 }}</h4>
              <el-tag :type="getTypeColor(q.type)" size="small" class="question-tag" style="margin-left: 10px;">
                {{ getTypeName(q.type) }}
              </el-tag>
              <el-button :type="getTypeColor(q.type)" size="small" class="question-tag" style=" margin-left: auto; color: red; font-weight: 400;"  @click="removeQuestion(q.id) ">
               删除 
              </el-button>
          </div>

          <div class="question-inner-content glass-box">

            <div class="question-display" v-if="q.type === 'choice'">
              <div class="question-text">{{ getParsedQuestion(q.content).text }}</div>
              <ul class="option-list">
                <li v-for="(opt, idx) in getParsedQuestion(q.content).options" :key="idx">{{ opt }}</li>
              </ul>
            </div>
            <div class="question-content" v-else >{{ q.content }}</div>


            <div class="question-meta">
              <span class="label">知识点：</span>
              <span class="value">{{ q.knowledge }}</span>
            </div>

            <div class="question-answer-block" v-if="q.answer">
              <span class="label" style="margin-left: 6px;">参考答案：</span>
              <div class="glass-answer">{{ q.answer }}</div>
            </div>

            <div class="question-explain-block glass-box" v-if="q.explain">
              <span class="label">解析：</span>
              <div class="explain-text">{{ q.explain }}</div>
            </div>
          </div>
        </div>
      </div>




    </div>

    <!-- 学习统计详情对话框 -->
    <el-dialog v-model="showStatsDialog" title="学习统计详情" width="800px" @opened="onStatsDialogOpened">
      <div class="stats-detail">
        <!-- 今日学习概况 -->
        <div class="stats-section">
          <h4>今日学习概况</h4>
          <div class="today-stats">
            <div class="stat-card">
              <div class="stat-number">{{ todayStudyTime }}</div>
              <div class="stat-desc">今日学习时长（分钟）</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ todayStudyResources }}</div>
              <div class="stat-desc">今日学习资源数</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ todayAiQuestions }}</div>
              <div class="stat-desc">今日AI提问数</div>
            </div>
          </div>
        </div>

        <!-- 学习时长趋势 -->
        <div class="stats-section">
          <div class="chart-container">
            <vue-echarts v-if="trendChartVisible" :option="trendOption" style="height: 300px; width: 100%" />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- AI对话框 -->
    <el-dialog v-model="showAIDialogVisible" title="AI题目生成" width="600px" @close="handleCloseAIDialog">
      <el-form ref="aiFormRef" :model="aiFormData" label-width="100px">
        <el-form-item label="知识点" prop="knowledge" required>
          <el-input v-model="aiFormData.knowledge" placeholder="请输入知识点，如：Java基础语法" />
        </el-form-item>
        <el-form-item label="题目类型" prop="type" required>
          <el-select v-model="aiFormData.type" placeholder="请选择题目类型" style="width: 100%">
            <el-option label="选择题" value="choice" />
            <el-option label="简答题" value="short" />
            <el-option label="编程题" value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目数量" prop="count">
          <el-input-number v-model="aiFormData.count" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="aiFormData.remark" type="textarea" placeholder="可选：对题目的特殊要求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseAIDialog">取消</el-button>
        <el-button type="primary" @click="handleAIGenerate" :loading="isGenerating">
          {{ isGenerating ? '生成中...' : '生成题目' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.question-display {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(16px);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  color: #222;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.question-text {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 12px;
}

.option-list {
  list-style-type: none;
  padding-left: 0;
}

.option-list li {
  padding: 6px 0;
  font-size: 15px;
  color: #333;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.08);
}


.question-display-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 20px;
}

.question-display-card {
  background: linear-gradient(135deg, rgba(128, 183, 255, 0.4), rgba(102, 161, 255, 0.5));
  backdrop-filter: blur(24px);
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.1);
  color: #222;
  transition: all 0.3s ease;
}

.question-top {
  display: flex;
  /* justify-content: space-between; */
  align-items: center;
  margin-bottom: 12px;
}

.question-title {
  font-size: 18px;
  font-weight: 700;
  color: #6b1ea0;
  margin: 0;
}

.question-tag {
  font-weight: 100;
  background: rgba(255, 255, 255, 0.25);
  border: none; 
  color: white;
  font-size: 15px;
  padding: 2px 10px;
  border-radius: 6px;
}

/* 内部内容整体玻璃壳 */
.question-inner-content.glass-box {
  padding: 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(16px);
  box-shadow: inset 0 0 12px rgba(0, 0, 0, 0.05);
}

/* 单项内容 */
.question-content {
  margin-bottom: 12px;
  font-size: 16px;
  line-height: 1.7;
}

.question-meta {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  border-left: 4px solid #409eff;
  padding: 10px 14px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.05);
  color: #2c3e50;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-meta .label {
  font-weight: 600;
  color: #333;
}

.question-meta .value {
  font-weight: 500;
  color: #222;
}

.question-answer-block {
  margin-bottom: 16px;
}

/* 参考答案玻璃样式 */
.glass-answer {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(12px);
  padding: 12px 16px;
  border-radius: 12px;
  font-family: 'Courier New', monospace;
  font-size: 15px;
  color: #2c3e50;
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 解析区域玻璃样式 */
.question-explain-block {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(14px);
  border-left: 4px solid #e67e22;
  border-radius: 12px;
  padding: 12px 16px;
  margin-top: 12px;
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.1);
}

.question-explain-block .label {
  display: block;
  font-weight: bold;
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
}

.explain-text {
  font-size: 15px;
  color: #444;
  white-space: pre-wrap;
  line-height: 1.6;
}

.explain-text {
  white-space: pre-wrap;
  padding-top: 4px;
}

.label {
  font-weight: bold;
  color: #333;
  display: inline-block;
  margin-bottom: 4px;
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

/* 统计卡片横排布局 */
.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
}

.stat-row .stat-card {
  flex: 1 1 0;
  min-width: 160px;
  max-width: 220px;
}

@media (max-width: 900px) {
  .stat-row {
    flex-wrap: wrap;
    gap: 12px;
  }
  .stat-row .stat-card {
    min-width: 140px;
    max-width: 100%;
  }
}

/* 计时器卡片样式 */
.timer-card {
  padding: 24px;
}

.timer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--student-glass-border);
}

.timer-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.timer-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--student-glass);
  border-radius: var(--student-border-radius-small);
  border: 1px solid var(--student-glass-border);
  transition: all var(--student-animation);
}

.timer-item:hover {
  background: var(--student-card-hover);
  /* transform: translateX(8px); */
}

.timer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.resource-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--student-text);
  margin-bottom: 4px;
}

.timer-display {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
}

.timer-actions {
  display: flex;
  gap: 8px;
}

/* 课件卡片样式 */
.courseware-card {
  padding: 20px;
  animation: student-slide-up 0.8s cubic-bezier(.4, 0, .2, 1);
  animation-delay: calc(var(--index, 0) * 0.1s);
  animation-fill-mode: both;
}

.courseware-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.file-icon {
  font-size: 24px;
  margin-right: 12px;
  color: var(--student-text);
  flex-shrink: 0;
}

.courseware-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--student-text);
  line-height: 1.4;
}

.courseware-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
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

.courseware-actions {
  display: flex;
  gap: 8px;
}

/* AI卡片样式 */
.ai-card {
  padding: 24px;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-content {
  color: var(--student-text-secondary);
}

/* 按钮样式扩展 */
.student-button.warning {
  background: var(--gradient-warning);
}

.student-button.danger {
  background: var(--gradient-danger);
}

.student-button.secondary {
  background: var(--gradient-secondary);
}

/* 统计详情对话框样式 */
.stats-detail {
  max-height: 600px;
  overflow-y: auto;
}

.stats-section {
  margin-bottom: 30px;
}

.stats-section h4 {
  margin-bottom: 15px;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.today-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: white;
}

.stat-desc {
  font-size: 14px;
  opacity: 0.9;
  color: white;
}

.chart-container {
  height: 200px;
  background: #f8f9fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #e0e0e0;
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
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .timer-header,
  .ai-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .timer-actions {
    flex-direction: column;
  }
  
  .courseware-actions {
    flex-direction: column;
  }
  
  .today-stats {
    grid-template-columns: 1fr;
  }
}
.student-section > .student-title.medium {
  font-size: clamp(18px, 4vw, 24px);
  margin-bottom: 32px !important;
}

.student-section > .student-title.medium + .student-grid.three-columns {
  margin-top: 32px !important;
}

/* 针对作业模块标题（假设为“作业”或“作业统计”）增加间距 */
.student-section > .student-title.homework {
  margin-bottom: 24px;
}
</style>
