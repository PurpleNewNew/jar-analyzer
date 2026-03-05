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
import me.n1ar4.jar.analyzer.gui.runtime.GuiLauncher;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SwingGuiLauncher implements GuiLauncher {
    private static final Logger logger = LogManager.getLogger();
    private static final AtomicBoolean TEMP_CLEANUP_HOOK_INSTALLED = new AtomicBoolean(false);

    @Override
    public void launch() {
        installLookAndFeel();
        installTemporaryCleanupHook();
        SwingUtilities.invokeLater(() -> {
            SwingSplash.show();
            WelcomeFrame welcome = new WelcomeFrame();
            welcome.setVisible(true);
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
                logger.debug("fallback look and feel setup failed: {}", ignored.toString());
            }
        }
    }

    private static void installTemporaryCleanupHook() {
        if (!TEMP_CLEANUP_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ProjectRegistryService.getInstance().cleanupTemporaryProject();
            } catch (Throwable ex) {
                logger.debug("shutdown cleanup temporary project fail: {}", ex.toString());
            }
        }, "jar-analyzer-temp-project-cleanup"));
    }
}
