# Jar Analyzer (JDK 21)

`jar-analyzer` 是一个面向 Java 代码审计的离线静态分析工具：使用 **ASM** 扫描字节码，在本地用 **Neo4j Embedded（官方依赖）** 建库，然后在同一套数据上提供 **GUI / HTTP API / MCP** 三种使用方式。

## 关键点

1. **运行/构建基线：JDK 21**（本项目自身用 JDK 21 编译与运行）
2. **分析目标兼容：低版本 JAR**（仍可分析 Java 6/7/8/11/17 等目标字节码，解析由 ASM 完成）
3. **架构统一：不依赖外部服务/额外进程**（MCP 已内置为 Java 实现；Neo4j 以进程内 Embedded 方式运行）
4. **数据库优先：先建库再查询/分析**（构建阶段写入当前 active project 的 Neo4j store，后续所有功能都围绕该 store 工作）
5. **rmclassic 硬切：主路径单轨**（Flow+Cypher+非 Flow 兼容桥接已下线，主分支不再提供 classic/legacy fallback）

## 环境要求

1. `Java 21+`（必须）
2. `Maven 3.x`（从源码构建需要）
3. `Python 3`（可选，用于 `build.py` 生成发版目录结构）

## 快速开始（推荐：使用 Release 包）

Release 目录结构（由 `build.py` 生成）仅保留一个包：

1. `*-21`：自带 `jre/`（必须是 **JBR 21 + JCEF** 运行时），默认使用 ZGC

启动：

1. Windows：双击 `start.bat`
2. Linux/macOS：执行 `./start.sh`

启动后会打开 GUI，同时在后台启动 HTTP API 服务（默认 `0.0.0.0:10032`）。

## 从源码构建

构建核心（fat jar）：

```bash
mvn -B clean package -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true
```

产物：

1. `target/jar-analyzer-<version>-jar-with-dependencies.jar`

本地直接运行（推荐用 `java -jar`）：

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar
```

生成发版目录（可选）：

```bash
python3 build.py --os macos --jbr /abs/path/to/jbr-21 --clean
```

可选参数：
1. `--jcef /abs/path/to/jcef-overlay`：额外 JCEF 覆盖目录（会覆盖复制到 `jre/`）

## 工作流程（GUI）

### 1) 启动与 API

GUI 启动时会同时启动内置 HTTP API 服务：

1. 默认地址：`http://127.0.0.1:10032`
2. 端口/Bind/Auth/Token 通过 GUI 的 `API Startup Config` 保存（写入 `.jar-analyzer`，重启生效）
3. GUI 的「API」Tab 会展示当前 API 配置，并提供 API/MCP/n8n 文档入口

### 2) 选择输入与构建数据库

核心流程是“建库”：

1. 启动后会先进入欢迎页：
   - `新建临时项目`：激活当前会话临时项目（`projectKey=temp-<session-id>`）
   - `新建项目`：创建新的项目键并初始化独立 Neo4j store
   - `打开项目`：从已注册项目列表切换 active project
2. 选择输入：`jar/war/class/目录(字节码)`（仅字节码输入；可选解析 fatjar 内嵌依赖）
3. 点击构建/分析按钮开始建库
4. 构建完成后会生成/更新对应项目库，并在 GUI 显示类/方法/边数量与 DB 大小：
   - 临时项目：`db/neo4j-temp/<session-id>/`（退出进程自动清理）
   - 正式项目：`db/neo4j-projects/<project-key>/`（项目间隔离）

### 2.1) 多项目生命周期（单活模型）

1. 项目类型仅两种：`TEMP`（会话临时）和 `PERSISTENT`（正式项目）
2. 项目注册（PERSISTENT）：绑定 `inputPath`（可选 `alias/runtimePath/resolveNestedJars`）
3. 项目切换：同一时刻只有一个 active project 对外提供查询/Flow 数据
4. 项目删除：可仅删注册信息，或 `deleteStore=true` 同时删除本地 Neo4j store
5. 重启行为：启动后始终进入 TEMP；PERSISTENT 注册列表会恢复

建库阶段会做的事情（高层）：

1. 发现/解析阶段：收集 class/header、方法签名、注解、资源索引、callsite/局部变量等元数据
2. 归属分类阶段：按 `forceTarget > sdk > commonLibrary > appHeuristic` 划分 APP/LIBRARY/SDK
3. 调用图阶段：统一使用 Tai-e（默认 `balanced` 档位），默认保留 APP 可达子图（APP + 可达 LIBRARY caller），不再回退 bytecode 调用图
4. 全 common jar 策略：默认 `continue-no-callgraph`（继续建库但不产出调用图边）
5. 写库阶段：写入 Neo4j，`call_graph_mode` 元数据为 `taie:<profile>` 或 `disabled-no-target`

### 3) 查询与定位（审计日常）

建库后常用能力：

1. 方法/类快速定位（精确/模糊）
2. 字符串/资源检索（定位关键配置、路由、危险参数拼接）
3. 调用关系跳转（caller/callee/实现类）
4. Spring/JavaWeb 入口枚举与映射查看

### 4) DFS 调用链

DFS 用于“从 source 到 sink”或“以 sink 反推 source”的链路搜索：

1. 配置最大深度/最大次数/过滤规则，避免路径爆炸
2. 支持把“可传播约束”前置到搜索过程（可选 stateful prune），减少无效扩展
3. 结果可导出，用于复现与报告
4. 当前为 **graph-only** 后端（已下线 classic fallback）

### 5) 污点验证

对 DFS 链路可进一步做字节码级污点传播验证：

1. 基于 summary/barrier/additional 规则集
2. 支持不同 profile（保守/平衡/激进），在漏报与误报之间做权衡
3. 输出传播证据，帮助做最终人工确认
4. `seed` 参数已移除，source 端口由引擎自动推断（this + params + 启发式）

### 5.1) TODO（污点引擎收敛）

1. 当前污点主链保持单引擎：`GraphTaintEngine`（graph + summary），不长期维护“双引擎并跑（Graph + Tai-e taint）”
2. `source/model/sink` 规则继续作为唯一规则源，优先提升现有链路精度与可读性（规则质量、summary 命中、Cypher 查询体验）
3. 如后续评估切换 Tai-e taint，采用“硬切迁移”：短期迁移窗口后删除旧链路，不保留长期双轨

## 硬切迁移说明（rmclassic）

以下变更为破坏性调整，不再自动兼容旧行为：

1. Search 后端统一走 contributor（旧模式仅保留为 preset，不再有 legacy executor）
2. Project Tree 仅消费 `project_model_*` 快照；缺失时返回 `project_model_missing_rebuild`
3. 图查询 / DFS / Taint / Cypher Explain 在 active project 构建中统一返回 `project_build_in_progress`
4. FTS 为必需能力：字符串检索不再从 FTS 异常回退到 LIKE
5. `jar.analyzer.taint.propagation` 仅支持 `strict|balanced`（`compat|legacy` 直接报错）
6. MCP 协议严格化：`initialize.protocolVersion` 必填且校验；unknown tool 返回 `METHOD_NOT_FOUND`
7. 分析范围规则硬切：
   - 仅 `rules/analysis-scope.json`（统一 force-target/sdk/common-library 规则）
8. 规则体系统一：`rules/sink.json`（sink 检索 + sink kind 分类）、`rules/source.json`（source/sourceAnnotations）、`rules/model.json`（summary/sanitizer/guard/additional）
9. 项目模型已切换为 `TEMP/PERSISTENT`（不再有 `default` 业务项目）
10. 首次进入新模型会清理旧项目库与旧注册表，不保留历史数据
11. 非 all-common 场景下，Tai-e 失败会直接终止建库（不再回退 bytecode 调用图）

## 自动化接口：HTTP API 与 MCP

### HTTP API

API 面向自动化与集成（脚本、CI、外部平台等）：

1. GUI 启动时默认开启
2. 可选开启鉴权（API Token），用于保护敏感查询能力
3. MCP 工具底层复用同一套 `/api/*` 逻辑（但 MCP 调用是进程内完成，不需要额外代理进程）

### MCP（内置）

MCP 已内置在 GUI 的「API」Tab 下的 `MCP` 区域，可按“分线”启动不同工具集合：

1. audit-fast（默认 `20033`）
2. graph-lite（默认 `20034`）
3. dfs（默认 `20035`）
4. sca-leak（默认 `20036`）
5. sink-rules（默认 `20037`）
6. report（默认 `20081`，用于上报；可选开启 report Web UI `20080`）

连接方式：

1. `SSE`：`http://127.0.0.1:<port>/sse`
2. `streamable-http`：`http://127.0.0.1:<port>/mcp`

鉴权（两套 Token，概念要分清）：

1. `API Token`：保护 jar-analyzer 的 `/api/*`
2. `MCP Token`：仅对 MCP 的 `tools/call` 生效（可单独开启）

## CLI（仅建库）

### 仅建库（无 GUI）

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar build --jar /path/to/app.jar --del-exist
```

常用开关：

1. `--del-exist`：删除旧 DB
2. `--del-cache`：删除临时缓存目录
3. `--inner-jars`：解析 jar in jar（fatjar 内嵌依赖）

默认行为：
1. 调用图引擎固定为 Tai-e（默认 `jar.analyzer.analysis.profile=balanced`）
2. 若输入全部命中 common library，默认 `jar.analyzer.all-common.policy=continue-no-callgraph`
3. 非 all-common 场景不再回退 bytecode 调用图

常用系统属性：
1. `jar.analyzer.analysis.profile`: `balanced|high|fast`
2. `jar.analyzer.all-common.policy`: 默认 `continue-no-callgraph`
3. `jar.analyzer.jdk.modules`: 默认 `core`（JDK9+ 为 `java.base,java.desktop,java.logging`）
4. `jar.analyzer.taie.edge.policy`: `app-caller|reachable-app|non-sdk-caller|full`（默认 `reachable-app`）

### 启动 GUI + API

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar
```

兼容占位命令（不再接收 GUI 参数）：

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar gui
```

## 生成文件与目录

运行目录下常见产物：

1. `db/neo4j-temp/<session-id>/`：会话临时项目 Neo4j store（退出自动清理）
2. `db/neo4j-projects/<project-key>/`：正式项目独立 Neo4j Embedded store（核心产物）
3. `.jar-analyzer-projects.json`：正式项目注册表（PERSISTENT 列表及元数据）
4. `jar-analyzer-temp/`：临时目录（解包/缓存）
5. `.jar-analyzer`：本地配置文件（properties，包含 API/MCP 启动配置等）
7. `logs/`：日志（如启用）

## 测试

```bash
mvn -q test
```

## 文档

本仓库的本地文档统一放在 `doc/`：

1. `doc/README.md`（索引）
2. `doc/README-api.md`（HTTP API）
3. `doc/mcp/README.md`（MCP）
4. `doc/n8n/README.md`（n8n 自动化示例）

## License

GPLv3（见 `LICENSE`）。
