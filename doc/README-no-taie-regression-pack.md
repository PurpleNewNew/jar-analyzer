# 去 Tai-e 迁移最小回归包（JA-NT-108）

本文档补充 [Phase 0 实施文档](/Users/veritas/Documents/projects/jar-analyzer/doc/README-no-taie-phase0.md) 中的 `JA-NT-108`。

当前代码里的 source of truth：

- `src/test/java/me/n1ar4/jar/analyzer/qa/NoTaieMigrationRegressionPack.java`

## 1. 固定口径

每个迁移 issue 都绑定一组最小必跑测试。

目标不是一次跑完整仓，而是：

- 每迁一包，都有固定的“最少要跑哪些”
- issue 到测试集的映射可被代码校验
- 命令格式统一，避免回归时临时拼接

## 2. Issue 到回归包映射

| issue | 最小必跑测试 |
| --- | --- |
| `JA-NT-103` | `CoreRunnerBytecodeMainlineTest`, `RealFrameworkRegressionTest`, `RealStrutsSpringMyBatisProjectModeRegressionTest` |
| `JA-NT-104` | `CoreRunnerBytecodeMainlineTest`, `RealFrameworkRegressionTest`, `RealStrutsSpringMyBatisAppRegressionTest` |
| `JA-NT-105` | `CoreRunnerBytecodeMainlineTest`, `RealGadgetFamilyRegressionTest`, `YsoserialPayloadRegressionTest`, `GadgetRouteCoverageBenchTest` |
| `JA-NT-106` | `BuildFactAssemblerTest`, `CoreRunnerBytecodeMainlineTest`, `RealFrameworkRegressionTest`, `RealStrutsSpringMyBatisProjectModeRegressionTest` |
| `JA-NT-107` | `CallGraphPlanTest`, `CoreRunnerCallGraphProfileTest`, `TaieBuildIntegrationTest` |
| `JA-NT-108` | `NoTaieMigrationRegressionPackTest`, `NoTaieQualityGateTest` |

## 3. 推荐命令

`JA-NT-106`

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=BuildFactAssemblerTest,CoreRunnerBytecodeMainlineTest,RealFrameworkRegressionTest,RealStrutsSpringMyBatisProjectModeRegressionTest \
  test
```

`JA-NT-107`

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=CallGraphPlanTest,CoreRunnerCallGraphProfileTest,TaieBuildIntegrationTest \
  test
```

`JA-NT-108`

```bash
mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
  -Dtest=NoTaieMigrationRegressionPackTest,NoTaieQualityGateTest \
  test
```

## 4. 契约测试

当前新增契约测试：

- `NoTaieMigrationRegressionPackTest`

它会校验：

- `JA-NT-106/107/108` 已被纳入回归包
- 映射到的测试类都真实存在
- 生成的 Maven 命令包含所有必跑测试

这一步的目标是先把“回归包定义”固定下来，再继续做后续迁移。
