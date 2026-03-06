## 目录结构

- `org.benf.cfr` CFR 反编译相关源码
- `me.n1ar4.log` 模仿 `Apache Log4j2 API` 的日志库

反编译策略：

- 当前仅保留 `CFR` 作为唯一反编译器

数据库层说明：

- 现在统一使用官方 `Neo4j Embedded` Maven 依赖
- 不再维护或平铺历史数据库驱动源码
