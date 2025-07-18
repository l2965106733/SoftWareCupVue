<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiQuestionHistoryApi, getChatApi, recordAiQuestionApi, } from '@/api/student'


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
  }
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  files.forEach(file => {
    // 检查文件大小（限制10MB）
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      ElMessage.error('文件大小不能超过10MB')
      return
    }

    // 检查文件数量（限制5个文件）
    if (selectedFiles.value.length >= 5) {
      ElMessage.error('最多只能上传5个文件')
      return
    }

    // 添加文件到列表
    selectedFiles.value.push({
      uid: Date.now() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      file: file
    })
  })

  // 清空input值，允许重复选择同一文件
  event.target.value = ''

  if (files.length > 0) {
    ElMessage.success(`已选择${files.length}个文件`)
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
  <div class="student-container">
    <!-- 页面标题 -->
    <div class="student-section">
      <h1 class="student-title large">
        <i class="fas fa-robot"></i>
        AI助手
      </h1>
      <p class="student-text secondary">智能AI助手，解答学习问题</p>
    </div>

    <!-- AI聊天区域 -->
    <div class="student-section">
      <div class="student-card chat-card">
        <div class="chat-header">
          <div class="chat-title">
            <i class="fas fa-comments"></i>
            AI学习助手
          </div>
          <div class="chat-status">
            <i class="fas fa-circle" :class="{ 'status-online': !aiTyping, 'status-typing': aiTyping }"></i>
            {{ aiTyping ? 'AI正在思考...' : 'AI在线' }}
          </div>
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
                      <i class="fas fa-paperclip"></i>
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
                <el-button type="info" size="small" text style="color:white">
                  <el-icon>
                    <Paperclip />
                  </el-icon>
                  上传文件 (最多5个)
                </el-button>
              </el-upload>

              <div class="file-preview" v-if="selectedFiles.length > 0">
                <div class="file-list-header">
                  <span>已选择 {{ selectedFiles.length }} 个文件</span>
                  <button class="clear-btn" @click="removeFile()">
                    <i class="fas fa-trash"></i>
                    清空全部
                  </button>
                </div>
                <div class="file-list">
                  <div class="file-item" v-for="file in selectedFiles" :key="file.uid">
                    <div class="file-preview-icon">
                      <i v-if="file.type?.includes('image')" class="fas fa-image"></i>
                      <i v-else-if="file.type?.includes('pdf')" class="fas fa-file-pdf"></i>
                      <i v-else class="fas fa-paperclip"></i>
                    </div>
                    <div class="file-preview-info">
                      <span class="file-preview-name">{{ file.name }}</span>
                      <span class="file-preview-size">{{ formatFileSize(file.size) }}</span>
                    </div>
                    <button class="remove-btn" @click="removeFile(file)">
                      <i class="fas fa-times"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="message-input">
              <textarea v-model="inputMessage" class="message-textarea" placeholder="请输入您的问题..."
                @keydown.ctrl.enter="sendMessage" :disabled="sending" rows="3"></textarea>
              <button class="send-btn" @click="sendMessage"
                :disabled="sending || (!inputMessage.trim() && selectedFiles.length === 0)">
                <i class="fas fa-paper-plane"></i>
                {{ sending ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 聊天卡片样式 */
.chat-card {
  padding: 0;
  overflow: hidden;
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--student-glass);
  border-bottom: 1px solid var(--student-glass-border);
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--student-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--student-text-secondary);
}

.status-online {
  color: #67c23a;
  animation: pulse 2s infinite;
}

.status-typing {
  color: #e6a23c;
  animation: pulse 1s infinite;
}

@keyframes pulse {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.5;
  }
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.user {
  align-items: flex-end;
}

.message-item.ai {
  align-items: flex-start;
}

.user-message,
.ai-message {
  display: inline-block;
  max-width: 70%;
  text-align: left;
}

.user-message .message-content {
  background: var(--gradient-primary);
  color: var(--student-text);
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  word-wrap: break-word;
}

.ai-message .message-content {
  background: var(--student-glass);
  color: var(--student-text);
  padding: 12px 16px;
  border-radius: 18px 18px 18px 4px;
  border: 1px solid var(--student-glass-border);
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  color: var(--student-text-muted);
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
  background: var(--student-glass);
  border-radius: 18px 18px 18px 4px;
  border: 1px solid var(--student-glass-border);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--student-text);
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
  border-top: 1px solid var(--student-glass-border);
  padding: 20px;
  background: var(--student-glass);
}

.input-toolbar {
  margin-bottom: 12px;
}

.upload-btn {
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
  gap: 6px;
}

.upload-btn:hover {
  background: var(--student-card-hover);
  color: var(--student-text);
}

.file-preview {
  margin-top: 12px;
  padding: 12px;
  background: var(--student-glass);
  border-radius: var(--student-border-radius-small);
  border: 1px solid var(--student-glass-border);
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--student-text);
}

.clear-btn {
  background: none;
  border: none;
  color: #ff6b6b;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--student-animation);
}

.clear-btn:hover {
  color: #ff4757;
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
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--student-border-radius-small);
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all var(--student-animation);
}

.file-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

.file-preview-icon {
  color: var(--student-text);
  font-size: 16px;
}

.file-preview-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.file-preview-name {
  font-weight: 600;
  color: var(--student-text);
  font-size: 12px;
}

.file-preview-size {
  font-size: 11px;
  color: var(--student-text-secondary);
}

.remove-btn {
  background: none;
  border: none;
  color: #ff6b6b;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all var(--student-animation);
}

.remove-btn:hover {
  background: rgba(255, 107, 107, 0.1);
  color: #ff4757;
}

.message-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-textarea {
  flex: 1;
  background: var(--student-glass);
  border: 1px solid var(--student-glass-border);
  border-radius: var(--student-border-radius-small);
  color: var(--student-text);
  padding: 12px 16px;
  font-size: 14px;
  resize: none;
  transition: all var(--student-animation);
  backdrop-filter: blur(10px);
}

.message-textarea:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.message-textarea::placeholder {
  color: var(--student-text-muted);
}

.send-btn {
  background: var(--gradient-primary);
  color: var(--student-text);
  border: none;
  border-radius: var(--student-border-radius-small);
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--student-animation);
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 80px;
  justify-content: center;
}

.send-btn:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  filter: brightness(1.05);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-card {
    height: 60vh;
  }

  .chat-header {
    padding: 16px 20px;
  }

  .chat-messages {
    padding: 16px;
  }

  .chat-input {
    padding: 16px;
  }

  .message-input {
    flex-direction: column;
    gap: 8px;
  }

  .send-btn {
    width: 100%;
  }

  .user-message,
  .ai-message {
    max-width: 85%;
  }
}
</style>
