package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernFeatureStrategyTest {
    @Test
    void shouldTreatReleasedModernFeaturesAsStableOutput() {
        ModernFeatureStrategy strategy = ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_21);

        assertTrue(strategy.supportsSealedTypes());
        assertTrue(strategy.supportsSwitchExpressions());
        assertTrue(strategy.supportsPatternSwitches());
        assertTrue(strategy.supportsBindingPatterns());
        assertTrue(strategy.supportsInstanceOfPatterns());
        assertTrue(strategy.supportsRecordTypes());
        assertTrue(strategy.supportsRecordPatterns());
        assertTrue(strategy.supportsGuardedPatterns());
        assertTrue(strategy.prefersPatternOutput());
        assertTrue(strategy.supportsVarOutput());
        assertTrue(strategy.supportsAnonymousObjectVarOutput(true));
        assertTrue(strategy.forbidsStandaloneUnderscoreIdentifier());
        assertFalse(strategy.shouldEmitPreviewSealedComment());
        assertFalse(strategy.shouldEmitPreviewSwitchExpressionComment());
    }

    @Test
    void shouldRespectPreviewGateAndPreviewComments() {
        ModernFeatureStrategy preview = ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_15_Experimental);
        assertTrue(preview.supportsSealedTypes());
        assertTrue(preview.supportsInstanceOfPatterns());
        assertTrue(preview.supportsRecordTypes());
        assertTrue(preview.shouldEmitPreviewSealedComment());

        ModernFeatureStrategy previewDisabled = ModernFeatureStrategy.from(
                new OptionsImpl(Map.of(OptionsImpl.PREVIEW_FEATURES.getName(), "false")),
                ClassFileVersion.JAVA_15_Experimental
        );
        assertFalse(previewDisabled.supportsSealedTypes());
        assertFalse(previewDisabled.supportsInstanceOfPatterns());
        assertFalse(previewDisabled.supportsRecordTypes());
        assertFalse(previewDisabled.supportsPatternSwitches());
        assertTrue(previewDisabled.supportsVarOutput());
        assertFalse(previewDisabled.supportsAnonymousObjectVarOutput(false));

        ModernFeatureStrategy switchPreview = ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_13_Experimental);
        assertTrue(switchPreview.supportsSwitchExpressions());
        assertFalse(switchPreview.supportsPatternSwitches());
        assertTrue(switchPreview.shouldEmitPreviewSwitchExpressionComment());
        assertTrue(switchPreview.forbidsStandaloneUnderscoreIdentifier());
    }

    @Test
    void shouldOnlyEnablePatternSwitchesForSupportedPreviewVersions() {
        assertFalse(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_15_Experimental)
                .supportsPatternSwitches());
        assertFalse(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_16_Experimental)
                .supportsPatternSwitches());

        assertTrue(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_17_Experimental)
                .supportsPatternSwitches());
        assertTrue(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_18_Experimental)
                .supportsPatternSwitches());
        assertTrue(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_19_Experimental)
                .supportsPatternSwitches());
        assertTrue(ModernFeatureStrategy.from(new OptionsImpl(Map.of()), ClassFileVersion.JAVA_20_Experimental)
                .supportsPatternSwitches());

        ModernFeatureStrategy previewDisabled = ModernFeatureStrategy.from(
                new OptionsImpl(Map.of(OptionsImpl.PREVIEW_FEATURES.getName(), "false")),
                ClassFileVersion.JAVA_19_Experimental
        );
        assertFalse(previewDisabled.supportsPatternSwitches());
    }
}
