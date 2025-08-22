<script setup>
import { ref, computed, onMounted,nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getAllResourcesApi, 
  deleteResourceApi, 
  updateResourceApi, 
  getResourceStatsApi 
} from '@/api/admin'
import axios from 'axios'

// 数据状态
const loading = ref({
  resources: false,
  stats: false
})

// 资源列表数据
const resourceList = ref([])

// 统计数据
const resourceStats = ref({
  totalResources: 0,
  totalSize: 0,
  totalDownloads: 0,
  totalViews: 0,
  teacherCount: 0,
  typeDistribution: {}
})

// 搜索和过滤条件
const searchKeyword = ref('')
const filterTeacher = ref('')
const filterType = ref('')
const sortBy = ref('uploadTime')
const sortOrder = ref('desc')

// 分页配置
const currentPage = ref(1)
const pageSize = ref(20)

// 编辑对话框相关
const editDialogVisible = ref(false)
const editingResource = ref({
  id: null,
  resourceName: '',
  description: ''
})

// 计算属性 - 过滤后的资源列表
const filteredResources = computed(() => {
  let filtered = resourceList.value

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(resource => 
      resource.resourceName.toLowerCase().includes(keyword) ||
      resource.description.toLowerCase().includes(keyword) ||
      resource.teacherName.toLowerCase().includes(keyword)
    )
  }

  // 按教师过滤
  if (filterTeacher.value) {
    filtered = filtered.filter(resource => 
      resource.teacherName.includes(filterTeacher.value)
    )
  }

  // 按类型过滤
  if (filterType.value) {
    filtered = filtered.filter(resource => 
      resource.resourceType === filterType.value
    )
  }

  // 排序
  if (sortBy.value === 'uploadTime') {
    filtered.sort((a, b) => {
      const timeA = new Date(a.uploadTime)
      const timeB = new Date(b.uploadTime)
      return sortOrder.value === 'desc' ? timeB - timeA : timeA - timeB
    })
  } else if (sortBy.value === 'fileSize') {
    filtered.sort((a, b) => {
      return sortOrder.value === 'desc' ? b.fileSize - a.fileSize : a.fileSize - b.fileSize
    })
  } else if (sortBy.value === 'downloadCount') {
    filtered.sort((a, b) => {
      return sortOrder.value === 'desc' ? b.downloadCount - a.downloadCount : a.downloadCount - b.downloadCount
    })
  } else if (sortBy.value === 'resourceName') {
    filtered.sort((a, b) => {
      return sortOrder.value === 'desc' ? 
        b.resourceName.localeCompare(a.resourceName) : 
        a.resourceName.localeCompare(b.resourceName)
    })
  }

  return filtered
})

// 分页后的数据
const paginatedResources = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredResources.value.slice(start, end)
})

// 获取唯一的教师列表
const teacherList = computed(() => {
  const teachers = [...new Set(resourceList.value.map(r => r.teacherName))]
  return teachers.sort()
})

// 获取文件类型列表
const typeList = computed(() => {
  const types = [...new Set(resourceList.value.map(r => r.resourceType))]
  return types.sort()
})

// 方法定义
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getTypeIcon = (type) => {
  const icons = {
    'pdf': 'Document',
    'doc': 'Document',
    'ppt': 'Present',
    'excel': 'Grid',
    'image': 'Picture',
    'video': 'VideoPlay',
    'audio': 'Headphones',
    'other': 'Files'
  }
  return icons[type] || 'Files'
}

const getTypeColor = (type) => {
  const colors = {
    'pdf': '#f56c6c',
    'doc': '#409eff',
    'ppt': '#e6a23c',
    'excel': '#67c23a',
    'image': '#9c27b0',
    'video': '#ff5722',
    'audio': '#607d8b',
    'other': '#909399'
  }
  return colors[type] || '#909399'
}

const showVideoDialog = ref(false)
const videoUrl = ref('')
const videoRef = ref(null)

const openVideo = (u) => {
  videoUrl.value = u
  showVideoDialog.value = true
  nextTick(() => {
    const el = videoRef.value
    if (!el) return
    el.src = u
    // 自动播放可能被拦截，不强求
    el.play().catch(() => {})
  })
}

const closeVideo = () => {
  const el = videoRef.value
  if (el) {
    el.pause()
    el.removeAttribute('src') // 释放资源
    el.load()
  }
}


const handlePreview = (resource) => {
  if (!resource.resourceUrl) {
    ElMessage.warning('文件链接不存在')
    return
  }

  const url = resource.resourceUrl.toLowerCase()

  if (url.endsWith('.pdf')) {
    // 改为跳转到你自己定义的 pdf-preview.html 页面src\views\public\pdf-preview.html
    const pdfUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(resource.resourceUrl)}&embedded=true`
    window.open(pdfUrl, '_blank')
  } else if (
    url.endsWith('.doc') || url.endsWith('.docx') ||
    url.endsWith('.ppt') || url.endsWith('.pptx') ||
    url.endsWith('.xls') || url.endsWith('.xlsx')
  ) {
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(resource.resourceUrl)}`
    window.open(officeUrl, '_blank')
  } else if (url.endsWith('.mp4') || url.endsWith('.mov')) {
    openVideo(url)
  }

  else {
    window.open(resource.resourceUrl, '_blank')
  }
}




// 下载文件
const handleDownload = (resource) => {
  if (!resource.resourceUrl) {
    ElMessage.warning('文件链接不存在')
    return
  }
  
  const link = document.createElement('a')
  link.href = resource.resourceUrl
  link.download = resource.resourceName
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  ElMessage.success(`开始下载：${resource.resourceName}`)
}

// 编辑资源
const editResource = (resource) => {
  editingResource.value = {
    id: resource.id,
    resourceName: resource.resourceName,
    description: resource.description || ''
  }
  editDialogVisible.value = true
}

// 保存编辑
const saveResourceEdit = async () => {
  try {
    loading.value.resources = true
    const result = await updateResourceApi(editingResource.value)
    
    if (result.code === 1) {
      ElMessage.success('资源信息更新成功')
      editDialogVisible.value = false
      loadResourceList() // 重新加载列表
    } else {
      ElMessage.error(result.msg || '更新失败')
    }
  } catch (error) {
    console.error('更新资源失败：', error)
    ElMessage.error('更新失败，请重试')
  } finally {
    loading.value.resources = false
  }
}

// 删除资源
const deleteResource = async (resource) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资源"${resource.resourceName}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    loading.value.resources = true
    
    // 删除数据库记录
    const result = await deleteResourceApi(resource.id)
    if (result.code === 1) {
      // 尝试删除文件（可选，如果后端没有处理）
      try {
        await axios.delete('/api/delete', {
          params: { url: resource.resourceUrl }
        })
      } catch (error) {
        console.warn('删除文件失败，但数据库记录已删除:', error)
      }
      
      ElMessage.success('资源删除成功')
      loadResourceList() // 重新加载列表
      loadResourceStats() // 重新加载统计
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除资源失败：', error)
      ElMessage.error('删除失败，请重试')
    }
  } finally {
    loading.value.resources = false
  }
}

// 批量删除
const selectedResources = ref([])
const batchDelete = async () => {
  if (selectedResources.value.length === 0) {
    ElMessage.warning('请选择要删除的资源')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedResources.value.length} 个资源吗？此操作不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    loading.value.resources = true
    
    // 依次删除选中的资源
    for (const resource of selectedResources.value) {
      await deleteResourceApi(resource.id)
    }
    
    ElMessage.success(`成功删除 ${selectedResources.value.length} 个资源`)
    selectedResources.value = []
    loadResourceList()
    loadResourceStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败：', error)
      ElMessage.error('批量删除失败，请重试')
    }
  } finally {
    loading.value.resources = false
  }
}


// 重置筛选
const resetFilters = () => {
  searchKeyword.value = ''
  filterTeacher.value = ''
  filterType.value = ''
  sortBy.value = 'uploadTime'
  sortOrder.value = 'desc'
  currentPage.value = 1
}

// 处理分页变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 处理多选
const handleSelectionChange = (selection) => {
  selectedResources.value = selection
}

// 加载资源列表
const loadResourceList = async () => {
  loading.value.resources = true
  try {
    const result = await getAllResourcesApi()
    if (result.code === 1) {
      resourceList.value = result.data || []
      console.log('资源列表加载成功:', resourceList.value)
    } else {
      ElMessage.error(result.msg || '获取资源列表失败')
    }
  } catch (error) {
    console.error('获取资源列表失败：', error)
    ElMessage.error('获取资源列表失败')
  } finally {
    loading.value.resources = false
  }
}

// 加载统计数据
const loadResourceStats = async () => {
  loading.value.stats = true
  try {
    const result = await getResourceStatsApi()
    if (result.code === 1) {
      resourceStats.value = result.data || {}
      console.log('统计数据加载成功:', resourceStats.value)
    } else {
      ElMessage.error(result.msg || '获取统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败：', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value.stats = false
  }
}

// 初始化
onMounted(() => {
  console.log('🚀 Admin资源管理页面已挂载，开始加载数据...')
  loadResourceList()
  loadResourceStats()
})
</script>

<template>
<el-dialog
      v-model="showVideoDialog"
      title="视频预览"
      width="80%"
      align-center
      destroy-on-close
      @close="closeVideo"
    >
      <video
        ref="videoRef"
        controls
        playsinline
        preload="metadata"
        style="width:100%;max-height:70vh;background:#000;outline:none"
        controlslist="nodownload noplaybackrate"
      ></video>
    </el-dialog>

  <div class="admin-resource-container">
    <!-- 页面标题和统计概览 -->
    <div class="header-section">
      <h2><i class="fas fa-folder-open nav-icon"></i> 资源管理中心</h2>
      <div class="stats-cards" v-loading="loading.stats">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ resourceStats.totalResources }}</div>

            <div class="stat-label">总资源数</div>
            <el-icon class="stat-icon"><Files /></el-icon>
          </div>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ formatFileSize(resourceStats.totalSize) }}</div>
            <div class="stat-label">总存储量</div>
          </div>
          <el-icon class="stat-icon"><Folder /></el-icon>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ resourceStats.downloadCount }}</div>
            <!-- <div class="stat-value">2</div> -->
            <div class="stat-label">总下载量</div>
          </div>
          <el-icon class="stat-icon"><Download /></el-icon>
        </el-card>
        
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ resourceStats.teacherCount }}</div>
            <div class="stat-label">活跃教师</div>
          </div>
          <el-icon class="stat-icon"><User /></el-icon>
        </el-card>
      </div>
    </div>

    <!-- 筛选和操作工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索资源名称、描述或教师"
            prefix-icon="Search"
            clearable
            style="width: 300px"
          />
          
          <el-select
            v-model="filterTeacher"
            placeholder="筛选教师"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="teacher in teacherList"
              :key="teacher"
              :label="teacher"
              :value="teacher"
            />
          </el-select>
          
          <el-select
            v-model="filterType"
            placeholder="筛选类型"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="type in typeList"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
          
          <el-select
            v-model="sortBy"
            style="width: 140px"
          >
            <el-option label="按时间排序" value="uploadTime" />
            <el-option label="按大小排序" value="fileSize" />
            <el-option label="按下载量" value="downloadCount" />
            <el-option label="按名称排序" value="resourceName" />
          </el-select>
          
          <el-button 
            :type="sortOrder === 'desc' ? 'primary' : 'default'"
            @click="sortOrder = sortOrder === 'desc' ? 'asc' : 'desc'"
          >
            <el-icon><Sort /></el-icon>
            {{ sortOrder === 'desc' ? '降序' : '升序' }}
          </el-button>
          
          <el-button @click="resetFilters">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
    
          <el-button 
            type="danger" 
            :disabled="selectedResources.length === 0"
            @click="batchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除({{ selectedResources.length }})
          </el-button>
        
          
        </div>
      </div>
    </el-card>

    <!-- 资源列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="paginatedResources"
        v-loading="loading.resources"
        stripe
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        
        <el-table-column prop="resourceName" label="资源名称" min-width="180">
          <template #default="scope">
            <div class="resource-name-cell">
              <el-icon 
                :style="{ color: getTypeColor(scope.row.resourceType) }"
                class="type-icon"
              >
                <component :is="getTypeIcon(scope.row.resourceType)" />
              </el-icon>
              <span class="name-text">{{ scope.row.resourceName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="teacherName" label="上传教师" width="90" />
        
        <el-table-column prop="resourceType" label="类型" width="80">
          <template #default="scope">
            <el-tag>
              {{ scope.row.resourceType }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="大小" width="100" sortable>
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="downloadCount" label="下载" width="80" sortable>
          <template #default="scope">
            <span class="download-count">{{ scope.row.downloadCount || 0 }}</span>
            <!-- <span class="download-count">1</span> -->
          </template>
        </el-table-column>
        
        <el-table-column prop="uploadTime" label="上传时间" width="160" sortable>
          <template #default="scope">
            {{ formatDate(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="info" @click="handlePreview(scope.row)">
                <el-icon><View /></el-icon>
                预览
              </el-button>
              
              <el-button size="small" type="success" @click="handleDownload(scope.row)">
                <el-icon><Download /></el-icon>
                下载
              </el-button>
              
              <!-- <el-button size="small" type="primary" @click="editResource(scope.row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button> -->
              
              <el-button size="small" type="danger" @click="deleteResource(scope.row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredResources.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 编辑资源对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑资源信息" width="500px">
      <el-form :model="editingResource" label-width="100px">
        <el-form-item label="资源名称" required>
          <el-input v-model="editingResource.resourceName" placeholder="请输入资源名称" />
        </el-form-item>
        
        <el-form-item label="资源描述">
          <el-input 
            v-model="editingResource.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入资源描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResourceEdit" :loading="loading.resources">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.admin-resource-container {
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  min-height: 100vh;
  animation: admin-page-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 头部区域 */
.header-section {
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.1s both;
}

.header-section h2 {
  color: white;
  margin: 0 0 20px 0;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  animation: admin-title-glow 3s ease-in-out infinite alternate;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px; /* 原本20px */
  margin-bottom: 20px;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.2s both;
}

.stat-card {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px; /* 原本16px */
  padding: 14px 16px;  /* 原本20px */
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15); /* 更轻的阴影 */
  color: #fff;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  animation: admin-card-slide-up 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  font-size: 20px; /* 原本28px */
  margin-bottom: 4px;
  color: white;
  animation: admin-icon-pulse 2s ease-in-out infinite;
}

.stat-value {
  font-size: 24px; /* 原本32px */
  font-weight: 600;
}

.stat-label {
  font-size: 20px; /* 原本16px */
  opacity: 0.75;
}

/* 工具栏 */
.toolbar-card {
  margin-bottom: 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  padding: 12px 16px;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.4s both;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

/* 针对 Element Plus 按钮和输入框的玻璃风样式 */
.toolbar .el-button,
.toolbar .el-input__inner,
.toolbar button,
.toolbar select,
.toolbar input {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(8px) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 8px !important;
  padding: 6px 12px !important;
  font-size: 14px !important;
  color: #fff !important;
  outline: none !important;
  transition: all 0.2s ease !important;
}

/* 悬停状态 */
.toolbar .el-button:hover,
.toolbar .el-input__inner:hover,
.toolbar button:hover,
.toolbar select:hover,
.toolbar input:hover {
  background: rgba(255, 255, 255, 0) !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1) !important;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

/* 筛选按钮或下拉组件样式（假设是 button 或 select） */
.filters button,
.filters select,
.filters input {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  padding: 6px 12px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.filters button:hover,
.filters select:hover,
.filters input:hover {
  background: rgba(255, 255, 255, 0.25);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 表格样式 */
.table-card {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  padding: 12px 16px;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.6s both;
}

.el-table th,
.el-table td {
  padding: 12px 16px !important; 
  white-space: nowrap;
}

.el-table .cell {
  overflow: hidden;
  text-overflow: ellipsis;
} 

/* 表格内容布局保持不变 */
.resource-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #fff; /* 玻璃背景下字体亮色更清晰 */
}

.name-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: black;
}

.download-count {
  color: #67c23a;
  font-weight: 600;
}

/* 操作按钮容器 */
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 按钮风格改成玻璃拟态 */
.action-buttons{
  min-width: 50px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.5); 
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5); 
  color: #fff;
  border-radius: 6px;
  transition: all 0.2s ease;
}
.el-button {
  min-width: 50px;
  padding: 4px 8px;
}

.action-buttons {
  background: rgba(255, 255, 255, 0.65); 
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: center;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.8s both;
}

@keyframes activities-fade-in {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.pagination-wrapper .el-pagination {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  border-radius: 10px;
  padding: 8px 16px;
  display: inline-flex;
  gap: 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.pagination-wrapper .el-pager li,
.pagination-wrapper .el-pagination__sizes,
.pagination-wrapper .el-pagination__jump {
  background-color: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  color: #fff;
  margin: 0 2px;
  transition: all 0.2s ease;
}

.pagination-wrapper .el-pager li:hover,
.pagination-wrapper .el-pagination__sizes:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 动画性能优化 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

@media (max-width: 768px) {
  .admin-resource-container {
    padding: 16px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filters, .actions {
    justify-content: space-between;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 2px;
  }
}

/* 卡片样式优化 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
}

.el-card :deep(.el-card__body) {
  padding: 24px;
}

/* 表格样式优化 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table :deep(.el-table__header-wrapper) {
  background: #f8f9fa;
}

.el-table :deep(.el-table__header th) {
  background: #f8f9fa;
  color: #333;
  font-weight: 600;
}

/* 按钮样式优化 */
.el-button {
  border-radius: 6px;
  font-weight: 500;
}

/* 标签样式 */
.el-tag {
  border-radius: 4px;
  font-weight: 500;
}
</style>