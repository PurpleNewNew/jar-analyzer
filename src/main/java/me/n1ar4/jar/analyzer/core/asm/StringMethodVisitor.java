/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.asm;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StringMethodVisitor extends MethodVisitor {
    private final Map<MethodReference.Handle, List<String>> strMap;

    private MethodReference ownerHandle = null;

    public StringMethodVisitor(int api, MethodVisitor methodVisitor,
                               String owner, String methodName, String desc,
                               Map<MethodReference.Handle, List<String>> strMap,
                               Map<ClassReference.Handle, ClassReference> classMap,
                               Map<MethodReference.Handle, MethodReference> methodMap) {
        super(api, methodVisitor);
        this.strMap = strMap;
        ClassReference.Handle ch = new ClassReference.Handle(owner);
        if (classMap.get(ch) != null) {
            MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, desc));
            if (m != null) {
                this.ownerHandle = m;
            }
        }
    }

    @Override
    public void visitLdcInsn(Object o) {
        if (this.ownerHandle != null) {
            if (o instanceof String) {
                addHint((String) o);
            } else if (o instanceof Type) {
                Type type = (Type) o;
                if (type.getSort() == Type.OBJECT) {
                    addHint(type.getInternalName());
                } else if (type.getSort() == Type.ARRAY) {
                    addHint(type.getDescriptor());
                }
            }
        }
        super.visitLdcInsn(o);
    }

    @Override
    public void visitInvokeDynamicInsn(String name,
                                       String descriptor,
                                       Handle bootstrapMethodHandle,
                                       Object... bootstrapMethodArguments) {
        if (this.ownerHandle != null && bootstrapMethodHandle != null) {
            String bsmOwner = bootstrapMethodHandle.getOwner();
            String bsmName = bootstrapMethodHandle.getName();
            if ("java/lang/invoke/StringConcatFactory".equals(bsmOwner)
                    && ("makeConcat".equals(bsmName) || "makeConcatWithConstants".equals(bsmName))) {
                if (bootstrapMethodArguments != null) {
                    for (Object arg : bootstrapMethodArguments) {
                        if (arg instanceof String) {
                            collectConcatRecipe((String) arg);
                            continue;
                        }
                        if (arg instanceof Type) {
                            Type type = (Type) arg;
                            if (type.getSort() == Type.OBJECT) {
                                addHint(type.getInternalName());
                            } else if (type.getSort() == Type.ARRAY) {
                                addHint(type.getDescriptor());
                            }
                        }
                    }
                }
            }
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    private void collectConcatRecipe(String recipe) {
        if (recipe == null || recipe.trim().isEmpty()) {
            return;
        }
        StringBuilder token = new StringBuilder(recipe.length());
        for (int i = 0; i < recipe.length(); i++) {
            char c = recipe.charAt(i);
            if (c == '\u0001' || c == '\u0002') {
                flushToken(token);
                continue;
            }
            token.append(c);
        }
        flushToken(token);
    }

    private void flushToken(StringBuilder token) {
        if (token == null || token.length() == 0) {
            return;
        }
        String value = token.toString().trim();
        if (!value.isEmpty()) {
            addHint(value);
        }
        token.setLength(0);
    }

    private void addHint(String value) {
        if (this.ownerHandle == null || value == null) {
            return;
        }
        String hint = value.trim();
        if (hint.isEmpty()) {
            return;
        }
        List<String> mList = strMap.getOrDefault(this.ownerHandle.getHandle(), new ArrayList<>());
        if (!mList.contains(hint)) {
            mList.add(hint);
        }
        strMap.put(this.ownerHandle.getHandle(), mList);
    }
}
