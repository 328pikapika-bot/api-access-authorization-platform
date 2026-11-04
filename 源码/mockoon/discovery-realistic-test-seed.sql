SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_test_categories;
CREATE TEMPORARY TABLE tmp_test_categories (
  id BIGINT PRIMARY KEY
);

INSERT INTO tmp_test_categories (id)
WITH RECURSIVE category_tree AS (
  SELECT id
  FROM content_category
  WHERE name LIKE '[TEST-%'
  UNION ALL
  SELECT c.id
  FROM content_category c
  INNER JOIN category_tree t ON c.parent_id = t.id
)
SELECT DISTINCT id FROM category_tree;

DELETE FROM interface_config
WHERE category_id IN (SELECT id FROM tmp_test_categories);

DELETE FROM content_category
WHERE id IN (SELECT id FROM tmp_test_categories);

DROP TEMPORARY TABLE IF EXISTS tmp_test_categories;

DROP TEMPORARY TABLE IF EXISTS tmp_platform_columns;
CREATE TEMPORARY TABLE tmp_platform_columns (
  id BIGINT PRIMARY KEY
);

INSERT INTO tmp_platform_columns (id)
SELECT id
FROM platform_column
WHERE platform_id IN (
  SELECT id FROM platform WHERE name IN ('[TEST-20260623] Open Search Platform', '科技文献开放检索平台')
);

DROP TEMPORARY TABLE IF EXISTS tmp_platform_categories;
CREATE TEMPORARY TABLE tmp_platform_categories (
  id BIGINT PRIMARY KEY
);

INSERT INTO tmp_platform_categories (id)
SELECT id
FROM content_category
WHERE column_id IN (SELECT id FROM tmp_platform_columns);

DELETE FROM interface_config
WHERE category_id IN (SELECT id FROM tmp_platform_categories);

DELETE FROM content_category
WHERE id IN (SELECT id FROM tmp_platform_categories);

DELETE FROM platform_column
WHERE id IN (SELECT id FROM tmp_platform_columns);

DELETE FROM platform
WHERE name IN ('[TEST-20260623] Open Search Platform', '科技文献开放检索平台', '[TEST-20260622]描述');

DROP TEMPORARY TABLE IF EXISTS tmp_platform_categories;
DROP TEMPORARY TABLE IF EXISTS tmp_platform_columns;

DELETE FROM mockoon_service
WHERE name IN ('[TEST-20260623] Discovery Realistic Mock', '[TEST-20260622] ??404??', '科技文献开放检索模拟服务');

INSERT INTO mockoon_service (
  name,
  service_url,
  description,
  status,
  sort
) VALUES (
  '科技文献开放检索模拟服务',
  'http://localhost:17302',
  '导入 mockoon/discovery-realistic-test.json 后，在 Mockoon 中启动此环境即可联调。',
  1,
  920
);

SET @mockoon_service_id = LAST_INSERT_ID();

INSERT INTO platform (
  name,
  description,
  sort,
  status,
  is_published
) VALUES (
  '科技文献开放检索平台',
  '独立的真实模拟平台，覆盖文献检索、机构分析与资源推荐等接口能力。',
  920,
  1,
  1
);

SET @platform_id = LAST_INSERT_ID();

INSERT INTO platform_column (
  platform_id,
  name,
  description,
  sort,
  status,
  is_published
) VALUES (
  @platform_id,
  '接入指南',
  '平台接入说明、调试步骤、鉴权要求与排障说明。',
  1,
  1,
  1
);

SET @guide_column_id = LAST_INSERT_ID();

INSERT INTO platform_column (
  platform_id,
  name,
  description,
  sort,
  status,
  is_published
) VALUES (
  @platform_id,
  '开放API',
  '面向文献检索、机构分析与资源推荐场景的真实模拟接口。',
  2,
  1,
  1
);

SET @api_column_id = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '科技文献开放检索接入指南',
  @guide_column_id,
  0,
  1,
  1,
  CONCAT(
    '# 科技文献开放检索接入指南\n\n',
    '本指南用于演示科技文献开放检索平台的接入方式、联调步骤与鉴权要求。\n\n',
    '## 联调地址\n',
    '- Mockoon 服务地址：http://localhost:17302\n',
    '- 调试方式：后台接口配置测试、前台沙箱调试\n\n',
    '## 支持场景\n',
    '1. 文献统一检索与统计分析\n',
    '2. 检索式校验与导出任务\n',
    '3. 机构画像与热点主题分析\n',
    '4. 资源推荐与专题卡片展示\n'
  ),
  1,
  0,
  '科技文献开放检索接入指南',
  'content',
  0
);

SET @guide_root = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '接入概览',
  @guide_column_id,
  @guide_root,
  1,
  1,
  CONCAT(
    '## 接入概览\n\n',
    '- 建议先联调“统一检索接口”，确认 Mockoon 服务与路径配置正常。\n',
    '- 检索统计接口可直接用于图表和数据概览模块。\n',
    '- 机构概览接口内置 404 场景，便于验证状态码展示与异常处理。\n'
  ),
  1,
  0,
  '接入概览',
  'content',
  0
);

SET @guide_overview = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '快速联调步骤',
  @guide_column_id,
  @guide_overview,
  1,
  1,
  CONCAT(
    '## 快速联调步骤\n\n',
    '1. 导入 mockoon/discovery-realistic-test.json 到 Mockoon。\n',
    '2. 启动端口 17302。\n',
    '3. 打开“科技文献开放检索平台”中的开放 API 节点进行联调。\n',
    '4. 将机构编码改为 NSTL404，可复现 404 返回场景。\n'
  ),
  1,
  0,
  '快速联调步骤',
  'content',
  0
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '鉴权与配额说明',
  @guide_column_id,
  @guide_root,
  2,
  1,
  CONCAT(
    '## 鉴权与配额说明\n\n',
    '- 推荐请求头：X-Client-Id = demo-open-platform\n',
    '- 推荐调试频率：每分钟不超过 60 次\n',
    '- 管理后台通过接口权限分配直接授权，不再经过申请审核流程\n'
  ),
  1,
  0,
  '鉴权与配额说明',
  'content',
  0
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '错误码与排障说明',
  @guide_column_id,
  @guide_root,
  3,
  1,
  CONCAT(
    '## 错误码与排障说明\n\n',
    '- 200：请求成功，返回完整模拟数据\n',
    '- 202：导出任务已受理，等待异步生成\n',
    '- 400：必要参数缺失，例如未传检索关键词\n',
    '- 404：机构概览接口传入 orgCode=NSTL404 时返回\n'
  ),
  0,
  0,
  '错误码与排障说明',
  'content',
  0
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '开放检索与分析服务',
  @api_column_id,
  0,
  1,
  1,
  CONCAT(
    '# 开放检索与分析服务\n\n',
    '该服务树覆盖文献检索、机构分析与资源推荐场景，适合进行权限分配、接口调试和前后台联调演示。\n\n',
    '已发布节点可直接用于授权；未发布节点保留为灰度模拟场景。'
  ),
  1,
  0,
  '开放检索与分析服务',
  'content',
  0
);

SET @api_root = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '文献检索服务',
  @api_column_id,
  @api_root,
  1,
  1,
  '包含统一检索、检索统计、检索式校验与批量导出任务等能力。',
  1,
  0,
  '文献检索服务',
  'content',
  0
);

SET @literature_group = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '统一检索接口',
  @api_column_id,
  @literature_group,
  1,
  1,
  '支持期刊、学位和专利等多类型科技文献资源的统一检索。',
  1,
  1,
  '统一检索接口',
  'api',
  1
);

SET @api_search = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_search,
  '/api/v1/literature/search',
  'GET',
  JSON_OBJECT(
    'keyword', '新能源电池',
    'pageNum', '1',
    'pageSize', '10',
    'sort', 'relevance',
    'source', 'journal,thesis,patent'
  ),
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform',
    'X-Trace-Source', 'admin-test'
  ),
  NULL,
  'application/json',
  @mockoon_service_id,
  '统一检索接口，返回真实风格的分页检索结果。',
  'none',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '检索统计接口',
  @api_column_id,
  @literature_group,
  2,
  1,
  '提供年度分布、资源类型占比与热点主题统计结果。',
  1,
  1,
  '检索统计接口',
  'api',
  1
);

SET @api_stats = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_stats,
  '/api/v1/literature/stats',
  'GET',
  JSON_OBJECT(
    'keyword', '新能源电池',
    'dimension', 'year,type'
  ),
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  NULL,
  'application/json',
  @mockoon_service_id,
  '检索统计接口，适合数据概览和图表联调。',
  'none',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '检索式校验接口',
  @api_column_id,
  @literature_group,
  3,
  1,
  'POST JSON 场景接口，用于联调复杂检索表达式。',
  0,
  1,
  '检索式校验接口',
  'api',
  1
);

SET @api_validate = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_validate,
  '/api/v1/literature/query/validate',
  'POST',
  NULL,
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  JSON_PRETTY(
    JSON_OBJECT(
      'query', 'TITLE:(新能源电池) AND YEAR:[2022 TO 2025]',
      'targetTypes', JSON_ARRAY('journal', 'thesis', 'patent'),
      'checkSyntax', TRUE
    )
  ),
  'application/json',
  @mockoon_service_id,
  '检索式校验接口，当前为未发布灰度能力。',
  'raw',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '批量导出任务接口',
  @api_column_id,
  @literature_group,
  4,
  1,
  '导出任务接口返回 202 受理状态，用于测试非 200 成功态展示。',
  0,
  1,
  '批量导出任务接口',
  'api',
  1
);

SET @api_export = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_export,
  '/api/v1/literature/export-task',
  'POST',
  NULL,
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  JSON_PRETTY(
    JSON_OBJECT(
      'keyword', '新能源电池',
      'exportFormat', 'xlsx',
      'fields', JSON_ARRAY('title', 'authors', 'source', 'publishYear')
    )
  ),
  'application/json',
  @mockoon_service_id,
  '批量导出任务接口，返回 202 accepted。',
  'raw',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '机构分析服务',
  @api_column_id,
  @api_root,
  2,
  1,
  '提供机构概览与热点主题分析能力。',
  1,
  0,
  '机构分析服务',
  'content',
  0
);

SET @org_group = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '机构概览接口',
  @api_column_id,
  @org_group,
  1,
  1,
  '返回机构地域、研究方向、近三年产出与合作网络信息。',
  1,
  1,
  '机构概览接口',
  'api',
  1
);

SET @api_org_profile = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_org_profile,
  '/api/v1/institutions/profile',
  'GET',
  JSON_OBJECT(
    'orgCode', 'NSTL1001'
  ),
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  NULL,
  'application/json',
  @mockoon_service_id,
  '机构概览接口，将 orgCode 改为 NSTL404 可复现 404。',
  'none',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '机构热点主题接口',
  @api_column_id,
  @org_group,
  2,
  1,
  '返回高频主题热度与增长趋势。',
  1,
  1,
  '机构热点主题接口',
  'api',
  1
);

SET @api_org_topics = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_org_topics,
  '/api/v1/institutions/topics',
  'GET',
  JSON_OBJECT(
    'orgCode', 'NSTL1001',
    'year', '2025'
  ),
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  NULL,
  'application/json',
  @mockoon_service_id,
  '机构热点主题接口。',
  'none',
  NULL
);

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '资源推荐服务',
  @api_column_id,
  @api_root,
  3,
  1,
  '提供首页专题卡片式资源推荐能力，当前为灰度演示分组。',
  0,
  0,
  '资源推荐服务',
  'content',
  0
);

SET @resource_group = LAST_INSERT_ID();

INSERT INTO content_category (
  name,
  column_id,
  parent_id,
  sort,
  status,
  doc_content,
  is_published,
  is_open,
  title,
  type,
  can_apply
) VALUES (
  '资源推荐接口',
  @api_column_id,
  @resource_group,
  1,
  1,
  '适合门户首页推荐位和专题资源卡片流展示。',
  0,
  1,
  '资源推荐接口',
  'api',
  1
);

SET @api_recommend = LAST_INSERT_ID();

INSERT INTO interface_config (
  category_id,
  api_path,
  api_method,
  default_params,
  default_headers,
  body_template,
  content_type,
  mockoon_service_id,
  description,
  body_type,
  body_form_data
) VALUES (
  @api_recommend,
  '/api/v1/resources/recommend',
  'GET',
  JSON_OBJECT(
    'subject', '人工智能',
    'limit', '6'
  ),
  JSON_OBJECT(
    'X-Client-Id', 'demo-open-platform'
  ),
  NULL,
  'application/json',
  @mockoon_service_id,
  '资源推荐接口，当前为未发布灰度能力。',
  'none',
  NULL
);
