import request from '@/api/index'

/**
 * 获取接口权限分配列表（后台管理）
 * @param {object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {number} params.status - 分配状态
 * @param {number} params.platformId - 平台ID
 * @param {number} params.columnId - 栏目ID
 * @param {string} params.keyword - 关键词
 */
export function getApplicationList(params) {
  return request({
    url: '/admin/applications',
    method: 'get',
    params
  })
}

/**
 * 获取接口权限分配详情
 * @param {number} id - 分配记录ID
 */
export function getApplicationDetail(id) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'get'
  })
}

/**
 * 删除接口权限分配记录
 * @param {number} id - 分配记录ID
 */
export function deleteApplication(id) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'delete'
  })
}

/**
 * 管理员直接新增并生效的接口权限分配
 */
export function assignApplication(data) {
  return request({
    url: '/admin/applications/assign',
    method: 'post',
    data
  })
}

/**
 * 更新接口权限分配记录（后台管理用）
 * @param {number} id - 分配记录ID
 * @param {object} data - 分配数据
 * @param {number} data.platformId - 平台ID
 * @param {string} data.platformName - 平台名称
 * @param {number} data.columnId - 栏目ID
 * @param {string} data.columnName - 栏目名称
 * @param {string} data.categoryIds - 分类ID数组JSON字符串
 * @param {string} data.categoryNames - 分类名称逗号分隔
 * @param {string} data.unitName - 单位名称
 * @param {string} data.reason - 分配说明
 */
export function updateApplication(id, data) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'put',
    data
  })
}
