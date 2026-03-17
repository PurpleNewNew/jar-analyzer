package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class ModernFeatureStrategy {
    enum FeatureLevel {
        DISABLED,
        PREVIEW,
        RELEASED;

        boolean enabled() {
            return this != DISABLED;
        }

        boolean preview() {
            return this == PREVIEW;
        }
    }

    private final ClassFileVersion classFileVersion;
    private final boolean previewFeaturesEnabled;
    private final boolean sealedExplicit;
    private final boolean switchExpressionExplicit;
    private final FeatureLevel sealed;
    private final FeatureLevel switchExpression;
    private final FeatureLevel recordTypes;
    private final FeatureLevel instanceofPattern;

    private ModernFeatureStrategy(ClassFileVersion classFileVersion,
                                  boolean previewFeaturesEnabled,
                                  boolean sealedExplicit,
                                  boolean switchExpressionExplicit,
                                  FeatureLevel sealed,
                                  FeatureLevel switchExpression,
                                  FeatureLevel recordTypes,
                                  FeatureLevel instanceofPattern) {
        this.classFileVersion = classFileVersion;
        this.previewFeaturesEnabled = previewFeaturesEnabled;
        this.sealedExplicit = sealedExplicit;
        this.switchExpressionExplicit = switchExpressionExplicit;
        this.sealed = sealed;
        this.switchExpression = switchExpression;
        this.recordTypes = recordTypes;
        this.instanceofPattern = instanceofPattern;
    }

    static ModernFeatureStrategy from(Options options, ClassFileVersion classFileVersion) {
        boolean previewFeaturesEnabled = options.getOption(OptionsImpl.PREVIEW_FEATURES);
        return new ModernFeatureStrategy(
                classFileVersion,
                previewFeaturesEnabled,
                options.optionIsSet(OptionsImpl.SEALED),
                options.optionIsSet(OptionsImpl.SWITCH_EXPRESSION),
                resolveFeatureLevel(options.getOption(OptionsImpl.SEALED, classFileVersion), OptionsImpl.SEALED_PREVIEW.isPreviewIn(classFileVersion)),
                resolveFeatureLevel(options.getOption(OptionsImpl.SWITCH_EXPRESSION, classFileVersion), OptionsImpl.SWITCH_EXPRESSION_PREVIEW.isPreviewIn(classFileVersion)),
                resolveFeatureLevel(options.getOption(OptionsImpl.RECORD_TYPES, classFileVersion), isRecordTypesPreview(classFileVersion)),
                resolveFeatureLevel(options.getOption(OptionsImpl.INSTANCEOF_PATTERN, classFileVersion), isInstanceOfPatternPreview(classFileVersion))
        );
    }

    private static boolean isRecordTypesPreview(ClassFileVersion classFileVersion) {
        return matchesPreviewVersion(classFileVersion, ClassFileVersion.JAVA_14_Experimental, ClassFileVersion.JAVA_15_Experimental);
    }

    private static boolean isInstanceOfPatternPreview(ClassFileVersion classFileVersion) {
        return matchesPreviewVersion(classFileVersion, ClassFileVersion.JAVA_14_Experimental, ClassFileVersion.JAVA_15_Experimental);
    }

    private static boolean matchesPreviewVersion(ClassFileVersion classFileVersion, ClassFileVersion... previewVersions) {
        if (!classFileVersion.isExperimental()) {
            return false;
        }
        for (ClassFileVersion previewVersion : previewVersions) {
            if (previewVersion.sameMajor(classFileVersion)) {
                return true;
            }
        }
        return false;
    }

    private static FeatureLevel resolveFeatureLevel(boolean enabled, boolean preview) {
        if (!enabled) {
            return FeatureLevel.DISABLED;
        }
        return preview ? FeatureLevel.PREVIEW : FeatureLevel.RELEASED;
    }

    boolean supportsSealedTypes() {
        return sealed.enabled();
    }

    boolean supportsSwitchExpressions() {
        return switchExpression.enabled();
    }

    boolean supportsInstanceOfPatterns() {
        return instanceofPattern.enabled();
    }

    boolean supportsRecordTypes() {
        return recordTypes.enabled();
    }

    boolean supportsPatternSwitches() {
        if (classFileVersion.equalOrLater(ClassFileVersion.JAVA_21)) {
            return true;
        }
        return previewFeaturesEnabled && classFileVersion.isExperimental();
    }

    boolean supportsBindingPatterns() {
        return supportsInstanceOfPatterns() || supportsPatternSwitches();
    }

    boolean supportsRecordPatterns() {
        return supportsPatternSwitches() && supportsRecordTypes();
    }

    boolean supportsGuardedPatterns() {
        return supportsPatternSwitches();
    }

    boolean shouldEmitPreviewSealedComment() {
        return sealed.preview() && !sealedExplicit;
    }

    boolean shouldEmitPreviewSwitchExpressionComment() {
        return switchExpression.preview() && !switchExpressionExplicit;
    }

    boolean prefersPatternOutput() {
        return supportsBindingPatterns() || supportsRecordPatterns() || supportsGuardedPatterns();
    }
}
