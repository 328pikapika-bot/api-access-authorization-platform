import request from './index'

/**
 * 获取Mockoon服务列表
 * @param {Object} params - 查询参数
 */
export function getMockoonServiceList(params) {
  return request({
    url: '/admin/mockoon-service/list',
    method: 'get',
    params
  })
}

/**
 * 获取所有启用的服务
 */
export function getActiveServiceList() {
  return request({
    url: '/admin/mockoon-service/active-list',
    method: 'get'
  })
}

/**
 * 根据ID获取服务详情
 * @param {number} id - 服务ID
 */
export function getMockoonServiceById(id) {
  return request({
    url: `/admin/mockoon-service/${id}`,
    method: 'get'
  })
}

/**
 * 新增服务
 * @param {Object} data - 服务数据
 */
export function createMockoonService(data) {
  return request({
    url: '/admin/mockoon-service',
    method: 'post',
    data
  })
}

/**
 * 更新服务
 * @param {number} id - 服务ID
 * @param {Object} data - 服务数据
 */
export function updateMockoonService(id, data) {
  return request({
    url: `/admin/mockoon-service/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除服务
 * @param {number} id - 服务ID
 */
export function deleteMockoonService(id) {
  return request({
    url: `/admin/mockoon-service/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除服务
 * @param {Array} ids - 服务ID列表
 */
export function batchDeleteMockoonService(ids) {
  return request({
    url: '/admin/mockoon-service/batch',
    method: 'delete',
    params: { ids }
  })
}
