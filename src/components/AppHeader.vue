<template>
  <header class="app-header">
    <div class="header-left">
      <slot name="left">
        <span class="logo-text">教学实训智能体软件</span>
      </slot>
    </div>
    <div class="header-right">
      <slot name="right"></slot>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  width: 100%;
  height: 56px;
  background: #2196f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  box-sizing: border-box;
  color: #fff;
  font-size: 1.15rem;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.header-left {
  display: flex;
  align-items: center;
}
.logo-text {
  font-size: 1.25rem;
  font-weight: bold;
  letter-spacing: 2px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}
</style> 