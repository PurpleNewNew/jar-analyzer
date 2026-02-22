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
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.leak.*;
import me.n1ar4.jar.analyzer.utils.ArchiveContentResolver;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

class LeakApiUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final int CONFIG_SCAN_MAX_BYTES = 1024 * 1024;

    static class LeakRequest {
        private final Set<String> types;
        private final Boolean detectBase64;
        private final Integer limit;
        private final List<String> whitelist;
        private final List<String> blacklist;
        private final Set<String> jarNames;
        private final Set<Integer> jarIds;
        LeakRequest(Set<String> types,
                    Boolean detectBase64,
                    Integer limit,
                    List<String> whitelist,
                    List<String> blacklist,
                    Set<String> jarNames,
                    Set<Integer> jarIds) {
            this.types = types;
            this.detectBase64 = detectBase64;
            this.limit = limit;
            this.whitelist = whitelist;
            this.blacklist = blacklist;
            this.jarNames = jarNames;
            this.jarIds = jarIds;
        }
    }

    static class ParseResult {
        private final LeakRequest request;
        private final NanoHTTPD.Response error;

        ParseResult(LeakRequest request, NanoHTTPD.Response error) {
            this.request = request;
            this.error = error;
        }

        LeakRequest getRequest() {
            return request;
        }

        NanoHTTPD.Response getError() {
            return error;
        }
    }

    private static class RuleConfig {
        private final String typeName;
        private final Set<String> aliases;
        private final Function<String, List<String>> ruleFunction;

        RuleConfig(String typeName, Function<String, List<String>> ruleFunction, String... alias) {
            this.typeName = typeName;
            this.ruleFunction = ruleFunction;
            this.aliases = new HashSet<>();
            for (String a : alias) {
                if (a != null && !a.trim().isEmpty()) {
                    this.aliases.add(a.trim().toLowerCase());
                }
            }
            this.aliases.add(typeName.toLowerCase());
        }
    }

    static ParseResult parse(NanoHTTPD.IHTTPSession session) {
        Set<String> types = parseTypes(session);
        Boolean base64 = getOptionalBool(session, "base64");
        Integer limit = getInt(session, "limit");
        if (limit != null && limit <= 0) {
            limit = null;
        }
        List<String> whitelist = parseList(resolveParam(session, "whitelist", "allowlist", "include", "pkg"));
        List<String> blacklist = parseList(resolveParam(session, "blacklist", "exclude"));
        Set<String> jarNames = parseNameList(resolveParam(session, "jar", "jarName"));
        Set<Integer> jarIds = parseIntSet(getParam(session, "jarId"));
        return new ParseResult(new LeakRequest(types, base64, limit,
                whitelist, blacklist, jarNames, jarIds), null);
    }

    static List<LeakResult> scan(LeakRequest req, CoreEngine engine) {
        List<RuleConfig> rules = resolveRules(req.types);
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        boolean detectBase64 = Boolean.TRUE.equals(req.detectBase64);
        return LeakContext.withDetectBase64(detectBase64, () -> {
            List<MemberEntity> members = engine.getAllMembersInfo();
            Map<String, String> stringMap = engine.getStringMap();
            Set<LeakResult> results = new LinkedHashSet<>();
            Map<String, String> jarNameCache = new HashMap<>();
            Map<Integer, String> jarIdNameCache = new HashMap<>();

            for (RuleConfig config : rules) {
                if (reachLimit(results, req.limit)) {
                    break;
                }
                processRule(config, members, stringMap, results, req, engine,
                        jarNameCache, jarIdNameCache);
            }

            return new ArrayList<>(results);
        });
    }

    private static void processRule(RuleConfig config,
                                    List<MemberEntity> members,
                                    Map<String, String> stringMap,
                                    Set<LeakResult> results,
                                    LeakRequest req,
                                    CoreEngine engine,
                                    Map<String, String> jarNameCache,
                                    Map<Integer, String> jarIdNameCache) {
        Integer limit = req.limit;
        for (MemberEntity member : members) {
            if (reachLimit(results, limit)) {
                return;
            }
            String className = member.getClassName();
            Integer jarId = member.getJarId();
            String jarName = resolveJarName(engine, className, jarId, jarNameCache, jarIdNameCache);
            if (!isAllowed(className, jarId, jarName, req)) {
                continue;
            }
            List<String> data = config.ruleFunction.apply(member.getValue());
            if (data.isEmpty()) {
                continue;
            }
            for (String s : data) {
                if (reachLimit(results, limit)) {
                    return;
                }
                LeakResult leakResult = new LeakResult();
                leakResult.setClassName(className);
                leakResult.setValue(s.trim());
                leakResult.setTypeName(config.typeName);
                leakResult.setJarId(jarId);
                leakResult.setJarName(jarName);
                results.add(leakResult);
            }
        }

        try {
            ArrayList<ResourceEntity> resources = engine.getTextResources(null);
            if (resources == null) {
                resources = new ArrayList<>();
            }
            for (ResourceEntity resource : resources) {
                if (reachLimit(results, limit)) {
                    return;
                }
                if (resource == null) {
                    continue;
                }
                String resourcePath = resource.getResourcePath();
                if (StringUtil.isNull(resourcePath) || !JarUtil.isConfigFile(resourcePath)) {
                    continue;
                }
                Integer jarId = resource.getJarId();
                String jarName = resource.getJarName();
                if (StringUtil.isNull(jarName)) {
                    jarName = resolveJarName(engine, null, jarId, jarNameCache, jarIdNameCache);
                }
                if (!isAllowed(resourcePath, jarId, jarName, req)) {
                    continue;
                }
                byte[] fileBytes = ArchiveContentResolver.readResourceBytes(resource, 0, CONFIG_SCAN_MAX_BYTES);
                if (fileBytes == null || fileBytes.length == 0) {
                    continue;
                }
                String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
                List<String> data = config.ruleFunction.apply(fileContent);
                if (data.isEmpty()) {
                    continue;
                }
                for (String s : data) {
                    if (reachLimit(results, limit)) {
                        return;
                    }
                    LeakResult leakResult = new LeakResult();
                    leakResult.setClassName(resourcePath);
                    leakResult.setValue(s.trim());
                    leakResult.setTypeName(config.typeName);
                    leakResult.setJarId(jarId);
                    leakResult.setJarName(jarName);
                    results.add(leakResult);
                }
            }
        } catch (Exception ex) {
            logger.debug("leak scan config resources failed: {}", ex.toString());
        }

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            if (reachLimit(results, limit)) {
                return;
            }
            String className = entry.getKey();
            String value = entry.getValue();
            Integer jarId = null;
            String jarName = resolveJarName(engine, className, jarId, jarNameCache, jarIdNameCache);
            if (!isAllowed(className, jarId, jarName, req)) {
                continue;
            }
            List<String> data = config.ruleFunction.apply(value);
            if (data.isEmpty()) {
                continue;
            }
            for (String s : data) {
                if (reachLimit(results, limit)) {
                    return;
                }
                LeakResult leakResult = new LeakResult();
                leakResult.setClassName(className);
                leakResult.setValue(s.trim());
                leakResult.setTypeName(config.typeName);
                leakResult.setJarId(jarId);
                leakResult.setJarName(jarName);
                results.add(leakResult);
            }
        }
    }

    private static List<RuleConfig> resolveRules(Set<String> types) {
        List<RuleConfig> all = buildAllRules();
        if (types.isEmpty()) {
            return all;
        }
        List<RuleConfig> selected = new ArrayList<>();
        for (RuleConfig rule : all) {
            for (String alias : rule.aliases) {
                if (types.contains(alias)) {
                    selected.add(rule);
                    break;
                }
            }
        }
        return selected;
    }

    private static List<RuleConfig> buildAllRules() {
        List<RuleConfig> rules = new ArrayList<>();
        rules.add(new RuleConfig("JWT-TOKEN", JWTRule::match, "jwt", "jwt-token", "jwt_token"));
        rules.add(new RuleConfig("ID-CARD", IDCardRule::match, "id", "id-card", "idcard"));
        rules.add(new RuleConfig("IP-ADDR", IPAddressRule::match, "ip", "ip-addr", "ipaddr"));
        rules.add(new RuleConfig("EMAIL", EmailRule::match, "email", "mail"));
        rules.add(new RuleConfig("URL", UrlRule::match, "url", "link"));
        rules.add(new RuleConfig("JDBC", JDBCRule::match, "jdbc"));
        rules.add(new RuleConfig("FILE-PATH", FilePathRule::match, "file", "file-path", "filepath", "path"));
        rules.add(new RuleConfig("MAC-ADDR", MacAddressRule::match, "mac", "mac-addr", "macaddr"));
        rules.add(new RuleConfig("PHONE", PhoneRule::match, "phone", "mobile"));
        rules.add(new RuleConfig("API-KEY", ApiKeyRule::match, "api", "api-key", "apikey"));
        rules.add(new RuleConfig("BANK-CARD", BankCardRule::match, "bank", "bank-card", "bankcard"));
        rules.add(new RuleConfig("CLOUD-AKSK", CloudAKSKRule::match, "aksk", "cloud", "cloud-aksk"));
        rules.add(new RuleConfig("CRYPTO-KEY", CryptoKeyRule::match, "crypto", "crypto-key", "private-key"));
        rules.add(new RuleConfig("AI-KEY", OpenAITokenRule::match, "ai", "ai-key", "openai", "openai-key"));
        rules.add(new RuleConfig("PASSWORD", PasswordRule::match, "password", "pass"));
        return rules;
    }

    private static Set<String> parseTypes(NanoHTTPD.IHTTPSession session) {
        Set<String> types = new HashSet<>();
        types.addAll(splitParam(getParam(session, "types")));
        types.addAll(splitParam(getParam(session, "type")));
        return types;
    }

    private static String resolveParam(NanoHTTPD.IHTTPSession session, String... keys) {
        for (String key : keys) {
            if (StringUtil.isNull(key)) {
                continue;
            }
            String value = getParam(session, key);
            if (!StringUtil.isNull(value)) {
                return value;
            }
        }
        return "";
    }

    private static boolean reachLimit(Set<LeakResult> results, Integer limit) {
        return limit != null && limit > 0 && results.size() >= limit;
    }

    private static String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private static Set<String> splitParam(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<String> result = new HashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            result.add(part.trim().toLowerCase());
        }
        return result;
    }

    private static List<String> parseList(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static Set<String> parseNameList(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed.toLowerCase());
            }
        }
        return result;
    }

    private static Set<Integer> parseIntSet(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<Integer> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ex) {
                logger.debug("ignore invalid integer in list: {}", trimmed);
            }
        }
        return result;
    }

    private static Integer getInt(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param {}={}", key, value);
            return null;
        }
    }

    private static Boolean getOptionalBool(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private static boolean isAllowed(String className,
                                     Integer jarId,
                                     String jarName,
                                     LeakRequest req) {
        if (isJdkClass(className) || CommonFilterUtil.isFilteredJar(jarName)) {
            return false;
        }
        if (!req.whitelist.isEmpty() && !isWhitelisted(className, req.whitelist)) {
            return false;
        }
        if (isBlacklisted(className, req.blacklist)) {
            return false;
        }
        if (!req.jarIds.isEmpty()) {
            if (jarId == null || !req.jarIds.contains(jarId)) {
                return false;
            }
        }
        if (!req.jarNames.isEmpty()) {
            if (StringUtil.isNull(jarName) || !matchesJarName(jarName, req.jarNames)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWhitelisted(String className, List<String> whitelist) {
        if (StringUtil.isNull(className) || whitelist == null || whitelist.isEmpty()) {
            return whitelist == null || whitelist.isEmpty();
        }
        String classNorm = normalizeClassName(className);
        for (String w : whitelist) {
            if (StringUtil.isNull(w)) {
                continue;
            }
            String wTrim = w.trim();
            if (wTrim.isEmpty()) {
                continue;
            }
            String wNorm = normalizeClassName(wTrim);
            if (className.equals(wTrim) || className.startsWith(wTrim)
                    || classNorm.equals(wNorm) || classNorm.startsWith(wNorm)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlacklisted(String className, List<String> blacklist) {
        if (StringUtil.isNull(className) || blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        String classNorm = normalizeClassName(className);
        for (String b : blacklist) {
            if (StringUtil.isNull(b)) {
                continue;
            }
            String bTrim = b.trim();
            if (bTrim.isEmpty()) {
                continue;
            }
            String bNorm = normalizeClassName(bTrim);
            if (className.equals(bTrim) || className.startsWith(bTrim)
                    || classNorm.equals(bNorm) || classNorm.startsWith(bNorm)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesJarName(String jarName, Set<String> jarNames) {
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

    private static boolean isJdkClass(String className) {
        return CommonFilterUtil.isFilteredClass(className);
    }

    private static String normalizeClassName(String value) {
        if (StringUtil.isNull(value)) {
            return "";
        }
        return value.replace('.', '/');
    }


    private static String resolveJarName(CoreEngine engine,
                                         String className,
                                         Integer jarId,
                                         Map<String, String> jarNameCache,
                                         Map<Integer, String> jarIdNameCache) {
        if (jarId != null) {
            String cached = jarIdNameCache.get(jarId);
            if (cached != null) {
                return cached;
            }
            String name = engine.getJarNameById(jarId);
            if (!StringUtil.isNull(name)) {
                jarIdNameCache.put(jarId, name);
                return name;
            }
        }
        if (!StringUtil.isNull(className)) {
            String cached = jarNameCache.get(className);
            if (cached != null) {
                return cached;
            }
            String name = engine.getJarByClass(className);
            if (!StringUtil.isNull(name)) {
                jarNameCache.put(className, name);
                return name;
            }
        }
        return null;
    }
}
