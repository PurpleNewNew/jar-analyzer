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
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.DFSResultEntity;
import me.n1ar4.jar.analyzer.entity.DFSResultListEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger();
    public static int PART_SIZE = resolveBatchSize();

    private static final AtomicLong BUILD_SEQ = new AtomicLong(0L);
    private static final AtomicBoolean BUILDING = new AtomicBoolean(false);
    private static final AtomicInteger NEXT_JAR_ID = new AtomicInteger(1);
    private static final AtomicInteger NEXT_VUL_ID = new AtomicInteger(1);
    private static final AtomicInteger NEXT_RESOURCE_ID = new AtomicInteger(1);

    private static final Map<String, JarEntity> JAR_BY_PATH = new ConcurrentHashMap<>();
    private static final Map<String, String> SEMANTIC_CACHE = new ConcurrentHashMap<>();
    private static final List<MethodResult> FAVORITES = Collections.synchronizedList(new ArrayList<>());
    private static final List<MethodResult> HISTORIES = Collections.synchronizedList(new ArrayList<>());
    private static final List<VulReportEntity> VUL_REPORTS = Collections.synchronizedList(new ArrayList<>());
    private static final List<ClassFileEntity> CLASS_FILES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<ClassFileEntity>> CLASS_FILES_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, ClassFileEntity> PRIMARY_CLASS_FILE_BY_NAME = new ConcurrentHashMap<>();

    private static final List<ClassReference> CLASS_REFERENCES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<ClassReference>> CLASS_REFS_BY_NAME = new ConcurrentHashMap<>();

    private static final List<MethodReference> METHOD_REFERENCES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<MethodReference>> METHODS_BY_CLASS = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> METHOD_STRINGS = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> METHOD_STRING_ANNOS = new ConcurrentHashMap<>();

    private static final List<ResourceEntity> RESOURCE_ENTRIES = Collections.synchronizedList(new ArrayList<>());
    private static final List<CallSiteEntity> CALL_SITE_ENTRIES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<CallSiteEntity>> CALL_SITES_BY_CALLER = new ConcurrentHashMap<>();
    private static final Map<String, List<CallSiteEntity>> CALL_SITES_BY_EDGE = new ConcurrentHashMap<>();
    private static final List<LocalVarEntity> LOCAL_VAR_ENTRIES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<LocalVarEntity>> LOCAL_VARS_BY_METHOD = new ConcurrentHashMap<>();

    private static final List<SpringController> SPRING_CONTROLLERS = Collections.synchronizedList(new ArrayList<>());
    private static final Set<String> SPRING_INTERCEPTORS = ConcurrentHashMap.newKeySet();
    private static final Set<String> SERVLETS = ConcurrentHashMap.newKeySet();
    private static final Set<String> FILTERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LISTENERS = ConcurrentHashMap.newKeySet();

    private static volatile ProjectModel lastProjectModel;

    static {
        logger.info("DatabaseManager running in neo4j-only mode");
    }

    private DatabaseManager() {
    }

    private static int resolveBatchSize() {
        int defaultSize = 500;
        String raw = System.getProperty("jar-analyzer.db.batch");
        if (raw == null || raw.trim().isEmpty()) {
            return defaultSize;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 50) {
                return 50;
            }
            if (value > 5000) {
                return 5000;
            }
            return value;
        } catch (NumberFormatException ex) {
            logger.debug("invalid db batch size: {}", raw);
            return defaultSize;
        }
    }

    public static void prepareBuild() {
        BUILD_SEQ.incrementAndGet();
        clearSemanticCache();
    }

    public static void finalizeBuild() {
        // no-op in neo4j-only mode
    }

    public static void clearAllData() {
        JAR_BY_PATH.clear();
        SEMANTIC_CACHE.clear();
        FAVORITES.clear();
        HISTORIES.clear();
        VUL_REPORTS.clear();
        CLASS_FILES.clear();
        CLASS_FILES_BY_NAME.clear();
        PRIMARY_CLASS_FILE_BY_NAME.clear();
        CLASS_REFERENCES.clear();
        CLASS_REFS_BY_NAME.clear();
        METHOD_REFERENCES.clear();
        METHODS_BY_CLASS.clear();
        METHOD_STRINGS.clear();
        METHOD_STRING_ANNOS.clear();
        RESOURCE_ENTRIES.clear();
        CALL_SITE_ENTRIES.clear();
        CALL_SITES_BY_CALLER.clear();
        CALL_SITES_BY_EDGE.clear();
        LOCAL_VAR_ENTRIES.clear();
        LOCAL_VARS_BY_METHOD.clear();
        SPRING_CONTROLLERS.clear();
        SPRING_INTERCEPTORS.clear();
        SERVLETS.clear();
        FILTERS.clear();
        LISTENERS.clear();
        lastProjectModel = null;
        try {
            GraphStore.invalidateCache();
        } catch (Exception ex) {
            logger.debug("invalidate graph snapshot cache fail: {}", ex.toString());
        }
    }

    public static void saveProjectModel(ProjectModel model) {
        lastProjectModel = model;
    }

    public static ProjectModel getProjectModel() {
        return lastProjectModel;
    }

    public static void saveDFS(DFSResultEntity dfsResultEntity) {
        // no-op in neo4j-only mode
    }

    public static void saveDFSList(DFSResultListEntity dfsResultListEntity) {
        // no-op in neo4j-only mode
    }

    public static void saveJar(String jarPath) {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            return;
        }
        JAR_BY_PATH.computeIfAbsent(jarPath, DatabaseManager::newJarEntity);
    }

    public static JarEntity getJarId(String jarPath) {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            return null;
        }
        return JAR_BY_PATH.computeIfAbsent(jarPath, DatabaseManager::newJarEntity);
    }

    public static void saveClassFiles(Set<ClassFileEntity> classFileList) {
        CLASS_FILES.clear();
        CLASS_FILES_BY_NAME.clear();
        PRIMARY_CLASS_FILE_BY_NAME.clear();
        if (classFileList == null || classFileList.isEmpty()) {
            return;
        }
        List<ClassFileEntity> rows = new ArrayList<>();
        for (ClassFileEntity row : classFileList) {
            if (row == null) {
                continue;
            }
            ClassFileEntity copy = copyClassFileEntity(row);
            if (copy == null) {
                continue;
            }
            rows.add(copy);
        }
        rows.sort(CLASS_FILE_COMPARATOR);
        CLASS_FILES.addAll(rows);
        for (ClassFileEntity row : rows) {
            String className = normalizeClassName(row.getClassName());
            if (className == null) {
                continue;
            }
            CLASS_FILES_BY_NAME.computeIfAbsent(className, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(row);
            PRIMARY_CLASS_FILE_BY_NAME.merge(className, row, DatabaseManager::pickPreferredClassFile);
        }
    }

    public static void saveClassInfo(Set<ClassReference> discoveredClasses) {
        CLASS_REFERENCES.clear();
        CLASS_REFS_BY_NAME.clear();
        if (discoveredClasses == null || discoveredClasses.isEmpty()) {
            return;
        }
        List<ClassReference> rows = new ArrayList<>();
        for (ClassReference row : discoveredClasses) {
            if (row == null || row.getName() == null || row.getName().isBlank()) {
                continue;
            }
            rows.add(row);
        }
        rows.sort(CLASS_REF_COMPARATOR);
        CLASS_REFERENCES.addAll(rows);
        for (ClassReference row : rows) {
            String className = normalizeClassName(row.getName());
            if (className == null) {
                continue;
            }
            CLASS_REFS_BY_NAME.computeIfAbsent(className, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(row);
        }
    }

    public static void saveMethods(Set<MethodReference> discoveredMethods) {
        METHOD_REFERENCES.clear();
        METHODS_BY_CLASS.clear();
        if (discoveredMethods == null || discoveredMethods.isEmpty()) {
            return;
        }
        List<MethodReference> rows = new ArrayList<>();
        for (MethodReference row : discoveredMethods) {
            if (row == null || row.getClassReference() == null) {
                continue;
            }
            String className = normalizeClassName(row.getClassReference().getName());
            String methodName = safe(row.getName());
            String methodDesc = safe(row.getDesc());
            if (className == null || methodName.isEmpty() || methodDesc.isEmpty()) {
                continue;
            }
            rows.add(row);
        }
        rows.sort(METHOD_REF_COMPARATOR);
        METHOD_REFERENCES.addAll(rows);
        for (MethodReference row : rows) {
            String className = normalizeClassName(row.getClassReference().getName());
            if (className == null) {
                continue;
            }
            METHODS_BY_CLASS.computeIfAbsent(className, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(row);
        }
    }

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
                                       Map<ClassReference.Handle, ClassReference> classMap,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                       List<CallSiteEntity> callSites) {
        // no-op in neo4j-only mode
    }

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
                                       Map<ClassReference.Handle, ClassReference> classMap,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        saveMethodCalls(methodCalls, classMap, methodCallMeta, Collections.emptyList());
    }

    public static void saveImpls(Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap,
                                 Map<ClassReference.Handle, ClassReference> classMap) {
        // no-op in neo4j-only mode
    }

    public static void saveStrMap(Map<MethodReference.Handle, List<String>> strMap,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap,
                                  Map<MethodReference.Handle, MethodReference> methodMap,
                                  Map<ClassReference.Handle, ClassReference> classMap) {
        METHOD_STRINGS.clear();
        METHOD_STRING_ANNOS.clear();
        saveMethodStringMap(METHOD_STRINGS, strMap);
        saveMethodStringMap(METHOD_STRING_ANNOS, stringAnnoMap);
    }

    public static void saveResources(List<ResourceEntity> resources) {
        RESOURCE_ENTRIES.clear();
        NEXT_RESOURCE_ID.set(1);
        if (resources == null || resources.isEmpty()) {
            return;
        }
        for (ResourceEntity row : resources) {
            if (row == null) {
                continue;
            }
            ResourceEntity copy = copyResourceEntity(row);
            if (copy.getRid() <= 0) {
                copy.setRid(NEXT_RESOURCE_ID.getAndIncrement());
            } else {
                NEXT_RESOURCE_ID.set(Math.max(NEXT_RESOURCE_ID.get(), copy.getRid() + 1));
            }
            RESOURCE_ENTRIES.add(copy);
        }
    }

    public static void saveCallSites(List<CallSiteEntity> callSites) {
        CALL_SITE_ENTRIES.clear();
        CALL_SITES_BY_CALLER.clear();
        CALL_SITES_BY_EDGE.clear();
        if (callSites == null || callSites.isEmpty()) {
            return;
        }
        for (CallSiteEntity row : callSites) {
            if (row == null) {
                continue;
            }
            CallSiteEntity copy = copyCallSiteEntity(row);
            CALL_SITE_ENTRIES.add(copy);
            String callerKey = methodKey(copy.getCallerClassName(), copy.getCallerMethodName(), copy.getCallerMethodDesc(), copy.getJarId());
            CALL_SITES_BY_CALLER.computeIfAbsent(callerKey, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(copy);
            String edgeKey = edgeKey(
                    copy.getCallerClassName(),
                    copy.getCallerMethodName(),
                    copy.getCallerMethodDesc(),
                    copy.getCalleeOwner(),
                    copy.getCalleeMethodName(),
                    copy.getCalleeMethodDesc());
            CALL_SITES_BY_EDGE.computeIfAbsent(edgeKey, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(copy);
        }
    }

    public static void saveLocalVars(List<LocalVarEntity> localVars) {
        LOCAL_VAR_ENTRIES.clear();
        LOCAL_VARS_BY_METHOD.clear();
        if (localVars == null || localVars.isEmpty()) {
            return;
        }
        for (LocalVarEntity row : localVars) {
            if (row == null) {
                continue;
            }
            LocalVarEntity copy = copyLocalVarEntity(row);
            LOCAL_VAR_ENTRIES.add(copy);
            String key = methodKey(copy.getClassName(), copy.getMethodName(), copy.getMethodDesc(), copy.getJarId());
            LOCAL_VARS_BY_METHOD.computeIfAbsent(key, ignore -> Collections.synchronizedList(new ArrayList<>()))
                    .add(copy);
        }
    }

    public static void saveSpringController(ArrayList<SpringController> controllers) {
        SPRING_CONTROLLERS.clear();
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        SPRING_CONTROLLERS.addAll(controllers);
    }

    public static void saveSpringInterceptor(ArrayList<String> interceptors,
                                             Map<ClassReference.Handle, ClassReference> classMap) {
        SPRING_INTERCEPTORS.clear();
        saveClassNameSet(SPRING_INTERCEPTORS, interceptors);
    }

    public static void saveServlets(ArrayList<String> servlets,
                                    Map<ClassReference.Handle, ClassReference> classMap) {
        SERVLETS.clear();
        saveClassNameSet(SERVLETS, servlets);
    }

    public static void saveFilters(ArrayList<String> filters,
                                   Map<ClassReference.Handle, ClassReference> classMap) {
        FILTERS.clear();
        saveClassNameSet(FILTERS, filters);
    }

    public static void saveListeners(ArrayList<String> listeners,
                                     Map<ClassReference.Handle, ClassReference> classMap) {
        LISTENERS.clear();
        saveClassNameSet(LISTENERS, listeners);
    }

    public static void cleanFav() {
        FAVORITES.clear();
    }

    public static void cleanFavItem(MethodResult m) {
        if (m == null) {
            return;
        }
        FAVORITES.removeIf(item -> sameMethod(item, m));
    }

    public static void addFav(MethodResult m) {
        if (m == null) {
            return;
        }
        cleanFavItem(m);
        FAVORITES.add(m);
    }

    public static void insertHistory(MethodResult m) {
        if (m == null) {
            return;
        }
        HISTORIES.add(m);
    }

    public static void cleanHistory() {
        HISTORIES.clear();
    }

    public static ArrayList<MethodResult> getAllFavMethods() {
        return new ArrayList<>(FAVORITES);
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        return new ArrayList<>(HISTORIES);
    }

    public static long getBuildSeq() {
        return BUILD_SEQ.get();
    }

    public static void setBuilding(boolean building) {
        BUILDING.set(building);
    }

    public static boolean isBuilding() {
        return BUILDING.get();
    }

    public static String getSemanticCacheValue(String cacheKey, String cacheType) {
        if (cacheKey == null || cacheType == null) {
            return null;
        }
        return SEMANTIC_CACHE.get(semanticKey(cacheType, cacheKey));
    }

    public static void putSemanticCacheValue(String cacheKey, String cacheType, String cacheValue) {
        if (cacheKey == null || cacheType == null || cacheValue == null) {
            return;
        }
        SEMANTIC_CACHE.put(semanticKey(cacheType, cacheKey), cacheValue);
    }

    public static void clearSemanticCacheType(String cacheType) {
        if (cacheType == null) {
            return;
        }
        String prefix = cacheType + "#";
        SEMANTIC_CACHE.keySet().removeIf(key -> key != null && key.startsWith(prefix));
    }

    public static void clearSemanticCache() {
        SEMANTIC_CACHE.clear();
    }

    public static void saveVulReport(VulReportEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() == null) {
            entity.setId(NEXT_VUL_ID.getAndIncrement());
        }
        VUL_REPORTS.add(entity);
    }

    public static List<VulReportEntity> getVulReports() {
        return new ArrayList<>(VUL_REPORTS);
    }

    public static List<JarEntity> getJarsMeta() {
        ArrayList<JarEntity> out = new ArrayList<>(JAR_BY_PATH.values());
        out.sort(Comparator.comparingInt(JarEntity::getJid));
        return out;
    }

    public static JarEntity getJarById(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return null;
        }
        for (JarEntity item : JAR_BY_PATH.values()) {
            if (item == null) {
                continue;
            }
            if (item.getJid() == jarId) {
                return item;
            }
        }
        return null;
    }

    public static List<ClassFileEntity> getClassFiles() {
        return new ArrayList<>(CLASS_FILES);
    }

    public static List<ClassFileEntity> getClassFilesByClass(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<ClassFileEntity> rows = CLASS_FILES_BY_NAME.get(normalized);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static ClassFileEntity getClassFileByClass(String className, Integer jarId) {
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return null;
        }
        if (jarId != null && jarId >= 0) {
            List<ClassFileEntity> rows = CLASS_FILES_BY_NAME.get(normalized);
            if (rows != null) {
                for (ClassFileEntity row : rows) {
                    if (row == null) {
                        continue;
                    }
                    Integer value = row.getJarId();
                    if (value != null && value == jarId) {
                        return row;
                    }
                }
            }
        }
        return PRIMARY_CLASS_FILE_BY_NAME.get(normalized);
    }

    public static List<ClassReference> getClassReferences() {
        return new ArrayList<>(CLASS_REFERENCES);
    }

    public static List<ClassReference> getClassReferencesByName(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<ClassReference> rows = CLASS_REFS_BY_NAME.get(normalized);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static ClassReference getClassReferenceByName(String className, Integer jarId) {
        List<ClassReference> rows = getClassReferencesByName(className);
        if (rows.isEmpty()) {
            return null;
        }
        if (jarId != null && jarId >= 0) {
            for (ClassReference row : rows) {
                if (row == null) {
                    continue;
                }
                Integer value = row.getJarId();
                if (value != null && value == jarId) {
                    return row;
                }
            }
        }
        return rows.get(0);
    }

    public static List<MethodReference> getMethodReferences() {
        return new ArrayList<>(METHOD_REFERENCES);
    }

    public static List<MethodReference> getMethodReferencesByClass(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<MethodReference> rows = METHODS_BY_CLASS.get(normalized);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static List<String> getMethodStringValues(String className,
                                                     String methodName,
                                                     String methodDesc,
                                                     Integer jarId) {
        String key = methodKey(className, methodName, methodDesc, jarId);
        List<String> rows = METHOD_STRINGS.get(key);
        if ((rows == null || rows.isEmpty()) && jarId != null && jarId >= 0) {
            rows = METHOD_STRINGS.get(methodKey(className, methodName, methodDesc, -1));
        }
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static Map<String, List<String>> getMethodStringsSnapshot() {
        return new HashMap<>(METHOD_STRINGS);
    }

    public static Map<String, List<String>> getMethodAnnoStringsSnapshot() {
        return new HashMap<>(METHOD_STRING_ANNOS);
    }

    public static List<String> getMethodAnnoStringValues(String className,
                                                         String methodName,
                                                         String methodDesc,
                                                         Integer jarId) {
        String key = methodKey(className, methodName, methodDesc, jarId);
        List<String> rows = METHOD_STRING_ANNOS.get(key);
        if ((rows == null || rows.isEmpty()) && jarId != null && jarId >= 0) {
            rows = METHOD_STRING_ANNOS.get(methodKey(className, methodName, methodDesc, -1));
        }
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static List<ResourceEntity> getResources() {
        return new ArrayList<>(RESOURCE_ENTRIES);
    }

    public static List<CallSiteEntity> getCallSites() {
        return new ArrayList<>(CALL_SITE_ENTRIES);
    }

    public static List<CallSiteEntity> getCallSitesByCaller(String className,
                                                            String methodName,
                                                            String methodDesc) {
        String key = methodKey(className, methodName, methodDesc, -1);
        List<CallSiteEntity> rows = CALL_SITES_BY_CALLER.get(key);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static List<CallSiteEntity> getCallSitesByEdge(String callerClassName,
                                                          String callerMethodName,
                                                          String callerMethodDesc,
                                                          String calleeClassName,
                                                          String calleeMethodName,
                                                          String calleeMethodDesc) {
        String key = edgeKey(
                callerClassName,
                callerMethodName,
                callerMethodDesc,
                calleeClassName,
                calleeMethodName,
                calleeMethodDesc
        );
        List<CallSiteEntity> rows = CALL_SITES_BY_EDGE.get(key);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static List<LocalVarEntity> getLocalVarsByMethod(String className,
                                                             String methodName,
                                                             String methodDesc) {
        String key = methodKey(className, methodName, methodDesc, -1);
        List<LocalVarEntity> rows = LOCAL_VARS_BY_METHOD.get(key);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    public static List<SpringController> getSpringControllers() {
        return new ArrayList<>(SPRING_CONTROLLERS);
    }

    public static Set<String> getSpringInterceptors() {
        return new HashSet<>(SPRING_INTERCEPTORS);
    }

    public static Set<String> getServlets() {
        return new HashSet<>(SERVLETS);
    }

    public static Set<String> getFilters() {
        return new HashSet<>(FILTERS);
    }

    public static Set<String> getListeners() {
        return new HashSet<>(LISTENERS);
    }

    private static JarEntity newJarEntity(String jarPath) {
        JarEntity entity = new JarEntity();
        entity.setJid(NEXT_JAR_ID.getAndIncrement());
        entity.setJarAbsPath(jarPath);
        entity.setJarName(resolveJarName(jarPath));
        return entity;
    }

    private static String resolveJarName(String jarPath) {
        if (jarPath == null) {
            return "";
        }
        String[] temp;
        if (OSUtil.isWindows()) {
            temp = jarPath.split("\\\\");
        } else {
            temp = jarPath.split("/");
        }
        if (temp.length == 0) {
            return jarPath;
        }
        return temp[temp.length - 1];
    }

    private static String semanticKey(String cacheType, String cacheKey) {
        return cacheType + "#" + cacheKey;
    }

    private static void saveMethodStringMap(Map<String, List<String>> target,
                                            Map<MethodReference.Handle, List<String>> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, List<String>> entry : source.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            List<String> values = entry.getValue();
            if (handle == null || handle.getClassReference() == null || values == null || values.isEmpty()) {
                continue;
            }
            String key = methodKey(
                    handle.getClassReference().getName(),
                    handle.getName(),
                    handle.getDesc(),
                    handle.getClassReference().getJarId()
            );
            List<String> out = new ArrayList<>();
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                out.add(value);
            }
            if (out.isEmpty()) {
                continue;
            }
            target.put(key, Collections.unmodifiableList(out));
            target.putIfAbsent(methodKey(
                    handle.getClassReference().getName(),
                    handle.getName(),
                    handle.getDesc(),
                    -1
            ), Collections.unmodifiableList(out));
        }
    }

    private static void saveClassNameSet(Set<String> out, List<String> values) {
        if (out == null) {
            return;
        }
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            String normalized = normalizeClassName(value);
            if (normalized == null) {
                continue;
            }
            out.add(normalized);
        }
    }

    private static ClassFileEntity copyClassFileEntity(ClassFileEntity src) {
        if (src == null) {
            return null;
        }
        ClassFileEntity out = new ClassFileEntity();
        out.setCfId(src.getCfId());
        out.setClassName(src.getClassName());
        out.setPath(src.getPath());
        out.setPathStr(src.getPathStr());
        out.setJarName(src.getJarName());
        out.setJarId(src.getJarId());
        return out;
    }

    private static ResourceEntity copyResourceEntity(ResourceEntity src) {
        ResourceEntity out = new ResourceEntity();
        out.setRid(src.getRid());
        out.setResourcePath(src.getResourcePath());
        out.setPathStr(src.getPathStr());
        out.setJarName(src.getJarName());
        out.setJarId(src.getJarId());
        out.setFileSize(src.getFileSize());
        out.setIsText(src.getIsText());
        return out;
    }

    private static CallSiteEntity copyCallSiteEntity(CallSiteEntity src) {
        CallSiteEntity out = new CallSiteEntity();
        out.setCallerClassName(src.getCallerClassName());
        out.setCallerMethodName(src.getCallerMethodName());
        out.setCallerMethodDesc(src.getCallerMethodDesc());
        out.setCalleeOwner(src.getCalleeOwner());
        out.setCalleeMethodName(src.getCalleeMethodName());
        out.setCalleeMethodDesc(src.getCalleeMethodDesc());
        out.setOpCode(src.getOpCode());
        out.setLineNumber(src.getLineNumber());
        out.setCallIndex(src.getCallIndex());
        out.setReceiverType(src.getReceiverType());
        out.setJarId(src.getJarId());
        out.setCallSiteKey(src.getCallSiteKey());
        return out;
    }

    private static LocalVarEntity copyLocalVarEntity(LocalVarEntity src) {
        LocalVarEntity out = new LocalVarEntity();
        out.setClassName(src.getClassName());
        out.setMethodName(src.getMethodName());
        out.setMethodDesc(src.getMethodDesc());
        out.setVarIndex(src.getVarIndex());
        out.setVarName(src.getVarName());
        out.setVarDesc(src.getVarDesc());
        out.setVarSignature(src.getVarSignature());
        out.setStartLine(src.getStartLine());
        out.setEndLine(src.getEndLine());
        out.setJarId(src.getJarId());
        return out;
    }

    private static ClassFileEntity pickPreferredClassFile(ClassFileEntity a, ClassFileEntity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        int aj = normalizeJarId(a.getJarId());
        int bj = normalizeJarId(b.getJarId());
        if (bj < aj) {
            return b;
        }
        if (bj > aj) {
            return a;
        }
        String ap = safe(a.getPathStr());
        String bp = safe(b.getPathStr());
        return bp.compareTo(ap) < 0 ? b : a;
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return Integer.MAX_VALUE;
        }
        return jarId;
    }

    private static String normalizeClassName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace('\\', '/');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.indexOf('/') < 0 && normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static String methodKey(String className, String methodName, String methodDesc, Integer jarId) {
        String clazz = normalizeClassName(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        int j = jarId == null ? -1 : jarId;
        if (clazz == null || name.isEmpty() || desc.isEmpty()) {
            return "";
        }
        return clazz + "#" + name + "#" + desc + "#" + j;
    }

    private static String edgeKey(String callerClassName,
                                  String callerMethodName,
                                  String callerMethodDesc,
                                  String calleeClassName,
                                  String calleeMethodName,
                                  String calleeMethodDesc) {
        String a = normalizeClassName(callerClassName);
        String b = safe(callerMethodName);
        String c = safe(callerMethodDesc);
        String d = normalizeClassName(calleeClassName);
        String e = safe(calleeMethodName);
        String f = safe(calleeMethodDesc);
        if (a == null || d == null || b.isEmpty() || c.isEmpty() || e.isEmpty() || f.isEmpty()) {
            return "";
        }
        return a + "#" + b + "#" + c + "->" + d + "#" + e + "#" + f;
    }

    private static final Comparator<ClassFileEntity> CLASS_FILE_COMPARATOR = (a, b) -> {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int c = safe(a.getClassName()).compareTo(safe(b.getClassName()));
        if (c != 0) {
            return c;
        }
        c = Integer.compare(normalizeJarId(a.getJarId()), normalizeJarId(b.getJarId()));
        if (c != 0) {
            return c;
        }
        return safe(a.getPathStr()).compareTo(safe(b.getPathStr()));
    };

    private static final Comparator<ClassReference> CLASS_REF_COMPARATOR = (a, b) -> {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int c = safe(a.getName()).compareTo(safe(b.getName()));
        if (c != 0) {
            return c;
        }
        return Integer.compare(normalizeJarId(a.getJarId()), normalizeJarId(b.getJarId()));
    };

    private static final Comparator<MethodReference> METHOD_REF_COMPARATOR = (a, b) -> {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        String ac = a.getClassReference() == null ? "" : safe(a.getClassReference().getName());
        String bc = b.getClassReference() == null ? "" : safe(b.getClassReference().getName());
        int c = ac.compareTo(bc);
        if (c != 0) {
            return c;
        }
        c = safe(a.getName()).compareTo(safe(b.getName()));
        if (c != 0) {
            return c;
        }
        c = safe(a.getDesc()).compareTo(safe(b.getDesc()));
        if (c != 0) {
            return c;
        }
        return Integer.compare(normalizeJarId(a.getJarId()), normalizeJarId(b.getJarId()));
    };

    private static boolean sameMethod(MethodResult a, MethodResult b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return safe(a.getClassName()).equals(safe(b.getClassName()))
                && safe(a.getMethodName()).equals(safe(b.getMethodName()))
                && safe(a.getMethodDesc()).equals(safe(b.getMethodDesc()));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
