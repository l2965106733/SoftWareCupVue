import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/public/Login.vue') },
  { path: '/reset', name: 'Reset', component: () => import('@/views/public/Reset.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/public/Register.vue') },
  // 学生模块
  {
    path: '/student',
    redirect: '/student/home',
    children: [
      {
        path: '',
        component: () => import('@/views/layouts/StudentLayout.vue'),
        meta: { role: 'student' },
        redirect: '/student/home',
        children: [
          { path: 'home', name: 'StudentHome', component: () => import('@/views/student/Home.vue') },
          { path: 'study', name: 'StudentStudy', component: () => import('@/views/student/Study.vue') },
          { path: 'practise', name: 'StudentPractise', component: () => import('@/views/student/Practise.vue') },
          { path: 'interact', name: 'StudentInteract', component: () => import('@/views/student/Interact.vue') }
        ]
      }
    ]
  },

  // 教师模块
  {
    path: '/teacher',
    redirect: '/teacher/home',
    children: [
      {
        path: '',
        component: () => import('@/views/layouts/TeacherLayout.vue'),
        meta: { role: 'teacher' },
        redirect: '/teacher/home',
        children: [
          { path: 'home', name: 'TeacherHome', component: () => import('@/views/teacher/Home.vue') },
          { path: 'resource', name: 'TeacherResource', component: () => import('@/views/teacher/Resource.vue') },
          { path: 'practise', name: 'TeacherPractise', component: () => import('@/views/teacher/Practise.vue') },
          { path: 'analysis', name: 'TeacherAnalysis', component: () => import('@/views/teacher/Analysis.vue') },
          { path: 'interact', name: 'TeacherInteract', component: () => import('@/views/teacher/Interact.vue') }
        ]
      }
    ]
  },

  // 管理员模块
  {
    path: '/admin',
    redirect: '/admin/home',
    children: [
      {
        path: '',
        component: () => import('@/views/layouts/AdminLayout.vue'),
        meta: { role: 'admin' },
        redirect: '/admin/home',
        children: [
          { path: 'home', name: 'AdminHome', component: () => import('@/views/admin/Home.vue') },
          { path: 'user', name: 'AdminUser', component: () => import('@/views/admin/User.vue') },
          { path: 'resource', name: 'AdminResource', component: () => import('@/views/admin/Resource.vue') },
          { path: 'teacherstats', name: 'AdminTeacherStats', component: () => import('@/views/admin/TeacherStats.vue') },
          { path: 'studentstats', name: 'AdminStudentStats', component: () => import('@/views/admin/StudentStats.vue') },
          { path: 'overallstats', name: 'AdminOverallStats', component: () => import('@/views/admin/OverallStats.vue') }
        ]
      }
    ]
  },

  // 通配 404 页（可选）
  // {
  //   path: '/:pathMatch(.*)*',
  //   name: 'NotFound',
  //   component: () => import('@/views/public/NotFound.vue')
  // }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
