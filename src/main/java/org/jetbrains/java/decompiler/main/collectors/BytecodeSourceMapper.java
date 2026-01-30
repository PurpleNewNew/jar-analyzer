// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.collectors;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;
import java.util.Map.Entry;

public class BytecodeSourceMapper {
    private int offset_total;

    // class, method, bytecode offset, source line
    private final Map<String, Map<String, Map<Integer, Integer>>> mapping = new LinkedHashMap<>();

    // original line to decompiled line
    private final Map<Integer, Integer> linesMapping = new HashMap<>();
    private final Set<Integer> unmappedLines = new TreeSet<>();
    private final Map<String, Map<Integer, Integer>> linesMappingByClass = new LinkedHashMap<>();
    private final Map<String, Set<Integer>> unmappedLinesByClass = new HashMap<>();
    private final Map<String, Integer> offsetByClass = new HashMap<>();
    private final Map<String, Map<String, Map<Integer, Integer>>> linesMappingByMethod = new LinkedHashMap<>();

    public void addMapping(String className, String methodName, int bytecodeOffset, int sourceLine) {
        Map<String, Map<Integer, Integer>> class_mapping = mapping.computeIfAbsent(className, k -> new LinkedHashMap<>()); // need to preserve order
        Map<Integer, Integer> method_mapping = class_mapping.computeIfAbsent(methodName, k -> new HashMap<>());

        // don't overwrite
        method_mapping.putIfAbsent(bytecodeOffset, sourceLine);
    }

    public void addTracer(String className, String methodName, BytecodeMappingTracer tracer) {
        for (Entry<Integer, Integer> entry : tracer.getMapping().entrySet()) {
            addMapping(className, methodName, entry.getKey(), entry.getValue());
        }
        Map<Integer, Integer> originalLines = tracer.getOriginalLinesMapping();
        if (!originalLines.isEmpty()) {
            linesMapping.putAll(originalLines);
            if (className != null) {
                Map<Integer, Integer> classLines = linesMappingByClass.computeIfAbsent(className, k -> new HashMap<>());
                classLines.putAll(originalLines);
            }
        }
        Set<Integer> missing = tracer.getUnmappedLines();
        if (missing != null && !missing.isEmpty()) {
            unmappedLines.addAll(missing);
            if (className != null) {
                Set<Integer> classMissing = unmappedLinesByClass.computeIfAbsent(className, k -> new TreeSet<>());
                classMissing.addAll(missing);
            }
        }
        if (className != null && methodName != null && originalLines != null && !originalLines.isEmpty()) {
            Map<String, Map<Integer, Integer>> byMethod = linesMappingByMethod.computeIfAbsent(className, k -> new LinkedHashMap<>());
            Map<Integer, Integer> methodLines = byMethod.computeIfAbsent(methodName, k -> new HashMap<>());
            methodLines.putAll(originalLines);
        }
    }

    public void dumpMapping(TextBuffer buffer, boolean offsetsToHex) {
        if (mapping.isEmpty() && linesMapping.isEmpty()) {
            return;
        }

        String lineSeparator = DecompilerContext.getNewLineSeparator();

        for (Entry<String, Map<String, Map<Integer, Integer>>> class_entry : mapping.entrySet()) {
            Map<String, Map<Integer, Integer>> class_mapping = class_entry.getValue();
            buffer.append("class '" + class_entry.getKey() + "' {" + lineSeparator);

            boolean is_first_method = true;
            for (Entry<String, Map<Integer, Integer>> method_entry : class_mapping.entrySet()) {
                Map<Integer, Integer> method_mapping = method_entry.getValue();

                if (!is_first_method) {
                    buffer.appendLineSeparator();
                }

                buffer.appendIndent(1).append("method '" + method_entry.getKey() + "' {" + lineSeparator);

                List<Integer> lstBytecodeOffsets = new ArrayList<>(method_mapping.keySet());
                Collections.sort(lstBytecodeOffsets);

                for (Integer offset : lstBytecodeOffsets) {
                    Integer line = method_mapping.get(offset);

                    String strOffset = offsetsToHex ? Integer.toHexString(offset) : line.toString();
                    buffer.appendIndent(2).append(strOffset).appendIndent(2).append((line + offset_total) + lineSeparator);
                }
                buffer.appendIndent(1).append("}").appendLineSeparator();

                is_first_method = false;
            }

            buffer.append("}").appendLineSeparator().appendLineSeparator();
        }

        // lines mapping
        buffer.append("Lines mapping:").appendLineSeparator();
        Map<Integer, Integer> sorted = new TreeMap<>(linesMapping);
        for (Entry<Integer, Integer> entry : sorted.entrySet()) {
            buffer.append(entry.getKey()).append(" <-> ").append(entry.getValue() + offset_total + 1).appendLineSeparator();
        }

        if (!unmappedLines.isEmpty()) {
            buffer.append("Not mapped:").appendLineSeparator();
            for (Integer line : unmappedLines) {
                if (!linesMapping.containsKey(line)) {
                    buffer.append(line).appendLineSeparator();
                }
            }
        }
    }

    public void addTotalOffset(int offset_total) {
        this.offset_total += offset_total;
    }

    /**
     * Original to decompiled line mapping.
     */
    public int[] getOriginalLinesMapping() {
        return buildOriginalLinesMapping(linesMapping, unmappedLines, offset_total);
    }

    public void addTotalOffset(String className, int offset) {
        if (className != null) {
            offsetByClass.merge(className, offset, Integer::sum);
        }
        addTotalOffset(offset);
    }

    public int[] getOriginalLinesMapping(String className) {
        if (className == null) {
            return getOriginalLinesMapping();
        }
        Map<Integer, Integer> classLines = linesMappingByClass.get(className);
        if (classLines == null || classLines.isEmpty()) {
            return new int[0];
        }
        int offset = offsetByClass.getOrDefault(className, 0);
        Set<Integer> classMissing = unmappedLinesByClass.get(className);
        return buildOriginalLinesMapping(classLines, classMissing, offset);
    }

    public int[] getOriginalLinesMapping(String className, String methodName) {
        if (className == null || methodName == null) {
            return new int[0];
        }
        Map<String, Map<Integer, Integer>> byMethod = linesMappingByMethod.get(className);
        if (byMethod == null) {
            return new int[0];
        }
        Map<Integer, Integer> methodLines = byMethod.get(methodName);
        if (methodLines == null || methodLines.isEmpty()) {
            return new int[0];
        }
        int offset = offsetByClass.getOrDefault(className, 0);
        return buildOriginalLinesMapping(methodLines, null, offset);
    }

    public Map<String, int[]> getOriginalLinesMappingByMethod(String className) {
        if (className == null) {
            return Collections.emptyMap();
        }
        Map<String, Map<Integer, Integer>> byMethod = linesMappingByMethod.get(className);
        if (byMethod == null || byMethod.isEmpty()) {
            return Collections.emptyMap();
        }
        int offset = offsetByClass.getOrDefault(className, 0);
        Map<String, int[]> result = new LinkedHashMap<>();
        for (Entry<String, Map<Integer, Integer>> entry : byMethod.entrySet()) {
            int[] mapping = buildOriginalLinesMapping(entry.getValue(), null, offset);
            if (mapping.length > 0) {
                result.put(entry.getKey(), mapping);
            }
        }
        return result;
    }

    private int[] buildOriginalLinesMapping(Map<Integer, Integer> source,
                                            Set<Integer> unmapped,
                                            int offset) {
        if (source == null || source.isEmpty()) {
            return new int[0];
        }
        int[] res = new int[source.size() * 2];
        int i = 0;
        for (Entry<Integer, Integer> entry : source.entrySet()) {
            res[i] = entry.getKey();
            if (unmapped != null) {
                unmapped.remove(entry.getKey());
            }
            res[i + 1] = entry.getValue() + offset + 1; // make it 1 based
            i += 2;
        }
        return res;
    }
}
