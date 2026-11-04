import request from './index'

/**
 * 获取已发布的分类树（前端专用）
 * @param {object} params - 查询参数
 * @param {number} params.columnId - 栏目ID
 * 只返回状态为已发布的分类数据
 */
export function getCategoryTree(params) {
  return request({
    url: `/api/frontend/my/columns/${params.columnId}/categories`,
    method: 'get'
  })
}

/**
 * 获取已发布的分类列表（前端专用）
 * @param {object} params - 查询参数
 * @param {number} params.columnId - 栏目ID
 * 只返回状态为已发布的分类数据
 */
export function getCategoryList(params) {
  return request({
    url: `/api/frontend/my/columns/${params.columnId}/categories`,
    method: 'get'
  })
}
