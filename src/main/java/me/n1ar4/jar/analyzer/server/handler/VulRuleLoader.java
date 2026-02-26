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

import com.alibaba.fastjson2.JSON;
import me.n1ar4.jar.analyzer.rules.vul.Rule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

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
        Path sinkPath = Paths.get("rules", "sink.json");
        if (!Files.exists(sinkPath)) {
            result.error = "rules/sink.json not found";
            return result;
        }
        try {
            byte[] jsonData = Files.readAllBytes(sinkPath);
            result.rule = JSON.parseObject(jsonData, Rule.class);
            if (result.rule == null) {
                result.error = "rules/sink.json parse failed";
                logger.warn("load rules/sink.json got null rule");
                return result;
            }
            result.source = "file";
            return result;
        } catch (Exception ex) {
            logger.warn("load rules/sink.json failed: {}", ex.toString());
            result.error = ex.toString();
            return result;
        }
    }
}
