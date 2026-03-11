package me.n1ar4.jar.analyzer.qa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NoTaieMigrationRegressionPack {
    private static final String MVN_PREFIX = "mvn -q -Dskip.npm=true -Dskip.installnodenpm=true";
    private static final Map<String, IssuePack> PACKS = buildPacks();

    private NoTaieMigrationRegressionPack() {
    }

    public static Map<String, IssuePack> all() {
        return PACKS;
    }

    public static IssuePack require(String issueKey) {
        String normalized = normalizeIssue(issueKey);
        IssuePack pack = PACKS.get(normalized);
        if (pack == null) {
            throw new IllegalArgumentException("unknown no-taie migration issue: " + issueKey);
        }
        return pack;
    }

    private static Map<String, IssuePack> buildPacks() {
        LinkedHashMap<String, IssuePack> out = new LinkedHashMap<>();
        out.put("JA-NT-103", pack(
                "JA-NT-103",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisProjectModeRegressionTest"
        ));
        out.put("JA-NT-104", pack(
                "JA-NT-104",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisAppRegressionTest"
        ));
        out.put("JA-NT-105", pack(
                "JA-NT-105",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealGadgetFamilyRegressionTest",
                "me.n1ar4.jar.analyzer.qa.YsoserialPayloadRegressionTest",
                "me.n1ar4.jar.analyzer.qa.GadgetRouteCoverageBenchTest"
        ));
        out.put("JA-NT-106", pack(
                "JA-NT-106",
                "me.n1ar4.jar.analyzer.core.BuildFactAssemblerTest",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisProjectModeRegressionTest"
        ));
        out.put("JA-NT-107", pack(
                "JA-NT-107",
                "me.n1ar4.jar.analyzer.core.CallGraphPlanTest",
                "me.n1ar4.jar.analyzer.core.CoreRunnerCallGraphProfileTest",
                "me.n1ar4.jar.analyzer.core.taie.TaieBuildIntegrationTest"
        ));
        out.put("JA-NT-108", pack(
                "JA-NT-108",
                "me.n1ar4.jar.analyzer.qa.NoTaieMigrationRegressionPackTest",
                "me.n1ar4.jar.analyzer.qa.NoTaieQualityGateTest"
        ));
        return Map.copyOf(out);
    }

    private static IssuePack pack(String issueKey, String... testClasses) {
        return new IssuePack(issueKey, List.of(testClasses));
    }

    private static String normalizeIssue(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            return "";
        }
        return issueKey.trim().toUpperCase(Locale.ROOT);
    }

    private static String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "";
        }
        int idx = fqcn.lastIndexOf('.');
        return idx < 0 ? fqcn : fqcn.substring(idx + 1);
    }

    public record IssuePack(String issueKey, List<String> requiredTests) {
        public IssuePack {
            issueKey = issueKey == null ? "" : issueKey.trim();
            requiredTests = requiredTests == null ? List.of() : List.copyOf(requiredTests);
        }

        public List<String> requiredTestSimpleNames() {
            return requiredTests.stream()
                    .map(NoTaieMigrationRegressionPack::simpleName)
                    .toList();
        }

        public String mavenCommand() {
            return MVN_PREFIX + " -Dtest=" + String.join(",", requiredTestSimpleNames()) + " test";
        }
    }
}
