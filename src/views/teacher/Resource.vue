<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeachingPlanApi, uploadResourceApi, getResourceListApi, deleteResourceApi, updateResourceApi } from '@/api/teacher'
import axios from 'axios'

const savePlan = () => ElMessage.success('保存成功（其实还没做，此处应调用API）')
const clearPlan = () => {
  teachingPlan.value = ''
  ElMessage.success('已清空')
}


const beforeUpload = (file) => {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('上传文件大小不能超过 100MB')
    return false
  }
  return true
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
    if (res.code === 1 && Array.isArray(res.data)) {
      teachingPlan.value = res.data
      ElMessage.success('AI 教学结构生成成功')
      aiFiles.value = [] 
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('网络错误，生成失败')
  }
}
const editLesson = (index) => {
  ElMessage.info(`第 ${index + 1} 节编辑功能开发中...`)
}

const teachingFileList = ref([])
const aiDialogVisible = ref(false)

// 获取当前登录教师ID
const getCurrentTeacherId = () => {
  const loginUser = JSON.parse(localStorage.getItem('loginUser'))
  return loginUser?.id
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

// 页面加载时获取资源列表
onMounted(() => {
  loadResourceList()
})
</script>


<template>
  <div class="lesson-plan-container">

    <div class="top-toolbar">

      <el-button type="success" @click="aiDialogVisible = true">🧠 AI生成教学内容</el-button>
      <el-button type="warning" @click="savePlan">💾 保存计划</el-button>
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
                  <el-button link type="primary" @click="editLesson(index)">编辑</el-button>
                  <p><strong>摘要：</strong>{{ item.summary }}</p>
                  <p v-if="!item.practice">❌ 无练习题（请前往“作业模块”添加）</p>
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
            <el-upload v-model:file-list="teachingFileList" action="/api/upload" :before-upload="beforeUpload"
              :on-success="handleSuccess" :on-preview="handlePreview" :on-remove="handleRemove" list-type="text">
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">
                  支持上传课程资料，多文件，最大100MB
                </div>
              </template>
            </el-upload>
          </div>
          
          <!-- 资源列表 -->
          <div v-if="teachingFileList.length > 0" class="resource-list">
            <h4>📋 资源列表</h4>
            <div v-for="file in teachingFileList" :key="file.uid" class="resource-item">
              <div class="resource-info">
                <div class="resource-name">
                  <el-icon><Document /></el-icon>
                  {{ file.name }}
                </div>
                <div class="resource-meta">
                  <span v-if="file.size">大小: {{ formatFileSize(file.size) }}</span>
                  <span v-if="file.uploadTime">上传时间: {{ file.uploadTime }}</span>
                  <span v-if="file.downloadCount">下载: {{ file.downloadCount }}次</span>
                </div>
              </div>
              <div class="resource-actions">
                <el-button size="small" @click="handlePreview(file)">预览</el-button>
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
          <el-input 
            v-model="editingResource.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入资源描述" 
          />
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

/* 资源管理样式 */
.upload-area {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
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
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.resource-info {
  flex: 1;
}

.resource-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.resource-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
}

.resource-actions {
  display: flex;
  gap: 8px;
}
</style>
