import axios from 'axios'

const sandboxRequest = axios.create({
  baseURL: '/api/v1/service/open_platform',
  timeout: 30000,
  validateStatus: () => true
})

export function executeAdminDebug(data) {
  const token = localStorage.getItem('admin_token')
  const headers = token ? { Authorization: `Bearer ${token}` } : {}

  return sandboxRequest({
    url: '/sandbox/admin/debug',
    method: 'post',
    data,
    headers
  }).then((response) => response.data)
}
