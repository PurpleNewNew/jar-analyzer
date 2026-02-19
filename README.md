# Jar Analyzer (JDK 21)

`jar-analyzer` 是一个面向 Java 代码审计的离线静态分析工具：使用 **ASM** 扫描字节码，在本地用 **SQLite** 建库，然后在同一套数据上提供 **GUI / HTTP API / MCP** 三种使用方式。

## 关键点

1. **运行/构建基线：JDK 21**（本项目自身用 JDK 21 编译与运行）
2. **分析目标兼容：低版本 JAR**（仍可分析 Java 6/7/8/11/17 等目标字节码，解析由 ASM 完成）
3. **架构统一：不依赖外部服务/额外进程**（MCP 已内置为 Java 实现；默认不引入 Neo4j，不做 SootUp 后端）
4. **数据库优先：先建库再查询/分析**（构建阶段生成 `jar-analyzer.db`，后续所有功能都围绕 DB 工作）
5. **rmclassic 硬切：主路径单轨**（Flow+Cypher+非 Flow 兼容桥接已下线，主分支不再提供 classic/legacy fallback）

## 环境要求

1. `Java 21+`（必须）
2. `Maven 3.x`（从源码构建需要）
3. `Python 3`（可选，用于 `build.py` 生成发版目录结构）

## 快速开始（推荐：使用 Release 包）

Release 目录结构（由 `build.py` 生成）通常包含三种启动方式：

1. `*-system`：使用系统 `JAVA_HOME` 启动（你需要自己安装 JDK 21）
2. `*-full`：自带 `jre/`，默认使用 G1GC
3. `*-21`：自带 `jre/`，默认使用 ZGC（更激进的 GC 选项）

启动：

1. Windows：双击 `start.bat`
2. Linux/macOS：执行 `./start.sh`

启动后会打开 GUI，同时在后台启动 HTTP API 服务（默认 `0.0.0.0:10032`）。

## 从源码构建

构建核心（fat jar）：

```bash
mvn -B clean package -DskipTests
```

产物：

1. `target/jar-analyzer-<version>-jar-with-dependencies.jar`

本地直接运行（推荐用 `java -jar`）：

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar gui -p 10032
```

生成发版目录（可选）：

```bash
python3 build.py --os macos   # windows|linux|macos
```

## 工作流程（GUI）

### 1) 启动与 API

GUI 启动时会同时启动内置 HTTP API 服务：

1. 默认地址：`http://127.0.0.1:10032`
2. 端口/Bind/Auth/Token 可通过启动参数调整（见下方“CLI 参数”）
3. GUI 的「API」Tab 会展示当前 API 配置，并提供 API/MCP/n8n 文档入口

### 2) 选择输入与构建数据库

核心流程是“建库”：

1. 选择输入：`jar/war/目录(classes)`（支持多文件；可选解析 fatjar 内嵌依赖）
2. 点击构建/分析按钮开始建库
3. 构建完成后会生成/更新 `jar-analyzer.db`（SQLite），并在 GUI 显示类/方法/边数量与 DB 大小

建库阶段会做的事情（高层）：

1. 发现阶段：收集 class/header、方法签名、注解、资源索引等
2. 解析阶段：扫描 method body，构建直接调用边、字符串常量、Web/Spring 入口等
3. 符号阶段（非 quick 模式）：补充 callsite/局部变量信息，用于更精确的边与类型推断
4. 继承/分派：扩展虚调用、override、类型分派边
5. 语义补边：补全线程/线程池回调、`doPrivileged` 等高价值语义边（带证据/置信度）

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

## 硬切迁移说明（rmclassic）

以下变更为破坏性调整，不再自动兼容旧行为：

1. Search 后端统一走 contributor（旧模式仅保留为 preset，不再有 legacy executor）
2. Project Tree 仅消费 `project_model_*` 快照；缺失时返回 `project_model_missing_rebuild`
3. FTS 为必需能力：字符串检索不再从 FTS 异常回退到 LIKE
4. `jar.analyzer.taint.propagation` 仅支持 `strict|balanced`（`compat|legacy` 直接报错）
5. MCP 协议严格化：`initialize.protocolVersion` 必填且校验；unknown tool 返回 `METHOD_NOT_FOUND`
6. 规则/配置文件名硬切：
   - 仅 `rules/search-filter.json`（不再读 `rules/common-filter.json`）
   - 仅 `rules/common-whitelist.json`（不再读 `rules/common-allowlist.json`）
7. `rules/vulnerability.yaml` 仅接受 `!!me.n1ar4.jar.analyzer.rules.vul.Rule`
8. 不再执行旧 DB 路径自动迁移；如使用历史数据请手工迁移或重建

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
5. vul-rules（默认 `20037`）
6. report（默认 `20081`，用于上报；可选开启 report Web UI `20080`）

连接方式：

1. `SSE`：`http://127.0.0.1:<port>/sse`
2. `streamable-http`：`http://127.0.0.1:<port>/mcp`

鉴权（两套 Token，概念要分清）：

1. `API Token`：保护 jar-analyzer 的 `/api/*`
2. `MCP Token`：仅对 MCP 的 `tools/call` 生效（可单独开启）

## CLI 参数（常用）

### 仅建库（无 GUI）

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar build --jar /path/to/app.jar --del-exist
```

常用开关：

1. `--del-exist`：删除旧 DB
2. `--del-cache`：删除临时缓存目录
3. `--inner-jars`：解析 jar in jar（fatjar 内嵌依赖）

### 启动 GUI + API

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar gui -p 10032 -sb 0.0.0.0 -sa -st JAR-ANALYZER-API-TOKEN
```

常用开关：

1. `-p/--port`：API 端口（默认 `10032`）
2. `-sb/--server-bind`：Bind 地址（默认 `0.0.0.0`）
3. `-sa/--server-auth`：开启 API 鉴权
4. `-st/--server-token`：API Token
5. `-t/--theme`：主题
6. `-l/--log-level`：日志级别（debug|info|warn|error）
7. `-sec/--security`：安全模式（敏感操作二次确认）

## 生成文件与目录

运行目录下常见产物：

1. `jar-analyzer.db`：SQLite 数据库（核心产物）
2. `jar-analyzer-temp/`：临时目录（解包/缓存）
3. `.jar-analyzer`：本地配置文件（properties，包含 MCP 开关/端口等）
4. `logs/`：日志（如启用）

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
