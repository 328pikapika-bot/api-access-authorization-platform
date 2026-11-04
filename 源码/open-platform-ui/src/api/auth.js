import request from './index'

/**
 * 用code换取token和用户信息
 */
export function ssoCallback(code, redirectUri) {
  return request({
    url: '/auth/sso/callback',
    method: 'post',
    params: { code, redirect_uri: redirectUri }
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
