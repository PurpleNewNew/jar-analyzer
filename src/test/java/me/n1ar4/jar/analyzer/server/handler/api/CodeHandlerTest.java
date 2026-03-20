/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.mcp.JarAnalyzerMcpTools;
import me.n1ar4.jar.analyzer.mcp.McpTool;
import me.n1ar4.jar.analyzer.mcp.McpToolCallContext;
import me.n1ar4.jar.analyzer.mcp.McpToolRegistry;
import me.n1ar4.jar.analyzer.mcp.McpToolResult;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeHandlerTest {
    private Path markerFile;

    @AfterEach
    void cleanup() throws Exception {
        EngineContext.setEngine(null);
        DatabaseManager.clearAllData();
        if (markerFile != null) {
            Files.deleteIfExists(markerFile);
        }
    }

    @Test
    void handleShouldReturnMethodNotFoundWhenExactMethodDoesNotMatch() throws Exception {
        String className = installReadyEngineForClass(CodeHandlerTest.class, 1);

        NanoHTTPD.Response response = new CodeHandler().handle(session(Map.of(
                "class", className,
                "method", "handleShouldReturnMethodNotFoundWhenExactMethodDoesNotMatch",
                "desc", "(I)V"
        )));

        assertEquals(NanoHTTPD.Response.Status.NOT_FOUND, response.getStatus());
        JSONObject body = JSON.parseObject(readBody(response));
        assertEquals("method_not_found", body.getString("code"));
    }

    @Test
    void handleShouldNotFallbackToAnotherJarWhenJarIdMisses() throws Exception {
        String className = installReadyEngineForClass(CodeHandlerTest.class, 1);

        NanoHTTPD.Response response = new CodeHandler().handle(session(Map.of(
                "class", className,
                "method", "handleShouldNotFallbackToAnotherJarWhenJarIdMisses",
                "desc", "()V",
                "jarId", "2"
        )));

        assertEquals(NanoHTTPD.Response.Status.NOT_FOUND, response.getStatus());
        JSONObject body = JSON.parseObject(readBody(response));
        assertEquals("class_not_found", body.getString("code"));
    }

    @Test
    void handleShouldNotGuessOverloadWhenDescMissing() throws Exception {
        String className = OverloadedFixture.class.getName().replace('.', '/');
        installReadyEngineForClass(OverloadedFixture.class, 1, Set.of(
                methodRef(className, 1, "overloaded", "()V"),
                methodRef(className, 1, "overloaded", "(Ljava/lang/String;)V")
        ));

        NanoHTTPD.Response response = new CodeHandler().handle(session(Map.of(
                "class", className,
                "method", "overloaded",
                "jarId", "1"
        )));

        assertEquals(NanoHTTPD.Response.Status.NOT_FOUND, response.getStatus());
        JSONObject body = JSON.parseObject(readBody(response));
        assertEquals("method_not_found", body.getString("code"));
        assertTrue(body.getString("message").contains("descriptor required"));
    }

    @Test
    void codeGetToolShouldForwardJarId() throws Exception {
        String className = installReadyEngineForClass(CodeHandlerTest.class, 1);
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(null));
        McpTool tool = registry.get("code_get");
        assertNotNull(tool);
        assertTrue(tool.getDefinition().toJSONString().contains("\"jarId\""));

        JSONObject args = new JSONObject();
        args.put("class", className);
        args.put("method", "handleShouldReturnMethodNotFoundWhenExactMethodDoesNotMatch");
        args.put("jarId", "2");

        McpToolResult result = tool.getHandler().call(new McpToolCallContext(Map.of()), args);

        assertTrue(result.isError());
        assertTrue(result.getText().contains("class_not_found"));
    }

    private String installReadyEngineForClass(Class<?> type, int jarId) throws Exception {
        return installReadyEngineForClass(type, jarId, Set.of());
    }

    private String installReadyEngineForClass(Class<?> type,
                                              int jarId,
                                              Set<MethodReference> methods) throws Exception {
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        Path projectHome = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
        Files.createDirectories(projectHome);
        markerFile = projectHome.resolve("code-handler-ready.marker");
        Files.writeString(markerFile, "ok");

        String resourceName = type.getName().substring(type.getPackageName().length() + 1) + ".class";
        Path classPath = Path.of(type.getResource(resourceName).toURI())
                .toAbsolutePath()
                .normalize();
        String className = type.getName().replace('.', '/');
        DatabaseManager.restoreProjectRuntime(new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                11L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        ProjectModel.artifact(classPath, null, List.of(classPath), false).buildMode().name(),
                        classPath.toString(),
                        "",
                        List.of(),
                        List.of(classPath.toString()),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        ));
        DatabaseManager.saveClassFiles(Set.of(classFile(className, classPath, jarId)));
        if (methods != null && !methods.isEmpty()) {
            DatabaseManager.saveMethods(methods);
        }

        ConfigFile config = new ConfigFile();
        config.setDbPath(projectHome.toString());
        EngineContext.setEngine(new CoreEngine(config));
        return className;
    }

    private static ClassFileEntity classFile(String className, Path path, int jarId) {
        ClassFileEntity entity = new ClassFileEntity();
        entity.setClassName(className);
        entity.setJarId(jarId);
        entity.setPath(path);
        return entity;
    }

    private static MethodReference methodRef(String className, int jarId, String methodName, String methodDesc) {
        return new MethodReference(
                new ClassReference.Handle(className, jarId),
                methodName,
                methodDesc,
                false,
                Set.of(),
                1,
                1,
                "test.jar",
                jarId
        );
    }

    private static NanoHTTPD.IHTTPSession session(Map<String, String> params) {
        Map<String, List<String>> multi = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            multi.put(entry.getKey(), List.of(entry.getValue()));
        }
        return new NanoHTTPD.IHTTPSession() {
            @Override
            public void execute() {
                throw new UnsupportedOperationException();
            }

            @Override
            public NanoHTTPD.CookieHandler getCookies() {
                return null;
            }

            @Override
            public Map<String, String> getHeaders() {
                return Collections.emptyMap();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public NanoHTTPD.Method getMethod() {
                return NanoHTTPD.Method.GET;
            }

            @Override
            public Map<String, String> getParms() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, List<String>> getParameters() {
                return multi;
            }

            @Override
            public String getQueryParameterString() {
                return "";
            }

            @Override
            public String getUri() {
                return "/api/code";
            }

            @Override
            public void parseBody(Map<String, String> files) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRemoteIpAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getRemoteHostName() {
                return "localhost";
            }
        };
    }

    private static String readBody(NanoHTTPD.Response response) throws Exception {
        try (InputStream in = response.getData()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class OverloadedFixture {
        public void overloaded() {
        }

        public void overloaded(String value) {
        }
    }
}
