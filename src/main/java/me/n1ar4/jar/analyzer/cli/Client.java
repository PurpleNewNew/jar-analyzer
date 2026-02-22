/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.cli;

import com.beust.jcommander.JCommander;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.DbFileUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {
    private static final Logger logger = LogManager.getLogger();

    public static void run(JCommander commander, BuildCmd buildCmd) {
        String cmd = commander.getParsedCommand();
        if (cmd == null || cmd.isBlank()) {
            commander.usage();
            System.exit(-1);
        }
        switch (cmd) {
            case BuildCmd.CMD -> {
                logger.info("use build command");
                if (buildCmd.getJar() == null || buildCmd.getJar().isBlank()) {
                    logger.error("need --jar file");
                    commander.usage();
                    System.exit(-1);
                }
                Path jarPathPath = Paths.get(buildCmd.getJar());
                if (!Files.exists(jarPathPath)) {
                    logger.error("jar file not exist");
                    commander.usage();
                    System.exit(-1);
                }
                String projectKey = buildCmd.getDatabase() == null ? "" : buildCmd.getDatabase().trim();
                if (!projectKey.isBlank()) {
                    System.setProperty("jar.analyzer.project", projectKey);
                    // keep compatibility with legacy property readers
                    System.setProperty("jar.analyzer.neo4j.database", projectKey);
                    logger.info("use project store: {}", projectKey);
                }
                if (buildCmd.delCache()) {
                    logger.info("delete cache files");
                    try {
                        DirUtil.removeDir(new File(Const.tempDir));
                    } catch (Exception ex) {
                        logger.warn("delete cache files fail: {}", ex.toString());
                    }
                }
                if (buildCmd.delExist()) {
                    logger.info("delete old db");
                    try {
                        if (!projectKey.isBlank()) {
                            DatabaseManager.selectDatabase(projectKey);
                            DatabaseManager.clearAllData();
                            logger.info("cleared project store: {}", projectKey);
                        } else {
                            int deleted = DbFileUtil.deleteDbFiles();
                            logger.info("deleted db files: {}", deleted);
                        }
                    } catch (Exception ex) {
                        logger.warn("delete old db fail: {}", ex.toString());
                    }
                }

                try {
                    WorkspaceContext.ensureArtifactProjectModel(
                            jarPathPath,
                            null,
                            buildCmd.enableInnerJars()
                    );
                } catch (Throwable t) {
                    me.n1ar4.jar.analyzer.utils.InterruptUtil.restoreInterruptIfNeeded(t);
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    logger.debug("set workspace context failed: {}", t.toString());
                }
                // CLI build always clears existing DB tables first (unless the DB file was deleted).
                CoreRunner.run(jarPathPath, null, false, false, true, null, true);
                Path activeHome = DatabaseManager.activeProjectHome();
                logger.info("active project store home: {}",
                        activeHome == null ? Const.neo4jHome : activeHome.toAbsolutePath().normalize());
                System.exit(0);
            }
            case StartCmd.CMD -> logger.info("run jar-analyzer gui");
            default -> throw new IllegalArgumentException("invalid params: " + cmd);
        }
    }
}
