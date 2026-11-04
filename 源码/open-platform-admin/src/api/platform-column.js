import request from '@/api/index';

/**
 * 获取栏目列表
 */
export function listColumns(params) {
  return request({
    url: '/admin/platform-column/list',
    method: 'get',
    params
  });
}

/**
 * 新增栏目
 */
export function addColumn(data) {
  return request({
    url: '/admin/platform-column/add',
    method: 'post',
    data
  });
}

/**
 * 修改栏目
 */
export function updateColumn(data) {
  return request({
    url: '/admin/platform-column/update',
    method: 'post',
    data
  });
}

/**
 * 删除栏目
 */
export function deleteColumn(id) {
  return request({
    url: `/admin/platform-column/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 发布栏目
 */
export function publishColumn(id) {
  return request({
    url: `/admin/platform-column/${id}/publish`,
    method: 'put'
  });
}

/**
 * 撤回栏目
 */
export function withdrawColumn(id) {
  return request({
    url: `/admin/platform-column/${id}/withdraw`,
    method: 'put'
  });
}

/**
 * 批量发布栏目
 */
export function batchPublishColumns(ids) {
  return request({
    url: '/admin/platform-column/batch/publish',
    method: 'put',
    data: ids
  });
}

/**
 * 批量撤回栏目
 */
export function batchWithdrawColumns(ids) {
  return request({
    url: '/admin/platform-column/batch/withdraw',
    method: 'put',
    data: ids
  });
}
