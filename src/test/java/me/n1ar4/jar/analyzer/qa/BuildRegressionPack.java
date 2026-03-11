package me.n1ar4.jar.analyzer.qa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BuildRegressionPack {
    private static final String MVN_PREFIX = "mvn -q -Dskip.npm=true -Dskip.installnodenpm=true";
    private static final Map<String, Pack> PACKS = buildPacks();

    private BuildRegressionPack() {
    }

    public static Map<String, Pack> all() {
        return PACKS;
    }

    public static Pack require(String packKey) {
        String normalized = normalizeKey(packKey);
        Pack pack = PACKS.get(normalized);
        if (pack == null) {
            throw new IllegalArgumentException("unknown build regression pack: " + packKey);
        }
        return pack;
    }

    private static Map<String, Pack> buildPacks() {
        LinkedHashMap<String, Pack> out = new LinkedHashMap<>();
        out.put("framework-project-mode", pack(
                "framework-project-mode",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisProjectModeRegressionTest"
        ));
        out.put("framework-app-mode", pack(
                "framework-app-mode",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisAppRegressionTest"
        ));
        out.put("gadget-route", pack(
                "gadget-route",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealGadgetFamilyRegressionTest",
                "me.n1ar4.jar.analyzer.qa.YsoserialPayloadRegressionTest",
                "me.n1ar4.jar.analyzer.qa.GadgetRouteCoverageBenchTest"
        ));
        out.put("fact-pipeline", pack(
                "fact-pipeline",
                "me.n1ar4.jar.analyzer.core.BuildFactAssemblerTest",
                "me.n1ar4.jar.analyzer.core.CoreRunnerBytecodeMainlineTest",
                "me.n1ar4.jar.analyzer.qa.RealFrameworkRegressionTest",
                "me.n1ar4.jar.analyzer.qa.RealStrutsSpringMyBatisProjectModeRegressionTest"
        ));
        out.put("callgraph-profile", pack(
                "callgraph-profile",
                "me.n1ar4.jar.analyzer.core.CallGraphPlanTest",
                "me.n1ar4.jar.analyzer.core.CoreRunnerCallGraphProfileTest"
        ));
        out.put("quality-gate", pack(
                "quality-gate",
                "me.n1ar4.jar.analyzer.qa.BuildRegressionPackTest",
                "me.n1ar4.jar.analyzer.qa.BuildQualityGateTest"
        ));
        return Map.copyOf(out);
    }

    private static Pack pack(String packKey, String... testClasses) {
        return new Pack(packKey, List.of(testClasses));
    }

    private static String normalizeKey(String packKey) {
        if (packKey == null || packKey.isBlank()) {
            return "";
        }
        return packKey.trim().toLowerCase(Locale.ROOT);
    }

    private static String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "";
        }
        int idx = fqcn.lastIndexOf('.');
        return idx < 0 ? fqcn : fqcn.substring(idx + 1);
    }

    public record Pack(String packKey, List<String> requiredTests) {
        public Pack {
            packKey = packKey == null ? "" : packKey.trim();
            requiredTests = requiredTests == null ? List.of() : List.copyOf(requiredTests);
        }

        public List<String> requiredTestSimpleNames() {
            return requiredTests.stream()
                    .map(BuildRegressionPack::simpleName)
                    .toList();
        }

        public String mavenCommand() {
            return MVN_PREFIX + " -Dtest=" + String.join(",", requiredTestSimpleNames()) + " test";
        }
    }
}
