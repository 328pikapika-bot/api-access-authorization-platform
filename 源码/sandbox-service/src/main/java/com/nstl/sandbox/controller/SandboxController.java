package com.nstl.sandbox.controller;

import com.nstl.sandbox.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 沙箱测试控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping
public class SandboxController {

    @PostMapping("/debug")
    public Result<Object> debug(@RequestBody Map<String, Object> request) {
        String contentPath = (String) request.get("contentPath");
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");

        log.info("内容访问调试请求 - 路径: {}, 方法: {}, 参数: {}", contentPath, method, params);

        // 模拟响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        
        // 根据不同的内容路径返回不同的模拟数据
        if (contentPath != null) {
            if (contentPath.contains("search")) {
                // 文献检索接口模拟数据
                Map<String, Object> data = new HashMap<>();
                data.put("total", 100);
                data.put("pageIndex", 1);
                data.put("pageSize", 10);
                data.put("list", getMockLiteratureList());
                response.put("data", data);
            } else if (contentPath.contains("fulltext")) {
                // 全文获取接口模拟数据
                response.put("data", Map.of(
                    "docId", params != null ? params.get("docId") : "",
                    "fileName", "文献全文.pdf",
                    "downloadUrl", "https://example.com/download/12345.pdf"
                ));
            } else if (contentPath.contains("metadata")) {
                // 元数据服务接口模拟数据
                response.put("data", getMockMetadataList());
            } else {
                response.put("data", Map.of("message", "内容模拟响应"));
            }
        }

        return Result.success(response);
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("沙箱服务运行正常");
    }

    /**
     * 获取模拟文献列表
     */
    private Map<String, Object>[] getMockLiteratureList() {
        return new Map[]{
            Map.of(
                "docId", "1001",
                "title", "基于深度学习的图像识别技术研究",
                "author", "张三,李四",
                "publishDate", "2023-01-15",
                "abstract", "本文提出了一种基于深度学习的图像识别方法..."
            ),
            Map.of(
                "docId", "1002",
                "title", "自然语言处理在智能客服中的应用",
                "author", "王五,赵六",
                "publishDate", "2023-02-20",
                "abstract", "研究了自然语言处理技术在智能客服系统中的应用..."
            ),
            Map.of(
                "docId", "1003",
                "title", "云计算环境下的数据安全保护机制",
                "author", "孙七",
                "publishDate", "2023-03-10",
                "abstract", "针对云计算环境下的数据安全问题,提出了一套完整的保护机制..."
            )
        };
    }

    /**
     * 获取模拟元数据列表
     */
    private Map<String, Object>[] getMockMetadataList() {
        return new Map[]{
            Map.of(
                "docId", "1001",
                "title", "基于深度学习的图像识别技术研究",
                "author", new String[]{"张三", "李四"},
                "keywords", new String[]{"深度学习", "图像识别", "神经网络"},
                "doi", "10.1234/example.12345",
                "source", "计算机学报",
                "year", "2023",
                "volume", "45",
                "issue", "2",
                "pages", "123-135"
            ),
            Map.of(
                "docId", "1002",
                "title", "自然语言处理在智能客服中的应用",
                "author", new String[]{"王五", "赵六"},
                "keywords", new String[]{"自然语言处理", "智能客服", "对话系统"},
                "doi", "10.1234/example.67890",
                "source", "软件学报",
                "year", "2023",
                "volume", "34",
                "issue", "3",
                "pages", "234-246"
            )
        };
    }
}
