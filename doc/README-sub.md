## 子项目

### Tomcat Analyzer

该项目位于 `me.n1ar4.shell.analyzer` 中，用于配合 `javaagent` 对运行中的 Tomcat 做辅助分析（面向内存马/动态行为排查等场景）。

[代码](../src/main/java/me/n1ar4/shell/analyzer)

GUI 入口：

1. 菜单 `远程(remote)` -> `启动 Tomcat 分析`

运行方式（Tomcat 侧）：

1. 构建 agent（目录 `agent/`，该模块以 Java 8 编译以提高兼容性）：
   - `cd agent && mvn -q package`
   - 产物会输出到项目根目录（文件名包含 `jar-with-dependencies`）
2. 修改 `catalina.bat/sh` 启动参数，加入 `-javaagent`（示例）：

```shell
-javaagent:agent.jar=port=18080;password=YOUR_PASSWORD
```

说明：

- `port/password` 用于 Tomcat Analyzer 与 agent 建立连接。
- agent 只用于辅助分析，请不要在生产环境长期启用。

### Y4-LOG

该项目位于 `me.n1ar4.log` 中，是一个模仿 `Log4j2 API` 的轻量日志库（Jar Analyzer 内部使用）。

[代码](../src/main/java/me/n1ar4/log)
