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

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LineMappingEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodCallEntity;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jEngineConfig;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jIdSequenceRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jReadRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jWriteRepository;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreEngine {
    private static final Logger logger = LogManager.getLogger();

    private final Neo4jStore store;
    private final Neo4jReadRepository readRepository;
    private final Neo4jWriteRepository writeRepository;
    private final Neo4jIdSequenceRepository idSequenceRepository;

    private volatile CallGraphCache callGraphCache;

    public boolean isEnabled() {
        Path homePath = DatabaseManager.activeProjectHome();
        if (homePath == null) {
            homePath = Neo4jStore.getInstance().activeHomeDir();
        }
        if (homePath == null) {
            homePath = Neo4jEngineConfig.defaults().homeDir();
        }
        if (!Files.exists(homePath) || !Files.isDirectory(homePath)) {
            return false;
        }
        Path tempDir = Paths.get("jar-analyzer-temp");
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            return false;
        }
        try (Stream<Path> stream = Files.walk(tempDir)) {
            return stream.anyMatch(path ->
                    Files.isRegularFile(path) &&
                            !"console.dll".equals(path.getFileName().toString()));
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    public CoreEngine(ConfigFile configFile) {
        String dbPathValue = configFile == null ? null : configFile.getDbPath();
        if (StringUtil.isNull(dbPathValue)) {
            throw new RuntimeException("start engine error");
        }
        this.store = Neo4jStore.getInstance();
        this.store.start();
        this.readRepository = new Neo4jReadRepository(store);
        this.writeRepository = new Neo4jWriteRepository(store);
        this.idSequenceRepository = new Neo4jIdSequenceRepository(store);
        logger.info("init core engine finish");
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
        List<MethodCallResult> edges = listMethodCallResults(
                "MATCH (mc:MethodCall) " +
                        "RETURN mc.callerClassName AS callerClassName, mc.callerMethodName AS callerMethodName, mc.callerMethodDesc AS callerMethodDesc, " +
                        "coalesce(mc.callerJarId,-1) AS callerJarId, coalesce(mc.callerJarName,'') AS callerJarName, " +
                        "mc.calleeClassName AS calleeClassName, mc.calleeMethodName AS calleeMethodName, mc.calleeMethodDesc AS calleeMethodDesc, " +
                        "coalesce(mc.calleeJarId,-1) AS calleeJarId, coalesce(mc.calleeJarName,'') AS calleeJarName, " +
                        "coalesce(mc.opCode,-1) AS opCode, coalesce(mc.edgeType,'') AS edgeType, " +
                        "coalesce(mc.edgeConfidence,'') AS edgeConfidence, coalesce(mc.edgeEvidence,'') AS edgeEvidence, coalesce(mc.callSiteKey,'') AS callSiteKey",
                Collections.emptyMap(),
                120_000L
        );
        CallGraphCache cache = CallGraphCache.build(edges);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        logger.info("call graph cache loaded: edges={}, methods={}, elapsedMs={}",
                cache.getEdgeCount(), cache.getMethodCount(), elapsedMs);
        return cache;
    }

    public ArrayList<MethodResult> getMethodsByClass(String className) {
        ArrayList<MethodResult> results = listMethodResults(
                "MATCH (m:Method {className:$className}) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, " +
                        "coalesce(m.lineNumber,-1) AS lineNumber, coalesce(m.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY m.jarId ASC, m.className ASC, m.methodName ASC, m.methodDesc ASC",
                Map.of("className", safe(className)),
                60_000L
        );
        results.sort(Comparator.comparing(MethodResult::getMethodName));
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
        ArrayList<ClassResult> results = listClassResults(
                "MATCH (c:Class {className:$className}) " +
                        "RETURN c.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(c.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY c.jarId ASC, c.jarName ASC",
                Map.of("className", safe(className)),
                60_000L
        );
        results.sort(Comparator.comparingInt(ClassResult::getJarId)
                .thenComparing(ClassResult::getJarName, Comparator.nullsLast(String::compareTo)));
        return results;
    }

    public ArrayList<String> includeClassByClassName(String className) {
        return includeClassByClassName(className, false);
    }

    public ArrayList<String> includeClassByClassName(String className, boolean includeJdk) {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (c:Class) WHERE toLower(c.className) CONTAINS toLower($className) " +
                        "RETURN DISTINCT c.className AS className ORDER BY c.className ASC",
                Map.of("className", safe(className)),
                60_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String value = safe(row.get("className"));
            if (value.isBlank()) {
                continue;
            }
            results.add(value);
        }
        if (results.isEmpty()) {
            return results;
        }
        if (includeJdk) {
            ArrayList<String> out = new ArrayList<>();
            for (String item : results) {
                if (CommonFilterUtil.isModuleInfoClassName(item)) {
                    continue;
                }
                out.add(item);
            }
            return out;
        }
        ArrayList<String> filtered = new ArrayList<>();
        for (String item : results) {
            if (CommonFilterUtil.isFilteredClass(item)) {
                continue;
            }
            filtered.add(item);
        }
        return filtered;
    }

    public String getAbsPath(String className) {
        String lookup = normalizeClassFileName(className);
        Map<String, Object> row = readRepository.one(
                "MATCH (c:ClassFile {className:$className}) RETURN c.pathStr AS pathStr ORDER BY c.jarId ASC LIMIT 1",
                Map.of("className", lookup),
                30_000L
        );
        return safeOrNull(row.get("pathStr"));
    }

    public String getAbsPath(String className, Integer jarId) {
        String lookup = normalizeClassFileName(className);
        if (jarId != null && jarId >= 0) {
            Map<String, Object> row = readRepository.one(
                    "MATCH (c:ClassFile {className:$className, jarId:$jarId}) RETURN c.pathStr AS pathStr LIMIT 1",
                    Map.of("className", lookup, "jarId", jarId),
                    30_000L
            );
            String value = safeOrNull(row.get("pathStr"));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return getAbsPath(className);
    }

    public ArrayList<MethodResult> getCallers(String calleeClass, String calleeMethod, String calleeDesc) {
        return getCallers(calleeClass, calleeMethod, calleeDesc, null);
    }

    public ArrayList<MethodResult> getCallers(String calleeClass,
                                              String calleeMethod,
                                              String calleeDesc,
                                              Integer calleeJarId) {
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallers(calleeClass, calleeMethod, calleeDesc, normalizeJarId(calleeJarId));
        }
        return listMethodResults(
                "MATCH (mc:MethodCall) " +
                        "WHERE mc.calleeClassName = $calleeClass AND mc.calleeMethodName = $calleeMethod " +
                        "AND ($calleeDesc = '' OR mc.calleeMethodDesc = $calleeDesc) " +
                        "AND ($calleeJarId < 0 OR coalesce(mc.calleeJarId,-1) = $calleeJarId) " +
                        "RETURN DISTINCT mc.callerClassName AS className, mc.callerMethodName AS methodName, mc.callerMethodDesc AS methodDesc, " +
                        "coalesce(mc.callerJarName,'') AS jarName, coalesce(mc.callerJarId,-1) AS jarId, 0 AS isStaticInt, 0 AS accessInt, -1 AS lineNumber " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of(
                        "calleeClass", safe(calleeClass),
                        "calleeMethod", safe(calleeMethod),
                        "calleeDesc", safe(calleeDesc),
                        "calleeJarId", normalizeJarId(calleeJarId)
                ),
                60_000L
        );
    }

    public ArrayList<MethodResult> getCallersLike(String calleeClass, String calleeMethod, String calleeDesc) {
        return listMethodResults(
                "MATCH (mc:MethodCall) " +
                        "WHERE toLower(mc.calleeClassName) CONTAINS toLower($calleeClass) " +
                        "AND toLower(mc.calleeMethodName) CONTAINS toLower($calleeMethod) " +
                        "AND ($calleeDesc = '' OR mc.calleeMethodDesc = $calleeDesc) " +
                        "RETURN DISTINCT mc.callerClassName AS className, mc.callerMethodName AS methodName, mc.callerMethodDesc AS methodDesc, " +
                        "coalesce(mc.callerJarName,'') AS jarName, coalesce(mc.callerJarId,-1) AS jarId, 0 AS isStaticInt, 0 AS accessInt, -1 AS lineNumber " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("calleeClass", safe(calleeClass), "calleeMethod", safe(calleeMethod), "calleeDesc", safe(calleeDesc)),
                60_000L
        );
    }

    public ArrayList<MethodResult> getCallee(String callerClass, String callerMethod, String callerDesc) {
        return getCallee(callerClass, callerMethod, callerDesc, null);
    }

    public ArrayList<MethodResult> getCallee(String callerClass,
                                             String callerMethod,
                                             String callerDesc,
                                             Integer callerJarId) {
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallees(callerClass, callerMethod, callerDesc, normalizeJarId(callerJarId));
        }
        return listMethodResults(
                "MATCH (mc:MethodCall) " +
                        "WHERE mc.callerClassName = $callerClass AND mc.callerMethodName = $callerMethod " +
                        "AND ($callerDesc = '' OR mc.callerMethodDesc = $callerDesc) " +
                        "AND ($callerJarId < 0 OR coalesce(mc.callerJarId,-1) = $callerJarId) " +
                        "RETURN DISTINCT mc.calleeClassName AS className, mc.calleeMethodName AS methodName, mc.calleeMethodDesc AS methodDesc, " +
                        "coalesce(mc.calleeJarName,'') AS jarName, coalesce(mc.calleeJarId,-1) AS jarId, 0 AS isStaticInt, 0 AS accessInt, -1 AS lineNumber " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of(
                        "callerClass", safe(callerClass),
                        "callerMethod", safe(callerMethod),
                        "callerDesc", safe(callerDesc),
                        "callerJarId", normalizeJarId(callerJarId)
                ),
                60_000L
        );
    }

    public ArrayList<MethodCallResult> getCallEdgesByCallee(String calleeClass,
                                                            String calleeMethod,
                                                            String calleeDesc,
                                                            Integer offset,
                                                            Integer limit) {
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit <= 0 ? 200 : limit;
        return listMethodCallResults(
                "MATCH (mc:MethodCall) " +
                        "WHERE mc.calleeClassName = $calleeClass AND mc.calleeMethodName = $calleeMethod " +
                        "AND ($calleeDesc = '' OR mc.calleeMethodDesc = $calleeDesc) " +
                        "RETURN mc.callerClassName AS callerClassName, mc.callerMethodName AS callerMethodName, mc.callerMethodDesc AS callerMethodDesc, " +
                        "coalesce(mc.callerJarId,-1) AS callerJarId, coalesce(mc.callerJarName,'') AS callerJarName, " +
                        "mc.calleeClassName AS calleeClassName, mc.calleeMethodName AS calleeMethodName, mc.calleeMethodDesc AS calleeMethodDesc, " +
                        "coalesce(mc.calleeJarId,-1) AS calleeJarId, coalesce(mc.calleeJarName,'') AS calleeJarName, " +
                        "coalesce(mc.opCode,-1) AS opCode, coalesce(mc.edgeType,'') AS edgeType, coalesce(mc.edgeConfidence,'') AS edgeConfidence, " +
                        "coalesce(mc.edgeEvidence,'') AS edgeEvidence, coalesce(mc.callSiteKey,'') AS callSiteKey " +
                        "ORDER BY callerJarId ASC, callerClassName ASC, callerMethodName ASC, callerMethodDesc ASC SKIP $offset LIMIT $limit",
                Map.of("calleeClass", safe(calleeClass), "calleeMethod", safe(calleeMethod), "calleeDesc", safe(calleeDesc),
                        "offset", off, "limit", lim),
                60_000L
        );
    }

    public ArrayList<MethodCallResult> getCallEdgesByCaller(String callerClass,
                                                            String callerMethod,
                                                            String callerDesc,
                                                            Integer offset,
                                                            Integer limit) {
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit <= 0 ? 200 : limit;
        return listMethodCallResults(
                "MATCH (mc:MethodCall) " +
                        "WHERE mc.callerClassName = $callerClass AND mc.callerMethodName = $callerMethod " +
                        "AND ($callerDesc = '' OR mc.callerMethodDesc = $callerDesc) " +
                        "RETURN mc.callerClassName AS callerClassName, mc.callerMethodName AS callerMethodName, mc.callerMethodDesc AS callerMethodDesc, " +
                        "coalesce(mc.callerJarId,-1) AS callerJarId, coalesce(mc.callerJarName,'') AS callerJarName, " +
                        "mc.calleeClassName AS calleeClassName, mc.calleeMethodName AS calleeMethodName, mc.calleeMethodDesc AS calleeMethodDesc, " +
                        "coalesce(mc.calleeJarId,-1) AS calleeJarId, coalesce(mc.calleeJarName,'') AS calleeJarName, " +
                        "coalesce(mc.opCode,-1) AS opCode, coalesce(mc.edgeType,'') AS edgeType, coalesce(mc.edgeConfidence,'') AS edgeConfidence, " +
                        "coalesce(mc.edgeEvidence,'') AS edgeEvidence, coalesce(mc.callSiteKey,'') AS callSiteKey " +
                        "ORDER BY calleeJarId ASC, calleeClassName ASC, calleeMethodName ASC, calleeMethodDesc ASC SKIP $offset LIMIT $limit",
                Map.of("callerClass", safe(callerClass), "callerMethod", safe(callerMethod), "callerDesc", safe(callerDesc),
                        "offset", off, "limit", lim),
                60_000L
        );
    }

    public MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getEdgeMeta(caller, callee);
        }
        Map<String, Object> row = readRepository.one(
                "MATCH (mc:MethodCall) " +
                        "WHERE mc.callerClassName = $callerClass AND mc.callerMethodName = $callerMethod " +
                        "AND mc.callerMethodDesc = $callerDesc AND coalesce(mc.callerJarId,-1) = $callerJarId " +
                        "AND mc.calleeClassName = $calleeClass AND mc.calleeMethodName = $calleeMethod " +
                        "AND mc.calleeMethodDesc = $calleeDesc AND coalesce(mc.calleeJarId,-1) = $calleeJarId " +
                        "RETURN mc.edgeType AS edgeType, mc.edgeConfidence AS edgeConfidence, mc.edgeEvidence AS edgeEvidence LIMIT 1",
                Map.of(
                        "callerClass", safe(caller.getClassReference().getName()),
                        "callerMethod", safe(caller.getName()),
                        "callerDesc", safe(caller.getDesc()),
                        "callerJarId", normalizeJarId(caller.getJarId()),
                        "calleeClass", safe(callee.getClassReference().getName()),
                        "calleeMethod", safe(callee.getName()),
                        "calleeDesc", safe(callee.getDesc()),
                        "calleeJarId", normalizeJarId(callee.getJarId())
                ),
                30_000L
        );
        if (row.isEmpty()) {
            return null;
        }
        return new MethodCallMeta(safe(row.get("edgeType")), safe(row.get("edgeConfidence")), safe(row.get("edgeEvidence")));
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
            Map<String, Object> row = readRepository.one(
                    "MATCH (mc:MethodCall) " +
                            "WHERE mc.callerClassName = $callerClass AND mc.callerMethodName = $callerMethod " +
                            "AND mc.callerMethodDesc = $callerDesc " +
                            "AND ($callerJarId < 0 OR coalesce(mc.callerJarId,-1) = $callerJarId) " +
                            "AND mc.calleeClassName = $calleeClass AND mc.calleeMethodName = $calleeMethod " +
                            "AND mc.calleeMethodDesc = $calleeDesc " +
                            "AND ($calleeJarId < 0 OR coalesce(mc.calleeJarId,-1) = $calleeJarId) " +
                            "RETURN mc.edgeType AS edgeType, mc.edgeConfidence AS edgeConfidence, mc.edgeEvidence AS edgeEvidence LIMIT 1",
                    Map.of(
                            "callerClass", safe(key.getCallerClass()),
                            "callerMethod", safe(key.getCallerMethod()),
                            "callerDesc", safe(key.getCallerDesc()),
                            "callerJarId", key.getCallerJarId() == null ? -1 : key.getCallerJarId(),
                            "calleeClass", safe(key.getCalleeClass()),
                            "calleeMethod", safe(key.getCalleeMethod()),
                            "calleeDesc", safe(key.getCalleeDesc()),
                            "calleeJarId", key.getCalleeJarId() == null ? -1 : key.getCalleeJarId()
                    ),
                    30_000L
            );
            if (row.isEmpty()) {
                continue;
            }
            MethodCallMeta meta = new MethodCallMeta(safe(row.get("edgeType")), safe(row.get("edgeConfidence")), safe(row.get("edgeEvidence")));
            out.put(key, meta);
            MethodCallKey looseKey = new MethodCallKey(
                    key.getCallerClass(),
                    key.getCallerMethod(),
                    key.getCallerDesc(),
                    key.getCalleeClass(),
                    key.getCalleeMethod(),
                    key.getCalleeDesc()
            );
            out.putIfAbsent(looseKey, meta);
        }
        return out;
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
    }

    public ArrayList<MethodResult> getMethod(String className, String methodName, String methodDesc) {
        return listMethodResults(
                "MATCH (m:Method) " +
                        "WHERE ($methodName = '' OR m.methodName = $methodName) " +
                        "AND ($methodDesc = '' OR m.methodDesc = $methodDesc) " +
                        "AND ($className = '' OR m.className = $className) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, coalesce(m.lineNumber,-1) AS lineNumber, " +
                        "coalesce(m.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc)),
                60_000L
        );
    }

    public ArrayList<CallSiteEntity> getCallSitesByCaller(String className,
                                                          String methodName,
                                                          String methodDesc) {
        return listCallSites(
                "MATCH (c:CallSite) WHERE c.callerClassName = $className AND c.callerMethodName = $methodName AND c.callerMethodDesc = $methodDesc " +
                        "RETURN c.callerClassName AS callerClassName, c.callerMethodName AS callerMethodName, c.callerMethodDesc AS callerMethodDesc, " +
                        "c.calleeOwner AS calleeOwner, c.calleeMethodName AS calleeMethodName, c.calleeMethodDesc AS calleeMethodDesc, " +
                        "coalesce(c.opCode,-1) AS opCode, coalesce(c.lineNumber,-1) AS lineNumber, coalesce(c.callIndex,-1) AS callIndex, " +
                        "coalesce(c.receiverType,'') AS receiverType, coalesce(c.jarId,-1) AS jarId, coalesce(c.callSiteKey,'') AS callSiteKey " +
                        "ORDER BY callIndex ASC, lineNumber ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc)),
                30_000L
        );
    }

    public ArrayList<CallSiteEntity> getCallSitesByEdge(String callerClassName,
                                                        String callerMethodName,
                                                        String callerMethodDesc,
                                                        String calleeOwner,
                                                        String calleeMethodName,
                                                        String calleeMethodDesc) {
        return listCallSites(
                "MATCH (c:CallSite) " +
                        "WHERE c.callerClassName = $callerClassName AND c.callerMethodName = $callerMethodName AND c.callerMethodDesc = $callerMethodDesc " +
                        "AND c.calleeOwner = $calleeOwner AND c.calleeMethodName = $calleeMethodName AND c.calleeMethodDesc = $calleeMethodDesc " +
                        "RETURN c.callerClassName AS callerClassName, c.callerMethodName AS callerMethodName, c.callerMethodDesc AS callerMethodDesc, " +
                        "c.calleeOwner AS calleeOwner, c.calleeMethodName AS calleeMethodName, c.calleeMethodDesc AS calleeMethodDesc, " +
                        "coalesce(c.opCode,-1) AS opCode, coalesce(c.lineNumber,-1) AS lineNumber, coalesce(c.callIndex,-1) AS callIndex, " +
                        "coalesce(c.receiverType,'') AS receiverType, coalesce(c.jarId,-1) AS jarId, coalesce(c.callSiteKey,'') AS callSiteKey " +
                        "ORDER BY callIndex ASC, lineNumber ASC",
                Map.of(
                        "callerClassName", safe(callerClassName),
                        "callerMethodName", safe(callerMethodName),
                        "callerMethodDesc", safe(callerMethodDesc),
                        "calleeOwner", safe(calleeOwner),
                        "calleeMethodName", safe(calleeMethodName),
                        "calleeMethodDesc", safe(calleeMethodDesc)
                ),
                30_000L
        );
    }

    public ArrayList<LocalVarEntity> getLocalVarsByMethod(String className,
                                                          String methodName,
                                                          String methodDesc) {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (v:LocalVar) " +
                        "WHERE v.className = $className AND v.methodName = $methodName AND v.methodDesc = $methodDesc " +
                        "RETURN v.className AS className, v.methodName AS methodName, v.methodDesc AS methodDesc, " +
                        "coalesce(v.varIndex,-1) AS varIndex, coalesce(v.varName,'') AS varName, coalesce(v.varDesc,'') AS varDesc, " +
                        "coalesce(v.varSignature,'') AS varSignature, coalesce(v.startLine,-1) AS startLine, coalesce(v.endLine,-1) AS endLine, " +
                        "coalesce(v.jarId,-1) AS jarId ORDER BY varIndex ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc)),
                30_000L
        );
        ArrayList<LocalVarEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LocalVarEntity entity = new LocalVarEntity();
            entity.setClassName(safe(row.get("className")));
            entity.setMethodName(safe(row.get("methodName")));
            entity.setMethodDesc(safe(row.get("methodDesc")));
            entity.setVarIndex(asInt(row.get("varIndex"), -1));
            entity.setVarName(safe(row.get("varName")));
            entity.setVarDesc(safe(row.get("varDesc")));
            entity.setVarSignature(safe(row.get("varSignature")));
            entity.setStartLine(asInt(row.get("startLine"), -1));
            entity.setEndLine(asInt(row.get("endLine"), -1));
            entity.setJarId(asInt(row.get("jarId"), -1));
            out.add(entity);
        }
        return out;
    }

    public ArrayList<MethodResult> getMethodLike(String className, String methodName, String methodDesc) {
        return listMethodResults(
                "MATCH (m:Method) " +
                        "WHERE ($methodName = '' OR toLower(m.methodName) CONTAINS toLower($methodName)) " +
                        "AND ($methodDesc = '' OR m.methodDesc = $methodDesc) " +
                        "AND ($className = '' OR toLower(m.className) CONTAINS toLower($className)) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, coalesce(m.lineNumber,-1) AS lineNumber, " +
                        "coalesce(m.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc)),
                60_000L
        );
    }

    public ArrayList<MethodResult> getMethodsByStr(String val) {
        return getMethodsByStr(val, null, null, null, "auto");
    }

    public ArrayList<MethodResult> getMethodsByStrEqual(String val) {
        return listMethodResults(
                "MATCH (s:StringLiteral) WHERE s.value = $value " +
                        "RETURN s.className AS className, s.methodName AS methodName, s.methodDesc AS methodDesc, " +
                        "coalesce(s.access,0) AS accessInt, 0 AS isStaticInt, -1 AS lineNumber, " +
                        "coalesce(s.jarName,'') AS jarName, coalesce(s.jarId,-1) AS jarId " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("value", safe(val)),
                60_000L
        );
    }

    public ArrayList<MethodResult> getMethodsByStr(String val,
                                                   Integer jarId,
                                                   String classLike,
                                                   Integer limit,
                                                   String mode) {
        String m = mode == null ? "auto" : mode.trim().toLowerCase();
        String classFilter = classLike == null ? "" : classLike.trim();
        if (classFilter.endsWith("/")) {
            classFilter = classFilter.substring(0, classFilter.length() - 1);
        }
        int limitVal = limit == null || limit <= 0 ? 1000 : limit;
        int jarFilter = jarId == null ? -1 : jarId;

        if ("equal".equals(m)) {
            return listMethodResults(
                    "MATCH (s:StringLiteral) " +
                            "WHERE s.value = $value AND ($jarId < 0 OR coalesce(s.jarId,-1) = $jarId) " +
                            "AND ($classLike = '' OR toLower(s.className) CONTAINS toLower($classLike)) " +
                            "RETURN s.className AS className, s.methodName AS methodName, s.methodDesc AS methodDesc, " +
                            "coalesce(s.access,0) AS accessInt, 0 AS isStaticInt, -1 AS lineNumber, coalesce(s.jarName,'') AS jarName, coalesce(s.jarId,-1) AS jarId " +
                            "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC LIMIT $limit",
                    Map.of("value", safe(val), "jarId", jarFilter, "classLike", classFilter, "limit", limitVal),
                    60_000L
            );
        }

        boolean tryFts = "fts".equals(m) || ("auto".equals(m) && isSafeFtsQuery(val));
        if (tryFts) {
            try {
                String ftsQuery = buildFtsQuery(val);
                return listMethodResults(
                        "CALL db.index.fulltext.queryNodes('string_literal_fulltext', $query) YIELD node, score " +
                                "WHERE ($jarId < 0 OR coalesce(node.jarId,-1) = $jarId) " +
                                "AND ($classLike = '' OR toLower(node.className) CONTAINS toLower($classLike)) " +
                                "RETURN node.className AS className, node.methodName AS methodName, node.methodDesc AS methodDesc, " +
                                "coalesce(node.access,0) AS accessInt, 0 AS isStaticInt, -1 AS lineNumber, " +
                                "coalesce(node.jarName,'') AS jarName, coalesce(node.jarId,-1) AS jarId " +
                                "ORDER BY score DESC, jarId ASC, className ASC LIMIT $limit",
                        Map.of("query", ftsQuery, "jarId", jarFilter, "classLike", classFilter, "limit", limitVal),
                        60_000L
                );
            } catch (Exception ex) {
                logger.debug("fulltext query fallback: {}", ex.toString());
            }
        }

        boolean prefix = "prefix".equals(m);
        String pattern = safe(val);
        if (!prefix) {
            return listMethodResults(
                    "MATCH (s:StringLiteral) " +
                            "WHERE toLower(s.value) CONTAINS toLower($pattern) " +
                            "AND ($jarId < 0 OR coalesce(s.jarId,-1) = $jarId) " +
                            "AND ($classLike = '' OR toLower(s.className) CONTAINS toLower($classLike)) " +
                            "RETURN s.className AS className, s.methodName AS methodName, s.methodDesc AS methodDesc, " +
                            "coalesce(s.access,0) AS accessInt, 0 AS isStaticInt, -1 AS lineNumber, " +
                            "coalesce(s.jarName,'') AS jarName, coalesce(s.jarId,-1) AS jarId " +
                            "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC LIMIT $limit",
                    Map.of("pattern", pattern, "jarId", jarFilter, "classLike", classFilter, "limit", limitVal),
                    60_000L
            );
        }
        return listMethodResults(
                "MATCH (s:StringLiteral) " +
                        "WHERE toLower(s.value) STARTS WITH toLower($pattern) " +
                        "AND ($jarId < 0 OR coalesce(s.jarId,-1) = $jarId) " +
                        "AND ($classLike = '' OR toLower(s.className) CONTAINS toLower($classLike)) " +
                        "RETURN s.className AS className, s.methodName AS methodName, s.methodDesc AS methodDesc, " +
                        "coalesce(s.access,0) AS accessInt, 0 AS isStaticInt, -1 AS lineNumber, " +
                        "coalesce(s.jarName,'') AS jarName, coalesce(s.jarId,-1) AS jarId " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC LIMIT $limit",
                Map.of("pattern", pattern, "jarId", jarFilter, "classLike", classFilter, "limit", limitVal),
                60_000L
        );
    }

    private boolean isSafeFtsQuery(String val) {
        if (val == null) {
            return false;
        }
        String v = val.trim();
        if (v.isEmpty()) {
            return false;
        }
        return v.matches("[A-Za-z0-9._-]{2,}");
    }

    private String buildFtsQuery(String val) {
        String v = val == null ? "" : val.trim();
        String escaped = v.replace("\"", "\"\"");
        return "value:\"" + escaped + "\"";
    }

    public ArrayList<String> getJarsPath() {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (j:Jar) RETURN j.jarAbsPath AS jarAbsPath ORDER BY j.jid ASC",
                Collections.emptyMap(),
                30_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            results.add(safe(row.get("jarAbsPath")));
        }
        return results;
    }

    public ArrayList<String> getJarNames() {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (j:Jar) RETURN j.jarName AS jarName ORDER BY j.jid ASC",
                Collections.emptyMap(),
                30_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            results.add(safe(row.get("jarName")));
        }
        return results;
    }

    public ArrayList<JarEntity> getJarsMeta() {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (j:Jar) RETURN j.jid AS jid, j.jarName AS jarName, j.jarAbsPath AS jarAbsPath ORDER BY j.jid ASC",
                Collections.emptyMap(),
                30_000L
        );
        ArrayList<JarEntity> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            JarEntity jar = new JarEntity();
            jar.setJid(asInt(row.get("jid"), -1));
            jar.setJarName(safe(row.get("jarName")));
            jar.setJarAbsPath(safe(row.get("jarAbsPath")));
            results.add(jar);
        }
        return results;
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
        return listMethodResults(
                "MATCH (mi:MethodImpl) " +
                        "WHERE ($className = '' OR mi.className = $className) " +
                        "AND ($methodName = '' OR mi.methodName = $methodName) " +
                        "AND ($methodDesc = '' OR mi.methodDesc = $methodDesc) " +
                        "AND ($classJarId < 0 OR coalesce(mi.classJarId,-1) = $classJarId) " +
                        "OPTIONAL MATCH (c:Class {className:mi.implClassName, jarId:mi.implClassJarId}) " +
                        "RETURN mi.implClassName AS className, mi.methodName AS methodName, mi.methodDesc AS methodDesc, " +
                        "coalesce(c.jarName,'') AS jarName, coalesce(mi.implClassJarId,-1) AS jarId, 0 AS isStaticInt, 0 AS accessInt, -1 AS lineNumber " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc),
                        "classJarId", classJarId == null ? -1 : classJarId),
                60_000L
        );
    }

    public ArrayList<MethodResult> getSuperImpls(String className, String methodName, String methodDesc) {
        return getSuperImpls(className, methodName, methodDesc, null);
    }

    public ArrayList<MethodResult> getSuperImpls(String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer classJarId) {
        return listMethodResults(
                "MATCH (mi:MethodImpl) " +
                        "WHERE ($className = '' OR mi.implClassName = $className) " +
                        "AND ($methodName = '' OR mi.methodName = $methodName) " +
                        "AND ($methodDesc = '' OR mi.methodDesc = $methodDesc) " +
                        "AND ($classJarId < 0 OR coalesce(mi.implClassJarId,-1) = $classJarId) " +
                        "OPTIONAL MATCH (c:Class {className:mi.className, jarId:mi.classJarId}) " +
                        "RETURN mi.className AS className, mi.methodName AS methodName, mi.methodDesc AS methodDesc, " +
                        "coalesce(c.jarName,'') AS jarName, coalesce(mi.classJarId,-1) AS jarId, 0 AS isStaticInt, 0 AS accessInt, -1 AS lineNumber " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC, methodDesc ASC",
                Map.of("className", safe(className), "methodName", safe(methodName), "methodDesc", safe(methodDesc),
                        "classJarId", classJarId == null ? -1 : classJarId),
                60_000L
        );
    }

    public String getJarByClass(String className) {
        Map<String, Object> row = readRepository.one(
                "MATCH (c:Class {className:$className}) RETURN coalesce(c.jarName,'') AS jarName ORDER BY c.jarId ASC LIMIT 1",
                Map.of("className", safe(className)),
                30_000L
        );
        return safeOrNull(row.get("jarName"));
    }

    public Integer getJarIdByClass(String className) {
        if (StringUtil.isNull(className)) {
            return null;
        }
        Map<String, Object> row = readRepository.one(
                "MATCH (c:Class {className:$className}) RETURN coalesce(c.jarId,-1) AS jarId ORDER BY c.jarId ASC LIMIT 1",
                Map.of("className", safe(className)),
                30_000L
        );
        if (row.isEmpty()) {
            return null;
        }
        return asInt(row.get("jarId"), -1);
    }

    public ArrayList<LineMappingEntity> getLineMappings(String className,
                                                        Integer jarId,
                                                        String decompiler) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (m:LineMapping) " +
                        "WHERE m.className = $className AND ($jarId < 0 OR coalesce(m.jarId,-1) = $jarId) " +
                        "AND ($decompiler = '' OR m.decompiler = $decompiler) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.jarId,-1) AS jarId, coalesce(m.decompiler,'') AS decompiler, coalesce(m.mappingText,'') AS mappingText",
                Map.of("className", safe(className), "jarId", jarId == null ? -1 : jarId, "decompiler", safe(decompiler)),
                30_000L
        );
        ArrayList<LineMappingEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LineMappingEntity entity = new LineMappingEntity();
            entity.setClassName(safe(row.get("className")));
            entity.setMethodName(safe(row.get("methodName")));
            entity.setMethodDesc(safe(row.get("methodDesc")));
            entity.setJarId(asInt(row.get("jarId"), -1));
            entity.setDecompiler(safe(row.get("decompiler")));
            entity.setMappingText(safe(row.get("mappingText")));
            out.add(entity);
        }
        return out;
    }

    public void saveLineMappings(String className,
                                 Integer jarId,
                                 String decompiler,
                                 List<LineMappingEntity> mappings) {
        if (StringUtil.isNull(className)) {
            return;
        }
        int normalizedJarId = jarId == null ? -1 : jarId;
        writeRepository.run(
                "MATCH (m:LineMapping {className:$className, jarId:$jarId, decompiler:$decompiler}) DETACH DELETE m",
                Map.of("className", safe(className), "jarId", normalizedJarId, "decompiler", safe(decompiler)),
                30_000L
        );
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        long start = idSequenceRepository.reserve("line_mapping", mappings.size());
        for (int i = 0; i < mappings.size(); i++) {
            LineMappingEntity mapping = mappings.get(i);
            if (mapping == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("mapId", start + i);
            row.put("className", safe(className));
            row.put("jarId", normalizedJarId);
            row.put("decompiler", safe(decompiler));
            row.put("methodName", safe(mapping.getMethodName()));
            row.put("methodDesc", safe(mapping.getMethodDesc()));
            row.put("mappingText", safe(mapping.getMappingText()));
            rows.add(row);
        }
        writeRepository.runBatched(
                "UNWIND $rows AS row " +
                        "CREATE (:LineMapping {mapId:row.mapId, className:row.className, jarId:row.jarId, decompiler:row.decompiler, " +
                        "methodName:row.methodName, methodDesc:row.methodDesc, mappingText:row.mappingText})",
                rows,
                "rows",
                60_000L
        );
    }

    public String getJarNameById(Integer jarId) {
        if (jarId == null) {
            return null;
        }
        Map<String, Object> row = readRepository.one(
                "MATCH (j:Jar {jid:$jarId}) RETURN j.jarName AS jarName LIMIT 1",
                Map.of("jarId", jarId),
                30_000L
        );
        return safeOrNull(row.get("jarName"));
    }

    public ArrayList<ClassResult> getAllSpringC() {
        List<ClassResult> res = listClassResults(
                "MATCH (s:SpringController) OPTIONAL MATCH (c:Class {className:s.className, jarId:s.jarId}) " +
                        "RETURN s.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(s.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY jarId ASC, className ASC",
                Collections.emptyMap(),
                60_000L
        );
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllSpringI() {
        List<ClassResult> res = listClassResults(
                "MATCH (s:SpringInterceptor) OPTIONAL MATCH (c:Class {className:s.className, jarId:s.jarId}) " +
                        "RETURN s.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(s.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY jarId ASC, className ASC",
                Collections.emptyMap(),
                60_000L
        );
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllServlets() {
        List<ClassResult> res = listClassResults(
                "MATCH (j:JavaWebEndpoint {kind:'servlet'}) OPTIONAL MATCH (c:Class {className:j.className, jarId:j.jarId}) " +
                        "RETURN j.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(j.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY jarId ASC, className ASC",
                Collections.emptyMap(),
                60_000L
        );
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllFilters() {
        List<ClassResult> res = listClassResults(
                "MATCH (j:JavaWebEndpoint {kind:'filter'}) OPTIONAL MATCH (c:Class {className:j.className, jarId:j.jarId}) " +
                        "RETURN j.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(j.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY jarId ASC, className ASC",
                Collections.emptyMap(),
                60_000L
        );
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllListeners() {
        List<ClassResult> res = listClassResults(
                "MATCH (j:JavaWebEndpoint {kind:'listener'}) OPTIONAL MATCH (c:Class {className:j.className, jarId:j.jarId}) " +
                        "RETURN j.className AS className, coalesce(c.superClassName,'') AS superClassName, " +
                        "CASE WHEN coalesce(c.isInterface,false) THEN 1 ELSE 0 END AS isInterfaceInt, " +
                        "coalesce(j.jarId,-1) AS jarId, coalesce(c.jarName,'') AS jarName " +
                        "ORDER BY jarId ASC, className ASC",
                Collections.emptyMap(),
                60_000L
        );
        return filterWebClasses(res);
    }

    public ArrayList<MethodResult> getSpringM(String className) {
        if (CommonFilterUtil.isFilteredClass(className)) {
            return new ArrayList<>();
        }
        List<MethodResult> res = listMethodResults(
                "MATCH (m:SpringMapping {className:$className}) OPTIONAL MATCH (real:Method {className:m.className, methodName:m.methodName, methodDesc:m.methodDesc, jarId:m.jarId}) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(real.isStaticInt,0) AS isStaticInt, coalesce(real.accessInt,0) AS accessInt, coalesce(real.lineNumber,-1) AS lineNumber, " +
                        "coalesce(real.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId, coalesce(m.path,'') AS path, coalesce(m.restfulType,'') AS restfulType " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC",
                Map.of("className", safe(className)),
                60_000L
        );
        return filterWebMethods(res);
    }

    public ArrayList<MethodResult> getSpringMappingsAll(Integer jarId,
                                                        String keyword,
                                                        Integer offset,
                                                        Integer limit) {
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit <= 0 ? 500 : limit;
        int jarFilter = jarId == null ? -1 : jarId;
        String kw = safe(keyword);
        List<MethodResult> res = listMethodResults(
                "MATCH (m:SpringMapping) OPTIONAL MATCH (real:Method {className:m.className, methodName:m.methodName, methodDesc:m.methodDesc, jarId:m.jarId}) " +
                        "WHERE ($jarId < 0 OR coalesce(m.jarId,-1) = $jarId) " +
                        "AND ($keyword = '' OR toLower(m.className) CONTAINS toLower($keyword) OR toLower(m.methodName) CONTAINS toLower($keyword) OR toLower(coalesce(m.path,'')) CONTAINS toLower($keyword)) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(real.isStaticInt,0) AS isStaticInt, coalesce(real.accessInt,0) AS accessInt, coalesce(real.lineNumber,-1) AS lineNumber, " +
                        "coalesce(real.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId, coalesce(m.path,'') AS path, coalesce(m.restfulType,'') AS restfulType " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC SKIP $offset LIMIT $limit",
                Map.of("jarId", jarFilter, "keyword", kw, "offset", off, "limit", lim),
                60_000L
        );
        return filterWebMethods(res);
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
        return listMethodResults(
                "MATCH (a:Annotation) " +
                        "WHERE a.annoName IN $annoNames " +
                        "OPTIONAL MATCH (m:Method {className:a.className, methodName:a.methodName, jarId:a.jarId}) " +
                        "WITH a, m WHERE coalesce(a.scope,'method') = 'method' AND m IS NOT NULL " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, coalesce(m.lineNumber,-1) AS lineNumber, " +
                        "coalesce(m.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC",
                Map.of("annoNames", annoNames),
                60_000L
        );
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
        String matchMode = match == null ? "contains" : match.trim().toLowerCase();
        if (!"equal".equals(matchMode)) {
            matchMode = "contains";
        }
        String scopeMode = scope == null ? "any" : scope.trim().toLowerCase();
        if (!"class".equals(scopeMode) && !"method".equals(scopeMode)) {
            scopeMode = "any";
        }
        int jarFilter = jarId == null ? -1 : jarId;
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit <= 0 ? 500 : limit;

        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (a:Annotation) " +
                        "WHERE ($matchMode = 'equal' AND a.annoName IN $annoNames) " +
                        "OR ($matchMode = 'contains' AND any(name IN $annoNames WHERE toLower(a.annoName) CONTAINS toLower(name))) " +
                        "AND ($scopeMode = 'any' OR coalesce(a.scope,'method') = $scopeMode) " +
                        "AND ($jarId < 0 OR coalesce(a.jarId,-1) = $jarId) " +
                        "OPTIONAL MATCH (m:Method {className:a.className, methodName:a.methodName, jarId:a.jarId}) " +
                        "RETURN a.className AS className, coalesce(a.methodName,'') AS methodName, " +
                        "coalesce(m.methodDesc,'') AS methodDesc, coalesce(m.lineNumber,-1) AS lineNumber, " +
                        "coalesce(a.jarId,-1) AS jarId, coalesce(m.jarName,'') AS jarName, a.annoName AS annoName, coalesce(a.scope,'method') AS annoScope " +
                        "ORDER BY jarId ASC, className ASC, methodName ASC SKIP $offset LIMIT $limit",
                Map.of(
                        "annoNames", annoNames,
                        "matchMode", matchMode,
                        "scopeMode", scopeMode,
                        "jarId", jarFilter,
                        "offset", off,
                        "limit", lim
                ),
                60_000L
        );
        ArrayList<AnnoMethodResult> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AnnoMethodResult result = new AnnoMethodResult();
            result.setClassName(safe(row.get("className")));
            result.setMethodName(safe(row.get("methodName")));
            result.setMethodDesc(safe(row.get("methodDesc")));
            result.setLineNumber(asInt(row.get("lineNumber"), -1));
            result.setJarId(asInt(row.get("jarId"), -1));
            result.setJarName(safe(row.get("jarName")));
            result.setAnnoName(safe(row.get("annoName")));
            result.setAnnoScope(safe(row.get("annoScope")));
            results.add(result);
        }
        return results;
    }

    public ArrayList<String> getAnnoByClassName(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (a:Annotation) WHERE a.className = $className AND coalesce(a.scope,'class') = 'class' " +
                        "RETURN DISTINCT a.annoName AS annoName ORDER BY annoName ASC",
                Map.of("className", safe(className)),
                30_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            results.add(safe(row.get("annoName")));
        }
        return results;
    }

    public ArrayList<String> getAnnoByClassAndMethod(String className, String methodName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (a:Annotation) WHERE a.className = $className AND a.methodName = $methodName AND coalesce(a.scope,'method') = 'method' " +
                        "RETURN DISTINCT a.annoName AS annoName ORDER BY annoName ASC",
                Map.of("className", safe(className), "methodName", safe(methodName)),
                30_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            results.add(safe(row.get("annoName")));
        }
        return results;
    }

    public ArrayList<String> getStrings(int page) {
        int offset = (page - 1) * 100;
        if (offset < 0) {
            offset = 0;
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (s:StringLiteral) RETURN DISTINCT s.value AS value ORDER BY value ASC SKIP $offset LIMIT 100",
                Map.of("offset", offset),
                30_000L
        );
        ArrayList<String> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(safe(row.get("value")));
        }
        return out;
    }

    public int getStringCount() {
        return (int) readRepository.count(
                "MATCH (s:StringLiteral) RETURN count(s) AS count",
                Collections.emptyMap(),
                "count",
                30_000L
        );
    }

    public ArrayList<MethodResult> getMethodsByClassNoJar(String className) {
        ArrayList<MethodResult> results = listMethodResults(
                "MATCH (m:Method {className:$className}) " +
                        "RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, " +
                        "coalesce(m.lineNumber,-1) AS lineNumber, '' AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY m.jarId ASC, m.className ASC, m.methodName ASC, m.methodDesc ASC",
                Map.of("className", safe(className)),
                30_000L
        );
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        return results;
    }

    public MemberEntity getMemberByClassAndName(String className, String memberName) {
        ArrayList<MemberEntity> results = getMembersByClassAndName(className, memberName);
        return results.isEmpty() ? null : results.get(0);
    }

    public ArrayList<MemberEntity> getMembersByClassAndName(String className, String memberName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(memberName)) {
            return new ArrayList<>();
        }
        return listMembers(
                "MATCH (m:Member) WHERE m.className = $className AND m.memberName = $memberName " +
                        "RETURN m.memberId AS mid, m.memberName AS memberName, coalesce(m.modifiers,0) AS modifiers, coalesce(m.value,'') AS value, " +
                        "coalesce(m.typeClassName,'') AS typeClassName, m.className AS className, coalesce(m.methodDesc,'') AS methodDesc, " +
                        "coalesce(m.methodSignature,'') AS methodSignature, coalesce(m.jarId,-1) AS jarId ORDER BY mid ASC",
                Map.of("className", safe(className), "memberName", safe(memberName)),
                30_000L
        );
    }

    public ArrayList<MemberEntity> getMembersByClass(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        return listMembers(
                "MATCH (m:Member {className:$className}) " +
                        "RETURN m.memberId AS mid, m.memberName AS memberName, coalesce(m.modifiers,0) AS modifiers, coalesce(m.value,'') AS value, " +
                        "coalesce(m.typeClassName,'') AS typeClassName, m.className AS className, coalesce(m.methodDesc,'') AS methodDesc, " +
                        "coalesce(m.methodSignature,'') AS methodSignature, coalesce(m.jarId,-1) AS jarId ORDER BY mid ASC",
                Map.of("className", safe(className)),
                30_000L
        );
    }

    public ArrayList<String> getInterfacesByClass(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (i:InterfaceDef {className:$className}) RETURN i.interfaceName AS interfaceName ORDER BY toLower(i.interfaceName) ASC",
                Map.of("className", safe(className)),
                30_000L
        );
        ArrayList<String> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            results.add(safe(row.get("interfaceName")));
        }
        results.sort(Comparator.naturalOrder());
        return results;
    }

    public int updateMethod(String className, String methodName, String methodDesc, String newItem) {
        if (StringUtil.isNull(className) || StringUtil.isNull(methodName) || StringUtil.isNull(methodDesc) || StringUtil.isNull(newItem)) {
            return 0;
        }
        return writeRepository.write(30_000L, tx -> {
            Result result = tx.execute(
                    "MATCH (m:Method {className:$className, methodName:$methodName, methodDesc:$methodDesc}) " +
                            "SET m.methodName = $newItem RETURN count(m) AS updated",
                    Map.of(
                            "className", safe(className),
                            "methodName", safe(methodName),
                            "methodDesc", safe(methodDesc),
                            "newItem", safe(newItem)
                    )
            );
            if (!result.hasNext()) {
                return 0;
            }
            return asInt(result.next().get("updated"), 0);
        });
    }

    public Set<ClassReference.Handle> getSuperClasses(ClassReference.Handle ch) {
        if (ch == null || StringUtil.isNull(ch.getName())) {
            return Set.of();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (m:MethodImpl) WHERE m.implClassName = $className RETURN DISTINCT m.className AS className ORDER BY toLower(className) ASC",
                Map.of("className", safe(ch.getName())),
                30_000L
        );
        Set<ClassReference.Handle> set = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String value = safe(row.get("className"));
            if (value.equals(ch.getName())) {
                continue;
            }
            set.add(new ClassReference.Handle(value));
        }
        return set;
    }

    public Set<ClassReference.Handle> getSubClasses(ClassReference.Handle ch) {
        if (ch == null || StringUtil.isNull(ch.getName())) {
            return Set.of();
        }
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (m:MethodImpl) WHERE m.className = $className RETURN DISTINCT m.implClassName AS className ORDER BY toLower(className) ASC",
                Map.of("className", safe(ch.getName())),
                30_000L
        );
        Set<ClassReference.Handle> set = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String value = safe(row.get("className"));
            if (value.equals(ch.getName())) {
                continue;
            }
            set.add(new ClassReference.Handle(value));
        }
        return set;
    }

    public ClassReference getClassRef(ClassReference.Handle ch, Integer jarId) {
        if (ch == null || StringUtil.isNull(ch.getName())) {
            return null;
        }
        ArrayList<ClassResult> results = getClassesByClass(ch.getName());
        if (results.isEmpty()) {
            return null;
        }
        results.sort(Comparator.comparingInt(ClassResult::getJarId)
                .thenComparing(ClassResult::getJarName, Comparator.nullsLast(String::compareTo)));
        ClassResult cr = results.get(0);
        if (jarId != null) {
            for (ClassResult item : results) {
                if (item != null && item.getJarId() == jarId) {
                    cr = item;
                    break;
                }
            }
        }

        ArrayList<String> interfaces = getInterfacesByClass(ch.getName());
        ArrayList<String> anno = getAnnoByClassName(ch.getName());
        ArrayList<MemberEntity> memberEntities = getMembersByClass(ch.getName());

        ArrayList<ClassReference.Member> members = new ArrayList<>();
        for (MemberEntity me : memberEntities) {
            ClassReference.Member member = new ClassReference.Member
                    (me.getMemberName(), me.getModifiers(), me.getValue(),
                            me.getMethodDesc(), me.getMethodSignature(), new ClassReference.Handle(me.getTypeClassName()));
            members.add(member);
        }

        return new ClassReference(
                cr.getClassName(),
                cr.getSuperClassName(),
                interfaces,
                cr.getIsInterfaceInt() == 1,
                members,
                anno,
                "none", jarId != null ? jarId : cr.getJarId());
    }

    public int getMethodsCount() {
        return (int) readRepository.count(
                "MATCH (m:Method) RETURN count(m) AS count",
                Collections.emptyMap(),
                "count",
                30_000L
        );
    }

    public ArrayList<MethodReference> getAllMethodRef(int offset) {
        int size = 100;
        if (offset < 0) {
            offset = 0;
        }
        ArrayList<MethodResult> results = listMethodResults(
                "MATCH (m:Method) RETURN m.className AS className, m.methodName AS methodName, m.methodDesc AS methodDesc, " +
                        "coalesce(m.isStaticInt,0) AS isStaticInt, coalesce(m.accessInt,0) AS accessInt, coalesce(m.lineNumber,-1) AS lineNumber, " +
                        "coalesce(m.jarName,'') AS jarName, coalesce(m.jarId,-1) AS jarId " +
                        "ORDER BY m.jarId ASC, m.className ASC, m.methodName ASC, m.methodDesc ASC SKIP $offset LIMIT $size",
                Map.of("offset", offset, "size", size),
                60_000L
        );
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        ArrayList<MethodReference> list = new ArrayList<>();
        for (MethodResult result : results) {
            MethodReference.Handle mh = new MethodReference.Handle(
                    new ClassReference.Handle(result.getClassName()),
                    result.getMethodName(),
                    result.getMethodDesc());
            ArrayList<String> ma = getAnnoByClassAndMethod(mh.getClassReference().getName(), mh.getName());
            MethodReference mr = new MethodReference(mh.getClassReference(),
                    mh.getName(), mh.getDesc(),
                    result.getIsStaticInt() == 1,
                    ma.stream().map(AnnoReference::new).collect(Collectors.toSet()),
                    result.getAccessInt(), result.getLineNumber(), result.getJarName(), result.getJarId());
            list.add(mr);
        }
        return list;
    }

    public List<MemberEntity> getAllMembersInfo() {
        ArrayList<MemberEntity> members = listMembers(
                "MATCH (m:Member) RETURN m.memberId AS mid, m.memberName AS memberName, coalesce(m.modifiers,0) AS modifiers, coalesce(m.value,'') AS value, " +
                        "coalesce(m.typeClassName,'') AS typeClassName, m.className AS className, coalesce(m.methodDesc,'') AS methodDesc, " +
                        "coalesce(m.methodSignature,'') AS methodSignature, coalesce(m.jarId,-1) AS jarId",
                Collections.emptyMap(),
                60_000L
        );
        ArrayList<MemberEntity> list = new ArrayList<>();
        for (MemberEntity me : members) {
            if ("java/lang/String".equals(me.getTypeClassName())) {
                list.add(me);
            }
        }
        return list;
    }

    public Map<String, String> getStringMap() {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (s:StringLiteral) RETURN s.className AS className, s.value AS strValue",
                Collections.emptyMap(),
                60_000L
        );
        Map<String, String> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            out.put(safe(row.get("className")), safe(row.get("strValue")));
        }
        return out;
    }

    public ArrayList<ResourceEntity> getResources(String path, Integer jarId, int offset, int limit) {
        int off = Math.max(0, offset);
        int lim = limit <= 0 ? 200 : limit;
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (r:Resource) WHERE ($path = '' OR toLower(r.resourcePath) CONTAINS toLower($path)) " +
                        "AND ($jarId < 0 OR coalesce(r.jarId,-1) = $jarId) " +
                        "RETURN coalesce(r.rid,-1) AS rid, coalesce(r.resourcePath,'') AS resourcePath, coalesce(r.pathStr,'') AS pathStr, " +
                        "coalesce(r.jarName,'') AS jarName, coalesce(r.jarId,-1) AS jarId, coalesce(r.fileSize,0) AS fileSize, coalesce(r.isText,0) AS isText " +
                        "ORDER BY rid ASC SKIP $offset LIMIT $limit",
                Map.of("path", safe(path), "jarId", jarId == null ? -1 : jarId, "offset", off, "limit", lim),
                60_000L
        );
        return filterResources(mapResources(rows));
    }

    public int getResourceCount(String path, Integer jarId) {
        return (int) readRepository.count(
                "MATCH (r:Resource) WHERE ($path = '' OR toLower(r.resourcePath) CONTAINS toLower($path)) " +
                        "AND ($jarId < 0 OR coalesce(r.jarId,-1) = $jarId) RETURN count(r) AS count",
                Map.of("path", safe(path), "jarId", jarId == null ? -1 : jarId),
                "count",
                30_000L
        );
    }

    public ResourceEntity getResourceById(int rid) {
        Map<String, Object> row = readRepository.one(
                "MATCH (r:Resource {rid:$rid}) RETURN coalesce(r.rid,-1) AS rid, coalesce(r.resourcePath,'') AS resourcePath, " +
                        "coalesce(r.pathStr,'') AS pathStr, coalesce(r.jarName,'') AS jarName, coalesce(r.jarId,-1) AS jarId, " +
                        "coalesce(r.fileSize,0) AS fileSize, coalesce(r.isText,0) AS isText LIMIT 1",
                Map.of("rid", rid),
                30_000L
        );
        if (row.isEmpty()) {
            return null;
        }
        ResourceEntity entity = toResource(row);
        if (entity != null && CommonFilterUtil.isFilteredResourcePath(entity.getResourcePath())) {
            return null;
        }
        return entity;
    }

    public ResourceEntity getResourceByPath(Integer jarId, String path) {
        Map<String, Object> row = readRepository.one(
                "MATCH (r:Resource {jarId:$jarId, resourcePath:$path}) RETURN coalesce(r.rid,-1) AS rid, coalesce(r.resourcePath,'') AS resourcePath, " +
                        "coalesce(r.pathStr,'') AS pathStr, coalesce(r.jarName,'') AS jarName, coalesce(r.jarId,-1) AS jarId, " +
                        "coalesce(r.fileSize,0) AS fileSize, coalesce(r.isText,0) AS isText LIMIT 1",
                Map.of("jarId", jarId == null ? -1 : jarId, "path", safe(path)),
                30_000L
        );
        if (row.isEmpty()) {
            return null;
        }
        ResourceEntity entity = toResource(row);
        if (entity != null && CommonFilterUtil.isFilteredResourcePath(entity.getResourcePath())) {
            return null;
        }
        return entity;
    }

    public ArrayList<ResourceEntity> getResourcesByPath(String path, Integer limit) {
        int lim = limit == null || limit <= 0 ? 200 : limit;
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (r:Resource) WHERE toLower(r.resourcePath) CONTAINS toLower($path) " +
                        "RETURN coalesce(r.rid,-1) AS rid, coalesce(r.resourcePath,'') AS resourcePath, coalesce(r.pathStr,'') AS pathStr, " +
                        "coalesce(r.jarName,'') AS jarName, coalesce(r.jarId,-1) AS jarId, coalesce(r.fileSize,0) AS fileSize, coalesce(r.isText,0) AS isText " +
                        "ORDER BY rid ASC LIMIT $limit",
                Map.of("path", safe(path), "limit", lim),
                60_000L
        );
        return filterResources(mapResources(rows));
    }

    public ArrayList<ResourceEntity> getTextResources(Integer jarId) {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (r:Resource) WHERE coalesce(r.isText,0) = 1 AND ($jarId < 0 OR coalesce(r.jarId,-1) = $jarId) " +
                        "RETURN coalesce(r.rid,-1) AS rid, coalesce(r.resourcePath,'') AS resourcePath, coalesce(r.pathStr,'') AS pathStr, " +
                        "coalesce(r.jarName,'') AS jarName, coalesce(r.jarId,-1) AS jarId, coalesce(r.fileSize,0) AS fileSize, coalesce(r.isText,0) AS isText " +
                        "ORDER BY rid ASC",
                Map.of("jarId", jarId == null ? -1 : jarId),
                60_000L
        );
        return filterResources(mapResources(rows));
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

    private ArrayList<MethodResult> listMethodResults(String cypher,
                                                      Map<String, Object> params,
                                                      long timeoutMs) {
        List<Map<String, Object>> rows = readRepository.list(cypher, params == null ? Collections.emptyMap() : params, timeoutMs);
        ArrayList<MethodResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MethodResult m = new MethodResult();
            m.setClassName(safe(row.get("className")));
            m.setMethodName(safe(row.get("methodName")));
            m.setMethodDesc(safe(row.get("methodDesc")));
            m.setIsStaticInt(asInt(row.get("isStaticInt"), 0));
            m.setAccessInt(asInt(row.get("accessInt"), 0));
            m.setLineNumber(asInt(row.get("lineNumber"), -1));
            m.setJarName(safe(row.get("jarName")));
            m.setJarId(asInt(row.get("jarId"), -1));
            if (row.containsKey("path")) {
                m.setPath(safe(row.get("path")));
            }
            if (row.containsKey("restfulType")) {
                m.setRestfulType(safe(row.get("restfulType")));
            }
            if (row.containsKey("strValue")) {
                m.setStrValue(safe(row.get("strValue")));
            }
            out.add(m);
        }
        return out;
    }

    private ArrayList<ClassResult> listClassResults(String cypher,
                                                    Map<String, Object> params,
                                                    long timeoutMs) {
        List<Map<String, Object>> rows = readRepository.list(cypher, params == null ? Collections.emptyMap() : params, timeoutMs);
        ArrayList<ClassResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ClassResult c = new ClassResult();
            c.setClassName(safe(row.get("className")));
            c.setSuperClassName(safe(row.get("superClassName")));
            c.setIsInterfaceInt(asInt(row.get("isInterfaceInt"), 0));
            c.setJarId(asInt(row.get("jarId"), -1));
            c.setJarName(safe(row.get("jarName")));
            out.add(c);
        }
        return out;
    }

    private ArrayList<MethodCallResult> listMethodCallResults(String cypher,
                                                              Map<String, Object> params,
                                                              long timeoutMs) {
        List<Map<String, Object>> rows = readRepository.list(cypher, params == null ? Collections.emptyMap() : params, timeoutMs);
        ArrayList<MethodCallResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MethodCallResult r = new MethodCallResult();
            r.setCallerClassName(safe(row.get("callerClassName")));
            r.setCallerMethodName(safe(row.get("callerMethodName")));
            r.setCallerMethodDesc(safe(row.get("callerMethodDesc")));
            r.setCallerJarId(asInt(row.get("callerJarId"), -1));
            r.setCallerJarName(safe(row.get("callerJarName")));
            r.setCalleeClassName(safe(row.get("calleeClassName")));
            r.setCalleeMethodName(safe(row.get("calleeMethodName")));
            r.setCalleeMethodDesc(safe(row.get("calleeMethodDesc")));
            r.setCalleeJarId(asInt(row.get("calleeJarId"), -1));
            r.setCalleeJarName(safe(row.get("calleeJarName")));
            r.setOpCode(asInt(row.get("opCode"), -1));
            r.setEdgeType(safe(row.get("edgeType")));
            r.setEdgeConfidence(safe(row.get("edgeConfidence")));
            r.setEdgeEvidence(safe(row.get("edgeEvidence")));
            r.setCallSiteKey(safe(row.get("callSiteKey")));
            out.add(r);
        }
        return out;
    }

    private ArrayList<CallSiteEntity> listCallSites(String cypher,
                                                    Map<String, Object> params,
                                                    long timeoutMs) {
        List<Map<String, Object>> rows = readRepository.list(cypher, params == null ? Collections.emptyMap() : params, timeoutMs);
        ArrayList<CallSiteEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CallSiteEntity entity = new CallSiteEntity();
            entity.setCallerClassName(safe(row.get("callerClassName")));
            entity.setCallerMethodName(safe(row.get("callerMethodName")));
            entity.setCallerMethodDesc(safe(row.get("callerMethodDesc")));
            entity.setCalleeOwner(safe(row.get("calleeOwner")));
            entity.setCalleeMethodName(safe(row.get("calleeMethodName")));
            entity.setCalleeMethodDesc(safe(row.get("calleeMethodDesc")));
            entity.setOpCode(asInt(row.get("opCode"), -1));
            entity.setLineNumber(asInt(row.get("lineNumber"), -1));
            entity.setCallIndex(asInt(row.get("callIndex"), -1));
            entity.setReceiverType(safe(row.get("receiverType")));
            entity.setJarId(asInt(row.get("jarId"), -1));
            entity.setCallSiteKey(safe(row.get("callSiteKey")));
            out.add(entity);
        }
        return out;
    }

    private ArrayList<MemberEntity> listMembers(String cypher,
                                                Map<String, Object> params,
                                                long timeoutMs) {
        List<Map<String, Object>> rows = readRepository.list(cypher, params == null ? Collections.emptyMap() : params, timeoutMs);
        ArrayList<MemberEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MemberEntity entity = new MemberEntity();
            entity.setMid(asInt(row.get("mid"), -1));
            entity.setMemberName(safe(row.get("memberName")));
            entity.setModifiers(asInt(row.get("modifiers"), 0));
            entity.setValue(safe(row.get("value")));
            entity.setTypeClassName(safe(row.get("typeClassName")));
            entity.setClassName(safe(row.get("className")));
            entity.setMethodDesc(safe(row.get("methodDesc")));
            entity.setMethodSignature(safe(row.get("methodSignature")));
            entity.setJarId(asInt(row.get("jarId"), -1));
            out.add(entity);
        }
        return out;
    }

    private ArrayList<ResourceEntity> mapResources(List<Map<String, Object>> rows) {
        ArrayList<ResourceEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ResourceEntity entity = toResource(row);
            if (entity != null) {
                out.add(entity);
            }
        }
        return out;
    }

    private ResourceEntity toResource(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        ResourceEntity entity = new ResourceEntity();
        entity.setRid(asInt(row.get("rid"), -1));
        entity.setResourcePath(safe(row.get("resourcePath")));
        entity.setPathStr(safe(row.get("pathStr")));
        entity.setJarName(safe(row.get("jarName")));
        entity.setJarId(asInt(row.get("jarId"), -1));
        entity.setFileSize(asLong(row.get("fileSize"), 0L));
        entity.setIsText(asInt(row.get("isText"), 0));
        return entity;
    }

    private static String normalizeClassFileName(String className) {
        if (className == null) {
            return "";
        }
        String lookup = className;
        if (!lookup.endsWith(".class")) {
            lookup = lookup + ".class";
        }
        return lookup;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String safeOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String v = String.valueOf(value);
        return v.isBlank() ? null : v;
    }

    private static int asInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long asLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }
}
