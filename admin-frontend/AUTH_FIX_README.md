# 管理员端认证问题修复说明

## 问题描述

管理员登录后，刷新页面或重新进入时仍然提示"请先登录"。

## 根本原因

**存储方式不一致导致的数据读取失败：**

1. **登录时**（useAuth.js）：
   ```javascript
   localStorage.setItem(STORAGE_KEYS.token, token.value);  // 存储纯字符串
   ```

2. **请求时**（request.js）：
   ```javascript
   const token = storage.get(STORAGE_KEYS.token);  // 尝试 JSON.parse
   ```

3. **storage.get() 实现**（storage.js）：
   ```javascript
   get(key) {
     const item = localStorage.getItem(key);
     return item ? JSON.parse(item) : null;  // ❌ JSON.parse 纯字符串失败
   }
   ```

4. **结果**：
   - `storage.get()` 对纯字符串 token 执行 `JSON.parse()` 失败
   - 返回 `null`
   - 请求没有携带 `Authorization` header
   - 后端返回 401
   - `request.js` 清除会话
   - 用户被强制退出

## 修复方案

**统一使用 `storage` 工具进行存储和读取：**

### 修改文件：`src/composables/useAuth.js`

1. **导入 storage 工具**：
   ```javascript
   import { storage } from '@/utils/storage';
   ```

2. **初始化状态时使用 storage.get()**：
   ```javascript
   const token = ref(storage.get(STORAGE_KEYS.token) || '');
   const user = ref(storage.get(STORAGE_KEYS.user));
   ```

3. **登录时使用 storage.set()**：
   ```javascript
   storage.set(STORAGE_KEYS.token, token.value);
   storage.set(STORAGE_KEYS.user, user.value);
   ```

4. **清除会话时使用 storage.remove()**：
   ```javascript
   storage.remove(STORAGE_KEYS.token);
   storage.remove(STORAGE_KEYS.user);
   ```

## 修复效果

- ✅ token 和 user 数据存储格式一致（都经过 JSON.stringify）
- ✅ 读取时能正确解析（都经过 JSON.parse）
- ✅ 请求能正确携带 Authorization header
- ✅ 会话恢复正常工作
- ✅ 刷新页面后保持登录状态

## 测试步骤

### 1. 清除旧数据（重要！）

打开浏览器开发者工具 Console，执行：
```javascript
localStorage.removeItem('tourismqa_admin_token');
localStorage.removeItem('tourismqa_admin_user');
```

### 2. 重新登录

1. 访问 http://localhost:5174
2. 使用管理员账号登录
3. 登录成功后，检查 localStorage：
   ```javascript
   // 应该看到 JSON 格式的数据
   localStorage.getItem('tourismqa_admin_token')  // "\"eyJhbGc...\""
   localStorage.getItem('tourismqa_admin_user')   // "{\"id\":1,...}"
   ```

### 3. 测试会话恢复

1. 刷新页面（F5 或 Cmd+R）
2. 应该直接进入管理员界面，不需要重新登录
3. 检查网络请求，`/api/users/me` 应该返回 200

### 4. 使用测试页面（可选）

打开 `admin-frontend/test-auth-fix.html` 进行详细测试。

## 技术细节

### storage 工具的工作原理

```javascript
// storage.set() - 自动序列化
set(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

// storage.get() - 自动反序列化
get(key) {
  const item = localStorage.getItem(key);
  return item ? JSON.parse(item) : null;
}
```

### 为什么需要统一

| 操作 | 旧方式（错误） | 新方式（正确） |
|------|---------------|---------------|
| 存储 token | `localStorage.setItem(key, "abc123")` | `storage.set(key, "abc123")` → 存储为 `"\"abc123\""` |
| 读取 token | `storage.get(key)` → `JSON.parse("abc123")` ❌ | `storage.get(key)` → `JSON.parse("\"abc123\"")` ✅ |
| 存储 user | `localStorage.setItem(key, JSON.stringify(obj))` | `storage.set(key, obj)` → 自动 stringify |
| 读取 user | `storage.get(key)` → `JSON.parse("{...}")` ✅ | `storage.get(key)` → `JSON.parse("{...}")` ✅ |

## 相关文件

- `src/composables/useAuth.js` - 认证状态管理（已修复）
- `src/utils/storage.js` - 存储工具
- `src/utils/request.js` - HTTP 请求工具
- `src/components/auth/AuthGuard.vue` - 认证守卫组件

## 提交信息

```
修复：解决管理员端登录后刷新页面仍提示需要登录的问题

问题原因：
- useAuth.js 使用 localStorage 直接存储 token（纯字符串）
- request.js 使用 storage.get() 读取，尝试 JSON.parse 失败
- 导致请求没有 Authorization header，后端返回 401

解决方案：
- 统一使用 storage 工具进行存储和读取
- 确保数据序列化和反序列化的一致性
```
