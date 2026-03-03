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

import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
import me.n1ar4.jar.analyzer.starter.Const;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommonFilterUtil {
    private CommonFilterUtil() {
    }

    public static List<String> getClassPrefixes() {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(AnalysisScopeRules.getJdkClassPrefixes());
        merged.addAll(AnalysisScopeRules.getCommonLibraryClassPrefixes());
        return new ArrayList<>(merged);
    }

    public static List<String> getJarPrefixes() {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(AnalysisScopeRules.getSdkJarPrefixes());
        merged.addAll(AnalysisScopeRules.getCommonLibraryJarPrefixes());
        return new ArrayList<>(merged);
    }

    public static List<String> getResourceAllowPrefixes() {
        return AnalysisScopeRules.getResourceAllowPrefixes();
    }

    public static boolean isFilteredClass(String className) {
        if (StringUtil.isNull(className)) {
            return false;
        }
        if (isModuleInfoClass(className)) {
            return true;
        }
        if (AnalysisScopeRules.isForceTargetClass(className)) {
            return false;
        }
        if (AnalysisScopeRules.isJdkClass(className)) {
            return true;
        }
        return AnalysisScopeRules.isCommonLibraryClass(className);
    }

    public static boolean isModuleInfoClassName(String className) {
        return isModuleInfoClass(className);
    }

    public static boolean isFilteredResourcePath(String resourcePath) {
        if (StringUtil.isNull(resourcePath)) {
            return false;
        }
        String normalized = normalizeResourcePath(resourcePath);
        if (normalized.isEmpty()) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        String path = stripJarRootPrefix(lower);
        if (isHardBlacklistedResource(path)) {
            return true;
        }
        return !isResourceAllowed(path);
    }

    public static synchronized void saveClassPrefixes(List<String> classPrefixes) {
        AnalysisScopeRules.saveCommonLibraryClassPrefixes(classPrefixes);
    }

    public static synchronized void saveJarPrefixes(List<String> jarPrefixes) {
        AnalysisScopeRules.saveCommonLibraryJarPrefixes(jarPrefixes);
    }

    public static synchronized void reload() {
        AnalysisScopeRules.reload();
    }

    public static boolean isFilteredJar(String jarName) {
        if (StringUtil.isNull(jarName)) {
            return false;
        }
        if (AnalysisScopeRules.isForceTargetJar(jarName)) {
            return false;
        }
        if (AnalysisScopeRules.isSdkJar(jarName)) {
            return true;
        }
        return AnalysisScopeRules.isCommonLibraryJar(jarName);
    }

    public static String buildClassPrefixText() {
        return AnalysisScopeRules.buildPrefixText(
                AnalysisScopeRules.getCommonLibraryClassPrefixes(),
                "common library class prefixes (rules/analysis-scope.json)"
        );
    }

    public static String buildJarPrefixText() {
        return AnalysisScopeRules.buildPrefixText(
                AnalysisScopeRules.getCommonLibraryJarPrefixes(),
                "common library jar prefixes (rules/analysis-scope.json)"
        );
    }

    private static boolean isModuleInfoClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.trim();
        if (norm.isEmpty()) {
            return false;
        }
        if (norm.endsWith(".class")) {
            norm = norm.substring(0, norm.length() - ".class".length());
        }
        norm = norm.replace('.', '/');
        if (norm.startsWith("/")) {
            norm = norm.substring(1);
        }
        return "module-info".equalsIgnoreCase(norm);
    }

    private static boolean isModuleInfoResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        String name = base.toLowerCase(Locale.ROOT);
        if ("module-info.class".equals(name)) {
            return true;
        }
        return "module-info.java".equals(name);
    }

    private static boolean isHardBlacklistedResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (isModuleInfoResource(path)) {
            return true;
        }
        if (path.endsWith(".kotlin_module")) {
            return true;
        }
        if (isHardMetaInfNoise(path)) {
            return true;
        }
        String stripped = stripBootWebClassesPrefix(path);
        if (!stripped.equals(path)) {
            if (isModuleInfoResource(stripped)) {
                return true;
            }
            if (stripped.endsWith(".kotlin_module")) {
                return true;
            }
            return isHardMetaInfNoise(stripped);
        }
        return false;
    }

    private static boolean isHardMetaInfNoise(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.startsWith("meta-inf/maven/")) {
            return true;
        }
        if (path.startsWith("meta-inf/") && endsWithAny(path, ".sf", ".rsa", ".dsa", ".ec")) {
            return true;
        }
        return false;
    }

    private static boolean isResourceAllowed(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String stripped = stripBootWebClassesPrefix(path);
        if (isAllowedByPrefixes(path) || isAllowedByPrefixes(stripped)) {
            return true;
        }
        if (isAllowedByFileName(path) || isAllowedByFileName(stripped)) {
            return true;
        }
        return isRootResource(path) || isRootResource(stripped);
    }

    private static boolean isAllowedByPrefixes(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        for (String prefix : AnalysisScopeRules.getResourceAllowPrefixes()) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedByFileName(String path) {
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        if (base.startsWith("application") && (base.endsWith(".yml") || base.endsWith(".yaml"))) {
            return true;
        }
        if (base.endsWith(".properties")) {
            return true;
        }
        return base.endsWith(".xml");
    }

    private static boolean isRootResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.indexOf('/') < 0;
    }

    private static String stripBootWebClassesPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String bootPrefix = "boot-inf/classes/";
        if (path.startsWith(bootPrefix)) {
            return path.substring(bootPrefix.length());
        }
        String webPrefix = "web-inf/classes/";
        if (path.startsWith(webPrefix)) {
            return path.substring(webPrefix.length());
        }
        return path;
    }

    private static String stripJarRootPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        int slash = path.indexOf('/');
        if (slash <= 0) {
            return path;
        }
        String head = path.substring(0, slash);
        if (isJarRootName(head)) {
            return path.substring(slash + 1);
        }
        return path;
    }

    private static boolean isJarRootName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!name.startsWith("jar-")) {
            return false;
        }
        int lastDash = name.lastIndexOf('-');
        if (lastDash <= 3) {
            return false;
        }
        String idPart = name.substring(4, lastDash);
        if (idPart.isEmpty()) {
            return false;
        }
        for (int i = 0; i < idPart.length(); i++) {
            if (!Character.isDigit(idPart.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean endsWithAny(String value, String... suffixes) {
        if (value == null || value.isEmpty() || suffixes == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String suffix : suffixes) {
            if (suffix == null || suffix.isEmpty()) {
                continue;
            }
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String baseName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < path.length()) {
            return path.substring(idx + 1);
        }
        return path;
    }

    private static String normalizeResourcePath(String path) {
        String normalized = path.replace("\\", "/");
        String tempRoot = Const.tempDir.replace("\\", "/");
        String tempPrefix = tempRoot.endsWith("/") ? tempRoot : tempRoot + "/";
        int idx = normalized.indexOf(tempPrefix);
        if (idx >= 0) {
            normalized = normalized.substring(idx + tempPrefix.length());
            String resourcesPrefix = Const.resourceDir + "/";
            if (normalized.startsWith(resourcesPrefix)) {
                normalized = normalized.substring(resourcesPrefix.length());
                int slash = normalized.indexOf('/');
                if (slash >= 0 && slash + 1 < normalized.length()) {
                    normalized = normalized.substring(slash + 1);
                }
            }
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
