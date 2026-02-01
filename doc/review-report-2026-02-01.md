# Jar Analyzer 全量通读审查报告（2026-02-01）

## 通读计划
1. 梳理入口与关键流程（CLI / GUI / Server）。
2. 通读 core/engine/utils/db/taint/dfs/semantic 等核心模块。
3. 通读 GUI / 搜索 / Lucene 与 Server API 处理链路。
4. 通读子项目（agent / mcp / native）与 rules / build / release 相关脚本。
5. 汇总问题与建议形成报告。

## 通读范围（已覆盖）
- 核心：`src/main/java/me/n1ar4/jar/analyzer`（core/engine/utils/taint/dfs/semantic/rules/config）。
- UI 与检索：`src/main/java/me/n1ar4/jar/analyzer/gui`、`lucene`、`engine/index`。
- 服务端：`src/main/java/me/n1ar4/jar/analyzer/server` 与所有 `handler`。
- 子项目：`agent/`（Java agent）、`mcp/`（Go MCP 服务）、`native/`（JNI 控制台）。
- 规则与构建：`rules/`、`build.py` 与发布脚本。

## 功能与设计理解（摘要）
- 入口与运行模式：`Application` 解析命令行（build/gui），初始化安全策略、主题、HTTP API、GUI。
- 输入解析与解包：`ClasspathResolver` 收集 jar/war/class/dir，`JarUtil` 解压到 temp 并收集资源。
- 数据库构建：`DiscoveryRunner`/`ClassAnalysisRunner` 解析字节码，`DatabaseManager` 写入 SQLite 与索引。
- 调用图与 DFS：`MethodCall*` + `InheritanceRunner` 生成调用边，`DFSEngine` 提供链路探索与剪枝。
- 反编译：FernFlower + CFR，`DecompileEngine` 提供缓存与行号映射。
- 语义能力：`semantic`/`TypeSolver` 结合 JavaParser 与字节码缓存，进行方法/类型解析。
- 安全分析：
  - 污点分析（`taint`）基于 DFS 结果做传播与验证；
  - 漏洞/SCA/泄漏/组件等基于规则文件与资源扫描。
- 检索能力：字符串/方法/类查询（DB + FTS），Lucene 全局搜索；资源内容检索。
- GUI：Swing 多面板、文件树、代码区、右键跳转、配置与主题。
- Server API：NanoHTTPD 提供 API/页面/资源读取、DFS/Taint Job。
- MCP 与 N8N：Go 服务桥接 API，实现自动化流程工具集。
- Java Agent：Tomcat 等容器运行时枚举与注入点收集。

## 问题清单（按风险级别）

### P0 / 高风险（功能正确性或安全暴露）
1. **数据库“覆盖”逻辑并未清空历史数据，导致数据重复/污染**：GUI 选择“overwrite database”只是不删除文件，后续写入直接叠加。`DatabaseManager` 没有执行清表。涉及 `src/main/java/me/n1ar4/jar/analyzer/gui/action/BuildAction.java` 与 `src/main/java/me/n1ar4/jar/analyzer/core/DatabaseManager.java`。
2. **API 默认对外暴露且无认证**：默认 `serverBind=0.0.0.0`、`serverAuth=false`，并且 token 默认固定字符串，局域网/同机其他用户可直接访问敏感源码与资源。涉及 `src/main/java/me/n1ar4/jar/analyzer/cli/StartCmd.java`、`src/main/java/me/n1ar4/jar/analyzer/server/PathMatcher.java`、`src/main/java/me/n1ar4/jar/analyzer/server/JarAnalyzerServer.java`。
3. **Agent 默认弱口令与开放端口**：`Agent` 默认 `12345678` + 10033 监听，未强制修改且无 TLS。可被同机/同网段滥用。涉及 `agent/src/main/java/com/n1ar4/agent/Agent.java`、`agent/src/main/java/com/n1ar4/agent/core/Task.java`。
4. **Taint 分析遇到单个 class 读取失败会直接返回空结果**：导致前面已计算链条全部丢失。涉及 `src/main/java/me/n1ar4/jar/analyzer/taint/TaintAnalyzer.java`。
5. **数据库会话全局单例且跨线程共享**：`DatabaseManager` 使用静态 `SqlSession` + mapper；在 GUI/Server/任务并发场景下有并发安全风险与写入异常风险。涉及 `src/main/java/me/n1ar4/jar/analyzer/core/DatabaseManager.java`。

### P1 / 中风险（稳定性/一致性/误报漏报）
1. **CLI 分析 `.class` 时可能 NPE**：`JarUtil.resolve()` 在 `.class` 分支读取 `MainForm`，CLI/无 GUI 场景可能不完整。涉及 `src/main/java/me/n1ar4/jar/analyzer/utils/JarUtil.java`。
2. **单实例锁未持有**：`Single.isInstanceRunning()` 获取锁后没有保存引用，锁可能被 GC/释放，导致多开。涉及 `src/main/java/me/n1ar4/jar/analyzer/starter/Single.java`。
3. **资源提取路径存在冲突风险**：`resources/<jarId>` 对 `jarId=-1`（嵌套 jar/未知 jar）会复用目录，可能覆盖不同 jar 资源。涉及 `src/main/java/me/n1ar4/jar/analyzer/utils/JarUtil.java`。
4. **`clearAnalyzeEnv` 在异常场景可触发 NPE**：`inheritanceMap` 未构建时直接调用 `.getInheritanceMap()`。涉及 `src/main/java/me/n1ar4/jar/analyzer/core/CoreRunner.java`。
5. **CORS 全开放**：API 响应统一 `Access-Control-Allow-Origin: *`，若用户未启用 auth 或使用默认 token，容易被恶意网页跨域利用。涉及 `src/main/java/me/n1ar4/jar/analyzer/server/PathMatcher.java`、`src/main/java/me/n1ar4/jar/analyzer/server/handler/base/BaseHandler.java`。
6. **命令执行未做路径引用/转义**：Windows `start` 与 `explorer.exe /select` 未加引号，路径含空格/特殊字符会失败，且存在命令拼接风险。涉及 `src/main/java/me/n1ar4/jar/analyzer/utils/OpenUtil.java`。

### P2 / 低风险（兼容性/体验/可维护性）
1. **文本编码依赖系统默认值**：多处 `new String(bytes)` / `FileWriter` 未指定 UTF-8，非 UTF-8 系统可能出现乱码或规则读取异常。涉及：
   - `src/main/java/me/n1ar4/jar/analyzer/config/ConfigEngine.java`
   - `src/main/java/me/n1ar4/jar/analyzer/utils/YamlUtil.java`
   - `src/main/java/me/n1ar4/jar/analyzer/gui/adapter/DecompileHelper.java`
   - `src/main/java/me/n1ar4/jar/analyzer/sca/utils/ReportUtil.java`
   - `src/main/java/me/n1ar4/jar/analyzer/starter/Logo.java`
   - `src/main/java/me/n1ar4/jar/analyzer/starter/ExpHandler.java`
2. **Swing 线程安全**：`DatabaseManager` 等核心逻辑在后台线程直接更新 GUI 组件，存在偶发 UI 不一致/卡顿风险。涉及 `src/main/java/me/n1ar4/jar/analyzer/core/DatabaseManager.java` 等。
3. **HTML 报告生成对输入格式过于依赖**：`ReportUtil.generateHtmlReport()` 假设漏洞文本每行必含 `:`，空输入或异常格式可能抛错。涉及 `src/main/java/me/n1ar4/jar/analyzer/sca/utils/ReportUtil.java`。
4. **缺少对新旧索引/缓存一致性的显式提示**：重建数据库后 Lucene/语义缓存是否完全刷新依赖用户操作，易产生“旧结果”。

## 建议（不改代码层面的方向性建议）
1. **默认安全基线**：Server 默认绑定 `127.0.0.1` + 启用 auth + 首次运行生成随机 token；Agent 默认随机口令，启动时强提示。
2. **数据库构建清理策略**：覆盖模式应清表或强制删除旧 DB（并提示影响），避免历史污染。
3. **并发与线程模型**：避免全局 `SqlSession`；改为“线程内 session + 自动关闭”，或在 `DatabaseManager` 增加统一同步策略。
4. **CLI/GUI 彻底解耦**：核心解析逻辑避免直接引用 `MainForm`；由 UI 层统一回调更新进度。
5. **资源/临时目录隔离**：对嵌套 jar 或 `jarId=-1` 使用更稳定且唯一的命名（hash/path），降低覆盖风险。
6. **编码规范**：统一 UTF-8 读写（包括 YAML/报告/配置），保证跨平台一致性。

## 需要你确认/我做出的假设
- CLI 模式是否需要支持 `.class` 单文件输入？若否，可在 CLI 入口明确拒绝；若是，则应改掉 `MainForm` 依赖。
- API 默认对外暴露是否为设计需求（便于局域网集成）？如果是，建议在 UI/启动时强弹警告。
- 嵌套 jar 的 jarId 设计是否有意指向父 jar？如果不是，应考虑为嵌套 jar 创建独立 jar 记录。