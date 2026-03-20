package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.ModernFeatureStrategy;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.getopt.Options;

import java.util.List;

/*
 * Keep this file for op3 transforms that either bridge package-private implementations
 * or represent a fixed phase boundary the pipeline should call as a single step.
 */
public class Op03Rewriters {
    public static void simplifyConditionals(List<Op03SimpleStatement> statements, boolean aggressive, Method method) {
        ConditionalSimplifier.simplifyConditionals(statements, aggressive, method);
    }

    public static void replaceReturningIfs(List<Op03SimpleStatement> statements, boolean aggressive) {
        ReturnRewriter.replaceReturningIfs(statements, aggressive);
    }

    public static void propagateToReturn2(List<Op03SimpleStatement> statements) {
        ReturnRewriter.propagateToReturn2(statements);
    }

    public static void collapseAssignmentsIntoConditionals(List<Op03SimpleStatement> statements, Options options, ClassFileVersion classFileVersion) {
        ConditionalCondenser.collapseAssignmentsIntoConditionals(statements, options, classFileVersion);
    }

    public static List<Op03SimpleStatement> prepareCatchBlocks(List<Op03SimpleStatement> statements,
                                                               BlockIdentifierFactory blockIdentifierFactory) {
        return ExceptionRewriters.prepareCatchBlocks(statements, blockIdentifierFactory);
    }

    public static void normalizeTryBounds(DCCommonState dcCommonState, List<Op03SimpleStatement> statements) {
        TryRewriter.normalizeTryBounds(dcCommonState, statements);
    }

    public static List<Op03SimpleStatement> normalizeTopSortedTryBounds(DCCommonState dcCommonState,
                                                                        List<Op03SimpleStatement> statements) {
        return TryRewriter.normalizeTopSortedTryBounds(dcCommonState, statements);
    }

    public static void extractExceptionJumps(List<Op03SimpleStatement> statements) {
        TryRewriter.extractExceptionJumps(statements);
    }

    public static void rejoinBlocks(List<Op03SimpleStatement> statements) {
        JoinBlocks.rejoinBlocks(statements);
    }

    public static void normalizeLoopShapes(Options options, List<Op03SimpleStatement> statements) {
        WhileRewriter.normalizeLoopShapes(options, statements);
    }

    public static void removeSynchronizedCatchBlocks(Options options, List<Op03SimpleStatement> statements) {
        SynchronizedRewriter.removeSynchronizedCatchBlocks(options, statements);
    }

    public static void extractAssertionJumps(List<Op03SimpleStatement> statements) {
        AssertionJumps.extractAssertionJumps(statements);
    }

    public static void pushPreChangeBack(List<Op03SimpleStatement> statements) {
        PrePostchangeAssignmentRewriter.pushPreChangeBack(statements);
    }

    public static void replacePrePostChangeAssignments(List<Op03SimpleStatement> statements) {
        PrePostchangeAssignmentRewriter.replacePrePostChangeAssignments(statements);
    }

    public static void extractExceptionMiddle(List<Op03SimpleStatement> statements) {
        ExceptionRewriters.extractExceptionMiddle(statements);
    }

    public static void removePointlessExpressionStatements(List<Op03SimpleStatement> statements) {
        PointlessExpressions.removePointlessExpressionStatements(statements);
    }

    public static void normalizeConstructionPhase(DCCommonState dcCommonState,
                                                  Options options,
                                                  Method method,
                                                  List<Op03SimpleStatement> statements,
                                                  AnonymousClassUsage anonymousClassUsage,
                                                  ModernFeatureStrategy modernFeatures) {
        if (options.getOption(org.benf.cfr.reader.util.getopt.OptionsImpl.COMMENT_MONITORS)) {
            MonitorRewriter.commentMonitors(statements);
        }
        CondenseConstruction.condenseConstruction(dcCommonState, method, statements, anonymousClassUsage, modernFeatures);
        StaticInitReturnRewriter.rewrite(options, method, statements);
    }

    public static void nopIsolatedStackValues(List<Op03SimpleStatement> statements) {
        IsolatedStackValue.nopIsolatedStackValues(statements);
    }

    public static void rewriteBadCompares(org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory vf,
                                          List<Op03SimpleStatement> statements) {
        new BadCompareRewriter(vf).rewrite(statements);
    }

    /*
     * Neither of these (cloneCodeFromLoop/moveJumpsIntoDo) are 'nice' transforms - they mess with the original code.
     */
    public static void cloneCodeFromLoop(List<Op03SimpleStatement> statements, Options options, DecompilerComments comments) {
        new JumpsIntoLoopCloneRewriter(options).rewrite(statements, comments);
    }

    public static void moveJumpsIntoDo(org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory vf,
                                       List<Op03SimpleStatement> statements,
                                       Options options,
                                       DecompilerComments comments) {
        new JumpsIntoDoRewriter(vf).rewrite(statements, comments);
    }
}
