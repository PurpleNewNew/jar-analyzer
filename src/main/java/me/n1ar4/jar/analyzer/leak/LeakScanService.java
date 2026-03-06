/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.leak;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class LeakScanService {
    private static final Logger logger = LogManager.getLogger();
    private static final List<RuleSpec> ALL_RULES = List.of(
            new RuleSpec("JWT-TOKEN", JWTRule::match, Set.of("jwt", "jwt-token", "jwt_token")),
            new RuleSpec("ID-CARD", IDCardRule::match, Set.of("id", "id-card", "idcard")),
            new RuleSpec("IP-ADDR", IPAddressRule::match, Set.of("ip", "ip-addr", "ipaddr")),
            new RuleSpec("EMAIL", EmailRule::match, Set.of("email", "mail")),
            new RuleSpec("URL", UrlRule::match, Set.of("url", "link")),
            new RuleSpec("JDBC", JDBCRule::match, Set.of("jdbc")),
            new RuleSpec("FILE-PATH", FilePathRule::match, Set.of("file", "file-path", "filepath", "path")),
            new RuleSpec("MAC-ADDR", MacAddressRule::match, Set.of("mac", "mac-addr", "macaddr")),
            new RuleSpec("PHONE", PhoneRule::match, Set.of("phone", "mobile")),
            new RuleSpec("API-KEY", ApiKeyRule::match, Set.of("api", "api-key", "apikey")),
            new RuleSpec("BANK-CARD", BankCardRule::match, Set.of("bank", "bank-card", "bankcard")),
            new RuleSpec("CLOUD-AKSK", CloudAKSKRule::match, Set.of("aksk", "cloud", "cloud-aksk")),
            new RuleSpec("CRYPTO-KEY", CryptoKeyRule::match, Set.of("crypto", "crypto-key", "private-key")),
            new RuleSpec("AI-KEY", OpenAITokenRule::match, Set.of("ai", "ai-key", "openai", "openai-key")),
            new RuleSpec("PASSWORD", PasswordRule::match, Set.of("password", "pass"))
    );
    private static final Listener NOOP_LISTENER = new Listener() {
    };

    public List<LeakResult> scan(CoreEngine engine, Request request) {
        return scan(engine, request, NOOP_LISTENER);
    }

    public List<LeakResult> scan(CoreEngine engine, Request request, Listener listener) {
        if (engine == null) {
            return List.of();
        }
        Request safeRequest = request == null ? Request.empty() : request;
        Listener safeListener = listener == null ? NOOP_LISTENER : listener;
        List<RuleSpec> selectedRules = resolveRules(safeRequest.types());
        if (selectedRules.isEmpty()) {
            return List.of();
        }
        return LeakContext.withDetectBase64(safeRequest.detectBase64(), () -> {
            List<MemberEntity> members = engine.getAllMembersInfo();
            Map<String, String> stringMap = engine.getStringMap();
            Set<LeakResult> results = new LinkedHashSet<>();
            Map<String, String> jarNameCache = new HashMap<>();
            Map<Integer, String> jarIdNameCache = new HashMap<>();
            for (RuleSpec rule : selectedRules) {
                if (reachLimit(results, safeRequest.limit())) {
                    break;
                }
                int before = results.size();
                safeListener.onRuleStart(rule.typeName());
                processRule(rule, members, stringMap, results, safeRequest, engine, jarNameCache, jarIdNameCache);
                safeListener.onRuleFinish(rule.typeName(), results.size() - before, results.size());
            }
            return new ArrayList<>(results);
        });
    }

    public List<RuleSpec> allRules() {
        return ALL_RULES;
    }

    private static void processRule(RuleSpec rule,
                                    List<MemberEntity> members,
                                    Map<String, String> stringMap,
                                    Set<LeakResult> results,
                                    Request request,
                                    CoreEngine engine,
                                    Map<String, String> jarNameCache,
                                    Map<Integer, String> jarIdNameCache) {
        Integer limit = request.limit();
        if (members != null) {
            for (MemberEntity member : members) {
                if (reachLimit(results, limit)) {
                    return;
                }
                if (member == null) {
                    continue;
                }
                String className = member.getClassName();
                Integer jarId = member.getJarId();
                String jarName = resolveJarName(engine, className, jarId, jarNameCache, jarIdNameCache);
                if (!isAllowed(className, jarId, jarName, request)) {
                    continue;
                }
                List<String> data = rule.ruleFunction().apply(member.getValue());
                if (data == null || data.isEmpty()) {
                    continue;
                }
                for (String item : data) {
                    if (reachLimit(results, limit)) {
                        return;
                    }
                    LeakResult leak = new LeakResult();
                    leak.setClassName(className);
                    leak.setValue(safe(item));
                    leak.setTypeName(rule.typeName());
                    leak.setJarId(jarId);
                    leak.setJarName(jarName);
                    results.add(leak);
                }
            }
        }

        Path tempDir = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        try {
            List<String> allFiles = DirUtil.getFiles(tempDir.toString());
            for (String filePath : allFiles) {
                if (reachLimit(results, limit)) {
                    return;
                }
                Path file = Paths.get(filePath);
                String fileName = file.getFileName() == null ? "" : file.getFileName().toString();
                if (!JarUtil.isConfigFile(fileName)) {
                    continue;
                }
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    List<String> data = rule.ruleFunction().apply(content);
                    if (data == null || data.isEmpty()) {
                        continue;
                    }
                    String relativePath = tempDir.relativize(file).toString().replace("\\", "/");
                    Integer jarId = JarUtil.parseJarIdFromResourcePath(relativePath);
                    String jarName = resolveJarName(engine, null, jarId, jarNameCache, jarIdNameCache);
                    if (!isAllowed(relativePath, jarId, jarName, request)) {
                        continue;
                    }
                    for (String item : data) {
                        if (reachLimit(results, limit)) {
                            return;
                        }
                        LeakResult leak = new LeakResult();
                        leak.setClassName(relativePath);
                        leak.setValue(safe(item));
                        leak.setTypeName(rule.typeName());
                        leak.setJarId(jarId);
                        leak.setJarName(jarName);
                        results.add(leak);
                    }
                } catch (Exception ex) {
                    logger.debug("leak scan file failed: {}: {}", file, ex.toString());
                }
            }
        } catch (Exception ex) {
            logger.debug("leak scan walk failed: {}", ex.toString());
        }

        if (stringMap == null || stringMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            if (reachLimit(results, limit)) {
                return;
            }
            String className = entry.getKey();
            Integer jarId = null;
            String jarName = resolveJarName(engine, className, jarId, jarNameCache, jarIdNameCache);
            if (!isAllowed(className, jarId, jarName, request)) {
                continue;
            }
            List<String> data = rule.ruleFunction().apply(entry.getValue());
            if (data == null || data.isEmpty()) {
                continue;
            }
            for (String item : data) {
                if (reachLimit(results, limit)) {
                    return;
                }
                LeakResult leak = new LeakResult();
                leak.setClassName(className);
                leak.setValue(safe(item));
                leak.setTypeName(rule.typeName());
                leak.setJarId(jarId);
                leak.setJarName(jarName);
                results.add(leak);
            }
        }
    }

    private static List<RuleSpec> resolveRules(Set<String> types) {
        if (types == null || types.isEmpty()) {
            return ALL_RULES;
        }
        List<RuleSpec> selected = new ArrayList<>();
        for (RuleSpec rule : ALL_RULES) {
            if (rule.matchesAny(types)) {
                selected.add(rule);
            }
        }
        return selected;
    }

    private static boolean reachLimit(Set<LeakResult> results, Integer limit) {
        return limit != null && limit > 0 && results.size() >= limit;
    }

    private static boolean isAllowed(String className,
                                     Integer jarId,
                                     String jarName,
                                     Request request) {
        if (CommonFilterUtil.isFilteredClass(className) || CommonFilterUtil.isFilteredJar(jarName)) {
            return false;
        }
        if (!request.whitelist().isEmpty() && !matchesPrefix(className, request.whitelist())) {
            return false;
        }
        if (matchesPrefix(className, request.blacklist())) {
            return false;
        }
        if (!request.jarIds().isEmpty()) {
            if (jarId == null || !request.jarIds().contains(jarId)) {
                return false;
            }
        }
        if (!request.jarNames().isEmpty()) {
            if (StringUtil.isNull(jarName) || !matchesJarName(jarName, request.jarNames())) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesPrefix(String className, List<String> values) {
        if (StringUtil.isNull(className) || values == null || values.isEmpty()) {
            return false;
        }
        String normalizedClass = normalizeClassName(className);
        for (String value : values) {
            if (StringUtil.isNull(value)) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String normalizedValue = normalizeClassName(trimmed);
            if (className.equals(trimmed)
                    || className.startsWith(trimmed)
                    || normalizedClass.equals(normalizedValue)
                    || normalizedClass.startsWith(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesJarName(String jarName, Set<String> jarNames) {
        if (StringUtil.isNull(jarName) || jarNames == null || jarNames.isEmpty()) {
            return false;
        }
        String value = jarName.toLowerCase(Locale.ROOT);
        for (String pattern : jarNames) {
            if (!StringUtil.isNull(pattern) && value.contains(pattern)) {
                return true;
            }
        }
        return false;
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
        if (engine == null) {
            return null;
        }
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public interface Listener {
        default void onRuleStart(String typeName) {
        }

        default void onRuleFinish(String typeName, int addedCount, int totalCount) {
        }
    }

    public record Request(Set<String> types,
                          boolean detectBase64,
                          Integer limit,
                          List<String> whitelist,
                          List<String> blacklist,
                          Set<String> jarNames,
                          Set<Integer> jarIds) {
        public Request {
            types = normalizeTypes(types);
            whitelist = normalizeStrings(whitelist);
            blacklist = normalizeStrings(blacklist);
            jarNames = normalizeTypes(jarNames);
            jarIds = jarIds == null ? Set.of() : Set.copyOf(jarIds);
            if (limit != null && limit <= 0) {
                limit = null;
            }
        }

        public static Request empty() {
            return new Request(Set.of(), false, null, List.of(), List.of(), Set.of(), Set.of());
        }
    }

    public record RuleSpec(String typeName,
                           Function<String, List<String>> ruleFunction,
                           Set<String> aliases) {
        public RuleSpec {
            aliases = normalizeTypes(aliases);
            if (StringUtil.isNull(typeName)) {
                throw new IllegalArgumentException("typeName");
            }
            if (ruleFunction == null) {
                throw new IllegalArgumentException("ruleFunction");
            }
        }

        private boolean matchesAny(Set<String> wanted) {
            if (wanted == null || wanted.isEmpty()) {
                return true;
            }
            if (wanted.contains(typeName.toLowerCase(Locale.ROOT))) {
                return true;
            }
            for (String alias : aliases) {
                if (wanted.contains(alias)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static Set<String> normalizeTypes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = safe(value).toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    private static List<String> normalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            String trimmed = safe(value);
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }
}
