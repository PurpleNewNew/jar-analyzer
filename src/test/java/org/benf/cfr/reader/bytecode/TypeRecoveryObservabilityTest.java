package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeRecoveryObservabilityTest {
    @Test
    void shouldDescribeTypeRecoveryPasses() {
        List<StructuredPassEntry> passes = TypeRecoveryPasses.describePasses();

        assertFalse(passes.isEmpty());
        assertTrue(passes.stream().anyMatch(entry -> "assignment-rhs-hint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "display-type-static-binder".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "display-type-static-return".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "display-type-member-binder".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "display-type-member-return".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "assignment-target-constraint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "variable-assignment-constraint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "return-target-constraint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "lambda-return-target-hint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().anyMatch(entry -> "ternary-branch-target-hint".equals(entry.getDescriptor().getName())));
        assertTrue(passes.stream().allMatch(entry -> !entry.getDescriptor().allowsStructuralChange()));
    }

    @Test
    void shouldCaptureTypeRecoveryTraceDuringMethodDump(@TempDir Path tempDir) throws Exception {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "TargetTypingConstructorSample",
                "--release", "21");

        ClassFile loadedClass = loadClass(classFile);
        loadedClass.dump(new ToStringDumper());

        Method method = loadedClass.getMethodByName("indexByValue").get(0);
        AnalysisResult analysisResult = method.getAnalysisResult();
        TypeRecoveryTrace trace = analysisResult.getTypeRecoveryTrace();
        MethodDecompileRecord record = analysisResult.getMethodDecompileRecord();

        assertNotNull(trace);
        assertNotNull(record);
        assertFalse(trace.getPasses().isEmpty());
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "variable-assignment-constraint".equals(pass.getPass().getDescriptor().getName())
                        && "LocalVariable".equals(pass.getExpressionKind())
                        && pass.getDetail() != null
                        && pass.getDetail().contains("local=")));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "assignment-target-constraint".equals(pass.getPass().getDescriptor().getName())
                        && "ConstructorInvokationSimple".equals(pass.getExpressionKind())));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "lambda-return-target-hint".equals(pass.getPass().getDescriptor().getName())
                        && "ConstructorInvokationSimple".equals(pass.getExpressionKind())
                        && pass.isChanged()));
        assertTrue(record.getStages().stream()
                .filter(stage -> "type-constraint".equals(stage.getStage()))
                .anyMatch(stage -> stage.getArtifacts().getTypePassNames().contains("variable-assignment-constraint")
                        && stage.getArtifacts().getTypePassNames().contains("assignment-target-constraint")));
        assertTrue(trace.getPasses().stream()
                .allMatch(pass -> pass.getExpectedType() != null && pass.getAfterType() != null));
    }

    @Test
    void shouldCaptureTernaryBranchHints(@TempDir Path tempDir) throws Exception {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "TernaryTypeMergeSample",
                "--release", "21");

        ClassFile loadedClass = loadClass(classFile);
        loadedClass.dump(new ToStringDumper());

        Method method = loadedClass.getMethodByName("chooseCollection").get(0);
        TypeRecoveryTrace trace = method.getAnalysisResult().getTypeRecoveryTrace();

        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "ternary-branch-target-hint".equals(pass.getPass().getDescriptor().getName())
                        && "ConstructorInvokationSimple".equals(pass.getExpressionKind())));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "return-target-constraint".equals(pass.getPass().getDescriptor().getName())
                        && "TernaryExpression".equals(pass.getExpressionKind())
                        && pass.getExpectedType() != null
                        && pass.getExpectedType().contains("java.util.Collection")));
    }

    @Test
    void shouldCaptureReturnTargetConstraintsForLambdaTernary(@TempDir Path tempDir) throws Exception {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "LambdaTernaryTypingSample",
                "--release", "21");

        ClassFile loadedClass = loadClass(classFile);
        loadedClass.dump(new ToStringDumper());

        Method method = loadedClass.getMethodByName("choose").get(0);
        TypeRecoveryTrace trace = method.getAnalysisResult().getTypeRecoveryTrace();

        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "return-target-constraint".equals(pass.getPass().getDescriptor().getName())
                        && "LambdaExpression".equals(pass.getExpressionKind())
                        && pass.getExpectedType() != null
                        && pass.getExpectedType().contains("java.util.function.Supplier")));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "ternary-branch-target-hint".equals(pass.getPass().getDescriptor().getName())
                        && "ConstructorInvokationSimple".equals(pass.getExpressionKind())
                        && pass.getExpectedType() != null
                        && pass.getExpectedType().contains("java.util.List")));
    }

    @Test
    void shouldCaptureDisplayTypeReturnResolutionTrace(@TempDir Path tempDir) throws Exception {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "DisplayTypeReturnSample",
                "--release", "21");

        ClassFile loadedClass = loadClass(classFile);
        loadedClass.dump(new ToStringDumper());

        Method method = loadedClass.getMethodByName("collect").get(0);
        TypeRecoveryTrace trace = method.getAnalysisResult().getTypeRecoveryTrace();

        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "display-type-static-binder".equals(pass.getPass().getDescriptor().getName())
                        && "StaticFunctionInvokation".equals(pass.getExpressionKind())
                        && pass.getDetail() != null
                        && pass.getDetail().contains("argBinder=")
                        && pass.getDetail().contains("selectedSource=")
                        && pass.getDetail().contains("selected=")),
                () -> describeTrace(trace));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "display-type-static-return".equals(pass.getPass().getDescriptor().getName())
                        && "StaticFunctionInvokation".equals(pass.getExpressionKind())
                        && pass.getExpectedType() != null
                        && pass.getExpectedType().contains("java.util.List")
                        && pass.getAfterType() != null
                        && pass.getAfterType().contains("java.util.List")),
                () -> describeTrace(trace));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "display-type-member-binder".equals(pass.getPass().getDescriptor().getName())
                        && "MemberFunctionInvokation".equals(pass.getExpressionKind())
                        && pass.getDetail() != null
                        && pass.getDetail().contains("objectBinder=")
                        && pass.getDetail().contains("expectedBinder=")
                        && pass.getDetail().contains("selectedSource=")
                        && pass.getDetail().contains("selected=")),
                () -> describeTrace(trace));
        assertTrue(trace.getPasses().stream()
                .anyMatch(pass -> "display-type-member-return".equals(pass.getPass().getDescriptor().getName())
                        && "MemberFunctionInvokation".equals(pass.getExpressionKind())
                        && pass.getExpectedType() != null
                        && pass.getExpectedType().contains("java.util.List")
                        && pass.getAfterType() != null
                        && pass.getAfterType().contains("java.util.List")),
                () -> describeTrace(trace));
    }

    private static ClassFile loadClass(Path classFile) throws IOException {
        Options options = new OptionsImpl(Map.of("showversion", "false", "silent", "true"));
        ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
        classFileSource.informAnalysisRelativePathDetail(classFile.toString(), classFile.toString());
        DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
        return dcCommonState.loadClassFileAtPath(classFile.toString());
    }

    private static String describeTrace(TypeRecoveryTrace trace) {
        return trace.getPasses().stream()
                .map(pass -> pass.getPass().getDescriptor().getName()
                        + " | " + pass.getExpressionKind()
                        + " | expected=" + pass.getExpectedType()
                        + " | before=" + pass.getBeforeType()
                        + " | after=" + pass.getAfterType()
                        + " | detail=" + pass.getDetail())
                .collect(Collectors.joining("\n"));
    }
}
