<script setup>
import { ref, onMounted } from 'vue'

const enter = (type) => {
    switch (type) {
        case 1:
            router.push('/student/login');
            break
        case 2:
            router.push('/teacher/login');
            break
        case 3:
            router.push('/admin/login');
            break
    }
}
</script>

<template>
    <div class="enter_select">
        <el-button type="primary" @click="enter(1)">学生入口</el-button>
        <el-button type="success" @click="enter(2)">教师入口</el-button>
        <el-button type="info" @click="enter(3)">管理员入口</el-button>
        111111111
    </div>
</template>

<style scoped>
</style>
