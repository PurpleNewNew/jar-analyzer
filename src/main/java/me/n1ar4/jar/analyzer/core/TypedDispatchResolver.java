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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TypedDispatchResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_TARGETS_PER_CALL = 200;

    private TypedDispatchResolver() {
    }

    public static int expandWithTypes(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                      Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                      Map<MethodReference.Handle, MethodReference> methodMap,
                                      Map<ClassReference.Handle, ClassReference> classMap,
                                      InheritanceMap inheritanceMap,
                                      List<CallSiteEntity> callSites) {
        if (methodCalls == null || methodMap == null || classMap == null
                || inheritanceMap == null || callSites == null || callSites.isEmpty()) {
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
            ClassReference.Handle receiverHandle = inheritanceMap.resolveClass(receiverType, site.getJarId());
            if (receiverHandle == null) {
                continue;
            }
            String declaredOwner = normalizeReceiverType(site.getCalleeOwner());
            if (declaredOwner != null && !isAssignable(receiverHandle, declaredOwner, inheritanceMap)) {
                continue;
            }
            Set<ClassReference.Handle> candidates = collectCandidates(receiverHandle, classMap, inheritanceMap);
            if (candidates.isEmpty()) {
                continue;
            }
            Set<MethodReference.Handle> targets = resolveTargets(
                    candidates,
                    site.getCalleeMethodName(),
                    site.getCalleeMethodDesc(),
                    methodMap,
                    inheritanceMap
            );
            if (targets.isEmpty()) {
                continue;
            }
            if (targets.size() > MAX_TARGETS_PER_CALL) {
                logger.debug("typed dispatch targets too many: {} -> {} ({} / {})",
                        site.getCallerMethodName(),
                        site.getCalleeMethodName(),
                        targets.size(),
                        MAX_TARGETS_PER_CALL);
                continue;
            }
            MethodReference.Handle caller = new MethodReference.Handle(
                    new ClassReference.Handle(site.getCallerClassName(), site.getJarId()),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc()
            );
            HashSet<MethodReference.Handle> callees = methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
            String reason = "typed:" + receiverType;
            for (MethodReference.Handle target : targets) {
                if (target == null) {
                    continue;
                }
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        opcode,
                        target.getName(),
                        target.getDesc()
                );
                boolean inserted = MethodCallUtils.addCallee(callees, callTarget);
                MethodCallMeta.record(
                        methodCallMeta,
                        MethodCallKey.of(caller, callTarget),
                        MethodCallMeta.TYPE_DISPATCH,
                        MethodCallMeta.CONF_HIGH,
                        reason,
                        opcode
                );
                if (inserted) {
                    added++;
                }
            }
        }
        return added;
    }

    private static Set<ClassReference.Handle> collectCandidates(ClassReference.Handle receiverHandle,
                                                                Map<ClassReference.Handle, ClassReference> classMap,
                                                                InheritanceMap inheritanceMap) {
        if (receiverHandle == null || classMap == null) {
            return Set.of();
        }
        LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
        out.add(receiverHandle);
        ClassReference ref = classMap.get(receiverHandle);
        if (ref == null || (ref.getAccess() & Opcodes.ACC_FINAL) != 0 || inheritanceMap == null) {
            return out;
        }
        Set<ClassReference.Handle> subs = inheritanceMap.getSubClasses(receiverHandle);
        if (!subs.isEmpty()) {
            out.addAll(subs);
        }
        return out;
    }

    private static Set<MethodReference.Handle> resolveTargets(Set<ClassReference.Handle> candidates,
                                                              String methodName,
                                                              String methodDesc,
                                                              Map<MethodReference.Handle, MethodReference> methodMap,
                                                              InheritanceMap inheritanceMap) {
        if (candidates == null || candidates.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<MethodReference.Handle> targets = new LinkedHashSet<>();
        for (ClassReference.Handle clazz : candidates) {
            MethodReference.Handle resolved = DispatchCallResolver.resolveInHierarchy(
                    clazz, methodName, methodDesc, methodMap, inheritanceMap
            );
            if (resolved != null) {
                targets.add(resolved);
                continue;
            }
            DispatchCallResolver.addInterfaceDefaults(
                    clazz, methodName, methodDesc, methodMap, inheritanceMap, targets
            );
        }
        return targets.isEmpty() ? Set.of() : targets;
    }

    private static boolean isAssignable(ClassReference.Handle receiverHandle,
                                        String declaredOwner,
                                        InheritanceMap inheritanceMap) {
        if (receiverHandle == null || declaredOwner == null) {
            return true;
        }
        ClassReference.Handle ownerHandle = inheritanceMap.resolveClass(declaredOwner, receiverHandle.getJarId());
        if (ownerHandle == null) {
            return true;
        }
        return inheritanceMap.isSubclassOf(receiverHandle, ownerHandle);
    }

    private static String normalizeReceiverType(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.startsWith("[")) {
            return null;
        }
        if (normalized.length() == 1) {
            return null;
        }
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }
}
