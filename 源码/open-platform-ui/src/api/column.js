import request from './index'

/**
 * 获取已发布的栏目列表（前端专用）
 * @param {number} platformId 平台ID
 * 只返回状态为已发布的栏目数据
 */
export function getColumnList(platformId) {
  return request({
    url: `/api/frontend/my/platforms/${platformId}/columns`,
    method: 'get'
  })
}
