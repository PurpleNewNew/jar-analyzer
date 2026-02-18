/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PtaSolverConfig {
    private static final String PROP_CONTEXT_DEPTH = "jar.analyzer.pta.context.depth";
    private static final String PROP_OBJECT_SENS_DEPTH = "jar.analyzer.pta.object.sensitivity.depth";
    private static final String PROP_FIELD_SENS_DEPTH = "jar.analyzer.pta.field.sensitivity.depth";
    private static final String PROP_ARRAY_SENS_DEPTH = "jar.analyzer.pta.array.sensitivity.depth";
    private static final String PROP_MAX_TARGETS = "jar.analyzer.pta.max.targets.per.call";
    private static final String PROP_MAX_CONTEXT_METHODS = "jar.analyzer.pta.max.context.methods";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";
    private static final String PROP_CONTEXT_CI_PREFIXES = "jar.analyzer.pta.context.ci.prefixes";
    private static final String PROP_CONTEXT_CS_PREFIXES = "jar.analyzer.pta.context.cs.prefixes";
    private static final String PROP_PLUGIN_CLASSES = "jar.analyzer.pta.plugins";
    private static final String PROP_PLUGIN_CONSTRAINT_CHECK = "jar.analyzer.pta.plugin.constraint.check";

    private final int contextDepth;
    private final int objectSensitivityDepth;
    private final int fieldSensitivityDepth;
    private final int arraySensitivityDepth;
    private final int maxTargetsPerCall;
    private final int maxContextMethods;
    private final boolean incremental;
    private final List<String> contextInsensitivePrefixes;
    private final List<String> forcedContextSensitivePrefixes;
    private final List<String> pluginClassNames;
    private final boolean constraintCheckerEnabled;

    private PtaSolverConfig(int contextDepth,
                            int objectSensitivityDepth,
                            int fieldSensitivityDepth,
                            int arraySensitivityDepth,
                            int maxTargetsPerCall,
                            int maxContextMethods,
                            boolean incremental,
                            List<String> contextInsensitivePrefixes,
                            List<String> forcedContextSensitivePrefixes,
                            List<String> pluginClassNames,
                            boolean constraintCheckerEnabled) {
        this.contextDepth = contextDepth;
        this.objectSensitivityDepth = objectSensitivityDepth;
        this.fieldSensitivityDepth = fieldSensitivityDepth;
        this.arraySensitivityDepth = arraySensitivityDepth;
        this.maxTargetsPerCall = maxTargetsPerCall;
        this.maxContextMethods = maxContextMethods;
        this.incremental = incremental;
        this.contextInsensitivePrefixes = contextInsensitivePrefixes == null
                ? Collections.emptyList() : contextInsensitivePrefixes;
        this.forcedContextSensitivePrefixes = forcedContextSensitivePrefixes == null
                ? Collections.emptyList() : forcedContextSensitivePrefixes;
        this.pluginClassNames = pluginClassNames == null
                ? Collections.emptyList() : pluginClassNames;
        this.constraintCheckerEnabled = constraintCheckerEnabled;
    }

    static PtaSolverConfig fromSystemProperties() {
        int depth = readInt(PROP_CONTEXT_DEPTH, 1, 0, 4);
        int objDepth = readInt(PROP_OBJECT_SENS_DEPTH, 1, 0, 3);
        int fieldDepth = readInt(PROP_FIELD_SENS_DEPTH, 1, 0, 2);
        int arrayDepth = readInt(PROP_ARRAY_SENS_DEPTH, 2, 0, 2);
        int maxTargets = readInt(PROP_MAX_TARGETS, 256, 16, 4096);
        int maxContexts = readInt(PROP_MAX_CONTEXT_METHODS, 50000, 512, 200000);
        boolean incremental = readBoolean(PROP_INCREMENTAL, true);
        List<String> ciPrefixes = readList(PROP_CONTEXT_CI_PREFIXES,
                "java/,javax/,jdk/,sun/,com/sun/");
        List<String> csPrefixes = readList(PROP_CONTEXT_CS_PREFIXES, "");
        List<String> pluginClasses = readList(PROP_PLUGIN_CLASSES, "");
        boolean checkPlugin = readBoolean(PROP_PLUGIN_CONSTRAINT_CHECK, false);
        return new PtaSolverConfig(depth, objDepth, fieldDepth, arrayDepth,
                maxTargets, maxContexts, incremental,
                ciPrefixes, csPrefixes, pluginClasses, checkPlugin);
    }

    int getContextDepth() {
        return contextDepth;
    }

    int getObjectSensitivityDepth() {
        return objectSensitivityDepth;
    }

    int getFieldSensitivityDepth() {
        return fieldSensitivityDepth;
    }

    int getArraySensitivityDepth() {
        return arraySensitivityDepth;
    }

    int getMaxTargetsPerCall() {
        return maxTargetsPerCall;
    }

    int getMaxContextMethods() {
        return maxContextMethods;
    }

    boolean isIncremental() {
        return incremental;
    }

    int contextDepthFor(MethodReference.Handle method) {
        if (contextDepth <= 0 || method == null
                || method.getClassReference() == null
                || method.getClassReference().getName() == null) {
            return contextDepth;
        }
        String owner = method.getClassReference().getName();
        if (matchesAny(owner, forcedContextSensitivePrefixes)) {
            return contextDepth;
        }
        if (matchesAny(owner, contextInsensitivePrefixes)) {
            return 0;
        }
        return contextDepth;
    }

    List<String> getPluginClassNames() {
        return pluginClassNames;
    }

    boolean isConstraintCheckerEnabled() {
        return constraintCheckerEnabled;
    }

    private static boolean matchesAny(String owner, List<String> prefixes) {
        if (owner == null || owner.isEmpty() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String raw : prefixes) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            String prefix = raw;
            if (prefix.endsWith("*")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            if (!prefix.isEmpty() && owner.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static int readInt(String key, int def, int min, int max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < min) {
                return min;
            }
            if (val > max) {
                return max;
            }
            return val;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static boolean readBoolean(String key, boolean def) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private static List<String> readList(String key, String def) {
        String raw = System.getProperty(key);
        String source = (raw == null || raw.trim().isEmpty()) ? def : raw;
        if (source == null || source.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = source.split(",");
        ArrayList<String> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            out.add(value);
        }
        if (out.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(out);
    }
}
