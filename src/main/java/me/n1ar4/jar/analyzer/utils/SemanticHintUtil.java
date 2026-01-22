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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class SemanticHintUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_PATH = "rules/semantic-hints.json";
    private static volatile SemanticConfig config;

    private SemanticHintUtil() {
    }

    public static List<SemanticCategory> getCategories() {
        return getConfig().categories;
    }

    public static List<String> resolveTags(List<String> annoNames) {
        if (annoNames == null || annoNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tags = new ArrayList<>();
        for (SemanticCategory category : getConfig().categories) {
            if (category == null || category.annotations == null || category.annotations.isEmpty()) {
                continue;
            }
            if (matchAnnotations(annoNames, category.annotations)) {
                tags.add(category.name);
            }
        }
        return tags;
    }

    public static boolean matchAnnotations(List<String> annos, List<String> patterns) {
        if (annos == null || patterns == null) {
            return false;
        }
        for (String anno : annos) {
            String norm = normalizeAnno(anno);
            for (String pat : patterns) {
                String p = normalizeAnno(pat);
                if (!p.isEmpty() && norm.contains(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String normalizeAnno(String anno) {
        if (anno == null) {
            return "";
        }
        String v = anno.trim();
        if (v.startsWith("L") && v.endsWith(";")) {
            v = v.substring(1, v.length() - 1);
        }
        v = v.replace('/', '.');
        return v.toLowerCase();
    }

    private static SemanticConfig getConfig() {
        if (config != null) {
            return config;
        }
        synchronized (SemanticHintUtil.class) {
            if (config == null) {
                config = loadConfig();
            }
        }
        return config;
    }

    private static SemanticConfig loadConfig() {
        SemanticConfig def = defaultConfig();
        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.warn("rules/semantic-hints.json not found");
            return def;
        }
        try (InputStream is = Files.newInputStream(path)) {
            String text = new String(IOUtils.readAllBytes(is), StandardCharsets.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            List<SemanticCategory> categories = obj.getList("categories", SemanticCategory.class);
            if (categories != null && !categories.isEmpty()) {
                SemanticConfig cfg = new SemanticConfig();
                cfg.categories = categories;
                return cfg;
            }
        } catch (Exception ex) {
            logger.warn("load rules/semantic-hints.json failed: {}", ex.getMessage());
        }
        return def;
    }

    private static SemanticConfig defaultConfig() {
        SemanticConfig cfg = new SemanticConfig();
        List<SemanticCategory> categories = new ArrayList<>();
        categories.add(category("authz",
                "Authorization checks and access control",
                Arrays.asList("PreAuthorize", "PostAuthorize", "Secured", "RolesAllowed",
                        "RequiresPermissions", "RequiresRoles", "PermitAll", "DenyAll"),
                Arrays.asList("authorize", "permission", "role", "access")));
        categories.add(category("authn",
                "Authentication / login / session",
                Arrays.asList("AuthenticationPrincipal", "Authenticated", "PreAuthenticated",
                        "RequiresAuthentication", "Login"),
                Arrays.asList("login", "authenticate", "token", "session")));
        categories.add(category("validation",
                "Input validation / constraints",
                Arrays.asList("Valid", "Validated", "NotNull", "NotBlank", "NotEmpty",
                        "Size", "Pattern", "Email", "Min", "Max"),
                Arrays.asList("validate", "sanitize")));
        categories.add(category("config_binding",
                "Configuration binding",
                Arrays.asList("ConfigurationProperties", "Value", "PropertySource",
                        "Configuration", "EnableConfigurationProperties"),
                Arrays.asList("config", "properties")));
        categories.add(category("crypto",
                "Crypto / signing / token usage",
                Collections.emptyList(),
                Arrays.asList("jwt", "token", "sign", "verify", "hmac", "encrypt", "decrypt")));
        cfg.categories = categories;
        return cfg;
    }

    private static SemanticCategory category(String name,
                                             String desc,
                                             List<String> annos,
                                             List<String> keywords) {
        SemanticCategory c = new SemanticCategory();
        c.name = name;
        c.description = desc;
        c.annotations = annos == null ? Collections.emptyList() : annos;
        c.stringKeywords = keywords == null ? Collections.emptyList() : keywords;
        return c;
    }

    public static class SemanticConfig {
        private List<SemanticCategory> categories = Collections.emptyList();
    }

    public static class SemanticCategory {
        public String name;
        public String description;
        public List<String> annotations;
        public List<String> stringKeywords;
    }
}
