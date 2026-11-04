import request from './index'

/**
 * 获取我的授权记录
 */
export function getMyApplications() {
  return request({
    url: '/user/applications',
    method: 'get'
  })
}

/**
 * 检查分类授权情况
 * @param {number} categoryId - 分类ID
 */
export function checkApplicationAuth(categoryId) {
  return request({
    url: '/user/application/check-auth',
    method: 'get',
    params: { categoryId }
  })
}

/**
 * 获取我的接口凭证
 * @param {number} applicationId - 授权记录ID
 */
export function getMyCredentials(applicationId) {
  return request({
    url: `/user/credentials/${applicationId}`,
    method: 'get'
  })
}


