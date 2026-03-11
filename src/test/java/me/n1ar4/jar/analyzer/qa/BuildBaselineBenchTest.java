/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.qa;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional build baseline harness.
 * <p>
 * Run with:
 * mvn -q -Dskip.npm=true -Dskip.installnodenpm=true \
 *   -Dtest=BuildBaselineBenchTest \
 *   -Dbench.build_baseline=true \
 *   -Dbench.build_baseline.iter=1 \
 *   test
 */
public class BuildBaselineBenchTest {
    private static final String ENABLE_PROP = "bench.build_baseline";
    private static final String ITER_PROP = "bench.build_baseline.iter";
    private static final String SCENARIOS_PROP = "bench.build_baseline.scenarios";
    private static final String CALLGRAPH_STAGE_KEY = "callgraph";

    private static final List<String> STAGE_KEYS = List.of(
            "prepare_project_model",
            "resolve_inputs",
            "prepare_class_files",
            "discovery",
            "class_analysis",
            "framework_entry",
            "method_semantic",
            "bytecode_symbol",
            CALLGRAPH_STAGE_KEY,
            "build_runtime_snapshot",
            "publish_runtime",
            "refresh_caches",
            "neo4j_commit"
    );

    @Test
    @SuppressWarnings("all")
    public void benchmarkBuildBaselineMatrix() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable build baseline benchmark");

        int iterations = resolveInt(ITER_PROP, 1, 1, 20);
        Set<String> selected = resolveScenarioSelection(System.getProperty(SCENARIOS_PROP));
        BaselineResult baseline = runBaseline(iterations, selected, true);
        assertTrue(baseline.passed(), "baseline scenario failures: " + baseline.failures());
    }

    static BaselineResult runBaseline(int iterations,
                                      Set<String> selected,
                                      boolean writeReports) throws Exception {
        List<Scenario> scenarios = allScenarios(selected);
        if (scenarios.isEmpty()) {
            throw new IllegalArgumentException("no scenarios selected");
        }

        List<RunRecord> records = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        long suiteStartNs = System.nanoTime();

        for (Scenario scenario : scenarios) {
            for (int iteration = 1; iteration <= iterations; iteration++) {
                resetState();
                RunRecord record = runScenario(scenario, iteration);
                records.add(record);
                if (!record.passed()) {
                    failures.add(record.scenario() + "#" + iteration + ": " + record.status());
                }
            }
        }

        long suiteWallMs = Math.max(0L, (System.nanoTime() - suiteStartNs) / 1_000_000L);
        if (writeReports) {
            writeReports(records, failures, iterations, suiteWallMs);
        }
        return BaselineResult.from(records, failures, iterations, suiteWallMs);
    }

    private static RunRecord runScenario(Scenario scenario, int iteration) {
        long startNs = System.nanoTime();
        try {
            scenario.prepare().run();
            CoreRunner.BuildResult build = CoreRunner.run(
                    scenario.input(),
                    null,
                    false,
                    false,
                    null
            );
            String validation = scenario.validate().run(build);
            long wallMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
            return RunRecord.success(scenario.name(), iteration, build, wallMs, validation);
        } catch (Throwable ex) {
            long wallMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
            return RunRecord.failure(scenario.name(), iteration, wallMs, ex);
        } finally {
            resetState();
        }
    }

    private static void writeReports(List<RunRecord> records,
                                     List<String> failures,
                                     int iterations,
                                     long suiteWallMs) {
        writeSummary(records, failures, iterations, suiteWallMs);
        writeMatrixCsv(records);
        writeBreakdownCsv(records);
        writeRegressionSummary(records, failures);
        writeEnv(records, iterations, suiteWallMs);
    }

    private static void writeSummary(List<RunRecord> records,
                                     List<String> failures,
                                     int iterations,
                                     long suiteWallMs) {
        List<Long> walls = new ArrayList<>();
        int passed = 0;
        for (RunRecord record : records) {
            if (record.passed()) {
                passed++;
                walls.add(record.buildWallMs());
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("## Summary");
        lines.add("- scenarios: `" + distinctScenarioCount(records) + "`");
        lines.add("- iterations: `" + iterations + "`");
        lines.add("- runs: `" + records.size() + "`");
        lines.add("- passed: `" + passed + "`");
        lines.add("- failed: `" + failures.size() + "`");
        lines.add("- suiteWallMs: `" + suiteWallMs + "`");
        lines.add("- buildWallP50Ms: `" + percentile(walls, 0.50) + "`");
        lines.add("- buildWallP95Ms: `" + percentile(walls, 0.95) + "`");
        lines.add("");
        lines.add("## Per Scenario");
        for (String scenario : distinctScenarioNames(records)) {
            List<Long> scenarioWalls = new ArrayList<>();
            int scenarioPass = 0;
            int scenarioFail = 0;
            String engine = "";
            String mode = "";
            long peakHeapUsedBytes = 0L;
            for (RunRecord record : records) {
                if (!scenario.equals(record.scenario())) {
                    continue;
                }
                if (record.passed()) {
                    scenarioPass++;
                    scenarioWalls.add(record.buildWallMs());
                    if (engine.isBlank()) {
                        engine = record.callGraphEngine();
                    }
                    if (mode.isBlank()) {
                        mode = record.callGraphMode();
                    }
                    peakHeapUsedBytes = Math.max(peakHeapUsedBytes, record.peakHeapUsedBytes());
                } else {
                    scenarioFail++;
                }
            }
            lines.add("- `" + scenario + "` runs=`" + (scenarioPass + scenarioFail)
                    + "` pass=`" + scenarioPass
                    + "` fail=`" + scenarioFail
                    + "` buildWallP50Ms=`" + percentile(scenarioWalls, 0.50)
                    + "` buildWallP95Ms=`" + percentile(scenarioWalls, 0.95)
                    + "` peakHeapMiB=`" + toMiB(peakHeapUsedBytes)
                    + "` engine=`" + safe(engine)
                    + "` mode=`" + safe(mode) + "`");
        }
        if (!failures.isEmpty()) {
            lines.add("");
            lines.add("## Failures");
            for (String failure : failures) {
                lines.add("- " + failure);
            }
        }
        BenchReportWriter.writeMarkdown(
                "build-baseline-summary.md",
                "Build Baseline Summary",
                lines
        );
    }

    private static void writeMatrixCsv(List<RunRecord> records) {
        List<String> rows = new ArrayList<>();
        rows.add(String.join(",",
                "scenario",
                "iteration",
                "status",
                "validation",
                "build_seq",
                "call_graph_engine",
                "call_graph_mode",
                "analysis_profile",
                "jar_count",
                "target_jar_count",
                "library_jar_count",
                "sdk_entry_count",
                "class_file_count",
                "class_count",
                "method_count",
                "call_site_count",
                "local_var_count",
                "edge_count",
                "db_size_bytes",
                "peak_heap_used_bytes",
                "peak_heap_used_mb",
                "peak_heap_committed_bytes",
                "peak_heap_committed_mb",
                "heap_max_bytes",
                "heap_max_mb",
                "build_wall_ms",
                "scenario_wall_ms",
                "build_stage_prepare_project_model_ms",
                "build_stage_resolve_inputs_ms",
                "build_stage_prepare_class_files_ms",
                "build_stage_discovery_ms",
                "build_stage_class_analysis_ms",
                "build_stage_framework_entry_ms",
                "build_stage_method_semantic_ms",
                "build_stage_bytecode_symbol_ms",
                "build_stage_callgraph_ms",
                "build_stage_build_runtime_snapshot_ms",
                "build_stage_publish_runtime_ms",
                "build_stage_refresh_caches_ms",
                "build_stage_neo4j_commit_ms"
        ));
        for (RunRecord record : records) {
            rows.add(String.join(",",
                    csv(record.scenario()),
                    String.valueOf(record.iteration()),
                    csv(record.status()),
                    csv(record.validation()),
                    String.valueOf(record.buildSeq()),
                    csv(record.callGraphEngine()),
                    csv(record.callGraphMode()),
                    csv(record.analysisProfile()),
                    String.valueOf(record.jarCount()),
                    String.valueOf(record.targetJarCount()),
                    String.valueOf(record.libraryJarCount()),
                    String.valueOf(record.sdkEntryCount()),
                    String.valueOf(record.classFileCount()),
                    String.valueOf(record.classCount()),
                    String.valueOf(record.methodCount()),
                    String.valueOf(record.callSiteCount()),
                    String.valueOf(record.localVarCount()),
                    String.valueOf(record.edgeCount()),
                    String.valueOf(record.dbSizeBytes()),
                    String.valueOf(record.peakHeapUsedBytes()),
                    String.valueOf(toMiB(record.peakHeapUsedBytes())),
                    String.valueOf(record.peakHeapCommittedBytes()),
                    String.valueOf(toMiB(record.peakHeapCommittedBytes())),
                    String.valueOf(record.heapMaxBytes()),
                    String.valueOf(toMiB(record.heapMaxBytes())),
                    String.valueOf(record.buildWallMs()),
                    String.valueOf(record.scenarioWallMs()),
                    String.valueOf(record.stageDurationMs("prepare_project_model")),
                    String.valueOf(record.stageDurationMs("resolve_inputs")),
                    String.valueOf(record.stageDurationMs("prepare_class_files")),
                    String.valueOf(record.stageDurationMs("discovery")),
                    String.valueOf(record.stageDurationMs("class_analysis")),
                    String.valueOf(record.stageDurationMs("framework_entry")),
                    String.valueOf(record.stageDurationMs("method_semantic")),
                    String.valueOf(record.stageDurationMs("bytecode_symbol")),
                    String.valueOf(record.stageDurationMs(CALLGRAPH_STAGE_KEY)),
                    String.valueOf(record.stageDurationMs("build_runtime_snapshot")),
                    String.valueOf(record.stageDurationMs("publish_runtime")),
                    String.valueOf(record.stageDurationMs("refresh_caches")),
                    String.valueOf(record.stageDurationMs("neo4j_commit"))
            ));
        }
        BenchReportWriter.writeCsv("build-baseline-matrix.csv", rows);
    }

    private static void writeBreakdownCsv(List<RunRecord> records) {
        List<String> rows = new ArrayList<>();
        rows.add("scenario,iteration,status,stage,duration_ms,details");
        for (RunRecord record : records) {
            for (String stageKey : STAGE_KEYS) {
                CoreRunner.BuildStageMetric metric = record.stageMetrics().get(stageKey);
                if (metric == null) {
                    continue;
                }
                rows.add(String.join(",",
                        csv(record.scenario()),
                        String.valueOf(record.iteration()),
                        csv(record.status()),
                        csv(stageKey),
                        String.valueOf(metric.getDurationMs()),
                        csv(formatDetails(metric.getDetails()))
                ));
            }
        }
        BenchReportWriter.writeCsv("build-stage-breakdown.csv", rows);
    }

    private static void writeRegressionSummary(List<RunRecord> records, List<String> failures) {
        List<String> lines = new ArrayList<>();
        lines.add("## Scenario Results");
        for (RunRecord record : records) {
            lines.add("- `" + record.scenario() + "#" + record.iteration()
                    + "` status=`" + record.status()
                    + "` validation=`" + safe(record.validation())
                    + "` buildWallMs=`" + record.buildWallMs()
                    + "`");
        }
        if (!failures.isEmpty()) {
            lines.add("");
            lines.add("## Failure Details");
            for (String failure : failures) {
                lines.add("- " + failure);
            }
        }
        BenchReportWriter.writeMarkdown(
                "build-regression-summary.md",
                "Build Regression Summary",
                lines
        );
    }

    private static void writeEnv(List<RunRecord> records, int iterations, long suiteWallMs) {
        List<String> lines = new ArrayList<>();
        lines.add("generated=" + OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        lines.add("java=" + safe(System.getProperty("java.version")));
        lines.add("os=" + safe(System.getProperty("os.name")) + " " + safe(System.getProperty("os.arch")));
        lines.add("iterations=" + iterations);
        lines.add("suite_wall_ms=" + suiteWallMs);
        lines.add("enabled_prop=" + ENABLE_PROP);
        lines.add("scenario_prop=" + safe(System.getProperty(SCENARIOS_PROP)));
        lines.add("scenarios=" + String.join(",", distinctScenarioNames(records)));
        lines.add("stage_keys=" + String.join(",", STAGE_KEYS));
        lines.add("peak_heap_used_mib=" + toMiB(maxPeakHeap(records)));
        lines.add("cwd=" + Path.of("").toAbsolutePath().normalize());
        BenchReportWriter.writeText("build-benchmark-env.txt", lines);
    }

    private static List<Scenario> allScenarios(Set<String> selected) {
        List<Scenario> scenarios = new ArrayList<>();
        addIfSelected(scenarios, selected, frameworkStackScenario());
        addIfSelected(scenarios, selected, ssmWarScenario());
        addIfSelected(scenarios, selected, ssmProjectModeScenario());
        addIfSelected(scenarios, selected, gadgetFamilyScenario());
        addIfSelected(scenarios, selected, ysoserialScenario());
        addIfSelected(scenarios, selected, callbackScenario());
        addIfSelected(scenarios, selected, springbootScenario());
        return scenarios;
    }

    private static void addIfSelected(List<Scenario> target, Set<String> selected, Scenario scenario) {
        if (target == null || scenario == null) {
            return;
        }
        if (selected == null || selected.isEmpty() || selected.contains(normalizeScenario(scenario.name()))) {
            target.add(scenario);
        }
    }

    private static Scenario frameworkStackScenario() {
        return new Scenario(
                "framework-stack",
                FixtureJars::frameworkStackTestJar,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    assertTrue(DatabaseManager.getServlets().contains("fixture/framework/web/XmlServlet"));
                    assertTrue(DatabaseManager.getFilters().contains("fixture/framework/web/AuditFilter"));
                    assertMethodSemantic(
                            "fixture/framework/netty/AuthHandler",
                            "channelRead0",
                            "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
                            MethodSemanticFlags.NETTY_HANDLER
                    );
                    return "framework signals stable";
                }
        );
    }

    private static Scenario ssmWarScenario() {
        return new Scenario(
                "ssm-war",
                FixtureJars::strutsSpringMyBatisAppArchive,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    assertTrue(DatabaseManager.getServlets().contains("com/example/ssm/web/DispatchServlet"));
                    assertTrue(DatabaseManager.getFilters().contains("com/example/ssm/web/AuthFilter"));
                    assertMethodSemantic(
                            "com/example/ssm/web/AdminController",
                            "search",
                            "(Ljava/lang/String;)Ljava/lang/String;",
                            MethodSemanticFlags.SPRING_ENDPOINT
                    );
                    return "ssm war framework signals stable";
                }
        );
    }

    private static Scenario ssmProjectModeScenario() {
        return new Scenario(
                "ssm-project-mode",
                FixtureJars::strutsSpringMyBatisAppArchive,
                () -> {
                    Path archive = FixtureJars.strutsSpringMyBatisAppArchive();
                    Path projectDir = Paths.get("test", "struts-spring-mybatis-app").toAbsolutePath().normalize();
                    ProjectRuntimeContext.replaceProjectModel(buildProjectModel(projectDir, archive));
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    GraphSnapshot snapshot = new GraphStore().loadSnapshot();
                    QueryResult path = new ProcedureRegistry().execute(
                            "ja.path.shortest",
                            List.of(
                                    ref("com/example/ssm/web/AdminController", "search", "(Ljava/lang/String;)Ljava/lang/String;"),
                                    ref("com/example/ssm/sink/SearchAuditSink", "record", "(Ljava/lang/String;)V"),
                                    "6"
                            ),
                            Map.of(),
                            QueryOptions.defaults(),
                            snapshot
                    );
                    assertTrue(path.getRows() != null && !path.getRows().isEmpty());
                    return "project-mode path stable";
                }
        );
    }

    private static Scenario gadgetFamilyScenario() {
        return new Scenario(
                "gadget-family",
                FixtureJars::gadgetFamilyTestJar,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    QueryResult result = new ProcedureRegistry().execute(
                            "ja.path.gadget",
                            List.of(
                                    ref("fixture/gadget/cc/CcPayload", "readObject", "(Ljava/io/ObjectInputStream;)V"),
                                    ref("fixture/gadget/sink/RuntimeSink", "exec", "(Ljava/lang/Object;)Ljava/lang/String;"),
                                    "8",
                                    "12"
                            ),
                            Map.of(),
                            QueryOptions.defaults(),
                            new GraphStore().loadSnapshot()
                    );
                    assertRoute(result, "container-callback");
                    return "gadget route stable";
                }
        );
    }

    private static Scenario ysoserialScenario() {
        return new Scenario(
                "ysoserial",
                FixtureJars::ysoserialPayloadTestJar,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    QueryResult result = new ProcedureRegistry().execute(
                            "ja.path.gadget",
                            List.of(
                                    ref("ysoserial/payloads/CommonsCollections6", "readObject", "(Ljava/io/ObjectInputStream;)V"),
                                    ref("ysoserial/secmgr/ExecSink", "exec", "(Ljava/lang/String;)Ljava/lang/String;"),
                                    "8",
                                    "12"
                            ),
                            Map.of(),
                            QueryOptions.defaults(),
                            new GraphStore().loadSnapshot()
                    );
                    assertRoute(result, "container-callback");
                    return "ysoserial route stable";
                }
        );
    }

    private static Scenario callbackScenario() {
        return new Scenario(
                "callback",
                FixtureJars::callbackTestJar,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    MethodReference method = requireMethod(
                            "me/n1ar4/cb/CallbackEntry",
                            "dynamicProxy",
                            "()V"
                    );
                    assertTrue(method.getClassReference() != null);
                    return "callback fixture built";
                }
        );
    }

    private static Scenario springbootScenario() {
        return new Scenario(
                "springboot-fatjar",
                FixtureJars::springbootTestJar,
                () -> {
                    ProjectRuntimeContext.clear();
                    ProjectRuntimeContext.updateResolveInnerJars(false);
                },
                build -> {
                    assertBuildSucceeded(build);
                    assertTrue(!DatabaseManager.getSpringControllers().isEmpty());
                    assertMethodSemantic(
                            "me/n1ar4/test/demos/web/DataController",
                            "getStatus",
                            "(Ljava/lang/String;)Ljava/util/Map;",
                            MethodSemanticFlags.SPRING_ENDPOINT
                    );
                    return "springboot signals stable";
                }
        );
    }

    private static void assertBuildSucceeded(CoreRunner.BuildResult build) {
        assertTrue(build != null);
        assertTrue(build.getBuildSeq() > 0L);
        assertTrue(build.getClassFileCount() > 0);
        assertTrue(build.getClassCount() > 0);
        assertTrue(build.getMethodCount() > 0);
        assertTrue(build.getBuildWallMs() >= 0L);
    }

    private static void assertMethodSemantic(String className,
                                             String methodName,
                                             String desc,
                                             int semanticMask) {
        MethodReference method = requireMethod(className, methodName, desc);
        assertTrue((method.getSemanticFlags() & semanticMask) == semanticMask,
                "semantic flags mismatch for " + className + "#" + methodName + desc
                        + " actual=" + method.getSemanticFlags() + " expectedMask=" + semanticMask);
    }

    private static MethodReference requireMethod(String className, String methodName, String desc) {
        for (MethodReference method : DatabaseManager.getMethodReferencesByClass(className)) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (className.equals(method.getClassReference().getName())
                    && methodName.equals(method.getName())
                    && desc.equals(method.getDesc())) {
                return method;
            }
        }
        throw new IllegalStateException("method not found: " + className + "#" + methodName + desc);
    }

    private static ProjectModel buildProjectModel(Path projectDir, Path archive) {
        ProjectModel.Builder builder = ProjectModel.builder()
                .buildMode(ProjectBuildMode.PROJECT)
                .primaryInputPath(archive)
                .resolveInnerJars(false);
        builder.addRoot(new ProjectRoot(
                ProjectRootKind.CONTENT_ROOT,
                ProjectOrigin.APP,
                projectDir,
                "",
                false,
                false,
                10
        ));
        addRootIfExists(builder, projectDir.resolve(Paths.get("src", "main", "resources")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, false, 25);
        addRootIfExists(builder, projectDir.resolve(Paths.get("src", "main", "webapp")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, false, 26);
        addRootIfExists(builder, projectDir.resolve(Paths.get("target", "classes")),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 15);
        addRootIfExists(builder, projectDir.resolve(Paths.get("target", "generated-sources")),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 40);
        addRootIfExists(builder, projectDir.resolve("target"),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 65);
        return builder.build();
    }

    private static void addRootIfExists(ProjectModel.Builder builder,
                                        Path path,
                                        ProjectRootKind kind,
                                        ProjectOrigin origin,
                                        boolean test,
                                        int priority) {
        if (builder == null || path == null || Files.notExists(path)) {
            return;
        }
        builder.addRoot(new ProjectRoot(
                kind,
                origin,
                path,
                "",
                Files.isRegularFile(path),
                test,
                priority
        ));
    }

    private static void assertRoute(QueryResult result, String expectedRoute) {
        if (result == null || result.getRows() == null) {
            throw new IllegalStateException("query result is empty");
        }
        for (List<Object> row : result.getRows()) {
            if (row == null || row.size() < 7) {
                continue;
            }
            String evidence = String.valueOf(row.get(6));
            if (evidence.contains("route=" + expectedRoute)) {
                return;
            }
        }
        throw new IllegalStateException("expected route not found: " + expectedRoute
                + " warnings=" + (result == null ? "" : result.getWarnings()));
    }

    private static String ref(String className, String methodName, String desc) {
        return className + "#" + methodName + "#" + desc;
    }

    private static void resetState() {
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        EngineContext.setEngine(null);
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    static Set<String> resolveScenarioSelection(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String normalized = normalizeScenario(part);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static String normalizeScenario(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static int resolveInt(String prop, int def, int min, int max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int distinctScenarioCount(List<RunRecord> records) {
        return distinctScenarioNames(records).size();
    }

    private static List<String> distinctScenarioNames(List<RunRecord> records) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (records != null) {
            for (RunRecord record : records) {
                if (record != null && record.scenario() != null && !record.scenario().isBlank()) {
                    out.add(record.scenario());
                }
            }
        }
        return List.copyOf(out);
    }

    private static long percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    private static long toMiB(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return bytes / (1024L * 1024L);
    }

    private static long maxPeakHeap(List<RunRecord> records) {
        long max = 0L;
        if (records == null) {
            return max;
        }
        for (RunRecord record : records) {
            if (record != null) {
                max = Math.max(max, record.peakHeapUsedBytes());
            }
        }
        return max;
    }

    private static String formatDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            parts.add(entry.getKey() + "=" + safe(String.valueOf(entry.getValue())));
        }
        return String.join("|", parts);
    }

    private static String csv(String value) {
        String safe = safe(value);
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Scenario(
            String name,
            InputSupplier inputSupplier,
            ThrowingRunnable prepare,
            ScenarioValidator validate
    ) {
        private Path input() throws Exception {
            return inputSupplier == null ? null : inputSupplier.get();
        }
    }

    private record RunRecord(
            String scenario,
            int iteration,
            boolean passed,
            String status,
            String validation,
            long scenarioWallMs,
            long buildSeq,
            String callGraphEngine,
            String callGraphMode,
            String analysisProfile,
            int jarCount,
            int targetJarCount,
            int libraryJarCount,
            int sdkEntryCount,
            int classFileCount,
            int classCount,
            int methodCount,
            int callSiteCount,
            int localVarCount,
            long edgeCount,
            long dbSizeBytes,
            long peakHeapUsedBytes,
            long peakHeapCommittedBytes,
            long heapMaxBytes,
            long buildWallMs,
            Map<String, CoreRunner.BuildStageMetric> stageMetrics
    ) {
        private static RunRecord success(String scenario,
                                         int iteration,
                                         CoreRunner.BuildResult build,
                                         long scenarioWallMs,
                                         String validation) {
            return new RunRecord(
                    safe(scenario),
                    Math.max(1, iteration),
                    true,
                    "PASS",
                    safe(validation),
                    Math.max(0L, scenarioWallMs),
                    build == null ? 0L : build.getBuildSeq(),
                    build == null ? "" : safe(build.getCallGraphEngine()),
                    build == null ? "" : safe(build.getCallGraphMode()),
                    build == null ? "" : safe(build.getAnalysisProfile()),
                    build == null ? 0 : build.getJarCount(),
                    build == null ? 0 : build.getTargetJarCount(),
                    build == null ? 0 : build.getLibraryJarCount(),
                    build == null ? 0 : build.getSdkEntryCount(),
                    build == null ? 0 : build.getClassFileCount(),
                    build == null ? 0 : build.getClassCount(),
                    build == null ? 0 : build.getMethodCount(),
                    build == null ? 0 : build.getCallSiteCount(),
                    intMetric(build, "bytecode_symbol", "local_vars"),
                    build == null ? 0L : build.getEdgeCount(),
                    build == null ? 0L : build.getDbSizeBytes(),
                    build == null ? 0L : build.getPeakHeapUsedBytes(),
                    build == null ? 0L : build.getPeakHeapCommittedBytes(),
                    build == null ? 0L : build.getHeapMaxBytes(),
                    build == null ? 0L : build.getBuildWallMs(),
                    build == null ? Collections.emptyMap() : build.getStageMetrics()
            );
        }

        private static RunRecord failure(String scenario,
                                         int iteration,
                                         long scenarioWallMs,
                                         Throwable ex) {
            return new RunRecord(
                    safe(scenario),
                    Math.max(1, iteration),
                    false,
                    "FAIL",
                    failureReason(ex),
                    Math.max(0L, scenarioWallMs),
                    0L,
                    "",
                    "",
                    "",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    Collections.emptyMap()
            );
        }

        private long stageDurationMs(String stageKey) {
            CoreRunner.BuildStageMetric metric = stageMetrics == null ? null : stageMetrics.get(stageKey);
            return metric == null ? 0L : Math.max(0L, metric.getDurationMs());
        }
    }

    private static int intMetric(CoreRunner.BuildResult build, String stageKey, String detailKey) {
        if (build == null || stageKey == null || detailKey == null) {
            return 0;
        }
        CoreRunner.BuildStageMetric metric = build.getStageMetric(stageKey);
        if (metric == null || metric.getDetails() == null) {
            return 0;
        }
        Object value = metric.getDetails().get(detailKey);
        if (!(value instanceof Number number)) {
            return 0;
        }
        return Math.max(0, number.intValue());
    }

    private static String failureReason(Throwable ex) {
        if (ex == null) {
            return "";
        }
        String message = safe(ex.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message;
    }

    @FunctionalInterface
    private interface InputSupplier {
        Path get() throws Exception;
    }

    @FunctionalInterface
    private interface ScenarioValidator {
        String run(CoreRunner.BuildResult build) throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    static final class BaselineResult {
        private final int scenarioCount;
        private final int iterations;
        private final int runCount;
        private final int passedCount;
        private final int failedCount;
        private final long suiteWallMs;
        private final long buildWallP50Ms;
        private final long buildWallP95Ms;
        private final long maxPeakHeapUsedBytes;
        private final List<RunRecord> records;
        private final List<String> failures;

        private BaselineResult(int scenarioCount,
                               int iterations,
                               int runCount,
                               int passedCount,
                               int failedCount,
                               long suiteWallMs,
                               long buildWallP50Ms,
                               long buildWallP95Ms,
                               long maxPeakHeapUsedBytes,
                               List<RunRecord> records,
                               List<String> failures) {
            this.scenarioCount = scenarioCount;
            this.iterations = iterations;
            this.runCount = runCount;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
            this.suiteWallMs = suiteWallMs;
            this.buildWallP50Ms = buildWallP50Ms;
            this.buildWallP95Ms = buildWallP95Ms;
            this.maxPeakHeapUsedBytes = maxPeakHeapUsedBytes;
            this.records = records == null ? List.of() : records;
            this.failures = failures == null ? List.of() : failures;
        }

        static BaselineResult from(List<RunRecord> records,
                                   List<String> failures,
                                   int iterations,
                                   long suiteWallMs) {
            List<RunRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
            List<String> safeFailures = failures == null ? List.of() : List.copyOf(failures);
            List<Long> walls = new ArrayList<>();
            int passedCount = 0;
            for (RunRecord record : safeRecords) {
                if (record != null && record.passed()) {
                    passedCount++;
                    walls.add(record.buildWallMs());
                }
            }
            return new BaselineResult(
                    distinctScenarioCount(safeRecords),
                    Math.max(1, iterations),
                    safeRecords.size(),
                    passedCount,
                    safeFailures.size(),
                    Math.max(0L, suiteWallMs),
                    percentile(walls, 0.50),
                    percentile(walls, 0.95),
                    maxPeakHeap(safeRecords),
                    safeRecords,
                    safeFailures
            );
        }

        int scenarioCount() {
            return scenarioCount;
        }

        int iterations() {
            return iterations;
        }

        int runCount() {
            return runCount;
        }

        int passedCount() {
            return passedCount;
        }

        int failedCount() {
            return failedCount;
        }

        long suiteWallMs() {
            return suiteWallMs;
        }

        long buildWallP50Ms() {
            return buildWallP50Ms;
        }

        long buildWallP95Ms() {
            return buildWallP95Ms;
        }

        long maxPeakHeapUsedBytes() {
            return maxPeakHeapUsedBytes;
        }

        List<RunRecord> records() {
            return records;
        }

        List<String> failures() {
            return failures;
        }

        boolean passed() {
            return failures.isEmpty();
        }
    }
}
