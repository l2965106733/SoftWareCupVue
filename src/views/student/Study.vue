<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getCoursewareListApi, getStudyStatsApi, recordStudyBehaviorApi, getQuestionApi, getStudyRecordsApi } from '@/api/student'
import { CircleClose } from '@element-plus/icons-vue'

// AI对话框相关
const showAIDialogVisible = ref(false)
const isGenerating = ref(false)
const aiFormRef = ref()
const questions = ref([
  {
    id: -1,
    type: 'choice',
    content: '以下哪个关键字用于创建 Java 类的实例？',
    knowledge: 'Java基础语法',
    answer: 'new',
    explain: '在 Java 中，使用 new 关键字可以创建类的实例。',
  },
  {
    id: -2,
    type: 'short',
    content: '简要说明 Java 中的多态特性。',
    knowledge: 'Java面向对象',
    answer: '多态是指相同的接口，不同的实现。可以通过方法重写或接口实现实现多态。',
    explain: '多态提高了程序的扩展性和可维护性，是面向对象编程的重要特性。',
  },
  {
    id: -3,
    type: 'code',
    content: '编写一个 Java 方法，判断一个整数是否为质数。',
    knowledge: 'Java算法基础',
    answer:
`public boolean isPrime(int n) {
  if (n <= 1) return false;
  for (int i = 2; i <= Math.sqrt(n); i++) {
    if (n % i == 0) return false;
  }
  return true;
}`,
    explain: '判断质数的常用方法是从 2 遍历到 √n，若存在能整除 n 的数，则不是质数。',
  }
])

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
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(courseware.url)}`

    // 监听窗口关闭事件
    const studyWindow = window.open(officeUrl, '_blank')
    monitorStudyWindow(studyWindow, resourceId)
  } else {
    const studyWindow = window.open(courseware.url, '_blank')
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
      totalCourseware.value = data.totalCourseware || 0
      studiedCourseware.value = data.studiedCourseware || 0
      aiQuestions.value = data.aiQuestions || 0
      totalStudyTime.value = Math.floor((data.totalStudyTime || 0) / 60) // 转换为分钟
      todayStudyTime.value = Math.floor((data.todayStudyTime || 0) / 60) // 转换为分钟
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
</script>

<template>
  <div class="student-study-layout">

    <!-- 右侧内容区域 -->
    <div class="right-panel">
      <!-- 学习计时器 -->
      <div class="study-timer-section" v-if="activeStudyResources.size > 0">
        <el-card shadow="hover">
          <div class="timer-header">
            <h4>
              <el-icon>
                <Timer />
              </el-icon>
              正在学习
            </h4>
            <el-button type="danger" size="small" @click="stopAllStudyTimers">
              <el-icon>
                <CircleClose />
              </el-icon>
              全部停止
            </el-button>
          </div>
          <div class="active-timers">
            <div v-for="resourceId in Array.from(activeStudyResources)" :key="resourceId" class="timer-item">
              <div class="timer-info">
                <div class="resource-title">{{ getResourceName(resourceId) }}</div>
                <div class="timer-display">{{ formatRealTimeStudyTime(resourceId) }}</div>
              </div>
              <div class="timer-actions">
                <el-button type="warning" size="small" @click="pauseStudyTimer(resourceId)">
                  <el-icon>
                    <VideoPause />
                  </el-icon>
                  暂停
                </el-button>
                <el-button type="danger" size="small" @click="stopStudyTimer(resourceId)">
                  <el-icon>
                    <CircleClose />
                  </el-icon>
                  停止
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 学习统计 -->
      <div class="stats-section">
        <el-card shadow="hover">
          <div class="card-header">
            <h4>
              <el-icon>
                <DataAnalysis />
              </el-icon>
              学习统计
            </h4>
            <el-button type="primary" size="small" @click="showStudyStatsDetail">
              <el-icon>
                <View />
              </el-icon>
              查看详情
            </el-button>
          </div>
          <div class="study-stats">
            <div class="stat-item">
              <div class="stat-value">{{ totalCourseware }}</div>
              <div class="stat-label">课件总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ studiedCourseware }}</div>
              <div class="stat-label">已学习</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ aiQuestions }}</div>
              <div class="stat-label">AI提问数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ totalStudyTime }}分钟</div>
              <div class="stat-label">总学习时长</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ todayStudyTime }}分钟</div>
              <div class="stat-label">今日学习</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 课件学习区域 -->
      <div class="courseware-section">
        <el-card shadow="hover">
          <div class="section-header">
            <h3>
              <el-icon>
                <FolderOpened />
              </el-icon>
              课程课件
            </h3>
          </div>

          <div class="courseware-list">
            <div
              v-for="courseware in coursewareList"
              :key="courseware.id"
              class="courseware-item"
            >
            
              <div class="courseware-info">
                <div class="file-icon">
                  <el-icon v-if="courseware.type === 'pdf'">
                    <Document />
                  </el-icon>
                  <el-icon v-else-if="courseware.type === 'ppt'">
                    <Monitor />
                  </el-icon>
                  <el-icon v-else-if="courseware.type === 'video'">
                    <VideoPlay />
                  </el-icon>
                  <el-icon v-else>
                    <Files />
                  </el-icon>
                </div>
                <div class="courseware-details">
                  <h4>{{ courseware.title }}</h4>
                  <div class="courseware-meta">
                    <span class="teacher-name">发布教师：{{ courseware.teacher }}</span>
                    <span class="upload-time">上传时间：{{ courseware.uploadTime }}</span>
                    <span class="file-size">文件大小：{{ courseware.size }}</span>
                  </div>
                </div>
              </div>

              <div class="courseware-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="handlePreview(courseware)"
                >
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
                <el-button
                  type="success"
                  size="small"
                  @click.stop="downloadCourseware(courseware)"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </div>
            </div>
          </div>

        </el-card>
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
      <!-- 移除footer按钮 -->
    </el-dialog>
  </div>

  <div class="control-section">
    <el-card shadow="hover">
      <div class="control-header">
        <h3>题目生成(自行保存)</h3>
      </div>

      <div class="button-group">
        <el-button type="primary" @click="showAIDialog" size="large">
          <el-icon>
            <MagicStick />
          </el-icon>
          AI 生成题目
        </el-button>
        <el-button type="primary" @click="clearQuestions" size="large">
          <el-icon>
            <MagicStick />
          </el-icon>
          清空生成题目
        </el-button>
      </div>
    </el-card>
    
  </div>

  <div class="question-block" v-for="(q, index) in questions" :key="q.id">
  <div class="question-header">
    <h4>题目 {{ index + 1 }}</h4>
    <div>
      <el-tag :type="getTypeColor(q.type)" size="small">{{ getTypeName(q.type) }}</el-tag>
    </div>
  </div>

  <div class="question-content">
    <p><strong>题干：</strong>{{ q.content }}</p>
    <p><strong>知识点：</strong>{{ q.knowledge }}</p>
    <p><strong>答案：</strong>{{ q.answer }}</p>
    <p><strong>解析：</strong>{{ q.explain }}</p>
  </div>

  <div class="question-actions">
    <el-button type="danger" size="small" @click="removeQuestion(q.id)">
      <el-icon><Close /></el-icon>
      删除
    </el-button>
  </div>

  <el-divider />
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
</template>

<style scoped>
.student-study-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.left-panel {
  flex: 2;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
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

/* 区块头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.section-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
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

/* 控制区域 */
.control-section {
  flex-shrink: 0;
}

.control-header {
  margin-bottom: 20px;
  text-align: center;
}

.control-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
}

.button-group {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.button-group .el-button {
  height: 60px;
  font-size: 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.button-group .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}


/* 学习统计 */
.stats-section {
  flex-shrink: 0;
}

h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.study-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
  color: white;
}

.stat-label {
  font-size: 13px;
  opacity: 0.9;
  color: white;
}

/* 课件列表 */
.courseware-section {
  flex: 1;
}

.courseware-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.courseware-item {

  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8f4fd;
  border-radius: 12px;
  background: #f8fcff;
  transition: all 0.3s ease;
  cursor: default;
}

.courseware-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.courseware-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.file-icon {
  font-size: 28px;
  color: #409eff;
}

.courseware-details h4 {
  margin: 0 0 6px 0;
  color: #2c3e50;
  font-size: 14px;
  font-weight: 600;
}

.courseware-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}

.courseware-meta span {
  padding: 2px 0;
}

.study-progress {
  color: #409eff !important;
  font-weight: 500;
}

.last-study-time {
  color: #67c23a !important;
  font-weight: 500;
}

.courseware-actions {
  display: flex;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .student-study-layout {
    gap: 16px;
    padding: 16px;
  }
}

@media (max-width: 1200px) {
  .student-study-layout {
    flex-direction: column;
  }

  .left-panel,
  .right-panel {
    flex: 1;
  }

  .study-stats {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .study-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .courseware-item {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .courseware-info {
    justify-content: center;
  }

  .courseware-actions {
    justify-content: center;
  }
}

/* 学习统计区域样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
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

.resource-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.resource-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.progress-percent {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.progress-time {
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}

.knowledge-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.knowledge-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #67c23a;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.mastery-rate {
  font-size: 14px;
  font-weight: 600;
  color: #67c23a;
}

.knowledge-details {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}

.knowledge-details span {
  padding: 2px 6px;
  background: #e9ecef;
  border-radius: 4px;
}

/* 学习计时器样式 */
.study-timer-section {
  margin-bottom: 24px;
}

.timer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.timer-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.active-timers {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.timer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.resource-title {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 4px;
}

.timer-display {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.timer-actions {
  display: flex;
  gap: 8px;
}

.timer-actions .el-button {
  padding: 6px 12px;
}
</style>
