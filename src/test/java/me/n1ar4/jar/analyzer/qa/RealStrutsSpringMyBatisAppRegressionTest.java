/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.qa;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.rules.FrameworkXmlSourceIndex;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import me.n1ar4.jar.analyzer.rules.MyBatisMapperXmlIndex;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RealStrutsSpringMyBatisAppRegressionTest {
    @AfterEach
    void cleanup() {
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    @SuppressWarnings("all")
    public void ssmWarFixtureShouldExposeFrameworkSignals() {
        try {
            Path archive = FixtureJars.strutsSpringMyBatisAppArchive();
            ProjectRuntimeContext.updateResolveInnerJars(false);
            CoreRunner.run(archive, null, false, false, null);

            assertTrue(DatabaseManager.getServlets().contains("com/example/ssm/web/DispatchServlet"));
            assertTrue(DatabaseManager.getFilters().contains("com/example/ssm/web/AuthFilter"));
            assertTrue(DatabaseManager.getListeners().contains("com/example/ssm/bootstrap/AppBootstrapListener"));

            FrameworkXmlSourceIndex.Result xmlIndex = FrameworkXmlSourceIndex.fromResources(DatabaseManager.getResources());
            assertTrue(xmlIndex.servletClasses().contains("com/example/ssm/web/DispatchServlet"));
            assertTrue(xmlIndex.filterClasses().contains("com/example/ssm/web/AuthFilter"));
            assertTrue(xmlIndex.listenerClasses().contains("com/example/ssm/bootstrap/AppBootstrapListener"));
            assertTrue(xmlIndex.methodPatterns().stream().anyMatch(pattern ->
                    "com/example/ssm/web/LegacyLoginAction".equals(pattern.className())
                            && "execute".equals(pattern.methodName())));

            MyBatisMapperXmlIndex.Result myBatisIndex = MyBatisMapperXmlIndex.fromResources(DatabaseManager.getResources());
            assertFalse(myBatisIndex.sinkPatterns().isEmpty());
            assertNotNull(myBatisIndex.resolve(
                    "com/example/ssm/mapper/UserMapper",
                    "findUserByKeyword",
                    "(Ljava/lang/String;)Ljava/lang/String;"
            ));

            MethodReference controller = requireMethod(
                    "com/example/ssm/web/AdminController",
                    "search",
                    "(Ljava/lang/String;)Ljava/lang/String;"
            );
            MethodReference action = requireMethod(
                    "com/example/ssm/web/LegacyLoginAction",
                    "execute",
                    "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;"
            );
            MethodReference servlet = requireMethod(
                    "com/example/ssm/web/DispatchServlet",
                    "doPost",
                    "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"
            );
            MethodReference filter = requireMethod(
                    "com/example/ssm/web/AuthFilter",
                    "doFilter",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V"
            );
            MethodReference listener = requireMethod(
                    "com/example/ssm/bootstrap/AppBootstrapListener",
                    "contextInitialized",
                    "(Ljavax/servlet/ServletContextEvent;)V"
            );
            MethodReference mapper = requireMethod(
                    "com/example/ssm/mapper/UserMapper",
                    "findUserByKeyword",
                    "(Ljava/lang/String;)Ljava/lang/String;"
            );
            MethodReference jspMethod = requireJspMethod("_jspService");

            assertSemantic(controller, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT);
            assertSemantic(action, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.STRUTS_ACTION);
            assertSemantic(servlet, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(filter, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(listener, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(mapper, MethodSemanticFlags.MYBATIS_DYNAMIC_SQL);
            assertSemantic(jspMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.JSP_ENDPOINT);

            assertWebSource(controller);
            assertWebSource(action);
            assertWebSource(servlet);
            assertWebSource(filter);
            assertWebSource(listener);
            assertWebSource(jspMethod);

            GraphSnapshot snapshot = new GraphStore().loadSnapshot();
            assertTrue(requireNode(snapshot, "com/example/ssm/mapper/UserMapper", "findUserByKeyword", "(Ljava/lang/String;)Ljava/lang/String;")
                    .hasMethodSemanticFlag(MethodSemanticFlags.MYBATIS_DYNAMIC_SQL));
            assertTrue(requireNode(snapshot, jspMethod.getClassReference().getName(), "_jspService", jspMethod.getDesc())
                    .hasSourceFlag(GraphNode.SOURCE_FLAG_WEB));
        } catch (Exception ex) {
            fail("ssm app regression failed: " + ex);
        }
    }

    private static void assertWebSource(MethodReference method) {
        int flags = SourceRuleSupport.resolveCurrentSourceFlags(
                method.getClassReference().getName(),
                method.getName(),
                method.getDesc(),
                method.getJarId()
        );
        assertTrue((flags & GraphNode.SOURCE_FLAG_ANY) != 0);
        assertTrue((flags & GraphNode.SOURCE_FLAG_WEB) != 0);
    }

    private static void assertSemantic(MethodReference method, int mask) {
        assertNotNull(method);
        assertTrue((method.getSemanticFlags() & mask) == mask,
                "semantic flags mismatch for " + method.getClassReference().getName()
                        + "#" + method.getName() + method.getDesc()
                        + " actual=" + method.getSemanticFlags()
                        + " expectedMask=" + mask);
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
        fail("method not found: " + className + "#" + methodName + desc);
        return null;
    }

    private static MethodReference requireJspMethod(String methodName) {
        for (MethodReference method : DatabaseManager.getMethodReferences()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String className = method.getClassReference().getName();
            if (methodName.equals(method.getName())
                    && className != null
                    && className.startsWith("org/apache/jsp/")
                    && className.contains("dashboard_jsp")) {
                return method;
            }
        }
        fail("compiled jsp method not found");
        return null;
    }

    private static GraphNode requireNode(GraphSnapshot snapshot, String className, String methodName, String desc) {
        long nodeId = snapshot.findMethodNodeId(className, methodName, desc, null);
        assertTrue(nodeId > 0L, "graph node not found for " + className + "#" + methodName + desc);
        return snapshot.getNode(nodeId);
    }
}
