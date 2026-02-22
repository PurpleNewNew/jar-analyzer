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

import java.util.ArrayList;
import java.util.Iterator;

public class JSONArray extends ArrayList<Object> {
    public JSONArray() {
        super();
    }

    public JSONArray(Iterable<?> values) {
        super();
        if (values == null) {
            return;
        }
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
            add(JSON.wrap(iterator.next()));
        }
    }

    public static JSONArray parse(String text) {
        return JSON.parseArray(text);
    }

    public JSONObject getJSONObject(int index) {
        Object value = get(index);
        if (value instanceof JSONObject obj) {
            return obj;
        }
        if (value instanceof java.util.Map<?, ?> map) {
            JSONObject obj = new JSONObject(map);
            set(index, obj);
            return obj;
        }
        return null;
    }

    public JSONArray getJSONArray(int index) {
        Object value = get(index);
        if (value instanceof JSONArray arr) {
            return arr;
        }
        if (value instanceof Iterable<?> iterable) {
            JSONArray arr = new JSONArray(iterable);
            set(index, arr);
            return arr;
        }
        return null;
    }

    public String getString(int index) {
        Object value = get(index);
        return value == null ? null : String.valueOf(value);
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
}
