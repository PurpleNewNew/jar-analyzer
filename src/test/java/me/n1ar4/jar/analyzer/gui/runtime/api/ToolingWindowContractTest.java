/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolingWindowContractTest {
    @Test
    void toolingWindowRequestShouldRejectMissingAction() {
        assertThrows(NullPointerException.class,
                () -> new ToolingWindowRequest(null, new ToolingWindowPayload.EmptyPayload()));
    }

    @Test
    void scaAndGadgetPickerShouldEmitTypedPathPayload() {
        List<ToolingWindowRequest> requests = new ArrayList<>();
        RuntimeFacades.setToolingWindowConsumer(requests::add);
        try {
            RuntimeFacades.sca().apply(new ScaSettingsDto(
                    true, false, true, "/tmp/sca-input.jar", ScaOutputMode.CONSOLE, ""
            ));
            RuntimeFacades.gadget().apply(new GadgetSettingsDto(
                    "/tmp/gadget-input", true, false, true, false
            ));

            RuntimeFacades.sca().chooseInput();
            RuntimeFacades.gadget().chooseDir();

            assertEquals(2, requests.size());

            ToolingWindowRequest scaReq = requests.get(0);
            assertEquals(ToolingWindowAction.SCA_INPUT_PICKER, scaReq.action());
            ToolingWindowPayload.PathPayload scaPayload =
                    assertInstanceOf(ToolingWindowPayload.PathPayload.class, scaReq.payload());
            assertEquals("/tmp/sca-input.jar", scaPayload.value());

            ToolingWindowRequest gadgetReq = requests.get(1);
            assertEquals(ToolingWindowAction.GADGET_DIR_PICKER, gadgetReq.action());
            ToolingWindowPayload.PathPayload gadgetPayload =
                    assertInstanceOf(ToolingWindowPayload.PathPayload.class, gadgetReq.payload());
            assertEquals("/tmp/gadget-input", gadgetPayload.value());
        } finally {
            RuntimeFacades.setToolingWindowConsumer(null);
        }
    }

    @Test
    void chainsViewerShouldEmitStructuredPayload() {
        ChainsResultStore store = ChainsResultStore.getInstance();
        store.clear();
        List<ToolingWindowRequest> requests = new ArrayList<>();
        RuntimeFacades.setToolingWindowConsumer(requests::add);
        try {
            MethodReference.Handle handle = new MethodReference.Handle(
                    new ClassReference.Handle("a/b/C"),
                    "run",
                    "(Ljava/lang/String;)V"
            );
            FlowPath dfs = new FlowPath();
            dfs.setSource(handle);
            dfs.setSink(handle);
            dfs.setDepth(3);
            dfs.setPathCount(2);
            dfs.setNodeCount(5);
            dfs.setEdgeCount(4);
            dfs.setElapsedMs(123);
            dfs.setTruncated(true);
            dfs.setTruncateReason("max depth");
            dfs.setRecommend("increase depth");
            dfs.setMethodList(List.of(handle));
            ChainsResultStore.Context context = ChainsResultStore.captureCurrentContext();
            store.replaceDfs(context, List.of(dfs));

            TaintResult taint = new TaintResult();
            taint.setDfsResult(dfs);
            taint.setSuccess(true);
            taint.setLowConfidence(false);
            taint.setTaintText("sanitizer hit: demo");
            store.replaceTaint(context, List.of(taint));

            RuntimeFacades.tooling().openChainsDfsResult();
            RuntimeFacades.tooling().openChainsTaintResult();

            assertEquals(2, requests.size());
            assertEquals(ToolingWindowAction.CHAINS_RESULT, requests.get(0).action());
            assertEquals(ToolingWindowAction.CHAINS_RESULT, requests.get(1).action());

            ToolingWindowPayload.ChainsResultPayload dfsPayload = assertInstanceOf(
                    ToolingWindowPayload.ChainsResultPayload.class, requests.get(0).payload()
            );
            assertFalse(dfsPayload.taintView());
            assertEquals("DFS Result", dfsPayload.title());
            assertEquals(1, dfsPayload.items().size());
            assertEquals(1, dfsPayload.items().get(0).methods().size());

            ToolingWindowPayload.ChainsResultPayload taintPayload = assertInstanceOf(
                    ToolingWindowPayload.ChainsResultPayload.class, requests.get(1).payload()
            );
            assertTrue(taintPayload.taintView());
            assertEquals("Taint Result", taintPayload.title());
            assertEquals(1, taintPayload.items().size());
            assertTrue(taintPayload.items().get(0).taintPass());
            assertNotNull(taintPayload.items().get(0).sanitizerDetail());
        } finally {
            RuntimeFacades.setToolingWindowConsumer(null);
            store.clear();
        }
    }
}
