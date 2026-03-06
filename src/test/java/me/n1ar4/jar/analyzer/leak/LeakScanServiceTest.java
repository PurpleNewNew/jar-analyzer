/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.leak;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakScanServiceTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void scanShouldShareRuleSelectionAndJarFiltering() {
        DatabaseManager.replaceJars(List.of("/tmp/app-one.jar", "/tmp/app-two.jar"));
        DatabaseManager.saveClassInfo(Set.of(
                classRef("demo/AppOne", 1, "app-one.jar", "jdbc:mysql://one"),
                classRef("demo/AppTwo", 2, "app-two.jar", "jdbc:mysql://two")
        ));

        LeakScanService service = new LeakScanService();
        List<me.n1ar4.jar.analyzer.entity.LeakResult> results = service.scan(
                newEngine(),
                new LeakScanService.Request(
                        Set.of("jdbc"),
                        false,
                        null,
                        List.of(),
                        List.of(),
                        Set.of(),
                        Set.of(2)
                )
        );

        assertEquals(1, results.size());
        assertEquals("JDBC", results.get(0).getTypeName());
        assertEquals("demo/AppTwo", results.get(0).getClassName());
        assertEquals(2, results.get(0).getJarId());
        assertTrue(results.get(0).getValue().contains("jdbc:mysql://two"));
    }

    private static ClassReference classRef(String className, int jarId, String jarName, String value) {
        ClassReference.Member member = new ClassReference.Member(
                "secret",
                0,
                value,
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                new ClassReference.Handle("java/lang/String")
        );
        return new ClassReference(
                61,
                1,
                className,
                "java/lang/Object",
                List.of(),
                false,
                List.of(member),
                Set.<AnnoReference>of(),
                jarName,
                jarId
        );
    }

    private static CoreEngine newEngine() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return new CoreEngine(config);
    }
}
