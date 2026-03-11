# No-Tai-e Baseline 2026-03-11

本报告冻结 `2026-03-11` 当天 `ASM + Tai-e` 主线在 Phase 0 样本集上的一次完整基线，用于后续对比“更快了/更准了”的说法。
这是一份默认主链切换前的历史快照，因此其中出现的 `engine=taie`、`mode=taie:balanced`、`taie_callgraph` 都是当时的原始产物，不代表当前默认口径。

## 环境

- 生成时间：`2026-03-11T01:23:00+08:00`
- Java：`25.0.2`
- OS：`Mac OS X aarch64`
- 工作目录：`/Users/veritas/Documents/projects/jar-analyzer`
- 样本：`framework-stack, ssm-war, ssm-project-mode, gadget-family, ysoserial, callback, springboot-fatjar`
- 迭代：`1`

## 执行命令

```bash
mvn -q -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true compile
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest=CoreRunnerBuildMetricsTest test
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest=NoTaieBaselineBenchTest -Dbench.no_taie=true -Dbench.no_taie.iter=1 test
```

## 总体结果

- 场景数：`7`
- 通过：`7/7`
- suiteWallMs：`88892`
- buildWallP50Ms：`6919`
- buildWallP95Ms：`13605`
- 观测到的最大 `peak_heap_used_mib`：`1322`

## 分场景

- `framework-stack`：`buildWallMs=8477`，`peakHeapMiB=1120`，`engine=taie`，`mode=taie:balanced`
- `ssm-war`：`buildWallMs=6499`，`peakHeapMiB=956`，`engine=disabled-no-target`，`mode=disabled-no-target`
- `ssm-project-mode`：`buildWallMs=6919`，`peakHeapMiB=1193`，`engine=taie`，`mode=taie:balanced`
- `gadget-family`：`buildWallMs=6684`，`peakHeapMiB=1322`，`engine=taie`，`mode=taie:balanced`
- `ysoserial`：`buildWallMs=6489`，`peakHeapMiB=1099`，`engine=taie`，`mode=taie:balanced`
- `callback`：`buildWallMs=7219`，`peakHeapMiB=1129`，`engine=taie`，`mode=taie:balanced`
- `springboot-fatjar`：`buildWallMs=13605`，`peakHeapMiB=1296`，`engine=taie`，`mode=taie:balanced`

## 关键观察

- `springboot-fatjar` 仍然是最重样本，历史快照里的 `taie_callgraph`（当前 canonical 阶段名为 `callgraph`）是主要耗时段。
- `gadget-family` 的峰值内存最高，说明“小样本不一定轻”，图规模和分析形态比归档体积更关键。
- `ssm-war` 在 `all-common` 分类下关闭调用图，功能可过，但这条路径不适合作为默认性能结论样本。
- `callback` 和 `springboot-fatjar` 现在都已经进入统一 baseline matrix，并且带出了 `call_site_count/local_var_count/peak_heap_*`。

## 原始产物

- [summary](/Users/veritas/Documents/projects/jar-analyzer/target/bench/no-taie-baseline-summary.md)
- [matrix](/Users/veritas/Documents/projects/jar-analyzer/target/bench/no-taie-baseline-matrix.csv)
- [stage breakdown](/Users/veritas/Documents/projects/jar-analyzer/target/bench/no-taie-build-stage-breakdown.csv)
- [regression summary](/Users/veritas/Documents/projects/jar-analyzer/target/bench/no-taie-regression-summary.md)
- [env](/Users/veritas/Documents/projects/jar-analyzer/target/bench/no-taie-env.txt)
