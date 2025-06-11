<script setup>
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
  // 跳转到学生主页
  router.push('/student/home');
}
const reset = () => {
  router.push('/student/reset');
}
const back = () => {
  router.push('/');
}
</script>

<template>
  <div id="container">
    <div class="login-form">
      <el-form label-width="80px">
        <p class="title">登录</p>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="loginForm.password" placeholder="请输入密码"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button class="button" type="info" @click="back">返回上一步</el-button>
          <el-button class="button" type="info" @click="reset">忘记密码</el-button>
          <el-button class="button" type="primary" @click="login">登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped></style>