/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Path;
import java.util.List;

public final class DecompileDispatcher {
    private static final Logger logger = LogManager.getLogger();
    private static final String PREF_KEY = "jar-analyzer.decompiler";

    private DecompileDispatcher() {
    }

    public static DecompileType resolvePreferred() {
        DecompileType fromProperty = DecompileType.parse(System.getProperty(PREF_KEY), null);
        if (fromProperty != null) {
            return fromProperty;
        }
        try {
            MainForm form = MainForm.getInstance();
            if (form != null && form.getCfrRadio() != null && form.getCfrRadio().isSelected()) {
                return DecompileType.CFR;
            }
        } catch (Throwable ignored) {
        }
        return DecompileType.FERNFLOWER;
    }

    public static DecompileType parseType(String value, DecompileType def) {
        return DecompileType.parse(value, def);
    }

    public static String decompile(Path path, DecompileType type) {
        if (path == null) {
            return null;
        }
        DecompileType resolved = type == null ? resolvePreferred() : type;
        if (resolved == DecompileType.CFR) {
            if (!CFRDecompileEngine.isAvailable()) {
                logger.warn("cfr not available: {}", path.toAbsolutePath());
                return null;
            }
            return CFRDecompileEngine.decompile(path.toAbsolutePath().toString());
        }
        return DecompileEngine.decompile(path, true);
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir, DecompileType type) {
        if (jarsPath == null || jarsPath.isEmpty()) {
            return false;
        }
        DecompileType resolved = type == null ? resolvePreferred() : type;
        if (resolved == DecompileType.CFR) {
            if (!CFRDecompileEngine.isAvailable()) {
                logger.warn("cfr not available for jars");
                return false;
            }
            return CFRDecompileEngine.decompileJars(jarsPath, outputDir);
        }
        return DecompileEngine.decompileJars(jarsPath, outputDir);
    }

    public static String stripPrefix(String code, DecompileType type) {
        if (code == null) {
            return null;
        }
        DecompileType resolved = type == null ? resolvePreferred() : type;
        String prefix = resolved == DecompileType.CFR
                ? CFRDecompileEngine.getCFR_PREFIX()
                : DecompileEngine.getFERN_PREFIX();
        if (prefix != null && code.startsWith(prefix)) {
            return code.substring(prefix.length());
        }
        return code;
    }
}
