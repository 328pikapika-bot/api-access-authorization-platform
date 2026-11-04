import request from '@/api/index'

/**
 * 获取当前用户的接口凭证列表
 * @param {number} applicationId - 授权记录ID
 */
export function getMyCredentials(applicationId) {
  return request({
    url: `/user/credentials/${applicationId}`,
    method: 'get'
  })
}
