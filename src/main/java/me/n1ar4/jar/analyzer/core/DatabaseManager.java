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
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.analyze.spring.SpringParam;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStore;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger();

    private static final AtomicLong BUILD_SEQ = new AtomicLong(0L);
    private static final AtomicLong PROJECT_BUILD_SEQ = new AtomicLong(0L);
    private static final AtomicBoolean BUILDING = new AtomicBoolean(false);
    private static final ReentrantReadWriteLock DATA_LOCK = new ReentrantReadWriteLock(true);
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
    private static final Map<String, List<CallSiteEntity>> CALL_SITES_BY_CALLER_CLASS = new ConcurrentHashMap<>();
    private static final Map<String, List<CallSiteEntity>> CALL_SITES_BY_CALLER = new ConcurrentHashMap<>();
    private static final Map<String, List<LocalVarEntity>> LOCAL_VARS_BY_METHOD = new ConcurrentHashMap<>();

    private static final List<SpringController> SPRING_CONTROLLERS = Collections.synchronizedList(new ArrayList<>());
    private static final Set<String> SPRING_INTERCEPTORS = ConcurrentHashMap.newKeySet();
    private static final Set<String> SERVLETS = ConcurrentHashMap.newKeySet();
    private static final Set<String> FILTERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LISTENERS = ConcurrentHashMap.newKeySet();

    private static volatile String loadedProjectKey = "";
    private static volatile String buildingProjectKey = "";
    private static volatile ProjectModel lastProjectModel;

    static {
        logger.info("DatabaseManager running in neo4j-only mode");
    }

    private DatabaseManager() {
    }

    private static void withWriteLock(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        ReentrantReadWriteLock.WriteLock lock = DATA_LOCK.writeLock();
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    private static <T> T withReadLock(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        ReentrantReadWriteLock.ReadLock lock = DATA_LOCK.readLock();
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    private static <T> T withWriteLockValue(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        ReentrantReadWriteLock.WriteLock lock = DATA_LOCK.writeLock();
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public static long beginBuild() {
        return beginBuild(ActiveProjectContext.getActiveProjectKey());
    }

    public static long beginBuild(String projectKey) {
        return withWriteLockValue(() -> {
            long buildSeq = BUILD_SEQ.incrementAndGet();
            String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
            ProjectMetadataSnapshotStore.getInstance()
                    .markUnavailable(resolvedProjectKey, buildSeq, "build_started");
            BUILDING.set(true);
            buildingProjectKey = resolvedProjectKey;
            clearProjectRuntimeLocked();
            return buildSeq;
        });
    }

    public static void clearAllData() {
        withWriteLock(DatabaseManager::clearAllDataLocked);
    }

    public static ProjectModel getProjectModel() {
        ProjectModel model = withReadLock(() -> lastProjectModel);
        if (model != null) {
            return model;
        }
        ProjectRuntimeSnapshot snapshot = readPersistedProjectSnapshot(ActiveProjectContext.getActiveProjectKey());
        return snapshot == null ? null : restoreProjectModel(snapshot.projectModel());
    }

    public static long getProjectBuildSeq() {
        return getProjectBuildSeq(ActiveProjectContext.getActiveProjectKey());
    }

    public static long getProjectBuildSeq(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (!shouldUsePersistedProjectSnapshot(resolvedProjectKey)) {
            return PROJECT_BUILD_SEQ.get();
        }
        return ProjectMetadataSnapshotStore.getInstance().readBuildSeq(resolvedProjectKey);
    }

    public static ProjectRuntimeSnapshot buildProjectRuntimeSnapshot(
            long buildSeq,
            ProjectModel projectModel,
            List<String> jarPaths,
            Set<ClassFileEntity> classFiles,
            Set<ClassReference> classReferences,
            Set<MethodReference> methodReferences,
            Map<MethodReference.Handle, List<String>> methodStrings,
            Map<MethodReference.Handle, List<String>> methodAnnoStrings,
            List<ResourceEntity> resources,
            List<CallSiteEntity> callSites,
            List<LocalVarEntity> localVars,
            List<SpringController> springControllers,
            List<String> springInterceptors,
            List<String> servlets,
            List<String> filters,
            List<String> listeners) {
        return buildProjectRuntimeSnapshotInternal(
                buildSeq,
                projectModel,
                snapshotJarData(jarPaths),
                snapshotClassFileData(classFiles),
                snapshotClassReferenceData(classReferences),
                snapshotMethodReferenceData(methodReferences),
                snapshotMethodStringMap(methodStrings),
                snapshotMethodStringMap(methodAnnoStrings),
                snapshotResourceData(resources),
                snapshotCallSiteData(callSites),
                snapshotLocalVarData(localVars),
                snapshotSpringControllerData(springControllers),
                snapshotClassNameSet(springInterceptors),
                snapshotClassNameSet(servlets),
                snapshotClassNameSet(filters),
                snapshotClassNameSet(listeners)
        );
    }

    public static void restoreProjectRuntime(ProjectRuntimeSnapshot snapshot) {
        restoreProjectRuntime(ActiveProjectContext.getActiveProjectKey(), snapshot);
    }

    public static void restoreProjectRuntime(String projectKey, ProjectRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            clearAllData();
            return;
        }
        withWriteLock(() -> {
            clearAllDataLocked();
            restoreJarData(snapshot.jars());
            saveClassFiles(new LinkedHashSet<>(restoreClassFiles(snapshot.classFiles())));
            saveClassInfo(new LinkedHashSet<>(restoreClassReferences(snapshot.classReferences())));
            saveMethods(new LinkedHashSet<>(restoreMethodReferences(snapshot.methodReferences())));
            restoreStringMap(METHOD_STRINGS, snapshot.methodStrings());
            restoreStringMap(METHOD_STRING_ANNOS, snapshot.methodAnnoStrings());
            saveResources(restoreResources(snapshot.resources()));
            saveSpringController(restoreSpringControllers(snapshot.springControllers()));
            saveSpringInterceptor(new ArrayList<>(snapshot.springInterceptors()));
            saveServlets(new ArrayList<>(snapshot.servlets()));
            saveFilters(new ArrayList<>(snapshot.filters()));
            saveListeners(new ArrayList<>(snapshot.listeners()));
            lastProjectModel = restoreProjectModel(snapshot.projectModel());
            PROJECT_BUILD_SEQ.set(Math.max(0L, snapshot.buildSeq()));
            loadedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        });
    }

    public static void replaceJars(List<String> jarPaths) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            NEXT_JAR_ID.set(1);
            JAR_BY_PATH.clear();
            if (jarPaths == null || jarPaths.isEmpty()) {
                return;
            }
            for (String jarPath : jarPaths) {
                if (jarPath == null || jarPath.trim().isEmpty()) {
                    continue;
                }
                JAR_BY_PATH.computeIfAbsent(jarPath, DatabaseManager::newJarEntity);
            }
        });
    }

    public static JarEntity getJarId(String jarPath) {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            return null;
        }
        return withWriteLockValue(() -> JAR_BY_PATH.computeIfAbsent(jarPath, DatabaseManager::newJarEntity));
    }

    public static void saveClassFiles(Set<ClassFileEntity> classFileList) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
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
        });
    }

    public static void saveClassInfo(Set<ClassReference> discoveredClasses) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
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
        });
    }

    public static void saveMethods(Set<MethodReference> discoveredMethods) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
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
        });
    }

    public static void saveStrMap(Map<MethodReference.Handle, List<String>> strMap,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            METHOD_STRINGS.clear();
            METHOD_STRING_ANNOS.clear();
            saveMethodStringMap(METHOD_STRINGS, strMap);
            saveMethodStringMap(METHOD_STRING_ANNOS, stringAnnoMap);
        });
    }

    public static void saveResources(List<ResourceEntity> resources) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
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
        });
    }

    public static void saveCallSites(List<CallSiteEntity> callSites) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            CALL_SITES_BY_CALLER_CLASS.clear();
            CALL_SITES_BY_CALLER.clear();
            if (callSites == null || callSites.isEmpty()) {
                return;
            }
            for (CallSiteEntity row : callSites) {
                if (row == null) {
                    continue;
                }
                CallSiteEntity copy = copyCallSiteEntity(row);
                saveCallSiteByCallerClass(copy.getCallerClassName(), copy);
                String callerKey = methodKey(copy.getCallerClassName(), copy.getCallerMethodName(), copy.getCallerMethodDesc(), copy.getJarId());
                saveCallSiteByCaller(callerKey, copy);
                Integer jarId = copy.getJarId();
                if (jarId != null && jarId >= 0) {
                    saveCallSiteByCaller(methodKey(
                            copy.getCallerClassName(),
                            copy.getCallerMethodName(),
                            copy.getCallerMethodDesc(),
                            -1
                    ), copy);
                }
            }
        });
    }

    public static void saveLocalVars(List<LocalVarEntity> localVars) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            LOCAL_VARS_BY_METHOD.clear();
            if (localVars == null || localVars.isEmpty()) {
                return;
            }
            for (LocalVarEntity row : localVars) {
                if (row == null) {
                    continue;
                }
                LocalVarEntity copy = copyLocalVarEntity(row);
                String key = methodKey(copy.getClassName(), copy.getMethodName(), copy.getMethodDesc(), copy.getJarId());
                saveLocalVarByMethod(key, copy);
                Integer jarId = copy.getJarId();
                if (jarId != null && jarId >= 0) {
                    saveLocalVarByMethod(methodKey(copy.getClassName(), copy.getMethodName(), copy.getMethodDesc(), -1), copy);
                }
            }
        });
    }

    public static void saveSpringController(ArrayList<SpringController> controllers) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            SPRING_CONTROLLERS.clear();
            if (controllers == null || controllers.isEmpty()) {
                return;
            }
            for (SpringController controller : controllers) {
                SpringController copy = copySpringController(controller);
                if (copy != null) {
                    SPRING_CONTROLLERS.add(copy);
                }
            }
        });
    }

    public static void saveSpringInterceptor(ArrayList<String> interceptors) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            SPRING_INTERCEPTORS.clear();
            saveClassNameSet(SPRING_INTERCEPTORS, interceptors);
        });
    }

    public static void saveServlets(ArrayList<String> servlets) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            SERVLETS.clear();
            saveClassNameSet(SERVLETS, servlets);
        });
    }

    public static void saveFilters(ArrayList<String> filters) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            FILTERS.clear();
            saveClassNameSet(FILTERS, filters);
        });
    }

    public static void saveListeners(ArrayList<String> listeners) {
        withWriteLock(() -> {
            markLoadedProjectRuntimeCurrent();
            LISTENERS.clear();
            saveClassNameSet(LISTENERS, listeners);
        });
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
        return withReadLock(() -> new ArrayList<>(FAVORITES));
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        return withReadLock(() -> new ArrayList<>(HISTORIES));
    }

    public static long getBuildSeq() {
        return BUILD_SEQ.get();
    }

    static void finishBuild() {
        finishBuild(true);
    }

    static void finishBuild(boolean buildCommitted) {
        String finishedProjectKey = ActiveProjectContext.resolveRequestedOrActive(buildingProjectKey);
        BUILDING.set(false);
        buildingProjectKey = "";
        if (buildCommitted && !finishedProjectKey.isBlank()) {
            try {
                ProjectMetadataSnapshotStore.getInstance().clearUnavailable(finishedProjectKey);
            } catch (Exception ex) {
                logger.debug("clear project runtime unavailable marker after build fail: key={} err={}",
                        finishedProjectKey, ex.toString());
            }
        } else if (!buildCommitted && !finishedProjectKey.isBlank()
                && finishedProjectKey.equals(ActiveProjectContext.getPublishedActiveProjectKey())) {
            try {
                ProjectRuntimeSnapshot.ProjectModelData modelData = ProjectMetadataSnapshotStore.getInstance()
                        .readProjectModelRegardlessOfAvailability(finishedProjectKey);
                if (modelData != null) {
                    restoreProjectRuntime(finishedProjectKey, unavailableRuntimeSnapshot(modelData));
                }
            } catch (Exception ex) {
                logger.debug("restore unavailable runtime after failed build fail: key={} err={}",
                        finishedProjectKey, ex.toString());
            }
        }
    }

    public static boolean isBuilding() {
        return BUILDING.get();
    }

    public static boolean isBuilding(String projectKey) {
        if (!BUILDING.get()) {
            return false;
        }
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return resolvedProjectKey.equals(ActiveProjectContext.resolveRequestedOrActive(buildingProjectKey));
    }

    public static boolean isProjectReady() {
        return isProjectReady(ActiveProjectContext.getActiveProjectKey());
    }

    public static boolean isProjectReady(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (!shouldUsePersistedProjectSnapshot(resolvedProjectKey)) {
            return withReadLock(() -> PROJECT_BUILD_SEQ.get() > 0L && hasProjectModelData(lastProjectModel));
        }
        ProjectRuntimeSnapshot snapshot = ProjectMetadataSnapshotStore.getInstance().read(resolvedProjectKey);
        return snapshot != null
                && snapshot.buildSeq() > 0L
                && hasProjectModelData(snapshot.projectModel());
    }

    public static void ensureProjectReadable() {
        ensureProjectReadable(ActiveProjectContext.getActiveProjectKey());
    }

    public static void ensureProjectReadable(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (isBuilding(resolvedProjectKey)
                || ActiveProjectContext.isProjectMutationInProgress(resolvedProjectKey)) {
            throw new IllegalStateException("project_build_in_progress");
        }
        if (!isProjectReady(resolvedProjectKey)) {
            throw new IllegalStateException("project_model_missing_rebuild");
        }
    }

    public static String getSemanticCacheValue(String cacheKey, String cacheType) {
        if (cacheKey == null || cacheType == null) {
            return null;
        }
        return withReadLock(() -> SEMANTIC_CACHE.get(semanticKey(cacheType, cacheKey)));
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
        return withReadLock(() -> new ArrayList<>(VUL_REPORTS));
    }

    public static List<JarEntity> getJarsMeta() {
        return runtimeView().jarsMeta();
    }

    public static JarEntity getJarById(Integer jarId) {
        return runtimeView().jarById(jarId);
    }

    public static List<ClassFileEntity> getClassFiles() {
        return runtimeView().classFiles();
    }

    public static List<ClassFileEntity> getClassFilesByClass(String className) {
        return runtimeView().classFilesByClass(className);
    }

    public static ClassFileEntity getClassFileByClass(String className, Integer jarId) {
        return runtimeView().classFileByClass(className, jarId);
    }

    public static List<ClassReference> getClassReferences() {
        return runtimeView().classReferences();
    }

    public static List<ClassReference> getClassReferencesByName(String className) {
        return runtimeView().classReferencesByName(className);
    }

    public static ClassReference getClassReferenceByName(String className, Integer jarId) {
        return runtimeView().classReferenceByName(className, jarId);
    }

    public static List<MethodReference> getMethodReferences() {
        return runtimeView().methodReferences();
    }

    public static List<MethodReference> getMethodReferencesByClass(String className) {
        return runtimeView().methodReferencesByClass(className);
    }

    public static List<String> getMethodStringValues(String className,
                                                     String methodName,
                                                     String methodDesc,
                                                     Integer jarId) {
        return runtimeView().methodStringValues(className, methodName, methodDesc, jarId);
    }

    public static Map<String, List<String>> getMethodStringsSnapshot() {
        return runtimeView().methodStringsSnapshot();
    }

    public static Map<String, List<String>> getMethodAnnoStringsSnapshot() {
        return runtimeView().methodAnnoStringsSnapshot();
    }

    public static List<String> getMethodAnnoStringValues(String className,
                                                         String methodName,
                                                         String methodDesc,
                                                         Integer jarId) {
        return runtimeView().methodAnnoStringValues(className, methodName, methodDesc, jarId);
    }

    public static List<ResourceEntity> getResources() {
        return runtimeView().resources();
    }

    public static List<CallSiteEntity> getCallSitesByCaller(String className,
                                                            String methodName,
                                                            String methodDesc) {
        return runtimeView().callSitesByCaller(className, methodName, methodDesc);
    }

    public static List<LocalVarEntity> getLocalVarsByMethod(String className,
                                                             String methodName,
                                                             String methodDesc) {
        return runtimeView().localVarsByMethod(className, methodName, methodDesc);
    }

    public static List<SpringController> getSpringControllers() {
        return runtimeView().springControllers();
    }

    public static Set<String> getSpringInterceptors() {
        return runtimeView().springInterceptors();
    }

    public static Set<String> getServlets() {
        return runtimeView().servlets();
    }

    public static Set<String> getFilters() {
        return runtimeView().filters();
    }

    public static Set<String> getListeners() {
        return runtimeView().listeners();
    }

    private static boolean usesLoadedProjectRuntime(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        String loaded = ActiveProjectContext.normalizeProjectKey(loadedProjectKey);
        return !loaded.isBlank() && loaded.equals(normalized);
    }

    private static boolean shouldUsePersistedProjectSnapshot(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (normalized.isBlank()) {
            return false;
        }
        String publishedActive = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        if (!publishedActive.isBlank()
                && publishedActive.equals(normalized)
                && ActiveProjectContext.isTemporaryProjectKey(normalized)) {
            return false;
        }
        if (usesLoadedProjectRuntime(normalized)) {
            return false;
        }
        if (isBuilding(normalized) || ActiveProjectContext.isProjectMutationInProgress(normalized)) {
            return false;
        }
        return true;
    }

    private static ProjectRuntimeSnapshot readPersistedProjectSnapshot(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (!shouldUsePersistedProjectSnapshot(resolvedProjectKey)) {
            return null;
        }
        return ProjectMetadataSnapshotStore.getInstance().read(resolvedProjectKey);
    }

    private static ProjectRuntimeReadView runtimeView() {
        String projectKey = ActiveProjectContext.resolveRequestedOrActive(
                ActiveProjectContext.getActiveProjectKey());
        if (shouldUsePersistedProjectSnapshot(projectKey)) {
            return new PersistedProjectRuntimeReadView(
                    projectKey,
                    ProjectMetadataSnapshotStore.getInstance().read(projectKey)
            );
        }
        return LiveProjectRuntimeReadView.INSTANCE;
    }

    private interface ProjectRuntimeReadView {
        List<JarEntity> jarsMeta();

        JarEntity jarById(Integer jarId);

        List<ClassFileEntity> classFiles();

        List<ClassFileEntity> classFilesByClass(String className);

        ClassFileEntity classFileByClass(String className, Integer jarId);

        List<ClassReference> classReferences();

        List<ClassReference> classReferencesByName(String className);

        ClassReference classReferenceByName(String className, Integer jarId);

        List<MethodReference> methodReferences();

        List<MethodReference> methodReferencesByClass(String className);

        List<String> methodStringValues(String className, String methodName, String methodDesc, Integer jarId);

        Map<String, List<String>> methodStringsSnapshot();

        Map<String, List<String>> methodAnnoStringsSnapshot();

        List<String> methodAnnoStringValues(String className, String methodName, String methodDesc, Integer jarId);

        List<ResourceEntity> resources();

        List<CallSiteEntity> callSitesByCaller(String className, String methodName, String methodDesc);

        List<LocalVarEntity> localVarsByMethod(String className, String methodName, String methodDesc);

        List<SpringController> springControllers();

        Set<String> springInterceptors();

        Set<String> servlets();

        Set<String> filters();

        Set<String> listeners();
    }

    private static final class PersistedProjectRuntimeReadView implements ProjectRuntimeReadView {
        private final String projectKey;
        private final ProjectRuntimeSnapshot snapshot;

        private PersistedProjectRuntimeReadView(String projectKey, ProjectRuntimeSnapshot snapshot) {
            this.projectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
            this.snapshot = snapshot;
        }

        @Override
        public List<JarEntity> jarsMeta() {
            return snapshot == null ? Collections.emptyList() : restoreJarEntities(snapshot.jars());
        }

        @Override
        public JarEntity jarById(Integer jarId) {
            if (jarId == null || jarId < 0 || snapshot == null) {
                return null;
            }
            for (ProjectRuntimeSnapshot.JarData row : snapshot.jars()) {
                if (row != null && row.jid() == jarId) {
                    return toJarEntity(row);
                }
            }
            return null;
        }

        @Override
        public List<ClassFileEntity> classFiles() {
            return snapshot == null ? Collections.emptyList() : restoreClassFiles(snapshot.classFiles());
        }

        @Override
        public List<ClassFileEntity> classFilesByClass(String className) {
            return snapshot == null ? Collections.emptyList() : filterSnapshotClassFiles(snapshot.classFiles(), className);
        }

        @Override
        public ClassFileEntity classFileByClass(String className, Integer jarId) {
            ProjectRuntimeSnapshot.ClassFileData row = ProjectMetadataSnapshotStore.getInstance()
                    .findClassFile(projectKey, className, jarId);
            return row == null ? null : toClassFileEntity(row);
        }

        @Override
        public List<ClassReference> classReferences() {
            return snapshot == null ? Collections.emptyList() : restoreClassReferences(snapshot.classReferences());
        }

        @Override
        public List<ClassReference> classReferencesByName(String className) {
            List<ProjectRuntimeSnapshot.ClassReferenceData> rows = ProjectMetadataSnapshotStore.getInstance()
                    .findClassReferences(projectKey, className);
            if (rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<ClassReference> out = new ArrayList<>(rows.size());
            for (ProjectRuntimeSnapshot.ClassReferenceData row : rows) {
                ClassReference ref = toClassReference(row);
                if (ref != null) {
                    out.add(ref);
                }
            }
            return out.isEmpty() ? Collections.emptyList() : out;
        }

        @Override
        public ClassReference classReferenceByName(String className, Integer jarId) {
            ProjectRuntimeSnapshot.ClassReferenceData row = ProjectMetadataSnapshotStore.getInstance()
                    .findClassReference(projectKey, className, jarId);
            return row == null ? null : toClassReference(row);
        }

        @Override
        public List<MethodReference> methodReferences() {
            return snapshot == null ? Collections.emptyList() : restoreMethodReferences(snapshot.methodReferences());
        }

        @Override
        public List<MethodReference> methodReferencesByClass(String className) {
            List<ProjectRuntimeSnapshot.MethodReferenceData> rows = ProjectMetadataSnapshotStore.getInstance()
                    .findMethodReferencesByClass(projectKey, className);
            if (rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<MethodReference> out = new ArrayList<>(rows.size());
            for (ProjectRuntimeSnapshot.MethodReferenceData row : rows) {
                MethodReference ref = toMethodReference(row);
                if (ref != null) {
                    out.add(ref);
                }
            }
            return out.isEmpty() ? Collections.emptyList() : out;
        }

        @Override
        public List<String> methodStringValues(String className, String methodName, String methodDesc, Integer jarId) {
            return snapshot == null
                    ? Collections.emptyList()
                    : readSnapshotMethodStrings(snapshot.methodStrings(), className, methodName, methodDesc, jarId);
        }

        @Override
        public Map<String, List<String>> methodStringsSnapshot() {
            return snapshot == null ? Collections.emptyMap() : copyStringMap(snapshot.methodStrings());
        }

        @Override
        public Map<String, List<String>> methodAnnoStringsSnapshot() {
            return snapshot == null ? Collections.emptyMap() : copyStringMap(snapshot.methodAnnoStrings());
        }

        @Override
        public List<String> methodAnnoStringValues(String className, String methodName, String methodDesc, Integer jarId) {
            return snapshot == null
                    ? Collections.emptyList()
                    : readSnapshotMethodStrings(snapshot.methodAnnoStrings(), className, methodName, methodDesc, jarId);
        }

        @Override
        public List<ResourceEntity> resources() {
            return snapshot == null ? Collections.emptyList() : restoreResources(snapshot.resources());
        }

        @Override
        public List<CallSiteEntity> callSitesByCaller(String className, String methodName, String methodDesc) {
            if (snapshot == null) {
                return Collections.emptyList();
            }
            List<ProjectRuntimeSnapshot.CallSiteData> rows = ProjectMetadataSnapshotStore.getInstance()
                    .findCallSitesByCaller(projectKey, className, methodName, methodDesc);
            return rows.isEmpty() ? Collections.emptyList() : restoreCallSites(rows);
        }

        @Override
        public List<LocalVarEntity> localVarsByMethod(String className, String methodName, String methodDesc) {
            if (snapshot == null) {
                return Collections.emptyList();
            }
            List<ProjectRuntimeSnapshot.LocalVarData> rows = ProjectMetadataSnapshotStore.getInstance()
                    .findLocalVarsByMethod(projectKey, className, methodName, methodDesc);
            return rows.isEmpty() ? Collections.emptyList() : restoreLocalVars(rows);
        }

        @Override
        public List<SpringController> springControllers() {
            return snapshot == null ? Collections.emptyList() : restoreSpringControllers(snapshot.springControllers());
        }

        @Override
        public Set<String> springInterceptors() {
            return snapshot == null ? Collections.emptySet() : new HashSet<>(snapshot.springInterceptors());
        }

        @Override
        public Set<String> servlets() {
            return snapshot == null ? Collections.emptySet() : new HashSet<>(snapshot.servlets());
        }

        @Override
        public Set<String> filters() {
            return snapshot == null ? Collections.emptySet() : new HashSet<>(snapshot.filters());
        }

        @Override
        public Set<String> listeners() {
            return snapshot == null ? Collections.emptySet() : new HashSet<>(snapshot.listeners());
        }
    }

    private static final class LiveProjectRuntimeReadView implements ProjectRuntimeReadView {
        private static final LiveProjectRuntimeReadView INSTANCE = new LiveProjectRuntimeReadView();

        @Override
        public List<JarEntity> jarsMeta() {
            return withReadLock(() -> {
                ArrayList<JarEntity> out = new ArrayList<>(JAR_BY_PATH.values());
                sortJarEntities(out);
                return out;
            });
        }

        @Override
        public JarEntity jarById(Integer jarId) {
            return withReadLock(() -> {
                if (jarId == null || jarId < 0) {
                    return null;
                }
                for (JarEntity item : JAR_BY_PATH.values()) {
                    if (item != null && item.getJid() == jarId) {
                        return item;
                    }
                }
                return null;
            });
        }

        @Override
        public List<ClassFileEntity> classFiles() {
            return withReadLock(() -> new ArrayList<>(CLASS_FILES));
        }

        @Override
        public List<ClassFileEntity> classFilesByClass(String className) {
            return withReadLock(() -> {
                String normalized = normalizeClassName(className);
                if (normalized == null) {
                    return Collections.emptyList();
                }
                List<ClassFileEntity> rows = CLASS_FILES_BY_NAME.get(normalized);
                if (rows == null || rows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(rows);
            });
        }

        @Override
        public ClassFileEntity classFileByClass(String className, Integer jarId) {
            return withReadLock(() -> {
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
                            if (value != null && value.equals(jarId)) {
                                return row;
                            }
                        }
                    }
                }
                return PRIMARY_CLASS_FILE_BY_NAME.get(normalized);
            });
        }

        @Override
        public List<ClassReference> classReferences() {
            return withReadLock(() -> new ArrayList<>(CLASS_REFERENCES));
        }

        @Override
        public List<ClassReference> classReferencesByName(String className) {
            return withReadLock(() -> {
                String normalized = normalizeClassName(className);
                if (normalized == null) {
                    return Collections.emptyList();
                }
                List<ClassReference> rows = CLASS_REFS_BY_NAME.get(normalized);
                if (rows == null || rows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(rows);
            });
        }

        @Override
        public ClassReference classReferenceByName(String className, Integer jarId) {
            return withReadLock(() -> {
                List<ClassReference> rows = classReferencesByName(className);
                if (rows.isEmpty()) {
                    return null;
                }
                if (jarId != null && jarId >= 0) {
                    for (ClassReference row : rows) {
                        if (row == null) {
                            continue;
                        }
                        Integer value = row.getJarId();
                        if (value != null && value.equals(jarId)) {
                            return row;
                        }
                    }
                }
                return rows.get(0);
            });
        }

        @Override
        public List<MethodReference> methodReferences() {
            return withReadLock(() -> new ArrayList<>(METHOD_REFERENCES));
        }

        @Override
        public List<MethodReference> methodReferencesByClass(String className) {
            return withReadLock(() -> {
                String normalized = normalizeClassName(className);
                if (normalized == null) {
                    return Collections.emptyList();
                }
                List<MethodReference> rows = METHODS_BY_CLASS.get(normalized);
                if (rows == null || rows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(rows);
            });
        }

        @Override
        public List<String> methodStringValues(String className, String methodName, String methodDesc, Integer jarId) {
            return withReadLock(() -> {
                String key = methodKey(className, methodName, methodDesc, jarId);
                List<String> rows = METHOD_STRINGS.get(key);
                if ((rows == null || rows.isEmpty()) && jarId != null && jarId >= 0) {
                    rows = METHOD_STRINGS.get(methodKey(className, methodName, methodDesc, -1));
                }
                if (rows == null || rows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(rows);
            });
        }

        @Override
        public Map<String, List<String>> methodStringsSnapshot() {
            return withReadLock(() -> new HashMap<>(METHOD_STRINGS));
        }

        @Override
        public Map<String, List<String>> methodAnnoStringsSnapshot() {
            return withReadLock(() -> new HashMap<>(METHOD_STRING_ANNOS));
        }

        @Override
        public List<String> methodAnnoStringValues(String className, String methodName, String methodDesc, Integer jarId) {
            return withReadLock(() -> {
                String key = methodKey(className, methodName, methodDesc, jarId);
                List<String> rows = METHOD_STRING_ANNOS.get(key);
                if ((rows == null || rows.isEmpty()) && jarId != null && jarId >= 0) {
                    rows = METHOD_STRING_ANNOS.get(methodKey(className, methodName, methodDesc, -1));
                }
                if (rows == null || rows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(rows);
            });
        }

        @Override
        public List<ResourceEntity> resources() {
            return withReadLock(() -> new ArrayList<>(RESOURCE_ENTRIES));
        }

        @Override
        public List<CallSiteEntity> callSitesByCaller(String className, String methodName, String methodDesc) {
            List<CallSiteEntity> rows = withReadLock(() -> {
                if (CALL_SITES_BY_CALLER_CLASS.isEmpty() && CALL_SITES_BY_CALLER.isEmpty()) {
                    return null;
                }
                String normalizedClass = normalizeClassName(className);
                if (normalizedClass == null) {
                    return Collections.emptyList();
                }
                String normalizedMethod = safe(methodName);
                String normalizedDesc = safe(methodDesc);
                if (normalizedMethod.isEmpty() || normalizedDesc.isEmpty()) {
                    List<CallSiteEntity> classRows = CALL_SITES_BY_CALLER_CLASS.get(normalizedClass);
                    if (classRows == null || classRows.isEmpty()) {
                        return Collections.emptyList();
                    }
                    if (normalizedMethod.isEmpty() && normalizedDesc.isEmpty()) {
                        return new ArrayList<>(classRows);
                    }
                    List<CallSiteEntity> out = new ArrayList<>();
                    for (CallSiteEntity row : classRows) {
                        if (row == null) {
                            continue;
                        }
                        if (!normalizedMethod.isEmpty() && !normalizedMethod.equals(safe(row.getCallerMethodName()))) {
                            continue;
                        }
                        if (!normalizedDesc.isEmpty() && !normalizedDesc.equals(safe(row.getCallerMethodDesc()))) {
                            continue;
                        }
                        out.add(row);
                    }
                    return out.isEmpty() ? Collections.emptyList() : out;
                }
                String key = methodKey(className, methodName, methodDesc, -1);
                List<CallSiteEntity> matchedRows = CALL_SITES_BY_CALLER.get(key);
                if (matchedRows == null || matchedRows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(matchedRows);
            });
            if (rows != null) {
                return rows;
            }
            return readPersistedCallSitesByCaller(ActiveProjectContext.getActiveProjectKey(), className, methodName, methodDesc);
        }

        @Override
        public List<LocalVarEntity> localVarsByMethod(String className, String methodName, String methodDesc) {
            List<LocalVarEntity> rows = withReadLock(() -> {
                if (LOCAL_VARS_BY_METHOD.isEmpty()) {
                    return null;
                }
                String key = methodKey(className, methodName, methodDesc, -1);
                List<LocalVarEntity> matchedRows = LOCAL_VARS_BY_METHOD.get(key);
                if (matchedRows == null || matchedRows.isEmpty()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(matchedRows);
            });
            if (rows != null) {
                return rows;
            }
            return readPersistedLocalVarsByMethod(ActiveProjectContext.getActiveProjectKey(), className, methodName, methodDesc);
        }

        @Override
        public List<SpringController> springControllers() {
            return withReadLock(() -> new ArrayList<>(SPRING_CONTROLLERS));
        }

        @Override
        public Set<String> springInterceptors() {
            return withReadLock(() -> new HashSet<>(SPRING_INTERCEPTORS));
        }

        @Override
        public Set<String> servlets() {
            return withReadLock(() -> new HashSet<>(SERVLETS));
        }

        @Override
        public Set<String> filters() {
            return withReadLock(() -> new HashSet<>(FILTERS));
        }

        @Override
        public Set<String> listeners() {
            return withReadLock(() -> new HashSet<>(LISTENERS));
        }
    }

    private static List<JarEntity> restoreJarEntities(List<ProjectRuntimeSnapshot.JarData> jars) {
        if (jars == null || jars.isEmpty()) {
            return Collections.emptyList();
        }
        List<JarEntity> out = new ArrayList<>(jars.size());
        for (ProjectRuntimeSnapshot.JarData row : jars) {
            JarEntity entity = toJarEntity(row);
            if (entity != null) {
                out.add(entity);
            }
        }
        sortJarEntities(out);
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    private static JarEntity toJarEntity(ProjectRuntimeSnapshot.JarData row) {
        if (row == null) {
            return null;
        }
        JarEntity entity = new JarEntity();
        entity.setJid(row.jid());
        entity.setJarName(row.jarName());
        entity.setJarAbsPath(row.jarAbsPath());
        return entity;
    }

    private static void sortJarEntities(List<JarEntity> jars) {
        if (jars == null || jars.size() < 2) {
            return;
        }
        jars.sort(new Comparator<JarEntity>() {
            @Override
            public int compare(JarEntity left, JarEntity right) {
                return Integer.compare(jarId(left), jarId(right));
            }
        });
    }

    private static int jarId(JarEntity entity) {
        return entity == null ? Integer.MAX_VALUE : entity.getJid();
    }

    private static List<String> readSnapshotMethodStrings(Map<String, List<String>> snapshot,
                                                          String className,
                                                          String methodName,
                                                          String methodDesc,
                                                          Integer jarId) {
        if (snapshot == null || snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        String key = methodKey(className, methodName, methodDesc, jarId);
        List<String> rows = snapshot.get(key);
        if ((rows == null || rows.isEmpty()) && jarId != null && jarId >= 0) {
            rows = snapshot.get(methodKey(className, methodName, methodDesc, -1));
        }
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows);
    }

    private static Map<String, List<String>> copyStringMap(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> out = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return out.isEmpty() ? Collections.emptyMap() : out;
    }

    private static List<ClassFileEntity> filterSnapshotClassFiles(List<ProjectRuntimeSnapshot.ClassFileData> rows,
                                                                  String className) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<ClassFileEntity> out = new ArrayList<>();
        for (ProjectRuntimeSnapshot.ClassFileData row : rows) {
            if (row == null || !normalized.equals(normalizeClassName(row.className()))) {
                continue;
            }
            ClassFileEntity entity = toClassFileEntity(row);
            if (entity != null) {
                out.add(entity);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    private static void markLoadedProjectRuntimeCurrent() {
        loadedProjectKey = ActiveProjectContext.resolveRequestedOrActive(null);
    }

    private static boolean hasProjectModelData(ProjectModel model) {
        if (model == null) {
            return false;
        }
        if (model.primaryInputPath() != null) {
            return true;
        }
        return model.roots() != null && !model.roots().isEmpty();
    }

    private static boolean hasProjectModelData(ProjectRuntimeSnapshot.ProjectModelData model) {
        if (model == null) {
            return false;
        }
        if (model.primaryInputPath() != null && !model.primaryInputPath().isBlank()) {
            return true;
        }
        return model.roots() != null && !model.roots().isEmpty();
    }

    private static void clearProjectRuntimeLocked() {
        NEXT_JAR_ID.set(1);
        NEXT_RESOURCE_ID.set(1);
        PROJECT_BUILD_SEQ.set(0L);
        loadedProjectKey = "";
        JAR_BY_PATH.clear();
        SEMANTIC_CACHE.clear();
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
        CALL_SITES_BY_CALLER_CLASS.clear();
        CALL_SITES_BY_CALLER.clear();
        LOCAL_VARS_BY_METHOD.clear();
        SPRING_CONTROLLERS.clear();
        SPRING_INTERCEPTORS.clear();
        SERVLETS.clear();
        FILTERS.clear();
        LISTENERS.clear();
        lastProjectModel = null;
        invalidateGraphSnapshotCache();
    }

    private static void clearAllDataLocked() {
        clearProjectRuntimeLocked();
        NEXT_VUL_ID.set(1);
        FAVORITES.clear();
        HISTORIES.clear();
        VUL_REPORTS.clear();
    }

    private static ProjectRuntimeSnapshot unavailableRuntimeSnapshot(ProjectRuntimeSnapshot.ProjectModelData modelData) {
        return new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                0L,
                modelData,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    private static void invalidateGraphSnapshotCache() {
        try {
            GraphStore.invalidateCache();
        } catch (Exception ex) {
            logger.debug("invalidate graph snapshot cache fail: {}", ex.toString());
        }
    }

    private static JarEntity newJarEntity(String jarPath) {
        JarEntity entity = new JarEntity();
        entity.setJid(NEXT_JAR_ID.getAndIncrement());
        entity.setJarAbsPath(jarPath);
        entity.setJarName(resolveJarName(jarPath));
        return entity;
    }

    private static ClassFileEntity toClassFileEntity(ProjectRuntimeSnapshot.ClassFileData row) {
        if (row == null) {
            return null;
        }
        ClassFileEntity entity = new ClassFileEntity();
        entity.setCfId(row.cfId());
        entity.setClassName(row.className());
        entity.setPathStr(row.pathStr());
        entity.setJarName(row.jarName());
        entity.setJarId(row.jarId());
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

    private static void saveCallSiteByCaller(String key, CallSiteEntity entity) {
        if (key == null || key.isEmpty() || entity == null) {
            return;
        }
        CALL_SITES_BY_CALLER.computeIfAbsent(key, ignore -> Collections.synchronizedList(new ArrayList<>()))
                .add(entity);
    }

    private static void saveCallSiteByCallerClass(String className, CallSiteEntity entity) {
        String normalizedClass = normalizeClassName(className);
        if (normalizedClass == null || entity == null) {
            return;
        }
        CALL_SITES_BY_CALLER_CLASS.computeIfAbsent(normalizedClass, ignore -> Collections.synchronizedList(new ArrayList<>()))
                .add(entity);
    }

    private static void saveLocalVarByMethod(String key, LocalVarEntity entity) {
        if (key == null || key.isEmpty() || entity == null) {
            return;
        }
        LOCAL_VARS_BY_METHOD.computeIfAbsent(key, ignore -> Collections.synchronizedList(new ArrayList<>()))
                .add(entity);
    }

    private static ProjectRuntimeSnapshot buildProjectRuntimeSnapshotInternal(
            long buildSeq,
            ProjectModel projectModel,
            List<ProjectRuntimeSnapshot.JarData> jars,
            List<ProjectRuntimeSnapshot.ClassFileData> classFiles,
            List<ProjectRuntimeSnapshot.ClassReferenceData> classReferences,
            List<ProjectRuntimeSnapshot.MethodReferenceData> methodReferences,
            Map<String, List<String>> methodStrings,
            Map<String, List<String>> methodAnnoStrings,
            List<ProjectRuntimeSnapshot.ResourceData> resources,
            List<ProjectRuntimeSnapshot.CallSiteData> callSites,
            List<ProjectRuntimeSnapshot.LocalVarData> localVars,
            List<ProjectRuntimeSnapshot.SpringControllerData> springControllers,
            Set<String> springInterceptors,
            Set<String> servlets,
            Set<String> filters,
            Set<String> listeners) {
        return new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                buildSeq,
                toProjectModelData(projectModel),
                jars,
                classFiles,
                classReferences,
                methodReferences,
                methodStrings,
                methodAnnoStrings,
                resources,
                callSites,
                localVars,
                springControllers,
                springInterceptors,
                servlets,
                filters,
                listeners
        );
    }

    private static List<ProjectRuntimeSnapshot.JarData> snapshotJarData(List<String> jarPaths) {
        if (jarPaths == null || jarPaths.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.JarData> out = new ArrayList<>();
        int nextJarId = 1;
        for (String jarPath : jarPaths) {
            String normalized = safe(jarPath).trim();
            if (normalized.isEmpty()) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.JarData(
                    nextJarId++,
                    resolveJarName(normalized),
                    normalized
            ));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.ClassFileData> snapshotClassFileData(java.util.Collection<ClassFileEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        ArrayList<ClassFileEntity> sorted = new ArrayList<>(rows);
        sorted.sort(CLASS_FILE_COMPARATOR);
        List<ProjectRuntimeSnapshot.ClassFileData> out = new ArrayList<>(sorted.size());
        for (ClassFileEntity row : sorted) {
            if (row == null) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.ClassFileData(
                    row.getCfId(),
                    row.getClassName(),
                    classFilePath(row),
                    row.getJarName(),
                    row.getJarId()
            ));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.ClassReferenceData> snapshotClassReferenceData(java.util.Collection<ClassReference> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        ArrayList<ClassReference> sorted = new ArrayList<>(rows);
        sorted.sort(CLASS_REF_COMPARATOR);
        List<ProjectRuntimeSnapshot.ClassReferenceData> out = new ArrayList<>(sorted.size());
        for (ClassReference row : sorted) {
            ProjectRuntimeSnapshot.ClassReferenceData data = toClassReferenceData(row);
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.MethodReferenceData> snapshotMethodReferenceData(java.util.Collection<MethodReference> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        ArrayList<MethodReference> sorted = new ArrayList<>(rows);
        sorted.sort(METHOD_REF_COMPARATOR);
        List<ProjectRuntimeSnapshot.MethodReferenceData> out = new ArrayList<>(sorted.size());
        for (MethodReference row : sorted) {
            ProjectRuntimeSnapshot.MethodReferenceData data = toMethodReferenceData(row);
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    private static Map<String, List<String>> snapshotMethodStringMap(Map<MethodReference.Handle, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> out = new HashMap<>();
        saveMethodStringMap(out, source);
        return snapshotStringMap(out);
    }

    private static Map<String, List<String>> snapshotStringMap(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> out = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key == null || values == null || values.isEmpty()) {
                continue;
            }
            out.put(key, List.copyOf(values));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.ResourceData> snapshotResourceData(java.util.Collection<ResourceEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.ResourceData> out = new ArrayList<>(rows.size());
        for (ResourceEntity row : rows) {
            if (row == null) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.ResourceData(
                    row.getRid(),
                    row.getResourcePath(),
                    row.getPathStr(),
                    row.getJarName(),
                    row.getJarId(),
                    row.getFileSize(),
                    row.getIsText()
            ));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.CallSiteData> snapshotCallSiteData(java.util.Collection<CallSiteEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.CallSiteData> out = new ArrayList<>(rows.size());
        for (CallSiteEntity row : rows) {
            if (row == null) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.CallSiteData(
                    row.getCallerClassName(),
                    row.getCallerMethodName(),
                    row.getCallerMethodDesc(),
                    row.getCalleeOwner(),
                    row.getCalleeMethodName(),
                    row.getCalleeMethodDesc(),
                    row.getOpCode(),
                    row.getLineNumber(),
                    row.getCallIndex(),
                    row.getReceiverType(),
                    row.getJarId(),
                    row.getCallSiteKey()
            ));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.LocalVarData> snapshotLocalVarData(java.util.Collection<LocalVarEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.LocalVarData> out = new ArrayList<>(rows.size());
        for (LocalVarEntity row : rows) {
            if (row == null) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.LocalVarData(
                    row.getClassName(),
                    row.getMethodName(),
                    row.getMethodDesc(),
                    row.getVarIndex(),
                    row.getVarName(),
                    row.getVarDesc(),
                    row.getVarSignature(),
                    row.getStartLine(),
                    row.getEndLine(),
                    row.getJarId()
            ));
        }
        return out;
    }

    private static List<ProjectRuntimeSnapshot.SpringControllerData> snapshotSpringControllerData(java.util.Collection<SpringController> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.SpringControllerData> out = new ArrayList<>(rows.size());
        for (SpringController controller : rows) {
            ProjectRuntimeSnapshot.SpringControllerData data = toSpringControllerData(controller);
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    private static Set<String> snapshotClassNameSet(java.util.Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        saveClassNameSet(out, new ArrayList<>(values));
        return out;
    }

    private static void restoreJarData(List<ProjectRuntimeSnapshot.JarData> jars) {
        NEXT_JAR_ID.set(1);
        JAR_BY_PATH.clear();
        if (jars == null || jars.isEmpty()) {
            return;
        }
        for (ProjectRuntimeSnapshot.JarData row : jars) {
            if (row == null) {
                continue;
            }
            JarEntity entity = new JarEntity();
            entity.setJid(row.jid());
            entity.setJarName(row.jarName());
            entity.setJarAbsPath(row.jarAbsPath());
            String key = safe(row.jarAbsPath());
            if (key.isBlank()) {
                key = row.jid() + ":" + safe(row.jarName());
            }
            JAR_BY_PATH.put(key, entity);
            NEXT_JAR_ID.set(Math.max(NEXT_JAR_ID.get(), row.jid() + 1));
        }
    }

    private static List<ClassFileEntity> restoreClassFiles(List<ProjectRuntimeSnapshot.ClassFileData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ClassFileEntity> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.ClassFileData row : rows) {
            if (row == null) {
                continue;
            }
            ClassFileEntity entity = new ClassFileEntity();
            entity.setCfId(row.cfId());
            entity.setClassName(row.className());
            entity.setPathStr(row.pathStr());
            entity.setJarName(row.jarName());
            entity.setJarId(row.jarId());
            out.add(entity);
        }
        return out;
    }

    private static List<ClassReference> restoreClassReferences(List<ProjectRuntimeSnapshot.ClassReferenceData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ClassReference> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.ClassReferenceData row : rows) {
            ClassReference ref = toClassReference(row);
            if (ref != null) {
                out.add(ref);
            }
        }
        return out;
    }

    private static List<MethodReference> restoreMethodReferences(List<ProjectRuntimeSnapshot.MethodReferenceData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<MethodReference> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.MethodReferenceData row : rows) {
            MethodReference ref = toMethodReference(row);
            if (ref != null) {
                out.add(ref);
            }
        }
        return out;
    }

    private static void restoreStringMap(Map<String, List<String>> target,
                                         Map<String, List<String>> snapshot) {
        target.clear();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key == null || values == null || values.isEmpty()) {
                continue;
            }
            List<String> normalized = new ArrayList<>();
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                normalized.add(value);
            }
            if (!normalized.isEmpty()) {
                target.put(key, Collections.unmodifiableList(normalized));
            }
        }
    }

    private static List<ResourceEntity> restoreResources(List<ProjectRuntimeSnapshot.ResourceData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ResourceEntity> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.ResourceData row : rows) {
            if (row == null) {
                continue;
            }
            ResourceEntity entity = new ResourceEntity();
            entity.setRid(row.rid());
            entity.setResourcePath(row.resourcePath());
            entity.setPathStr(row.pathStr());
            entity.setJarName(row.jarName());
            entity.setJarId(row.jarId());
            entity.setFileSize(row.fileSize());
            entity.setIsText(row.isText());
            out.add(entity);
        }
        return out;
    }

    private static List<CallSiteEntity> restoreCallSites(List<ProjectRuntimeSnapshot.CallSiteData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<CallSiteEntity> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.CallSiteData row : rows) {
            if (row == null) {
                continue;
            }
            CallSiteEntity entity = new CallSiteEntity();
            entity.setCallerClassName(row.callerClassName());
            entity.setCallerMethodName(row.callerMethodName());
            entity.setCallerMethodDesc(row.callerMethodDesc());
            entity.setCalleeOwner(row.calleeOwner());
            entity.setCalleeMethodName(row.calleeMethodName());
            entity.setCalleeMethodDesc(row.calleeMethodDesc());
            entity.setOpCode(row.opCode());
            entity.setLineNumber(row.lineNumber());
            entity.setCallIndex(row.callIndex());
            entity.setReceiverType(row.receiverType());
            entity.setJarId(row.jarId());
            entity.setCallSiteKey(row.callSiteKey());
            out.add(entity);
        }
        return out;
    }

    private static List<LocalVarEntity> restoreLocalVars(List<ProjectRuntimeSnapshot.LocalVarData> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<LocalVarEntity> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.LocalVarData row : rows) {
            if (row == null) {
                continue;
            }
            LocalVarEntity entity = new LocalVarEntity();
            entity.setClassName(row.className());
            entity.setMethodName(row.methodName());
            entity.setMethodDesc(row.methodDesc());
            entity.setVarIndex(row.varIndex());
            entity.setVarName(row.varName());
            entity.setVarDesc(row.varDesc());
            entity.setVarSignature(row.varSignature());
            entity.setStartLine(row.startLine());
            entity.setEndLine(row.endLine());
            entity.setJarId(row.jarId());
            out.add(entity);
        }
        return out;
    }

    private static ArrayList<SpringController> restoreSpringControllers(List<ProjectRuntimeSnapshot.SpringControllerData> rows) {
        ArrayList<SpringController> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (ProjectRuntimeSnapshot.SpringControllerData row : rows) {
            if (row == null || row.classHandle() == null) {
                continue;
            }
            SpringController controller = new SpringController();
            controller.setRest(row.rest());
            controller.setBasePath(row.basePath());
            controller.setClassName(toClassHandle(row.classHandle()));
            controller.setClassReference(findClassReference(row.classHandle()));
            for (ProjectRuntimeSnapshot.SpringMappingData mappingData : row.mappings()) {
                SpringMapping mapping = toSpringMapping(mappingData, controller);
                if (mapping != null) {
                    controller.addMapping(mapping);
                }
            }
            out.add(controller);
        }
        return out;
    }

    private static ProjectRuntimeSnapshot.ProjectModelData toProjectModelData(ProjectModel model) {
        if (model == null) {
            return null;
        }
        List<ProjectRuntimeSnapshot.ProjectRootData> roots = new ArrayList<>();
        if (model.roots() != null) {
            for (ProjectRoot root : model.roots()) {
                if (root == null || root.path() == null) {
                    continue;
                }
                roots.add(new ProjectRuntimeSnapshot.ProjectRootData(
                        root.kind() == null ? "" : root.kind().name(),
                        root.origin() == null ? "" : root.origin().name(),
                        root.path().toString(),
                        root.presentableName(),
                        root.archive(),
                        root.test(),
                        root.priority()
                ));
            }
        }
        List<String> archives = new ArrayList<>();
        if (model.analyzedArchives() != null) {
            for (Path path : model.analyzedArchives()) {
                if (path != null) {
                    archives.add(path.toString());
                }
            }
        }
        return new ProjectRuntimeSnapshot.ProjectModelData(
                model.buildMode() == null ? "" : model.buildMode().name(),
                model.primaryInputPath() == null ? "" : model.primaryInputPath().toString(),
                model.runtimePath() == null ? "" : model.runtimePath().toString(),
                roots,
                archives,
                model.resolveInnerJars()
        );
    }

    private static ProjectModel restoreProjectModel(ProjectRuntimeSnapshot.ProjectModelData data) {
        if (data == null) {
            return null;
        }
        List<ProjectRoot> roots = new ArrayList<>();
        for (ProjectRuntimeSnapshot.ProjectRootData root : data.roots()) {
            if (root == null || root.path() == null || root.path().isBlank()) {
                continue;
            }
            try {
                roots.add(new ProjectRoot(
                        parseEnum(ProjectRootKind.class, root.kind(), ProjectRootKind.CONTENT_ROOT),
                        parseEnum(ProjectOrigin.class, root.origin(), ProjectOrigin.APP),
                        Path.of(root.path()),
                        root.presentableName(),
                        root.archive(),
                        root.test(),
                        root.priority()
                ));
            } catch (Exception ex) {
                logger.debug("restore project root fail: {}", ex.toString());
            }
        }
        List<Path> archives = new ArrayList<>();
        for (String archive : data.analyzedArchives()) {
            if (archive == null || archive.isBlank()) {
                continue;
            }
            try {
                archives.add(Path.of(archive));
            } catch (Exception ex) {
                logger.debug("restore analyzed archive fail: {}", ex.toString());
            }
        }
        return new ProjectModel(
                parseEnum(ProjectBuildMode.class, data.buildMode(), ProjectBuildMode.ARTIFACT),
                toPath(data.primaryInputPath()),
                toPath(data.runtimePath()),
                roots,
                archives,
                data.resolveInnerJars()
        );
    }

    private static ProjectRuntimeSnapshot.ClassReferenceData toClassReferenceData(ClassReference ref) {
        if (ref == null || ref.getName() == null || ref.getName().isBlank()) {
            return null;
        }
        List<ProjectRuntimeSnapshot.ClassMemberData> members = new ArrayList<>();
        if (ref.getMembers() != null) {
            for (ClassReference.Member member : ref.getMembers()) {
                if (member == null) {
                    continue;
                }
                members.add(new ProjectRuntimeSnapshot.ClassMemberData(
                        member.getName(),
                        member.getModifiers(),
                        member.getValue(),
                        member.getDesc(),
                        member.getSignature(),
                        toClassHandleData(member.getType())
                ));
            }
        }
        return new ProjectRuntimeSnapshot.ClassReferenceData(
                ref.getVersion(),
                ref.getAccess(),
                ref.getName(),
                ref.getSuperClass(),
                ref.getInterfaces() == null ? List.of() : List.copyOf(ref.getInterfaces()),
                ref.isInterface(),
                members,
                toAnnoData(ref.getAnnotations()),
                ref.getJarName(),
                ref.getJarId()
        );
    }

    private static ClassReference toClassReference(ProjectRuntimeSnapshot.ClassReferenceData data) {
        if (data == null || data.name() == null || data.name().isBlank()) {
            return null;
        }
        List<ClassReference.Member> members = new ArrayList<>();
        for (ProjectRuntimeSnapshot.ClassMemberData member : data.members()) {
            if (member == null) {
                continue;
            }
            members.add(new ClassReference.Member(
                    member.name(),
                    member.modifiers(),
                    member.value(),
                    member.desc(),
                    member.signature(),
                    toClassHandle(member.type())
            ));
        }
        return new ClassReference(
                data.version(),
                data.access(),
                data.name(),
                data.superClass(),
                data.interfaces() == null ? List.of() : new ArrayList<>(data.interfaces()),
                data.isInterface(),
                members,
                toAnnoReferences(data.annotations()),
                data.jarName(),
                data.jarId()
        );
    }

    private static ProjectRuntimeSnapshot.MethodReferenceData toMethodReferenceData(MethodReference ref) {
        if (ref == null || ref.getClassReference() == null) {
            return null;
        }
        return new ProjectRuntimeSnapshot.MethodReferenceData(
                toClassHandleData(ref.getClassReference()),
                toAnnoData(ref.getAnnotations()),
                ref.getName(),
                ref.getDesc(),
                ref.getAccess(),
                ref.isStatic(),
                ref.getLineNumber(),
                ref.getJarName(),
                ref.getJarId(),
                ref.getSemanticFlags()
        );
    }

    private static MethodReference toMethodReference(ProjectRuntimeSnapshot.MethodReferenceData data) {
        if (data == null || data.classReference() == null || data.name() == null || data.desc() == null) {
            return null;
        }
        return new MethodReference(
                toClassHandle(data.classReference()),
                data.name(),
                data.desc(),
                data.isStatic(),
                toAnnoReferences(data.annotations()),
                data.access(),
                data.lineNumber(),
                data.jarName(),
                data.jarId(),
                data.semanticFlags()
        );
    }

    private static List<ProjectRuntimeSnapshot.AnnoData> toAnnoData(Set<AnnoReference> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.AnnoData> out = new ArrayList<>(annotations.size());
        for (AnnoReference annotation : annotations) {
            if (annotation == null) {
                continue;
            }
            out.add(new ProjectRuntimeSnapshot.AnnoData(
                    annotation.getAnnoName(),
                    annotation.getVisible(),
                    annotation.getParameter()
            ));
        }
        return out;
    }

    private static Set<AnnoReference> toAnnoReferences(List<ProjectRuntimeSnapshot.AnnoData> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return Set.of();
        }
        Set<AnnoReference> out = new LinkedHashSet<>();
        for (ProjectRuntimeSnapshot.AnnoData annotation : annotations) {
            if (annotation == null || annotation.annoName() == null || annotation.annoName().isBlank()) {
                continue;
            }
            AnnoReference ref = new AnnoReference();
            ref.setAnnoName(annotation.annoName());
            ref.setVisible(annotation.visible());
            ref.setParameter(annotation.parameter());
            out.add(ref);
        }
        return out;
    }

    private static ProjectRuntimeSnapshot.ClassHandleData toClassHandleData(ClassReference.Handle handle) {
        if (handle == null || handle.getName() == null || handle.getName().isBlank()) {
            return null;
        }
        return new ProjectRuntimeSnapshot.ClassHandleData(handle.getName(), handle.getJarId());
    }

    private static ClassReference.Handle toClassHandle(ProjectRuntimeSnapshot.ClassHandleData data) {
        if (data == null || data.name() == null || data.name().isBlank()) {
            return null;
        }
        return new ClassReference.Handle(data.name(), data.jarId());
    }

    private static ProjectRuntimeSnapshot.SpringControllerData toSpringControllerData(SpringController controller) {
        if (controller == null || controller.getClassName() == null) {
            return null;
        }
        List<ProjectRuntimeSnapshot.SpringMappingData> mappings = new ArrayList<>();
        for (SpringMapping mapping : controller.getMappings()) {
            ProjectRuntimeSnapshot.SpringMappingData data = toSpringMappingData(mapping);
            if (data != null) {
                mappings.add(data);
            }
        }
        return new ProjectRuntimeSnapshot.SpringControllerData(
                controller.isRest(),
                controller.getBasePath(),
                toClassHandleData(controller.getClassName()),
                mappings
        );
    }

    private static ProjectRuntimeSnapshot.SpringMappingData toSpringMappingData(SpringMapping mapping) {
        if (mapping == null) {
            return null;
        }
        MethodReference method = mapping.getMethodReference();
        ClassReference.Handle owner = method == null ? null : method.getClassReference();
        if (owner == null && mapping.getMethodName() != null) {
            owner = mapping.getMethodName().getClassReference();
        }
        String methodName = method == null ? null : method.getName();
        String methodDesc = method == null ? null : method.getDesc();
        if ((methodName == null || methodName.isBlank()) && mapping.getMethodName() != null) {
            methodName = mapping.getMethodName().getName();
            methodDesc = mapping.getMethodName().getDesc();
        }
        if (owner == null || methodName == null || methodName.isBlank() || methodDesc == null || methodDesc.isBlank()) {
            return null;
        }
        List<ProjectRuntimeSnapshot.SpringParamData> params = new ArrayList<>();
        if (mapping.getParamMap() != null) {
            for (SpringParam param : mapping.getParamMap()) {
                if (param == null) {
                    continue;
                }
                params.add(new ProjectRuntimeSnapshot.SpringParamData(
                        param.getParamIndex(),
                        param.getParamName(),
                        param.getParamType(),
                        param.getReqName()
                ));
            }
        }
        return new ProjectRuntimeSnapshot.SpringMappingData(
                mapping.isRest(),
                toClassHandleData(owner),
                methodName,
                methodDesc,
                mapping.getPath(),
                null,
                mapping.getPathRestful(),
                params
        );
    }

    private static SpringMapping toSpringMapping(ProjectRuntimeSnapshot.SpringMappingData data,
                                                 SpringController controller) {
        if (data == null || data.methodOwner() == null) {
            return null;
        }
        MethodReference method = findMethodReference(data.methodOwner(), data.methodName(), data.methodDesc());
        if (method == null) {
            return null;
        }
        SpringMapping mapping = new SpringMapping();
        mapping.setController(controller);
        mapping.setRest(data.rest());
        mapping.setMethodReference(method);
        mapping.setMethodName(method.getHandle());
        mapping.setPath(data.path());
        mapping.setRestfulType(data.restfulType());
        mapping.setPathRestful(data.pathRestful());
        List<SpringParam> params = new ArrayList<>();
        for (ProjectRuntimeSnapshot.SpringParamData paramData : data.params()) {
            if (paramData == null) {
                continue;
            }
            SpringParam param = new SpringParam();
            param.setParamIndex(paramData.paramIndex());
            param.setParamName(paramData.paramName());
            param.setParamType(paramData.paramType());
            param.setReqName(paramData.reqName());
            params.add(param);
        }
        mapping.setParamMap(params);
        return mapping;
    }

    private static SpringController copySpringController(SpringController src) {
        ProjectRuntimeSnapshot.SpringControllerData data = toSpringControllerData(src);
        if (data == null) {
            return null;
        }
        ArrayList<SpringController> restored = restoreSpringControllers(List.of(data));
        return restored.isEmpty() ? null : restored.get(0);
    }

    private static ClassReference findClassReference(ProjectRuntimeSnapshot.ClassHandleData handle) {
        if (handle == null || handle.name() == null || handle.name().isBlank()) {
            return null;
        }
        List<ClassReference> rows = getClassReferencesByName(handle.name());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        int wantedJarId = handle.jarId() == null ? -1 : handle.jarId();
        for (ClassReference row : rows) {
            if (row != null && Objects.equals(row.getJarId(), wantedJarId)) {
                return row;
            }
        }
        return rows.get(0);
    }

    private static MethodReference findMethodReference(ProjectRuntimeSnapshot.ClassHandleData owner,
                                                       String methodName,
                                                       String methodDesc) {
        if (owner == null || owner.name() == null || owner.name().isBlank()) {
            return null;
        }
        List<MethodReference> rows = getMethodReferencesByClass(owner.name());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        int wantedJarId = owner.jarId() == null ? -1 : owner.jarId();
        for (MethodReference row : rows) {
            if (row == null) {
                continue;
            }
            if (!safe(methodName).equals(safe(row.getName())) || !safe(methodDesc).equals(safe(row.getDesc()))) {
                continue;
            }
            Integer jarId = row.getJarId();
            if (jarId != null && jarId == wantedJarId) {
                return row;
            }
        }
        for (MethodReference row : rows) {
            if (row == null) {
                continue;
            }
            if (safe(methodName).equals(safe(row.getName())) && safe(methodDesc).equals(safe(row.getDesc()))) {
                return row;
            }
        }
        return null;
    }

    private static Path toPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value);
        } catch (Exception ex) {
            logger.debug("restore path fail: {}", ex.toString());
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (type == null) {
            return fallback;
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static ClassFileEntity copyClassFileEntity(ClassFileEntity src) {
        if (src == null) {
            return null;
        }
        ClassFileEntity out = new ClassFileEntity();
        out.setCfId(src.getCfId());
        out.setClassName(src.getClassName());
        String path = classFilePath(src);
        if (path != null && !path.isBlank()) {
            out.setPathStr(path);
        }
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
        String ap = safe(classFilePath(a));
        String bp = safe(classFilePath(b));
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

    private static List<CallSiteEntity> readPersistedCallSitesByCaller(String projectKey,
                                                                       String className,
                                                                       String methodName,
                                                                       String methodDesc) {
        List<ProjectRuntimeSnapshot.CallSiteData> rows = ProjectMetadataSnapshotStore.getInstance()
                .findCallSitesByCaller(
                        ActiveProjectContext.resolveRequestedOrActive(projectKey),
                        className,
                        methodName,
                        methodDesc
                );
        return rows.isEmpty() ? Collections.emptyList() : restoreCallSites(rows);
    }

    private static List<LocalVarEntity> readPersistedLocalVarsByMethod(String projectKey,
                                                                       String className,
                                                                       String methodName,
                                                                       String methodDesc) {
        List<ProjectRuntimeSnapshot.LocalVarData> rows = ProjectMetadataSnapshotStore.getInstance()
                .findLocalVarsByMethod(
                        ActiveProjectContext.resolveRequestedOrActive(projectKey),
                        className,
                        methodName,
                        methodDesc
                );
        return rows.isEmpty() ? Collections.emptyList() : restoreLocalVars(rows);
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
        return safe(classFilePath(a)).compareTo(safe(classFilePath(b)));
    };

    private static String classFilePath(ClassFileEntity row) {
        if (row == null) {
            return "";
        }
        String path = row.resolvePathStr();
        if (path != null && !path.isBlank()) {
            return path;
        }
        Path resolved = row.resolvePath();
        return resolved == null ? "" : resolved.toString();
    }

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
