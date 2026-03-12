package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.taint.SinkKindResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedRuleSystemTest {
    @Test
    void shouldLoadSourcesFromSourceJson() {
        List<SourceModel> sources = ModelRegistry.getSourceModels();
        assertFalse(sources.isEmpty());
        assertTrue(
                sources.stream().anyMatch(s ->
                        "org/apache/dubbo/rpc/service/GenericService".equals(s.getClassName())
                                && "$invoke".equals(s.getMethodName()))
        );

        List<String> sourceAnnotations = ModelRegistry.getSourceAnnotations();
        assertFalse(sourceAnnotations.isEmpty());
        assertTrue(sourceAnnotations.stream().anyMatch("Lorg/springframework/web/bind/annotation/RequestParam;"::equals));
    }

    @Test
    void shouldExposeSinkModelsFromSinkJsonToUnifiedModel() {
        assertFalse(ModelRegistry.getSinkModels().isEmpty());
    }

    @Test
    void shouldResolveSinkKindFromUnifiedRules() {
        try {
            SinkKindResolver.clearOverride();

            MethodReference.Handle sink = new MethodReference.Handle(
                    new ClassReference.Handle("javax/naming/Context"),
                    "lookup",
                    "(Ljava/lang/String;)Ljava/lang/Object;"
            );
            SinkKindResolver.Result resolved = SinkKindResolver.resolve(sink);
            assertEquals("jndi", resolved.getKind());
            assertEquals(SinkKindResolver.Origin.RULES, resolved.getOrigin());
        } finally {
            SinkKindResolver.clearOverride();
        }
    }

    @Test
    void shouldPreferThreadLocalOverrideBeforeUnifiedRules() {
        try {
            SinkKindResolver.setOverride("sql");
            MethodReference.Handle sink = new MethodReference.Handle(
                    new ClassReference.Handle("javax/naming/Context"),
                    "lookup",
                    "(Ljava/lang/String;)Ljava/lang/Object;"
            );

            SinkKindResolver.Result resolved = SinkKindResolver.resolve(sink);

            assertEquals("sql", resolved.getKind());
            assertEquals(SinkKindResolver.Origin.OVERRIDE, resolved.getOrigin());
        } finally {
            SinkKindResolver.clearOverride();
        }
    }
}
