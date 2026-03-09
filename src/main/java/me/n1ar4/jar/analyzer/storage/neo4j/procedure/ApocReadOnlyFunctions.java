/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ApocReadOnlyFunctions {
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("\\p{IsM}+");
    private static final String[][] UMLAUT_REPLACEMENTS = {
            {"Ä", "Ae"},
            {"Ü", "Ue"},
            {"Ö", "Oe"},
            {"ä", "ae"},
            {"ü", "ue"},
            {"ö", "oe"},
            {"ß", "ss"}
    };

    @UserFunction(name = "apoc.coll.contains")
    @Description("Returns whether or not the given value exists in the given collection.")
    public Boolean collContains(@Name("coll") List<Object> coll,
                                @Name("value") Object value) {
        require("apoc.coll.contains");
        if (coll == null || coll.isEmpty()) {
            return false;
        }
        for (Object item : coll) {
            if (Objects.equals(item, value)) {
                return true;
            }
        }
        return false;
    }

    @UserFunction(name = "apoc.coll.containsAll")
    @Description("Returns whether or not all of the given values exist in the given collection.")
    public Boolean collContainsAll(@Name("coll1") List<Object> coll,
                                   @Name("coll2") List<Object> values) {
        require("apoc.coll.containsAll");
        if (coll == null || coll.isEmpty() || values == null) {
            return false;
        }
        for (Object value : values) {
            if (!collContains(coll, value)) {
                return false;
            }
        }
        return true;
    }

    @UserFunction(name = "apoc.coll.toSet")
    @Description("Returns a unique LIST from the given LIST.")
    public List<Object> collToSet(@Name("coll") List<Object> list) {
        require("apoc.coll.toSet");
        if (list == null) {
            return null;
        }
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    @UserFunction(name = "apoc.coll.intersection")
    @Description("Returns the distinct intersection of two LIST values.")
    public List<Object> collIntersection(@Name("list1") List<Object> first,
                                         @Name("list2") List<Object> second) {
        require("apoc.coll.intersection");
        if (first == null || second == null) {
            return List.of();
        }
        Set<Object> other = new LinkedHashSet<>(second);
        LinkedHashSet<Object> result = new LinkedHashSet<>();
        for (Object item : first) {
            if (other.contains(item)) {
                result.add(item);
            }
        }
        return new ArrayList<>(result);
    }

    @UserFunction(name = "apoc.coll.subtract")
    @Description("Returns the first LIST as a set with all elements of the second LIST removed.")
    public List<Object> collSubtract(@Name("list1") List<Object> first,
                                     @Name("list2") List<Object> second) {
        require("apoc.coll.subtract");
        if (first == null) {
            return null;
        }
        if (second == null) {
            return first;
        }
        Set<Object> removed = new LinkedHashSet<>(second);
        LinkedHashSet<Object> result = new LinkedHashSet<>();
        for (Object item : first) {
            if (!removed.contains(item)) {
                result.add(item);
            }
        }
        return new ArrayList<>(result);
    }

    @UserFunction(name = "apoc.coll.flatten")
    @Description("Flattens the given LIST. Set recursive=true to flatten nested lists recursively.")
    public List<Object> collFlatten(@Name("coll") List<Object> coll,
                                    @Name(value = "recursive", defaultValue = "false") boolean recursive) {
        require("apoc.coll.flatten");
        if (coll == null) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        flattenInto(coll, recursive, 0, result);
        return result;
    }

    @UserFunction(name = "apoc.map.fromPairs")
    @Description("Creates a MAP from the given LIST of key-value pairs.")
    public Map<String, Object> mapFromPairs(@Name("pairs") List<List<Object>> pairs) {
        require("apoc.map.fromPairs");
        if (pairs == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (List<Object> pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }
            Object key = pair.get(0);
            if (key == null) {
                continue;
            }
            Object value = pair.size() > 1 ? pair.get(1) : null;
            result.put(String.valueOf(key), value);
        }
        return result;
    }

    @UserFunction(name = "apoc.map.fromLists")
    @Description("Creates a MAP from the keys and values in the given LIST values.")
    public Map<String, Object> mapFromLists(@Name("keys") List<Object> keys,
                                            @Name("values") List<Object> values) {
        require("apoc.map.fromLists");
        if (keys == null || values == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        int count = Math.min(keys.size(), values.size());
        for (int i = 0; i < count; i++) {
            Object key = keys.get(i);
            if (key == null) {
                continue;
            }
            result.put(String.valueOf(key), values.get(i));
        }
        return result;
    }

    @UserFunction(name = "apoc.map.values")
    @Description("Returns a LIST of values indicated by the given keys.")
    public List<Object> mapValues(@Name("map") Map<String, Object> map,
                                  @Name(value = "keys", defaultValue = "[]") List<Object> keys,
                                  @Name(value = "addNullsForMissing", defaultValue = "false") boolean addNullsForMissing) {
        require("apoc.map.values");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<Object> values = new ArrayList<>(keys.size());
        for (Object key : keys) {
            String name = key == null ? null : String.valueOf(key);
            if (name == null) {
                if (addNullsForMissing) {
                    values.add(null);
                }
                continue;
            }
            if (map != null && map.containsKey(name)) {
                values.add(map.get(name));
                continue;
            }
            if (addNullsForMissing) {
                values.add(null);
            }
        }
        return values;
    }

    @UserFunction(name = "apoc.map.merge")
    @Description("Merges the two given MAP values into one MAP.")
    public Map<String, Object> mapMerge(@Name("map1") Map<String, Object> first,
                                        @Name("map2") Map<String, Object> second) {
        require("apoc.map.merge");
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        Map<String, Object> result = new LinkedHashMap<>(first);
        result.putAll(second);
        return result;
    }

    @UserFunction(name = "apoc.map.mergeList")
    @Description("Merges all MAP values in the given LIST into one MAP.")
    public Map<String, Object> mapMergeList(@Name("maps") List<Map<String, Object>> maps) {
        require("apoc.map.mergeList");
        if (maps == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> map : maps) {
            if (map != null) {
                result.putAll(map);
            }
        }
        return result;
    }

    @UserFunction(name = "apoc.map.get")
    @Description("Returns a value for the given key.")
    public Object mapGet(@Name("map") Map<String, Object> map,
                         @Name("key") String key,
                         @Name(value = "value", defaultValue = "null") Object value,
                         @Name(value = "fail", defaultValue = "true") boolean fail) {
        require("apoc.map.get");
        if (map == null) {
            if (fail && value == null) {
                throw new IllegalArgumentException("Key " + key + " is not of one of the existing keys []");
            }
            return value;
        }
        if (fail && value == null && !map.containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " is not of one of the existing keys " + map.keySet());
        }
        return map.getOrDefault(key, value);
    }

    @UserFunction(name = "apoc.map.removeKeys")
    @Description("Removes the given keys from the MAP.")
    public Map<String, Object> mapRemoveKeys(@Name("map") Map<String, Object> map,
                                             @Name("keys") List<Object> keys,
                                             @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        require("apoc.map.removeKeys");
        if (map == null) {
            return null;
        }
        Set<String> names = normalizeKeys(keys);
        boolean recursive = config != null && Boolean.TRUE.equals(config.get("recursive"));
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (names.contains(entry.getKey())) {
                continue;
            }
            Object value = recursive ? removeKeysRecursively(entry.getValue(), names) : entry.getValue();
            if (recursive && value instanceof Map<?, ?> nestedMap && nestedMap.isEmpty()) {
                continue;
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    @UserFunction(name = "apoc.text.indexOf")
    @Description("Returns the first occurrence of the lookup string in the given string, or -1 if not found.")
    public Long textIndexOf(@Name("text") String text,
                            @Name("lookup") String lookup,
                            @Name(value = "from", defaultValue = "0") long from,
                            @Name(value = "to", defaultValue = "-1") long to) {
        require("apoc.text.indexOf");
        if (text == null) {
            return null;
        }
        if (lookup == null) {
            return -1L;
        }
        if (to == -1L || to > text.length()) {
            return (long) text.indexOf(lookup, (int) from);
        }
        if (to <= from) {
            return -1L;
        }
        return (long) text.substring(0, (int) to).indexOf(lookup, (int) from);
    }

    @UserFunction(name = "apoc.text.replace")
    @Description("Finds and replaces all matches found by the given regular expression.")
    public String textReplace(@Name("text") String text,
                              @Name("regex") String regex,
                              @Name("replacement") String replacement) {
        require("apoc.text.replace");
        if (text == null || regex == null || replacement == null) {
            return null;
        }
        return text.replaceAll(regex, replacement);
    }

    @UserFunction(name = "apoc.text.split")
    @Description("Splits the given string using a regular expression as a separator.")
    public List<String> textSplit(@Name("text") String text,
                                  @Name("regex") String regex,
                                  @Name(value = "limit", defaultValue = "0") Long limit) {
        require("apoc.text.split");
        if (text == null || regex == null || limit == null) {
            return null;
        }
        return List.of(text.split(regex, limit.intValue()));
    }

    @UserFunction(name = "apoc.text.join")
    @Description("Joins the given strings using the given delimiter.")
    public String textJoin(@Name("texts") List<Object> texts,
                           @Name("delimiter") String delimiter) {
        require("apoc.text.join");
        if (texts == null || delimiter == null) {
            return null;
        }
        List<String> values = new ArrayList<>(texts.size());
        for (Object text : texts) {
            values.add(text == null ? "" : String.valueOf(text));
        }
        return String.join(delimiter, values);
    }

    @UserFunction(name = "apoc.text.clean")
    @Description("Strips the string of everything except alpha numeric characters and converts it to lower case.")
    public String textClean(@Name("text") String text) {
        require("apoc.text.clean");
        if (text == null) {
            return null;
        }
        String result = text;
        for (String[] replacement : UMLAUT_REPLACEMENTS) {
            result = result.replace(replacement[0], replacement[1]);
        }
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        String normalized = SPECIAL_CHAR_PATTERN.matcher(result).replaceAll("");
        return CLEAN_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    @UserFunction(name = "apoc.text.urlencode")
    @Description("Encodes the given URL string.")
    public String textUrlEncode(@Name("text") String text) {
        require("apoc.text.urlencode");
        if (text == null) {
            return null;
        }
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    @UserFunction(name = "apoc.text.urldecode")
    @Description("Decodes the given URL encoded string.")
    public String textUrlDecode(@Name("text") String text) {
        require("apoc.text.urldecode");
        if (text == null) {
            return null;
        }
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static void require(String functionName) {
        ApocWhitelist.requireAllowed(functionName);
    }

    private static void flattenInto(Object value, boolean recursive, int depth, List<Object> out) {
        if (value == null) {
            out.add(null);
            return;
        }
        if (value instanceof List<?> list) {
            if (!recursive && depth >= 1) {
                out.add(value);
                return;
            }
            for (Object item : list) {
                flattenInto(item, recursive, depth + 1, out);
            }
            return;
        }
        if (value.getClass().isArray()) {
            if (!recursive && depth >= 1) {
                out.add(value);
                return;
            }
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                flattenInto(Array.get(value, i), recursive, depth + 1, out);
            }
            return;
        }
        out.add(value);
    }

    private static Set<String> normalizeKeys(List<Object> keys) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (keys == null) {
            return result;
        }
        for (Object key : keys) {
            if (key != null) {
                result.add(String.valueOf(key));
            }
        }
        return result;
    }

    private static Object removeKeysRecursively(Object value, Set<String> keys) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (keys.contains(key)) {
                    continue;
                }
                Object nested = removeKeysRecursively(entry.getValue(), keys);
                if (nested instanceof Map<?, ?> nestedMap && nestedMap.isEmpty()) {
                    continue;
                }
                result.put(key, nested);
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>(collection.size());
            for (Object item : collection) {
                result.add(removeKeysRecursively(item, keys));
            }
            return result;
        }
        return value;
    }
}
