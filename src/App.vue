<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()

// 计算是否显示页眉 - 只在公共页面显示
const showHeader = computed(() => {
  const publicRoutes = ['/', '/register', '/reset']
  return publicRoutes.includes(route.path) || route.path.startsWith('/reset')
})

// 计算是否禁用滚动 - 在公共页面禁用滚动
const disableScroll = computed(() => {
  const noScrollRoutes = ['/', '/register', '/reset']
  return noScrollRoutes.includes(route.path) || route.path.startsWith('/reset')
})

// 计算当前系统类型，用于设置背景色
const currentSystem = computed(() => {
  if (route.path.startsWith('/student')) {
    return 'student'
  } else if (route.path.startsWith('/teacher')) {
    return 'teacher'
  } else if (route.path.startsWith('/admin')) {
    return 'admin'
  } else {
    return 'public'
  }
})
</script>

<template>
  <div id="app" :class="{ 'no-scroll': disableScroll, [`system-${currentSystem}`]: true }">
    <!-- 全局页眉 - 只在登录相关页面显示 -->
    <!-- <header v-if="showHeader" class="global-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="system-title">
            <div>
              <img src="@/assets/publicIcon.png" alt="示例图片" style="width: 80px;" />
            </div>
            欢迎使用智学苏云
          </h1>
        </div>
        <div class="header-right">
          <div class="startup-text">
            <i class="fas fa-rocket startup-icon"></i>
            启动页
          </div>
        </div>
      </div>
    </header> -->

    <!-- 主要内容区域 -->
    <main class="main-content" :class="{ 'with-header': showHeader, 'no-scroll': disableScroll }">
      <router-view v-slot="{ Component }">
        <transition name="page-transition" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

  </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

#app {
  min-height: 100vh;
  transition: background 0.5s ease;
}

/* 公共页面背景（登录、注册、找回密码） */
#app.system-public {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
}

/* 学生系统背景 */
#app.system-student {
  background: rgb(93, 93, 209)
}

/* 教师系统背景 */
#app.system-teacher {
  /* background: linear-gradient(135deg, #8e64cd 0%, #8953e5 50%, #a855f7 100%); */
  background: rgb(93, 93, 209)
}

/* 管理员系统背景 */
#app.system-admin {
  /* background: linear-gradient(135deg, #df3e66 0%, #df3e61 50%, #ec7085 100%); */
  background: rgb(93, 93, 209)
}

/* 禁用滚动的样式 */
#app.no-scroll {
  height: 100vh;
  overflow: hidden;
}

/* 全局页眉样式 */
.global-header {
  height: 70px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(15px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  animation: header-slide-down 0.8s cubic-bezier(.4, 0, .2, 1);
}

@keyframes header-slide-down {
  0% {
    opacity: 0;
    transform: translateY(-20px);
  }

  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
}

.system-title {
  font-size: 24px;
  font-weight: 700;
  /* color: #fff; */
  margin: 0;
  display: flex;
  align-items: center;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: title-glow 3s ease-in-out infinite alternate;
}

.title-icon {
  margin-right: 12px;
  font-size: 28px;
  animation: icon-rotate 4s ease-in-out infinite;
}

@keyframes title-glow {
  0% {
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1), 0 0 20px rgba(255, 255, 255, 0.3);
  }

  100% {
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1), 0 0 30px rgba(255, 255, 255, 0.5);
  }
}

@keyframes icon-rotate {

  0%,
  100% {
    transform: rotate(0deg);
  }

  25% {
    transform: rotate(-5deg);
  }

  75% {
    transform: rotate(5deg);
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.startup-text {
  font-size: 18px;
  font-weight: 600;
  /* color: rgba(255, 255, 255, 0.9); */
  display: flex;
  align-items: center;
  animation: startup-pulse 2s ease-in-out infinite;
}

.startup-icon {
  margin-right: 8px;
  animation: rocket-fly 3s ease-in-out infinite;
}

@keyframes startup-pulse {

  0%,
  100% {
    opacity: 0.9;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}

@keyframes rocket-fly {

  0%,
  100% {
    transform: translateY(0px) rotate(0deg);
  }

  25% {
    transform: translateY(-3px) rotate(-5deg);
  }

  75% {
    transform: translateY(3px) rotate(5deg);
  }
}

.main-content {
  min-height: 100vh;
  width: 100%;
}

.main-content.with-header {
  padding-top: 70px;
}

.main-content.no-scroll {
  height: 100vh;
  overflow: hidden;
}

/* 针对学生系统等内部页面，不需要居中 */
.main-content>* {
  width: 100%;
}

.page-transition-enter-active,
.page-transition-leave-active {
  transition: all 0.7s cubic-bezier(.4, 0, .2, 1);
}

.page-transition-enter-from {
  opacity: 0;
  transform: translateY(40px) scale(0.98);
}

.page-transition-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(1.02);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 20px;
  }

  .system-title {
    font-size: 20px;
  }

  .title-icon {
    font-size: 24px;
  }

  .startup-text {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: 0 16px;
  }

  .system-title {
    font-size: 18px;
  }

  .startup-text {
    font-size: 14px;
  }
}
</style>
