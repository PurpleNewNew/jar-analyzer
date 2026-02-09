/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GetterSetterResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, GetterSetterSummary> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISS = ConcurrentHashMap.newKeySet();
    private static final Set<String> SCANNED = ConcurrentHashMap.newKeySet();

    private GetterSetterResolver() {
    }

    public static GetterSetterSummary resolve(String owner, String name, String desc) {
        if (owner == null || name == null || desc == null) {
            return null;
        }
        String key = owner + "#" + name + desc;
        GetterSetterSummary cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISS.contains(key)) {
            return null;
        }
        if (SCANNED.contains(owner)) {
            MISS.add(key);
            return null;
        }
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null) {
            MISS.add(key);
            return null;
        }
        String absPath = engine.getAbsPath(owner);
        if (absPath == null || absPath.trim().isEmpty()) {
            MISS.add(key);
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(absPath));
            String scanned = analyzeAll(bytes);
            if (scanned != null) {
                SCANNED.add(scanned);
                if (!scanned.equals(owner)) {
                    SCANNED.add(owner);
                }
                GetterSetterSummary summary = CACHE.get(key);
                if (summary != null) {
                    return summary;
                }
            }
        } catch (Exception ex) {
            logger.warn("getter/setter resolve failed: {}", ex.toString());
        }
        MISS.add(key);
        return null;
    }

    private static String analyzeAll(byte[] bytes) {
        try {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, Const.GlobalASMOptions);
            if (cn.methods == null) {
                return null;
            }
            for (MethodNode mn : cn.methods) {
                if (mn == null) {
                    continue;
                }
                String methodKey = buildMethodKey(cn.name, mn);
                if (mn.instructions == null) {
                    MISS.add(methodKey);
                    continue;
                }
                GetterSetterSummary summary = buildSummary(cn.name, mn);
                if (summary != null) {
                    CACHE.put(methodKey, summary);
                } else {
                    MISS.add(methodKey);
                }
            }
            return cn.name;
        } catch (Exception ex) {
            logger.warn("getter/setter analyze failed: {}", ex.toString());
        }
        return null;
    }

    private static GetterSetterSummary buildSummary(String owner, MethodNode mn) {
        List<AbstractInsnNode> ops = collectOps(mn);
        if (ops.isEmpty()) {
            return null;
        }
        GetterSetterSummary getter = tryGetter(owner, ops);
        if (getter != null) {
            return getter;
        }
        return trySetter(owner, ops);
    }

    private static GetterSetterSummary tryGetter(String owner, List<AbstractInsnNode> ops) {
        if (ops.size() == 2) {
            if (ops.get(0) instanceof FieldInsnNode && ops.get(1).getOpcode() > 0) {
                FieldInsnNode f = (FieldInsnNode) ops.get(0);
                if (f.getOpcode() == Opcodes.GETSTATIC && owner.equals(f.owner)) {
                    int retOp = ops.get(1).getOpcode();
                    if (retOp == expectedReturnOpcode(f.desc)) {
                        return new GetterSetterSummary(
                                GetterSetterSummary.Kind.GETTER,
                                f.owner, f.name, f.desc,
                                true, -1);
                    }
                }
            }
            return null;
        }
        if (ops.size() != 3) {
            return null;
        }
        if (!(ops.get(0) instanceof VarInsnNode) || !(ops.get(1) instanceof FieldInsnNode)) {
            return null;
        }
        VarInsnNode v0 = (VarInsnNode) ops.get(0);
        FieldInsnNode f = (FieldInsnNode) ops.get(1);
        if (v0.getOpcode() != Opcodes.ALOAD || v0.var != 0) {
            return null;
        }
        if (f.getOpcode() != Opcodes.GETFIELD || !owner.equals(f.owner)) {
            return null;
        }
        int retOp = ops.get(2).getOpcode();
        if (retOp != expectedReturnOpcode(f.desc)) {
            return null;
        }
        return new GetterSetterSummary(
                GetterSetterSummary.Kind.GETTER,
                f.owner, f.name, f.desc,
                false, -1);
    }

    private static GetterSetterSummary trySetter(String owner, List<AbstractInsnNode> ops) {
        if (ops.size() == 3) {
            if (!(ops.get(0) instanceof VarInsnNode) || !(ops.get(1) instanceof FieldInsnNode)) {
                return null;
            }
            VarInsnNode v = (VarInsnNode) ops.get(0);
            FieldInsnNode f = (FieldInsnNode) ops.get(1);
            if (f.getOpcode() != Opcodes.PUTSTATIC || !owner.equals(f.owner)) {
                return null;
            }
            if (ops.get(2).getOpcode() != Opcodes.RETURN) {
                return null;
            }
            if (!isLoadForType(v, Type.getType(f.desc), 0)) {
                return null;
            }
            return new GetterSetterSummary(
                    GetterSetterSummary.Kind.SETTER,
                    f.owner, f.name, f.desc,
                    true, 0);
        }
        if (ops.size() != 4) {
            return null;
        }
        if (!(ops.get(0) instanceof VarInsnNode)
                || !(ops.get(1) instanceof VarInsnNode)
                || !(ops.get(2) instanceof FieldInsnNode)) {
            return null;
        }
        VarInsnNode thisVar = (VarInsnNode) ops.get(0);
        VarInsnNode argVar = (VarInsnNode) ops.get(1);
        FieldInsnNode f = (FieldInsnNode) ops.get(2);
        if (thisVar.getOpcode() != Opcodes.ALOAD || thisVar.var != 0) {
            return null;
        }
        if (f.getOpcode() != Opcodes.PUTFIELD || !owner.equals(f.owner)) {
            return null;
        }
        if (ops.get(3).getOpcode() != Opcodes.RETURN) {
            return null;
        }
        if (!isLoadForType(argVar, Type.getType(f.desc), 1)) {
            return null;
        }
        return new GetterSetterSummary(
                GetterSetterSummary.Kind.SETTER,
                f.owner, f.name, f.desc,
                false, 0);
    }

    private static List<AbstractInsnNode> collectOps(MethodNode mn) {
        List<AbstractInsnNode> ops = new ArrayList<>();
        for (AbstractInsnNode insn = mn.instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode < 0 || opcode == Opcodes.NOP) {
                continue;
            }
            ops.add(insn);
        }
        return ops;
    }

    private static String buildMethodKey(String owner, MethodNode mn) {
        return owner + "#" + mn.name + mn.desc;
    }

    private static int expectedReturnOpcode(String fieldDesc) {
        Type t = Type.getType(fieldDesc);
        switch (t.getSort()) {
            case Type.VOID:
                return Opcodes.RETURN;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.IRETURN;
            case Type.FLOAT:
                return Opcodes.FRETURN;
            case Type.LONG:
                return Opcodes.LRETURN;
            case Type.DOUBLE:
                return Opcodes.DRETURN;
            case Type.ARRAY:
            case Type.OBJECT:
            default:
                return Opcodes.ARETURN;
        }
    }

    private static boolean isLoadForType(VarInsnNode insn, Type type, int index) {
        if (insn == null || insn.var != index) {
            return false;
        }
        int opcode = insn.getOpcode();
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return opcode == Opcodes.ILOAD;
            case Type.FLOAT:
                return opcode == Opcodes.FLOAD;
            case Type.LONG:
                return opcode == Opcodes.LLOAD;
            case Type.DOUBLE:
                return opcode == Opcodes.DLOAD;
            case Type.ARRAY:
            case Type.OBJECT:
            default:
                return opcode == Opcodes.ALOAD;
        }
    }
}
