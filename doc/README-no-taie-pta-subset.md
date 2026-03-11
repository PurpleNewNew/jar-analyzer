# `ContextSensitivePtaEngine` 最小可迁子集设计（JA-NT-105）

本文档对应 [Phase 0 实施文档](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie-phase0.md) 中的 `JA-NT-105`，目标是把 `dev` 分支里的 `ContextSensitivePtaEngine` 拆成“当前主干真正需要的最小 PTA 子集”，并明确：

- 哪些类和算法值得迁入
- 哪些边界必须收敛
- 哪些能力明确不迁
- PTA 在当前主干中的定位、输入、输出和验收方式

本文档只解决“迁什么、怎么裁”，不直接落代码。真正代码实施应以本文档为白名单。

## 1. 结论

结论先行：

- 要迁 PTA，但只迁成 `SelectivePtaRefiner`，不迁回 `dev` 里的全局 `pta mode`。
- 要保留的是 `PAG + 上下文方法 + 热点调用点精化` 这条最小主链，不是插件系统、系统属性矩阵和旧 `BuildContext` 耦合面。
- `JA-NT-105` 完成后，项目内对 PTA 的统一口径应当变成：
  - `bytecode-mainline` 先产出 `direct/dispatch/reflection/method_handle/callback/framework`
  - 只有少量热点调用点再进入 `SelectivePtaRefiner`
  - PTA 只补 `CALLS_PTA` 或提升现有边置信度，不重新定义整个调用图

一句话总结：

- 当前主干需要的是“局部精化引擎”
- 不是“第二条全量建库主链”

## 2. 为什么现在该开 `JA-NT-105`

前置条件已经满足：

1. `JA-NT-102` 已经把 `BuildFactSnapshot / BuildEdgeAccumulator` 的边界定义清楚
2. `JA-NT-103` 已经把 `InheritanceMap + Dispatch/TypedDispatch` 接到当前主链
3. `JA-NT-104` 已经把 `Reflection + Semantic Edge` 接到当前主链，并覆盖了 `loadClass/helper-flow`

这意味着：

- 便宜的边已经拿到了
- 剩下真正高价值的，是 field/array/object-flow 驱动的精化
- 再继续只补更多规则，收益会开始递减

因此现在引入 PTA 的合理定位，不是“替代前两步”，而是：

- 为已经存在但仍然过粗或缺失的热点调用点补精度

## 3. `dev` PTA 的真实价值与真实负担

### 3.1 真正值钱的部分

`dev` 分支 PTA 里最值得迁的是这些东西：

- `PointerAssignmentGraph`
  - 承载 `alloc / assign / invoke / field / array / synthetic call`
- `PtaAllocNode`
- `PtaVarNode`
- `PtaInvokeSite`
- `PtaSyntheticCall`
- `PtaContext`
- `PtaContextMethod`
- `ContextSensitivePtaEngine` 内部 solver 的：
  - worklist 驱动
  - points-to 传播
  - 调用点上下文化
  - field/array/native-copy 闭包
  - 局部反射/MethodHandle 精化

这些部分的共同特征是：

- 直接提升精度
- 算法价值明确
- 可以被当前主干的 facts 消费

### 3.2 真正沉重的部分

`dev` PTA 里最不该原样迁回来的，是这些东西：

- `PtaSolverConfig`
  - 系统属性面过宽，违反当前主干“收敛优先”
- `PtaPluginLoader`
  - 运行时反射装载插件，不符合当前主干的封闭式主链
- `PtaCompositePlugin`
  - 把 PTA 主流程做成事件总线，增加理解和测试成本
- `PtaPluginBridge`
  - 继续暴露“PTA 过程中随时加边”的开放扩展点
- `IncrementalPtaState`
  - 全局静态缓存模型和当前单活项目 build 版本边界不一致
- `PtaConstraintCheckerPlugin`
  - 调试壳，不是产品主能力
- `PtaOnTheFlySpringFrameworkPlugin`
- `PtaOnTheFlySemanticPlugin`
- `PtaOnTheFlyDynamicProxyPlugin`
- `PtaOnTheFlyReflectionPlugin`
  - 这些插件里有启发式价值，但插件壳本身不该迁

核心判断：

- PTA 值钱的是 solver
- 不是 plugin shell

## 4. 当前主干到底需要 PTA 解决什么

当前主干在 `JA-NT-104` 之后，已经具备：

- `direct`
- `declared-dispatch`
- `typed-dispatch`
- `CHA/RTA dispatch`
- `reflection`
- `method_handle`
- `callback`
- `framework`

因此 PTA 不应该再去解决这些已经覆盖的问题。  
当前主干需要 PTA 的地方，应该严格收敛为以下三类：

### 4.1 heap-flow 驱动的虚调用精化

主要是：

- field store/load
- array store/load
- `System.arraycopy`
- 简单局部赋值链

目标：

- 在 `typed-dispatch` 还不够时，补出更准的 receiver object 集合
- 为真实 callback/gadget/web 场景减少误报或补足漏报

### 4.2 热点反射/MethodHandle 调用点的对象侧精化

字符串解析已经在 `JA-NT-104` 里补了不少，但仍可能存在：

- receiver object 来自 heap-flow
- helper method 返回的对象经 field/array 传播
- 需要在局部对象图里确认调用链是否真实可达

目标：

- PTA 不负责重新做字符串求值
- PTA 只负责在对象侧补充精度

### 4.3 热点路径上的局部二次确认

PTA 的第三个价值是作为图路径上的局部 refiner，例如：

- 某条 gadget route 已经被 `ja.path.gadget` 找到
- 某个 Web -> sink 路径已经被 `ja.taint.track` 找到
- 此时只对路径上的少数调用点做局部 PTA 确认

目标：

- 不是“建库时全量重算”
- 而是“命中热点后做小范围补刀”

## 5. 新定位：`SelectivePtaRefiner`

`JA-NT-105` 的目标态命名建议直接改掉：

- 不再叫 `ContextSensitivePtaEngine`
- 改成 `SelectivePtaRefiner`

原因很简单：

- 现在的业务目标不是“全项目 context-sensitive PTA”
- 而是“对少量热点调用点做 context-sensitive refinement”

建议目标接口：

```java
final class SelectivePtaRefiner {
    Result refine(BuildFactSnapshot facts,
                  BuildEdgeAccumulator edges,
                  PtaRefineRequest request);
}
```

其中：

- `BuildFactSnapshot` 提供输入事实
- `BuildEdgeAccumulator` 提供 provisional edges 与边写回
- `PtaRefineRequest` 明确本次只精化哪些调用点/方法/路径

## 6. 最小可迁子集白名单

### 6.1 直接保留的算法/数据结构

这批类建议原理保留、代码可重写但语义不变：

| `dev` 类 | 处理建议 | 原因 |
| --- | --- | --- |
| `PointerAssignmentGraph` | 保留核心结构，改到当前包层级 | 这是 PTA 的最小约束图 |
| `PtaAllocNode` | 保留 | 基础对象节点 |
| `PtaVarNode` | 保留 | 基础变量节点 |
| `PtaInvokeSite` | 保留 | 调用点精化需要 |
| `PtaSyntheticCall` | 保留，但只留最小字段 | 桥接反射/MethodHandle/语义边 |
| `PtaContext` | 保留简化版 | 1-call-site 上下文足够 |
| `PtaContextMethod` | 保留简化版 | 作为 worklist 单元 |

### 6.2 需要拆迁后再接入的部分

| `dev` 类/部分 | 处理建议 | 当前主干对应位置 |
| --- | --- | --- |
| `ContextSensitivePtaEngine.buildConstraintIndex` | 拆成 `ConstraintFactAssembler` 过渡实现 | 未来并入 `JA-NT-106` 的 `BytecodeFactRunner` |
| `ContextSensitivePtaEngine.Solver` | 拆成 `SelectivePtaSolver` | `core/pta` 新主实现 |
| `ContextSensitivePtaEngine.Result` | 改成只输出当前主干真正消费的统计 | 继续接到 build metrics |
| `appendReflectionSyntheticCalls` | 只保留与对象精化直接相关部分 | 与 `JA-NT-104` 的 reflection 主链衔接 |
| `MethodCallMeta.record` 调用侧 | 保留 evidence/ctx 产出协议 | 不改 Neo4j/Graph 消费契约 |

### 6.3 明确不迁的部分

| `dev` 类/机制 | 裁决 | 理由 |
| --- | --- | --- |
| `PtaPluginLoader` | 不迁 | 运行时开放插件不符合当前主干 |
| `PtaCompositePlugin` | 不迁 | 事件总线放大复杂度 |
| `PtaPluginBridge` | 不迁 | 不再允许 PTA 内部任意扩边 |
| `PtaConstraintCheckerPlugin` | 不迁 | 调试壳 |
| `PtaOnTheFlySpringFrameworkPlugin` | 不迁 | 当前 `MethodSemanticSupport + SemanticEdgeRunner` 已覆盖主价值 |
| `PtaOnTheFlySemanticPlugin` | 不迁 | callback 规则已在主链 |
| `PtaOnTheFlyDynamicProxyPlugin` | 不迁 | proxy 规则已在主链 |
| `PtaOnTheFlyReflectionPlugin` | 不迁插件壳，只择优吸收启发式 | 避免重复第二条反射逻辑 |
| `IncrementalPtaState` | Phase 0 不迁 | 与当前 build/version 边界不一致 |
| 全量 `pta` callgraph mode | 不迁 | 目标是 selective refine，不是重新引入第二主链 |

## 7. 新 PTA 的硬边界

### 7.1 输入边界

新 PTA 只允许读取：

- `TypeFacts`
- `MethodFacts`
- `SymbolFacts.callSites`
- `ConstraintFacts`
- `SemanticFacts.methodSemanticFlags`
- `BuildEdgeAccumulator` 当前 provisional edges

新 PTA 不允许直接读取：

- GUI/runtime 状态
- Neo4j
- rule registry 动态判定
- XML/JSP 原始资源

### 7.2 输出边界

新 PTA 只允许输出两类结果：

1. `CALLS_PTA` 边
2. 对现有 provisional edge 的置信度补充或 evidence 增补

不允许：

- 重写已有 `direct/dispatch/reflection/framework` 的定义
- 另造一套 side table 给 Neo4j 导入

### 7.3 触发边界

默认 build 中，PTA 只允许在以下场景触发：

- `typed-dispatch` 仍然多目标且目标数超过阈值
- 调用点所在方法命中了 gadget/web/taint 热路径
- 反射/MethodHandle 调用点对象侧仍然不确定
- callback fixture 类似的 field/array/native-copy 场景

默认不允许：

- 对所有 `invokevirtual/interface` 全量跑 PTA

## 8. 最小配置面

`dev` 的 `PtaSolverConfig` 配置面太宽。  
迁到当前主干后，Phase 0 只保留下面这组内部配置对象，不再直接开放系统属性矩阵：

```java
record PtaBudget(
    int contextDepth,
    int objectDepth,
    int fieldDepth,
    int arrayDepth,
    int maxTargetsPerCall,
    int maxContextsPerMethod,
    int maxContextsPerSite
) {}
```

Phase 0 推荐默认值：

- `contextDepth = 1`
- `objectDepth = 1`
- `fieldDepth = 1`
- `arrayDepth = 1`
- `maxTargetsPerCall = 64`
- `maxContextsPerMethod = 32`
- `maxContextsPerSite = 8`

原因：

- 当前主干首先追求的是“稳定、可控、可测”
- 不是把 `dev` 里那套十几个系统属性重新放出来

如果后续确实需要调优，也应该：

- 先做代码内固定 budget
- 再通过少量 profile 级配置暴露

不允许回到 `dev` 的属性洪泛模式。

## 9. 需要从 `dev` 测试里保住的行为

`dev` 的 PTA 测试很多，但不是都需要迁。  
当前主干真正要保住的，是这些行为而不是旧测试壳：

### 9.1 必保行为

来自 `PtaHeapAndReflectionClosureTest`：

- field-sensitive dispatch
- array-sensitive dispatch
- native `System.arraycopy` 导致的 dispatch
- `Class.forName(..., loader)` 反射闭包
- `ClassLoader.loadClass(...)` 反射闭包
- helper-flow `MethodHandle` 闭包

来自 `PtaQualityRegressionTest`：

- recall 不低于主链基础边
- 不允许低置信度边无界膨胀
- `pta_ctx` 之类上下文 evidence 必须可观测

来自 `PtaAdaptiveSensitivityBudgetTest`：

- 在紧预算下不丢核心命中
- 预算控制必须真实生效

### 9.2 不必原样保留的旧验证形态

以下旧验证形态不值得原样带回：

- 基于 SQLite `method_call_table` 的旧断言方式
- 全局 `callgraph.mode=pta` 的模式测试
- 通过系统属性矩阵穷举 adaptive 分支

新主干里应改成：

- 基于当前 `CoreRunner.BuildResult`
- 基于 `CoreEngine.getCallEdgesByCaller(...)`
- 基于现有 QA/bench harness

## 10. 推荐拆分为四个子任务

### 10.1 `JA-NT-105-A` 数据结构最小核

交付：

- `PtaAllocNode`
- `PtaVarNode`
- `PtaInvokeSite`
- `PtaSyntheticCall`
- `PtaContext`
- `PtaContextMethod`
- `PointerAssignmentGraph`

要求：

- 先与当前主干 facts/edge 契约对齐
- 不带 plugin、配置、增量壳

### 10.2 `JA-NT-105-B` 约束提取过渡层

交付：

- `ConstraintFactAssembler` 过渡实现

要求：

- 临时仍可用 ASM 自提约束
- 但接口必须按 `ConstraintFacts` 设计
- 后续能无痛并入 `JA-NT-106`

### 10.3 `JA-NT-105-C` 局部 solver

交付：

- `SelectivePtaSolver`
- `PtaBudget`
- `PtaRefineRequest`
- `PtaRefineResult`

要求：

- 只支持局部热点调用点精化
- 默认 budget 固定
- 不开放系统属性矩阵

### 10.4 `JA-NT-105-D` 接入当前主链

交付：

- 在 `BytecodeMainlineCallGraphRunner` 后增加可选 refinement 钩子
- 指标写入 `BuildResult.stageMetrics`

要求：

- 默认仍可关闭
- 不允许替换已经存在的基础边构建链

## 11. Phase 0 的验收标准

`JA-NT-105` 这个 issue 结束时，至少要达到以下结果：

1. 有一份明确的最小 PTA 子集白名单
2. 已明确不迁的类和机制
3. 已明确 PTA 在当前主干中的输入、输出、触发边界
4. 已明确旧测试中哪些行为要保、哪些壳不要
5. 后续代码实施不再允许把 `dev` PTA 整包搬回

## 12. 对 `JA-NT-106` 和 `JA-NT-107` 的直接约束

### 12.1 对 `JA-NT-106`

`BytecodeFactRunner` 必须把下面这些 PTA 输入最终收敛成 facts：

- `receiverVarByCallSiteKey`
- `allocEdges`
- `assignEdges`
- `fieldStoreEdges`
- `fieldLoadEdges`
- `arrayStoreEdges`
- `arrayLoadEdges`
- `arrayCopyEdges`
- `nativeModelHints`

否则 `SelectivePtaRefiner` 迟早会重新回到“自己再扫一遍字节码”。

### 12.2 对 `JA-NT-107`

profile 切换只能是：

- `bytecode-mainline`
- `bytecode-mainline+pta-refine`

不允许重新引入：

- `pta` 作为第二条全量 callgraph mode
- 十几个 PTA 系统属性直通 GUI/API

## 13. `JA-NT-105` 完成后的统一口径

从这一步开始，项目内对 PTA 的统一口径应为：

- PTA 是当前主干的局部精化器，不是默认全量建库主链
- PTA 读 facts，写 `BuildEdgeAccumulator`
- PTA 不再拥有开放插件系统和大规模系统属性面
- PTA 只在热点调用点上补精度，不重新定义全图
