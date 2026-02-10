/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.build;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build-scoped containers to reduce global state and allow concurrent builds/tests.
 * <p>
 * Note: analysis-time state should not live here.
 */
public final class BuildContext {
    public final Set<ClassFileEntity> classFileList = new HashSet<>();
    public final Set<ClassReference> discoveredClasses = new HashSet<>();
    public final Set<MethodReference> discoveredMethods = new HashSet<>();
    public final Map<ClassReference.Handle, List<MethodReference>> methodsInClassMap = new HashMap<>();
    public final Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
    public final Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
    public final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
    public final Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
    public InheritanceMap inheritanceMap;
    public final Map<MethodReference.Handle, List<String>> strMap = new HashMap<>();
    public final ArrayList<SpringController> controllers = new ArrayList<>();
    public final ArrayList<String> interceptors = new ArrayList<>();
    public final ArrayList<String> servlets = new ArrayList<>();
    public final ArrayList<String> filters = new ArrayList<>();
    public final ArrayList<String> listeners = new ArrayList<>();
    public final Map<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();
    public final ArrayList<ResourceEntity> resources = new ArrayList<>();

    // RTA: instantiated classes collected during bytecode scan (NEW).
    public final Set<ClassReference.Handle> instantiatedClasses = new HashSet<>();
}

