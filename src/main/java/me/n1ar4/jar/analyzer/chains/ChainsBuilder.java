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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
     * 加载 sink 规则，只从 rules/dfs-sink.json 加载
     */
    private static void loadSinkRules() {
        File rulesFile = new File("rules/dfs-sink.json");
        if (!rulesFile.exists() || !rulesFile.isFile()) {
            logger.error("rules/dfs-sink.json not found");
            return;
        }
        try (FileInputStream fis = new FileInputStream(rulesFile)) {
            if (loadFromInputStream(fis)) {
                logger.info("load sink rule: {}", rulesFile.getPath());
            }
        } catch (Exception e) {
            logger.warn("load rules/dfs-sink.json failed: {}", e.getMessage());
        }
    }

    /**
     * 从输入流加载 JSON 格式的 sink 规则
     */
    private static boolean loadFromInputStream(InputStream inputStream) {
        try {
            String jsonData = IOUtil.readString(inputStream);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.warn("JSON 数据为空");
                return false;
            }
            List<SinkModel> sinkList = JSON.parseObject(jsonData, new TypeReference<List<SinkModel>>() {
            });
            if (sinkList == null || sinkList.isEmpty()) {
                logger.warn("解析的 sink 规则列表为空");
                return false;
            }
            sinkData.clear();
            for (SinkModel sink : sinkList) {
                if (sink.getBoxName() != null && !sink.getBoxName().trim().isEmpty()) {
                    sinkData.put(sink.getBoxName(), sink);
                }
            }
            logger.info("load {} sink rule", sinkData.size());
            return true;
        } catch (Exception e) {
            logger.error("解析 JSON 格式的 sink 规则失败: {}", e.getMessage());
            return false;
        }
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
