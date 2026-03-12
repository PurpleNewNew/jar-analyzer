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
            RecordingCallback callback = new RecordingCallback(payload, latch);

            boolean accepted = router.dispatchQuery("{\"channel\":\"ja.query.execute\",\"payload\":{}}", callback);

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
            RecordingCallback callback = new RecordingCallback(payload, latch);

            boolean accepted = router.dispatchQuery("{\"channel\":\"ja.query.execute\",\"payload\":{}}", callback);

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

    @Test
    void shouldDropLateReplyAfterClose() throws Exception {
        JcefBridgeRouter router = new JcefBridgeRouter();
        try {
            CountDownLatch handlerStarted = new CountDownLatch(1);
            CountDownLatch releaseHandler = new CountDownLatch(1);
            AtomicReference<String> payload = new AtomicReference<>();
            CountDownLatch callbackLatch = new CountDownLatch(1);
            RecordingCallback callback = new RecordingCallback(payload, callbackLatch);
            router.register("ja.query.execute", ignored -> {
                handlerStarted.countDown();
                releaseHandler.await(2, TimeUnit.SECONDS);
                JSONObject result = new JSONObject();
                result.put("ok", true);
                return result;
            });

            boolean accepted = router.dispatchQuery("{\"channel\":\"ja.query.execute\",\"payload\":{}}", callback);

            Assertions.assertTrue(accepted);
            Assertions.assertTrue(handlerStarted.await(2, TimeUnit.SECONDS));
            router.close();
            releaseHandler.countDown();
            Assertions.assertFalse(callbackLatch.await(300, TimeUnit.MILLISECONDS));
            Assertions.assertNull(payload.get());
        } finally {
            router.close();
        }
    }

    private static final class RecordingCallback implements CefQueryCallback {
        private final AtomicReference<String> payload;
        private final CountDownLatch latch;

        private RecordingCallback(AtomicReference<String> payload, CountDownLatch latch) {
            this.payload = payload;
            this.latch = latch;
        }

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
    }
}
