import request from '@/api/index'

/**
 * 保存接口凭证分配
 * @param {number} applicationId - 授权记录ID
 * @param {Array} credentials - 凭证列表
 */
export function saveApiCredentials(applicationId, credentials) {
  return request({
    url: `/admin/credentials`,
    method: 'post',
    params: { applicationId },
    data: credentials
  })
}

/**
 * 获取接口凭证列表
 * @param {number} applicationId - 授权记录ID
 */
export function getApiCredentials(applicationId) {
  return request({
    url: `/admin/credentials`,
    method: 'get',
    params: { applicationId }
  })
}
