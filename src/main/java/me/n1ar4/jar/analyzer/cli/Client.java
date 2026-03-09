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

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
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

    public static void runBuild(BuildCmd buildCmd) {
        logger.info("use build command");
        if (buildCmd == null || buildCmd.getJar() == null || buildCmd.getJar().isBlank()) {
            throw new IllegalArgumentException("need --jar file");
        }
        Path jarPathPath = Paths.get(buildCmd.getJar());
        if (!Files.exists(jarPathPath)) {
            throw new IllegalArgumentException("jar file not exist");
        }
        if (buildCmd.delCache()) {
            logger.info("delete cache files");
            try {
                DirUtil.removeDir(new File(Const.tempDir));
            } catch (Exception ex) {
                logger.warn("delete cache files fail: {}", ex.toString());
            }
        }

        try {
            ProjectRegistryService service = ProjectRegistryService.getInstance();
            service.upsertActiveProjectBuildSettings(
                    "",
                    jarPathPath.toAbsolutePath().normalize().toString(),
                    "",
                    buildCmd.enableInnerJars()
            );
            service.publishActiveProjectModel(ProjectModel.artifact(
                    jarPathPath,
                    null,
                    java.util.List.of(jarPathPath.toAbsolutePath().normalize()),
                    buildCmd.enableInnerJars()
            ));
        } catch (Throwable t) {
            me.n1ar4.jar.analyzer.utils.InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("set workspace context failed: {}", t.toString());
        }

        // CLI build always replaces the active project graph with a fresh build.
        CoreRunner.run(jarPathPath, null, false, false, null, buildCmd.enableInnerJars());
        logger.info("build finished; project store replaced atomically");
    }
}
