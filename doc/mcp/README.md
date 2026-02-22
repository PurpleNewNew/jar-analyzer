# MCP（内置）

MCP 是 Jar Analyzer 的内置自动化接口（**Java 实现**，无需额外进程）。它把常用审计能力封装为 MCP tools，便于在 AI 客户端、脚本或工作流平台中调用。

## 前置条件

1. 已完成建库（本地存在 `db/neo4j-projects/<project-key>/`）
2. GUI 已启动内置 HTTP API（默认 `10032`，可在启动参数中修改）

> 说明：除 `report` 线以外，其它 MCP 线底层调用的是 Jar Analyzer 的 `/api/*` 逻辑（进程内调用，不走真实网络），因此 API 鉴权开启时 MCP 会自动携带 API Token。

## 在 GUI 中启动

GUI -> `API` Tab -> `MCP` 区域：

1. 配置 `Bind`（默认 `0.0.0.0`）
2. 可选开启 `Enable MCP Auth` 并设置 `Token`
3. 勾选需要的 MCP Lines，配置端口
4. 点击 `Apply + Start Enabled`

默认 Lines 与端口：

- `audit-fast`: `20033`
- `graph-lite`: `20034`
- `dfs-taint`（GUI 中显示为 `dfs`）: `20035`
- `sca-leak`: `20036`
- `vul-rules`: `20037`
- `report`: `20081`（可选）

可选组件：

- `Report Web UI`：默认 `http://127.0.0.1:20080/`（在 GUI 中单独勾选启用）

## 连接方式

Jar Analyzer MCP 同时提供两种传输：

1. `SSE`：`http://127.0.0.1:<port>/sse`
2. `streamable-http`：`http://127.0.0.1:<port>/mcp`

## 协议严格化（rmclassic）

1. `initialize` 必须携带合法 `protocolVersion`（缺失/非法返回 `INVALID_PARAMS`）
2. `tools/call` 访问未知工具返回 JSON-RPC `METHOD_NOT_FOUND`
3. Flow 相关工具固定 graph 后端；不再存在 classic fallback 或 seed 参数

## 客户端配置示例

### SSE（Cherry Studio 等）

```json
{
  "mcpServers": {
    "jar-analyzer-audit-fast": { "url": "http://127.0.0.1:20033/sse" },
    "jar-analyzer-graph-lite": { "url": "http://127.0.0.1:20034/sse" },
    "jar-analyzer-dfs-taint": { "url": "http://127.0.0.1:20035/sse" },
    "jar-analyzer-sca-leak": { "url": "http://127.0.0.1:20036/sse" },
    "jar-analyzer-vul-rules": { "url": "http://127.0.0.1:20037/sse" },
    "jar-analyzer-report": { "url": "http://127.0.0.1:20081/sse" }
  }
}
```

### Streamable HTTP（Codex CLI 等）

```json
{
  "mcpServers": {
    "jar-analyzer-audit-fast": { "url": "http://127.0.0.1:20033/mcp" },
    "jar-analyzer-graph-lite": { "url": "http://127.0.0.1:20034/mcp" },
    "jar-analyzer-dfs-taint": { "url": "http://127.0.0.1:20035/mcp" },
    "jar-analyzer-sca-leak": { "url": "http://127.0.0.1:20036/mcp" },
    "jar-analyzer-vul-rules": { "url": "http://127.0.0.1:20037/mcp" },
    "jar-analyzer-report": { "url": "http://127.0.0.1:20081/mcp" }
  }
}
```

## 工具列表（按线划分）

说明：具体工具以客户端 `tools/list` 展示为准。这里给出“按线划分”的常见范围，便于选线降噪。

### audit-fast（全量审计）

- 项目生命周期：`project_list` `project_active` `project_register` `project_switch` `project_remove`
- 元信息/类：`jar_list` `jar_resolve` `class_info`
- 入口/路由：`entrypoints_list` `spring_mappings`
- 方法检索：`methods_search` `methods_impls`
- 调用图：`callgraph_edges` `callgraph_by_sink`
- 证据：`code_get`
- 资源：`resources_list` `resources_get` `resources_search`
- 语义：`semantic_hints` `config_usage`
- 安全：`vul_rules` `vul_search` `sca_scan` `leak_scan` `gadget_scan`
- 流程：`flow_start` `flow_job`

### graph-lite（轻量调用图）

- `project_list` `project_active` `project_switch`
- `callgraph_edges` `callgraph_by_sink`
- `semantic_hints`
- `jar_list` `jar_resolve` `class_info`
- `methods_search` `methods_impls`

### dfs-taint（异步链路）

- `project_list` `project_active` `project_switch`
- `flow_start` `flow_job`
- `code_get`（链路取证用）
- 说明：
  - Flow 后端固定 graph（不再提供 classic fallback）
  - taint seed 参数已移除，改为自动端口推断
  - `flow_start` / `query_cypher` / `cypher_explain` / `taint_chain_cypher` 支持可选 `projectKey`

### Cypher 工具

- `query_cypher`：只读 Cypher 查询（支持 `params/options/projectKey`）
- `cypher_explain`：解释计划（支持 `projectKey`）
- `taint_chain_cypher`：基于 `CALL ja.taint.track(...)` 的全局数据流跟踪（支持 `projectKey`）

### 项目管理工具

- `project_list`：列出全部项目及 active project
- `project_active`：查询当前 active project
- `project_register`：注册项目并设为 active
- `project_switch`：切换 active project
- `project_remove`：删除项目（可选 `deleteStore=true` 删除本地 Neo4j store）

### sca-leak / vul-rules（安全扫描）

- `vul_rules` `vul_search`
- `sca_scan` `leak_scan` `gadget_scan`

### report（上报）

- `report`：接收并持久化报告数据（并可通过 Report Web UI 查看历史/推送）

## 鉴权（两套 Token）

1. **API Token**（保护 `/api/*`）：GUI 启动参数 `-sa -st <TOKEN>`，API 请求头 `Token: <TOKEN>`
2. **MCP Token**（保护 MCP 的 `tools/call`）：在 GUI `MCP` 区域勾选 `Enable MCP Auth` 并设置 Token；客户端请求头同样使用 `Token: <MCP_TOKEN>`

注意：API 与 MCP 是不同端口的服务，虽然 Header 名都叫 `Token`，但它们的 Token 值相互独立。

## 常见问题

- **启动失败/无响应**：优先检查端口占用；换端口后点击 `Apply + Start Enabled`。
- **工具返回无数据**：通常是当前 active project 尚未建库，或切到了错误的 `projectKey`。
- **鉴权失败**：如果开启了 MCP Auth，客户端必须带 `Token`；如果开启了 API Auth，API 请求必须带 `Token`（MCP 线会自动携带，无需客户端额外设置）。
