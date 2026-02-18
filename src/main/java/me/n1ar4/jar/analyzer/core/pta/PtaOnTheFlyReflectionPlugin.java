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

import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reflection fallback based on string pools and light-weight call-neighbor propagation.
 * This complements ReflectionCallResolver when local constant propagation misses
 * helper-method string assembly or class-loader chains.
 */
final class PtaOnTheFlyReflectionPlugin implements PtaPlugin {
    private static final int MAX_CLASS_HINTS = 24;
    private static final int MAX_METHOD_HINTS = 16;
    private static final int MAX_DESC_HINTS = 16;
    private static final int MAX_TARGETS_PER_TRIGGER = 96;
    private static final int MAX_CTORS_PER_CLASS = 2;

    private static final String PROP_HINT_HOPS = "jar.analyzer.pta.reflection.hint.hops";
    private static final String PROP_HINT_POOL_SIZE = "jar.analyzer.pta.reflection.max.pool.size";

    private static final String CLASS_OWNER = "java/lang/Class";
    private static final String CLASS_LOADER_OWNER = "java/lang/ClassLoader";
    private static final String METHOD_OWNER = "java/lang/reflect/Method";
    private static final String CONSTRUCTOR_OWNER = "java/lang/reflect/Constructor";
    private static final String MH_OWNER = "java/lang/invoke/MethodHandle";
    private static final String LOOKUP_OWNER = "java/lang/invoke/MethodHandles$Lookup";

    private static final String REASON_REFLECTION = "pta_reflection_pool";
    private static final String REASON_METHOD_HANDLE = "pta_method_handle_pool";

    private PtaPluginBridge bridge;
    private BuildContext ctx;

    private int hintHopLimit;
    private int maxHintPoolSize;

    private final Map<String, List<MethodReference.Handle>> methodsByClass = new HashMap<>();
    private final Set<String> knownMethodNames = new HashSet<>();
    private final ArrayList<String> knownClasses = new ArrayList<>();

    private final Map<MethodReference.Handle, LinkedHashSet<String>> localStringPoolByMethod = new HashMap<>();
    private final Map<MethodReference.Handle, Set<MethodReference.Handle>> callersByMethod = new HashMap<>();

    private final Map<MethodReference.Handle, ReflectionHints> hintsByMethod = new HashMap<>();
    private final Set<MethodReference.Handle> unresolvedHints = new HashSet<>();
    private final Set<String> fired = new HashSet<>();

    @Override
    public void setBridge(PtaPluginBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void onStart() {
        if (bridge == null) {
            return;
        }
        ctx = bridge.getBuildContext();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return;
        }
        hintHopLimit = readInt(PROP_HINT_HOPS, 1, 0, 2);
        maxHintPoolSize = readInt(PROP_HINT_POOL_SIZE, 160, 32, 1024);

        indexDeclaredMethods();
        indexLocalStringPool();
        indexReverseCallers();
        hintsByMethod.clear();
        unresolvedHints.clear();
        fired.clear();
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        if (method == null || method.getMethod() == null || ctx == null || ctx.methodCalls == null) {
            return;
        }
        MethodReference.Handle caller = canonical(method.getMethod());
        if (caller == null) {
            return;
        }
        Set<MethodReference.Handle> callees = ctx.methodCalls.get(caller);
        if (callees == null || callees.isEmpty()) {
            return;
        }
        applyReflectionFallback(method, caller, new ArrayList<>(callees));
    }

    @Override
    public void onNewCallEdge(PtaContextMethod callerContext,
                              MethodReference.Handle caller,
                              MethodReference.Handle callee,
                              String edgeType,
                              String confidence,
                              int opcode) {
        if (callerContext == null || callerContext.getMethod() == null || callee == null) {
            return;
        }
        MethodReference.Handle canonicalCaller = canonical(callerContext.getMethod());
        if (canonicalCaller == null) {
            return;
        }
        ArrayList<MethodReference.Handle> one = new ArrayList<>(1);
        one.add(callee);
        applyReflectionFallback(callerContext, canonicalCaller, one);
    }

    private void applyReflectionFallback(PtaContextMethod callerContext,
                                         MethodReference.Handle caller,
                                         List<MethodReference.Handle> calleeSnapshot) {
        if (bridge == null || callerContext == null || caller == null
                || calleeSnapshot == null || calleeSnapshot.isEmpty()) {
            return;
        }
        ReflectionTrigger trigger = inspectTrigger(calleeSnapshot);
        if (!trigger.hasAny()) {
            return;
        }
        ReflectionHints hints = resolveHints(caller);
        if (hints == null || hints.classHints.isEmpty()) {
            return;
        }

        LinkedHashSet<MethodReference.Handle> targets = new LinkedHashSet<>();
        if (trigger.needsConstructors()) {
            collectCtorTargets(hints.classHints, hints.ctorDescHints, targets);
        }
        if (trigger.needsMethodCalls()) {
            collectMethodTargets(hints.classHints, hints.methodHints, hints.methodDescHints, targets);
        }
        if (targets.isEmpty()) {
            return;
        }

        String type = trigger.methodHandle
                ? MethodCallMeta.TYPE_METHOD_HANDLE
                : MethodCallMeta.TYPE_REFLECTION;
        String confidence = resolveConfidence(
                hints.classHints.size(),
                hints.methodHints.size(),
                hints.methodDescHints.size() + hints.ctorDescHints.size(),
                targets.size(),
                hints.neighborDerived);
        String reason = buildReason(
                trigger,
                type,
                hints.classHints.size(),
                hints.methodHints.size(),
                hints.methodDescHints.size() + hints.ctorDescHints.size(),
                hints.neighborDerived);

        String fireKey = callerContext.id() + "|" + type + "|" + reason;
        if (!fired.add(fireKey)) {
            return;
        }

        int count = 0;
        for (MethodReference.Handle target : targets) {
            if (target == null) {
                continue;
            }
            if (count >= MAX_TARGETS_PER_TRIGGER) {
                break;
            }
            count++;
            int opcode = "<init>".equals(target.getName()) ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
            bridge.addSemanticEdge(callerContext,
                    target,
                    type,
                    confidence,
                    reason,
                    opcode,
                    null,
                    "sem:reflection_pool");
        }
    }

    private ReflectionHints resolveHints(MethodReference.Handle caller) {
        if (caller == null) {
            return null;
        }
        ReflectionHints cached = hintsByMethod.get(caller);
        if (cached != null) {
            return cached;
        }
        if (unresolvedHints.contains(caller)) {
            return null;
        }

        HintPool pool = collectHintPool(caller);
        ReflectionHints hints = buildHints(caller, pool.values, pool.neighborDerived);
        if (hints == null || hints.classHints.isEmpty()) {
            unresolvedHints.add(caller);
            return null;
        }
        hintsByMethod.put(caller, hints);
        return hints;
    }

    private HintPool collectHintPool(MethodReference.Handle caller) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        boolean neighborDerived = false;

        addPool(values, localStringPoolByMethod.get(caller), maxHintPoolSize);
        if (hintHopLimit <= 0 || values.size() >= maxHintPoolSize) {
            return new HintPool(values, false);
        }

        ArrayDeque<NeighborNode> queue = new ArrayDeque<>();
        HashSet<MethodReference.Handle> visited = new HashSet<>();
        visited.add(caller);
        queue.addLast(new NeighborNode(caller, 0));

        while (!queue.isEmpty() && values.size() < maxHintPoolSize) {
            NeighborNode node = queue.pollFirst();
            if (node == null || node.method == null || node.depth >= hintHopLimit) {
                continue;
            }
            Set<MethodReference.Handle> neighbors = neighborsOf(node.method);
            if (neighbors.isEmpty()) {
                continue;
            }
            int nextDepth = node.depth + 1;
            for (MethodReference.Handle neighbor : neighbors) {
                if (neighbor == null || !visited.add(neighbor)) {
                    continue;
                }
                queue.addLast(new NeighborNode(neighbor, nextDepth));
                if (!acceptNeighborForHints(caller, neighbor, nextDepth)) {
                    continue;
                }
                if (addPool(values, localStringPoolByMethod.get(neighbor), maxHintPoolSize)) {
                    neighborDerived = true;
                }
                if (values.size() >= maxHintPoolSize) {
                    break;
                }
            }
        }

        if (values.isEmpty()) {
            Set<MethodReference.Handle> directNeighbors = neighborsOf(caller);
            for (MethodReference.Handle neighbor : directNeighbors) {
                if (addPool(values, localStringPoolByMethod.get(neighbor), maxHintPoolSize)) {
                    neighborDerived = true;
                }
                if (values.size() >= maxHintPoolSize) {
                    break;
                }
            }
        }

        return new HintPool(values, neighborDerived);
    }

    private Set<MethodReference.Handle> neighborsOf(MethodReference.Handle method) {
        if (method == null || ctx == null || ctx.methodCalls == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();

        Set<MethodReference.Handle> callees = ctx.methodCalls.get(method);
        if (callees != null && !callees.isEmpty()) {
            for (MethodReference.Handle callee : callees) {
                MethodReference.Handle canonical = canonical(callee);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }

        Set<MethodReference.Handle> callers = callersByMethod.get(method);
        if (callers != null && !callers.isEmpty()) {
            out.addAll(callers);
        }

        return out;
    }

    private boolean acceptNeighborForHints(MethodReference.Handle caller,
                                           MethodReference.Handle neighbor,
                                           int depth) {
        if (caller == null || neighbor == null || depth <= 0) {
            return false;
        }
        String callerClass = ownerName(caller);
        String neighborClass = ownerName(neighbor);
        if (callerClass == null || neighborClass == null) {
            return false;
        }
        if (callerClass.equals(neighborClass)) {
            return true;
        }
        String callerPkg = packageName(callerClass);
        String neighborPkg = packageName(neighborClass);
        if (callerPkg != null && callerPkg.equals(neighborPkg)) {
            return true;
        }
        return depth == 1 && localStringPoolByMethod.get(caller) == null;
    }

    private void collectCtorTargets(Set<String> classes,
                                    Set<String> ctorDescHints,
                                    Set<MethodReference.Handle> out) {
        if (classes == null || classes.isEmpty() || out == null) {
            return;
        }
        for (String owner : classes) {
            List<MethodReference.Handle> declared = methodsByClass.get(owner);
            if (declared == null || declared.isEmpty()) {
                continue;
            }

            int before = out.size();
            if (ctorDescHints != null && !ctorDescHints.isEmpty()) {
                for (MethodReference.Handle handle : declared) {
                    if (handle == null || !"<init>".equals(handle.getName())) {
                        continue;
                    }
                    if (!ctorDescHints.contains(handle.getDesc())) {
                        continue;
                    }
                    out.add(handle);
                    if (out.size() - before >= MAX_CTORS_PER_CLASS) {
                        break;
                    }
                }
                if (out.size() > before) {
                    continue;
                }
            }

            int perClass = 0;
            MethodReference.Handle noArgCtor = null;
            for (MethodReference.Handle handle : declared) {
                if (handle == null || !"<init>".equals(handle.getName())) {
                    continue;
                }
                if ("()V".equals(handle.getDesc())) {
                    noArgCtor = handle;
                    break;
                }
            }
            if (noArgCtor != null) {
                out.add(noArgCtor);
                perClass++;
            }
            for (MethodReference.Handle handle : declared) {
                if (handle == null || !"<init>".equals(handle.getName())) {
                    continue;
                }
                if (noArgCtor != null && noArgCtor.equals(handle)) {
                    continue;
                }
                out.add(handle);
                perClass++;
                if (perClass >= MAX_CTORS_PER_CLASS) {
                    break;
                }
            }
        }
    }

    private void collectMethodTargets(Set<String> classes,
                                      Set<String> methodHints,
                                      Set<String> methodDescHints,
                                      Set<MethodReference.Handle> out) {
        if (classes == null || classes.isEmpty() || out == null) {
            return;
        }
        for (String owner : classes) {
            List<MethodReference.Handle> declared = methodsByClass.get(owner);
            if (declared == null || declared.isEmpty()) {
                continue;
            }
            ArrayList<MethodReference.Handle> candidates = new ArrayList<>();
            for (MethodReference.Handle handle : declared) {
                if (handle == null || "<init>".equals(handle.getName()) || "<clinit>".equals(handle.getName())) {
                    continue;
                }
                candidates.add(handle);
            }
            if (candidates.isEmpty()) {
                continue;
            }

            boolean added = false;
            if (methodHints != null && !methodHints.isEmpty()) {
                ArrayList<MethodReference.Handle> nameMatched = new ArrayList<>();
                for (MethodReference.Handle handle : candidates) {
                    if (methodHints.contains(handle.getName())) {
                        nameMatched.add(handle);
                    }
                }
                if (!nameMatched.isEmpty()) {
                    if (methodDescHints != null && !methodDescHints.isEmpty()) {
                        ArrayList<MethodReference.Handle> strict = new ArrayList<>();
                        for (MethodReference.Handle handle : nameMatched) {
                            if (methodDescHints.contains(handle.getDesc())) {
                                strict.add(handle);
                            }
                        }
                        if (!strict.isEmpty()) {
                            out.addAll(strict);
                            added = true;
                        }
                    }
                    if (!added) {
                        out.addAll(nameMatched);
                        added = true;
                    }
                }
            } else if (methodDescHints != null && !methodDescHints.isEmpty()) {
                for (MethodReference.Handle handle : candidates) {
                    if (methodDescHints.contains(handle.getDesc())) {
                        out.add(handle);
                        added = true;
                    }
                }
            }

            if (added) {
                continue;
            }

            MethodReference.Handle single = uniqueNonCtorMethod(candidates);
            if (single != null) {
                out.add(single);
            }
        }
    }

    private MethodReference.Handle uniqueNonCtorMethod(List<MethodReference.Handle> methods) {
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        MethodReference.Handle out = null;
        for (MethodReference.Handle handle : methods) {
            if (handle == null || "<init>".equals(handle.getName()) || "<clinit>".equals(handle.getName())) {
                continue;
            }
            if (out != null) {
                return null;
            }
            out = handle;
        }
        return out;
    }

    private ReflectionTrigger inspectTrigger(List<MethodReference.Handle> callees) {
        ReflectionTrigger out = new ReflectionTrigger();
        if (callees == null || callees.isEmpty()) {
            return out;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null
                    || callee.getClassReference().getName() == null || callee.getName() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            String name = callee.getName();
            String desc = callee.getDesc();

            if (CLASS_OWNER.equals(owner) && "forName".equals(name)) {
                out.classLookup = true;
                if ("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;".equals(desc)) {
                    out.loaderReason = "for_name_loader";
                } else if ("(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;".equals(desc)) {
                    out.loaderReason = "for_name_module";
                }
                continue;
            }
            if ((CLASS_LOADER_OWNER.equals(owner) || owner.endsWith("ClassLoader")) && "loadClass".equals(name)) {
                out.classLookup = true;
                if ("(Ljava/lang/String;Z)Ljava/lang/Class;".equals(desc)) {
                    out.loaderReason = "load_class_resolve";
                } else {
                    out.loaderReason = "load_class";
                }
                continue;
            }
            if ((CLASS_LOADER_OWNER.equals(owner) || owner.endsWith("ClassLoader"))
                    && ("findClass".equals(name) || "defineClass".equals(name))) {
                out.classLookup = true;
                out.loaderReason = "loader_define";
                continue;
            }
            if (CLASS_OWNER.equals(owner)
                    && ("getMethod".equals(name) || "getDeclaredMethod".equals(name)
                    || "getMethods".equals(name) || "getDeclaredMethods".equals(name)) ) {
                out.methodLookup = true;
                continue;
            }
            if (CLASS_OWNER.equals(owner)
                    && ("getConstructor".equals(name) || "getDeclaredConstructor".equals(name)
                    || "getConstructors".equals(name) || "getDeclaredConstructors".equals(name))) {
                out.constructorLookup = true;
                continue;
            }
            if (METHOD_OWNER.equals(owner) && "invoke".equals(name)) {
                out.methodInvoke = true;
                continue;
            }
            if ((CONSTRUCTOR_OWNER.equals(owner) && "newInstance".equals(name))
                    || (CLASS_OWNER.equals(owner) && "newInstance".equals(name))) {
                out.constructorInvoke = true;
                continue;
            }
            if (MH_OWNER.equals(owner)
                    && ("invoke".equals(name) || "invokeExact".equals(name) || "invokeWithArguments".equals(name))) {
                out.methodHandle = true;
                out.methodInvoke = true;
                continue;
            }
            if (LOOKUP_OWNER.equals(owner)
                    && ("findVirtual".equals(name) || "findStatic".equals(name)
                    || "findSpecial".equals(name) || "findConstructor".equals(name)
                    || "unreflect".equals(name) || "unreflectSpecial".equals(name)
                    || "unreflectConstructor".equals(name))) {
                out.methodHandle = true;
                out.methodLookup = true;
                if ("findConstructor".equals(name) || "unreflectConstructor".equals(name)) {
                    out.constructorLookup = true;
                }
                continue;
            }
            if ("java/lang/Thread".equals(owner)
                    && "getContextClassLoader".equals(name)
                    && "()Ljava/lang/ClassLoader;".equals(desc)) {
                out.loaderReason = "thread_context";
                continue;
            }
            if ("java/lang/ClassLoader".equals(owner)
                    && "getSystemClassLoader".equals(name)
                    && "()Ljava/lang/ClassLoader;".equals(desc)) {
                out.loaderReason = "system";
                continue;
            }
            if ("java/lang/Class".equals(owner)
                    && "getClassLoader".equals(name)
                    && "()Ljava/lang/ClassLoader;".equals(desc)) {
                out.loaderReason = "class_loader";
            }
        }
        return out;
    }

    private String resolveConfidence(int classCount,
                                     int methodHintCount,
                                     int descHintCount,
                                     int targetCount,
                                     boolean neighborDerived) {
        String level;
        if (classCount <= 2 && (methodHintCount > 0 || descHintCount > 0) && targetCount <= 6) {
            level = MethodCallMeta.CONF_HIGH;
        } else if (classCount <= 8 && targetCount <= 32) {
            level = MethodCallMeta.CONF_MEDIUM;
        } else {
            level = MethodCallMeta.CONF_LOW;
        }
        if (!neighborDerived) {
            return level;
        }
        if (MethodCallMeta.CONF_HIGH.equals(level)) {
            return MethodCallMeta.CONF_MEDIUM;
        }
        return MethodCallMeta.CONF_LOW;
    }

    private String buildReason(ReflectionTrigger trigger,
                               String edgeType,
                               int classCount,
                               int methodHintCount,
                               int descHintCount,
                               boolean neighborDerived) {
        String base = MethodCallMeta.TYPE_METHOD_HANDLE.equals(edgeType)
                ? REASON_METHOD_HANDLE : REASON_REFLECTION;
        StringBuilder sb = new StringBuilder(base);
        sb.append(":classes=").append(classCount)
                .append(";hints=").append(methodHintCount)
                .append(";desc=").append(descHintCount)
                .append(";ctor=").append(trigger.needsConstructors() ? 1 : 0)
                .append(";method=").append(trigger.needsMethodCalls() ? 1 : 0)
                .append(";pool=").append(neighborDerived ? "neighbor" : "local");
        if (trigger.loaderReason != null && !trigger.loaderReason.isEmpty()) {
            sb.append(";loader=").append(trigger.loaderReason);
        }
        return sb.toString();
    }

    private void indexDeclaredMethods() {
        methodsByClass.clear();
        knownMethodNames.clear();
        knownClasses.clear();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || handle.getClassReference() == null
                    || handle.getClassReference().getName() == null
                    || handle.getName() == null || handle.getDesc() == null) {
                continue;
            }
            int jarId = ref == null || ref.getJarId() == null ? -1 : ref.getJarId();
            MethodReference.Handle canonical = new MethodReference.Handle(
                    new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                    handle.getName(),
                    handle.getDesc()
            );
            methodsByClass.computeIfAbsent(handle.getClassReference().getName(), k -> new ArrayList<>()).add(canonical);
            if (!"<init>".equals(handle.getName()) && !"<clinit>".equals(handle.getName())) {
                knownMethodNames.add(handle.getName());
            }
        }
        knownClasses.addAll(methodsByClass.keySet());
        if (knownClasses.size() > 1) {
            knownClasses.sort(String::compareTo);
        }
        for (List<MethodReference.Handle> list : methodsByClass.values()) {
            if (list != null && list.size() > 1) {
                list.sort(Comparator.comparing(PtaOnTheFlyReflectionPlugin::methodKey));
            }
        }
    }

    private void indexLocalStringPool() {
        localStringPoolByMethod.clear();
        mergeStringPool(localStringPoolByMethod, ctx == null ? null : ctx.strMap);
        mergeStringPool(localStringPoolByMethod, ctx == null ? null : ctx.stringAnnoMap);
    }

    private void indexReverseCallers() {
        callersByMethod.clear();
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey());
            if (caller == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            for (MethodReference.Handle calleeRaw : entry.getValue()) {
                MethodReference.Handle callee = canonical(calleeRaw);
                if (callee == null) {
                    continue;
                }
                callersByMethod.computeIfAbsent(callee, k -> new LinkedHashSet<>()).add(caller);
            }
        }
    }

    private ReflectionHints buildHints(MethodReference.Handle caller,
                                       Set<String> strings,
                                       boolean neighborDerived) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> classHints = new LinkedHashSet<>();
        LinkedHashSet<String> methodHints = new LinkedHashSet<>();
        LinkedHashSet<String> methodDescHints = new LinkedHashSet<>();
        LinkedHashSet<String> ctorDescHints = new LinkedHashSet<>();
        LinkedHashSet<String> fragments = new LinkedHashSet<>();
        LinkedHashSet<String> simpleClassNames = new LinkedHashSet<>();

        for (String raw : strings) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String classCandidate = normalizeClassHint(raw);
            if (classCandidate != null && methodsByClass.containsKey(classCandidate)) {
                classHints.add(classCandidate);
            }

            MethodSignatureHint sig = parseMethodSignatureHint(raw);
            if (sig != null) {
                if (sig.methodName != null && knownMethodNames.contains(sig.methodName)) {
                    methodHints.add(sig.methodName);
                }
                if (sig.descriptor != null) {
                    if (sig.constructor) {
                        ctorDescHints.add(sig.descriptor);
                    } else {
                        methodDescHints.add(sig.descriptor);
                    }
                }
            }

            String methodCandidate = normalizeMethodHint(raw);
            if (methodCandidate != null) {
                if (knownMethodNames.contains(methodCandidate)) {
                    methodHints.add(methodCandidate);
                }
                String lower = methodCandidate.toLowerCase(Locale.ROOT);
                if (knownMethodNames.contains(lower)) {
                    methodHints.add(lower);
                }
            }

            String descCandidate = normalizeMethodDesc(raw);
            if (descCandidate != null) {
                if (looksConstructorText(raw)) {
                    ctorDescHints.add(descCandidate);
                } else {
                    methodDescHints.add(descCandidate);
                }
            }

            String fragment = normalizeFragment(raw);
            if (fragment != null) {
                fragments.add(fragment);
            }

            String simpleClassName = normalizeSimpleClassName(raw);
            if (simpleClassName != null) {
                simpleClassNames.add(simpleClassName);
            }
        }

        if (!simpleClassNames.isEmpty()) {
            classHints.addAll(expandClassHintsBySimpleNames(caller, simpleClassNames));
        }
        if (!fragments.isEmpty()) {
            classHints.addAll(expandClassHintsByFragments(fragments));
        }

        trim(classHints, MAX_CLASS_HINTS);
        trim(methodHints, MAX_METHOD_HINTS);
        trim(methodDescHints, MAX_DESC_HINTS);
        trim(ctorDescHints, MAX_DESC_HINTS);

        if (classHints.isEmpty()) {
            return null;
        }
        return new ReflectionHints(classHints, methodHints, methodDescHints, ctorDescHints, neighborDerived);
    }

    private Set<String> expandClassHintsBySimpleNames(MethodReference.Handle caller,
                                                       Set<String> simpleNames) {
        if (simpleNames == null || simpleNames.isEmpty() || knownClasses.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String callerClass = ownerName(caller);
        String callerPkg = packageName(callerClass);

        if (callerPkg != null && !callerPkg.isEmpty()) {
            for (String simple : simpleNames) {
                if (simple == null || simple.isEmpty()) {
                    continue;
                }
                String scoped = callerPkg + "/" + simple;
                if (methodsByClass.containsKey(scoped)) {
                    out.add(scoped);
                    if (out.size() >= MAX_CLASS_HINTS) {
                        return out;
                    }
                }
            }
        }

        for (String className : knownClasses) {
            if (className == null || className.isEmpty()) {
                continue;
            }
            for (String simple : simpleNames) {
                if (simple == null || simple.isEmpty()) {
                    continue;
                }
                if (className.endsWith("/" + simple) || className.equals(simple)) {
                    out.add(className);
                    break;
                }
            }
            if (out.size() >= MAX_CLASS_HINTS) {
                break;
            }
        }
        return out;
    }

    private Set<String> expandClassHintsByFragments(Set<String> fragments) {
        if (fragments == null || fragments.isEmpty() || knownClasses.isEmpty()) {
            return Collections.emptySet();
        }
        ArrayList<String> strong = new ArrayList<>();
        ArrayList<String> weak = new ArrayList<>();
        for (String fragment : fragments) {
            if (fragment == null || fragment.length() < 3) {
                continue;
            }
            if (fragment.indexOf('/') >= 0 || fragment.indexOf('.') >= 0) {
                strong.add(fragment);
            } else {
                weak.add(fragment);
            }
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String className : knownClasses) {
            if (className == null || className.isEmpty()) {
                continue;
            }
            String slash = className.toLowerCase(Locale.ROOT);
            String dot = slash.replace('/', '.');
            if (!matchesStrongFragments(strong, slash, dot)) {
                continue;
            }
            if (!matchesWeakFragments(weak, slash, dot)) {
                continue;
            }
            out.add(className);
            if (out.size() >= MAX_CLASS_HINTS) {
                break;
            }
        }
        return out;
    }

    private static boolean matchesStrongFragments(List<String> strong, String slash, String dot) {
        if (strong == null || strong.isEmpty()) {
            return true;
        }
        int matched = 0;
        for (String fragment : strong) {
            if (fragment == null || fragment.isEmpty()) {
                continue;
            }
            String slashFragment = fragment.replace('.', '/');
            String dotFragment = fragment.replace('/', '.');
            if (slash.contains(slashFragment) || dot.contains(dotFragment)) {
                matched++;
            }
        }
        return matched > 0;
    }

    private static boolean matchesWeakFragments(List<String> weak, String slash, String dot) {
        if (weak == null || weak.isEmpty()) {
            return true;
        }
        int matched = 0;
        String longest = null;
        for (String fragment : weak) {
            if (fragment == null || fragment.isEmpty()) {
                continue;
            }
            if (slash.contains(fragment) || dot.contains(fragment)) {
                matched++;
                if (longest == null || fragment.length() > longest.length()) {
                    longest = fragment;
                }
            }
        }
        if (matched >= 2) {
            return true;
        }
        if (matched == 1 && weak.size() == 1 && longest != null) {
            return slash.endsWith("/" + longest) || dot.endsWith("." + longest) || slash.endsWith(longest);
        }
        return false;
    }

    private static MethodSignatureHint parseMethodSignatureHint(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int lp = value.indexOf('(');
        int rp = value.indexOf(')', lp + 1);
        if (lp <= 0 || rp <= lp) {
            return null;
        }

        String before = value.substring(0, lp);
        int sep = Math.max(before.lastIndexOf('.'), Math.max(before.lastIndexOf('/'), before.lastIndexOf('#')));
        if (sep >= 0 && sep + 1 < before.length()) {
            before = before.substring(sep + 1);
        }
        String methodName = normalizeMethodHint(before);
        if (methodName == null) {
            return null;
        }

        String desc = normalizeMethodDesc(value.substring(lp));
        boolean ctor = "<init>".equals(methodName) || looksConstructorText(value);
        return new MethodSignatureHint(methodName, desc, ctor);
    }

    private static String normalizeClassHint(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        int sharp = value.indexOf('#');
        if (sharp > 0) {
            value = value.substring(0, sharp);
        }
        int sep = value.indexOf("::");
        if (sep > 0) {
            value = value.substring(0, sep);
        }
        int lp = value.indexOf('(');
        if (lp > 0) {
            value = value.substring(0, lp);
        }

        if (value.startsWith("[")) {
            try {
                Type t = Type.getType(value);
                Type elem = t;
                while (elem != null && elem.getSort() == Type.ARRAY) {
                    elem = elem.getElementType();
                }
                if (elem != null && elem.getSort() == Type.OBJECT) {
                    value = elem.getInternalName();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }
        }

        if (value.startsWith("L") && value.endsWith(";") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        value = value.replace('.', '/');
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty() || value.indexOf(' ') >= 0 || value.charAt(0) == '[') {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '/' || c == '_' || c == '$') {
                continue;
            }
            return null;
        }
        return value;
    }

    private static String normalizeMethodHint(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 96) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && !Character.isJavaIdentifierPart(value.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isJavaIdentifierPart(value.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return null;
        }
        String candidate = value.substring(start, end);
        if (!Character.isJavaIdentifierStart(candidate.charAt(0))) {
            return null;
        }
        for (int i = 1; i < candidate.length(); i++) {
            if (!Character.isJavaIdentifierPart(candidate.charAt(i))) {
                return null;
            }
        }
        return candidate;
    }

    private static String normalizeMethodDesc(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int idx = value.indexOf('(');
        if (idx < 0) {
            return null;
        }
        value = value.substring(idx).replace(" ", "");
        if (!value.startsWith("(") || !value.contains(")")) {
            return null;
        }
        int right = value.indexOf(')');
        if (right < 0 || right >= value.length() - 1) {
            return null;
        }
        try {
            Type.getMethodType(value);
            return value;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String normalizeFragment(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '$') {
                sb.append(Character.toLowerCase(c));
            }
        }
        if (sb.length() < 3) {
            return null;
        }
        return sb.toString();
    }

    private static String normalizeSimpleClassName(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int lp = value.indexOf('(');
        if (lp > 0) {
            value = value.substring(0, lp);
        }
        int sep = Math.max(value.lastIndexOf('.'), Math.max(value.lastIndexOf('/'), value.lastIndexOf('#')));
        if (sep >= 0 && sep + 1 < value.length()) {
            value = value.substring(sep + 1);
        }
        int start = 0;
        int end = value.length();
        while (start < end && !Character.isJavaIdentifierPart(value.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isJavaIdentifierPart(value.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return null;
        }
        String candidate = value.substring(start, end);
        if (candidate.isEmpty() || !Character.isUpperCase(candidate.charAt(0))) {
            return null;
        }
        for (int i = 1; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c == '$') {
                continue;
            }
            return null;
        }
        return candidate;
    }

    private static boolean looksConstructorText(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("<init>") || lower.contains("constructor") || lower.contains("newinstance");
    }

    private static String ownerName(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null) {
            return null;
        }
        return method.getClassReference().getName();
    }

    private static String packageName(String owner) {
        if (owner == null || owner.isEmpty()) {
            return null;
        }
        int idx = owner.lastIndexOf('/');
        if (idx <= 0) {
            return "";
        }
        return owner.substring(0, idx);
    }

    private static boolean addPool(LinkedHashSet<String> out,
                                   Set<String> source,
                                   int max) {
        if (out == null || source == null || source.isEmpty() || max <= 0) {
            return false;
        }
        boolean changed = false;
        for (String value : source) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (out.add(value.trim())) {
                changed = true;
                if (out.size() >= max) {
                    break;
                }
            }
        }
        return changed;
    }

    private static <T> void trim(LinkedHashSet<T> set, int limit) {
        if (set == null || set.size() <= limit) {
            return;
        }
        ArrayList<T> items = new ArrayList<>(set);
        set.clear();
        int keep = Math.min(limit, items.size());
        for (int i = 0; i < keep; i++) {
            set.add(items.get(i));
        }
    }

    private void mergeStringPool(Map<MethodReference.Handle, LinkedHashSet<String>> out,
                                 Map<MethodReference.Handle, List<String>> source) {
        if (out == null || source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, List<String>> entry : source.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey());
            List<String> values = entry.getValue();
            if (caller == null || values == null || values.isEmpty()) {
                continue;
            }
            LinkedHashSet<String> pool = out.computeIfAbsent(caller, k -> new LinkedHashSet<>());
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    pool.add(value.trim());
                }
            }
        }
    }

    private MethodReference.Handle canonical(MethodReference.Handle handle) {
        if (handle == null || handle.getClassReference() == null || handle.getClassReference().getName() == null
                || handle.getName() == null || handle.getDesc() == null
                || ctx == null || ctx.methodMap == null) {
            return null;
        }
        MethodReference probe = ctx.methodMap.get(new MethodReference.Handle(
                new ClassReference.Handle(handle.getClassReference().getName()),
                handle.getName(),
                handle.getDesc()));
        int jarId = probe == null || probe.getJarId() == null ? -1 : probe.getJarId();
        return new MethodReference.Handle(
                new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                handle.getName(),
                handle.getDesc()
        );
    }

    private static int readInt(String key, int def, int min, int max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        } catch (Exception ex) {
            return def;
        }
    }

    private static String methodKey(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null || method.getClassReference().getName() == null) {
            return "";
        }
        return method.getClassReference().getName() + "#" + method.getName() + method.getDesc();
    }

    private static final class ReflectionHints {
        private final Set<String> classHints;
        private final Set<String> methodHints;
        private final Set<String> methodDescHints;
        private final Set<String> ctorDescHints;
        private final boolean neighborDerived;

        private ReflectionHints(Set<String> classHints,
                                Set<String> methodHints,
                                Set<String> methodDescHints,
                                Set<String> ctorDescHints,
                                boolean neighborDerived) {
            this.classHints = classHints == null ? Collections.emptySet() : classHints;
            this.methodHints = methodHints == null ? Collections.emptySet() : methodHints;
            this.methodDescHints = methodDescHints == null ? Collections.emptySet() : methodDescHints;
            this.ctorDescHints = ctorDescHints == null ? Collections.emptySet() : ctorDescHints;
            this.neighborDerived = neighborDerived;
        }
    }

    private static final class ReflectionTrigger {
        private boolean classLookup;
        private boolean methodLookup;
        private boolean constructorLookup;
        private boolean methodInvoke;
        private boolean constructorInvoke;
        private boolean methodHandle;
        private String loaderReason;

        private boolean hasAny() {
            return classLookup || methodLookup || constructorLookup
                    || methodInvoke || constructorInvoke || methodHandle;
        }

        private boolean needsConstructors() {
            return classLookup || constructorLookup || constructorInvoke;
        }

        private boolean needsMethodCalls() {
            return methodLookup || methodInvoke || methodHandle;
        }
    }

    private static final class HintPool {
        private final Set<String> values;
        private final boolean neighborDerived;

        private HintPool(Set<String> values, boolean neighborDerived) {
            this.values = values == null ? Collections.emptySet() : values;
            this.neighborDerived = neighborDerived;
        }
    }

    private static final class NeighborNode {
        private final MethodReference.Handle method;
        private final int depth;

        private NeighborNode(MethodReference.Handle method, int depth) {
            this.method = method;
            this.depth = depth;
        }
    }

    private static final class MethodSignatureHint {
        private final String methodName;
        private final String descriptor;
        private final boolean constructor;

        private MethodSignatureHint(String methodName,
                                    String descriptor,
                                    boolean constructor) {
            this.methodName = methodName;
            this.descriptor = descriptor;
            this.constructor = constructor;
        }
    }
}
