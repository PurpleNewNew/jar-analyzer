package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.util.collections.ListFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaGenericRefTypeInstanceAssignabilityTest {
    @Test
    void shouldHandleSameBaseWildcardAssignability() {
        JavaGenericPlaceholderTypeInstance t = new JavaGenericPlaceholderTypeInstance("T", null);
        JavaTypeInstance exact = new JavaGenericRefTypeInstance(TypeConstants.CLASS, ListFactory.newImmutableList(t));
        JavaTypeInstance upper = new JavaGenericRefTypeInstance(
                TypeConstants.CLASS,
                ListFactory.newImmutableList(new JavaWildcardTypeInstance(WildcardType.EXTENDS, t)));
        JavaTypeInstance lower = new JavaGenericRefTypeInstance(
                TypeConstants.CLASS,
                ListFactory.newImmutableList(new JavaWildcardTypeInstance(WildcardType.SUPER, t)));

        assertTrue(exact.implicitlyCastsTo(upper, null));
        assertTrue(exact.implicitlyCastsTo(lower, null));
        assertFalse(lower.implicitlyCastsTo(exact, null));
        assertFalse(upper.implicitlyCastsTo(exact, null));
    }
}
