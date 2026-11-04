import axios from 'axios'

// 创建一个不显示错误提示的请求实例
const silentRequest = axios.create({
  baseURL: '/api/v1/service/open_platform',
  timeout: 30000
})

// 请求拦截器（复制主请求的配置）
silentRequest.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器（不显示错误提示）
silentRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // 静默处理，不弹出错误消息
      return Promise.reject(res)
    }
    return res
  },
  error => {
    // 静默处理，不弹出错误消息
    return Promise.reject(error)
  }
)

/**
 * 根据分类ID获取接口配置（静默模式，不显示错误提示）
 * @param {number} categoryId - 分类ID
 */
export function getInterfaceConfigByCategoryId(categoryId) {
  return silentRequest({
    url: `/admin/interface-config/by-category/${categoryId}`,
    method: 'get'
  })
}
