<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
    getSystemOverviewApi, 
    getRecentActivitiesApi 
} from '@/api/admin'

const router = useRouter()
const adminName = ref('')

// 系统统计数据
const stats = ref({
    totalUsers: 0,
    teacherCount: 0,
    studentCount: 0,
    resourceCount: 0
})



// 加载状态
const loading = ref({
    stats: false,
    status: false,
    activities: false
})

// 获取系统统计数据
const fetchSystemStats = async () => {
    loading.value.stats = true
    try {
        const response = await getSystemOverviewApi()
        if (response.code === 1) {
            stats.value = {
                totalUsers: response.data.totalUsers || 0,
                teacherCount: response.data.totalTeachers || 0,
                studentCount: response.data.totalStudents || 0,
                resourceCount: response.data.totalResources || 0
            }
        }
    } catch (error) {
        console.error('获取系统统计数据失败:', error)
    } finally {
        loading.value.stats = false
    }
}





// 根据活动类型获取图标
const getActivityIcon = (type) => {
    const iconMap = {
        'USER_REGISTER': 'fas fa-user-plus',
        'USER_LOGIN': 'fas fa-sign-in-alt',
        'USER_LOGOUT': 'fas fa-sign-out-alt',
        'RESOURCE_UPLOAD': 'fas fa-upload',
        'RESOURCE_DOWNLOAD': 'fas fa-download',
        'SYSTEM_MAINTENANCE': 'fas fa-cog',
        'REPORT_GENERATE': 'fas fa-chart-bar',
        'USER_UPDATE': 'fas fa-user-edit',
        'RESOURCE_UPDATE': 'fas fa-edit',
        'USER_DELETE': 'fas fa-user-times',
        'RESOURCE_DELETE': 'fas fa-trash'
    }
    return iconMap[type] || 'fas fa-info-circle'
}

// 格式化时间
const formatTime = (timestamp) => {
    if (!timestamp) return '刚刚'
    
    const now = new Date()
    const time = new Date(timestamp)
    const diff = now - time
    
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    
    return time.toLocaleDateString()
}

onMounted(async () => {
    // 获取管理员信息
    const loginUser = JSON.parse(localStorage.getItem('loginUser'))
    if (loginUser && loginUser.name) {
        adminName.value = loginUser.name
    }
    
    // 并行获取所有数据
    await Promise.all([
        fetchSystemStats()
    ])
})

// 导航方法
const goToUser = () => {
    router.push('/admin/user')
}

const goToResource = () => {
    router.push('/admin/resource')
}

const goToOverallStats = () => {
    router.push('/admin/overallstats')
}

const goToStudentStats = () => {
    router.push('/admin/studentstats')
}

const goToTeacherStats = () => {
    router.push('/admin/teacherstats')
}
</script>

<template>
    <div class="admin-home">
        <!-- 欢迎区域 -->
        <div class="welcome-section">
            <div class="welcome-content">
                <h1 class="welcome-title">
                    <i class="fas fa-user-shield welcome-icon"></i>
                    ，{{ adminName }}管理员！欢迎回来
                    <span style="margin-left: 10px;">欢迎回来，{{ adminName }}管理员！</span>
                </h1>
                <p class="welcome-subtitle">掌控全局，管理系统，为教育事业保驾护航</p>
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
                <div class="action-card" @click="goToUser">
                    <div class="card-icon">
                        <i class="fas fa-users"></i>
                    </div>
                    <h3 class="card-title">用户管理</h3>
                    <p class="card-desc">管理教师和学生账户信息</p>
                    <div class="card-arrow">
                        <i class="fas fa-arrow-right"></i>
                    </div>
                </div>

                <div class="action-card" @click="goToResource">
                    <div class="card-icon">
                        <i class="fas fa-folder-open"></i>
                    </div>
                    <h3 class="card-title">资源管理</h3>
                    <p class="card-desc">管理系统教学资源</p>
                    <div class="card-arrow">
                        <i class="fas fa-arrow-right"></i>
                    </div>
                </div>

                <div class="action-card" @click="goToOverallStats">
                    <div class="card-icon">
                        <i class="fas fa-chart-pie"></i>
                    </div>
                    <h3 class="card-title">总体统计</h3>
                    <p class="card-desc">查看系统整体运行情况</p>
                    <div class="card-arrow">
                        <i class="fas fa-arrow-right"></i>
                    </div>
                </div>
            </div>
        </div>

        <!-- 系统统计 -->
        <div class="system-stats">
            <h2 class="section-title">
                <i class="fas fa-chart-line"></i>
                系统概览
                <span v-if="loading.stats" class="loading-indicator">
                    <i class="fas fa-spinner fa-spin"></i>
                </span>
            </h2>
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-icon">
                        <i class="fas fa-users"></i>
                    </div>
                    <div class="stat-content">
                        <h3 class="stat-number">{{ stats.totalUsers }}</h3>
                        <p class="stat-label">总用户数</p>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">
                        <i class="fas fa-chalkboard-teacher"></i>
                    </div>
                    <div class="stat-content">
                        <h3 class="stat-number">{{ stats.teacherCount }}</h3>
                        <p class="stat-label">教师数量</p>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">
                        <i class="fas fa-user-graduate"></i>
                    </div>
                    <div class="stat-content">
                        <h3 class="stat-number">{{ stats.studentCount }}</h3>
                        <p class="stat-label">学生数量</p>
                    </div>
                </div>

                <div class="stat-card">
                    <div class="stat-icon">
                        <i class="fas fa-folder"></i>
                    </div>
                    <div class="stat-content">
                        <h3 class="stat-number">{{ stats.resourceCount }}</h3>
                        <p class="stat-label">资源数量</p>
                    </div>
                </div>
            </div>
        </div>



    
    </div>
</template>

<!-- <style scoped>

/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.admin-home {
    min-height: 100%;
    animation: admin-page-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
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
    animation: admin-welcome-slide-up 1s cubic-bezier(0.4, 0, 0.2, 1);
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
    animation: admin-title-glow 3s ease-in-out infinite alternate;
}

.welcome-icon {
    font-size: clamp(28px, 5vw, 40px);
    animation: admin-icon-pulse 2s ease-in-out infinite;
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
    animation: admin-float 6s ease-in-out infinite;
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

/* 通用区域标题 */
.section-title {
    font-size: clamp(20px, 3vw, 24px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 clamp(16px, 3vw, 24px) 0;
    display: flex;
    align-items: center;
    gap: clamp(8px, 2vw, 12px);
    animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
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
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
    animation: admin-card-slide-up 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-card:hover {
    transform: translateY(-8px);
    background: rgba(255, 255, 255, 0.15);
    box-shadow: 0 12px 32px rgba(0,0,0,0.2);
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

/* 系统统计 */
.system-stats {
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
    transform: translateY(-4px);
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


.status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: clamp(16px, 3vw, 20px);
}

.status-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 16px;
    padding: clamp(16px, 3vw, 20px);
    display: flex;
    align-items: center;
    gap: clamp(12px, 2vw, 16px);
    transition: all 0.3s ease;
    animation: status-fade-in 0.8s ease-out;
}

@keyframes status-fade-in {
    0% { opacity: 0; transform: translateX(-20px); }
    100% { opacity: 1; transform: translateX(0); }
}

.status-card:hover {
    background: rgba(255, 255, 255, 0.15);
    transform: translateY(-2px);
}

.status-indicator {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    flex-shrink: 0;
    animation: pulse 2s ease-in-out infinite;
}

.status-indicator.online {
    background: #4ade80;
    box-shadow: 0 0 8px rgba(74, 222, 128, 0.5);
}

.status-indicator.warning {
    background: #fbbf24;
    box-shadow: 0 0 8px rgba(251, 191, 36, 0.5);
}

.status-indicator.checking {
    background: #6b7280;
    box-shadow: 0 0 8px rgba(107, 114, 128, 0.5);
    animation: checking-pulse 1.5s ease-in-out infinite;
}

@keyframes checking-pulse {
    0%, 100% { opacity: 0.6; }
    50% { opacity: 1; }
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.6; }
}

.status-content {
    flex: 1;
    min-width: 0;
}

.status-title {
    font-size: clamp(14px, 2.5vw, 16px);
    font-weight: 600;
    color: #fff;
    margin: 0 0 4px 0;
    word-break: break-word;
}

.status-desc {
    font-size: clamp(12px, 2vw, 13px);
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
    word-break: break-word;
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

/* 加载指示器 */
.loading-indicator {
    margin-left: 8px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .actions-grid {
        grid-template-columns: 1fr;
    }
    
    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }
    
    .status-grid {
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
    
    .status-grid {
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
    
    .status-card {
        padding: 12px;
    }
    
    .activities-list {
        padding: 12px;
    }
}
</style> -->
<style scoped>
/* 引入FontAwesome */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css');

.admin-home {
    min-height: 100%;
    background: #f9f9f9;
    color: #333;
    animation: admin-page-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 欢迎区域 */
.welcome-section {
    position: relative;
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    margin-bottom: 32px;
    animation: admin-welcome-slide-up 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.welcome-content {
    position: relative;
    z-index: 2;
}

.welcome-title {
    font-size: clamp(24px, 4vw, 36px);
    font-weight: 700;
    color: #333;
    margin: 0 0 16px 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.welcome-icon {
    font-size: clamp(28px, 5vw, 40px);
}

.welcome-subtitle {
    font-size: clamp(14px, 2.5vw, 18px);
    color: #666;
    margin: 0;
    line-height: 1.6;
    word-break: break-word;
    overflow-wrap: break-word;
}

/* 通用区域标题 */
.section-title {
    font-size: clamp(20px, 3vw, 24px);
    font-weight: 600;
    color: #333;
    margin: 0 0 24px 0;
    display: flex;
    align-items: center;
    gap: 8px;
    animation: admin-section-fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 快捷操作区域 */
.quick-actions {
    margin-bottom: 32px;
}

.actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 24px;
}

.action-card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 20px;
    padding: 32px;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
    animation: admin-card-slide-up 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-card:hover {
    transform: translateY(-8px);
    background: #f5f5f5;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.card-icon {
    width: 60px;
    height: 60px;
    background: rgba(0, 0, 0, 0.1);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
}

.card-icon i {
    font-size: 24px;
    color: #333;
}

.card-title {
    font-size: clamp(16px, 2.5vw, 20px);
    font-weight: 600;
    color: #333;
    margin: 0 0 12px 0;
}

.card-desc {
    font-size: clamp(13px, 2vw, 14px);
    color: #666;
    margin: 0;
    line-height: 1.5;
}

/* 系统统计 */
.system-stats {
    margin-bottom: 32px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.stat-card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 16px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    transition: all 0.3s ease;
}

.stat-card:hover {
    background: #f5f5f5;
    transform: translateY(-4px);
}

.stat-icon {
    width: 48px;
    height: 48px;
    background: #f0f0f0;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.stat-icon i {
    font-size: 20px;
    color: #333;
}

.stat-number {
    font-size: clamp(20px, 3vw, 28px);
    font-weight: 700;
    color: #333;
    margin: 0 0 4px 0;
}

.stat-label {
    font-size: clamp(12px, 2vw, 14px);
    color: #666;
}

/* 状态指示 */
.status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.status-card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 16px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    transition: all 0.3s ease;
    animation: status-fade-in 0.8s ease-out;
}

@keyframes status-fade-in {
    0% { opacity: 0; transform: translateX(-20px); }
    100% { opacity: 1; transform: translateX(0); }
}

.status-card:hover {
    background: #f5f5f5;
    transform: translateY(-2px);
}

.status-indicator {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    flex-shrink: 0;
}

.status-indicator.online {
    background: #4ade80;
}

.status-indicator.warning {
    background: #fbbf24;
}

.status-indicator.checking {
    background: #6b7280;
    animation: checking-pulse 1.5s ease-in-out infinite;
}

@keyframes checking-pulse {
    0%, 100% { opacity: 0.6; }
    50% { opacity: 1; }
}

.status-content {
    flex: 1;
    min-width: 0;
}

.status-title {
    font-size: clamp(14px, 2.5vw, 16px);
    font-weight: 600;
    color: #333;
    margin: 0 0 4px 0;
}

.status-desc {
    font-size: clamp(12px, 2vw, 13px);
    color: #666;
    margin: 0;
}

/* 活动列表 */
.activities-list {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 20px;
    padding: 24px;
    animation: activities-fade-in 0.8s ease-out;
}

.activity-item {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 16px 0;
    border-bottom: 1px solid #e0e0e0;
}

.activity-item:last-child {
    border-bottom: none;
}

.activity-item:hover {
    background: #f5f5f5;
    border-radius: 12px;
    padding: 16px;
}

.activity-icon {
    width: 40px;
    height: 40px;
    background: #f0f0f0;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.activity-icon i {
    font-size: 16px;
    color: #333;
}

.activity-title {
    font-size: clamp(14px, 2.5vw, 16px);
    font-weight: 600;
    color: #333;
    margin: 0 0 6px 0;
}

.activity-desc {
    font-size: clamp(12px, 2vw, 14px);
    color: #666;
    margin: 0 0 6px 0;
}

.activity-time {
    font-size: clamp(11px, 1.8vw, 12px);
    color: #999;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .actions-grid {
        grid-template-columns: 1fr;
    }

    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .status-grid {
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

@media (max-width: 480px) {
    .stats-grid {
        grid-template-columns: 1fr;
    }
    
    .activity-item {
        flex-direction: column;
        align-items: flex-start;
    }
}

@media (max-width: 360px) {
    .welcome-section {
        padding: 16px;
    }

    .action-card, .stat-card, .status-card, .activities-list {
        padding: 16px;
    }
}
</style>
