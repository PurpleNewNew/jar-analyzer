/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.analyze.spring.asm;

import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.analyze.spring.SpringConstant;
import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.starter.Const;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpringClassVisitor extends ClassVisitor {
    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final Map<MethodReference.Handle, MethodReference> methodMap;
    private final List<SpringController> controllers;
    private final int ownerJarId;
    private boolean isSpring;
    private SpringController currentController;
    private String name;
    private SpringPathAnnoAdapter pathAnnoAdapter = null;

    public SpringClassVisitor(List<SpringController> controllers,
                              Map<ClassReference.Handle, ClassReference> classMap,
                              Map<MethodReference.Handle, MethodReference> methodMap,
                              Integer ownerJarId,
                              ClassVisitor cv) {
        super(Const.ASMVersion, cv);
        this.methodMap = methodMap;
        this.controllers = controllers;
        this.classMap = classMap;
        this.ownerJarId = ownerJarId == null ? -1 : ownerJarId;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (descriptor.equals(SpringConstant.RequestMappingAnno)) {
            this.pathAnnoAdapter = new SpringPathAnnoAdapter(Const.ASMVersion, av);
            return this.pathAnnoAdapter;
        }
        return av;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        this.name = name;
        ClassReference ref = classMap.get(new ClassReference.Handle(name, ownerJarId));
        if (ref == null || ref.getAnnotations() == null || ref.getAnnotations().isEmpty()) {
            super.visit(version, access, name, signature, superName, interfaces);
            return;
        }
        Set<AnnoReference> annotations = ref.getAnnotations();
        if (annotations.stream().parallel().anyMatch(annoReference -> StrUtil.containsAny(annoReference.getAnnoName(),
                SpringConstant.ControllerAnno,
                SpringConstant.RestControllerAnno, SpringConstant.RequestMappingAnno)
        )) {
            this.isSpring = true;
            currentController = new SpringController();
            currentController.setClassReference(ref);
            currentController.setClassName(ref.getHandle());
            currentController.setRest(annotations.stream().parallel().noneMatch(annoReference -> annoReference.getAnnoName().contains(SpringConstant.ControllerAnno)));
            if (pathAnnoAdapter != null) {
                if (!pathAnnoAdapter.getResults().isEmpty()) {
                    currentController.setBasePath(pathAnnoAdapter.getResults().get(0));
                }
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (this.isSpring) {
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return mv;
            }
            if (pathAnnoAdapter != null) {
                if (!pathAnnoAdapter.getResults().isEmpty()) {
                    currentController.setBasePath(pathAnnoAdapter.getResults().get(0));
                }
            }
            return new SpringMethodAdapter(name, descriptor, this.name, Const.ASMVersion, mv,
                    currentController, this.methodMap, ownerJarId);
        } else {
            return mv;
        }
    }

    @Override
    public void visitEnd() {
        if (isSpring && currentController != null) {
            controllers.add(currentController);
        }
        super.visitEnd();
    }
}
