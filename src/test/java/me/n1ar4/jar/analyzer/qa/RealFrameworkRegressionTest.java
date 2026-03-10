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

public class RealFrameworkRegressionTest {
    @AfterEach
    void cleanup() {
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    @SuppressWarnings("all")
    public void springbootFixtureShouldExposeStableSpringSignals() {
        try {
            Path jar = FixtureJars.springbootTestJar();
            ProjectRuntimeContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, null);

            assertFalse(DatabaseManager.getSpringControllers().isEmpty());

            MethodReference endpoint = requireMethod(
                    "me/n1ar4/test/demos/web/DataController",
                    "getStatus",
                    "(Ljava/lang/String;)Ljava/util/Map;"
            );
            assertSemantic(endpoint, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT);

            int dynamicSourceFlags = SourceRuleSupport.resolveCurrentSourceFlags(
                    "me/n1ar4/test/demos/web/DataController",
                    "getStatus",
                    "(Ljava/lang/String;)Ljava/util/Map;",
                    endpoint.getJarId()
            );
            assertTrue((dynamicSourceFlags & GraphNode.SOURCE_FLAG_ANY) != 0);
            assertTrue((dynamicSourceFlags & GraphNode.SOURCE_FLAG_WEB) != 0);

            GraphSnapshot snapshot = new GraphStore().loadSnapshot();
            GraphNode node = requireNode(
                    snapshot,
                    "me/n1ar4/test/demos/web/DataController",
                    "getStatus",
                    "(Ljava/lang/String;)Ljava/util/Map;"
            );
            assertTrue(node.hasSourceFlag(GraphNode.SOURCE_FLAG_WEB));
            assertTrue(node.hasMethodSemanticFlag(MethodSemanticFlags.SPRING_ENDPOINT));
        } catch (Exception ex) {
            fail("springboot real framework regression failed: " + ex);
        }
    }

    @Test
    @SuppressWarnings("all")
    public void frameworkStackFixtureShouldExposeXmlJspRpcAndMapperSignals() {
        try {
            Path jar = FixtureJars.frameworkStackTestJar();
            ProjectRuntimeContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, null);

            assertTrue(DatabaseManager.getServlets().contains("fixture/framework/web/XmlServlet"));
            assertTrue(DatabaseManager.getFilters().contains("fixture/framework/web/AuditFilter"));
            assertTrue(DatabaseManager.getListeners().contains("fixture/framework/web/BootstrapListener"));

            FrameworkXmlSourceIndex.Result xmlIndex = FrameworkXmlSourceIndex.currentProject();
            assertTrue(xmlIndex.servletClasses().contains("fixture/framework/web/XmlServlet"));
            assertTrue(xmlIndex.filterClasses().contains("fixture/framework/web/AuditFilter"));
            assertTrue(xmlIndex.listenerClasses().contains("fixture/framework/web/BootstrapListener"));
            assertTrue(xmlIndex.methodPatterns().stream().anyMatch(pattern ->
                    "fixture/framework/struts/LoginAction".equals(pattern.className())
                            && "execute".equals(pattern.methodName())));

            MyBatisMapperXmlIndex.Result myBatisIndex = MyBatisMapperXmlIndex.currentProject();
            assertFalse(myBatisIndex.sinkPatterns().isEmpty());

            MethodReference springMethod = requireMethod(
                    "fixture/framework/spring/AdminController",
                    "ping",
                    "()Ljava/lang/String;"
            );
            MethodReference servletMethod = requireMethod(
                    "fixture/framework/web/XmlServlet",
                    "doGet",
                    "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"
            );
            MethodReference filterMethod = requireMethod(
                    "fixture/framework/web/AuditFilter",
                    "doFilter",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V"
            );
            MethodReference listenerMethod = requireMethod(
                    "fixture/framework/web/BootstrapListener",
                    "contextInitialized",
                    "(Ljavax/servlet/ServletContextEvent;)V"
            );
            MethodReference strutsMethod = requireMethod(
                    "fixture/framework/struts/LoginAction",
                    "execute",
                    "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;"
            );
            MethodReference nettyMethod = requireMethod(
                    "fixture/framework/netty/AuthHandler",
                    "channelRead0",
                    "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V"
            );
            MethodReference rpcMethod = requireMethod(
                    "fixture/framework/rpc/OrderServiceImpl",
                    "submit",
                    "(Ljava/lang/String;)Ljava/lang/String;"
            );
            MethodReference mapperMethod = requireMethod(
                    "fixture/framework/mapper/UserMapper",
                    "findByName",
                    "(Ljava/lang/String;)Ljava/lang/String;"
            );
            MethodReference jspMethod = requireJspMethod("_jspService");

            assertSemantic(springMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT);
            assertSemantic(servletMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(filterMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(listenerMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK);
            assertSemantic(strutsMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.STRUTS_ACTION);
            assertSemantic(nettyMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.NETTY_HANDLER);
            assertSemantic(rpcMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.RPC_ENTRY);
            assertSemantic(mapperMethod, MethodSemanticFlags.MYBATIS_DYNAMIC_SQL);
            assertSemantic(jspMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.JSP_ENDPOINT);

            assertWebSource(servletMethod);
            assertWebSource(filterMethod);
            assertWebSource(listenerMethod);
            assertWebSource(strutsMethod);
            assertWebSource(nettyMethod);
            assertWebSource(jspMethod);

            int rpcSourceFlags = SourceRuleSupport.resolveCurrentSourceFlags(
                    "fixture/framework/rpc/OrderServiceImpl",
                    "submit",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    rpcMethod.getJarId()
            );
            assertTrue((rpcSourceFlags & GraphNode.SOURCE_FLAG_RPC) != 0);

            GraphSnapshot snapshot = new GraphStore().loadSnapshot();
            assertTrue(requireNode(snapshot, "fixture/framework/spring/AdminController", "ping", "()Ljava/lang/String;")
                    .hasMethodSemanticFlag(MethodSemanticFlags.SPRING_ENDPOINT));
            assertTrue(requireNode(snapshot, "fixture/framework/mapper/UserMapper", "findByName", "(Ljava/lang/String;)Ljava/lang/String;")
                    .hasMethodSemanticFlag(MethodSemanticFlags.MYBATIS_DYNAMIC_SQL));
            assertTrue(requireNode(snapshot, jspMethod.getClassReference().getName(), "_jspService", jspMethod.getDesc())
                    .hasSourceFlag(GraphNode.SOURCE_FLAG_WEB));
        } catch (Exception ex) {
            fail("framework stack regression failed: " + ex);
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
                    && className.contains("admin_jsp")) {
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
