<script setup>
import { useRouter, useRoute } from 'vue-router'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { registerApi } from '@/api/public'

const router = useRouter()
const route = useRoute()

const role = ref(Number(route.query.role || 1))
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

// 注册表单
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  email: '',
  role: role.value
})

// 注册操作
const register = async () => {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.name) {
    ElMessage.error('请填写完整信息')
    return
  }
  
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  
  if (registerForm.value.password.length < 6) {
    ElMessage.error('密码长度至少6位')
    return
  }
  
  try {
    const result = await registerApi(registerForm.value)
    if (result.code) {
      ElMessage.success('注册成功，请登录')
      router.push('/')
    } else {
      ElMessage.error(result.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败，请重试')
  }
}

// 返回登录
const backToLogin = () => {
  router.push('/')
}
</script>

<template>
  <div id="container">
    <div class="register-form page-transition">
      <el-form label-width="80px">
        <p class="title">{{ roleLabel }}注册</p>
        
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        
        <el-form-item label="姓名">
          <el-input v-model="registerForm.name" placeholder="请输入真实姓名"></el-input>
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱地址" type="email"></el-input>
        </el-form-item>
        
        <el-form-item label="密码">
          <el-input type="password" v-model="registerForm.password" placeholder="请输入密码（至少6位）"></el-input>
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码"></el-input>
        </el-form-item>
        
        <div class="button-group">
          <el-button class="register-button back-btn" @click="backToLogin">返回登录</el-button>
          <el-button class="register-button submit-btn" type="primary" @click="register">立即注册</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
#container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 70px);
  padding: 20px;
}

.register-form {
  width: 400px;
  background: rgba(255,255,255,0.92);
  padding: 36px 32px 28px 32px;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.13);
  position: relative;
  backdrop-filter: blur(6px);
}

.page-transition {
  animation: page-fade-in 0.7s cubic-bezier(.4,0,.2,1);
}

@keyframes page-fade-in {
  0% { opacity: 0; transform: translateY(40px) scale(0.98); }
  100% { opacity: 1; transform: none; }
}

.title {
  font-size: 26px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 24px;
  color: var(--student-color);
  letter-spacing: 0.04em;
}

.el-form-item {
  margin-bottom: 18px;
}

.button-group {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 24px;
}

.register-button {
  flex: 1;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
  padding: 12px 24px;
}

.back-btn {
  background: rgb(254, 254, 254);
  color: #666;
}

.back-btn:hover {
  background: rgb(228, 228, 228);
  transform: translateY(-1px);
}

.submit-btn {
  background: rgb(70, 161, 253);
  color: #fff;
}

.submit-btn:hover {
  background: rgb(9, 227, 71);
  transform: translateY(-1px);
}
</style> 