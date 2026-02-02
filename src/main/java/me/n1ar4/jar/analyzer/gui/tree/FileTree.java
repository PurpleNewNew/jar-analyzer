/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.tree;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FileTree extends JTree {
    private static ImageIcon classIcon;
    private static final String PLACEHOLDER_NODE = "fake";
    private static final Set<String> PACKAGE_ROOTS = new HashSet<>(Arrays.asList(
            "com", "org", "net", "io", "me", "cn", "edu", "gov",
            "java", "javax", "jakarta", "sun", "jdk",
            "android", "androidx", "kotlin", "scala"
    ));
    static {
        try {
            classIcon = new ImageIcon(ImageIO.read(Objects.requireNonNull(
                    FileTree.class.getClassLoader().getResourceAsStream("img/class.png"))));
        } catch (Exception ignored) {
        }
    }

    private final DefaultTreeModel savedModel;
    private final AtomicInteger refreshSeq = new AtomicInteger(0);
    private volatile boolean listenersInitialized = false;
    private volatile Map<String, String> jarRootDisplay = Collections.emptyMap();
    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel fileTreeModel;

    public FileTree() {
        savedModel = (DefaultTreeModel) this.getModel();
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object user = node.getUserObject();
                    String fileName = user == null ? "" : user.toString();
                    String display = null;
                    if (user instanceof FileTreeNode) {
                        FileTreeNode fileNode = (FileTreeNode) user;
                        fileName = fileNode.file.getName();
                        display = fileNode.getDisplayName();
                    }
                    String label = (display == null || display.isEmpty()) ? fileName : display;
                    if (leaf) {
                        String fileExtension = getFileExtension(fileName);
                        if (fileExtension != null && fileExtension.equalsIgnoreCase("class")) {
                            label = stripClassSuffix(fileName);
                            setIcon(classIcon);
                        }
                    }
                    setText(label);
                }
                return this;
            }

            private String getFileExtension(String fileName) {
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    return fileName.substring(dotIndex + 1);
                }
                return null;
            }

            private String stripClassSuffix(String fileName) {
                if (fileName == null) {
                    return "";
                }
                String suffix = ".class";
                if (fileName.toLowerCase().endsWith(suffix)) {
                    return fileName.substring(0, fileName.length() - suffix.length());
                }
                return fileName;
            }
        };
        this.setCellRenderer(renderer);

        setModel(null);
    }

    public void refresh() {
        refreshInternal(null);
    }

    private void refreshInternal(String targetClass) {
        int seq = refreshSeq.incrementAndGet();
        UiExecutor.runAsync(() -> {
            refreshJarRootDisplay();
            SearchBuild build = targetClass == null
                    ? buildRootOnly()
                    : buildRootWithSelection(targetClass);
            UiExecutor.runOnEdt(() -> {
                if (seq != refreshSeq.get()) {
                    return;
                }
                applyRoot(build.root);
                if (build.selectionPath != null) {
                    setSelectionPath(build.selectionPath);
                    scrollPathToVisible(build.selectionPath);
                    FileTree.setFound(true);
                } else {
                    FileTree.setFound(false);
                }
            });
        });
    }

    private SearchBuild buildRootOnly() {
        DefaultMutableTreeNode root = buildRootNode();
        return new SearchBuild(root, null);
    }

    private SearchBuild buildRootWithSelection(String targetClass) {
        DefaultMutableTreeNode root = buildRootNode();
        TreePath selectionPath = resolveSelectionPath(root, targetClass);
        return new SearchBuild(root, selectionPath);
    }

    private void applyRoot(DefaultMutableTreeNode root) {
        setModel(savedModel);
        DefaultTreeModel model = new DefaultTreeModel(root);
        setModel(model);
        fileTreeModel = model;
        rootNode = root;
        setEditable(false);
        initListenersOnce();
        repaint();
    }

    private void initListenersOnce() {
        if (listenersInitialized) {
            return;
        }
        listenersInitialized = true;
        // 2024-07-31 删除 addTreeSelectionListener
        // 不需要提供自动的滚动功能 影响正常使用
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
            }

            public void treeExpanded(TreeExpansionEvent event) {
                clearSelection();
                TreePath path = event.getPath();
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                populateSubTreeAsync(treeNode);
            }
        });
    }

    private DefaultMutableTreeNode buildRootNode() {
        File root = new File(Const.tempDir);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileTreeNode(root));
        List<DefaultMutableTreeNode> children = groupByJar() ? buildChildren(root) : buildFlatRootChildren(root);
        for (DefaultMutableTreeNode child : children) {
            rootNode.add(child);
        }
        return rootNode;
    }

    private boolean groupByJar() {
        return MenuUtil.isGroupTreeByJarEnabled();
    }

    private boolean mergePackageRoot() {
        return MenuUtil.isMergePackageRootEnabled();
    }

    private void populateSubTreeAsync(DefaultMutableTreeNode node) {
        if (node == null || !needsLoad(node)) {
            return;
        }
        UiExecutor.runAsync(() -> {
            File file = getFile(node);
            if (file == null) {
                return;
            }
            List<DefaultMutableTreeNode> children = buildChildren(file);
            UiExecutor.runOnEdt(() -> applyChildren(node, children));
        });
    }

    private void applyChildren(DefaultMutableTreeNode node, List<DefaultMutableTreeNode> children) {
        if (node == null) {
            return;
        }
        node.removeAllChildren();
        if (children != null) {
            for (DefaultMutableTreeNode child : children) {
                node.add(child);
            }
        }
        if (fileTreeModel != null) {
            fileTreeModel.nodeStructureChanged(node);
        }
    }

    private boolean needsLoad(DefaultMutableTreeNode node) {
        if (node == null || node.getChildCount() != 1) {
            return false;
        }
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(0);
        Object user = child.getUserObject();
        return PLACEHOLDER_NODE.equals(user == null ? null : user.toString());
    }

    private List<DefaultMutableTreeNode> buildChildren(File file) {
        if (file == null || !file.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = file.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<File> directories = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();

        for (File child : files) {
            TreeFileFilter filter = TreeFileFilter.defaults(child);
            if (filter.shouldFilter()) {
                continue;
            }
            if (child.isDirectory()) {
                directories.add(child);
            } else {
                regularFiles.add(child);
            }
        }

        regularFiles.sort((o1, o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();
            boolean isClassFile1 = name1.endsWith(".class");
            boolean isClassFile2 = name2.endsWith(".class");
            if (isClassFile1 && !isClassFile2) {
                return 1;
            }
            if (!isClassFile1 && isClassFile2) {
                return -1;
            }
            return name1.compareToIgnoreCase(name2);
        });

        List<DirEntry> dirEntries = buildDirEntries(file, directories);
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (DirEntry entry : dirEntries) {
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(new FileTreeNode(entry.file, entry.displayName));
            subNode.add(new DefaultMutableTreeNode(PLACEHOLDER_NODE));
            nodes.add(subNode);
        }
        for (File child : regularFiles) {
            nodes.add(new DefaultMutableTreeNode(new FileTreeNode(child)));
        }
        return nodes;
    }

    private List<DefaultMutableTreeNode> buildFlatRootChildren(File root) {
        if (root == null || !root.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = root.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<DirEntry> dirEntries = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();
        for (File child : files) {
            TreeFileFilter filter = TreeFileFilter.defaults(child);
            if (filter.shouldFilter()) {
                continue;
            }
            if (child.isDirectory() && isJarRootDir(child)) {
                File[] nested = child.listFiles();
                if (nested == null) {
                    continue;
                }
                for (File inner : nested) {
                    TreeFileFilter innerFilter = TreeFileFilter.defaults(inner);
                    if (innerFilter.shouldFilter()) {
                        continue;
                    }
                    if (inner.isDirectory()) {
                        addDirEntriesForContainer(child, inner, dirEntries);
                    } else {
                        regularFiles.add(inner);
                    }
                }
                continue;
            }
            if (child.isDirectory()) {
                addDirEntriesForContainer(root, child, dirEntries);
            } else {
                regularFiles.add(child);
            }
        }

        dirEntries.sort((o1, o2) -> o1.sortKey.compareToIgnoreCase(o2.sortKey));
        regularFiles.sort((o1, o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();
            boolean isClassFile1 = name1.endsWith(".class");
            boolean isClassFile2 = name2.endsWith(".class");
            if (isClassFile1 && !isClassFile2) {
                return 1;
            }
            if (!isClassFile1 && isClassFile2) {
                return -1;
            }
            return name1.compareToIgnoreCase(name2);
        });

        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (DirEntry entry : dirEntries) {
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(new FileTreeNode(entry.file, entry.displayName));
            subNode.add(new DefaultMutableTreeNode(PLACEHOLDER_NODE));
            nodes.add(subNode);
        }
        for (File child : regularFiles) {
            nodes.add(new DefaultMutableTreeNode(new FileTreeNode(child)));
        }
        return nodes;
    }

    private List<DirEntry> buildDirEntries(File container, List<File> directories) {
        List<DirEntry> entries = new ArrayList<>();
        boolean merge = mergePackageRoot() && isPackageRootContainer(container);
        for (File dir : directories) {
            if (merge && shouldMergePackageRoot(dir)) {
                appendMergedPackageRoot(dir, entries);
                continue;
            }
            entries.add(new DirEntry(dir, resolveDisplayName(dir)));
        }
        entries.sort((o1, o2) -> o1.sortKey.compareToIgnoreCase(o2.sortKey));
        return entries;
    }

    private void addDirEntriesForContainer(File container, File dir, List<DirEntry> entries) {
        if (dir == null) {
            return;
        }
        boolean merge = mergePackageRoot() && isPackageRootContainer(container);
        if (merge && shouldMergePackageRoot(dir)) {
            appendMergedPackageRoot(dir, entries);
            return;
        }
        entries.add(new DirEntry(dir, resolveDisplayName(dir)));
    }

    private void appendMergedPackageRoot(File rootDir, List<DirEntry> entries) {
        if (rootDir == null) {
            return;
        }
        List<File> subDirs = listVisibleDirs(rootDir);
        subDirs.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        String prefix = rootDir.getName();
        for (File subDir : subDirs) {
            entries.add(new DirEntry(subDir, prefix + "." + subDir.getName()));
        }
    }

    private boolean shouldMergePackageRoot(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        if (!isPackageRootName(dir.getName())) {
            return false;
        }
        if (hasVisibleFiles(dir)) {
            return false;
        }
        return !listVisibleDirs(dir).isEmpty();
    }

    private boolean isPackageRootContainer(File dir) {
        if (dir == null) {
            return false;
        }
        Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        Path current = dir.toPath().toAbsolutePath().normalize();
        if (current.equals(tempRoot)) {
            return true;
        }
        return isJarRootDir(dir);
    }

    private boolean isPackageRootName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return PACKAGE_ROOTS.contains(name.toLowerCase());
    }

    private List<File> listVisibleDirs(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> dirs = new ArrayList<>();
        for (File file : files) {
            TreeFileFilter filter = TreeFileFilter.defaults(file);
            if (filter.shouldFilter()) {
                continue;
            }
            if (file.isDirectory()) {
                dirs.add(file);
            }
        }
        return dirs;
    }

    private boolean hasVisibleFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            TreeFileFilter filter = TreeFileFilter.defaults(file);
            if (filter.shouldFilter()) {
                continue;
            }
            if (file.isFile()) {
                return true;
            }
        }
        return false;
    }

    private boolean isJarRootDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        return parseJarDirId(dir.getName()) != null;
    }

    private void refreshJarRootDisplay() {
        if (!groupByJar()) {
            jarRootDisplay = Collections.emptyMap();
            return;
        }
        Map<String, String> display = new HashMap<>();
        CoreEngine engine = MainForm.getEngine();
        try {
            if (engine != null) {
                List<JarEntity> jars = engine.getJarsMeta();
                for (JarEntity jar : jars) {
                    if (jar == null) {
                        continue;
                    }
                    int id = jar.getJid();
                    String name = jar.getJarName();
                    String absPath = jar.getJarAbsPath();
                    if (name == null || name.trim().isEmpty() || absPath == null || absPath.trim().isEmpty()) {
                        continue;
                    }
                    String rootName = buildJarRootName(id, absPath);
                    display.putIfAbsent(rootName, name);
                }
            }
            addNestedJarDisplay(display);
            jarRootDisplay = display;
        } catch (Throwable t) {
            jarRootDisplay = Collections.emptyMap();
        }
    }

    private String resolveDisplayName(File file) {
        if (file == null || !file.isDirectory()) {
            return null;
        }
        Path parent = file.toPath().getParent();
        if (parent == null) {
            return null;
        }
        Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        if (!parent.toAbsolutePath().normalize().equals(tempRoot)) {
            return null;
        }
        return jarRootDisplay.get(file.getName());
    }

    private Integer parseJarDirId(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.startsWith("jar-")) {
            int lastDash = name.lastIndexOf('-');
            if (lastDash <= 3) {
                return null;
            }
            String idStr = name.substring(4, lastDash);
            return parseIntSafe(idStr);
        }
        if (isNumericId(name)) {
            return parseIntSafe(name);
        }
        return null;
    }

    private void addNestedJarDisplay(Map<String, String> display) {
        if (display == null) {
            return;
        }
        Path tempRoot = Paths.get(Const.tempDir);
        if (!Files.isDirectory(tempRoot)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(tempRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> isArchiveName(p.getFileName().toString()))
                    .forEach(p -> {
                        Path jarRoot = findAncestorJarRoot(p, tempRoot);
                        if (jarRoot == null) {
                            return;
                        }
                        Integer jarId = parseJarDirId(jarRoot.getFileName().toString());
                        if (jarId == null) {
                            return;
                        }
                        String rootName = buildJarRootName(jarId, p.toString());
                        String jarName = p.getFileName().toString();
                        display.putIfAbsent(rootName, jarName);
                    });
        } catch (Exception ignored) {
        }
    }

    private boolean isArchiveName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".jar") || lower.endsWith(".war");
    }

    private Path findAncestorJarRoot(Path file, Path tempRoot) {
        if (file == null || tempRoot == null) {
            return null;
        }
        Path current = file.getParent();
        Path rootAbs = tempRoot.toAbsolutePath().normalize();
        while (current != null) {
            Path currentAbs = current.toAbsolutePath().normalize();
            if (currentAbs.equals(rootAbs)) {
                return null;
            }
            String name = current.getFileName() == null ? null : current.getFileName().toString();
            if (parseJarDirId(name) != null) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private String buildJarRootName(int jarId, String jarPath) {
        int safeId = jarId;
        String hash = jarPath == null ? "0" : Integer.toHexString(jarPath.hashCode());
        return "jar-" + safeId + "-" + hash;
    }

    private Integer parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isNumericId(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int i = 0;
        if (value.charAt(0) == '-') {
            if (value.length() == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static volatile boolean found = false;

    public static void setFound(boolean found) {
        FileTree.found = found;
    }

    public static boolean isFound() {
        return found;
    }

    public void searchPathTarget(String classname) {
        if (classname == null || classname.trim().isEmpty()) {
            refresh();
            return;
        }
        refreshInternal(classname);
    }

    private TreePath resolveSelectionPath(DefaultMutableTreeNode root, String classname) {
        if (root == null || classname == null || classname.trim().isEmpty()) {
            return null;
        }
        Path targetPath = resolveTargetFile(classname);
        if (targetPath == null) {
            LogUtil.warn("class not found");
            return null;
        }
        List<Path> pathParts = buildPathParts(targetPath);
        if (pathParts.isEmpty()) {
            return new TreePath(root);
        }
        if (!groupByJar() && isJarRootPath(pathParts.get(0))) {
            pathParts.remove(0);
        }
        applyMergedPackageRootPath(pathParts);
        if (pathParts.isEmpty()) {
            return new TreePath(root);
        }
        DefaultMutableTreeNode current = root;
        List<Object> pathNodes = new ArrayList<>();
        pathNodes.add(current);
        for (Path part : pathParts) {
            ensureChildrenLoaded(current);
            DefaultMutableTreeNode child = findChildByFile(current, part.toFile());
            if (child == null) {
                return null;
            }
            pathNodes.add(child);
            current = child;
        }
        return new TreePath(pathNodes.toArray());
    }

    private void applyMergedPackageRootPath(List<Path> pathParts) {
        if (!mergePackageRoot() || pathParts == null || pathParts.size() < 2) {
            return;
        }
        int index = 0;
        if (groupByJar() && isJarRootPath(pathParts.get(0))) {
            if (pathParts.size() < 3) {
                return;
            }
            index = 1;
        }
        if (pathParts.size() <= index + 1) {
            return;
        }
        Path rootPath = pathParts.get(index);
        if (shouldMergePackageRoot(rootPath.toFile())) {
            pathParts.remove(index);
        }
    }

    private List<Path> buildPathParts(Path targetPath) {
        if (targetPath == null) {
            return Collections.emptyList();
        }
        Path base = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        Path current = targetPath.toAbsolutePath().normalize();
        List<Path> parts = new ArrayList<>();
        while (current != null && !current.equals(base)) {
            parts.add(0, current);
            current = current.getParent();
        }
        return parts;
    }

    private boolean isJarRootPath(Path path) {
        if (path == null) {
            return false;
        }
        Path name = path.getFileName();
        if (name == null) {
            return false;
        }
        return parseJarDirId(name.toString()) != null;
    }

    private void ensureChildrenLoaded(DefaultMutableTreeNode node) {
        if (node == null || !needsLoad(node)) {
            return;
        }
        File file = getFile(node);
        if (file == null) {
            return;
        }
        List<DefaultMutableTreeNode> children = buildChildren(file);
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
        }
    }

    private DefaultMutableTreeNode findChildByFile(DefaultMutableTreeNode parent, File target) {
        if (parent == null || target == null) {
            return null;
        }
        String targetPath = normalizePath(target.toPath());
        Enumeration<?> children = parent.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            Object obj = child.getUserObject();
            if (!(obj instanceof FileTreeNode)) {
                continue;
            }
            File file = ((FileTreeNode) obj).file;
            if (file == null) {
                continue;
            }
            if (pathsEqual(targetPath, normalizePath(file.toPath()))) {
                return child;
            }
        }
        return null;
    }

    private String normalizePath(Path path) {
        if (path == null) {
            return "";
        }
        return path.toAbsolutePath().normalize().toString();
    }

    private boolean pathsEqual(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        if (OSUtil.isWindows()) {
            return left.equalsIgnoreCase(right);
        }
        return left.equals(right);
    }

    private File getFile(DefaultMutableTreeNode node) {
        if (node == null) {
            return null;
        }
        Object obj = node.getUserObject();
        if (obj instanceof FileTreeNode) {
            return ((FileTreeNode) obj).file;
        }
        return null;
    }

    private Path resolveTargetFile(String classname) {
        String originClassName = classname.replace("\\", "/");
        Path dir = Paths.get(Const.tempDir);
        Path classPath = resolveClassPath(dir, originClassName);
        if (classPath != null && Files.exists(classPath)) {
            return classPath;
        }

        Path directPath = dir.resolve(originClassName);
        if (Files.exists(directPath)) {
            return directPath;
        }

        int innerIdx = originClassName.indexOf('$');
        if (innerIdx > 0) {
            String outerClass = originClassName.substring(0, innerIdx);
            Path outerPath = resolveClassPath(dir, outerClass);
            if (outerPath != null && Files.exists(outerPath)) {
                return outerPath;
            }
        }
        return null;
    }

    private Path resolveClassPath(Path baseDir, String className) {
        Path resolved = JarUtil.resolveClassFileInTemp(className);
        if (resolved != null && Files.exists(resolved)) {
            return resolved;
        }
        if (baseDir == null || className == null || className.trim().isEmpty()) {
            return null;
        }
        Path classPath = baseDir.resolve(className + ".class");
        if (Files.exists(classPath)) {
            return classPath;
        }
        Path boot = baseDir.resolve("BOOT-INF/classes/" + className + ".class");
        if (Files.exists(boot)) {
            return boot;
        }
        Path web = baseDir.resolve("WEB-INF/classes/" + className + ".class");
        if (Files.exists(web)) {
            return web;
        }
        return null;
    }

    private static final class DirEntry {
        private final File file;
        private final String displayName;
        private final String sortKey;

        private DirEntry(File file, String displayName) {
            this.file = file;
            this.displayName = displayName;
            String key = displayName;
            if (key == null || key.isEmpty()) {
                key = file == null ? "" : file.getName();
            }
            this.sortKey = key;
        }
    }

    private static final class SearchBuild {
        private final DefaultMutableTreeNode root;
        private final TreePath selectionPath;

        private SearchBuild(DefaultMutableTreeNode root, TreePath selectionPath) {
            this.root = root;
            this.selectionPath = selectionPath;
        }
    }

}
