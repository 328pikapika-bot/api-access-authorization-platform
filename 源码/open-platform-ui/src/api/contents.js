import request from './index'

/**
 * 获取内容列表
 * @param {Object} params - 查询参数
 */
export function getContentList(params) {
  return request({
    url: `/api/frontend/my/categories/${params.categoryId}/contents`,
    method: 'get',
  })
}

/**
 * 获取内容详情
 * @param {Number} id - 内容ID
 */
export function getContentDetail(id) {
  return request({
    url: `/contents/${id}`,
    method: 'get'
  })
}

/**
 * 搜索内容
 * @param {String} keyword - 搜索关键字
 */
export function searchContents(keyword) {
  return request({
    url: '/contents/search',
    method: 'get',
    params: { keyword }
  })
}
