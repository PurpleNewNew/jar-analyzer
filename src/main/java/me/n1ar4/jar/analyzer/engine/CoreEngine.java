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
import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.core.mapper.*;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
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
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreEngine {
    private static final Logger logger = LogManager.getLogger();
    private final SqlSessionFactory factory;
    private volatile CallGraphCache callGraphCache;

    public boolean isEnabled() {
        Path dbPath = Paths.get(Const.dbFile);
        if (!Files.exists(dbPath)) {
            return false;
        }
        Path tempDir = Paths.get(Const.tempDir);
        if (!Files.exists(tempDir)) {
            return false;
        }
        if (!Files.isDirectory(tempDir)) {
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
        Path dbPath = Paths.get(dbPathValue);
        if (!"jar-analyzer.db".equals(dbPath.getFileName().toString()) ||
                !Files.exists(dbPath)) {
            throw new RuntimeException("start engine error");
        }
        factory = SqlSessionFactoryUtil.sqlSessionFactory;
        // 开启 二级缓存
        // 因为数据库不涉及修改操作 仅查询 不会变化 开二级缓存没有问题
        factory.getConfiguration().setCacheEnabled(true);
        applyQueryPragmas();
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
        SqlSession session = factory.openSession(true);
        try {
            MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
            List<MethodCallResult> edges = methodCallMapper.selectAllCallEdges();
            CallGraphCache cache = CallGraphCache.build(edges);
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            logger.info("call graph cache loaded: edges={}, methods={}, elapsedMs={}",
                    cache.getEdgeCount(), cache.getMethodCount(), elapsedMs);
            return cache;
        } finally {
            session.close();
        }
    }

    private void applyQueryPragmas() {
        SqlSession session = factory.openSession(true);
        try {
            Statement stmt = session.getConnection().createStatement();
            try {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA temp_store=MEMORY");
                stmt.execute("PRAGMA cache_size=-64000");
            } finally {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.warn("apply query pragmas failed: {}", ex.toString());
        } finally {
            session.close();
        }
    }

    public ArrayList<MethodResult> getMethodsByClass(String className) {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodMapper.selectMethodsByClassName(className));
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        session.close();
        return results;
    }

    public ClassResult getClassByClass(String className) {
        if (CommonFilterUtil.isModuleInfoClassName(className)) {
            return null;
        }
        SqlSession session = factory.openSession(true);
        ClassMapper classMapper = session.getMapper(ClassMapper.class);
        ArrayList<ClassResult> results = new ArrayList<>(classMapper.selectClassByClassName(className));
        session.close();
        return results.isEmpty() ? null : results.get(0);
    }

    public ArrayList<String> includeClassByClassName(String className) {
        return includeClassByClassName(className, false);
    }

    public ArrayList<String> includeClassByClassName(String className, boolean includeJdk) {
        SqlSession session = factory.openSession(true);
        ClassMapper classMapper = session.getMapper(ClassMapper.class);
        ArrayList<String> results = new ArrayList<>(classMapper.includeClassByClassName(className));
        session.close();
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
        SqlSession session = factory.openSession(true);
        ClassFileMapper classMapper = session.getMapper(ClassFileMapper.class);
        className = className + ".class";
        String res = classMapper.selectPathByClass(className);
        session.close();
        return res;
    }

    public ArrayList<MethodResult> getCallers(String calleeClass, String calleeMethod, String calleeDesc) {
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallers(calleeClass, calleeMethod, calleeDesc);
        }
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodCallMapper.selectCallers(
                calleeMethod, calleeDesc, calleeClass));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getCallersLike(String calleeClass, String calleeMethod, String calleeDesc) {
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodCallMapper.selectCallersLike(
                calleeMethod, calleeDesc, calleeClass));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getCallee(String callerClass, String callerMethod, String callerDesc) {
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallees(callerClass, callerMethod, callerDesc);
        }
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodCallMapper.selectCallee(
                callerMethod, callerDesc, callerClass));
        session.close();
        return results;
    }

    public ArrayList<MethodCallResult> getCallEdgesByCallee(String calleeClass,
                                                            String calleeMethod,
                                                            String calleeDesc,
                                                            Integer offset,
                                                            Integer limit) {
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        ArrayList<MethodCallResult> results = new ArrayList<>(methodCallMapper.selectCallEdgesByCallee(
                calleeClass, calleeMethod, calleeDesc, offset, limit));
        session.close();
        return results;
    }

    public ArrayList<MethodCallResult> getCallEdgesByCaller(String callerClass,
                                                            String callerMethod,
                                                            String callerDesc,
                                                            Integer offset,
                                                            Integer limit) {
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        ArrayList<MethodCallResult> results = new ArrayList<>(methodCallMapper.selectCallEdgesByCaller(
                callerClass, callerMethod, callerDesc, offset, limit));
        session.close();
        return results;
    }

    public MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getEdgeMeta(caller, callee);
        }
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        MethodCallEntity meta = methodCallMapper.selectEdgeMeta(
                caller.getClassReference().getName(),
                caller.getName(),
                caller.getDesc(),
                callee.getClassReference().getName(),
                callee.getName(),
                callee.getDesc());
        session.close();
        if (meta == null) {
            return null;
        }
        return new MethodCallMeta(meta.getEdgeType(), meta.getEdgeConfidence(), meta.getEdgeEvidence());
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
                        new ClassReference.Handle(key.getCallerClass()),
                        key.getCallerMethod(),
                        key.getCallerDesc());
                MethodReference.Handle callee = new MethodReference.Handle(
                        new ClassReference.Handle(key.getCalleeClass()),
                        key.getCalleeMethod(),
                        key.getCalleeDesc());
                MethodCallMeta meta = cache.getEdgeMeta(caller, callee);
                if (meta != null) {
                    out.put(key, meta);
                }
            }
            return out;
        }
        List<MethodCallEntity> params = new ArrayList<>();
        for (MethodCallKey key : keys) {
            if (key == null) {
                continue;
            }
            MethodCallEntity entity = new MethodCallEntity();
            entity.setCallerClassName(key.getCallerClass());
            entity.setCallerMethodName(key.getCallerMethod());
            entity.setCallerMethodDesc(key.getCallerDesc());
            entity.setCalleeClassName(key.getCalleeClass());
            entity.setCalleeMethodName(key.getCalleeMethod());
            entity.setCalleeMethodDesc(key.getCalleeDesc());
            params.add(entity);
        }
        if (params.isEmpty()) {
            return out;
        }
        SqlSession session = factory.openSession(true);
        MethodCallMapper methodCallMapper = session.getMapper(MethodCallMapper.class);
        final int batchSize = 150; // 6 params per edge, SQLite default max vars is 999.
        for (int i = 0; i < params.size(); i += batchSize) {
            int end = Math.min(i + batchSize, params.size());
            List<MethodCallEntity> rows = methodCallMapper.selectEdgeMetaBatch(params.subList(i, end));
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            for (MethodCallEntity row : rows) {
                if (row == null) {
                    continue;
                }
                MethodCallKey key = new MethodCallKey(
                        row.getCallerClassName(),
                        row.getCallerMethodName(),
                        row.getCallerMethodDesc(),
                        row.getCalleeClassName(),
                        row.getCalleeMethodName(),
                        row.getCalleeMethodDesc()
                );
                out.put(key, new MethodCallMeta(row.getEdgeType(), row.getEdgeConfidence(), row.getEdgeEvidence()));
            }
        }
        session.close();
        return out;
    }

    public ArrayList<MethodResult> getMethod(String className, String methodName, String methodDesc) {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                methodMapper.selectMethods(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public ArrayList<CallSiteEntity> getCallSitesByCaller(String className,
                                                          String methodName,
                                                          String methodDesc) {
        SqlSession session = factory.openSession(true);
        CallSiteMapper mapper = session.getMapper(CallSiteMapper.class);
        ArrayList<CallSiteEntity> results = new ArrayList<>(
                mapper.selectByCaller(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public ArrayList<LocalVarEntity> getLocalVarsByMethod(String className,
                                                          String methodName,
                                                          String methodDesc) {
        SqlSession session = factory.openSession(true);
        LocalVarMapper mapper = session.getMapper(LocalVarMapper.class);
        ArrayList<LocalVarEntity> results = new ArrayList<>(
                mapper.selectByMethod(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getMethodLike(String className, String methodName, String methodDesc) {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                methodMapper.selectMethodsLike(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getMethodsByStr(String val) {
        return getMethodsByStr(val, null, null, null, "auto");
    }

    public ArrayList<MethodResult> getMethodsByStrEqual(String val) {
        SqlSession session = factory.openSession(true);
        StringMapper stringMapper = session.getMapper(StringMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                stringMapper.selectMethodByStringEqual(val));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getMethodsByStr(String val,
                                                   Integer jarId,
                                                   String classLike,
                                                   Integer limit,
                                                   String mode) {
        SqlSession session = factory.openSession(true);
        StringMapper stringMapper = session.getMapper(StringMapper.class);
        ArrayList<MethodResult> results;

        String m = mode == null ? "auto" : mode.trim().toLowerCase();
        String classFilter = classLike == null ? null : classLike.trim();
        if (classFilter != null && classFilter.endsWith("/")) {
            classFilter = classFilter.substring(0, classFilter.length() - 1);
        }
        Integer limitVal = limit;
        if (limitVal == null || limitVal <= 0) {
            limitVal = 1000;
        }

        if ("equal".equals(m)) {
            results = new ArrayList<>(stringMapper.selectMethodByStringEqualFiltered(
                    val, jarId, classFilter, limitVal));
            session.close();
            return results;
        }

        boolean tryFts = "fts".equals(m) || ("auto".equals(m) && isSafeFtsQuery(val));
        if (tryFts) {
            try {
                String ftsQuery = buildFtsQuery(val);
                results = new ArrayList<>(stringMapper.selectMethodByStringFts(
                        ftsQuery, jarId, classFilter, limitVal));
                if (!results.isEmpty() || "fts".equals(m)) {
                    session.close();
                    return results;
                }
            } catch (Throwable t) {
                // fallback to LIKE
            }
        }

        boolean prefix = "prefix".equals(m);
        String pattern = prefix ? (val + "%") : ("%" + val + "%");
        results = new ArrayList<>(stringMapper.selectMethodByStringPattern(
                pattern, jarId, classFilter, limitVal));
        session.close();
        return results;
    }

    private boolean isSafeFtsQuery(String val) {
        if (val == null) {
            return false;
        }
        String v = val.trim();
        if (v.isEmpty()) {
            return false;
        }
        // allow simple tokens: letters, digits, dot, dash, underscore
        return v.matches("[A-Za-z0-9._-]{2,}");
    }

    private String buildFtsQuery(String val) {
        String v = val == null ? "" : val.trim();
        String escaped = v.replace("\"", "\"\"");
        return "value:\"" + escaped + "\"";
    }

    public ArrayList<String> getJarsPath() {
        SqlSession session = factory.openSession(true);
        JarMapper jarMapper = session.getMapper(JarMapper.class);
        ArrayList<String> results = new ArrayList<>(
                jarMapper.selectAllJars());
        session.close();
        return results;
    }

    public ArrayList<String> getJarNames() {
        SqlSession session = factory.openSession(true);
        JarMapper jarMapper = session.getMapper(JarMapper.class);
        ArrayList<String> results = new ArrayList<>(
                jarMapper.selectAllJarNames());
        session.close();
        return results;
    }

    public ArrayList<JarEntity> getJarsMeta() {
        SqlSession session = factory.openSession(true);
        JarMapper jarMapper = session.getMapper(JarMapper.class);
        ArrayList<JarEntity> results = new ArrayList<>(
                jarMapper.selectAllJarMeta());
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getImpls(String className,
                                            String methodName,
                                            String methodDesc) {
        SqlSession session = factory.openSession(true);
        MethodImplMapper methodMapper = session.getMapper(MethodImplMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                methodMapper.selectImplClassName(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public ArrayList<MethodResult> getSuperImpls(String className, String methodName, String methodDesc) {
        SqlSession session = factory.openSession(true);
        MethodImplMapper methodMapper = session.getMapper(MethodImplMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                methodMapper.selectSuperImpls(className, methodName, methodDesc));
        session.close();
        return results;
    }

    public String getJarByClass(String className) {
        SqlSession session = factory.openSession(true);
        ClassMapper classMapper = session.getMapper(ClassMapper.class);
        String result = classMapper.selectJarByClass(className);
        session.close();
        return result;
    }

    public Integer getJarIdByClass(String className) {
        if (StringUtil.isNull(className)) {
            return null;
        }
        SqlSession session = factory.openSession(true);
        ClassMapper classMapper = session.getMapper(ClassMapper.class);
        Integer result = classMapper.selectJarIdByClass(className);
        session.close();
        return result;
    }

    public ArrayList<LineMappingEntity> getLineMappings(String className,
                                                        Integer jarId,
                                                        String decompiler) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        SqlSession session = factory.openSession(true);
        LineMappingMapper mapper = session.getMapper(LineMappingMapper.class);
        List<LineMappingEntity> rows = mapper.selectByClass(className, jarId, decompiler);
        session.close();
        return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    public void saveLineMappings(String className,
                                 Integer jarId,
                                 String decompiler,
                                 List<LineMappingEntity> mappings) {
        if (StringUtil.isNull(className)) {
            return;
        }
        SqlSession session = factory.openSession(true);
        LineMappingMapper mapper = session.getMapper(LineMappingMapper.class);
        mapper.deleteByClass(className, jarId, decompiler);
        if (mappings != null && !mappings.isEmpty()) {
            mapper.insertMappings(mappings);
        }
        session.close();
    }

    public String getJarNameById(Integer jarId) {
        if (jarId == null) {
            return null;
        }
        SqlSession session = factory.openSession(true);
        JarMapper jarMapper = session.getMapper(JarMapper.class);
        JarEntity jar = jarMapper.selectJarById(jarId);
        session.close();
        if (jar == null) {
            return null;
        }
        return jar.getJarName();
    }

    public ArrayList<ClassResult> getAllSpringC() {
        SqlSession session = factory.openSession(true);
        SpringControllerMapper springControllerMapper = session.getMapper(SpringControllerMapper.class);
        List<ClassResult> res = springControllerMapper.selectAllSpringC();
        session.close();
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllSpringI() {
        SqlSession session = factory.openSession(true);
        SpringInterceptorMapper springInterceptorMapper = session.getMapper(SpringInterceptorMapper.class);
        List<ClassResult> res = springInterceptorMapper.selectAllSpringI();
        session.close();
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllServlets() {
        SqlSession session = factory.openSession(true);
        JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
        List<ClassResult> res = javaWebMapper.selectAllServlets();
        session.close();
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllFilters() {
        SqlSession session = factory.openSession(true);
        JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
        List<ClassResult> res = javaWebMapper.selectAllFilters();
        session.close();
        return filterWebClasses(res);
    }

    public ArrayList<ClassResult> getAllListeners() {
        SqlSession session = factory.openSession(true);
        JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
        List<ClassResult> res = javaWebMapper.selectAllListeners();
        session.close();
        return filterWebClasses(res);
    }

    public ArrayList<MethodResult> getSpringM(String className) {
        if (CommonFilterUtil.isFilteredClass(className)) {
            return new ArrayList<>();
        }
        SqlSession session = factory.openSession(true);
        SpringMethodMapper springMethodMapper = session.getMapper(SpringMethodMapper.class);
        List<MethodResult> res = springMethodMapper.selectMappingsByClassName(className);
        session.close();
        return filterWebMethods(res);
    }

    public ArrayList<MethodResult> getSpringMappingsAll(Integer jarId,
                                                        String keyword,
                                                        Integer offset,
                                                        Integer limit) {
        SqlSession session = factory.openSession(true);
        SpringMethodMapper springMethodMapper = session.getMapper(SpringMethodMapper.class);
        List<MethodResult> res = springMethodMapper.selectMappingsAll(jarId, keyword, offset, limit);
        session.close();
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
        SqlSession session = factory.openSession(true);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(
                annoMapper.selectMethodsByAnnoNames(annoNames));
        session.close();
        return results;
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
        SqlSession session = factory.openSession(true);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        ArrayList<AnnoMethodResult> results = new ArrayList<>(
                annoMapper.selectMethodsByAnno(annoNames, matchMode, scopeMode, jarId, offset, limit));
        session.close();
        return results;
    }

    public ArrayList<String> getAnnoByClassName(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        SqlSession session = factory.openSession(true);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        ArrayList<String> results = new ArrayList<>(annoMapper.selectAnnoByClassName(className));
        session.close();
        return results;
    }

    public ArrayList<String> getAnnoByClassAndMethod(String className, String methodName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
            return new ArrayList<>();
        }
        SqlSession session = factory.openSession(true);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        ArrayList<String> results = new ArrayList<>(
                annoMapper.selectAnnoByClassAndMethod(className, methodName));
        session.close();
        return results;
    }

    public ArrayList<String> getStrings(int page) {
        SqlSession session = factory.openSession(true);
        StringMapper stringMapper = session.getMapper(StringMapper.class);
        int offset = (page - 1) * 100;
        List<String> res = stringMapper.selectStrings(offset);
        session.close();
        return new ArrayList<>(res);
    }

    public int getStringCount() {
        SqlSession session = factory.openSession(true);
        StringMapper stringMapper = session.getMapper(StringMapper.class);
        int res = stringMapper.selectCount();
        session.close();
        return res;
    }

    public ArrayList<MethodResult> getMethodsByClassNoJar(String className) {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodMapper.selectMethodsByClassNameNoJar(className));
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        session.close();
        return results;
    }

    public MemberEntity getMemberByClassAndName(String className, String memberName) {
        if (StringUtil.isNull(className) || StringUtil.isNull(memberName)) {
            return null;
        }
        SqlSession session = factory.openSession(true);
        MemberMapper memberMapper = session.getMapper(MemberMapper.class);
        ArrayList<MemberEntity> results = new ArrayList<>(
                memberMapper.selectMembersByClassAndName(className, memberName));
        session.close();
        return results.isEmpty() ? null : results.get(0);
    }

    public ArrayList<String> getInterfacesByClass(String className) {
        if (StringUtil.isNull(className)) {
            return new ArrayList<>();
        }
        SqlSession session = factory.openSession(true);
        InterfaceMapper interfaceMapper = session.getMapper(InterfaceMapper.class);
        ArrayList<String> results = new ArrayList<>(
                interfaceMapper.selectInterfacesByClass(className));
        session.close();
        return results;
    }

    public int updateMethod(String className, String methodName, String methodDesc, String newItem) {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        int res = methodMapper.updateMethod(className, methodName, methodDesc, newItem);
        session.close();
        return res;
    }

    public Set<ClassReference.Handle> getSuperClasses(ClassReference.Handle ch) {
        SqlSession session = factory.openSession(true);
        MethodImplMapper miMapper = session.getMapper(MethodImplMapper.class);
        List<String> tempRes = miMapper.selectSuperClasses(ch.getName());
        Set<ClassReference.Handle> set = new HashSet<>();
        for (String temp : tempRes) {
            if (temp.equals(ch.getName())) {
                continue;
            }
            set.add(new ClassReference.Handle(temp));
        }
        session.close();
        return set;
    }

    public Set<ClassReference.Handle> getSubClasses(ClassReference.Handle ch) {
        SqlSession session = factory.openSession(true);
        MethodImplMapper miMapper = session.getMapper(MethodImplMapper.class);
        List<String> tempRes = miMapper.selectSubClasses(ch.getName());
        Set<ClassReference.Handle> set = new HashSet<>();
        for (String temp : tempRes) {
            if (temp.equals(ch.getName())) {
                continue;
            }
            set.add(new ClassReference.Handle(temp));
        }
        session.close();
        return set;
    }

    public ClassReference getClassRef(ClassReference.Handle ch, Integer jarId) {
        SqlSession session = factory.openSession(true);
        ClassMapper classMapper = session.getMapper(ClassMapper.class);
        InterfaceMapper interfaceMapper = session.getMapper(InterfaceMapper.class);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        MemberMapper memberMapper = session.getMapper(MemberMapper.class);

        ArrayList<ClassResult> results = new ArrayList<>(classMapper.selectClassByClassName(ch.getName()));
        ClassResult cr = results.get(0);

        ArrayList<String> interfaces = interfaceMapper.selectInterfacesByClass(ch.getName());
        ArrayList<String> anno = annoMapper.selectAnnoByClassName(ch.getName());
        ArrayList<MemberEntity> memberEntities = memberMapper.selectMembersByClass(ch.getName());
        ArrayList<ClassReference.Member> members = new ArrayList<>();
        for (MemberEntity me : memberEntities) {
            ClassReference.Member member = new ClassReference.Member
                    (me.getMemberName(), me.getModifiers(), me.getValue(),
                            me.getMethodDesc(), me.getMethodSignature(), new ClassReference.Handle(me.getTypeClassName()));
            members.add(member);
        }

        session.close();
        return new ClassReference(
                cr.getClassName(),
                cr.getSuperClassName(),
                interfaces,
                cr.getIsInterfaceInt() == 1,
                members,
                anno,
                "none", jarId);
    }

    public int getMethodsCount() {
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        int count = methodMapper.selectCount();
        session.close();
        return count;
    }

    public ArrayList<MethodReference> getAllMethodRef(int offset) {
        int size = 100;
        SqlSession session = factory.openSession(true);
        MethodMapper methodMapper = session.getMapper(MethodMapper.class);
        AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
        ArrayList<MethodResult> results = new ArrayList<>(methodMapper.selectAllMethods(size, offset));
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        ArrayList<MethodReference> list = new ArrayList<>();
        for (MethodResult result : results) {
            MethodReference.Handle mh = new MethodReference.Handle(
                    new ClassReference.Handle(result.getClassName()),
                    result.getMethodName(),
                    result.getMethodDesc());
            ArrayList<String> ma = annoMapper.selectAnnoByClassAndMethod(
                    mh.getClassReference().getName(), mh.getName());
            MethodReference mr = new MethodReference(mh.getClassReference(),
                    mh.getName(), mh.getDesc(),
                    result.getIsStaticInt() == 1,
                    ma.stream().map(a -> new AnnoReference(a)).collect(Collectors.toSet()),
                    result.getAccessInt(), result.getLineNumber(), result.getJarName(), result.getJarId());
            list.add(mr);
        }
        session.close();
        return list;
    }

    public List<MemberEntity> getAllMembersInfo() {
        SqlSession session = factory.openSession(true);
        MemberMapper memberMapper = session.getMapper(MemberMapper.class);
        ArrayList<MemberEntity> members = memberMapper.selectMembers();
        session.close();
        ArrayList<MemberEntity> list = new ArrayList<>();
        for (MemberEntity me : members) {
            if (me.getTypeClassName().equals("java/lang/String")) {
                list.add(me);
            }
        }
        return list;
    }

    public Map<String, String> getStringMap() {
        SqlSession session = factory.openSession(true);
        StringMapper stringMapper = session.getMapper(StringMapper.class);
        List<MethodResult> res = stringMapper.selectStringInfos();
        session.close();
        Map<String, String> strMap = new HashMap<>();
        for (MethodResult m : res) {
            strMap.put(m.getClassName(), m.getStrValue());
        }
        return strMap;
    }

    public ArrayList<ResourceEntity> getResources(String path, Integer jarId, int offset, int limit) {
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        ArrayList<ResourceEntity> results = new ArrayList<>(
                resourceMapper.selectResources(path, jarId, offset, limit));
        session.close();
        return filterResources(results);
    }

    public int getResourceCount(String path, Integer jarId) {
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        int count = resourceMapper.selectCount(path, jarId);
        session.close();
        return count;
    }

    public ResourceEntity getResourceById(int rid) {
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        ResourceEntity res = resourceMapper.selectResourceById(rid);
        session.close();
        if (res != null && CommonFilterUtil.isFilteredResourcePath(res.getResourcePath())) {
            return null;
        }
        return res;
    }

    public ResourceEntity getResourceByPath(Integer jarId, String path) {       
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        ResourceEntity res = resourceMapper.selectResourceByPath(jarId, path);
        session.close();
        if (res != null && CommonFilterUtil.isFilteredResourcePath(res.getResourcePath())) {
            return null;
        }
        return res;
    }

    public ArrayList<ResourceEntity> getResourcesByPath(String path, Integer limit) {
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        ArrayList<ResourceEntity> res = new ArrayList<>(
                resourceMapper.selectResourcesByPath(path, limit));
        session.close();
        return filterResources(res);
    }

    public ArrayList<ResourceEntity> getTextResources(Integer jarId) {
        SqlSession session = factory.openSession(true);
        ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
        ArrayList<ResourceEntity> results = new ArrayList<>(
                resourceMapper.selectTextResources(jarId));
        session.close();
        return filterResources(results);
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
}
