import request from './index'

/**
 * 分页查询用户列表
 */
export function getUserPage(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 更新用户角色
 */
export function updateUserRole(id, role) {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'put',
    params: { role }
  })
}

/**
 * 删除系统用户
 */
export function deleteUser(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'delete'
  })
}

/**
 * 新增用户
 * @param {Object} data 用户信息
 */
export function addUser(data) {
  return request({
    url: '/admin/users',
    method: 'post',
    data
  })
}

/**
 * 预校验新增用户信息
 */
export function previewUser(params) {
  return request({
    url: '/admin/users/preview',
    method: 'get',
    params
  })
}
