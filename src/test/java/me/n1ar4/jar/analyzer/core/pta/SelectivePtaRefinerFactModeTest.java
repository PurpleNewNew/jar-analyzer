package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.BytecodeSymbolRunner;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.facts.BuildFactAssembler;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectivePtaRefinerFactModeTest {
    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldRefineHotspotEdgesFromSnapshotAndEdgeAccumulator() {
        BuildFactSnapshot snapshot = buildCallbackSnapshot();
        BuildEdgeAccumulator edges = BuildEdgeAccumulator.empty();

        SelectivePtaRefiner.Result result = SelectivePtaRefiner.refine(
                snapshot,
                edges,
                snapshot.bytecode().workspace(),
                snapshot.types().inheritanceMap(),
                false
        );

        assertTrue(result.ptaEdges() > 0);
        assertTrue(result.scannedMethods() > 0);
        assertTrue(result.scannedMethods() < snapshot.methods().methodsByHandle().size());
        CallSiteEntity fieldDispatchSite = snapshot.symbols().callSites().stream()
                .filter(site -> site != null
                        && "me/n1ar4/cb/CallbackEntry".equals(site.getCallerClassName())
                        && "ptaFieldSensitiveDispatch".equals(site.getCallerMethodName())
                        && "run".equals(site.getCalleeMethodName())
                        && "()V".equals(site.getCalleeMethodDesc()))
                .findFirst()
                .orElse(null);
        assertNotNull(fieldDispatchSite);
        assertEquals(
                "task",
                snapshot.constraints().receiverVarByCallSiteKey().get(fieldDispatchSite.getCallSiteKey())
        );
        MethodReference.Handle caller = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "ptaFieldSensitiveDispatch",
                "()V"
        );
        BuildFactSnapshot.MethodConstraintFacts fieldFacts = snapshot.constraints().methodConstraints(caller);
        assertFalse(fieldFacts.objectAssignEdgesByVar().isEmpty());
        assertFalse(fieldFacts.fieldStoreEdgesByFieldKey().isEmpty());

        MethodReference.Handle arrayCopyCaller = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "ptaNativeArrayCopyDispatch",
                "()V"
        );
        BuildFactSnapshot.MethodConstraintFacts arrayCopyFacts = snapshot.constraints().methodConstraints(arrayCopyCaller);
        assertFalse(arrayCopyFacts.arrayCopyEdges().isEmpty());
        assertFalse(arrayCopyFacts.nativeModelHints().isEmpty());
        assertEquals("arraycopy", arrayCopyFacts.nativeModelHints().get(0).kind());

        MethodReference.Handle callee = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/FastTask", 1),
                Opcodes.INVOKEVIRTUAL,
                "run",
                "()V"
        );
        MethodCallMeta meta = MethodCallMeta.resolve(edges.methodCallMeta(), caller, callee);
        assertNotNull(meta);
        assertTrue(meta.getEvidence().contains("field"));
        assertTrue(meta.getEvidence().contains("receiver=task"));
    }

    private static BuildFactSnapshot buildCallbackSnapshot() {
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        BuildContext context = new BuildContext();
        context.classFileList.addAll(classFiles);
        DiscoveryRunner.start(
                workspace,
                context.discoveredClasses,
                context.discoveredMethods,
                context.classMap,
                context.methodMap,
                context.stringAnnoMap
        );
        BytecodeSymbolRunner.Result symbolResult = BytecodeSymbolRunner.start(workspace);
        context.callSites.addAll(symbolResult.getCallSites());

        return BuildFactAssembler.from(
                context,
                workspace
        );
    }
}
