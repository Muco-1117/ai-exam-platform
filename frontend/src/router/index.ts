import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'
import Layout from '@/layout/Layout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
        { path: 'questions', name: 'QuestionBank', component: () => import('@/views/QuestionBank.vue') },
        { path: 'questions/:id', name: 'QuestionDetail', component: () => import('@/views/QuestionDetail.vue') },
        { path: 'practice', name: 'PracticeRecords', component: () => import('@/views/PracticeRecords.vue') },
        { path: 'review', name: 'ReviewSession', component: () => import('@/views/ReviewSession.vue') },
        { path: 'wrong', name: 'WrongBook', component: () => import('@/views/WrongBook.vue') },
        { path: 'favorites', name: 'MyFavorites', component: () => import('@/views/MyFavorites.vue') },
        { path: 'exams', name: 'ExamList', component: () => import('@/views/ExamList.vue') },
        { path: 'exams/:examId/rank', name: 'ExamRank', component: () => import('@/views/ExamRank.vue') },
        { path: 'exam-taking/:examId', name: 'ExamTaking', component: () => import('@/views/ExamTaking.vue') },
        { path: 'exam-result/:recordId', name: 'ExamResult', component: () => import('@/views/ExamResult.vue') },
        { path: 'exam-records', name: 'ExamRecords', component: () => import('@/views/ExamRecords.vue') },
        { path: 'admin/services', name: 'ServiceMonitor', component: () => import('@/views/admin/ServiceMonitor.vue'), meta: { requiresAdmin: true } },
        // 管理端
        { path: 'admin/questions', name: 'QuestionManage', component: () => import('@/views/admin/QuestionManage.vue'), meta: { requiresAdmin: true } },
        { path: 'admin/exams', name: 'ExamManage', component: () => import('@/views/admin/ExamManage.vue'), meta: { requiresAdmin: true } },
        { path: 'admin/categories', name: 'CategoryManage', component: () => import('@/views/admin/CategoryManage.vue'), meta: { requiresAdmin: true } },
        { path: 'admin/users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { requiresAdmin: true } },
        { path: 'admin/ai-logs', name: 'AiLog', component: () => import('@/views/admin/AiLog.vue'), meta: { requiresAdmin: true } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    if (to.path === '/login' && getToken()) {
      return '/'
    }
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined }
  }
  // 有 token 但未加载用户信息时拉取一次
  if (!userStore.userInfo) {
    try {
      await userStore.fetchMe()
    } catch {
      return { path: '/login' }
    }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return '/'
  }
  return true
})

export default router
