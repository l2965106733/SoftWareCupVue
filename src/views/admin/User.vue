<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { pageQueryApi, deleteByIdApi, getUserInfoApi, updateUserApi, addUserApi, getAllStudentsApi, sumbitSelectStudentsApi } from '@/api/admin';

const searchUser = ref({ name: '', gender: '', subject: '', role: '' })

const genders = ref([{ name: '男', value: 1 }, { name: '女', value: 2 }])

const roles = ref([{ name: '学生', value: 1 }, { name: '教师', value: 2 }])

// 用于表单的角色选项（不包含管理员）
const formRoles = ref([{ name: '学生', value: 1 }, { name: '教师', value: 2 }])

const user = ref([{
  id: '',              // 用户唯一ID
  username: '',          // 用户名
  password: '',          // 加密后的密码
  name: '',              // 姓名
  gender: '',            // 性别，可为 '男' / '女' / ''
  role: 0,             // 角色，可为 '学生' / '教师' 
  identifier: '',        // 学号/教师号/管理员号
  createTime: '',        // 注册时间（字符串）
  updateTime: ''         // 最后修改时间（字符串）
}])

const role = computed(() => searchUser.value.role);
const tempRole = ref('')
const roleLabel = computed(() => {
  switch (role.value) {
    case 1:
      return '学生'
    case 2:
      return '教师'
    case 3:
      return '管理员'
    default:
      return '用户'
  }
})


const pageSize = ref(10)      // 每页数量
const total = ref(0)          // 总条数
const background = true
const currentPage = ref(1)    // 当前页

const tableRef = ref();

const search = async () => {
  searchUser.value.role = tempRole.value;
  const result = await pageQueryApi(
    searchUser.value.name, searchUser.value.gender, searchUser.value.role, searchUser.value.subject, currentPage.value, pageSize.value)

  if (result.code) {
    // 按角色排序：管理员(3) -> 教师(2) -> 学生(1)
    user.value = result.data.rows.sort((a, b) => {
      return b.role - a.role;
    });
    total.value = result.data.total;
    // 清空选择状态
    selectIds.value = [];
    // 清空表格选择
    if (tableRef.value) {
      tableRef.value.clearSelection();
    }
  }
}

onMounted(() => {
  search();

  // 强制显示表格标题
  setTimeout(() => {
    const headerElements = document.querySelectorAll('.modern-table .el-table__header-wrapper');
    headerElements.forEach(el => {
      el.style.display = 'block';
      el.style.visibility = 'visible';
      el.style.opacity = '1';
      el.style.height = 'auto';
    });

    const thElements = document.querySelectorAll('.modern-table .el-table__header th');
    thElements.forEach(el => {
      el.style.color = '#ffffff';
      el.style.background = 'rgba(255, 255, 255, 0.2)';
      el.style.display = 'table-cell';
    });
  }, 100);
})

const clear = () => {
  searchUser.value = { name: '', gender: '', subject: '', role: '' };
  tempRole.value = '';
  search();
}


const handleSizeChange = async () => {
  search();
}
const handleCurrentChange = () => {
  search();
}

const handleSelectionChange = (val) => {
  selectIds.value = val.map(item => item.id);
  selectedStudentIds.value = val.map(item => item.id);
}

const deleteById = async (id) => {
  ElMessageBox.confirm('是否确认删除该员工？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const result = await deleteByIdApi(id);
    if (result.code) {
      ElMessage.success('删除成功');
      search();
    } else {
      ElMessage.error(result.msg);
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
}

const selectIds = ref([]);

const deleteByIds = () => {
  if (!selectIds.value || selectIds.value.length === 0) {
    ElMessage.info('请先选择要删除的记录');
    return;
  }

  ElMessageBox.confirm(`是否确认批量删除选中的 ${selectIds.value.length} 条记录？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const result = await deleteByIdApi(selectIds.value);
    if (result.code) {
      ElMessage.success('批量删除成功');
      selectIds.value = []; // 清空选择项
      search(); // 重新加载数据
    } else {
      ElMessage.error(result.msg || '批量删除失败');
    }
  }).catch(() => {
    ElMessage.info('已取消批量删除');
  });
}

const dialogVisible = ref(false)
const formLabelWidth = '140px'
const saveLoading = ref(false)

const formUser = ref({
  name: '',
  username: '',
  gender: '',
  role: '',
  identifier: '',
  subject: ''
})

const formUserRef = ref();

const dialogTitle = ref('新增用户')

// 表单验证规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度应为2-20个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度应为2-10个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  identifier: [
    { required: true, message: '请输入用户号', trigger: 'blur' },
    { min: 5, max: 20, message: '用户号长度应为5-20个字符', trigger: 'blur' }
  ],
}

const save = async () => {
  console.log('=== save函数被调用 ===');
  console.log('formUserRef存在:', !!formUserRef.value);
  console.log('当前表单数据:', JSON.stringify(formUser.value, null, 2));

  if (saveLoading.value) {
    console.log('⏳ 正在保存中，忽略重复点击');
    return;
  }

  if (!formUserRef.value) {
    console.log('❌ formUserRef不存在，退出');
    ElMessage.error('表单引用不存在');
    return;
  }

  // 先检查必填字段
  const requiredFields = ['username', 'name', 'gender', 'role', 'identifier'];
  const missingFields = requiredFields.filter(field => !formUser.value[field]);

  if (missingFields.length > 0) {
    console.log('❌ 缺少必填字段:', missingFields);
    ElMessage.error(`请填写必填字段: ${missingFields.join(', ')}`);
    return;
  }


  console.log('✅ 基础验证通过，开始表单验证');

  try {
    const isValid = await new Promise((resolve) => {
      formUserRef.value.validate((valid, fields) => {
        console.log('表单验证结果:', valid);
        if (!valid) {
          console.log('验证失败的字段:', fields);
        }
        resolve(valid);
      });
    });

    if (isValid) {
      console.log('✅ 表单验证通过，准备提交');
      saveLoading.value = true;
      try {
        let result;
        const userData = { ...formUser.value };

        if (userData.id) {
          console.log('🔄 执行更新用户');
          result = await updateUserApi(userData);
        } else {
          console.log('➕ 执行新增用户');
          delete userData.id; // 确保新增时删除id字段
          result = await addUserApi(userData);
        }

        console.log('📝 API返回结果:', result);

        if (result && result.code) {
          console.log('✅ 保存成功');
          ElMessage.success('用户信息保存成功');
          dialogVisible.value = false;
          await search();
        } else {
          console.log('❌ 保存失败:', result);
          ElMessage.error(result?.msg || '保存失败，请重试');
        }
      } catch (error) {
        console.error('❌ API调用错误:', error);
        ElMessage.error(`网络错误: ${error.message || '请检查网络连接'}`);
      } finally {
        saveLoading.value = false;
      }
    } else {
      console.log('❌ 表单验证失败');
      ElMessage.error('请检查并完善表单信息');
    }
  } catch (error) {
    console.error('❌ 表单验证异常:', error);
    ElMessage.error('验证失败，请重试');
  }
}

const edit = async (id) => {
  const result = await getUserInfoApi(id);
  if (result.code) {
    formUser.value = result.data;
    dialogVisible.value = true;
    dialogTitle.value = '修改用户';
  } else {
    ElMessage.error(result.msg);
  }
}

const addUser = () => {
  console.log('=== 开始新增用户 ===');
  dialogVisible.value = true;
  dialogTitle.value = '新增用户';

  // 重置表单数据
  formUser.value = {
    id: undefined, // 新增时不需要id
    name: '',
    username: '',
    gender: '',
    role: '',
    identifier: '',
    subject: ''
  }

  console.log('初始化表单数据:', formUser.value);

  // 延迟重置表单验证，确保表单渲染完成
  setTimeout(() => {
    if (formUserRef.value) {
      console.log('重置表单验证状态');
      formUserRef.value.resetFields();
      formUserRef.value.clearValidate();
    }
  }, 100);
}

const studentDialogVisible = ref(false);
const allStudents = ref([]);
const relatedStudents = ref([]);
const recentTeacherId = ref(null);
const selectedStudentIds = ref([]);
const studentTableRef = ref(null);

const openStudentDialog = async (teacherId) => {
  try {
    const res = await getAllStudentsApi(teacherId);
    if (res.code) {
      studentDialogVisible.value = true;
      allStudents.value = res.data.all || [];
      const relatedIds = (res.data.related || []).map(student => student.id);
      relatedStudents.value = relatedIds;
      recentTeacherId.value = teacherId;
      nextTick(() => {
        setTimeout(() => {
          allStudents.value.forEach((student) => {
            if (relatedStudents.value.includes(student.id)) {
              studentTableRef.value.toggleRowSelection(student, true);
            }
          });
        }, 50);
      });
    } else {
      console.warn('API returned error code');
    }
  } catch (err) {
    console.error(err);
    ElMessage.error("获取学生数据失败");
  }
};




/**
 * 提交选中学生
 */
const submitStudentSelection = async () => {
  try {
    const result = await sumbitSelectStudentsApi(selectedStudentIds.value, recentTeacherId.value);
    ElMessage.success("学生关系已更新");
    studentDialogVisible.value = false;
  }
  catch (err) {
    console.error(err);
    ElMessage.error("更新学生关系失败");
  }
};

</script>

<template>
  <div class="user-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-users" style="color: white;"></i>
        {{ roleLabel }}管理
      </h1>
      <p class="page-subtitle">管理系统中的所有用户信息</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-card">
      <div class="card-header">
        <i class="fas fa-search"></i>
        <span>筛选条件</span>
      </div>
      <!-- 查询项 + 查询/重置按钮，第一行 -->
      <el-form :inline="true" :model="searchUser" class="search-form">
        <!-- 查询字段 -->
        <el-form-item label="角色">
          <el-select v-model="tempRole" placeholder="请选择查询角色" clearable class="form-select">
            <el-option v-for="r in roles" :key="r.value" :label="r.name" :value="r.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="searchUser.name" :placeholder="`请输入${roleLabel}姓名`" clearable class="form-input" />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="searchUser.gender" placeholder="请选择性别" clearable class="form-select">
            <el-option v-for="g in genders" :key="g.value" :label="g.name" :value="g.value" />
          </el-select>
        </el-form-item>

        <!-- 所有按钮：查询、重置、新增、删除，一起放在右侧 -->
        <div class="action-row">
          <div class="action-left">
            <el-button type="primary" @click="search" class="search-btn">
              <i class="fas fa-search"></i> 查询
            </el-button>
            <el-button @click="clear" class="clear-btn">
              <i class="fas fa-redo"></i> 重置
            </el-button>
            <!-- <el-button type="primary" @click="addUser" class="add-btn">
        <i class="fas fa-plus"></i> 新增{{ roleLabel }}
      </el-button> -->
            <el-button type="danger" @click="deleteByIds" class="delete-btn">
              <i class="fas fa-trash-alt"></i> 批量删除
            </el-button>
          </div>
          <div class="record-count">共 {{ total }} 条记录</div>
        </div>
      </el-form>

    </div>


    <!-- 数据表格 -->
    <div class="table-card">
      <el-table style="border-radius: 10px; margin-bottom: 10px;" ref="tableRef" :data="user" :show-header="true"
        :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }"
        @selection-change="handleSelectionChange">
        <!-- 多选 -->
        <el-table-column type="selection" align="center" width="40">
          <template #header>
            <span style="color: #ffffff; font-weight: 700;">全选</span>
          </template>
        </el-table-column>

        <!-- 用户名 -->
        <el-table-column prop="username" label="用户名" align="center" min-width="80" show-overflow-tooltip
          style="color:black">
          <template #default="scope">
            <div>
              <i class="fas fa-user"></i>
              {{ scope.row.username }}
            </div>
          </template>
        </el-table-column>

        <!-- 姓名 -->
        <el-table-column prop="name" label="姓名" align="center" min-width="80" show-overflow-tooltip style="color:black">
          <template #default="scope">
            <div>
              {{ scope.row.name }}
            </div>
          </template>
        </el-table-column>

        <!-- 性别 -->
        <el-table-column label="性别" align="center" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.gender == '1' ? 'primary' : 'danger'" size="small" class="gender-tag">
              <i :class="scope.row.gender == '1' ? 'fas fa-mars' : 'fas fa-venus'"></i>
              {{ scope.row.gender == '1' ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 角色 -->
        <el-table-column label="角色" align="center" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.role == 3 ? 'danger' : scope.row.role == 2 ? 'warning' : 'success'" size="small"
              class="role-tag">
              <i
                :class="scope.row.role == 3 ? 'fas fa-crown' : scope.row.role == 2 ? 'fas fa-chalkboard-teacher' : 'fas fa-graduation-cap'"></i>
              {{ scope.row.role == 3 ? '管理员' : scope.row.role == 2 ? '教师' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 用户号 -->
        <el-table-column prop="identifier" label="用户号" align="center" min-width="80" style="color:black">
          <template #default="scope">
            <div>
              <i class="fas fa-id-card"></i>
              {{ scope.row.identifier }}
            </div>
          </template>
        </el-table-column>



        <!-- 操作按钮 -->
        <el-table-column label="操作" align="center" width="160">
          <template #default="scope">
            <div class="action-buttons">
              <span>
                <el-button type="primary" size="small" @click="edit(scope.row.id)">
                  <i class="fas fa-edit"></i>
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="deleteById(scope.row.id)">
                  <i class="fas fa-trash"></i>
                  删除
                </el-button>
              </span>
              <span style="width:  85%;">
                <el-button v-if="scope.row.role == 2" align="center" type="info" size="small"
                  @click="openStudentDialog(scope.row.id)">
                  <i class="fas fa-pen"></i>关联学生
                </el-button>
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-card">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50, 75, 100]" :background="background"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" class="modern-pagination" />
      </div>
    </div>

    <el-dialog title="关联学生" v-model="studentDialogVisible" width="600px">

      <el-table :data="allStudents" style="width: 100%" ref="studentTableRef" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="学生姓名"></el-table-column>
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="studentDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitStudentSelection">确 定</el-button>
        </span>
      </template>
    </el-dialog>


    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" class="modern-dialog">
      <el-form :model="formUser" :rules="formRules" ref="formUserRef" label-width="100px" class="dialog-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formUser.username" placeholder="请输入用户名" clearable>
            <template #prefix>
              <i class="fas fa-user"></i>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="formUser.name" placeholder="请输入姓名" clearable>
            <template #prefix>
              <i class="fas fa-signature"></i>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="formUser.gender" placeholder="请选择性别" clearable style="width: 100%">
            <el-option v-for="g in genders" :key="g.value" :label="g.name" :value="g.value">
              <i :class="g.value === 1 ? 'fas fa-mars' : 'fas fa-venus'"></i>
              {{ g.name }}
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="formUser.role" placeholder="请选择角色" clearable style="width: 100%">
            <el-option v-for="r in formRoles" :key="r.value" :label="r.name" :value="r.value">
              <i :class="r.value == 2 ? 'fas fa-chalkboard-teacher' : 'fas fa-graduation-cap'"></i>
              {{ r.name }}
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="用户号" prop="identifier">
          <el-input v-model="formUser.identifier" placeholder="请输入用户号" clearable>
            <template #prefix>
              <i class="fas fa-id-card"></i>
            </template>
          </el-input>
        </el-form-item>

      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" class="cancel-btn">
            <i class="fas fa-times"></i>
            取消
          </el-button>
          <el-button type="primary" @click="save" :loading="saveLoading" class="confirm-btn">
            <i class="fas fa-check" v-if="!saveLoading"></i>
            {{ saveLoading ? '保存中...' : '确认' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  padding: 24px;
  min-height: 100vh;
  animation: admin-page-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-title {
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title i {
  color: #f43f5e;
  font-size: 0.9em;
}

.page-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  margin: 0;
}

/* 卡片通用样式 */
.search-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, box-shadow;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.2s both;
}

.action-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, box-shadow;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.4s both;
}

.table-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, box-shadow;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.6s both;
}

.pagination-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, box-shadow;
  animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.8s both;
}






/* 覆盖分页按钮背景和边框 */
.modern-pagination>>>.el-pager li,
.modern-pagination>>>.el-pagination__sizes {
  background-color: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  color: white;
}

/* 高亮页码 */
.modern-pagination>>>.el-pager li.is-active {
  background-color: #409eff !important;
  color: white !important;
  border: none;
}

/* hover 效果 */
.modern-pagination>>>.el-pager li:hover {
  background-color: rgba(255, 255, 255, 0.3);
}


.search-card:hover,
.action-card:hover,
.table-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 35px rgba(0, 0, 0, 0.18);
}

.resource-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #fff;
  /* 玻璃背景下字体亮色更清晰 */
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

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  /* 小屏换行用 */
  margin-top: 16px;
  gap: 12px;
}

.action-left {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.record-count {
  font-size: 14px;
  color: white;
  white-space: nowrap;
}

/* 搜索卡片 */
.search-card {
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #ffffff;
  font-weight: 600;
  font-size: 16px;
}

.card-header i {
  color: #f43f5e;
}


.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: end;
}

.search-form .el-form-item {
  margin-bottom: 0;
}

.form-input,
.form-select {
  min-width: 200px;
}

.form-buttons {
  display: flex;
  gap: 12px;
}

.form-buttons .form-action {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: end;
}

.search-btn,
.clear-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-btn {
  background: linear-gradient(135deg, #be123c, #e11d48);
  border: none;
  color: white;
}

.search-btn:hover {
  background: linear-gradient(135deg, #9f1239, #be123c);
  transform: translateY(-1px);
}

.clear-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

/* 操作按钮区域 */
.action-card {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.add-btn,
.delete-btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.add-btn {
  background: linear-gradient(135deg, #be123c, #e11d48);
  border: none;
  color: white;
}

.add-btn:hover {
  background: linear-gradient(135deg, #9f1239, #be123c);
  transform: translateY(-1px);
}

.delete-btn {
  background: linear-gradient(135deg, #dc2626, #ef4444);
  border: none;
  color: white;
}

.delete-btn:hover {
  background: linear-gradient(135deg, #b91c1c, #dc2626);
  transform: translateY(-1px);
}

.table-info {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

/* 表格卡片 */
.table-card {
  padding: 24px;
  overflow-x: auto;
  width: 100%;
  margin-bottom: 10px;
}

/* 确保表格完全填充容器 */
/* .modern-table :deep(.el-table) {
  width: 100% !important;
}

.modern-table {
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
} */

/* 表格标题 - 更清晰可见的设计 */
/* .modern-table :deep(.el-table__header-wrapper) {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.8), rgba(190, 18, 60, 0.9)) !important;
  border-radius: 12px 12px 0 0 !important;
}

.modern-table :deep(.el-table__header) {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.8), rgba(190, 18, 60, 0.9)) !important;
}

.modern-table :deep(.el-table__header th) {
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.8), rgba(190, 18, 60, 0.9)) !important;
  color: #ffffff !important;
  font-weight: 700 !important;
  font-size: 14px !important;
  text-align: center !important;
  padding: 18px 12px !important;
  border: none !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3) !important;
}

.modern-table :deep(.el-table__header th .cell) {
  color: #ffffff !important;
  font-weight: 700 !important;
  text-align: center !important;
} */

/* 表格数据行 */
/* .modern-table :deep(.el-table__body tr) {
  background: rgba(255, 255, 255, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.modern-table :deep(.el-table__body tr:nth-child(even)) {
  background: rgba(255, 255, 255, 0.12);
}

.modern-table :deep(.el-table__body tr:hover) {
  background: rgba(255, 255, 255, 0.2) !important;
  transform: scale(1.005) translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.modern-table :deep(.el-table__body td) {
  border: none !important;
  color: #ffffff !important;
  padding: 16px 12px !important;
  text-align: center !important;
  vertical-align: middle !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2) !important;
  font-weight: 500 !important;
}

.modern-table :deep(.el-table__body td .cell) {
  text-align: center !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
} */

/* 标签样式优化 */
/* .gender-tag,
.role-tag,
.subject-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  padding: 6px 10px;
  font-size: 12px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  border: none !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.gender-tag i,
.role-tag i,
.subject-tag i {
  font-size: 11px;
  min-width: 11px;
  color: #ffffff;
  flex-shrink: 0;
} */

/* 自定义标签颜色 - 稍微深一点 */
/* .gender-tag.el-tag--primary {
  background: linear-gradient(135deg, #1e40af, #1d4ed8) !important;
  color: #ffffff !important;
}

.gender-tag.el-tag--primary:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e3a8a) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(30, 64, 175, 0.3);
}

.gender-tag.el-tag--danger {
  background: linear-gradient(135deg, #be123c, #dc2626) !important;
  color: #ffffff !important;
}

.gender-tag.el-tag--danger:hover {
  background: linear-gradient(135deg, #dc2626, #991b1b) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(190, 18, 60, 0.3);
}

.role-tag.el-tag--danger {
  background: linear-gradient(135deg, #7c2d12, #9a3412) !important;
  color: #ffffff !important;
}

.role-tag.el-tag--danger:hover {
  background: linear-gradient(135deg, #9a3412, #7c2d12) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(124, 45, 18, 0.3);
}

.role-tag.el-tag--warning {
  background: linear-gradient(135deg, #a16207, #ca8a04) !important;
  color: #ffffff !important;
}

.role-tag.el-tag--warning:hover {
  background: linear-gradient(135deg, #ca8a04, #92400e) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(161, 98, 7, 0.3);
}

.role-tag.el-tag--success {
  background: linear-gradient(135deg, #166534, #15803d) !important;
  color: #ffffff !important;
}

.role-tag.el-tag--success:hover {
  background: linear-gradient(135deg, #15803d, #14532d) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(22, 101, 52, 0.3);
}

.subject-tag.el-tag--success {
  background: linear-gradient(135deg, #166534, #15803d) !important;
  color: #ffffff !important;
}

.subject-tag.el-tag--success:hover {
  background: linear-gradient(135deg, #15803d, #14532d) !important;
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 3px 8px rgba(22, 101, 52, 0.3);
} */

.table-card {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  padding: 12px 16px;
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

/* 分页器 */
.pagination-card {
  padding: 20px 24px;
  display: flex;
  justify-content: center;
}

.modern-pagination :deep(.el-pagination) {
  color: rgba(255, 255, 255, 0.9);
}

.modern-pagination :deep(.el-pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  border-radius: 6px;
  margin: 0 2px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.modern-pagination :deep(.el-pagination .el-pager li:hover) {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.modern-pagination :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #be123c, #e11d48);
  color: white;
  box-shadow: 0 2px 8px rgba(190, 18, 60, 0.3);
}

/* 对话框样式 */
.modern-dialog :deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.modern-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #be123c, #e11d48);
  color: white;
  padding: 20px 24px;
  border-radius: 16px 16px 0 0;
}

.modern-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
}

.dialog-form {
  padding: 24px;
}

.dialog-form :deep(.el-form-item__label) {
  color: #374151;
  font-weight: 500;
}

.dialog-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background: #ffffff !important;
}

.dialog-form :deep(.el-input__inner) {
  color: #374151 !important;
  background: #ffffff !important;
}

.dialog-form :deep(.el-input__inner::placeholder) {
  color: #9ca3af !important;
}

.dialog-form :deep(.el-select .el-input__wrapper) {
  background: #ffffff !important;
}

.dialog-form :deep(.el-select .el-input__inner) {
  color: #374151 !important;
  background: #ffffff !important;
}

.dialog-footer {
  padding: 0 24px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.confirm-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cancel-btn {
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  color: #374151;
}

.confirm-btn {
  background: linear-gradient(135deg, #be123c, #e11d48);
  border: none;
  color: white;
}

/* 性能优化 - 硬件加速和减少重绘 */
.user-management {
  /* 创建新的堆叠上下文 */
  transform: translateZ(0);
  backface-visibility: hidden;
  perspective: 1000px;
}

.modern-table :deep(.el-table__body tr),
.search-card,
.action-card,
.table-card,
.pagination-card,
.gender-tag,
.role-tag,
.subject-tag,
.edit-btn,
.delete-btn-small,
.search-btn,
.clear-btn,
.add-btn,
.delete-btn {
  /* 启用硬件加速 */
  transform: translateZ(0);
  backface-visibility: hidden;
}

/* 减少动画期间的重绘 */
@media (prefers-reduced-motion: no-preference) {
  * {
    scroll-behavior: smooth;
  }
}

/* 为有动画的元素预留GPU资源 */
.modern-table :deep(.el-table__body tr:hover),
.search-card:hover,
.action-card:hover,
.table-card:hover,
.edit-btn:hover,
.delete-btn-small:hover {
  contain: layout style paint;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .table-card {
    padding: 16px;
  }

  .modern-table :deep(.el-table__body td),
  .modern-table :deep(.el-table__header th) {
    padding: 12px 8px;
  }
}

@media (max-width: 768px) {
  .user-management {
    padding: 16px;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .form-input,
  .form-select {
    min-width: auto;
    width: 100%;
  }

  .action-card {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .action-buttons {
    justify-content: center;
  }

  .table-card {
    padding: 12px;
    overflow-x: auto;
  }

  .modern-table {
    min-width: 700px;
  }

  .modern-table :deep(.el-table__body td),
  .modern-table :deep(.el-table__header th) {
    padding: 10px 6px;
  }
}

/* Element Plus 组件样式覆盖 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
}

:deep(.el-input__inner) {
  color: white;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

:deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

/* 强制显示表格标题 - 多重选择器确保生效
.modern-table :deep(.el-table thead),
.modern-table :deep(.el-table__header thead),
.modern-table :deep(thead) {
  color: #ffffff !important;
  display: table-header-group !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.modern-table :deep(.el-table thead th),
.modern-table :deep(.el-table__header thead th),
.modern-table :deep(thead th),
.modern-table :deep(th) {
  color: #ffffff !important;
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.8), rgba(190, 18, 60, 0.9)) !important;
  display: table-cell !important;
  visibility: visible !important;
  opacity: 1 !important;
  font-weight: 700 !important;
  padding: 18px 12px !important;
  border: none !important;
  text-align: center !important;
  vertical-align: middle !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3) !important;
}

.modern-table :deep(.el-table thead th div),
.modern-table :deep(.el-table__header thead th div),
.modern-table :deep(thead th div),
.modern-table :deep(th div),
.modern-table :deep(.cell) {
  color: #ffffff !important;
  font-weight: 700 !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.modern-table :deep(.el-table thead th span),
.modern-table :deep(.el-table__header thead th span),
.modern-table :deep(thead th span),
.modern-table :deep(th span) {
  color: #ffffff !important;
  font-weight: 700 !important;
  display: inline !important;
  visibility: visible !important;
  opacity: 1 !important;
} */

/* 确保表格结构完整 */
.modern-table :deep(.el-table) {
  display: table !important;
  width: 100% !important;
}

.modern-table :deep(.el-table__header-wrapper) {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  height: auto !important;
  overflow: visible !important;
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.8), rgba(190, 18, 60, 0.9)) !important;
  min-height: 56px !important;
  border-radius: 12px 12px 0 0 !important;
}

/* 表格数据行样式 - 与上面的样式保持一致 */
/* 注意：这里的样式已在上面定义，删除重复定义以避免冲突 */

.modern-table :deep(.el-table__body td) {
  border: none !important;
  color: #ffffff !important;
  padding: 16px 12px !important;
  text-align: center !important;
  vertical-align: middle !important;
  height: 60px !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2) !important;
  font-weight: 500 !important;
}

.modern-table :deep(.el-table__body td .cell) {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  height: 100% !important;
  width: 100% !important;
}

.modern-table :deep(.el-table__header th .cell) {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  height: 100% !important;
  width: 100% !important;
  color: #ffffff !important;
  font-weight: 700 !important;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 100%;
  text-align: center;
  font-weight: 500;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.user-info i {
  color: rgba(244, 63, 94, 0.8);
  font-size: 13px;
  min-width: 13px;
  flex-shrink: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.action-buttons-cell {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  flex-wrap: nowrap;
  min-width: 160px;
}

.edit-btn,
.delete-btn-small {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 68px;
  justify-content: center;
  height: 32px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, box-shadow;
}

.edit-btn {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border: none;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.edit-btn:hover {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.delete-btn-small {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  border: none;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.delete-btn-small:hover {
  background: linear-gradient(135deg, #b91c1c, #991b1b);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}
</style>
