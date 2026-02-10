# jar-analyzer-mcp

## 功能展示

![](../mcp-img/001.png)

![](../mcp-img/002.png)

![](../mcp-img/003.png)

## 前置工作

使用 MCP 前必须确保你的项目已经分析完毕。

![](../mcp-img/005.png)

选择 `Jar`，点击 `Start` 等待完成分析。

`jar-analyzer api` 默认开放在 `127.0.0.1:10032`。

![](../mcp-img/006.png)

你也可以在启动时使用 `java -jar jar-analyzer.jar gui --port 10033` 指定其他端口。

当你确认已完成分析并且 `api` 可用时，即可使用分线 MCP（主 MCP 已取消）：
- audit-fast
- graph-lite
- dfs-taint
- sca-leak
- vul-rules

## 启动 MCP

当前版本 MCP 已内置在 Jar Analyzer 中，无需额外下载/编译 Go 可执行文件。

启动方式：

1. 启动 Jar Analyzer GUI（确保分析完成且 API 可用）
2. 打开「API」Tab
3. 在 `MCP` 区域开启需要的分线并设置端口
4. 点击 `Apply + Start Enabled`

默认端口如下（可自行修改）：

- audit-fast: `20033`
- graph-lite: `20034`
- dfs-taint: `20035`
- sca-leak: `20036`
- vul-rules: `20037`

如启动失败，通常是端口被占用；请在面板里换端口后重试。

## 配置 MCP

当前版本同时支持 `SSE` 与 `streamable-http` 两种传输方式。
以 `Cherry Studio` 为例进行配置。

请勾选“服务器发送事件（SSE）”，并且 `URL` 使用 `/sse` 结尾。

![](../mcp-img/007.png)

或者使用 `JSON` 配置：

```json
{
  "mcpServers": {
    "jar-analyzer-audit-fast": {
      "url": "http://127.0.0.1:20033/sse"
    },
    "jar-analyzer-graph-lite": {
      "url": "http://127.0.0.1:20034/sse"
    },
    "jar-analyzer-dfs-taint": {
      "url": "http://127.0.0.1:20035/sse"
    },
    "jar-analyzer-sca-leak": {
      "url": "http://127.0.0.1:20036/sse"
    },
    "jar-analyzer-vul-rules": {
      "url": "http://127.0.0.1:20037/sse"
    }
  }
}
```

### Streamable HTTP（如 Codex CLI 等客户端）
如果客户端使用 `streamable-http` 传输，请使用 `/mcp` 端点：
```json
{
  "mcpServers": {
    "jar-analyzer-audit-fast": {
      "url": "http://127.0.0.1:20033/mcp"
    },
    "jar-analyzer-graph-lite": {
      "url": "http://127.0.0.1:20034/mcp"
    },
    "jar-analyzer-dfs-taint": {
      "url": "http://127.0.0.1:20035/mcp"
    },
    "jar-analyzer-sca-leak": {
      "url": "http://127.0.0.1:20036/mcp"
    },
    "jar-analyzer-vul-rules": {
      "url": "http://127.0.0.1:20037/mcp"
    }
  }
}
```

右上角开启 `MCP`：

![](../mcp-img/008.png)

使用 `MCP` 时请开启：
![](../mcp-img/009.png)

接下来就可以正常使用了：
![](../mcp-img/004.png)

## 工具列表

以下是 MCP 工具清单（按分线整理，以 MCP 客户端展示为准）。

### audit-fast（全量审计）
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
- `callgraph_edges` `callgraph_by_sink`
- `semantic_hints`
- `jar_list` `jar_resolve` `class_info`
- `methods_search` `methods_impls`

### dfs-taint（异步链路）
- `flow_start` `flow_job`
- `code_get`（链路取证）

### sca-leak / vul-rules（安全扫描）
- `vul_rules` `vul_search`
- `sca_scan` `leak_scan` `gadget_scan`

## 安全

当前版本同时支持：

1. `jar-analyzer-api` 鉴权（API Token）
2. `jar-analyzer-mcp` 鉴权（MCP Token，仅对 `tools/call` 生效）

开启 `jar-analyzer-api` 鉴权（可选）：

```shell
java -jar jar-analyzer.jar gui -sa -st YOUR_API_TOKEN
```

开启 `MCP` 鉴权：

- 打开 Jar Analyzer GUI「API」Tab -> `MCP`
- 勾选 `Enable MCP Auth` 并设置 Token

在 `MCP Client` 端设置 Token Header（不同客户端配置方式不同）：
![](../mcp-img/018.png)

可以把这部分加进 `AGENTS.md`，提高 AI 的使用效果。

## 使用建议（推荐流程）

### 1) 快速找到高危入口（首选）
- 入口枚举：`entrypoints_list`
- DFS/taint：`flow_start` + `flow_job`
- 证据确认：`code_get`

### 2) 降噪 & 补全调用关系
- `callgraph_edges` / `callgraph_by_sink`
- 必要时用 `scope=all` 覆盖 JDK/三方库

### 3) 资源证据补齐（SQL/模板/配置）
- `resources_search` / `resources_get`

### 4) 依赖风险补充
- `sca_scan` / `gadget_scan`

### 5) 自动化审计（n8n 等）
- 将 `flow_start` + `flow_job` 串成固定链路流程
