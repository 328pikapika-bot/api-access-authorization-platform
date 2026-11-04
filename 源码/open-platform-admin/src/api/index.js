import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { isAdminLoggingOut, markAdminLoggingOut } from '@/utils/logoutState'

const request = axios.create({
  baseURL: '/api/v1/service/open_platform',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

const rejectSessionError = (error) => {
  error.__adminLoggingOut = true
  markAdminLoggingOut(10000)
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_user')

  try {
    const userStore = useUserStore()
    userStore.clearLoginInfo(false)
  } catch (storeError) {
    console.error('Failed to clear admin user store after session exit', storeError)
  }

  const loginUrl = new URL(
    'login?error=logged_out',
    window.location.origin + (import.meta.env.BASE_URL || '/')
  )
  if (window.location.pathname !== loginUrl.pathname) {
    window.location.href = loginUrl.toString()
  }
  return Promise.reject(error)
}

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data?.code === 401) {
      const error = new Error(data.message || '当前登录已退出，请重新登录')
      error.response = { status: 401, data }
      return rejectSessionError(error)
    }
    return data
  },
  (error) => {
    console.error('请求错误:', error)

    if (isAdminLoggingOut()) {
      error.__adminLoggingOut = true
      return Promise.reject(error)
    }

    if (error.response?.status === 401
      || (error.response?.status === 403 && !localStorage.getItem('admin_token'))) {
      return rejectSessionError(error)
    }
    return Promise.reject(error)
  }
)

export default request
