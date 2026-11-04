-- 后台管理字段长度修复脚本
-- 适用范围：本地库与测试环境库 open_platform
-- 执行前建议先备份数据库。

ALTER TABLE `platform`
  MODIFY COLUMN `description` varchar(5000)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
  NULL DEFAULT NULL COMMENT '描述';

ALTER TABLE `mockoon_service`
  MODIFY COLUMN `name` varchar(100)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
  NOT NULL COMMENT '服务名称',
  MODIFY COLUMN `description` varchar(500)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
  NULL DEFAULT NULL COMMENT '服务说明';
