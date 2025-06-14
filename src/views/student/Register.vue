<template>
  <AppHeader>
    <template #left>
      <span class="logo-text">注册</span>
    </template>
  </AppHeader>
  <div class="register-page">
    <div class="register-card">
      <div class="register-title">学生注册</div>
      <el-form label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="registerForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="register-btn" type="primary" @click="register">注册</el-button>
          <el-button class="link-btn" type="info" @click="back">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import { useRouter } from 'vue-router';
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
const router = useRouter();
let registerForm = ref({ username: '', password: '', confirmPassword: '' });

const register = () => {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.confirmPassword) {
    ElMessage.error('请填写所有字段');
    return;
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致');
    return;
  }
  // 这里可以添加实际的注册逻辑
  ElMessage.success('注册成功');
  router.push('/student/login');
}
const back = () => {
  router.push('/student/login');
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.register-card {
  width: 360px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 32px rgba(0,0,0,0.10);
  padding: 40px 32px 32px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.register-title {
  font-size: 1.6rem;
  font-weight: 600;
  margin-bottom: 32px;
  color: #2979ff;
  letter-spacing: 1px;
}
.register-btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  background: #2979ff;
  color: #fff;
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.register-btn:hover {
  background: #1565c0;
}
.link-btn {
  background: none;
  color: #2979ff;
  border: none;
  margin-left: 8px;
  font-size: 0.98rem;
  cursor: pointer;
  padding: 0 8px;
}
.link-btn:hover {
  text-decoration: underline;
}
</style> 