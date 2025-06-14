<script setup>
import AppHeader from '@/components/AppHeader.vue';
import { useRouter } from 'vue-router'
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
const router = useRouter();
let loginForm = ref({username: '', password: '' })

const login = () => {
    if (loginForm.value.username === '' || loginForm.value.password === '') {
        ElMessage.error('用户名或密码不能为空');
        return;
    } 
    // 这里可以添加实际的登录逻辑，比如调用API验证用户名和密码

    // 模拟登录成功
    ElMessage.success('登录成功');
    router.push('/admin/home');
}
const reset = () => {
    router.push('/admin/reset');
}
const back = () => {
    router.push('/');
}
const register = () => {
    router.push('/admin/register');
}
</script>

<template>
  <AppHeader>
    <template #left>
      <span class="logo-text">管理员登录</span>
    </template>
  </AppHeader>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">管理员登录</div>
      <div class="login-subtitle">欢迎回来，请登录您的账号</div>
      <el-form label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="loginForm.password" placeholder="密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="login-btn" type="primary" @click="login">登录</el-button>
          <el-button class="forgot-link" type="text" @click="reset">忘记密码？</el-button>
          <el-button class="register-link" type="text" @click="register">注册</el-button>
          <el-button class="link-btn" type="info" @click="back">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.login-card {
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  background: #fff;
  padding: 40px 32px 32px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: box-shadow var(--transition), transform var(--transition);
}
.login-card:hover {
  box-shadow: 0 8px 32px rgba(0,188,212,0.18);
  transform: translateY(-6px) scale(1.02);
}
.login-title {
  font-size: 1.6rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--admin-color);
  letter-spacing: 1px;
}
.login-subtitle {
  font-size: 1rem;
  color: var(--text-sub);
  margin-bottom: 28px;
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px var(--admin-color);
  border-color: var(--admin-color);
}
.login-btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  background: var(--admin-color);
  color: #fff !important;
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background var(--transition), box-shadow var(--transition), transform var(--transition);
}
.login-btn:hover {
  background: #0097a7;
  box-shadow: 0 4px 16px rgba(0,188,212,0.12);
  transform: scale(1.04);
}
.forgot-link, .register-link, .link-btn {
  background: none;
  color: var(--admin-color) !important;
  border: none;
  font-size: 0.98rem;
  cursor: pointer;
  padding: 0 8px;
  margin-left: 8px;
  transition: color var(--transition);
}
.forgot-link:hover, .register-link:hover, .link-btn:hover {
  text-decoration: underline;
  color: #0097a7 !important;
}
</style>