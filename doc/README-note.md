## 注意事项

### 体积与资源消耗

请谨慎分析数量极多或体积巨大的 `JAR`/目录：

- 构建阶段会解包/缓存，临时目录与 SQLite 数据库可能迅速膨胀
- 类/方法/边数量越大，建库耗时与内存占用越高
- 建议优先分析“业务模块”或使用白名单/黑名单减少噪声

常见缓解方式：

- 仅选择关键 `jar/war`，避免把整个依赖目录一次性喂给工具
- 关闭不需要的“额外补边/额外扫描模块”，分阶段跑（先建库，再按需做 DFS/污点/扫描）
- 为 JVM 提供足够内存（例如 `-Xms2g -Xmx6g` 起步，视目标体积调整）

### 生成文件/目录（运行目录）

常见产物（可通过 GUI 的清理按钮一键删除）：

- `jar-analyzer.db`：SQLite 数据库（核心产物）
- `jar-analyzer-temp/`：解包/缓存临时目录
- `.jar-analyzer`：本地配置（properties，包含 MCP 开关/端口等）
- `logs/`：日志目录
- `jar-analyzer-document/`：部分图形化分析/报告输出目录（如 HTML Graph）
- `jar-analyzer-export/`：导出结果目录（如导出代码/链路等）
- `jar-analyzer-download/`：远程加载/下载相关缓存（如启用）

### Windows 乱码

注意：
- 在 `Windows` 下请勿双击启动，请使用 `java -jar` 或双击 `bat` 脚本启动
- 如果使用 `java -jar` 启动乱码，请加入 `-Dfile.encoding=UTF-8` 参数

### GUI 显示/缩放

GUI 会受系统缩放影响：

- Windows：建议把缩放调整为 `100%` 或接近 `100%` 再尝试
- macOS：如显示不完整，可在显示器设置中选择“更多空间”

### 原理概览（高层）

Jar Analyzer 的核心流程：

- 解压所有 `Jar` 文件到 `jar-analyzer-temp` 目录
- 扫描字节码并在当前目录构建 `jar-analyzer.db`（SQLite）
- 使用 GUI/HTTP API/MCP 在同一份 DB 上做查询、调用图、DFS、污点与安全扫描

注意：当 `Jar` 数量较多或巨大时**可能导致临时目录和数据库文件巨大**
