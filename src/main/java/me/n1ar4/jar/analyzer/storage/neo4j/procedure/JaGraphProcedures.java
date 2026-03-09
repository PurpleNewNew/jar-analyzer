/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.stream.Stream;

public final class JaGraphProcedures {
    @Procedure(name = "ja.path.shortest", mode = Mode.READ)
    @Description("Jar Analyzer shortest path search over the in-memory graph snapshot.")
    public Stream<JaNativeBridge.JaProcedureRow> shortest(@Name("from") Object from,
                                                          @Name("to") Object to,
                                                          @Name("maxHops") Long maxHops) {
        return rows("ja.path.shortest", List.of(from, to, maxHops));
    }

    @Procedure(name = "ja.path.shortest_pruned", mode = Mode.READ)
    @Description("Jar Analyzer shortest path search with bounded pruning over the in-memory graph snapshot.")
    public Stream<JaNativeBridge.JaProcedureRow> shortestPruned(@Name("from") Object from,
                                                                @Name("to") Object to,
                                                                @Name("maxHops") Long maxHops) {
        return rows("ja.path.shortest_pruned", List.of(from, to, maxHops));
    }

    @Procedure(name = "ja.path.from_to", mode = Mode.READ)
    @Description("Jar Analyzer bounded path enumeration over the in-memory graph snapshot.")
    public Stream<JaNativeBridge.JaProcedureRow> fromTo(@Name("from") Object from,
                                                        @Name("to") Object to,
                                                        @Name("maxHops") Long maxHops,
                                                        @Name("maxPaths") Long maxPaths) {
        return rows("ja.path.from_to", List.of(from, to, maxHops, maxPaths));
    }

    @Procedure(name = "ja.path.from_to_pruned", mode = Mode.READ)
    @Description("Jar Analyzer bounded path enumeration with pruning over the in-memory graph snapshot.")
    public Stream<JaNativeBridge.JaProcedureRow> fromToPruned(@Name("from") Object from,
                                                              @Name("to") Object to,
                                                              @Name("maxHops") Long maxHops,
                                                              @Name("maxPaths") Long maxPaths) {
        return rows("ja.path.from_to_pruned", List.of(from, to, maxHops, maxPaths));
    }

    @Procedure(name = "ja.taint.track", mode = Mode.READ)
    @Description("Jar Analyzer taint tracking over the in-memory graph snapshot.")
    public Stream<JaNativeBridge.JaProcedureRow> taintTrack(@Name("sourceClass") String sourceClass,
                                                            @Name("sourceMethod") String sourceMethod,
                                                            @Name("sourceDesc") String sourceDesc,
                                                            @Name("sinkClass") String sinkClass,
                                                            @Name("sinkMethod") String sinkMethod,
                                                            @Name("sinkDesc") String sinkDesc,
                                                            @Name("depth") Long depth,
                                                            @Name("timeoutMs") Long timeoutMs,
                                                            @Name("maxPaths") Long maxPaths,
                                                            @Name(value = "mode", defaultValue = "\"source\"") String mode,
                                                            @Name(value = "searchAllSources", defaultValue = "false") Boolean searchAllSources,
                                                            @Name(value = "onlyFromWeb", defaultValue = "false") Boolean onlyFromWeb) {
        return rows("ja.taint.track", List.of(
                sourceClass,
                sourceMethod,
                sourceDesc,
                sinkClass,
                sinkMethod,
                sinkDesc,
                depth,
                timeoutMs,
                maxPaths,
                mode,
                searchAllSources,
                onlyFromWeb
        ));
    }

    private static Stream<JaNativeBridge.JaProcedureRow> rows(String procName, List<Object> args) {
        return JaNativeBridge.toProcedureRows(JaNativeBridge.executeProcedure(procName, args)).stream();
    }
}
