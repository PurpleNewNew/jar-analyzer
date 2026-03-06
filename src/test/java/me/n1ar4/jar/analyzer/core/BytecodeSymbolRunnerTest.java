/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytecodeSymbolRunnerTest {
    @Test
    public void invalidClassBytesShouldFailSymbolBuild() {
        String previous = System.getProperty("jar.analyzer.symbol.threads");
        try {
            System.setProperty("jar.analyzer.symbol.threads", "2");
            Set<ClassFileEntity> rows = new LinkedHashSet<>();
            rows.add(invalidClass("bad/A", 1));
            rows.add(invalidClass("bad/B", 2));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> BytecodeSymbolRunner.start(rows));
            assertTrue(ex.getMessage().contains("symbol index"));
        } finally {
            restoreProp("jar.analyzer.symbol.threads", previous);
        }
    }

    private static ClassFileEntity invalidClass(String className, int jarId) {
        ClassFileEntity row = new ClassFileEntity();
        row.setClassName(className);
        row.setJarId(jarId);
        row.setPath(Paths.get(className.replace('/', '_') + ".class"));
        row.setCachedBytes(new byte[]{1, 2, 3, 4});
        return row;
    }

    private static void restoreProp(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
