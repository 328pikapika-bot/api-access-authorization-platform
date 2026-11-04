# NSTL 接口平台

## 项目概述

NSTL 接口平台面向接口开放、接口申请、权限发放、接口文档展示、沙箱调试和后台管理等场景，提供前台门户、后台管理端、后端服务和沙箱服务四个核心模块。

## 功能模块

- 前台门户：接口分类浏览、接口文档查看、接口申请、个人申请与凭证查看、沙箱调试入口。
- 后台管理：数据概览、栏目与分类管理、接口配置、申请审核、权限分配、Mockoon 服务管理、内容日志与系统日志。
- 后端服务：统一认证接入、JWT 会话、权限控制、审计日志、内容管理、申请审批、凭证管理、文件上传。
- 沙箱服务：接口调试代理、Mockoon 联调、访问频率控制和调试日志记录。

## 技术栈

- 后端：Java 11、Spring Boot 2.7、Spring Security、MyBatis-Plus、Druid、MariaDB、Redis、MinIO。
- 前端：Vue 3、Vite、Pinia、Vue Router、TDesign、Axios。
- 沙箱：Spring Boot、MyBatis-Plus、MariaDB。
- 部署：Docker、Docker Compose、Nginx。

## 项目结构

```text
.
├── open-platform-server/   # 后端接口服务
├── open-platform-ui/       # 前台门户
├── open-platform-admin/    # 后台管理端
├── sandbox-service/        # 沙箱调试服务
├── mockoon/                # Mockoon 示例接口和初始化脚本
├── scripts/                # 本地开发和部署辅助脚本
├── shared/                 # 共用脚本
├── doc/                    # 项目文档
├── docker-compose.yml      # 测试/部署编排
└── docker-compose.local-dev.yml
```

## 环境要求

- JDK 11+
- Maven 3.6+
- Node.js 16+
- MariaDB 10.x
- Redis 6.x 或 Redis Cluster
- MinIO（使用文件上传功能时需要）

## 快速启动

### 启动本地依赖

```bash
docker compose -f docker-compose.local-dev.yml up -d
```

### 启动后端服务

```bash
cd open-platform-server
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:17300/api/v1/service/open_platform
```

### 启动沙箱服务

```bash
cd sandbox-service
mvn spring-boot:run
```

默认沙箱地址：

```text
http://localhost:8081
```

### 启动前台门户

```bash
cd open-platform-ui
npm install
npm run dev
```

### 启动后台管理端

```bash
cd open-platform-admin
npm install
npm run dev
```

NSTL 本地联调模式：

```bash
npm run dev:nstl
```

## 构建

### 后端构建

```bash
cd open-platform-server
mvn clean package
```

### 沙箱服务构建

```bash
cd sandbox-service
mvn clean package
```

### 前端构建

```bash
cd open-platform-ui
npm run build

cd ../open-platform-admin
npm run build
```

## Docker 部署

```bash
docker compose up -d
```

默认服务端口：

- 前台门户：`18300`
- 后台管理端：`18301`
- 后端服务：`17300`
- Mockoon：`17301`

## 提交说明

仓库保留项目源码、配置文件、脚本和必要文档；依赖目录、构建产物、运行日志等由 `.gitignore` 控制。

## 相关文档

- `编译指南.md`
- `接口平台测试环境部署文档.md`
- `doc/` 下的需求、设计、开发和验收材料
