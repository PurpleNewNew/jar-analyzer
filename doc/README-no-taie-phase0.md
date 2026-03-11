# 去 Tai-e 主链化 Phase 0 实施文档与 Issue 清单

本文档是 [去 Tai-e 主链化设计](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie.md) 的执行版，目标是把：

- Phase 0 的基线测量
- `dev` 分支自研内核迁移

拆成可以直接开工的 issue 列表。

本文档默认约束：

- 当前默认主链仍然是 `ASM + Tai-e`
- 不直接改对外 API/GUI/MCP 契约
- 不长期保留双主链
- 优先复用现有 regression/bench/fixture，不重复造样本

## 1. Phase 0 交付目标

Phase 0 不做“去 Tai-e 切换”，只做两件事：

1. 建立可信的现状基线
2. 把 `dev` 分支的可迁移内核拆成明确的迁移包

完成标准：

- 我们能稳定回答“现在到底慢在哪里、重在哪里、哪些能力必须保住”
- 我们能明确回答“先迁什么、后迁什么、每一步怎么验收”

## 2. 当前可复用资产

### 2.1 现有回归与 bench

现有可直接复用的 QA 资产：

- `src/test/java/me/n1ar4/jar/analyzer/qa/RealFrameworkRegressionTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/RealStrutsSpringMyBatisAppRegressionTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/RealStrutsSpringMyBatisProjectModeRegressionTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/RealGadgetFamilyRegressionTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/YsoserialPayloadRegressionTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/GadgetRouteCoverageBenchTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/NavigationQualityBenchTest.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/GlobalSearchIncrementalBenchTest.java`

现有 fixture：

- `test/framework-stack-test`
- `test/struts-spring-mybatis-app`
- `test/gadget-family-test`
- `test/ysoserial-payload-test`
- `test/callback-test`
- `test/springboot-test`
- `test/leak-test`

### 2.2 现有指标入口

当前已经有可复用的测量入口：

- `CoreRunner` 已输出逐阶段耗时和 `heapUsage`
- `Neo4jBulkImportService` 已输出导入耗时、节点数、边数
- `BuildResult` / build meta 已记录 `call_graph_engine`、`call_graph_mode`、`taie_edge_count`
- `BenchReportWriter` 已约定 bench 输出目录为 `target/bench`

这意味着 Phase 0 不需要从零开始造 metrics 体系，应该先把现有日志与 QA harness 收敛成结构化报告。

## 3. Phase 0 范围边界

### 3.1 这阶段必须做

- 固定样本集
- 固定命令与 JVM 参数
- 固定输出格式
- 固定比较指标
- 固定迁移顺序与依赖图

### 3.2 这阶段不做

- 不切默认主链
- 不删除 Tai-e 依赖
- 不重写 `CoreRunner`
- 不引入新的持久化后端
- 不做大规模 UI/API 改动

## 4. 基线测量方案

### 4.1 样本矩阵

Phase 0 的基线样本统一固定为以下七组：

| 样本组 | 目标 | 现有资产 |
| --- | --- | --- |
| `framework-stack` | JSP/XML/Web 框架识别 | `RealFrameworkRegressionTest` |
| `ssm-war` | 真实 war 形态 source/framework 识别 | `RealStrutsSpringMyBatisAppRegressionTest` |
| `ssm-project-mode` | 真实 Web 调用图与污点链 | `RealStrutsSpringMyBatisProjectModeRegressionTest` |
| `gadget-family` | 真实 gadget 家族回归 | `RealGadgetFamilyRegressionTest` |
| `ysoserial` | 真实 payload 家族 | `YsoserialPayloadRegressionTest` |
| `callback` | 回调/代理/导航质量 | `NavigationQualityBenchTest` |
| `springboot-fatjar` | fat jar / nested lib / boot app 建库压力 | `test/springboot-test` |

补充样本：

- `leak-test` 用于规则热刷新/污点行为补充验证
- `GlobalSearchIncrementalBenchTest` 用于建库后索引/搜索增量侧面观测

### 4.2 基线比较指标

每个样本至少记录以下指标：

- `build_wall_ms`
- `build_stage_resolve_inputs_ms`
- `build_stage_prepare_class_files_ms`
- `build_stage_discovery_ms`
- `build_stage_class_analysis_ms`
- `build_stage_framework_entry_ms`
- `build_stage_method_semantic_ms`
- `build_stage_bytecode_symbol_ms`
- `build_stage_taie_ms`
- `build_stage_neo4j_commit_ms`
- `peak_heap_hint`
- `jar_count`
- `class_file_count`
- `class_count`
- `method_count`
- `call_site_count`
- `edge_count`
- `taie_edge_count`
- `project_store_size_bytes`
- `call_graph_engine`
- `call_graph_mode`

功能性结果至少记录：

- regression 是否通过
- gadget route 命中率与 p95
- navigation hit rate 与 p95
- 真实 Web path/taint 是否通过

### 4.3 输出产物约定

Phase 0 的报告统一落在 `target/bench`，建议产物固定为：

- `target/bench/no-taie-baseline-summary.md`
- `target/bench/no-taie-baseline-matrix.csv`
- `target/bench/no-taie-build-stage-breakdown.csv`
- `target/bench/no-taie-regression-summary.md`
- `target/bench/no-taie-env.txt`

如果需要长期保留某次基线，可再人工复制到：

- `doc/benchmarks/no-taie-baseline-YYYYMMDD.md`

### 4.4 建议执行命令

基础编译：

```bash
mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile
```

核心真实回归：

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=RealFrameworkRegressionTest,RealStrutsSpringMyBatisAppRegressionTest,RealStrutsSpringMyBatisProjectModeRegressionTest,RealGadgetFamilyRegressionTest,YsoserialPayloadRegressionTest \
  test
```

可选 bench：

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=GadgetRouteCoverageBenchTest \
  -Dbench.gadget=true -Dbench.gadget.iter=3 -Dbench.gadget.maxP95Ms=200 \
  test
```

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=NavigationQualityBenchTest \
  -Dbench.nav=true -Dbench.nav.iter=10 -Dbench.nav.maxP95Ms=300 \
  test
```

Phase 0 最小门禁：

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=NoTaieQualityGateTest \
  -Dbench.no_taie.gate=true \
  test
```

说明：

- Phase 0 不要求一开始就把所有样本塞进一个超大命令
- 关键是每类样本都有固定入口、固定产物、固定阈值

## 5. Issue 组织方式

建议以两个 Epic 管理：

- `EPIC-A`: 基线测量
- `EPIC-B`: `dev` 内核迁移准备

Issue 命名格式建议：

- `[Phase0][基线] ...`
- `[Phase0][迁移] ...`

每个 issue 必须包含：

- 目标
- 交付物
- 主要改动点
- 验收命令
- 依赖 issue

## 6. EPIC-A：基线测量 Issue 列表

### `JA-NT-001` `[Phase0][基线] 收敛建库阶段指标并输出结构化结果`

目标：

- 把当前 `CoreRunner` / `Neo4jBulkImportService` 的日志指标收敛成结构化输出，避免手工抄日志

交付物：

- 基于现有 build 输出生成一份结构化指标对象
- 至少支持导出 markdown + csv

主要改动点：

- `src/main/java/me/n1ar4/jar/analyzer/core/CoreRunner.java`
- `src/main/java/me/n1ar4/jar/analyzer/storage/neo4j/Neo4jBulkImportService.java`
- `src/test/java/me/n1ar4/jar/analyzer/qa/*`

验收：

- 至少一条真实样本 build 能生成结构化基线报告
- 报告中必须包含阶段耗时、边数、db 大小、call graph engine

依赖：

- 无

### `JA-NT-002` `[Phase0][基线] 固化 no-taie 基线样本清单与运行矩阵`

目标：

- 把当前分散在 `qa` 和 `test/*` 的样本收敛成固定清单，明确每个样本测什么

交付物：

- 一份样本矩阵定义
- 样本到测试类/命令的映射

主要改动点：

- `doc/README-no-taie-phase0.md`
- 新增样本清单文件时放到 `doc/` 或 `src/test/java/me/n1ar4/jar/analyzer/qa/`

验收：

- 所有 Phase 0 样本都有唯一入口
- 不再依赖口头约定

依赖：

- `JA-NT-001`

### `JA-NT-003` `[Phase0][基线] 新增 no-taie 基线总控 bench harness`

目标：

- 提供一个总控 bench/harness，统一跑样本、收集指标、写报告

交付物：

- 一个新的 QA bench test 或独立 runner
- 输出 `target/bench/no-taie-baseline-summary.md`

主要改动点：

- `src/test/java/me/n1ar4/jar/analyzer/qa/`
- 复用 `BenchReportWriter`

验收：

- 运行单个命令即可生成 Phase 0 基线总报告
- 可按样本开关启停，不要求一次性跑全量

依赖：

- `JA-NT-001`
- `JA-NT-002`

### `JA-NT-004` `[Phase0][基线] 补齐 springboot-fatjar 与 callback 样本的建库指标回归`

目标：

- 让目前更偏功能回归的样本也具备建库耗时与内存基线记录

交付物：

- `springboot-fatjar`
- `callback`

两组样本进入统一基线矩阵。

主要改动点：

- `test/springboot-test`
- `test/callback-test`
- `src/test/java/me/n1ar4/jar/analyzer/qa/`

验收：

- 两组样本进入 Phase 0 报告
- 能看到 build stage 分解数据

依赖：

- `JA-NT-003`

### `JA-NT-005` `[Phase0][基线] 固化当前 ASM+Tai-e 主线基线报告`

目标：

- 在切主链前，冻结一份可信的现状基线

交付物：

- 一份带日期的基线报告
- 记录机器/JDK/JVM 参数/样本集/通过率

主要改动点：

- `doc/benchmarks/` 或 `doc/`

验收：

- 后续所有“快了/准了”的说法都能回到这份基线

依赖：

- `JA-NT-003`
- `JA-NT-004`

### `JA-NT-006` `[Phase0][基线] 为回归和 bench 建立最小阈值门禁`

目标：

- 防止迁移期性能和准确性静默回退

交付物：

- gadget/navigation/真实 Web 回归的最小阈值
- 失败时可观测输出
- 默认门禁报告：`target/bench/no-taie-quality-gate.md`

主要改动点：

- `src/test/java/me/n1ar4/jar/analyzer/qa/`

验收：

- 至少以下阈值受控：
  - gadget route 命中率
  - navigation hit rate
  - 真实 Web regression pass
- 默认同时收敛保守性能上限：
  - 真实 Web 子集 `buildWallP95Ms`
  - 真实 Web 子集 `peakHeapMiB`

依赖：

- `JA-NT-005`

## 7. EPIC-B：`dev` 内核迁移准备 Issue 列表

### `JA-NT-101` `[Phase0][迁移] 盘点并收敛 dev 可迁移分析内核的边界`

目标：

- 把 `dev` 分支中值得迁移的类明确列成白名单，不做整包回滚

交付物：

- 白名单清单
- 每个类的职责、输入、输出、依赖说明
- 当前盘点结果：`doc/README-no-taie-dev-kernel-inventory.md`

首批候选：

- `DispatchCallResolver`
- `TypedDispatchResolver`
- `ReflectionCallResolver`
- `EdgeInferencePipeline`
- `ContextSensitivePtaEngine`

验收：

- 迁移白名单明确
- 不再出现“把 dev 整套搬回来”的模糊表述
- 明确区分：
  - 第一批白名单
  - 拆迁对象
  - 明确不迁对象

依赖：

- 无

### `JA-NT-102` `[Phase0][迁移] 设计当前分支 BuildFactSnapshot 与 dev 内核的适配层`

目标：

- 先定义“现在的事实模型怎样喂给 dev 内核”，再迁代码

交付物：

- 新的事实模型草案
- `BuildContext -> BuildFactSnapshot` 的演进方案
- 每个迁移内核需要的 facts 列表
- 当前设计文档：`doc/README-no-taie-fact-snapshot.md`

主要改动点：

- 设计文档优先
- 后续代码预留在 `src/main/java/me/n1ar4/jar/analyzer/core/build/` 或 `core/facts/`

验收：

- `Dispatch/Reflection/PTA` 三类能力的输入 facts 明确
- 明确区分：
  - 输入 facts
  - 边输出 accumulator

依赖：

- `JA-NT-101`

### `JA-NT-103` `[Phase0][迁移] 拆出 Dispatch 迁移包`

目标：

- 把 `dev` 的分派扩展能力独立成第一批迁移包

交付物：

- `DispatchCallResolver`
- `TypedDispatchResolver`

的迁移方案、依赖列表、验收计划。

验收：

- 明确哪些数据依赖当前已有：
  - `methodMap`
  - `classMap`
  - `inheritanceMap`
  - `callSites`
  - `instantiatedClasses`

依赖：

- `JA-NT-102`

### `JA-NT-104` `[Phase0][迁移] 拆出 Reflection 与 Semantic Edge 迁移包`

目标：

- 把 `ReflectionCallResolver + EdgeInferencePipeline` 作为第二批迁移包独立出来

交付物：

- 反射/代理/lambda/callback/framework 规则清单
- 当前分支已有 `MethodSemanticSupport`、`FrameworkEntryDiscovery` 的复用边界

验收：

- 明确哪些语义边在 facts 层做，哪些在 edge 层做
- 不允许迁移后又出现二次读字节码的散乱入口

依赖：

- `JA-NT-102`

### `JA-NT-105` `[Phase0][迁移] 拆出 ContextSensitivePtaEngine 迁移包`

目标：

- 评估 `dev` PTA 中真正需要迁的部分，先切成“局部精化引擎”而不是全局主链

详细设计见 [README-no-taie-pta-subset.md](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie-pta-subset.md)。

交付物：

- PTA 核心类清单
- 插件/配置/缓存/约束图各部分边界
- 明确哪些特性保留，哪些先砍掉

验收：

- 形成“最小可迁 PTA 子集”清单
- 明确不迁的部分

依赖：

- `JA-NT-102`

### `JA-NT-106` `[Phase0][迁移] 制定 BytecodeFactRunner 收敛计划`

目标：

- 把当前 `DiscoveryRunner + ClassAnalysisRunner + BytecodeSymbolRunner` 的重复解析收敛路线写清楚

交付物：

- `BytecodeFactRunner` 的目标输出模型
- 单次解析与按需 frame 分析的切分策略

验收：

- 明确旧 runner 哪些会被吸收
- 明确新 facts 对 `dispatch/reflection/PTA` 的供给关系

依赖：

- `JA-NT-102`

### `JA-NT-107` `[Phase0][迁移] 设计 bytecode-mainline / oracle-taie profile 切换方案`

目标：

- 提前定义 profile，而不是迁到一半才讨论默认路径

交付物：

- profile 定义：
  - `fast`
  - `balanced`
  - `precision`
  - `oracle-taie`

- 每个 profile 的引擎组合与适用场景

验收：

- 能明确说出默认 build 未来走哪条路
- Tai-e 的角色被限定为可选模式

依赖：

- `JA-NT-103`
- `JA-NT-104`
- `JA-NT-105`

### `JA-NT-108` `[Phase0][迁移] 建立迁移后的最小回归包`

目标：

- 在真正迁代码前，先固定“每迁一包必须跑哪些回归”

交付物：

- 迁移最小回归包命令
- 每个迁移 issue 对应的必跑 QA 集

建议最小集合：

- `RealFrameworkRegressionTest`
- `RealStrutsSpringMyBatisProjectModeRegressionTest`
- `RealGadgetFamilyRegressionTest`
- `YsoserialPayloadRegressionTest`
- `GadgetRouteCoverageBenchTest`

验收：

- 所有后续迁移 issue 都绑定具体验收测试

依赖：

- `JA-NT-006`
- `JA-NT-107`

## 8. 建议的执行顺序

推荐顺序如下：

1. `JA-NT-001`
2. `JA-NT-002`
3. `JA-NT-003`
4. `JA-NT-004`
5. `JA-NT-005`
6. `JA-NT-006`
7. `JA-NT-101`
8. `JA-NT-102`
9. `JA-NT-103`
10. `JA-NT-104`
11. `JA-NT-105`
12. `JA-NT-106`
13. `JA-NT-107`
14. `JA-NT-108`

原因：

- 先把“现状”量清楚
- 再把“迁什么”写清楚
- 最后再开始真正的代码迁移

## 9. Phase 0 完成判定

只有同时满足以下条件，Phase 0 才算完成：

- 现有 ASM+Tai-e 主线有一份冻结基线
- 样本矩阵、命令、输出报告都固定
- `dev` 内核迁移白名单与依赖关系明确
- `BytecodeFactRunner` 与 `bytecode-mainline` profile 的目标模型写清楚
- 后续每个迁移 issue 都有明确验收回归

如果这些条件没满足，就不应进入默认主链切换阶段。
