import {
  createNstlAuthManager,
  isNstlUnifiedLogoutStorageEvent,
} from '@shared/nstl-auth'
import { createNstlSession } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const PORTAL_PENDING_REDIRECT_KEY = 'portal_pending_redirect'

const authManager = createNstlAuthManager({
  storageKey: 'nstl_unified_session',
  userInfoEndpoint: '/api/service/nstl/web/vilidate/isLogin',
  userInfoOrigin: import.meta.env.VITE_NSTL_USER_INFO_ORIGIN,
  trustedOrigins: ['https://login.nstl.gov.cn', 'https://www.nstl.gov.cn', 'https://nstl.gov.cn'],
})

let syncingPromise = null

function getPendingRedirect() {
  return sessionStorage.getItem(PORTAL_PENDING_REDIRECT_KEY) || '/contents'
}

function setPendingRedirect(redirect = '/contents') {
  sessionStorage.setItem(PORTAL_PENDING_REDIRECT_KEY, redirect || '/contents')
}

function clearPendingRedirect() {
  sessionStorage.removeItem(PORTAL_PENDING_REDIRECT_KEY)
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
    loginType: 'portal',
  })

  if (res.code !== 200) {
    throw new Error(res.message || '未能建立接口平台登录会话')
  }

  userStore.setToken(res.data.token)
  userStore.setUserInfo(res.data.userInfo)
  return true
}

export async function ensurePortalSession(pinia, redirect = '/contents', options = {}) {
  const { interactive = true, force = false } = options
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

export async function syncPortalSession(pinia) {
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

export async function logoutPortal(pinia) {
  const userStore = useUserStore(pinia)
  try {
    await userStore.logout()
  } finally {
    clearPendingRedirect()
    await authManager.logout()
  }
}

export function setupPortalUnifiedAuth(router, pinia) {
  window.addEventListener('authSuccess', async () => {
    const redirect = getPendingRedirect()
    try {
      const ok = await ensurePortalSession(pinia, redirect, { interactive: false, force: true })
      if (ok) {
        router.push(redirect)
      }
    } catch (error) {
      console.error('Failed to finalize portal login', error)
    }
  })

  window.addEventListener('authLogout', () => {
    const userStore = useUserStore(pinia)
    userStore.clearLoginInfo()
    if (router.currentRoute.value.meta.requiresAuth === true) {
      router.replace('/contents')
    }
  })

  window.addEventListener('storage', (event) => {
    if (!isNstlUnifiedLogoutStorageEvent(event)) {
      return
    }
    authManager.clearLocalSession()
    clearPendingRedirect()
    const userStore = useUserStore(pinia)
    userStore.clearLoginInfo()
    if (router.currentRoute.value.meta.requiresAuth === true) {
      router.replace('/contents')
    }
  })

  const syncCurrentSession = () => {
    if (document.visibilityState === 'visible') {
      void syncPortalSession(pinia)
    }
  }

  window.addEventListener('focus', syncCurrentSession)
  document.addEventListener('visibilitychange', syncCurrentSession)
}
