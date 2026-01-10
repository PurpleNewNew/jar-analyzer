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

当你确认已完成分析并且 `api` 可用时，即可使用 `jar-analyzer-mcp`

## 启动 MCP

下载好对应版本的 `MCP` 可执行文件（或自行编译）

直接在命令行运行即可，默认端口 `20032`

你也可以自行指定 `MCP 端口` 和 `jar-analyzer api` 地址

```shell
mcp.exe -port 20032 -url http://127.0.0.1:10032
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
    "jar-analyzer-mcp": {
      "url": "http://127.0.0.1:20032/sse"
    }
  }
}
```

### Streamable HTTP（如 Codex CLI 等客户端）

如果客户端使用 `streamable-http` 传输，请使用 `/mcp` 端点：

```json
{
  "mcpServers": {
    "jar-analyzer-mcp": {
      "url": "http://127.0.0.1:20032/mcp"
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

以下为 MCP 工具清单（按类别整理），以 MCP 客户端可用列表为准。

### Jar/路径

- `get_jars_list`：查询所有输入的 JAR 文件
- `get_jar_by_class`：根据类名查询归属 JAR
- `get_abs_path`：获取 CLASS 文件的本地绝对路径

### 方法/类

- `get_methods_by_class`：查询指定类中的所有方法信息
- `get_methods_by_str`：搜索包含指定字符串的方法（模糊）
- `get_class_by_class`：查询类的基本信息

### 调用关系

- `get_callers`：查询方法的所有调用者
- `get_callers_like`：模糊查询方法的调用者
- `get_callee`：查询方法的被调用者
- `get_method`：精确查询方法
- `get_method_like`：模糊查询方法
- `get_impls`：查询接口/抽象方法的实现
- `get_super_impls`：查询父类/接口的实现

### Spring

- `get_all_spring_controllers`：列出所有 Spring 控制器类
- `get_spring_mappings`：查询某控制器的映射方法

### Java Web

- `get_all_filters`：列出所有 Filter 实现类
- `get_all_servlets`：列出所有 Servlet 实现类
- `get_all_listeners`：列出所有 Listener 实现类

### 反编译

- `get_code_fernflower`：Fernflower 反编译方法代码
- `get_code_cfr`：CFR 反编译方法代码

### 资源文件

- `get_resources`：资源文件列表（支持过滤/分页）
- `get_resource`：读取资源文件内容
- `search_resources`：搜索资源文件内容

### DFS / 污点分析

- `get_sinks`：获取内置 SINK 规则列表
- `get_dfs_chains`：DFS 调用链分析
- `taint_analyze`：DFS + 污点分析验证

## 安全

`1.1.0` 版本的 `MCP` 支持设置各种 `Token` 使用

在 `jar-analyzer` 端添加启动参数：

```shell
java -jar jar-analyzer.jar gui -sa -st YOUR_API_TOKEN
```

在 `MCP` 端设置 `jar-analyzer-api` 的 `Token`

```shell
mcp.exe -ja -jt YOUR_API_TOKEN
```

同时 `MCP` 也可以设置 `Token`

```shell
mcp.exe -auth -token YOUR_MCP_TOKEN -ja -jt YOUR_API_TOKEN
```

在 `MCP Client` 端设置即可

![](../mcp-img/018.png)

