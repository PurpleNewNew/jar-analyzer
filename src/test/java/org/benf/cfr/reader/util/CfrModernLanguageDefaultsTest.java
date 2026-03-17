package org.benf.cfr.reader.util;

import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CfrModernLanguageDefaultsTest {
    @Test
    void shouldParseModernJavaVersions() {
        assertEquals(ClassFileVersion.JAVA_21, ClassFileVersion.parse("j21"));
        assertEquals(ClassFileVersion.JAVA_21_Experimental, ClassFileVersion.parse("j21pre"));
        assertEquals(ClassFileVersion.JAVA_20, ClassFileVersion.parse("j20"));
        assertEquals(ClassFileVersion.JAVA_19, ClassFileVersion.parse("j19"));
        assertEquals("66.0 (Java 22)", ClassFileVersion.parse("j22").toString());
        assertEquals("66.65535 (Java 22) preview", ClassFileVersion.parse("j22pre").toString());
        assertEquals("65.0 (Java 21)", ClassFileVersion.JAVA_21.toString());
    }

    @Test
    void shouldEnableReleasedModernLanguageFeaturesByDefault() {
        OptionsImpl options = new OptionsImpl(Map.of());
        ClassFileVersion java22 = ClassFileVersion.parse("j22");

        assertTrue(options.getOption(OptionsImpl.SEALED, ClassFileVersion.JAVA_17));
        assertTrue(options.getOption(OptionsImpl.SWITCH_EXPRESSION, ClassFileVersion.JAVA_14));
        assertTrue(options.getOption(OptionsImpl.RECORD_TYPES, ClassFileVersion.JAVA_16));
        assertTrue(options.getOption(OptionsImpl.INSTANCEOF_PATTERN, ClassFileVersion.JAVA_16));
        assertTrue(options.getOption(OptionsImpl.SEALED, ClassFileVersion.JAVA_21));
        assertTrue(options.getOption(OptionsImpl.RECORD_TYPES, java22));
        assertTrue(options.getOption(OptionsImpl.INSTANCEOF_PATTERN, java22));
    }

    @Test
    void shouldRespectPreviewGateForPreviewClassFiles() {
        OptionsImpl defaults = new OptionsImpl(Map.of());
        assertTrue(defaults.getOption(OptionsImpl.SEALED, ClassFileVersion.JAVA_15_Experimental));
        assertTrue(defaults.getOption(OptionsImpl.SWITCH_EXPRESSION, ClassFileVersion.JAVA_13_Experimental));
        assertTrue(defaults.getOption(OptionsImpl.RECORD_TYPES, ClassFileVersion.JAVA_15_Experimental));

        OptionsImpl previewDisabled = new OptionsImpl(Map.of(
                OptionsImpl.PREVIEW_FEATURES.getName(), "false"
        ));
        assertFalse(previewDisabled.getOption(OptionsImpl.SEALED, ClassFileVersion.JAVA_15_Experimental));
        assertFalse(previewDisabled.getOption(OptionsImpl.SWITCH_EXPRESSION, ClassFileVersion.JAVA_13_Experimental));
        assertFalse(previewDisabled.getOption(OptionsImpl.RECORD_TYPES, ClassFileVersion.JAVA_15_Experimental));
    }

    @Test
    void shouldDefaultBatchThreadsToAuto() {
        OptionsImpl options = new OptionsImpl(Map.of());
        assertEquals(0, options.getOption(OptionsImpl.BATCH_THREADS));
        OptionsImpl custom = new OptionsImpl(Map.of(
                OptionsImpl.BATCH_THREADS.getName(), "6"
        ));
        assertEquals(6, custom.getOption(OptionsImpl.BATCH_THREADS));
    }
}
