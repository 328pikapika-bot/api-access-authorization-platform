package com.nstl.openplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nstl.openplatform.common.Result;
import com.nstl.openplatform.entity.AuditLog;
import com.nstl.openplatform.entity.ContentCategory;
import com.nstl.openplatform.entity.SandboxLog;
import com.nstl.openplatform.mapper.AuditLogMapper;
import com.nstl.openplatform.service.ContentCategoryService;
import com.nstl.openplatform.service.SandboxLogService;
import com.nstl.openplatform.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计控制器
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ContentCategoryService contentCategoryService;
    private final SysUserService sysUserService;
    private final SandboxLogService sandboxLogService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long publishedCount = contentCategoryService.count(
                new LambdaQueryWrapper<ContentCategory>().eq(ContentCategory::getIsPublished, 1)
        );
        stats.put("publishedCount", publishedCount);

        long userCount = sysUserService.count();
        stats.put("userCount", userCount);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayVisitCount = auditLogMapper.selectCount(
                new LambdaQueryWrapper<AuditLog>().ge(AuditLog::getCreateTime, startOfDay)
        );
        stats.put("todayVisitCount", todayVisitCount);

        List<SandboxLog> recentSandboxLogs = sandboxLogService.query()
                .orderByDesc("create_time")
                .orderByDesc("id")
                .last("limit 10")
                .list();
        stats.put("recentSandboxLogs", recentSandboxLogs);

        stats.put("visitTrend", buildAuditVisitTrend());
        stats.put("visitDistribution", buildAuditModuleDistribution());

        return Result.success(stats);
    }

    private Map<String, Object> buildAuditVisitTrend() {
        List<String> last7Days = new ArrayList<>();
        List<Long> visitCounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            last7Days.add(date.format(formatter));

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            long count = auditLogMapper.selectCount(
                    new LambdaQueryWrapper<AuditLog>()
                            .ge(AuditLog::getCreateTime, start)
                            .le(AuditLog::getCreateTime, end)
            );
            visitCounts.add(count);
        }

        return Map.of("dates", last7Days, "counts", visitCounts);
    }

    private List<Map<String, Object>> buildAuditModuleDistribution() {
        List<Map<String, Object>> moduleRows = auditLogMapper.selectMaps(
                new QueryWrapper<AuditLog>()
                        .select("COALESCE(NULLIF(module_name, ''), '未分类模块') AS name", "COUNT(*) AS value")
                        .groupBy("COALESCE(NULLIF(module_name, ''), '未分类模块')")
        );

        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Map<String, Object> row : moduleRows) {
            String name = String.valueOf(row.getOrDefault("name", "未分类模块"));
            Object rawValue = row.get("value");
            long value = rawValue instanceof Number
                    ? ((Number) rawValue).longValue()
                    : Long.parseLong(String.valueOf(rawValue));
            pieData.add(Map.of("name", name, "value", value));
        }

        pieData.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        if (pieData.size() <= 5) {
            return pieData;
        }

        List<Map<String, Object>> top5 = new ArrayList<>(pieData.subList(0, 5));
        long othersCount = pieData.subList(5, pieData.size()).stream()
                .mapToLong(m -> (Long) m.get("value"))
                .sum();
        top5.add(Map.of("name", "其他", "value", othersCount));
        return top5;
    }
}
