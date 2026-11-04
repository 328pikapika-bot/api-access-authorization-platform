package com.nstl.openplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nstl.openplatform.entity.ContentApplication;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.Platform;
import com.nstl.openplatform.entity.PlatformColumn;
import com.nstl.openplatform.service.UserPermissionScopeService.PermissionScope;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPermissionScopeServiceTest {

    @Mock
    private ApplicationService applicationService;
    @Mock
    private ContentCategoryService contentCategoryService;
    @Mock
    private PlatformColumnService platformColumnService;
    @Mock
    private PlatformService platformService;

    private UserPermissionScopeService service;

    @BeforeEach
    void setUp() {
        service = new UserPermissionScopeService(
                applicationService,
                contentCategoryService,
                platformColumnService,
                platformService
        );
    }

    @Test
    void activeAssignmentProducesPublishedPermissionScope() {
        ContentApplication application = new ContentApplication();
        application.setCategoryIds("[10]");
        when(applicationService.list(any(Wrapper.class))).thenReturn(List.of(application));

        ContentCategory category = category(10L, 5L, 0L);
        when(contentCategoryService.listByIds(any(Collection.class))).thenReturn(List.of(category));

        PlatformColumn column = new PlatformColumn();
        column.setId(5L);
        column.setPlatformId(2L);
        column.setStatus(1);
        column.setIsPublished(1);
        when(platformColumnService.listByIds(any(Collection.class))).thenReturn(List.of(column));

        Platform platform = new Platform();
        platform.setId(2L);
        platform.setStatus(1);
        platform.setIsPublished(1);
        when(platformService.listByIds(any(Collection.class))).thenReturn(List.of(platform));

        PermissionScope scope = service.getPermissionScope(7L);

        assertEquals(Set.of(2L), scope.getPlatformIds());
        assertEquals(Set.of(5L), scope.getColumnIds());
        assertEquals(Set.of(10L), scope.getCategoryIds());
    }

    @Test
    void authorizedFirstLevelCategoryIncludesItsChildrenOnly() {
        ContentCategory allowedRoot = category(10L, 5L, 0L);
        allowedRoot.setChildren(Arrays.asList(
                category(11L, 5L, 10L),
                category(12L, 5L, 10L)
        ));
        ContentCategory deniedRoot = category(20L, 5L, 0L);

        List<ContentCategory> filtered = service.filterAuthorizedTree(
                Arrays.asList(allowedRoot, deniedRoot),
                Set.of(10L)
        );

        assertEquals(1, filtered.size());
        assertEquals(10L, filtered.get(0).getId());
        assertEquals(2, filtered.get(0).getChildren().size());
    }

    @Test
    void nestedLegacyPermissionKeepsAncestorsButRemovesUnauthorizedSiblings() {
        ContentCategory root = category(10L, 5L, 0L);
        root.setChildren(Arrays.asList(
                category(11L, 5L, 10L),
                category(12L, 5L, 10L)
        ));

        List<ContentCategory> filtered = service.filterAuthorizedTree(
                Collections.singletonList(root),
                Set.of(11L)
        );

        assertEquals(1, filtered.size());
        assertEquals(1, filtered.get(0).getChildren().size());
        assertEquals(11L, filtered.get(0).getChildren().get(0).getId());
        assertTrue(filtered.get(0).getChildren().get(0).getChildren() == null);
    }

    private ContentCategory category(Long id, Long columnId, Long parentId) {
        ContentCategory category = new ContentCategory();
        category.setId(id);
        category.setColumnId(columnId);
        category.setParentId(parentId);
        category.setStatus(1);
        category.setIsPublished(1);
        return category;
    }
}
