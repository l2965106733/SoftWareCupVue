<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getChatApi, getCoursewareListApi, getStudyStatsApi, recordStudyBehaviorApi, recordAiQuestionApi } from '@/api/student'

// 对话框
const showCoursewareDialog = ref(false)
const currentCourseware = ref(null)

// AI聊天相关
const inputMessage = ref('')
const selectedFile = ref(null)
const sending = ref(false)
const aiTyping = ref(false)
const chatMessages = ref(null)
const fileUpload = ref(null)

// 课件数据
const coursewareList = ref([])

// AI对话历史
const chatHistory = ref([
  {
    id: 1,
    sender: 'ai',
    type: 'ai-reply',
    content: '你好！我是你的AI学习助手，可以帮你解答学习中的各种问题。有什么问题尽管问我吧！',
    time: '09:00'
  }
])

// 统计数据
const totalCourseware = ref(0)
const studiedCourseware = ref(0)
const aiQuestions = ref(0)

// 方法
const handlePreview = async (courseware) => {
  if (!courseware.url) return ElMessage.warning('文件未就绪')
  
  // 记录学习行为
  try {
    const studentId = getCurrentStudentId()
    if (studentId) {
      await recordStudyBehaviorApi({
        studentId: studentId,
        resourceId: courseware.id,
        studyStatus: 1 // 已查看
      })
      
      // 更新本地统计（可选，主要依赖后端统计）
      loadStudyStats()
    }
  } catch (error) {
    console.error('记录学习行为失败：', error)
  }
  
  const url = courseware.url.toLowerCase()

  if (
    url.endsWith('.doc') || url.endsWith('.docx') ||
    url.endsWith('.ppt') || url.endsWith('.pptx') ||
    url.endsWith('.xls') || url.endsWith('.xlsx')
  ) {
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(courseware.url)}`
    window.open(officeUrl, '_blank')
  } else {
    window.open(courseware.url, '_blank') // 其它如 PDF 直接打开
  }
}

const downloadCourseware = (courseware) => {
  ElMessage.success(`开始下载：${courseware.title}`)
  // 这里实现下载逻辑
}

const handleFileSelect = (file, fileList) => {
  // 检查文件大小（限制10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  
  selectedFile.value = file
  ElMessage.success(`已选择文件：${file.name}`)
  return true
}

const removeFile = () => {
  selectedFile.value = null
  if (fileUpload.value) {
    fileUpload.value.clearFiles()
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() && !selectedFile.value) {
    ElMessage.warning('请输入问题或上传文件')
    return
  }

  sending.value = true
  
  // 添加用户消息
  const userMessage = {
    id: Date.now(),
    sender: 'user',
    type: selectedFile.value ? 'file' : 'text',
    content: inputMessage.value || (selectedFile.value ? `上传了文件：${selectedFile.value.name}` : ''),
    fileName: selectedFile.value?.name,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  chatHistory.value.push(userMessage)
  
  // 保存问题信息
  const question = inputMessage.value
  
  // 清空输入
  inputMessage.value = ''
  removeFile()
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  // 显示AI正在输入
  aiTyping.value = true
  
  try {
    const result = await getChatApi({question: userMessage.content,fileUrl: selectedFile.value?.url})
    if (result.code === 1 && Array.isArray(result.data)) {
      // 添加AI回复消息
      const aiMessage = {
        id: Date.now() + 1,
        sender: 'ai',
        type: 'ai-reply',
        content: result.data.join('\n'),
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
            aiResponse: result.data.join('\n')
          })
        }
      } catch (error) {
        console.error('记录AI提问失败：', error)
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

const generateAIResponse = (question) => {
  const responses = [
    `关于"${question}"这个问题，我来为你详细解答：\n\n这是一个很好的问题。根据我的理解，主要涉及以下几个方面：\n\n1. 基础概念理解\n2. 实际应用场景\n3. 最佳实践建议\n\n希望这个回答对你有帮助！如果还有疑问，欢迎继续提问。`,
    `很高兴你提出这个问题！让我来帮你分析一下：\n\n首先，我们需要理解问题的核心。然后，我会提供一些实用的解决方案和建议。\n\n如果你需要更具体的帮助，可以提供更多上下文信息。`,
    `这是一个经典的问题！让我为你提供一个全面的解答：\n\n从理论角度来看...\n从实践角度来看...\n\n建议你可以通过以下方式深入学习这个话题。`
  ]
  return responses[Math.floor(Math.random() * responses.length)]
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
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
        id: item.id,
        title: item.resourceName,
        type: item.resourceType,
        teacher: item.teacherName,
        uploadTime: item.uploadTime,
        size: formatFileSize(item.fileSize),
        url: item.resourceUrl
      }))
    }
  } catch (error) {
    console.error('加载课件列表失败：', error)
  }
}

// 加载学习统计数据
const loadStudyStats = async () => {
  try {
    const studentId = getCurrentStudentId()
    if (!studentId) return
    
    const result = await getStudyStatsApi(studentId)
    if (result.code === 1) {
      totalCourseware.value = result.data.totalCourseware || 0
      studiedCourseware.value = result.data.studiedCourseware || 0
      aiQuestions.value = result.data.aiQuestions || 0
    }
  } catch (error) {
    console.error('加载学习统计失败：', error)
  }
}

// 初始化
onMounted(() => {
  console.log('学生学习模块初始化')
  loadCoursewareList()
  loadStudyStats()
  scrollToBottom()
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
              <el-icon><ChatDotRound /></el-icon>
              AI学习助手
            </h3>
          </div>

          <div class="chat-container">
            <div ref="chatMessages" class="chat-messages">
              <div v-for="message in chatHistory" :key="message.id" class="message-item" :class="message.sender">
                <!-- 用户消息 -->
                <div v-if="message.sender === 'user'" class="user-message">
                  <div class="message-content">{{ message.content }}</div>
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
                <el-upload
                  ref="fileUpload"
                  :auto-upload="false"
                  :on-change="handleFileSelect"
                  :before-upload="() => false"
                  :show-file-list="false"
                  accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.gif"
                  :limit="1"
                  class="upload-demo"
                >
                  <el-button type="info" size="small" text>
                    <el-icon><Paperclip /></el-icon>
                    上传文件
                  </el-button>
                </el-upload>
                
                <div class="file-preview" v-if="selectedFile">
                  <div class="file-item">
                    <div class="file-preview-icon">
                      <el-icon v-if="selectedFile.type?.includes('image')"><Picture /></el-icon>
                      <el-icon v-else-if="selectedFile.type?.includes('pdf')"><Document /></el-icon>
                      <el-icon v-else><Paperclip /></el-icon>
                    </div>
                    <div class="file-preview-info">
                      <span class="file-preview-name">{{ selectedFile.name }}</span>
                      <span class="file-preview-size">{{ formatFileSize(selectedFile.size) }}</span>
                    </div>
                    <el-button type="danger" size="small" text @click="removeFile">
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
              
              <div class="message-input">
                <el-input
                  v-model="inputMessage"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入您的问题..."
                  @keydown.ctrl.enter="sendMessage"
                  :disabled="sending"
                />
                <el-button 
                  type="primary" 
                  @click="sendMessage" 
                  :loading="sending"
                  class="send-button"
                >
                  <el-icon><Promotion /></el-icon>
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
      <!-- 学习统计 -->
      <div class="stats-section">
        <el-card shadow="hover">
          <h4>
            <el-icon><DataAnalysis /></el-icon>
            学习统计
          </h4>
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
          </div>
        </el-card>
      </div>

      <!-- 课件学习区域 -->
      <div class="courseware-section">
        <el-card shadow="hover">
          <div class="section-header">
            <h3>
              <el-icon><FolderOpened /></el-icon>
              课程课件
            </h3>
          </div>

          <div class="courseware-list">
            <div v-for="courseware in coursewareList" :key="courseware.id" class="courseware-item" @click="handlePreview(courseware)">
              <div class="courseware-info">
                <div class="file-icon">
                  <el-icon v-if="courseware.type === 'pdf'"><Document /></el-icon>
                  <el-icon v-else-if="courseware.type === 'ppt'"><Monitor /></el-icon>
                  <el-icon v-else-if="courseware.type === 'video'"><VideoPlay /></el-icon>
                  <el-icon v-else><Files /></el-icon>
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
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
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
  0%, 80%, 100% {
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
  margin-top: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 14px;
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
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
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
  gap: 2px;
  font-size: 11px;
  color: #666;
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
</style>
