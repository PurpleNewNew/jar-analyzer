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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.leak.*;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

class LeakApiUtil {
    static class LeakRequest {
        private final Set<String> types;
        private final Boolean detectBase64;
        private final Integer limit;

        LeakRequest(Set<String> types, Boolean detectBase64, Integer limit) {
            this.types = types;
            this.detectBase64 = detectBase64;
            this.limit = limit;
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
        return new ParseResult(new LeakRequest(types, base64, limit), null);
    }

    static List<LeakResult> scan(LeakRequest req, CoreEngine engine) {
        List<RuleConfig> rules = resolveRules(req.types);
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }

        JCheckBox base64Box = MainForm.getInstance().getLeakDetBase64Box();
        boolean originBase64 = base64Box.isSelected();
        if (req.detectBase64 != null) {
            base64Box.setSelected(req.detectBase64);
        }

        try {
            List<MemberEntity> members = engine.getAllMembersInfo();
            Map<String, String> stringMap = engine.getStringMap();
            Set<LeakResult> results = new LinkedHashSet<>();

            for (RuleConfig config : rules) {
                if (reachLimit(results, req.limit)) {
                    break;
                }
                processRule(config, members, stringMap, results, req.limit);
            }

            return new ArrayList<>(results);
        } finally {
            base64Box.setSelected(originBase64);
        }
    }

    private static void processRule(RuleConfig config,
                                    List<MemberEntity> members,
                                    Map<String, String> stringMap,
                                    Set<LeakResult> results,
                                    Integer limit) {
        for (MemberEntity member : members) {
            if (reachLimit(results, limit)) {
                return;
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
                leakResult.setClassName(member.getClassName());
                leakResult.setValue(s.trim());
                leakResult.setTypeName(config.typeName);
                results.add(leakResult);
            }
        }

        Path tempDir = Paths.get(Const.tempDir).toAbsolutePath();
        try {
            List<String> allFiles = DirUtil.GetFiles(tempDir.toString());
            for (String filePath : allFiles) {
                if (reachLimit(results, limit)) {
                    return;
                }
                Path file = Paths.get(filePath);
                String fileName = file.getFileName().toString();
                if (!JarUtil.isConfigFile(fileName)) {
                    continue;
                }
                try {
                    byte[] fileBytes = Files.readAllBytes(file);
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
                        String relativePath = tempDir.relativize(file).toString().replace("\\", "/");
                        leakResult.setClassName(relativePath);
                        leakResult.setValue(s.trim());
                        leakResult.setTypeName(config.typeName);
                        results.add(leakResult);
                    }
                } catch (Exception ignored) {
                    // ignore single file error
                }
            }
        } catch (Exception ignored) {
            // ignore walk error
        }

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            if (reachLimit(results, limit)) {
                return;
            }
            String className = entry.getKey();
            String value = entry.getValue();
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

    private static Integer getInt(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
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
}

