# 文档（JDK 21）

本目录存放 `jar-analyzer` 的文本说明文档（不依赖截图）。项目本身以 **JDK 21** 构建/运行，但仍可分析低版本目标字节码（由 ASM 解析 classfile）。

## GUI 快速入口

GUI 的「API」Tab 右上方提供三个按钮，可直接打开本地文档：

- `API 文档` -> `doc/README-api.md`
- `MCP 文档` -> `doc/mcp/README.md`
- `高级教程`（n8n 等自动化）-> `doc/n8n/README.md`

## 主题文档

- `doc/README-screenshot.md`：GUI 模块导览（文字版）
- `doc/README-el.md`：表达式搜索语法（方法级筛选）
- `doc/README-advance.md`：`advance` 模块（CFG/Frame/HTML Graph + 内置工具）
- `doc/README-no-taie.md`：去 Tai-e 主链化设计（建库主链、当前状态、迁移阶段与验收指标）
- `doc/README-no-taie-phase0.md`：Phase 0 实施文档与 issue 清单（当前已收口，可直接作为关闭记录）
- `doc/README-no-taie-dev-kernel-inventory.md`：`dev` 分支可迁移分析内核白名单（迁/拆迁/不迁裁决）
- `doc/README-no-taie-fact-snapshot.md`：`BuildContext -> BuildFactSnapshot` 适配层设计（输入 facts 与边输出分离，含当前实现态）
- `doc/README-no-taie-pta-subset.md`：`ContextSensitivePtaEngine` 最小可迁子集设计（Selective PTA 白名单与不迁边界）
- `doc/README-no-taie-bytecode-fact-runner.md`：单次解析前端收口说明（workspace/facts/edge owner 与 Phase 0 收口结论）
- `doc/README-no-taie-profile-switch.md`：`bytecode-mainline` profile 切换口径与已移除历史配置值（`JA-NT-107`）
- `doc/README-no-taie-regression-pack.md`：去 Tai-e 迁移最小回归包（`JA-NT-108`）
- `doc/benchmarks/no-taie-baseline-20260311.md`：当前 ASM+Tai-e 主线基线快照（Phase 0 冻结报告）
- `doc/README-note.md`：注意事项 / 常见问题
- `doc/README-sub.md`：子项目说明（Tomcat Analyzer / Y4-LOG 等）
- `doc/README-test.md`：性能与压测建议（基准方法与 JVM 参数）
- `doc/README-others.md`：一些原理说明与外部资料
- `doc/README-thanks.md`：致谢
