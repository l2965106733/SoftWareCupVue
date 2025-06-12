<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { pageQueryApi, deleteByIdApi, getUserInfoApi, updateUserApi, addUserApi } from '@/api/admin';

const searchUser = ref({ name: '', gender: '', subject: '', role: '' })

const genders = ref([{ name: '男', value: 1 }, { name: '女', value: 2 }])

const roles = ref([{ name: '学生', value: 1 }, { name: '教师', value: 2 }, { name: '管理员', value: 3 }])

const user = ref([{
  id: '',              // 用户唯一ID
  username: '',          // 用户名
  password: '',          // 加密后的密码
  name: '',              // 姓名
  gender: '',            // 性别，可为 '男' / '女' / ''
  role: 0,             // 角色，可为 '学生' / '教师' / '管理员'
  identifier: '',        // 学号/教师号/管理员号
  subject: '',           // 教师教学科目（仅教师）
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

const search = async () => {
  searchUser.value.role = tempRole.value;
  const result = await pageQueryApi(
    searchUser.value.name, searchUser.value.gender, searchUser.value.role, searchUser.value.subject, currentPage.value, pageSize.value)

  if (result.code) {
    user.value = result.data.rows;
    total.value = result.data.total;
  }
}

onMounted(() => {
  search();
})

const clear = () => {
  searchUser.value = { name: '', gender: '', date: [], role: 0, subject: '' };
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
  ElMessageBox.confirm('是否确认批量删除选中的员工？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {

    if (selectIds.value && selectIds.value.length > 0) {
      const result = await deleteByIdApi(selectIds.value);
      if (result.code) {
        ElMessage.success('批量删除成功');
        search();
        selectIds.value = [];
      } else {
        ElMessage.error(result.msg);
      }
    } else {
      ElMessage.info('您没有选择任何记录');
    }
  }).catch(() => {
    ElMessage.info('已取消批量删除');
  });
}

const dialogVisible = ref(false)
const formLabelWidth = '140px'

const formUser = ref({
  name: '',
  username: '',
  gender: '',
  role: '',
  identifier: '',
  subject: ''
})

const formUserRef = ref();

const dialogTitle = ref('新增' + roleLabel.value)

const save = async () => {
  if (!formUserRef.value) { return; }
  formUserRef.value.validate(async (valid) => {
    if (valid) {
      let result;
      if (formUser.value.id) {
        result = await updateUserApi(formUser.value);
      }
      else {
        result = await addUserApi(formUser.value);
      }
      if (result.code) {
        ElMessage.success(roleLabel + '信息保存成功');
        dialogVisible.value = false;
        search();
      } else {
        ElMessage.error(result.msg);
      }
    }
    else {
      ElMessage.error('表单校验不通过');
    }
  });
}

const edit = async (id) => {
  const result = await getUserInfoApi(id);
  if (result.code) {
    employee.value = result.data;
    dialogVisible.value = true;
    dialogTitle.value = '修改' + roleLabel;
  } else {
    ElMessage.error(result.msg);
  }
}

const addUser = () => {
  dialogVisible.value = true;
  dialogTitle.value = '新增' + roleLabel.value;
  formUser.value = {
    name: '',
    username: '',
    gender: '',
    role: '',
    identifier: '',
    subject: ''
  }
  if (formUserRef.value) {
    formUserRef.value.resetFields();
  }
}
</script>

<template>
  <h1>{{ roleLabel }}管理</h1>
  <div class="container">
    <el-form :inline="true" :model="searchUser" class="demo-form-inline">
      <el-form-item label="角色">
        <el-select v-model="tempRole" placeholder="请选择查询角色" clearable>
          <el-option v-for="r in roles" :key="r.value" :label="r.name" :value="r.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="姓名">
        <el-input v-model="searchUser.name" :placeholder="`请输入${roleLabel}姓名`" clearable />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="searchUser.gender" placeholder="请选择性别" clearable>
          <el-option v-for="g in genders" :key="g.value" :label="g.name" :value="g.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="科目" v-if="role === 2">
        <el-input v-model="searchUser.subject" placeholder="请输入教学科目" clearable />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="primary" @click="clear">清空</el-button>
      </el-form-item>
    </el-form>
  </div>

  <div class=container>
    <el-button type="primary" @click="addUser">新增{{ roleLabel }}</el-button>
    <el-button type="danger" @click="deleteByIds">批量删除</el-button>
  </div>

  <div class="container">
    <el-table :data="user" border style="width: 100%" @selection-change="handleSelectionChange">
      <!-- 多选 -->
      <el-table-column type="selection" align="center" />

      <!-- 姓名 -->
      <el-table-column prop="name" label="姓名" align="center" />

      <!-- 性别 -->
      <el-table-column label="性别" align="center">
        <template #default="scope">
          {{ scope.row.gender == '1' ? '男' : '女' }}
        </template>
      </el-table-column>

      <!-- 用户号 -->
      <el-table-column prop="identifier" :label="`${roleLabel}号`" align="center" />

      <!-- 科目（仅教师显示） -->
      <el-table-column v-if="role === 2" prop="subject" label="科目" align="center" />

      <!-- 最后操作时间 -->
      <el-table-column prop="updateTime" label="最后操作时间" align="center" />

      <!-- 操作按钮 -->
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button type="primary" @click="edit(scope.row.id)">编辑</el-button>
          <el-button type="danger" @click="deleteById(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="container">
    <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
      :page-sizes="[5, 10, 20, 50, 75, 100]" :background="background" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
  </div>

  <el-dialog v-model="dialogVisible" :title=dialogTitle>
    <el-form :model="formUser" ref="formUserRef">
      <el-form-item label="用户名" :label-width="formLabelWidth">
        <el-input v-model="formUser.username" :placeholder="`请输入用户名`" clearable />
      </el-form-item>

      <el-form-item label="姓名" :label-width="formLabelWidth">
        <el-input v-model="formUser.name" :placeholder="`请输入${roleLabel}姓名`" clearable />
      </el-form-item>

      <el-form-item label="性别" :label-width="formLabelWidth">
        <el-select v-model="formUser.gender" placeholder="请选择性别" clearable>
          <el-option v-for="g in genders" :key="g.value" :label="g.name" :value="g.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="角色" :label-width="formLabelWidth">
        <el-select v-model="formUser.role" placeholder="请选择查询角色" clearable>
          <el-option v-for="r in roles" :key="r.value" :label="r.name" :value="r.value" />
        </el-select>
      </el-form-item>

      <el-form-item :label="`用户号`" :label-width="formLabelWidth">
        <el-input v-model="formUser.identifier" placeholder="请输入用户号" clearable />
      </el-form-item>

      <el-form-item label="科目" v-if="formUser.role === 2" :label-width="formLabelWidth">
        <el-input v-model="formUser.subject" placeholder="请输入教学科目" clearable />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="save">Confirm</el-button>
      </span>
    </template>
  </el-dialog>


</template>


<style scoped>
.container {
  padding: 10px;
}

.avatar {
  height: 40px;
}

.avatar-uploader .avatar {
  width: 78px;
  height: 78px;
  display: block;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 78px;
  height: 78px;
  text-align: center;
  border-radius: 10px;
  /* 添加灰色的虚线边框 */
  border: 1px dashed var(--el-border-color);
}
</style>
