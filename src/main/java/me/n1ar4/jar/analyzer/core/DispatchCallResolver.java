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
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DispatchCallResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final int MIN_TARGETS_PER_CALL = 50;
    private static final int MAX_TARGETS_PER_CALL = 400;
    private static final int MAX_HIERARCHY_DEPTH = 32;

    private DispatchCallResolver() {
    }

    public static Set<ClassReference.Handle> collectInstantiatedClasses(Collection<ClassFileEntity> classFiles,
                                                                        InheritanceMap inheritanceMap) {
        if (classFiles == null || classFiles.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
        for (ClassFileEntity file : classFiles) {
            if (file == null) {
                continue;
            }
            byte[] bytes = file.getFile();
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            try {
                ClassReader cr = new ClassReader(bytes);
                ClassNode cn = new ClassNode();
                cr.accept(cn, Const.GlobalASMOptions);
                List<MethodNode> methods = cn.methods;
                if (methods == null || methods.isEmpty()) {
                    continue;
                }
                for (MethodNode mn : methods) {
                    if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                        continue;
                    }
                    for (int i = 0; i < mn.instructions.size(); i++) {
                        AbstractInsnNode insn = mn.instructions.get(i);
                        if (!(insn instanceof TypeInsnNode tin) || tin.getOpcode() != Opcodes.NEW) {
                            continue;
                        }
                        String desc = tin.desc;
                        if (desc == null || desc.isBlank()) {
                            continue;
                        }
                        ClassReference.Handle handle = inheritanceMap == null
                                ? new ClassReference.Handle(desc, file.getJarId())
                                : inheritanceMap.resolveClass(desc, file.getJarId());
                        if (handle == null) {
                            handle = new ClassReference.Handle(desc, file.getJarId());
                        }
                        out.add(handle);
                    }
                }
            } catch (Exception ex) {
                logger.debug("collect instantiated classes error: {}", ex.toString());
            }
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    public static int expandVirtualCalls(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                         Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                         Map<MethodReference.Handle, MethodReference> methodMap,
                                         Map<ClassReference.Handle, ClassReference> classMap,
                                         InheritanceMap inheritanceMap,
                                         Set<ClassReference.Handle> instantiatedClasses) {
        if (methodCalls == null || methodCalls.isEmpty()
                || methodMap == null || methodMap.isEmpty()
                || classMap == null || classMap.isEmpty()
                || inheritanceMap == null) {
            return 0;
        }
        boolean hasRta = instantiatedClasses != null && !instantiatedClasses.isEmpty();
        int added = 0;
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            List<MethodReference.Handle> snapshot = new ArrayList<>(callees);
            for (MethodReference.Handle callee : snapshot) {
                if (callee == null) {
                    continue;
                }
                MethodCallMeta meta = MethodCallMeta.resolve(methodCallMeta, caller, callee);
                int opcode = meta == null ? -1 : meta.getBestOpcode();
                if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
                    continue;
                }
                MethodReference declared = methodMap.get(callee);
                if (declared == null || declared.isStatic()) {
                    continue;
                }
                ClassReference owner = classMap.get(declared.getClassReference());
                if (isFinalOrPrivate(declared, owner)) {
                    continue;
                }
                Set<ClassReference.Handle> candidates = collectCandidateClasses(declared.getClassReference(), inheritanceMap);
                if (candidates.isEmpty()) {
                    continue;
                }
                boolean usedRta = false;
                Set<ClassReference.Handle> filtered = candidates;
                if (hasRta) {
                    LinkedHashSet<ClassReference.Handle> rta = new LinkedHashSet<>();
                    for (ClassReference.Handle handle : candidates) {
                        if (instantiatedClasses.contains(handle)) {
                            rta.add(handle);
                        }
                    }
                    if (!rta.isEmpty()) {
                        filtered = rta;
                        usedRta = true;
                    }
                }
                Set<MethodReference.Handle> targets = resolveTargets(
                        filtered,
                        callee.getName(),
                        callee.getDesc(),
                        methodMap,
                        inheritanceMap
                );
                if (targets.isEmpty()) {
                    continue;
                }
                int limit = resolveTargetLimit(classMap.size(), usedRta);
                if (targets.size() > limit) {
                    logger.info("dispatch targets too many: {} -> {} ({} / {})",
                            caller.getName(),
                            callee.getName(),
                            targets.size(),
                            limit);
                    continue;
                }
                String reason = buildDispatchReason(opcode, usedRta);
                String conf = usedRta ? MethodCallMeta.CONF_MEDIUM : MethodCallMeta.CONF_LOW;
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
                            conf,
                            reason,
                            opcode
                    );
                    if (inserted) {
                        added++;
                    }
                }
            }
        }
        return added;
    }

    private static int resolveTargetLimit(int classCount, boolean usedRta) {
        int scaled = (int) Math.sqrt(Math.max(1, classCount));
        int limit = MIN_TARGETS_PER_CALL + (scaled * 2);
        if (!usedRta) {
            limit = (int) (limit * 0.6);
        }
        if (limit < MIN_TARGETS_PER_CALL) {
            return MIN_TARGETS_PER_CALL;
        }
        if (limit > MAX_TARGETS_PER_CALL) {
            return MAX_TARGETS_PER_CALL;
        }
        return limit;
    }

    private static boolean isFinalOrPrivate(MethodReference method, ClassReference owner) {
        if (method == null) {
            return false;
        }
        int access = method.getAccess();
        if ((access & Opcodes.ACC_PRIVATE) != 0 || (access & Opcodes.ACC_FINAL) != 0) {
            return true;
        }
        return owner != null && (owner.getAccess() & Opcodes.ACC_FINAL) != 0;
    }

    private static Set<ClassReference.Handle> collectCandidateClasses(ClassReference.Handle owner,
                                                                      InheritanceMap inheritanceMap) {
        if (owner == null || inheritanceMap == null) {
            return Set.of();
        }
        LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
        out.add(owner);
        Set<ClassReference.Handle> subs = inheritanceMap.getSubClasses(owner);
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
            MethodReference.Handle resolved = resolveInHierarchy(clazz, methodName, methodDesc, methodMap, inheritanceMap);
            if (resolved != null) {
                targets.add(resolved);
                continue;
            }
            addInterfaceDefaults(clazz, methodName, methodDesc, methodMap, inheritanceMap, targets);
        }
        return targets.isEmpty() ? Set.of() : targets;
    }

    static MethodReference.Handle resolveInHierarchy(ClassReference.Handle clazz,
                                                     String methodName,
                                                     String methodDesc,
                                                     Map<MethodReference.Handle, MethodReference> methodMap,
                                                     InheritanceMap inheritanceMap) {
        if (clazz == null || methodName == null || methodDesc == null) {
            return null;
        }
        Deque<ClassReference.Handle> queue = new ArrayDeque<>();
        Set<ClassReference.Handle> visited = new LinkedHashSet<>();
        queue.add(clazz);
        int depth = 0;
        while (!queue.isEmpty() && depth++ < MAX_HIERARCHY_DEPTH) {
            ClassReference.Handle current = queue.removeFirst();
            if (current == null || !visited.add(current)) {
                continue;
            }
            MethodReference.Handle handle = new MethodReference.Handle(current, methodName, methodDesc);
            if (methodMap.containsKey(handle)) {
                return handle;
            }
            for (ClassReference.Handle parent : inheritanceMap.getDirectParents(current)) {
                if (parent != null && !visited.contains(parent)) {
                    queue.addLast(parent);
                }
            }
        }
        return null;
    }

    static void addInterfaceDefaults(ClassReference.Handle clazz,
                                     String methodName,
                                     String methodDesc,
                                     Map<MethodReference.Handle, MethodReference> methodMap,
                                     InheritanceMap inheritanceMap,
                                     Set<MethodReference.Handle> targets) {
        if (clazz == null || inheritanceMap == null || targets == null) {
            return;
        }
        for (ClassReference.Handle parent : inheritanceMap.getSuperClasses(clazz)) {
            if (parent == null) {
                continue;
            }
            MethodReference.Handle handle = new MethodReference.Handle(parent, methodName, methodDesc);
            if (methodMap.containsKey(handle)) {
                targets.add(handle);
            }
        }
    }

    private static String buildDispatchReason(int opcode, boolean usedRta) {
        StringBuilder sb = new StringBuilder();
        sb.append(usedRta ? "rta" : "cha");
        sb.append(opcode == Opcodes.INVOKEINTERFACE ? "_interface" : "_virtual");
        return sb.toString();
    }
}
