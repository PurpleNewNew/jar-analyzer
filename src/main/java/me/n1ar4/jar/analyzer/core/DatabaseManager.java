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
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.PartitionUtils;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger();
    public static int PART_SIZE = resolveBatchSize();
    private static final SqlSessionFactory factory = SqlSessionFactoryUtil.sqlSessionFactory;
    private static final AtomicLong BUILD_SEQ = new AtomicLong(0);
    private static final AtomicBoolean BUILDING = new AtomicBoolean(false);

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
        } catch (NumberFormatException ex) {
            logger.debug("invalid db batch size: {}", raw);
            return defaultSize;
        }
    }

    static {
        logger.info("init database");
        try (SqlSession session = factory.openSession(true)) {
            InitMapper initMapper = session.getMapper(InitMapper.class);
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
            } catch (Exception ex) {
                logger.debug("add edge_type column fail: {}", ex.toString());
            }
            try {
                initMapper.addMethodCallEdgeConfidenceColumn();
            } catch (Exception ex) {
                logger.debug("add edge_confidence column fail: {}", ex.toString());
            }
            try {
                initMapper.addMethodCallEdgeEvidenceColumn();
            } catch (Exception ex) {
                logger.debug("add edge_evidence column fail: {}", ex.toString());
            }
            try {
                initMapper.createMethodCallIndex();
            } catch (Exception ex) {
                logger.warn("create method_call index fail: {}", ex.toString());
            }
            initMapper.createMethodImplTable();
            initMapper.createStringTable();
            try {
                initMapper.createStringFtsTable();
            } catch (Exception ex) {
                logger.warn("create string_fts fail: {}", ex.toString());
            }
            try {
                initMapper.createStringIndex();
            } catch (Exception ex) {
                logger.warn("create string index fail: {}", ex.toString());
            }
            initMapper.createResourceTable();
            try {
                initMapper.createResourceIndex();
            } catch (Exception ex) {
                logger.warn("create resource index fail: {}", ex.toString());
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
            } catch (Exception ex) {
                logger.debug("upgrade call_site table skip: {}", ex.toString());
            }
            initMapper.createLocalVarTable();
            try {
                initMapper.createCallSiteIndex();
            } catch (Exception ex) {
                logger.warn("create call_site index fail: {}", ex.toString());
            }
            try {
                initMapper.createLocalVarIndex();
            } catch (Exception ex) {
                logger.warn("create local_var index fail: {}", ex.toString());
            }
            initMapper.createLineMappingTable();
            try {
                initMapper.createLineMappingIndex();
            } catch (Exception ex) {
                logger.warn("create line_mapping index fail: {}", ex.toString());
            }
            initMapper.createSemanticCacheTable();
            try {
                initMapper.createSemanticCacheIndex();
            } catch (Exception ex) {
                logger.warn("create semantic_cache index fail: {}", ex.toString());
            }
            // report MCP (n8n agent)
            try {
                initMapper.createVulReportTable();
            } catch (Exception ex) {
                logger.warn("create vul_report table fail: {}", ex.toString());
            }
        }
        logger.info("create database finish");
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

    public static void clearAllData() {
        String[] tables = new String[]{
                "jar_table",
                "class_table",
                "class_file_table",
                "member_table",
                "method_table",
                "anno_table",
                "interface_table",
                "method_call_table",
                "method_impl_table",
                "string_table",
                "string_fts",
                "resource_table",
                "spring_controller_table",
                "spring_method_table",
                "spring_interceptor_table",
                "java_web_table",
                "dfs_result_table",
                "dfs_result_list_table",
                "note_favorite_table",
                "note_history_table",
                "bytecode_call_site_table",
                "bytecode_local_var_table",
                "line_mapping_table",
                "semantic_cache_table"
        };
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    for (String table : tables) {
                        statement.execute("DELETE FROM " + table);
                    }
                    try {
                        statement.execute("DELETE FROM sqlite_sequence");
                    } catch (SQLException ignored) {
                        logger.debug("clear sqlite_sequence fail: {}", ignored.toString());
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                logger.warn("clear db data error: {}", e.toString());
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.warn("clear db rollback error");
                }
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.warn("restore auto commit error");
                }
            }
        } catch (SQLException e) {
            logger.warn("clear db data error: {}", e.toString());
        }
    }

    private static void applyBuildPragmas() {
        executeSql("PRAGMA journal_mode=WAL");
        executeSql("PRAGMA synchronous=NORMAL");
        executeSql("PRAGMA temp_store=MEMORY");
        executeSql("PRAGMA wal_autocheckpoint=" + resolvePragmaInt("jar-analyzer.db.wal_autocheckpoint", 10000, 0, 1_000_000));
        executeSql("PRAGMA cache_size=" + resolvePragmaInt("jar-analyzer.db.cache_size", -64000, -500_000, 500_000));
        long mmapSize = resolvePragmaLong("jar-analyzer.db.mmap_size", 0L, 0L, 1_073_741_824L);
        if (mmapSize > 0L) {
            executeSql("PRAGMA mmap_size=" + mmapSize);
        }
    }

    private static void applyFinalizePragmas() {
        executeSql("PRAGMA wal_checkpoint(TRUNCATE)");
        executeSql("PRAGMA optimize");
    }

    private static void dropBuildIndexes() {
        executeSql("DROP INDEX IF EXISTS idx_method_call_callee");
        executeSql("DROP INDEX IF EXISTS idx_method_call_caller");
        executeSql("DROP INDEX IF EXISTS idx_method_call_edge");
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
        try (SqlSession session = factory.openSession(true)) {
            InitMapper initMapper = session.getMapper(InitMapper.class);
            try {
                initMapper.createMethodCallIndex();
            } catch (Exception ex) {
                logger.warn("create method_call index fail: {}", ex.toString());
            }
            try {
                initMapper.createStringIndex();
            } catch (Exception ex) {
                logger.warn("create string index fail: {}", ex.toString());
            }
            try {
                initMapper.createResourceIndex();
            } catch (Exception ex) {
                logger.warn("create resource index fail: {}", ex.toString());
            }
            try {
                initMapper.createCallSiteIndex();
            } catch (Exception ex) {
                logger.warn("create call_site index fail: {}", ex.toString());
            }
            try {
                initMapper.createLocalVarIndex();
            } catch (Exception ex) {
                logger.warn("create local_var index fail: {}", ex.toString());
            }
            try {
                initMapper.createSemanticCacheIndex();
            } catch (Exception ex) {
                logger.warn("create semantic_cache index fail: {}", ex.toString());
            }
        }
    }

    private static int resolvePragmaInt(String prop, int defaultValue, int min, int max) {
        String raw = prop == null ? null : System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return clampInt(defaultValue, min, max);
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return clampInt(value, min, max);
        } catch (NumberFormatException ex) {
            logger.debug("invalid int property {}={}", prop, raw);
            return clampInt(defaultValue, min, max);
        }
    }

    private static long resolvePragmaLong(String prop, long defaultValue, long min, long max) {
        String raw = prop == null ? null : System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return clampLong(defaultValue, min, max);
        }
        try {
            long value = Long.parseLong(raw.trim());
            return clampLong(value, min, max);
        } catch (NumberFormatException ex) {
            logger.debug("invalid long property {}={}", prop, raw);
            return clampLong(defaultValue, min, max);
        }
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static long clampLong(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void executeSql(String sql) {
        try (SqlSession session = factory.openSession(true);
             Statement statement = session.getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            logger.debug("exec sql fail: {}", e.toString());
        }
    }

    public static void saveDFS(DFSResultEntity dfsResultEntity) {
        try (SqlSession session = factory.openSession(true)) {
            DFSMapper dfsMapper = session.getMapper(DFSMapper.class);
            int a = dfsMapper.insertDFSResult(dfsResultEntity);
            if (a < 1) {
                logger.warn("save dfs error");
            }
        }
    }

    public static void saveDFSList(DFSResultListEntity dfsResultListEntity) {
        try (SqlSession session = factory.openSession(true)) {
            DFSListMapper dfsListMapper = session.getMapper(DFSListMapper.class);
            int a = dfsListMapper.insertDFSResultList(dfsResultListEntity);
            if (a < 1) {
                logger.warn("save dfs list error");
            }
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
        try (SqlSession session = factory.openSession(true)) {
            JarMapper jarMapper = session.getMapper(JarMapper.class);
            int i = jarMapper.insertJar(js);
            if (i != 0) {
                logger.debug("save jar finish");
            }
        }
    }

    public static JarEntity getJarId(String jarPath) {
        try (SqlSession session = factory.openSession(true)) {
            JarMapper jarMapper = session.getMapper(JarMapper.class);
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
        try (SqlSession session = factory.openSession(false)) {
            ClassFileMapper classFileMapper = session.getMapper(ClassFileMapper.class);
            try {
                for (List<ClassFileEntity> data : partition) {
                    int a = classFileMapper.insertClassFile(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save class file error: {}", e.toString());
            }
        }
        logger.info("save class file finish");
    }

    public static void saveClassInfo(Set<ClassReference> discoveredClasses) {
        logger.info("total class: {}", discoveredClasses.size());
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
        try (SqlSession session = factory.openSession(false)) {
            ClassMapper classMapper = session.getMapper(ClassMapper.class);
            MemberMapper memberMapper = session.getMapper(MemberMapper.class);
            AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
            InterfaceMapper interfaceMapper = session.getMapper(InterfaceMapper.class);
            try {
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

                saveAnno(annoMapper, aList);
                logger.info("save class anno success");

                List<List<InterfaceEntity>> iPartition = PartitionUtils.partition(iList, PART_SIZE);
                for (List<InterfaceEntity> data : iPartition) {
                    int a = interfaceMapper.insertInterface(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                logger.info("save interface success");
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save class info error: {}", e.toString());
            }
        }
    }

    private static void saveAnno(AnnoMapper annoMapper, List<AnnoEntity> aList) {
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
        try (SqlSession session = factory.openSession(false)) {
            MethodMapper methodMapper = session.getMapper(MethodMapper.class);
            AnnoMapper annoMapper = session.getMapper(AnnoMapper.class);
            try {
                for (List<MethodEntity> data : mPartition) {
                    int a = methodMapper.insertMethod(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                logger.info("save method success");

                saveAnno(annoMapper, aList);
                logger.info("save method anno success");
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save method error: {}", e.toString());
            }
        }
    }

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
                                       Map<ClassReference.Handle, ClassReference> classMap,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            logger.info("method call map is empty");
            return;
        }
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(METHOD_CALL_INSERT_SQL)) {
                    for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> call :
                            methodCalls.entrySet()) {
                        MethodReference.Handle caller = call.getKey();
                        HashSet<MethodReference.Handle> callee = call.getValue();
                        ClassReference callerClass = classMap == null ? null : classMap.get(caller.getClassReference());
                        int callerJarId = callerClass == null ? -1 : callerClass.getJarId();

                        for (MethodReference.Handle mh : callee) {
                            MethodCallMeta meta = methodCallMeta == null ? null : methodCallMeta.get(MethodCallKey.of(caller, mh));
                            if (meta == null) {
                                meta = fallbackEdgeMeta(mh);
                            }
                            ClassReference calleeClass = classMap == null
                                    ? notFoundClassReference
                                    : classMap.getOrDefault(mh.getClassReference(), notFoundClassReference);
                            String edgeEvidence = meta.getEvidence();
                            if (edgeEvidence == null) {
                                edgeEvidence = "";
                            }
                            int opCode = meta.getBestOpcode();
                            if (opCode <= 0) {
                                Integer legacy = mh.getOpcode();
                                opCode = legacy == null ? -1 : legacy;
                            }
                            ps.setString(1, caller.getName());
                            ps.setString(2, caller.getDesc());
                            ps.setString(3, caller.getClassReference().getName());
                            ps.setInt(4, callerJarId);
                            ps.setString(5, mh.getName());
                            ps.setString(6, mh.getDesc());
                            ps.setString(7, mh.getClassReference().getName());
                            ps.setInt(8, calleeClass.getJarId());
                            ps.setInt(9, opCode);
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
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.warn("method call rollback error");
                }
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.warn("restore auto commit error");
                }
            }
        } catch (SQLException e) {
            logger.warn("save method call error: {}", e.toString());
        }
    }

    private static MethodCallMeta fallbackEdgeMeta(MethodReference.Handle callee) {
        Integer opcode = callee == null ? null : callee.getOpcode();
        if (callee != null && callee.getOpcode() != null && callee.getOpcode() > 0) {
            MethodCallMeta meta = new MethodCallMeta(MethodCallMeta.TYPE_DIRECT, MethodCallMeta.CONF_HIGH);
            meta.updateBestOpcode(opcode);
            return meta;
        }
        MethodCallMeta meta = new MethodCallMeta(MethodCallMeta.TYPE_UNKNOWN, MethodCallMeta.CONF_LOW);
        meta.updateBestOpcode(opcode);
        return meta;
    }

    public static void saveImpls(Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap,
                                 Map<ClassReference.Handle, ClassReference> classMap) {
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
                ClassReference owner = classMap == null ? null : classMap.get(method.getClassReference());
                ClassReference implOwner = classMap == null ? null : classMap.get(mh.getClassReference());
                impl.setClassJarId(owner == null ? -1 : owner.getJarId());
                impl.setImplClassJarId(implOwner == null ? -1 : implOwner.getJarId());
                mList.add(impl);
            }
        }
        List<List<MethodImplEntity>> mPartition = PartitionUtils.partition(mList, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            MethodImplMapper methodImplMapper = session.getMapper(MethodImplMapper.class);
            try {
                for (List<MethodImplEntity> data : mPartition) {
                    int a = methodImplMapper.insertMethodImpl(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save method impl error: {}", e.toString());
            }
        }
        logger.info("save method impl success");
    }

    public static void saveStrMap(Map<MethodReference.Handle, List<String>> strMap,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap,
                                  Map<MethodReference.Handle, MethodReference> methodMap,
                                  Map<ClassReference.Handle, ClassReference> classMap) {
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;

        logger.info("save str map length: {}", strMap.size());
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(STRING_INSERT_SQL)) {
                    for (Map.Entry<MethodReference.Handle, List<String>> strEntry : strMap.entrySet()) {
                        MethodReference.Handle method = strEntry.getKey();
                        List<String> strList = strEntry.getValue();
                        MethodReference mr = methodMap == null ? null : methodMap.get(method);
                        ClassReference cr = mr == null || classMap == null ? null : classMap.get(mr.getClassReference());
                        if (mr == null || cr == null) {
                            continue;
                        }
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
                        MethodReference mr = methodMap == null ? null : methodMap.get(method);
                        ClassReference cr = mr == null || classMap == null ? null : classMap.get(mr.getClassReference());
                        if (mr == null || cr == null) {
                            continue;
                        }
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
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.warn("string rollback error");
                }
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.warn("restore auto commit error");
                }
            }
        } catch (SQLException e) {
            logger.warn("save string error: {}", e.toString());
        }
        try (SqlSession session = factory.openSession(true)) {
            StringMapper stringMapper = session.getMapper(StringMapper.class);
            stringMapper.rebuildStringFts();
        } catch (Exception ex) {
            logger.warn("rebuild string_fts fail: {}", ex.toString());
        }
        logger.info("save all string success: {}", total);
    }

    public static void saveResources(List<ResourceEntity> resources) {
        if (resources == null || resources.isEmpty()) {
            logger.info("resource list is empty");
            return;
        }
        List<List<ResourceEntity>> partition = PartitionUtils.partition(resources, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
            try {
                for (List<ResourceEntity> data : partition) {
                    int a = resourceMapper.insertResources(data);
                    if (a == 0) {
                        logger.warn("save resource error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save resource error: {}", e.toString());
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
        try (SqlSession session = factory.openSession(false)) {
            CallSiteMapper callSiteMapper = session.getMapper(CallSiteMapper.class);
            try {
                for (List<CallSiteEntity> data : partition) {
                    int a = callSiteMapper.insertCallSites(data);
                    if (a == 0) {
                        logger.warn("save call site error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save call site error: {}", e.toString());
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
        try (SqlSession session = factory.openSession(false)) {
            LocalVarMapper localVarMapper = session.getMapper(LocalVarMapper.class);
            try {
                for (List<LocalVarEntity> data : partition) {
                    int a = localVarMapper.insertLocalVars(data);
                    if (a == 0) {
                        logger.warn("save local var error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save local var error: {}", e.toString());
            }
        }
        logger.info("save local vars success");
    }

    public static void saveSpringController(ArrayList<SpringController> controllers) {
        List<SpringControllerEntity> cList = new ArrayList<>();
        List<SpringMethodEntity> mList = new ArrayList<>();
        // 2025/06/26 处理 SPRING 分析错误时报警
        if (controllers == null || controllers.isEmpty()) {
            // 2025/08/05 SPRING 数据为空时不应该使用 WARN 日志
            logger.info("SPRING CONTROLLER 分析错误数据为空");
            return;
        }
        try (SqlSession session = factory.openSession(true)) {
            SpringControllerMapper springCMapper = session.getMapper(SpringControllerMapper.class);
            SpringMethodMapper springMMapper = session.getMapper(SpringMethodMapper.class);
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
                session.commit();
            } catch (Exception ex) {
                InterruptUtil.restoreInterruptIfNeeded(ex);
                session.rollback();
                logger.warn("SPRING CONTROLLER 分析错误 请提 ISSUE 解决");
                logger.debug("SPRING CONTROLLER error details: {}", ex.toString());
            }
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

    public static void saveSpringInterceptor(ArrayList<String> interceptors,
                                             Map<ClassReference.Handle, ClassReference> classMap) {
        List<SpringInterceptorEntity> list = new ArrayList<>();
        for (String interceptor : interceptors) {
            SpringInterceptorEntity ce = new SpringInterceptorEntity();
            ce.setClassName(interceptor);
            ce.setJarId((classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(interceptor), notFoundClassReference)).getJarId());
            list.add(ce);
        }
        List<List<SpringInterceptorEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            SpringInterceptorMapper springIMapper = session.getMapper(SpringInterceptorMapper.class);
            try {
                for (List<SpringInterceptorEntity> data : partition) {
                    int a = springIMapper.insertInterceptors(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save spring interceptor error: {}", e.toString());
            }
        }
    }

    public static void saveServlets(ArrayList<String> servlets,
                                    Map<ClassReference.Handle, ClassReference> classMap) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String servlet : servlets) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(servlet);
            ce.setJarId((classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(servlet), notFoundClassReference)).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
            try {
                for (List<JavaWebEntity> data : partition) {
                    int a = javaWebMapper.insertServlets(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save servlet error: {}", e.toString());
            }
        }
    }

    public static void saveFilters(ArrayList<String> filters,
                                   Map<ClassReference.Handle, ClassReference> classMap) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String filter : filters) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(filter);
            ce.setJarId((classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(filter), notFoundClassReference)).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
            try {
                for (List<JavaWebEntity> data : partition) {
                    int a = javaWebMapper.insertFilters(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save filter error: {}", e.toString());
            }
        }
    }

    public static void saveListeners(ArrayList<String> listeners,
                                     Map<ClassReference.Handle, ClassReference> classMap) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String listener : listeners) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(listener);
            ce.setJarId((classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(listener), notFoundClassReference)).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory.openSession(false)) {
            JavaWebMapper javaWebMapper = session.getMapper(JavaWebMapper.class);
            try {
                for (List<JavaWebEntity> data : partition) {
                    int a = javaWebMapper.insertListeners(data);
                    if (a == 0) {
                        logger.warn("save error");
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.warn("save listener error: {}", e.toString());
            }
        }
    }

    public static void cleanFav() {
        try (SqlSession session = factory.openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.cleanFav();
        }
    }

    public static void cleanFavItem(MethodResult m) {
        try (SqlSession session = factory.openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.cleanFavItem(m);
        }
    }

    public static void addFav(MethodResult m) {
        try (SqlSession session = factory.openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.addFav(m);
        }
    }

    public static void insertHistory(MethodResult m) {
        try (SqlSession session = factory.openSession(true)) {
            HisMapper hisMapper = session.getMapper(HisMapper.class);
            hisMapper.insertHistory(m);
        }
    }

    public static void cleanHistory() {
        try (SqlSession session = factory.openSession(true)) {
            HisMapper hisMapper = session.getMapper(HisMapper.class);
            hisMapper.cleanHistory();
        }
    }

    public static ArrayList<MethodResult> getAllFavMethods() {
        try (SqlSession session = factory.openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            return favMapper.getAllFavMethods();
        }
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        try (SqlSession session = factory.openSession(true)) {
            HisMapper hisMapper = session.getMapper(HisMapper.class);
            return hisMapper.getAllHisMethods();
        }
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
        try (SqlSession session = factory.openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            return semanticCacheMapper.selectValue(cacheKey, cacheType);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("semantic cache query fail: {}", ex.toString());
            return null;
        }
    }

    public static void putSemanticCacheValue(String cacheKey, String cacheType, String cacheValue) {
        if (cacheKey == null || cacheType == null || cacheValue == null) {
            return;
        }
        try (SqlSession session = factory.openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.upsert(cacheKey, cacheType, cacheValue);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("semantic cache write fail: {}", ex.toString());
        }
    }

    public static void clearSemanticCacheType(String cacheType) {
        if (cacheType == null) {
            return;
        }
        try (SqlSession session = factory.openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.deleteByType(cacheType);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("semantic cache clear fail: {}", ex.toString());
        }
    }

    public static void clearSemanticCache() {
        try (SqlSession session = factory.openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.deleteAll();
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("semantic cache clear all fail: {}", ex.toString());
        }
    }

    // --- report MCP ---

    public static void saveVulReport(VulReportEntity entity) {
        if (entity == null) {
            return;
        }
        try (SqlSession session = factory.openSession(true)) {
            VulReportMapper mapper = session.getMapper(VulReportMapper.class);
            mapper.insert(entity);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("vul report insert fail: {}", ex.toString());
        }
    }

    public static List<VulReportEntity> getVulReports() {
        try (SqlSession session = factory.openSession(true)) {
            VulReportMapper mapper = session.getMapper(VulReportMapper.class);
            return mapper.selectAll();
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("vul report query fail: {}", ex.toString());
            return new ArrayList<>();
        }
    }
}
