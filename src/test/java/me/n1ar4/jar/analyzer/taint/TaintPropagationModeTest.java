package me.n1ar4.jar.analyzer.taint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaintPropagationModeTest {
    @Test
    void parseShouldHandleExplicitValues() {
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.parse(null));
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.parse(""));
        assertEquals(TaintPropagationMode.STRICT, TaintPropagationMode.parse("strict"));
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.parse("balanced"));
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.parse("default"));
    }

    @Test
    void resolveShouldUseExplicitPropagationMode() {
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationConfig.resolve().getPropagationMode());
        assertEquals(TaintPropagationMode.STRICT, TaintPropagationConfig.resolve(TaintPropagationMode.STRICT).getPropagationMode());
    }

    @Test
    void invalidParseShouldBeRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TaintPropagationMode.parse("unsupported")
        );
        assertEquals(
                "invalid taint propagation mode=unsupported, supported: strict|balanced",
                ex.getMessage()
        );
    }
}
