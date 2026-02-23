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
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.JarFingerprintUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.PartitionUtils;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private static final AtomicLong BUILD_SEQ = new AtomicLong(0);
    private static final AtomicBoolean BUILDING = new AtomicBoolean(false);
    private static final String MAIN_DB = Const.dbFile;
    private static final String NEXT_DB = Const.dbNextFile;
    private static final String PREV_DB = Const.dbPrevFile;
    private static final String DB_SUFFIX_WAL = "-wal";
    private static final String DB_SUFFIX_SHM = "-shm";
    private static final int SCHEMA_VERSION = 2;
    private static final AtomicBoolean BUILD_ON_NEXT_DB = new AtomicBoolean(false);

    private static SqlSessionFactory factory() {
        return SqlSessionFactoryUtil.getSqlSessionFactory();
    }

    // --inner-jar 仅解析此jar包引用的 jdk 类及其它jar中的类,但不会保存其它jar的jarId等信息
    private static final ClassReference notFoundClassReference = new ClassReference(-1, -1, null, null, null, false, null, null, "unknown", -1);
    private static final String METHOD_CALL_INSERT_SQL =
            "INSERT OR IGNORE INTO method_call_table " +
                    "(caller_mid, callee_mid, op_code, edge_type, edge_confidence, edge_evidence) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String STRING_VALUE_INSERT_SQL =
            "INSERT OR IGNORE INTO string_value_table (value) VALUES (?)";
    private static final String METHOD_STRING_INSERT_SQL =
            "INSERT OR IGNORE INTO method_string_table (mid, sid, src_type) " +
                    "VALUES (?, (SELECT sid FROM string_value_table WHERE value = ?), ?)";


    
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
        initDatabaseSchema();
        logger.info("create database finish");
        LogUtil.info("create database finish");
    }

    private static synchronized void initDatabaseSchema() {
        try (SqlSession session = factory().openSession(true)) {
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
            initMapper.createEntryPointTable();
            initMapper.createSchemaMetaTable();
            initMapper.createFindingCacheTable();
            initMapper.upsertSchemaMeta(SCHEMA_VERSION, System.currentTimeMillis() / 1000L, Const.version);

            // DFS
            initMapper.createDFSResultTable();
            initMapper.createDFSResultListTable();
            // NOTE
            initMapper.createFavoriteTable();
            initMapper.createHistoryTable();
            // deprecated storage tables (kept empty for compatibility)
            initMapper.createCallSiteTable();
            initMapper.createLocalVarTable();
            initMapper.createLineMappingTable();
            initMapper.createSemanticCacheTable();
            try {
                initMapper.createCallSiteIndex();
                initMapper.createLocalVarIndex();
                initMapper.createLineMappingIndex();
                initMapper.createSemanticCacheIndex();
            } catch (Throwable t) {
                logger.warn("create compatibility index fail: {}", t.toString());
            }
        } catch (Throwable t) {
            logger.error("init database schema fail: {}", t.toString());
            throw t;
        }
    }

    public static synchronized void beginRebuildToNextDb() {
        cleanupDbArtifacts(Paths.get(NEXT_DB));
        SqlSessionFactoryUtil.switchDatabase(NEXT_DB);
        initDatabaseSchema();
        BUILD_ON_NEXT_DB.set(true);
    }

    public static synchronized void publishRebuiltDatabase() {
        if (!BUILD_ON_NEXT_DB.get()) {
            return;
        }
        SqlSessionFactoryUtil.shutdownCurrentFactory();
        rotateNextToMain();
        SqlSessionFactoryUtil.switchDatabase(MAIN_DB);
        initDatabaseSchema();
        BUILD_ON_NEXT_DB.set(false);
    }

    public static synchronized void abortRebuildToMainDb() {
        if (!BUILD_ON_NEXT_DB.get()) {
            return;
        }
        try {
            SqlSessionFactoryUtil.shutdownCurrentFactory();
        } catch (Throwable ignored) {
        }
        cleanupDbArtifacts(Paths.get(NEXT_DB));
        SqlSessionFactoryUtil.switchDatabase(MAIN_DB);
        initDatabaseSchema();
        BUILD_ON_NEXT_DB.set(false);
    }

    private static void rotateNextToMain() {
        Path main = Paths.get(MAIN_DB);
        Path next = Paths.get(NEXT_DB);
        Path prev = Paths.get(PREV_DB);
        if (!Files.exists(next)) {
            throw new IllegalStateException("next db not found: " + next);
        }
        cleanupDbArtifacts(prev);
        if (Files.exists(main)) {
            moveDbWithSidecars(main, prev);
        }
        try {
            moveDbWithSidecars(next, main);
        } catch (Throwable t) {
            logger.error("promote next db fail: {}", t.toString());
            if (!Files.exists(main) && Files.exists(prev)) {
                moveDbWithSidecars(prev, main);
            }
            throw new IllegalStateException("promote next db fail", t);
        }
    }

    private static void moveDbWithSidecars(Path source, Path target) {
        moveFile(source, target);
        moveOptionalFile(Paths.get(source.toString() + DB_SUFFIX_WAL), Paths.get(target.toString() + DB_SUFFIX_WAL));
        moveOptionalFile(Paths.get(source.toString() + DB_SUFFIX_SHM), Paths.get(target.toString() + DB_SUFFIX_SHM));
    }

    private static void moveOptionalFile(Path source, Path target) {
        if (Files.exists(source)) {
            moveFile(source, target);
        }
    }

    private static void moveFile(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new IllegalStateException("move file fail: " + source + " -> " + target, ex);
            }
        }
    }

    private static void cleanupDbArtifacts(Path base) {
        if (base == null) {
            return;
        }
        try {
            Files.deleteIfExists(base);
            Files.deleteIfExists(Paths.get(base.toString() + DB_SUFFIX_WAL));
            Files.deleteIfExists(Paths.get(base.toString() + DB_SUFFIX_SHM));
        } catch (Exception e) {
            logger.debug("cleanup db artifacts fail: {}", e.toString());
        }
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
                "string_value_table",
                "method_string_table",
                "string_fts",
                "resource_table",
                "entrypoint_table",
                "schema_meta",
                "finding_cache",
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
        try (SqlSession session = factory().openSession(false)) {
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
        executeSql("PRAGMA cache_size=-64000");
    }

    private static void applyFinalizePragmas() {
        executeSql("PRAGMA wal_checkpoint(TRUNCATE)");
        executeSql("PRAGMA optimize");
    }

    private static void dropBuildIndexes() {
        executeSql("DROP INDEX IF EXISTS idx_string_value_nocase");
        executeSql("DROP INDEX IF EXISTS idx_method_string_sid");
        executeSql("DROP INDEX IF EXISTS idx_method_string_mid");
        executeSql("DROP INDEX IF EXISTS idx_resource_path");
        executeSql("DROP INDEX IF EXISTS idx_resource_jar_path");
        executeSql("DROP INDEX IF EXISTS idx_method_call_callee_mid");
        executeSql("DROP INDEX IF EXISTS idx_method_call_caller_mid");
        executeSql("DROP INDEX IF EXISTS idx_method_call_pair");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller_idx");
        executeSql("DROP INDEX IF EXISTS idx_call_site_callee");
        executeSql("DROP INDEX IF EXISTS idx_local_var_method");
        executeSql("DROP INDEX IF EXISTS idx_semantic_cache_type");
    }

    private static void createBuildIndexes() {
        try (SqlSession session = factory().openSession(true)) {
            InitMapper initMapper = session.getMapper(InitMapper.class);
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
                initMapper.createMethodCallIndex();
            } catch (Throwable t) {
                logger.warn("create method_call index fail: {}", t.toString());
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
    }

    private static void executeSql(String sql) {
        try (SqlSession session = factory().openSession(true);
             Statement statement = session.getConnection().createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            logger.debug("exec sql fail: {}", e.toString());
        }
    }

    public static void saveDFS(DFSResultEntity dfsResultEntity) {
        try (SqlSession session = factory().openSession(true)) {
            DFSMapper dfsMapper = session.getMapper(DFSMapper.class);
            int a = dfsMapper.insertDFSResult(dfsResultEntity);
            if (a < 1) {
                logger.warn("save dfs error");
            }
        }
    }

    public static void saveDFSList(DFSResultListEntity dfsResultListEntity) {
        try (SqlSession session = factory().openSession(true)) {
            DFSListMapper dfsListMapper = session.getMapper(DFSListMapper.class);
            int a = dfsListMapper.insertDFSResultList(dfsResultListEntity);
            if (a < 1) {
                logger.warn("save dfs list error");
            }
        }
    }

    public static void saveJar(String jarPath) {
        saveJar(jarPath, 0);
    }

    public static void saveJar(String jarPath, int depth) {
        JarEntity en = new JarEntity();
        en.setJarAbsPath(jarPath);
        if (OSUtil.isWindows()) {
            String[] temp = jarPath.split("\\\\");
            en.setJarName(temp[temp.length - 1]);
        } else {
            String[] temp = jarPath.split("/");
            en.setJarName(temp[temp.length - 1]);
        }
        en.setJarSha256(JarFingerprintUtil.sha256(jarPath));
        en.setDepth(Math.max(depth, 0));
        en.setJarRole(resolveJarRole(jarPath, depth).name());
        List<JarEntity> js = new ArrayList<>();
        js.add(en);
        try (SqlSession session = factory().openSession(true)) {
            JarMapper jarMapper = session.getMapper(JarMapper.class);
            int i = jarMapper.insertJar(js);
            if (i != 0) {
                logger.debug("save jar finish");
            }
        }
    }

    private static JarRole resolveJarRole(String jarPath, int depth) {
        if (jarPath == null) {
            return depth <= 0 ? JarRole.APP : JarRole.LIB;
        }
        String normalized = jarPath.replace("\\", "/").toLowerCase(Locale.ROOT);
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            String home = javaHome.replace("\\", "/").toLowerCase(Locale.ROOT);
            if (normalized.startsWith(home)) {
                return JarRole.JDK;
            }
        }
        if (normalized.endsWith("/rt.jar") || normalized.contains("/jre/lib/")
                || normalized.contains("/lib/modules")) {
            return JarRole.RUNTIME;
        }
        return depth <= 0 ? JarRole.APP : JarRole.LIB;
    }

    public static JarEntity getJarId(String jarPath) {
        try (SqlSession session = factory().openSession(true)) {
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
            if (classFile.getJarId() == null) {
                classFile.setJarId(-1);
            }
            list.add(classFile);
        }
        List<List<ClassFileEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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
        UiExecutor.runOnEdt(() -> MainForm.getInstance()
                .getTotalClassVal()
                .setText(String.valueOf(discoveredClasses.size())));
        List<ClassEntity> list = new ArrayList<>();
        Map<Integer, String> jarRoleById = loadJarRoleMap();
        for (ClassReference reference : discoveredClasses) {
            ClassEntity classEntity = new ClassEntity();
            classEntity.setJarName(reference.getJarName());
            classEntity.setJarId(reference.getJarId());
            classEntity.setVersion(reference.getVersion());
            classEntity.setAccess(reference.getAccess());
            classEntity.setClassName(reference.getName());
            classEntity.setSuperClassName(reference.getSuperClass());
            classEntity.setInterface(reference.isInterface());
            classEntity.setClassRole(resolveClassRole(reference.getName(),
                    jarRoleById.get(reference.getJarId())).name());
            list.add(classEntity);
        }
        List<List<ClassEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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

    private static Map<Integer, String> loadJarRoleMap() {
        Map<Integer, String> out = new HashMap<>();
        try (SqlSession session = factory().openSession(true)) {
            JarMapper jarMapper = session.getMapper(JarMapper.class);
            List<JarEntity> jars = jarMapper.selectAllJarMeta();
            if (jars != null) {
                for (JarEntity jar : jars) {
                    if (jar == null) {
                        continue;
                    }
                    out.put(jar.getJid(), jar.getJarRole());
                }
            }
        } catch (Throwable t) {
            logger.debug("load jar role map fail: {}", t.toString());
        }
        return out;
    }

    private static ClassRole resolveClassRole(String className, String jarRole) {
        if (className != null) {
            String c = className.replace('.', '/');
            if (c.startsWith("java/")
                    || c.startsWith("javax/")
                    || c.startsWith("jdk/")
                    || c.startsWith("sun/")
                    || c.startsWith("com/sun/")) {
                return ClassRole.JDK;
            }
        }
        if (JarRole.RUNTIME.name().equalsIgnoreCase(jarRole)) {
            return ClassRole.RUNTIME;
        }
        if (JarRole.JDK.name().equalsIgnoreCase(jarRole)) {
            return ClassRole.JDK;
        }
        if (JarRole.APP.name().equalsIgnoreCase(jarRole)) {
            return ClassRole.APP;
        }
        return ClassRole.LIB;
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
        UiExecutor.runOnEdt(() -> MainForm.getInstance()
                .getTotalMethodVal()
                .setText(String.valueOf(discoveredMethods.size())));
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
        try (SqlSession session = factory().openSession(false)) {
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
            HashSet<MethodReference.Handle>> methodCalls) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            logger.info("method call map is empty");
            return;
        }
        Map<String, Integer> methodIdMap = loadMethodIdMap();
        ensureMethodIds(methodCalls, methodIdMap);
        if (methodIdMap.isEmpty()) {
            logger.warn("save method call skipped: method id map is empty");
            return;
        }
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;
        try (SqlSession session = factory().openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement(METHOD_CALL_INSERT_SQL)) {
                    for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> call :
                            methodCalls.entrySet()) {
                        MethodReference.Handle caller = call.getKey();
                        HashSet<MethodReference.Handle> callee = call.getValue();
                        MethodReference callerRef = AnalyzeEnv.methodMap.get(caller);
                        int callerJarId = callerRef == null ? -1 : callerRef.getJarId();
                        Integer callerMid = methodIdMap.get(methodKey(caller, callerJarId));
                        if (callerMid == null) {
                            continue;
                        }

                        for (MethodReference.Handle mh : callee) {
                            MethodCallMeta meta = AnalyzeEnv.methodCallMeta.get(MethodCallKey.of(caller, mh));
                            if (meta == null) {
                                meta = fallbackEdgeMeta(mh);
                            }
                            MethodReference calleeRef = AnalyzeEnv.methodMap.get(mh);
                            int calleeJarId;
                            if (calleeRef != null) {
                                calleeJarId = calleeRef.getJarId();
                            } else {
                                ClassReference calleeClass = AnalyzeEnv.classMap.getOrDefault(
                                        mh.getClassReference(), notFoundClassReference);
                                calleeJarId = calleeClass.getJarId();
                            }
                            Integer calleeMid = methodIdMap.get(methodKey(mh, calleeJarId));
                            if (calleeMid == null) {
                                continue;
                            }
                            String edgeEvidence = meta.getEvidence();
                            if (edgeEvidence == null) {
                                edgeEvidence = "";
                            }
                            ps.setInt(1, callerMid);
                            ps.setInt(2, calleeMid);
                            ps.setInt(3, mh.getOpcode() == null ? -1 : mh.getOpcode());
                            ps.setString(4, meta.getType());
                            ps.setString(5, meta.getConfidence());
                            ps.setString(6, edgeEvidence);
                            ps.addBatch();
                            batchCount++;
                            total++;
                            if (batchCount >= batchSize) {
                                ps.executeBatch();
                                batchCount = 0;
                            }
                        }
                    }
                    if (batchCount > 0) {
                        ps.executeBatch();
                    }
                }
                connection.commit();
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

    private static Map<String, Integer> loadMethodIdMap() {
        Map<String, Integer> out = new HashMap<>();
        String sql = "SELECT method_id, class_name, method_name, method_desc, jar_id FROM method_table";
        try (SqlSession session = factory().openSession(true);
             Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                int methodId = rs.getInt("method_id");
                String className = rs.getString("class_name");
                String methodName = rs.getString("method_name");
                String methodDesc = rs.getString("method_desc");
                int jarId = rs.getInt("jar_id");
                String key = className + "#" + methodName + "#" + methodDesc + "#" + jarId;
                out.put(key, methodId);
            }
        } catch (Throwable t) {
            logger.warn("load method id map fail: {}", t.toString());
        }
        return out;
    }

    private static String methodKey(MethodReference.Handle method, int jarId) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        return method.getClassReference().getName()
                + "#" + method.getName()
                + "#" + method.getDesc()
                + "#" + jarId;
    }

    private static void ensureMethodIds(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                        Map<String, Integer> methodIdMap) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return;
        }
        List<MethodEntity> missing = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            int callerJarId = resolveJarId(caller);
            addMissingMethod(missing, seen, methodIdMap, caller, callerJarId);
            Set<MethodReference.Handle> callees = entry.getValue();
            if (callees == null || callees.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                int calleeJarId = resolveJarId(callee);
                addMissingMethod(missing, seen, methodIdMap, callee, calleeJarId);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        List<List<MethodEntity>> partition = PartitionUtils.partition(missing, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
            MethodMapper mapper = session.getMapper(MethodMapper.class);
            for (List<MethodEntity> data : partition) {
                mapper.insertMethod(data);
            }
            session.commit();
        } catch (Throwable t) {
            logger.warn("ensure method ids fail: {}", t.toString());
        }
        methodIdMap.putAll(loadMethodIdMap());
    }

    private static void addMissingMethod(List<MethodEntity> missing,
                                         Set<String> seen,
                                         Map<String, Integer> methodIdMap,
                                         MethodReference.Handle handle,
                                         int jarId) {
        if (handle == null || handle.getClassReference() == null) {
            return;
        }
        String key = methodKey(handle, jarId);
        if (methodIdMap.containsKey(key) || !seen.add(key)) {
            return;
        }
        MethodEntity entity = new MethodEntity();
        entity.setMethodName(handle.getName());
        entity.setMethodDesc(handle.getDesc());
        entity.setClassName(handle.getClassReference().getName());
        entity.setStatic(false);
        entity.setAccess(0);
        entity.setLineNumber(-1);
        entity.setJarId(jarId);
        missing.add(entity);
    }

    private static int resolveJarId(MethodReference.Handle handle) {
        if (handle == null) {
            return -1;
        }
        MethodReference method = AnalyzeEnv.methodMap.get(handle);
        if (method != null) {
            return method.getJarId();
        }
        ClassReference clazz = AnalyzeEnv.classMap.get(handle.getClassReference());
        if (clazz != null) {
            return clazz.getJarId();
        }
        return -1;
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
        try (SqlSession session = factory().openSession(false)) {
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
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap) {
        Map<String, Integer> methodIdMap = loadMethodIdMap();
        if (methodIdMap.isEmpty()) {
            logger.warn("save string skipped: method id map is empty");
            return;
        }
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;

        logger.info("save str map length: {}", strMap.size());
        try (SqlSession session = factory().openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement valuePs = connection.prepareStatement(STRING_VALUE_INSERT_SQL);
                     PreparedStatement mappingPs = connection.prepareStatement(METHOD_STRING_INSERT_SQL)) {
                    for (Map.Entry<MethodReference.Handle, List<String>> strEntry : strMap.entrySet()) {
                        MethodReference.Handle method = strEntry.getKey();
                        List<String> strList = strEntry.getValue();
                        MethodReference mr = AnalyzeEnv.methodMap.get(method);
                        if (mr == null || strList == null || strList.isEmpty()) {
                            continue;
                        }
                        Integer mid = methodIdMap.get(methodKey(method, mr.getJarId()));
                        if (mid == null) {
                            continue;
                        }
                        for (String s : strList) {
                            if (s == null) {
                                continue;
                            }
                            valuePs.setString(1, s);
                            valuePs.addBatch();
                            mappingPs.setInt(1, mid);
                            mappingPs.setString(2, s);
                            mappingPs.setString(3, "literal");
                            mappingPs.addBatch();
                            batchCount++;
                            total++;
                            if (batchCount >= batchSize) {
                                valuePs.executeBatch();
                                mappingPs.executeBatch();
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
                        if (mr == null || strList == null || strList.isEmpty()) {
                            continue;
                        }
                        Integer mid = methodIdMap.get(methodKey(method, mr.getJarId()));
                        if (mid == null) {
                            continue;
                        }
                        for (String s : strList) {
                            if (s == null) {
                                continue;
                            }
                            valuePs.setString(1, s);
                            valuePs.addBatch();
                            mappingPs.setInt(1, mid);
                            mappingPs.setString(2, s);
                            mappingPs.setString(3, "annotation");
                            mappingPs.addBatch();
                            batchCount++;
                            total++;
                            if (batchCount >= batchSize) {
                                valuePs.executeBatch();
                                mappingPs.executeBatch();
                                batchCount = 0;
                            }
                        }
                    }
                    if (batchCount > 0) {
                        valuePs.executeBatch();
                        mappingPs.executeBatch();
                    }
                }
                connection.commit();
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
        try (SqlSession session = factory().openSession(true)) {
            StringMapper stringMapper = session.getMapper(StringMapper.class);
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
        try (SqlSession session = factory().openSession(false)) {
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
        logger.info("skip persist call sites in v2 storage");
    }

    public static void saveLocalVars(List<LocalVarEntity> localVars) {
        logger.info("skip persist local vars in v2 storage");
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
        try (SqlSession session = factory().openSession(true)) {
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
            } catch (Throwable t) {
                session.rollback();
                logger.warn("SPRING CONTROLLER 分析错误 请提 ISSUE 解决");
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

    public static void saveSpringInterceptor(ArrayList<String> interceptors) {
        List<SpringInterceptorEntity> list = new ArrayList<>();
        for (String interceptor : interceptors) {
            SpringInterceptorEntity ce = new SpringInterceptorEntity();
            ce.setClassName(interceptor);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(interceptor), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<SpringInterceptorEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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

    public static void saveServlets(ArrayList<String> servlets) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String servlet : servlets) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(servlet);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(servlet), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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

    public static void saveFilters(ArrayList<String> filters) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String filter : filters) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(filter);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(filter), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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

    public static void saveListeners(ArrayList<String> listeners) {
        List<JavaWebEntity> list = new ArrayList<>();
        for (String listener : listeners) {
            JavaWebEntity ce = new JavaWebEntity();
            ce.setClassName(listener);
            ce.setJarId(AnalyzeEnv.classMap.getOrDefault(new ClassReference.Handle(listener), notFoundClassReference).getJarId());
            list.add(ce);
        }
        List<List<JavaWebEntity>> partition = PartitionUtils.partition(list, PART_SIZE);
        try (SqlSession session = factory().openSession(false)) {
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
        try (SqlSession session = factory().openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.cleanFav();
        }
    }

    public static void cleanFavItem(MethodResult m) {
        try (SqlSession session = factory().openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.cleanFavItem(m);
        }
    }

    public static void addFav(MethodResult m) {
        try (SqlSession session = factory().openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            favMapper.addFav(m);
        }
    }

    public static void insertHistory(MethodResult m) {
        try (SqlSession session = factory().openSession(true)) {
            HisMapper hisMapper = session.getMapper(HisMapper.class);
            hisMapper.insertHistory(m);
        }
    }

    public static void cleanHistory() {
        try (SqlSession session = factory().openSession(true)) {
            HisMapper hisMapper = session.getMapper(HisMapper.class);
            hisMapper.cleanHistory();
        }
    }

    public static ArrayList<MethodResult> getAllFavMethods() {
        try (SqlSession session = factory().openSession(true)) {
            FavMapper favMapper = session.getMapper(FavMapper.class);
            return favMapper.getAllFavMethods();
        }
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        try (SqlSession session = factory().openSession(true)) {
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
        try (SqlSession session = factory().openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            return semanticCacheMapper.selectValue(cacheKey, cacheType);
        } catch (Throwable t) {
            logger.debug("semantic cache query fail: {}", t.toString());
            return null;
        }
    }

    public static void putSemanticCacheValue(String cacheKey, String cacheType, String cacheValue) {
        if (cacheKey == null || cacheType == null || cacheValue == null) {
            return;
        }
        try (SqlSession session = factory().openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.upsert(cacheKey, cacheType, cacheValue);
        } catch (Throwable t) {
            logger.debug("semantic cache write fail: {}", t.toString());
        }
    }

    public static void clearSemanticCacheType(String cacheType) {
        if (cacheType == null) {
            return;
        }
        try (SqlSession session = factory().openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.deleteByType(cacheType);
        } catch (Throwable t) {
            logger.debug("semantic cache clear fail: {}", t.toString());
        }
    }

    public static void clearSemanticCache() {
        try (SqlSession session = factory().openSession(true)) {
            SemanticCacheMapper semanticCacheMapper = session.getMapper(SemanticCacheMapper.class);
            semanticCacheMapper.deleteAll();
        } catch (Throwable t) {
            logger.debug("semantic cache clear all fail: {}", t.toString());
        }
    }
}
