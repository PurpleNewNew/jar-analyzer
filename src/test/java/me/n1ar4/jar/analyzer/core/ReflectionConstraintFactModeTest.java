package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BuildFactAssembler;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

class ReflectionConstraintFactModeTest {
    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldCollectReflectionAndMethodHandleHintsIntoConstraintFacts() {
        BuildFactSnapshot snapshot = buildCallbackSnapshot();

        BuildFactSnapshot.AliasValueFact fieldClassFacts = snapshot.constraints()
                .instanceFieldFact("me/n1ar4/cb/ReflectionBox", "className");
        BuildFactSnapshot.AliasValueFact fieldMethodFacts = snapshot.constraints()
                .instanceFieldFact("me/n1ar4/cb/ReflectionBox", "methodName");
        assertEquals("me.n1ar4.cb.ReflectionTarget", fieldClassFacts.uniqueStringValue());
        assertEquals("target", fieldMethodFacts.uniqueStringValue());

        MethodReference.Handle reflectHelper = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "reflectViaHelperFlow",
                "()V"
        );
        BuildFactSnapshot.MethodConstraintFacts reflectFacts = snapshot.constraints().methodConstraints(reflectHelper);
        assertFalse(reflectFacts.reflectionHints().reflectionInvokeHints().isEmpty());
        BuildFactSnapshot.MethodInvokeHint reflectHint = reflectFacts.reflectionHints().reflectionInvokeHints()
                .values()
                .stream()
                .filter(hint -> hint != null
                        && hint.targets().stream().anyMatch(target ->
                        "me/n1ar4/cb/ReflectionTarget".equals(target.getClassReference().getName())
                                && "target".equals(target.getName())
                                && "()V".equals(target.getDesc())))
                .findFirst()
                .orElse(null);
        assertTrue(reflectFacts.reflectionHints().reflectionInvokeHints().values().stream().anyMatch(hint ->
                hint != null && hint.targets().stream().anyMatch(target -> "<init>".equals(target.getName()))));
        assertTrue(reflectHint != null);
        assertTrue(reflectHint.reason().contains("for_name"));
        assertEquals(BuildFactSnapshot.ReflectionHintTier.CONST, reflectHint.tier());
        assertFalse(reflectHint.imprecise());

        MethodReference.Handle methodHandleHelper = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "methodHandleViaHelperFlow",
                "()V"
        );
        BuildFactSnapshot.MethodConstraintFacts methodHandleFacts = snapshot.constraints().methodConstraints(methodHandleHelper);
        assertFalse(methodHandleFacts.reflectionHints().methodHandleInvokeHints().isEmpty());
        BuildFactSnapshot.MethodInvokeHint methodHandleHint = methodHandleFacts.reflectionHints().methodHandleInvokeHints()
                .values()
                .iterator()
                .next();
        assertTrue(methodHandleHint.reason().contains("method_handle"));
        assertEquals(1, methodHandleHint.targets().size());
        assertEquals("me/n1ar4/cb/ReflectionTarget", methodHandleHint.targets().get(0).getClassReference().getName());
        assertEquals("target", methodHandleHint.targets().get(0).getName());
        assertEquals("()V", methodHandleHint.targets().get(0).getDesc());
        assertEquals(BuildFactSnapshot.ReflectionHintTier.CONST, methodHandleHint.tier());
        assertFalse(methodHandleHint.imprecise());

        assertMethodHandleHint(
                snapshot,
                "methodHandleFindStatic",
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "staticTarget",
                "()V"
        );
        assertMethodHandleHint(
                snapshot,
                "methodHandleFindConstructor",
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "<init>",
                "()V"
        );
        assertMethodHandleHint(
                snapshot,
                "methodHandleBindToReceiver",
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "target",
                "()V"
        );
        assertMethodHandleHint(
                snapshot,
                "methodHandleFindSpecial",
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "specialTarget",
                "()V"
        );

        MethodReference.Handle castFallback = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "reflectViaCastFallback",
                "(Ljava/lang/String;)V"
        );
        BuildFactSnapshot.MethodConstraintFacts castFacts = snapshot.constraints().methodConstraints(castFallback);
        BuildFactSnapshot.MethodInvokeHint castHint = castFacts.reflectionHints().reflectionInvokeHints()
                .values()
                .stream()
                .filter(hint -> hint != null
                        && hint.targets().stream().anyMatch(target ->
                        "me/n1ar4/cb/ReflectionTarget".equals(target.getClassReference().getName())
                                && "target".equals(target.getName())
                                && "()V".equals(target.getDesc())))
                .findFirst()
                .orElse(null);
        assertNotNull(castHint);
        assertEquals(BuildFactSnapshot.ReflectionHintTier.CAST, castHint.tier());
        assertFalse(castHint.imprecise());
        assertTrue(castHint.reason().contains("cast"));

        MethodReference.Handle imprecise = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "reflectViaImpreciseThreshold",
                "(Ljava/lang/String;)V"
        );
        BuildFactSnapshot.MethodConstraintFacts impreciseFacts = snapshot.constraints().methodConstraints(imprecise);
        assertTrue(impreciseFacts.reflectionHints().reflectionInvokeHints().isEmpty());
        BuildFactSnapshot.ReflectionHintDiagnostic diagnostic = impreciseFacts.reflectionHints().diagnostics()
                .stream()
                .filter(item -> item != null && item.thresholdExceeded())
                .findFirst()
                .orElse(null);
        assertNotNull(diagnostic);
        assertEquals(BuildFactSnapshot.ReflectionHintTier.CAST, diagnostic.tier());
        assertTrue(diagnostic.reason().contains("cast"));
        assertEquals(5, diagnostic.candidateCount());

        MethodReference.Handle arrayCopyAlias = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "reflectViaStringArrayCopyAlias",
                "()V"
        );
        BuildFactSnapshot.MethodConstraintFacts arrayFacts = snapshot.constraints().methodConstraints(arrayCopyAlias);
        assertFalse(arrayFacts.arrayCopyEdges().isEmpty());
        assertTrue(arrayFacts.arrayElementFactsByVar().values().stream()
                .flatMap(entry -> entry.values().stream())
                .flatMap(fact -> fact.stringValues().stream())
                .anyMatch("me.n1ar4.cb.ReflectionTarget"::equals));
        assertTrue(arrayFacts.arrayElementFactsByVar().values().stream()
                .flatMap(entry -> entry.values().stream())
                .flatMap(fact -> fact.stringValues().stream())
                .anyMatch("target"::equals));
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

    private static void assertMethodHandleHint(BuildFactSnapshot snapshot,
                                               String methodName,
                                               String methodDesc,
                                               String targetOwner,
                                               String targetName,
                                               String targetDesc) {
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                methodName,
                methodDesc
        );
        BuildFactSnapshot.MethodConstraintFacts facts = snapshot.constraints().methodConstraints(handle);
        BuildFactSnapshot.MethodInvokeHint hint = facts.reflectionHints().methodHandleInvokeHints()
                .values()
                .stream()
                .filter(item -> item != null
                        && item.targets().stream().anyMatch(target ->
                        targetOwner.equals(target.getClassReference().getName())
                                && targetName.equals(target.getName())
                                && targetDesc.equals(target.getDesc())))
                .findFirst()
                .orElse(null);
        assertNotNull(hint);
        assertTrue(hint.reason().contains("method_handle_find"));
        assertEquals(BuildFactSnapshot.ReflectionHintTier.CONST, hint.tier());
        assertFalse(hint.imprecise());
    }
}
