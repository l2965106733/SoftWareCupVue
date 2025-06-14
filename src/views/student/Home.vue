<script setup>
import { ref, onMounted } from 'vue'

const loginUser = ref({})
const currentTime = ref('')

onMounted(() => {
    // 获取登录用户信息
    const user = JSON.parse(localStorage.getItem('loginUser') || '{}')
    loginUser.value = user
    
    // 更新时间
    updateTime()
    setInterval(updateTime, 1000)
})

const updateTime = () => {
    const now = new Date()
    const options = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    }
    currentTime.value = now.toLocaleDateString('zh-CN', options)
}

// 快捷功能数据
const quickActions = ref([
    {
        title: '开始学习',
        subtitle: '进入学习模块',
        icon: 'fas fa-book-open',
        color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        route: '/student/study'
    },
    {
        title: '查看作业',
        subtitle: '完成课程作业',
        icon: 'fas fa-tasks',
        color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        route: '/student/practise'
    },
    {
        title: '互动交流',
        subtitle: '参与课堂讨论',
        icon: 'fas fa-comments',
        color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        route: '/student/interact'
    }
])

// 学习统计数据
const studyStats = ref([
    { label: '今日学习时长', value: '2.5小时', icon: 'fas fa-clock', color: '#667eea' },
    { label: '完成作业数', value: '8个', icon: 'fas fa-check-circle', color: '#f5576c' },
    { label: '参与讨论', value: '12次', icon: 'fas fa-comment-dots', color: '#4facfe' },
    { label: '学习进度', value: '75%', icon: 'fas fa-chart-line', color: '#26d0ce' }
])
</script>

<template>
    <div class="student-home">
        <!-- 欢迎区域 -->
        <section class="welcome-section">
            <div class="welcome-card">
                <div class="welcome-content">
                    <div class="welcome-text">
                        <h1 class="welcome-title">
                            <i class="fas fa-sun welcome-icon"></i>
                            欢迎回来，{{ loginUser.name || '同学' }}！
                        </h1>
                        <p class="welcome-subtitle">开始今天的学习之旅吧</p>
                        <div class="current-time">
                            <i class="fas fa-calendar-alt"></i>
                            <span class="time-text">{{ currentTime }}</span>
                        </div>
                    </div>
                    <div class="welcome-illustration">
                        <i class="fas fa-graduation-cap illustration-icon"></i>
                    </div>
                </div>
            </div>
        </section>

        <!-- 快捷操作区域 -->
        <section class="quick-actions-section">
            <h2 class="section-title">
                <i class="fas fa-rocket"></i>
                快捷操作
            </h2>
            <div class="actions-grid">
                <router-link 
                    v-for="(action, index) in quickActions" 
                    :key="index"
                    :to="action.route"
                    class="action-card"
                    :style="{ '--card-bg': action.color, '--delay': index * 0.1 + 's' }"
                >
                    <div class="action-icon">
                        <i :class="action.icon"></i>
                    </div>
                    <div class="action-content">
                        <h3 class="action-title">{{ action.title }}</h3>
                        <p class="action-subtitle">{{ action.subtitle }}</p>
                    </div>
                    <div class="action-arrow">
                        <i class="fas fa-arrow-right"></i>
                    </div>
                </router-link>
            </div>
        </section>

        <!-- 学习统计区域 -->
        <section class="stats-section">
            <h2 class="section-title">
                <i class="fas fa-chart-bar"></i>
                学习统计
            </h2>
            <div class="stats-grid">
                <div 
                    v-for="(stat, index) in studyStats" 
                    :key="index"
                    class="stat-card"
                    :style="{ '--delay': index * 0.1 + 's' }"
                >
                    <div class="stat-icon" :style="{ color: stat.color }">
                        <i :class="stat.icon"></i>
                    </div>
                    <div class="stat-content">
                        <div class="stat-value">{{ stat.value }}</div>
                        <div class="stat-label">{{ stat.label }}</div>
                    </div>
                </div>
            </div>
        </section>

        <!-- 最近活动区域 -->
        <section class="recent-section">
            <h2 class="section-title">
                <i class="fas fa-history"></i>
                最近活动
            </h2>
            <div class="activity-card">
                <div class="activity-list">
                    <div class="activity-item">
                        <div class="activity-icon">
                            <i class="fas fa-book"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-title">完成了《Vue.js基础》学习</div>
                            <div class="activity-time">2小时前</div>
                        </div>
                    </div>
                    <div class="activity-item">
                        <div class="activity-icon">
                            <i class="fas fa-edit"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-title">提交了作业《组件开发实践》</div>
                            <div class="activity-time">5小时前</div>
                        </div>
                    </div>
                    <div class="activity-item">
                        <div class="activity-icon">
                            <i class="fas fa-comment"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-title">参与了课堂讨论</div>
                            <div class="activity-time">1天前</div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
.student-home {
    max-width: 1200px;
    margin: 0 auto;
    animation: page-fade-in 0.8s cubic-bezier(.4,0,.2,1);
    padding: 0 16px;
}

@keyframes page-fade-in {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

/* 欢迎区域 */
.welcome-section {
    margin-bottom: 32px;
}

.welcome-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border-radius: 24px;
    padding: 40px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    animation: welcome-slide-in 1s cubic-bezier(.4,0,.2,1);
}

@keyframes welcome-slide-in {
    0% { opacity: 0; transform: translateY(-30px) scale(0.95); }
    100% { opacity: 1; transform: translateY(0) scale(1); }
}

.welcome-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 20px;
}

.welcome-text {
    flex: 1;
    min-width: 300px;
}

.welcome-title {
    font-size: clamp(24px, 4vw, 36px);
    font-weight: 700;
    color: #fff;
    margin: 0 0 12px 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    text-shadow: 0 2px 12px rgba(0,0,0,0.15);
    animation: title-glow 3s ease-in-out infinite alternate;
    line-height: 1.2;
}

.welcome-icon {
    margin-right: 16px;
    color: #ffd700;
    animation: sun-rotate 8s linear infinite;
    flex-shrink: 0;
}

@keyframes sun-rotate {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

@keyframes title-glow {
    0% { text-shadow: 0 2px 12px rgba(0,0,0,0.15), 0 0 20px rgba(255,255,255,0.3); }
    100% { text-shadow: 0 2px 12px rgba(0,0,0,0.15), 0 0 30px rgba(255,255,255,0.5); }
}

.welcome-subtitle {
    font-size: clamp(14px, 2.5vw, 18px);
    color: rgba(255, 255, 255, 0.8);
    margin: 0 0 16px 0;
    line-height: 1.4;
}

.current-time {
    font-size: clamp(12px, 2vw, 16px);
    color: rgba(255, 255, 255, 0.7);
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
}

.current-time i {
    flex-shrink: 0;
}

.welcome-illustration {
    font-size: clamp(60px, 8vw, 120px);
    color: rgba(255, 255, 255, 0.1);
    animation: float 6s ease-in-out infinite;
    flex-shrink: 0;
}

@keyframes float {
    0%, 100% { transform: translateY(0px); }
    50% { transform: translateY(-20px); }
}

/* 区域标题 */
.section-title {
    font-size: clamp(18px, 3vw, 24px);
    font-weight: 700;
    color: #fff;
    margin: 0 0 24px 0;
    display: flex;
    align-items: center;
    text-shadow: 0 2px 8px rgba(0,0,0,0.1);
    line-height: 1.3;
}

.section-title i {
    margin-right: 12px;
    color: rgba(255, 255, 255, 0.8);
    flex-shrink: 0;
}

/* 快捷操作区域 */
.quick-actions-section {
    margin-bottom: 32px;
}

.actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
}

.action-card {
    background: var(--card-bg);
    border-radius: 20px;
    padding: 20px;
    text-decoration: none;
    color: #fff;
    display: flex;
    align-items: center;
    transition: all 0.3s ease;
    border: 1px solid rgba(255, 255, 255, 0.2);
    animation: card-slide-up 0.8s cubic-bezier(.4,0,.2,1);
    animation-delay: var(--delay);
    animation-fill-mode: both;
    min-height: 80px;
}

@keyframes card-slide-up {
    0% { opacity: 0; transform: translateY(30px); }
    100% { opacity: 1; transform: translateY(0); }
}

.action-card:hover {
    transform: translateY(-8px) scale(1.02);
    box-shadow: 0 12px 32px rgba(0,0,0,0.2);
}

.action-icon {
    font-size: clamp(24px, 4vw, 32px);
    margin-right: 16px;
    opacity: 0.9;
    flex-shrink: 0;
}

.action-content {
    flex: 1;
    min-width: 0;
}

.action-title {
    font-size: clamp(16px, 2.5vw, 20px);
    font-weight: 600;
    margin: 0 0 4px 0;
    line-height: 1.3;
}

.action-subtitle {
    font-size: clamp(12px, 2vw, 14px);
    opacity: 0.8;
    margin: 0;
    line-height: 1.4;
}

.action-arrow {
    font-size: clamp(14px, 2.5vw, 18px);
    opacity: 0.7;
    transition: transform 0.3s ease;
    flex-shrink: 0;
    margin-left: 8px;
}

.action-card:hover .action-arrow {
    transform: translateX(8px);
}

/* 学习统计区域 */
.stats-section {
    margin-bottom: 32px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
}

.stat-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border-radius: 16px;
    padding: 20px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    animation: stat-fade-in 0.8s cubic-bezier(.4,0,.2,1);
    animation-delay: var(--delay);
    animation-fill-mode: both;
    transition: transform 0.3s ease;
    min-height: 80px;
}

.stat-card:hover {
    transform: translateY(-4px);
}

@keyframes stat-fade-in {
    0% { opacity: 0; transform: scale(0.9); }
    100% { opacity: 1; transform: scale(1); }
}

.stat-icon {
    font-size: clamp(20px, 3vw, 28px);
    margin-right: 12px;
    animation: icon-pulse 2s ease-in-out infinite;
    flex-shrink: 0;
}

@keyframes icon-pulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.1); }
}

.stat-content {
    flex: 1;
    min-width: 0;
}

.stat-value {
    font-size: clamp(18px, 3vw, 24px);
    font-weight: 700;
    color: #fff;
    margin-bottom: 4px;
    line-height: 1.2;
}

.stat-label {
    font-size: clamp(11px, 2vw, 14px);
    color: rgba(255, 255, 255, 0.7);
    line-height: 1.3;
}

/* 最近活动区域 */
.recent-section {
    margin-bottom: 32px;
}

.activity-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(15px);
    border-radius: 20px;
    padding: 20px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    animation: activity-slide-in 1s cubic-bezier(.4,0,.2,1);
}

@keyframes activity-slide-in {
    0% { opacity: 0; transform: translateX(-30px); }
    100% { opacity: 1; transform: translateX(0); }
}

.activity-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.activity-item {
    display: flex;
    align-items: center;
    padding: 16px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    transition: all 0.3s ease;
    gap: 12px;
}

.activity-item:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateX(8px);
}

.activity-item .activity-icon {
    font-size: clamp(16px, 2.5vw, 20px);
    color: rgba(255, 255, 255, 0.8);
    width: 24px;
    text-align: center;
    flex-shrink: 0;
}

.activity-content {
    flex: 1;
    min-width: 0;
}

.activity-title {
    font-size: clamp(14px, 2.5vw, 16px);
    color: #fff;
    margin-bottom: 4px;
    line-height: 1.4;
    word-wrap: break-word;
}

.activity-time {
    font-size: clamp(11px, 2vw, 14px);
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.3;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .student-home {
        padding: 0 12px;
    }
    
    .welcome-card {
        padding: 24px 20px;
    }
    
    .welcome-content {
        flex-direction: column;
        text-align: center;
    }
    
    .welcome-text {
        min-width: auto;
    }
    
    .welcome-title {
        justify-content: center;
    }
    
    .actions-grid {
        grid-template-columns: 1fr;
        gap: 16px;
    }
    
    .stats-grid {
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 12px;
    }
    
    .action-card, .stat-card {
        padding: 16px;
    }
    
    .activity-card {
        padding: 16px;
    }
    
    .activity-item {
        padding: 12px;
    }
}

@media (max-width: 480px) {
    .welcome-card {
        padding: 20px 16px;
    }
    
    .stats-grid {
        grid-template-columns: 1fr;
    }
    
    .current-time {
        justify-content: center;
        text-align: center;
    }
    
    .section-title {
        justify-content: center;
        text-align: center;
    }
}
</style>