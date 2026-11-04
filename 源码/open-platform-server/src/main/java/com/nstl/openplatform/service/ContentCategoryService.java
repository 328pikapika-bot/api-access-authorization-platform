package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nstl.openplatform.entity.ContentCategory;

import java.util.List;

public interface ContentCategoryService extends IService<ContentCategory> {
    /**
     * 获取分类树
     * @param columnId 栏目ID
     * @param publishedOnly 是否只查询已发布的分类
     */
    List<ContentCategory> getCategoryTree(Long columnId, Boolean publishedOnly);

}
