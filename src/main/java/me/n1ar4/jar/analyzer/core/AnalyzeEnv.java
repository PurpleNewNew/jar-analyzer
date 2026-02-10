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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;

import java.util.*;

public class AnalyzeEnv {
    public static boolean isCli = false;
    public static boolean jarsInJar = false;
    public static Set<ClassFileEntity> classFileList = new HashSet<>();
    public static Set<ClassReference> discoveredClasses = new HashSet<>();
    public static Set<MethodReference> discoveredMethods = new HashSet<>();
    public static Map<ClassReference.Handle, List<MethodReference>> methodsInClassMap = new HashMap<>();
    public static Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
    public static Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
    public static HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
    public static Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
    public static InheritanceMap inheritanceMap;
    public static Map<MethodReference.Handle, List<String>> strMap = new HashMap<>();
    public static ArrayList<SpringController> controllers = new ArrayList<>();
    public static ArrayList<String> interceptors = new ArrayList<>();
    public static ArrayList<String> servlets = new ArrayList<>();
    public static ArrayList<String> filters = new ArrayList<>();
    public static ArrayList<String> listeners = new ArrayList<>();
    public static Map<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();
    public static ArrayList<ResourceEntity> resources = new ArrayList<>();

    // RTA instantiated classes collected during build (NEW).
    public static Set<ClassReference.Handle> instantiatedClasses = new HashSet<>();

    /**
     * Bind the legacy global holders to a build-scoped context.
     * <p>
     * This is a transition helper to reduce churn while we progressively move code to BuildContext.
     */
    public static void use(BuildContext ctx) {
        if (ctx == null) {
            ctx = new BuildContext();
        }
        classFileList = ctx.classFileList;
        discoveredClasses = ctx.discoveredClasses;
        discoveredMethods = ctx.discoveredMethods;
        methodsInClassMap = ctx.methodsInClassMap;
        classMap = ctx.classMap;
        methodMap = ctx.methodMap;
        methodCalls = ctx.methodCalls;
        methodCallMeta = ctx.methodCallMeta;
        inheritanceMap = ctx.inheritanceMap;
        strMap = ctx.strMap;
        controllers = ctx.controllers;
        interceptors = ctx.interceptors;
        servlets = ctx.servlets;
        filters = ctx.filters;
        listeners = ctx.listeners;
        stringAnnoMap = ctx.stringAnnoMap;
        resources = ctx.resources;
        instantiatedClasses = ctx.instantiatedClasses;
    }
}
