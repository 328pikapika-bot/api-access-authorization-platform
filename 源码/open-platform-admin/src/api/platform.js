import request from '@/api/index';

/**
 * 获取平台列表
 */
export function listPlatforms(params) {
  return request({
    url: '/admin/platform/list',
    method: 'get',
    params
  });
}

/**
 * 新增平台
 */
export function addPlatform(data) {
  return request({
    url: '/admin/platform/add',
    method: 'post',
    data
  });
}

/**
 * 修改平台
 */
export function updatePlatform(data) {
  return request({
    url: '/admin/platform/update',
    method: 'post',
    data
  });
}

/**
 * 删除平台
 */
export function deletePlatform(id) {
  return request({
    url: `/admin/platform/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 发布平台
 */
export function publishPlatform(id) {
  return request({
    url: `/admin/platform/${id}/publish`,
    method: 'put'
  });
}

/**
 * 撤回平台
 */
export function withdrawPlatform(id) {
  return request({
    url: `/admin/platform/${id}/withdraw`,
    method: 'put'
  });
}

/**
 * 批量发布平台
 */
export function batchPublishPlatforms(ids) {
  return request({
    url: '/admin/platform/batch/publish',
    method: 'put',
    data: ids
  });
}

/**
 * 批量撤回平台
 */
export function batchWithdrawPlatforms(ids) {
  return request({
    url: '/admin/platform/batch/withdraw',
    method: 'put',
    data: ids
  });
}
