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

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface JSON {
    ObjectMapper MAPPER = createMapper();

    public static Object parse(String text) {
        String normalized = sanitizeInput(text);
        if (normalized == null || normalized.trim().isEmpty()) {
            return null;
        }
        try {
            Object raw = MAPPER.readValue(normalized, Object.class);
            return wrap(raw);
        } catch (IOException ex) {
            throw new IllegalArgumentException("invalid json", ex);
        }
    }

    public static JSONObject parseObject(String text) {
        Object value = parse(text);
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject obj) {
            return obj;
        }
        if (value instanceof Map<?, ?> map) {
            return new JSONObject(map);
        }
        throw new IllegalArgumentException("json is not object");
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        String normalized = sanitizeInput(text);
        if (normalized == null || normalized.trim().isEmpty()) {
            return null;
        }
        if (clazz == JSONObject.class) {
            return clazz.cast(parseObject(text));
        }
        if (clazz == JSONArray.class) {
            return clazz.cast(parseArray(text));
        }
        try {
            return MAPPER.readValue(normalized, clazz);
        } catch (IOException ex) {
            throw new IllegalArgumentException("invalid json", ex);
        }
    }

    public static <T> T parseObject(String text, com.alibaba.fastjson2.TypeReference<T> typeReference) {
        String normalized = sanitizeInput(text);
        if (normalized == null || normalized.trim().isEmpty()) {
            return null;
        }
        JavaType type = MAPPER.getTypeFactory().constructType(typeReference.getType());
        try {
            return MAPPER.readValue(normalized, type);
        } catch (IOException ex) {
            throw new IllegalArgumentException("invalid json", ex);
        }
    }

    public static JSONArray parseArray(String text) {
        Object value = parse(text);
        if (value == null) {
            return null;
        }
        if (value instanceof JSONArray arr) {
            return arr;
        }
        if (value instanceof Iterable<?> iterable) {
            return new JSONArray(iterable);
        }
        throw new IllegalArgumentException("json is not array");
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        String normalized = sanitizeInput(text);
        if (normalized == null || normalized.trim().isEmpty()) {
            return List.of();
        }
        JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
        try {
            return MAPPER.readValue(normalized, type);
        } catch (IOException ex) {
            throw new IllegalArgumentException("invalid json", ex);
        }
    }

    public static String toJSONString(Object value) {
        return toJSONString(value, new JSONWriter.Feature[0]);
    }

    public static String toJSONString(Object value, JSONWriter.Feature... features) {
        boolean pretty = false;
        if (features != null) {
            for (JSONWriter.Feature feature : features) {
                if (feature == JSONWriter.Feature.PrettyFormat) {
                    pretty = true;
                    break;
                }
            }
        }
        Object normalized = unwrap(value);
        try {
            if (pretty) {
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
            }
            return MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("serialize json failed", ex);
        }
    }

    static Object wrap(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject object = new JSONObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                object.put(key, wrap(entry.getValue()));
            }
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            JSONArray array = new JSONArray();
            for (Object item : iterable) {
                array.add(wrap(item));
            }
            return array;
        }
        return value;
    }

    private static Object unwrap(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject object) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                map.put(entry.getKey(), unwrap(entry.getValue()));
            }
            return map;
        }
        if (value instanceof JSONArray array) {
            List<Object> list = new ArrayList<>(array.size());
            for (Object item : array) {
                list.add(unwrap(item));
            }
            return list;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                out.put(key, unwrap(entry.getValue()));
            }
            return out;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> out = new ArrayList<>();
            for (Object item : iterable) {
                out.add(unwrap(item));
            }
            return out;
        }
        return value;
    }

    static <T> T convert(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return MAPPER.convertValue(unwrap(value), clazz);
    }

    private static String sanitizeInput(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text;
        if (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        AnnotationIntrospector ser = new FastjsonAnnotationIntrospector(true);
        AnnotationIntrospector deser = new FastjsonAnnotationIntrospector(false);
        mapper.setAnnotationIntrospectors(ser, deser);
        return mapper;
    }

    final class FastjsonAnnotationIntrospector extends JacksonAnnotationIntrospector {
        private final boolean serialization;

        private FastjsonAnnotationIntrospector(boolean serialization) {
            this.serialization = serialization;
        }

        @Override
        public PropertyName findNameForSerialization(Annotated annotated) {
            JSONField field = _findAnnotation(annotated, JSONField.class);
            if (field != null && field.name() != null && !field.name().isEmpty()) {
                return PropertyName.construct(field.name());
            }
            return super.findNameForSerialization(annotated);
        }

        @Override
        public PropertyName findNameForDeserialization(Annotated annotated) {
            JSONField field = _findAnnotation(annotated, JSONField.class);
            if (field != null && field.name() != null && !field.name().isEmpty()) {
                return PropertyName.construct(field.name());
            }
            return super.findNameForDeserialization(annotated);
        }

        @Override
        public boolean hasIgnoreMarker(AnnotatedMember member) {
            JSONField field = _findAnnotation(member, JSONField.class);
            if (field != null) {
                if (serialization && !field.serialize()) {
                    return true;
                }
                if (!serialization && !field.deserialize()) {
                    return true;
                }
            }
            return super.hasIgnoreMarker(member);
        }
    }
}
