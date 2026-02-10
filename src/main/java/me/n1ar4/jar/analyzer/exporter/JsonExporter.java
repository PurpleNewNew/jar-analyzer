/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.exporter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.utils.StableOrder;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonExporter implements Exporter {
    private static final Logger logger = LogManager.getLogger();
    private final CoreEngine engine;
    private String fileName;

    public JsonExporter() {
        this.engine = EngineContext.getEngine();
    }

    @Override
    public boolean doExport() {
        ArrayList<ClassResult> servlets = this.engine.getAllServlets();
        ArrayList<ClassResult> filters = this.engine.getAllFilters();
        ArrayList<ClassResult> listeners = this.engine.getAllListeners();
        ArrayList<ClassResult> interceptors = this.engine.getAllSpringI();
        ArrayList<ClassResult> controllers = this.engine.getAllSpringC();

        servlets.sort(StableOrder.CLASS_RESULT);
        filters.sort(StableOrder.CLASS_RESULT);
        listeners.sort(StableOrder.CLASS_RESULT);
        interceptors.sort(StableOrder.CLASS_RESULT);
        controllers.sort(StableOrder.CLASS_RESULT);

        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("servlets", servlets);
        exportData.put("filters", filters);
        exportData.put("listeners", listeners);
        exportData.put("interceptors", interceptors);

        ArrayList<Map<String, Object>> controllersList = new ArrayList<>();
        for (ClassResult cr : controllers) {
            Map<String, Object> controllerMap = new LinkedHashMap<>();
            controllerMap.put("className", cr.getClassName());
            ArrayList<MethodResult> methods = this.engine.getSpringM(cr.getClassName());
            methods.sort(StableOrder.METHOD_RESULT);
            controllerMap.put("methods", methods);
            controllersList.add(controllerMap);
        }
        exportData.put("controllers", controllersList);

        this.fileName = String.format("jar-analyzer-%d.json", System.currentTimeMillis());
        try {
            String jsonString = JSON.toJSONString(exportData, JSONWriter.Feature.PrettyFormat);
            Files.write(Paths.get(fileName), jsonString.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception ex) {
            logger.error("export error : " + ex.getMessage());
        }
        return false;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }
}
