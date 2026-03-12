# AGENTS.md

> Jar Analyzer 协作、架构与提交规范（团队统一执行版）

本文件定义本仓库默认协作规则、架构边界、代码风格与提交标准。
若与明确的人类指令冲突，以人类指令为准。

## 1. 基本原则（必须遵守）
- 干净优先：能删就删，拒绝遗留分支、无效开关、历史回退链路共存。
- 收敛优先：同一能力只保留一条主链，不做长期双轨并行。
- 可维护优先：避免过度拆分薄函数；单点、直接、可读。
- 可验证优先：改动必须可编译、可测试、可解释，禁止“看起来完成”。
- 同步拔旧优先：新逻辑一旦进入主链，当轮必须同步删除旧入口、旧开关、旧兼容层、旧测试残留。
- 收尾清理优先：每次任务结束前，必须主动做一轮残留清理；“功能完成但代码未收口”不算完成。

### 1.1 执行口径（必须按此理解需求）
- 用户要求“修复/优化/重构”时，默认包含：
  1. 让目标行为正确工作
  2. 删除与新行为冲突或重复的旧逻辑
  3. 清理对应测试、文档、配置、UI 残留
- 用户要求“清理/体检/收敛”时，不只看死代码；还必须检查：
  - 旧开关
  - 隐藏系统属性
  - 兼容入口
  - 无效 fallback
  - 单点薄包装
  - 失效指标/失效 stage
  - 只剩一处调用的历史 helper
- 不允许把“先保留，后面再清”当默认策略；除非人类明确要求分两轮，否则当轮就要收干净。
- 判断是否该删除时，默认立场是“删”；只有存在明确业务价值、测试覆盖和正式配置面时，才允许保留。

## 2. 技术架构总览（当前事实）

### 2.1 运行形态
- 单进程架构：GUI + HTTP API + MCP + Neo4j Embedded 全部进程内运行。
- 运行/构建基线：JBR 21 + JCEF。
- 分析输入：仅字节码（jar/war/class/目录），不支持源码索引链路。

### 2.2 核心技术栈
- 字节码扫描：ASM。
- 调用图：`bytecode-mainline`（`fast / balanced / precision`）。
- 图存储：Neo4j Embedded（官方依赖）。
- 反编译：CFR（单引擎）。

### 2.3 单活项目模型
- 同一时刻只有一个 active project 对外提供查询/Flow 数据。
- 每个项目独立 store：`db/neo4j-projects/<project-key>/`。
- 项目上下文由 `ActiveProjectContext + Neo4jProjectStore` 统一管理。

## 3. 模块边界（目录级）
- `core/*`：建库主流程、字节码发现、符号提取、作用域分类、bytecode-mainline 调用图主链。
- `core/scope/*`：APP/LIBRARY/SDK 归属分类与范围规则。
- `storage/neo4j/*`：项目 store 生命周期、批量导入、图构建元数据。
- `graph/*`：图查询、DFS、Flow、Procedure、Cypher 执行。
- `taint/*`：污点传播、summary、规则驱动语义。
- `rules/*`：`sink/model/source` 规则加载与统一注册。
- `server/handler/*`：HTTP API 入口与参数编排。
- `mcp/*`：MCP 传输、tool 编排、报告链路。
- `gui/runtime/*` + `gui/swing/*`：GUI 状态层与界面层。
- `engine/*`：查询引擎、索引、反编译调度。

边界约束：
- GUI 层不直接实现分析逻辑，只做编排与展示。
- 规则解释逻辑只能在 `rules/*` 与 `taint/*`，禁止散落在 handler/UI。
- 调用图边来源只允许 `bytecode-mainline`，禁止引入第二条隐藏边生成链或历史 fallback。

## 4. 建库主流程（规范主链）
1. `CoreRunner` 启动构建，准备上下文与项目信息。
2. 发现阶段：class/resource/method/callsite/localvar 元数据扫描。
3. 归属阶段：`forceTarget > sdk > commonLibrary > appHeuristic`。
4. 调用图阶段：运行 `bytecode-mainline` 并回填 `methodCalls/methodCallMeta`。
5. 入库阶段：
   - 内存元数据通过 `DatabaseManager` 原子更新。
   - 图边通过 `Neo4jGraphBuildService/Neo4jBulkImportService` 写入项目库。
6. 完成阶段：刷新缓存、更新构建元信息。

强约束：
- 非 all-common 场景下，调用图阶段失败直接终止建库（不回退旧调用图）。
- all-common 默认策略：`continue-no-callgraph`（构建成功但边数可为 0）。

## 5. 查询与分析流程（规范）
- 搜索/调用图/资源检索：围绕 active project 的同一 Neo4j 图与元数据快照。
- DFS：图上路径搜索（graph-only）。
- Taint：基于 DFS 结果 + summary 语义规则做链路验证。
- MCP：复用同一套后端能力，不另起“第二逻辑实现”。

### 5.1 图模型目标态（对外契约）
- 物理节点标签只保留结构标签：`JANode` `Method` `Class`。
- 禁止继续向节点标签扩散安全语义标签；`Source` `SourceWeb` `Sink` `Endpoint` 这类信息只能通过属性或动态 `ja.*` 呈现。
- 物理关系类型允许保留分析精度：`CALLS_DIRECT` `CALLS_DISPATCH` `CALLS_REFLECTION` `CALLS_CALLBACK` `CALLS_OVERRIDE` `CALLS_INDY` `CALLS_METHOD_HANDLE` `CALLS_FRAMEWORK` `CALLS_PTA` `ALIAS` `HAS` `EXTEND` `INTERFACES`。
- 对外展示与查询心智必须收敛为逻辑关系类别：`CALL` `ALIAS` `HAS` `EXTEND` `INTERFACES`。
- `CALLS_*` 只属于存储层与分析层；Workbench/Legend/Overview 默认显示聚合后的 `CALL`，细分类型只能作为 inspector/细分视图信息出现。
- `ALIAS` 与 `CALL` 在图模型上是平级主边，不允许在展示层把 `ALIAS` 降级成附属边；是否默认参与路径搜索由遍历策略决定，不由图例层级决定。
- `Class/HAS/EXTEND/INTERFACES` 与 `Method/CALL/ALIAS` 进入同一项目库，但默认主舞台仍是方法调用图；结构边通过结构模板或显式结构查询突出展示，不再维护单独的“结构图模式”。

### 5.2 静态入图与动态语义边界
- 必须静态入图的事实：`Method/Class` 节点、`CALLS_*`、`ALIAS`、`HAS`、`EXTEND`、`INTERFACES` 以及 `call_site_key/line_number/call_index/source_flags/confidence/evidence/alias_kind` 等构建期元数据。
- 必须动态判定、禁止静态固化进图的语义：`ja.isSource` `ja.isSink` `ja.sinkKind` `ja.ruleVersion` `ja.rulesFingerprint` `ja.ruleValidation` `ja.ruleValidationIssues`。
- 规则热刷新后，动态 `ja.*` 的结果必须即时反映新规则，不允许要求“重建项目后才生效”。

### 5.3 默认遍历策略
- 路径与污点搜索默认遍历模式是 `call-only`。
- 仅在显式启用时允许 `call+alias`；`ALIAS` 是否参与搜索必须是显式选择，而不是静默放开。
- `HAS` `EXTEND` `INTERFACES` 禁止参与默认 DFS/Taint 主链，禁止把结构边混入方法调用路径搜索。
- Workbench 必须提供清晰的遍历模式提示；模板、图例和结果说明应让用户明确知道当前是否启用了 `ALIAS`。

## 6. 规则体系与热刷新（必须保持一致）

### 6.1 规则文件职责
- `rules/sink.json`：sink 检索、风险分类入口。
- `rules/source.json`：source/sourceAnnotations。
- `rules/model.json`：summary/sanitizer/guard/additional。

### 6.2 注册器行为
- `SinkRuleRegistry`：版本化快照，支持 `reload()/checkNow()/getVersion()`。
- `ModelRegistry`：版本化快照，融合 `source + model + sink`，支持 `reload()/checkNow()/getVersion()/getRulesFingerprint()`。
- 规则文件变化后必须可生效，不允许“重启后才生效”的隐式约束。

### 6.3 污点语义一致性
- `SummaryEngine` 必须绑定规则版本；规则版本变化时必须清理 summary 缓存并重建指纹。
- 禁止 summary 继续复用旧规则缓存。

## 7. 并发与一致性约束
- 涉及建库元数据的批量写入必须原子化（禁止暴露半更新状态）。
- 读路径必须在一致性边界内，禁止读写竞态导致“部分可见”。
- 队列满、写失败、规则加载失败、缓存失效失败必须显式处理（日志 + 行为确定），禁止静默吞错。
- 快照缓存必须具备明确失效策略（版本号/指纹/显式 reload）。

## 8. 配置与开关治理
- 不新增无业务价值开关，尤其是调试后遗留开关。
- GUI 出现的选项必须接入真实逻辑；未接入的 UI 选项必须删除。
- 禁止把“兼容旧路径”作为长期双分支保留。
- 禁止同一行为同时存在“显式配置 + 隐式系统属性 + 默认猜测”三套来源；必须收敛为单一配置面。
- 禁止通过全局系统属性、静态变量、隐藏环境变量传递项目级输入；项目级输入必须进入显式模型、DTO 或正式配置对象。
- 兼容入口只能作为短期迁移手段，默认不允许落地到长期主干；一旦主链稳定，当轮删除。
- 新增系统属性时必须：
  1. 有明确默认值
  2. 有文档
  3. 有测试覆盖（至少正向/默认路径）
  4. 不与已有正式配置面重复
  5. 不改变无配置时的主链语义

### 8.1 开关删除规则（强制）
- 以下内容默认应删除，而不是继续保留：
  - 只服务旧路径的兼容开关
  - 只在测试中改写、生产也能读到的隐藏开关
  - GUI 已消失但核心仍然读取的系统属性
  - 已固定默认策略、却仍保留可切换逻辑的旧 policy
  - 实际恒为 `false` / 恒为空 / 恒走默认分支的遗留判断
- 删除开关时，必须同步删除：
  - 读取点
  - 传递链
  - 文档说明
  - 测试 fixture
  - build meta / metrics / DTO 字段

## 9. 代码风格规范

### 9.1 函数设计
- 仅单点调用且逻辑极薄（如一次判断/一次映射）的函数，优先内联收敛。
- 禁止为一行语义等价操作新增包装函数。
- 单文件内不允许大量“薄函数跳转链”影响可读性。
- 新增逻辑时，优先直接并入现有主链；除非存在明确复用价值，否则不要通过增加一层 helper 来“摆放代码”。
- 如果一个函数在重构后只剩单点调用且只承担参数转发、空值兜底或命名改写，应优先删除并内联。
- 历史包装函数一旦失去抽象价值，应当轮删除，不保留“以后也许会用”。

### 9.2 注释与命名
- 注释只解释“为什么”，不解释显而易见的“做了什么”。
- 清理乱码、过期、误导性注释。
- 命名使用业务语义，避免 `tmp/flag2/data3` 这类弱语义命名。
- 禁止沿用历史遗留命名掩盖新语义；当行为已经变化时，命名、日志、metric、测试名必须一起改到位。

### 9.3 死代码清理
- 每轮改动必须体检：无引用类、无引用方法、无效字段即删。
- 删除后同步清理 import、测试、文档、配置残留。
- 目标是“像从未出现过”，不是“先留着以后清”。
- 不只清理“无引用死代码”，还要清理“有引用但已无独立价值”的残留代码：
  - 恒真/恒假的分支
  - 永远走默认值的参数
  - 只做透传的 DTO 字段
  - 已无真实工作、只留下打点/占位的 stage
  - 已被主链替代、但还残留调用点的旧 helper

### 9.4 改动时的同步清理（强制）
- 每次新增能力、修改行为或优化性能时，必须同时检查并处理：
  1. 是否产生第二条逻辑主链
  2. 是否留下 fallback / legacy / compat 残留
  3. 是否新增了影子配置来源
  4. 是否出现薄函数跳转链
  5. 是否有指标、日志、DTO、build meta 已失真
  6. 是否有测试仍在覆盖旧行为而非当前行为
- 只改“核心逻辑”而不处理外围残留，视为未完成交付。
- 允许保留的唯一例外是：立即删除会破坏编译或会影响用户显式承诺的兼容面；此时必须在同一轮向人类明确说明阻塞原因。

### 9.5 性能改动风格（强制）
- 性能优化优先删除错误逻辑、重复装配、无效复制、过宽生命周期，而不是先堆新缓存、新模式、新开关。
- 不允许通过新增“快模式/兼容模式/应急模式”掩盖主链性能问题。
- 当新方案替代旧方案后，旧的 stage、metrics、配置项、测试入口必须同步收口，不能留下双轨 benchmark。

## 10. 禁止引入的内容
- 旧调用图模式、legacy/classic fallback、源码兜底编译链路。
- SQL 兼容残留路径（项目主存储为 Neo4j）。
- 未接线的开关、无验证的优化、无出口的 TODO 分支。
- 同一能力多实现长期并存。
- 全局侧信道：通过系统属性/静态状态偷偷传递项目级参数。
- 只为“保留以后可能有用”而存在的兼容分支、旧 DTO 字段、旧 build meta 字段。
- 新逻辑落地后仍保留旧测试、旧文档、旧日志文案并行描述。

## 11. 提交风格（强制）

### 11.1 标题格式
- 必须使用中文类型前缀：
  - `[重构]`
  - `[修复]`
  - `[测试]`
  - `[文档]`
  - `[优化]`
  - `[其他]`
- 格式：`[类型] 动作 + 对象 + 结果`
- 示例：
  - `[重构] 收敛规则加载链路并移除冗余分支`
  - `[修复] 修复建库原子更新期间的半状态可见问题`

### 11.2 禁止项
- 禁止 `refactor:` / `fix:` / `feat:` 等英文前缀。
- 禁止“update/调整一下”这类无信息标题。
- 禁止把多个不相关主题混成一个提交。

### 11.3 拆分原则
- 一次提交只解决一个清晰主题。
- 推荐拆分顺序：
  1. 删除/收敛（重构）
  2. 行为修复（修复）
  3. 测试补齐（测试）
  4. 文档同步（文档）
- 只有“拆开会破坏可编译/可运行”时允许合并。

### 11.4 提交前的清理要求
- 提交前必须自检：当前提交是否仍残留被新逻辑替代的旧代码、旧配置、旧测试、旧文档。
- 如果提交主题是“优化/修复/重构”，但故意保留已确认可删的残留，视为提交不合格。
- “后续再清理”不能写进默认提交策略；只有人类明确要求分批提交时，才允许分批清理。

## 12. 交付前检查清单（必须执行）
- 前置环境：
  - 若项目目录内存在 JDK（例如 `./jdk-21`），默认优先将其作为项目 JDK；只有当它不满足 **JBR 21 + JCEF** 基线时，才回退到外部 JDK
  - `JAVA_HOME` 必须指向 **JBR 21 + JCEF**
  - `java.vendor` 必须为 `JetBrains`
  - `${java.home}/jmods/jcef.jmod` 必须存在
- 编译：
  - PowerShell 下优先使用：`mvn --% -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
  - `mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
- 打包：
  - 禁止在正式打包命令中携带 `-Dskip.npm=true`
  - 原因：会跳过 `frontend/cypher-workbench` 构建，导致产物缺少 `cypher-workbench/index.html` 等前端资源
- 测试：按改动范围执行最小充分测试集。
- 引用扫描：`rg` 检查删除能力是否仍有引用残留。
- 清理验收：
  - `rg` 检查旧开关、旧函数、旧字段、旧日志文案是否仍有残留引用
  - 检查主链是否只剩一个正式入口
  - 检查是否还存在影子配置来源或隐式输入
  - 检查 metrics/build meta/stage 名称是否仍反映真实行为
  - 检查测试名、断言、fixture 是否已经切换到新行为
- 规则改动验证：
  - 版本号变化
  - 指纹变化
  - 热刷新后语义生效
- 并发改动验证：至少覆盖一次并发读写或队列压力场景。
- 文档验收：
  - README / API 文档 / UI 文案 / 日志说明不能继续描述已删除的旧行为

### 12.1 任务完成定义（强制）
- 同时满足以下条件，才算“任务完成”：
  1. 目标行为已实现或问题已修复
  2. 被替代的旧逻辑已删除
  3. 编译与最小充分测试通过
  4. 文档、测试、配置、日志、指标已同步
  5. 工作区不存在“明知应删但暂时没删”的残留
- 只完成第 1 条，不算完成。

## 13. 评审输出规范
- 先列问题，再给摘要；问题按严重度排序。
- 每条问题必须给文件与行号。
- 明确区分：
  - 功能性 Bug
  - 并发一致性问题
  - 可观测性改进
  - 风格/清理建议
- 禁止夸大：死代码清理不称为 Bug 修复。

## 14. 常用命令（仓库默认）
- 前置：`JAVA_HOME=<JBR21+JCEF>`
- 若仓库根目录自带 JDK（例如 `./jdk-21`），优先先把 `JAVA_HOME` 指到这个目录，再校验它是否为 **JBR 21 + JCEF**
- 编译：
  - `mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
  - PowerShell 下优先使用：`mvn --% -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
- 打包：`mvn -q -DskipTests package`
- 打包约束：除非明确只验证后端且不需要可运行 GUI 产物，否则不要为 `package` 增加 `-Dskip.npm=true`
- 目标测试：`mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest=<TestClass> test`
  - PowerShell 下优先使用：`mvn --% -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest=<TestClass> test`
- 引用扫描：`rg -n "<symbol>" src/main src/test`
- 文件列表：`rg --files`
## 15. 文档同步要求
- 影响使用方式/API/配置的改动，必须同步 `README.md` 与 `doc/README-api.md`。
- 影响规则体系的改动，必须同步说明：规则来源、优先级、刷新机制。
- 文档必须与当前代码行为一致，禁止保留过期描述。

---

执行口径：本文件是仓库默认协作规范。新增能力或架构切换时，先更新本文件，再改代码。
