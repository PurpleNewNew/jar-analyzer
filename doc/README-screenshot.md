## GUI 导览（文字版）

Jar Analyzer 的主流程是“先建库再分析”。大多数 Tab 都依赖当前活动项目的 Neo4j 图库，因此请先在 `start` Tab 完成一次分析/建库。

## start

用于选择输入并构建数据库：

- 选择输入：`jar/war/目录(classes)`
- 常用选项：解析 fatjar 内嵌依赖（jars in jar）、自动保存、删除旧缓存、rt.jar 相关选项等
- 构建完成后会显示类/方法/边数量、数据库大小等信息

## search

用于检索与定位（审计最常用）：

- 方法定义检索（类名/方法名/描述符，支持精确/模糊）
- 方法调用检索（caller/callee 方向追踪）
- 字符串包含检索（定位关键常量、危险关键字、配置项）
- 二进制搜索（按需使用）
- 表达式搜索：更强的组合筛选（见 `doc/README-el.md`）

## call / impl

用于调用关系与分派补全后的跳转：

- `call`：查看 caller/callee（调用图边）
- `impl`：查看接口实现/父类子类关系下的方法实现与上溯

## web

用于 Web 入口枚举与映射：

- Spring Controller / Interceptor
- Servlet / Filter / Listener
- 支持导出为 JSON/TXT/CSV，方便离线整理与报告

## sca / leak / gadget

安全相关扫描能力：

- `sca`：依赖风险扫描（第三方组件、典型高危依赖等）
- `leak`：敏感信息/泄露线索扫描（按规则集）
- `gadget`：gadget/反序列化相关线索与依赖分析（规则见 `src/main/resources/gadget.dat`）

## chains

链路分析（DFS + 可选污点验证）：

- 支持从 `Sink` 反推（更常用）或从 `Source` 出发
- `高级设置` 中可配置深度、数量限制、导出等
- 可选勾选“污点分析验证”，对 DFS 结果做字节码级传播确认

## advance

进阶分析与内置小工具集合（CFG/Frame/HTML Graph、Cypher Workbench、自定义工具等）。详见 `doc/README-advance.md`。

## API

展示内置 HTTP API 的运行信息，并在同一处提供 MCP 的开关与配置：

- HTTP API 文档：`doc/README-api.md`
- MCP 文档：`doc/mcp/README.md`
- n8n/自动化教程：`doc/n8n/README.md`
