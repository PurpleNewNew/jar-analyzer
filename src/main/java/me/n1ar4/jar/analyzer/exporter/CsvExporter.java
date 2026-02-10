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

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.utils.StableOrder;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.ArrayList;

public class CsvExporter implements Exporter {
    private static final Logger logger = LogManager.getLogger();
    private final CoreEngine engine;
    private String fileName;

    public CsvExporter() {
        this.engine = EngineContext.getEngine();
    }

    @Override
    public boolean doExport() {
        this.fileName = String.format("jar-analyzer-%d.csv", System.currentTimeMillis());
        try (FileWriter out = new FileWriter(fileName);
             CSVPrinter printer = new CSVPrinter(out,
                     CSVFormat.DEFAULT.builder().setHeader("Type", "ClassName",
                             "MethodName", "RestfulType", "Path").get())) {
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

            for (ClassResult cr : servlets) {
                printer.printRecord("Servlet", cr.getClassName(), "", "", "");
            }
            for (ClassResult cr : filters) {
                printer.printRecord("Filter", cr.getClassName(), "", "", "");
            }
            for (ClassResult cr : listeners) {
                printer.printRecord("Listener", cr.getClassName(), "", "", "");
            }
            for (ClassResult cr : interceptors) {
                printer.printRecord("Interceptor", cr.getClassName(), "", "", "");
            }
            for (ClassResult cr : controllers) {
                String className = cr.getClassName();
                ArrayList<MethodResult> methods = this.engine.getSpringM(className);
                methods.sort(StableOrder.METHOD_RESULT);
                if (methods.isEmpty()) {
                    printer.printRecord("Controller", className, "", "", "");
                } else {
                    for (MethodResult method : methods) {
                        printer.printRecord("Controller", className,
                                method.getMethodName(),
                                method.getRestfulType(),
                                method.getActualPath());
                    }
                }
            }
            return true;
        } catch (Exception ex) {
            logger.error("export error : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }
}
