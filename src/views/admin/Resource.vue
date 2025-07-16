<script setup>
import { ref, computed, onMounted } from 'vue'
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

// 预览文件
const handlePreview = (resource) => {
  if (!resource.resourceUrl) {
    ElMessage.warning('文件链接不存在')
    return
  }
  
  const url = resource.resourceUrl.toLowerCase()
  
  if (
    url.endsWith('.doc') || url.endsWith('.docx') ||
    url.endsWith('.ppt') || url.endsWith('.pptx') ||
    url.endsWith('.xls') || url.endsWith('.xlsx')
  ) {
    const officeUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(resource.resourceUrl)}`
    window.open(officeUrl, '_blank')
  } else {
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
  <div class="admin-resource-container">
    <!-- 页面标题和统计概览 -->
    <div class="header-section">
      <h2>📂 资源管理中心</h2>
      <div class="stats-cards" v-loading="loading.stats">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ resourceStats.totalResources }}</div>
            <div class="stat-label">总资源数</div>
          </div>
          <el-icon class="stat-icon"><Files /></el-icon>
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
        </div>
        
        <div class="actions">
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
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="resourceName" label="资源名称" min-width="200">
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
        
        <el-table-column prop="teacherName" label="上传教师" width="120" />
        
        <el-table-column prop="resourceType" label="类型" width="100">
          <template #default="scope">
            <el-tag 
              :color="getTypeColor(scope.row.resourceType)"
              effect="light"
              size="small"
            >
              {{ scope.row.resourceType }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="大小" width="120" sortable>
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="downloadCount" label="下载" width="80" sortable>
          <template #default="scope">
            <span class="download-count">{{ scope.row.downloadCount || 0 }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="uploadTime" label="上传时间" width="160" sortable>
          <template #default="scope">
            {{ formatDate(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        
        <el-table-column label="操作" width="240" fixed="right">
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
              
              <el-button size="small" type="primary" @click="editResource(scope.row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              
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
.admin-resource-container {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

/* 头部区域 */
.header-section h2 {
  color: white;
  margin: 0 0 20px 0;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
  overflow: hidden;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-icon {
  font-size: 40px;
  color: #409eff;
  opacity: 0.3;
}

/* 工具栏 */
.toolbar-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 表格样式 */
.table-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.resource-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.name-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.download-count {
  color: #67c23a;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  min-width: 50px;
  padding: 4px 8px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
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