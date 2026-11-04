import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo, logout as logoutApi } from '@/api/auth'

const ADMIN_LOGOUT_EVENT_KEY = 'admin_logout_at'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.phone || userInfo.value?.userName || userInfo.value?.userId || userInfo.value?.nickname || '管理员')
  const isAdmin = computed(() => userInfo.value?.userRole === 1)

  // 设置登录信息
  function setLoginInfo(newToken, newUser) {
    token.value = newToken
    userInfo.value = newUser
    localStorage.setItem('admin_token', newToken)
    localStorage.setItem('admin_user', JSON.stringify(newUser))
  }

  // 清除登录信息
  function notifyLogout() {
    localStorage.setItem(ADMIN_LOGOUT_EVENT_KEY, String(Date.now()))
  }

  function clearLoginInfo(emitLogoutEvent = true) {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    if (emitLogoutEvent) {
      notifyLogout()
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      if (res.code === 200) {
        userInfo.value = res.data
        localStorage.setItem('admin_user', JSON.stringify(res.data))
        return res.data
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
    return null
  }

  // 退出登录
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      clearLoginInfo()
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    isAdmin,
    setLoginInfo,
    clearLoginInfo,
    notifyLogout,
    fetchUserInfo,
    logout
  }
})
