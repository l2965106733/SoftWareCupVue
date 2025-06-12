<script setup>
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';

const searchUser = ref({ name: '', gender: '', subject: '' })

const genders = ref([{ name: '男', value: 1 }, { name: '女', value: 2 }])

const user = ref([{
  id: '',              // 用户唯一ID
  username: '',          // 用户名
  password: '',          // 加密后的密码
  name: '',              // 姓名
  gender: '',            // 性别，可为 '男' / '女' / ''
  role: '',             // 角色，可为 '学生' / '教师' / '管理员'
  identifier: '',        // 学号/教师号/管理员号
  subject: '',           // 教师教学科目（仅教师）
  createTime: '',        // 注册时间（字符串）
  updateTime: ''         // 最后修改时间（字符串）
}])

const pageSize = ref(10)      // 每页数量
const total = ref(0)          // 总条数
const background = true
const currentPage = ref(1)    // 当前页

const handleSizeChange = (newSize) => {
  console.log(`每页 ${newSize} 条`);
};

const handleCurrentChange = (newPage) => {
  console.log(`当前页: ${newPage}`);
};

const handleSelectionChange = (val) => {
  console.log('选中项：', val)
}

</script>

<template>
  <h1>教师管理</h1>
  <div class="container">
    <el-form :inline="true" :model="searchUser" class="demo-form-inline">
      <el-form-item label="姓名">
        <el-input v-model="searchUser.name" placeholder="请输入教师姓名" clearable />
      </el-form-item>

      <el-form-item label="性别">
        <el-select v-model="searchUser.gender" placeholder="请选择性别" clearable>
          <el-option v-for="g in genders" :key="g.value" :label="g.name" :value="g.value" />
        </el-select>
        <el-form-item label="科目">
          <el-input v-model="searchUser.subject" placeholder="请输入教学科目" clearable />
        </el-form-item>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="">查询</el-button>
        <el-button type="primary" @click="">清空</el-button>
      </el-form-item>
    </el-form>
  </div>

  <div class=container>
    <el-button type="primary" @click="">新增教师</el-button>
    <el-button type="danger" @click="">批量删除</el-button>
  </div>

  <div class="container">
    <el-table :data="user" border style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="name" label="姓名" width="150" align="center" />
      <el-table-column label="性别" width="120" align="center">
        <template #default="scope">
          {{ scope.row.gender == '1' ? '男' : '女' }}
        </template>
      </el-table-column>
      <!-- {/* 头像 */}
      {/* <el-table-column prop="image" label="头像" width="120" align="center">
        <template #default="scope">
          <img :src="scope.row.image" alt="头像" style="height: 40px" align="center" />
        </template>
      </el-table-column> */} -->
      <el-table-column prop="identifier" label="教工号" width="150" align="center" />
      <el-table-column prop="subject" label="科目" width="150" align="center" />
      <el-table-column prop="udpateTime" label="最后操作时间" width="200" align="center" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button type="primary" @click="">编辑</el-button>
          <el-button type="danger" @click="">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="container">
    <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
      :page-sizes="[5, 10, 20, 50, 75, 100]" :background="background" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
  </div>


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
