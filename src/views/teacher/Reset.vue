<script setup>
import { useRouter } from 'vue-router'
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
const router = useRouter();
let loginForm = ref({t_id:'',new_password: '', repeat_password: '' })

const sure = () => {
  if (loginForm.value.new_password === '' || loginForm.value.repeat_password === '') {
    ElMessage.error('新密码不能为空');
    return;
  }
  ElMessage.success('密码重置成功');
  router.push('/teacher/login');
}

const back = () => {
  router.push('/teacher/login');
}
</script>

<template>
  <div id="container">
    <div class="login-form">
      <el-form label-width="80px">
        <p class="title">修改密码</p>
        <el-form-item label="教工号" prop="t_id">
          <el-input v-model="loginForm.t_id" placeholder="请输入教工号"></el-input>
        </el-form-item>

        <el-form-item label="新密码" prop="new_password">
          <el-input v-model="loginForm.new_passsword" placeholder="请输入新密码"></el-input>
        </el-form-item>

        <el-form-item label="再次输入" prop="repeat_password">
          <el-input type="repeat_password" v-model="loginForm.repeat_password" placeholder="请再次输入新密码"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button class="button" type="info" @click="back">返回上一步</el-button>
          <el-button class="button" type="primary" @click="sure">确认</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped></style>