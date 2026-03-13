/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.qa;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.RuleRegistryTestSupport;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RealStrutsSpringMyBatisProjectModeRegressionTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        RuleRegistryTestSupport.clearRuleFiles();
    }

    @Test
    @SuppressWarnings("all")
    public void projectModeWarFixtureShouldExposeCallGraphAndWebTaintPaths() {
        try {
            Path archive = FixtureJars.strutsSpringMyBatisAppArchive();
            Path projectDir = Paths.get("test", "struts-spring-mybatis-app").toAbsolutePath().normalize();
            configureSinkRule(tempDir.resolve("sink.json"));

            ProjectRuntimeContext.replaceProjectModel(buildProjectModel(projectDir, archive));
            ProjectRuntimeContext.updateResolveInnerJars(false);
            CoreRunner.run(archive, null, false, null);

            GraphSnapshot snapshot = new GraphStore().loadSnapshot();
            assertPath(snapshot,
                    "com/example/ssm/web/AdminController",
                    "search",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    "com/example/ssm/sink/SearchAuditSink",
                    "record",
                    "(Ljava/lang/String;)V");
            assertPath(snapshot,
                    "com/example/ssm/web/LegacyLoginAction",
                    "execute",
                    "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;",
                    "com/example/ssm/sink/SearchAuditSink",
                    "record",
                    "(Ljava/lang/String;)V");
            assertPath(snapshot,
                    "com/example/ssm/web/DispatchServlet",
                    "doPost",
                    "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V",
                    "com/example/ssm/sink/SearchAuditSink",
                    "record",
                    "(Ljava/lang/String;)V");

            QueryResult taint = new ProcedureRegistry().execute(
                    "ja.taint.track",
                    List.of(
                            "", "", "",
                            "com/example/ssm/sink/SearchAuditSink", "record", "(Ljava/lang/String;)V",
                            "6", "15000", "12",
                            "sink", "true", "true"
                    ),
                    Map.of(),
                    QueryOptions.defaults(),
                    snapshot
            );

            assertFalse(taint.getRows().isEmpty());
            assertTrue(taint.getWarnings().contains("taint_search_backend=graph-pruned"));
            Set<String> sourceClasses = resolveSourceClasses(taint, snapshot);
            assertTrue(sourceClasses.contains("com/example/ssm/web/AdminController"),
                    "taint sources=" + sourceClasses + " rows=" + taint.getRows());
            assertTrue(sourceClasses.contains("com/example/ssm/web/LegacyLoginAction"),
                    "taint sources=" + sourceClasses + " rows=" + taint.getRows());
            assertTrue(sourceClasses.contains("com/example/ssm/web/DispatchServlet"),
                    "taint sources=" + sourceClasses + " rows=" + taint.getRows());
        } catch (Exception ex) {
            fail("ssm project-mode flow regression failed: " + ex);
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
        }
    }

    private static void configureSinkRule(Path sinkFile) throws Exception {
        Files.writeString(sinkFile, """
                {
                  "name": "ssm-project-mode",
                  "levels": {
                    "high": {
                      "sql": [
                        {
                          "className": "com/example/ssm/sink/SearchAuditSink",
                          "methodName": "record",
                          "methodDesc": "(Ljava/lang/String;)V"
                        }
                      ]
                    }
                  }
                }
                """, StandardCharsets.UTF_8);
        RuleRegistryTestSupport.useSinkFile(sinkFile);
        ModelRegistry.reload();
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

    private static void assertPath(GraphSnapshot snapshot,
                                   String sourceClass,
                                   String sourceMethod,
                                   String sourceDesc,
                                   String sinkClass,
                                   String sinkMethod,
                                   String sinkDesc) {
        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.shortest",
                List.of(
                        ref(sourceClass, sourceMethod, sourceDesc),
                        ref(sinkClass, sinkMethod, sinkDesc),
                        "6"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );
        long sourceNodeId = snapshot.findMethodNodeId(sourceClass, sourceMethod, sourceDesc, null);
        assertFalse(result.getRows().isEmpty(),
                "path missing from " + sourceClass + "#" + sourceMethod + sourceDesc + " to " + sinkClass + "#" + sinkMethod + sinkDesc
                        + " outgoing=" + describeOutgoing(snapshot, sourceNodeId));
        List<Object> row = result.getRows().get(0);
        assertEquals(2, ((Number) row.get(1)).intValue());
        assertTrue(String.valueOf(row.get(6)).contains("ja.path.shortest"));
    }

    private static String describeOutgoing(GraphSnapshot snapshot, long nodeId) {
        if (snapshot == null || nodeId <= 0L) {
            return "[]";
        }
        List<String> out = new java.util.ArrayList<>();
        for (var edge : snapshot.getOutgoingView(nodeId)) {
            if (edge == null) {
                continue;
            }
            GraphNode node = snapshot.getNode(edge.getDstId());
            if (node == null) {
                continue;
            }
            out.add(edge.getRelType() + "->" + node.getClassName() + "#" + node.getMethodName() + node.getMethodDesc());
        }
        return out.toString();
    }

    private static Set<String> resolveSourceClasses(QueryResult result, GraphSnapshot snapshot) {
        Set<String> out = new HashSet<>();
        if (result == null || result.getRows() == null || snapshot == null) {
            return out;
        }
        for (List<Object> row : result.getRows()) {
            if (row == null || row.size() < 3) {
                continue;
            }
            long nodeId = firstNodeId(String.valueOf(row.get(2)));
            if (nodeId <= 0L) {
                continue;
            }
            GraphNode node = snapshot.getNode(nodeId);
            if (node == null || node.getClassName() == null || node.getClassName().isBlank()) {
                continue;
            }
            out.add(node.getClassName());
        }
        return out;
    }

    private static long firstNodeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1L;
        }
        int comma = raw.indexOf(',');
        String first = comma >= 0 ? raw.substring(0, comma) : raw;
        try {
            return Long.parseLong(first.trim());
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static String ref(String className, String methodName, String desc) {
        return className + "#" + methodName + "#" + desc;
    }
}
