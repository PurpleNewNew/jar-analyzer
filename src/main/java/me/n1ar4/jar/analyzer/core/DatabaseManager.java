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
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.entity.AnnoEntity;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.DFSResultEntity;
import me.n1ar4.jar.analyzer.entity.DFSResultListEntity;
import me.n1ar4.jar.analyzer.entity.InterfaceEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.JavaWebEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodCallEntity;
import me.n1ar4.jar.analyzer.entity.MethodImplEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.entity.SpringControllerEntity;
import me.n1ar4.jar.analyzer.entity.SpringInterceptorEntity;
import me.n1ar4.jar.analyzer.entity.SpringMethodEntity;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jIdSequenceRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jNativeBulkWriter;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jReadRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jSchemaInitializer;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jWriteRepository;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.PartitionUtils;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger();
    private static final String PROJECTS_ROOT_PROP = "jar.analyzer.neo4j.projectsRoot";
    private static final String PROJECTS_ROOT_ENV = "JAR_ANALYZER_NEO4J_PROJECTS_ROOT";
    private static final String DEFAULT_PROJECT_KEY = "default";
    public static int PART_SIZE = resolveBatchSize();

    private static final Neo4jStore STORE = Neo4jStore.getInstance();
    private static final Neo4jWriteRepository WRITE_REPO = new Neo4jWriteRepository(STORE);
    private static final Neo4jReadRepository READ_REPO = new Neo4jReadRepository(STORE);
    private static final Neo4jIdSequenceRepository ID_SEQ = new Neo4jIdSequenceRepository(STORE);
    private static final Neo4jNativeBulkWriter NATIVE_BULK = new Neo4jNativeBulkWriter(STORE);
    private static final Neo4jSchemaInitializer SCHEMA = new Neo4jSchemaInitializer(WRITE_REPO);

    private static final AtomicLong BUILD_SEQ = new AtomicLong(0);
    private static final AtomicBoolean BUILDING = new AtomicBoolean(false);

    private static final Map<String, MethodIdentity> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CALLSITE_NODE_CACHE = new ConcurrentHashMap<>();

    private static final ClassReference notFoundClassReference =
            new ClassReference(-1, -1, null, null, null, false, null, null, "unknown", -1);

    static {
        logger.info("init neo4j schema");
        try {
            STORE.start();
            SCHEMA.init();
            ensureBuildMetaNodes();
            syncBuildSeqFromStore();
            logger.info("neo4j schema ready");
        } catch (Throwable ex) {
            logger.error("init neo4j schema error: {}", ex.toString());
            throw ex;
        }
    }

    private static int resolveBatchSize() {
        int defaultSize = 500;
        String raw = System.getProperty("jar-analyzer.store.batch");
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

    private static void ensureBuildMetaNodes() {
        WRITE_REPO.run(
                "MERGE (b:BuildMeta {name:'build'}) " +
                        "ON CREATE SET b.buildSeq = 0, b.building = false, b.updatedAt = timestamp()",
                Collections.emptyMap(),
                30_000L
        );
        WRITE_REPO.run(
                "MERGE (b:BuildMeta {name:'graph_projection'}) " +
                        "ON CREATE SET b.buildSeq = 0, b.quickMode = false, b.callGraphMode = '', b.updatedAt = timestamp()",
                Collections.emptyMap(),
                30_000L
        );
    }

    private static void syncBuildSeqFromStore() {
        try {
            long seq = READ_REPO.count(
                    "MATCH (b:BuildMeta {name:'build'}) RETURN coalesce(b.buildSeq, 0) AS seq",
                    Collections.emptyMap(),
                    "seq",
                    30_000L);
            BUILD_SEQ.set(Math.max(0, seq));
        } catch (Exception ex) {
            logger.debug("sync build seq fail: {}", ex.toString());
        }
    }

    public static void prepareBuild() {
        long nextSeq = BUILD_SEQ.incrementAndGet();
        BUILDING.set(true);
        METHOD_CACHE.clear();
        CALLSITE_NODE_CACHE.clear();
        WRITE_REPO.run(
                "MERGE (b:BuildMeta {name:'build'}) " +
                        "SET b.buildSeq = $seq, b.building = true, b.updatedAt = timestamp()",
                Map.of("seq", nextSeq),
                30_000L
        );
        clearSemanticCache();
    }

    public static void finalizeBuild() {
        long seq = BUILD_SEQ.get();
        BUILDING.set(false);
        WRITE_REPO.run(
                "MERGE (b:BuildMeta {name:'build'}) " +
                        "SET b.buildSeq = $seq, b.building = false, b.updatedAt = timestamp()",
                Map.of("seq", seq),
                30_000L
        );
        WRITE_REPO.run(
                "MERGE (g:BuildMeta {name:'graph_projection'}) " +
                        "SET g.buildSeq = $seq, g.updatedAt = timestamp()",
                Map.of("seq", seq),
                30_000L
        );
        GraphStore.invalidateCache();
    }

    public static void clearAllData() {
        try {
            STORE.clearAll(180_000L);
            ID_SEQ.clearCache();
            SCHEMA.init();
            ensureBuildMetaNodes();
            BUILD_SEQ.set(0L);
            BUILDING.set(false);
            METHOD_CACHE.clear();
            CALLSITE_NODE_CACHE.clear();
            GraphStore.invalidateCache();
        } catch (Exception ex) {
            logger.warn("clear neo4j data error: {}", ex.toString());
        }
    }

    public static void selectDatabase(String databaseName) {
        String projectKey = normalizeProjectKey(databaseName);
        Path targetHome = resolveProjectStoreHome(projectKey);
        Path currentHome = STORE.activeHomeDir();
        String currentDb = safe(STORE.activeDatabaseName());
        if (targetHome.equals(currentHome) && "neo4j".equalsIgnoreCase(currentDb)) {
            return;
        }
        if (!STORE.switchStore(targetHome, "neo4j")) {
            logger.warn("switch neo4j project store failed: project={}, home={}", projectKey, targetHome);
            return;
        }
        SCHEMA.init();
        ID_SEQ.clearCache();
        ensureBuildMetaNodes();
        syncBuildSeqFromStore();
        BUILDING.set(false);
        METHOD_CACHE.clear();
        CALLSITE_NODE_CACHE.clear();
        GraphStore.invalidateCache();
        logger.info("switched neo4j project store: project={}, home={}", projectKey, targetHome);
    }

    public static String activeProjectKey() {
        Path activeHome = STORE.activeHomeDir();
        Path root = resolveProjectsRoot();
        if (activeHome != null && root != null) {
            try {
                Path normalizedHome = activeHome.toAbsolutePath().normalize();
                Path normalizedRoot = root.toAbsolutePath().normalize();
                if (normalizedHome.startsWith(normalizedRoot)) {
                    Path relative = normalizedRoot.relativize(normalizedHome);
                    if (relative.getNameCount() > 0) {
                        return normalizeProjectKey(relative.getName(0).toString());
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return DEFAULT_PROJECT_KEY;
    }

    public static Path activeProjectHome() {
        return STORE.activeHomeDir();
    }

    public static List<Map<String, Object>> listProjectStores() {
        Path root = resolveProjectsRoot();
        String active = activeProjectKey();
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            logger.debug("create project root fail: {}", ex.toString());
            return out;
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName() == null ? "" : path.getFileName().toString())
                    .filter(name -> !name.isBlank())
                    .forEach(names::add);
        } catch (IOException ex) {
            logger.debug("list project stores fail: {}", ex.toString());
        }
        Path defaultHome = resolveProjectStoreHome(DEFAULT_PROJECT_KEY);
        if (Files.exists(defaultHome) && !names.contains(DEFAULT_PROJECT_KEY)) {
            names.add(DEFAULT_PROJECT_KEY);
        }
        names.sort(String::compareTo);
        for (String name : names) {
            Path home = resolveProjectStoreHome(name);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("project", name);
            item.put("home", home.toString());
            item.put("active", name.equals(active));
            item.put("hasStore", Files.exists(home.resolve("databases").resolve("neo4j")));
            out.add(item);
        }
        if (out.stream().noneMatch(item -> safe(item.get("project")).equals(active))) {
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("project", active);
            Path home = STORE.activeHomeDir();
            current.put("home", home == null ? "" : home.toAbsolutePath().normalize().toString());
            current.put("active", true);
            current.put("hasStore", home != null && Files.exists(home.resolve("databases").resolve("neo4j")));
            out.add(0, current);
        }
        return out;
    }

    public static boolean deleteProjectStore(String projectKey) {
        String target = normalizeProjectKey(projectKey);
        if (DEFAULT_PROJECT_KEY.equals(target) && DEFAULT_PROJECT_KEY.equals(activeProjectKey())) {
            logger.warn("refuse deleting active default project store");
            return false;
        }
        if (target.equals(activeProjectKey())) {
            selectDatabase(DEFAULT_PROJECT_KEY);
            if (target.equals(activeProjectKey())) {
                logger.warn("cannot delete active project store: {}", target);
                return false;
            }
        }
        Path targetHome = resolveProjectStoreHome(target);
        if (!Files.exists(targetHome)) {
            return true;
        }
        return deleteDirectory(targetHome);
    }

    public static void saveProjectModel(ProjectModel model) {
        if (model == null) {
            return;
        }
        long buildSeq = BUILD_SEQ.get();
        if (buildSeq <= 0) {
            return;
        }
        WRITE_REPO.write(120_000L, tx -> {
            tx.execute("MATCH (n:ProjectModel) WHERE n.buildSeq = $buildSeq DETACH DELETE n", Map.of("buildSeq", buildSeq));
            tx.execute("MATCH (n:ProjectRoot) WHERE n.buildSeq = $buildSeq DETACH DELETE n", Map.of("buildSeq", buildSeq));
            tx.execute("MATCH (n:ProjectEntry) WHERE n.buildSeq = $buildSeq DETACH DELETE n", Map.of("buildSeq", buildSeq));
            Map<String, Object> modelParams = new HashMap<>();
            modelParams.put("buildSeq", buildSeq);
            modelParams.put("buildMode", model.buildMode() == null ? "artifact" : model.buildMode().value());
            modelParams.put("projectName", resolveProjectName(model));
            modelParams.put("primaryInputPath", pathToString(model.primaryInputPath()));
            modelParams.put("runtimePath", pathToString(model.runtimePath()));
            modelParams.put("resolveInnerJars", model.resolveInnerJars());
            tx.execute("CREATE (m:ProjectModel {buildSeq:$buildSeq, buildMode:$buildMode, projectName:$projectName, " +
                            "primaryInputPath:$primaryInputPath, runtimePath:$runtimePath, resolveInnerJars:$resolveInnerJars, updatedAt:timestamp()})",
                    modelParams);

            List<ProjectRootPath> rootPaths = new ArrayList<>();
            if (model.roots() != null) {
                for (ProjectRoot root : model.roots()) {
                    if (root == null || root.path() == null) {
                        continue;
                    }
                    long rootId = ID_SEQ.next("project_root");
                    Map<String, Object> rootParams = new HashMap<>();
                    rootParams.put("rootId", rootId);
                    rootParams.put("buildSeq", buildSeq);
                    rootParams.put("rootKind", root.kind() == null ? "" : root.kind().value());
                    rootParams.put("originKind", root.origin() == null ? "" : root.origin().value());
                    rootParams.put("rootPath", pathToString(root.path()));
                    rootParams.put("presentableName", safeString(root.presentableName()));
                    rootParams.put("archive", root.archive());
                    rootParams.put("test", root.test());
                    rootParams.put("priority", root.priority());
                    tx.execute("CREATE (r:ProjectRoot {rootId:$rootId, buildSeq:$buildSeq, rootKind:$rootKind, originKind:$originKind, " +
                            "rootPath:$rootPath, presentableName:$presentableName, archive:$archive, test:$test, priority:$priority})",
                            rootParams);
                    tx.execute("MATCH (m:ProjectModel {buildSeq:$buildSeq}), (r:ProjectRoot {rootId:$rootId}) MERGE (m)-[:HAS_ROOT]->(r)",
                            Map.of("buildSeq", buildSeq, "rootId", rootId));
                    rootPaths.add(new ProjectRootPath(normalizePath(root.path()), (int) rootId,
                            root.origin() == null ? "app" : root.origin().value(),
                            root.kind() == null ? "" : root.kind().value()));
                }
            }
            rootPaths.sort((a, b) -> Integer.compare(pathDepth(b.path), pathDepth(a.path)));

            if (model.roots() != null) {
                for (ProjectRoot root : model.roots()) {
                    if (root == null || root.path() == null) {
                        continue;
                    }
                    long entryId = ID_SEQ.next("project_entry");
                    ProjectRootPath matched = matchExactRoot(rootPaths, root);
                    Map<String, Object> params = new HashMap<>();
                    params.put("entryId", entryId);
                    params.put("buildSeq", buildSeq);
                    params.put("rootId", matched == null ? -1 : matched.rootId);
                    params.put("entryKind", "root");
                    params.put("originKind", root.origin() == null ? "" : root.origin().value());
                    params.put("entryPath", pathToString(root.path()));
                    tx.execute("CREATE (e:ProjectEntry {entryId:$entryId, buildSeq:$buildSeq, rootId:$rootId, entryKind:$entryKind, " +
                                    "originKind:$originKind, entryPath:$entryPath})",
                            params);
                }
            }
            if (model.analyzedArchives() != null) {
                for (Path archive : model.analyzedArchives()) {
                    if (archive == null) {
                        continue;
                    }
                    long entryId = ID_SEQ.next("project_entry");
                    ProjectRootPath matched = matchRootPath(rootPaths, archive);
                    Map<String, Object> params = new HashMap<>();
                    params.put("entryId", entryId);
                    params.put("buildSeq", buildSeq);
                    params.put("rootId", matched == null ? -1 : matched.rootId);
                    params.put("entryKind", "archive");
                    params.put("originKind", matched == null ? "app" : matched.origin);
                    params.put("entryPath", pathToString(archive));
                    tx.execute("CREATE (e:ProjectEntry {entryId:$entryId, buildSeq:$buildSeq, rootId:$rootId, entryKind:$entryKind, " +
                                    "originKind:$originKind, entryPath:$entryPath})",
                            params);
                }
            }
            return null;
        });
    }

    public static void saveDFS(DFSResultEntity dfsResultEntity) {
        if (dfsResultEntity == null) {
            return;
        }
        long id = ID_SEQ.next("dfs_job");
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("sourceClassName", safe(dfsResultEntity.getSourceClassName()));
        params.put("sourceMethodName", safe(dfsResultEntity.getSourceMethodName()));
        params.put("sourceMethodDesc", safe(dfsResultEntity.getSourceMethodDesc()));
        params.put("sinkClassName", safe(dfsResultEntity.getSinkClassName()));
        params.put("sinkMethodName", safe(dfsResultEntity.getSinkMethodName()));
        params.put("sinkMethodDesc", safe(dfsResultEntity.getSinkMethodDesc()));
        params.put("dfsDepth", dfsResultEntity.getDfsDepth());
        params.put("dfsMode", dfsResultEntity.getDfsMode());
        params.put("dfsListUid", safe(dfsResultEntity.getDfsListUid()));
        WRITE_REPO.run("CREATE (:DfsJob {id:$id, sourceClassName:$sourceClassName, sourceMethodName:$sourceMethodName, " +
                        "sourceMethodDesc:$sourceMethodDesc, sinkClassName:$sinkClassName, sinkMethodName:$sinkMethodName, " +
                        "sinkMethodDesc:$sinkMethodDesc, dfsDepth:$dfsDepth, dfsMode:$dfsMode, dfsListUid:$dfsListUid, createdAt:timestamp()})",
                params,
                30_000L);
    }

    public static void saveDFSList(DFSResultListEntity dfsResultListEntity) {
        if (dfsResultListEntity == null) {
            return;
        }
        long id = ID_SEQ.next("dfs_path");
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("dfsListUid", safe(dfsResultListEntity.getDfsListUid()));
        params.put("dfsListIndex", dfsResultListEntity.getDfsListIndex());
        params.put("dfsClassName", safe(dfsResultListEntity.getDfsClassName()));
        params.put("dfsMethodName", safe(dfsResultListEntity.getDfsMethodName()));
        params.put("dfsMethodDesc", safe(dfsResultListEntity.getDfsMethodDesc()));
        WRITE_REPO.run("CREATE (:DfsPath {id:$id, dfsListUid:$dfsListUid, dfsListIndex:$dfsListIndex, " +
                        "dfsClassName:$dfsClassName, dfsMethodName:$dfsMethodName, dfsMethodDesc:$dfsMethodDesc, createdAt:timestamp()})",
                params,
                30_000L);
    }

    public static void saveJar(String jarPath) {
        if (jarPath == null || jarPath.isBlank()) {
            return;
        }
        String jarName;
        if (OSUtil.isWindows()) {
            String[] temp = jarPath.split("\\\\");
            jarName = temp[temp.length - 1];
        } else {
            String[] temp = jarPath.split("/");
            jarName = temp[temp.length - 1];
        }
        WRITE_REPO.write(30_000L, tx -> {
            try (Result rs = tx.execute("MATCH (j:Jar {jarAbsPath:$path}) RETURN j.jid AS jid LIMIT 1", Map.of("path", jarPath))) {
                if (rs.hasNext()) {
                    return null;
                }
            }
            long jid = ID_SEQ.next("jar");
            tx.execute("CREATE (:Jar {jid:$jid, jarName:$jarName, jarAbsPath:$jarAbsPath})",
                    Map.of("jid", jid, "jarName", jarName, "jarAbsPath", jarPath));
            return null;
        });
    }

    public static JarEntity getJarId(String jarPath) {
        if (jarPath == null || jarPath.isBlank()) {
            return null;
        }
        Map<String, Object> row = READ_REPO.one(
                "MATCH (j:Jar {jarAbsPath:$path}) RETURN j.jid AS jid, j.jarName AS jarName, j.jarAbsPath AS jarAbsPath LIMIT 1",
                Map.of("path", jarPath),
                30_000L);
        if (row.isEmpty()) {
            return null;
        }
        JarEntity entity = new JarEntity();
        entity.setJid(asInt(row.get("jid"), -1));
        entity.setJarName(safe(row.get("jarName")));
        entity.setJarAbsPath(safe(row.get("jarAbsPath")));
        return entity;
    }

    public static void saveClassFiles(Set<ClassFileEntity> classFileList) {
        if (classFileList == null || classFileList.isEmpty()) {
            logger.info("total class file: 0");
            return;
        }
        logger.info("total class file: {}", classFileList.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        long start = ID_SEQ.reserve("class_file", classFileList.size());
        long idx = 0L;
        for (ClassFileEntity classFile : classFileList) {
            if (classFile == null || classFile.getPath() == null) {
                continue;
            }
            classFile.setPathStr(classFile.getPath().toAbsolutePath().toString());
            int jarId = classFile.getJarId() == null ? -1 : classFile.getJarId();
            Map<String, Object> row = new HashMap<>();
            row.put("cfid", start + idx++);
            row.put("className", safe(classFile.getClassName()));
            row.put("pathStr", safe(classFile.getPathStr()));
            row.put("jarName", safe(classFile.getJarName()));
            row.put("jarId", jarId);
            rows.add(row);
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (c:ClassFile {className:row.className, jarId:row.jarId, pathStr:row.pathStr}) " +
                            "ON CREATE SET c.cfid = row.cfid, c.jarName = row.jarName " +
                            "SET c.jarName = row.jarName",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save class file finish");
    }

    public static void saveClassInfo(Set<ClassReference> discoveredClasses) {
        if (discoveredClasses == null || discoveredClasses.isEmpty()) {
            logger.info("total class: 0");
            return;
        }
        logger.info("total class: {}", discoveredClasses.size());

        List<Map<String, Object>> classRows = new ArrayList<>();
        List<Map<String, Object>> memberRows = new ArrayList<>();
        List<Map<String, Object>> annoRows = new ArrayList<>();
        List<Map<String, Object>> interfaceRows = new ArrayList<>();

        long classStart = ID_SEQ.reserve("class", discoveredClasses.size());
        long classIdx = 0L;

        int memberCount = 0;
        int annoCount = 0;
        int interfaceCount = 0;
        for (ClassReference reference : discoveredClasses) {
            if (reference == null) {
                continue;
            }
            Map<String, Object> classRow = new HashMap<>();
            classRow.put("cid", classStart + classIdx++);
            classRow.put("jarName", safe(reference.getJarName()));
            classRow.put("jarId", reference.getJarId());
            classRow.put("version", safe(reference.getVersion()));
            classRow.put("access", safe(reference.getAccess()));
            classRow.put("className", safe(reference.getName()));
            classRow.put("superClassName", safe(reference.getSuperClass()));
            classRow.put("isInterface", reference.isInterface());
            classRows.add(classRow);

            for (ClassReference.Member member : reference.getMembers()) {
                Map<String, Object> row = new HashMap<>();
                row.put("className", safe(reference.getName()));
                row.put("jarId", reference.getJarId());
                row.put("memberName", safe(member.getName()));
                row.put("modifiers", member.getModifiers());
                row.put("value", safe(member.getValue()));
                row.put("typeClassName", member.getType() == null ? "" : safe(member.getType().getName()));
                row.put("methodDesc", safe(member.getDesc()));
                row.put("methodSignature", safe(member.getSignature()));
                memberRows.add(row);
                memberCount++;
            }
            for (AnnoReference anno : reference.getAnnotations()) {
                Map<String, Object> row = new HashMap<>();
                row.put("className", safe(reference.getName()));
                row.put("jarId", reference.getJarId());
                row.put("annoName", safe(anno.getAnnoName()));
                row.put("visible", anno.getVisible() ? 1 : 0);
                row.put("parameter", anno.getParameter());
                row.put("methodName", "");
                row.put("scope", "class");
                annoRows.add(row);
                annoCount++;
            }
            for (String inter : reference.getInterfaces()) {
                Map<String, Object> row = new HashMap<>();
                row.put("className", safe(reference.getName()));
                row.put("jarId", reference.getJarId());
                row.put("interfaceName", safe(inter));
                interfaceRows.add(row);
                interfaceCount++;
            }
        }

        long memberStart = memberCount <= 0 ? 1 : ID_SEQ.reserve("member", memberCount);
        long annoStart = annoCount <= 0 ? 1 : ID_SEQ.reserve("annotation", annoCount);
        long interfaceStart = interfaceCount <= 0 ? 1 : ID_SEQ.reserve("interface", interfaceCount);

        for (int i = 0; i < memberRows.size(); i++) {
            memberRows.get(i).put("memberId", memberStart + i);
        }
        for (int i = 0; i < annoRows.size(); i++) {
            annoRows.get(i).put("annoId", annoStart + i);
        }
        for (int i = 0; i < interfaceRows.size(); i++) {
            interfaceRows.get(i).put("interfaceId", interfaceStart + i);
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(classRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (c:Class {className:row.className, jarId:row.jarId}) " +
                            "ON CREATE SET c.cid = row.cid " +
                            "SET c.jarName = row.jarName, c.version = row.version, c.access = row.access, " +
                            "c.superClassName = row.superClassName, c.isInterface = row.isInterface",
                    part,
                    "rows",
                    120_000L
            );
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(memberRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (m:Member {className:row.className, jarId:row.jarId, memberName:row.memberName, methodDesc:row.methodDesc}) " +
                            "ON CREATE SET m.memberId = row.memberId " +
                            "SET m.modifiers = row.modifiers, m.value = row.value, m.typeClassName = row.typeClassName, " +
                            "m.methodSignature = row.methodSignature",
                    part,
                    "rows",
                    120_000L
            );
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(annoRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (a:Annotation {className:row.className, jarId:row.jarId, annoName:row.annoName, methodName:row.methodName, scope:row.scope}) " +
                            "ON CREATE SET a.annoId = row.annoId " +
                            "SET a.visible = row.visible, a.parameter = row.parameter",
                    part,
                    "rows",
                    120_000L
            );
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(interfaceRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (i:InterfaceDef {className:row.className, jarId:row.jarId, interfaceName:row.interfaceName}) " +
                            "ON CREATE SET i.interfaceId = row.interfaceId",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save class info success");
    }

    public static void saveMethods(Set<MethodReference> discoveredMethods) {
        if (discoveredMethods == null || discoveredMethods.isEmpty()) {
            logger.info("total method: 0");
            return;
        }
        logger.info("total method: {}", discoveredMethods.size());

        List<Map<String, Object>> methodRows = new ArrayList<>();
        List<Map<String, Object>> annoRows = new ArrayList<>();
        long buildSeq = BUILD_SEQ.get();

        long methodStart = ID_SEQ.reserve("method", discoveredMethods.size());
        long graphNodeStart = ID_SEQ.reserve("graph_node", discoveredMethods.size());
        long methodIdx = 0L;

        int annoCount = 0;
        for (MethodReference reference : discoveredMethods) {
            if (reference == null || reference.getClassReference() == null) {
                continue;
            }
            long mid = methodStart + methodIdx;
            long graphNodeId = graphNodeStart + methodIdx;
            methodIdx++;

            String className = safe(reference.getClassReference().getName());
            String methodName = safe(reference.getName());
            String methodDesc = safe(reference.getDesc());
            int jarId = reference.getJarId();

            Map<String, Object> row = new HashMap<>();
            row.put("mid", mid);
            row.put("graphNodeId", graphNodeId);
            row.put("methodName", methodName);
            row.put("methodDesc", methodDesc);
            row.put("className", className);
            row.put("isStaticInt", reference.isStatic() ? 1 : 0);
            row.put("accessInt", reference.getAccess());
            row.put("lineNumber", reference.getLineNumber());
            row.put("jarId", jarId);
            row.put("jarName", safe(reference.getJarName()));
            row.put("methodKey", methodKey(className, methodName, methodDesc, jarId));
            row.put("buildSeq", buildSeq);
            methodRows.add(row);

            METHOD_CACHE.put(methodKey(className, methodName, methodDesc, jarId), new MethodIdentity(mid, graphNodeId));

            for (AnnoReference anno : reference.getAnnotations()) {
                Map<String, Object> annoRow = new HashMap<>();
                annoRow.put("className", className);
                annoRow.put("methodName", methodName);
                annoRow.put("jarId", jarId);
                annoRow.put("annoName", safe(anno.getAnnoName()));
                annoRow.put("visible", anno.getVisible() ? 1 : 0);
                annoRow.put("parameter", anno.getParameter());
                annoRow.put("scope", "method");
                annoRows.add(annoRow);
                annoCount++;
            }
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(methodRows, Math.max(1, PART_SIZE))) {
            NATIVE_BULK.upsertMethods(part, 180_000L);
        }

        if (!annoRows.isEmpty()) {
            long annoStart = ID_SEQ.reserve("annotation", annoCount);
            for (int i = 0; i < annoRows.size(); i++) {
                annoRows.get(i).put("annoId", annoStart + i);
            }
            for (List<Map<String, Object>> part : PartitionUtils.partition(annoRows, Math.max(1, PART_SIZE))) {
                WRITE_REPO.runBatched(
                        "UNWIND $rows AS row " +
                                "MERGE (a:Annotation {className:row.className, jarId:row.jarId, annoName:row.annoName, methodName:row.methodName, scope:row.scope}) " +
                                "ON CREATE SET a.annoId = row.annoId " +
                                "SET a.visible = row.visible, a.parameter = row.parameter",
                        part,
                        "rows",
                        120_000L
                );
            }
        }
        logger.info("save method success");
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
        if (methodCallMeta == null || methodCallMeta.isEmpty()) {
            throw new IllegalStateException("method call metadata is required in strict mode");
        }

        Map<String, String> callSiteKeyByEdge = buildPrimaryCallSiteByEdge(callSites);
        long buildSeq = BUILD_SEQ.get();

        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0;
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> call : methodCalls.entrySet()) {
            MethodReference.Handle caller = call.getKey();
            HashSet<MethodReference.Handle> calleeSet = call.getValue();
            if (caller == null || calleeSet == null || calleeSet.isEmpty()) {
                continue;
            }
            ClassReference callerClass = classMap == null ? null : classMap.get(caller.getClassReference());
            int callerJarId = callerClass == null ? -1 : callerClass.getJarId();
            String callerJarName = callerClass == null ? "" : safe(callerClass.getJarName());

            for (MethodReference.Handle callee : calleeSet) {
                MethodCallMeta meta = resolveMethodCallMeta(methodCallMeta, caller, callee);
                if (meta == null) {
                    throw new IllegalStateException("missing method call metadata for edge: " + edgeLabel(caller, callee));
                }
                ClassReference calleeClass = classMap == null
                        ? notFoundClassReference
                        : classMap.getOrDefault(callee.getClassReference(), notFoundClassReference);
                int opCode = meta.getBestOpcode();
                if (opCode <= 0) {
                    throw new IllegalStateException("missing method call opcode for edge: " + edgeLabel(caller, callee));
                }
                String callSiteKey = callSiteKeyByEdge.get(CallSiteKeyUtil.buildEdgeLookupKey(
                        callerJarId,
                        caller.getClassReference().getName(),
                        caller.getName(),
                        caller.getDesc(),
                        callee.getClassReference().getName(),
                        callee.getName(),
                        callee.getDesc(),
                        opCode
                ));
                if (callSiteKey == null) {
                    callSiteKey = "";
                }

                MethodIdentity callerIdentity = ensureMethodIdentity(
                        caller.getClassReference().getName(), caller.getName(), caller.getDesc(), callerJarId, callerJarName);
                MethodIdentity calleeIdentity = ensureMethodIdentity(
                        callee.getClassReference().getName(), callee.getName(), callee.getDesc(), calleeClass.getJarId(), safe(calleeClass.getJarName()));

                Map<String, Object> row = new HashMap<>();
                row.put("callId", ID_SEQ.next("method_call"));
                row.put("edgeId", ID_SEQ.next("graph_edge"));
                row.put("callerClass", safe(caller.getClassReference().getName()));
                row.put("callerMethod", safe(caller.getName()));
                row.put("callerDesc", safe(caller.getDesc()));
                row.put("callerJarId", callerJarId);
                row.put("callerJarName", callerJarName);
                row.put("callerMid", callerIdentity.mid);
                row.put("callerGraphNodeId", callerIdentity.graphNodeId);

                row.put("calleeClass", safe(callee.getClassReference().getName()));
                row.put("calleeMethod", safe(callee.getName()));
                row.put("calleeDesc", safe(callee.getDesc()));
                row.put("calleeJarId", calleeClass.getJarId());
                row.put("calleeJarName", safe(calleeClass.getJarName()));
                row.put("calleeMid", calleeIdentity.mid);
                row.put("calleeGraphNodeId", calleeIdentity.graphNodeId);

                row.put("opCode", opCode);
                row.put("edgeType", safe(meta.getType()));
                row.put("confidence", safe(meta.getConfidence()));
                row.put("evidence", safe(meta.getEvidence()));
                row.put("callSiteKey", callSiteKey);
                row.put("relType", GraphRelationType.fromEdgeType(meta.getType()).name());
                row.put("buildSeq", buildSeq);
                rows.add(row);
                total++;
            }
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            NATIVE_BULK.upsertMethodCalls(part, 240_000L);
        }
        logger.info("save method call success: {}", total);
    }

    public static void saveMethodCalls(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
                                       Map<ClassReference.Handle, ClassReference> classMap,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        saveMethodCalls(methodCalls, classMap, methodCallMeta, Collections.emptyList());
    }

    public static void saveImpls(Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap,
                                 Map<ClassReference.Handle, ClassReference> classMap) {
        if (implMap == null || implMap.isEmpty()) {
            logger.info("save method impl success");
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> call : implMap.entrySet()) {
            MethodReference.Handle method = call.getKey();
            Set<MethodReference.Handle> impls = call.getValue();
            if (method == null || impls == null) {
                continue;
            }
            for (MethodReference.Handle impl : impls) {
                if (impl == null) {
                    continue;
                }
                ClassReference owner = classMap == null ? null : classMap.get(method.getClassReference());
                ClassReference implOwner = classMap == null ? null : classMap.get(impl.getClassReference());
                Map<String, Object> row = new HashMap<>();
                row.put("implId", ID_SEQ.next("method_impl"));
                row.put("className", safe(method.getClassReference().getName()));
                row.put("methodName", safe(impl.getName()));
                row.put("methodDesc", safe(impl.getDesc()));
                row.put("implClassName", safe(impl.getClassReference().getName()));
                row.put("classJarId", owner == null ? -1 : owner.getJarId());
                row.put("implClassJarId", implOwner == null ? -1 : implOwner.getJarId());
                rows.add(row);
            }
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (m:MethodImpl {className:row.className, methodName:row.methodName, methodDesc:row.methodDesc, implClassName:row.implClassName, classJarId:row.classJarId, implClassJarId:row.implClassJarId}) " +
                            "ON CREATE SET m.implId = row.implId",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save method impl success");
    }

    public static void saveStrMap(Map<MethodReference.Handle, List<String>> strMap,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap,
                                  Map<MethodReference.Handle, MethodReference> methodMap,
                                  Map<ClassReference.Handle, ClassReference> classMap) {
        int normalCount = countStrings(strMap);
        int annoCount = countStrings(stringAnnoMap);
        int totalCount = normalCount + annoCount;
        if (totalCount <= 0) {
            logger.info("save all string success: 0");
            return;
        }

        long stringStart = ID_SEQ.reserve("string", totalCount);
        long stringIdx = 0L;
        List<Map<String, Object>> rows = new ArrayList<>(totalCount);

        stringIdx = appendStringRows(rows, strMap, methodMap, classMap, stringStart, stringIdx);
        appendStringRows(rows, stringAnnoMap, methodMap, classMap, stringStart, stringIdx);

        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "CREATE (:StringLiteral {stringId:row.stringId, methodName:row.methodName, methodDesc:row.methodDesc, access:row.access, className:row.className, value:row.value, jarName:row.jarName, jarId:row.jarId})",
                    part,
                    "rows",
                    180_000L
            );
        }
        logger.info("save all string success: {}", rows.size());
    }

    public static void saveResources(List<ResourceEntity> resources) {
        if (resources == null || resources.isEmpty()) {
            logger.info("resource list is empty");
            return;
        }
        long start = ID_SEQ.reserve("resource", resources.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            ResourceEntity resource = resources.get(i);
            if (resource == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("rid", start + i);
            row.put("resourcePath", safe(resource.getResourcePath()));
            row.put("pathStr", safe(resource.getPathStr()));
            row.put("jarName", safe(resource.getJarName()));
            row.put("jarId", resource.getJarId() == null ? -1 : resource.getJarId());
            row.put("fileSize", resource.getFileSize());
            row.put("isText", resource.getIsText());
            rows.add(row);
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (r:Resource {resourcePath:row.resourcePath, jarId:row.jarId}) " +
                            "ON CREATE SET r.rid = row.rid " +
                            "SET r.pathStr = row.pathStr, r.jarName = row.jarName, r.fileSize = row.fileSize, r.isText = row.isText",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save resources success");
    }

    public static void saveCallSites(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            logger.info("call site list is empty");
            return;
        }
        long start = ID_SEQ.reserve("call_site", callSites.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        List<CallSiteEntity> normalized = new ArrayList<>();
        long buildSeq = BUILD_SEQ.get();
        for (int i = 0; i < callSites.size(); i++) {
            CallSiteEntity site = callSites.get(i);
            if (site == null) {
                continue;
            }
            if (site.getCallSiteKey() == null || site.getCallSiteKey().trim().isEmpty()) {
                site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site));
            }
            normalized.add(site);
            long csId = start + i;
            long graphNodeId = resolveOrCreateCallSiteNodeId(site.getCallSiteKey());
            Map<String, Object> row = new HashMap<>();
            row.put("csId", csId);
            row.put("graphNodeId", graphNodeId);
            row.put("callerClassName", safe(site.getCallerClassName()));
            row.put("callerMethodName", safe(site.getCallerMethodName()));
            row.put("callerMethodDesc", safe(site.getCallerMethodDesc()));
            row.put("calleeOwner", safe(site.getCalleeOwner()));
            row.put("calleeMethodName", safe(site.getCalleeMethodName()));
            row.put("calleeMethodDesc", safe(site.getCalleeMethodDesc()));
            row.put("opCode", site.getOpCode() == null ? -1 : site.getOpCode());
            row.put("lineNumber", site.getLineNumber() == null ? -1 : site.getLineNumber());
            row.put("callIndex", site.getCallIndex() == null ? -1 : site.getCallIndex());
            row.put("receiverType", safe(site.getReceiverType()));
            row.put("jarId", site.getJarId() == null ? -1 : site.getJarId());
            row.put("callSiteKey", safe(site.getCallSiteKey()));
            row.put("buildSeq", buildSeq);
            rows.add(row);
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            NATIVE_BULK.upsertCallSites(part, 180_000L);
        }

        saveCallSiteGraphEdges(normalized);
        logger.info("save call sites success");
    }

    public static void saveLocalVars(List<LocalVarEntity> localVars) {
        if (localVars == null || localVars.isEmpty()) {
            logger.info("local var list is empty");
            return;
        }
        long start = ID_SEQ.reserve("local_var", localVars.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < localVars.size(); i++) {
            LocalVarEntity var = localVars.get(i);
            if (var == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("lvId", start + i);
            row.put("className", safe(var.getClassName()));
            row.put("methodName", safe(var.getMethodName()));
            row.put("methodDesc", safe(var.getMethodDesc()));
            row.put("varIndex", var.getVarIndex() == null ? -1 : var.getVarIndex());
            row.put("varName", safe(var.getVarName()));
            row.put("varDesc", safe(var.getVarDesc()));
            row.put("varSignature", safe(var.getVarSignature()));
            row.put("startLine", var.getStartLine() == null ? -1 : var.getStartLine());
            row.put("endLine", var.getEndLine() == null ? -1 : var.getEndLine());
            row.put("jarId", var.getJarId() == null ? -1 : var.getJarId());
            rows.add(row);
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (v:LocalVar {className:row.className, methodName:row.methodName, methodDesc:row.methodDesc, varIndex:row.varIndex, jarId:row.jarId}) " +
                            "ON CREATE SET v.lvId = row.lvId " +
                            "SET v.varName=row.varName, v.varDesc=row.varDesc, v.varSignature=row.varSignature, v.startLine=row.startLine, v.endLine=row.endLine",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save local vars success");
    }

    public static void saveSpringController(ArrayList<SpringController> controllers) {
        if (controllers == null || controllers.isEmpty()) {
            logger.info("SPRING CONTROLLER 分析错误数据为空");
            return;
        }
        List<Map<String, Object>> controllerRows = new ArrayList<>();
        List<Map<String, Object>> mappingRows = new ArrayList<>();
        for (SpringController controller : controllers) {
            if (controller == null || controller.getClassName() == null || controller.getClassReference() == null) {
                continue;
            }
            Map<String, Object> c = new HashMap<>();
            c.put("className", safe(controller.getClassName().getName()));
            c.put("jarId", controller.getClassReference().getJarId());
            c.put("id", ID_SEQ.next("spring_controller"));
            controllerRows.add(c);

            for (SpringMapping mapping : controller.getMappings()) {
                if (mapping == null || mapping.getMethodName() == null) {
                    continue;
                }
                SpringMethodEntity methodEntity = new SpringMethodEntity();
                methodEntity.setClassName(controller.getClassName().getName());
                methodEntity.setJarId(controller.getClassReference().getJarId());
                methodEntity.setPath(mapping.getPath());
                methodEntity.setMethodName(mapping.getMethodName().getName());
                methodEntity.setMethodDesc(mapping.getMethodName().getDesc());
                if (mapping.getPathRestful() != null && !mapping.getPathRestful().isEmpty()) {
                    methodEntity.setRestfulType(mapping.getPathRestful());
                    initPath(mapping, methodEntity);
                } else {
                    for (AnnoReference annotation : mapping.getMethodReference().getAnnotations()) {
                        if (annotation.getAnnoName().startsWith(SpringConstant.ANNO_PREFIX)) {
                            methodEntity.setRestfulType(annotation.getAnnoName()
                                    .replace(SpringConstant.ANNO_PREFIX, "")
                                    .replace(SpringConstant.MappingAnno, "")
                                    .replace(";", " "));
                            initPath(mapping, methodEntity);
                        }
                    }
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", ID_SEQ.next("spring_mapping"));
                row.put("className", safe(methodEntity.getClassName()));
                row.put("methodName", safe(methodEntity.getMethodName()));
                row.put("methodDesc", safe(methodEntity.getMethodDesc()));
                row.put("path", safe(methodEntity.getPath()));
                row.put("restfulType", safe(methodEntity.getRestfulType()));
                row.put("jarId", methodEntity.getJarId() == null ? -1 : methodEntity.getJarId());
                mappingRows.add(row);
            }
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(controllerRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (c:SpringController {className:row.className, jarId:row.jarId}) " +
                            "ON CREATE SET c.id=row.id",
                    part,
                    "rows",
                    120_000L
            );
        }

        for (List<Map<String, Object>> part : PartitionUtils.partition(mappingRows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (m:SpringMapping {className:row.className, methodName:row.methodName, methodDesc:row.methodDesc, jarId:row.jarId}) " +
                            "ON CREATE SET m.id=row.id " +
                            "SET m.path = CASE WHEN row.path = '' THEN 'none' ELSE row.path END, m.restfulType=row.restfulType",
                    part,
                    "rows",
                    120_000L
            );
        }
        logger.info("save all spring data success");
    }

    public static void saveSpringInterceptor(ArrayList<String> interceptors,
                                             Map<ClassReference.Handle, ClassReference> classMap) {
        if (interceptors == null || interceptors.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String interceptor : interceptors) {
            ClassReference ref = classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(interceptor), notFoundClassReference);
            Map<String, Object> row = new HashMap<>();
            row.put("id", ID_SEQ.next("spring_interceptor"));
            row.put("className", safe(interceptor));
            row.put("jarId", ref.getJarId());
            rows.add(row);
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (i:SpringInterceptor {className:row.className, jarId:row.jarId}) " +
                            "ON CREATE SET i.id=row.id",
                    part,
                    "rows",
                    120_000L
            );
        }
    }

    public static void saveServlets(ArrayList<String> servlets,
                                    Map<ClassReference.Handle, ClassReference> classMap) {
        saveJavaWebEntities(servlets, classMap, "servlet");
    }

    public static void saveFilters(ArrayList<String> filters,
                                   Map<ClassReference.Handle, ClassReference> classMap) {
        saveJavaWebEntities(filters, classMap, "filter");
    }

    public static void saveListeners(ArrayList<String> listeners,
                                     Map<ClassReference.Handle, ClassReference> classMap) {
        saveJavaWebEntities(listeners, classMap, "listener");
    }

    public static void cleanFav() {
        WRITE_REPO.run("MATCH (n:Favorite) DETACH DELETE n", Collections.emptyMap(), 30_000L);
    }

    public static void cleanFavItem(MethodResult m) {
        if (m == null) {
            return;
        }
        WRITE_REPO.run(
                "MATCH (n:Favorite {className:$className, methodName:$methodName, methodDesc:$methodDesc, jarId:$jarId}) DETACH DELETE n",
                Map.of(
                        "className", safe(m.getClassName()),
                        "methodName", safe(m.getMethodName()),
                        "methodDesc", safe(m.getMethodDesc()),
                        "jarId", m.getJarId()
                ),
                30_000L
        );
    }

    public static void addFav(MethodResult m) {
        if (m == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", ID_SEQ.next("favorite"));
        params.put("className", safe(m.getClassName()));
        params.put("methodName", safe(m.getMethodName()));
        params.put("methodDesc", safe(m.getMethodDesc()));
        params.put("jarName", safe(m.getJarName()));
        params.put("jarId", m.getJarId());
        WRITE_REPO.run(
                "MERGE (n:Favorite {className:$className, methodName:$methodName, methodDesc:$methodDesc, jarId:$jarId}) " +
                        "ON CREATE SET n.id=$id, n.jarName=$jarName, n.createdAt=timestamp() " +
                        "SET n.jarName=$jarName, n.updatedAt=timestamp()",
                params,
                30_000L
        );
    }

    public static void insertHistory(MethodResult m) {
        if (m == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", ID_SEQ.next("history"));
        params.put("className", safe(m.getClassName()));
        params.put("methodName", safe(m.getMethodName()));
        params.put("methodDesc", safe(m.getMethodDesc()));
        params.put("jarName", safe(m.getJarName()));
        params.put("jarId", m.getJarId());
        WRITE_REPO.run(
                "CREATE (:History {id:$id, className:$className, methodName:$methodName, methodDesc:$methodDesc, jarName:$jarName, jarId:$jarId, createdAt:timestamp()})",
                params,
                30_000L
        );
    }

    public static void cleanHistory() {
        WRITE_REPO.run("MATCH (n:History) DETACH DELETE n", Collections.emptyMap(), 30_000L);
    }

    public static ArrayList<MethodResult> getAllFavMethods() {
        List<Map<String, Object>> rows = READ_REPO.list(
                "MATCH (n:Favorite) RETURN n.className AS className, n.methodName AS methodName, n.methodDesc AS methodDesc, " +
                        "coalesce(n.jarName,'') AS jarName, coalesce(n.jarId,-1) AS jarId ORDER BY n.createdAt ASC, n.id ASC"
        );
        ArrayList<MethodResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(toMethodResult(row));
        }
        return out;
    }

    public static ArrayList<MethodResult> getAllHisMethods() {
        List<Map<String, Object>> rows = READ_REPO.list(
                "MATCH (n:History) RETURN n.className AS className, n.methodName AS methodName, n.methodDesc AS methodDesc, " +
                        "coalesce(n.jarName,'') AS jarName, coalesce(n.jarId,-1) AS jarId ORDER BY n.createdAt DESC, n.id DESC"
        );
        ArrayList<MethodResult> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(toMethodResult(row));
        }
        return out;
    }

    public static long getBuildSeq() {
        return BUILD_SEQ.get();
    }

    public static void setBuilding(boolean building) {
        BUILDING.set(building);
        WRITE_REPO.run(
                "MERGE (b:BuildMeta {name:'build'}) SET b.building = $building, b.updatedAt = timestamp()",
                Map.of("building", building),
                30_000L
        );
    }

    public static boolean isBuilding() {
        return BUILDING.get();
    }

    public static String getSemanticCacheValue(String cacheKey, String cacheType) {
        if (cacheKey == null || cacheType == null) {
            return null;
        }
        Map<String, Object> row = READ_REPO.one(
                "MATCH (c:SemanticCache {cacheKey:$cacheKey, cacheType:$cacheType}) RETURN c.cacheValue AS cacheValue LIMIT 1",
                Map.of("cacheKey", cacheKey, "cacheType", cacheType),
                30_000L
        );
        if (row.isEmpty()) {
            return null;
        }
        return safe(row.get("cacheValue"));
    }

    public static void putSemanticCacheValue(String cacheKey, String cacheType, String cacheValue) {
        if (cacheKey == null || cacheType == null || cacheValue == null) {
            return;
        }
        WRITE_REPO.run(
                "MERGE (c:SemanticCache {cacheKey:$cacheKey, cacheType:$cacheType}) " +
                        "ON CREATE SET c.id=$id " +
                        "SET c.cacheValue=$cacheValue, c.updatedAt=timestamp()",
                Map.of(
                        "id", ID_SEQ.next("semantic_cache"),
                        "cacheKey", cacheKey,
                        "cacheType", cacheType,
                        "cacheValue", cacheValue
                ),
                30_000L
        );
    }

    public static void clearSemanticCacheType(String cacheType) {
        if (cacheType == null) {
            return;
        }
        WRITE_REPO.run(
                "MATCH (c:SemanticCache {cacheType:$cacheType}) DETACH DELETE c",
                Map.of("cacheType", cacheType),
                30_000L
        );
    }

    public static void clearSemanticCache() {
        WRITE_REPO.run("MATCH (c:SemanticCache) DETACH DELETE c", Collections.emptyMap(), 30_000L);
    }

    public static void saveVulReport(VulReportEntity entity) {
        if (entity == null) {
            return;
        }
        int id = entity.getId() == null || entity.getId() <= 0
                ? (int) ID_SEQ.next("vul_report")
                : entity.getId();
        WRITE_REPO.run(
                "MERGE (v:VulReport {reportId:$reportId}) " +
                        "SET v.type=$type, v.reason=$reason, v.score=$score, v.trace=$trace, v.updatedAt=timestamp()",
                Map.of(
                        "reportId", id,
                        "type", safe(entity.getType()),
                        "reason", safe(entity.getReason()),
                        "score", entity.getScore() == null ? 0 : entity.getScore(),
                        "trace", safe(entity.getTrace())
                ),
                30_000L
        );
    }

    public static List<VulReportEntity> getVulReports() {
        List<Map<String, Object>> rows = READ_REPO.list(
                "MATCH (v:VulReport) RETURN v.reportId AS id, v.type AS type, v.reason AS reason, v.score AS score, v.trace AS trace ORDER BY v.reportId ASC"
        );
        List<VulReportEntity> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            VulReportEntity entity = new VulReportEntity();
            entity.setId(asInt(row.get("id"), 0));
            entity.setType(safe(row.get("type")));
            entity.setReason(safe(row.get("reason")));
            entity.setScore(asInt(row.get("score"), 0));
            entity.setTrace(safe(row.get("trace")));
            out.add(entity);
        }
        return out;
    }

    private static void saveCallSiteGraphEdges(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            return;
        }
        Map<String, List<CallSiteEntity>> grouped = new LinkedHashMap<>();
        List<Map<String, Object>> edgeRows = new ArrayList<>();
        long buildSeq = BUILD_SEQ.get();
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            String key = safe(site.getCallerClassName()) + "#" + safe(site.getCallerMethodName()) + "#" +
                    safe(site.getCallerMethodDesc()) + "#" + (site.getJarId() == null ? -1 : site.getJarId());
            grouped.computeIfAbsent(key, ignore -> new ArrayList<>()).add(site);

            int callerJarId = site.getJarId() == null ? -1 : site.getJarId();
            MethodIdentity caller = ensureMethodIdentity(site.getCallerClassName(), site.getCallerMethodName(), site.getCallerMethodDesc(), callerJarId, "");
            MethodIdentity callee = ensureMethodIdentity(site.getCalleeOwner(), site.getCalleeMethodName(), site.getCalleeMethodDesc(), -1, "");
            long callSiteNode = resolveOrCreateCallSiteNodeId(site.getCallSiteKey());
            long containEdgeId = ID_SEQ.next("graph_edge");
            long calleeEdgeId = ID_SEQ.next("graph_edge");
            int opCode = site.getOpCode() == null ? -1 : site.getOpCode();
            if (caller.graphNodeId > 0 && callSiteNode > 0) {
                Map<String, Object> contain = new HashMap<>();
                contain.put("src", caller.graphNodeId);
                contain.put("dst", callSiteNode);
                contain.put("edgeId", containEdgeId);
                contain.put("relType", "CONTAINS_CALLSITE");
                contain.put("confidence", "high");
                contain.put("evidence", "callsite");
                contain.put("opCode", opCode);
                contain.put("buildSeq", buildSeq);
                edgeRows.add(contain);
            }
            if (callSiteNode > 0 && callee.graphNodeId > 0) {
                Map<String, Object> callToCallee = new HashMap<>();
                callToCallee.put("src", callSiteNode);
                callToCallee.put("dst", callee.graphNodeId);
                callToCallee.put("edgeId", calleeEdgeId);
                callToCallee.put("relType", "CALLSITE_TO_CALLEE");
                callToCallee.put("confidence", "medium");
                callToCallee.put("evidence", "bytecode");
                callToCallee.put("opCode", opCode);
                callToCallee.put("buildSeq", buildSeq);
                edgeRows.add(callToCallee);
            }
        }

        for (List<CallSiteEntity> rows : grouped.values()) {
            rows.sort((a, b) -> {
                int ai = normalizeInt(a == null ? null : a.getCallIndex());
                int bi = normalizeInt(b == null ? null : b.getCallIndex());
                if (ai != bi) {
                    return Integer.compare(ai, bi);
                }
                int al = normalizeInt(a == null ? null : a.getLineNumber());
                int bl = normalizeInt(b == null ? null : b.getLineNumber());
                return Integer.compare(al, bl);
            });
            for (int i = 0; i + 1 < rows.size(); i++) {
                CallSiteEntity cur = rows.get(i);
                CallSiteEntity next = rows.get(i + 1);
                long curNode = resolveOrCreateCallSiteNodeId(cur.getCallSiteKey());
                long nextNode = resolveOrCreateCallSiteNodeId(next.getCallSiteKey());
                if (curNode <= 0 || nextNode <= 0 || curNode == nextNode) {
                    continue;
                }
                Map<String, Object> nextEdge = new HashMap<>();
                nextEdge.put("src", curNode);
                nextEdge.put("dst", nextNode);
                nextEdge.put("edgeId", ID_SEQ.next("graph_edge"));
                nextEdge.put("relType", "NEXT_CALLSITE");
                nextEdge.put("confidence", "high");
                nextEdge.put("evidence", "order");
                nextEdge.put("opCode", -1);
                nextEdge.put("buildSeq", buildSeq);
                edgeRows.add(nextEdge);
            }
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(edgeRows, Math.max(1, PART_SIZE))) {
            NATIVE_BULK.upsertGraphEdges(part, 120_000L);
        }
    }

    private static void saveJavaWebEntities(ArrayList<String> classes,
                                            Map<ClassReference.Handle, ClassReference> classMap,
                                            String kind) {
        if (classes == null || classes.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String className : classes) {
            ClassReference ref = classMap == null ? notFoundClassReference :
                    classMap.getOrDefault(new ClassReference.Handle(className), notFoundClassReference);
            Map<String, Object> row = new HashMap<>();
            row.put("id", ID_SEQ.next("java_web"));
            row.put("className", safe(className));
            row.put("jarId", ref.getJarId());
            row.put("kind", safe(kind));
            rows.add(row);
        }
        for (List<Map<String, Object>> part : PartitionUtils.partition(rows, Math.max(1, PART_SIZE))) {
            WRITE_REPO.runBatched(
                    "UNWIND $rows AS row " +
                            "MERGE (j:JavaWebEndpoint {className:row.className, jarId:row.jarId, kind:row.kind}) " +
                            "ON CREATE SET j.id=row.id",
                    part,
                    "rows",
                    120_000L
            );
        }
    }

    private static long appendStringRows(List<Map<String, Object>> rows,
                                         Map<MethodReference.Handle, List<String>> strMap,
                                         Map<MethodReference.Handle, MethodReference> methodMap,
                                         Map<ClassReference.Handle, ClassReference> classMap,
                                         long stringStart,
                                         long startIdx) {
        long idx = startIdx;
        if (strMap == null || strMap.isEmpty()) {
            return idx;
        }
        for (Map.Entry<MethodReference.Handle, List<String>> entry : strMap.entrySet()) {
            MethodReference.Handle method = entry.getKey();
            List<String> values = entry.getValue();
            MethodReference mr = methodMap == null ? null : methodMap.get(method);
            ClassReference cr = mr == null || classMap == null ? null : classMap.get(mr.getClassReference());
            if (mr == null || cr == null || values == null || values.isEmpty()) {
                continue;
            }
            for (String value : values) {
                Map<String, Object> row = new HashMap<>();
                row.put("stringId", stringStart + idx++);
                row.put("methodName", safe(mr.getName()));
                row.put("methodDesc", safe(mr.getDesc()));
                row.put("access", mr.getAccess());
                row.put("className", safe(cr.getName()));
                row.put("value", safe(value));
                row.put("jarName", safe(cr.getJarName()));
                row.put("jarId", cr.getJarId());
                rows.add(row);
            }
        }
        return idx;
    }

    private static int countStrings(Map<MethodReference.Handle, List<String>> map) {
        if (map == null || map.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (List<String> list : map.values()) {
            if (list != null) {
                count += list.size();
            }
        }
        return count;
    }

    private static MethodIdentity ensureMethodIdentity(String className,
                                                       String methodName,
                                                       String methodDesc,
                                                       int jarId,
                                                       String jarName) {
        String key = methodKey(className, methodName, methodDesc, jarId);
        MethodIdentity identity = METHOD_CACHE.get(key);
        if (identity != null) {
            return identity;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("className", safe(className));
        params.put("methodName", safe(methodName));
        params.put("methodDesc", safe(methodDesc));
        params.put("jarId", jarId);
        params.put("methodKey", key);

        Map<String, Object> row = READ_REPO.one(
                "MATCH (m:Method) WHERE m.methodKey=$methodKey OR (m.className=$className AND m.methodName=$methodName AND m.methodDesc=$methodDesc AND m.jarId=$jarId) " +
                        "RETURN coalesce(m.mid,-1) AS mid, coalesce(m.graphNodeId,-1) AS graphNodeId LIMIT 1",
                params,
                30_000L
        );
        long mid = asLong(row.get("mid"), -1L);
        long graphNodeId = asLong(row.get("graphNodeId"), -1L);
        if (mid <= 0L) {
            mid = ID_SEQ.next("method");
        }
        if (graphNodeId <= 0L) {
            graphNodeId = ID_SEQ.next("graph_node");
        }

        Map<String, Object> upsert = new HashMap<>();
        upsert.put("className", safe(className));
        upsert.put("methodName", safe(methodName));
        upsert.put("methodDesc", safe(methodDesc));
        upsert.put("jarId", jarId);
        upsert.put("jarName", safe(jarName));
        upsert.put("methodKey", key);
        upsert.put("mid", mid);
        upsert.put("graphNodeId", graphNodeId);
        WRITE_REPO.run(
                "MERGE (m:Method {methodKey:$methodKey}) " +
                        "ON CREATE SET m.className=$className, m.methodName=$methodName, m.methodDesc=$methodDesc, m.jarId=$jarId, " +
                        "m.mid=$mid, m.graphNodeId=$graphNodeId, m.jarName=$jarName, m.isStaticInt=0, m.accessInt=0, m.lineNumber=-1 " +
                        "SET m.className=coalesce(m.className,$className), m.methodName=coalesce(m.methodName,$methodName), " +
                        "m.methodDesc=coalesce(m.methodDesc,$methodDesc), m.jarId=coalesce(m.jarId,$jarId), " +
                        "m.mid=coalesce(m.mid,$mid), m.graphNodeId=coalesce(m.graphNodeId,$graphNodeId), " +
                        "m.jarName=CASE WHEN $jarName = '' THEN coalesce(m.jarName,'') ELSE $jarName END",
                upsert,
                30_000L
        );
        WRITE_REPO.run(
                "MERGE (g:GraphNode:MethodNode {nodeId:$nodeId}) " +
                        "SET g.kind='method', g.jarId=$jarId, g.className=$className, g.methodName=$methodName, g.methodDesc=$methodDesc, " +
                        "g.callSiteKey='', g.lineNumber=-1, g.callIndex=-1, g.buildSeq=$buildSeq",
                Map.of(
                        "nodeId", graphNodeId,
                        "jarId", jarId,
                        "className", safe(className),
                        "methodName", safe(methodName),
                        "methodDesc", safe(methodDesc),
                        "buildSeq", BUILD_SEQ.get()
                ),
                30_000L
        );

        MethodIdentity resolved = new MethodIdentity(mid, graphNodeId);
        METHOD_CACHE.put(key, resolved);
        return resolved;
    }

    private static long resolveOrCreateCallSiteNodeId(String callSiteKey) {
        String key = safe(callSiteKey);
        if (key.isEmpty()) {
            return -1L;
        }
        Long existing = CALLSITE_NODE_CACHE.get(key);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> row = READ_REPO.one(
                "MATCH (g:GraphNode {callSiteKey:$callSiteKey}) WHERE toLower(coalesce(g.kind,''))='callsite' " +
                        "RETURN g.nodeId AS nodeId LIMIT 1",
                Map.of("callSiteKey", key),
                30_000L
        );
        long nodeId = asLong(row.get("nodeId"), -1L);
        if (nodeId <= 0L) {
            nodeId = ID_SEQ.next("graph_node");
        }
        CALLSITE_NODE_CACHE.putIfAbsent(key, nodeId);
        return nodeId;
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

    private static void initPath(SpringMapping mapping, SpringMethodEntity me) {
        if (StrUtil.isBlank(mapping.getPath()) &&
                StrUtil.isNotBlank(mapping.getController().getBasePath())) {
            me.setPath(mapping.getController().getBasePath());
        }
        if (StrUtil.isNotBlank(mapping.getPath()) && mapping.getPath().endsWith("/")) {
            me.setPath(mapping.getPath().substring(0, mapping.getPath().length() - 1));
        }
    }

    private static String methodKey(String className, String methodName, String methodDesc, int jarId) {
        return safe(className) + "#" + safe(methodName) + "#" + safe(methodDesc) + "#" + jarId;
    }

    private static MethodResult toMethodResult(Map<String, Object> row) {
        MethodResult m = new MethodResult();
        m.setClassName(safe(row.get("className")));
        m.setMethodName(safe(row.get("methodName")));
        m.setMethodDesc(safe(row.get("methodDesc")));
        m.setJarName(safe(row.get("jarName")));
        m.setJarId(asInt(row.get("jarId"), -1));
        return m;
    }

    private static String resolveProjectName(ProjectModel model) {
        if (model == null) {
            return "";
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

    private static String pathToString(Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = normalizePath(path);
        return normalized == null ? "" : normalized.toString();
    }

    private static Path resolveProjectsRoot() {
        String raw = System.getProperty(PROJECTS_ROOT_PROP);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(PROJECTS_ROOT_ENV);
        }
        Path root;
        if (raw == null || raw.isBlank()) {
            root = Paths.get(Const.dbDir, "neo4j-projects");
        } else {
            root = Paths.get(raw.trim());
        }
        return root.toAbsolutePath().normalize();
    }

    private static Path resolveProjectStoreHome(String projectKey) {
        String normalized = normalizeProjectKey(projectKey);
        if (DEFAULT_PROJECT_KEY.equals(normalized)) {
            String explicitHome = System.getProperty("jar.analyzer.neo4j.home");
            if (explicitHome != null && !explicitHome.trim().isEmpty()) {
                return Paths.get(explicitHome.trim()).toAbsolutePath().normalize();
            }
            return Paths.get(Const.neo4jHome).toAbsolutePath().normalize();
        }
        return resolveProjectsRoot().resolve(normalized).toAbsolutePath().normalize();
    }

    private static String normalizeProjectKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PROJECT_KEY;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean allow = (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '-'
                    || ch == '.';
            sb.append(allow ? ch : '-');
        }
        String cleaned = sb.toString().replaceAll("[.-]{2,}", "-");
        cleaned = cleaned.replaceAll("^[.-]+", "").replaceAll("[.-]+$", "");
        if (cleaned.isBlank()) {
            return DEFAULT_PROJECT_KEY;
        }
        if (!Character.isLetter(cleaned.charAt(0))) {
            cleaned = "p-" + cleaned;
        }
        if (cleaned.length() > 63) {
            cleaned = cleaned.substring(0, 63);
            cleaned = cleaned.replaceAll("[.-]+$", "");
            if (cleaned.isBlank()) {
                return DEFAULT_PROJECT_KEY;
            }
        }
        if (cleaned.startsWith("system")) {
            cleaned = "p-" + cleaned;
        }
        return cleaned;
    }

    private static boolean deleteDirectory(Path root) {
        if (root == null || !Files.exists(root)) {
            return true;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new IllegalStateException("delete path fail: " + path, ex);
                }
            });
            return true;
        } catch (Exception ex) {
            logger.warn("delete project store fail: {}: {}", root, ex.toString());
            return false;
        }
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
        return Math.max(0, count);
    }

    private static int normalizeInt(Integer value) {
        if (value == null || value < 0) {
            return Integer.MAX_VALUE;
        }
        return value;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private record MethodIdentity(long mid, long graphNodeId) {
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
}
