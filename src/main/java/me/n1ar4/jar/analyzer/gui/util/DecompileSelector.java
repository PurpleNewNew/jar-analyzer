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
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
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
        if (shouldUseCfr()) {
            String code = CFRDecompileEngine.decompile(path.toAbsolutePath().toString());
            if (code != null) {
                return code;
            }
            logger.debug("cfr decompile empty, fallback to fernflower: {}", path.toAbsolutePath());
        }
        return DecompileEngine.decompile(path);
    }

    public static boolean shouldUseCfr() {
        MainForm form = MainForm.getInstance();
        if (form == null || form.getCfrRadio() == null) {
            return false;
        }
        if (!form.getCfrRadio().isSelected()) {
            return false;
        }
        return CFRDecompileEngine.isAvailable();
    }
}
