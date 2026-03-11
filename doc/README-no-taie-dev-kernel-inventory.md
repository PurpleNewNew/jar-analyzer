# `dev` 内核迁移白名单（JA-NT-101）

本文档对应 [Phase 0 实施文档](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie-phase0.md) 中的 `JA-NT-101`，目标是把 `dev` 分支里真正值得迁移的分析内核列成白名单，并明确：

- 哪些类应该迁
- 哪些类只能拆迁，不能原样搬
- 哪些类不在迁移范围
- 每类能力对当前主干 facts 的要求

本文档只解决“迁什么”，不解决“怎么接进当前主链”。后者由 `JA-NT-102` 处理。

## 1. 结论

结论先行：

- 不迁 `dev` 的旧 `CoreRunner`、旧持久化、旧调用图模式编排。
- 迁移对象只限于 `ASM/字节码` 分析内核，不包括 `JavaParser/source navigation` 侧代码。
- 第一批白名单是：
  - `InheritanceMap`
  - `DispatchCallResolver`
  - `TypedDispatchResolver`
  - `ReflectionCallResolver`
  - `ContextSensitivePtaEngine` 的最小内核
- `EdgeInferencePipeline` 和 `PtaOnTheFly*Plugin` 不能原样搬，只能拆成规则库或局部插件逻辑迁入。

一句话总结：

- 迁“算法核”
- 不迁“旧编排”
- 不迁“旧持久化”
- 不迁“源码侧语义工具”

## 2. 当前主干真正需要什么

当前主干不是缺一个“新的 `CoreRunner`”，而是缺一组能替代 Tai-e 默认主链的边构建能力。落到当前代码，后续迁移必须继续满足这条输出契约：

- [BuildContext.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/build/BuildContext.java)
- [CoreRunner.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/CoreRunner.java)
- [Neo4jBulkImportService.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/storage/neo4j/Neo4jBulkImportService.java)

后续自研主链必须继续产出：

- `methodCalls`
- `methodCallMeta`
- `callSites`
- `method semantic flags`
- `framework/source entry facts`
- 可进入 Neo4j 与 `ProjectRuntimeSnapshot` 的完整边元数据

换句话说，迁移对象必须服务于“边和边元数据”，而不是单独跑一个分析器自娱自乐。

## 3. 当前主干可直接复用的 facts

当前主干已经具备一部分 `dev` 内核需要的输入，不需要重造：

| facts | 当前提供位置 | 说明 |
| --- | --- | --- |
| `classMap` / `methodMap` | [BuildContext.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/build/BuildContext.java) | 已具备 |
| `callSites` | [BytecodeSymbolRunner.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/BytecodeSymbolRunner.java) | 已含 `calleeOwner / receiverType / callIndex / callSiteKey` |
| `method semantic flags` | [MethodSemanticSupport.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/rules/MethodSemanticSupport.java) | 已具备 |
| `framework entry facts` | [FrameworkEntryDiscovery.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/FrameworkEntryDiscovery.java) | 已具备 |
| `class bytes` | [BuildContext.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/build/BuildContext.java) `classFileList` | 当前仍可拿到 |

当前主干还缺两类关键 facts：

| facts | 当前状态 | 备注 |
| --- | --- | --- |
| `inheritanceMap` | 缺失 | `dev` 的 dispatch/PTA 都依赖它 |
| `instantiatedClasses` | 缺失 | `DispatchCallResolver` 的 RTA 裁剪需要 |

还有一类长期应下沉到 `BytecodeFactRunner` 的 facts，目前只能靠 PTA 内部二次读字节码临时补：

- receiver/base var 约束
- local assign/alloc/field/array/throw 约束
- reflection helper string assembly 约束

这也是为什么 `ContextSensitivePtaEngine` 不能直接原样接成默认主链。

## 4. 白名单总表

### 4.1 一级白名单：应迁入当前分支

| 类/能力 | 迁移结论 | 原因 | 当前所需 facts | 输出 |
| --- | --- | --- | --- | --- |
| `InheritanceMap` | 迁，且应重写成小工具 | 结构简单，收益高，是 dispatch/PTA 的公共依赖 | `classMap` / `ClassReference.superClass` / `interfaces` | 父子类型查询 |
| `DispatchCallResolver` | 迁 | 直接解决 `invokevirtual/invokeinterface` 的 CHA/RTA 展开 | `methodCalls` / `methodCallMeta` / `methodMap` / `classMap` / `inheritanceMap` / `instantiatedClasses` | `CALLS_DISPATCH` 边 |
| `TypedDispatchResolver` | 迁 | 直接消费当前已有 `callSites.receiverType`，性价比高 | `callSites` / `methodMap` / `classMap` / `inheritanceMap` | 高置信 `CALLS_DISPATCH` 边 |
| `ReflectionCallResolver` | 迁，但要拆出纯算法层 | 它本质是 ASM 常量传播 + reflection/indy/lambda 解析，能替 Tai-e 补掉大块能力 | `class bytes` / `methodMap` / `methodCalls` / `methodCallMeta` | `CALLS_REFLECTION` / `CALLS_METHOD_HANDLE` / `CALLS_INDY` |
| `ContextSensitivePtaEngine` 最小核 | 迁，但只能作为局部精化层 | 它能承接热点调用点精化，适合作为 `SelectivePtaRefiner` 的种子 | `BuildFactSnapshot` 级别 facts，不能只靠当前松散容器 | `CALLS_PTA` / alias/receiver refinement |

### 4.2 二级白名单：只迁“思想和规则”，不迁类本身

| 类/能力 | 迁移结论 | 原因 | 迁移方式 |
| --- | --- | --- | --- |
| `EdgeInferencePipeline` | 不原样迁，拆迁 | 当前主干已经有 `MethodSemanticSupport + FrameworkEntryDiscovery`，原类里很多规则会和现有动态语义重叠 | 拆成 callback/framework rule library |
| `SpringFrameworkEdgeRule` | 不原样迁 | 它依赖 `dev` 时期的“框架猜测型补边”思路，和当前稳定 `semanticFlags` 模型不一致 | 只迁可复用规则，不迁大类 |
| `DynamicProxyEdgeRule` | 不原样迁 | 当前图模型已经有更稳定的 gadget/proxy 语义位 | 把命中模式迁成 edge rule |
| `ReflectionLogEdgeRule` | 条件迁 | 若后续仍保留 reflection 日志/提示流，可复用；否则意义不大 | 作为可选低优先级补边 |
| `PtaOnTheFlySemanticPlugin` | 不原样迁 | 它本质是 callback/framework 规则集，不是 PTA 核心 | 抽规则，不抽插件壳 |
| `PtaOnTheFlyReflectionPlugin` | 不原样迁 | 里面有可复用的 hint 池思路，但类本身绑在 `PtaPluginBridge` 生命周期上 | 抽 reflection hint resolver |
| `PtaOnTheFlyDynamicProxyPlugin` | 不原样迁 | 同上 | 抽 proxy-specific rule |
| `PtaOnTheFlySpringFrameworkPlugin` | 不原样迁 | 当前主干已有 framework source/semantic 体系 | 只保留必要 callback 模式 |

## 5. 非白名单

以下内容明确不在迁移范围：

| 类/模块 | 不迁原因 |
| --- | --- |
| `dev` 的 `CoreRunner.java` | 旧编排、旧持久化、旧开关治理都不适合当前主干 |
| `CallGraphMode` / 旧 build 开关体系 | 当前主干要收敛成单主链，不引回一套历史模式矩阵 |
| `BuildDbWriter` / SQLite 写库路径 | 当前项目主存储和一致性边界已经是 Neo4j 项目库 |
| `semantic/CallResolver` | 这是源码/导航侧 JavaParser 工具，不是字节码建库内核 |
| `ReflectionProbeClassVisitor` | 会引入一次额外 ASM 预扫，和后续 `BytecodeFactRunner` 方向冲突 |
| `PtaPluginLoader` 的运行时反射插件机制 | 会扩大系统面和不确定性，不适合当前“收敛优先”原则 |
| `dev` 的整套旧测试命名与旧质量模型 | 需要迁的是 benchmark/fixture 的思路，不是测试壳本身 |

## 6. 每个白名单对象的迁移裁决

### 6.1 `InheritanceMap`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/InheritanceMap.java` 非常简单，职责也清晰：

- `child -> all parents`
- `parent -> all children`
- `isSubclassOf`

迁移建议：

- 不直接 cherry-pick 文件
- 在当前分支按同样的数据结构重写一个小工具
- 由新的 facts 层统一构建，不允许每个 resolver 自己扫 `classMap`

这是最典型的“低风险高收益”迁移对象。

### 6.2 `DispatchCallResolver`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/DispatchCallResolver.java` 干了两件正确的事：

1. 收集 `NEW` 指令得到 `instantiatedClasses`
2. 基于 `inheritanceMap + methodMap + instantiatedClasses` 做 `invokevirtual/invokeinterface` 展开

这类能力完全符合当前主干需要，因为它直接补的是 Tai-e 当前最重、但我们可以自己控制的一段：

- CHA 展开
- RTA 裁剪
- `MethodCallMeta.TYPE_DISPATCH`

迁移要求：

- 不能再让它直接依赖“二次大范围读字节码”
- `instantiatedClasses` 应由未来 `BytecodeFactRunner` 一次性产出
- 输出必须继续写回当前 `methodCalls + methodCallMeta`

裁决：

- 列入第一批白名单

### 6.3 `TypedDispatchResolver`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/TypedDispatchResolver.java` 直接消费 `CallSiteEntity.receiverType`，这一点对当前主干尤其合适，因为：

- 当前 [CallSiteEntity.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/entity/CallSiteEntity.java) 已经保留了 `receiverType`
- 当前 `BytecodeSymbolRunner` 已经在默认 build 中产出了这些 call sites

也就是说，这个 resolver 的主要输入我们已经有了。

裁决：

- 列入第一批白名单
- 可早于 PTA 落地
- 是替代 Tai-e 一部分精度收益的最好切口之一

### 6.4 `ReflectionCallResolver`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/asm/ReflectionCallResolver.java` 是整个 `dev` 最值钱的类之一，因为它不只是“反射补边”，还顺手覆盖了：

- `Class.forName`
- `Method.invoke`
- `Constructor.newInstance`
- `MethodHandle`
- `invokedynamic`
- `LambdaMetafactory`

这类逻辑正好弥补 Tai-e 退出默认主链后的两块空白：

- 反射补边
- `indy/lambda/method-handle` 边

但它不能原样搬，原因是：

- 它仍然按 class 逐个二次解析
- 常量解析和字节码遍历逻辑应纳入未来统一 fact 解析链

裁决：

- 列入第一批白名单
- 但迁移时要拆成：
  - reflection/const propagation core
  - lambda/indy resolver
  - method-handle resolver

### 6.5 `EdgeInferencePipeline`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/edge/EdgeInferencePipeline.java` 本身不是坏设计，但它所在的历史阶段和当前主干不一致：

- `dev` 当时缺稳定方法语义位，只能靠 rule class 直接补边
- 当前主干已经有稳定 `method semantic flags`
- 当前主干已经有 `FrameworkEntryDiscovery`

所以这条线的正确迁法不是“把 pipeline 原封不动接回来”，而是把里面真正需要保留的 callback/framework 规则拆出来，转成：

- `facts -> semantic flags`
- `semantic flags -> edge rule`

裁决：

- 不原样迁
- 只迁规则，不迁类壳

### 6.6 `ContextSensitivePtaEngine`

`dev` 的 `src/main/java/me/n1ar4/jar/analyzer/core/pta/ContextSensitivePtaEngine.java` 是第三阶段的核心种子，但必须明确：

- 它适合做热点精化
- 不适合直接替代默认 build 主链

原因：

- 它内部仍会重新读取 class bytes 和做约束抽取
- 它绑定了 `PtaContext/PtaVarNode/PtaAllocNode/PtaInvokeSite` 一整套 solver 对象体系
- 还挂着一层 `PtaPlugin` 生命周期

因此迁移策略必须是：

- 保留 solver core
- 保留最小对象模型
- 砍掉可插拔壳
- 把 `constraint extraction` 从 solver 中抽出来，交给 facts 层

裁决：

- 列入白名单，但归类为第三批迁移对象
- 不能作为第一批默认主链交付

## 7. PTA 相关子类的裁决

`ContextSensitivePtaEngine` 不是单文件迁移，需要一并审视其簇状依赖。

### 7.1 保留为 PTA 最小核候选

建议保留这批对象模型与状态结构：

- `PtaContext`
- `PtaContextMethod`
- `PtaInvokeSite`
- `PtaVarNode`
- `PtaAllocNode`
- `PtaSyntheticCall`
- `IncrementalPtaState`

原因：

- 这些类属于 solver 的基础数据结构
- 重写成本高
- 和当前主干 UI/API/Neo4j 契约耦合低

### 7.2 只保留策略，不保留实现壳

建议不直接迁这批类，而是抽其策略：

- `PtaSolverConfig`
- `PtaOnTheFlySemanticPlugin`
- `PtaOnTheFlyReflectionPlugin`
- `PtaOnTheFlyDynamicProxyPlugin`
- `PtaOnTheFlySpringFrameworkPlugin`

原因：

- 配置项过多，直接迁入会重新引入大片系统属性面
- 插件边界太开放，不符合当前主干“收敛优先”
- 其中真正值钱的是启发式，不是 class 壳

### 7.3 明确不迁

- `PtaPluginLoader`
- 运行时反射加载插件机制
- 约束检查型调试插件壳

## 8. 白名单迁移顺序

`JA-NT-101` 的结果必须直接约束后续 issue 顺序。推荐顺序如下：

1. `JA-NT-102`
   - 定义 `BuildFactSnapshot`
   - 明确 `inheritance / instantiated / callSite receiver / semantic flags` 的提供方式
2. `JA-NT-103`
   - 迁 `InheritanceMap`
   - 迁 `DispatchCallResolver`
   - 迁 `TypedDispatchResolver`
3. `JA-NT-104`
   - 迁 `ReflectionCallResolver`
   - 拆 `EdgeInferencePipeline` 中仍有价值的 callback/framework 规则
4. `JA-NT-105`
   - 迁 `ContextSensitivePtaEngine` 最小核
   - 只做热点精化，不碰默认全量 build

这个顺序的核心理由是：

- 先拿到最便宜的 `dispatch + typed dispatch`
- 再补 `reflection/indy/lambda`
- 最后再接入 PTA 精化

## 9. 对后续设计的硬约束

从现在开始，后续文档和代码实现必须遵守以下约束：

### 9.1 不允许“整包搬回 dev”

如果某个方案需要：

- 迁旧 `CoreRunner`
- 迁旧 DB writer
- 迁旧 callgraph mode 矩阵
- 迁旧 source navigation 语义器

则该方案直接视为偏离 `JA-NT-101` 结论。

### 9.2 不允许让多个 resolver 各自重扫 class bytes

后续 `Dispatch/Reflection/PTA` 所需的字节码 facts，必须收敛到同一套事实模型里，不能继续：

- `Dispatch` 扫一遍
- `Reflection` 再扫一遍
- `PTA` 再扫一遍

### 9.3 不允许把旧启发式覆盖当前稳定语义位

当前主干已有：

- [FrameworkEntryDiscovery.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/core/FrameworkEntryDiscovery.java)
- [MethodSemanticSupport.java](/Users/veritas/Documents/projects/jar-analyzer/src/main/java/me/n1ar4/jar/analyzer/rules/MethodSemanticSupport.java)

后续迁移只能复用这些稳定事实，不能重新退回“靠一组大 rule class 猜框架”的旧路径。

## 10. 本 issue 的完成定义

`JA-NT-101` 完成后，项目内对于 `dev` 分支的迁移口径应统一为：

- 迁移白名单：
  - `InheritanceMap`
  - `DispatchCallResolver`
  - `TypedDispatchResolver`
  - `ReflectionCallResolver`
  - `ContextSensitivePtaEngine` 最小核
- 拆迁对象：
  - `EdgeInferencePipeline`
  - `PtaOnTheFly*Plugin`
- 明确不迁：
  - 旧 `CoreRunner`
  - 旧 DB/持久化
  - `semantic/CallResolver`
  - 运行时动态 plugin 装载

如果后续讨论又回到“要不要把 `dev` 整体搬回来”，则说明该 issue 没有真正被执行。
