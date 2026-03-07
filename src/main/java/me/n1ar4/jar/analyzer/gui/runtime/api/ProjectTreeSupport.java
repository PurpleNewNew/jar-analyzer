package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ProjectTreeSupport {
    private static final Logger logger = LogManager.getLogger();
    private static final int RESOURCE_TREE_FETCH_LIMIT = 200_000;
    private static final String CATEGORY_ORIGIN_APP = "origin:app";
    private static final String CATEGORY_ORIGIN_LIBRARY = "origin:library";
    private static final String CATEGORY_ORIGIN_SDK = "origin:sdk";
    private static final String CATEGORY_ORIGIN_GENERATED = "origin:generated";
    private static final String CATEGORY_ORIGIN_EXCLUDED = "origin:excluded";
    private static final List<ProjectOrigin> ORIGIN_ORDER = List.of(
            ProjectOrigin.APP,
            ProjectOrigin.LIBRARY,
            ProjectOrigin.SDK,
            ProjectOrigin.GENERATED,
            ProjectOrigin.EXCLUDED
    );

    private final UiActions ui;

    ProjectTreeSupport(UiActions ui) {
        this.ui = ui == null ? UiActions.noop() : ui;
    }

    List<TreeNodeDto> buildTree(String filterKeywordLower, TreeSettings settings) {
        List<ClassFileEntity> classRows = loadClassFiles();
        List<ResourceEntity> resourceRows = loadResources();
        List<JarEntity> jarRows = loadJarMeta();
        ProjectModelSnapshot snapshot = loadProjectModelSnapshot();
        if (!snapshot.available()) {
            return projectModelMissingTree();
        }
        return buildSemanticTree(snapshot, classRows, resourceRows, jarRows, filterKeywordLower, settings);
    }

    void openNode(String value) {
        String raw = safe(value).trim();
        if (raw.isEmpty()) {
            return;
        }
        if (raw.startsWith("res:")) {
            openResourceNode(raw);
            return;
        }
        if (raw.startsWith("jarpath:")) {
            openJarPathNode(raw);
            return;
        }
        if (raw.startsWith("path:")) {
            openPathNode(raw);
            return;
        }
        if (raw.startsWith("error:")) {
            ui.showText("Project Tree", raw.substring("error:".length()));
            return;
        }
        if (raw.startsWith("cls:")) {
            raw = raw.substring(4);
        } else {
            return;
        }
        int split = raw.lastIndexOf('|');
        Integer jarId = null;
        String className = raw;
        if (split > 0 && split < raw.length() - 1) {
            className = raw.substring(0, split);
            try {
                jarId = Integer.parseInt(raw.substring(split + 1));
            } catch (NumberFormatException ignored) {
                jarId = null;
            }
        }
        className = normalizeClass(className);
        if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
        }
        if (className.isEmpty()) {
            return;
        }
        ui.openClass(className, jarId);
    }

    private List<TreeNodeDto> projectModelMissingTree() {
        TreeNodeDto reason = new TreeNodeDto(
                "project_model_missing_rebuild",
                "error:project_model_missing_rebuild",
                false,
                List.of()
        );
        return List.of(new TreeNodeDto("Project Model", "cat:project-model", true, List.of(reason)));
    }

    private List<TreeNodeDto> buildSemanticTree(ProjectModelSnapshot snapshot,
                                                List<ClassFileEntity> classRows,
                                                List<ResourceEntity> resourceRows,
                                                List<JarEntity> jarRows,
                                                String filterKeywordLower,
                                                TreeSettings settings) {
        TreeSettings safeSettings = settings == null ? TreeSettings.defaults() : settings;
        Map<Integer, String> jarNameById = new HashMap<>();
        Map<Integer, Path> jarPathById = new HashMap<>();
        for (JarEntity row : jarRows) {
            if (row == null) {
                continue;
            }
            int jarId = row.getJid();
            String jarName = safe(row.getJarName()).trim();
            if (!jarName.isBlank()) {
                jarNameById.put(jarId, jarName);
            }
            Path jarPath = normalizeFsPath(row.getJarAbsPath());
            if (jarPath != null) {
                jarPathById.put(jarId, jarPath);
            }
        }
        SemanticOriginResolver resolver = new SemanticOriginResolver(snapshot, jarPathById);
        Map<ProjectOrigin, MutableTreeNode> categories = new EnumMap<>(ProjectOrigin.class);
        categories.put(ProjectOrigin.APP, new MutableTreeNode("App", CATEGORY_ORIGIN_APP, true));
        categories.put(ProjectOrigin.LIBRARY, new MutableTreeNode("Libraries", CATEGORY_ORIGIN_LIBRARY, true));
        categories.put(ProjectOrigin.SDK, new MutableTreeNode("SDK", CATEGORY_ORIGIN_SDK, true));
        categories.put(ProjectOrigin.GENERATED, new MutableTreeNode("Generated", CATEGORY_ORIGIN_GENERATED, true));
        categories.put(ProjectOrigin.EXCLUDED, new MutableTreeNode("Excluded", CATEGORY_ORIGIN_EXCLUDED, true));

        addSemanticRootNodes(snapshot, categories, filterKeywordLower);
        addSemanticArchiveNodes(snapshot, jarRows, resolver, categories, filterKeywordLower);
        addSemanticClassNodes(classRows, jarNameById, resolver, categories, filterKeywordLower, safeSettings.groupTreeByJar(), safeSettings.showInnerClass());
        addSemanticResourceNodes(resourceRows, jarNameById, resolver, categories, filterKeywordLower, safeSettings.groupTreeByJar());

        List<TreeNodeDto> out = new ArrayList<>();
        boolean hasFilter = filterKeywordLower != null && !filterKeywordLower.isBlank();
        for (ProjectOrigin origin : ORIGIN_ORDER) {
            MutableTreeNode category = categories.get(origin);
            if (category == null) {
                continue;
            }
            TreeNodeDto node = category.freeze();
            if (safeSettings.mergePackageRoot()) {
                node = mergeSemanticSections(node);
            }
            if (hasFilter) {
                if (node.children().isEmpty() && !matchesFilter(filterKeywordLower, node.label())) {
                    continue;
                }
            } else if (node.children().isEmpty()) {
                continue;
            }
            out.add(node);
        }
        return out;
    }

    private List<ClassFileEntity> loadClassFiles() {
        List<ClassFileEntity> rows = DatabaseManager.getClassFiles();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows;
    }

    private List<ResourceEntity> loadResources() {
        List<ResourceEntity> rows = DatabaseManager.getResources();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int total = rows.size();
        int limit = Math.min(total, RESOURCE_TREE_FETCH_LIMIT);
        if (total > limit) {
            logger.warn("resource tree truncated: {} > {}", total, limit);
            return new ArrayList<>(rows.subList(0, limit));
        }
        return rows;
    }

    private List<JarEntity> loadJarMeta() {
        List<JarEntity> rows = DatabaseManager.getJarsMeta();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows;
    }

    private ProjectModelSnapshot loadProjectModelSnapshot() {
        ProjectModel model = DatabaseManager.getProjectModel();
        if (model == null) {
            return ProjectModelSnapshot.empty();
        }
        long buildSeq = DatabaseManager.getProjectBuildSeq();
        List<ProjectRootRecord> roots = loadProjectRoots(model);
        List<ProjectEntryRecord> entries = loadProjectEntries(model, roots);
        return ProjectModelSnapshot.of(buildSeq, roots, entries);
    }

    private List<ProjectRootRecord> loadProjectRoots(ProjectModel model) {
        List<ProjectRootRecord> out = new ArrayList<>();
        if (model == null || model.roots() == null || model.roots().isEmpty()) {
            return out;
        }
        int rootId = 1;
        for (ProjectRoot root : model.roots()) {
            if (root == null || root.path() == null) {
                continue;
            }
            ProjectRootKind kind = root.kind() == null ? ProjectRootKind.CONTENT_ROOT : root.kind();
            ProjectOrigin origin = root.origin();
            if (origin == null || origin == ProjectOrigin.UNKNOWN) {
                origin = kind.defaultOrigin();
            }
            out.add(new ProjectRootRecord(
                    rootId++,
                    kind,
                    origin,
                    root.path(),
                    safe(root.presentableName()),
                    root.archive(),
                    root.test(),
                    root.priority()
            ));
        }
        out.sort(Comparator
                .comparingInt(ProjectRootRecord::priority)
                .thenComparing(item -> item.kind().value())
                .thenComparing(item -> pathToString(item.path())));
        return out;
    }

    private List<ProjectEntryRecord> loadProjectEntries(ProjectModel model, List<ProjectRootRecord> roots) {
        List<ProjectEntryRecord> out = new ArrayList<>();
        if (model == null || model.analyzedArchives() == null || model.analyzedArchives().isEmpty()) {
            return out;
        }
        for (Path entryPath : model.analyzedArchives()) {
            if (entryPath == null) {
                continue;
            }
            int rootId = resolveEntryRootId(entryPath, roots);
            ProjectOrigin origin = resolveEntryOrigin(entryPath, model, roots);
            out.add(new ProjectEntryRecord(rootId, "archive", origin, entryPath));
        }
        out.sort(Comparator.comparing(item -> pathToString(item.path())));
        return out;
    }

    private int resolveEntryRootId(Path entryPath, List<ProjectRootRecord> roots) {
        if (entryPath == null || roots == null || roots.isEmpty()) {
            return 0;
        }
        int bestRootId = 0;
        int bestDepth = -1;
        for (ProjectRootRecord root : roots) {
            if (root == null || root.path() == null) {
                continue;
            }
            try {
                if (entryPath.startsWith(root.path())) {
                    int depth = root.path().getNameCount();
                    if (depth > bestDepth) {
                        bestDepth = depth;
                        bestRootId = root.rootId();
                    }
                }
            } catch (Exception ignored) {
                logger.debug("resolve entry root id failed: {}", ignored.toString());
            }
        }
        return bestRootId;
    }

    private ProjectOrigin resolveEntryOrigin(Path entryPath,
                                             ProjectModel model,
                                             List<ProjectRootRecord> roots) {
        if (entryPath == null) {
            return ProjectOrigin.APP;
        }
        ProjectOrigin best = null;
        int bestDepth = -1;
        if (roots != null) {
            for (ProjectRootRecord root : roots) {
                if (root == null || root.path() == null) {
                    continue;
                }
                try {
                    if (!entryPath.startsWith(root.path())) {
                        continue;
                    }
                } catch (Exception ignored) {
                    continue;
                }
                int depth = root.path().getNameCount();
                if (depth > bestDepth) {
                    bestDepth = depth;
                    best = root.origin();
                }
            }
        }
        if (best != null && best != ProjectOrigin.UNKNOWN) {
            return best;
        }
        try {
            Path runtime = model == null ? null : model.runtimePath();
            if (runtime != null && entryPath.startsWith(runtime)) {
                return ProjectOrigin.SDK;
            }
        } catch (Exception ignored) {
            logger.debug("resolve entry origin from runtime failed: {}", ignored.toString());
        }
        try {
            Path primary = model == null ? null : model.primaryInputPath();
            if (primary != null && entryPath.startsWith(primary)) {
                return ProjectOrigin.APP;
            }
        } catch (Exception ignored) {
            logger.debug("resolve entry origin from primary input failed: {}", ignored.toString());
        }
        return ProjectOrigin.LIBRARY;
    }

    private void addSemanticRootNodes(ProjectModelSnapshot snapshot,
                                      Map<ProjectOrigin, MutableTreeNode> categories,
                                      String filterKeywordLower) {
        for (ProjectRootRecord root : snapshot.roots()) {
            if (root == null) {
                continue;
            }
            ProjectOrigin origin = root.origin() == null ? ProjectOrigin.APP : root.origin();
            MutableTreeNode category = resolveCategory(categories, origin);
            if (category == null) {
                continue;
            }
            String rootPath = pathToString(root.path());
            String presentable = safe(root.presentableName()).trim();
            if (presentable.isBlank()) {
                Path fileName = root.path() == null ? null : root.path().getFileName();
                presentable = fileName == null ? rootPath : fileName.toString();
            }
            if (presentable.isBlank()) {
                presentable = root.kind().value();
            }
            String rootKind = rootKindLabel(root.kind());
            String label = presentable + " [" + rootKind + "]";
            if (root.test()) {
                label = label + " (test)";
            }
            if (root.archive()) {
                label = label + " (archive)";
            }
            if (!matchesFilter(filterKeywordLower, label, rootPath, rootKind, origin.value())) {
                continue;
            }
            MutableTreeNode section = ensureOriginSection(category, origin, "roots", "Roots");
            String nodeKey = "origin-root:" + origin.value() + ":" + root.rootId();
            String nodeValue = "origin-root:" + origin.value() + ":" + root.kind().value() + ":" + root.rootId();
            String finalLabel = label;
            MutableTreeNode rootNode = section.children.computeIfAbsent(
                    nodeKey,
                    ignored -> new MutableTreeNode(finalLabel, nodeValue, true)
            );
            if (!rootPath.isBlank()) {
                rootNode.children.computeIfAbsent(
                        "origin-root-path:" + origin.value() + ":" + root.rootId(),
                        ignored -> new MutableTreeNode(rootPath, "path:" + rootPath, false)
                );
            }
        }
    }

    private void addSemanticArchiveNodes(ProjectModelSnapshot snapshot,
                                         List<JarEntity> jarRows,
                                         SemanticOriginResolver resolver,
                                         Map<ProjectOrigin, MutableTreeNode> categories,
                                         String filterKeywordLower) {
        Set<String> seenArchivePaths = new HashSet<>();
        for (JarEntity row : jarRows) {
            if (row == null) {
                continue;
            }
            int jarId = row.getJid();
            String jarName = resolveJarName(jarId, row.getJarName(), null);
            String absPath = safe(row.getJarAbsPath()).trim();
            ProjectOrigin origin = resolver.resolve(jarId, absPath);
            if (!matchesFilter(filterKeywordLower, jarName, absPath, origin.value(), String.valueOf(jarId))) {
                continue;
            }
            MutableTreeNode category = resolveCategory(categories, origin);
            if (category == null) {
                continue;
            }
            MutableTreeNode section = ensureOriginSection(category, origin, "archives", "Archives");
            String nodeKey = "origin-archive:" + origin.value() + ":" + jarId;
            String nodeValue = "origin-archive:" + origin.value() + ":" + jarId;
            String finalJarName = jarName;
            MutableTreeNode archiveNode = section.children.computeIfAbsent(
                    nodeKey,
                    ignored -> new MutableTreeNode(finalJarName, nodeValue, true)
            );
            if (!absPath.isBlank()) {
                String absPathKey = pathKey(normalizeFsPath(absPath));
                if (!absPathKey.isBlank()) {
                    seenArchivePaths.add(absPathKey);
                }
                archiveNode.children.computeIfAbsent(
                        "origin-archive-path:" + origin.value() + ":" + jarId,
                        ignored -> new MutableTreeNode(absPath, "jarpath:" + jarId, false)
                );
            }
        }
        for (ProjectEntryRecord entry : snapshot.entries()) {
            if (entry == null || !"archive".equals(entry.entryKind()) || entry.path() == null) {
                continue;
            }
            String entryPath = pathToString(entry.path());
            if (entryPath.isBlank()) {
                continue;
            }
            String entryPathKey = pathKey(entry.path());
            if (!entryPathKey.isBlank() && seenArchivePaths.contains(entryPathKey)) {
                continue;
            }
            String name = entry.path().getFileName() == null ? entryPath : entry.path().getFileName().toString();
            if (!matchesFilter(filterKeywordLower, name, entryPath, entry.origin().value())) {
                continue;
            }
            ProjectOrigin origin = entry.origin();
            MutableTreeNode category = resolveCategory(categories, origin);
            if (category == null) {
                continue;
            }
            MutableTreeNode section = ensureOriginSection(category, origin, "archives", "Archives");
            String nodeKey = "origin-archive-entry:" + origin.value() + ":" + entryPath;
            String nodeValue = "origin-archive-entry:" + origin.value() + ":" + entryPath;
            String finalName = name;
            MutableTreeNode archiveNode = section.children.computeIfAbsent(
                    nodeKey,
                    ignored -> new MutableTreeNode(finalName, nodeValue, true)
            );
            archiveNode.children.computeIfAbsent(
                    "origin-archive-entry-path:" + origin.value() + ":" + entryPath,
                    ignored -> new MutableTreeNode(entryPath, "path:" + entryPath, false)
            );
        }
    }

    private void addSemanticClassNodes(List<ClassFileEntity> rows,
                                       Map<Integer, String> jarNameById,
                                       SemanticOriginResolver resolver,
                                       Map<ProjectOrigin, MutableTreeNode> categories,
                                       String filterKeywordLower,
                                       boolean groupByJar,
                                       boolean showInnerClass) {
        for (ClassFileEntity row : rows) {
            if (row == null) {
                continue;
            }
            String normalized = normalizeClassName(row.getClassName(), showInnerClass);
            if (normalized == null) {
                continue;
            }
            int jarId = row.getJarId() == null ? 0 : row.getJarId();
            String jarName = resolveJarName(jarId, row.getJarName(), jarNameById);
            String classPath = safe(row.getPathStr()).trim();
            ProjectOrigin origin = resolver.resolve(jarId, classPath);
            if (!matchesFilter(filterKeywordLower, normalized, jarName, classPath, origin.value())) {
                continue;
            }
            MutableTreeNode category = resolveCategory(categories, origin);
            if (category == null) {
                continue;
            }
            MutableTreeNode section = ensureOriginSection(category, origin, "classes", "Classes");
            MutableTreeNode cursor = section;
            if (groupByJar) {
                String jarKey = "origin-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                String jarValue = "origin-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                String finalJarName = jarName;
                cursor = section.children.computeIfAbsent(
                        jarKey,
                        ignored -> new MutableTreeNode(finalJarName, jarValue, true)
                );
            }
            String[] parts = normalized.split("/");
            StringBuilder packagePath = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part == null || part.isBlank()) {
                    continue;
                }
                boolean leaf = i == parts.length - 1;
                if (leaf) {
                    String label = part + ".class";
                    if (!groupByJar) {
                        label = label + " [" + jarName + "]";
                    }
                    String value = "cls:" + normalized + "|" + jarId;
                    String leafKey = "origin-class-leaf:" + origin.value() + ":" + normalized + "|" + jarId;
                    String finalLabel = label;
                    String finalValue = value;
                    cursor.children.computeIfAbsent(
                            leafKey,
                            ignored -> new MutableTreeNode(finalLabel, finalValue, false)
                    );
                } else {
                    if (packagePath.length() > 0) {
                        packagePath.append('/');
                    }
                    packagePath.append(part);
                    String pkg = packagePath.toString().replace('/', '.');
                    String dirKey = "origin-pkg:" + origin.value() + ":" + (groupByJar ? jarId + ":" : "") + pkg;
                    String dirValue = "origin-pkg:" + origin.value() + ":" + pkg;
                    cursor = cursor.children.computeIfAbsent(
                            dirKey,
                            ignored -> new MutableTreeNode(part, dirValue, true)
                    );
                }
            }
        }
    }

    private void addSemanticResourceNodes(List<ResourceEntity> rows,
                                          Map<Integer, String> jarNameById,
                                          SemanticOriginResolver resolver,
                                          Map<ProjectOrigin, MutableTreeNode> categories,
                                          String filterKeywordLower,
                                          boolean groupByJar) {
        for (ResourceEntity row : rows) {
            if (row == null) {
                continue;
            }
            String normalized = normalizeResourcePath(row.getResourcePath());
            if (normalized == null) {
                continue;
            }
            int jarId = row.getJarId() == null ? 0 : row.getJarId();
            String jarName = resolveJarName(jarId, row.getJarName(), jarNameById);
            String resourcePath = safe(row.getPathStr()).trim();
            ProjectOrigin origin = resolver.resolve(jarId, resourcePath);
            if (!matchesFilter(filterKeywordLower, normalized, jarName, resourcePath, origin.value())) {
                continue;
            }
            MutableTreeNode category = resolveCategory(categories, origin);
            if (category == null) {
                continue;
            }
            MutableTreeNode section = ensureOriginSection(category, origin, "resources", "Resources");
            MutableTreeNode cursor = section;
            if (groupByJar) {
                String jarKey = "origin-res-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                String jarValue = "origin-res-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                String finalJarName = jarName;
                cursor = section.children.computeIfAbsent(
                        jarKey,
                        ignored -> new MutableTreeNode(finalJarName, jarValue, true)
                );
            }
            String[] parts = normalized.split("/");
            StringBuilder path = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part == null || part.isBlank()) {
                    continue;
                }
                boolean leaf = i == parts.length - 1;
                if (leaf) {
                    String label = part;
                    if (!groupByJar) {
                        label = label + " [" + jarName + "]";
                    }
                    String leafKey = "origin-res-leaf:" + origin.value() + ":" + row.getRid();
                    String leafValue = "res:" + row.getRid();
                    String finalLabel = label;
                    cursor.children.computeIfAbsent(
                            leafKey,
                            ignored -> new MutableTreeNode(finalLabel, leafValue, false)
                    );
                } else {
                    if (path.length() > 0) {
                        path.append('/');
                    }
                    path.append(part);
                    String p = path.toString().replace('/', '.');
                    String dirKey = "origin-resdir:" + origin.value() + ":" + (groupByJar ? jarId + ":" : "") + p;
                    String dirValue = "origin-resdir:" + origin.value() + ":" + p;
                    cursor = cursor.children.computeIfAbsent(
                            dirKey,
                            ignored -> new MutableTreeNode(part, dirValue, true)
                    );
                }
            }
        }
    }

    private MutableTreeNode ensureOriginSection(MutableTreeNode category,
                                                ProjectOrigin origin,
                                                String suffix,
                                                String label) {
        String value = "origin-sec:" + origin.value() + ":" + suffix;
        return category.children.computeIfAbsent(value, ignored -> new MutableTreeNode(label, value, true));
    }

    private MutableTreeNode resolveCategory(Map<ProjectOrigin, MutableTreeNode> categories,
                                            ProjectOrigin origin) {
        ProjectOrigin safeOrigin = origin == null ? ProjectOrigin.APP : origin;
        MutableTreeNode result = categories.get(safeOrigin);
        if (result != null) {
            return result;
        }
        return categories.get(ProjectOrigin.APP);
    }

    private TreeNodeDto mergeSemanticSections(TreeNodeDto category) {
        if (category == null || !category.directory()) {
            return category;
        }
        List<TreeNodeDto> children = category.children();
        if (children == null || children.isEmpty()) {
            return category;
        }
        List<TreeNodeDto> out = new ArrayList<>();
        for (TreeNodeDto child : children) {
            if (child == null) {
                continue;
            }
            if (child.directory() && shouldMergeSection(child.value())) {
                out.add(new TreeNodeDto(
                        child.label(),
                        child.value(),
                        true,
                        mergeNodeList(child.children())
                ));
            } else {
                out.add(child);
            }
        }
        sortNodes(out);
        return new TreeNodeDto(category.label(), category.value(), true, out);
    }

    private boolean shouldMergeSection(String value) {
        String normalized = safe(value);
        if (!normalized.startsWith("origin-sec:")) {
            return false;
        }
        return normalized.endsWith(":classes") || normalized.endsWith(":resources");
    }

    private void sortNodes(List<TreeNodeDto> nodes) {
        nodes.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                .thenComparing(TreeNodeDto::label));
    }

    private boolean matchesFilter(String filterKeywordLower, String... values) {
        if (filterKeywordLower == null || filterKeywordLower.isBlank()) {
            return true;
        }
        for (String value : values) {
            if (safe(value).toLowerCase(Locale.ROOT).contains(filterKeywordLower)) {
                return true;
            }
        }
        return false;
    }

    private String resolveJarName(int jarId, String jarName, Map<Integer, String> jarNameById) {
        String direct = safe(jarName).trim();
        if (!direct.isEmpty()) {
            return direct;
        }
        if (jarNameById != null) {
            String mapped = safe(jarNameById.get(jarId)).trim();
            if (!mapped.isEmpty()) {
                return mapped;
            }
        }
        return jarId == 0 ? "jar-unknown" : "jar-" + jarId;
    }

    private List<TreeNodeDto> mergeNodeList(List<TreeNodeDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<TreeNodeDto> out = new ArrayList<>();
        for (TreeNodeDto node : nodes) {
            if (node == null) {
                continue;
            }
            out.add(mergeNode(node));
        }
        out.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                .thenComparing(TreeNodeDto::label));
        return out;
    }

    private TreeNodeDto mergeNode(TreeNodeDto node) {
        if (node == null || !node.directory()) {
            return node;
        }
        List<TreeNodeDto> mergedChildren = mergeNodeList(node.children());
        TreeNodeDto merged = new TreeNodeDto(node.label(), node.value(), true, mergedChildren);
        while (merged.children() != null
                && merged.children().size() == 1
                && merged.children().get(0).directory()) {
            TreeNodeDto only = merged.children().get(0);
            merged = new TreeNodeDto(
                    merged.label() + "/" + only.label(),
                    only.value(),
                    true,
                    only.children()
            );
        }
        return merged;
    }

    private String normalizeClassName(String raw, boolean showInnerClass) {
        String value = safe(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        value = value.replace('\\', '/');
        if (value.endsWith(".class")) {
            value = value.substring(0, value.length() - 6);
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            return null;
        }
        if (!showInnerClass && value.contains("$")) {
            return null;
        }
        return value;
    }

    private String rootKindLabel(ProjectRootKind kind) {
        if (kind == null) {
            return "content-root";
        }
        return switch (kind) {
            case CONTENT_ROOT -> "content-root";
            case SOURCE_ROOT -> "source-root";
            case RESOURCE_ROOT -> "resource-root";
            case LIBRARY -> "library";
            case SDK -> "sdk";
            case GENERATED -> "generated";
            case EXCLUDED -> "excluded";
        };
    }

    private String normalizeResourcePath(String raw) {
        String value = safe(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        value = value.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    private void openResourceNode(String rawValue) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return;
        }
        int rid;
        try {
            rid = Integer.parseInt(rawValue.substring(4).trim());
        } catch (Throwable ignored) {
            return;
        }
        ResourceEntity resource = engine.getResourceById(rid);
        if (resource == null) {
            ui.showText("Resource", "resource not found: " + rid);
            return;
        }
        Path filePath;
        try {
            filePath = Paths.get(safe(resource.getPathStr()));
        } catch (Throwable ex) {
            ui.showText("Resource", "invalid resource path: " + ex.getMessage());
            return;
        }
        if (Files.notExists(filePath)) {
            ui.showText("Resource", "resource file not found: " + filePath);
            return;
        }
        String title = "Resource: " + safe(resource.getResourcePath());
        if (resource.getIsText() == 1) {
            ui.showTooling(new ToolingWindowRequest(
                    ToolingWindowAction.TEXT_VIEWER,
                    new ToolingWindowPayload.TextPayload(title, renderTextResource(resource, filePath))
            ));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("resource: ").append(safe(resource.getResourcePath())).append('\n');
            sb.append("jar: ").append(safe(resource.getJarName())).append('\n');
            sb.append("jar id: ").append(resource.getJarId()).append('\n');
            sb.append("size: ").append(resource.getFileSize()).append('\n');
            sb.append("text: ").append(resource.getIsText() == 1).append('\n');
            sb.append("file: ").append(filePath.toAbsolutePath()).append('\n');
            ui.showTooling(new ToolingWindowRequest(
                    ToolingWindowAction.TEXT_VIEWER,
                    new ToolingWindowPayload.TextPayload(title, sb.toString())
            ));
        }
    }

    private String renderTextResource(ResourceEntity resource, Path path) {
        final int maxBytes = 512 * 1024;
        try {
            long size = Files.size(path);
            byte[] bytes;
            try (InputStream input = Files.newInputStream(path)) {
                bytes = input.readNBytes(maxBytes + 1);
            }
            boolean truncated = bytes.length > maxBytes;
            int len = truncated ? maxBytes : bytes.length;
            String body = new String(bytes, 0, len, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            sb.append("// resource: ").append(safe(resource.getResourcePath())).append('\n');
            sb.append("// jar: ").append(safe(resource.getJarName())).append('\n');
            sb.append("// size: ").append(size).append('\n');
            if (truncated) {
                sb.append("// preview truncated to ").append(maxBytes).append(" bytes").append('\n');
            }
            sb.append('\n').append(body);
            return sb.toString();
        } catch (Throwable ex) {
            return "read resource failed: " + ex.getMessage();
        }
    }

    private void openJarPathNode(String rawValue) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return;
        }
        int jarId;
        try {
            jarId = Integer.parseInt(rawValue.substring("jarpath:".length()).trim());
        } catch (Throwable ignored) {
            return;
        }
        JarEntity matched = null;
        List<JarEntity> jars = engine.getJarsMeta();
        if (jars != null) {
            for (JarEntity jar : jars) {
                if (jar != null && jar.getJid() == jarId) {
                    matched = jar;
                    break;
                }
            }
        }
        if (matched == null) {
            ui.showText("Dependency", "jar not found: " + jarId);
            return;
        }
        String absPath = safe(matched.getJarAbsPath());
        Path path = absPath.isBlank() ? null : Paths.get(absPath);
        StringBuilder sb = new StringBuilder();
        sb.append("jar: ").append(safe(matched.getJarName())).append('\n');
        sb.append("jar id: ").append(matched.getJid()).append('\n');
        sb.append("path: ").append(absPath).append('\n');
        if (path != null) {
            sb.append("exists: ").append(Files.exists(path)).append('\n');
        }
        ui.showTooling(new ToolingWindowRequest(
                ToolingWindowAction.TEXT_VIEWER,
                new ToolingWindowPayload.TextPayload("Dependency Path", sb.toString())
        ));
    }

    private void openPathNode(String rawValue) {
        String text = safe(rawValue.substring("path:".length())).trim();
        if (text.isBlank()) {
            return;
        }
        Path path = normalizeFsPath(text);
        if (path == null) {
            ui.showText("Path", "invalid path: " + text);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("path: ").append(path).append('\n');
        sb.append("exists: ").append(Files.exists(path)).append('\n');
        sb.append("directory: ").append(Files.isDirectory(path)).append('\n');
        if (Files.exists(path) && !Files.isDirectory(path)) {
            try {
                sb.append("size: ").append(Files.size(path)).append('\n');
            } catch (Exception ex) {
                sb.append("size: unknown (").append(ex.getMessage()).append(")").append('\n');
            }
        }
        ui.showTooling(new ToolingWindowRequest(
                ToolingWindowAction.TEXT_VIEWER,
                new ToolingWindowPayload.TextPayload("Path", sb.toString())
        ));
    }

    private static Path normalizeFsPath(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Paths.get(value).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            try {
                return Paths.get(value).normalize();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private static String pathToString(Path path) {
        return path == null ? "" : path.toString();
    }

    private static String pathKey(Path path) {
        if (path == null) {
            return "";
        }
        try {
            return path.toAbsolutePath().normalize().toString();
        } catch (Exception ex) {
            return path.normalize().toString();
        }
    }

    private static String normalizeClass(String className) {
        String value = safe(className).trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record TreeSettings(boolean showInnerClass,
                        boolean groupTreeByJar,
                        boolean mergePackageRoot) {
        static TreeSettings defaults() {
            return new TreeSettings(false, false, false);
        }
    }

    interface UiActions {
        void openClass(String className, Integer jarId);

        void showText(String title, String text);

        void showTooling(ToolingWindowRequest request);

        static UiActions noop() {
            return new UiActions() {
                @Override
                public void openClass(String className, Integer jarId) {
                }

                @Override
                public void showText(String title, String text) {
                }

                @Override
                public void showTooling(ToolingWindowRequest request) {
                }
            };
        }
    }

    private record ProjectModelSnapshot(long buildSeq,
                                        List<ProjectRootRecord> roots,
                                        List<ProjectEntryRecord> entries) {
        private static ProjectModelSnapshot empty() {
            return new ProjectModelSnapshot(-1L, List.of(), List.of());
        }

        private static ProjectModelSnapshot of(long buildSeq,
                                               List<ProjectRootRecord> roots,
                                               List<ProjectEntryRecord> entries) {
            return new ProjectModelSnapshot(
                    buildSeq,
                    roots == null ? List.of() : List.copyOf(roots),
                    entries == null ? List.of() : List.copyOf(entries)
            );
        }

        private boolean available() {
            return buildSeq > 0
                    && ((roots != null && !roots.isEmpty()) || (entries != null && !entries.isEmpty()));
        }
    }

    private record ProjectRootRecord(int rootId,
                                     ProjectRootKind kind,
                                     ProjectOrigin origin,
                                     Path path,
                                     String presentableName,
                                     boolean archive,
                                     boolean test,
                                     int priority) {
    }

    private record ProjectEntryRecord(int rootId,
                                      String entryKind,
                                      ProjectOrigin origin,
                                      Path path) {
    }

    private record OriginPathRule(Path path, ProjectOrigin origin, int depth) {
    }

    private static final class SemanticOriginResolver {
        private final Map<Integer, Path> jarPathById;
        private final Map<String, ProjectOrigin> exactPathOrigins = new HashMap<>();
        private final List<OriginPathRule> pathRules = new ArrayList<>();

        private SemanticOriginResolver(ProjectModelSnapshot snapshot, Map<Integer, Path> jarPathById) {
            this.jarPathById = jarPathById == null ? Map.of() : jarPathById;
            if (snapshot == null) {
                return;
            }
            if (snapshot.roots() != null) {
                for (ProjectRootRecord root : snapshot.roots()) {
                    if (root == null || root.path() == null) {
                        continue;
                    }
                    registerPath(root.path(), root.origin());
                }
            }
            if (snapshot.entries() != null) {
                for (ProjectEntryRecord entry : snapshot.entries()) {
                    if (entry == null || entry.path() == null) {
                        continue;
                    }
                    registerPath(entry.path(), entry.origin());
                }
            }
            pathRules.sort(Comparator.comparingInt(OriginPathRule::depth).reversed());
        }

        private void registerPath(Path path, ProjectOrigin origin) {
            if (path == null) {
                return;
            }
            ProjectOrigin safeOrigin = origin == null ? ProjectOrigin.UNKNOWN : origin;
            String key = pathKey(path);
            if (!key.isBlank()) {
                exactPathOrigins.putIfAbsent(key, safeOrigin);
            }
            int depth = Math.max(0, path.getNameCount());
            pathRules.add(new OriginPathRule(path, safeOrigin, depth));
        }

        private ProjectOrigin resolve(Integer jarId, String pathText) {
            if (jarId != null && jarId > 0) {
                Path jarPath = jarPathById.get(jarId);
                ProjectOrigin byJarName = resolveByJarName(jarPath);
                if (byJarName != ProjectOrigin.UNKNOWN) {
                    return byJarName;
                }
                ProjectOrigin byJarPath = resolveByPath(jarPath);
                if (byJarPath != ProjectOrigin.UNKNOWN) {
                    return byJarPath;
                }
            }
            Path resolvedPath = normalizeFsPath(pathText);
            ProjectOrigin byPathName = resolveByJarName(resolvedPath);
            if (byPathName != ProjectOrigin.UNKNOWN) {
                return byPathName;
            }
            ProjectOrigin byPath = resolveByPath(resolvedPath);
            if (byPath != ProjectOrigin.UNKNOWN) {
                return byPath;
            }
            return ProjectOrigin.APP;
        }

        private ProjectOrigin resolveByPath(Path candidate) {
            if (candidate == null) {
                return ProjectOrigin.UNKNOWN;
            }
            String key = pathKey(candidate);
            if (!key.isBlank()) {
                ProjectOrigin exact = exactPathOrigins.get(key);
                if (exact != null && exact != ProjectOrigin.UNKNOWN) {
                    return exact;
                }
            }
            for (OriginPathRule rule : pathRules) {
                if (rule == null || rule.path() == null) {
                    continue;
                }
                try {
                    if (candidate.startsWith(rule.path())) {
                        return rule.origin();
                    }
                } catch (Exception ignored) {
                    logger.debug("resolve origin by path failed: {}", ignored.toString());
                }
            }
            return ProjectOrigin.UNKNOWN;
        }

        private ProjectOrigin resolveByJarName(Path path) {
            if (path == null || path.getFileName() == null) {
                return ProjectOrigin.UNKNOWN;
            }
            String fileName = path.getFileName().toString();
            if (AnalysisScopeRules.isForceTargetJar(fileName)) {
                return ProjectOrigin.APP;
            }
            if (AnalysisScopeRules.isSdkJar(fileName)) {
                return ProjectOrigin.SDK;
            }
            if (AnalysisScopeRules.isCommonLibraryJar(fileName)) {
                return ProjectOrigin.LIBRARY;
            }
            return ProjectOrigin.UNKNOWN;
        }
    }

    private static final class MutableTreeNode {
        private final String label;
        private final String value;
        private final boolean directory;
        private final Map<String, MutableTreeNode> children = new HashMap<>();

        private MutableTreeNode(String label, String value, boolean directory) {
            this.label = label;
            this.value = value;
            this.directory = directory;
        }

        private TreeNodeDto freeze() {
            List<TreeNodeDto> nodes = new ArrayList<>();
            for (MutableTreeNode child : children.values()) {
                if (child == null) {
                    continue;
                }
                nodes.add(child.freeze());
            }
            nodes.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                    .thenComparing(TreeNodeDto::label));
            return new TreeNodeDto(label, value, directory, nodes);
        }
    }
}
