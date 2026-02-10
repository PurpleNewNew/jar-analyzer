# n8n 自动化（基于 MCP）

本目录提供基于 n8n 的工作流示例，用于把 Jar Analyzer 的多个 MCP Lines 串起来做自动化审计，并可选把发现上报到 `report` 线（配合 Report Web UI 查看）。

特别致谢（工作流思路来源）：

- whw1sfb (https://github.com/whwlsfb)
- L-codes (https://github.com/L-codes)
- osword (https://github.com/zhzhdoai)

## 前置条件（Jar Analyzer 侧）

1. 已完成建库（本地存在 `jar-analyzer.db`）
2. GUI 已启动 HTTP API（默认 `10032`）
3. GUI -> `API` Tab -> `MCP` 中启动以下 Lines（按工作流需要）：
   - `audit-fast`（20033）
   - `graph-lite`（20034）
   - `dfs-taint`（20035）
   - `sca-leak`（20036）
   - `vul-rules`（20037）
   - `report`（20081，用于上报，可选但推荐开启）
4. 可选开启 `Report Web UI`（默认 `20080`），用于查看上报历史/实时推送

> 提示：示例工作流默认使用 MCP 的 `SSE` 传输（连接地址以 `/sse` 结尾）。

## 0x01 部署 n8n（Docker 示例）

> n8n 镜像/参数以官方文档为准，这里给出一个最小示例。

```bash
docker pull n8nio/n8n:latest

docker run -it --name n8n -p 5678:5678 \
  -e N8N_HOST="0.0.0.0" \
  -e N8N_PORT=5678 \
  -e N8N_PROTOCOL="http" \
  -e GENERIC_TIMEZONE="Asia/Shanghai" \
  -e N8N_SECURE_COOKIE=false \
  n8nio/n8n:latest
```

启动后访问：`http://127.0.0.1:5678/`

## 0x02 导入工作流

本目录包含两个示例：

- `doc/n8n/jar-analyzer-workflow.json`
- `doc/n8n/jar-analyzer-workflow-with-redis.json`

在 n8n 界面中选择 `Import from file` 导入即可。

## 0x03 配置 Global Constants（强烈建议）

示例工作流使用 `Global Constants` 节点读取统一配置（常见做法是安装 `n8n-nodes-globals` 插件后使用）。

建议配置以下常量（**注意以 `/` 结尾**）：

```json
{
  "jar-analyzer-api": "http://127.0.0.1:10032/",
  "jar-analyzer-api-token": "",

  "jar-analyzer-mcp-audit-fast": "http://127.0.0.1:20033/",
  "jar-analyzer-mcp-graph-lite": "http://127.0.0.1:20034/",
  "jar-analyzer-mcp-dfs-taint": "http://127.0.0.1:20035/",
  "jar-analyzer-mcp-sca-leak": "http://127.0.0.1:20036/",
  "jar-analyzer-mcp-vul-rules": "http://127.0.0.1:20037/",

  "report-service-mcp": "http://127.0.0.1:20081/"
}
```

说明：

- 如果你启用了 Jar Analyzer API 鉴权，把 `jar-analyzer-api-token` 填上；工作流会把它作为 `Token` 头发送给 `/api/*`。
- 如果你启用了 Jar Analyzer MCP 鉴权，需要在每个 `MCP *` 节点中配置请求头 `Token: <MCP_TOKEN>`（不同 n8n 版本/节点实现的设置项可能不同；最省事的做法是先关闭 MCP Auth）。

## 0x04 配置 LLM / 凭据

导入后需要在 n8n 中配置对应的 LLM 凭据（示例中使用了 `@n8n/n8n-nodes-langchain` 相关节点）。

## 0x05 运行与查看报告

如果启用了：

- `report` 线：工作流会调用 MCP `report` 工具上报发现
- `Report Web UI`：可在浏览器打开 `http://127.0.0.1:20080/` 查看历史与实时推送

## 常见问题

- **连接 MCP 失败**：检查端口是否启动、是否被占用、地址是否以 `/` 结尾并拼接了 `sse`。
- **API 返回 401/need token**：确认是否开启了 API/MCP 鉴权，以及对应 Token 是否配置到工作流/节点里。
- **无结果**：通常是还没建库，或分析的 DB 不是当前目录下的 `jar-analyzer.db`。

