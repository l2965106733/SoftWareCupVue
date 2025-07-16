<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiQuestionHistoryApi, getChatApi, getCoursewareListApi, getStudyStatsApi, recordStudyBehaviorApi, recordAiQuestionApi, getStudyRecordsApi } from '@/api/student'
import { CircleClose } from '@element-plus/icons-vue'


// AI聊天相关
const inputMessage = ref('')
const selectedFiles = ref([])  // 改为数组支持多个文件
const sending = ref(false)
const aiTyping = ref(false)
const chatMessages = ref(null)
const fileUpload = ref(null)
const uploadHeaders = computed(() => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}');
  return {
    token: loginUser.token || ''
  };
});

// 课件数据
const coursewareList = ref([])

// 学习时长跟踪相关
const studyTimer = ref({})          // 存储每个资源的计时器
const studyStartTime = ref({})      // 存储学习开始时间
const studySessionTime = ref({})    // 存储本次学习会话时间
const activeStudyResources = ref(new Set()) // 当前正在学习的资源
const pausedStudyResources = ref(new Set()) // 暂停的学习资源
const realTimeStudyTime = ref({})   // 实时学习时长显示

// AI对话历史
const chatHistory = ref([
  {
    id: 'welcome',
    sender: 'ai',
    type: 'ai-reply',
    content: '你好！我是你的AI学习助手，可以帮你解答学习中的各种问题。有什么问题尽管问我吧！',
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
])

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


const beforeUpload = (file) => { // 检查文件大小（限制10MB）
  const maxSize = 10 * 1024 * 1024; // 10MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB');
    return false; // 阻止文件上传
  }

  // 检查文件数量（限制5个文件）
  if (selectedFiles.value.length >= 5) {
    ElMessage.error('最多只能上传5个文件');
    return false; // 阻止文件上传
  }

  ElMessage.success(`已选择${selectedFiles.value.length + 1}个文件`);
  return true; // 允许上传
};

const handleUploadSuccess = (res, file) => {
  console.log("文件上传成功", res, file)
  // 上传成功时的回调处理
  if (res.code === 1) {
    file.url = res.data.url || res.data; // 设置上传成功后的文件 URL
  }
  console.log("文件上传成功", file);
};

const removeFile = (file) => {
  if (file) {
    // 移除指定文件
    const index = selectedFiles.value.findIndex(f => f.uid === file.uid)
    if (index > -1) {
      selectedFiles.value.splice(index, 1)
    }
  } else {
    // 清空所有文件
    selectedFiles.value = []
    if (fileUpload.value) {
      fileUpload.value.clearFiles()
    }
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() && selectedFiles.value.length === 0) {
    ElMessage.warning('请输入问题或上传文件')
    return
  }

  sending.value = true

  // 添加用户消息
  const userMessage = {
    id: Date.now(),
    sender: 'user',
    type: selectedFiles.value.length > 0 ? 'file' : 'text',
    content: inputMessage.value || (selectedFiles.value.length > 0 ?
      `上传了${selectedFiles.value.length}个文件：${selectedFiles.value.map(f => f.name).join(', ')}` : ''),
    fileNames: selectedFiles.value.map(f => f.name),
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  chatHistory.value.push(userMessage)

  // 保存问题信息和文件URLs
  const question = inputMessage.value
  const fileUrls = selectedFiles.value.length > 0 ?
    selectedFiles.value.map(file => file.url).filter(url => url) : []  // 文件URL数组

  // 清空输入
  inputMessage.value = ''
  removeFile()  // 清空所有文件

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 显示AI正在输入
  aiTyping.value = true

  try {
    const result = await getChatApi({
      question: question,
      fileUrls: fileUrls  // 传递文件URL数组
    })

    if (result.code === 1 && result.data) {
      // 添加AI回复消息，直接使用返回的纯文本
      const aiMessage = {
        id: Date.now() + 1,
        sender: 'ai',
        type: 'ai-reply',
        content: result.data,  // 直接使用返回的字符串
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      chatHistory.value.push(aiMessage)
      ElMessage.success('AI回复成功！')

      // 记录AI提问到后端
      try {
        const studentId = getCurrentStudentId()
        if (studentId) {
          await recordAiQuestionApi({
            studentId: studentId,
            questionContent: userMessage.content,
            answer: result.data  // 使用纯文本答案测问题分类
          })
        }
      } catch (error) {
        console.error('记录AI提问失败:', error)
      }

      // 更新统计数据
      loadStudyStats()
    } else {
      ElMessage.error(result.msg || '生成失败')
    }

  } catch (error) {
    ElMessage.error('AI回复失败，请重试')
  } finally {
    aiTyping.value = false
    sending.value = false
    await nextTick()
    scrollToBottom()
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

const scrollToBottom = () => {
  if (chatMessages.value) {
    chatMessages.value.scrollTop = chatMessages.value.scrollHeight
  }
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

// 加载AI聊天历史
const loadChatHistory = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    const result = await getAiQuestionHistoryApi(studentId, { limit: 50 })
    if (result.code === 1 && Array.isArray(result.data)) {
      // 历史消息：每条数据库记录拆分为两条消息
      const history = []
      result.data.forEach(item => {
        history.push({
          id: `q_${item.id}`,
          sender: 'user',
          type: 'user-question',
          content: item.questionContent,
          time: new Date(item.createdTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        })
        history.push({
          id: `a_${item.id}`,
          sender: 'ai',
          type: 'ai-reply',
          content: item.aiResponse,
          time: new Date(item.createdTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        })
      })
      // 保留开场白+历史
      chatHistory.value = [chatHistory.value[0], ...history]
    }
  } catch (error) {
    console.error('加载AI聊天历史失败:', error)
  }
}


// 初始化
onMounted(() => {
  console.log('学生学习模块初始化')
  loadCoursewareList()
  loadStudyStats()
  loadChatHistory() // 加载AI聊天历史
  scrollToBottom()

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
    <!-- 左侧AI助手区域 -->
    <div class="left-panel">
      <!-- AI互动提问区域 -->
      <div class="ai-section">
        <el-card shadow="hover">
          <div class="section-header">
            <h3>
              <el-icon>
                <ChatDotRound />
              </el-icon>
              AI学习助手
            </h3>
          </div>

          <div class="chat-container">
            <div ref="chatMessages" class="chat-messages">
              <div v-for="message in chatHistory" :key="message.id" class="message-item" :class="message.sender">
                <!-- 用户消息 -->
                <div v-if="message.sender === 'user'" class="user-message">
                  <div class="message-content">
                    {{ message.content }}
                    <!-- 显示上传的文件信息 -->
                    <div v-if="message.fileNames && message.fileNames.length > 0" class="message-files">
                      <div class="files-indicator">
                        <el-icon>
                          <Paperclip />
                        </el-icon>
                        附件 ({{ message.fileNames.length }})
                      </div>
                      <div class="file-names">
                        <span v-for="fileName in message.fileNames" :key="fileName" class="file-name">
                          {{ fileName }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div class="message-time">{{ message.time }}</div>
                </div>

                <!-- AI回复 -->
                <div v-else class="ai-message">
                  <div class="message-content">{{ message.content }}</div>
                  <div class="message-time">{{ message.time }}</div>
                </div>
              </div>

              <!-- AI正在输入 -->
              <div v-if="aiTyping" class="message-item ai typing">
                <div class="ai-message">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="chat-input">
              <div class="input-toolbar">
                <el-upload v-model:file-list="selectedFiles" action="/api/upload" list-type="text"
                  :headers="uploadHeaders" :on-success="handleUploadSuccess" accept=".pdf,.docx" :limit="5" multiple
                  class="upload-demo" :show-file-list="false" :before-upload="beforeUpload">
                  <el-button type="info" size="small" text>
                    <el-icon>
                      <Paperclip />
                    </el-icon>
                    上传文件 (最多5个)
                  </el-button>
                </el-upload>

                <div class="file-preview" v-if="selectedFiles.length > 0">
                  <div class="file-list-header">
                    <span>已选择 {{ selectedFiles.length }} 个文件</span>
                    <el-button type="danger" size="small" text @click="removeFile()">
                      <el-icon>
                        <Delete />
                      </el-icon>
                      清空全部
                    </el-button>
                  </div>
                  <div class="file-list">
                    <div class="file-item" v-for="file in selectedFiles" :key="file.uid">
                      <div class="file-preview-icon">
                        <el-icon v-if="file.type?.includes('image')">
                          <Picture />
                        </el-icon>
                        <el-icon v-else-if="file.type?.includes('pdf')">
                          <Document />
                        </el-icon>
                        <el-icon v-else>
                          <Paperclip />
                        </el-icon>
                      </div>
                      <div class="file-preview-info">
                        <span class="file-preview-name">{{ file.name }}</span>
                        <span class="file-preview-size">{{ formatFileSize(file.size) }}</span>
                      </div>
                      <el-button type="danger" size="small" text @click="removeFile(file)">
                        <el-icon>
                          <Close />
                        </el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="message-input">
                <el-input v-model="inputMessage" type="textarea" :rows="3" placeholder="请输入您的问题..."
                  @keydown.ctrl.enter="sendMessage" :disabled="sending" />
                <el-button type="primary" @click="sendMessage" :loading="sending" class="send-button">
                  <el-icon>
                    <Promotion />
                  </el-icon>
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

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
            <div v-for="courseware in coursewareList" :key="courseware.id" class="courseware-item"
              @click="handlePreview(courseware)">
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

              <div class="courseware-actions" @click.stop>
                <el-button type="success" size="small" @click="downloadCourseware(courseware)">
                  <el-icon>
                    <Download />
                  </el-icon>
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

/* AI聊天区域 */
.ai-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-section .el-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-section .el-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f9f9f9;
  margin-bottom: 16px;
}

.message-item {
  margin-bottom: 16px;
}

.message-item.user {
  text-align: right;
}

.message-item.ai {
  text-align: left;
}

.user-message,
.ai-message {
  display: inline-block;
  max-width: 70%;
  text-align: left;
}

.user-message .message-content {
  background: #409eff;
  color: white;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  word-wrap: break-word;
}

.ai-message .message-content {
  background: white;
  color: #333;
  padding: 12px 16px;
  border-radius: 18px 18px 18px 4px;
  border: 1px solid #e0e0e0;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 聊天消息中的文件显示样式 */
.message-files {
  margin-top: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.files-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 6px;
  font-weight: 500;
}

.file-names {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  word-break: break-all;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border-radius: 18px 18px 18px 4px;
  border: 1px solid #e0e0e0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {

  0%,
  80%,
  100% {
    transform: scale(0);
    opacity: 0.5;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-input {
  border-top: 1px solid #e0e0e0;
  padding-top: 16px;
}

.input-toolbar {
  margin-bottom: 12px;
}

.file-preview {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
  font-size: 13px;
  font-weight: 500;
}

.file-list-header span {
  color: #2c3e50;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  border: 1px solid #e8e8e8;
  transition: all 0.2s ease;
}

.file-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.file-preview-icon {
  color: #409eff;
}

.file-preview-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.file-preview-name {
  font-weight: 600;
  color: #333;
}

.file-preview-size {
  font-size: 12px;
  color: #666;
}

.message-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input .el-textarea {
  flex: 1;
}

.send-button {
  height: 40px;
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
  cursor: pointer;
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
