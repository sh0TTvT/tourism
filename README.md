# Tourism QA (Spring Boot + Vue + Vite + MySQL)

一个可运行的旅游知识问答系统，支持：
- ChatGPT 风格对话界面
- 旅游知识问答
- 对话中一键生成旅游路线
- 路线点位地图可视化（OpenStreetMap + Leaflet）
- 多模型切换（SiliconFlow + Ollama，Ollama 为低优先级兜底）
- 管理员可维护可用大模型（增删改查、默认模型）
- 登录注册与 JWT 鉴权（密码 BCrypt 哈希）

## 技术栈
- Java 17+
- Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA + MySQL
- Spring Data Neo4j + Neo4j
- Vue 3 + Vite + Leaflet

## 数据库设计
后端默认连接 MySQL（库名 `tourism_qa`），核心表：
- `users` 用户账号
- `chat_conversations` 聊天会话
- `chat_messages` 聊天消息
- `llm_models` 大模型配置（管理员维护）
- `route_plans` 路线规划主记录
- `route_points` 路线点位

可参考建表脚本：`src/main/resources/db/mysql-schema.sql`

## 启动前配置

### 1) 准备 MySQL
```sql
CREATE DATABASE IF NOT EXISTS tourism_qa
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2) 环境变量
```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/tourism_qa?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true"
export DB_USERNAME="root"
export DB_PASSWORD="123456"

export SILICONFLOW_API_KEY="你的key"
export JWT_SECRET="至少32字符的随机字符串"
export NEO4J_URI="bolt://127.0.0.1:7687"
export NEO4J_USERNAME="neo4j"
export NEO4J_PASSWORD="你的neo4j密码"

# 可选
export DEFAULT_LLM_PROVIDER="siliconflow"
export DEFAULT_LLM_MODEL="Qwen/Qwen2.5-7B-Instruct"
export OLLAMA_ENABLED="true"
export OLLAMA_BASE_URL="http://localhost:11434"
```

## 启动

### 一体化启动（推荐）
```bash
mvn spring-boot:run
```

该命令会自动：
- 安装前端 Node/npm（由 Maven 插件管理）
- 执行 `frontend` 下 `npm install` 和 `npm run build`
- 将 Vite 构建产物复制到 Spring Boot 静态资源目录并启动后端

浏览器打开：
- http://localhost:8080

### 前端本地开发（可选）
```bash
cd frontend
npm install
npm run dev
```

## 前端目录
- `frontend/`：Vue + Vite 源码
- `target/frontend-dist/`：Vite 构建产物（构建后生成）

## 接口概览
- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录
- `GET /api/users/me` 当前用户
- `GET /api/models` 模型列表
- `GET /api/chat/conversations` 当前用户历史会话列表
- `GET /api/chat/conversations/{conversationId}` 当前用户指定会话详情（含完整消息）
- `POST /api/chat` 旅游问答（可选 `conversationId`）
- `POST /api/routes/plan` 路线规划（可选 `conversationId`）
- `POST /api/kg/nodes` 创建知识图谱实体
- `GET /api/kg/nodes/{nodeId}` 获取实体详情
- `GET /api/kg/nodes?keyword=北京&limit=20` 搜索实体
- `PUT /api/kg/nodes/{nodeId}` 更新实体
- `DELETE /api/kg/nodes/{nodeId}` 删除实体（会级联删除关系）
- `POST /api/kg/relationships` 创建关系
- `GET /api/kg/relationships/{relationshipId}` 获取关系详情
- `GET /api/kg/relationships?nodeId=1&limit=50` 按实体查询关系
- `PUT /api/kg/relationships/{relationshipId}` 更新关系
- `DELETE /api/kg/relationships/{relationshipId}` 删除关系
- `POST /api/kg/context` 图谱检索上下文（供大模型提示词注入/调试）
- `GET /api/admin/users` 管理员查看用户列表
- `PUT /api/admin/users/{userId}/role` 管理员更新用户角色（`USER` / `ADMIN`）
- `GET /api/admin/models` 管理员查看模型配置列表
- `POST /api/admin/models` 管理员新增模型配置
- `PUT /api/admin/models/{id}` 管理员更新模型配置
- `PUT /api/admin/models/{id}/default` 管理员设置默认模型
- `DELETE /api/admin/models/{id}` 管理员删除模型配置

## 知识图谱接入说明
- 后端已将知识图谱检索结果自动注入 `POST /api/chat` 与 `POST /api/routes/plan` 的系统提示词中。
- 若当前问题命中图谱实体/关系，模型会优先参考图谱事实生成答案；未命中则退化为普通模型回答。
- 图谱节点统一标签为 `KgNode`，关系类型统一为 `KG_REL`，业务语义使用关系属性 `predicate` 表达。
- `POST/PUT/DELETE /api/kg/**` 与 `GET /api/kg/**` 统一要求管理员权限。

## 管理员权限说明
- 用户角色：`USER`、`ADMIN`。
- 若 `app.security.first-user-admin=true`，系统会自动保证至少一个管理员：
  - 首次注册成功的账号会成为管理员；
  - 老库升级场景下，如果还没有管理员，则首个成功登录账号会自动升为管理员。
- 普通用户访问管理员接口会返回 `403` JSON：`无权限访问该资源，仅管理员可操作`。
- 模型管理约束：
  - 至少保留一个启用模型；
  - 默认模型必须是启用状态；
  - 当 `llm_models` 为空时，系统会在启动时从 `app.llm.providers` 自动导入初始化数据（仅首次导入）。

## 知识图谱示例数据
可直接执行示例脚本快速初始化：
- 文件：`src/main/resources/db/neo4j-seed.cypher`
- 执行方式（示例）：
```bash
cypher-shell -a bolt://127.0.0.1:7687 -u neo4j -p 你的密码 -f src/main/resources/db/neo4j-seed.cypher
```

## 真实来源一键导入脚本（推荐）
项目已提供自动导入脚本：从 Wikidata/WDQS + 多站点网页来源查询真实数据，清洗后写入 Neo4j。

- Python 脚本：`scripts/import_wikidata_kg.py`
- 一键包装脚本：`scripts/import_real_kg.sh`

### 快速执行
```bash
chmod +x scripts/import_real_kg.sh
NEO4J_PASSWORD=你的密码 ./scripts/import_real_kg.sh "Beijing,Shanghai,Chengdu"
```
说明：一键脚本已启用 WDQS 504 降级重试与网页多源采集（百科/论坛/贴吧/小红书/官网）。

### 自定义执行
```bash
python3 scripts/import_wikidata_kg.py \
  --cities "Beijing,Shanghai,Chengdu" \
  --lang zh \
  --max-attractions 25 \
  --max-foods 12 \
  --max-web-sources 24 \
  --max-sources-per-site 2 \
  --timeout 90 \
  --retries 5 \
  --retry-delay 2 \
  --wdqs-max-seconds 45 \
  --wdqs-max-failures 4 \
  --clear-existing \
  --apply
```

如果你希望仅导入 Wikidata，不抓网页来源：
```bash
python3 scripts/import_wikidata_kg.py \
  --cities "北京,上海,成都" \
  --lang zh \
  --disable-web-sources \
  --timeout 90 \
  --retries 5 \
  --retry-delay 2 \
  --clear-existing \
  --apply
```

如果希望 WDQS 一旦失败就中断（严格模式）：
```bash
python3 scripts/import_wikidata_kg.py \
  --cities "北京,上海,成都" \
  --lang zh \
  --strict-wdqs \
  --apply
```

如果网络不稳定，建议先只抓取并生成 Cypher（不直接写库）：
```bash
python3 scripts/import_wikidata_kg.py \
  --cities "北京,上海,成都" \
  --lang zh \
  --timeout 90 \
  --retries 5 \
  --retry-delay 2 \
  --output src/main/resources/db/real-kg-import.cypher
```
可选参数补充：
- `--search-providers bing,duckduckgo`：网页检索 provider 顺序（默认两者都启用）。
- `--wdqs-max-seconds 45`：每个城市 WDQS 总预算秒数，超时后自动跳过该城市后续 WDQS。
- `--wdqs-max-failures 4`：每个城市 WDQS 最大失败次数，达到后自动跳过。
- `--max-web-sources 24`：每个城市最多抓取多少条网页来源。
- `--max-sources-per-site 2`：每个目标站点最多抓取多少条。
- `--disable-web-sources`：禁用百科/论坛/贴吧/小红书/官网网页来源抓取。

确认文件生成后再执行：
```bash
cypher-shell -a bolt://127.0.0.1:7687 -u neo4j -p 你的密码 -f src/main/resources/db/real-kg-import.cypher
```

### 数据来源（官方）
- Wikidata Action API（`wbsearchentities` / `wbgetentities`）
- Wikidata Query Service（SPARQL endpoint）
- Bing RSS Search（站点定向检索）
- DuckDuckGo HTML Search（站点定向检索兜底）
- 文档：
  - https://www.wikidata.org/w/api.php
  - https://www.wikidata.org/wiki/Wikidata:Data_access
  - https://query.wikidata.org/
  - https://www.bing.com/search?format=rss
  - https://duckduckgo.com/html/

脚本会在节点和关系里写入 `source` 与 `sourceUrl`，便于溯源审计。

## 安全建议
- 对话中暴露过 API Key，建议在 SiliconFlow 控制台轮换（重置）该 Key。
- 生产环境请配置 HTTPS、限流、审计日志、邮箱验证与找回密码流程。
