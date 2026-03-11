package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.cef.callback.CefQueryCallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class JcefBridgeRouterTest {
    @Test
    void shouldRejectQueriesAfterClose() throws Exception {
        JcefBridgeRouter router = new JcefBridgeRouter();
        try {
            router.close();
            AtomicReference<String> payload = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            boolean accepted = router.dispatchQuery("{\"channel\":\"ja.query.execute\",\"payload\":{}}",
                    callback(payload, latch));

            Assertions.assertTrue(accepted);
            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
            JSONObject result = JSON.parseObject(payload.get());
            Assertions.assertFalse(result.getBooleanValue("ok"));
            Assertions.assertEquals("bridge_closed", result.getString("code"));
        } finally {
            router.close();
        }
    }

    @Test
    void shouldMapIllegalArgumentToQueryErrorEnvelope() throws Exception {
        JcefBridgeRouter router = new JcefBridgeRouter();
        try {
            router.register("ja.query.execute", payload -> {
                throw new IllegalArgumentException("cypher_empty_query: query required");
            });
            AtomicReference<String> payload = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            boolean accepted = router.dispatchQuery("{\"channel\":\"ja.query.execute\",\"payload\":{}}",
                    callback(payload, latch));

            Assertions.assertTrue(accepted);
            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
            JSONObject result = JSON.parseObject(payload.get());
            Assertions.assertFalse(result.getBooleanValue("ok"));
            Assertions.assertEquals("cypher_empty_query", result.getString("code"));
            Assertions.assertEquals("cypher_empty_query: query required", result.getString("message"));
        } finally {
            router.close();
        }
    }

    private static CefQueryCallback callback(AtomicReference<String> payload, CountDownLatch latch) {
        return new CefQueryCallback() {
            @Override
            public void success(String response) {
                payload.set(response);
                latch.countDown();
            }

            @Override
            public void failure(int errorCode, String errorMessage) {
                payload.set("{\"ok\":false,\"code\":\"native_failure\",\"message\":\""
                        + errorCode + ":" + errorMessage + "\"}");
                latch.countDown();
            }
        };
    }
}
