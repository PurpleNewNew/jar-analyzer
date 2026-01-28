/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.gui.util;

import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileType;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Path;

public final class DecompileSelector {
    private static final Logger logger = LogManager.getLogger();

    private DecompileSelector() {
    }

    public static String decompile(Path path) {
        if (path == null) {
            return null;
        }
        DecompileType type = isCfrSelected() ? DecompileType.CFR : DecompileType.FERNFLOWER;
        String code = DecompileDispatcher.decompile(path, type);
        if (code == null && type == DecompileType.CFR) {
            logger.debug("cfr decompile empty, fallback to fernflower: {}", path.toAbsolutePath());
            return DecompileEngine.decompile(path);
        }
        return code;
    }

    public static boolean shouldUseCfr() {
        return isCfrSelected() && CFRDecompileEngine.isAvailable();
    }

    private static boolean isCfrSelected() {
        MainForm form = MainForm.getInstance();
        if (form == null || form.getCfrRadio() == null) {
            return false;
        }
        return form.getCfrRadio().isSelected();
    }
}
