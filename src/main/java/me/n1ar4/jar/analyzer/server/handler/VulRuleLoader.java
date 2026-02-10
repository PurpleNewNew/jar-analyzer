/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.rules.vul.Rule;
import me.n1ar4.jar.analyzer.utils.IOUtils;
import me.n1ar4.jar.analyzer.utils.YamlUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class VulRuleLoader {
    private static final Logger logger = LogManager.getLogger();

    static class Result {
        private Rule rule;
        private String source;
        private String error;

        Rule getRule() {
            return rule;
        }

        String getSource() {
            return source;
        }

        String getError() {
            return error;
        }
    }

    private VulRuleLoader() {
    }

    static Result load() {
        Result result = new Result();
        Path vPath = Paths.get("rules", "vulnerability.yaml");
        if (!Files.exists(vPath)) {
            result.error = "rules/vulnerability.yaml not found";
            return result;
        }
        try (InputStream is = Files.newInputStream(vPath)) {
            byte[] yamlData = IOUtils.readAllBytes(is);
            result.rule = YamlUtil.loadAs(yamlData);
            result.source = "file";
            return result;
        } catch (Exception ex) {
            logger.warn("load rules/vulnerability.yaml failed: {}", ex.toString());
            result.error = ex.toString();
            return result;
        }
    }
}
