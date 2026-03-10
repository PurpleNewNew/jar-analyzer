# jar-analyzer API

## 基本信息
- 默认端口: `10032`（默认 Bind `0.0.0.0`；本机访问一般用 `http://127.0.0.1:10032`）
- 认证: 在 GUI 的 `API Startup Config` 中配置 `auth/token`（重启生效），请求头 `Token: <TOKEN>`
- 接口以 `GET` 为主（Cypher/项目生命周期为 `POST`/`DELETE`）

## 项目模型
- 仅两种项目类型：
  - `TEMP`：会话临时项目，`projectKey` 形如 `temp-<session-id>`
  - `PERSISTENT`：正式项目，`projectKey` 为稳定键
- 不再有 `default` 业务项目。
- 数据路径：
  - TEMP: `db/neo4j-temp/<session-id>/`（进程退出自动清理）
  - PERSISTENT: `db/neo4j-projects/<project-key>/`（项目间隔离）
- 启动行为：进程启动后默认 active project 为 TEMP。

## 建库前提（当前实现）
- 输入仅支持字节码：`jar/war/class/目录(含字节码)`，不再支持源码索引链路；目录输入会递归收集其中的 `.class/.jar/.war`。
- CLI 建库不再提供 `--del-exist`；项目库替换固定走 staging + atomic swap，失败不会先删旧库。
- 调用图引擎固定为 Tai-e（默认 profile: `balanced`）。
- Tai-e 边保留策略默认 `reachable-app`（保留 APP 边界 + APP 可达的非 SDK LIBRARY caller），可通过 `jar.analyzer.taie.edge.policy` 调整：
  - `reachable-app`（默认）
  - `app-caller`
  - `non-sdk-caller`
  - `full`
- Tai-e invokedynamic 采用内置双档重试（`on -> off`），不再暴露外部模式开关。
- JDK 依赖策略：
  - JDK8: 使用 `rt.jar/jce.jar`
  - JDK9+: 使用 `jmods`（默认 `core` 模块集合，经转换后入分析 classpath）

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

## 行为变更（rmclassic 硬切）

1. Flow 后端固定 graph（不再存在 classic fallback 语义）
2. Taint seed 参数已移除（自动端口推断）
3. Cypher 仅走 Cypher 执行路径（不再做 Cypher->SQL 兼容回退）
4. 字符串检索相关能力依赖 FTS，异常时直接返回 FTS 错误（不再回退 LIKE）
5. active project 未构建（含切换项目后未重建）统一返回 `project_model_missing_rebuild`
6. active project 构建进行中时，图查询 / DFS / Taint / Cypher Explain 统一返回 `project_build_in_progress`
7. DFS / Taint / Cypher 仅面向当前 active project；如需访问其他项目，先调用 `/api/projects/switch`
8. 建库失败不会回退旧快照；失败项目保持 `project_model_missing_rebuild`

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
  说明: 当同一 `class/method/desc` 在多个 jar 中同时存在时，返回会按实际命中的 `jarId/jarName` 分项，而不是把 caller 混成一组。

### 反编译 / 证据
- `GET /api/code`
  参数: `class` `method` `desc` `engine` `full`
  `engine`: 仅支持 `cfr`（可省略）

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
- `GET /api/security/sink-rules`
- `GET /api/security/sink-search`
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
        `depth` `maxLimit` `maxPaths` `timeoutMs` `minEdgeConfidence` `traversalMode`
        `blacklist`
  说明:
  - 后端固定 graph（不再提供 classic fallback）
  - `mode=sink` 且 `searchAllSources=false` 时会从 sink 逆向精确搜索指定 source；返回结果仍按 `source -> sink` 顺序输出，便于后续 taint 复用
  - `onlyFromWeb` 仅在 `searchAllSources=true` 时生效
  - `traversalMode` 仅支持 `call-only`（默认）和 `call+alias`
  - `searchAllSources` / `onlyFromWeb` 会跟随最新 `rules/source.json` 热刷新选择 source，无需重建项目
  - active project 构建中会返回 `project_build_in_progress`
  - 提交队列已收敛为有界队列；满载时返回 `503 job_queue_full`
- `GET /api/flow/dfs/jobs/{jobId}`
  状态
  说明:
  - 返回的 `buildSeq` 是 DFS 执行时绑定的图构建序号
  - 若任务完成后 active project 切换或项目已重建，接口仍可读取结果，但会附带 `stale=true` 与 `staleReason`
- `GET /api/flow/dfs/jobs/{jobId}/results`
  参数: `offset` `limit` `compact`
  说明:
  - `edges` 在可用时会附带 `callSiteKey` `lineNumber` `callIndex`
  - 已完成 job 在切项目/重建后仍可导出结果，但会附带 `stale=true` 与 `staleReason`
- `GET /api/flow/dfs/jobs/{jobId}/cancel`
  或 `DELETE /api/flow/dfs/jobs/{jobId}`

- `GET /api/flow/taint`
  参数: `dfsJobId` `timeoutMs` `maxPaths`
  说明:
  - 后端固定 graph（不再提供 classic fallback）
  - seed 参数已移除，不提供手工 seed 入口
  - 仅接受来自当前 active project 的 DFS job；若任务排队/运行期间切换项目，job 会失败
  - 仅当 DFS 绑定的 `buildSeq` 仍与当前项目图一致时才允许启动 taint
  - active project 构建中会返回 `project_build_in_progress`
  - 提交队列已收敛为有界队列；满载时返回 `503 job_queue_full`
- `GET /api/flow/taint/jobs/{jobId}`
  状态
  说明:
  - 返回的 `buildSeq` 是 taint 执行时绑定的图构建序号
  - 若任务完成后 active project 切换或项目已重建，接口仍可读取结果，但会附带 `stale=true` 与 `staleReason`
- `GET /api/flow/taint/jobs/{jobId}/results`
  参数: `offset` `limit`
  说明:
  - 已完成 job 在切项目/重建后仍可导出结果，但会附带 `stale=true` 与 `staleReason`
- `GET /api/flow/taint/jobs/{jobId}/cancel`
  或 `DELETE /api/flow/taint/jobs/{jobId}`

### Cypher
- `POST /api/query/cypher`
  Body:
  ```json
  {
    "query": "MATCH (n:JANode) RETURN n LIMIT 10",
    "params": {},
    "options": {
      "maxRows": 500,
      "maxMs": 15000,
      "maxHops": 0,
      "maxPaths": 0,
      "profile": "default",
      "expandBudget": 0,
      "pathBudget": 0,
      "timeoutCheckInterval": 0
    }
  }
  ```
  说明:
  - 仅支持只读 Cypher；写语句会返回 `cypher_feature_not_supported`
  - 查询执行固定采用 Neo4j 原生引擎；`CALL ja.*` 不再回退到 ANTLR4/内存 `GraphSnapshot` 兼容过程链
  - 提供一组本地只读 `apoc.*` 兼容函数白名单，当前仅覆盖 `apoc.coll.*` / `apoc.map.*` / `apoc.text.*` 的小集合，不包含 `apoc.path.*`、`load/export/trigger/periodic`
  - `jar.analyzer.cypher.apoc.whitelist` 默认为 `default`；可设为 `none|off|disabled` 彻底关闭，或用 `coll,text,map,apoc.text.join` 这类逗号列表精确控制
  - `maxMs/maxRows` 对原生查询生效
  - `maxHops/maxPaths/expandBudget/pathBudget` 仅对 `ja.*` 过程生效
  - `ja.path.shortest_pruned` / `ja.path.from_to_pruned` 现在走基于 `SummaryEngine + GraphSnapshot` 的 stateful pruning；响应 `warnings` 会附带 `pruned_state_cuts` / `pruned_expanded_edges` 等统计
  - `rules/model.json.pruningPolicy` 可热刷新控制 pruned 语义：
    `sourceSelection=merged|graph|rules`
    `sanitizerMode=hard|ignore`
    `allowAdditionalFlows=true|false`
    `confidenceMode=balanced|strict`
    `scenarios[]` 可按 `sinkKind/sinkKinds`、`sinkTier/sinkTiers`、`sinkTags`、`mode(source|sink)`、`searchAllSources` 覆盖上述值，按数组顺序生效，后写覆盖前写；`sinkTags` 为任一标签命中即匹配
  - `rules/model.json` / `rules/source.json` / `rules/sink.json` 额外支持 `dsl.rules[]` 声明式规则；当前会编译到现有 `summaryModel/additionalTaintSteps/sanitizerModel/guardSanitizers/sourceModel/sourceAnnotations` 与 sink registry 主链，支持 `summary` `additional` `sanitizer` `guard` `pruning-hint` `source` `source-annotation` `sink`
  - `pruning-hint` 会编译到现有 `additionalStepHints`，并按 `container/builder/reflect/optional/stream/array/rules` 等 canonical hint 生效
  - `ja.taint.track` 保留旧的 9 参数写法；可选追加 `mode` `searchAllSources` `onlyFromWeb`
  - `ja.taint.track(..., mode='source')` 会按显式 `source -> sink` 搜索；`mode='sink'` 时会逆向剪枝搜索指定 source
  - `ja.taint.track(..., mode='sink', searchAllSources=true)` 允许 source 为空，并会按最新 `rules/source.json` 选择 source；`onlyFromWeb` 仅在该模式下生效
  - `ja.path.shortest` / `ja.path.from_to` / `ja.taint.track` 默认只遍历调用边；可选追加 `traversalMode='call+alias'`，把 `ALIAS` 一并纳入路径搜索
  - pruned 搜索命中时，`evidence` 中会带 `search backend: graph-pruned`
  - 内置脚本和用户查询都要求使用原生 `CALL ... YIELD ... RETURN ...`；旧式 `CALL ja.path.shortest(...) RETURN *` 不再兼容
  - 内置函数：`ja.isSource(node)` `ja.isSink(node)` `ja.sinkKind(node)` `ja.relGroup(typeOrRel)` `ja.relSubtype(typeOrRel)` `ja.ruleVersion()` `ja.rulesFingerprint()` `ja.ruleValidation()` `ja.ruleValidationIssues(scope)`
  - 只读 `apoc.*` whitelist 默认包含：
    `apoc.coll.contains` `apoc.coll.containsAll` `apoc.coll.toSet` `apoc.coll.intersection` `apoc.coll.subtract` `apoc.coll.flatten`
    `apoc.map.fromPairs` `apoc.map.fromLists` `apoc.map.values` `apoc.map.merge` `apoc.map.mergeList` `apoc.map.get` `apoc.map.removeKeys`
    `apoc.text.indexOf` `apoc.text.replace` `apoc.text.split` `apoc.text.join` `apoc.text.clean` `apoc.text.urlencode` `apoc.text.urldecode`
  - 查询对象固定为当前 active project
  - active project 构建中会返回 `project_build_in_progress`

- `POST /api/query/cypher/explain`
  Body:
  ```json
  {
    "query": "MATCH (n:JANode) RETURN n LIMIT 10"
  }
  ```
  说明:
  - 解释对象固定为当前 active project
  - active project 构建中会返回 `project_build_in_progress`

- `GET /api/query/cypher/capabilities`
  返回当前 Cypher 能力、过程列表、函数列表、支持的 options/profile，以及当前 `procedureMode/apocMode/apocWhitelistMode/apocWhitelist` 策略信息；`ruleValidation` 会显式返回 `model/source/modelSource/sink` 四块规则校验结果（compiled/rejected/errors/warnings），不再只依赖日志观察 DSL 跳过情况。

- Cypher Workbench 图视图补充
  - 结果图投影不再只识别 `src_id/dst_id`、`node_ids/edge_ids`；普通 Cypher 返回的 `Node/Relationship/Path` 及其嵌套 map/list 结果都会自动投影到 `Graph` 视图
  - 纯节点结果（无边）同样可以直接在 `Graph` 视图查看
  - Workbench 顶部支持 `调用图 / 结构图` 模式切换：调用图模式默认强调 `Method + CALL/ALIAS`；结构图模式默认强调 `Class + HAS/EXTEND/INTERFACES`，内置模板也会同步切到类结构浏览，不会把结构边直接污染默认调用图视图
  - 方法节点默认只保留结构标签（`JANode;Method`）；`Source/SourceWeb` 这类语义不再作为官方查询入口，推荐 `MATCH (n:Method) WHERE ja.isSource(n) RETURN n`
  - `source_flags` 仍属于建库期元数据；`ja.isSource` 和 Workbench 的节点语义摘要固定按“当前规则 + 当前项目稳定 Web 入口”动态判定，不再回退旧 `source_flags`
  - 边底层仍存 `CALLS_*`，另有规则驱动导出的 `ALIAS`；Workbench 默认把调用边聚合显示为 `CALL`，`ALIAS` 作为独立关系类别展示；切到“细分”模式才展示 `DIRECT/DISPATCH/REFLECTION/...`，右侧 inspector 会同时展示关系类别、关系子类与 `alias_kind`
  - 同一项目库也会写入 `Class` 节点与 `HAS/EXTEND/INTERFACES` 结构边；只有结构图模式或显式结构查询模板会突出这些关系，默认主舞台仍是方法调用图
  - Graph inspector 中的可点击属性会优先定位对应 table 行/列并高亮，同时把条件片段插入查询编辑器当前光标位置；`display_rel_type` / `rel_subtype` 会分别插入 `ja.relGroup(type(r)) = ...` / `ja.relSubtype(type(r)) = ...`
  - Table 行会反向关联出当前行涉及的 graph 节点/边并做高亮；双击行可直接切回 `Graph` 查看；Overview 中的结构标签 / 关系类别 / 关系子类 legend 会生成显式可清除的 graph filter chips，并同步向查询编辑器插入片段
  - 内置模板新增 `查看 Alias 关系`；结构模板与调用模板按当前图模式切换

- `GET /api/security/rule-validation`
  直接返回当前 `model/source/modelSource/sink` 规则校验摘要，以及按 `scope=all|model|source|sink` 过滤后的扁平 issue 列表。非法 `scope` 会返回 `rule_validation_scope_invalid`。适合 GUI、脚本和运维检查直接读取，不需要先走 capabilities。GUI 的独立规则校验对话框（`Start` 面板、`Tools -> 规则校验...`）以及 `Search -> Java 漏洞` / `Chains` 面板都会直接消费同一套摘要/issue 视图。

- 兼容下线:
  - `/api/query/sql` 已删除

### 项目生命周期
- GUI 侧推荐通过启动欢迎页做项目选择（临时项目 / 新建项目 / 打开项目）。
- API 侧保留 `register/switch/delete` 作为自动化入口。

- `GET /api/projects`
  返回 PERSISTENT 项目列表 + 当前 active project（可能是 TEMP）。
- `GET /api/projects/active`
  返回当前 active project 详情（包含 `type` 字段：`TEMP|PERSISTENT`）。
- `POST /api/projects/register`
  Body:
  ```json
  {
    "alias": "demo",
    "inputPath": "/abs/path/app.jar",
    "runtimePath": "/abs/path/jdk",
    "resolveNestedJars": true
  }
  ```
- `POST /api/projects/switch`
  Body:
  ```json
  {
    "projectKey": "a1b2c3d4e5f6a7b8"
  }
  ```
  说明：
  - 传 PERSISTENT 的 `projectKey`：切换到正式项目
  - 传 TEMP 键（`temp-<session-id>`）：切换/返回当前会话临时项目
- `DELETE /api/projects/{projectKey}?deleteStore=true|false`
  `deleteStore=true` 时同时删除该项目的本地 Neo4j store。
