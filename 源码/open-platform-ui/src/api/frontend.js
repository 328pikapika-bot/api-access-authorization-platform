import request from '@/api/index';

/**
 * 获取已发布的平台列表（前端专用）
 */
export function getPublishedPlatforms() {
  return request({
    url: '/api/frontend/platforms',
    method: 'get'
  });
}

/**
 * 获取指定平台下已发布的栏目列表（前端专用）
 */
export function getPublishedColumns(platformId) {
  return request({
    url: `/api/frontend/platforms/${platformId}/columns`,
    method: 'get'
  });
}

/**
 * 获取指定栏目下已发布的分类列表（前端专用）
 */
export function getPublishedCategories(columnId) {
  return request({
    url: `/api/frontend/columns/${columnId}/categories`,
    method: 'get'
  });
}

/**
 * 获取已发布分类的详细内容（前端专用）
 */
export function getPublishedCategoryContent(id) {
  return request({
    url: `/api/frontend/categories/${id}/content`,
    method: 'get'
  });
}
