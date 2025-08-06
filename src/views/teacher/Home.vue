<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherHomeOverviewApi, getTeacherActivitiesApi } from '@/api/teacher'

const router = useRouter()
const teacherName = ref('')
const loading = ref(true)
const error = ref('')

// 教学统计数据
const stats = ref({
    studentCount: 0,
    assignmentCount: 0,
    completionRate: 0,
    averageScore: 0,
    resourceCount: 0,
    interactionCount: 0
})

// 最近活动数据
const recentActivities = ref([])

// 获取教师首页数据
const fetchHomeData = async () => {
    try {
        loading.value = true
        error.value = ''
        const loginUser = JSON.parse(localStorage.getItem('loginUser'))
        if (!loginUser || !loginUser.id) {
            throw new Error('未找到教师信息，请重新登录')
        }
        teacherName.value = loginUser.name || '老师'
        const [overviewRes, activitiesRes] = await Promise.all([
            getTeacherHomeOverviewApi(loginUser.id),
            getTeacherActivitiesApi(loginUser.id, 10)
        ])
        // 调试输出
        console.log('overviewRes', overviewRes)
        console.log('activitiesRes', activitiesRes)
        // 适配 stats 字段名
        if (overviewRes.code === 1 && overviewRes.data) {
            stats.value = overviewRes.data || stats.value
        }
        if (activitiesRes.code === 1 && activitiesRes.data) {
            recentActivities.value = activitiesRes.data || []
        }
    } catch (err) {
        console.error('获取教师首页数据失败:', err)
        error.value = err.message || '获取数据失败，请稍后重试'
        stats.value = {
            studentCount: 0,
            assignmentCount: 0,
            completionRate: 0,
            averageScore: 0,
            resourceCount: 0,
            interactionCount: 0
        }
        recentActivities.value = []
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchHomeData()
})

// 导航方法
const goToResource = () => {
    router.push('/teacher/resource')
}

const goToPractise = () => {
    router.push('/teacher/practise')
}

const goToInteract = () => {
    router.push('/teacher/interact')
}

const goToAnalysis = () => {
    router.push('/teacher/analysis')
}

// 刷新数据
const refreshData = () => {
    fetchHomeData()
}
</script>

<template>
    <div class="teacher-home">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <div class="loading-spinner">
                <i class="fas fa-spinner fa-spin"></i>
                <p>正在加载数据...</p>
            </div>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="error-container">
            <div class="error-content">
                <i class="fas fa-exclamation-triangle"></i>
                <h3>加载失败</h3>
                <p>{{ error }}</p>
                <button @click="refreshData" class="retry-btn">
                    <i class="fas fa-redo"></i>
                    重新加载
                </button>
            </div>
        </div>

        <!-- 主要内容 -->
        <div v-else>
            <!-- 欢迎区域 -->
            <div class="welcome-section">
                <div class="welcome-content">
                    <h1 class="welcome-title">
                        <i class="fas fa-chalkboard-teacher welcome-icon"></i>
                        欢迎回来，{{ teacherName }}老师！
                    </h1>
                    <p class="welcome-subtitle">开始您今天的教学工作，让每一堂课都精彩纷呈</p>
                </div>
                <div class="welcome-decoration">
                    <div class="floating-element element-1"></div>
                    <div class="floating-element element-2"></div>
                    <div class="floating-element element-3"></div>
                </div>
            </div>

            <!-- 快捷操作区域 -->
            <div class="quick-actions">
                <h2 class="section-title">
                    <i class="fas fa-bolt"></i>
                    快捷操作
                </h2>
                <div class="actions-grid">
                    <div class="action-card" @click="goToResource">
                        <div class="card-icon">
                            <i class="fas fa-book-open"></i>
                        </div>
                        <h3 class="card-title">备课资源</h3>
                        <p class="card-desc">管理教学资源，准备精彩课程</p>
                        <div class="card-arrow">
                            <i class="fas fa-arrow-right"></i>
                        </div>
                    </div>

                    <div class="action-card" @click="goToPractise">
                        <div class="card-icon">
                            <i class="fas fa-tasks"></i>
                        </div>
                        <h3 class="card-title">作业管理</h3>
                        <p class="card-desc">布置和批改学生作业</p>
                        <div class="card-arrow">
                            <i class="fas fa-arrow-right"></i>
                        </div>
                    </div>

                    <div class="action-card" @click="goToInteract">
                        <div class="card-icon">
                            <i class="fas fa-comments"></i>
                        </div>
                        <h3 class="card-title">师生互动</h3>
                        <p class="card-desc">与学生交流，答疑解惑</p>
                        <div class="card-arrow">
                            <i class="fas fa-arrow-right"></i>
                        </div>
                    </div>

                    <div class="action-card" @click="goToAnalysis">
                        <div class="card-icon">
                            <i class="fas fa-chart-bar"></i>
                        </div>
                        <h3 class="card-title">数据分析</h3>
                        <p class="card-desc">查看教学效果和学生表现</p>
                        <div class="card-arrow">
                            <i class="fas fa-arrow-right"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 教学统计 -->
            <div class="teaching-stats">
                <h2 class="section-title">
                    <i class="fas fa-chart-line"></i>
                    教学概览
                </h2>
                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-users"></i>
                        </div>
                        <div class="stat-content">
                            <h3 class="stat-number">{{ stats.studentCount }}</h3>
                            <p class="stat-label">学生总数</p>
                        </div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-clipboard-list"></i>
                        </div>
                        <div class="stat-content">
                            <h3 class="stat-number">{{ stats.assignmentCount }}</h3>
                            <p class="stat-label">作业数量</p>
                        </div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-check-circle"></i>
                        </div>
                        <div class="stat-content">
                            <h3 class="stat-number">{{ stats.completionRate }}%</h3>
                            <p class="stat-label">完成率</p>
                        </div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-star"></i>
                        </div>
                        <div class="stat-content">
                            <h3 class="stat-number">{{ stats.averageScore * 100 }} %</h3>
                            
                            <p class="stat-label">学生平均得分率</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 最近活动 -->
            <div class="recent-activities">
                <h2 class="section-title">
                    <i class="fas fa-clock"></i>
                    最近活动
                </h2>
                <div class="activities-list">
                    <div v-if="recentActivities.length === 0" class="empty-activities">
                        <i class="fas fa-inbox"></i>
                        <p>暂无最近活动</p>
                    </div>
                    <div v-else class="activity-item" v-for="activity in recentActivities" :key="activity.id">
                        <div class="activity-content">
                            <h4 class="activity-title">{{ activity.title }}</h4>
                            <p class="activity-desc">{{ activity.description }}</p>
                            <span class="activity-time">{{ activity.relativeTime || activity.time }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.teacher-home {
    min-height: 100%;
    animation: page-fade-in 0.8s ease-out;
}

@keyframes page-fade-in {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

/* 欢迎区域 */
.welcome-section {
    position: relative;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    padding: clamp(24px, 4vw, 48px);
    margin-bottom: clamp(24px, 4vw, 32px);
    overflow: hidden;
    animation: welcome-slide-up 1s cubic-bezier(.4,0,.2,1);
}

@keyframes welcome-slide-up {
    0% { opacity: 0; transform: translateY(30px); }
    100% { opacity: 1; transform: translateY(0); }
}

.welcome-content {
    position: relative;
    z-index: 2;
}

.welcome-title {
    font-size: clamp(24px, 4vw, 36px);
    font-weight: 700;
    color: #fff;
    margin: 0 0 clamp(12px, 2vw, 16px) 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: clamp(8px, 2vw, 16px);
    animation: title-glow 3s ease-in-out infinite alternate;
}

.welcome-icon {
    font-size: clamp(28px, 5vw, 40px);
    animation: icon-bounce 2s ease-in-out infinite;
}

@keyframes icon-bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-8px); }
}

.welcome-subtitle {
    font-size: clamp(14px, 2.5vw, 18px);
    color: rgba(255, 255, 255, 0.8);
    margin: 0;
    line-height: 1.6;
    word-break: break-word;
    overflow-wrap: break-word;
}

.welcome-decoration {
    position: absolute;
    top: 0;
    right: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
}

.floating-element {
    position: absolute;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    animation: float 6s ease-in-out infinite;
}

.element-1 {
    width: 80px;
    height: 80px;
    top: 20%;
    right: 10%;
    animation-delay: 0s;
}

.element-2 {
    width: 60px;
    height: 60px;
    top: 60%;
    right: 20%;
    animation-delay: 2s;
}

.element-3 {
    width: 40px;
    height: 40px;
    top: 40%;
    right: 5%;
    animation-delay: 4s;
}

@keyframes float {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-20px) rotate(180deg); }
}

/* 通用区域标题 */
.section-title {
    font-size: clamp(20px, 3vw, 24px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 clamp(16px, 3vw, 24px) 0;
    display: flex;
    align-items: center;
    gap: clamp(8px, 2vw, 12px);
    animation: section-fade-in 0.8s ease-out;
}

@keyframes section-fade-in {
    0% { opacity: 0; transform: translateX(-20px); }
    100% { opacity: 1; transform: translateX(0); }
}

/* 快捷操作区域 */
.quick-actions {
    margin-bottom: clamp(24px, 4vw, 32px);
}

.actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: clamp(16px, 3vw, 24px);
}

.action-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    padding: clamp(20px, 4vw, 32px);
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    animation: card-slide-up 0.8s ease-out;
}

@keyframes card-slide-up {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

.action-card:hover {
    /* transform: translateY(-8px) scale(1.1); */
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);
}

.card-icon {
    width: 60px;
    height: 60px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    transition: all 0.3s ease;
}

.card-icon i {
    font-size: 24px;
    color: #fff;
}

.action-card:hover .card-icon {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(1.1);
}

.card-title {
    font-size: clamp(16px, 2.5vw, 20px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 clamp(8px, 1.5vw, 12px) 0;
    word-break: break-word;
}

.card-desc {
    font-size: clamp(13px, 2vw, 14px);
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
    line-height: 1.5;
    word-break: break-word;
    overflow-wrap: break-word;
}

.card-arrow {
    position: absolute;
    top: 20px;
    right: 20px;
    opacity: 0;
    transition: all 0.3s ease;
}

.card-arrow i {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.6);
}

.action-card:hover .card-arrow {
    opacity: 1;
    transform: translateX(4px);
}

/* 教学统计 */
.teaching-stats {
    margin-bottom: clamp(24px, 4vw, 32px);
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: clamp(16px, 3vw, 20px);
}

.stat-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 16px;
    padding: clamp(16px, 3vw, 24px);
    display: flex;
    align-items: center;
    gap: clamp(12px, 2vw, 16px);
    transition: all 0.3s ease;
    animation: stat-fade-in 0.8s ease-out;
}

@keyframes stat-fade-in {
    0% { opacity: 0; transform: scale(0.9); }
    100% { opacity: 1; transform: scale(1); }
}

.stat-card:hover {
    background: rgba(255, 255, 255, 0.15);
    /* transform: translateY(-4px); */
}

.stat-icon {
    width: 48px;
    height: 48px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-icon i {
    font-size: 20px;
    color: #fff;
}

.stat-content {
    flex: 1;
    min-width: 0;
}

.stat-number {
    font-size: clamp(20px, 3vw, 28px);
    font-weight: 700;
    color: #fff;
    margin: 0 0 4px 0;
    word-break: break-word;
}

.stat-label {
    font-size: clamp(12px, 2vw, 14px);
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
    word-break: break-word;
}

/* 最近活动 */
.recent-activities {
    margin-bottom: clamp(24px, 4vw, 32px);
}

.activities-list {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    padding: clamp(16px, 3vw, 24px);
    animation: activities-fade-in 0.8s ease-out;
}

@keyframes activities-fade-in {
    0% { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

.activity-item {
    display: flex;
    align-items: flex-start;
    gap: clamp(12px, 2vw, 16px);
    padding: clamp(12px, 2vw, 16px) 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.3s ease;
}

.activity-item:last-child {
    border-bottom: none;
}

.activity-item:hover {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    padding: clamp(12px, 2vw, 16px);
    margin: 0 -16px;
}

.activity-icon {
    width: 40px;
    height: 40px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.activity-icon i {
    font-size: 16px;
    color: #fff;
}

.activity-content {
    flex: 1;
    min-width: 0;
}

.activity-title {
    font-size: clamp(14px, 2.5vw, 16px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 clamp(4px, 1vw, 6px) 0;
    word-break: break-word;
}

.activity-desc {
    font-size: clamp(12px, 2vw, 14px);
    color: rgba(255, 255, 255, 0.7);
    margin: 0 0 clamp(4px, 1vw, 6px) 0;
    line-height: 1.4;
    word-break: break-word;
    overflow-wrap: break-word;
}

.activity-time {
    font-size: clamp(11px, 1.8vw, 12px);
    color: rgba(255, 255, 255, 0.5);
    word-break: break-word;
}

/* 加载状态 */
.loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    border: 1px solid rgba(255, 255, 255, 0.2);
}

.loading-spinner {
    text-align: center;
    color: #fff;
}

.loading-spinner i {
    font-size: 48px;
    margin-bottom: 16px;
    animation: spin 1s linear infinite;
}

.loading-spinner p {
    font-size: 16px;
    margin: 0;
    opacity: 0.8;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    border: 1px solid rgba(255, 255, 255, 0.2);
}

.error-content {
    text-align: center;
    color: #fff;
    max-width: 400px;
    padding: 32px;
}

.error-content i {
    font-size: 48px;
    color: #ff6b6b;
    margin-bottom: 16px;
}

.error-content h3 {
    font-size: 24px;
    margin: 0 0 12px 0;
    color: #ff6b6b;
}

.error-content p {
    font-size: 16px;
    margin: 0 0 24px 0;
    opacity: 0.8;
    line-height: 1.5;
}

.retry-btn {
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 12px;
    padding: 12px 24px;
    color: #fff;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}

.retry-btn:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: translateY(-2px);
}

/* 空状态 */
.empty-activities {
    text-align: center;
    padding: 48px 24px;
    color: rgba(255, 255, 255, 0.6);
}

.empty-activities i {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
}

.empty-activities p {
    font-size: 16px;
    margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .actions-grid {
        grid-template-columns: 1fr;
    }
    
    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }
    
    .welcome-title {
        flex-direction: column;
        align-items: flex-start;
        text-align: left;
    }
}

@media (max-width: 480px) {
    .stats-grid {
        grid-template-columns: 1fr;
    }
    
    .activity-item {
        flex-direction: column;
        align-items: flex-start;
    }
    
    .activity-icon {
        align-self: flex-start;
    }
}

@media (max-width: 360px) {
    .welcome-section {
        padding: 16px;
    }
    
    .action-card {
        padding: 16px;
    }
    
    .stat-card {
        padding: 12px;
    }
    
    .activities-list {
        padding: 12px;
    }
}
</style>