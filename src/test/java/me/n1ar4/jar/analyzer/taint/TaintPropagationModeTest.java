package me.n1ar4.jar.analyzer.taint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaintPropagationModeTest {
    @AfterEach
    void cleanup() {
        System.clearProperty(TaintPropagationMode.PROP_KEY);
    }

    @Test
    void currentShouldResolvePropertyDynamically() {
        System.clearProperty(TaintPropagationMode.PROP_KEY);
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.current());

        System.setProperty(TaintPropagationMode.PROP_KEY, "strict");
        assertEquals(TaintPropagationMode.STRICT, TaintPropagationMode.current());

        System.setProperty(TaintPropagationMode.PROP_KEY, "balanced");
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationMode.current());
        assertEquals(TaintPropagationMode.BALANCED, TaintPropagationConfig.resolve().getPropagationMode());
    }

    @Test
    void invalidPropertyShouldBeRejected() {
        System.setProperty(TaintPropagationMode.PROP_KEY, "unsupported");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                TaintPropagationMode::current
        );
        assertEquals(
                "invalid jar.analyzer.taint.propagation=unsupported, supported: strict|balanced",
                ex.getMessage()
        );
    }
}
