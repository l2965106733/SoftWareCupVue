<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeachingPlanApi, uploadResourceApi, getResourceListApi, deleteResourceApi, updateResourceApi } from '@/api/teacher'
import axios from 'axios'
import { ElLoading } from 'element-plus'


const clearPlan = () => {
  teachingPlan.value = ''
  ElMessage.success('已清空')
}


const beforeUpload = (file) => {
  
  const isLt100M = file.size / 1024 / 1024 < 1000
  if (!isLt100M) {
    ElMessage.error('上传文件大小不能超过 100MB')
    return false
  }

  return true; // 允许上传
}

const uploadHeaders = computed(() => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}');
  return {
    token: loginUser.token || ''
  };
});

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

const isGenerating = ref(false)

const generateTeachingPlan = async () => {
  aiDialogVisible.value = false
  isGenerating.value = true  // 开始加载
  const loading = ElLoading.service({
    lock: true,
    text: '教学计划生成中，请稍候...',
    background: 'rgba(255, 255, 255, 0.8)',
  })

  const uploadedUrls = aiFiles.value
    .filter(file => file.url)
    .map(file => file.url)

  try {
    const res = await getTeachingPlanApi(aiRemark.value, uploadedUrls)
    if (res.code === 1) {
      // 如果返回的是docx文件URL
      if (res.data && typeof res.data === 'string' && res.data.endsWith('.docx')) {
        ElMessage.success('教学计划文档生成成功，提供下载连接')
        showDocxDownload(docxUrl)
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
  } finally {
    loading.close()
    isGenerating.value = false
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
  <div class="lesson-plan-container vertical-layout">

    <div class="top-toolbar">

      <el-button type="success" @click="aiDialogVisible = true"><i class="fas fa-magic"></i> AI生成教学内容</el-button>
      <el-button type="danger" @click="clearPlan"><i class="fas fa-broom"></i> 清空</el-button>
    </div>

    <div class="vertical-blocks">
      <el-card class="lesson-section" shadow="never">
        <h3 style = "color #777"><i class="fas fa-book-open"></i> 教学内容结构</h3>
        <div v-if="teachingPlan">
          <div class="vertical-plan-list">
            <div v-for="(item, index) in teachingPlan" :key="index" class="vertical-plan-item">
              <div class="plan-title-row">
                <strong>第{{ index + 1 }}讲：{{ item.title }}</strong>
                <el-button link type="primary" @click="editLesson(index)" v-if="!item.downloadUrl">编辑</el-button>
                <el-button type="success" @click="downloadTeachingPlan(item.downloadUrl)" v-if="item.downloadUrl">
                  <el-icon>
                    <Download />
                  </el-icon>
                  下载完整文档
                </el-button>
              </div>
              <p class="plan-summary"><strong>摘要：</strong>{{ item.summary }}</p>
              <p v-if="!item.practice && !item.downloadUrl" class="plan-no-practice">❌ 无练习题（请前往"作业模块"添加）</p>
              <div v-if="item.downloadUrl" class="download-info">
                <p>📄 完整的教学计划已生成为Word文档</p>
                <p>💡 包含详细的教学目标、重点难点、教学方法等内容</p>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          <el-empty>
            <template #description>
              <span style="color: #fff; font-weight: 600;">尚未生成教学节次结构</span>
            </template>
          </el-empty>
        </div>
      </el-card>

      <el-card class="upload-section" shadow="never">
        <h3><i class="fas fa-folder-open"></i> 课程资料管理</h3>
        <div class="upload-area">
          <div class="upload-controls">
            <el-upload v-model:file-list="teachingFileList" action="/api/upload" :before-upload="beforeUpload"
              :on-success="handleSuccess" :on-preview="handlePreview" :on-remove="handleRemove" :headers="uploadHeaders"
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
        <div v-if="teachingFileList.length > 0" class="resource-list">
          <h4><i class="fas fa-list"></i> 资源列表</h4>
          <div v-for="file in teachingFileList" :key="file.uid" class="resource-item">
            <div class="resource-name">
              <i class="fas fa-file-alt"></i>
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
    </div>

    <!-- AI生成教学内容对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI教学设计说明" width="500px" class="ai-dialog home-style-dialog">
      <div class="ai-dialog-content">
        <div class="ai-dialog-desc home-style-desc">你可以上传额外文件用于 AI 分析（不影响课程资料），或单独填写备注。</div>
        <el-form label-width="80px">
          <el-form-item label="备注">
            <el-input v-model="aiRemark" placeholder="如重点章节、教学目标等" class="home-style-input" />
          </el-form-item>
          <el-form-item label="上传资料">
            <el-upload :headers="uploadHeaders" v-model:file-list="aiFiles" action="/api/upload" list-type="text" :on-success="(res, file) => {
              if (res.code === 1) file.url = res.data.url || res.data
            }" class="home-style-upload">
              <el-button class="home-style-btn">选择文件</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button class="home-style-btn" @click="aiDialogVisible = false">取消</el-button>
          <el-button class="home-style-btn primary" type="primary" @click="generateTeachingPlan">生成节次结构</el-button>
        </span>
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
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');
.lesson-plan-container {
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

.card, .el-card, .resource-item, .download-info, .upload-section, .lesson-section {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);
    padding: clamp(20px, 4vw, 32px);
    transition: all 0.3s ease;
    color: #fff;
}

.lesson-plan-container:hover,
.el-card:hover,
.upload-section:hover,
.lesson-section:hover {
    /* 不改变背景色 */
    /* background: none !important; */
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
  gap: 10px;
  flex-wrap: wrap;
}

.upload-section,
.lesson-section {
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 24px;
  border: none;
}

.lesson-section h3,
.upload-section h3 {
  font-size: 20px;
  font-weight: bold;
  color: #222;
  margin-bottom: 18px;
}

.lesson-block {
  margin-bottom: 15px;
}

.lesson-block p {
  margin: 5px 0;
}

/* 下载信息样式 */
.download-info {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  margin-top: 10px;
}

.download-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #1761a0;
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
  color: #fff;
  line-height: 1.4;
}

.resource-list {
  margin-top: 20px;
}

.resource-list h4 {
  margin-bottom: 15px;
  color: #222;
  font-size: 16px;
  font-weight: bold;
}

.resource-item {
  display: flex;
  flex-direction: column;
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  border: 2px solid #e9ecef;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: box-shadow 0.3s;
}

.resource-item:hover {
  box-shadow: 0 4px 24px rgba(64,158,255,0.10);
  border-color: #b3c6e0;
}

.resource-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #a18cd1;
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
  gap: 8px;
  flex-wrap: nowrap;
}

.resource-actions .el-button {
  background: transparent !important;
  color: #a18cd1 !important;
  border: 1.5px solid #a18cd1 !important;
  border-radius: 8px !important;
  font-size: 13px;
  padding: 4px 10px;
  transition: all 0.2s;
}
.resource-actions .el-button:hover {
  background: #a18cd1 !important;
  color: #fff !important;
  border-color: #a18cd1 !important;
}

/* 统一el-dialog圆角 */
.el-dialog {
  border-radius: 16px !important;
}
.vertical-plan-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 8px;
}
.vertical-plan-item {
  background: rgba(255,255,255,0.08);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 18px 20px;
  color: #fff;
  border: 1px solid rgba(255,255,255,0.12);
  transition: box-shadow 0.3s;
}
.vertical-plan-item:hover {
  box-shadow: 0 6px 24px rgba(64,158,255,0.10);
  border-color: #b3c6e0;
}
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.plan-summary {
  margin: 6px 0 0 0;
  color: #e0e0e0;
  font-size: 15px;
}
.plan-no-practice {
  color: #ffb300;
  font-size: 14px;
  margin: 4px 0 0 0;
}
.vertical-layout {
  display: flex;
  flex-direction: column;
  gap: 32px;
}
.vertical-blocks {
  display: flex;
  flex-direction: column;
  gap: 32px;
}
@media (max-width: 900px) {
  .vertical-layout, .vertical-blocks {
    gap: 16px;
  }
}
.ai-dialog .el-dialog__wrapper,
.ai-dialog .el-dialog {
  background: none !important;
  box-shadow: none !important;
}
.ai-dialog .el-dialog__header,
.ai-dialog .el-dialog__body {
  background: rgba(161,140,209,0.85);
  backdrop-filter: blur(16px);
  border-radius: 20px 20px 0 0;
  color: #fff;
}
.ai-dialog .el-dialog__body {
  border-radius: 0 0 20px 20px;
  padding: 32px 32px 18px 32px;
}
.ai-dialog .el-dialog__title {
  color: #fff;
  font-weight: 700;
  font-size: 22px;
}
.ai-dialog-content {
  color: #fff;
}
.ai-dialog-desc {
  color: #fff;
  font-size: 15px;
  font-weight: 400;
  margin-bottom: 22px;
  text-shadow: 0 2px 8px rgba(80,0,80,0.10);
}
.ai-input .el-input__inner {
  background: rgba(255,255,255,0.15) !important;
  color: #fff !important;
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(161,140,209,0.08);
}
.ai-upload .el-upload {
  background: rgba(255,255,255,0.10);
  border-radius: 12px;
  padding: 10px 0;
}
.ai-upload .el-upload-list {
  color: #fff;
}
.ai-btn {
  background: linear-gradient(120deg, #a18cd1 0%, #fbc2eb 100%) !important;
  color: #fff !important;
  border-radius: 12px !important;
  border: none !important;
  font-weight: 600;
  font-size: 15px !important;
  padding: 10px 28px !important;
  box-shadow: 0 2px 12px rgba(161,140,209,0.10);
  transition: background 0.2s, box-shadow 0.2s;
}
.ai-btn.primary {
  background: linear-gradient(120deg, #a18cd1 0%, #fbc2eb 100%) !important;
  color: #fff !important;
}
.ai-btn:hover {
  background: linear-gradient(120deg, #b993d6 0%, #8ca6db 100%) !important;
  box-shadow: 0 4px 24px rgba(161,140,209,0.18);
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 10px 24px 18px 24px;
}
.home-style-dialog .el-dialog__wrapper,
.home-style-dialog .el-dialog {
  background: none !important;
  box-shadow: none !important;
}
.home-style-dialog .el-dialog__header,
.home-style-dialog .el-dialog__body {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(18px);
  border-radius: 20px 20px 0 0;
  color: #222;
}
.home-style-dialog .el-dialog__body {
  border-radius: 0 0 20px 20px;
  padding: 32px 32px 18px 32px;
}
.home-style-dialog .el-dialog__title {
  color: #222;
  font-weight: 700;
  font-size: 22px;
}
.home-style-desc {
  color: #444;
  font-size: 15px;
  font-weight: 400;
  margin-bottom: 22px;
  text-shadow: none;
}
.home-style-input .el-input__inner {
  background: rgba(255,255,255,0.7) !important;
  color: #222 !important;
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(161,140,209,0.04);
}
.home-style-upload .el-upload {
  background: rgba(255,255,255,0.7);
  border-radius: 12px;
  padding: 10px 0;
}
.home-style-upload .el-upload-list {
  color: #222;
}
.home-style-btn {
  background: rgba(255,255,255,0.7) !important;
  color: #7c4dff !important;
  border-radius: 12px !important;
  border: 1px solid #a18cd1 !important;
  font-weight: 600;
  font-size: 15px !important;
  padding: 10px 28px !important;
  box-shadow: 0 2px 12px rgba(161,140,209,0.06);
  transition: background 0.2s, box-shadow 0.2s, color 0.2s;
}
.home-style-btn.primary {
  background: rgba(161,140,209,0.12) !important;
  color: #7c4dff !important;
  border: 1px solid #a18cd1 !important;
}
.home-style-btn:hover {
  background: rgba(161,140,209,0.18) !important;
  color: #4f277e !important;
  box-shadow: 0 4px 24px rgba(161,140,209,0.12);
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 10px 24px 18px 24px;
}
.home-style-upload .el-upload .el-button,
.home-style-upload .el-upload .el-button.home-style-btn {
  background: rgba(255,255,255,0.7) !important;
  color: #7c4dff !important;
  border-radius: 12px !important;
  border: 1px solid #a18cd1 !important;
  font-weight: 600;
  font-size: 15px !important;
  padding: 10px 28px !important;
  box-shadow: 0 2px 12px rgba(161,140,209,0.06);
  transition: background 0.2s, box-shadow 0.2s, color 0.2s;
}
.home-style-upload .el-upload .el-button:hover,
.home-style-upload .el-upload .el-button.home-style-btn:hover {
  background: rgba(161,140,209,0.18) !important;
  color: #4f277e !important;
  box-shadow: 0 4px 24px rgba(161,140,209,0.12);
}
/* 让主要标题和el-empty描述为白色 */
.lesson-section h3,
.upload-section h3,
.resource-list h4,
::v-deep(.el-empty__description) {
  color: #fff !important;
}
</style>
