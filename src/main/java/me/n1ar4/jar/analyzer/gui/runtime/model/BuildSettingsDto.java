package me.n1ar4.jar.analyzer.gui.runtime.model;

import me.n1ar4.jar.analyzer.core.CallGraphPlan;
import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver;
import me.n1ar4.jar.analyzer.taint.TaintPropagationMode;

import java.util.Locale;

public record BuildSettingsDto(
        String inputPath,
        String sdkPath,
        boolean resolveNestedJars,
        boolean fixClassPath,
        String jdkModules,
        String callGraphProfile,
        String taintPropagationMode
) {
    public BuildSettingsDto {
        inputPath = normalizePath(inputPath);
        sdkPath = normalizePath(sdkPath);
        jdkModules = normalizeJdkModules(jdkModules);
        callGraphProfile = normalizeCallGraphProfile(callGraphProfile);
        taintPropagationMode = normalizeTaintPropagationMode(taintPropagationMode);
    }

    public BuildSettingsDto(String inputPath,
                            String sdkPath,
                            boolean resolveNestedJars,
                            boolean fixClassPath) {
        this(
                inputPath,
                sdkPath,
                resolveNestedJars,
                fixClassPath,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public BuildSettingsDto(String inputPath,
                            String sdkPath,
                            boolean resolveNestedJars,
                            boolean fixClassPath,
                            String jdkModules) {
        this(
                inputPath,
                sdkPath,
                resolveNestedJars,
                fixClassPath,
                jdkModules,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public BuildSettingsDto(String inputPath,
                            String sdkPath,
                            boolean resolveNestedJars,
                            boolean fixClassPath,
                            String jdkModules,
                            String callGraphProfile) {
        this(
                inputPath,
                sdkPath,
                resolveNestedJars,
                fixClassPath,
                jdkModules,
                callGraphProfile,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public String activeInputPath() {
        return inputPath;
    }

    private static String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeJdkModules(String value) {
        return JdkArchiveResolver.normalizePolicy(value);
    }

    private static String normalizeCallGraphProfile(String value) {
        return CallGraphPlan.normalizeProfile(value);
    }

    private static String normalizeTaintPropagationMode(String value) {
        return TaintPropagationMode.parse(value).name().toLowerCase(Locale.ROOT);
    }
}
