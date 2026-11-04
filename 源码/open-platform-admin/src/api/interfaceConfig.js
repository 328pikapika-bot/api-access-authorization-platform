import request from './index'

/**
 * 根据分类ID获取接口配置
 * @param {number} categoryId - 分类ID
 */
export function getInterfaceConfigByCategoryId(categoryId) {
  return request({
    url: `/admin/interface-config/by-category/${categoryId}`,
    method: 'get'
  })
}

/**
 * 保存或更新接口配置
 * @param {Object} data - 配置数据
 */
export function saveInterfaceConfig(data) {
  return request({
    url: '/admin/interface-config/save',
    method: 'post',
    data
  })
}

/**
 * 根据ID获取接口配置详情
 * @param {number} id - 配置ID
 */
export function getInterfaceConfigById(id) {
  return request({
    url: `/admin/interface-config/${id}`,
    method: 'get'
  })
}

/**
 * 删除接口配置
 * @param {number} id - 配置ID
 */
export function deleteInterfaceConfig(id) {
  return request({
    url: `/admin/interface-config/${id}`,
    method: 'delete'
  })
}

/**
 * 根据分类ID删除配置
 * @param {number} categoryId - 分类ID
 */
export function deleteInterfaceConfigByCategoryId(categoryId) {
  return request({
    url: `/admin/interface-config/by-category/${categoryId}`,
    method: 'delete'
  })
}
