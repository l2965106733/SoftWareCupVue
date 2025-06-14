<script setup>
import { useRouter } from 'vue-router'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { loginApi } from '@/api/public'

const router = useRouter()

// 控制显示哪个模块（null 为入口界面）
const role = ref(0)
const selectedRole = computed(() => {
  switch (role.value) {
    case 1:
      return 'student'
    case 2:
      return 'teacher'
    case 3:
      return 'admin'
  }
})
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
// 登录表单
const loginForm = ref({
  username: '',
  password: ''
})

// 登录操作
const login = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.error('用户名或密码不能为空')
    return
  }
  const result = await loginApi(loginForm.value)
  if (result.code && result.data.role === role.value) {
    ElMessage.success('登录成功')
    localStorage.setItem('loginUser', JSON.stringify(result.data))
    // 跳转至对应角色页面
    router.push(`/${selectedRole.value}/home`)
  }
  else {
    ElMessage.error('用户或密码不正确')
  }
}

// 返回入口界面
const back = () => {
  role.value = null
  loginForm.value.username = ''
  loginForm.value.password = ''
}

// 忘记密码
const reset = () => {
  router.push(`/reset?mode=forget&role=${role.value}`)
}
</script>

<template>
  <div id="container">
    <!-- 初始入口选择 -->
    <div v-if="!role" class="enter_select">
      <el-button type="primary" @click="role = 1">学生入口</el-button>
      <el-button type="success" @click="role = 2">教师入口</el-button>
      <el-button type="info" @click="role = 3">管理员入口</el-button>
    </div>

    <!-- 登录表单界面 -->
    <div v-else class="login-form">
      <el-form label-width="80px">
        <p class="title">{{ role === 1 ? '学生' : role === 2 ? '教师' : '管理员' }}登录</p>

        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>

        <el-form-item label="密码">
          <el-input type="password" v-model="loginForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button class="button" type="info" @click="back">返回</el-button>
          <el-button class="button" type="info" @click="reset">忘记密码</el-button>
          <el-button class="button" type="primary" @click="login">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
#container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f2f2f2;
}

.enter_select {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-form {
  width: 350px;
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
}
</style>
