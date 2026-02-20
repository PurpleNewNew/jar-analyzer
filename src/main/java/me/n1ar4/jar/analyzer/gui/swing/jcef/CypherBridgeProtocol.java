/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CypherBridgeProtocol {
    private CypherBridgeProtocol() {
    }

    public static BridgeRequest parseRequest(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BridgeException("bridge_invalid_request", "empty request");
        }
        JSONObject obj;
        try {
            obj = JSON.parseObject(raw);
        } catch (Exception ex) {
            throw new BridgeException("bridge_invalid_request", "invalid json request");
        }
        if (obj == null) {
            throw new BridgeException("bridge_invalid_request", "invalid request");
        }
        String channel = safe(obj.getString("channel"));
        if (channel.isBlank()) {
            throw new BridgeException("bridge_invalid_request", "channel required");
        }
        Object payloadObj = obj.get("payload");
        JSONObject payload;
        if (payloadObj instanceof JSONObject jsonObject) {
            payload = jsonObject;
        } else if (payloadObj == null) {
            payload = new JSONObject();
        } else {
            payload = JSON.parseObject(JSON.toJSONString(payloadObj));
            if (payload == null) {
                payload = new JSONObject();
            }
        }
        return new BridgeRequest(channel, payload);
    }

    public static String success(String channel, Object data) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("channel", safe(channel));
        out.put("data", data);
        return JSON.toJSONString(out);
    }

    public static String error(String channel, String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("channel", safe(channel));
        out.put("code", safe(code));
        out.put("message", safe(message));
        return JSON.toJSONString(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record BridgeRequest(String channel, JSONObject payload) {
    }

    public static final class BridgeException extends RuntimeException {
        private final String code;

        public BridgeException(String code, String message) {
            super(message);
            this.code = safe(code).isBlank() ? "bridge_error" : safe(code);
        }

        public String code() {
            return code;
        }
    }
}
