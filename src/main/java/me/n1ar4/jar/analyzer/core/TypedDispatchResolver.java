/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TypedDispatchResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_TARGETS_PER_CALL = 200;

    private TypedDispatchResolver() {
    }

    public static int expandWithTypes(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                      Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                      Map<MethodReference.Handle, MethodReference> methodMap,
                                      Map<ClassReference.Handle, ClassReference> classMap,
                                      InheritanceMap inheritanceMap,
                                      List<CallSiteEntity> callSites) {
        if (methodCalls == null || methodMap == null || classMap == null
                || callSites == null || callSites.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            Integer opcode = site.getOpCode();
            if (opcode == null || opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
                continue;
            }
            String receiverType = normalizeReceiverType(site.getReceiverType());
            if (receiverType == null) {
                continue;
            }
            String declaredOwner = normalizeReceiverType(site.getCalleeOwner());
            if (declaredOwner != null && !isAssignable(receiverType, declaredOwner, inheritanceMap)) {
                continue;
            }
            Set<ClassReference.Handle> candidates =
                    collectCandidates(receiverType, classMap, inheritanceMap);
            if (candidates.isEmpty()) {
                continue;
            }
            Set<MethodReference.Handle> targets = resolveTargets(candidates,
                    site.getCalleeMethodName(), site.getCalleeMethodDesc(),
                    methodMap, classMap, inheritanceMap);
            if (targets.isEmpty()) {
                continue;
            }
            if (targets.size() > MAX_TARGETS_PER_CALL) {
                logger.debug("typed dispatch targets too many: {} -> {} ({} / {})",
                        site.getCallerMethodName(), site.getCalleeMethodName(),
                        targets.size(), MAX_TARGETS_PER_CALL);
                continue;
            }
            MethodReference.Handle caller = new MethodReference.Handle(
                    new ClassReference.Handle(site.getCallerClassName()),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc());
            HashSet<MethodReference.Handle> callees =
                    methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
            String reason = "typed:" + receiverType;
            for (MethodReference.Handle target : targets) {
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(), opcode, target.getName(), target.getDesc());
                if (MethodCallUtils.addCallee(callees, callTarget)) {
                    added++;
                    MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callTarget),
                            MethodCallMeta.TYPE_DISPATCH, MethodCallMeta.CONF_HIGH, reason);
                }
            }
        }
        return added;
    }

    private static Set<ClassReference.Handle> collectCandidates(String receiverType,
                                                                Map<ClassReference.Handle, ClassReference> classMap,
                                                                InheritanceMap inheritanceMap) {
        Set<ClassReference.Handle> out = new HashSet<>();
        if (receiverType == null || classMap == null) {
            return out;
        }
        ClassReference.Handle handle = new ClassReference.Handle(receiverType);
        ClassReference ref = classMap.get(handle);
        if (ref == null) {
            return out;
        }
        boolean isFinal = (ref.getAccess() & Opcodes.ACC_FINAL) != 0;
        out.add(handle);
        if (isFinal) {
            return out;
        }
        if (inheritanceMap == null) {
            return out;
        }
        Set<ClassReference.Handle> subs = inheritanceMap.getSubClasses(handle);
        if (subs != null && !subs.isEmpty()) {
            out.addAll(subs);
        }
        return out;
    }

    private static Set<MethodReference.Handle> resolveTargets(Set<ClassReference.Handle> candidates,
                                                              String methodName,
                                                              String methodDesc,
                                                              Map<MethodReference.Handle, MethodReference> methodMap,
                                                              Map<ClassReference.Handle, ClassReference> classMap,
                                                              InheritanceMap inheritanceMap) {
        Set<MethodReference.Handle> targets = new HashSet<>();
        if (candidates == null || candidates.isEmpty() || methodName == null || methodDesc == null) {
            return targets;
        }
        for (ClassReference.Handle clazz : candidates) {
            MethodReference.Handle resolved = resolveInHierarchy(clazz, methodName, methodDesc, methodMap, classMap);
            if (resolved != null) {
                targets.add(resolved);
            } else {
                addInterfaceDefaults(clazz, methodName, methodDesc, methodMap, inheritanceMap, targets);
            }
        }
        return targets;
    }

    private static MethodReference.Handle resolveInHierarchy(ClassReference.Handle clazz,
                                                             String methodName,
                                                             String methodDesc,
                                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                                             Map<ClassReference.Handle, ClassReference> classMap) {
        ClassReference.Handle cur = clazz;
        int depth = 0;
        while (cur != null && depth < 20) {
            MethodReference.Handle handle = new MethodReference.Handle(cur, methodName, methodDesc);
            if (methodMap.containsKey(handle)) {
                return handle;
            }
            ClassReference ref = classMap.get(cur);
            if (ref == null) {
                return null;
            }
            String superName = ref.getSuperClass();
            if (superName == null || superName.trim().isEmpty()) {
                return null;
            }
            cur = new ClassReference.Handle(superName);
            depth++;
        }
        return null;
    }

    private static void addInterfaceDefaults(ClassReference.Handle clazz,
                                             String methodName,
                                             String methodDesc,
                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                             InheritanceMap inheritanceMap,
                                             Set<MethodReference.Handle> targets) {
        if (inheritanceMap == null || clazz == null) {
            return;
        }
        Set<ClassReference.Handle> parents = inheritanceMap.getSuperClasses(clazz);
        if (parents == null || parents.isEmpty()) {
            return;
        }
        for (ClassReference.Handle parent : parents) {
            MethodReference.Handle handle = new MethodReference.Handle(parent, methodName, methodDesc);
            if (methodMap.containsKey(handle)) {
                targets.add(handle);
            }
        }
    }

    private static boolean isAssignable(String receiverType,
                                        String declaredOwner,
                                        InheritanceMap inheritanceMap) {
        if (receiverType == null || declaredOwner == null) {
            return true;
        }
        if (receiverType.equals(declaredOwner)) {
            return true;
        }
        if (inheritanceMap == null) {
            return true;
        }
        return inheritanceMap.isSubclassOf(new ClassReference.Handle(receiverType),
                new ClassReference.Handle(declaredOwner));
    }

    private static String normalizeReceiverType(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.startsWith("[")) {
            return null;
        }
        if (v.length() == 1) {
            return null;
        }
        if (v.startsWith("L") && v.endsWith(";")) {
            v = v.substring(1, v.length() - 1);
        }
        if (v.contains(".")) {
            v = v.replace('.', '/');
        }
        return v;
    }
}
