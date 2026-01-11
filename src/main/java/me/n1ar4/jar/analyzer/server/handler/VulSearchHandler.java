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

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.ListParser;
import me.n1ar4.jar.analyzer.gui.vul.Rule;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class VulSearchHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        VulRuleLoader.Result res = VulRuleLoader.load();
        Rule rule = res.getRule();
        if (rule == null || rule.getVulnerabilities() == null) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "text/html",
                    "<h1>JAR ANALYZER SERVER</h1>" +
                            "<h2>VULNERABILITY RULE NOT FOUND</h2>");
        }

        String nameParam = getParam(session, "name");
        String levelParam = getParam(session, "level");
        int limit = getIntParam(session, "limit", 0);
        String blacklistParam = getParam(session, "blacklist");
        List<String> blacklist = parseList(blacklistParam);

        Set<String> nameFilter = parseNames(nameParam);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, List<SearchCondition>> entry : rule.getVulnerabilities().entrySet()) {
            String vulName = entry.getKey();
            if (!nameFilter.isEmpty() && !nameFilter.contains(vulName)) {
                continue;
            }
            List<SearchCondition> conditions = entry.getValue();
            if (conditions == null || conditions.isEmpty()) {
                continue;
            }
            Map<String, MethodResult> uniq = new LinkedHashMap<>();
            String entryLevel = null;
            for (SearchCondition condition : conditions) {
                if (condition == null) {
                    continue;
                }
                if (entryLevel == null && !StringUtil.isNull(condition.getLevel())) {
                    entryLevel = condition.getLevel();
                }
                if (!StringUtil.isNull(levelParam)) {
                    if (StringUtil.isNull(condition.getLevel())) {
                        continue;
                    }
                    if (!levelParam.trim().equalsIgnoreCase(condition.getLevel().trim())) {
                        continue;
                    }
                }
                String className = normalizeValue(condition.getClassName());
                String methodName = normalizeValue(condition.getMethodName());
                String methodDesc = normalizeValue(condition.getMethodDesc());
                ArrayList<MethodResult> results = engine.getCallers(className, methodName, methodDesc);
                for (MethodResult m : results) {
                    if (m == null) {
                        continue;
                    }
                    if (isBlacklisted(m.getClassName(), blacklist)) {
                        continue;
                    }
                    String key = String.format("%s#%s#%s",
                            m.getClassName(), m.getMethodName(), m.getMethodDesc());
                    if (!uniq.containsKey(key)) {
                        uniq.put(key, m);
                        if (limit > 0 && uniq.size() >= limit) {
                            break;
                        }
                    }
                }
                if (limit > 0 && uniq.size() >= limit) {
                    break;
                }
            }
            List<MethodResult> finalResults = new ArrayList<>(uniq.values());
            Map<String, Object> item = new HashMap<>();
            item.put("name", vulName);
            item.put("level", entryLevel);
            item.put("count", finalResults.size());
            item.put("results", finalResults);
            items.add(item);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("source", res.getSource());
        out.put("items", items);
        out.put("total", items.size());
        String json = JSON.toJSONString(out);
        return buildJSON(json);
    }

    private String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private int getIntParam(NanoHTTPD.IHTTPSession session, String key, int def) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return def;
        }
        String value = data.get(0);
        if (StringUtil.isNull(value)) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return def;
        }
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
}
