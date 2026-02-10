/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.rules.vul.Rule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class YamlUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final LoaderOptions lOptions = new LoaderOptions();
    private static final DumperOptions dOptions = new DumperOptions();
    private static final Yaml yaml;

    static {
        final String legacyRule = "me.n1ar4.jar.analyzer.gui.vul.Rule";
        final String rule = Rule.class.getName();
        // 允许反序列化的类
        TagInspector taginspector = tag ->
                // Rule
                tag.getClassName().equals(rule) ||
                        tag.getClassName().equals(legacyRule) ||
                        // SearchCondition
                        tag.getClassName().equals(SearchCondition.class.getName());
        lOptions.setTagInspector(taginspector);
        // 输出格式
        dOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dOptions.setPrettyFlow(true);
        yaml = new Yaml(lOptions, dOptions);
    }

    public static Rule loadAs(byte[] data) {
        return yaml.loadAs(new String(data, StandardCharsets.UTF_8), Rule.class);
    }

    public static void dumpFile(Rule rule, String output) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output), StandardCharsets.UTF_8)) {
            yaml.dump(rule, writer);
        } catch (Exception ex) {
            logger.debug("dump yaml failed: {}: {}", output, ex.toString());
        }
    }
}
