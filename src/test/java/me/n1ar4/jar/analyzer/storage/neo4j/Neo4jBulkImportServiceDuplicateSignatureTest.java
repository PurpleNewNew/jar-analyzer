/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Neo4jBulkImportServiceDuplicateSignatureTest {
    @Test
    void ambiguousLooseFallbackShouldNotBindEdgeToArbitraryJarNode() throws Exception {
        Class<?> looseKeyClass = Class.forName(
                "me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService$MethodLooseKey");
        var ctor = looseKeyClass.getDeclaredConstructor(String.class, String.class, String.class);
        ctor.setAccessible(true);
        Object looseKey = ctor.newInstance("dup/Shared", "target", "()V");

        @SuppressWarnings("unchecked")
        Map<Object, Long> methodNodeByLooseKey = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Set<Object> ambiguousLooseKeys = new java.util.HashSet<>();
        ambiguousLooseKeys.add(looseKey);

        Method resolve = Neo4jBulkImportService.class.getDeclaredMethod(
                "resolveMethodNode",
                Map.class,
                Map.class,
                Set.class,
                String.class,
                String.class,
                String.class,
                int.class
        );
        resolve.setAccessible(true);

        Long resolved = (Long) resolve.invoke(
                null,
                Map.of(),
                methodNodeByLooseKey,
                ambiguousLooseKeys,
                "dup/Shared",
                "target",
                "()V",
                -1
        );

        assertNull(resolved);
    }

    @Test
    void exactSyntheticNodeShouldStillResolveWhenLooseKeyIsAmbiguous() throws Exception {
        MethodReference synthetic = new MethodReference(
                new ClassReference.Handle("dup/Shared", -1),
                "target",
                "()V",
                false,
                Set.of(),
                1,
                10,
                "callgraph-synth",
                -1
        );

        Class<?> methodKeyClass = Class.forName(
                "me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService$MethodKey");
        var methodKeyCtor = methodKeyClass.getDeclaredConstructor(String.class, String.class, String.class, int.class);
        methodKeyCtor.setAccessible(true);
        Object exactKey = methodKeyCtor.newInstance("dup/Shared", "target", "()V", -1);

        Class<?> looseKeyClass = Class.forName(
                "me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService$MethodLooseKey");
        var looseCtor = looseKeyClass.getDeclaredConstructor(String.class, String.class, String.class);
        looseCtor.setAccessible(true);
        Object looseKey = looseCtor.newInstance("dup/Shared", "target", "()V");

        @SuppressWarnings("unchecked")
        Map<Object, Long> methodNodeByKey = new LinkedHashMap<>();
        methodNodeByKey.put(exactKey, 77L);
        @SuppressWarnings("unchecked")
        Set<Object> ambiguousLooseKeys = new java.util.HashSet<>();
        ambiguousLooseKeys.add(looseKey);

        Method resolve = Neo4jBulkImportService.class.getDeclaredMethod(
                "resolveMethodNode",
                Map.class,
                Map.class,
                Set.class,
                MethodReference.Handle.class,
                int.class
        );
        resolve.setAccessible(true);

        Long resolved = (Long) resolve.invoke(
                null,
                methodNodeByKey,
                Map.of(),
                ambiguousLooseKeys,
                synthetic.getHandle(),
                -1
        );

        assertEquals(77L, resolved);
    }
}
