# Jar Analyzer 全量通读审查报告（2026-02-01）

## 通读计划
1. 梳理入口与关键流程（CLI / GUI / Server）。
2. 通读 core/engine/utils/db/taint/dfs/semantic 等核心模块。
3. 通读 GUI / 搜索 / Lucene / Server API 处理链路。
4. 通读子项目（agent / mcp / native）与 rules / build / release 脚本。
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
- 调用图与 DFS：`MethodCall*` + `InheritanceRunner` 生成调用边，`DFSEngine` 提供路径探索与剪枝。
- 反编译：FernFlower + CFR，`DecompileEngine` 提供缓存与行号映射。
- 语义能力：`semantic`/`TypeSolver` 结合 JavaParser 与字节码缓存，进行方法/类型解析。
- 安全分析：
  - 污点分析（`taint`）基于 DFS 结果做传播与验证。
  - 漏洞/SCA/泄漏/组件等基于规则文件与资源扫描。
- 检索能力：字符串/方法/类查询（DB + FTS），Lucene 全局检索，资源内容检索。
- GUI：Swing 多面板、文件树、代码区、右键跳转、配置与主题。
- Server API：NanoHTTPD 提供 API/页面/资源读取，DFS/Taint Job。
- MCP/N8N：Go 服务桥接 API，提供自动化流程工具。
- Java Agent：Tomcat 等容器运行时枚举与注入点收集。

## 问题清单（按风险级别）
### P0 / 高风险（功能正确性或安全暴露）
1. **数据库“覆盖”逻辑未清空历史数据导致污染**【已修复】
   处理：在 overwrite 路径执行全表清理。
2. **API 默认对外暴露且无认证**【未修复-安全】
   默认 `serverBind=0.0.0.0`、`serverAuth=false`，token 固定，存在暴露风险。
3. **Agent 默认弱口令与开放端口**【未修复-安全】
   默认 `12345678` + 10033 监听，未强制修改且无 TLS。
4. **Taint 分析遇到单个 class 读取失败会直接返回空结果**【已修复】
   处理：改为标记链路未证实并继续汇总，不再整体返回空结果。
5. **数据库会话全局单例且跨线程共享**【已修复】
   处理：DatabaseManager 改为每次调用独立 SqlSession/Mapper，移除 synchronized，恢复并行性能。

### P1 / 中风险（稳定性/一致性/误报漏报）
1. **CLI 分析 `.class` 时可能 NPE**【已修复】
   处理：不再强依赖 MainForm，缺失时回退 ASM 读类名。
2. **单实例锁未保持**【已修复】
   处理：持久保存 FileLock/RandomAccessFile，避免锁被释放导致多开。
3. **资源提取路径存在冲突风险**【已修复】
   处理：`jarId=-1` 时引入路径哈希，避免资源覆盖。
4. **`clearAnalyzeEnv` 在异常场景可触发 NPE**【已修复】
   处理：inheritanceMap 为空时跳过清理。
5. **CORS 全开放**【未修复-安全】
   `Access-Control-Allow-Origin: *` 在未启用 auth 时存在跨域利用风险。
6. **命令执行未做路径引用/转义**【未修复-安全】
   Windows `start` / `explorer.exe /select` 未加引号，含空格路径可能失败且有拼接风险。

### P2 / 低风险（兼容性/体验/可维护性）
1. **文本编码依赖系统默认值**【已修复】
   多处 `new String(bytes)` / `FileWriter` 未指定 UTF‑8，已统一为 UTF‑8。
2. **Swing 线程安全**【已修复】
   `DatabaseManager` 等后台逻辑直接更新 GUI 的路径已切到 EDT。
3. **HTML 报告生成对输入格式过于依赖**【已修复】
   空输入/无冒号/异常 CVSS 已做容错。
4. **缺少对新旧索引/缓存一致性的显式提示**【已修复】
   重建数据库后已显式刷新 Lucene/索引/反编译缓存。

## 建议（不改代码层面的方向性建议）
1. **默认安全基线**：Server 默认绑定 `127.0.0.1` + 启用 auth + 首次运行生成随机 token；Agent 默认随机口令并启动时强提示。
2. **数据库构建清理策略**：覆盖模式明确清表或强制删除旧 DB 并提示影响。
3. **并发与线程模型**：避免全局 SqlSession；使用“每次调用独立 session + 自动关闭”或统一同步策略。
4. **CLI/GUI 彻底解耦**：核心解析逻辑避免直接引用 `MainForm`，由 UI 层统一回调更新进度。
5. **资源/临时目录隔离**：对嵌套 jar 或 `jarId=-1` 使用稳定且唯一命名（hash/path）。
6. **编码规范**：统一 UTF‑8 读写（包含 YAML/报告/配置），确保跨平台一致性。

## 需要你确认/我做出的假设
- CLI 模式是否需要支持 `.class` 单文件输入？若不需要，可在 CLI 入口明确拒绝；若需要，应继续避免 `MainForm` 依赖。
- API 默认对外暴露是否是设计需求（便于局域网集成）？若是，建议在 UI/启动时强提示。
- 嵌套 jar 的 jarId 设计是否有意指向父 jar？若不是，应考虑为子 jar 创建独立 jar 记录。
