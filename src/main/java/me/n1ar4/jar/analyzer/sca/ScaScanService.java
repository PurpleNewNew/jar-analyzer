/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.sca;

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

public final class ScaScanService {
    private static final Map<String, CVEData> CVE_MAP = safeCveMap(SCAVulDB.getCVEMap());
    private static final List<SCARule> LOG4J_RULES = safeRules(SCAParser.getApacheLog4j2Rules());
    private static final List<SCARule> FASTJSON_RULES = safeRules(SCAParser.getFastjsonRules());
    private static final List<SCARule> SHIRO_RULES = safeRules(SCAParser.getShiroRules());

    public List<SCAApiResult> scan(Request request) {
        Request safeRequest = request == null ? Request.empty() : request;
        if (safeRequest.jarList().isEmpty() || safeRequest.enabledRules().isEmpty()) {
            return List.of();
        }
        List<SCAApiResult> results = new ArrayList<>();
        for (String jarPath : safeRequest.jarList()) {
            List<String> exist = new ArrayList<>();
            if (safeRequest.isEnabled(RuleKind.LOG4J)) {
                execWithOneRule(results, jarPath, exist, LOG4J_RULES);
            }
            if (safeRequest.isEnabled(RuleKind.FASTJSON)) {
                execWithOneRule(results, jarPath, exist, FASTJSON_RULES);
            }
            if (safeRequest.isEnabled(RuleKind.SHIRO)) {
                execWithManyRules(results, jarPath, exist, SHIRO_RULES);
            }
        }
        return results;
    }

    public static List<String> resolveJarList(String input) {
        if (StringUtil.isNull(input)) {
            return Collections.emptyList();
        }
        Path path = Paths.get(input.trim());
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        if (Files.isDirectory(path)) {
            List<String> files = DirUtil.getFiles(path.toAbsolutePath().toString());
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
        return Collections.emptyList();
    }

    public static EnumSet<RuleKind> enabledRules(boolean log4j, boolean fastjson, boolean shiro) {
        EnumSet<RuleKind> enabled = EnumSet.noneOf(RuleKind.class);
        if (log4j) {
            enabled.add(RuleKind.LOG4J);
        }
        if (fastjson) {
            enabled.add(RuleKind.FASTJSON);
        }
        if (shiro) {
            enabled.add(RuleKind.SHIRO);
        }
        return enabled;
    }

    private static void execWithOneRule(List<SCAApiResult> results,
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
        String actualHash = SCAHashUtil.sha256(data);
        for (SCARule rule : ruleList) {
            if (rule == null || exist.contains(rule.getCVE())) {
                continue;
            }
            if (!safe(rule.getOnlyHash()).equals(actualHash)) {
                continue;
            }
            exist.add(rule.getCVE());
            results.add(buildResult(rule, jarPath, keyClass, actualHash));
        }
    }

    private static void execWithManyRules(List<SCAApiResult> results,
                                          String jarPath,
                                          List<String> exist,
                                          List<SCARule> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        Map<String, String> baseMap = ruleList.get(0).getHashMap();
        Map<String, byte[]> resultMap = SCAMultiUtil.exploreJarEx(Paths.get(jarPath).toFile(), baseMap);
        if (resultMap == null || resultMap.isEmpty()) {
            return;
        }
        for (SCARule rule : ruleList) {
            if (rule == null || exist.contains(rule.getCVE())) {
                continue;
            }
            Map<String, String> ruleHashMap = rule.getHashMap();
            if (!matchesAllHashes(resultMap, ruleHashMap)) {
                continue;
            }
            exist.add(rule.getCVE());
            Map.Entry<String, String> first = baseMap.entrySet().iterator().next();
            results.add(buildResult(rule, jarPath, first.getKey(), first.getValue()));
        }
    }

    private static boolean matchesAllHashes(Map<String, byte[]> resultMap, Map<String, String> expectedHashes) {
        if (resultMap == null || resultMap.isEmpty()) {
            return false;
        }
        if (expectedHashes == null || expectedHashes.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, byte[]> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            String expected = expectedHashes.get(key);
            String actual = SCAHashUtil.sha256(entry.getValue());
            if (!actual.equals(expected)) {
                return false;
            }
        }
        return true;
    }

    private static SCAApiResult buildResult(SCARule rule,
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
        CVEData cve = CVE_MAP.get(rule.getCVE());
        if (cve != null) {
            result.setCvss(cve.getCvss());
            result.setDesc(cve.getDesc());
        }
        return result;
    }

    private static boolean isSupportedArchive(String path) {
        if (StringUtil.isNull(path)) {
            return false;
        }
        String name = path.toLowerCase();
        if (!(name.endsWith(".jar") || name.endsWith(".war"))) {
            return false;
        }
        return !name.endsWith("-sources.jar")
                && !name.endsWith("-javadoc.jar")
                && !name.endsWith("-tests.jar")
                && !name.endsWith("-test.jar");
    }

    private static Map<String, CVEData> safeCveMap(Map<String, CVEData> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    private static List<SCARule> safeRules(List<SCARule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rules);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public enum RuleKind {
        LOG4J,
        FASTJSON,
        SHIRO
    }

    public record Request(List<String> jarList, EnumSet<RuleKind> enabledRules) {
        public Request {
            jarList = normalizeJarList(jarList);
            enabledRules = enabledRules == null || enabledRules.isEmpty()
                    ? EnumSet.noneOf(RuleKind.class)
                    : EnumSet.copyOf(enabledRules);
        }

        public static Request empty() {
            return new Request(List.of(), EnumSet.noneOf(RuleKind.class));
        }

        public boolean isEnabled(RuleKind kind) {
            return kind != null && enabledRules.contains(kind);
        }
    }

    private static List<String> normalizeJarList(List<String> rawJarList) {
        if (rawJarList == null || rawJarList.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String jar : rawJarList) {
            if (StringUtil.isNull(jar)) {
                continue;
            }
            out.add(jar.trim());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }
}
