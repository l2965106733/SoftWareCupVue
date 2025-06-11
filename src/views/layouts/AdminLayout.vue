<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import router from '../../router';

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
    router.push('/admin/reset');
}

const logOut = () => {
    ElMessageBox.confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        // 清除登录用户信息
        // localStorage.removeItem('loginUser');
        // 跳转到登录页面
        router.push('/admin/login')
    }).catch(() => {
        ElMessage.info('已取消退出登录');
    });
}

</script>

<template>
    <div class="common-layout">
        <el-container>
            <!-- Header 区域 -->
            <el-header class="header">
                <span class="title">管理员系统</span>
                <span class="right_tool">
                    <a href="javascript:;" @click="reset">
                        <el-icon>
                            <EditPen />
                        </el-icon> 修改密码 &nbsp;&nbsp;&nbsp; | &nbsp;&nbsp;&nbsp;
                    </a>
                    <a href="javascript:;" @click="logOut">
                        <el-icon>
                            <SwitchButton />
                        </el-icon> 退出登录 [{{ loginName }}]
                    </a>
                </span>
            </el-header>

            <el-container>
                <!-- 左侧菜单 -->
                <el-aside width="200px" class="aside">
                    <!-- 左侧菜单栏 -->
                    <el-menu router>
                        <!-- 首页菜单 -->
                        <el-menu-item index="/admin/home">
                            <el-icon>
                                <Promotion />
                            </el-icon> 首页
                        </el-menu-item>


                        <el-sub-menu index="/managent">
                            <template #title>
                                <el-icon>
                                    <Tools />
                                </el-icon>用户管理
                            </template>
                            <el-menu-item index="/admin/students">
                                <el-icon>
                                    <HelpFilled />
                                </el-icon>学生管理
                            </el-menu-item>
                            <el-menu-item index="/admin/teachers">
                                <el-icon>
                                    <Avatar />
                                </el-icon>教师管理
                            </el-menu-item>
                            <el-menu-item index="/admin/admins">
                                <el-icon>
                                    <Avatar />
                                </el-icon>管理员管理
                            </el-menu-item>
                        </el-sub-menu>


                        <el-menu-item index="/admin/resources">
                            <el-icon>
                                <Menu />
                            </el-icon> 课程资源管理
                        </el-menu-item>

                        <el-sub-menu index="/status">
                            <template #title>
                                <el-icon>
                                    <Tools />
                                </el-icon>数据概览
                            </template>
                            <el-menu-item index="/admin/teacherstats">
                                <el-icon>
                                    <HelpFilled />
                                </el-icon>教师统计
                            </el-menu-item>
                            <el-menu-item index="/admin/studentstats">
                                <el-icon>
                                    <Avatar />
                                </el-icon>学生统计
                            </el-menu-item>
                            <el-menu-item index="/admin/overallstats">
                                <el-icon>
                                    <Avatar />
                                </el-icon>总体统计
                            </el-menu-item>
                        </el-sub-menu>
                    </el-menu>
                </el-aside>

                <el-main>
                    <RouterView></RouterView>
                </el-main>
            </el-container>

        </el-container>
    </div>
</template>

<style scoped>
.header {
    background-image: linear-gradient(to right, #00547d, #007fa4, #00aaa0, #00d072, #a8eb12);
}

.title {
    color: white;
    font-size: 40px;
    font-family: 楷体;
    line-height: 60px;
    font-weight: bolder;
}

.right_tool {
    float: right;
    line-height: 60px;
}

a {
    color: white;
    text-decoration: none;
}

.aside {
    width: 220px;
    border-right: 1px solid #ccc;
    height: 730px;
}
</style>
