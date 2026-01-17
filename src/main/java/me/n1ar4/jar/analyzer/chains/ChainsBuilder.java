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
            SinkModel model = sinkData.get(key);
            sinkClassText.setText(model.getClassName());
            sinkClassText.setCaretPosition(0);
            sinkMethodText.setText(model.getMethodName());
            sinkMethodText.setCaretPosition(0);
            sinkDescText.setText(model.getMethodDesc());
            sinkDescText.setCaretPosition(0);
        });
    }
}
