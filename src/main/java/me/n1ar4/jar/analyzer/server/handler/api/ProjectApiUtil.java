/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoHTTPD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProjectApiUtil {
    private ProjectApiUtil() {
    }

    static String resolveString(NanoHTTPD.IHTTPSession session, String key) {
        if (session == null || key == null || key.isBlank()) {
            return "";
        }
        List<String> values = session.getParameters().get(key);
        if (values != null && !values.isEmpty()) {
            String value = values.get(0);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        JSONObject body = parseBody(session);
        if (body == null) {
            return "";
        }
        String value = body.getString(key);
        return value == null ? "" : value.trim();
    }

    private static JSONObject parseBody(NanoHTTPD.IHTTPSession session) {
        if (session == null || session.getMethod() == NanoHTTPD.Method.GET) {
            return null;
        }
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            if (postData == null || postData.isBlank()) {
                return null;
            }
            return JSON.parseObject(postData);
        } catch (Exception ignored) {
            return null;
        }
    }
}

