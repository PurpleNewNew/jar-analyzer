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

import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FileTree extends JTree {
    private static ImageIcon classIcon;
    private static final String PLACEHOLDER_NODE = "fake";

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
                if (leaf && value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    String nodeText = node.getUserObject().toString();
                    String fileExtension = getFileExtension(nodeText);
                    if (fileExtension != null && fileExtension.equalsIgnoreCase("class")) {
                        setText(nodeText.split("\\.")[0]);
                        setIcon(classIcon);
                    }
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
        List<DefaultMutableTreeNode> children = buildChildren(root);
        for (DefaultMutableTreeNode child : children) {
            rootNode.add(child);
        }
        return rootNode;
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

        directories.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
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
        for (File dir : directories) {
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(new FileTreeNode(dir));
            subNode.add(new DefaultMutableTreeNode(PLACEHOLDER_NODE));
            nodes.add(subNode);
        }
        for (File child : regularFiles) {
            nodes.add(new DefaultMutableTreeNode(new FileTreeNode(child)));
        }
        return nodes;
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
        Path base = Paths.get(Const.tempDir);
        Path relative;
        try {
            relative = base.relativize(targetPath);
        } catch (Exception ex) {
            return null;
        }
        String relPath = relative.toString().replace("\\", "/");
        if (relPath.trim().isEmpty()) {
            return new TreePath(root);
        }
        String[] split = relPath.split("/");
        DefaultMutableTreeNode current = root;
        List<Object> pathNodes = new ArrayList<>();
        pathNodes.add(current);
        for (String part : split) {
            ensureChildrenLoaded(current);
            DefaultMutableTreeNode child = findChild(current, part);
            if (child == null) {
                return null;
            }
            pathNodes.add(child);
            current = child;
        }
        return new TreePath(pathNodes.toArray());
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

    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String name) {
        if (parent == null || name == null) {
            return null;
        }
        Enumeration<?> children = parent.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (name.equals(child.toString())) {
                return child;
            }
        }
        return null;
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

    private static final class SearchBuild {
        private final DefaultMutableTreeNode root;
        private final TreePath selectionPath;

        private SearchBuild(DefaultMutableTreeNode root, TreePath selectionPath) {
            this.root = root;
            this.selectionPath = selectionPath;
        }
    }
}
