<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '@/api/public'

const router = useRouter()

// 角色选择状态
const role = ref(null)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 角色映射（用于页面跳转）
const roleMap = {
  1: 'student',
  2: 'teacher', 
  3: 'admin'
}

// 登录处理
const login = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  const selectedRole = roleMap[role.value]
  
  try {
    const result = await loginApi({
      username: loginForm.username,
      password: loginForm.password,
      role: role.value  // 直接传数字类型的role
    })
    
    if (result.code === 1) {
      // 保存用户信息到本地存储
      localStorage.setItem('loginUser', JSON.stringify(result.data))
      
      // 记录用户登录日志（直接调用后端接口）
      try {
        // 这里可以调用后端接口记录登录日志
        // await recordUserLoginApi(result.data.id)
        console.log('用户登录成功')
      } catch (error) {
        console.error('记录用户登录日志失败:', error)
      }
      
      // 跳转至对应角色页面
      router.push(`/${selectedRole}/home`)
    }
    else {
      ElMessage.error('用户或密码不正确')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败，请稍后重试')
  }
}

// 返回入口界面
const back = () => {
  role.value = null
  loginForm.username = ''
  loginForm.password = ''
}

// 忘记密码
const reset = () => {
  router.push(`/reset?mode=forget&role=${role.value}`)
}

// 注册
const goRegister = () => {
  if (role.value === 3) {
    // 只有管理员可以注册
    router.push(`/register?role=${role.value}`)
  } else {
    // 学生和教师显示提示
    ElMessage.info('学生和教师账号请联系学校管理员开通')
  }
}
</script>

<template>
  <div id="container">
    <!-- 初始入口选择：卡片风格 -->
    <div v-if="!role" class="enter_select_card">
      <!-- 引导文字 -->
      <div class="welcome-text">
        <h2 class="welcome-title">请选择您的身份</h2>
        <p class="welcome-subtitle">开启您的智能教学之旅</p>
      </div>
      
      <!-- 卡片选择区域 -->
      <div class="cards-container">
        <div class="card card-student" @click="role = 1">
          <div class="icon student">
            <i class="fas fa-user-graduate"></i>
          </div>
          <div class="card-title">我是学生</div>
          <div class="card-subtitle"></div>
          <el-button class="card-btn card-btn-student">继续</el-button>
        </div>
        <div class="card card-design" @click="role = 2">
          <div class="icon design">
            <i class="fas fa-chalkboard-teacher"></i>
          </div>
          <div class="card-title">我是教师</div>
          <div class="card-subtitle"></div>
          <el-button class="card-btn card-btn-teacher">继续</el-button>
        </div>
        <div class="card card-admin" @click="role = 3">
          <div class="icon admin">
            <i class="fas fa-user-shield"></i>
          </div>
          <div class="card-title">我是管理员</div>
          <div class="card-subtitle"></div>
          <el-button class="card-btn card-btn-admin">继续</el-button>
        </div>
      </div>
    </div>
    <!-- 登录表单界面 -->
    <div v-else class="login-form page-transition">
      <el-form label-width="80px">
        <p class="title">{{ role === 1 ? '学生' : role === 2 ? '教师' : '管理员' }}登录</p>
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="loginForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <div class="button-group">
          <el-button class="login-button back-btn" type="primary" @click="back">返回</el-button>
          <el-button class="login-button forgot-btn" type="primary" @click="reset">忘记密码</el-button>
          <el-button class="login-button login-btn" type="primary" @click="login">登录</el-button>
        </div>
        <!-- 注册链接 -->
        <div class="register-link">
          <span v-if="role === 3">还没有账号？</span>
          <span v-else>没有账号？</span>
          <a href="javascript:;" @click="goRegister" class="register-btn">
            {{ role === 3 ? '立即注册' : '请询问学校管理员' }}
          </a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

#container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 90px 20px 20px 20px;
  width: 100%;
}
.enter_select_card {
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: fade-in 1s;
  margin-top: 60px;
}
.welcome-text {
  text-align: center;
  margin-bottom: 40px;
  animation: welcome-fade-in 1.2s cubic-bezier(.4,0,.2,1);
}
.welcome-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 12px rgba(0,0,0,0.15);
  letter-spacing: 0.05em;
  animation: welcome-glow 3s ease-in-out infinite alternate;
}
.welcome-subtitle {
  font-size: 18px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
  letter-spacing: 0.02em;
  animation: subtitle-float 4s ease-in-out infinite;
}
@keyframes welcome-fade-in {
  0% { 
    opacity: 0; 
    transform: translateY(-30px);
  }
  100% { 
    opacity: 1; 
    transform: translateY(0);
  }
}
@keyframes welcome-glow {
  0% { 
    text-shadow: 0 2px 12px rgba(0,0,0,0.15), 0 0 20px rgba(255,255,255,0.3);
  }
  100% { 
    text-shadow: 0 2px 12px rgba(0,0,0,0.15), 0 0 30px rgba(255,255,255,0.5), 0 0 40px rgba(255,255,255,0.2);
  }
}
@keyframes subtitle-float {
  0%, 100% { 
    transform: translateY(0);
    opacity: 0.85;
  }
  50% { 
    transform: translateY(-2px);
    opacity: 1;
  }
}
.cards-container {
  display: flex;
  gap: 32px;
  justify-content: center;
  align-items: center;
}
@keyframes fade-in {
  0% { opacity: 0; transform: translateY(40px);}
  100% { opacity: 1; transform: none;}
}
.card {
  width: 240px;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
  border-radius: 22px;
  box-shadow: 0 4px 24px rgba(30,64,175,0.15);
  padding: 32px 18px 28px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.22s, box-shadow 0.22s;
  position: relative;
  overflow: hidden;
  animation: card-fade-in 0.7s cubic-bezier(.4,0,.2,1);
}
.card:hover {
  transform: translateY(-8px) scale(1.05);
  box-shadow: 0 12px 36px rgba(30,64,175,0.25);
}
.card-student {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
}
.card-design {
  background: linear-gradient(135deg, #4c1d95 0%, #7c3aed 50%, #a855f7 100%);
}
.card-admin {
  background: linear-gradient(135deg, #be123c 0%, #e11d48 50%, #f43f5e 100%);
}
.icon {
  width: 56px;
  height: 56px;
  margin-bottom: 18px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
}
.icon i {
  font-size: 28px;
  color: #fff;
}
.card-title {
  font-size: 22px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.card-subtitle {
  font-size: 15px;
  color: #e0f0ff;
  margin-bottom: 18px;
}
.card-btn {
  width: 100%;
  border-radius: 16px;
  font-size: 1.1em;
  transition: all 0.18s;
  border: none;
  color: #fff;
  font-weight: 600;
}
.card-btn-student {
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(10px);
}
.card-btn-student:hover {
  background: rgba(255,255,255,0.35);
  transform: translateY(-2px);
}
.card-btn-teacher {
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(10px);
}
.card-btn-teacher:hover {
  background: rgba(255,255,255,0.35);
  transform: translateY(-2px);
}
.card-btn-admin {
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(10px);
}
.card-btn-admin:hover {
  background: rgba(255,255,255,0.35);
  transform: translateY(-2px);
}
.login-form {
  width: 370px;
  background: rgba(255,255,255,0.92);
  padding: 36px 32px 28px 32px;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.13);
  position: relative;
  backdrop-filter: blur(6px);
  margin-top: 90px;
}
.page-transition {
  animation: page-fade-in 0.7s cubic-bezier(.4,0,.2,1);
}
@keyframes page-fade-in {
  0% { opacity: 0; transform: translateY(40px) scale(0.98); }
  100% { opacity: 1; transform: none; }
}
@keyframes card-fade-in {
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
  gap: 12px;
  justify-content: space-between;
  margin-top: 24px;
}
.login-button {
  flex: 1;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
}
.back-btn {
  background:rgb(254, 254, 254);
  color: #666;
}
.back-btn:hover {
  background:rgb(228, 228, 228);
  transform: translateY(-1px);
}
.forgot-btn {
  background: linear-gradient(135deg, #be123c 0%, #e11d48 100%);
  color: #fff;
}
.forgot-btn:hover {
  background: linear-gradient(135deg, #4c1d95 0%, #7c3aed 100%);
  transform: translateY(-1px);
}
.login-btn {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  color: #fff;
}
.login-btn:hover {
  background: linear-gradient(135deg, #4c1d95 0%, #7c3aed 100%);
  transform: translateY(-1px);
}
.register-link {
  text-align: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.register-link span {
  color: #666;
  font-size: 14px;
}
.register-btn {
  color: #74b9ff;
  text-decoration: none;
  font-weight: 500;
  margin-left: 8px;
  transition: all 0.2s;
}
.register-btn:hover {
  color: #0984e3;
  text-decoration: underline;
}
</style>
