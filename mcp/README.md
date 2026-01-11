## jar-analyzer-mcp

请使用 `golang 1.24` 以上版本编译

为什么不用 `Java` 写 `MCP`

因为官方 `Java MCP SDK` 需要 `Java 17` 这和 `Jar Analzyer` 最低要求冲突

而我不想手搓或者用小众的基础库，于是选择支持平台多且容易部署的 `golang`

### 分线 MCP

主 MCP 已取消，改为 5 条分线 MCP（每条线一个服务端）：

- audit-fast（入口 + 证据 + 资源）默认端口 `20033`
- graph-lite（调用图 + 方法索引）默认端口 `20034`
- dfs-taint（异步深挖）默认端口 `20035`
- sca-leak（依赖/泄露）默认端口 `20036`
- vul-rules（规则/筛选）默认端口 `20037`

示例：

```shell
jar-analyzer> mcp-audit-fast.exe -port 20033 -url http://127.0.0.1:10032
jar-analyzer> mcp-graph-lite.exe -port 20034 -url http://127.0.0.1:10032
jar-analyzer> mcp-dfs.exe -port 20035 -url http://127.0.0.1:10032
jar-analyzer> mcp-sca-leak.exe -port 20036 -url http://127.0.0.1:10032
jar-analyzer> mcp-vul-rules.exe -port 20037 -url http://127.0.0.1:10032
```

所有分线 MCP 的启动参数一致：

- `-auth` 启用 MCP 鉴权
- `-token` MCP token
- `-ja` 启用 jar-analyzer-api token
- `-jt` jar-analyzer-api token
- `-port` 监听端口
- `-url` Jar Analyzer URL

### report mcp

这是为了配合 `n8n workflow` 而存在的 `agent`

参考文档：[n8n-doc](../n8n-doc)

![](../mcp-img/012.png)

```shell
jar-analyzer> report_mcp_v1.0.0_windows_amd64.exe -h
  -debug
        enable debug mode
  -port int
        mcp port to listen on (default 20081)
  -web-host string
        mcp web server host (default "127.0.0.1")
  -web-port int
        web server port to listen on (default 20080)
```

为什么要配置 `mcp web server host`

因为：`web` 端使用 `websocket` 连接 `web server port`

需要确保本地浏览器可以访问通这个 `host:port`
