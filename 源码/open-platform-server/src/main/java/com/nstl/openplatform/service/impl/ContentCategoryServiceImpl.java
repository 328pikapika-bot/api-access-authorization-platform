package com.nstl.openplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.mapper.ContentCategoryMapper;
import com.nstl.openplatform.service.ContentCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentCategoryServiceImpl extends ServiceImpl<ContentCategoryMapper, ContentCategory> implements ContentCategoryService {

    @Override
    public List<ContentCategory> getCategoryTree(Long columnId, Boolean publishedOnly) {
        var query = this.query().eq(columnId != null, "column_id", columnId);
        if (Boolean.TRUE.equals(publishedOnly)) {
            query.eq("is_published", 1);
        }

        List<ContentCategory> allCategories = query.list();
        if (allCategories == null || allCategories.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<ContentCategory>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(ContentCategory::getParentId));

        return allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .peek(c -> c.setChildren(getChildren(c, parentMap)))
                .sorted(Comparator.comparingInt(this::sortValue))
                .collect(Collectors.toList());
    }

    private List<ContentCategory> getChildren(ContentCategory root, Map<Long, List<ContentCategory>> parentMap) {
        List<ContentCategory> children = parentMap.get(root.getId());
        if (children == null || children.isEmpty()) {
            return null;
        }
        return children.stream()
                .peek(c -> c.setChildren(getChildren(c, parentMap)))
                .sorted(Comparator.comparingInt(this::sortValue))
                .collect(Collectors.toList());
    }

    private int sortValue(ContentCategory category) {
        return category.getSort() == null ? 0 : category.getSort();
    }
}
