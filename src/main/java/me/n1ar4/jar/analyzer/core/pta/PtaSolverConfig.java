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
    private static final String PROP_ONFLY_SEMANTIC = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_ONFLY_REFLECTION = "jar.analyzer.pta.reflection.onfly.enable";
    private static final String PROP_ADAPTIVE_ENABLE = "jar.analyzer.pta.adaptive.enable";
    private static final String PROP_ADAPTIVE_WIDE_RECEIVER = "jar.analyzer.pta.adaptive.wide.receiver.threshold";
    private static final String PROP_ADAPTIVE_MAX_CONTEXT = "jar.analyzer.pta.adaptive.max.context.depth";
    private static final String PROP_ADAPTIVE_MAX_OBJECT = "jar.analyzer.pta.adaptive.max.object.depth";
    private static final String PROP_ADAPTIVE_PRECISION_PREFIXES = "jar.analyzer.pta.adaptive.precision.prefixes";
    private static final String PROP_ADAPTIVE_MAX_CTX_PER_METHOD =
            "jar.analyzer.pta.adaptive.max.contexts.per.method";
    private static final String PROP_ADAPTIVE_MAX_CTX_PER_SITE =
            "jar.analyzer.pta.adaptive.max.contexts.per.site";
    private static final String PROP_ADAPTIVE_HUGE_DISPATCH =
            "jar.analyzer.pta.adaptive.huge.dispatch.threshold";
    private static final String PROP_ADAPTIVE_OBJECT_WIDEN =
            "jar.analyzer.pta.adaptive.object.widen.context.threshold";

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
    private final boolean onTheFlySemanticEnabled;
    private final boolean onTheFlyReflectionEnabled;
    private final boolean adaptiveEnabled;
    private final int adaptiveWideReceiverThreshold;
    private final int adaptiveMaxContextDepth;
    private final int adaptiveMaxObjectDepth;
    private final List<String> adaptivePrecisionPrefixes;
    private final int adaptiveMaxContextsPerMethod;
    private final int adaptiveMaxContextsPerSite;
    private final int adaptiveHugeDispatchThreshold;
    private final int adaptiveObjectWidenContextThreshold;

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
                            boolean constraintCheckerEnabled,
                            boolean onTheFlySemanticEnabled,
                            boolean onTheFlyReflectionEnabled,
                            boolean adaptiveEnabled,
                            int adaptiveWideReceiverThreshold,
                            int adaptiveMaxContextDepth,
                            int adaptiveMaxObjectDepth,
                            List<String> adaptivePrecisionPrefixes,
                            int adaptiveMaxContextsPerMethod,
                            int adaptiveMaxContextsPerSite,
                            int adaptiveHugeDispatchThreshold,
                            int adaptiveObjectWidenContextThreshold) {
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
        this.onTheFlySemanticEnabled = onTheFlySemanticEnabled;
        this.onTheFlyReflectionEnabled = onTheFlyReflectionEnabled;
        this.adaptiveEnabled = adaptiveEnabled;
        this.adaptiveWideReceiverThreshold = adaptiveWideReceiverThreshold;
        this.adaptiveMaxContextDepth = adaptiveMaxContextDepth;
        this.adaptiveMaxObjectDepth = adaptiveMaxObjectDepth;
        this.adaptivePrecisionPrefixes = adaptivePrecisionPrefixes == null
                ? Collections.emptyList() : adaptivePrecisionPrefixes;
        this.adaptiveMaxContextsPerMethod = adaptiveMaxContextsPerMethod;
        this.adaptiveMaxContextsPerSite = adaptiveMaxContextsPerSite;
        this.adaptiveHugeDispatchThreshold = adaptiveHugeDispatchThreshold;
        this.adaptiveObjectWidenContextThreshold = adaptiveObjectWidenContextThreshold;
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
        boolean onTheFlySemantic = readBoolean(PROP_ONFLY_SEMANTIC, true);
        boolean onTheFlyReflection = readBoolean(PROP_ONFLY_REFLECTION, true);
        boolean adaptive = readBoolean(PROP_ADAPTIVE_ENABLE, true);
        int adaptiveWideReceiver = readInt(PROP_ADAPTIVE_WIDE_RECEIVER, 32, 4, 512);
        int adaptiveMaxContext = readInt(PROP_ADAPTIVE_MAX_CONTEXT, 2, 0, 4);
        int adaptiveMaxObject = readInt(PROP_ADAPTIVE_MAX_OBJECT, 2, 0, 4);
        List<String> adaptivePrecisionPrefixes = readList(PROP_ADAPTIVE_PRECISION_PREFIXES,
                "org/springframework/,org/aopalliance/,java/lang/reflect/,java/lang/invoke/");
        int adaptiveMaxContextsPerMethod = readInt(PROP_ADAPTIVE_MAX_CTX_PER_METHOD, 96, 16, 8192);
        int adaptiveMaxContextsPerSite = readInt(PROP_ADAPTIVE_MAX_CTX_PER_SITE, 24, 4, 2048);
        int adaptiveHugeDispatchThreshold = readInt(PROP_ADAPTIVE_HUGE_DISPATCH, 64, 8, 4096);
        int adaptiveObjectWidenThreshold = readInt(PROP_ADAPTIVE_OBJECT_WIDEN, 48, 8, 4096);
        return new PtaSolverConfig(depth, objDepth, fieldDepth, arrayDepth,
                maxTargets, maxContexts, incremental,
                ciPrefixes, csPrefixes, pluginClasses, checkPlugin, onTheFlySemantic,
                onTheFlyReflection, adaptive, adaptiveWideReceiver,
                adaptiveMaxContext, adaptiveMaxObject, adaptivePrecisionPrefixes,
                adaptiveMaxContextsPerMethod, adaptiveMaxContextsPerSite,
                adaptiveHugeDispatchThreshold, adaptiveObjectWidenThreshold);
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

    boolean isOnTheFlySemanticEnabled() {
        return onTheFlySemanticEnabled;
    }

    boolean isOnTheFlyReflectionEnabled() {
        return onTheFlyReflectionEnabled;
    }

    boolean isAdaptiveEnabled() {
        return adaptiveEnabled;
    }

    int contextDepthForDispatch(MethodReference.Handle method,
                                String declaredOwner,
                                int receiverTypeCount) {
        int depth = contextDepthFor(method);
        if (!adaptiveEnabled) {
            return depth;
        }
        String owner = ownerName(method);
        if (isPrecisionSensitive(owner) || isPrecisionSensitive(declaredOwner)) {
            depth = Math.min(adaptiveMaxContextDepth, Math.max(depth, 1) + 1);
        }
        if (receiverTypeCount >= adaptiveHugeDispatchThreshold) {
            if (isPrecisionSensitive(owner) || isPrecisionSensitive(declaredOwner)) {
                depth = Math.min(depth, 1);
            } else {
                depth = 0;
            }
        }
        if (receiverTypeCount >= adaptiveWideReceiverThreshold && matchesAny(owner, contextInsensitivePrefixes)) {
            depth = Math.min(depth, 1);
        }
        if (depth < 0) {
            return 0;
        }
        return Math.min(depth, adaptiveMaxContextDepth);
    }

    int objectSensitivityDepthFor(MethodReference.Handle method) {
        int depth = objectSensitivityDepth;
        if (!adaptiveEnabled) {
            return depth;
        }
        String owner = ownerName(method);
        if (matchesAny(owner, contextInsensitivePrefixes)) {
            depth = Math.min(depth, 1);
        }
        if (isPrecisionSensitive(owner)) {
            depth = Math.min(adaptiveMaxObjectDepth, Math.max(depth, 1) + 1);
        }
        if (depth < 0) {
            return 0;
        }
        return Math.min(depth, adaptiveMaxObjectDepth);
    }

    int objectSensitivityDepthFor(MethodReference.Handle method, int activeContextCount) {
        int depth = objectSensitivityDepthFor(method);
        if (!adaptiveEnabled) {
            return depth;
        }
        if (activeContextCount >= adaptiveObjectWidenContextThreshold) {
            depth = Math.min(depth, 1);
        }
        if (depth < 0) {
            return 0;
        }
        return depth;
    }

    int getAdaptiveWideReceiverThreshold() {
        return adaptiveWideReceiverThreshold;
    }

    int getAdaptiveMaxContextsPerMethod() {
        return adaptiveMaxContextsPerMethod;
    }

    int getAdaptiveMaxContextsPerSite() {
        return adaptiveMaxContextsPerSite;
    }

    int getAdaptiveHugeDispatchThreshold() {
        return adaptiveHugeDispatchThreshold;
    }

    private boolean isPrecisionSensitive(String owner) {
        if (owner == null || owner.isEmpty()) {
            return false;
        }
        return matchesAny(owner, forcedContextSensitivePrefixes)
                || matchesAny(owner, adaptivePrecisionPrefixes);
    }

    private static String ownerName(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null) {
            return null;
        }
        return method.getClassReference().getName();
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
