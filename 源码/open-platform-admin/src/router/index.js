import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ensureAdminSession } from '@/services/unifiedAuth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('@/views/Callback.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard',
    meta: { requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/category-manage',
    name: 'CategoryManage',
    component: () => import('@/views/CategoryManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/permission-manage',
    name: 'PermissionManage',
    component: () => import('@/views/PermissionManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/api-permission-assign',
    name: 'ApiPermissionAssign',
    component: () => import('@/views/ApiPermissionAssign.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/system-logs',
    name: 'SystemLogs',
    component: () => import('@/views/SystemLogs.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/content-logs',
    name: 'ContentLogs',
    component: () => import('@/views/ContentLogs.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/mockoon-service',
    name: 'MockoonService',
    component: () => import('@/views/MockoonServiceManage.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const hadAdminSession = userStore.isLoggedIn
  let loggedIn = false
  try {
    loggedIn = await ensureAdminSession(undefined, to.fullPath, { interactive: false })
  } catch (error) {
    console.error('Failed to verify NSTL admin session', error)
  }

  if (!loggedIn || !userStore.isLoggedIn) {
    if (hadAdminSession && !loggedIn) {
      userStore.clearLoginInfo(false)
    }
    next({
      path: '/login',
      query: {
        redirect: to.fullPath,
        ...(hadAdminSession && !loggedIn ? { error: 'logged_out' } : {})
      }
    })
    return
  }

  if (!userStore.isAdmin) {
    next({
      path: '/login',
      query: { redirect: to.fullPath, error: 'no_admin' }
    })
    return
  }

  next()
})

export default router
