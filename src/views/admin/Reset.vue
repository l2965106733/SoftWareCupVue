<script setup>
import AppHeader from '@/components/AppHeader.vue';
import { useRouter } from 'vue-router'
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
const router = useRouter();
let loginForm = ref({a_id:'',new_password: '', repeat_password: '' })

const sure = () => {
  if (loginForm.value.new_password === '' || loginForm.value.repeat_password === '') {
    ElMessage.error('新密码不能为空');
    return;
  }
  ElMessage.success('密码重置成功');
  router.push('/admin/login');
}

const back = () => {
  router.push('/admin/login');
}
</script>

<template>
  <AppHeader>
    <template #left>
      <span class="logo-text">欢迎使用教学实训智能体软件</span>
    </template>
  </AppHeader>
  <div class="reset-page admin-theme">
    <div class="reset-card">
      <div class="reset-title">重置密码</div>
      <el-form label-width="80px">
        <el-form-item label="管理员号" prop="a_id">
          <el-input v-model="loginForm.a_id" placeholder="请输入管理员号"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="new_password">
          <el-input type="password" v-model="loginForm.new_password" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="再次输入" prop="repeat_password">
          <el-input type="password" v-model="loginForm.repeat_password" placeholder="请再次输入新密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button class="reset-btn" type="primary" @click="sure">确认</el-button>
          <el-button class="link-btn" type="info" @click="back">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.reset-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.reset-card {
  width: 360px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 32px rgba(0,0,0,0.10);
  padding: 40px 32px 32px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.reset-title {
  font-size: 1.6rem;
  font-weight: 600;
  margin-bottom: 32px;
  color: #00bcd4;
  letter-spacing: 1px;
}
.reset-btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  background: #00bcd4;
  color: #fff !important;
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.reset-btn:hover {
  background: #0097a7;
}
.link-btn {
  background: none;
  color: #00bcd4 !important;
  border: none;
  margin-left: 8px;
  font-size: 0.98rem;
  cursor: pointer;
  padding: 0 8px;
}
.link-btn:hover {
  text-decoration: underline;
}
.admin-theme .reset-title, .admin-theme .reset-btn, .admin-theme .link-btn { color: #00bcd4; }
</style>