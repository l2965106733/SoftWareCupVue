<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiQuestionHistoryApi, getChatApi, recordAiQuestionApi,  } from '@/api/student'


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

// 获取当前登录学生ID
const getCurrentStudentId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
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
  loadChatHistory() // 加载AI聊天历史
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

</style>
