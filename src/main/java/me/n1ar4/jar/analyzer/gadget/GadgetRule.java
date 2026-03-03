/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gadget;

import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.IOUtils;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GadgetRule {
    private static final Logger logger = LogManager.getLogger();
    private static final String EMBED_DAT_FILE = "gadget.dat";
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized;
    private static volatile List<GadgetInfo> rules = List.of();

    private GadgetRule() {
    }

    public static void build() {
        if (initialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            try {
                rules = List.copyOf(loadRules());
                initialized = true;
            } catch (Exception ex) {
                InterruptUtil.restoreInterruptIfNeeded(ex);
                logger.debug("load gadget rules failed: {}", ex.toString());
            }
        }
    }

    public static List<GadgetInfo> getRules() {
        build();
        return rules;
    }

    private static List<GadgetInfo> loadRules() throws Exception {
        List<GadgetInfo> loaded = new ArrayList<>();
        InputStream stream = GadgetRule.class.getClassLoader().getResourceAsStream(EMBED_DAT_FILE);
        if (stream == null) {
            throw new IllegalStateException("gadget.dat not found in classpath");
        }
        byte[] gadgetBytes;
        try (InputStream is = stream) {
            gadgetBytes = IOUtils.readAllBytes(is);
        }
        String content = new String(gadgetBytes, StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        int id = 1;
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            if (parts.length != 3) {
                continue;
            }
            GadgetInfo info = new GadgetInfo();
            info.setID(id);
            id++;
            String[] jars = parts[0].trim().split(",");
            List<String> jarsList = new ArrayList<>();
            for (String jar : jars) {
                if (!jar.trim().isEmpty()) {
                    jarsList.add(jar.trim());
                }
            }
            info.setJarsName(jarsList);
            info.setType(parts[1].trim());
            info.setResult(parts[2].trim());
            loaded.add(info);
        }
        return loaded;
    }
}
