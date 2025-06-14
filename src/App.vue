<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <transition name="fade-slide" mode="out-in">
    <RouterView />
  </transition>
</template>

<style scoped>
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(.4,0,.2,1);
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
