package me.n1ar4.jar.analyzer.rules;

import java.nio.file.Path;

public final class RuleRegistryTestSupport {
    private RuleRegistryTestSupport() {
    }

    public static void useRuleFiles(Path modelPath, Path sourcePath, Path sinkPath) {
        ModelRegistry.setRulePathsForTesting(stringPath(modelPath), stringPath(sourcePath));
        SinkRuleRegistry.setSinkPathForTesting(stringPath(sinkPath));
        SinkRuleRegistry.reload();
        ModelRegistry.reload();
    }

    public static void useSinkFile(Path sinkPath) {
        SinkRuleRegistry.setSinkPathForTesting(stringPath(sinkPath));
        SinkRuleRegistry.reload();
    }

    public static void clearRuleFiles() {
        ModelRegistry.clearRulePathsForTesting();
        SinkRuleRegistry.clearSinkPathForTesting();
        SinkRuleRegistry.reload();
        ModelRegistry.reload();
    }

    private static String stringPath(Path path) {
        return path == null ? "" : path.toString();
    }
}
