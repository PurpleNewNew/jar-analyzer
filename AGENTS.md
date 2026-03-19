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

### 1.2 Subagent 协作口径（必须遵守）
- Codex 执行任务时，默认应自主判断是否放出适量 subagent 辅助完成。
- subagent 数量以“刚好覆盖问题切面、且不制造重复劳动”为准；简单任务也允许放出 subagent 做交叉验证，复杂任务应并行覆盖多个独立切面，但禁止为了表面并行而无意义重复分析同一问题。
- 只要放出 subagent，默认优先使用当前可用的最强模型，且与主代理保持同档；禁止为了省资源、图快或习惯性降档到更弱模型。
- 若主代理处于最高档配置（例如 `GPT-5.4 xhigh`），则放出的 subagent 也必须保持同一最高档配置；其他档位同理，默认始终跟随主代理当前档位。
- 当任务可自然拆分为并行切面时，应优先拆成高价值子问题分派给 subagent；立即阻塞下一步的关键路径工作默认仍由主代理直接处理，不得把本可本地立即完成的急迫步骤外包后原地等待。
- subagent 适合承担：仓库摸底、调用链梳理、测试面扫描、风险交叉验证、局部方案比较、并行阅读多模块代码；最终收敛、取舍、改动落地与对人类的正式结论必须由主代理负责。
- 若工具链、权限或运行时限制导致无法放出 subagent，主代理必须明确说明阻塞原因；禁止静默降级为弱模型 subagent，或伪称已完成代理协作。
- 主代理在放出 subagent 前，必须先收敛本轮唯一目标、当前主链方案与拆分边界；禁止在“主方案未定”时同时让多个 subagent 各自发明实现路线，导致竞争性双轨落地。
- 并行编码前，主代理必须显式维护一份 `owner -> write set` 对照，至少写清每个代理负责的文件/目录范围；禁止只靠口头默认分工让多个代理自行理解边界。
- 并行改代码时必须满足“一块职责只允许一个写入 owner”：
  - 同一文件不可同时交给多个 subagent 修改
  - 同一模块中的同一能力不可并行做两版实现
  - 同一问题若需要多代理参与，默认一个负责改代码，其余只做审阅、验证或补测试
- 主代理与 subagent 之间也必须遵守写入边界；一旦某个切面已明确委派给 subagent，主代理不得同时在同一写入范围内另起一版实现，除非先明确终止或回收该委派。
- 若任何 subagent 在实现过程中发现自己需要扩张写入边界、跨出既定 `write set`、或触碰共享契约附近代码，则必须先停下来回到主代理重新确认 owner 与边界；未经确认不得自行扩写。
- subagent 任务描述必须显式写清：
  - 目标问题
  - 允许修改的文件/目录
  - 禁止触碰的共享区域
  - 期望输出是“实现”还是“只读审阅”
  任务边界不清晰时，宁可少放，不允许模糊委派。
- 若多个 subagent 的结论、补丁或设计假设发生冲突，主代理必须先停下来收敛成单一方案，再继续集成；禁止把互相竞争的 patch 一起合入后再靠人工碰运气。
- 对共享契约、公共数据模型、入口行为、默认策略这类会影响全局心智的改动，默认先由主代理定方案与接口，再把局部实现/测试下放；禁止让 subagent 各自改公共契约后再反向合并。
- subagent 的价值优先级应为“并行补充主链”，而不是“复制主链”。允许多个 subagent 并行验证同一问题，但不允许多个 subagent 并行提交同一问题的两套实现。
- 主代理对 subagent 不是“发包后等结果”的关系，而应保持持续 review：只要看到实现方向、代码风格、抽象层级、测试口径或清理力度有异样，就必须主动追问 subagent 是否考虑不足、是否偏离主链、是否留下了第二实现。
- subagent 交付时不能只汇报“已完成”，还必须主动报告：
  - 采用了什么实现思路
  - 改了哪些文件
  - 有哪些关键假设
  - 哪些地方自己不确定或觉得可能还不够到位
  - 是否观察到与主代理方案、仓库风格或其他 subagent 补丁存在潜在冲突
- 若 subagent 在执行中发现：
  - 主代理给的边界不清晰
  - 当前实现可能与仓库既有风格不一致
  - 同一能力已有另一条实现正在形成
  - 自己的补丁需要新增兼容层或临时桥接才能落地
  则必须先上报并发起讨论，不能闷头继续做出第二轨实现。
- 主代理在集成 subagent 结果前，必须至少完成一轮“探讨式复核”：
  - 不是只看测试是否通过
  - 还要看抽象是否收敛、命名是否一致、边界是否统一、是否出现多余 helper / fallback / 包装层
  - 若感觉 patch 只是“能跑”而非“收口”，应继续追问或要求返工
- 主代理在决定采纳 subagent 结果前，必须亲自审阅真实 diff、实际改动文件以及实际测试命令/结果；禁止只依据“已经完成”“测试通过”这类口头汇报直接合入。
- 若一个 subagent 的实现让主代理产生明显疑虑，默认动作不是自己立刻悄悄重写一版，而是先把疑点抛回给该 subagent 解释、修正或补证据；只有在确认其方案不成立或无法继续时，主代理才接管该写入范围。
- subagent 之间也应具备“探讨感”而非彼此隔离：当某个 subagent 的发现会影响另一块实现边界时，主代理应把该发现同步给相关 subagent，要求其显式确认是否需要调整，而不是各自按旧假设继续推进。
- 对风格一致性要求更高的任务（重构、规则收敛、公共 API、核心主链改动），默认必须带一轮“实现后互审”：
  - 一个 subagent 负责实现
  - 另一个 subagent 负责针对风格一致性、主链收敛度和残留清理做挑战式审阅
  但审阅代理默认只提问题，不得再并行落第二版实现。
- 若任务属于明显局部的小型热修、单文件低风险修补或时间敏感的阻断修复，允许主代理直接完成并补一轮轻量复核，而不是机械拉起完整“实现后互审”；但只要改动范围扩大、触及共享契约/公共模块或已经出现风格分歧，就必须立即升级回完整互审口径。
- 主代理最终提交前，必须额外检查一轮 subagent 协作残留：
  - 是否出现重复 helper / 重复分支
  - 是否留下临时兼容逻辑
  - 是否因并行开发产生双入口 / 双策略 / 双测试口径
  - 是否存在已无人使用但为兼容子补丁保留的残留代码

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
- 同一工作区下，禁止并发执行 `mvn ... compile` 与 `mvn ... test`；两者都会写 `target/classes` 与前端复制产物（尤其 `cypher-workbench/assets/*`），在 Windows 上会频繁触发文件锁并制造假失败。
- 需要同时验证编译和测试时，必须串行执行：先 `compile`，再按改动范围执行最小充分测试；禁止为了“提速”并发起两个 Maven 进程共享同一工作区输出目录。

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
- 当一轮改动已经形成单一清晰主题，且满足本节与 **12.1 任务完成定义** 时，默认直接按规范提交；除非人类明确要求暂不提交、继续合并后续改动，或要求拆成多轮提交。

## 12. 交付前检查清单（必须执行）
- 前置环境：
  - 若项目目录内存在 JDK（例如 `./jdk-21`），默认优先将其作为项目 JDK；只有当它不满足 **JBR 21 + JCEF** 基线时，才回退到外部 JDK
  - `JAVA_HOME` 必须指向 **JBR 21 + JCEF**
  - `java.vendor` 必须为 `JetBrains`
  - `${java.home}/jmods/jcef.jmod` 必须存在
  - PowerShell 下执行 Java/Maven/测试前，必须显式统一输出编码为 UTF-8，至少设置：
    - `chcp 65001 > $null`
    - `[Console]::InputEncoding  = [System.Text.UTF8Encoding]::new($false)`
    - `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
    - `$OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
    - `$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'`
  - 目的：终端 code page 即使已经是 UTF-8，Java 在 Windows 上默认 `stdout/stderr` 仍可能是 `GBK`，不显式设置会导致 Maven/Surefire/Javac 中文输出乱码
- 编译：
  - PowerShell 下优先使用：`mvn --% -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
  - `mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
- 编译/测试串行约束：
  - 同一工作区内，`compile` 与 `test` 必须串行执行，禁止并发。
  - 原因：两者会同时改写 `target/classes` 和 `cypher-workbench/assets/*`，Windows 下极易出现 `FileSystemException`/文件占用假失败。
  - 默认顺序：先 `compile`，再执行目标测试。
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
- PowerShell 下统一 UTF-8 输出前缀：
  - `chcp 65001 > $null`
  - `[Console]::InputEncoding  = [System.Text.UTF8Encoding]::new($false)`
  - `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `$OutputEncoding = [System.Text.UTF8Encoding]::new($false)`
  - `$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'`
- 编译：
  - `mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
  - PowerShell 下优先使用：`mvn --% -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile`
- 串行执行约束：
  - 同一工作区内，禁止并发执行 `compile` 与 `test`
  - 推荐顺序：先 `mvn ... compile`，再 `mvn ... -Dtest=<TestClass> test`
  - 原因：两者会竞争写入 `target/classes` 和 `cypher-workbench/assets/*`，Windows 文件锁会导致假失败
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
