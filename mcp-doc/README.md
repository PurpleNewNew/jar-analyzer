# jar-analyzer-mcp

## 功能展示

![](../mcp-img/001.png)

![](../mcp-img/002.png)

![](../mcp-img/003.png)

## 前置工作

使用 `MCP` 必须确保你的项目已经分析完毕

![](../mcp-img/005.png)

选择 `Jar` 点击 `Start` 等待完成分析

`jar-analyzer api` 默认开在 `127.0.0.1:10032`

![](../mcp-img/006.png)

你也可以编辑启动脚本或者启动时使用 `java -jar jar-analyzer.jar gui --port 10033` 指定其他端口

当你确认已完成分析并且 `api` 可用时，即可使用 **分线 MCP**（主 MCP 已取消）：

- audit-fast
- graph-lite
- dfs-taint
- sca-leak
- vul-rules

## 启动 MCP

下载好对应版本的 `MCP` 可执行文件（或自行编译）

默认端口如下（可自行修改）：

- audit-fast: `20033`
- graph-lite: `20034`
- dfs-taint: `20035`
- sca-leak: `20036`
- vul-rules: `20037`

示例：

```shell
mcp-audit-fast.exe -port 20033 -url http://127.0.0.1:10032
mcp-graph-lite.exe -port 20034 -url http://127.0.0.1:10032
mcp-dfs.exe -port 20035 -url http://127.0.0.1:10032
mcp-sca-leak.exe -port 20036 -url http://127.0.0.1:10032
mcp-vul-rules.exe -port 20037 -url http://127.0.0.1:10032
```

如果启动不报错，说明已经完成启动

## 配置 MCP

当前版本同时支持 `SSE` 与 `streamable-http` 两种传输方式。

以 `Cherry Studio` 为例进行配置

请勾选 `服务器发送事件（SSE）` 并且 `URL` 使用 `/sse` 结尾

![](../mcp-img/007.png)

或者使用 `JSON` 配置

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

右上角开启 `MCP`

![](../mcp-img/008.png)

使用 `MCP` 时请开启

![](../mcp-img/009.png)

接下来就可以正常使用了

![](../mcp-img/004.png)

## 工具列表

以下为 MCP 工具清单（按分线整理），以 MCP 客户端可用列表为准。

### audit-fast（入口 + 证据 + 资源）

- 入口枚举：`get_all_spring_controllers` `get_all_spring_interceptors` `get_all_servlets` `get_all_filters` `get_all_listeners`
- 路由：`get_spring_mappings`
- 证据：`get_code_fernflower` `get_code_cfr`
- 资源：`search_resources` `get_resource` `get_resources`
- 元信息：`get_jars_list`
- 可选快扫：`get_methods_by_str`

### graph-lite（调用图 + 方法索引）

- 调用关系：`get_callers` `get_callee` `get_callers_batch` `get_callee_batch` `get_callers_like` `get_callers_by_sink`
- 方法检索：`get_method` `get_method_batch` `get_method_like`
- 类/实现关系：`get_class_by_class` `get_impls` `get_super_impls`
- 归属定位：`get_jar_by_class` `get_abs_path`

### dfs-taint（异步深挖）

- DFS：`get_sinks`（必须带过滤/分页参数）`get_dfs_chains` `get_dfs_job` `get_dfs_results` `cancel_dfs_job`
- Taint：`taint_job` `get_taint_job` `get_taint_results` `cancel_taint_job`

### sca-leak（依赖/泄露）

- `sca_scan` `leak_scan` `gadget_scan`

### vul-rules（规则/筛选）

- `get_vul_rules` `vul_search`

## 安全

`1.1.0` 版本的 `MCP` 支持设置各种 `Token` 使用

在 `jar-analyzer` 端添加启动参数：

```shell
java -jar jar-analyzer.jar gui -sa -st YOUR_API_TOKEN
```

在 `MCP` 端设置 `jar-analyzer-api` 的 `Token`

```shell
mcp-audit-fast.exe -ja -jt YOUR_API_TOKEN
```

同时 `MCP` 也可以设置 `Token`

```shell
mcp-audit-fast.exe -auth -token YOUR_MCP_TOKEN -ja -jt YOUR_API_TOKEN
```

在 `MCP Client` 端设置即可

![](../mcp-img/018.png)

可以把这个加入 AGENTS.md，提高 AI 的使用效率

## 使用建议（推荐流程）

### 1) 快速找高危链路（首选）
- 入口枚举：audit-fast
- DFS/taint：dfs-taint
- 证据确认：audit-fast

### 2) 降噪 & 提升准确度
- graph-lite 做调用关系补证
- dfs-taint 控制 depth/maxLimit

### 3) 资源证据补齐（SQL/模板/配置）
- audit-fast 的 search_resources / get_resource

### 4) 依赖风险补充
- sca-leak

### 5) 自动化/巡检（n8n）
- 先 DFS/taint 再上报（保证链路证据）
