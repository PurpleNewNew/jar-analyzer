## advance 模块（进阶功能）

`advance` Tab 集中放置两类能力：

1. **分析类工具**：对“当前选中的方法”做更深入的字节码/控制流/栈帧展示。
2. **内置小工具（Plugins）**：一些审计/逆向常用的辅助功能。

> 提示：这些功能都建立在“先建库”的流程之上。请先在 `start` Tab 完成分析。

## Analysis

以下按钮会基于“当前打开的类/方法”工作：

- `Show CFG`：生成控制流图（CFG），用于理解分支/循环/异常结构。
- `HTML Graph`：生成 HTML 图形化报告（通常输出到 `jar-analyzer-document/` 目录）。
- `Simple Frame`：展示简化版栈帧/局部变量栈变化。
- `Full Frame`：展示更完整的栈帧/局部变量栈变化（更重，适合需要精确跟踪时使用）。

## Plugins

`Plugins` 区域是“可独立使用的小工具”，常见用途如下：

- `Spring EL search`：帮助定位/分析 Spring 表达式（SPEL）相关用法。
- `Cypher Workbench`：对当前活动项目的 Neo4j 图执行 Cypher 查询。高频导航仍走内存 `GraphSnapshot`，Workbench 中的分析型查询走 Neo4j 原生引擎，可直接使用 `ja.path.*` / `ja.taint.track` / `ja.isSink` / `ja.isSource` / `ja.relGroup` / `ja.relSubtype` 等内置过程与函数；其中 `*_pruned` 会启用基于 summary 的状态剪枝，适合路径爆炸场景，显式传入 `call+alias` 时会把 `ALIAS` 一并纳入路径搜索，也可显式传入 `direction=forward|backward|bidirectional` 控制正向、逆向或双向搜索心智。Workbench 顶部提供显式的 `CALL / CALL + ALIAS` 遍历切换；内置 `ja.path.*` 模板与用户查询可通过 `{{TRAVERSAL_MODE_LITERAL}}` 占位符绑定当前选择。Workbench 同时内置本地只读 `apoc.coll.*` / `apoc.map.*` / `apoc.text.*` 白名单函数，普通 Cypher 返回的 `Node/Relationship/Path` 也会自动投影到 `Graph` 视图；默认主舞台仍是方法调用图，只强调 `Method + CALL/ALIAS`，切到结构图模式时才突出 `Class + HAS/EXTEND/INTERFACES`。方法节点默认只展示结构标签，安全语义改由右侧 inspector 和 `ja.*` 动态判定呈现，关系边默认聚合显示为 `CALL`，`ALIAS` 作为独立关系类别显示，可切换细分模式查看 `DIRECT/DISPATCH/...`。更细的查询能力、规则校验与 `pruningPolicy` 说明见 [`doc/README-api.md`](/Users/veritas/Documents/projects/jar-analyzer/doc/README-api.md)。
- `Encode/Decode`：常见编码/解码、加密/解密辅助（用于快速还原常见混淆/编码片段）。
- `Listener`：监听端口并通过 socket 收发（用于测试一些网络交互场景）。
- `SerUtil`：从 Java 序列化数据中提取/分析字节码片段（只做静态解析，不执行目标代码）。
- `BCEL Util`：解析/转换 BCEL 相关字节码表达。
