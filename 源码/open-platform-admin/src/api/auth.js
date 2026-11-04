import request from './index'

/**
 * 用code换取token和用户信息
 */
export function ssoCallback(code, redirectUri, loginType) {
  const params = { code, redirect_uri: redirectUri }
  if (loginType) {
    params.login_type = loginType
  }
  return request({
    url: '/auth/sso/callback',
    method: 'post',
    params
  })
}

export function createNstlSession(data) {
  return request({
    url: '/auth/nstl/session',
    method: 'post',
    data
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/auth/user/info',
    method: 'get'
  })
}
