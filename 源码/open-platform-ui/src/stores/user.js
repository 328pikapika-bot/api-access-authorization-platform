import { defineStore } from 'pinia'
import { getUserInfo, logout as apiLogout } from '@/api/auth'

function readStoredUserInfo() {
  const raw = localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    console.warn('Invalid cached portal user info, clearing it', error)
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: readStoredUserInfo(),
    isLoggedIn: !!localStorage.getItem('token')
  }),

  getters: {
    username: (state) => state.userInfo?.phone || state.userInfo?.userName || state.userInfo?.userId || state.userInfo?.nickname || '开发者',
    userId: (state) => state.userInfo?.id,
    userRole: (state) => state.userInfo?.userRole || 0
  },

  actions: {
    setToken(token) {
      this.token = token
      this.isLoggedIn = true
      localStorage.setItem('token', token)
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    clearLoginInfo() {
      this.token = ''
      this.userInfo = null
      this.isLoggedIn = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },

    async getUserInfo() {
      try {
        const res = await getUserInfo()
        if (res.code === 200) {
          this.setUserInfo(res.data)
          return res.data
        }
        return null
      } catch (error) {
        console.error('获取用户信息失败', error)
        return null
      }
    },

    async logout() {
      try {
        await apiLogout()
      } catch (error) {
        console.error('退出登录失败', error)
      } finally {
        this.clearLoginInfo()
      }
    }
  }
})
