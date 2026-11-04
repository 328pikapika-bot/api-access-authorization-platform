import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ensurePortalSession } from '@/services/unifiedAuth'

const routes = [
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('@/views/auth/Callback.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    children: [
      {
        path: '',
        redirect: '/contents'
      },
      {
        path: 'contents',
        name: 'ContentList',
        component: () => import('@/views/contents/Index.vue')
      },
      {
        path: 'user',
        name: 'UserCenter',
        component: () => import('@/views/user/Index.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth === true) {
    try {
      const loggedIn = await ensurePortalSession(undefined, to.fullPath, { interactive: true })
      loggedIn ? next() : next(false)
    } catch (error) {
      console.error('Failed to establish portal session', error)
      next(false)
    }
    return
  }

  next()
})

export default router
