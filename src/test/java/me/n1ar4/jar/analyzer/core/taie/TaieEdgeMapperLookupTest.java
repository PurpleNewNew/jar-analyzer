/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.core.taie;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TaieEdgeMapperLookupTest {
    @Test
    void duplicateExactSignaturesShouldNotResolveToArbitraryJarScopedHandle() throws Exception {
        MethodReference jarOne = method("dup/Shared", "target", "()V", 1, "app.jar");
        MethodReference jarTwo = method("dup/Shared", "target", "()V", 2, "lib.jar");

        Map<MethodReference.Handle, MethodReference> methodMap = new LinkedHashMap<>();
        methodMap.put(jarOne.getHandle(), jarOne);
        methodMap.put(jarTwo.getHandle(), jarTwo);

        Method buildLookup = TaieEdgeMapper.class.getDeclaredMethod("buildLookup", Map.class);
        buildLookup.setAccessible(true);
        Object lookup = buildLookup.invoke(null, methodMap);

        Method exactAccessor = lookup.getClass().getDeclaredMethod("exact");
        exactAccessor.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, MethodReference.Handle> exact =
                (Map<String, MethodReference.Handle>) exactAccessor.invoke(lookup);

        assertFalse(exact.containsKey("dup/Shared#target#()V"));
    }

    private static MethodReference method(String className,
                                          String methodName,
                                          String methodDesc,
                                          int jarId,
                                          String jarName) {
        return new MethodReference(
                new ClassReference.Handle(className, jarId),
                methodName,
                methodDesc,
                false,
                Set.of(),
                1,
                10,
                jarName,
                jarId
        );
    }
}
