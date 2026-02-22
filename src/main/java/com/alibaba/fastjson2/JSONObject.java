/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package com.alibaba.fastjson2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONObject extends LinkedHashMap<String, Object> {
    public JSONObject() {
        super();
    }

    public JSONObject(Map<?, ?> map) {
        super();
        if (map == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
            put(key, JSON.wrap(entry.getValue()));
        }
    }

    public JSONObject getJSONObject(String key) {
        Object value = get(key);
        if (value instanceof JSONObject obj) {
            return obj;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject obj = new JSONObject(map);
            put(key, obj);
            return obj;
        }
        return null;
    }

    public JSONArray getJSONArray(String key) {
        Object value = get(key);
        if (value instanceof JSONArray arr) {
            return arr;
        }
        if (value instanceof Iterable<?> iterable) {
            JSONArray arr = new JSONArray(iterable);
            put(key, arr);
            return arr;
        }
        return null;
    }

    public String getString(String key) {
        Object value = get(key);
        return value == null ? null : String.valueOf(value);
    }

    public Integer getInteger(String key) {
        Number number = asNumber(get(key));
        return number == null ? null : number.intValue();
    }

    public int getIntValue(String key) {
        Integer value = getInteger(key);
        return value == null ? 0 : value;
    }

    public Long getLong(String key) {
        Number number = asNumber(get(key));
        return number == null ? null : number.longValue();
    }

    public long getLongValue(String key) {
        Long value = getLong(key);
        return value == null ? 0L : value;
    }

    public Float getFloat(String key) {
        Number number = asNumber(get(key));
        return number == null ? null : number.floatValue();
    }

    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        return null;
    }

    public boolean getBooleanValue(String key) {
        Boolean value = getBoolean(key);
        return value != null && value;
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    public String toJSONString(JSONWriter.Feature... features) {
        return JSON.toJSONString(this, features);
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = get(key);
        if (!(value instanceof Iterable<?> iterable)) {
            return null;
        }
        List<T> out = new java.util.ArrayList<>();
        for (Object item : iterable) {
            out.add(JSON.convert(item, clazz));
        }
        return out;
    }

    private static Number asNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                if (trimmed.contains(".") || trimmed.contains("e") || trimmed.contains("E")) {
                    return Double.parseDouble(trimmed);
                }
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
