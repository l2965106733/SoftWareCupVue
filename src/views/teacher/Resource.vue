<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeachingPlanApi, uploadResourceApi, getResourceListApi, deleteResourceApi, updateResourceApi } from '@/api/teacher'
import axios from 'axios'



const clearPlan = () => {
  teachingPlan.value = ''
  ElMessage.success('已清空')
}


const beforeUpload = (file) => {
  console.error('Token 11');
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('上传文件大小不能超过 100MB')
    return false
  }
  const headers = uploadHeaders.value;
  console.error('Token 22');
  // 手动设置请求头
  if (!headers.token) {
    console.error('Token missing');
    return false; // 阻止上传
  }
  file.headers = {
    ...file.headers,
    token: headers.token // 确保 token 被传递
  };
  return true; // 允许上传
}


const handleSuccess = async (response, file) => {
  console.log('上传成功，后端返回:', response)
  if (response && response.data) {
    file.url = response.data

    try {
      const teacherId = getCurrentTeacherId()
      if (!teacherId) {
        ElMessage.error('无法获取教师信息，请重新登录')
        return
      }

      const resourceData = {
        teacherId: teacherId,
        resourceName: file.name,
        resourceUrl: response.data,
        resourceType: getFileType(file.name),
        fileSize: file.size,
        description: file.name
      }

      console.log('发送的资源数据：', resourceData)

      const result = await uploadResourceApi(resourceData)

      if (result.code === 1) {
        file.uid = result.data.id // 保存资源ID
        ElMessage.success('资源上传并保存成功')
      } else {
        ElMessage.error(`资源信息保存失败: ${result.msg || '未知错误'}`)
      }
    } catch (error) {
      console.error('保存资源信息失败：', error)
      ElMessage.error(`保存资源信息失败: ${error.message || '网络错误'}`)
    }
  }
}

// 获取文件类型
const getFileType = (fileName) => {
  const ext = fileName.split('.').pop().toLowerCase()
  const typeMap = {
    'pdf': 'pdf',
    'doc': 'doc', 'docx': 'doc',
    'ppt': 'ppt', 'pptx': 'ppt',
    'xls': 'excel', 'xlsx': 'excel',
    'jpg': 'image', 'jpeg': 'image', 'png': 'image', 'gif': 'image',
    'mp4': 'video', 'avi': 'video', 'mov': 'video',
    'mp3': 'audio', 'wav': 'audio'
  }
  return typeMap[ext] || 'other'
}
const handlePreview = (file) => {
  if (!file.url) return ElMessage.warning('文件未就绪')
  const url = file.url.toLowerCase()

  if (
    url.endsWith('.doc') || url.endsWith('.docx') ||
    url.endsWith('.ppt') || url.endsWith('.pptx') ||
    url.endsWith('.xls') || url.endsWith('.xlsx')
  ) {
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(url)}`
    window.open(officeUrl, '_blank')
  } else {
    window.open(url, '_blank')
  }
}


const handleRemove = async (file) => {
  if (!file?.url) return

  try {
    await ElMessageBox.confirm('确定要删除这个资源吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    let dbDeleteSuccess = false
    let fileDeleteSuccess = false

    if (file.uid) {
      const result = await deleteResourceApi(file.uid)
      if (result.code === 1) {
        dbDeleteSuccess = true
      } else {
        ElMessage.error('删除资源记录失败')
        return
      }
    }

    try {
      const res = await axios.delete('/api/delete', {
        params: { url: file.url }
      })
      if (res.data.code === 1) {
        fileDeleteSuccess = true
      }
    } catch (error) {
      console.error('删除文件失败：', error)
    }

    if (dbDeleteSuccess) {
      const index = teachingFileList.value.findIndex(item => item.uid === file.uid)
      if (index > -1) {
        teachingFileList.value.splice(index, 1)
      }

      if (fileDeleteSuccess) {
        ElMessage.success('资源已删除')
      } else {
        ElMessage.warning('资源记录已删除，但文件删除失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试')
    }
  }
}

// AI 教学生成相关
const aiFiles = ref([])
const aiRemark = ref('')
const teachingPlan = ref('')

const generateTeachingPlan = async () => {
  aiDialogVisible.value = false

  const uploadedUrls = aiFiles.value
    .filter(file => file.url)
    .map(file => file.url)

  try {
    const res = await getTeachingPlanApi(aiRemark.value, uploadedUrls)
    if (res.code === 1) {
      // 如果返回的是docx文件URL
      if (res.data && typeof res.data === 'string' && res.data.endsWith('.docx')) {
        ElMessage.success('教学计划文档生成成功，正在解析内容...')
        await parseDocxContent(res.data)
      }
      // 如果返回的是数组（原来的格式）
      else if (Array.isArray(res.data)) {
        teachingPlan.value = res.data
        ElMessage.success('AI 教学结构生成成功')
      }
      // 如果返回的是对象包含docx URL
      else if (res.data && res.data.docxUrl) {
        ElMessage.success('教学计划文档生成成功，正在解析内容...')
        await parseDocxContent(res.data.docxUrl)
      }
      else {
        ElMessage.error('返回数据格式不正确')
      }

      aiFiles.value = []
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (e) {
    console.error('生成教学计划失败:', e)
    ElMessage.error('网络错误，生成失败')
  }
}

// 解析docx文件内容
const parseDocxContent = async (docxUrl) => {
  try {
    // 方案1：尝试使用mammoth.js解析docx
    if (window.mammoth) {
      const response = await fetch(docxUrl)
      const arrayBuffer = await response.arrayBuffer()

      const result = await mammoth.convertToHtml({ arrayBuffer: arrayBuffer })
      const htmlContent = result.value

      // 将HTML内容转换为教学计划结构
      const planStructure = parseHtmlToTeachingPlan(htmlContent)
      if (planStructure && planStructure.length > 0) {
        teachingPlan.value = planStructure
        ElMessage.success('教学计划内容解析成功')
      } else {
        // 如果解析失败，提供下载链接
        showDocxDownload(docxUrl)
      }
    } else {
      // 如果没有mammoth.js，直接提供下载
      showDocxDownload(docxUrl)
    }
  } catch (error) {
    console.error('解析docx文件失败:', error)
    ElMessage.warning('无法解析文档内容，为您提供下载链接')
    showDocxDownload(docxUrl)
  }
}

// 将HTML内容转换为教学计划结构
const parseHtmlToTeachingPlan = (htmlContent) => {
  try {
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = htmlContent

    const plans = []
    const headings = tempDiv.querySelectorAll('h1, h2, h3, h4')

    headings.forEach((heading, index) => {
      let nextSibling = heading.nextElementSibling
      let content = ''

      // 收集标题下的内容直到下一个标题
      while (nextSibling && !nextSibling.matches('h1, h2, h3, h4')) {
        content += nextSibling.textContent + '\n'
        nextSibling = nextSibling.nextElementSibling
      }

      plans.push({
        title: heading.textContent.trim(),
        summary: content.trim() || '详细内容请查看完整文档',
        duration: `第${index + 1}讲`,
        practice: false
      })
    })

    return plans.length > 0 ? plans : null
  } catch (error) {
    console.error('解析HTML内容失败:', error)
    return null
  }
}

// 显示docx下载链接
const showDocxDownload = (docxUrl) => {
  teachingPlan.value = [{
    title: '📄 教学计划文档已生成',
    summary: '点击下方链接下载完整的教学计划文档',
    duration: '文档下载',
    practice: false,
    downloadUrl: docxUrl
  }]
  ElMessage.info('教学计划文档已准备就绪，请点击下载')
}
const editLesson = (index) => {
  ElMessage.info(`第 ${index + 1} 节编辑功能开发中...`)
}

const teachingFileList = ref([])
const aiDialogVisible = ref(false)

// 获取当前登录教师ID
const getCurrentTeacherId = () => {
  console.log('🔐 检查localStorage中的登录用户信息...')
  const loginUserStr = localStorage.getItem('loginUser')
  console.log('📱 localStorage中的原始数据:', loginUserStr)

  if (!loginUserStr) {
    console.warn('❌ localStorage中没有找到loginUser')
    return null
  }

  try {
    const loginUser = JSON.parse(loginUserStr)
    console.log('👤 解析后的用户信息:', loginUser)
    console.log('🆔 用户ID:', loginUser?.id)
    return loginUser?.id
  } catch (error) {
    console.error('💥 解析localStorage数据失败:', error)
    return null
  }
}

// 加载资源列表
const loadResourceList = async () => {
  try {
    const teacherId = getCurrentTeacherId()
    if (!teacherId) return

    const result = await getResourceListApi(teacherId)
    if (result.code === 1) {
      teachingFileList.value = result.data.map(item => ({
        name: item.resourceName,
        url: item.resourceUrl,
        size: item.fileSize,
        uid: item.id,
        resourceType: item.resourceType,
        description: item.description,
        uploadTime: item.uploadTime,
        downloadCount: item.downloadCount
      }))
    }
  } catch (error) {
    console.error('加载资源列表失败：', error)
  }
}

// 编辑资源信息
const editDialogVisible = ref(false)
const editingResource = ref({})

const editResource = (file) => {
  editingResource.value = {
    id: file.uid,
    resourceName: file.name,
    description: file.description || file.name
  }
  editDialogVisible.value = true
}

const saveResourceEdit = async () => {
  try {
    const result = await updateResourceApi(editingResource.value)
    if (result.code === 1) {
      ElMessage.success('资源信息更新成功')
      editDialogVisible.value = false
      loadResourceList() // 重新加载列表
    } else {
      ElMessage.error('更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败，请重试')
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 下载文件
const handleDownload = (file) => {
  if (!file.url) {
    ElMessage.warning('文件链接不存在')
    return
  }

  // 创建下载链接
  const link = document.createElement('a')
  link.href = file.url
  link.download = file.name
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success(`开始下载：${file.name}`)
}

// 下载教学计划文档
const downloadTeachingPlan = (docxUrl) => {
  if (!docxUrl) {
    ElMessage.warning('下载链接不存在')
    return
  }
  const link = document.createElement('a')
  link.href = docxUrl
  link.download = '教学计划.docx' // 可以自定义下载文件名
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('开始下载教学计划文档...')
}

// 页面加载时获取资源列表
onMounted(() => {
  console.log('🚀 页面已挂载，开始加载资源列表...')
  loadResourceList()
})
</script>


<template>
  <div class="lesson-plan-container">

    <div class="top-toolbar">

      <el-button type="success" @click="aiDialogVisible = true">🧠 AI生成教学内容</el-button>
      <el-button type="danger" @click="clearPlan">🧹 清空</el-button>
    </div>

    <el-row :gutter="20">

      <el-col :span="16">
        <el-card class="lesson-section" shadow="never">
          <h3>📘 教学内容结构</h3>
          <div v-if="teachingPlan">
            <el-timeline>
              <el-timeline-item v-for="(item, index) in teachingPlan" :key="index" :timestamp="item.duration || '待设定'">
                <div class="lesson-block">
                  <strong>第{{ index + 1 }}讲：{{ item.title }}</strong>
                  <el-button link type="primary" @click="editLesson(index)" v-if="!item.downloadUrl">编辑</el-button>

                  <!-- 如果有下载链接，显示下载按钮 -->
                  <el-button type="success" @click="downloadTeachingPlan(item.downloadUrl)" v-if="item.downloadUrl">
                    <el-icon>
                      <Download />
                    </el-icon>
                    下载完整文档
                  </el-button>

                  <p><strong>摘要：</strong>{{ item.summary }}</p>
                  <p v-if="!item.practice && !item.downloadUrl">❌ 无练习题（请前往"作业模块"添加）</p>

                  <!-- 如果有下载链接，显示额外信息 -->
                  <div v-if="item.downloadUrl" class="download-info">
                    <p>📄 完整的教学计划已生成为Word文档</p>
                    <p>💡 包含详细的教学目标、重点难点、教学方法等内容</p>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else>
            <el-empty description="尚未生成教学节次结构"></el-empty>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="upload-section" shadow="never">
          <h3>📂 课程资料管理</h3>

          <!-- 上传区域 -->
          <div class="upload-area">
            <div class="upload-controls">
              <el-upload v-model:file-list="teachingFileList" action="/api/upload" :before-upload="beforeUpload"
                :on-success="handleSuccess" :on-preview="handlePreview" :on-remove="handleRemove"
                :show-file-list="false">
                <el-button type="primary">
                  <el-icon>
                    <Upload />
                  </el-icon>
                  上传文件
                </el-button>
              </el-upload>
              <span class="upload-tip">支持上传课程资料，多文件，最大100MB</span>
            </div>
          </div>

          <!-- 资源列表 -->
          <div v-if="teachingFileList.length > 0" class="resource-list">
            <h4>📋 资源列表</h4>
            <div v-for="file in teachingFileList" :key="file.uid" class="resource-item">
              <div class="resource-name">
                <el-icon>
                  <Document />
                </el-icon>
                {{ file.name }}
              </div>
              <div class="resource-meta">
                <span v-if="file.size">大小: {{ formatFileSize(file.size) }}</span>
                <span v-if="file.uploadTime">上传时间: {{ file.uploadTime }}</span>
              </div>
              <div class="resource-actions">
                <el-button size="small" type="info" @click="handlePreview(file)">预览</el-button>
                <el-button size="small" type="success" @click="handleDownload(file)">下载</el-button>
                <el-button size="small" type="primary" @click="editResource(file)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleRemove(file)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI生成教学内容对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI教学设计说明" width="500px">
      <p>你可以上传额外文件用于 AI 分析（不影响课程资料），或单独填写备注。</p>

      <el-form label-width="80px">
        <el-form-item label="备注">
          <el-input v-model="aiRemark" placeholder="如重点章节、教学目标等" />
        </el-form-item>
        <el-form-item label="上传资料">
          <el-upload v-model:file-list="aiFiles" action="/api/upload" list-type="text" :on-success="(res, file) => {
            if (res.code === 1) file.url = res.data.url || res.data
          }">
            <el-button>选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateTeachingPlan">生成节次结构</el-button>
      </template>
    </el-dialog>

    <!-- 编辑资源信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑资源信息" width="400px">
      <el-form :model="editingResource" label-width="80px">
        <el-form-item label="资源名称" required>
          <el-input v-model="editingResource.resourceName" placeholder="请输入资源名称" />
        </el-form-item>
        <el-form-item label="资源描述">
          <el-input v-model="editingResource.description" type="textarea" :rows="3" placeholder="请输入资源描述" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResourceEdit">保存</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<style scoped>
.lesson-plan-container {
  padding: 30px;
  background: linear-gradient(to right, #7b2ff7, #f107a3);
  min-height: 100vh;
  color: #333;
}

.top-toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-section,
.lesson-section {
  background: #fff;
}

.lesson-block {
  margin-bottom: 15px;
}

.lesson-block p {
  margin: 5px 0;
}

/* 下载信息样式 */
.download-info {
  background: linear-gradient(135deg, #e3f2fd 0%, #f1f8e9 100%);
  padding: 12px 16px;
  border-radius: 8px;
  border-left: 4px solid #4caf50;
  margin-top: 10px;
}

.download-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #2e7d32;
}

.download-info p:first-child {
  font-weight: 600;
}

/* 资源管理样式 */
.upload-area {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.upload-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-tip {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.resource-list {
  margin-top: 20px;
}

.resource-list h4 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.resource-item {
  display: flex;
  flex-direction: column;
  padding: 16px;
  margin-bottom: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px solid #e9ecef;
  transition: all 0.3s ease;
}

.resource-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.resource-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 15px;
}

.resource-name .el-icon {
  font-size: 20px;
  color: #409eff;
}

.resource-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.resource-actions {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
}

.resource-actions .el-button {
  flex: 1;
  min-width: 45px;
  font-size: 12px;
  padding: 4px 8px;
}
</style>
