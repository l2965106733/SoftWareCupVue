<script setup>
import { useRouter, useRoute } from 'vue-router'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { resetApi } from '@/api/public'

const router = useRouter()
const route = useRoute()

const mode = ref(route.query.mode || 'modify')  // 默认修改密码
const role = ref(Number(route.query.role || ''))
const titleText = computed(() => {
  return mode.value === 'forget' ? '找回密码' : '修改密码'
})
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
      return '学'
    case 2:
      return '教工'
    case 3:
      return '管理员编'
    default:
      return '用户'
  }
})
const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')

// 表单数据，根据模式使用不同字段
const form = ref({
  id: loginUser.id,
  role: role.value,
  identifier: '',      // 忘记密码模式填写
  oldPassword: '',     // 修改密码模式填写
  newPassword: '',
  repeatPassword: ''
})

// 确认按钮逻辑
const sure = async () => {
  if (!form.value.newPassword || !form.value.repeatPassword) {
    ElMessage.error('新密码不能为空')
    return
  }

  if (form.value.newPassword !== form.value.repeatPassword) {
    ElMessage.error('两次密码不一致')
    return
  }

  if (mode.value === 'forget' && !form.value.identifier) {
    ElMessage.error(`请填写${roleLabel.value}号`)
    return
  }
  else if (mode.value === 'modify' && !form.value.oldPassword) {
    ElMessage.error('请输入原密码')
    return
  }
  const result = await resetApi(form.value);
  if (result.code) {
    ElMessage.success('密码重置成功')
  }
  else {
    ElMessage.error(`无法更新，请检查后重新操作`)
    if (mode.value === 'forget') {
      router.push('/')
    }
    else {
      router.push(`/${selectedRole.value}`)
    }
  }
}

// 返回按钮
const back = () => {
  router.back()
}
</script>

<template>
  <div id="container">
    <div class="login-form">
      <el-form label-width="80px">
        <p class="title">{{ titleText }}</p>

        <el-form-item v-if="mode === 'forget'" :label="`${roleLabel}号`">
          <el-input v-model="form.identifier" :placeholder="`请输入${roleLabel}号`" />
        </el-form-item>

        <el-form-item v-if="mode === 'modify'" label="原密码">
          <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>

        <el-form-item label="确认密码">
          <el-input v-model="form.repeatPassword" type="password" placeholder="请再次输入新密码" />
        </el-form-item>

        <el-form-item>
          <el-button class="button" type="info" @click="back">返回</el-button>
          <el-button class="button" type="primary" @click="sure">确认</el-button>
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
  min-height: 100vh;
}

.login-form {
  width: 400px;
  padding: 40px;
  border-radius: 8px;
  background-color: #f8f8f8;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 20px;
  font-weight: bold;
}
</style>
