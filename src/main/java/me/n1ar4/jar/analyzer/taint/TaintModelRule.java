/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaintModelRule {
    private static final Logger logger = LogManager.getLogger();

    @JSONField
    private List<TaintModel> rules;
    @JSONField(serialize = false, deserialize = false)
    private transient Map<String, Map<String, List<TaintModel>>> ruleIndex = new HashMap<>();
    @JSONField(serialize = false, deserialize = false)
    private transient boolean indexReady = false;

    public static TaintModelRule loadJSON(InputStream in) {
        if (in == null) {
            logger.warn("taint model json not found");
            return new TaintModelRule();
        }
        try {
            String jsonData = IOUtil.readString(in);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.warn("taint model json data is empty");
                return new TaintModelRule();
            }
            TaintModelRule rule = JSON.parseObject(jsonData, TaintModelRule.class);
            if (rule == null) {
                logger.warn("failed to parse taint model json");
                return new TaintModelRule();
            }
            rule.buildIndex();
            logger.info("loaded {} taint model rules", rule.getRules() != null ? rule.getRules().size() : 0);
            return rule;
        } catch (Exception ex) {
            logger.error("error loading taint model rules: {}", ex.toString());
            return new TaintModelRule();
        }
    }

    public List<TaintModel> getRules(String className, String methodName, String methodDesc) {
        ensureIndex();
        if (className == null || methodName == null || methodDesc == null) {
            return Collections.emptyList();
        }
        Map<String, List<TaintModel>> bySig = ruleIndex.get(className);
        if (bySig == null || bySig.isEmpty()) {
            return Collections.emptyList();
        }
        List<TaintModel> models = bySig.get(signatureKey(methodName, methodDesc));
        if (models == null) {
            return Collections.emptyList();
        }
        return models;
    }

    public List<TaintModel> getRules() {
        return rules;
    }

    public void setRules(List<TaintModel> rules) {
        this.rules = rules;
        this.indexReady = false;
        buildIndex();
    }

    private void ensureIndex() {
        if (!indexReady) {
            buildIndex();
        }
    }

    private void buildIndex() {
        ruleIndex.clear();
        indexReady = true;
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (TaintModel model : rules) {
            if (model == null) {
                continue;
            }
            String className = model.getClassName();
            String methodName = model.getMethodName();
            String methodDesc = model.getMethodDesc();
            if (className == null || methodName == null || methodDesc == null) {
                continue;
            }
            String sigKey = signatureKey(methodName, methodDesc);
            Map<String, List<TaintModel>> bySig = ruleIndex.computeIfAbsent(className, k -> new HashMap<>());
            List<TaintModel> list = bySig.computeIfAbsent(sigKey, k -> new java.util.ArrayList<>());
            list.add(model);
        }
    }

    private static String signatureKey(String methodName, String methodDesc) {
        return methodName + "#" + methodDesc;
    }
}
