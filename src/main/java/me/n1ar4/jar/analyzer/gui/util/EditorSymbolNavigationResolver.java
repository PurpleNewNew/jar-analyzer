/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationTargetDto;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EditorSymbolNavigationResolver {
    private static final int MAX_NAV_TARGETS = 256;
    private static final int MAX_CLASS_SCAN = 128;

    private EditorSymbolNavigationResolver() {
    }

    public static EditorDeclarationResultDto resolveUsages(CoreEngine engine,
                                                           me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto doc,
                                                           int caretOffset) {
        return resolve(engine, doc, caretOffset, Action.USAGES);
    }

    public static EditorDeclarationResultDto resolveImplementations(CoreEngine engine,
                                                                    me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto doc,
                                                                    int caretOffset) {
        return resolve(engine, doc, caretOffset, Action.IMPLEMENTATIONS);
    }

    public static EditorDeclarationResultDto resolveHierarchy(CoreEngine engine,
                                                              me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto doc,
                                                              int caretOffset) {
        return resolve(engine, doc, caretOffset, Action.HIERARCHY);
    }

    private static EditorDeclarationResultDto resolve(CoreEngine engine,
                                                      me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto doc,
                                                      int caretOffset,
                                                      Action action) {
        if (engine == null || !engine.isEnabled()) {
            return EditorDeclarationResultDto.empty("engine is not ready");
        }
        EditorDeclarationResultDto base = EditorDeclarationResolver.resolve(engine, doc, caretOffset);
        if (base == null || !base.hasTargets()) {
            return base == null ? EditorDeclarationResultDto.empty(action.emptyStatus()) : base;
        }
        Map<String, EditorDeclarationTargetDto> out = new LinkedHashMap<>();
        switch (action) {
            case USAGES -> collectUsages(engine, base.targets(), out);
            case IMPLEMENTATIONS -> collectImplementations(engine, base.targets(), out);
            case HIERARCHY -> collectHierarchy(engine, base.targets(), out);
        }
        List<EditorDeclarationTargetDto> targets = rankTargets(out, doc);
        if (targets.isEmpty()) {
            return new EditorDeclarationResultDto(
                    base.token(),
                    base.symbolKind(),
                    List.of(),
                    action.emptyStatus()
            );
        }
        return new EditorDeclarationResultDto(
                base.token(),
                base.symbolKind(),
                targets,
                action.statusPrefix() + ": " + targets.size()
        );
    }

    private static void collectUsages(CoreEngine engine,
                                      List<EditorDeclarationTargetDto> seeds,
                                      Map<String, EditorDeclarationTargetDto> out) {
        if (seeds == null || seeds.isEmpty()) {
            return;
        }
        for (EditorDeclarationTargetDto seed : seeds) {
            if (seed == null || seed.localTarget()) {
                continue;
            }
            if (seed.methodTarget()) {
                List<MethodResult> callers = engine.getCallers(
                        normalizeClass(seed.className()),
                        seed.methodName(),
                        seed.methodDesc(),
                        normalizeJarId(seed.jarId()));
                if (callers != null && !callers.isEmpty()) {
                    callers.sort(Comparator
                            .comparing((MethodResult item) -> safe(item.getClassName()))
                            .thenComparing(item -> safe(item.getMethodName()))
                            .thenComparing(item -> safe(item.getMethodDesc()))
                            .thenComparingInt(MethodResult::getJarId));
                    for (MethodResult caller : callers) {
                        appendMethodTarget(out, caller, "usage-caller");
                    }
                    continue;
                }
                // Fallback for unresolved call edges: keep declaration target visible.
                addTarget(out, withSource(seed, "usage-fallback"));
                continue;
            }
            addTarget(out, withSource(seed, "usage-declaration"));
        }
    }

    private static void collectImplementations(CoreEngine engine,
                                               List<EditorDeclarationTargetDto> seeds,
                                               Map<String, EditorDeclarationTargetDto> out) {
        if (seeds == null || seeds.isEmpty()) {
            return;
        }
        for (EditorDeclarationTargetDto seed : seeds) {
            if (seed == null || seed.localTarget()) {
                continue;
            }
            if (seed.methodTarget()) {
                String owner = normalizeClass(seed.className());
                String method = safe(seed.methodName());
                String desc = safe(seed.methodDesc());
                Integer jarId = normalizeJarId(seed.jarId());
                List<MethodResult> impls = engine.getImpls(owner, method, desc, jarId);
                if (impls != null && !impls.isEmpty()) {
                    for (MethodResult impl : impls) {
                        if (sameMethod(seed, impl)) {
                            continue;
                        }
                        appendMethodTarget(out, impl, "impl");
                    }
                }
                appendDefaultMethodFallback(engine, owner, method, desc, out);
                if (out.isEmpty()) {
                    addTarget(out, withSource(seed, "impl-self"));
                }
                continue;
            }
            appendClassImplementations(engine, seed, out);
        }
    }

    private static void collectHierarchy(CoreEngine engine,
                                         List<EditorDeclarationTargetDto> seeds,
                                         Map<String, EditorDeclarationTargetDto> out) {
        if (seeds == null || seeds.isEmpty()) {
            return;
        }
        for (EditorDeclarationTargetDto seed : seeds) {
            if (seed == null || seed.localTarget()) {
                continue;
            }
            if (seed.methodTarget()) {
                addTarget(out, withSource(seed, "hier-self"));
                List<MethodResult> supers = engine.getSuperImpls(
                        normalizeClass(seed.className()),
                        seed.methodName(),
                        seed.methodDesc(),
                        normalizeJarId(seed.jarId()));
                if (supers != null) {
                    for (MethodResult superMethod : supers) {
                        appendMethodTarget(out, superMethod, "hier-super");
                    }
                }
                List<MethodResult> impls = engine.getImpls(
                        normalizeClass(seed.className()),
                        seed.methodName(),
                        seed.methodDesc(),
                        normalizeJarId(seed.jarId()));
                if (impls != null) {
                    for (MethodResult impl : impls) {
                        appendMethodTarget(out, impl, "hier-sub");
                    }
                }
                appendBridgeCompanions(engine, seed, out);
                continue;
            }
            addTarget(out, withSource(seed, "hier-self"));
            appendClassHierarchy(engine, seed, out);
        }
    }

    private static void appendClassImplementations(CoreEngine engine,
                                                   EditorDeclarationTargetDto seed,
                                                   Map<String, EditorDeclarationTargetDto> out) {
        String className = normalizeClass(seed.className());
        if (className == null) {
            return;
        }
        Set<ClassReference.Handle> subs = engine.getSubClasses(new ClassReference.Handle(className));
        if (subs == null || subs.isEmpty()) {
            addTarget(out, withSource(seed, "impl-self"));
            return;
        }
        int scanned = 0;
        for (ClassReference.Handle handle : subs) {
            if (handle == null || handle.getName() == null || handle.getName().isBlank()) {
                continue;
            }
            appendClassTargets(engine, out, handle.getName(), "impl-class");
            scanned++;
            if (scanned >= MAX_CLASS_SCAN) {
                break;
            }
        }
    }

    private static void appendClassHierarchy(CoreEngine engine,
                                             EditorDeclarationTargetDto seed,
                                             Map<String, EditorDeclarationTargetDto> out) {
        String className = normalizeClass(seed.className());
        if (className == null) {
            return;
        }
        Set<ClassReference.Handle> supers = engine.getSuperClasses(new ClassReference.Handle(className));
        Set<ClassReference.Handle> subs = engine.getSubClasses(new ClassReference.Handle(className));
        int scanned = 0;
        if (supers != null) {
            List<ClassReference.Handle> list = new ArrayList<>(supers);
            list.sort(Comparator.comparing(item -> safe(item.getName())));
            for (ClassReference.Handle handle : list) {
                if (handle == null || handle.getName() == null || handle.getName().isBlank()) {
                    continue;
                }
                appendClassTargets(engine, out, handle.getName(), "hier-super");
                scanned++;
                if (scanned >= MAX_CLASS_SCAN) {
                    break;
                }
            }
        }
        scanned = 0;
        if (subs != null) {
            List<ClassReference.Handle> list = new ArrayList<>(subs);
            list.sort(Comparator.comparing(item -> safe(item.getName())));
            for (ClassReference.Handle handle : list) {
                if (handle == null || handle.getName() == null || handle.getName().isBlank()) {
                    continue;
                }
                appendClassTargets(engine, out, handle.getName(), "hier-sub");
                scanned++;
                if (scanned >= MAX_CLASS_SCAN) {
                    break;
                }
            }
        }
    }

    private static void appendDefaultMethodFallback(CoreEngine engine,
                                                    String ownerClass,
                                                    String methodName,
                                                    String methodDesc,
                                                    Map<String, EditorDeclarationTargetDto> out) {
        if (ownerClass == null || methodName == null || methodDesc == null) {
            return;
        }
        Set<ClassReference.Handle> subs = engine.getSubClasses(new ClassReference.Handle(ownerClass));
        if (subs == null || subs.isEmpty()) {
            return;
        }
        int scanned = 0;
        for (ClassReference.Handle handle : subs) {
            if (handle == null || handle.getName() == null) {
                continue;
            }
            List<MethodResult> methods = engine.getMethod(handle.getName(), methodName, methodDesc);
            if (methods == null || methods.isEmpty()) {
                scanned++;
                if (scanned >= MAX_CLASS_SCAN) {
                    break;
                }
                continue;
            }
            for (MethodResult method : methods) {
                appendMethodTarget(out, method, "impl-fallback");
            }
            scanned++;
            if (scanned >= MAX_CLASS_SCAN) {
                break;
            }
        }
    }

    private static void appendBridgeCompanions(CoreEngine engine,
                                               EditorDeclarationTargetDto seed,
                                               Map<String, EditorDeclarationTargetDto> out) {
        String className = normalizeClass(seed.className());
        if (className == null || seed.methodName() == null || seed.methodName().isBlank()) {
            return;
        }
        List<MethodResult> methods = engine.getMethod(className, seed.methodName(), null);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        for (MethodResult method : methods) {
            if (method == null) {
                continue;
            }
            if ((method.getAccessInt() & Opcodes.ACC_BRIDGE) == 0) {
                continue;
            }
            appendMethodTarget(out, method, "hier-bridge");
        }
    }

    private static void appendClassTargets(CoreEngine engine,
                                           Map<String, EditorDeclarationTargetDto> out,
                                           String className,
                                           String source) {
        String normalized = normalizeClass(className);
        if (normalized == null) {
            return;
        }
        List<ClassResult> classes = engine.getClassesByClass(normalized);
        if (classes == null || classes.isEmpty()) {
            addTarget(out, new EditorDeclarationTargetDto(
                    normalized, "", "", "", 0, -1, "class", source));
            return;
        }
        classes.sort(Comparator.comparingInt(ClassResult::getJarId)
                .thenComparing(item -> safe(item.getJarName())));
        for (ClassResult item : classes) {
            if (item == null) {
                continue;
            }
            addTarget(out, new EditorDeclarationTargetDto(
                    normalizeClass(item.getClassName()),
                    "",
                    "",
                    safe(item.getJarName()),
                    Math.max(0, item.getJarId()),
                    -1,
                    "class",
                    source
            ));
        }
    }

    private static void appendMethodTarget(Map<String, EditorDeclarationTargetDto> out,
                                           MethodResult method,
                                           String source) {
        if (method == null) {
            return;
        }
        addTarget(out, new EditorDeclarationTargetDto(
                normalizeClass(method.getClassName()),
                safe(method.getMethodName()),
                safe(method.getMethodDesc()),
                safe(method.getJarName()),
                Math.max(0, method.getJarId()),
                -1,
                "method",
                source
        ));
    }

    private static boolean sameMethod(EditorDeclarationTargetDto target, MethodResult method) {
        if (target == null || method == null) {
            return false;
        }
        if (!safe(target.className()).equals(normalizeClass(method.getClassName()))) {
            return false;
        }
        if (!safe(target.methodName()).equals(safe(method.getMethodName()))) {
            return false;
        }
        if (!safe(target.methodDesc()).equals(safe(method.getMethodDesc()))) {
            return false;
        }
        if (target.jarId() <= 0 || method.getJarId() <= 0) {
            return true;
        }
        return target.jarId() == method.getJarId();
    }

    private static EditorDeclarationTargetDto withSource(EditorDeclarationTargetDto base, String source) {
        if (base == null) {
            return null;
        }
        return new EditorDeclarationTargetDto(
                base.className(),
                base.methodName(),
                base.methodDesc(),
                base.jarName(),
                base.jarId(),
                base.caretOffset(),
                base.kind(),
                source
        );
    }

    private static void addTarget(Map<String, EditorDeclarationTargetDto> out, EditorDeclarationTargetDto target) {
        if (out == null || target == null) {
            return;
        }
        String key = safe(target.className()) + "|" + safe(target.methodName()) + "|" + safe(target.methodDesc())
                + "|" + target.jarId() + "|" + target.caretOffset();
        out.putIfAbsent(key, target);
    }

    private static List<EditorDeclarationTargetDto> rankTargets(Map<String, EditorDeclarationTargetDto> map,
                                                                me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto doc) {
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        List<EditorDeclarationTargetDto> out = new ArrayList<>(map.values());
        String currentClass = normalizeClass(doc == null ? null : doc.className());
        int currentJar = doc == null || doc.jarId() == null ? 0 : Math.max(0, doc.jarId());
        out.sort(Comparator
                .comparingInt((EditorDeclarationTargetDto item) -> score(item, currentClass, currentJar))
                .reversed()
                .thenComparing(EditorDeclarationTargetDto::source)
                .thenComparing(EditorDeclarationTargetDto::className)
                .thenComparing(EditorDeclarationTargetDto::methodName)
                .thenComparing(EditorDeclarationTargetDto::methodDesc)
                .thenComparingInt(EditorDeclarationTargetDto::jarId));
        if (out.size() > MAX_NAV_TARGETS) {
            return List.copyOf(out.subList(0, MAX_NAV_TARGETS));
        }
        return List.copyOf(out);
    }

    private static int score(EditorDeclarationTargetDto item, String currentClass, int currentJar) {
        if (item == null) {
            return Integer.MIN_VALUE;
        }
        int score = sourceScore(item.source());
        if (item.localTarget()) {
            score += 120;
        }
        if (currentClass != null && currentClass.equals(normalizeClass(item.className()))) {
            score += 20;
        }
        if (currentJar > 0 && currentJar == item.jarId()) {
            score += 16;
        }
        if (item.methodTarget()) {
            score += 4;
        }
        return score;
    }

    private static int sourceScore(String source) {
        return switch (safe(source)) {
            case "hier-self" -> 90;
            case "impl" -> 80;
            case "impl-fallback" -> 74;
            case "hier-super" -> 72;
            case "hier-sub" -> 68;
            case "hier-bridge" -> 66;
            case "usage-caller" -> 64;
            case "impl-class" -> 62;
            case "usage-fallback" -> 58;
            case "usage-declaration" -> 56;
            case "impl-self" -> 52;
            default -> 40;
        };
    }

    private static String normalizeClass(String className) {
        if (className == null) {
            return null;
        }
        String value = className.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.endsWith(".class")) {
            value = value.substring(0, value.length() - 6);
        }
        return value.replace('.', '/');
    }

    private static Integer normalizeJarId(int jarId) {
        if (jarId <= 0) {
            return null;
        }
        return jarId;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private enum Action {
        USAGES("usages", "usage target not found"),
        IMPLEMENTATIONS("implementations", "implementation target not found"),
        HIERARCHY("hierarchy", "hierarchy target not found");

        private final String statusPrefix;
        private final String emptyStatus;

        Action(String statusPrefix, String emptyStatus) {
            this.statusPrefix = statusPrefix;
            this.emptyStatus = emptyStatus;
        }

        private String statusPrefix() {
            return statusPrefix;
        }

        private String emptyStatus() {
            return emptyStatus;
        }
    }
}
