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
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.starter.Const;
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
                if (buildCmd.delCache()) {
                    logger.info("delete cache files");
                    try {
                        DirUtil.removeDir(new File(Const.tempDir));
                    } catch (Exception ex) {
                        logger.warn("delete cache files fail: {}", ex.toString());
                    }
                }
                if (buildCmd.delExist()) {
                    logger.info("delete old project store");
                    try {
                        String projectKey = ActiveProjectContext.getActiveProjectKey();
                        Path projectHome = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
                        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
                        logger.info("deleted project store: {}", projectHome);
                    } catch (Exception ex) {
                        logger.warn("delete old project store fail: {}", ex.toString());
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

                // CLI build always clears existing project graph data before rebuild.
                CoreRunner.run(jarPathPath, null, false, false, null, true, buildCmd.enableInnerJars());
                Path projectHome = Neo4jProjectStore.getInstance()
                        .resolveProjectHome(ActiveProjectContext.getActiveProjectKey());
                logger.info("write project store to: {}", projectHome);
                System.exit(0);
            }
            case StartCmd.CMD -> logger.info("run jar-analyzer gui");
            default -> throw new IllegalArgumentException("invalid params: " + cmd);
        }
    }
}
