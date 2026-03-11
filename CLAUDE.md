# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jar Analyzer is an offline static analysis tool for Java code auditing. It scans bytecode with ASM, stores results in an embedded Neo4j database, and exposes GUI / HTTP API / MCP interfaces — all running in a single JVM process. The project is primarily Chinese-authored; commit messages, README, and comments use Chinese.

## Build & Run Commands

**Compile (fast check):**
```bash
mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile
```

**Package (fat jar):**
```bash
mvn -q -DskipTests package
```
Output: `target/jar-analyzer-<version>-jar-with-dependencies.jar`

**Run:**
```bash
java -Xms2g -Xmx6g -jar target/jar-analyzer-*-jar-with-dependencies.jar
```

**Run a single test:**
```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest=<TestClass> test
```

**Run all tests:**
```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true test
```

**Run benchmarks:**
```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Pbench-nav test
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Pbench-search test
```

**Build release directory:**
```bash
python3 build.py --os windows --jbr /path/to/jbr-21 --clean
```

开发时可在 `compile` 或目标测试中使用 `-Dskip.npm=true -Dskip.installnodenpm=true` 跳过前端构建；正式 `package` 不应携带该参数。前端位于 `frontend/cypher-workbench/`，通过 `frontend-maven-plugin` 在打包阶段构建并打进产物。

## Environment Requirements

- **JDK 21** (mandatory for build and runtime; `maven.compiler.release=21`)
- Maven 3.x
- JDK 17+ profile auto-activates `--enable-native-access=ALL-UNNAMED` for surefire
- **本地 JDK**：项目根目录下的 `jdk-21/` 是 JetBrains Runtime（含 JCEF），编译和运行时应优先使用它。设置 `JAVA_HOME` 指向该目录：
  ```bash
  export JAVA_HOME="$(pwd)/jdk-21"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```

## Architecture

### Single-process monolith
Entry point: `me.n1ar4.jar.analyzer.starter.Application`. Three modes:
1. **GUI mode** (default) — launches Swing GUI + embedded HTTP server (default port 10032)
2. **CLI build mode** (`build --jar <path>`) — headless build via `BuildCmd`/`Client`
3. **Help** (`-h`)

### Core packages (`src/main/java/me/n1ar4/jar/analyzer/`)

| Package | Role |
|---------|------|
| `core/` | Build pipeline: discovery → scope classification → bytecode-mainline call graph → Neo4j import |
| `core/scope/` | APP/LIBRARY/SDK classification via `analysis-scope.json` rules |
| `storage/neo4j/` | Neo4j embedded lifecycle, bulk import, project registry, custom procedures |
| `graph/` | Graph queries, DFS path search, taint flow engine, Cypher execution |
| `taint/` | Taint propagation analysis with JVM simulation and method summaries |
| `rules/` | DSL rule compiler, model/sink/source registries with hot-reload |
| `engine/` | Query engine, CFR decompiler, class lookup, search |
| `server/` | NanoHTTPD-based HTTP API server and request handlers |
| `mcp/` | MCP protocol server (Java-native, in-process) |
| `gui/runtime/` | GUI state layer: facades, DTOs, bootstrap |
| `gui/swing/` | Swing UI panels, JCEF Cypher workbench integration |
| `analyze/` | ASM visitors for bytecode/Spring/annotation analysis |
| `sca/` | Software Composition Analysis (dependency vulnerability scanning) |
| `leak/` | Regex-based sensitive data leak detection |
| `semantic/` | Type resolution, call-site analysis, overload resolution |

### Build pipeline (`CoreRunner`)
1. **Discovery** — scan class/method/callsite/annotation metadata via ASM
2. **Scope classification** — `forceTarget > sdk > commonLibrary > appHeuristic`
3. **Call graph** — bytecode-mainline analysis with semantic edges and selective PTA
4. **Import** — batch write to Neo4j project store via `DatabaseManager`

### Single-active project model
Only one project is active at a time. Projects are either `TEMP` (session-scoped, auto-cleaned) or `PERSISTENT` (stored in `db/neo4j-projects/<key>/`). Managed by `ActiveProjectContext` + `Neo4jProjectStore`.

### Rule files (`rules/`)
- `sink.json` — sink methods by severity/category
- `source.json` — taint source methods and annotations
- `model.json` — summary/sanitizer/guard propagation models
- `analysis-scope.json` — scope classifier prefix lists
- `semantic-hints.json` — authorization/validation annotation categories

Registries (`SinkRuleRegistry`, `ModelRegistry`) support versioned hot-reload.

### Separate modules
- `agent/` — Java instrumentation agent (Java 8 target) for runtime endpoint discovery in live JVMs
- `frontend/cypher-workbench/` — TypeScript/Vite SPA (CodeMirror + D3) embedded via JCEF
- `native/` — JNI C library for Windows ANSI console support
- `build/` — Windows launcher (`start.exe`) built with GCC

## Commit Style (mandatory)

Use Chinese type prefixes. Format: `[类型] 动作 + 对象 + 结果`

Allowed prefixes: `[重构]`, `[修复]`, `[测试]`, `[文档]`, `[优化]`, `[其他]`

Examples:
- `[重构] 收敛规则加载链路并移除冗余分支`
- `[修复] 修复建库原子更新期间的半状态可见问题`

English prefixes (`fix:`, `feat:`, `refactor:`) are forbidden. One clear topic per commit.

## Key Constraints (from AGENTS.md)

- **No legacy/classic fallbacks** — the project has completed a "rmclassic" hard-cut migration
- **Bytecode-mainline is the sole call graph source** — no second edge generation path allowed
- **Neo4j is the sole storage** — no SQL compatibility remnants
- **Convergence over coexistence** — same capability must have only one implementation path
- **Dead code must be deleted completely** — not commented out or gated behind flags
- **GUI must not implement analysis logic** — only orchestration and display
- **Rule interpretation only in `rules/` and `taint/`** — not scattered in handlers/UI
