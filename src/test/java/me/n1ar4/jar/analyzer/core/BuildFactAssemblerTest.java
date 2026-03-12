package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BuildFactAssembler;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.core.facts.CallSiteEntity;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildFactAssemblerTest {
    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldAssembleSnapshotFromBuildContext() {
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
        context.discoveredClasses.add(owner);
        context.classMap.put(owner.getHandle(), owner);
        context.discoveredMethods.add(caller);
        context.discoveredMethods.add(callee);
        context.methodMap.put(caller.getHandle(), caller);
        context.methodMap.put(callee.getHandle(), callee);
        context.callSites.add(callSite);
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

        BuildFactSnapshot snapshot = BuildFactAssembler.from(context, null);

        assertEquals(owner, snapshot.types().classesByHandle().get(owner.getHandle()));
        assertEquals(2, snapshot.methods().methodsByHandle().size());
        assertEquals(1, snapshot.symbols().callSites().size());
        assertEquals(1, snapshot.symbols().callSitesByCaller().get(caller.getHandle()).size());
        assertEquals(3, snapshot.semantics().explicitSourceMethodFlags().get(caller.getHandle()));
        assertEquals(7, snapshot.semantics().methodSemanticFlags().get(caller.getHandle()));
        assertTrue(snapshot.bytecode().workspace().parsedClasses().isEmpty());
    }

    @Test
    void shouldNotPreloadSourceFramesForMethodsWithoutFrameDependentConstraints() throws Exception {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(java.util.Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        BuildContext context = new BuildContext();
        DiscoveryRunner.start(
                workspace,
                context.discoveredClasses,
                context.discoveredMethods,
                context.classMap,
                context.methodMap,
                context.stringAnnoMap
        );

        BuildBytecodeWorkspace.ParsedMethod candidate = workspace.parsedClasses().stream()
                .filter(parsedClass -> parsedClass != null && parsedClass.methods() != null)
                .flatMap(parsedClass -> parsedClass.methods().stream())
                .filter(method -> method != null
                        && method.methodNode() != null
                        && method.methodNode().instructions != null
                        && method.methodNode().instructions.size() > 0
                        && !requiresConstraintFrames(method.methodNode())
                        && !containsConstructorInvoke(method.methodNode())
                        && !containsReflectionInvoke(method.methodNode()))
                .findFirst()
                .orElse(null);
        assertNotNull(candidate);
        assertFalse(sourceFramesLoaded(candidate));

        BuildFactAssembler.from(context, workspace);

        assertFalse(sourceFramesLoaded(candidate));
    }

    @Test
    void shouldMaterializeConstraintFactsLazily() throws Exception {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(java.util.Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        BuildContext context = new BuildContext();
        DiscoveryRunner.start(
                workspace,
                context.discoveredClasses,
                context.discoveredMethods,
                context.classMap,
                context.methodMap,
                context.stringAnnoMap
        );
        context.callSites.addAll(BytecodeSymbolRunner.collectCallSites(workspace));

        BuildFactSnapshot snapshot = BuildFactAssembler.from(context, workspace);
        MethodReference.Handle reflectHelper = new MethodReference.Handle(
                new ClassReference.Handle("me/n1ar4/cb/CallbackEntry", 1),
                "reflectViaHelperFlow",
                "()V"
        );

        assertEquals(0, materializedConstraintCount(snapshot));
        assertFalse(snapshot.constraints().methodReflectionHints(reflectHelper).isEmpty());
        assertTrue(snapshot.constraints().hasReflectionPrecisionSignal(reflectHelper));
        assertEquals(0, materializedConstraintCount(snapshot));

        BuildFactSnapshot.MethodConstraintFacts facts = snapshot.constraints().methodConstraints(reflectHelper);
        assertFalse(facts.reflectionHints().isEmpty());
        assertEquals(1, materializedConstraintCount(snapshot));
    }

    private static boolean requiresConstraintFrames(org.objectweb.asm.tree.MethodNode methodNode) {
        if (methodNode == null || methodNode.instructions == null) {
            return false;
        }
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn == null) {
                continue;
            }
            if (insn.getOpcode() == Opcodes.AASTORE) {
                return true;
            }
            if (insn instanceof MethodInsnNode methodInsn
                    && "java/lang/System".equals(methodInsn.owner)
                    && "arraycopy".equals(methodInsn.name)
                    && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(methodInsn.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsReflectionInvoke(org.objectweb.asm.tree.MethodNode methodNode) {
        if (methodNode == null || methodNode.instructions == null) {
            return false;
        }
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            String owner = methodInsn.owner;
            if ("java/lang/Class".equals(owner)
                    || "java/lang/reflect/Method".equals(owner)
                    || "java/lang/reflect/Constructor".equals(owner)
                    || "java/lang/invoke/MethodHandle".equals(owner)
                    || "java/lang/invoke/MethodHandles$Lookup".equals(owner)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsConstructorInvoke(org.objectweb.asm.tree.MethodNode methodNode) {
        if (methodNode == null || methodNode.instructions == null) {
            return false;
        }
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(methodInsn.name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sourceFramesLoaded(BuildBytecodeWorkspace.ParsedMethod method) throws Exception {
        Field loaded = BuildBytecodeWorkspace.ParsedMethod.class.getDeclaredField("sourceFramesLoaded");
        loaded.setAccessible(true);
        return loaded.getBoolean(method);
    }

    @SuppressWarnings("unchecked")
    private static int materializedConstraintCount(BuildFactSnapshot snapshot) throws Exception {
        Field cacheField = BuildFactSnapshot.ConstraintFacts.class.getDeclaredField("methodConstraintCache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<MethodReference.Handle, BuildFactSnapshot.MethodConstraintFacts> cache =
                (ConcurrentHashMap<MethodReference.Handle, BuildFactSnapshot.MethodConstraintFacts>) cacheField.get(snapshot.constraints());
        return cache.size();
    }
}
