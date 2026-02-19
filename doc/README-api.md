# jar-analyzer API

## 基本信息
- 默认端口: `10032`（默认 Bind `0.0.0.0`；本机访问一般用 `http://127.0.0.1:10032`）
- 认证: 启动参数 `-sa -st <TOKEN>`，请求头 `Token: <TOKEN>`
- 接口以 `GET` 为主（部分任务取消接口也支持 `DELETE`）

## 统一响应格式
成功:
```json
{
  "ok": true,
  "data": {},
  "meta": {},
  "warnings": []
}
```

错误:
```json
{
  "ok": false,
  "code": "invalid_param",
  "message": "xxx",
  "status": 400
}
```

- `meta` / `warnings` 字段在需要时出现。
- 结果默认过滤 JDK/噪声库，传 `scope=all` 或 `includeJdk=1` 可包含。

## 通用参数
- `class`: 类名，支持 `a.b.C` 或 `a/b/C`
- `offset` / `limit`: 分页
- `scope`: `app|all`（默认 `app`）

## API 列表

### 元信息 / 类
- `GET /api/meta/jars`
  参数: `offset` `limit`
  返回: `jar_id` `jar_name` `jar_fingerprint`
- `GET /api/meta/jars/resolve`
  参数: `class`
- `GET /api/class/info`
  参数: `class` `scope`

### 入口 & 路由
- `GET /api/entrypoints`
  参数: `type` `limit` `scope`
  `type`: `spring_controller` `spring_interceptor` `servlet` `filter` `listener` `all`
- `GET /api/entrypoints/mappings`
  参数: `class` 或 `jarId`/`keyword`/`offset`/`limit` `scope`

### 方法检索
- `GET /api/methods/search`
  参数组合:
  - 按注解: `anno` `annoMatch` `annoScope` `jarId` `offset` `limit`
  - 按字符串: `str` `strMode` `classLike` `jarId` `offset` `limit`
  - 按签名: `class` `method` `desc` `match` `offset` `limit`
  - 仅 `class` 时返回该类所有方法
  其他: `scope`
- `GET /api/methods/impls`
  参数: `class` `method` `desc` `direction` `offset` `limit` `scope`
  `direction`: `impls` 或 `super`

### 调用图
- `GET /api/callgraph/edges`
  参数: `class` `method` `desc` `direction` `offset` `limit` `scope`
  `direction`: `callers` / `callees`
- `GET /api/callgraph/by-sink`
  参数:
  - `sinkName`（内置 sink 名称列表）
  - 或 `sinkClass` `sinkMethod` `sinkDesc`
  - 或 `items`（JSON 数组）
  - 其他: `limit` `scope`

### 反编译 / 证据
- `GET /api/code`
  参数: `class` `method` `desc` `engine` `full`
  `engine`: `cfr` / `fernflower`

### 资源
- `GET /api/resources/list`
  参数: `path` `jarId` `offset` `limit`
- `GET /api/resources/get`
  参数: `id` 或 `path`（可配合 `jarId`）
  其他: `offset` `limit` `base64`
- `GET /api/resources/search`
  参数: `query`/`q` `mode` `case` `jarId` `limit` `maxBytes`

### 语义 / 配置
- `GET /api/semantic/hints`
  参数: `jarId` `limit` `strLimit` `scope`
- `GET /api/config/usage`
  参数: `keys`/`items` `jarId` `maxKeys` `maxPerKey` `maxResources` `maxBytes`
        `maxDepth` `mappingLimit` `maxEntry` `mask` `includeResources` `scope`

### 安全
- `GET /api/security/vul-rules`
- `GET /api/security/vul-search`
  参数: `name` `level` `groupBy` `limit` `totalLimit` `offset`
        `blacklist` `whitelist` `jar` `jarId` `scope`
- `GET /api/security/sca`
  参数: `path` `paths` `log4j` `fastjson` `shiro`
- `GET /api/security/leak`
  参数: `types` `base64` `limit` `whitelist` `blacklist` `jar` `jarId` `scope`
- `GET /api/security/gadget`
  参数: `dir` `native` `hessian` `fastjson` `jdbc`

### DFS / Taint（异步任务）
- `GET /api/flow/dfs`
  参数: `mode` `sinkName` `sinkClass` `sinkMethod` `sinkDesc`
        `sourceClass` `sourceMethod` `sourceDesc`
        `searchAllSources` `onlyFromWeb`
        `depth` `maxLimit` `maxPaths` `timeoutMs` `blacklist`
  说明:
  - 后端固定 graph（不再提供 classic fallback）
  - `onlyFromWeb` 仅在 `searchAllSources=true` 时生效
- `GET /api/flow/dfs/jobs/{jobId}`
  状态
- `GET /api/flow/dfs/jobs/{jobId}/results`
  参数: `offset` `limit` `compact`
- `GET /api/flow/dfs/jobs/{jobId}/cancel`
  或 `DELETE /api/flow/dfs/jobs/{jobId}`

- `GET /api/flow/taint`
  参数: `dfsJobId` `timeoutMs` `maxPaths`
  说明:
  - 后端固定 graph（不再提供 classic fallback）
  - seed 参数已移除，不提供手工 seed 入口
- `GET /api/flow/taint/jobs/{jobId}`
  状态
- `GET /api/flow/taint/jobs/{jobId}/results`
  参数: `offset` `limit`
- `GET /api/flow/taint/jobs/{jobId}/cancel`
  或 `DELETE /api/flow/taint/jobs/{jobId}`

### Cypher
- `GET /api/query/cypher`
  参数: `q`（Cypher 文本）
  说明:
  - 仅走 Cypher 执行路径，不再提供 GUI contributor 侧 Cypher->SQL fallback
  - 图过程 `ja.taint.track` 已升级为图原生全局数据流实现
