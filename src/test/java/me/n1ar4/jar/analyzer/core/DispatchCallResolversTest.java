package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatchCallResolversTest {
    @Test
    void dispatchResolverShouldExpandCrossJarInterfaceTargets() {
        ClassReference callerClass = classRef("demo/Caller", "java/lang/Object", List.of(), 1, 0, false);
        ClassReference taskInterface = classRef("demo/Task", "java/lang/Object", List.of(), 2, Opcodes.ACC_INTERFACE, true);
        ClassReference fastTask = classRef("demo/FastTask", "java/lang/Object", List.of("demo/Task"), 1, 0, false);
        ClassReference slowTask = classRef("demo/SlowTask", "java/lang/Object", List.of("demo/Task"), 3, 0, false);

        MethodReference callerMethod = methodRef(callerClass, "trigger", "()V", 1, 0, false);
        MethodReference interfaceRun = methodRef(taskInterface, "run", "()V", 2, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, false);
        MethodReference fastRun = methodRef(fastTask, "run", "()V", 1, Opcodes.ACC_PUBLIC, false);
        MethodReference slowRun = methodRef(slowTask, "run", "()V", 3, Opcodes.ACC_PUBLIC, false);

        Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        classMap.put(callerClass.getHandle(), callerClass);
        classMap.put(taskInterface.getHandle(), taskInterface);
        classMap.put(fastTask.getHandle(), fastTask);
        classMap.put(slowTask.getHandle(), slowTask);

        Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
        methodMap.put(callerMethod.getHandle(), callerMethod);
        methodMap.put(interfaceRun.getHandle(), interfaceRun);
        methodMap.put(fastRun.getHandle(), fastRun);
        methodMap.put(slowRun.getHandle(), slowRun);

        InheritanceMap inheritanceMap = InheritanceMap.fromClasses(classMap);
        assertTrue(inheritanceMap.isSubclassOf(fastTask.getHandle(), taskInterface.getHandle()));
        assertTrue(inheritanceMap.isSubclassOf(slowTask.getHandle(), taskInterface.getHandle()));

        MethodReference.Handle callerHandle = callerMethod.getHandle();
        MethodReference.Handle declaredTarget = new MethodReference.Handle(
                taskInterface.getHandle(),
                Opcodes.INVOKEINTERFACE,
                "run",
                "()V"
        );
        Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        methodCalls.put(callerHandle, new HashSet<>(Set.of(declaredTarget)));
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        MethodCallMeta.record(
                methodCallMeta,
                MethodCallKey.of(callerHandle, declaredTarget),
                MethodCallMeta.TYPE_DISPATCH,
                MethodCallMeta.CONF_LOW,
                "declared:interface",
                Opcodes.INVOKEINTERFACE
        );

        int added = DispatchCallResolver.expandVirtualCalls(
                methodCalls,
                methodCallMeta,
                methodMap,
                classMap,
                inheritanceMap,
                Set.of(fastTask.getHandle(), slowTask.getHandle())
        );

        assertEquals(2, added);
        Set<MethodReference.Handle> callees = methodCalls.get(callerHandle);
        assertNotNull(callees);
        assertTrue(callees.contains(fastRun.getHandle()));
        assertTrue(callees.contains(slowRun.getHandle()));

        MethodCallMeta fastMeta = MethodCallMeta.resolve(methodCallMeta, callerHandle, fastRun.getHandle());
        MethodCallMeta slowMeta = MethodCallMeta.resolve(methodCallMeta, callerHandle, slowRun.getHandle());
        assertNotNull(fastMeta);
        assertNotNull(slowMeta);
        assertEquals(MethodCallMeta.TYPE_DISPATCH, fastMeta.getType());
        assertEquals(MethodCallMeta.CONF_MEDIUM, fastMeta.getConfidence());
        assertTrue((fastMeta.getEvidenceBits() & MethodCallMeta.EVIDENCE_DISPATCH) != 0);
    }

    @Test
    void typedDispatchResolverShouldPreferReceiverTypeAndMarkTypedEvidence() {
        ClassReference callerClass = classRef("demo/Caller", "java/lang/Object", List.of(), 1, 0, false);
        ClassReference taskInterface = classRef("demo/Task", "java/lang/Object", List.of(), 1, Opcodes.ACC_INTERFACE, true);
        ClassReference fastTask = classRef("demo/FastTask", "java/lang/Object", List.of("demo/Task"), 1, 0, false);
        ClassReference slowTask = classRef("demo/SlowTask", "java/lang/Object", List.of("demo/Task"), 1, 0, false);

        MethodReference callerMethod = methodRef(callerClass, "typed", "()V", 1, 0, false);
        MethodReference interfaceRun = methodRef(taskInterface, "run", "()V", 1, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, false);
        MethodReference fastRun = methodRef(fastTask, "run", "()V", 1, Opcodes.ACC_PUBLIC, false);
        MethodReference slowRun = methodRef(slowTask, "run", "()V", 1, Opcodes.ACC_PUBLIC, false);

        Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        classMap.put(callerClass.getHandle(), callerClass);
        classMap.put(taskInterface.getHandle(), taskInterface);
        classMap.put(fastTask.getHandle(), fastTask);
        classMap.put(slowTask.getHandle(), slowTask);

        Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
        methodMap.put(callerMethod.getHandle(), callerMethod);
        methodMap.put(interfaceRun.getHandle(), interfaceRun);
        methodMap.put(fastRun.getHandle(), fastRun);
        methodMap.put(slowRun.getHandle(), slowRun);

        CallSiteEntity site = new CallSiteEntity();
        site.setCallerClassName("demo/Caller");
        site.setCallerMethodName("typed");
        site.setCallerMethodDesc("()V");
        site.setCalleeOwner("demo/Task");
        site.setCalleeMethodName("run");
        site.setCalleeMethodDesc("()V");
        site.setOpCode(Opcodes.INVOKEINTERFACE);
        site.setReceiverType("demo/FastTask");
        site.setJarId(1);

        Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        InheritanceMap inheritanceMap = InheritanceMap.fromClasses(classMap);

        int added = TypedDispatchResolver.expandWithTypes(
                methodCalls,
                methodCallMeta,
                methodMap,
                classMap,
                inheritanceMap,
                List.of(site)
        );

        assertEquals(1, added);
        Set<MethodReference.Handle> callees = methodCalls.get(callerMethod.getHandle());
        assertNotNull(callees);
        assertTrue(callees.contains(fastRun.getHandle()));
        assertEquals(1, callees.size());

        MethodCallMeta fastMeta = MethodCallMeta.resolve(methodCallMeta, callerMethod.getHandle(), fastRun.getHandle());
        assertNotNull(fastMeta);
        assertEquals(MethodCallMeta.TYPE_DISPATCH, fastMeta.getType());
        assertEquals(MethodCallMeta.CONF_HIGH, fastMeta.getConfidence());
        assertTrue((fastMeta.getEvidenceBits() & MethodCallMeta.EVIDENCE_TYPED) != 0);
    }

    private static ClassReference classRef(String name,
                                           String superClass,
                                           List<String> interfaces,
                                           int jarId,
                                           int access,
                                           boolean isInterface) {
        return new ClassReference(
                61,
                access,
                name,
                superClass,
                interfaces,
                isInterface,
                List.of(),
                Set.of(),
                "fixture-" + jarId + ".jar",
                jarId
        );
    }

    private static MethodReference methodRef(ClassReference owner,
                                             String name,
                                             String desc,
                                             int jarId,
                                             int access,
                                             boolean isStatic) {
        return new MethodReference(
                owner.getHandle(),
                name,
                desc,
                isStatic,
                Set.of(),
                access,
                1,
                "fixture-" + jarId + ".jar",
                jarId
        );
    }
}
