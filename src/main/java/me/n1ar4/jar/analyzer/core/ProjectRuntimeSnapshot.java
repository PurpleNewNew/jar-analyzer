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

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProjectRuntimeSnapshot(
        int schemaVersion,
        long buildSeq,
        ProjectModelData projectModel,
        List<JarData> jars,
        List<ClassFileData> classFiles,
        List<ClassReferenceData> classReferences,
        List<MethodReferenceData> methodReferences,
        Map<String, List<String>> methodStrings,
        Map<String, List<String>> methodAnnoStrings,
        List<ResourceData> resources,
        List<CallSiteData> callSites,
        List<LocalVarData> localVars,
        List<SpringControllerData> springControllers,
        Set<String> springInterceptors,
        Set<String> servlets,
        Set<String> filters,
        Set<String> listeners
) {
    public static final int CURRENT_SCHEMA_VERSION = 4;

    public ProjectRuntimeSnapshot {
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        buildSeq = Math.max(0L, buildSeq);
        jars = immutableList(jars);
        classFiles = immutableList(classFiles);
        classReferences = immutableList(classReferences);
        methodReferences = immutableList(methodReferences);
        methodStrings = immutableMap(methodStrings);
        methodAnnoStrings = immutableMap(methodAnnoStrings);
        resources = immutableList(resources);
        callSites = immutableList(callSites);
        localVars = immutableList(localVars);
        springControllers = immutableList(springControllers);
        springInterceptors = immutableSet(springInterceptors);
        servlets = immutableSet(servlets);
        filters = immutableSet(filters);
        listeners = immutableSet(listeners);
    }

    public record ProjectModelData(
            String buildMode,
            String primaryInputPath,
            String runtimePath,
            List<ProjectRootData> roots,
            List<String> analyzedArchives,
            boolean resolveInnerJars,
            String jdkModules,
            String callGraphProfile,
            String taintPropagationMode
    ) {
        public ProjectModelData {
            roots = immutableList(roots);
            analyzedArchives = immutableList(analyzedArchives);
        }

        public ProjectModelData(String buildMode,
                                String primaryInputPath,
                                String runtimePath,
                                List<ProjectRootData> roots,
                                List<String> analyzedArchives,
                                boolean resolveInnerJars) {
            this(buildMode, primaryInputPath, runtimePath, roots, analyzedArchives, resolveInnerJars, "core", "", "");
        }

        public ProjectModelData(String buildMode,
                                String primaryInputPath,
                                String runtimePath,
                                List<ProjectRootData> roots,
                                List<String> analyzedArchives,
                                boolean resolveInnerJars,
                                String jdkModules) {
            this(buildMode, primaryInputPath, runtimePath, roots, analyzedArchives, resolveInnerJars, jdkModules, "", "");
        }

        public ProjectModelData(String buildMode,
                                String primaryInputPath,
                                String runtimePath,
                                List<ProjectRootData> roots,
                                List<String> analyzedArchives,
                                boolean resolveInnerJars,
                                String jdkModules,
                                String callGraphProfile) {
            this(buildMode, primaryInputPath, runtimePath, roots, analyzedArchives, resolveInnerJars, jdkModules, callGraphProfile, "");
        }
    }

    public record ProjectRootData(
            String kind,
            String origin,
            String path,
            String presentableName,
            boolean archive,
            boolean test,
            int priority
    ) {
    }

    public record JarData(
            int jid,
            String jarName,
            String jarAbsPath,
            String origin
    ) {
        public JarData(int jid, String jarName, String jarAbsPath) {
            this(jid, jarName, jarAbsPath, "unknown");
        }
    }

    public record ClassFileData(
            int cfId,
            String className,
            String pathStr,
            String jarName,
            Integer jarId
    ) {
    }

    public record AnnoData(
            String annoName,
            Boolean visible,
            Integer parameter
    ) {
    }

    public record ClassHandleData(
            String name,
            Integer jarId
    ) {
    }

    public record ClassMemberData(
            String name,
            int modifiers,
            String value,
            String desc,
            String signature,
            ClassHandleData type
    ) {
    }

    public record ClassReferenceData(
            Integer version,
            Integer access,
            String name,
            String superClass,
            List<String> interfaces,
            boolean isInterface,
            List<ClassMemberData> members,
            List<AnnoData> annotations,
            String jarName,
            Integer jarId
    ) {
        public ClassReferenceData {
            interfaces = immutableList(interfaces);
            members = immutableList(members);
            annotations = immutableList(annotations);
        }
    }

    public record MethodReferenceData(
            ClassHandleData classReference,
            List<AnnoData> annotations,
            String name,
            String desc,
            int access,
            boolean isStatic,
            int lineNumber,
            String jarName,
            Integer jarId,
            int semanticFlags
    ) {
        public MethodReferenceData {
            annotations = immutableList(annotations);
        }

        public MethodReferenceData(ClassHandleData classReference,
                                   List<AnnoData> annotations,
                                   String name,
                                   String desc,
                                   int access,
                                   boolean isStatic,
                                   int lineNumber,
                                   String jarName,
                                   Integer jarId) {
            this(classReference, annotations, name, desc, access, isStatic, lineNumber, jarName, jarId, 0);
        }
    }

    public record ResourceData(
            int rid,
            String resourcePath,
            String pathStr,
            String jarName,
            Integer jarId,
            long fileSize,
            int isText
    ) {
    }

    public record CallSiteData(
            String callerClassName,
            String callerMethodName,
            String callerMethodDesc,
            String calleeOwner,
            String calleeMethodName,
            String calleeMethodDesc,
            Integer opCode,
            Integer lineNumber,
            Integer callIndex,
            String receiverType,
            Integer jarId,
            String callSiteKey
    ) {
    }

    public record LocalVarData(
            String className,
            String methodName,
            String methodDesc,
            Integer varIndex,
            String varName,
            String varDesc,
            String varSignature,
            Integer startLine,
            Integer endLine,
            Integer jarId
    ) {
    }

    public record SpringControllerData(
            boolean rest,
            String basePath,
            ClassHandleData classHandle,
            List<SpringMappingData> mappings
    ) {
        public SpringControllerData {
            mappings = immutableList(mappings);
        }
    }

    public record SpringMappingData(
            boolean rest,
            ClassHandleData methodOwner,
            String methodName,
            String methodDesc,
            String path,
            String restfulType,
            String pathRestful,
            List<SpringParamData> params
    ) {
        public SpringMappingData {
            params = immutableList(params);
        }
    }

    public record SpringParamData(
            int paramIndex,
            String paramName,
            String paramType,
            String reqName
    ) {
    }

    static <T> List<T> ownedList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values instanceof OwnedView ? values : new OwnedListView<>(values);
    }

    static <K, V> Map<K, V> ownedMap(Map<K, V> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values instanceof OwnedView ? values : new OwnedMapView<>(values);
    }

    static <T> Set<T> ownedSet(Set<T> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values instanceof OwnedView ? values : new OwnedSetView<>(values);
    }

    private static <T> List<T> immutableList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values instanceof OwnedView ? values : List.copyOf(values);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values instanceof OwnedView ? values : Map.copyOf(values);
    }

    private static <T> Set<T> immutableSet(Set<T> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values instanceof OwnedView ? values : Set.copyOf(values);
    }

    private interface OwnedView {
    }

    private static final class OwnedListView<T> extends AbstractList<T> implements java.util.RandomAccess, OwnedView {
        private final List<T> delegate;

        private OwnedListView(List<T> delegate) {
            this.delegate = Collections.unmodifiableList(delegate);
        }

        @Override
        public T get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }
    }

    private static final class OwnedMapView<K, V> extends AbstractMap<K, V> implements OwnedView {
        private final Map<K, V> delegate;

        private OwnedMapView(Map<K, V> delegate) {
            this.delegate = Collections.unmodifiableMap(delegate);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }
    }

    private static final class OwnedSetView<T> extends AbstractSet<T> implements OwnedView {
        private final Set<T> delegate;

        private OwnedSetView(Set<T> delegate) {
            this.delegate = Collections.unmodifiableSet(delegate);
        }

        @Override
        public Iterator<T> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }
    }
}
