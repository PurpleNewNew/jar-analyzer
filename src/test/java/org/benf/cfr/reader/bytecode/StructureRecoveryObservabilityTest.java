package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredLocalVariableRecovery;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureRecoveryObservabilityTest {
    @Test
    void shouldDescribeRecoveryPipelinePassesByPhase() {
        Options options = new OptionsImpl(Map.of());
        ModernFeatureStrategy modernFeatures = ModernFeatureStrategy.from(options, ClassFileVersion.JAVA_17);
        StructureRecoveryPipeline pipeline = new StructureRecoveryPipeline(new PatternSemanticsRewriter(modernFeatures));

        List<StructuredPassEntry> entries = pipeline.describePasses();

        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(this::isInitialCleanupCatchTidier));
        assertTrue(entries.stream()
                .filter(entry -> "output-polish".equals(entry.getStage()))
                .allMatch(entry -> !entry.getDescriptor().allowsStructuralChange()));
        assertTrue(entries.stream().anyMatch(this::isOutputPolishPrimitiveDeconversion));
    }

    @Test
    void shouldDescribeSemanticOutputAndLocalRecoveryPasses() {
        assertTrue(StructuredSemanticTransforms.describePasses().stream()
                .anyMatch(entry -> "rewrite-lambdas".equals(entry.getDescriptor().getName())
                        && "modern-semantics".equals(entry.getStage())));
        assertTrue(StructuredOutputTransforms.describePasses().stream()
                .anyMatch(entry -> "apply-checker".equals(entry.getDescriptor().getName())
                        && "output-polish.validation-and-metadata".equals(entry.getStage())));
        assertTrue(StructuredLocalVariableRecovery.describePasses().stream()
                .anyMatch(entry -> "restore-creators-before-first-use".equals(entry.getDescriptor().getName())
                        && "output-polish.recovery-polish".equals(entry.getStage())));
    }

    @Test
    void shouldCaptureStructureRecoveryTraceForAnalysedMethod(@TempDir Path tempDir) throws Exception {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "RecoveryTraceFixture",
                "public class RecoveryTraceFixture {\n" +
                        "    int demo(boolean flag) {\n" +
                        "        int base = flag ? 1 : 2;\n" +
                        "        if (flag) {\n" +
                        "            return base;\n" +
                        "        }\n" +
                        "        return base + 1;\n" +
                        "    }\n" +
                        "}\n"
        );

        Options options = new OptionsImpl(Map.of("showversion", "false", "silent", "true"));
        ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
        classFileSource.informAnalysisRelativePathDetail(classFile.toString(), classFile.toString());
        DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
        ClassFile loadedClass = dcCommonState.loadClassFileAtPath(classFile.toString());
        Method method = loadedClass.getMethodByName("demo").get(0);

        AnalysisResult analysisResult = method.getAnalysisResult();
        StructureRecoveryTrace trace = analysisResult.getStructureRecoveryTrace();

        assertNotNull(trace);
        assertFalse(trace.getPhases().isEmpty());
        assertTrue(trace.getPhases().stream().anyMatch(phase -> "initial-cleanup".equals(phase.getPhase()) && !phase.isSkipped()));
        assertTrue(trace.getPhases().stream()
                .flatMap(phase -> phase.getRounds().stream())
                .flatMap(round -> round.getPasses().stream())
                .anyMatch(pass -> pass.getPass().getDescriptor().getName().equals("convert-unstructured-if")));
        assertTrue(trace.getPhases().stream()
                .flatMap(phase -> phase.getRounds().stream())
                .flatMap(round -> round.getPasses().stream())
                .allMatch(pass -> pass.getBefore() != null && pass.getAfter() != null));
        assertTrue(trace.getPhases().stream()
                .flatMap(phase -> phase.getInvariants().stream())
                .allMatch(StructureRecoveryTrace.InvariantTrace::isPassed));
        assertTrue(trace.getPhases().stream()
                .filter(phase -> "control-flow-recovery".equals(phase.getPhase()))
                .flatMap(phase -> phase.getRounds().stream())
                .flatMap(round -> round.getInvariants().stream())
                .filter(invariant -> "unstructured-non-increasing".equals(invariant.getName()))
                .allMatch(StructureRecoveryTrace.InvariantTrace::isPassed));
        assertTrue(trace.getPhases().stream()
                .filter(phase -> "output-polish".equals(phase.getPhase()))
                .flatMap(phase -> phase.getInvariants().stream())
                .anyMatch(invariant -> "no-structural-delta".equals(invariant.getName()) && invariant.isPassed()));
    }

    private boolean isInitialCleanupCatchTidier(StructuredPassEntry entry) {
        return "structure-recovery".equals(entry.getCategory())
                && "initial-cleanup".equals(entry.getStage())
                && "tidy-empty-catch".equals(entry.getDescriptor().getName())
                && "any-structure-state".equals(entry.getInputRequirement());
    }

    private boolean isOutputPolishPrimitiveDeconversion(StructuredPassEntry entry) {
        return "structure-recovery".equals(entry.getCategory())
                && "output-polish".equals(entry.getStage())
                && "remove-primitive-deconversion".equals(entry.getDescriptor().getName())
                && "fully-structured".equals(entry.getInputRequirement());
    }
}
