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
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.analyze.spring.SpringConstant;
import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.mapper.*;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ArtifactEntry;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.entity.*;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.PartitionUtils;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final int CLEAR_DB_BUSY_TIMEOUT_MS =
            clamp(Integer.getInteger("jar-analyzer.db.clear.busy-timeout-ms", 4000), 500, 30000);
    private static final int CLEAR_DB_MAX_RETRIES =
            clamp(Integer.getInteger("jar-analyzer.db.clear.max-retries", 6), 1, 20);
    private static final int CLEAR_DB_RETRY_BASE_MS =
            clamp(Integer.getInteger("jar-analyzer.db.clear.retry-base-ms", 120), 50, 2000);

    // --inner-jar 仅解析此jar包引用的 jdk 类及其它jar中的类,但不会保存其它jar的jarId等信息
    private static final ClassReference notFoundClassReference = new ClassReference(-1, -1, null, null, null, false, null, null, "unknown", -1);
    private static final String METHOD_CALL_INSERT_SQL =
            "INSERT INTO method_call_table " +
                    "(caller_method_name, caller_method_desc, caller_class_name, caller_jar_id, " +
                    "callee_method_name, callee_method_desc, callee_class_name, callee_jar_id, op_code, " +
                    "edge_type, edge_confidence, edge_evidence, call_site_key) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                initMapper.addMethodCallSiteKeyColumn();
            } catch (Exception ex) {
                logger.debug("add call_site_key column fail: {}", ex.toString());
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
                logger.error("create string_fts fail: {}", ex.toString());
                throw new IllegalStateException("create string_fts fail", ex);
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
            try {
                initMapper.addCallSiteKeyColumn();
            } catch (Exception ex) {
                logger.debug("add call_site.call_site_key column fail: {}", ex.toString());
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
            initMapper.createProjectModelMetaTable();
            initMapper.createProjectModelRootTable();
            initMapper.createProjectModelEntryTable();
            initMapper.createProjectClassOriginTable();
            initMapper.createProjectResourceOriginTable();
            try {
                initMapper.createProjectModelIndex();
            } catch (Exception ex) {
                logger.warn("create project_model index fail: {}", ex.toString());
            }
            initMapper.createGraphMetaTable();
            initMapper.createGraphNodeTable();
            initMapper.createGraphEdgeTable();
            try {
                initMapper.addGraphNodeLastSeenBuildSeqColumn();
            } catch (Exception ex) {
                logger.debug("add graph_node.last_seen_build_seq column fail: {}", ex.toString());
            }
            try {
                initMapper.addGraphEdgeLastSeenBuildSeqColumn();
            } catch (Exception ex) {
                logger.debug("add graph_edge.last_seen_build_seq column fail: {}", ex.toString());
            }
            initMapper.createGraphLabelTable();
            initMapper.createGraphAttrTable();
            initMapper.createGraphStatsTable();
            try {
                initMapper.createGraphIndex();
            } catch (Exception ex) {
                logger.warn("create graph index fail: {}", ex.toString());
            }
            // report MCP (n8n agent)
            try {
                initMapper.createVulReportTable();
            } catch (Exception ex) {
                logger.warn("create vul_report table fail: {}", ex.toString());
            }
        }
        ensureSplitIndexes();
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
                "semantic_cache_table",
                "project_model_meta",
                "project_model_root",
                "project_model_entry",
                "project_class_origin",
                "project_resource_origin",
                "graph_meta",
                "graph_node",
                "graph_edge",
                "graph_label",
                "graph_attr",
                "graph_stats"
        };
        SQLException lastBusy = null;
        boolean cleared = false;
        for (int attempt = 1; attempt <= CLEAR_DB_MAX_RETRIES; attempt++) {
            try (SqlSession session = factory.openSession(false)) {
                Connection connection = session.getConnection();
                boolean autoCommit = connection.getAutoCommit();
                boolean retry;
                try {
                    connection.setAutoCommit(false);
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("PRAGMA busy_timeout=" + CLEAR_DB_BUSY_TIMEOUT_MS);
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
                    cleared = true;
                    break;
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException ignored) {
                        logger.warn("clear db rollback error");
                    }
                    retry = isBusyLockError(e) && attempt < CLEAR_DB_MAX_RETRIES;
                    if (retry) {
                        lastBusy = e;
                        logger.debug("clear db data busy (attempt {}/{}): {}",
                                attempt, CLEAR_DB_MAX_RETRIES, e.toString());
                    } else {
                        logger.warn("clear db data error: {}", e.toString());
                    }
                } finally {
                    try {
                        connection.setAutoCommit(autoCommit);
                    } catch (SQLException ignored) {
                        logger.warn("restore auto commit error");
                    }
                }
                if (retry && !sleepForClearRetry(attempt)) {
                    break;
                }
                if (!retry) {
                    break;
                }
            } catch (SQLException e) {
                if (isBusyLockError(e) && attempt < CLEAR_DB_MAX_RETRIES) {
                    lastBusy = e;
                    logger.debug("open clear db session busy (attempt {}/{}): {}",
                            attempt, CLEAR_DB_MAX_RETRIES, e.toString());
                    if (!sleepForClearRetry(attempt)) {
                        break;
                    }
                    continue;
                }
                logger.warn("clear db data error: {}", e.toString());
                break;
            }
        }
        if (!cleared && lastBusy != null) {
            logger.warn("clear db data locked after {} retries: {}",
                    CLEAR_DB_MAX_RETRIES, lastBusy.toString());
        }
        try {
            me.n1ar4.jar.analyzer.graph.store.GraphStore.invalidateCache();
        } catch (Exception ex) {
            logger.debug("invalidate graph snapshot cache fail: {}", ex.toString());
        }
    }

    private static boolean isBusyLockError(SQLException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlEx) {
                int code = sqlEx.getErrorCode();
                if (code == 5 || code == 6 || code == 261 || code == 517 || code == 773) {
                    return true;
                }
                String state = sqlEx.getSQLState();
                if (state != null) {
                    String normalizedState = state.toLowerCase(Locale.ROOT);
                    if (normalizedState.contains("busy") || normalizedState.contains("locked")) {
                        return true;
                    }
                }
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("sqlite_busy")
                        || normalized.contains("sqlite_locked")
                        || normalized.contains("database is locked")
                        || normalized.contains("table is locked")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean sleepForClearRetry(int attempt) {
        long delay = (long) CLEAR_DB_RETRY_BASE_MS * attempt;
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("clear db retry interrupted");
            return false;
        }
    }

    private static int clamp(Integer value, int min, int max) {
        int actual = value == null ? min : value;
        if (actual < min) {
            return min;
        }
        if (actual > max) {
            return max;
        }
        return actual;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static void saveProjectModel(ProjectModel model) {
        if (model == null) {
            return;
        }
        long buildSeq = BUILD_SEQ.get();
        if (buildSeq <= 0) {
            return;
        }
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                clearProjectModelByBuildSeq(connection, buildSeq);
                insertProjectModelMeta(connection, buildSeq, model);
                List<ProjectRootPath> rootPaths = insertProjectRoots(connection, buildSeq, model.roots());
                insertProjectEntries(connection, buildSeq, model, rootPaths);
                connection.commit();
            } catch (Exception ex) {
                InterruptUtil.restoreInterruptIfNeeded(ex);
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    logger.debug("rollback project_model fail: {}", ignored.toString());
                }
                logger.warn("save project_model fail: {}", ex.toString());
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                    logger.debug("restore auto commit for project_model fail: {}", ignored.toString());
                }
            }
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.warn("save project_model open session fail: {}", ex.toString());
        }
    }

    private static void clearProjectModelByBuildSeq(Connection connection, long buildSeq) throws SQLException {
        String[] sqlList = new String[]{
                "DELETE FROM project_model_meta WHERE build_seq = ?",
                "DELETE FROM project_model_root WHERE build_seq = ?",
                "DELETE FROM project_model_entry WHERE build_seq = ?",
                "DELETE FROM project_class_origin WHERE build_seq = ?",
                "DELETE FROM project_resource_origin WHERE build_seq = ?"
        };
        for (String sql : sqlList) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, buildSeq);
                statement.executeUpdate();
            }
        }
    }

    private static void insertProjectModelMeta(Connection connection,
                                               long buildSeq,
                                               ProjectModel model) throws SQLException {
        String sql = "INSERT INTO project_model_meta " +
                "(build_seq, build_mode, project_name, primary_input_path, runtime_path, resolve_inner_jars, options_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String projectName = resolveProjectName(model);
        String primaryInput = pathToString(model.primaryInputPath());
        String runtime = pathToString(model.runtimePath());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildSeq);
            statement.setString(2, model.buildMode() == null ? "artifact" : model.buildMode().value());
            statement.setString(3, projectName);
            statement.setString(4, primaryInput);
            statement.setString(5, runtime);
            statement.setInt(6, model.resolveInnerJars() ? 1 : 0);
            JSONObject options = new JSONObject();
            options.put("projectId", safeString(model.projectId()));
            options.put("artifactCatalogVersion", model.artifactCatalogVersion());
            options.put("selectedRuntimeProfileId", safeString(model.selectedRuntimeProfileId()));
            statement.setString(7, options.toJSONString());
            statement.executeUpdate();
        }
    }

    private static List<ProjectRootPath> insertProjectRoots(Connection connection,
                                                            long buildSeq,
                                                            List<ProjectRoot> roots) throws SQLException {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        String sql = "INSERT INTO project_model_root " +
                "(build_seq, root_kind, origin_kind, root_path, presentable_name, is_archive, is_test, priority, options_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<ProjectRootPath> rootPaths = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (ProjectRoot root : roots) {
                if (root == null || root.path() == null) {
                    continue;
                }
                statement.setLong(1, buildSeq);
                statement.setString(2, root.kind() == null ? "" : root.kind().value());
                statement.setString(3, root.origin() == null ? "" : root.origin().value());
                statement.setString(4, pathToString(root.path()));
                statement.setString(5, safeString(root.presentableName()));
                statement.setInt(6, root.archive() ? 1 : 0);
                statement.setInt(7, root.test() ? 1 : 0);
                statement.setInt(8, root.priority());
                JSONObject options = new JSONObject();
                options.put("rootKind", root.kind() == null ? "" : root.kind().value());
                options.put("origin", root.origin() == null ? "" : root.origin().value());
                statement.setString(9, options.toJSONString());
                statement.executeUpdate();
                int rootId = -1;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys != null && keys.next()) {
                        rootId = keys.getInt(1);
                    }
                } catch (SQLException ignored) {
                    logger.debug("read project_root generated key fail: {}", ignored.toString());
                }
                String origin = root.origin() == null ? "app" : root.origin().value();
                String kind = root.kind() == null ? "" : root.kind().value();
                rootPaths.add(new ProjectRootPath(root.path(), rootId, origin, kind));
            }
        }
        rootPaths.sort((a, b) -> Integer.compare(pathDepth(b.path), pathDepth(a.path)));
        return rootPaths;
    }

    private static void insertProjectEntries(Connection connection,
                                             long buildSeq,
                                             ProjectModel model,
                                             List<ProjectRootPath> rootPaths) throws SQLException {
        String sql = "INSERT INTO project_model_entry " +
                "(build_seq, root_id, entry_kind, origin_kind, entry_path, class_name, resource_path, jar_id, jar_name, options_json) " +
                "VALUES (?, ?, ?, ?, ?, '', '', -1, '', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (model.roots() != null) {
                for (ProjectRoot root : model.roots()) {
                    if (root == null || root.path() == null) {
                        continue;
                    }
                    ProjectRootPath matched = matchExactRoot(rootPaths, root);
                    int rootId = matched == null ? -1 : matched.rootId;
                    statement.setLong(1, buildSeq);
                    statement.setInt(2, rootId);
                    statement.setString(3, "root");
                    statement.setString(4, root.origin() == null ? "" : root.origin().value());
                    statement.setString(5, pathToString(root.path()));
                    JSONObject options = new JSONObject();
                    options.put("projectId", safeString(model.projectId()));
                    options.put("entryType", "root");
                    statement.setString(6, options.toJSONString());
                    statement.executeUpdate();
                }
            }
            if (model.analyzedArchives() != null) {
                for (Path archive : model.analyzedArchives()) {
                    if (archive == null) {
                        continue;
                    }
                    ProjectRootPath matched = matchRootPath(rootPaths, archive);
                    int rootId = matched == null ? -1 : matched.rootId;
                    String origin = matched == null ? "app" : matched.origin;
                    statement.setLong(1, buildSeq);
                    statement.setInt(2, rootId);
                    statement.setString(3, "archive");
                    statement.setString(4, origin);
                    statement.setString(5, pathToString(archive));
                    JSONObject options = new JSONObject();
                    options.put("indexPolicy", "INDEX_FULL");
                    options.put("projectId", safeString(model.projectId()));
                    statement.setString(6, options.toJSONString());
                    statement.executeUpdate();
                }
            }
            if (model.artifactEntries() != null && !model.artifactEntries().isEmpty()) {
                Set<String> seen = new HashSet<>();
                for (ArtifactEntry entry : model.artifactEntries()) {
                    if (entry == null) {
                        continue;
                    }
                    String path = pathToString(entry.path());
                    String locator = safeString(entry.locator());
                    String dedupe = entry.role().name() + "|" + entry.indexPolicy().name() + "|" + path + "|" + locator;
                    if (!seen.add(dedupe)) {
                        continue;
                    }
                    ProjectRootPath matched = matchRootPath(rootPaths, entry.path());
                    int rootId = matched == null ? -1 : matched.rootId;
                    String origin = entry.origin() == null
                            ? (matched == null ? "unknown" : matched.origin)
                            : entry.origin().value();
                    statement.setLong(1, buildSeq);
                    statement.setInt(2, rootId);
                    statement.setString(3, "artifact");
                    statement.setString(4, origin);
                    statement.setString(5, path);
                    JSONObject options = new JSONObject();
                    options.put("role", entry.role().name());
                    options.put("indexPolicy", entry.indexPolicy().name());
                    options.put("displayName", safeString(entry.displayName()));
                    options.put("locator", locator);
                    options.put("projectId", safeString(model.projectId()));
                    statement.setString(6, options.toJSONString());
                    statement.executeUpdate();
                }
            }
        }
    }

    private static ProjectRootPath matchRootPath(List<ProjectRootPath> rootPaths, Path path) {
        if (rootPaths == null || rootPaths.isEmpty() || path == null) {
            return null;
        }
        Path normalized = normalizePath(path);
        for (ProjectRootPath rootPath : rootPaths) {
            if (rootPath == null || rootPath.path == null) {
                continue;
            }
            try {
                if (normalized.startsWith(rootPath.path)) {
                    return rootPath;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static ProjectRootPath matchExactRoot(List<ProjectRootPath> rootPaths, ProjectRoot root) {
        if (rootPaths == null || rootPaths.isEmpty() || root == null || root.path() == null) {
            return null;
        }
        Path normalized = normalizePath(root.path());
        String kind = root.kind() == null ? "" : root.kind().value();
        for (ProjectRootPath rootPath : rootPaths) {
            if (rootPath == null || rootPath.path == null) {
                continue;
            }
            if (!safeString(rootPath.rootKind).equals(kind)) {
                continue;
            }
            if (normalized.equals(rootPath.path)) {
                return rootPath;
            }
        }
        return null;
    }

    private static String resolveProjectName(ProjectModel model) {
        if (model == null) {
            return "";
        }
        String declared = safeString(model.projectName());
        if (!declared.isBlank()) {
            return declared;
        }
        Path input = model.primaryInputPath();
        if (input == null) {
            return "workspace";
        }
        Path fileName = input.getFileName();
        if (fileName == null) {
            return input.toString();
        }
        String name = fileName.toString().trim();
        return name.isEmpty() ? input.toString() : name;
    }

    private static String pathToString(Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = normalizePath(path);
        return normalized == null ? "" : normalized.toString();
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static int pathDepth(Path path) {
        if (path == null) {
            return 0;
        }
        int count = path.getNameCount();
        return count < 0 ? 0 : count;
    }

    private static final class ProjectRootPath {
        private final Path path;
        private final int rootId;
        private final String origin;
        private final String rootKind;

        private ProjectRootPath(Path path, int rootId, String origin, String rootKind) {
            this.path = path;
            this.rootId = rootId;
            this.origin = origin == null ? "app" : origin;
            this.rootKind = rootKind == null ? "" : rootKind;
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
        executeSql("DROP INDEX IF EXISTS idx_method_call_site_key");
        executeSql("DROP INDEX IF EXISTS idx_string_value_nocase");
        executeSql("DROP INDEX IF EXISTS idx_resource_path");
        executeSql("DROP INDEX IF EXISTS idx_resource_jar_path");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller");
        executeSql("DROP INDEX IF EXISTS idx_call_site_caller_idx");
        executeSql("DROP INDEX IF EXISTS idx_call_site_callee");
        executeSql("DROP INDEX IF EXISTS idx_call_site_key");
        executeSql("DROP INDEX IF EXISTS idx_local_var_method");
        executeSql("DROP INDEX IF EXISTS idx_semantic_cache_type");
        executeSql("DROP INDEX IF EXISTS idx_project_model_mode");
        executeSql("DROP INDEX IF EXISTS idx_project_root_build_kind");
        executeSql("DROP INDEX IF EXISTS idx_project_root_origin_path");
        executeSql("DROP INDEX IF EXISTS idx_project_entry_build_kind");
        executeSql("DROP INDEX IF EXISTS idx_project_entry_origin_path");
        executeSql("DROP INDEX IF EXISTS idx_project_entry_class");
        executeSql("DROP INDEX IF EXISTS idx_project_class_origin_lookup");
        executeSql("DROP INDEX IF EXISTS idx_project_resource_origin_lookup");
        executeSql("DROP INDEX IF EXISTS idx_graph_node_kind_sig");
        executeSql("DROP INDEX IF EXISTS idx_graph_node_callsite");
        executeSql("DROP INDEX IF EXISTS idx_graph_edge_src_rel_dst");
        executeSql("DROP INDEX IF EXISTS idx_graph_edge_dst_rel_src");
        executeSql("DROP INDEX IF EXISTS idx_graph_edge_semantic");
        executeSql("DROP INDEX IF EXISTS idx_graph_node_last_seen");
        executeSql("DROP INDEX IF EXISTS idx_graph_edge_last_seen");
        executeSql("DROP INDEX IF EXISTS idx_graph_label_label");
        executeSql("DROP INDEX IF EXISTS idx_graph_attr_lookup");
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
            try {
                initMapper.createProjectModelIndex();
            } catch (Exception ex) {
                logger.warn("create project_model index fail: {}", ex.toString());
            }
            try {
                initMapper.createGraphIndex();
            } catch (Exception ex) {
                logger.warn("create graph index fail: {}", ex.toString());
            }
        }
        ensureSplitIndexes();
    }

    private static void ensureSplitIndexes() {
        // Some JDBC/MyBatis setups only execute the first statement in a mapped <update>.
        // Ensure all multi-statement index groups are created with one SQL per execute.
        executeSql("CREATE INDEX IF NOT EXISTS idx_method_call_callee " +
                "ON method_call_table(callee_class_name, callee_method_name, callee_method_desc)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_method_call_caller " +
                "ON method_call_table(caller_class_name, caller_method_name, caller_method_desc)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_method_call_edge " +
                "ON method_call_table(" +
                "caller_class_name, caller_method_name, caller_method_desc, " +
                "callee_class_name, callee_method_name, callee_method_desc)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_method_call_site_key " +
                "ON method_call_table(call_site_key)");

        executeSql("CREATE INDEX IF NOT EXISTS idx_resource_path " +
                "ON resource_table(resource_path)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_resource_jar_path " +
                "ON resource_table(jar_id, resource_path)");

        executeSql("CREATE INDEX IF NOT EXISTS idx_call_site_caller " +
                "ON bytecode_call_site_table(caller_class_name, caller_method_name, caller_method_desc)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_call_site_caller_idx " +
                "ON bytecode_call_site_table(caller_class_name, caller_method_name, caller_method_desc, call_index)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_call_site_callee " +
                "ON bytecode_call_site_table(callee_owner, callee_method_name, callee_method_desc)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_call_site_key " +
                "ON bytecode_call_site_table(call_site_key)");

        executeSql("CREATE INDEX IF NOT EXISTS idx_project_model_mode " +
                "ON project_model_meta(build_mode, build_seq)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_root_build_kind " +
                "ON project_model_root(build_seq, root_kind, origin_kind)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_root_origin_path " +
                "ON project_model_root(origin_kind, root_path)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_entry_build_kind " +
                "ON project_model_entry(build_seq, entry_kind, origin_kind)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_entry_origin_path " +
                "ON project_model_entry(origin_kind, entry_path)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_entry_class " +
                "ON project_model_entry(class_name, jar_id, origin_kind)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_class_origin_lookup " +
                "ON project_class_origin(origin_kind, class_name, jar_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_project_resource_origin_lookup " +
                "ON project_resource_origin(origin_kind, resource_path, jar_id)");

        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_node_kind_sig " +
                "ON graph_node(kind, class_name, method_name, method_desc, jar_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_node_callsite " +
                "ON graph_node(call_site_key, class_name, method_name, method_desc, jar_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_edge_src_rel_dst " +
                "ON graph_edge(src_id, rel_type, dst_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_edge_dst_rel_src " +
                "ON graph_edge(dst_id, rel_type, src_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_edge_semantic " +
                "ON graph_edge(src_id, dst_id, rel_type, confidence, evidence, op_code)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_node_last_seen " +
                "ON graph_node(last_seen_build_seq, node_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_edge_last_seen " +
                "ON graph_edge(last_seen_build_seq, edge_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_label_label " +
                "ON graph_label(label, node_id)");
        executeSql("CREATE INDEX IF NOT EXISTS idx_graph_attr_lookup " +
                "ON graph_attr(owner_type, k, v_text, v_num, v_bool, owner_id)");
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
            if (StrUtil.isBlank(classFile.getPathStr())) {
                Path path = classFile.getPath();
                if (path != null) {
                    classFile.setPathStr(path.toAbsolutePath().toString());
                }
            }
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
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                       List<CallSiteEntity> callSites) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            logger.info("method call map is empty");
            return;
        }
        Map<MethodCallKey, MethodCallMeta> effectiveMeta = methodCallMeta;
        if (effectiveMeta == null || effectiveMeta.isEmpty()) {
            logger.warn("method call metadata is empty, use fallback metadata for all edges");
            effectiveMeta = Collections.emptyMap();
        }
        Map<String, String> callSiteKeyByEdge = buildPrimaryCallSiteByEdge(callSites);
        int batchSize = Math.max(1, PART_SIZE);
        int total = 0;
        int batchCount = 0;
        int missingMetaCount = 0;
        int fallbackOpcodeCount = 0;
        String firstMissingMetaEdge = null;
        String firstFallbackOpcodeEdge = null;
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
                            ClassReference calleeClass = classMap == null
                                    ? notFoundClassReference
                                    : classMap.getOrDefault(mh.getClassReference(), notFoundClassReference);
                            MethodCallMeta meta = resolveMethodCallMeta(effectiveMeta, caller, mh);
                            if (meta == null) {
                                missingMetaCount++;
                                if (firstMissingMetaEdge == null) {
                                    firstMissingMetaEdge = edgeLabel(caller, mh);
                                }
                            }
                            int opCode = resolveEdgeOpcode(meta, mh, calleeClass);
                            if (meta == null || meta.getBestOpcode() <= 0) {
                                fallbackOpcodeCount++;
                                if (firstFallbackOpcodeEdge == null) {
                                    firstFallbackOpcodeEdge = edgeLabel(caller, mh);
                                }
                            }
                            String edgeType = resolveEdgeType(meta);
                            String edgeConfidence = resolveEdgeConfidence(meta);
                            String edgeEvidence = resolveEdgeEvidence(meta);
                            String callSiteKey = callSiteKeyByEdge.get(CallSiteKeyUtil.buildEdgeLookupKey(
                                    callerJarId,
                                    caller.getClassReference().getName(),
                                    caller.getName(),
                                    caller.getDesc(),
                                    mh.getClassReference().getName(),
                                    mh.getName(),
                                    mh.getDesc(),
                                    opCode
                            ));
                            if (callSiteKey == null) {
                                callSiteKey = "";
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
                            ps.setString(10, edgeType);
                            ps.setString(11, edgeConfidence);
                            ps.setString(12, edgeEvidence);
                            ps.setString(13, callSiteKey);
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
                if (missingMetaCount > 0) {
                    logger.warn("method call metadata fallback applied: {} edges, sample={}",
                            missingMetaCount, firstMissingMetaEdge);
                }
                if (fallbackOpcodeCount > 0) {
                    logger.warn("method call opcode fallback applied: {} edges, sample={}",
                            fallbackOpcodeCount, firstFallbackOpcodeEdge);
                }
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

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
                                       Map<ClassReference.Handle, ClassReference> classMap,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        saveMethodCalls(methodCalls, classMap, methodCallMeta, Collections.emptyList());
    }

    private static String resolveEdgeType(MethodCallMeta meta) {
        if (meta == null || meta.getType() == null || meta.getType().trim().isEmpty()) {
            return MethodCallMeta.TYPE_UNKNOWN;
        }
        return meta.getType();
    }

    private static String resolveEdgeConfidence(MethodCallMeta meta) {
        if (meta == null || meta.getConfidence() == null || meta.getConfidence().trim().isEmpty()) {
            return MethodCallMeta.CONF_LOW;
        }
        return meta.getConfidence();
    }

    private static String resolveEdgeEvidence(MethodCallMeta meta) {
        if (meta == null) {
            return "";
        }
        String evidence = meta.getEvidence();
        return evidence == null ? "" : evidence;
    }

    private static int resolveEdgeOpcode(MethodCallMeta meta,
                                         MethodReference.Handle callee,
                                         ClassReference calleeClass) {
        if (meta != null && meta.getBestOpcode() > 0) {
            return meta.getBestOpcode();
        }
        if (callee != null && callee.getOpcode() != null && callee.getOpcode() > 0) {
            return callee.getOpcode();
        }
        if (callee != null && "<init>".equals(callee.getName())) {
            return Opcodes.INVOKESPECIAL;
        }
        if (calleeClass != null && calleeClass.isInterface()) {
            return Opcodes.INVOKEINTERFACE;
        }
        return Opcodes.INVOKEVIRTUAL;
    }

    private static Map<String, String> buildPrimaryCallSiteByEdge(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, CallSiteEntity> best = new HashMap<>();
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            String edgeKey = CallSiteKeyUtil.buildEdgeLookupKey(site);
            if (edgeKey.isEmpty()) {
                continue;
            }
            if (site.getCallSiteKey() == null || site.getCallSiteKey().trim().isEmpty()) {
                site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site));
            }
            CallSiteEntity existing = best.get(edgeKey);
            if (existing == null || compareCallSiteOrder(site, existing) < 0) {
                best.put(edgeKey, site);
            }
        }
        if (best.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>(best.size() * 2);
        for (Map.Entry<String, CallSiteEntity> entry : best.entrySet()) {
            CallSiteEntity site = entry.getValue();
            if (site == null) {
                continue;
            }
            String callSiteKey = site.getCallSiteKey();
            if (callSiteKey == null || callSiteKey.trim().isEmpty()) {
                callSiteKey = CallSiteKeyUtil.buildCallSiteKey(site);
            }
            out.put(entry.getKey(), callSiteKey);
        }
        return out;
    }

    private static int compareCallSiteOrder(CallSiteEntity a, CallSiteEntity b) {
        int ai = normalizeCallIndex(a == null ? null : a.getCallIndex());
        int bi = normalizeCallIndex(b == null ? null : b.getCallIndex());
        if (ai != bi) {
            return Integer.compare(ai, bi);
        }
        int al = normalizeLineNumber(a == null ? null : a.getLineNumber());
        int bl = normalizeLineNumber(b == null ? null : b.getLineNumber());
        return Integer.compare(al, bl);
    }

    private static int normalizeCallIndex(Integer idx) {
        if (idx == null || idx < 0) {
            return Integer.MAX_VALUE;
        }
        return idx;
    }

    private static int normalizeLineNumber(Integer line) {
        if (line == null || line < 0) {
            return Integer.MAX_VALUE;
        }
        return line;
    }

    private static MethodCallMeta resolveMethodCallMeta(Map<MethodCallKey, MethodCallMeta> metaMap,
                                                        MethodReference.Handle caller,
                                                        MethodReference.Handle callee) {
        return MethodCallMeta.resolve(metaMap, caller, callee);
    }

    private static String edgeLabel(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return "unknown";
        }
        return caller.getClassReference().getName() + "." + caller.getName() + caller.getDesc()
                + " -> "
                + callee.getClassReference().getName() + "." + callee.getName() + callee.getDesc();
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
            logger.error("rebuild string_fts fail: {}", ex.toString());
            throw new IllegalStateException("rebuild string_fts fail", ex);
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
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            if (site.getCallSiteKey() == null || site.getCallSiteKey().trim().isEmpty()) {
                site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site));
            }
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
