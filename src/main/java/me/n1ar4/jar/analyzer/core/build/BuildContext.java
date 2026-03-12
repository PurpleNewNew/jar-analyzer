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
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
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
    public final Set<ClassFileEntity> classFileList;
    public final Set<ClassReference> discoveredClasses;
    public final Set<MethodReference> discoveredMethods;
    public final Map<ClassReference.Handle, ClassReference> classMap;
    public final Map<MethodReference.Handle, MethodReference> methodMap;
    public final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
    public final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
    public final Map<MethodReference.Handle, List<String>> strMap;
    public final List<SpringController> controllers;
    public final ArrayList<String> interceptors;
    public final ArrayList<String> servlets;
    public final ArrayList<String> filters;
    public final ArrayList<String> listeners;
    public final Map<MethodReference.Handle, Integer> explicitSourceMethodFlags;
    public final Map<MethodReference.Handle, List<String>> stringAnnoMap;
    public final List<ResourceEntity> resources;
    public final List<CallSiteEntity> callSites;

    public BuildContext() {
        this(null, null, null, null, null);
    }

    public static BuildContext callGraphView(Map<ClassReference.Handle, ClassReference> classMap,
                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                             List<CallSiteEntity> callSites,
                                             HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                             Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        return new BuildContext(classMap, methodMap, callSites, methodCalls, methodCallMeta);
    }

    private BuildContext(Map<ClassReference.Handle, ClassReference> classMap,
                         Map<MethodReference.Handle, MethodReference> methodMap,
                         List<CallSiteEntity> callSites,
                         HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                         Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        this.classFileList = new HashSet<>();
        this.discoveredClasses = new HashSet<>();
        this.discoveredMethods = new HashSet<>();
        this.classMap = classMap == null ? new HashMap<>() : classMap;
        this.methodMap = methodMap == null ? new HashMap<>() : methodMap;
        this.methodCalls = methodCalls == null ? new HashMap<>() : methodCalls;
        this.methodCallMeta = methodCallMeta == null ? new HashMap<>() : methodCallMeta;
        this.strMap = new HashMap<>();
        this.controllers = new ArrayList<>();
        this.interceptors = new ArrayList<>();
        this.servlets = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.explicitSourceMethodFlags = new HashMap<>();
        this.stringAnnoMap = new HashMap<>();
        this.resources = new ArrayList<>();
        this.callSites = callSites == null ? new ArrayList<>() : callSites;
    }
}
