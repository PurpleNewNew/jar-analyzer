## 性能与压测建议（JDK 21）

Jar Analyzer 以 **JDK 21** 构建/运行。建库属于 IO + CPU + 内存混合负载，性能会受到目标 JAR 规模、机器磁盘/CPU、以及 JVM 参数影响。

以下是一个更可复现的“本地基准”做法（建议用 CLI 进行，避免 GUI 交互带来的波动）。

### 1) 固定输入与清理环境

- 固定同一份输入（同一个 `jar/war/目录`）
- 每次测试前删除旧产物：
  - `jar-analyzer.db`
  - `jar-analyzer-temp/`

CLI 示例（仅建库，无 GUI）：

```bash
java -jar target/jar-analyzer-*-jar-with-dependencies.jar build \
  --jar /path/to/app.jar \
  --del-exist \
  --del-cache
```

### 2) 固定 JVM 参数

建议先固定内存，再对比不同 GC（示例）：

```bash
# G1（默认）
java -Xms4g -Xmx8g -jar target/jar-analyzer-*.jar build --jar /path/to/app.jar --del-exist --del-cache

# ZGC（可选对比）
java -Xms4g -Xmx8g -XX:+UseZGC -jar target/jar-analyzer-*.jar build --jar /path/to/app.jar --del-exist --del-cache
```

经验上：

- `ZGC` 更关注暂停时间，某些场景下可能带来更高的内存占用
- `G1GC` 更通用，通常是默认首选

### 3) 记录可对比指标

建议至少记录：

- 建库总耗时（wall time）
- `jar-analyzer.db` 文件大小
- 类/方法/边数量（GUI 信息面板或日志中可见）
