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

import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.analyze.spring.SpringConstant;
import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.mapper.*;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.*;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.PartitionUtils;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger();
    public static int PART_SIZE = resolveBatchSize();
    private static final SqlSession session;
    private static final AtomicLong BUILD_SEQ = new AtomicLong(0);
    private static final ClassMapper classMapper;
    private static final MemberMapper memberMapper;
    private static final JarMapper jarMapper;
    private static final AnnoMapper annoMapper;
    private static final MethodMapper methodMapper;
    private static final StringMapper stringMapper;
    private static final ResourceMapper resourceMapper;
    private static final InterfaceMapper interfaceMapper;
    private static final ClassFileMapper classFileMapper;
    private static final MethodImplMapper methodImplMapper;
    private static final MethodCallMapper methodCallMapper;
    private static final CallSiteMapper callSiteMapper;
    private static final LocalVarMapper localVarMapper;
    private static final SpringControllerMapper springCMapper;
    private static final SpringInterceptorMapper springIMapper;
    private static final SpringMethodMapper springMMapper;
    private static final JavaWebMapper javaWebMapper;
    private static final DFSMapper dfsMapper;
    private static final DFSListMapper dfsListMapper;
    private static final FavMapper favMapper;
    private static final HisMapper hisMapper;
    private static final InitMapper initMapper;
    private static final SemanticCacheMapper semanticCacheMapper;

    // --inner-jar 仅解析此jar包引用的 jdk 类及其它jar中的类,但不会保存其它jar的jarId等信息
    private static final ClassReference notFoundClassReference = new ClassReference(-1, -1, null, null, null, false, null, null, "unknown", -1);
    private static final String METHOD_CALL_INSERT_SQL =
            "INSERT INTO method_call_table " +
                    "(caller_method_name, caller_method_desc, caller_class_name, caller_jar_id, " +
                    "callee_method_name, callee_method_desc, callee_class_name, callee_jar_id, op_code, " +
                    "edge_type, edge_confidence, edge_evidence) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String STRING_INSERT_SQL =
            "INSERT INTO string_table " +
                    "(method_name, method_desc, access, class_name, value, jar_name, jar_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";


    
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
        } catch (Exception ignored) {
            return defaultSize;
        }
    }

static {
        logger.info("init database");
        LogUtil.info("init database");
        SqlSessionFactory factory = SqlSessionFactoryUtil.sqlSessionFactory;
        session = factory.openSession(true);
        classMapper = session.getMapper(ClassMapper.class);
        jarMapper = session.getMapper(JarMapper.class);
        annoMapper = session.getMapper(AnnoMapper.class);
        methodMapper = session.getMapper(MethodMapper.class);
        memberMapper = session.getMapper(MemberMapper.class);
        stringMapper = session.getMapper(StringMapper.class);
        resourceMapper = session.getMapper(ResourceMapper.class);
        classFileMapper = session.getMapper(ClassFileMapper.class);
        interfaceMapper = session.getMapper(InterfaceMapper.class);
        methodCallMapper = session.getMapper(MethodCallMapper.class);
        methodImplMapper = session.getMapper(MethodImplMapper.class);
        callSiteMapper = session.getMapper(CallSiteMapper.class);
        localVarMapper = session.getMapper(LocalVarMapper.class);
        springCMapper = session.getMapper(SpringControllerMapper.class);
        springIMapper = session.getMapper(SpringInterceptorMapper.class);
        springMMapper = session.getMapper(SpringMethodMapper.class);
        javaWebMapper = session.getMapper(JavaWebMapper.class);
        dfsMapper = session.getMapper(DFSMapper.class);
        dfsListMapper = session.getMapper(DFSListMapper.class);
        favMapper = session.getMapper(FavMapper.class);
        hisMapper = session.getMapper(HisMapper.class);
        initMapper = session.getMapper(InitMapper.class);
        semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
        initMapper.createJarTable();
        initMapper.createClassTable();
        initMapper.createClassFileTable();
        initMapper.createMemberTable();
        initMapper.createMethodTable();
        initMapper.createAnnoTable();
        initMapper.createInterfaceTable();
        initMapper.createMethodCallTable();
        try {
            initMapper.addMethodCallEdgeTypeColumn();
        } catch (Throwable t) {
            logger.debug("add edge_type column fail: {}", t.toString());
        }
        try {
            initMapper.addMethodCallEdgeConfidenceColumn();
        } catch (Throwable t) {
            logger.debug("add edge_confidence column fail: {}", t.toString());
        }
        try {
            initMapper.addMethodCallEdgeEvidenceColumn();
        } catch (Throwable t) {
            logger.debug("add edge_evidence column fail: {}", t.toString());
        }
        try {
            initMapper.createMethodCallIndex();
        } catch (Throwable t) {
            logger.warn("create method_call index fail: {}", t.toString());
        }
        initMapper.createMethodImplTable();
        initMapper.createStringTable();
        try {
            initMapper.createStringFtsTable();
        } catch (Throwable t) {
            logger.warn("create string_fts fail: {}", t.toString());
        }
        try {
            initMapper.createStringIndex();
        } catch (Throwable t) {
            logger.warn("create string index fail: {}", t.toString());
        }
        initMapper.createResourceTable();
        try {
            initMapper.createResourceIndex();
        } catch (Throwable t) {
            logger.warn("create resource index fail: {}", t.toString());
        }
        initMapper.createSpringControllerTable();
        initMapper.createSpringMappingTable();
        initMapper.createSpringInterceptorTable();
        initMapper.createJavaWebTable();
        // DFS
        initMapper.createDFSResultTable();
        initMapper.createDFSResultListTable();
        // NOTE
        initMapper.createFavoriteTable();
        initMapper.createHistoryTable();
        initMapper.createCallSiteTable();
        try {
            initMapper.upgradeCallSiteTable();
        } catch (Throwable t) {
            logger.debug("upgrade call_site table skip: {}", t.toString());
        }
        initMapper.createLocalVarTable();
        try {
            initMapper.createCallSiteIndex();
        } catch (Throwable t) {
            logger.warn("create call_site index fail: {}", t.toString());
        }
        try {
            initMapper.createLocalVarIndex();
        } catch (Throwable t) {
            logger.warn("create local_var index fail: {}", t.toString());
        }
        initMapper.createLineMappingTable();
        try {
            initMapper.createLineMappingIndex();
        } catch (Throwable t) {
            logger.warn("create line_mapping index fail: {}", t.toString());
        }
        initMapper.createSemanticCacheTable();
        try {
            initMapper.createSemanticCacheIndex();
        } catch (Throwable t) {
            logger.warn("create semantic_cache index fail: {}", t.toString());
        }
        logger.info("create database finish");
        LogUtil.info("create database finish");
    }

    public static void prepareBuild() {
        BUILD_SEQ.incrementAndGet();
        applyBuildPragmas();
        clearSemanticCache();
        dropBuildIndexes();
    }

    public static void finalizeBuild() {
        createBuildIndexes();
        applyFinalizePragmas();
    }

    private static void applyBuildPragmas() {
        executeSql("PRAGMA journal_mode=WAL");
        executeSql("PRAGMA synchronous=NORMAL");
        executeSql("PRAGMA temp_store=MEMORY");
        executeSql("PRAGMA cache_size=-64000");
    }

    private static void applyFinalizePragmas() {
        executeSql("PRAGMA wal_checkpoint(TRUNCATE)");
        executeSql("PRAGMA optimize");
    }

    private static void dropBuildIndexes() {
        executeSql("DROP INDEX IF EXISTS idx_string_value_nocase");
        executeSql("DROP INDEX IF EXISTS idx_resource_path");
        executeSql("DROP INDEX IF EXISTS idx_resource_jar_path");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller_idx");
        executeSql("DROP INDEX IF EXISTS idx_call_site_callee");
        executeSql("DROP INDEX IF EXISTS idx_local_var_method");
        executeSql("DROP INDEX IF EXISTS idx_semantic_cache_type");
    }

    private static void createBuildIndexes() {
        try {
            initMapper.createStringIndex();
        } catch (Throwable t) {
            logger.warn("create string index fail: {}", t.toString());
        }
        try {
            initMapper.createResourceIndex();
        } catch (Throwable t) {
            logger.warn("create resource index fail: {}", t.toString());
        }
        try {
            initMapper.createCallSiteIndex();
        } catch (Throwable t) {
            logger.warn("create call_site index fail: {}", t.toString());
        }
        try {
            initMapper.createLocalVarIndex();
        } catch (Throwable t) {
            logger.warn("create local_var index fail: {}", t.toString());
        }
        try {
            initMapper.createSemanticCacheIndex();
        } catch (Throwable t) {
            logger.warn("create semantic_cache index fail: {}", t.toString());
        }
    }

    private static void executeSql(String sql) {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            logger.debug("exec sql fail: {}", e.toString());
        }
    }

    public static void saveDFS(DFSResultEntity dfsResultEntity) {
        int a = dfsMapper.insertDFSResult(dfsResultEntity);
        if (a < 1) {
            logger.warn("save dfs error");
        }
    }

    public static void saveDFSList(DFSResultListEntity dfsResultListEntity) {
        int a = dfsListMapper.insertDFSResultList(dfsResultListEntity);
        if (a < 1) {
            logger.warn("save dfs list error");
        }
    }

    public static void saveJar(String jarPath) {
        JarEntity en = new JarEntity();
        en.setJarAbsPath(jarPath);
        if (OSUtil.isWindows()) {
            String[] temp = jarPath.split("\\\\");
            en.setJarName(temp[temp.length - 1]);
        } else {
            String[] temp = jarPath.split("/");
            en.setJarName(temp[temp.length - 1]);
        }
        List<JarEntity> js = new ArrayList<>();
        js.add(en);
        int i = jarMapper.insertJar(js);
        if (i != 0) {
            logger.debug("save jar finish");
        }
    }

    public static JarEntity getJarId(String jarPath) {
        List<JarEntity> jarEntities = jarMapper.selectJarByAbsPath(jarPath);
        if (jarEntities == null || jarEntities.isEmpty()) {
            return null;
        }
        Map<String, JarEntity> distinct = new LinkedHashMap<>();
        for (JarEntity jarEntity : jarEntities) {
            distinct.putIfAbsent(jarEntity.getJarName(), jarEntity);
        }
        return distinct.values().stream().findFirst().orElse(null);
    }

    public static void saveClassFiles(Set<ClassFileEntity> classFileList) {
        logger.info("total class file: {}", classFileList.size());
        List<ClassFileEntity> list = new ArrayList<>();
        for (ClassFileEntity classFile : classFileList) {
            classFile.setPathStr(classFile.getPath().toAbsolutePath().toString());
            if (classFile.getJarId() == null) {
                classFile.setJarId(-1);
            }
            list.add(classFile);
        }
        List<List<ClassFileEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<ClassFileEntity> data : partition) {
            int a = classFileMapper.insertClassFile(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save class file finish");
    }

    public static void saveClassInfo(Set<ClassReference> discoveredClasses) {
        logger.info("total class: {}", discoveredClasses.size());
        MainForm.getInstance().getTotalClassVal().setText(String.valueOf(discoveredClasses.size()));
        List<ClassEntity> list = new ArrayList<>();
        for (ClassReference reference : discoveredClasses) {
            ClassEntity classEntity = new ClassEntity();
            classEntity.setJarName(reference.getJarName());
            classEntity.setJarId(reference.getJarId());
            classEntity.setVersion(reference.getVersion());
            classEntity.setAccess(reference.getAccess());
            classEntity.setClassName(reference.getName());
            classEntity.setSuperClassName(reference.getSuperClass());
            classEntity.setInterface(reference.isInterface());
            list.add(classEntity);
        }
        List<List<ClassEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<ClassEntity> data : partition) {
            int a = classMapper.insertClass(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save class finish");

        List<MemberEntity> mList = new ArrayList<>();
        List<AnnoEntity> aList = new ArrayList<>();
        List<InterfaceEntity> iList = new ArrayList<>();
        for (ClassReference reference : discoveredClasses) {
            for (ClassReference.Member member : reference.getMembers()) {
                MemberEntity memberEntity = new MemberEntity();
                memberEntity.setMemberName(member.getName());
                memberEntity.setModifiers(member.getModifiers());
                memberEntity.setValue(member.getValue());
                memberEntity.setTypeClassName(member.getType().getName());
                memberEntity.setClassName(reference.getName());
                memberEntity.setMethodDesc(member.getDesc());
                memberEntity.setMethodSignature(member.getSignature());
                memberEntity.setJarId(reference.getJarId());
                mList.add(memberEntity);
            }
            for (AnnoReference anno : reference.getAnnotations()) {
                AnnoEntity annoEntity = new AnnoEntity();
                annoEntity.setAnnoName(anno.getAnnoName());
                annoEntity.setVisible(anno.getVisible() ? 1 : 0);
                annoEntity.setClassName(reference.getName());
                annoEntity.setJarId(reference.getJarId());
                annoEntity.setParameter(anno.getParameter());
                aList.add(annoEntity);
            }
            for (String inter : reference.getInterfaces()) {
                InterfaceEntity interfaceEntity = new InterfaceEntity();
                interfaceEntity.setClassName(reference.getName());
                interfaceEntity.setInterfaceName(inter);
                interfaceEntity.setJarId(reference.getJarId());
                iList.add(interfaceEntity);
            }
        }
        List<List<MemberEntity>> mPartition = PartitionUtils.partition(mList, PART_SIZE);
        for (List<MemberEntity> data : mPartition) {
            int a = memberMapper.insertMember(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save member success");

        saveAnno(aList);
        logger.info("save class anno success");

        List<List<InterfaceEntity>> iPartition = PartitionUtils.partition(iList, PART_SIZE);
        for (List<InterfaceEntity> data : iPartition) {
            int a = interfaceMapper.insertInterface(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save interface success");
    }

    private static void saveAnno(List<AnnoEntity> aList) {
        List<List<AnnoEntity>> aPartition = PartitionUtils.partition(aList, PART_SIZE);
        for (List<AnnoEntity> data : aPartition) {
            int a = annoMapper.insertAnno(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
    }

    public static void saveMethods(Set<MethodReference> discoveredMethods) {
        logger.info("total method: {}", discoveredMethods.size());
        MainForm.getInstance().getTotalMethodVal().setText(String.valueOf(discoveredMethods.size()));
        List<MethodEntity> mList = new ArrayList<>();
        List<AnnoEntity> aList = new ArrayList<>();
        for (MethodReference reference : discoveredMethods) {
            MethodEntity methodEntity = new MethodEntity();
            methodEntity.setMethodName(reference.getName());
            methodEntity.setMethodDesc(reference.getDesc());
            methodEntity.setClassName(reference.getClassReference().getName());
            methodEntity.setStatic(reference.isStatic());
            methodEntity.setAccess(reference.getAccess());
            methodEntity.setLineNumber(reference.getLineNumber());
            methodEntity.setJarId(reference.getJarId());
            mList.add(methodEntity);
            for (AnnoReference anno : reference.getAnnotations()) {
                AnnoEntity annoEntity = new AnnoEntity();
                annoEntity.setAnnoName(anno.getAnnoName());
                annoEntity.setMethodName(reference.getName());
                annoEntity.setClassName(reference.getClassReference().getName());
                annoEntity.setJarId(reference.getJarId());
                annoEntity.setVisible(anno.getVisible() ? 1 : 0);
                annoEntity.setParameter(anno.getParameter());
                aList.add(annoEntity);
            }
        }
        List<List<MethodEntity>> mPartition = PartitionUtils.partition(mList, PART_SIZE);
        for (List<MethodEntity> data : mPartition) {
            int a = methodMapper.insertMethod(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save method success");

        saveAnno(aList);
        logger.info("save method anno success");
    }

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            logger.info("method call map is empty");
            return;
        }
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;
        Connection connection = null;
        boolean autoCommit = true;
        try {
            connection = session.getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(METHOD_CALL_INSERT_SQL)) {
                for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> call :
                        methodCalls.entrySet()) {
                    MethodReference.Handle caller = call.getKey();
                    HashSet<MethodReference.Handle> callee = call.getValue();
                    ClassReference callerClass = AnalyzeEnv.classMap.get(caller.getClassReference());
                    int callerJarId = callerClass == null ? -1 : callerClass.getJarId();

                    for (MethodReference.Handle mh : callee) {
                        MethodCallMeta meta = AnalyzeEnv.methodCallMeta.get(MethodCallKey.of(caller, mh));
                        if (meta == null) {
                            meta = fallbackEdgeMeta(mh);
                        }
                        ClassReference calleeClass = AnalyzeEnv.classMap.getOrDefault(mh.getClassReference(), notFoundClassReference);
                        String edgeEvidence = meta.getEvidence();
                        if (edgeEvidence == null) {
                            edgeEvidence = "";
                        }
                        ps.setString(1, caller.getName());
                        ps.setString(2, caller.getDesc());
                        ps.setString(3, caller.getClassReference().getName());
                        ps.setInt(4, callerJarId);
                        ps.setString(5, mh.getName());
                        ps.setString(6, mh.getDesc());
                        ps.setString(7, mh.getClassReference().getName());
                        ps.setInt(8, calleeClass.getJarId());
                        ps.setInt(9, mh.getOpcode() == null ? -1 : mh.getOpcode());
                        ps.setString(10, meta.getType());
                        ps.setString(11, meta.getConfidence());
                        ps.setString(12, edgeEvidence);
                        ps.addBatch();
                        batchCount++;
                        total++;
                        if (batchCount >= batchSize) {
                            ps.executeBatch();
                            connection.commit();
                            batchCount = 0;
                        }
                    }
                }
                if (batchCount > 0) {
                    ps.executeBatch();
                    connection.commit();
                }
            }
            logger.info("save method call success: {}", total);
        } catch (SQLException e) {
            logger.warn("save method call error: {}", e.toString());
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.warn("method call rollback error");
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.warn("restore auto commit error");
                }
            }
        }
    }

    private static MethodCallMeta fallbackEdgeMeta(MethodReference.Handle callee) {
        if (callee != null && callee.getOpcode() != null && callee.getOpcode() > 0) {
            return new MethodCallMeta(MethodCallMeta.TYPE_DIRECT, MethodCallMeta.CONF_HIGH);
        }
        return new MethodCallMeta(MethodCallMeta.TYPE_UNKNOWN, MethodCallMeta.CONF_LOW);
    }

    public static void saveImpls(Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap) {
        List<MethodImplEntity> mList = new ArrayList<>();
        for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> call :
                implMap.entrySet()) {
            MethodReference.Handle method = call.getKey();
            Set<MethodReference.Handle> impls = call.getValue();
            for (MethodReference.Handle mh : impls) {
                MethodImplEntity impl = new MethodImplEntity();
                impl.setImplClassName(mh.getClassReference().getName());
                impl.setClassName(method.getClassReference().getName());
                impl.setMethodName(mh.getName());
                impl.setMethodDesc(mh.getDesc());
                impl.setClassJarId(AnalyzeEnv.classMap.get(method.getClassReference()).getJarId());
                impl.setImplClassJarId(AnalyzeEnv.classMap.get(mh.getClassReference()).getJarId());
                mList.add(impl);
            }
        }
        List<List<MethodImplEntity>> mPartition = PartitionUtils.partition(mList, PART_SIZE);
        for (List<MethodImplEntity> data : mPartition) {
            int a = methodImplMapper.insertMethodImpl(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
        logger.info("save method impl success");
    }

    public static void saveStrMap(Map<MethodReference.Handle, List<String>> strMap,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap) {
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;
        Connection connection = null;
        boolean autoCommit = true;

        logger.info("save str map length: {}", strMap.size());
        try {
            connection = session.getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(STRING_INSERT_SQL)) {
                for (Map.Entry<MethodReference.Handle, List<String>> strEntry : strMap.entrySet()) {
                    MethodReference.Handle method = strEntry.getKey();
                    List<String> strList = strEntry.getValue();
                    MethodReference mr = AnalyzeEnv.methodMap.get(method);
                    ClassReference cr = AnalyzeEnv.classMap.get(mr.getClassReference());
                    for (String s : strList) {
                        ps.setString(1, mr.getName());
                        ps.setString(2, mr.getDesc());
                        ps.setInt(3, mr.getAccess());
                        ps.setString(4, cr.getName());
                        ps.setString(5, s);
                        ps.setString(6, cr.getJarName());
                        ps.setInt(7, cr.getJarId());
                        ps.addBatch();
                        batchCount++;
                        total++;
                        if (batchCount >= batchSize) {
                            ps.executeBatch();
                            connection.commit();
                            batchCount = 0;
                        }
                    }
                }

                // 2024/12/05 处理注解部分的字符串搜索
                logger.info("save string anno map length: {}", stringAnnoMap.size());
                for (Map.Entry<MethodReference.Handle, List<String>> strEntry : stringAnnoMap.entrySet()) {
                    MethodReference.Handle method = strEntry.getKey();
                    List<String> strList = strEntry.getValue();
                    MethodReference mr = AnalyzeEnv.methodMap.get(method);
                    ClassReference cr = AnalyzeEnv.classMap.get(mr.getClassReference());
                    for (String s : strList) {
                        ps.setString(1, mr.getName());
                        ps.setString(2, mr.getDesc());
                        ps.setInt(3, mr.getAccess());
                        ps.setString(4, cr.getName());
                        ps.setString(5, s);
                        ps.setString(6, cr.getJarName());
                        ps.setInt(7, cr.getJarId());
                        ps.addBatch();
                        batchCount++;
                        total++;
                        if (batchCount >= batchSize) {
                            ps.executeBatch();
                            connection.commit();
                            batchCount = 0;
                        }
                    }
                }
                if (batchCount > 0) {
                    ps.executeBatch();
                    connection.commit();
                }
            }
        } catch (SQLException e) {
            logger.warn("save string error: {}", e.toString());
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.warn("string rollback error");
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.warn("restore auto commit error");
                }
            }
        }
        try {
            stringMapper.rebuildStringFts();
        } catch (Throwable t) {
            logger.warn("rebuild string_fts fail: {}", t.toString());
        }
        logger.info("save all string success: {}", total);
    }

    public static void saveResources(List<ResourceEntity> resources) {
        if (resources == null || resources.isEmpty()) {
            logger.info("resource list is empty");
            return;
        }
        List<List<ResourceEntity>> partition = PartitionUtils.partition(resources, PART_SIZE);
        for (List<ResourceEntity> data : partition) {
            int a = resourceMapper.insertResources(data);
            if (a == 0) {
                logger.warn("save resource error");
            }
        }
        logger.info("save resources success");
    }

    public static void saveCallSites(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            logger.info("call site list is empty");
            return;
        }
        List<List<CallSiteEntity>> partition = PartitionUtils.partition(callSites, PART_SIZE);
        for (List<CallSiteEntity> data : partition) {
            int a = callSiteMapper.insertCallSites(data);
            if (a == 0) {
                logger.warn("save call site error");
            }
        }
        logger.info("save call sites success");
    }

    public static void saveLocalVars(List<LocalVarEntity> localVars) {
        if (localVars == null || localVars.isEmpty()) {
            logger.info("local var list is empty");
            return;
        }
        List<List<LocalVarEntity>> partition = PartitionUtils.partition(localVars, PART_SIZE);
        for (List<LocalVarEntity> data : partition) {
            int a = localVarMapper.insertLocalVars(data);
            if (a == 0) {
                logger.warn("save local var error");
            }
        }
        logger.info("save local vars success");
    }

    public static void saveSpringController(ArrayList<SpringController> controllers) {
        List<SpringControllerEntity> cList = new ArrayList<>();
        List<SpringMethodEntity> mList = new ArrayList<>();
        // 2025/06/26 处理 SPRING 分析错误时报错
        if (controllers == null || controllers.isEmpty()) {
            // 2025/08/05 SPRING 数据为空时不应该使用 WARN 日志
            logger.info("SPRING CONTROLLER 分析错误数据为空");
            return;
        }
        try {
            for (SpringController controller : controllers) {
                SpringControllerEntity ce = new SpringControllerEntity();
                ce.setClassName(controller.getClassName().getName());
                ce.setJarId(controller.getClassReference().getJarId());
                cList.add(ce);
                for (SpringMapping mapping : controller.getMappings()) {
                    SpringMethodEntity me = new SpringMethodEntity();
                    me.setClassName(controller.getClassName().getName());
                    me.setJarId(controller.getClassReference().getJarId());
                    me.setPath(mapping.getPath());
                    me.setMethodName(mapping.getMethodName().getName());
                    me.setMethodDesc(mapping.getMethodName().getDesc());
                    if (mapping.getPathRestful() != null && !mapping.getPathRestful().isEmpty()) {
                        me.setRestfulType(mapping.getPathRestful());
                        initPath(mapping, me);
                    } else {
                        for (AnnoReference annotation : mapping.getMethodReference().getAnnotations()) {
                            if (annotation.getAnnoName().startsWith(SpringConstant.ANNO_PREFIX)) {
                                me.setRestfulType(annotation.getAnnoName()
                                        .replace(SpringConstant.ANNO_PREFIX, "")
                                        .replace(SpringConstant.MappingAnno, "")
                                        .replace(";", " "));
                                initPath(mapping, me);
                            }
                        }
                    }
                    mList.add(me);
                }
            }
            List<List<SpringControllerEntity>> cPartition = PartitionUtils.partition(cList, PART_SIZE);
            for (List<SpringControllerEntity> data : cPartition) {
                int a = springCMapper.insertControllers(data);
                if (a == 0) {
                    logger.warn("save error");
                }
            }
            List<List<SpringMethodEntity>> mPartition = PartitionUtils.partition(mList, PART_SIZE);

            for (List<SpringMethodEntity> data : mPartition) {

                // FIX PATH NOT NULL BUG
                List<SpringMethodEntity> newList = new ArrayList<>();
                for (SpringMethodEntity entity : data) {
                    if (entity.getPath() == null || entity.getPath().isEmpty()) {
                        entity.setPath("none");
                    }
                    newList.add(entity);
                }

                int a = springMMapper.insertMappings(newList);
                if (a == 0) {
                    logger.warn("save error");
                }
            }
        } catch (Throwable t) {
            logger.warn("SPRING CONTROLLER 分析错误 请提 ISSUE 解决");
        }
        logger.info("save all spring data success");
    }

    private static void initPath(SpringMapping mapping, SpringMethodEntity me) {
        if (StrUtil.isBlank(mapping.getPath()) &&
                StrUtil.isNotBlank(mapping.getController().getBasePath())) {
            me.setPath(mapping.getController().getBasePath());
        }
        if (StrUtil.isNotBlank(mapping.getPath()) && mapping.getPath().endsWith("/")) {
            me.setPath(mapping.getPath().substring(0, mapping.getPath().length() - 1));
        }
    }

    public static void saveSpringInterceptor(ArrayList<String> interceptors) {
        List<SpringInterceptorEntity> list = new ArrayList<>();
        for (String interceptor : interceptors) {
            SpringInterceptorEntity ce = new SpringInterceptorEntity();
            ce.setClassName(interceptor);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(interceptor), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<SpringInterceptorEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<SpringInterceptorEntity> data : partition) {
            int a = springIMapper.insertInterceptors(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
    }

    public static void saveServlets(ArrayList<String> servlets) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String servlet : servlets) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(servlet);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(servlet), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<JavaWebEntity> data : partition) {
            int a = javaWebMapper.insertServlets(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
    }

    public static void saveFilters(ArrayList<String> filters) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String filter : filters) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(filter);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(filter), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<JavaWebEntity> data : partition) {
            int a = javaWebMapper.insertFilters(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
    }

    public static void saveListeners(ArrayList<String> listeners) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String listener : listeners) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(listener);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(listener), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        for (List<JavaWebEntity> data : partition) {
            int a = javaWebMapper.insertListeners(data);
            if (a == 0) {
                logger.warn("save error");
            }
        }
    }

    public static void cleanFav() {
        favMapper.cleanFav();
    }

    public static void cleanFavItem(MethodResult m) {
        favMapper.cleanFavItem(m);
    }

    public static void addFav(MethodResult m) {
        favMapper.addFav(m);
    }

    public static void insertHistory(MethodResult m) {
        hisMapper.insertHistory(m);
    }

    public static void cleanHistory() {
        hisMapper.cleanHistory();
    }

    public static ArrayList<MethodResult> getAllFavMethods() {
        return favMapper.getAllFavMethods();
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        return hisMapper.getAllHisMethods();
    }

    public static long getBuildSeq() {
        return BUILD_SEQ.get();
    }

    public static String getSemanticCacheValue(String cacheKey, String cacheType) {
        if (semanticCacheMapper == null || cacheKey == null || cacheType == null) {
            return null;
        }
        try {
            return semanticCacheMapper.selectValue(cacheKey, cacheType);
        } catch (Throwable t) {
            logger.debug("semantic cache query fail: {}", t.toString());
            return null;
        }
    }

    public static void putSemanticCacheValue(String cacheKey, String cacheType, String cacheValue) {
        if (semanticCacheMapper == null || cacheKey == null || cacheType == null || cacheValue == null) {
            return;
        }
        try {
            semanticCacheMapper.upsert(cacheKey, cacheType, cacheValue);
        } catch (Throwable t) {
            logger.debug("semantic cache write fail: {}", t.toString());
        }
    }

    public static void clearSemanticCacheType(String cacheType) {
        if (semanticCacheMapper == null || cacheType == null) {
            return;
        }
        try {
            semanticCacheMapper.deleteByType(cacheType);
        } catch (Throwable t) {
            logger.debug("semantic cache clear fail: {}", t.toString());
        }
    }

    public static void clearSemanticCache() {
        if (semanticCacheMapper == null) {
            return;
        }
        try {
            semanticCacheMapper.deleteAll();
        } catch (Throwable t) {
            logger.debug("semantic cache clear all fail: {}", t.toString());
        }
    }
}
