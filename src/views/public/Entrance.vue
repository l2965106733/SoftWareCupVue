<script setup>
import AppHeader from '@/components/AppHeader.vue';
import { useRouter } from 'vue-router'

const router = useRouter()

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
  <AppHeader>
    <template #left>
      <span class="logo-text">教学实训智能体软件</span>
    </template>
  </AppHeader>
  <div class="entrance-page">
    <div class="welcome-title">请选择您的身份</div>
    <div class="entrance-cards">
      <div class="entrance-card personal" @click="enter(1)">
        <div class="icon">👤</div>
        <div class="title">学生入口</div>
        <button class="signup-btn">进入</button>
      </div>
      <div class="entrance-card art" @click="enter(2)">
        <div class="icon">✏️</div>
        <div class="title">教师入口</div>
        <button class="signup-btn">进入</button>
      </div>
      <div class="entrance-card project" @click="enter(3)">
        <div class="icon">💻</div>
        <div class="title">管理员入口</div>
        <button class="signup-btn">进入</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.entrance-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 60px;
}
.welcome-title {
  font-size: 2rem;
  font-weight: 600;
  color: #222;
  margin-bottom: 40px;
  letter-spacing: 2px;
}
.entrance-cards {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 40px;
  flex-wrap: wrap;
}
.entrance-card {
  width: 260px;
  background: #fff;
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px 24px 16px;
  transition: transform var(--transition), box-shadow var(--transition);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.entrance-card::before {
  content: '';
  position: absolute;
  left: 0; top: 0; right: 0; height: 8px;
  border-radius: var(--card-radius) var(--card-radius) 0 0;
  background: currentColor;
  opacity: 0.18;
}
.entrance-card .icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  font-size: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin-top: 8px;
}
.entrance-card .title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 8px;
  letter-spacing: 1px;
}
.entrance-card .desc {
  color: #fff;
  font-size: 15px;
  margin-bottom: 24px;
}
.entrance-card .signup-btn {
  width: 80%;
  padding: 10px 0;
  border: none;
  border-radius: 24px;
  font-size: 18px;
  background: #fff;
  font-weight: bold;
  cursor: pointer;
  transition: background var(--transition), color var(--transition), box-shadow var(--transition), transform var(--transition);
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.entrance-card.personal { background: var(--student-color); color: #fff; }
.entrance-card.art { background: var(--teacher-color); color: #fff; }
.entrance-card.project { background: var(--admin-color); color: #fff; }
.entrance-card.personal .signup-btn { color: var(--student-color) !important; }
.entrance-card.art .signup-btn { color: var(--teacher-color) !important; }
.entrance-card.project .signup-btn { color: var(--admin-color) !important; }
.entrance-card:hover {
  transform: translateY(-12px) scale(1.04);
  box-shadow: 0 12px 36px rgba(0,0,0,0.20);
  z-index: 2;
}
.entrance-card .signup-btn:hover {
  background: #f5f7fa;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
  transform: scale(1.05);
}
</style>
