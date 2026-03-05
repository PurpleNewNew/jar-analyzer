/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class SwingSplash {
    private static final Logger logger = LogManager.getLogger();
    private static final String SPLASH_RESOURCE = "/img/splash-waifu.jpg";
    private static final int DEFAULT_DURATION_MS = 1400;

    private SwingSplash() {
    }

    public static void show() {
        show(DEFAULT_DURATION_MS);
    }

    public static void show(int durationMs) {
        if (durationMs <= 0) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> show(durationMs));
            } catch (Exception ex) {
                logger.debug("show splash invoke in edt fail: {}", ex.toString());
            }
            return;
        }
        BufferedImage source = loadSplashImage();
        if (source == null) {
            return;
        }

        int targetWidth = Math.min(980, Math.max(680, source.getWidth()));
        int targetHeight = Math.max(300, Math.min(560, targetWidth * source.getHeight() / Math.max(1, source.getWidth())));
        BufferedImage scaled = scaleImage(source, targetWidth, targetHeight);
        if (scaled == null) {
            return;
        }

        JDialog dialog = new JDialog((Frame) null, true);
        dialog.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(0x202020), 1));
        root.add(new JLabel(new ImageIcon(scaled)), BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(640, 320));
        dialog.setLocationRelativeTo(null);

        Timer timer = new Timer(durationMs, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private static BufferedImage loadSplashImage() {
        try {
            URL url = SwingSplash.class.getResource(SPLASH_RESOURCE);
            if (url == null) {
                logger.debug("splash resource not found: {}", SPLASH_RESOURCE);
                return null;
            }
            return ImageIO.read(url);
        } catch (Exception ex) {
            logger.debug("load splash image fail: {}", ex.toString());
            return null;
        }
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        if (source == null || width <= 0 || height <= 0) {
            return null;
        }
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(scaled, 0, 0, null);
            return out;
        } finally {
            g.dispose();
        }
    }
}
