# `bytecode-mainline / oracle-taie` Profile 切换口径（JA-NT-107）

本文档补充 [Phase 0 实施文档](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie-phase0.md) 中的 `JA-NT-107`，并与当前代码保持一致。

## 1. 入口约定

首选入口：

- `jar.analyzer.callgraph.profile`

兼容入口：

- `jar.analyzer.callgraph.engine`
- `jar.analyzer.analysis.profile`

优先级：

1. 如果显式设置了 `jar.analyzer.callgraph.engine`，优先按 engine 走。
2. 否则读取 `jar.analyzer.callgraph.profile`。
3. 如果两者都没设置，当前默认仍回到 `taie`。

## 2. Profile 到 pipeline 的映射

| profile | call_graph_engine | call_graph_mode | selective PTA | 用途 |
| --- | --- | --- | --- | --- |
| `fast` | `bytecode-mainline` | `bytecode:fast-v1` | 否 | 快速建库、先看主调用链 |
| `balanced` | `bytecode-mainline+pta-refine` | `bytecode:balanced-v1` | 是 | 未来默认 no-taie 主链候选 |
| `precision` | `bytecode-mainline+pta-refine` | `bytecode:precision-v1` | 是 | 在同一 bytecode 主链下追求更高精度 |
| `oracle-taie` | `oracle-taie` | `oracle-taie:<taie-profile>` | 否 | 把 Tai-e 限定为 oracle/对照模式 |

说明：

- `precision` 仍然属于 bytecode 主链，不重新引入第二条 PTA 全量主链。
- `oracle-taie` 的 Tai-e context profile 仍由 `jar.analyzer.analysis.profile` 控制，例如 `fast/balanced/high`。

## 3. Engine 兼容映射

为了兼容现有调用方式，当前仍保留以下 engine：

| engine | 行为 |
| --- | --- |
| `taie` | 旧默认主链，`call_graph_mode=taie:<profile>` |
| `bytecode-mainline` | 兼容旧实验入口，走 `bytecode:semantic-v1` |
| `bytecode-mainline+pta-refine` | 走 `bytecode:balanced-v1` |
| `oracle-taie` | 走 `oracle-taie:<taie-profile>` |

兼容口径：

- engine 只作为迁移期 escape hatch。
- profile 才是后续 GUI/API 真正应该暴露的切换面。

## 4. 当前代码落点

- 解析入口：`src/main/java/me/n1ar4/jar/analyzer/core/CallGraphPlan.java`
- 主链接线：`src/main/java/me/n1ar4/jar/analyzer/core/CoreRunner.java`
- bytecode 主链：`src/main/java/me/n1ar4/jar/analyzer/core/BytecodeMainlineCallGraphRunner.java`

## 5. 最小验证

建议命令：

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=CallGraphPlanTest,CoreRunnerCallGraphProfileTest \
  test
```

当前验证点：

- `fast` 会落到 `bytecode-mainline / bytecode:fast-v1`
- `oracle-taie` 会落到 `oracle-taie / oracle-taie:<taie-profile>`
- `jar.analyzer.callgraph.engine` 显式设置时会覆盖 profile
