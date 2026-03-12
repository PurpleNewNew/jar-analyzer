package me.n1ar4.jar.analyzer.server.handler;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.rules.SinkModel;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsApiUtilParseTest {
    @Test
    void parseShouldResolveSinkNameDirectlyFromRuleRegistry() {
        SinkModel sink = SinkRuleRegistry.getSinkModels().stream()
                .filter(model -> model != null && model.getBoxName() != null && !model.getBoxName().isBlank())
                .findFirst()
                .orElseThrow();

        DfsApiUtil.ParseResult result = DfsApiUtil.parse(session(Map.ofEntries(
                Map.entry("mode", "sink"),
                Map.entry("sinkName", sink.getBoxName()),
                Map.entry("sourceClass", "demo/Source"),
                Map.entry("sourceMethod", "entry"),
                Map.entry("sourceDesc", "()V")
        )));

        assertNull(result.getError());
        assertNotNull(result.getRequest());
        assertEquals(sink.getClassName(), result.getRequest().sinkClass);
        assertEquals(sink.getMethodName(), result.getRequest().sinkMethod);
        assertEquals("*".equals(sink.getMethodDesc()) ? "*" : sink.getMethodDesc(), result.getRequest().sinkDesc);
    }

    @Test
    void parseShouldHonorDocumentedTraversalParamsOnly() {
        DfsApiUtil.ParseResult result = DfsApiUtil.parse(session(Map.ofEntries(
                Map.entry("mode", "sink"),
                Map.entry("sinkClass", "demo/Sink"),
                Map.entry("sinkMethod", "exec"),
                Map.entry("sinkDesc", "()V"),
                Map.entry("sourceClass", "demo/Source"),
                Map.entry("sourceMethod", "entry"),
                Map.entry("sourceDesc", "()V"),
                Map.entry("searchAllSources", "true"),
                Map.entry("onlyFromWeb", "true"),
                Map.entry("minEdgeConfidence", "medium"),
                Map.entry("traversalMode", "call+alias"),
                Map.entry("timeoutMs", "321")
        )));
        assertNull(result.getError());
        assertNotNull(result.getRequest());
        assertTrue(result.getRequest().searchAllSources);
        assertEquals(Boolean.TRUE, result.getRequest().onlyFromWeb);
        assertEquals("medium", result.getRequest().minEdgeConfidence);
        assertEquals("call+alias", result.getRequest().traversalMode);
        assertEquals(321, result.getRequest().timeoutMs);
    }

    @Test
    void parseShouldIgnoreRemovedAliasParams() {
        DfsApiUtil.ParseResult result = DfsApiUtil.parse(session(Map.ofEntries(
                Map.entry("mode", "sink"),
                Map.entry("sinkClass", "demo/Sink"),
                Map.entry("sinkMethod", "exec"),
                Map.entry("sinkDesc", "()V"),
                Map.entry("sourceClass", "demo/Source"),
                Map.entry("sourceMethod", "entry"),
                Map.entry("sourceDesc", "()V"),
                Map.entry("allSources", "true"),
                Map.entry("sourceOnlyWeb", "true"),
                Map.entry("confidence", "high"),
                Map.entry("edgeMode", "call+alias"),
                Map.entry("timeout", "999")
        )));
        assertNull(result.getError());
        assertNotNull(result.getRequest());
        assertFalse(result.getRequest().searchAllSources);
        assertNull(result.getRequest().onlyFromWeb);
        assertEquals("low", result.getRequest().minEdgeConfidence);
        assertEquals("call-only", result.getRequest().traversalMode);
        assertNull(result.getRequest().timeoutMs);
    }

    private static NanoHTTPD.IHTTPSession session(Map<String, String> params) {
        Map<String, List<String>> multi = new LinkedHashMap<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                multi.put(entry.getKey(), List.of(entry.getValue()));
            }
        }
        return new NanoHTTPD.IHTTPSession() {
            @Override
            public void execute() throws IOException {
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
                return "/api/flow/dfs";
            }

            @Override
            public void parseBody(Map<String, String> files) throws IOException, NanoHTTPD.ResponseException {
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
}
