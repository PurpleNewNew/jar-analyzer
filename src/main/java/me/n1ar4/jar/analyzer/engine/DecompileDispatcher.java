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

import java.nio.file.Path;
import java.util.List;

public final class DecompileDispatcher {
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
            return CFRDecompileEngine.decompile(path.toAbsolutePath().toString());
        }
        return DecompileEngine.decompile(path);
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir, DecompileType type) {
        return decompileJars(jarsPath, outputDir, type, false);
    }

    public static boolean decompileJars(List<String> jarsPath,
                                        String outputDir,
                                        DecompileType type,
                                        boolean decompileNested) {
        if (jarsPath == null || jarsPath.isEmpty()) {
            return false;
        }
        DecompileType resolved = type == null ? resolvePreferred() : type;
        if (resolved == DecompileType.CFR) {
            return CFRDecompileEngine.decompileJars(jarsPath, outputDir, decompileNested);
        }
        return DecompileEngine.decompileJars(jarsPath, outputDir, decompileNested);
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
