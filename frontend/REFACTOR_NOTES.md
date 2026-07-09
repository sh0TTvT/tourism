# 前端重构说明

本次重构目标：在不改变现有业务功能和接口语义的前提下，抽离 API 层，并将原本过重的 `App.vue` 拆分为视图层与业务控制层。

## 新目录

```text
src
├── api
│   ├── request.js          # 统一请求封装：JSON 请求、NDJSON 流式请求、token 注入、401 处理
│   ├── authApi.js          # 登录/注册接口封装
│   ├── chatApi.js          # 对话、会话、流式聊天接口封装
│   ├── routeApi.js         # 路线列表、详情、生成、保存、删除接口封装
│   ├── exploreApi.js       # 社区帖子、点赞、收藏、评论、路线导入接口封装
│   ├── modelApi.js         # 模型列表接口封装
│   ├── serviceApi.js       # 公共服务状态接口封装
│   ├── userApi.js          # 用户资料、偏好、密码接口封装
│   └── index.js
├── composables
│   └── useUserApp.js       # 原 App.vue 中的主要状态、计算属性、业务方法和生命周期
├── views
│   ├── ChatView.vue        # 聊天主视图容器
│   ├── RoutesView.vue      # 路线规划主视图容器
│   └── ExploreView.vue     # 探索社区主视图容器
├── components              # 原有基础/业务组件，保持不变
├── constants
├── utils
├── App.vue                 # 只保留全局壳、侧边栏、弹窗、主视图装配
└── main.js
```

## 注意

1. 本次没有主动修改接口地址、请求 payload、响应解析逻辑和 UI 样式。
2. `App.vue` 的业务逻辑已迁移到 `useUserApp.js`，后续继续拆分时可以再按 `useAuth`、`useChat`、`useRoutes`、`useExplore` 进一步细化。
3. 新增 `.gitignore`，避免提交 `node_modules`、`.vite`、`dist` 等缓存或构建产物。
4. 当前压缩包不包含依赖目录，请在本地项目根目录执行 `npm install` 后再运行 `npm run dev` 或 `npm run build`。
```

## 第二阶段：拆分 useUserApp

本次继续在不改变页面组件调用方式的前提下，将原来的 `src/composables/useUserApp.js` 拆为一个轻量组装器和多个按职责划分的 composable：

- `src/composables/app/useAppState.js`：集中声明全局状态、表单状态和 computed 派生状态。
- `src/composables/app/useFeedback.js`：通知 toast 与确认弹窗。
- `src/composables/app/useLayoutActions.js`：视图切换、侧边栏、账号弹窗、菜单关闭、窗口事件处理。
- `src/composables/app/useServiceActions.js`：模型列表、公共服务状态和地图运行配置。
- `src/composables/app/useChatActions.js`：聊天历史、会话选择、流式发送、中断恢复。
- `src/composables/app/useRouteActions.js`：路线草稿、路线生成、路线保存、路线点编辑。
- `src/composables/app/useExploreActions.js`：探索社区帖子、点赞收藏评论、路线分享与导入。
- `src/composables/app/useAuthActions.js`：登录注册、用户资料、偏好设置、密码修改、退出登录。
- `src/composables/app/helpers.js`：通用辅助函数。

`src/composables/useUserApp.js` 仍然保留原有对外返回字段和方法名，因此 `App.vue`、`views` 和现有组件不需要改调用方式。
