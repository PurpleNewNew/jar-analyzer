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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionConstraintFactModeTest {
    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldCollectReflectionAndMethodHandleHintsIntoConstraintFacts() {
        BuildFactSnapshot snapshot = buildCallbackSnapshot();

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
                null,
                Map.of(),
                symbolResult.getLocalVars(),
                workspace
        );
    }
}
