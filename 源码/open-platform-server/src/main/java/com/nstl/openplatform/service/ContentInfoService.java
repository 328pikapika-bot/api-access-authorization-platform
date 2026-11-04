package com.nstl.openplatform.service;

import com.nstl.openplatform.entity.ContentCategory;

import java.util.List;

/**
 * 内容信息服务（已从content_category表读取）
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface ContentInfoService {

    /**
     * 获取已发布的内容列表
     *
     * @param category 内容分类名称
     * @return 内容列表
     */
    List<ContentCategory> getPublishedContents(Long categoryId, String category);

    /**
     * 根据ID获取内容详情
     *
     * @param id 分类ID
     * @return 内容详情
     */
    ContentCategory getContentDetail(Long id);

    /**
     * 发布内容
     *
     * @param id 分类ID
     * @return 是否成功
     */
    boolean publishContent(Long id);

    /**
     * 撤回内容
     *
     * @param id 分类ID
     * @return 是否成功
     */
    boolean withdrawContent(Long id);
}
