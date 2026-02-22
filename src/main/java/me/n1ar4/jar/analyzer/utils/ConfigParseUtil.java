/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import me.n1ar4.jar.analyzer.entity.ConfigItem;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ConfigParseUtil {
    private static final int DEFAULT_MAX_BYTES = 512 * 1024;
    private static final int DEFAULT_MAX_ITEMS = 800;

    private ConfigParseUtil() {
    }

    public static List<ConfigItem> parse(ResourceEntity resource,
                                         int maxBytes,
                                         int maxItems,
                                         boolean maskValue) {
        if (resource == null) {
            return Collections.emptyList();
        }
        int bytes = maxBytes > 0 ? maxBytes : DEFAULT_MAX_BYTES;
        int itemsLimit = maxItems > 0 ? maxItems : DEFAULT_MAX_ITEMS;
        byte[] data = ArchiveContentResolver.readResourceBytes(resource, 0, bytes);
        if (data == null || data.length == 0) {
            return Collections.emptyList();
        }
        String content = new String(data, StandardCharsets.UTF_8);
        String fileName = extractFileName(resource.getResourcePath());
        String kind = guessKind(resource.getResourcePath(), fileName);
        List<ConfigItem> items;
        switch (kind) {
            case "json":
                items = parseJson(content, resource, maskValue, itemsLimit);
                break;
            case "yaml":
                items = parseYaml(content, resource, maskValue, itemsLimit);
                break;
            case "xml":
                items = parseXml(content, resource, maskValue, itemsLimit);
                break;
            case "env":
                items = parseEnv(content, resource, maskValue, itemsLimit);
                break;
            case "ini":
                items = parseIni(content, resource, maskValue, itemsLimit);
                break;
            default:
                items = parseProperties(content, resource, maskValue, itemsLimit);
                break;
        }
        for (ConfigItem item : items) {
            item.setKind(kind);
        }
        return items;
    }

    private static String extractFileName(String resourcePath) {
        if (StringUtil.isNull(resourcePath)) {
            return "";
        }
        String normalized = resourcePath.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            return normalized.substring(slash + 1);
        }
        return normalized;
    }

    public static boolean isConfigResource(ResourceEntity resource) {
        if (resource == null) {
            return false;
        }
        String path = resource.getResourcePath();
        if (StringUtil.isNull(path)) {
            return false;
        }
        String fileName = path;
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            fileName = path.substring(slash + 1);
        }
        return JarUtil.isConfigFile(fileName);
    }

    private static String guessKind(String resourcePath, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        String path = resourcePath == null ? "" : resourcePath.toLowerCase();
        if (name.endsWith(".yml") || name.endsWith(".yaml")) return "yaml";
        if (name.endsWith(".json")) return "json";
        if (name.endsWith(".xml")) return "xml";
        if (name.endsWith(".env") || name.endsWith(".dotenv")) return "env";
        if (name.endsWith(".ini")) return "ini";
        if (name.endsWith(".conf") || name.endsWith(".config")) return "properties";
        if (path.endsWith("web.xml")) return "xml";
        return "properties";
    }

    private static List<ConfigItem> parseProperties(String content,
                                                    ResourceEntity resource,
                                                    boolean maskValue,
                                                    int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length && out.size() < limit; i++) {
            String raw = lines[i];
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int idxEq = line.indexOf('=');
            int idxColon = line.indexOf(':');
            int idx = idxEq >= 0 && idxColon >= 0 ? Math.min(idxEq, idxColon)
                    : (idxEq >= 0 ? idxEq : idxColon);
            if (idx < 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (!isValidKey(key)) {
                continue;
            }
            ConfigItem item = buildItem(resource, key, value, raw, i + 1, maskValue);
            out.add(item);
        }
        return out;
    }

    private static List<ConfigItem> parseIni(String content,
                                             ResourceEntity resource,
                                             boolean maskValue,
                                             int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        String[] lines = content.split("\n");
        String section = "";
        for (int i = 0; i < lines.length && out.size() < limit; i++) {
            String raw = lines[i];
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int idx = line.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (!isValidKey(key)) {
                continue;
            }
            String fullKey = section.isEmpty() ? key : (section + "." + key);
            ConfigItem item = buildItem(resource, fullKey, value, raw, i + 1, maskValue);
            out.add(item);
        }
        return out;
    }

    private static List<ConfigItem> parseEnv(String content,
                                             ResourceEntity resource,
                                             boolean maskValue,
                                             int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length && out.size() < limit; i++) {
            String raw = lines[i];
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx < 1) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (!isValidKey(key)) {
                continue;
            }
            ConfigItem item = buildItem(resource, key, value, raw, i + 1, maskValue);
            out.add(item);
        }
        return out;
    }

    private static List<ConfigItem> parseYaml(String content,
                                              ResourceEntity resource,
                                              boolean maskValue,
                                              int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        String[] lines = content.split("\n");
        Deque<YamlNode> stack = new ArrayDeque<>();
        for (int i = 0; i < lines.length && out.size() < limit; i++) {
            String raw = lines[i];
            if (raw.trim().isEmpty()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("---") || trimmed.startsWith("...")) {
                continue;
            }
            if (trimmed.startsWith("- ")) {
                continue;
            }
            int indent = countIndent(raw);
            int idx = trimmed.indexOf(':');
            if (idx < 0) {
                continue;
            }
            String key = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (!isValidKey(key)) {
                continue;
            }
            while (!stack.isEmpty() && indent <= stack.peek().indent) {
                stack.pop();
            }
            if (value.isEmpty() || "|".equals(value) || ">".equals(value)) {
                stack.push(new YamlNode(key, indent));
                continue;
            }
            String fullKey = buildYamlKey(stack, key);
            ConfigItem item = buildItem(resource, fullKey, stripQuotes(value), raw, i + 1, maskValue);
            out.add(item);
        }
        return out;
    }

    private static List<ConfigItem> parseJson(String content,
                                              ResourceEntity resource,
                                              boolean maskValue,
                                              int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        Object obj;
        try {
            obj = JSON.parse(content);
        } catch (Exception e) {
            return out;
        }
        Map<String, Object> flat = new LinkedHashMap<>();
        flattenJson("", obj, flat, 0, 6);
        int line = 1;
        for (Map.Entry<String, Object> entry : flat.entrySet()) {
            if (out.size() >= limit) {
                break;
            }
            String key = entry.getKey();
            if (!isValidKey(key)) {
                continue;
            }
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            ConfigItem item = buildItem(resource, key, value, key + ": " + value, line, maskValue);
            out.add(item);
        }
        return out;
    }

    private static void flattenJson(String prefix,
                                    Object obj,
                                    Map<String, Object> out,
                                    int depth,
                                    int maxDepth) {
        if (obj == null || depth > maxDepth) {
            return;
        }
        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;
            for (String key : jo.keySet()) {
                String full = prefix.isEmpty() ? key : prefix + "." + key;
                flattenJson(full, jo.get(key), out, depth + 1, maxDepth);
            }
        } else if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            for (int i = 0; i < arr.size(); i++) {
                String full = prefix + "[" + i + "]";
                flattenJson(full, arr.get(i), out, depth + 1, maxDepth);
            }
        } else {
            out.put(prefix, obj);
        }
    }

    private static List<ConfigItem> parseXml(String content,
                                             ResourceEntity resource,
                                             boolean maskValue,
                                             int limit) {
        List<ConfigItem> out = new ArrayList<>();
        if (StringUtil.isNull(content)) {
            return out;
        }
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length && out.size() < limit; i++) {
            String raw = lines[i];
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("<!--")) {
                continue;
            }
            String key = null;
            String value = null;

            key = matchAttr(line, "name");
            if (key == null) key = matchAttr(line, "key");
            value = matchAttr(line, "value");
            if (value == null) value = matchAttr(line, "val");

            if (key == null) {
                String entryKey = matchEntryKey(line);
                if (entryKey != null) {
                    key = entryKey;
                    value = matchEntryValue(line);
                }
            }

            if (key == null) {
                continue;
            }
            if (!isValidKey(key)) {
                continue;
            }
            ConfigItem item = buildItem(resource, key, value == null ? "" : value, raw, i + 1, maskValue);
            out.add(item);
        }
        return out;
    }

    private static String matchAttr(String line, String attr) {
        String pattern = attr + "\\s*=\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static String matchEntryKey(String line) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("<entry\\s+[^>]*key\\s*=\\s*\"([^\"]+)\"[^>]*>");
        java.util.regex.Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static String matchEntryValue(String line) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("<entry\\s+[^>]*>([^<]*)</entry>");
        java.util.regex.Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static int countIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') count++;
            else if (c == '\t') count += 2;
            else break;
        }
        return count;
    }

    private static String buildYamlKey(Deque<YamlNode> stack, String key) {
        if (stack.isEmpty()) {
            return key;
        }
        List<String> parts = new ArrayList<>();
        Iterator<YamlNode> it = stack.descendingIterator();
        while (it.hasNext()) {
            parts.add(it.next().key);
        }
        parts.add(key);
        return String.join(".", parts);
    }

    private static String stripQuotes(String value) {
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static boolean isValidKey(String key) {
        if (StringUtil.isNull(key)) {
            return false;
        }
        String k = key.trim();
        if (k.length() < 2) {
            return false;
        }
        if (k.length() > 200) {
            return false;
        }
        if (k.contains(" ")) {
            return false;
        }
        return true;
    }

    private static ConfigItem buildItem(ResourceEntity resource,
                                        String key,
                                        String value,
                                        String rawLine,
                                        int lineNum,
                                        boolean maskValue) {
        ConfigItem item = new ConfigItem();
        item.setKey(key);
        item.setValue(limitValue(value));
        item.setMaskedValue(maskValue ? mask(value) : null);
        item.setResourcePath(resource.getResourcePath());
        item.setFilePath(resource.getPathStr());
        item.setJarId(resource.getJarId());
        item.setJarName(resource.getJarName());
        item.setLineNumber(lineNum);
        item.setPreview(rawLine == null ? "" : rawLine.trim());
        return item;
    }

    private static String limitValue(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() > 300) {
            return v.substring(0, 300) + "...";
        }
        return v;
    }

    private static String mask(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (v.length() <= 8) {
            return v.substring(0, 1) + "***";
        }
        return v.substring(0, 4) + "***" + v.substring(v.length() - 4);
    }

    private static class YamlNode {
        private final String key;
        private final int indent;

        private YamlNode(String key, int indent) {
            this.key = key;
            this.indent = indent;
        }
    }
}
