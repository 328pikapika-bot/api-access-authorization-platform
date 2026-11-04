import request from './index'

/**
 * 获取已发布的平台列表（前端专用）
 * 只返回状态为已发布的平台数据
 */
export function getPlatformList() {
  return request({
    url: '/api/frontend/my/platforms',
    method: 'get'
  })
}
