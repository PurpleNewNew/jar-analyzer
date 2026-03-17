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
import me.n1ar4.jar.analyzer.engine.model.AnnoMethodView;
import me.n1ar4.jar.analyzer.core.facts.CallSiteEntity;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.engine.model.ClassView;
import me.n1ar4.jar.analyzer.core.facts.JarEntity;
import me.n1ar4.jar.analyzer.core.facts.LocalVarEntity;
import me.n1ar4.jar.analyzer.core.facts.MemberEntity;
import me.n1ar4.jar.analyzer.engine.model.CallEdgeView;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectGraphStoreFacade;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StableOrder;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final int PAGE_SIZE = 100;
    private static final ProjectGraphStoreFacade PROJECT_STORE = ProjectGraphStoreFacade.getInstance();

    private final GraphStore graphStore = new GraphStore();
    private volatile CallGraphCache callGraphCache;

    public boolean isEnabled() {
        try {
            if (!DatabaseManager.isProjectReady()) {
                return false;
            }
            String projectKey = ActiveProjectContext.getActiveProjectKey();
            Path home = PROJECT_STORE.resolveProjectHome(projectKey);
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
        GraphSnapshot snapshot = loadQuerySnapshotOrThrow();
        Map<Integer, String> jarNames = new HashMap<>();
        for (JarEntity jar : DatabaseManager.getJarsMeta()) {
            if (jar == null) {
                continue;
            }
            jarNames.put(jar.getJid(), safe(jar.getJarName()));
        }
        CallGraphCache cache = CallGraphCache.build(snapshot, jarNames);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        logger.info("call graph cache loaded: edges={}, methods={}, elapsedMs={}",
                cache.getEdgeCount(), cache.getMethodCount(), elapsedMs);
        return cache;
    }

    public ArrayList<MethodView> getMethodsByClass(String className) {
        ArrayList<MethodView> results = new ArrayList<>();
        for (MethodReference method : DatabaseManager.getMethodReferencesByClass(className)) {
            results.add(toMethodView(method));
        }
        results.sort(Comparator.comparing(MethodView::getMethodName)
                .thenComparing(MethodView::getMethodDesc)
                .thenComparingInt(MethodView::getJarId));
        return results;
    }

    public ClassView getClassByClass(String className) {
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return null;
        }
        ArrayList<ClassView> results = getClassesByClass(className);
        return results.isEmpty() ? null : results.get(0);
    }

    public ArrayList<ClassView> getClassesByClass(String className) {
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return new ArrayList<>();
        }
        List<ClassReference> rows = DatabaseManager.getClassReferencesByName(className);
        ArrayList<ClassView> results = new ArrayList<>();
        for (ClassReference row : rows) {
            ClassView result = toClassView(row);
            if (result != null) {
                results.add(result);
            }
        }
        results.sort(Comparator.comparingInt(ClassView::getJarId)
                .thenComparing(ClassView::getJarName, Comparator.nullsLast(String::compareTo)));
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
        return row.resolvePathStr();
    }

    public String getAbsPath(String className, Integer jarId) {
        ClassFileEntity row = DatabaseManager.getClassFileByClass(className, jarId);
        if (row == null) {
            return getAbsPath(className);
        }
        return row.resolvePathStr();
    }

    public ArrayList<MethodView> getCallers(String calleeClass, String calleeMethod, String calleeDesc) {
        return getCallers(calleeClass, calleeMethod, calleeDesc, null);
    }

    public ArrayList<MethodView> getCallers(String calleeClass,
                                              String calleeMethod,
                                              String calleeDesc,
                                              Integer calleeJarId) {
        return getCallGraphCache().getCallers(calleeClass, calleeMethod, calleeDesc, normalizeJarId(calleeJarId));
    }

    public ArrayList<MethodView> getCallersLike(String calleeClass, String calleeMethod, String calleeDesc) {
        String cls = normalizeClassName(calleeClass);
        String method = safe(calleeMethod);
        String desc = safe(calleeDesc);
        ArrayList<MethodView> out = new ArrayList<>();
        GraphSnapshot snapshot = loadQuerySnapshotOrThrow();
        Map<Long, MethodView> methods = methodNodeMap(snapshot);
        for (Map.Entry<Long, MethodView> entry : methods.entrySet()) {
            MethodView callee = entry.getValue();
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

    public ArrayList<MethodView> getCallee(String callerClass, String callerMethod, String callerDesc) {
        return getCallee(callerClass, callerMethod, callerDesc, null);
    }

    public ArrayList<MethodView> getCallee(String callerClass,
                                             String callerMethod,
                                             String callerDesc,
                                             Integer callerJarId) {
        return getCallGraphCache().getCallees(callerClass, callerMethod, callerDesc, normalizeJarId(callerJarId));
    }

    public ArrayList<CallEdgeView> getCallEdgesByCallee(String calleeClass,
                                                            String calleeMethod,
                                                            String calleeDesc,
                                                            Integer offset,
                                                            Integer limit) {
        return getCallEdgesByCallee(calleeClass, calleeMethod, calleeDesc, null, offset, limit);
    }

    public ArrayList<CallEdgeView> getCallEdgesByCallee(String calleeClass,
                                                            String calleeMethod,
                                                            String calleeDesc,
                                                            Integer calleeJarId,
                                                            Integer offset,
                                                            Integer limit) {
        return filterCallEdges(false, calleeClass, calleeMethod, calleeDesc, calleeJarId, offset, limit);
    }

    public ArrayList<CallEdgeView> getCallEdgesByCaller(String callerClass,
                                                            String callerMethod,
                                                            String callerDesc,
                                                            Integer offset,
                                                            Integer limit) {
        return getCallEdgesByCaller(callerClass, callerMethod, callerDesc, null, offset, limit);
    }

    public ArrayList<CallEdgeView> getCallEdgesByCaller(String callerClass,
                                                            String callerMethod,
                                                            String callerDesc,
                                                            Integer callerJarId,
                                                            Integer offset,
                                                            Integer limit) {
        return filterCallEdges(true, callerClass, callerMethod, callerDesc, callerJarId, offset, limit);
    }

    public MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        return getCallGraphCache().getEdgeMeta(caller, callee);
    }

    public PageSlice<CallEdgeView> getCallEdgesPageByCallee(String calleeClass,
                                                                String calleeMethod,
                                                                String calleeDesc,
                                                                Integer calleeJarId,
                                                                int offset,
                                                                int limit,
                                                                boolean includeJdk) {
        List<CallEdgeView> rows = getCallGraphCache().viewCallEdgesByCallee(
                normalizeClassName(calleeClass),
                safe(calleeMethod).trim(),
                safe(calleeDesc).trim(),
                calleeJarId
        );
        OrderedPageCollector<CallEdgeView> collector =
                new OrderedPageCollector<>(StableOrder.METHOD_CALL_RESULT, offset, limit);
        for (CallEdgeView row : rows) {
            if (isVisibleCallEdge(row, includeJdk, true)) {
                collector.accept(row);
            }
        }
        return collector.finish();
    }

    public PageSlice<CallEdgeView> getCallEdgesPageByCaller(String callerClass,
                                                                String callerMethod,
                                                                String callerDesc,
                                                                Integer callerJarId,
                                                                int offset,
                                                                int limit,
                                                                boolean includeJdk) {
        List<CallEdgeView> rows = getCallGraphCache().viewCallEdgesByCaller(
                normalizeClassName(callerClass),
                safe(callerMethod).trim(),
                safe(callerDesc).trim(),
                callerJarId
        );
        OrderedPageCollector<CallEdgeView> collector =
                new OrderedPageCollector<>(StableOrder.METHOD_CALL_RESULT, offset, limit);
        for (CallEdgeView row : rows) {
            if (isVisibleCallEdge(row, includeJdk, false)) {
                collector.accept(row);
            }
        }
        return collector.finish();
    }

    public Map<MethodCallKey, MethodCallMeta> getEdgeMetaBatch(List<MethodCallKey> keys) {
        Map<MethodCallKey, MethodCallMeta> out = new HashMap<>();
        if (keys == null || keys.isEmpty()) {
            return out;
        }
        CallGraphCache cache = getCallGraphCache();
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

    public ArrayList<MethodView> getMethod(String className, String methodName, String methodDesc) {
        ArrayList<MethodView> out = new ArrayList<>();
        for (MethodReference ref : getMethodReferences(className, methodName, methodDesc)) {
            out.add(toMethodView(ref));
        }
        return out;
    }

    public ArrayList<MethodReference> getMethodReferences(String className, String methodName, String methodDesc) {
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();

        Collection<MethodReference> source;
        if (!cls.isEmpty()) {
            source = DatabaseManager.getMethodReferencesByClass(cls);
        } else {
            source = DatabaseManager.getMethodReferences();
        }

        ArrayList<MethodReference> out = new ArrayList<>();
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
            out.add(ref);
        }
        out.sort(Comparator.comparing((MethodReference ref) ->
                        safe(ref.getClassReference() == null ? null : ref.getClassReference().getName()))
                .thenComparing(ref -> safe(ref.getName()))
                .thenComparing(ref -> safe(ref.getDesc()))
                .thenComparingInt(ref -> normalizeJarId(ref.getJarId())));
        return out;
    }

    public ArrayList<CallSiteEntity> getCallSitesByCaller(String className,
                                                          String methodName,
                                                          String methodDesc) {
        return new ArrayList<>(DatabaseManager.getCallSitesByCaller(className, methodName, methodDesc));
    }

    public ArrayList<LocalVarEntity> getLocalVarsByMethod(String className,
                                                          String methodName,
                                                          String methodDesc) {
        return new ArrayList<>(DatabaseManager.getLocalVarsByMethod(className, methodName, methodDesc));
    }

    public ArrayList<MethodView> getMethodLike(String className, String methodName, String methodDesc) {
        String cls = normalizeClassName(className);
        String method = safe(methodName).trim();
        String desc = safe(methodDesc).trim();
        ArrayList<MethodView> out = new ArrayList<>();
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
            out.add(toMethodView(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<MethodView> getMethodsByStr(String val) {
        return getMethodsByStr(val, null, null, null, "auto");
    }

    public ArrayList<MethodView> getMethodsByStrEqual(String val) {
        return getMethodsByStr(val, null, null, null, "equal");
    }

    public ArrayList<MethodView> getMethodsByStr(String val,
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

        ArrayList<MethodView> out = new ArrayList<>();
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
            LinkedHashSet<String> mergedValues = new LinkedHashSet<>(
                    DatabaseManager.getMethodStringValues(cls, ref.getName(), ref.getDesc(), refJarId));
            mergedValues.addAll(DatabaseManager.getMethodAnnoStringValues(
                    cls, ref.getName(), ref.getDesc(), refJarId));
            List<String> values = mergedValues.isEmpty() ? List.of() : new ArrayList<>(mergedValues);
            if (values.isEmpty()) {
                continue;
            }
            String matched = firstMatchedString(values, needle, m);
            if (matched == null) {
                continue;
            }
            MethodView result = toMethodView(ref);
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

    public PageSlice<MethodView> searchMethodsByStr(String val,
                                                      Integer jarId,
                                                      String classLike,
                                                      String mode,
                                                      int offset,
                                                      int limit,
                                                      boolean includeJdk) {
        String needle = safe(val);
        if (needle.isEmpty()) {
            return PageSlice.empty();
        }
        String classFilter = normalizeClassName(classLike);
        String m = safe(mode).trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty()) {
            m = "auto";
        }

        OrderedPageCollector<MethodView> collector =
                new OrderedPageCollector<>(StableOrder.METHOD_RESULT, offset, limit);
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
            LinkedHashSet<String> mergedValues = new LinkedHashSet<>(
                    DatabaseManager.getMethodStringValues(cls, ref.getName(), ref.getDesc(), refJarId));
            mergedValues.addAll(DatabaseManager.getMethodAnnoStringValues(
                    cls, ref.getName(), ref.getDesc(), refJarId));
            if (mergedValues.isEmpty()) {
                continue;
            }
            String matched = firstMatchedString(new ArrayList<>(mergedValues), needle, m);
            if (matched == null) {
                continue;
            }
            MethodView result = toMethodView(ref);
            result.setStrValue(matched);
            if (!isVisibleMethodView(result, includeJdk)) {
                continue;
            }
            String key = methodKey(result.getClassName(), result.getMethodName(), result.getMethodDesc(), result.getJarId());
            if (!seen.add(key)) {
                continue;
            }
            collector.accept(result);
        }
        return collector.finish();
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

    public ArrayList<MethodView> getImpls(String className,
                                            String methodName,
                                            String methodDesc) {
        return getImpls(className, methodName, methodDesc, null);
    }

    public ArrayList<MethodView> getImpls(String className,
                                            String methodName,
                                            String methodDesc,
                                            Integer classJarId) {
        String owner = normalizeClassName(className);
        if (owner.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> candidates = collectSubClasses(owner);
        ArrayList<MethodView> out = new ArrayList<>();
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
            out.add(toMethodView(ref));
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<MethodView> getSuperImpls(String className, String methodName, String methodDesc) {
        return getSuperImpls(className, methodName, methodDesc, null);
    }

    public ArrayList<MethodView> getSuperImpls(String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer classJarId) {
        String owner = normalizeClassName(className);
        if (owner.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> candidates = collectSuperClasses(owner);
        ArrayList<MethodView> out = new ArrayList<>();
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
            out.add(toMethodView(ref));
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

    public String getJarNameById(Integer jarId) {
        if (jarId == null) {
            return null;
        }
        JarEntity jar = DatabaseManager.getJarById(jarId);
        return jar == null ? null : jar.getJarName();
    }

    public ArrayList<ClassView> getAllSpringC() {
        ArrayList<ClassView> out = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null || controller.getClassName() == null) {
                continue;
            }
            String className = normalizeClassName(controller.getClassName().getName());
            if (className.isEmpty()) {
                continue;
            }
            ClassView result = getClassByClass(className);
            if (result == null) {
                result = new ClassView();
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

    public ArrayList<ClassView> getAllSpringI() {
        return filterWebClasses(classesFromNames(DatabaseManager.getSpringInterceptors()));
    }

    public ArrayList<ClassView> getAllServlets() {
        return filterWebClasses(classesFromNames(DatabaseManager.getServlets()));
    }

    public ArrayList<ClassView> getAllFilters() {
        return filterWebClasses(classesFromNames(DatabaseManager.getFilters()));
    }

    public ArrayList<ClassView> getAllListeners() {
        return filterWebClasses(classesFromNames(DatabaseManager.getListeners()));
    }

    public ArrayList<MethodView> getSpringM(String className) {
        if (CommonFilterUtil.isFilteredClass(className)) {
            return new ArrayList<>();
        }
        String target = normalizeClassName(className);
        ArrayList<MethodView> out = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null || controller.getClassName() == null) {
                continue;
            }
            String owner = normalizeClassName(controller.getClassName().getName());
            if (!owner.equals(target)) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodView item = toSpringMethod(mapping);
                if (item != null) {
                    out.add(item);
                }
            }
        }
        return filterWebMethods(out);
    }

    public ArrayList<MethodView> getSpringMappingsAll(Integer jarId,
                                                        String keyword,
                                                        Integer offset,
                                                        Integer limit) {
        String kw = safe(keyword);
        int begin = offset == null || offset < 0 ? 0 : offset;
        int size = limit == null || limit <= 0 ? 100 : limit;

        ArrayList<MethodView> all = new ArrayList<>();
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodView item = toSpringMethod(mapping);
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

    public int getSpringMappingsCount(Integer jarId, String keyword) {
        String kw = safe(keyword);
        int count = 0;
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodView item = toSpringMethod(mapping);
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
                if (CommonFilterUtil.isFilteredClass(item.getClassName())
                        || CommonFilterUtil.isFilteredJar(item.getJarName())) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    public PageSlice<MethodView> getSpringMappingsPage(Integer jarId,
                                                         String keyword,
                                                         int offset,
                                                         int limit,
                                                         boolean includeJdk) {
        String kw = safe(keyword);
        OrderedPageCollector<MethodView> collector =
                new OrderedPageCollector<>(StableOrder.METHOD_RESULT, offset, limit);
        for (SpringController controller : DatabaseManager.getSpringControllers()) {
            if (controller == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodView item = toSpringMethod(mapping);
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
                if (CommonFilterUtil.isFilteredClass(item.getClassName())
                        || CommonFilterUtil.isFilteredJar(item.getJarName())) {
                    continue;
                }
                if (!isVisibleMethodView(item, includeJdk)) {
                    continue;
                }
                collector.accept(item);
            }
        }
        return collector.finish();
    }

    private ArrayList<ClassView> filterWebClasses(List<ClassView> input) {
        ArrayList<ClassView> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        for (ClassView c : input) {
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

    private ArrayList<MethodView> filterWebMethods(List<MethodView> input) {
        ArrayList<MethodView> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        for (MethodView m : input) {
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

    public ArrayList<MethodView> getMethodsByAnnoNames(List<String> annoNames) {
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
        ArrayList<MethodView> out = new ArrayList<>();
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
                out.add(toMethodView(method));
            }
        }
        out.sort(METHOD_RESULT_COMPARATOR);
        return out;
    }

    public ArrayList<AnnoMethodView> getMethodsByAnno(List<String> annoNames,
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

        ArrayList<AnnoMethodView> out = new ArrayList<>();
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
                appendAnnoMatches(out::add, annoNames, matchMode, "class", classAnnos, method, methodJarId);
            }
            if (!"class".equals(scopeMode)) {
                appendAnnoMatches(out::add, annoNames, matchMode, "method", methodAnnos, method, methodJarId);
            }
        }
        out.sort(Comparator.comparing(AnnoMethodView::getClassName)
                .thenComparing(AnnoMethodView::getMethodName)
                .thenComparing(AnnoMethodView::getMethodDesc)
                .thenComparing(AnnoMethodView::getAnnoName));
        int from = Math.min(begin, out.size());
        int to = Math.min(from + max, out.size());
        return new ArrayList<>(out.subList(from, to));
    }

    public PageSlice<AnnoMethodView> getMethodsByAnnoPage(List<String> annoNames,
                                                            String match,
                                                            String scope,
                                                            Integer jarId,
                                                            int offset,
                                                            int limit,
                                                            boolean includeJdk) {
        if (annoNames == null || annoNames.isEmpty()) {
            return PageSlice.empty();
        }
        String matchMode = safe(match).trim().toLowerCase(Locale.ROOT);
        if (!"equal".equals(matchMode)) {
            matchMode = "contains";
        }
        String scopeMode = safe(scope).trim().toLowerCase(Locale.ROOT);
        if (!"class".equals(scopeMode) && !"method".equals(scopeMode)) {
            scopeMode = "any";
        }

        OrderedPageCollector<AnnoMethodView> collector =
                new OrderedPageCollector<>(StableOrder.ANNO_METHOD_RESULT, offset, limit);
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
                appendAnnoMatches(row -> {
                    if (isVisibleAnnoMethodView(row, includeJdk)) {
                        collector.accept(row);
                    }
                }, annoNames, matchMode, "class", classAnnos, method, methodJarId);
            }
            if (!"class".equals(scopeMode)) {
                appendAnnoMatches(row -> {
                    if (isVisibleAnnoMethodView(row, includeJdk)) {
                        collector.accept(row);
                    }
                }, annoNames, matchMode, "method", methodAnnos, method, methodJarId);
            }
        }
        return collector.finish();
    }

    private void appendAnnoMatches(java.util.function.Consumer<AnnoMethodView> sink,
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
                AnnoMethodView row = new AnnoMethodView();
                row.setClassName(normalizeClassName(method.getClassReference().getName()));
                row.setMethodName(safe(method.getName()));
                row.setMethodDesc(safe(method.getDesc()));
                row.setLineNumber(method.getLineNumber());
                row.setJarId(methodJarId);
                row.setJarName(resolveJarName(methodJarId, method.getJarName()));
                row.setAnnoName(c);
                row.setAnnoScope(scope);
                if (sink != null) {
                    sink.accept(row);
                }
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

    public ArrayList<MethodView> getMethodsByClassNoJar(String className) {
        ArrayList<MethodView> rows = getMethodsByClass(className);
        LinkedHashMap<String, MethodView> unique = new LinkedHashMap<>();
        for (MethodView row : rows) {
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
        return DatabaseManager.getClassReferenceByName(ch.getName(), jarId);
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

    public Map<String, List<String>> getStringMap() {
        Map<String, LinkedHashSet<String>> merged = new LinkedHashMap<>();
        appendClassStrings(merged, DatabaseManager.getMethodStringsSnapshot());
        appendClassStrings(merged, DatabaseManager.getMethodAnnoStringsSnapshot());
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : merged.entrySet()) {
            if (entry == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), new ArrayList<>(entry.getValue()));
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
        String p = safe(path);
        int count = 0;
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
            if (CommonFilterUtil.isFilteredResourcePath(row.getResourcePath())) {
                continue;
            }
            count++;
        }
        return count;
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

    public void cleanFavItem(MethodView m) {
        DatabaseManager.cleanFavItem(m);
    }

    public void addFav(MethodView m) {
        DatabaseManager.addFav(m);
    }

    public void insertHistory(MethodView m) {
        DatabaseManager.insertHistory(m);
    }

    public void cleanHistory() {
        DatabaseManager.cleanHistory();
    }

    public ArrayList<MethodView> getAllFavMethods() {
        return DatabaseManager.getAllFavMethods();
    }

    public ArrayList<MethodView> getAllHisMethods() {
        return DatabaseManager.getAllHisMethods();
    }

    private GraphSnapshot loadSnapshotOrThrow() {
        return graphStore.loadSnapshot();
    }

    private GraphSnapshot loadQuerySnapshotOrThrow() {
        return graphStore.loadQuerySnapshot();
    }

    private Map<Long, MethodView> methodNodeMap(GraphSnapshot snapshot) {
        Map<Long, MethodView> methods = new HashMap<>();
        if (snapshot == null) {
            return methods;
        }
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            if (node == null) {
                continue;
            }
            methods.put(node.getNodeId(), toMethodView(node));
        }
        return methods;
    }

    private ArrayList<CallEdgeView> filterCallEdges(boolean byCaller,
                                                        String className,
                                                        String methodName,
                                                        String methodDesc,
                                                        Integer jarId,
                                                        Integer offset,
                                                        Integer limit) {
        CallGraphCache cache = getCallGraphCache();
        ArrayList<CallEdgeView> out = byCaller
                ? cache.getCallEdgesByCaller(normalizeClassName(className), safe(methodName).trim(), safe(methodDesc).trim(), jarId)
                : cache.getCallEdgesByCallee(normalizeClassName(className), safe(methodName).trim(), safe(methodDesc).trim(), jarId);
        int begin = offset == null || offset < 0 ? 0 : offset;
        int size = limit == null || limit <= 0 ? out.size() : limit;
        int from = Math.min(begin, out.size());
        int to = Math.min(from + size, out.size());
        return new ArrayList<>(out.subList(from, to));
    }

    private MethodView toMethodView(GraphNode node) {
        MethodReference ref = findMethodReference(node.getClassName(), node.getMethodName(), node.getMethodDesc(), node.getJarId());
        if (ref != null) {
            return toMethodView(ref);
        }
        MethodView out = new MethodView();
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

    private MethodView toMethodView(MethodReference ref) {
        MethodView out = new MethodView();
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
        }
        return null;
    }

    private ClassView toClassView(ClassReference row) {
        if (row == null || row.getName() == null || row.getName().isBlank()) {
            return null;
        }
        ClassView out = new ClassView();
        out.setClassName(normalizeClassName(row.getName()));
        out.setSuperClassName(normalizeClassName(row.getSuperClass()));
        out.setIsInterfaceInt(row.isInterface() ? 1 : 0);
        int jarId = normalizeJarId(row.getJarId());
        out.setJarId(jarId);
        out.setJarName(resolveJarName(jarId, row.getJarName()));
        return out;
    }

    private ArrayList<ClassView> classesFromNames(Set<String> classNames) {
        ArrayList<ClassView> out = new ArrayList<>();
        if (classNames == null || classNames.isEmpty()) {
            return out;
        }
        for (String className : classNames) {
            ArrayList<ClassView> rows = getClassesByClass(className);
            if (!rows.isEmpty()) {
                out.addAll(rows);
            }
        }
        out.sort(Comparator.comparing(ClassView::getClassName)
                .thenComparingInt(ClassView::getJarId));
        return out;
    }

    private MethodView toSpringMethod(SpringMapping mapping) {
        if (mapping == null) {
            return null;
        }
        MethodReference method = mapping.getMethodReference();
        if (method == null) {
            return null;
        }
        MethodView out = toMethodView(method);
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

    private ArrayList<MethodView> dedupMethods(List<MethodView> input) {
        LinkedHashMap<String, MethodView> map = new LinkedHashMap<>();
        if (input != null) {
            for (MethodView row : input) {
                if (row == null) {
                    continue;
                }
                String key = methodKey(row.getClassName(), row.getMethodName(), row.getMethodDesc(), row.getJarId());
                map.putIfAbsent(key, row);
            }
        }
        ArrayList<MethodView> out = new ArrayList<>(map.values());
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

    private void appendClassStrings(Map<String, LinkedHashSet<String>> target,
                                    Map<String, List<String>> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String className = parseClassFromMethodKey(entry.getKey());
            if (className.isEmpty()) {
                continue;
            }
            LinkedHashSet<String> values = target.computeIfAbsent(className, ignore -> new LinkedHashSet<>());
            for (String value : entry.getValue()) {
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        }
    }

    private String parseClassFromMethodKey(String key) {
        String value = safe(key);
        int idx = value.indexOf('#');
        if (idx <= 0) {
            return "";
        }
        return value.substring(0, idx);
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

    private static boolean isVisibleMethodView(MethodView result, boolean includeJdk) {
        if (result == null) {
            return false;
        }
        if (CommonFilterUtil.isModuleInfoClassName(result.getClassName())) {
            return false;
        }
        if (includeJdk) {
            return true;
        }
        return !CommonFilterUtil.isFilteredClass(result.getClassName())
                && !CommonFilterUtil.isFilteredJar(result.getJarName());
    }

    private static boolean isVisibleAnnoMethodView(AnnoMethodView result, boolean includeJdk) {
        if (result == null) {
            return false;
        }
        if (CommonFilterUtil.isModuleInfoClassName(result.getClassName())) {
            return false;
        }
        if (includeJdk) {
            return true;
        }
        return !CommonFilterUtil.isFilteredClass(result.getClassName())
                && !CommonFilterUtil.isFilteredJar(result.getJarName());
    }

    private static boolean isVisibleCallEdge(CallEdgeView result, boolean includeJdk, boolean byCallee) {
        if (result == null) {
            return false;
        }
        String className = byCallee ? result.getCallerClassName() : result.getCalleeClassName();
        String jarName = byCallee ? result.getCallerJarName() : result.getCalleeJarName();
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return false;
        }
        if (includeJdk) {
            return true;
        }
        return !CommonFilterUtil.isFilteredClass(className)
                && !CommonFilterUtil.isFilteredJar(jarName);
    }

    public record PageSlice<T>(List<T> items, int total, boolean truncated) {
        public PageSlice {
            items = items == null ? List.of() : List.copyOf(items);
            total = Math.max(0, total);
        }

        public static <T> PageSlice<T> empty() {
            return new PageSlice<>(List.of(), 0, false);
        }
    }

    private static final class OrderedPageCollector<T> {
        private final Comparator<? super T> comparator;
        private final int offset;
        private final int limit;
        private final int windowSize;
        private final PriorityQueue<T> heap;
        private int total;

        private OrderedPageCollector(Comparator<? super T> comparator, int offset, int limit) {
            this.comparator = Objects.requireNonNull(comparator, "comparator");
            this.offset = Math.max(0, offset);
            this.limit = limit <= 0 ? 100 : limit;
            long desired = (long) this.offset + this.limit + 1L;
            this.windowSize = desired > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) desired;
            this.heap = new PriorityQueue<>(Math.max(1, Math.min(this.windowSize, 256)), comparator.reversed());
            this.total = 0;
        }

        private void accept(T item) {
            if (item == null) {
                return;
            }
            total++;
            if (windowSize <= 0) {
                return;
            }
            if (heap.size() < windowSize) {
                heap.offer(item);
                return;
            }
            T largest = heap.peek();
            if (largest != null && comparator.compare(item, largest) < 0) {
                heap.poll();
                heap.offer(item);
            }
        }

        private PageSlice<T> finish() {
            if (total == 0) {
                return PageSlice.empty();
            }
            List<T> ordered = new ArrayList<>(heap);
            ordered.sort(comparator);
            int start = Math.min(offset, ordered.size());
            int end = Math.min(ordered.size(), start + limit);
            List<T> page = start >= end ? List.of() : new ArrayList<>(ordered.subList(start, end));
            boolean truncated = total > offset + page.size();
            return new PageSlice<>(page, total, truncated);
        }
    }

    private static final Comparator<MethodView> METHOD_RESULT_COMPARATOR = Comparator
            .comparing(MethodView::getClassName, Comparator.nullsLast(String::compareTo))
            .thenComparing(MethodView::getMethodName, Comparator.nullsLast(String::compareTo))
            .thenComparing(MethodView::getMethodDesc, Comparator.nullsLast(String::compareTo))
            .thenComparingInt(MethodView::getJarId);
}
