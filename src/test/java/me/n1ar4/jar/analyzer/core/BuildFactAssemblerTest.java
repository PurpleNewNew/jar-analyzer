package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.facts.BuildFactAssembler;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.scope.ArchiveScopeClassifier.ScopeSummary;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildFactAssemblerTest {
    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldAssembleSnapshotAndLegacyViewFromBuildContext() {
        BuildContext context = new BuildContext();

        ClassReference owner = new ClassReference(
                61,
                Opcodes.ACC_PUBLIC,
                "sample/Foo",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
        MethodReference caller = new MethodReference(
                owner.getHandle(),
                "run",
                "()V",
                false,
                Set.of(),
                Opcodes.ACC_PUBLIC,
                12,
                "app.jar",
                1,
                7
        );
        MethodReference callee = new MethodReference(
                owner.getHandle(),
                "target",
                "()V",
                false,
                Set.of(),
                Opcodes.ACC_PUBLIC,
                22,
                "app.jar",
                1
        );
        CallSiteEntity callSite = new CallSiteEntity();
        callSite.setCallerClassName("sample/Foo");
        callSite.setCallerMethodName("run");
        callSite.setCallerMethodDesc("()V");
        callSite.setCalleeOwner("sample/Foo");
        callSite.setCalleeMethodName("target");
        callSite.setCalleeMethodDesc("()V");
        callSite.setOpCode(Opcodes.INVOKEVIRTUAL);
        callSite.setJarId(1);
        callSite.setCallSiteKey("sample/Foo#run()V@0");

        LocalVarEntity localVar = new LocalVarEntity();
        localVar.setClassName("sample/Foo");
        localVar.setMethodName("run");
        localVar.setMethodDesc("()V");
        localVar.setVarIndex(1);
        localVar.setVarName("arg");
        localVar.setVarDesc("Ljava/lang/String;");
        localVar.setJarId(1);

        ClassFileEntity classFile = new ClassFileEntity("sample/Foo", Path.of("target/test-facts/Foo.class"), 1);
        context.classFileList.add(classFile);
        context.discoveredClasses.add(owner);
        context.classMap.put(owner.getHandle(), owner);
        context.discoveredMethods.add(caller);
        context.discoveredMethods.add(callee);
        context.methodMap.put(caller.getHandle(), caller);
        context.methodMap.put(callee.getHandle(), callee);
        context.callSites.add(callSite);
        context.strMap.put(caller.getHandle(), List.of("hello"));
        context.stringAnnoMap.put(caller.getHandle(), List.of("demo"));
        context.explicitSourceMethodFlags.put(caller.getHandle(), 3);
        HashSet<MethodReference.Handle> callees = new HashSet<>();
        callees.add(callee.getHandle());
        context.methodCalls.put(caller.getHandle(), callees);
        MethodCallMeta.record(
                context.methodCallMeta,
                MethodCallKey.of(caller.getHandle(), callee.getHandle()),
                MethodCallMeta.TYPE_DIRECT,
                MethodCallMeta.CONF_HIGH,
                "unit-test"
        );

        ScopeSummary scopeSummary = new ScopeSummary(
                Map.of(
                        Path.of("/tmp/app.jar"), ProjectOrigin.APP,
                        Path.of("/tmp/lib.jar"), ProjectOrigin.LIBRARY,
                        Path.of("/tmp/rt.jar"), ProjectOrigin.SDK
                ),
                1,
                1,
                1
        );
        Map<Integer, ProjectOrigin> jarOriginsById = Map.of(
                1, ProjectOrigin.APP,
                2, ProjectOrigin.LIBRARY
        );

        BuildFactSnapshot snapshot = BuildFactAssembler.from(
                context,
                scopeSummary,
                jarOriginsById,
                List.of(localVar)
        );

        assertEquals(3, snapshot.archives().allArchives().size());
        assertEquals(1, snapshot.archives().targetArchiveCount());
        assertEquals(1, snapshot.archives().libraryArchiveCount());
        assertEquals(1, snapshot.archives().sdkEntryCount());
        assertEquals(ProjectOrigin.APP, snapshot.archives().jarOriginsById().get(1));
        assertEquals(owner, snapshot.types().classesByHandle().get(owner.getHandle()));
        assertTrue(snapshot.methods().methodsByClass().containsKey(owner.getHandle()));
        assertSame(callSite, snapshot.symbols().callSitesByKey().get("sample/Foo#run()V@0"));
        assertSame(localVar, snapshot.symbols().localVarsByMethod().get(caller.getHandle()).get(0));
        assertEquals(List.of("hello"), snapshot.symbols().stringLiteralsByMethod().get(caller.getHandle()));
        assertEquals(List.of("demo"), snapshot.symbols().annotationStringsByMethod().get(caller.getHandle()));
        assertEquals(7, snapshot.semantics().methodSemanticFlags().get(caller.getHandle()));
        assertSame(classFile, snapshot.bytecode().classFiles().iterator().next());

        BuildEdgeAccumulator edges = BuildEdgeAccumulator.fromContext(context);
        BuildContext legacyView = BuildFactAssembler.legacyView(snapshot, edges);

        assertEquals(context.methodCalls.keySet(), legacyView.methodCalls.keySet());
        assertEquals(context.methodCallMeta.keySet(), legacyView.methodCallMeta.keySet());
        assertEquals(1, legacyView.callSites.size());
        assertEquals(2, legacyView.methodMap.size());
        assertEquals(1, legacyView.strMap.get(caller.getHandle()).size());
        assertEquals(3, legacyView.explicitSourceMethodFlags.get(caller.getHandle()));
    }
}
