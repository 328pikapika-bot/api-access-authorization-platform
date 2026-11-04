-- =============================================================================
-- 为账号 17334981104 创建完整的接口平台模拟数据
-- 包含：用户创建、应用权限分配、接口凭证分配、API 详细文档更新
-- 依赖：需先执行 discovery-realistic-test-seed.sql 创建平台和分类数据
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- Step 1: 确保用户存在
-- =============================================================================
INSERT INTO sys_user (id, password, nick_name, user_name, email, unit_name, user_role, status, create_time, update_time)
SELECT 17334981104, 'test_pwd', '测试用户', 'test_user_17334981104', 'test@nstl.gov.cn', '测试单位', 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 17334981104);

-- =============================================================================
-- Step 2: 查找科技文献开放检索平台及其分类
-- =============================================================================
-- 查找平台 ID
SET @platform_id = (SELECT id FROM platform WHERE name = '科技文献开放检索平台' LIMIT 1);

-- 查找开放API栏目下的所有 API 分类（type='api' 且 is_published=1）
-- 用于构建 categoryIds
SET @api_category_ids = (
    SELECT CONCAT('[', GROUP_CONCAT(id ORDER BY id), ']')
    FROM content_category
    WHERE column_id = (SELECT id FROM platform_column WHERE platform_id = @platform_id AND name = '开放API' LIMIT 1)
      AND type = 'api'
      AND is_published = 1
      AND status = 1
);

-- 查找开放API栏目 ID
SET @api_column_id = (SELECT id FROM platform_column WHERE platform_id = @platform_id AND name = '开放API' LIMIT 1);

-- 查找接入指南栏目 ID
SET @guide_column_id = (SELECT id FROM platform_column WHERE platform_id = @platform_id AND name = '接入指南' LIMIT 1);

-- =============================================================================
-- Step 3: 创建应用权限分配记录
-- =============================================================================
INSERT INTO content_application (
    user_id, platform_id, platform_name, column_id, column_name,
    category_ids, category_names, app_key, app_secret,
    reason, status, apply_time, audit_time,
    create_time, update_time, deleted
)
SELECT
    17334981104,
    @platform_id,
    '科技文献开放检索平台',
    @api_column_id,
    '开放API',
    @api_category_ids,
    (SELECT GROUP_CONCAT(name ORDER BY id) FROM content_category WHERE FIND_IN_SET(id, REPLACE(REPLACE(@api_category_ids, '[', ''), ']', ''))),
    CONCAT('AK', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16)),
    CONCAT('SK', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 30)),
    '后台管理员直接授权 - 测试数据',
    1,
    NOW(),
    NOW(),
    NOW(),
    NOW(),
    0
FROM DUAL
WHERE @platform_id IS NOT NULL
  AND @api_category_ids IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM content_application
      WHERE user_id = 17334981104 AND platform_id = @platform_id AND deleted = 0
  );

-- 获取刚创建的应用 ID
SET @application_id = (
    SELECT id FROM content_application
    WHERE user_id = 17334981104 AND platform_id = @platform_id AND deleted = 0
    ORDER BY id DESC LIMIT 1
);

-- =============================================================================
-- Step 4: 为每个已发布的 API 分类创建接口凭证
-- =============================================================================
INSERT INTO api_credential (application_id, category_id, open_id, client_id, token, secret_key, description, create_time, update_time, deleted)
SELECT
    @application_id,
    cc.id,
    CONCAT('OPEN_', cc.id, '_', 17334981104),
    CONCAT('CLIENT_', cc.id, '_demo'),
    CONCAT('TK_', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 24)),
    CONCAT('SEC_', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 32)),
    CONCAT(cc.name, ' - 模拟凭证（测试环境）'),
    NOW(),
    NOW(),
    0
FROM content_category cc
WHERE cc.column_id = @api_column_id
  AND cc.type = 'api'
  AND cc.is_published = 1
  AND cc.status = 1
  AND @application_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM api_credential ac
      WHERE ac.application_id = @application_id AND ac.category_id = cc.id AND ac.deleted = 0
  );

-- =============================================================================
-- Step 5: 更新 API 分类节点的详细接口文档（docContent）
-- 让前端显示完整的接口说明和步骤向导
-- =============================================================================

-- 5.1 开放检索与分析服务（根节点）
UPDATE content_category
SET doc_content = CONCAT(
    '# 开放检索与分析服务\n\n',
    '该服务树覆盖文献检索、机构分析与资源推荐场景，提供以下接口能力：\n\n',
    '| 服务分组 | 接口数量 | 说明 |\n',
    '|---------|---------|------|\n',
    '| 文献检索服务 | 4 个接口 | 统一检索、统计、校验、批量导出 |\n',
    '| 机构分析服务 | 2 个接口 | 机构概览、热点主题分析 |\n',
    '| 资源推荐服务 | 1 个接口 | 个性化资源推荐 |\n\n',
    '## 联调地址\n',
    '- **Mockoon 服务地址**：`http://localhost:17302`\n',
    '- **调试方式**：后台接口配置测试 / 前台沙箱调试\n\n',
    '## 使用步骤\n',
    '1. **查看接口说明**：了解接口功能、请求参数和响应格式\n',
    '2. **沙箱调试**：在线调用接口，查看实际返回结果\n',
    '3. **开发测试**：复制示例代码，集成到您的应用中\n',
    '4. **获取授权信息**：查看您的 AppKey、Token 等凭证信息\n'
)
WHERE column_id = @api_column_id
  AND type = 'content'
  AND name = '开放检索与分析服务';

-- 5.2 文献检索服务（分组节点）
UPDATE content_category
SET doc_content = CONCAT(
    '## 文献检索服务\n\n',
    '提供科技文献的统一检索、统计分析、检索式校验与批量导出等核心能力。\n\n',
    '| 接口名称 | 请求方式 | 接口路径 | 状态 |\n',
    '|---------|---------|---------|------|\n',
    '| 统一检索接口 | GET | `/api/v1/literature/search` | 已发布 |\n',
    '| 检索统计接口 | GET | `/api/v1/literature/stats` | 已发布 |\n',
    '| 检索式校验接口 | POST | `/api/v1/literature/query/validate` | 灰度 |\n',
    '| 批量导出任务接口 | POST | `/api/v1/literature/export` | 灰度 |\n\n',
    '### 通用请求头\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| X-Client-Id | String | 是 | 客户端标识，测试环境使用 `demo-open-platform` |\n',
    '| X-Trace-Source | String | 否 | 链路追踪来源标识 |\n',
    '| Authorization | String | 否 | Bearer Token 认证 |\n'
)
WHERE column_id = @api_column_id
  AND type = 'content'
  AND name = '文献检索服务';

-- 5.3 统一检索接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 统一检索接口\n\n',
    '## 接口概述\n',
    '支持期刊、学位论文和专利等多类型科技文献资源的统一检索，返回分页结果列表。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`GET`\n',
    '- **接口路径**：`/api/v1/literature/search`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 默认值 | 说明 |\n',
    '|-------|------|------|--------|------|\n',
    '| keyword | String | 是 | - | 检索关键词，支持中英文 |\n',
    '| pageNum | Integer | 否 | 1 | 页码，从 1 开始 |\n',
    '| pageSize | Integer | 否 | 10 | 每页条数，最大 50 |\n',
    '| sort | String | 否 | relevance | 排序方式：`relevance`（相关度）、`date`（日期）、`cited`（被引） |\n',
    '| source | String | 否 | - | 文献来源过滤，逗号分隔：`journal`（期刊）、`thesis`（学位）、`patent`（专利） |\n\n',
    '## 响应参数\n',
    '| 参数名 | 类型 | 说明 |\n',
    '|-------|------|------|\n',
    '| code | Integer | 状态码，200 表示成功 |\n',
    '| message | String | 响应消息 |\n',
    '| data.total | Integer | 总记录数 |\n',
    '| data.pageNum | Integer | 当前页码 |\n',
    '| data.pageSize | Integer | 每页条数 |\n',
    '| data.list | Array | 文献列表 |\n',
    '| data.list[].id | String | 文献唯一标识 |\n',
    '| data.list[].title | String | 文献标题 |\n',
    '| data.list[].authors | Array | 作者列表 |\n',
    '| data.list[].source | String | 文献来源 |\n',
    '| data.list[].publishDate | String | 出版日期 |\n',
    '| data.list[].abstract | String | 摘要 |\n',
    '| data.list[].keywords | Array | 关键词列表 |\n\n',
    '## 请求示例\n',
    '```bash\n',
    'curl -X GET "http://localhost:17302/api/v1/literature/search?keyword=新能源电池&pageNum=1&pageSize=10&sort=relevance&source=journal,thesis,patent" \\\n',
    '  -H "X-Client-Id: demo-open-platform" \\\n',
    '  -H "X-Trace-Source: admin-test"\n',
    '```\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "message": "success",\n',
    '  "data": {\n',
    '    "total": 1286,\n',
    '    "pageNum": 1,\n',
    '    "pageSize": 10,\n',
    '    "list": [\n',
    '      {\n',
    '        "id": "ART_2024001",\n',
    '        "title": "固态锂电池界面工程研究进展",\n',
    '        "authors": ["张三", "李四"],\n',
    '        "source": "journal",\n',
    '        "publishDate": "2024-03-15",\n',
    '        "abstract": "本文综述了固态锂电池界面工程的最新研究进展...",\n',
    '        "keywords": ["固态电池", "界面工程", "锂电池"]\n',
    '      }\n',
    '    ]\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '统一检索接口';

-- 5.4 检索统计接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 检索统计接口\n\n',
    '## 接口概述\n',
    '提供检索结果的年度分布、资源类型占比与热点主题等统计信息，适合数据概览和图表联调。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`GET`\n',
    '- **接口路径**：`/api/v1/literature/stats`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 默认值 | 说明 |\n',
    '|-------|------|------|--------|------|\n',
    '| keyword | String | 是 | - | 检索关键词 |\n',
    '| dimension | String | 否 | year,type | 统计维度，逗号分隔：`year`（年度）、`type`（资源类型）、`topic`（主题） |\n\n',
    '## 响应参数\n',
    '| 参数名 | 类型 | 说明 |\n',
    '|-------|------|------|\n',
    '| code | Integer | 状态码 |\n',
    '| data.yearDistribution | Array | 年度分布数据 |\n',
    '| data.typeDistribution | Array | 资源类型分布 |\n',
    '| data.hotTopics | Array | 热点主题列表 |\n\n',
    '## 请求示例\n',
    '```bash\n',
    'curl -X GET "http://localhost:17302/api/v1/literature/stats?keyword=新能源电池&dimension=year,type,topic" \\\n',
    '  -H "X-Client-Id: demo-open-platform"\n',
    '```\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "data": {\n',
    '    "yearDistribution": [\n',
    '      {"year": 2020, "count": 156},\n',
    '      {"year": 2021, "count": 203},\n',
    '      {"year": 2022, "count": 278},\n',
    '      {"year": 2023, "count": 342},\n',
    '      {"year": 2024, "count": 307}\n',
    '    ],\n',
    '    "typeDistribution": [\n',
    '      {"type": "期刊论文", "count": 856, "percentage": 66.6},\n',
    '      {"type": "学位论文", "count": 245, "percentage": 19.1},\n',
    '      {"type": "专利", "count": 185, "percentage": 14.4}\n',
    '    ],\n',
    '    "hotTopics": [\n',
    '      {"topic": "固态电解质", "count": 89},\n',
    '      {"topic": "锂硫电池", "count": 67},\n',
    '      {"topic": "钠离子电池", "count": 54}\n',
    '    ]\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '检索统计接口';

-- 5.5 检索式校验接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 检索式校验接口\n\n',
    '## 接口概述\n',
    '提交复杂的检索表达式进行语法校验，返回校验结果和优化建议。支持 POST JSON 请求体。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`POST`\n',
    '- **接口路径**：`/api/v1/literature/query/validate`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| query | String | 是 | 检索表达式，支持布尔运算符 AND/OR/NOT |\n',
    '| fields | Array | 否 | 限定检索字段：`title`、`abstract`、`keywords`、`all` |\n',
    '| lang | String | 否 | 语种：`zh`（中文）、`en`（英文），默认 `zh` |\n\n',
    '## 请求体示例\n',
    '```json\n',
    '{\n',
    '  "query": "(新能源 OR 可再生能源) AND 电池 NOT 铅酸",\n',
    '  "fields": ["title", "abstract", "keywords"],\n',
    '  "lang": "zh"\n',
    '}\n',
    '```\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "data": {\n',
    '    "valid": true,\n',
    '    "normalizedQuery": "(新能源 OR 可再生能源) AND 电池 NOT 铅酸",\n',
    '    "estimatedResults": 1250,\n',
    '    "suggestions": [\n',
    '      "建议将"新能源"扩展为同义词：新能源、清洁能源、替代能源"\n',
    '    ],\n',
    '    "parsedTree": {\n',
    '      "operator": "AND",\n',
    '      "left": {"operator": "OR", "terms": ["新能源", "可再生能源"]},\n',
    '      "right": {"operator": "NOT", "include": "电池", "exclude": "铅酸"}\n',
    '    }\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '检索式校验接口';

-- 5.6 批量导出任务接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 批量导出任务接口\n\n',
    '## 接口概述\n',
    '提交批量导出任务，支持导出检索结果为 Excel 或 CSV 格式，异步处理并返回任务 ID 用于查询进度。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`POST`\n',
    '- **接口路径**：`/api/v1/literature/export`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| query | String | 是 | 检索条件表达式 |\n',
    '| format | String | 否 | 导出格式：`excel`（默认）、`csv` |\n',
    '| maxRecords | Integer | 否 | 最大导出条数，默认 5000，最大 10000 |\n',
    '| fields | Array | 否 | 导出字段列表：`title`、`authors`、`source`、`date`、`abstract` |\n',
    '| callback | String | 否 | 导出完成后的回调 URL |\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 202,\n',
    '  "message": "导出任务已受理",\n',
    '  "data": {\n',
    '    "taskId": "EXP_20240623_001",\n',
    '    "status": "processing",\n',
    '    "estimatedTime": 120,\n',
    '    "queryUrl": "/api/v1/literature/export/status?taskId=EXP_20240623_001"\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '批量导出任务接口';

-- 5.7 机构分析服务（分组节点）
UPDATE content_category
SET doc_content = CONCAT(
    '## 机构分析服务\n\n',
    '提供科研机构的综合画像分析与热点研究主题识别能力。\n\n',
    '| 接口名称 | 请求方式 | 接口路径 | 状态 |\n',
    '|---------|---------|---------|------|\n',
    '| 机构概览接口 | GET | `/api/v1/organization/profile` | 已发布 |\n',
    '| 机构热点主题接口 | GET | `/api/v1/organization/topics` | 已发布 |\n'
)
WHERE column_id = @api_column_id
  AND type = 'content'
  AND name = '机构分析服务';

-- 5.8 机构概览接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 机构概览接口\n\n',
    '## 接口概述\n',
    '根据机构编码查询机构的基本信息、科研产出统计和研究方向概览。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`GET`\n',
    '- **接口路径**：`/api/v1/organization/profile`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| orgCode | String | 是 | 机构编码，如 `NSTL001`；使用 `NSTL404` 可复现 404 场景 |\n\n',
    '## 响应参数\n',
    '| 参数名 | 类型 | 说明 |\n',
    '|-------|------|------|\n',
    '| data.orgCode | String | 机构编码 |\n',
    '| data.orgName | String | 机构全称 |\n',
    '| data.orgType | String | 机构类型 |\n',
    '| data.totalPublications | Integer | 总发表量 |\n',
    '| data.researchFields | Array | 研究方向列表 |\n',
    '| data.topAuthors | Array | 高产作者 Top10 |\n\n',
    '## 请求示例\n',
    '```bash\n',
    'curl -X GET "http://localhost:17302/api/v1/organization/profile?orgCode=NSTL001" \\\n',
    '  -H "X-Client-Id: demo-open-platform"\n',
    '```\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "data": {\n',
    '    "orgCode": "NSTL001",\n',
    '    "orgName": "国家科技图书文献中心",\n',
    '    "orgType": "研究机构",\n',
    '    "totalPublications": 15230,\n',
    '    "researchFields": [\n',
    '      {"field": "信息科学", "count": 4520},\n',
    '      {"field": "图书馆学", "count": 3210},\n',
    '      {"field": "数据挖掘", "count": 2180}\n',
    '    ],\n',
    '    "topAuthors": [\n',
    '      {"name": "王研究", "publications": 156},\n',
    '      {"name": "李教授", "publications": 132}\n',
    '    ]\n',
    '  }\n',
    '}\n',
    '```\n\n',
    '### 404 场景\n',
    '传入 `orgCode=NSTL404` 可模拟机构不存在的错误响应：\n',
    '```json\n',
    '{\n',
    '  "code": 404,\n',
    '  "message": "机构不存在",\n',
    '  "data": null\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '机构概览接口';

-- 5.9 机构热点主题接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 机构热点主题接口\n\n',
    '## 接口概述\n',
    '获取指定机构当前研究热点主题，按热度排序，支持时间范围筛选。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`GET`\n',
    '- **接口路径**：`/api/v1/organization/topics`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| orgCode | String | 是 | 机构编码 |\n',
    '| year | Integer | 否 | 统计年份，默认最近一年 |\n',
    '| topN | Integer | 否 | 返回 Top N 主题，默认 10 |\n\n',
    '## 响应参数\n',
    '| 参数名 | 类型 | 说明 |\n',
    '|-------|------|------|\n',
    '| data.topics | Array | 热点主题列表 |\n',
    '| data.topics[].rank | Integer | 排名 |\n',
    '| data.topics[].topic | String | 主题名称 |\n',
    '| data.topics[].heat | Integer | 热度指数 |\n',
    '| data.topics[].trend | String | 趋势：`rising`、`stable`、`declining` |\n\n',
    '## 请求示例\n',
    '```bash\n',
    'curl -X GET "http://localhost:17302/api/v1/organization/topics?orgCode=NSTL001&year=2024&topN=5" \\\n',
    '  -H "X-Client-Id: demo-open-platform"\n',
    '```\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "data": {\n',
    '    "orgCode": "NSTL001",\n',
    '    "year": 2024,\n',
    '    "topics": [\n',
    '      {"rank": 1, "topic": "大语言模型在文献检索中的应用", "heat": 98, "trend": "rising"},\n',
    '      {"rank": 2, "topic": "知识图谱构建方法", "heat": 92, "trend": "stable"},\n',
    '      {"rank": 3, "topic": "开放获取政策研究", "heat": 87, "trend": "rising"}\n',
    '    ]\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '机构热点主题接口';

-- 5.10 资源推荐服务（分组节点）
UPDATE content_category
SET doc_content = CONCAT(
    '## 资源推荐服务\n\n',
    '基于用户行为和偏好，提供个性化科技文献资源推荐。\n\n',
    '| 接口名称 | 请求方式 | 接口路径 | 状态 |\n',
    '|---------|---------|---------|------|\n',
    '| 资源推荐接口 | GET | `/api/v1/resource/recommend` | 灰度 |\n'
)
WHERE column_id = @api_column_id
  AND type = 'content'
  AND name = '资源推荐服务';

-- 5.11 资源推荐接口
UPDATE content_category
SET doc_content = CONCAT(
    '# 资源推荐接口\n\n',
    '## 接口概述\n',
    '根据用户兴趣标签和行为历史，推荐相关科技文献资源，支持分页和多样性控制。\n\n',
    '## 请求信息\n',
    '- **请求方式**：`GET`\n',
    '- **接口路径**：`/api/v1/resource/recommend`\n',
    '- **Content-Type**：`application/json`\n\n',
    '## 请求参数\n',
    '| 参数名 | 类型 | 必填 | 说明 |\n',
    '|-------|------|------|------|\n',
    '| userId | String | 否 | 用户标识，不传则返回热门推荐 |\n',
    '| tags | String | 否 | 兴趣标签，逗号分隔 |\n',
    '| pageSize | Integer | 否 | 推荐数量，默认 10 |\n',
    '| diversity | Float | 否 | 多样性因子（0-1），值越大推荐越多样，默认 0.3 |\n\n',
    '## 响应示例\n',
    '```json\n',
    '{\n',
    '  "code": 200,\n',
    '  "data": {\n',
    '    "recommendations": [\n',
    '      {\n',
    '        "id": "REC_001",\n',
    '        "title": "深度学习在自然语言处理中的应用综述",\n',
    '        "score": 0.956,\n',
    '        "reason": "基于您对"机器学习"的关注"\n',
    '      }\n',
    '    ]\n',
    '  }\n',
    '}\n',
    '```\n'
)
WHERE column_id = @api_column_id
  AND type = 'api'
  AND name = '资源推荐接口';

-- =============================================================================
-- Step 6: 为接入指南的分类也分配权限（使其可见）
-- =============================================================================
-- 为接入指南栏目也创建一个应用记录
SET @guide_category_ids = (
    SELECT CONCAT('[', GROUP_CONCAT(id ORDER BY id), ']')
    FROM content_category
    WHERE column_id = @guide_column_id
      AND is_published = 1
      AND status = 1
);

INSERT INTO content_application (
    user_id, platform_id, platform_name, column_id, column_name,
    category_ids, category_names, app_key, app_secret,
    reason, status, apply_time, audit_time,
    create_time, update_time, deleted
)
SELECT
    17334981104,
    @platform_id,
    '科技文献开放检索平台',
    @guide_column_id,
    '接入指南',
    @guide_category_ids,
    (SELECT GROUP_CONCAT(name ORDER BY id) FROM content_category WHERE FIND_IN_SET(id, REPLACE(REPLACE(@guide_category_ids, '[', ''), ']', ''))),
    CONCAT('AK', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16)),
    CONCAT('SK', SUBSTRING(REPLACE(UUID(), '-', ''), 1, 30)),
    '后台管理员直接授权 - 测试数据（接入指南）',
    1,
    NOW(),
    NOW(),
    NOW(),
    NOW(),
    0
FROM DUAL
WHERE @platform_id IS NOT NULL
  AND @guide_category_ids IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM content_application
      WHERE user_id = 17334981104 AND platform_id = @platform_id AND column_id = @guide_column_id AND deleted = 0
  );

-- =============================================================================
-- 完成输出
-- =============================================================================
SELECT CONCAT(
    '=== 种子数据创建完成 ===\n',
    '用户 ID: ', 17334981104, '\n',
    '平台: 科技文献开放检索平台 (ID=', @platform_id, ')\n',
    'API 分类 ID 列表: ', @api_category_ids, '\n',
    '接入指南分类 ID 列表: ', @guide_category_ids, '\n',
    '应用记录 ID (开放API): ', @application_id, '\n',
    '\n请刷新前端页面查看效果。'
) AS summary;
