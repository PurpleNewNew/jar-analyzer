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
- `SQLite`：对本地 `jar-analyzer.db` 执行自定义 SQL 查询（适合做临时统计/排查）。
- `Encode/Decode`：常见编码/解码、加密/解密辅助（用于快速还原常见混淆/编码片段）。
- `Listener`：监听端口并通过 socket 收发（用于测试一些网络交互场景）。
- `SerUtil`：从 Java 序列化数据中提取/分析字节码片段（只做静态解析，不执行目标代码）。
- `BCEL Util`：解析/转换 BCEL 相关字节码表达。
