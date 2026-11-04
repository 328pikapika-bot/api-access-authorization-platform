package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.mapper.ContentCategoryMapper;
import com.nstl.openplatform.service.ContentInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentInfoServiceImpl implements ContentInfoService {

    private final ContentCategoryMapper contentCategoryMapper;

    @Override
    public List<ContentCategory> getPublishedContents(Long categoryId, String category) {
        LambdaQueryWrapper<ContentCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentCategory::getIsPublished, 1)
                .eq(ContentCategory::getStatus, 1)
                .orderByDesc(ContentCategory::getUpdateTime);

        if (categoryId != null) {
            wrapper.eq(ContentCategory::getId, categoryId);
        } else if (StringUtils.hasText(category)) {
            wrapper.eq(ContentCategory::getName, category.trim());
        }

        List<ContentCategory> contents = contentCategoryMapper.selectList(wrapper);
        log.info("Query published contents, categoryId={}, category={}, count={}",
                categoryId, category, contents.size());
        return contents;
    }

    @Override
    public ContentCategory getContentDetail(Long id) {
        ContentCategory category = contentCategoryMapper.selectById(id);
        if (category == null) {
            return null;
        }
        if (category.getIsPublished() == null || category.getIsPublished() != 1) {
            return null;
        }
        return category;
    }

    @Override
    public boolean publishContent(Long id) {
        ContentCategory category = contentCategoryMapper.selectById(id);
        if (category == null) {
            return false;
        }
        category.setIsPublished(1);
        return contentCategoryMapper.updateById(category) > 0;
    }

    @Override
    public boolean withdrawContent(Long id) {
        ContentCategory category = contentCategoryMapper.selectById(id);
        if (category == null) {
            return false;
        }
        category.setIsPublished(2);
        return contentCategoryMapper.updateById(category) > 0;
    }
}
