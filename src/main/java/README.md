## 目录结构

- `com.github.rjeschke.txtmark` 一个开源的 `markdown` 解析库
- `org.jetbrains.java.decompiler` 来自 `jetbrains` 的 `fern-flower` 反编译库
- `me.n1ar4.log` 模仿 `Apache Log4j2 API` 的日志库
- `me.n1ar4.shell.analyzer` 是远程分析和查杀 `Tomcat` 内存马的项目

为什么保留源码级 fernflower

因为：

- fernflower 没有官方 maven 仓库，第三方不支持 java 8 于是 fork 一份并修复 bug

数据库层说明：

- 现在统一使用官方 `Neo4j Embedded` Maven 依赖
- 不再维护或平铺历史数据库驱动源码
