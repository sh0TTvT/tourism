# Tourism QA

基于大语言模型与知识图谱的旅游智能问答平台，提供旅游问答、路线规划、社区互动和后台管理能力。

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42B883.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-5.4-646CFF.svg)](https://vite.dev/)

## 项目简介

Tourism QA 面向旅游信息查询和行程规划场景。系统将大模型对话、Neo4j 知识图谱与实时天气信息结合，并提供独立的用户端和管理端。

### 核心功能

- 流式旅游问答与历史会话管理
- 多模型切换，支持 SiliconFlow 和本地 Ollama
- 旅游路线生成、编辑、地理编码及地图展示
- 基于 Neo4j 的旅游知识检索和问答上下文增强
- 旅游内容发布、评论、点赞与收藏
- 用户注册、登录、资料管理和 JWT 鉴权
- 模型、外部服务、用户及知识图谱后台管理
- 实时天气查询和外部服务状态检查

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 用户端 | Vue 3、Vite、Leaflet、D3.js、Markdown-it |
| 管理端 | Vue 3、Vite、D3.js、VueUse |
| 后端 | Java 17、Spring Boot 3.3、Spring Security、Spring Data JPA |
| 数据存储 | MySQL、Neo4j |
| 模型服务 | SiliconFlow、Ollama |
| 鉴权 | JWT、BCrypt |
| 测试 | JUnit 5、Mockito、H2 |

## 项目结构

```text
tourism/
├── frontend/             # 用户端 Vue 应用
├── admin-frontend/       # 管理端 Vue 应用
├── backend/              # Spring Boot API 服务
│   └── src/main/resources/db/
│       ├── mysql-schema.sql
│       ├── neo4j-seed.cypher
│       └── real-kg-import.cypher
├── .gitignore
└── README.md
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Neo4j 5+
- SiliconFlow API Key，或可用的 Ollama 服务

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/sh0TTvT/tourism.git
cd tourism
```

### 2. 创建 MySQL 数据库

```sql
CREATE DATABASE IF NOT EXISTS tourism_qa
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 3. 配置环境变量

以下变量是后端启动所需的基础配置：

```bash
export DB_PASSWORD="你的 MySQL 密码"
export NEO4J_PASSWORD="你的 Neo4j 密码"
export JWT_SECRET="至少 32 个字符的随机字符串"
export SILICONFLOW_API_KEY="你的 SiliconFlow API Key"
```

可按实际环境覆盖以下默认配置：

```bash
export NEO4J_URI="bolt://127.0.0.1:7687"
export NEO4J_USERNAME="neo4j"
```

> 不要将真实密码、Token 或 API Key 写入配置文件并提交到仓库。

### 4. 启动后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

后端默认运行于 `http://localhost:8080`。

### 5. 启动用户端

```bash
cd frontend
npm ci
npm run dev
```

用户端默认运行于 `http://localhost:5173`，开发服务器会将 `/api` 请求代理到后端。

### 6. 启动管理端

打开另一个终端：

```bash
cd admin-frontend
npm ci
npm run dev
```

管理端默认运行于 `http://localhost:5174`。

## 数据初始化

MySQL 表结构可由 JPA 自动维护，也可参考：

```text
backend/src/main/resources/db/mysql-schema.sql
```

导入 Neo4j 示例数据：

```bash
cypher-shell \
  -a bolt://127.0.0.1:7687 \
  -u neo4j \
  -p "你的密码" \
  -f backend/src/main/resources/db/neo4j-seed.cypher
```

仓库还提供 `real-kg-import.cypher`，用于导入已整理的知识图谱数据。

## 常用命令

### 构建前端

```bash
npm --prefix frontend run build
npm --prefix admin-frontend run build
```

### 运行后端测试

```bash
mvn -f backend/pom.xml test
```

## API 概览

| 模块 | 主要接口 |
| --- | --- |
| 认证 | `POST /api/auth/register`、`POST /api/auth/login` |
| 用户 | `GET /api/users/me`、`PUT /api/users/me/profile` |
| 问答 | `POST /api/chat/stream`、`GET /api/chat/conversations` |
| 路线 | `POST /api/routes/plan`、`GET /api/routes` |
| 社区 | `GET /api/explore/posts`、`POST /api/explore/posts` |
| 天气 | `GET /api/weather/forecast` |
| 知识图谱 | `/api/kg/nodes`、`/api/kg/relationships`、`/api/kg/context` |
| 后台管理 | `/api/admin/users`、`/api/admin/models`、`/api/admin/services` |

除注册、登录、公开模型和服务状态等接口外，业务接口通常需要在请求头中携带 JWT：

```http
Authorization: Bearer <token>
```

## 权限说明

系统包含 `USER` 和 `ADMIN` 两种角色。默认配置启用了首位用户管理员机制：

- 新数据库中，第一个注册成功的用户会成为管理员。
- 已有数据但没有管理员时，第一个成功登录的用户会自动升级为管理员。
- 管理员可维护用户、模型、外部服务和知识图谱。

生产环境部署前，请根据实际安全策略检查 `app.security.first-user-admin` 配置。

## 配置说明

主要配置位于：

```text
backend/src/main/resources/application.yml
```

常用配置项包括：

- MySQL 和 Neo4j 连接参数
- JWT 密钥与有效期
- 默认模型和模型服务地址
- 知识图谱检索限制
- 实时天气和景点状态开关

## 安全建议

- 所有密钥和数据库凭据均通过环境变量注入。
- 生产环境应启用 HTTPS、访问限流和审计日志。
- 首次公开部署前应轮换开发阶段使用过的 API Key。
- 不要提交 `.env`、日志、构建产物或本地数据库文件。

## 参与开发

1. Fork 本仓库并创建功能分支。
2. 保持改动范围明确，并补充必要测试。
3. 提交前运行后端测试和两个前端构建。
4. 发起 Pull Request，说明改动内容及验证结果。
