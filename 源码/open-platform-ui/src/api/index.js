import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/v1/service/open_platform',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      MessagePlugin.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')

      try {
        const userStore = useUserStore()
        userStore.clearLoginInfo()
      } catch (storeError) {
        console.error('Failed to clear portal user store after 401', storeError)
      }

      const basePath = import.meta.env.BASE_URL || '/'
      if (window.location.pathname !== basePath) {
        window.location.href = new URL(basePath, window.location.origin).toString()
      }
    } else {
      MessagePlugin.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
