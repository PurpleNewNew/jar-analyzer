/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.server.handler;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.ListParser;
import me.n1ar4.jar.analyzer.gui.vul.Rule;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class VulSearchHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        VulRuleLoader.Result res = VulRuleLoader.load();
        Rule rule = res.getRule();
        if (rule == null || rule.getLevels() == null) {
            return buildError(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "vul_rule_not_found",
                    "vulnerability rule not found");
        }
        Map<String, Map<String, List<SearchCondition>>> levels = rule.getLevels();

        String nameParam = getParam(session, "name");
        String levelParam = getParam(session, "level");
        int limit = getIntParam(session, "limit", 0);
        int totalLimit = getIntParam(session, "totalLimit", 0);
        int offset = getIntParam(session, "offset", 0);
        if (offset < 0) {
            offset = 0;
        }
        String blacklistParam = getParam(session, "blacklist");
        List<String> blacklist = parseList(blacklistParam);
        String whitelistParam = getParam(session, "whitelist");
        if (StringUtil.isNull(whitelistParam)) {
            whitelistParam = getParam(session, "allowlist");
        }
        List<String> whitelist = parseList(whitelistParam);
        String groupBy = getParam(session, "groupBy");
        if (StringUtil.isNull(groupBy)) {
            groupBy = getParam(session, "group");
        }
        boolean groupByMethod = "method".equalsIgnoreCase(groupBy)
                || "flat".equalsIgnoreCase(groupBy);

        String jarNameParam = getParam(session, "jar");
        if (StringUtil.isNull(jarNameParam)) {
            jarNameParam = getParam(session, "jarName");
        }
        Set<String> jarNames = parseNameList(jarNameParam);
        Set<Integer> jarIds = parseIntSet(getParam(session, "jarId"));
        Set<String> nameFilter = parseNames(nameParam);
        boolean includeJdk = includeJdk(session);

        if (groupByMethod) {
            return buildGroupedByMethod(engine, res, rule, nameFilter, levelParam,
                    limit, totalLimit, offset, blacklist, whitelist, jarNames, jarIds, includeJdk);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        boolean truncated = false;
        int totalResults = 0;
        outer:
        for (Map.Entry<String, Map<String, List<SearchCondition>>> levelEntry : levels.entrySet()) {
            String entryLevel = levelEntry.getKey();
            if (!StringUtil.isNull(levelParam)
                    && (StringUtil.isNull(entryLevel)
                    || !levelParam.trim().equalsIgnoreCase(entryLevel.trim()))) {
                continue;
            }
            Map<String, List<SearchCondition>> byType = levelEntry.getValue();
            if (byType == null) {
                continue;
            }
            for (Map.Entry<String, List<SearchCondition>> entry : byType.entrySet()) {
                String vulName = entry.getKey();
                if (!nameFilter.isEmpty() && !nameFilter.contains(vulName)) {
                    continue;
                }
                List<SearchCondition> conditions = entry.getValue();
                if (conditions == null || conditions.isEmpty()) {
                    continue;
                }
                Map<String, MethodResult> uniq = new LinkedHashMap<>();
                for (SearchCondition condition : conditions) {
                    if (condition == null) {
                        continue;
                    }
                    String className = normalizeValue(condition.getClassName());
                    String methodName = normalizeValue(condition.getMethodName());
                    String methodDesc = normalizeValue(condition.getMethodDesc());
                    ArrayList<MethodResult> results = engine.getCallers(className, methodName, methodDesc);
                    for (MethodResult m : results) {
                        if (m == null) {
                            continue;
                        }
                        if (!isAllowed(m, blacklist, whitelist, jarNames, jarIds, includeJdk)) {
                            continue;
                        }
                        String key = String.format("%s#%s#%s",
                                m.getClassName(), m.getMethodName(), m.getMethodDesc());
                        if (!uniq.containsKey(key)) {
                            uniq.put(key, m);
                            totalResults++;
                            if (totalLimit > 0 && totalResults >= totalLimit) {
                                truncated = true;
                                break;
                            }
                            if (limit > 0 && uniq.size() >= limit) {
                                break;
                            }
                        }
                    }
                    if (truncated) {
                        break;
                    }
                    if (limit > 0 && uniq.size() >= limit) {
                        break;
                    }
                }
                if (truncated) {
                    break outer;
                }
                List<MethodResult> finalResults = new ArrayList<>(uniq.values());
                Map<String, Object> item = new HashMap<>();
                item.put("name", vulName);
                item.put("level", entryLevel);
                item.put("count", finalResults.size());
                item.put("results", finalResults);
                items.add(item);
            }
            if (truncated) {
                break;
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("source", res.getSource());
        out.put("groupBy", "rule");
        out.put("limitScope", "per_rule");
        out.put("items", items);
        out.put("total", items.size());
        out.put("totalResults", totalResults);
        out.put("limit", limit);
        out.put("totalLimit", totalLimit);
        out.put("truncated", truncated);
        return ok(out);
    }

    private Set<String> parseNames(String input) {
        if (StringUtil.isNull(input)) {
            return Collections.emptySet();
        }
        String[] parts = input.split(",");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String s = p.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private List<String> parseList(String text) {
        if (StringUtil.isNull(text)) {
            return Collections.emptyList();
        }
        return ListParser.parse(text);
    }

    private Set<String> parseNameList(String text) {
        if (StringUtil.isNull(text)) {
            return Collections.emptySet();
        }
        String[] parts = text.split("[,;\\r\\n]+");
        Set<String> out = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String s = p.trim();
            if (!s.isEmpty()) {
                out.add(s.toLowerCase());
            }
        }
        return out;
    }

    private Set<Integer> parseIntSet(String text) {
        if (StringUtil.isNull(text)) {
            return Collections.emptySet();
        }
        String[] parts = text.split("[,;\\r\\n]+");
        Set<Integer> out = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.parseInt(s));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private boolean isAllowed(MethodResult m, List<String> blacklist, List<String> whitelist,
                              Set<String> jarNames, Set<Integer> jarIds, boolean includeJdk) {
        if (m == null) {
            return false;
        }
        String className = m.getClassName();
        if (!includeJdk) {
            if (isJdkClass(className) || isNoisyJar(m.getJarName())) {
                return false;
            }
        }
        if (!whitelist.isEmpty() && !isWhitelisted(className, whitelist)) {
            return false;
        }
        if (isBlacklisted(className, blacklist)) {
            return false;
        }
        if (!jarIds.isEmpty()) {
            if (m.getJarId() <= 0 || !jarIds.contains(m.getJarId())) {
                return false;
            }
        }
        if (!jarNames.isEmpty()) {
            String jarName = m.getJarName();
            if (StringUtil.isNull(jarName) || !matchesJarName(jarName, jarNames)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesJarName(String jarName, Set<String> jarNames) {
        if (StringUtil.isNull(jarName) || jarNames == null || jarNames.isEmpty()) {
            return false;
        }
        String nameLower = jarName.toLowerCase();
        for (String p : jarNames) {
            if (nameLower.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWhitelisted(String className, List<String> whitelist) {
        if (StringUtil.isNull(className) || whitelist == null || whitelist.isEmpty()) {
            return whitelist == null || whitelist.isEmpty();
        }
        for (String w : whitelist) {
            if (StringUtil.isNull(w)) {
                continue;
            }
            if (className.equals(w) || className.startsWith(w)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlacklisted(String className, List<String> blacklist) {
        if (StringUtil.isNull(className) || blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        for (String b : blacklist) {
            if (StringUtil.isNull(b)) {
                continue;
            }
            if (className.equals(b) || className.startsWith(b)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeValue(String value) {
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v.replace('.', '/');
    }

    private NanoHTTPD.Response buildGroupedByMethod(CoreEngine engine,
                                                    VulRuleLoader.Result res,
                                                    Rule rule,
                                                    Set<String> nameFilter,
                                                    String levelParam,
                                                    int limit,
                                                    int totalLimit,
                                                    int offset,
                                                    List<String> blacklist,
                                                    List<String> whitelist,
                                                    Set<String> jarNames,
                                                    Set<Integer> jarIds,
                                                    boolean includeJdk) {
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        Map<String, Set<String>> ruleIndex = new HashMap<>();
        Map<String, Map<String, List<SearchCondition>>> levels = rule.getLevels();
        if (levels == null) {
            levels = Collections.emptyMap();
        }

        int stopAfter = 0;
        if (limit > 0) {
            stopAfter = offset + limit;
        }
        if (totalLimit > 0 && (stopAfter == 0 || totalLimit < stopAfter)) {
            stopAfter = totalLimit;
        }
        boolean truncated = false;

        outer:
        for (Map.Entry<String, Map<String, List<SearchCondition>>> levelEntry : levels.entrySet()) {
            String entryLevel = levelEntry.getKey();
            if (!StringUtil.isNull(levelParam)
                    && (StringUtil.isNull(entryLevel)
                    || !levelParam.trim().equalsIgnoreCase(entryLevel.trim()))) {
                continue;
            }
            Map<String, List<SearchCondition>> byType = levelEntry.getValue();
            if (byType == null) {
                continue;
            }
            for (Map.Entry<String, List<SearchCondition>> entry : byType.entrySet()) {
                String vulName = entry.getKey();
                if (!nameFilter.isEmpty() && !nameFilter.contains(vulName)) {
                    continue;
                }
                List<SearchCondition> conditions = entry.getValue();
                if (conditions == null || conditions.isEmpty()) {
                    continue;
                }
                for (SearchCondition condition : conditions) {
                    if (condition == null) {
                        continue;
                    }
                    String className = normalizeValue(condition.getClassName());
                    String methodName = normalizeValue(condition.getMethodName());
                    String methodDesc = normalizeValue(condition.getMethodDesc());
                        ArrayList<MethodResult> results = engine.getCallers(className, methodName, methodDesc);
                        for (MethodResult m : results) {
                            if (!isAllowed(m, blacklist, whitelist, jarNames, jarIds, includeJdk)) {
                                continue;
                            }
                        String key = String.format("%s#%s#%s",
                                m.getClassName(), m.getMethodName(), m.getMethodDesc());
                        Map<String, Object> rec = agg.get(key);
                        if (rec == null) {
                            rec = new LinkedHashMap<>();
                            rec.put("className", m.getClassName());
                            rec.put("methodName", m.getMethodName());
                            rec.put("methodDesc", m.getMethodDesc());
                            rec.put("jarName", m.getJarName());
                            rec.put("jarId", m.getJarId());
                            rec.put("rules", new ArrayList<Map<String, String>>());
                            agg.put(key, rec);
                        }
                        Set<String> seen = ruleIndex.computeIfAbsent(key, k -> new LinkedHashSet<>());
                        if (!seen.contains(vulName)) {
                            seen.add(vulName);
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> ruleList = (List<Map<String, String>>) rec.get("rules");
                            Map<String, String> ruleInfo = new LinkedHashMap<>();
                            ruleInfo.put("name", vulName);
                            ruleInfo.put("level", entryLevel);
                            ruleList.add(ruleInfo);
                        }
                        if (stopAfter > 0 && agg.size() >= stopAfter) {
                            truncated = true;
                            break outer;
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> all = new ArrayList<>(agg.values());
        int total = all.size();
        int from = Math.min(offset, total);
        int to = limit > 0 ? Math.min(from + limit, total) : total;
        List<Map<String, Object>> items = all.subList(from, to);

        Map<String, Object> out = new HashMap<>();
        out.put("source", res.getSource());
        out.put("groupBy", "method");
        out.put("limitScope", "total");
        out.put("offset", offset);
        out.put("limit", limit);
        out.put("total", total);
        out.put("totalLimit", totalLimit);
        out.put("truncated", truncated);
        out.put("items", items);
        return ok(out);
    }

}
