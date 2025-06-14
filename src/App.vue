<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <!-- 常驻页眉 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="app-title">欢迎使用教学实训智能体软件</h1>
        <div class="header-right">
          <span class="startup-text">启动页</span>
        </div>
      </div>
    </header>
    
    <!-- 主要内容区域 -->
    <main class="main-content">
      <transition name="page-transition" mode="out-in">
        <RouterView />
      </transition>
    </main>
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
  background: var(--gradient-blue);
  display: flex;
  flex-direction: column;
}

.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1000;
  animation: header-fade-in 0.8s cubic-bezier(.4,0,.2,1);
}

@keyframes header-fade-in {
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
}

.app-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
  letter-spacing: 0.05em;
  animation: title-glow 2s ease-in-out infinite alternate;
}

@keyframes title-glow {
  0% { 
    text-shadow: 0 2px 8px rgba(0,0,0,0.1), 0 0 20px rgba(255,255,255,0.3);
  }
  100% { 
    text-shadow: 0 2px 8px rgba(0,0,0,0.1), 0 0 30px rgba(255,255,255,0.5);
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.startup-text {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
  letter-spacing: 0.05em;
  animation: title-glow 2s ease-in-out infinite alternate;
}

.main-content {
  flex: 1;
  padding-top: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-transition-enter-active, .page-transition-leave-active {
  transition: all 0.7s cubic-bezier(.4,0,.2,1);
}
.page-transition-enter-from {
  opacity: 0;
  transform: translateY(40px) scale(0.98);
}
.page-transition-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(1.02);
}
</style>
