# API 开放与授权平台

[English](README.en.md) | 简体中文

面向 API 提供方、调用方和管理员的接口开放平台，管理接口目录、权限申请与审批、调用凭证、沙箱联调和审计记录。

## 主要功能

- 门户浏览接口目录并申请调用权限；管理端维护资源与授权范围。
- Java 后端处理用户准入、角色、分类权限与接口凭证。
- 独立沙箱服务代理联调请求，记录调用与审计信息。

## 源码与运行入口

`源码/open-platform-ui/` 门户；`源码/open-platform-admin/` 管理端；`源码/open-platform-server/` Spring Boot 后端；`源码/sandbox-service/` 沙箱。

JDK 11、Maven、Node.js，以及自行配置的 MariaDB/MySQL、Redis、MinIO 和身份认证服务。先核对各模块配置，再分别安装依赖和启动。

后端配置在 `源码/open-platform-server/src/main/resources/`。可从 `源码/` 执行 `mvn -f open-platform-server/pom.xml test`；这条命令用于单元测试，不启动服务。

## 配置

`OPEN_PLATFORM_JWT_SECRET` 必填，使用至少 32 字节随机密钥；`TEST_DB_PASSWORD`、`TEST_REDIS_PASSWORD`、`TEST_MINIO_SECRET_KEY` 分别配置数据库、缓存与存储口令。证书辅助脚本读取 `NSTL_PFX_PASSWORD`。

配置示例位于源码中的 `.env.example`。真实凭证只保存在本机环境或不跟踪的 `.env` 中。`127.0.0.1` 与 `example.invalid` 为本地/不可解析示例地址，部署前替换为自己的配置。

## 验证与边界

沙箱接受的目标 URL、实际权限边界和真实身份服务需要在目标环境单独验证。`start.bat` 和 HTTPS 辅助脚本可能修改 hosts/证书或回收端口，先阅读脚本后使用。

本目录于 2026-11-04 完成公开范围整理与静态核验。代码发布不等于已部署或全部功能可运行。配置与依赖准备好后，应再执行适用于目标环境的测试。

公开范围及 GitHub/Gitee 接手规则见[仓库发布说明](仓库发布说明.md)。资料库原有的文档、会话、经验和管理记录继续保留在本目录，只供本地知识库使用。
