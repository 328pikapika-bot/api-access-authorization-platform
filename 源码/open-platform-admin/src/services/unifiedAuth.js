import {
  createNstlAuthManager,
  isNstlUnifiedLogoutStorageEvent,
} from '@shared/nstl-auth'
import { createNstlSession } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { clearAdminLoggingOut, isAdminLoggingOut, markAdminLoggingOut } from '@/utils/logoutState'
import { MessagePlugin } from 'tdesign-vue-next'

const ADMIN_PENDING_REDIRECT_KEY = 'admin_pending_redirect'

const authManager = createNstlAuthManager({
  storageKey: 'nstl_unified_session',
  userInfoEndpoint: '/api/service/nstl/web/vilidate/isLogin',
  userInfoOrigin: import.meta.env.VITE_NSTL_USER_INFO_ORIGIN,
  trustedOrigins: ['https://login.nstl.gov.cn', 'https://www.nstl.gov.cn', 'https://nstl.gov.cn'],
})

let syncingPromise = null
let handlingSessionExit = false

function getPendingRedirect() {
  return sessionStorage.getItem(ADMIN_PENDING_REDIRECT_KEY) || '/dashboard'
}

function setPendingRedirect(redirect = '/dashboard') {
  sessionStorage.setItem(ADMIN_PENDING_REDIRECT_KEY, redirect || '/dashboard')
}

function clearPendingRedirect() {
  sessionStorage.removeItem(ADMIN_PENDING_REDIRECT_KEY)
}

async function establishBackendSession(pinia, force = false) {
  const userStore = useUserStore(pinia)
  const nstlUser = authManager.getUserInfo()
  if (!nstlUser?.userId) {
    return false
  }
  if (!force && userStore.token && userStore.userInfo?.userId === nstlUser.userId) {
    return true
  }

  const res = await createNstlSession({
    userId: nstlUser.userId,
    username: nstlUser.username,
    email: nstlUser.email,
    nickname: nstlUser.nickname,
    phone: nstlUser.phone,
    nstlUnionId: nstlUser.nstlUnionId,
    ssoId: nstlUser.ssoId,
    unitName: nstlUser.unitName,
    loginType: 'admin',
  })

  if (res.code !== 200) {
    throw new Error(res.message || '未能建立后台管理登录会话')
  }

  userStore.setLoginInfo(res.data.token, res.data.userInfo)
  return true
}

export async function ensureAdminSession(pinia, redirect = '/dashboard', options = {}) {
  const { interactive = false, force = false } = options
  const userStore = useUserStore(pinia)

  if (syncingPromise) {
    return syncingPromise
  }

  syncingPromise = (async () => {
    setPendingRedirect(redirect)
    const loggedIn = await authManager.checkAuth()
    if (!loggedIn) {
      if (interactive) {
        authManager.showLoginModal()
      }
      return false
    }

    const ok = await establishBackendSession(pinia, force)
    if (ok) {
      clearPendingRedirect()
    }
    return ok
  })()

  try {
    return await syncingPromise
  } finally {
    syncingPromise = null
  }
}

export async function syncAdminSession(pinia) {
  const userStore = useUserStore(pinia)
  if (!userStore.token) {
    return false
  }

  const loggedIn = await authManager.checkAuth()
  if (!loggedIn) {
    userStore.clearLoginInfo()
    return false
  }

  return establishBackendSession(pinia, true)
}

export async function logoutAdmin(pinia) {
  markAdminLoggingOut()
  const userStore = useUserStore(pinia)
  try {
    await userStore.logout()
  } finally {
    clearPendingRedirect()
    await authManager.logout()
  }
}

export function setupAdminUnifiedAuth(router, pinia) {
  const handleSessionExit = () => {
    if (handlingSessionExit) {
      return
    }

    const explicitLogout = isAdminLoggingOut()
    handlingSessionExit = true
    markAdminLoggingOut(10000)
    authManager.clearLocalSession()
    clearPendingRedirect()
    const userStore = useUserStore(pinia)
    userStore.clearLoginInfo(false)

    if (router.currentRoute.value.meta.requiresAuth !== false) {
      router.replace({
        path: '/login',
        query: explicitLogout ? {} : { error: 'logged_out' },
      })
    }
  }

  window.addEventListener('authSuccess', async () => {
    handlingSessionExit = false
    clearAdminLoggingOut()
    const redirect = getPendingRedirect()
    try {
      const ok = await ensureAdminSession(pinia, redirect, { interactive: false, force: true })
      if (ok) {
        router.push(redirect)
      }
    } catch (error) {
      console.error('Failed to finalize admin login', error)
      MessagePlugin.error(error.message || '统一认证登录失败，请稍后重试')
    }
  })

  window.addEventListener('authLogout', () => {
    handleSessionExit()
  })

  window.addEventListener('storage', (event) => {
    if (!isNstlUnifiedLogoutStorageEvent(event)) {
      return
    }
    handleSessionExit()
  })

  const syncCurrentSession = () => {
    const userStore = useUserStore(pinia)
    if (document.visibilityState === 'visible' && userStore.token) {
      void syncAdminSession(pinia).then((loggedIn) => {
        if (!loggedIn) {
          handleSessionExit()
        }
      })
    }
  }

  window.addEventListener('focus', syncCurrentSession)
  document.addEventListener('visibilitychange', syncCurrentSession)
}
