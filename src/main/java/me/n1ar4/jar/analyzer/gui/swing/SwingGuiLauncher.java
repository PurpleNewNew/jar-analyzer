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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.runtime.GuiLauncher;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.SwingUtilities;

public final class SwingGuiLauncher implements GuiLauncher {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void launch(StartCmd startCmd) {
        installLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            SwingMainFrame frame = new SwingMainFrame(startCmd);
            MainForm.setFrame(frame);
            MainForm.setMasterPanel(frame.getContentPane());
            frame.setVisible(true);
        });
        logger.info("launch gui with swing shell");
    }

    private static void installLookAndFeel() {
        try {
            String theme = RuntimeFacades.tooling().configSnapshot().theme();
            if ("dark".equalsIgnoreCase(theme)) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Throwable ex) {
            logger.warn("init look and feel failed: {}", ex.toString());
            try {
                FlatLightLaf.setup();
            } catch (Throwable ignored) {
            }
        }
    }
}
