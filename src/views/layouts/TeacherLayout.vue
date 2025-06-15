<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const loginName = ref('');

onMounted(() => {
    // 从localStorage中获取登录用户信息
    const loginUser = JSON.parse(localStorage.getItem('loginUser'));
    if (loginUser && loginUser.name) {
        loginName.value = loginUser.name;
    }
});

const reset = () => {
    // 跳转到修改密码页面
    router.push('/reset?mode=modify&role=2');
}

const logOut = () => {
    ElMessageBox.confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        ElMessage.success("退出成功")
        // 清除登录用户信息
        localStorage.removeItem('loginUser');
        // 跳转到登录页面
        router.push('/')
    }).catch(() => {
        ElMessage.info('已取消退出登录');
    });
}

</script>

<template>
    <div class="teacher-layout">
        <!-- 现代化页眉 -->
        <header class="modern-header">
            <div class="header-content">
                <div class="header-left">
                    <h1 class="system-title">
                        <i class="fas fa-chalkboard-teacher title-icon"></i>
                        教师教学系统
                    </h1>
                </div>
                <div class="header-right">
                    <div class="user-info">
                        <span class="welcome-text">欢迎，{{ loginName }}</span>
                        <div class="user-actions">
                            <button class="action-btn" @click="reset">
                                <i class="fas fa-key"></i>
                                修改密码
                            </button>
                            <button class="action-btn logout-btn" @click="logOut">
                                <i class="fas fa-sign-out-alt"></i>
                                退出登录
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </header>

        <div class="main-container">
            <!-- 现代化侧边栏 -->
            <aside class="modern-sidebar">
                <nav class="nav-menu">
                    <router-link to="/teacher/home" class="nav-item" active-class="active">
                        <i class="fas fa-home nav-icon"></i>
                        <span class="nav-text">首页</span>
                    </router-link>
                    <router-link to="/teacher/resource" class="nav-item" active-class="active">
                        <i class="fas fa-book-open nav-icon"></i>
                        <span class="nav-text">资源管理</span>
                    </router-link>
                    <router-link to="/teacher/practise" class="nav-item" active-class="active">
                        <i class="fas fa-tasks nav-icon"></i>
                        <span class="nav-text">作业管理</span>
                    </router-link>
                    <router-link to="/teacher/interact" class="nav-item" active-class="active">
                        <i class="fas fa-comments nav-icon"></i>
                        <span class="nav-text">互动管理</span>
                    </router-link>
                    <router-link to="/teacher/analysis" class="nav-item" active-class="active">
                        <i class="fas fa-chart-bar nav-icon"></i>
                        <span class="nav-text">数据分析</span>
                    </router-link>
                </nav>
            </aside>

            <!-- 主内容区域 -->
            <main class="main-content">
                <div class="content-wrapper">
                    <RouterView />
                </div>
            </main>
        </div>
    </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.teacher-layout {
    height: 100vh;
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    display: flex;
    flex-direction: column;
    max-width: 75vw;
    margin: 0 auto;
    border-radius: 0 0 24px 24px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}

/* 现代化页眉 */
.modern-header {
    height: 80px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    animation: header-slide-down 0.8s cubic-bezier(.4,0,.2,1);
    flex-shrink: 0;
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
}

.header-left {
    display: flex;
    align-items: center;
}

.system-title {
    font-size: 28px;
    font-weight: 700;
    color: #fff;
    margin: 0;
    display: flex;
    align-items: center;
    text-shadow: 0 2px 8px rgba(0,0,0,0.1);
    animation: title-glow 3s ease-in-out infinite alternate;
}

.title-icon {
    margin-right: 12px;
    font-size: 32px;
    animation: icon-rotate 4s ease-in-out infinite;
}

@keyframes title-glow {
    0% { 
        text-shadow: 0 2px 8px rgba(0,0,0,0.1), 0 0 20px rgba(255,255,255,0.3);
    }
    100% { 
        text-shadow: 0 2px 8px rgba(0,0,0,0.1), 0 0 30px rgba(255,255,255,0.5);
    }
}

@keyframes icon-rotate {
    0%, 100% { transform: rotate(0deg); }
    25% { transform: rotate(-5deg); }
    75% { transform: rotate(5deg); }
}

.header-right {
    display: flex;
    align-items: center;
}

.user-info {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.welcome-text {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 8px;
    animation: text-fade-in 1s ease-out;
}

@keyframes text-fade-in {
    0% { opacity: 0; transform: translateX(20px); }
    100% { opacity: 1; transform: translateX(0); }
}

.user-actions {
    display: flex;
    gap: 12px;
}

.action-btn {
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    color: #fff;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;
    backdrop-filter: blur(10px);
}

.action-btn:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.logout-btn:hover {
    background: rgba(255, 108, 108, 0.8);
    border-color: rgba(255, 108, 108, 0.9);
}

/* 主容器 */
.main-container {
    display: flex;
    flex: 1;
    overflow: hidden;
}

/* 现代化侧边栏 */
.modern-sidebar {
    width: 260px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border-right: 1px solid rgba(255, 255, 255, 0.2);
    animation: sidebar-slide-in 0.8s cubic-bezier(.4,0,.2,1);
    flex-shrink: 0;
    overflow-y: auto;
}

/* 隐藏侧边栏滚动条 */
.modern-sidebar::-webkit-scrollbar {
    display: none;
}

.modern-sidebar {
    -ms-overflow-style: none;
    scrollbar-width: none;
}

@keyframes sidebar-slide-in {
    0% { 
        opacity: 0; 
        transform: translateX(-20px);
    }
    100% { 
        opacity: 1; 
        transform: translateX(0);
    }
}

.nav-menu {
    padding: 24px 0;
}

.nav-item {
    display: flex;
    align-items: center;
    padding: 16px 24px;
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    transition: all 0.3s ease;
    border-left: 3px solid transparent;
    margin: 4px 0;
}

.nav-item:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
    border-left-color: rgba(255, 255, 255, 0.5);
    transform: translateX(8px);
}

.nav-item.active {
    background: rgba(255, 255, 255, 0.2);
    color: #fff;
    border-left-color: #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.nav-icon {
    font-size: 18px;
    margin-right: 12px;
    width: 20px;
    text-align: center;
}

.nav-text {
    font-size: 16px;
    font-weight: 500;
}

/* 主内容区域 */
.main-content {
    flex: 1;
    overflow-y: auto;
    animation: content-fade-in 1s cubic-bezier(.4,0,.2,1);
}

/* 隐藏主内容区域滚动条 */
.main-content::-webkit-scrollbar {
    display: none;
}

.main-content {
    -ms-overflow-style: none;
    scrollbar-width: none;
}

@keyframes content-fade-in {
    0% { 
        opacity: 0; 
        transform: translateY(20px);
    }
    100% { 
        opacity: 1; 
        transform: translateY(0);
    }
}

.content-wrapper {
    padding: 32px;
    min-height: 100%;
}
</style>
