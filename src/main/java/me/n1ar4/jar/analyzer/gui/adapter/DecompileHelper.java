/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.adapter;

import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.tree.FileTreeNode;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DecompileHelper {
    private static final int MAX_DETECT_BYTES = 8192;
    private static final int MAX_PREVIEW_BYTES = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    public static void decompile(TreePath selPath) {
        if (selPath == null) {
            return;
        }
        File targetFile = resolveTreeFile(selPath);
        String filePath;
        Path thePath;
        if (targetFile != null) {
            filePath = targetFile.getPath();
            thePath = targetFile.toPath();
        } else {
            String sel = selPath.toString();
            sel = sel.substring(1, sel.length() - 1);
            String[] selArray = sel.split(",");
            ArrayList<String> pathList = new ArrayList<>();
            for (String s : selArray) {
                s = s.trim();
                pathList.add(s);
            }
            String[] path = pathList.toArray(new String[0]);
            filePath = String.join(File.separator, path);
            thePath = Paths.get(filePath);
        }

        if (!filePath.endsWith(".class")) {
            previewResource(thePath, filePath);
            return;
        }

        String className = JarUtil.resolveClassNameFromPath(filePath);
        if (className == null || className.trim().isEmpty()) {
            return;
        }

        String finalClassName = className;
        UiExecutor.runAsync(() -> {
            if (!Files.exists(thePath)) {
                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                        MainForm.getInstance().getMasterPanel(),
                        "file not exist"));
                return;
            }
            if (LuceneSearchForm.getInstance() != null && LuceneSearchForm.usePaLucene()) {
                IndexPluginsSupport.addIndex(thePath.toFile());
            }
            String code = DecompileSelector.decompile(thePath);
            if (code == null || code.trim().isEmpty()) {
                String finalHint = "decompile failed";
                UiExecutor.runOnEdt(() -> {
                    applySyntaxStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                    MainForm.getCodeArea().setText(finalHint);
                    MainForm.getCodeArea().setCaretPosition(0);
                });
            } else {
                UiExecutor.runOnEdt(() -> {
                    applySyntaxStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(0);
                });
            }
            CoreHelper.refreshAllMethods(finalClassName);
            String jarName = MainForm.getEngine() == null ? null : MainForm.getEngine().getJarByClass(finalClassName);
            UiExecutor.runOnEdt(() -> {
                MainForm.setCurClass(finalClassName);
                MainForm.getInstance().getCurClassText().setText(finalClassName);
                MainForm.getInstance().getCurJarText().setText(jarName);
                MainForm.getInstance().getCurMethodText().setText(null);
                MainForm.setCurMethod(null);
            });
        });
    }

    private static void previewResource(Path thePath, String filePath) {
        UiExecutor.runAsync(() -> {
            try {
                if (!Files.exists(thePath)) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "file not exist"));
                    return;
                }
                if (Files.isDirectory(thePath)) {
                    return;
                }
                long size = Files.size(thePath);
                BufferedImage image = tryReadImage(thePath, size);
                if (image != null) {
                    showImage(image, filePath);
                    return;
                }
                byte[] data = readBytesLimited(thePath, size, MAX_PREVIEW_BYTES);
                if (data == null) {
                    return;
                }
                boolean truncated = size > MAX_PREVIEW_BYTES;
                String text = decodeText(data);
                if (text == null) {
                    long hintSize = size > 0 ? size : data.length;
                    String hint = "binary file (" + hintSize + " bytes)";
                    UiExecutor.runOnEdt(() -> {
                        MainForm.getCodeArea().setText(hint);
                        MainForm.getCodeArea().setCaretPosition(0);
                    });
                    return;
                }
                final String displayText = truncated
                        ? text + "\n\n... (truncated, showing first " + data.length + " bytes of " + size + ")"
                        : text;
                UiExecutor.runOnEdt(() -> {
                    MainForm.getCodeArea().setText(displayText);
                    MainForm.getCodeArea().setCaretPosition(0);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private static BufferedImage tryReadImage(Path path, long size) {
        if (path == null) {
            return null;
        }
        if (size > 0 && size > MAX_IMAGE_BYTES) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return ImageIO.read(in);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] readBytesLimited(Path path, long size, int maxBytes) {
        if (path == null || maxBytes <= 0) {
            return null;
        }
        int target = (int) Math.min(size, maxBytes);
        if (target <= 0) {
            return new byte[0];
        }
        try (InputStream in = Files.newInputStream(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int remaining = target;
            while (remaining > 0) {
                int read = in.read(buffer, 0, Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
            return out.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void showImage(BufferedImage image, String filePath) {
        UiExecutor.runOnEdt(() -> {
            JLabel label = new JLabel(new ImageIcon(image));
            JScrollPane scrollPane = new JScrollPane(label);
            scrollPane.setPreferredSize(new Dimension(800, 600));

            Window owner = null;
            if (MainForm.getInstance() != null) {
                owner = SwingUtilities.getWindowAncestor(MainForm.getInstance().getMasterPanel());
            }
            JDialog dialog = new JDialog(owner, resolveFileName(filePath));
            dialog.setModal(false);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setContentPane(scrollPane);
            dialog.pack();
            if (owner != null) {
                dialog.setLocationRelativeTo(owner);
            }
            dialog.setVisible(true);
        });
    }

    private static String decodeText(byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length == 0) {
            return "";
        }
        int offset = 0;
        Charset bomCharset = null;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            bomCharset = StandardCharsets.UTF_8;
            offset = 3;
        } else if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            bomCharset = StandardCharsets.UTF_16LE;
            offset = 2;
        } else if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            bomCharset = StandardCharsets.UTF_16BE;
            offset = 2;
        }
        if (bomCharset != null) {
            return new String(data, offset, data.length - offset, bomCharset);
        }
        Charset utf16 = detectUtf16NoBom(data);
        if (utf16 != null) {
            return new String(data, utf16);
        }
        if (isValidUtf8(data)) {
            return new String(data, StandardCharsets.UTF_8);
        }
        if (isProbablyBinary(data)) {
            return null;
        }
        String[] candidates = new String[]{
                "GB18030",
                "GBK",
                "Shift_JIS",
                "EUC-JP",
                "Big5",
                "EUC-KR"
        };
        for (String name : candidates) {
            String decoded = decodeWithCharset(data, Charset.forName(name));
            if (decoded != null) {
                return decoded;
            }
        }
        return new String(data, StandardCharsets.ISO_8859_1);
    }

    private static Charset detectUtf16NoBom(byte[] data) {
        int sample = Math.min(data.length, MAX_DETECT_BYTES);
        if (sample < 4) {
            return null;
        }
        int evenZero = 0;
        int oddZero = 0;
        for (int i = 0; i < sample; i++) {
            if (data[i] == 0) {
                if ((i & 1) == 0) {
                    evenZero++;
                } else {
                    oddZero++;
                }
            }
        }
        if (evenZero == 0 && oddZero == 0) {
            return null;
        }
        int threshold = sample / 4;
        if (evenZero > threshold && oddZero < threshold / 2) {
            return StandardCharsets.UTF_16BE;
        }
        if (oddZero > threshold && evenZero < threshold / 2) {
            return StandardCharsets.UTF_16LE;
        }
        return null;
    }

    private static String decodeWithCharset(byte[] data, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException ignored) {
            return null;
        }
    }

    private static boolean isValidUtf8(byte[] data) {
        int i = 0;
        while (i < data.length) {
            int b = data[i] & 0xFF;
            if ((b & 0x80) == 0) {
                i++;
                continue;
            }
            int remaining;
            if ((b & 0xE0) == 0xC0) {
                remaining = 1;
                if (b < 0xC2) {
                    return false;
                }
            } else if ((b & 0xF0) == 0xE0) {
                remaining = 2;
            } else if ((b & 0xF8) == 0xF0) {
                remaining = 3;
                if (b > 0xF4) {
                    return false;
                }
            } else {
                return false;
            }
            if (i + remaining >= data.length) {
                return false;
            }
            for (int j = 1; j <= remaining; j++) {
                int bb = data[i + j] & 0xFF;
                if ((bb & 0xC0) != 0x80) {
                    return false;
                }
            }
            i += remaining + 1;
        }
        return true;
    }

    private static boolean isProbablyBinary(byte[] data) {
        int sample = Math.min(data.length, MAX_DETECT_BYTES);
        int suspicious = 0;
        for (int i = 0; i < sample; i++) {
            int b = data[i] & 0xFF;
            if (b == 0) {
                return true;
            }
            if (b < 0x09) {
                suspicious++;
                continue;
            }
            if (b > 0x0D && b < 0x20) {
                suspicious++;
            }
        }
        if (sample == 0) {
            return false;
        }
        return (suspicious * 100 / sample) > 30;
    }

    private static void applySyntaxStyle(String style) {
        if (style == null) {
            return;
        }
        JTextArea area = MainForm.getCodeArea();
        if (area instanceof RSyntaxTextArea) {
            ((RSyntaxTextArea) area).setSyntaxEditingStyle(style);
        }
    }

    private static String resolveFileName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "resource";
        }
        try {
            Path path = Paths.get(filePath);
            Path name = path.getFileName();
            if (name != null) {
                return name.toString();
            }
        } catch (Exception ignored) {
        }
        int idx = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (idx >= 0 && idx + 1 < filePath.length()) {
            return filePath.substring(idx + 1);
        }
        return filePath;
    }

    private static File resolveTreeFile(TreePath selPath) {
        Object last = selPath.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode)) {
            return null;
        }
        Object user = ((DefaultMutableTreeNode) last).getUserObject();
        if (user instanceof FileTreeNode) {
            return ((FileTreeNode) user).file;
        }
        return null;
    }
}
