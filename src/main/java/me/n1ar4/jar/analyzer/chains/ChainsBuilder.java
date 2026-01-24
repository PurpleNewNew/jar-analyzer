/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.chains;

import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChainsBuilder {
    private static final Logger logger = LogManager.getLogger();
    public static final Map<String, SinkModel> sinkData = new LinkedHashMap<>();

    static {
        loadSinkRules();
    }

    /**
     * 加载 sink 规则，统一从 ModelRegistry 入口读取
     */
    private static void loadSinkRules() {
        List<SinkModel> sinkList = ModelRegistry.getSinkModels();
        if (sinkList == null || sinkList.isEmpty()) {
            logger.warn("sink rule list is empty");
            return;
        }
        sinkData.clear();
        for (SinkModel sink : sinkList) {
            if (sink.getBoxName() != null && !sink.getBoxName().trim().isEmpty()) {
                sinkData.put(sink.getBoxName(), sink);
            }
        }
        logger.info("load {} sink rule", sinkData.size());
    }

    public static void buildBox(
            JComboBox<String> sinkBox,
            JTextField sinkClassText,
            JTextField sinkMethodText,
            JTextField sinkDescText) {
        if (sinkData.isEmpty()) {
            logger.warn("sink rule list is empty");
            return;
        }
        for (String sink : sinkData.keySet()) {
            sinkBox.addItem(sink);
        }
        if (sinkBox.getItemCount() > 0) {
            sinkBox.setSelectedIndex(0);
        }
        sinkBox.addActionListener(e -> {
            String key = (String) sinkBox.getSelectedItem();
            SinkModel model = getSinkByName(key);
            if (model == null) {
                return;
            }
            sinkClassText.setText(model.getClassName());
            sinkClassText.setCaretPosition(0);
            sinkMethodText.setText(model.getMethodName());
            sinkMethodText.setCaretPosition(0);
            sinkDescText.setText(model.getMethodDesc());
            sinkDescText.setCaretPosition(0);
        });
    }

    public static SinkModel getSinkByName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim();
        if (key.isEmpty()) {
            return null;
        }
        SinkModel direct = sinkData.get(key);
        if (direct != null) {
            return direct;
        }
        String simpleKey = stripParams(key);
        for (SinkModel sink : sinkData.values()) {
            if (sink == null) {
                continue;
            }
            String box = sink.getBoxName();
            if (box != null && box.equalsIgnoreCase(key)) {
                return sink;
            }
            String simple = buildSimpleName(sink);
            if (simple != null && simple.equalsIgnoreCase(simpleKey)) {
                return sink;
            }
        }
        return null;
    }

    private static String stripParams(String name) {
        int idx = name.indexOf('(');
        if (idx > 0) {
            return name.substring(0, idx);
        }
        return name;
    }

    private static String buildSimpleName(SinkModel sink) {
        if (sink == null) {
            return null;
        }
        String className = sink.getClassName();
        String methodName = sink.getMethodName();
        if (className == null || methodName == null) {
            return null;
        }
        String simple = className;
        int idx = simple.lastIndexOf('/');
        if (idx >= 0 && idx < simple.length() - 1) {
            simple = simple.substring(idx + 1);
        }
        simple = simple.replace('$', '.');
        return simple + "." + methodName;
    }
}
