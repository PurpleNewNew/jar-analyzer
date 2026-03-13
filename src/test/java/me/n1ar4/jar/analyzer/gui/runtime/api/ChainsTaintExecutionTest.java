/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPathEdge;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainsTaintExecutionTest {
    @AfterEach
    void cleanup() {
        ChainsResultStore.getInstance().clear();
        DatabaseManager.clearAllData();
    }

    @Test
    void startTaintShouldAnalyzeCachedDfsResultsEvenWhenSourceIsExplicit() throws Exception {
        MethodReference.Handle source = new MethodReference.Handle(
                new ClassReference.Handle("demo/Source"),
                "entry",
                "()V"
        );
        MethodReference.Handle sink = new MethodReference.Handle(
                new ClassReference.Handle("demo/Sink"),
                "sink",
                "()V"
        );
        FlowPathEdge edge = new FlowPathEdge();
        edge.setFrom(source);
        edge.setTo(sink);
        edge.setType("direct");

        FlowPath dfs = new FlowPath();
        dfs.setMode(FlowPath.FROM_SOURCE_TO_SINK);
        dfs.setSource(source);
        dfs.setSink(sink);
        dfs.setMethodList(List.of(source, sink));
        dfs.setEdges(List.of(edge));
        ChainsResultStore.getInstance().replaceDfs(
                ChainsResultStore.captureCurrentContext(),
                List.of(dfs)
        );

        RuntimeFacades.chains().apply(new ChainsSettingsDto(
                true,
                false,
                "demo/Sink",
                "sink",
                "()V",
                "demo/Source",
                "entry",
                "()V",
                false,
                true,
                8,
                false,
                false,
                "",
                "low",
                false,
                true,
                20
        ));

        RuntimeFacades.chains().startTaint();

        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline
                && RuntimeFacades.chains().snapshot().taintCount() == 0) {
            Thread.sleep(25L);
        }

        assertEquals(1, RuntimeFacades.chains().snapshot().taintCount());
    }
}
