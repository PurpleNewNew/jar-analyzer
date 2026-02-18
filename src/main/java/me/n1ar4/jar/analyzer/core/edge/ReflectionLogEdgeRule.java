/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ReflectionLogEdgeRule implements EdgeInferRule {
    private static final Logger logger = LogManager.getLogger();
    private static final String PROP_ENABLE = "jar.analyzer.edge.reflection.log.enable";
    private static final String PROP_LOG_PATH = "jar.analyzer.reflection.log";

    private static final String OWNER_CLASS = "java/lang/Class";
    private static final String OWNER_METHOD = "java/lang/reflect/Method";
    private static final String OWNER_CTOR = "java/lang/reflect/Constructor";

    private static final String NAME_FOR_NAME = "forName";
    private static final String NAME_NEW_INSTANCE = "newInstance";
    private static final String NAME_METHOD_INVOKE = "invoke";

    private static final String REASON_PREFIX = "tamiflex_log:";

    @Override
    public String id() {
        return "reflectionLog";
    }

    @Override
    public int apply(BuildContext ctx) {
        if (!isEnabled()) {
            return 0;
        }
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return 0;
        }
        Path log = resolveLogFile();
        if (log == null) {
            return 0;
        }
        List<ReflectionLogEntry> entries = loadEntries(log);
        if (entries.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (ReflectionLogEntry entry : entries) {
            if (entry == null || entry.kind == ReflectionKind.UNKNOWN) {
                continue;
            }
            List<MethodReference.Handle> callers = resolveCallers(entry, ctx);
            if (callers.isEmpty()) {
                continue;
            }
            List<MethodReference.Handle> targets = resolveTargets(entry, ctx);
            if (targets.isEmpty()) {
                continue;
            }
            Integer opcode = opcodeFor(entry.kind);
            String reason = REASON_PREFIX + entry.kind.logLabel;
            for (MethodReference.Handle caller : callers) {
                if (caller == null) {
                    continue;
                }
                HashSet<MethodReference.Handle> callees =
                        ctx.methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                for (MethodReference.Handle target : targets) {
                    if (target == null) {
                        continue;
                    }
                    MethodReference.Handle callTarget = new MethodReference.Handle(
                            target.getClassReference(),
                            opcode == null ? -1 : opcode,
                            target.getName(),
                            target.getDesc()
                    );
                    if (MethodCallUtils.addCallee(callees, callTarget)) {
                        added++;
                        MethodCallMeta.record(ctx.methodCallMeta, MethodCallKey.of(caller, callTarget),
                                MethodCallMeta.TYPE_REFLECTION, MethodCallMeta.CONF_HIGH, reason, opcode);
                    }
                }
            }
        }
        return added;
    }

    private static boolean isEnabled() {
        String raw = System.getProperty(PROP_ENABLE);
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.strip());
    }

    private static Path resolveLogFile() {
        String raw = System.getProperty(PROP_LOG_PATH);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Path p = Paths.get(raw.trim());
        if (!Files.exists(p) || !Files.isRegularFile(p)) {
            logger.warn("reflection log not found: {}", p);
            return null;
        }
        return p;
    }

    private static List<ReflectionLogEntry> loadEntries(Path log) {
        List<ReflectionLogEntry> out = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ReflectionLogEntry entry = parseEntry(line);
                if (entry != null) {
                    out.add(entry);
                }
            }
        } catch (Exception ex) {
            logger.warn("load reflection log failed: {}", ex.toString());
        }
        return out;
    }

    private static ReflectionLogEntry parseEntry(String raw) {
        if (raw == null) {
            return null;
        }
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }
        String[] parts = line.split(";", -1);
        if (parts.length < 4) {
            return null;
        }
        ReflectionKind kind = ReflectionKind.parse(parts[0].trim());
        if (kind == ReflectionKind.UNKNOWN) {
            return null;
        }
        SourceMethod source = parseSource(parts[2].trim());
        if (source == null) {
            return null;
        }
        int lineNo = -1;
        String linePart = parts[3] == null ? "" : parts[3].trim();
        if (!linePart.isEmpty()) {
            try {
                lineNo = Integer.parseInt(linePart);
            } catch (Exception ignored) {
                lineNo = -1;
            }
        }
        return new ReflectionLogEntry(kind, parts[1].trim(), source.className, source.methodName, lineNo);
    }

    private static SourceMethod parseSource(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int idx = raw.lastIndexOf('.');
        if (idx <= 0 || idx >= raw.length() - 1) {
            return null;
        }
        String className = toInternalClass(raw.substring(0, idx).trim());
        String methodName = raw.substring(idx + 1).trim();
        if (className == null || className.isEmpty() || methodName.isEmpty()) {
            return null;
        }
        return new SourceMethod(className, methodName);
    }

    private static List<MethodReference.Handle> resolveCallers(ReflectionLogEntry entry, BuildContext ctx) {
        if (entry == null || ctx == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        List<CallSiteEntity> callSites = ctx.callSites;
        if (callSites != null && !callSites.isEmpty()) {
            for (CallSiteEntity site : callSites) {
                if (site == null) {
                    continue;
                }
                if (!entry.sourceClass.equals(site.getCallerClassName())) {
                    continue;
                }
                if (!entry.sourceMethod.equals(site.getCallerMethodName())) {
                    continue;
                }
                if (entry.lineNumber >= 0) {
                    Integer ln = site.getLineNumber();
                    if (ln == null || ln != entry.lineNumber) {
                        continue;
                    }
                }
                if (!matchesReflectionApiCall(entry.kind, site)) {
                    continue;
                }
                out.add(new MethodReference.Handle(
                        new ClassReference.Handle(site.getCallerClassName(), normalizeJarId(site.getJarId())),
                        site.getCallerMethodName(),
                        site.getCallerMethodDesc()
                ));
            }
            if (!out.isEmpty()) {
                return new ArrayList<>(out);
            }
        }
        for (MethodReference.Handle handle : ctx.methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!entry.sourceClass.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!entry.sourceMethod.equals(handle.getName())) {
                continue;
            }
            out.add(handle);
        }
        return new ArrayList<>(out);
    }

    private static boolean matchesReflectionApiCall(ReflectionKind kind, CallSiteEntity site) {
        if (site == null || kind == null) {
            return false;
        }
        String owner = site.getCalleeOwner();
        String name = site.getCalleeMethodName();
        if (owner == null || name == null) {
            return false;
        }
        return switch (kind) {
            case CLASS_FOR_NAME -> OWNER_CLASS.equals(owner) && NAME_FOR_NAME.equals(name);
            case CLASS_NEW_INSTANCE -> OWNER_CLASS.equals(owner) && NAME_NEW_INSTANCE.equals(name);
            case CONSTRUCTOR_NEW_INSTANCE -> OWNER_CTOR.equals(owner) && NAME_NEW_INSTANCE.equals(name);
            case METHOD_INVOKE -> OWNER_METHOD.equals(owner) && NAME_METHOD_INVOKE.equals(name);
            default -> false;
        };
    }

    private static List<MethodReference.Handle> resolveTargets(ReflectionLogEntry entry, BuildContext ctx) {
        if (entry == null || ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Collections.emptyList();
        }
        if (entry.kind == ReflectionKind.CLASS_FOR_NAME) {
            return Collections.emptyList();
        }
        if (entry.kind == ReflectionKind.CLASS_NEW_INSTANCE) {
            return resolveClassNewInstanceTargets(entry.mappedTarget, ctx.methodMap);
        }
        TargetMethod target = parseTargetMethod(entry.mappedTarget);
        if (target == null) {
            return Collections.emptyList();
        }
        MethodReference.Handle exact = new MethodReference.Handle(
                new ClassReference.Handle(target.className),
                target.methodName,
                target.desc
        );
        if (ctx.methodMap.containsKey(exact)) {
            return Collections.singletonList(exact);
        }
        List<MethodReference.Handle> loose = new ArrayList<>();
        for (MethodReference.Handle handle : ctx.methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!target.className.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!target.methodName.equals(handle.getName())) {
                continue;
            }
            if (target.paramCount >= 0 && Type.getArgumentTypes(handle.getDesc()).length != target.paramCount) {
                continue;
            }
            loose.add(handle);
        }
        return loose;
    }

    private static List<MethodReference.Handle> resolveClassNewInstanceTargets(String mappedTarget,
                                                                               Map<MethodReference.Handle, MethodReference> methodMap) {
        String className = toInternalClass(mappedTarget);
        if (className == null || className.isEmpty()) {
            return Collections.emptyList();
        }
        MethodReference.Handle noArgCtor =
                new MethodReference.Handle(new ClassReference.Handle(className), "<init>", "()V");
        if (methodMap.containsKey(noArgCtor)) {
            return Collections.singletonList(noArgCtor);
        }
        List<MethodReference.Handle> ctors = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!className.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!"<init>".equals(handle.getName())) {
                continue;
            }
            ctors.add(handle);
        }
        return ctors;
    }

    private static TargetMethod parseTargetMethod(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (!(value.startsWith("<") && value.endsWith(">"))) {
            return null;
        }
        String inner = value.substring(1, value.length() - 1);
        int colon = inner.indexOf(':');
        if (colon <= 0 || colon >= inner.length() - 1) {
            return null;
        }
        String className = toInternalClass(inner.substring(0, colon).trim());
        String right = inner.substring(colon + 1).trim();
        int lp = right.indexOf('(');
        int rp = right.lastIndexOf(')');
        if (lp <= 0 || rp < lp) {
            return null;
        }
        String head = right.substring(0, lp).trim();
        int sp = head.lastIndexOf(' ');
        String methodName = sp >= 0 ? head.substring(sp + 1).trim() : head;
        String retType = sp >= 0 ? head.substring(0, sp).trim() : "void";
        String paramsRaw = right.substring(lp + 1, rp).trim();
        List<String> params = splitParamList(paramsRaw);
        StringBuilder desc = new StringBuilder();
        desc.append('(');
        for (String param : params) {
            desc.append(toDescriptor(param));
        }
        desc.append(')').append(toDescriptor(retType));
        return new TargetMethod(className, methodName, desc.toString(), params.size());
    }

    private static List<String> splitParamList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String[] arr = raw.split(",");
        List<String> out = new ArrayList<>(arr.length);
        for (String s : arr) {
            String v = s == null ? "" : s.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }

    private static String toDescriptor(String rawType) {
        String t = rawType == null ? "" : rawType.trim();
        if (t.isEmpty()) {
            return "Ljava/lang/Object;";
        }
        while (t.contains("<") && t.contains(">")) {
            int l = t.indexOf('<');
            int r = t.lastIndexOf('>');
            if (l >= 0 && r > l) {
                t = t.substring(0, l) + t.substring(r + 1);
            } else {
                break;
            }
        }
        if (t.endsWith("...")) {
            t = t.substring(0, t.length() - 3) + "[]";
        }
        int dims = 0;
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2);
        }
        String base = switch (t) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "char" -> "C";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> {
                String cls = toInternalClass(t);
                if (cls == null || cls.isEmpty()) {
                    yield "Ljava/lang/Object;";
                }
                if (cls.startsWith("L") && cls.endsWith(";")) {
                    yield cls;
                }
                yield "L" + cls + ";";
            }
        };
        if (dims <= 0) {
            return base;
        }
        StringBuilder sb = new StringBuilder(dims + base.length());
        for (int i = 0; i < dims; i++) {
            sb.append('[');
        }
        sb.append(base);
        return sb.toString();
    }

    private static String toInternalClass(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.startsWith("L") && v.endsWith(";")) {
            return v.substring(1, v.length() - 1);
        }
        if (v.contains("/")) {
            return v;
        }
        return v.replace('.', '/');
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
    }

    private static Integer opcodeFor(ReflectionKind kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case METHOD_INVOKE, CLASS_NEW_INSTANCE, CONSTRUCTOR_NEW_INSTANCE, CLASS_FOR_NAME -> Opcodes.INVOKEVIRTUAL;
            default -> null;
        };
    }

    private record SourceMethod(String className, String methodName) {
    }

    private record TargetMethod(String className, String methodName, String desc, int paramCount) {
    }

    private static final class ReflectionLogEntry {
        private final ReflectionKind kind;
        private final String mappedTarget;
        private final String sourceClass;
        private final String sourceMethod;
        private final int lineNumber;

        private ReflectionLogEntry(ReflectionKind kind,
                                   String mappedTarget,
                                   String sourceClass,
                                   String sourceMethod,
                                   int lineNumber) {
            this.kind = kind;
            this.mappedTarget = mappedTarget;
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.lineNumber = lineNumber;
        }
    }

    private enum ReflectionKind {
        CLASS_FOR_NAME("class.forname"),
        CLASS_NEW_INSTANCE("class.newinstance"),
        CONSTRUCTOR_NEW_INSTANCE("constructor.newinstance"),
        METHOD_INVOKE("method.invoke"),
        UNKNOWN("unknown");

        private final String logLabel;

        ReflectionKind(String logLabel) {
            this.logLabel = logLabel;
        }

        private static ReflectionKind parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return UNKNOWN;
            }
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if ("class.forname".equals(v)) {
                return CLASS_FOR_NAME;
            }
            if ("class.newinstance".equals(v)) {
                return CLASS_NEW_INSTANCE;
            }
            if ("constructor.newinstance".equals(v)) {
                return CONSTRUCTOR_NEW_INSTANCE;
            }
            if ("method.invoke".equals(v)) {
                return METHOD_INVOKE;
            }
            return UNKNOWN;
        }
    }
}
