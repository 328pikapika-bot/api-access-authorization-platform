import axios from 'axios'
import request from './index'

const sandboxRequest = axios.create({
  baseURL: '/api/v1/service/open_platform',
  timeout: 30000,
  validateStatus: () => true
})

export function executeSandboxDebug(data, apiKey) {
  return sandboxRequest({
    url: '/sandbox/debug',
    method: 'post',
    data,
    headers: {
      'X-API-KEY': apiKey
    }
  }).then((response) => response.data)
}

export function getSandboxLogs() {
  return request({
    url: '/sandbox/logs',
    method: 'get'
  })
}
