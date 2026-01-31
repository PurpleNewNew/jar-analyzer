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
import me.n1ar4.jar.analyzer.sca.SCAParser;
import me.n1ar4.jar.analyzer.sca.SCAVulDB;
import me.n1ar4.jar.analyzer.sca.dto.CVEData;
import me.n1ar4.jar.analyzer.sca.dto.SCAApiResult;
import me.n1ar4.jar.analyzer.sca.dto.SCARule;
import me.n1ar4.jar.analyzer.sca.utils.SCAHashUtil;
import me.n1ar4.jar.analyzer.sca.utils.SCAMultiUtil;
import me.n1ar4.jar.analyzer.sca.utils.SCASingleUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ScaApiUtil {
    enum ScaRuleKind {
        LOG4J,
        FASTJSON,
        SHIRO
    }

    static class ScaRequest {
        private final List<String> jarList;
        private final EnumSet<ScaRuleKind> enabledRules;

        ScaRequest(List<String> jarList, EnumSet<ScaRuleKind> enabledRules) {
            this.jarList = jarList;
            this.enabledRules = enabledRules == null
                    ? EnumSet.noneOf(ScaRuleKind.class)
                    : EnumSet.copyOf(enabledRules);
        }

        boolean isEnabled(ScaRuleKind kind) {
            return enabledRules.contains(kind);
        }
    }

    static class ParseResult {
        private final ScaRequest request;
        private final NanoHTTPD.Response error;

        ParseResult(ScaRequest request, NanoHTTPD.Response error) {
            this.request = request;
            this.error = error;
        }

        ScaRequest getRequest() {
            return request;
        }

        NanoHTTPD.Response getError() {
            return error;
        }
    }

    static ParseResult parse(NanoHTTPD.IHTTPSession session, CoreEngine engine) {
        EnumSet<ScaRuleKind> enabled = EnumSet.noneOf(ScaRuleKind.class);
        if (getBool(session, "log4j", true)) {
            enabled.add(ScaRuleKind.LOG4J);
        }
        if (getBool(session, "fastjson", true)) {
            enabled.add(ScaRuleKind.FASTJSON);
        }
        if (getBool(session, "shiro", true)) {
            enabled.add(ScaRuleKind.SHIRO);
        }
        if (enabled.isEmpty()) {
            return new ParseResult(null, buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "no_rule_enabled",
                    "no rule enabled"));
        }

        List<String> inputPaths = new ArrayList<>();
        inputPaths.addAll(getParams(session, "path"));
        inputPaths.addAll(splitParam(getParam(session, "paths")));

        List<String> jarList = new ArrayList<>();
        if (inputPaths.isEmpty()) {
            if (engine != null) {
                jarList.addAll(engine.getJarsPath());
            }
        } else {
            for (String p : inputPaths) {
                if (StringUtil.isNull(p)) {
                    continue;
                }
                jarList.addAll(resolveJarList(p.trim()));
            }
        }

        if (jarList.isEmpty()) {
            return new ParseResult(null, buildError(
                    NanoHTTPD.Response.Status.NOT_FOUND,
                    "no_jar_found",
                    "no jar found"));
        }

        return new ParseResult(new ScaRequest(jarList, enabled), null);
    }

    static List<SCAApiResult> scan(ScaRequest req) {
        Map<String, CVEData> cveMap = SCAVulDB.getCVEMap();
        if (cveMap == null) {
            cveMap = new HashMap<>();
        }
        List<SCARule> log4jRules = req.isEnabled(ScaRuleKind.LOG4J)
                ? safeRules(SCAParser.getApacheLog4j2Rules()) : Collections.emptyList();
        List<SCARule> fastjsonRules = req.isEnabled(ScaRuleKind.FASTJSON)
                ? safeRules(SCAParser.getFastjsonRules()) : Collections.emptyList();
        List<SCARule> shiroRules = req.isEnabled(ScaRuleKind.SHIRO)
                ? safeRules(SCAParser.getShiroRules()) : Collections.emptyList();

        List<SCAApiResult> results = new ArrayList<>();
        for (String jarPath : req.jarList) {
            List<String> exist = new ArrayList<>();
            if (req.isEnabled(ScaRuleKind.LOG4J)) {
                execWithOneRule(results, cveMap, jarPath, exist, log4jRules);
            }
            if (req.isEnabled(ScaRuleKind.FASTJSON)) {
                execWithOneRule(results, cveMap, jarPath, exist, fastjsonRules);
            }
            if (req.isEnabled(ScaRuleKind.SHIRO)) {
                execWithManyRules(results, cveMap, jarPath, exist, shiroRules);
            }
        }
        return results;
    }

    private static void execWithOneRule(List<SCAApiResult> results,
                                        Map<String, CVEData> cveMap,
                                        String jarPath,
                                        List<String> exist,
                                        List<SCARule> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        String keyClass = ruleList.get(0).getOnlyClassName();
        byte[] data = SCASingleUtil.exploreJar(Paths.get(jarPath).toFile(), keyClass);
        if (data == null) {
            return;
        }
        String targetJarClassHash = SCAHashUtil.sha256(data);
        for (SCARule rule : ruleList) {
            String hash = rule.getOnlyHash();
            if (!hash.equals(targetJarClassHash)) {
                continue;
            }
            if (exist.contains(rule.getCVE())) {
                continue;
            }
            exist.add(rule.getCVE());
            addResult(results, cveMap, rule, jarPath, keyClass, hash);
        }
    }

    private static void execWithManyRules(List<SCAApiResult> results,
                                          Map<String, CVEData> cveMap,
                                          String jarPath,
                                          List<String> exist,
                                          List<SCARule> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        Map<String, String> hashMap = ruleList.get(0).getHashMap();
        Map<String, byte[]> resultMap = SCAMultiUtil.exploreJarEx(Paths.get(jarPath).toFile(), hashMap);
        if (resultMap == null || resultMap.isEmpty()) {
            return;
        }
        for (SCARule rule : ruleList) {
            if (exist.contains(rule.getCVE())) {
                continue;
            }
            boolean flag = true;
            Map<String, String> ruleHashMap = rule.getHashMap();
            for (String key : resultMap.keySet()) {
                String data = SCAHashUtil.sha256(resultMap.get(key));
                String ruleHash = ruleHashMap.get(key);
                if (!data.equals(ruleHash)) {
                    flag = false;
                }
            }
            if (!flag) {
                continue;
            }
            exist.add(rule.getCVE());
            String keyClass = hashMap.entrySet().iterator().next().getKey();
            String hash = hashMap.entrySet().iterator().next().getValue();
            addResult(results, cveMap, rule, jarPath, keyClass, hash);
        }
    }

    private static void addResult(List<SCAApiResult> results,
                                  Map<String, CVEData> cveMap,
                                  SCARule rule,
                                  String jarPath,
                                  String keyClass,
                                  String hash) {
        SCAApiResult result = new SCAApiResult();
        result.setCve(rule.getCVE());
        result.setProject(rule.getProjectName());
        result.setVersion(rule.getVersion());
        result.setJarPath(jarPath);
        result.setKeyClass(keyClass);
        result.setHash(hash);
        CVEData data = cveMap.get(rule.getCVE());
        if (data != null) {
            result.setCvss(data.getCvss());
            result.setDesc(data.getDesc());
        }
        results.add(result);
    }

    private static List<SCARule> safeRules(List<SCARule> rules) {
        if (rules == null) {
            return Collections.emptyList();
        }
        return rules;
    }

    private static List<String> resolveJarList(String input) {
        Path path = Paths.get(input);
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                List<String> files = DirUtil.GetFiles(path.toAbsolutePath().toString());
                List<String> jars = new ArrayList<>();
                for (String file : files) {
                    if (isSupportedArchive(file)) {
                        jars.add(file);
                    }
                }
                return jars;
            }
            String abs = path.toAbsolutePath().toString();
            if (isSupportedArchive(abs)) {
                return Collections.singletonList(abs);
            }
        }
        return Collections.emptyList();
    }

    private static boolean isSupportedArchive(String path) {
        if (StringUtil.isNull(path)) {
            return false;
        }
        String name = path.toLowerCase();
        if (!(name.endsWith(".jar") || name.endsWith(".war"))) {
            return false;
        }
        // 排除 source/javadoc/test 等非运行包
        if (name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")
                || name.endsWith("-tests.jar") || name.endsWith("-test.jar")) {
            return false;
        }
        return true;
    }

    private static String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private static List<String> getParams(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        return data;
    }

    private static List<String> splitParam(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            result.add(part.trim());
        }
        return result;
    }

    private static boolean getBool(NanoHTTPD.IHTTPSession session, String key, boolean def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private static NanoHTTPD.Response buildError(NanoHTTPD.Response.Status status, String code, String msg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("code", code);
        result.put("message", msg);
        result.put("status", status.getRequestStatus());
        String json = JSON.toJSONString(result);
        return NanoHTTPD.newFixedLengthResponse(
                status,
                "application/json",
                json);
    }
}
