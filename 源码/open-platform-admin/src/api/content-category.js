import request from '@/api/index';

/**
 * 获取分类树
 */
export function getCategoryTree(params) {
  return request({
    url: '/admin/category/tree',
    method: 'get',
    params
  });
}

/**
 * 获取分类列表
 */
export function listCategories(params) {
  return request({
    url: '/admin/category/list',
    method: 'get',
    params
  });
}


/**
 * 新增分类
 */
export function addCategory(data) {
  return request({
    url: '/admin/category/add',
    method: 'post',
    data
  });
}

/**
 * 修改分类
 */
export function updateCategory(data) {
  return request({
    url: '/admin/category/update',
    method: 'post',
    data
  });
}

/**
 * 删除分类
 */
export function deleteCategory(id) {
  return request({
    url: `/admin/category/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 获取分类详情
 */
export function getCategoryDetail(id) {
  return request({
    url: `/admin/category/${id}`,
    method: 'get'
  });
}

/**
 * 更新分类内容
 */
export function updateCategoryContent(id, data) {
  return request({
    url: `/admin/category/${id}/content`,
    method: 'post',
    data
  });
}

/**
 * 发布分类内容
 */
export function publishCategory(id) {
  return request({
    url: `/admin/category/${id}/publish`,
    method: 'put'
  });
}

/**
 * 撤回分类内容
 */
export function withdrawCategory(id) {
  return request({
    url: `/admin/category/${id}/withdraw`,
    method: 'put'
  });
}

/**
 * 更新分类接口开放状态
 */
export function updateCategoryOpen(id, isOpen) {
  return request({
    url: `/admin/category/${id}/open/${isOpen}`,
    method: 'put'
  });
}

/**
 * 批量发布分类
 */
export function batchPublishCategories(ids) {
  return request({
    url: '/admin/category/batch/publish',
    method: 'put',
    data: ids
  });
}

/**
 * 批量撤回分类
 */
export function batchWithdrawCategories(ids) {
  return request({
    url: '/admin/category/batch/withdraw',
    method: 'put',
    data: ids
  });
}

/**
 * 上传图片到 MinIO
 * @param {File} file - 图片文件
 */
export function uploadImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: '/admin/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
