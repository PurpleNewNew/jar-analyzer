package org.benf.cfr.reader;

import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.ClassFileSourceSupport;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryFunction;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.ExceptionDumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.NopSummaryDumper;
import org.benf.cfr.reader.util.output.ProgressDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Driver {
    private static final class JarVersionScope {
        private final DCCommonState dcCommonState;
        private final DumperFactory dumperFactory;
        private final List<JavaTypeInstance> types;
        private final boolean lomem;
        private final boolean silent;
        private final int threadCount;

        private JarVersionScope(DCCommonState dcCommonState,
                                DumperFactory dumperFactory,
                                List<JavaTypeInstance> types,
                                boolean lomem,
                                boolean silent,
                                int threadCount) {
            this.dcCommonState = dcCommonState;
            this.dumperFactory = dumperFactory;
            this.types = types;
            this.lomem = lomem;
            this.silent = silent;
            this.threadCount = threadCount;
        }
    }

    private static DCCommonState withConfiguredObfuscationMapping(DCCommonState dcCommonState) {
        ObfuscationMapping existingMapping = dcCommonState.getObfuscationMapping();
        if (existingMapping != NullMapping.INSTANCE) {
            return dcCommonState;
        }
        ObfuscationMapping configuredMapping = MappingFactory.get(dcCommonState.getOptions(), dcCommonState);
        if (configuredMapping == existingMapping) {
            return dcCommonState;
        }
        return new DCCommonState(dcCommonState, configuredMapping);
    }

    private static void resolveMemberNamesIfConfigured(DCCommonState dcCommonState,
                                                       Collection<? extends JavaTypeInstance> types,
                                                       boolean includeEnumMembers) {
        Options options = dcCommonState.getOptions();
        if (options.getOption(OptionsImpl.RENAME_DUP_MEMBERS) ||
                (includeEnumMembers && options.getOption(OptionsImpl.RENAME_ENUM_MEMBERS))) {
            MemberNameResolver.resolveNames(dcCommonState, types);
        }
    }

    private static void loadInnerClassesIfConfigured(DCCommonState dcCommonState, ClassFile classFile) {
        if (dcCommonState.getOptions().getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
            classFile.loadInnerClasses(dcCommonState);
        }
    }

    private static TypeUsageInformation analyseClass(DCCommonState dcCommonState, ClassFile classFile) {
        Options options = dcCommonState.getOptions();
        TypeUsageCollectingDumper collectingDumper = new TypeUsageCollectingDumper(options, classFile);
        classFile.analyseTop(dcCommonState, collectingDumper);
        return collectingDumper.getRealTypeUsageInformation();
    }

    private static Dumper getTopLevelDumper(DCCommonState dcCommonState,
                                            DumperFactory dumperFactory,
                                            SummaryDumper summaryDumper,
                                            JavaTypeInstance classType,
                                            TypeUsageInformation typeUsageInformation) {
        Options options = dcCommonState.getOptions();
        Dumper dumper = dumperFactory.getNewTopLevelDumper(dcCommonState.getObfuscationMapping().get(classType),
                summaryDumper,
                typeUsageInformation,
                IllegalIdentifierDump.Factory.get(options));
        if (options.getOption(OptionsImpl.TRACK_BYTECODE_LOC)) {
            dumper = dumperFactory.wrapLineNoDumper(dumper);
        }
        return dcCommonState.getObfuscationMapping().wrap(dumper);
    }

    private static DCCommonState withVersionSpecificClassLookup(DCCommonState dcCommonState, List<Integer> versionsSeen) {
        final List<Integer> descendingVersionsSeen = ListFactory.newList(versionsSeen);
        Collections.reverse(descendingVersionsSeen);
        return new DCCommonState(dcCommonState, new BinaryFunction<String, DCCommonState, ClassFile>() {
            @Override
            public ClassFile invoke(String arg, DCCommonState arg2) {
                Exception lastException = null;
                for (int version : descendingVersionsSeen) {
                    try {
                        if (version == 0) {
                            return arg2.loadClassFileAtPath(arg);
                        }
                        return arg2.loadClassFileAtPath(MiscConstants.MULTI_RELEASE_PREFIX + version + "/" + arg);
                    } catch (CannotLoadClassException e) {
                        lastException = e;
                    }
                }
                throw new CannotLoadClassException(arg, lastException);
            }
        });
    }

    private static int getBatchThreadCount(Options options, int typeCount) {
        int threadCount = options.getOption(OptionsImpl.BATCH_THREADS);
        if (threadCount <= 0) {
            threadCount = Runtime.getRuntime().availableProcessors();
        }
        return Math.max(1, Math.min(threadCount, typeCount));
    }

    private static JarVersionScope prepareJarVersionScope(int forVersion,
                                                          List<Integer> versionsSeen,
                                                          DCCommonState dcCommonState,
                                                          DumperFactory dumperFactory,
                                                          List<JavaTypeInstance> types) {
        Options options = dcCommonState.getOptions();
        if (forVersion > 0) {
            dumperFactory = dumperFactory.getFactoryWithPrefix("/" + MiscConstants.MULTI_RELEASE_PREFIX + forVersion + "/", forVersion);
            dcCommonState = withVersionSpecificClassLookup(dcCommonState, versionsSeen);
        }
        final Predicate<String> matcher = MiscUtils.mkRegexFilter(options.getOption(OptionsImpl.JAR_FILTER), true);
        types = Functional.filter(types, new Predicate<JavaTypeInstance>() {
            @Override
            public boolean test(JavaTypeInstance in) {
                return matcher.test(in.getRawName());
            }
        });
        resolveMemberNamesIfConfigured(dcCommonState, types, true);
        return new JarVersionScope(dcCommonState,
                dumperFactory,
                types,
                options.getOption(OptionsImpl.LOMEM),
                options.getOption(OptionsImpl.SILENT),
                getBatchThreadCount(options, types.size()));
    }

    private static void dumpJarTypes(JarVersionScope scope,
                                     SummaryDumper summaryDumper,
                                     ProgressDumper progressDumper) {
        if (scope.threadCount <= 1) {
            for (JavaTypeInstance type : scope.types) {
                dumpJarType(scope.dcCommonState,
                        scope.dumperFactory,
                        summaryDumper,
                        progressDumper,
                        scope.lomem,
                        scope.silent,
                        type);
            }
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(scope.threadCount);
        List<Future<?>> futures = new ArrayList<>(scope.types.size());
        try {
            for (JavaTypeInstance type : scope.types) {
                final JavaTypeInstance workType = type;
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        dumpJarType(scope.dcCommonState.forkForBatchWorker(),
                                scope.dumperFactory,
                                summaryDumper,
                                progressDumper,
                                scope.lomem,
                                scope.silent,
                                workType);
                    }
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while decompiling jar", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Dumper.CannotCreate) {
                        throw (Dumper.CannotCreate) cause;
                    }
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new IllegalStateException(cause);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /*
     * When analysing individual classes, we behave a bit differently to jars - this *Could* probably
     * be refactored to a call to doJarVersionTypes, however, we need to cope with a few oddities.
     *
     * * we don't know all available classes up front - we might be analysing one class file in a temp directory.
     *   If we scan the entire directory in advance (i.e. pretend it's in a jar), we'll potentially process many
     *   files which are irrelevant, at significant cost.
     * * class file names may not match class files! - this isn't likely to happen inside a jar, because the JRE
     *   mandates file names match declared names, but absolutely could happen when analysing randomly named class
     *   files in a junk directory.
     */
    public static void doClass(DCCommonState dcCommonState, String path, boolean skipInnerClass, DumperFactory dumperFactory) {
        dcCommonState = withConfiguredObfuscationMapping(dcCommonState);
        Options options = dcCommonState.getOptions();
        Dumper d = new ToStringDumper(); // sentinel dumper.
        ExceptionDumper ed = dumperFactory.getExceptionDumper();
        try {
            SummaryDumper summaryDumper = new NopSummaryDumper();
            ClassFile c = dcCommonState.getClassFileForAnalysis(path);
            if (skipInnerClass && c.isInnerClass()) return;
            dumperFactory.getProgressDumper().analysingType(c.getClassType());

            loadInnerClassesIfConfigured(dcCommonState, c);
            resolveMemberNamesIfConfigured(dcCommonState, ListFactory.newList(dcCommonState.getClassCache().getLoadedTypes()), false);
            TypeUsageInformation typeUsageInformation = analyseClass(dcCommonState, c);
            d = getTopLevelDumper(dcCommonState, dumperFactory, summaryDumper, c.getClassType(), typeUsageInformation);

            String methname = options.getOption(OptionsImpl.METHODNAME);
            if (methname == null) {
                c.dump(d);
            } else {
                try {
                    for (Method method : c.getMethodByName(methname)) {
                        method.dump(d, true);
                    }
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("No such method '" + methname + "'.");
                }
            }
            d.print("");
        } catch (Exception e) {
            ed.noteException(path, null, e);
        } finally {
            if (d != null) d.close();
        }
    }

    public static void doJar(DCCommonState dcCommonState, String path, AnalysisType analysisType, DumperFactory dumperFactory) {
        dcCommonState = withConfiguredObfuscationMapping(dcCommonState);
        SummaryDumper summaryDumper = null;
        try {
            ProgressDumper progressDumper = dumperFactory.getProgressDumper();
            summaryDumper = dumperFactory.getSummaryDumper();
            summaryDumper.notify("Summary for " + path);
            summaryDumper.notify(MiscConstants.CFR_HEADER_BRA + " " + CfrVersionInfo.VERSION_INFO);
            progressDumper.analysingPath(path);
            Map<Integer, List<JavaTypeInstance>> clstypes = ClassFileSourceSupport.explicitlyLoadJar(dcCommonState, path, analysisType);
            Set<JavaTypeInstance> versionCollisions = getVersionCollisions(clstypes);
            dcCommonState.setCollisions(versionCollisions);
            List<Integer> versionsSeen = ListFactory.newList();
            
            addMissingOuters(clstypes);
            
            for (Map.Entry<Integer, List<JavaTypeInstance>> entry : clstypes.entrySet()) {
                int forVersion = entry.getKey();
                versionsSeen.add(forVersion);
                List<Integer> localVersionsSeen = ListFactory.newList(versionsSeen);
                List<JavaTypeInstance> types = entry.getValue();
                doJarVersionTypes(forVersion, localVersionsSeen, dcCommonState, dumperFactory, summaryDumper, progressDumper, types);
            }
        } catch (Exception e) {
            dumperFactory.getExceptionDumper().noteException(path, "Exception analysing jar", e);
            if (summaryDumper != null) summaryDumper.notify("Exception analysing jar " + e);
        } finally {
            if (summaryDumper != null) {
                summaryDumper.close();
            }
        }
    }

    /*
     * If there are any inner classes in values which are orphaned, then we want to
     * additionally add their outer classes, to ensure that they are not skipped as
     * not required.
     */
    private static void addMissingOuters(Map<Integer, List<JavaTypeInstance>> clstypes) {
        for (Map.Entry<Integer, List<JavaTypeInstance>> entry : clstypes.entrySet()) {
            int version = entry.getKey();
            if (version == 0) continue;
            Set<JavaTypeInstance> distinct = SetFactory.newOrderedSet(entry.getValue());
            Set<JavaTypeInstance> toAdd = SetFactory.newOrderedSet();
            for (JavaTypeInstance typ : entry.getValue()) {
                InnerClassInfo ici = typ.getInnerClassHereInfo();
                while (ici != null && ici.isInnerClass()) {
                    typ = ici.getOuterClass();
                    if (distinct.add(typ)) {
                        toAdd.add(typ);
                    }
                    ici = typ.getInnerClassHereInfo();
                }
            }
            entry.getValue().addAll(toAdd);
        }
    }

    private static Set<JavaTypeInstance> getVersionCollisions(Map<Integer, List<JavaTypeInstance>> clstypes) {
        if (clstypes.size() <= 1) return Collections.emptySet();
        Set<JavaTypeInstance> collisions = SetFactory.newOrderedSet();
        Set<JavaTypeInstance> seen = SetFactory.newSet();
        for (List<JavaTypeInstance> types : clstypes.values()) {
            for (JavaTypeInstance type : types) {
                if (!seen.add(type)) collisions.add(type);
            }
        }
        return collisions;
    }

    private static void doJarVersionTypes(int forVersion, final List<Integer> versionsSeen, DCCommonState dcCommonState, DumperFactory dumperFactory, SummaryDumper summaryDumper, ProgressDumper progressDumper, List<JavaTypeInstance> types) {
        dumpJarTypes(prepareJarVersionScope(forVersion, versionsSeen, dcCommonState, dumperFactory, types),
                summaryDumper,
                progressDumper);
    }

    private static void dumpJarType(DCCommonState dcCommonState,
                                    DumperFactory dumperFactory,
                                    SummaryDumper summaryDumper,
                                    ProgressDumper progressDumper,
                                    boolean lomem,
                                    boolean silent,
                                    JavaTypeInstance inputType) {
        JavaTypeInstance type = inputType;
        Dumper d = new ToStringDumper();
        try {
            ClassFile c = dcCommonState.getClassFile(type);
            if (c.isInnerClass()) {
                d = null;
                return;
            }
            if (!silent) {
                type = dcCommonState.getObfuscationMapping().get(type);
                progressDumper.analysingType(type);
            }
            Options options = dcCommonState.getOptions();
            loadInnerClassesIfConfigured(dcCommonState, c);
            TypeUsageInformation typeUsageInformation = analyseClass(dcCommonState, c);
            d = getTopLevelDumper(dcCommonState, dumperFactory, summaryDumper, c.getClassType(), typeUsageInformation);
            c.dump(d);
            d.newln();
            d.newln();
            if (lomem) {
                c.releaseCode();
            }
        } catch (Dumper.CannotCreate e) {
            throw e;
        } catch (RuntimeException e) {
            d.print(e.toString()).newln().newln().newln();
        } finally {
            if (d != null) d.close();
        }
    }

}
