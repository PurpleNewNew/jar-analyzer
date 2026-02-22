/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LineMappingEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final int PAGE_SIZE = 100;

    private final GraphStore graphStore = new GraphStore();
    private final Map<String, List<LineMappingEntity>> lineMappings = new ConcurrentHashMap<>();
    private volatile CallGraphCache callGraphCache;

    public boolean isEnabled() {
        try {
            String projectKey = ActiveProjectContext.getActiveProjectKey();
            Path home = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
            if (home == null || !Files.exists(home) || !Files.isDirectory(home)) {
                return false;
            }
            try (Stream<Path> stream = Files.walk(home)) {
                return stream.anyMatch(Files::isRegularFile);
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            return false;
        } catch (Exception ex) {
            logger.debug("check engine enabled failed: {}", ex.toString());
            return false;
        }
    }

    public CoreEngine(ConfigFile configFile) {
        String dbPathValue = configFile == null ? null : configFile.getDbPath();
        if (StringUtil.isNull(dbPathValue)) {
            throw new RuntimeException("start engine error");
        }
        logger.info("init core engine finish (neo4j-only)");
    }

    public CallGraphCache getCallGraphCache() {
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache;
        }
        synchronized (this) {
            if (callGraphCache == null) {
                callGraphCache = buildCallGraphCache();
            }
            return callGraphCache;
        }
    }

    public void clearCallGraphCache() {
        callGraphCache = null;
    }

    private CallGraphCache buildCallGraphCache() {
        long startNs = System.nanoTime();
        GraphSnapshot snapshot = loadSnapshotSafe();
        Map<Long, MethodResult> methods = new HashMap<>();
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            if (node == null) {
                continue;
            }
            methods.put(node.getNodeId(), toMethodResult(node));
        }
        List<MethodCallResult> edges = new ArrayList<>();
        for (Map.Entry<Long, MethodResult> entry : methods.entrySet()) {
            long srcId = entry.getKey();
            MethodResult src = entry.getValue();
            for (GraphEdge edge : snapshot.getOutgoingView(srcId)) {
                if (!isCallEdge(edge.getRelType())) {
                    continue;
                }
                MethodResult dst = methods.get(edge.getDstId());
                if (dst == null) {
                    continue;
                }
                MethodCallResult row = new MethodCallResult();
                row.setCallerClassName(src.getClassName());
                row.setCallerMethodName(src.getMethodName());
                row.setCallerMethodDesc(src.getMethodDesc());
                row.setCallerJarId(src.getJarId());
                row.setCallerJarName(src.getJarName());
                row.setCalleeClassName(dst.getClassName());
                row.setCalleeMethodName(dst.getMethodName());
                row.setCalleeMethodDesc(dst.getMethodDesc());
                row.setCalleeJarId(dst.getJarId());
                row.setCalleeJarName(dst.getJarName());
                row.setOpCode(edge.getOpCode());
                row.setEdgeType(resolveEdgeType(edge));
                row.setEdgeConfidence(safe(edge.getConfidence()));
                row.setEdgeEvidence(safe(edge.getEvidence()));
                edges.add(row);
            }
        }
        CallGraphCache cache = CallGraphCache.build(edges);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        logger.info("call graph cache loaded: edges={}, methods={}, elapsedMs={}",
                cache.getEdgeCount(), cache.getMethodCount(), elapsedMs);
        return cache;
    }

    public ArrayList<MethodResult> getMethodsByClass(String className) {
        ArrayList<MethodResult> results = new ArrayList<>();
        for (MethodReference method : DatabaseManager.getMethodReferencesByClass(className)) {
            results.add(toMethodResult(method));
        }
        results.sort(Comparator.comparing(MethodResult::getMethodName)
                .thenComparing(MethodResult::getMethodDesc)
                .thenComparingInt(MethodResult::getJarId));
        return results;
    }

    public ClassResult getClassByClass(String className) {
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return null;
        }
        ArrayList<ClassResult> results = getClassesByClass(className);
        return results.isEmpty() ? null : results.get(0);
    }

    public ArrayList<ClassResult> getClassesByClass(String className) {
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return new ArrayList<>();
        }
        List<ClassReference> rows = DatabaseManager.getClassReferencesByName(className);
        ArrayList<ClassResult> results = new ArrayList<>();
        for (ClassReference row : rows) {
            ClassResult result = toClassResult(row);
            if (result != null) {
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            // Fallback to method-level info for source-index / stripped metadata scenarios.
            List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(className);
            if (!methods.isEmpty()) {
                MethodReference first = methods.get(0);
                ClassResult fallback = new ClassResult();
                fallback.setClassName(normalizeClassName(className));
                fallback.setSuperClassName("java/lang/Object");
                fallback.setIsInterfaceInt(0);
                int jarId = normalizeJarId(first.getJarId());
                fallback.setJarId(jarId);
                fallback.setJarName(resolveJarName(jarId, first.getJarName()));
                results.add(fallback);
            }
        }
        results.sort(Comparator.comparingInt(ClassResult::getJarId)
                .thenComparing(ClassResult::getJarName, Comparator.nullsLast(String::compareTo)));
        return results;
    }

    public ArrayList<String> includeClassByClassName(String className) {
        return includeClassByClassName(className, false);
    }

    public ArrayList<String> includeClassByClassName(String className, boolean includeJdk) {
        String keyword = safe(className).trim();
        if (keyword.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String candidate : collectKnownClasses()) {
            if (!candidate.contains(keyword)) {
                continue;
            }
            if (CommonFilterUtil.isModuleInfoClassName(candidate)) {
                continue;
            }
            if (!includeJdk && CommonFilterUtil.isFilteredClass(candidate)) {
                continue;
            }
            out.add(candidate);
        }
        return new ArrayList<>(out);
    }

    public String getAbsPath(String className) {
        ClassFileEntity row = DatabaseManager.getClassFileByClass(className, null);
        if (row == null) {
            return null;
        }
        String path = safe(row.getPathStr());
        if (!path.isEmpty()) {
            return path;
        }
        Path p = row.getPath();
        return p == null ? null : p.toAbsolutePath().normalize().toString();
    }

    public String getAbsPath(String className, Integer jarId) {
        ClassFileEntity row = DatabaseManager.getClassFileByClass(className, jarId);
        if (row == null) {
            return getAbsPath(className);
        }
        String path = safe(row.getPathStr());
        if (!path.isEmpty()) {
            return path;
        }
        Path p = row.getPath();
        return p == null ? null : p.toAbsolutePath().normalize().toString();
    }

    public ArrayList<MethodResult> getCallers(String calleeClass, String calleeMethod, String calleeDesc) {
        return getCallers(calleeClass, calleeMethod, calleeDesc, null);
    }

    public ArrayList<MethodResult> getCallers(String calleeClass,
                                              String calleeMethod,
                                              String calleeDesc,
                                              Integer calleeJarId) {
        return getCallGraphCache().getCallers(calleeClass, calleeMethod, calleeDesc, normalizeJarId(calleeJarId));
    }

    public ArrayList<MethodResult> getCallersLike(String calleeClass, String calleeMethod, String calleeDesc) {
        String cls = normalizeClassName(calleeClass);
        String method = safe(calleeMethod);
        String desc = safe(calleeDesc);
        ArrayList<MethodResult> out = new ArrayList<>();
        GraphSnapshot snapshot = loadSnapshotSafe();
        Map<Long, MethodResult> methods = methodNodeMap(snapshot);
        for (Map.Entry<Long, MethodResult> entry : methods.entrySet()) {
            MethodResult callee = entry.getValue();
            if (!matchesLike(callee.getClassName(), cls)
                    || !matchesLike(callee.getMethodName(), method)
                    || !matchesLike(callee.getMethodDesc(), desc)) {
                continue;
            }
            out.addAll(getCallGraphCache().getCallers(
                    callee.getClassName(),
                    callee.getMethodName(),
                    callee.getMethodDesc(),
                    callee.getJarId()
            ));
        }
        return dedupMethods(out);
    }

    public ArrayList<MethodResult> getCallee(String callerClass, String callerMethod, String callerDesc) {
        return getCallee(callerClass, callerMethod, callerDesc, null);
    }

    public ArrayList<MethodResult> getCallee(String callerClass,
                                             String callerMethod,
                                             String callerDesc,
                                             Integer callerJarId) {
        return getCallGraphCache().getCallees(callerClass, callerMethod, callerDesc, normalizeJarId(callerJarId));
    }

    public ArrayList<MethodCallResult> getCallEdgesByCallee(String calleeClass,
                                                            String calleeMethod,
                                                            String calleeDesc,
                                                            Integer offset,
                                                            Integer limit) {
        return filterCallEdges(false, calleeClass, calleeMethod, calleeDesc, offset, limit);
    }

    public ArrayList<MethodCallResult> getCallEdgesByCaller(String callerClass,
                                                            String callerMethod,
                                                            String callerDesc,
                                                            Integer offset,
                                                            Integer limit) {
        return filterCallEdges(true, callerClass, callerMethod, callerDesc, offset, limit);
    }

    public MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getEdgeMeta(caller, callee);
        }

        GraphSnapshot snapshot = loadSnapshotSafe();
        long callerId = snapshot.findMethodNodeId(
                caller.getClassReference().getName(),
                caller.getName(),
                caller.getDesc(),
                caller.getJarId());
        if (callerId <= 0L) {
            return null;
        }
        long calleeId = snapshot.findMethodNodeId(
                callee.getClassReference().getName(),
                callee.getName(),
                callee.getDesc(),
                callee.getJarId());
        if (calleeId <= 0L) {
            return null;
        }
        for (GraphEdge edge : snapshot.getOutgoingView(callerId)) {
            if (edge.getDstId() != calleeId || !isCallEdge(edge.getRelType())) {
                continue;
            }
            return new MethodCallMeta(resolveEdgeType(edge), safe(edge.getConfidence()), safe(edge.getEvidence()));
        }
        return null;
    }

    public Map<MethodCallKey, MethodCallMeta> getEdgeMetaBatch(List<MethodCallKey> keys) {
        Map<MethodCallKey, MethodCallMeta> out = new HashMap<>();
        if (keys == null || keys.isEmpty()) {
            return out;
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            for (MethodCallKey key : keys) {
                if (key == null) {
                    continue;
                }
                MethodReference.Handle caller = new MethodReference.Handle(
                        new ClassReference.Handle(key.getCallerClass(), key.getCallerJarId()),
                        key.getCallerMethod(),
                        key.getCallerDesc());
                MethodReference.Handle callee = new MethodReference.Handle(
                        new ClassReference.Handle(key.getCalleeClass(), key.getCalleeJarId()),
                        key.getCalleeMethod(),
                        key.getCalleeDesc());
                MethodCallMeta meta = cache.getEdgeMeta(caller, callee);
                if (meta != null) {
                    out.put(key, meta);
                }
            }
            return out;
        }

        for (MethodCallKey key : keys) {
            if (key == null) {
                continue;
            }
            MethodReference.Handle caller = new MethodReference.Handle(
                    new ClassReference.Handle(key.getCallerClass(), key.getCallerJarId()),
                    key.getCallerMethod(),
                    key.getCallerDesc());
            MethodReference.Handle callee = new MethodReference.Handle(
                    new ClassReference.Handle(key.getCalleeClass(), key.getCalleeJarId()),
                    key.getCalleeMethod(),
                    key.getCalleeDesc());
            MethodCallMeta meta = getEdgeMeta(caller, callee);
            if (meta != null) {
                out.put(key, meta);
            }
        }
        return out;
    }

    public ArrayList<MethodResult> getMethod(String className, String methodName, String methodDesc) {
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();

        Collection<MethodReference> source;
        if (!cls.isEmpty()) {
            source = DatabaseManager.getMethodReferencesByClass(cls);
        } else {
            source = DatabaseManager.getMethodReferences();
        }

        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodReference ref : source) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            if (!cls.isEmpty() && !cls.equals(normalizeClassName(ref.getClassReference().getName()))) {
                continue;
            }
            if (!method.isEmpty() && !method.equals(ref.getName())) {
                continue;
            }
            if (!desc.isEmpty() && !desc.equals(ref.getDesc())) {
                continue;
            }
            out.add(toMethodResult(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<CallSiteEntity> getCallSitesByCaller(String className,
                                                          String methodName,
                                                          String methodDesc) {
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();
        ArrayList<CallSiteEntity> out = new ArrayList<>();
        for (CallSiteEntity row : DatabaseManager.getCallSites()) {
            if (row == null) {
                continue;
            }
            if (!cls.isEmpty() && !cls.equals(normalizeClassName(row.getCallerClassName()))) {
                continue;
            }
            if (!method.isEmpty() && !method.equals(row.getCallerMethodName())) {
                continue;
            }
            if (!desc.isEmpty() && !desc.equals(row.getCallerMethodDesc())) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    public ArrayList<CallSiteEntity> getCallSitesByEdge(String callerClassName,
                                                        String callerMethodName,
                                                        String callerMethodDesc,
                                                        String calleeOwner,
                                                        String calleeMethodName,
                                                        String calleeMethodDesc) {
        ArrayList<CallSiteEntity> out = new ArrayList<>();
        String c1 = normalizeClassName(callerClassName);
        String m1 = safe(callerMethodName).trim();
        String d1 = safe(callerMethodDesc).trim();
        String c2 = normalizeClassName(calleeOwner);
        String m2 = safe(calleeMethodName).trim();
        String d2 = safe(calleeMethodDesc).trim();
        for (CallSiteEntity row : DatabaseManager.getCallSites()) {
            if (row == null) {
                continue;
            }
            if (!c1.isEmpty() && !c1.equals(normalizeClassName(row.getCallerClassName()))) {
                continue;
            }
            if (!m1.isEmpty() && !m1.equals(row.getCallerMethodName())) {
                continue;
            }
            if (!d1.isEmpty() && !d1.equals(row.getCallerMethodDesc())) {
                continue;
            }
            if (!c2.isEmpty() && !c2.equals(normalizeClassName(row.getCalleeOwner()))) {
                continue;
            }
            if (!m2.isEmpty() && !m2.equals(row.getCalleeMethodName())) {
                continue;
            }
            if (!d2.isEmpty() && !d2.equals(row.getCalleeMethodDesc())) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    public ArrayList<LocalVarEntity> getLocalVarsByMethod(String className,
                                                          String methodName,
                                                          String methodDesc) {
        return new ArrayList<>(DatabaseManager.getLocalVarsByMethod(className, methodName, methodDesc));
    }

    public ArrayList<MethodResult> getMethodLike(String className, String methodName, String methodDesc) {
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();
        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodReference ref : DatabaseManager.getMethodReferences()) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            String refClass = normalizeClassName(ref.getClassReference().getName());
            if (!cls.isEmpty() && !refClass.contains(cls)) {
                continue;
            }
            if (!method.isEmpty() && !safe(ref.getName()).contains(method)) {
                continue;
            }
            if (!desc.isEmpty() && !safe(ref.getDesc()).contains(desc)) {
                continue;
            }
            out.add(toMethodResult(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<MethodResult> getMethodsByStr(String val) {
        return getMethodsByStr(val, null, null, null, "auto");
    }

    public ArrayList<MethodResult> getMethodsByStrEqual(String val) {
        return getMethodsByStr(val, null, null, null, "equal");
    }

    public ArrayList<MethodResult> getMethodsByStr(String val,
                                                   Integer jarId,
                                                   String classLike,
                                                   Integer limit,
                                                   String mode) {
        String needle = safe(val);
        if (needle.isEmpty()) {
            return new ArrayList<>();
        }
        String classFilter = normalizeClassName(classLike);
        String m = safe(mode).trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty()) {
            m = "auto";
        }
        int max = (limit == null || limit <= 0) ? 1000 : limit;

        ArrayList<MethodResult> out = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MethodReference ref : DatabaseManager.getMethodReferences()) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            int refJarId = normalizeJarId(ref.getJarId());
            if (jarId != null && jarId >= 0 && refJarId != jarId) {
                continue;
            }
            String cls = normalizeClassName(ref.getClassReference().getName());
            if (!classFilter.isEmpty() && !cls.contains(classFilter)) {
                continue;
            }
            List<String> values = DatabaseManager.getMethodStringValues(cls, ref.getName(), ref.getDesc(), refJarId);
            if (values.isEmpty()) {
                values = DatabaseManager.getMethodAnnoStringValues(cls, ref.getName(), ref.getDesc(), refJarId);
            }
            if (values.isEmpty()) {
                continue;
            }
            String matched = firstMatchedString(values, needle, m);
            if (matched == null) {
                continue;
            }
            MethodResult result = toMethodResult(ref);
            result.setStrValue(matched);
            String key = methodKey(result.getClassName(), result.getMethodName(), result.getMethodDesc(), result.getJarId());
            if (!seen.add(key)) {
                continue;
            }
            out.add(result);
            if (out.size() >= max) {
                break;
            }
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    private String firstMatchedString(List<String> values, String needle, String mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String value : values) {
            String v = safe(value);
            if (v.isEmpty()) {
                continue;
            }
            if ("equal".equals(mode) && v.equals(needle)) {
                return v;
            }
            if ("prefix".equals(mode) && v.startsWith(needle)) {
                return v;
            }
            if (("auto".equals(mode) || "fts".equals(mode) || "contains".equals(mode)) && v.contains(needle)) {
                return v;
            }
        }
        return null;
    }

    public ArrayList<String> getJarsPath() {
        ArrayList<String> out = new ArrayList<>();
        for (JarEntity jar : DatabaseManager.getJarsMeta()) {
            if (jar == null) {
                continue;
            }
            out.add(safe(jar.getJarAbsPath()));
        }
        return out;
    }

    public ArrayList<String> getJarNames() {
        ArrayList<String> out = new ArrayList<>();
        for (JarEntity jar : DatabaseManager.getJarsMeta()) {
            if (jar == null) {
                continue;
            }
            out.add(safe(jar.getJarName()));
        }
        return out;
    }

    public ArrayList<JarEntity> getJarsMeta() {
        return new ArrayList<>(DatabaseManager.getJarsMeta());
    }

    public ArrayList<MethodResult> getImpls(String className,
                                            String methodName,
                                            String methodDesc) {
        return getImpls(className, methodName, methodDesc, null);
    }

    public ArrayList<MethodResult> getImpls(String className,
                                            String methodName,
                                            String methodDesc,
                                            Integer classJarId) {
        String owner = normalizeClassName(className);
        if (owner.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> candidates = collectSubClasses(owner);
        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodReference ref : DatabaseManager.getMethodReferences()) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            String cls = normalizeClassName(ref.getClassReference().getName());
            if (!candidates.contains(cls)) {
                continue;
            }
            if (!safe(ref.getName()).equals(safe(methodName))) {
                continue;
            }
            if (!safe(ref.getDesc()).equals(safe(methodDesc))) {
                continue;
            }
            if (classJarId != null && classJarId >= 0 && normalizeJarId(ref.getJarId()) != classJarId) {
                continue;
            }
            out.add(toMethodResult(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<MethodResult> getSuperImpls(String className, String methodName, String methodDesc) {
        return getSuperImpls(className, methodName, methodDesc, null);
    }

    public ArrayList<MethodResult> getSuperImpls(String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer classJarId) {
        String owner = normalizeClassName(className);
        if (owner.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> candidates = collectSuperClasses(owner);
        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodReference ref : DatabaseManager.getMethodReferences()) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            String cls = normalizeClassName(ref.getClassReference().getName());
            if (!candidates.contains(cls)) {
                continue;
            }
            if (!safe(ref.getName()).equals(safe(methodName))) {
                continue;
            }
            if (!safe(ref.getDesc()).equals(safe(methodDesc))) {
                continue;
            }
            if (classJarId != null && classJarId >= 0 && normalizeJarId(ref.getJarId()) != classJarId) {
                continue;
            }
            out.add(toMethodResult(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public String getJarByClass(String className) {
        ClassReference ref = DatabaseManager.getClassReferenceByName(className, null);
        if (ref != null && ref.getJarName() != null && !ref.getJarName().isBlank()) {
            return ref.getJarName();
        }
        List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(className);
        if (!methods.isEmpty()) {
            return resolveJarName(normalizeJarId(methods.get(0).getJarId()), methods.get(0).getJarName());
        }
        ClassFileEntity file = DatabaseManager.getClassFileByClass(className, null);
        if (file != null) {
            return safe(file.getJarName());
        }
        return null;
    }

    public Integer getJarIdByClass(String className) {
        if (StringUtil.isNull(className)) {
            return null;
        }
        ClassReference ref = DatabaseManager.getClassReferenceByName(className, null);
        if (ref != null) {
            return normalizeJarId(ref.getJarId());
        }
        List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(className);
        if (!methods.isEmpty()) {
            return normalizeJarId(methods.get(0).getJarId());
        }
        ClassFileEntity file = DatabaseManager.getClassFileByClass(className, null);
        if (file != null) {
            return normalizeJarId(file.getJarId());
        }
        return null;
    }

    public ArrayList<LineMappingEntity> getLineMappings(String className,
                                                        Integer jarId,
                                                        String decompiler) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        String key = lineMappingKey(className, jarId, decompiler);
        List<LineMappingEntity> rows = lineMappings.get(key);
        return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    public void saveLineMappings(String className,
                                 Integer jarId,
                                 String decompiler,
                                 List<LineMappingEntity> mappings) {
        if (StringUtil.isNull(className)) {
            return;
        }
        String key = lineMappingKey(className, jarId, decompiler);
        if (mappings == null || mappings.isEmpty()) {
            lineMappings.remove(key);
            return;
        }
        lineMappings.put(key, new ArrayList<>(mappings));
    }

    public String getJarNameById(Integer jarId) {
        if (jarId == null) {
            return null;
        }
        JarEntity jar = DatabaseManager.getJarById(jarId);
        return jar == null ? null : jar.getJarName();
    }

    public ArrayList<ClassResult> getAllSpringC() {
        ArrayList<ClassResult> out = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null || controller.getClassName() == null) {
                continue;
            }
            String className = normalizeClassName(controller.getClassName().getName());
            if (className.isEmpty()) {
                continue;
            }
            ClassResult result = getClassByClass(className);
            if (result == null) {
                result = new ClassResult();
                result.setClassName(className);
                result.setJarId(normalizeJarId(controller.getClassName().getJarId()));
                result.setJarName(getJarNameById(result.getJarId()));
                result.setSuperClassName("java/lang/Object");
                result.setIsInterfaceInt(0);
            }
            out.add(result);
        }
        return filterWebClasses(out);
    }

    public ArrayList<ClassResult> getAllSpringI() {
        return filterWebClasses(classesFromNames(DatabaseManager.getSpringInterceptors()));
    }

    public ArrayList<ClassResult> getAllServlets() {
        return filterWebClasses(classesFromNames(DatabaseManager.getServlets()));
    }

    public ArrayList<ClassResult> getAllFilters() {
        return filterWebClasses(classesFromNames(DatabaseManager.getFilters()));
    }

    public ArrayList<ClassResult> getAllListeners() {
        return filterWebClasses(classesFromNames(DatabaseManager.getListeners()));
    }

    public ArrayList<MethodResult> getSpringM(String className) {
        if (CommonFilterUtil.isFilteredClass(className)) {
            return new ArrayList<>();
        }
        String target = normalizeClassName(className);
        ArrayList<MethodResult> out = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null || controller.getClassName() == null) {
                continue;
            }
            String owner = normalizeClassName(controller.getClassName().getName());
            if (!owner.equals(target)) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodResult item = toSpringMethod(mapping);
                if (item != null) {
                    out.add(item);
                }
            }
        }
        return filterWebMethods(out);
    }

    public ArrayList<MethodResult> getSpringMappingsAll(Integer jarId,
                                                        String keyword,
                                                        Integer offset,
                                                        Integer limit) {
        String kw = safe(keyword);
        int begin = offset == null || offset < 0 ? 0 : offset;
        int size = limit == null || limit <= 0 ? 100 : limit;

        ArrayList<MethodResult> all = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodResult item = toSpringMethod(mapping);
                if (item == null) {
                    continue;
                }
                if (jarId != null && jarId >= 0 && item.getJarId() != jarId) {
                    continue;
                }
                if (!kw.isEmpty() && !(safe(item.getClassName()).contains(kw)
                        || safe(item.getMethodName()).contains(kw)
                        || safe(item.getActualPath()).contains(kw))) {
                    continue;
                }
                all.add(item);
            }
        }
        all.sort(METHOD_RESULT_COMPARATOR);
        int from = Math.min(begin, all.size());
        int to = Math.min(from + size, all.size());
        return filterWebMethods(all.subList(from, to));
    }

    private ArrayList<ClassResult> filterWebClasses(List<ClassResult> input) {
        ArrayList<ClassResult> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        for (ClassResult c : input) {
            if (c == null) {
                continue;
            }
            if (CommonFilterUtil.isFilteredClass(c.getClassName())
                    || CommonFilterUtil.isFilteredJar(c.getJarName())) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    private ArrayList<MethodResult> filterWebMethods(List<MethodResult> input) {
        ArrayList<MethodResult> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        for (MethodResult m : input) {
            if (m == null) {
                continue;
            }
            if (CommonFilterUtil.isFilteredClass(m.getClassName())
                    || CommonFilterUtil.isFilteredJar(m.getJarName())) {
                continue;
            }
            out.add(m);
        }
        return out;
    }

    private ArrayList<ResourceEntity> filterResources(List<ResourceEntity> input) {
        ArrayList<ResourceEntity> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        for (ResourceEntity resource : input) {
            if (resource == null) {
                continue;
            }
            if (CommonFilterUtil.isFilteredResourcePath(resource.getResourcePath())) {
                continue;
            }
            out.add(resource);
        }
        return out;
    }

    public ArrayList<MethodResult> getMethodsByAnnoNames(List<String> annoNames) {
        if (annoNames == null || annoNames.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> targets = annoNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (targets.isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodReference method : DatabaseManager.getMethodReferences()) {
            if (method == null || method.getAnnotations() == null || method.getAnnotations().isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (AnnoReference anno : method.getAnnotations()) {
                if (anno != null && targets.contains(safe(anno.getAnnoName()))) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                out.add(toMethodResult(method));
            }
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<AnnoMethodResult> getMethodsByAnno(List<String> annoNames,
                                                        String match,
                                                        String scope,
                                                        Integer jarId,
                                                        Integer offset,
                                                        Integer limit) {
        if (annoNames == null || annoNames.isEmpty()) {
            return new ArrayList<>();
        }
        String matchMode = safe(match).trim().toLowerCase(Locale.ROOT);
        if (!"equal".equals(matchMode)) {
            matchMode = "contains";
        }
        String scopeMode = safe(scope).trim().toLowerCase(Locale.ROOT);
        if (!"class".equals(scopeMode) && !"method".equals(scopeMode)) {
            scopeMode = "any";
        }
        int begin = offset == null || offset < 0 ? 0 : offset;
        int max = limit == null || limit <= 0 ? 100 : limit;

        ArrayList<AnnoMethodResult> out = new ArrayList<>();
        for (MethodReference method : DatabaseManager.getMethodReferences()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            int methodJarId = normalizeJarId(method.getJarId());
            if (jarId != null && jarId >= 0 && methodJarId != jarId) {
                continue;
            }

            Set<String> classAnnos = new LinkedHashSet<>(getAnnoByClassName(method.getClassReference().getName()));
            Set<String> methodAnnos = new LinkedHashSet<>();
            if (method.getAnnotations() != null) {
                for (AnnoReference anno : method.getAnnotations()) {
                    if (anno != null && anno.getAnnoName() != null) {
                        methodAnnos.add(anno.getAnnoName());
                    }
                }
            }

            if (!"method".equals(scopeMode)) {
                appendAnnoMatches(out, annoNames, matchMode, "class", classAnnos, method, methodJarId);
            }
            if (!"class".equals(scopeMode)) {
                appendAnnoMatches(out, annoNames, matchMode, "method", methodAnnos, method, methodJarId);
            }
        }
        out.sort(Comparator.comparing(AnnoMethodResult::getClassName)
                .thenComparing(AnnoMethodResult::getMethodName)
                .thenComparing(AnnoMethodResult::getMethodDesc)
                .thenComparing(AnnoMethodResult::getAnnoName));
        int from = Math.min(begin, out.size());
        int to = Math.min(from + max, out.size());
        return new ArrayList<>(out.subList(from, to));
    }

    private void appendAnnoMatches(List<AnnoMethodResult> out,
                                   List<String> targets,
                                   String matchMode,
                                   String scope,
                                   Set<String> candidates,
                                   MethodReference method,
                                   int methodJarId) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (String target : targets) {
            String t = safe(target);
            if (t.isEmpty()) {
                continue;
            }
            for (String candidate : candidates) {
                String c = safe(candidate);
                if (c.isEmpty()) {
                    continue;
                }
                boolean ok = "equal".equals(matchMode) ? c.equals(t) : c.contains(t);
                if (!ok) {
                    continue;
                }
                AnnoMethodResult row = new AnnoMethodResult();
                row.setClassName(normalizeClassName(method.getClassReference().getName()));
                row.setMethodName(safe(method.getName()));
                row.setMethodDesc(safe(method.getDesc()));
                row.setLineNumber(method.getLineNumber());
                row.setJarId(methodJarId);
                row.setJarName(resolveJarName(methodJarId, method.getJarName()));
                row.setAnnoName(c);
                row.setAnnoScope(scope);
                out.add(row);
            }
        }
    }

    public ArrayList<String> getAnnoByClassName(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        ClassReference ref = DatabaseManager.getClassReferenceByName(className, null);
        if (ref == null || ref.getAnnotations() == null || ref.getAnnotations().isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<String> out = new ArrayList<>();
        for (AnnoReference anno : ref.getAnnotations()) {
            if (anno == null || anno.getAnnoName() == null || anno.getAnnoName().isBlank()) {
                continue;
            }
            out.add(anno.getAnnoName());
        }
        return out;
    }

    public ArrayList<String> getAnnoByClassAndMethod(String className, String methodName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
            return new ArrayList<>();
        }
        ArrayList<String> out = new ArrayList<>();
        for (MethodReference method : DatabaseManager.getMethodReferencesByClass(className)) {
            if (method == null || !safe(method.getName()).equals(methodName)) {
                continue;
            }
            if (method.getAnnotations() == null) {
                continue;
            }
            for (AnnoReference anno : method.getAnnotations()) {
                if (anno == null || anno.getAnnoName() == null || anno.getAnnoName().isBlank()) {
                    continue;
                }
                out.add(anno.getAnnoName());
            }
        }
        return out;
    }

    public ArrayList<String> getStrings(int page) {
        int p = Math.max(page, 1);
        int offset = (p - 1) * PAGE_SIZE;
        ArrayList<String> all = allStrings();
        if (offset >= all.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(offset + PAGE_SIZE, all.size());
        return new ArrayList<>(all.subList(offset, to));
    }

    public int getStringCount() {
        return allStrings().size();
    }

    public ArrayList<MethodResult> getMethodsByClassNoJar(String className) {
        ArrayList<MethodResult> rows = getMethodsByClass(className);
        LinkedHashMap<String, MethodResult> unique = new LinkedHashMap<>();
        for (MethodResult row : rows) {
            String key = safe(row.getClassName()) + "#" + safe(row.getMethodName()) + "#" + safe(row.getMethodDesc());
            unique.putIfAbsent(key, row);
        }
        return new ArrayList<>(unique.values());
    }

    public MemberEntity getMemberByClassAndName(String className, String memberName) {
        ArrayList<MemberEntity> rows = getMembersByClassAndName(className, memberName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ArrayList<MemberEntity> getMembersByClassAndName(String className, String memberName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(memberName)) {
            return new ArrayList<>();
        }
        ArrayList<MemberEntity> rows = new ArrayList<>();
        for (MemberEntity member : getMembersByClass(className)) {
            if (member != null && safe(member.getMemberName()).equals(memberName)) {
                rows.add(member);
            }
        }
        return rows;
    }

    public ArrayList<MemberEntity> getMembersByClass(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        ClassReference ref = DatabaseManager.getClassReferenceByName(className, null);
        if (ref == null || ref.getMembers() == null || ref.getMembers().isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<MemberEntity> out = new ArrayList<>();
        int mid = 1;
        for (ClassReference.Member member : ref.getMembers()) {
            if (member == null) {
                continue;
            }
            MemberEntity row = new MemberEntity();
            row.setMid(mid++);
            row.setClassName(normalizeClassName(ref.getName()));
            row.setJarId(normalizeJarId(ref.getJarId()));
            row.setMemberName(safe(member.getName()));
            row.setModifiers(member.getModifiers());
            row.setValue(safe(member.getValue()));
            row.setMethodDesc(safe(member.getDesc()));
            row.setMethodSignature(safe(member.getSignature()));
            String typeClass = member.getType() == null ? "" : safe(member.getType().getName());
            row.setTypeClassName(typeClass);
            out.add(row);
        }
        return out;
    }

    public ArrayList<String> getInterfacesByClass(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        ClassReference ref = DatabaseManager.getClassReferenceByName(className, null);
        if (ref == null || ref.getInterfaces() == null || ref.getInterfaces().isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String item : ref.getInterfaces()) {
            String normalized = normalizeClassName(item);
            if (normalized.isEmpty()) {
                continue;
            }
            out.add(normalized);
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    public int updateMethod(String className, String methodName, String methodDesc, String newItem) {
        // no-op in neo4j-only mode
        return 0;
    }

    public Set<ClassReference.Handle> getSuperClasses(ClassReference.Handle ch) {
        if (ch == null) {
            return new HashSet<>();
        }
        Set<ClassReference.Handle> out = new LinkedHashSet<>();
        for (String value : collectSuperClasses(ch.getName())) {
            if (value.equals(normalizeClassName(ch.getName()))) {
                continue;
            }
            out.add(new ClassReference.Handle(value));
        }
        return out;
    }

    public Set<ClassReference.Handle> getSubClasses(ClassReference.Handle ch) {
        if (ch == null) {
            return new HashSet<>();
        }
        Set<ClassReference.Handle> out = new LinkedHashSet<>();
        String normalized = normalizeClassName(ch.getName());
        for (String value : collectSubClasses(normalized)) {
            if (value.equals(normalized)) {
                continue;
            }
            out.add(new ClassReference.Handle(value));
        }
        return out;
    }

    public ClassReference getClassRef(ClassReference.Handle ch, Integer jarId) {
        if (ch == null || ch.getName() == null || ch.getName().isBlank()) {
            return null;
        }
        ClassReference ref = DatabaseManager.getClassReferenceByName(ch.getName(), jarId);
        if (ref != null) {
            return ref;
        }
        ClassResult fallback = getClassByClass(ch.getName());
        if (fallback == null) {
            return null;
        }
        return new ClassReference(
                fallback.getClassName(),
                fallback.getSuperClassName(),
                Collections.emptyList(),
                fallback.getIsInterfaceInt() == 1,
                Collections.emptyList(),
                new ArrayList<>(),
                safe(fallback.getJarName()),
                fallback.getJarId()
        );
    }

    public int getMethodsCount() {
        return DatabaseManager.getMethodReferences().size();
    }

    public ArrayList<MethodReference> getAllMethodRef(int offset) {
        int begin = Math.max(offset, 0);
        int end = Math.min(begin + PAGE_SIZE, DatabaseManager.getMethodReferences().size());
        if (begin >= end) {
            return new ArrayList<>();
        }
        List<MethodReference> methods = DatabaseManager.getMethodReferences();
        methods.sort((a, b) -> {
            String ac = a == null || a.getClassReference() == null ? "" : safe(a.getClassReference().getName());
            String bc = b == null || b.getClassReference() == null ? "" : safe(b.getClassReference().getName());
            int c = ac.compareTo(bc);
            if (c != 0) {
                return c;
            }
            c = safe(a == null ? null : a.getName()).compareTo(safe(b == null ? null : b.getName()));
            if (c != 0) {
                return c;
            }
            c = safe(a == null ? null : a.getDesc()).compareTo(safe(b == null ? null : b.getDesc()));
            if (c != 0) {
                return c;
            }
            int aj = a == null ? -1 : normalizeJarId(a.getJarId());
            int bj = b == null ? -1 : normalizeJarId(b.getJarId());
            return Integer.compare(aj, bj);
        });
        return new ArrayList<>(methods.subList(begin, end));
    }

    public List<MemberEntity> getAllMembersInfo() {
        ArrayList<MemberEntity> members = new ArrayList<>();
        for (ClassReference ref : DatabaseManager.getClassReferences()) {
            if (ref == null || ref.getMembers() == null || ref.getMembers().isEmpty()) {
                continue;
            }
            for (MemberEntity item : getMembersByClass(ref.getName())) {
                if (item != null && "java/lang/String".equals(item.getTypeClassName())) {
                    members.add(item);
                }
            }
        }
        return members;
    }

    public Map<String, String> getStringMap() {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : DatabaseManager.getMethodStringsSnapshot().entrySet()) {
            if (entry == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String className = parseClassFromMethodKey(entry.getKey());
            if (className.isEmpty()) {
                continue;
            }
            out.putIfAbsent(className, entry.getValue().get(0));
        }
        return out;
    }

    public ArrayList<ResourceEntity> getResources(String path, Integer jarId, int offset, int limit) {
        String p = safe(path);
        int begin = Math.max(offset, 0);
        int size = limit <= 0 ? 100 : limit;
        ArrayList<ResourceEntity> filtered = new ArrayList<>();
        for (ResourceEntity row : DatabaseManager.getResources()) {
            if (row == null) {
                continue;
            }
            if (jarId != null && jarId >= 0 && normalizeJarId(row.getJarId()) != jarId) {
                continue;
            }
            if (!p.isEmpty() && !safe(row.getResourcePath()).contains(p)) {
                continue;
            }
            filtered.add(row);
        }
        filtered = filterResources(filtered);
        int from = Math.min(begin, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new ArrayList<>(filtered.subList(from, to));
    }

    public int getResourceCount(String path, Integer jarId) {
        return getResources(path, jarId, 0, Integer.MAX_VALUE).size();
    }

    public ResourceEntity getResourceById(int rid) {
        for (ResourceEntity row : DatabaseManager.getResources()) {
            if (row == null || row.getRid() != rid) {
                continue;
            }
            if (CommonFilterUtil.isFilteredResourcePath(row.getResourcePath())) {
                return null;
            }
            return row;
        }
        return null;
    }

    public ResourceEntity getResourceByPath(Integer jarId, String path) {
        String target = safe(path);
        if (target.isEmpty()) {
            return null;
        }
        for (ResourceEntity row : DatabaseManager.getResources()) {
            if (row == null) {
                continue;
            }
            if (jarId != null && jarId >= 0 && normalizeJarId(row.getJarId()) != jarId) {
                continue;
            }
            if (!target.equals(safe(row.getResourcePath()))) {
                continue;
            }
            if (CommonFilterUtil.isFilteredResourcePath(row.getResourcePath())) {
                return null;
            }
            return row;
        }
        return null;
    }

    public ArrayList<ResourceEntity> getResourcesByPath(String path, Integer limit) {
        String target = safe(path);
        int size = limit == null || limit <= 0 ? 100 : limit;
        ArrayList<ResourceEntity> out = new ArrayList<>();
        for (ResourceEntity row : DatabaseManager.getResources()) {
            if (row == null) {
                continue;
            }
            if (!target.isEmpty() && !safe(row.getResourcePath()).contains(target)) {
                continue;
            }
            out.add(row);
            if (out.size() >= size) {
                break;
            }
        }
        return filterResources(out);
    }

    public ArrayList<ResourceEntity> getTextResources(Integer jarId) {
        ArrayList<ResourceEntity> out = new ArrayList<>();
        for (ResourceEntity row : DatabaseManager.getResources()) {
            if (row == null) {
                continue;
            }
            if (jarId != null && jarId >= 0 && normalizeJarId(row.getJarId()) != jarId) {
                continue;
            }
            if (row.getIsText() != 1) {
                continue;
            }
            out.add(row);
        }
        return filterResources(out);
    }

    public void cleanFav() {
        DatabaseManager.cleanFav();
    }

    public void cleanFavItem(MethodResult m) {
        DatabaseManager.cleanFavItem(m);
    }

    public void addFav(MethodResult m) {
        DatabaseManager.addFav(m);
    }

    public void insertHistory(MethodResult m) {
        DatabaseManager.insertHistory(m);
    }

    public void cleanHistory() {
        DatabaseManager.cleanHistory();
    }

    public ArrayList<MethodResult> getAllFavMethods() {
        return DatabaseManager.getAllFavMethods();
    }

    public ArrayList<MethodResult> getAllHisMethods() {
        return DatabaseManager.getAllHisMethods();
    }

    private GraphSnapshot loadSnapshotSafe() {
        try {
            return graphStore.loadSnapshot();
        } catch (Exception ex) {
            logger.debug("load graph snapshot failed: {}", ex.toString());
            return GraphSnapshot.empty();
        }
    }

    private Map<Long, MethodResult> methodNodeMap(GraphSnapshot snapshot) {
        Map<Long, MethodResult> methods = new HashMap<>();
        if (snapshot == null) {
            return methods;
        }
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            if (node == null) {
                continue;
            }
            methods.put(node.getNodeId(), toMethodResult(node));
        }
        return methods;
    }

    private ArrayList<MethodCallResult> filterCallEdges(boolean byCaller,
                                                        String className,
                                                        String methodName,
                                                        String methodDesc,
                                                        Integer offset,
                                                        Integer limit) {
        GraphSnapshot snapshot = loadSnapshotSafe();
        Map<Long, MethodResult> methods = methodNodeMap(snapshot);
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();

        ArrayList<MethodCallResult> out = new ArrayList<>();
        for (Map.Entry<Long, MethodResult> entry : methods.entrySet()) {
            long srcId = entry.getKey();
            MethodResult src = entry.getValue();
            for (GraphEdge edge : snapshot.getOutgoingView(srcId)) {
                if (!isCallEdge(edge.getRelType())) {
                    continue;
                }
                MethodResult dst = methods.get(edge.getDstId());
                if (dst == null) {
                    continue;
                }
                MethodResult side = byCaller ? src : dst;
                if (!cls.isEmpty() && !cls.equals(normalizeClassName(side.getClassName()))) {
                    continue;
                }
                if (!method.isEmpty() && !method.equals(side.getMethodName())) {
                    continue;
                }
                if (!desc.isEmpty() && !desc.equals(side.getMethodDesc())) {
                    continue;
                }
                MethodCallResult row = new MethodCallResult();
                row.setCallerClassName(src.getClassName());
                row.setCallerMethodName(src.getMethodName());
                row.setCallerMethodDesc(src.getMethodDesc());
                row.setCallerJarId(src.getJarId());
                row.setCallerJarName(src.getJarName());
                row.setCalleeClassName(dst.getClassName());
                row.setCalleeMethodName(dst.getMethodName());
                row.setCalleeMethodDesc(dst.getMethodDesc());
                row.setCalleeJarId(dst.getJarId());
                row.setCalleeJarName(dst.getJarName());
                row.setOpCode(edge.getOpCode());
                row.setEdgeType(resolveEdgeType(edge));
                row.setEdgeConfidence(safe(edge.getConfidence()));
                row.setEdgeEvidence(safe(edge.getEvidence()));
                out.add(row);
            }
        }
        out.sort(Comparator.comparing(MethodCallResult::getCallerClassName)
                .thenComparing(MethodCallResult::getCallerMethodName)
                .thenComparing(MethodCallResult::getCallerMethodDesc)
                .thenComparing(MethodCallResult::getCalleeClassName)
                .thenComparing(MethodCallResult::getCalleeMethodName)
                .thenComparing(MethodCallResult::getCalleeMethodDesc));
        int begin = offset == null || offset < 0 ? 0 : offset;
        int size = limit == null || limit <= 0 ? out.size() : limit;
        int from = Math.min(begin, out.size());
        int to = Math.min(from + size, out.size());
        return new ArrayList<>(out.subList(from, to));
    }

    private static boolean isCallEdge(String relType) {
        if (relType == null || relType.isBlank()) {
            return false;
        }
        return relType.startsWith("CALLS_");
    }

    private String resolveEdgeType(GraphEdge edge) {
        if (edge == null) {
            return MethodCallMeta.TYPE_DIRECT;
        }
        String relation = safe(edge.getRelType());
        return switch (relation) {
            case "CALLS_DISPATCH" -> MethodCallMeta.TYPE_DISPATCH;
            case "CALLS_REFLECTION" -> MethodCallMeta.TYPE_REFLECTION;
            case "CALLS_CALLBACK" -> MethodCallMeta.TYPE_CALLBACK;
            case "CALLS_OVERRIDE" -> MethodCallMeta.TYPE_OVERRIDE;
            case "CALLS_FRAMEWORK" -> MethodCallMeta.TYPE_FRAMEWORK;
            case "CALLS_METHOD_HANDLE" -> MethodCallMeta.TYPE_METHOD_HANDLE;
            case "CALLS_PTA" -> MethodCallMeta.TYPE_PTA;
            case "CALLS_INDY" -> MethodCallMeta.TYPE_INDY;
            default -> resolveEdgeTypeFromEvidence(edge.getEvidence());
        };
    }

    private String resolveEdgeTypeFromEvidence(String evidence) {
        String ev = safe(evidence).toLowerCase(Locale.ROOT);
        if (ev.contains("pta:" ) || ev.contains("pta_ctx=") || ev.contains("|pta:")) {
            return MethodCallMeta.TYPE_PTA;
        }
        if (ev.contains("method_handle") || ev.contains("mh:")) {
            return MethodCallMeta.TYPE_METHOD_HANDLE;
        }
        if (ev.contains("framework") || ev.contains("spring_web_entry")) {
            return MethodCallMeta.TYPE_FRAMEWORK;
        }
        if (ev.contains("callback") || ev.contains("dynamic_proxy")) {
            return MethodCallMeta.TYPE_CALLBACK;
        }
        if (ev.contains("reflection") || ev.contains("tamiflex") || ev.contains("reflect")) {
            return MethodCallMeta.TYPE_REFLECTION;
        }
        return MethodCallMeta.TYPE_DIRECT;
    }

    private MethodResult toMethodResult(GraphNode node) {
        MethodReference ref = findMethodReference(node.getClassName(), node.getMethodName(), node.getMethodDesc(), node.getJarId());
        if (ref != null) {
            return toMethodResult(ref);
        }
        MethodResult out = new MethodResult();
        out.setClassName(normalizeClassName(node.getClassName()));
        out.setMethodName(safe(node.getMethodName()));
        out.setMethodDesc(safe(node.getMethodDesc()));
        out.setJarId(normalizeJarId(node.getJarId()));
        out.setJarName(resolveJarName(out.getJarId(), null));
        out.setLineNumber(node.getLineNumber());
        out.setAccessInt(0);
        out.setIsStaticInt(0);
        return out;
    }

    private MethodResult toMethodResult(MethodReference ref) {
        MethodResult out = new MethodResult();
        String className = ref.getClassReference() == null ? "" : normalizeClassName(ref.getClassReference().getName());
        out.setClassName(className);
        out.setMethodName(safe(ref.getName()));
        out.setMethodDesc(safe(ref.getDesc()));
        out.setIsStaticInt(ref.isStatic() ? 1 : 0);
        out.setAccessInt(ref.getAccess());
        out.setLineNumber(ref.getLineNumber());
        int jarId = normalizeJarId(ref.getJarId());
        out.setJarId(jarId);
        out.setJarName(resolveJarName(jarId, ref.getJarName()));
        return out;
    }

    private MethodReference findMethodReference(String className,
                                                String methodName,
                                                String methodDesc,
                                                Integer jarId) {
        String cls = normalizeClassName(className);
        int j = normalizeJarId(jarId);
        MethodReference fallback = null;
        for (MethodReference ref : DatabaseManager.getMethodReferencesByClass(cls)) {
            if (ref == null || ref.getClassReference() == null) {
                continue;
            }
            if (!safe(ref.getName()).equals(safe(methodName))) {
                continue;
            }
            if (!safe(ref.getDesc()).equals(safe(methodDesc))) {
                continue;
            }
            if (normalizeJarId(ref.getJarId()) == j) {
                return ref;
            }
            if (fallback == null) {
                fallback = ref;
            }
        }
        return fallback;
    }

    private ClassResult toClassResult(ClassReference row) {
        if (row == null || row.getName() == null || row.getName().isBlank()) {
            return null;
        }
        ClassResult out = new ClassResult();
        out.setClassName(normalizeClassName(row.getName()));
        out.setSuperClassName(normalizeClassName(row.getSuperClass()));
        out.setIsInterfaceInt(row.isInterface() ? 1 : 0);
        int jarId = normalizeJarId(row.getJarId());
        out.setJarId(jarId);
        out.setJarName(resolveJarName(jarId, row.getJarName()));
        return out;
    }

    private ArrayList<ClassResult> classesFromNames(Set<String> classNames) {
        ArrayList<ClassResult> out = new ArrayList<>();
        if (classNames == null || classNames.isEmpty()) {
            return out;
        }
        for (String className : classNames) {
            ArrayList<ClassResult> rows = getClassesByClass(className);
            if (rows.isEmpty()) {
                ClassResult fallback = new ClassResult();
                fallback.setClassName(normalizeClassName(className));
                fallback.setJarId(-1);
                fallback.setJarName("unknown");
                fallback.setSuperClassName("java/lang/Object");
                fallback.setIsInterfaceInt(0);
                out.add(fallback);
                continue;
            }
            out.addAll(rows);
        }
        out.sort(Comparator.comparing(ClassResult::getClassName)
                .thenComparingInt(ClassResult::getJarId));
        return out;
    }

    private MethodResult toSpringMethod(SpringMapping mapping) {
        if (mapping == null) {
            return null;
        }
        MethodReference method = mapping.getMethodReference();
        if (method == null) {
            return null;
        }
        MethodResult out = toMethodResult(method);
        String path = safe(mapping.getPathRestful());
        if (path.isEmpty()) {
            path = safe(mapping.getPath());
        }
        out.setPath(path);
        return out;
    }

    private Set<String> collectKnownClasses() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (ClassReference ref : DatabaseManager.getClassReferences()) {
            if (ref == null) {
                continue;
            }
            String normalized = normalizeClassName(ref.getName());
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        for (MethodReference method : DatabaseManager.getMethodReferences()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String normalized = normalizeClassName(method.getClassReference().getName());
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private Set<String> collectSuperClasses(String className) {
        String start = normalizeClassName(className);
        if (start.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        ArrayList<String> queue = new ArrayList<>();
        queue.add(start);
        int idx = 0;
        while (idx < queue.size() && out.size() < 256) {
            String current = queue.get(idx++);
            if (current.isEmpty() || !out.add(current)) {
                continue;
            }
            ClassReference ref = DatabaseManager.getClassReferenceByName(current, null);
            if (ref == null) {
                continue;
            }
            String superClass = normalizeClassName(ref.getSuperClass());
            if (!superClass.isEmpty()) {
                queue.add(superClass);
            }
            if (ref.getInterfaces() != null) {
                for (String iface : ref.getInterfaces()) {
                    String normalized = normalizeClassName(iface);
                    if (!normalized.isEmpty()) {
                        queue.add(normalized);
                    }
                }
            }
        }
        return out;
    }

    private Set<String> collectSubClasses(String className) {
        String target = normalizeClassName(className);
        if (target.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(target);
        boolean changed;
        do {
            changed = false;
            for (ClassReference ref : DatabaseManager.getClassReferences()) {
                if (ref == null) {
                    continue;
                }
                String cls = normalizeClassName(ref.getName());
                if (cls.isEmpty() || out.contains(cls)) {
                    continue;
                }
                String superClass = normalizeClassName(ref.getSuperClass());
                if (!superClass.isEmpty() && out.contains(superClass)) {
                    out.add(cls);
                    changed = true;
                    continue;
                }
                if (ref.getInterfaces() == null || ref.getInterfaces().isEmpty()) {
                    continue;
                }
                for (String iface : ref.getInterfaces()) {
                    String normalized = normalizeClassName(iface);
                    if (!normalized.isEmpty() && out.contains(normalized)) {
                        out.add(cls);
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed && out.size() < 4096);
        return out;
    }

    private ArrayList<MethodResult> dedupMethods(List<MethodResult> input) {
        LinkedHashMap<String, MethodResult> map = new LinkedHashMap<>();
        if (input != null) {
            for (MethodResult row : input) {
                if (row == null) {
                    continue;
                }
                String key = methodKey(row.getClassName(), row.getMethodName(), row.getMethodDesc(), row.getJarId());
                map.putIfAbsent(key, row);
            }
        }
        ArrayList<MethodResult> out = new ArrayList<>(map.values());
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    private ArrayList<String> allStrings() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (List<String> values : DatabaseManager.getMethodStringsSnapshot().values()) {
            if (values == null) {
                continue;
            }
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    out.add(value);
                }
            }
        }
        ArrayList<String> rows = new ArrayList<>(out);
        rows.sort(Comparator.naturalOrder());
        return rows;
    }

    private String parseClassFromMethodKey(String key) {
        String value = safe(key);
        int idx = value.indexOf('#');
        if (idx <= 0) {
            return "";
        }
        return value.substring(0, idx);
    }

    private String lineMappingKey(String className, Integer jarId, String decompiler) {
        String cls = normalizeClassName(className);
        return cls + "#" + normalizeJarId(jarId) + "#" + safe(decompiler).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesLike(String target, String expected) {
        String t = safe(target);
        String e = safe(expected);
        if (e.isEmpty()) {
            return true;
        }
        return t.contains(e);
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            return "";
        }
        name = name.replace('\\', '/');
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
        }
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.indexOf('/') < 0 && name.indexOf('.') >= 0) {
            name = name.replace('.', '/');
        }
        return name;
    }

    private static String resolveJarName(int jarId, String fallback) {
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if (jarId < 0) {
            return "unknown";
        }
        JarEntity jar = DatabaseManager.getJarById(jarId);
        if (jar == null || jar.getJarName() == null || jar.getJarName().isBlank()) {
            return "unknown";
        }
        return jar.getJarName();
    }

    private static String methodKey(String className,
                                    String methodName,
                                    String methodDesc,
                                    Integer jarId) {
        return normalizeClassName(className) + "#"
                + safe(methodName) + "#"
                + safe(methodDesc) + "#"
                + normalizeJarId(jarId);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final Comparator<MethodResult> METHOD_RESULT_COMPARATOR = Comparator
            .comparing(MethodResult::getClassName, Comparator.nullsLast(String::compareTo))
            .thenComparing(MethodResult::getMethodName, Comparator.nullsLast(String::compareTo))
            .thenComparing(MethodResult::getMethodDesc, Comparator.nullsLast(String::compareTo))
            .thenComparingInt(MethodResult::getJarId);
}
