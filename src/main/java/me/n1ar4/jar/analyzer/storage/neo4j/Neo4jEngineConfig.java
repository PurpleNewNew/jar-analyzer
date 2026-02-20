/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.starter.Const;

import java.nio.file.Path;
import java.nio.file.Paths;

public record Neo4jEngineConfig(Path homeDir, String databaseName, boolean disableConnectors) {
    private static final String HOME_PROP = "jar.analyzer.neo4j.home";
    private static final String DATABASE_PROP = "jar.analyzer.neo4j.database";

    public static Neo4jEngineConfig defaults() {
        String rawHome = System.getProperty(HOME_PROP, Paths.get(Const.dbDir, "neo4j-home").toString());
        Path home = Paths.get(rawHome).toAbsolutePath().normalize();
        String database = normalizeDatabaseName(System.getProperty(DATABASE_PROP, "neo4j"));
        return new Neo4jEngineConfig(home, database, true);
    }

    private static String normalizeDatabaseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "neo4j";
        }
        return raw.trim();
    }
}
