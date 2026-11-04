import request from './index'

/**
 * 分页查询审计日志
 */
export function getAuditLogPage(params) {
  return request({
    url: '/admin/logs/audit',
    method: 'get',
    params
  })
}

/**
 * 获取审计日志详情
 */
export function getAuditLogDetail(id) {
  return request({
    url: `/admin/logs/audit/${id}`,
    method: 'get'
  })
}

/**
 * 分页查询沙箱请求日志
 */
export function getSandboxLogPage(params) {
  return request({
    url: '/admin/logs/sandbox/page',
    method: 'get',
    params
  })
}

/**
 * 获取沙箱日志详情
 */
export function getSandboxLogDetail(id) {
  return request({
    url: `/admin/logs/sandbox/${id}`,
    method: 'get'
  })
}
