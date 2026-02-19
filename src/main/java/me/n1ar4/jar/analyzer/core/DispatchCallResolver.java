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
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DispatchCallResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final int MIN_TARGETS_PER_CALL = 50;
    private static final int MAX_TARGETS_PER_CALL = 400;

    private DispatchCallResolver() {
    }

    public static Set<ClassReference.Handle> collectInstantiatedClasses(Set<ClassFileEntity> classFileList) {
        if (classFileList == null || classFileList.isEmpty()) {
            return new HashSet<>();
        }
        Set<ClassReference.Handle> out = new HashSet<>();
        for (ClassFileEntity file : classFileList) {
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
                if (cn.methods == null || cn.methods.isEmpty()) {
                    continue;
                }
                for (MethodNode mn : cn.methods) {
                    if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                        continue;
                    }
                    for (int i = 0; i < mn.instructions.size(); i++) {
                        AbstractInsnNode insn = mn.instructions.get(i);
                        if (!(insn instanceof TypeInsnNode)) {
                            continue;
                        }
                        TypeInsnNode tin = (TypeInsnNode) insn;
                        if (tin.getOpcode() == Opcodes.NEW && tin.desc != null && !tin.desc.isEmpty()) {
                            out.add(new ClassReference.Handle(tin.desc));
                        }
                    }
                }
            } catch (Exception ex) {
                logger.debug("collect instantiated classes error: {}", ex.toString());
            }
        }
        return out;
    }

    @CompatibilityCode(
            primary = "MethodCallMeta opcode on dispatch edges",
            reason = "Legacy call edges may only carry opcode on MethodReference.Handle; keep bridge while mixed edge formats coexist"
    )
    public static int expandVirtualCalls(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                         Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                         Map<MethodReference.Handle, MethodReference> methodMap,
                                         Map<ClassReference.Handle, ClassReference> classMap,
                                         InheritanceMap inheritanceMap,
                                         Set<ClassReference.Handle> instantiatedClasses) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return 0;
        }
        if (methodMap == null || methodMap.isEmpty() || classMap == null || classMap.isEmpty()) {
            return 0;
        }
        if (inheritanceMap == null) {
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
                int opcode = -1;
                if (methodCallMeta != null) {
                    MethodCallMeta meta = methodCallMeta.get(MethodCallKey.of(caller, callee));
                    if (meta != null) {
                        opcode = meta.getBestOpcode();
                    }
                }
                if (opcode <= 0) {
                    Integer legacy = callee.getOpcode();
                    opcode = legacy == null ? -1 : legacy;
                }
                if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
                    continue;
                }
                MethodReference declared = methodMap.get(callee);
                if (declared == null) {
                    continue;
                }
                if (declared.isStatic()) {
                    continue;
                }
                ClassReference owner = classMap.get(declared.getClassReference());
                if (owner == null) {
                    continue;
                }
                if (isFinalOrPrivate(declared, owner)) {
                    continue;
                }
                Set<ClassReference.Handle> candidates =
                        collectCandidateClasses(declared.getClassReference(), inheritanceMap);
                if (candidates.isEmpty()) {
                    continue;
                }
                boolean usedRta = false;
                Set<ClassReference.Handle> filtered = candidates;
                if (hasRta) {
                    Set<ClassReference.Handle> rta = new HashSet<>();
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
                Set<MethodReference.Handle> targets = resolveTargets(filtered, callee.getName(), callee.getDesc(),
                        methodMap, classMap, inheritanceMap);
                if (targets.isEmpty()) {
                    continue;
                }
                int limit = resolveTargetLimit(classMap.size(), usedRta);
                if (targets.size() > limit) {
                    logger.info("dispatch targets too many: {} -> {} ({} / {})",
                            caller.getName(), callee.getName(), targets.size(), limit);
                    continue;
                }
                String reason = buildDispatchReason(opcode, usedRta);
                String conf = usedRta ? MethodCallMeta.CONF_MEDIUM : MethodCallMeta.CONF_LOW;
                for (MethodReference.Handle target : targets) {
                    if (target == null) {
                        continue;
                    }
                    MethodReference.Handle callTarget = new MethodReference.Handle(
                            target.getClassReference(), opcode, target.getName(), target.getDesc());
                    if (MethodCallUtils.addCallee(callees, callTarget)) {
                        added++;
                        MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callTarget),
                                MethodCallMeta.TYPE_DISPATCH, conf, reason, opcode);
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
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return true;
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            return true;
        }
        if (owner != null && (owner.getAccess() & Opcodes.ACC_FINAL) != 0) {
            return true;
        }
        return false;
    }

    private static Set<ClassReference.Handle> collectCandidateClasses(ClassReference.Handle owner,
                                                                      InheritanceMap inheritanceMap) {
        Set<ClassReference.Handle> out = new HashSet<>();
        if (owner == null) {
            return out;
        }
        out.add(owner);
        Set<ClassReference.Handle> subs = inheritanceMap.getSubClasses(owner);
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
        if (candidates == null || candidates.isEmpty()) {
            return targets;
        }
        for (ClassReference.Handle clazz : candidates) {
            MethodReference.Handle resolved = resolveInHierarchy(clazz, methodName, methodDesc,
                    methodMap, classMap);
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

    private static String buildDispatchReason(int opcode, boolean usedRta) {
        StringBuilder sb = new StringBuilder();
        sb.append(usedRta ? "rta" : "cha");
        if (opcode == Opcodes.INVOKEINTERFACE) {
            sb.append("_interface");
        } else {
            sb.append("_virtual");
        }
        return sb.toString();
    }
}
