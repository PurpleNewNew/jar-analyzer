/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

public final class JaNativeRegistration {
    private static final Logger logger = LogManager.getLogger();

    private JaNativeRegistration() {
    }

    public static void register(GraphDatabaseService database) {
        if (!(database instanceof GraphDatabaseAPI api)) {
            logger.warn("skip native ja.* registration because database is not GraphDatabaseAPI");
            return;
        }
        GlobalProcedures procedures = api.getDependencyResolver().resolveDependency(GlobalProcedures.class);
        try {
            procedures.registerProcedure(JaGraphProcedures.class);
        } catch (Exception ex) {
            logger.warn("register native ja.* procedures fail: {}", ex.toString());
        }
        try {
            procedures.registerFunction(JaRuleFunctions.class);
        } catch (Exception ex) {
            logger.warn("register native ja.* functions fail: {}", ex.toString());
        }
        try {
            procedures.registerFunction(ApocReadOnlyFunctions.class);
        } catch (Exception ex) {
            logger.warn("register native read-only apoc.* functions fail: {}", ex.toString());
        }
        try {
            logger.info("registered native ja.* procedures/functions and read-only apoc.* functions");
        } catch (Exception ex) {
            logger.warn("log native registration fail: {}", ex.toString());
        }
    }
}
