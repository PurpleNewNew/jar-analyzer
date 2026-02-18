package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.analyze.asm.ASMPrint;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCallEngine;
import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.core.mapper.ClassFileMapper;
import me.n1ar4.jar.analyzer.core.mapper.JarMapper;
import me.n1ar4.jar.analyzer.core.mapper.ResourceMapper;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DFSUtil;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.exporter.LeakCsvExporter;
import me.n1ar4.jar.analyzer.gadget.GadgetAnalyzer;
import me.n1ar4.jar.analyzer.gadget.GadgetInfo;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.util.DecompiledMethodLocator;
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsResultItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ClassNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetRowDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakRulesDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpLineConfigDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey;
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.NoteSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import me.n1ar4.jar.analyzer.gui.runtime.model.WebClassBucket;
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;
import me.n1ar4.jar.analyzer.leak.ApiKeyRule;
import me.n1ar4.jar.analyzer.leak.BankCardRule;
import me.n1ar4.jar.analyzer.leak.CloudAKSKRule;
import me.n1ar4.jar.analyzer.leak.CryptoKeyRule;
import me.n1ar4.jar.analyzer.leak.EmailRule;
import me.n1ar4.jar.analyzer.leak.FilePathRule;
import me.n1ar4.jar.analyzer.leak.IDCardRule;
import me.n1ar4.jar.analyzer.leak.IPAddressRule;
import me.n1ar4.jar.analyzer.leak.JDBCRule;
import me.n1ar4.jar.analyzer.leak.JWTRule;
import me.n1ar4.jar.analyzer.leak.LeakContext;
import me.n1ar4.jar.analyzer.leak.MacAddressRule;
import me.n1ar4.jar.analyzer.leak.OpenAITokenRule;
import me.n1ar4.jar.analyzer.leak.PasswordRule;
import me.n1ar4.jar.analyzer.leak.PhoneRule;
import me.n1ar4.jar.analyzer.leak.UrlRule;
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;
import me.n1ar4.jar.analyzer.mcp.McpLine;
import me.n1ar4.jar.analyzer.mcp.McpManager;
import me.n1ar4.jar.analyzer.mcp.McpReportWebConfig;
import me.n1ar4.jar.analyzer.mcp.McpServiceConfig;
import me.n1ar4.jar.analyzer.sca.SCAParser;
import me.n1ar4.jar.analyzer.sca.SCAVulDB;
import me.n1ar4.jar.analyzer.sca.dto.CVEData;
import me.n1ar4.jar.analyzer.sca.dto.SCAResult;
import me.n1ar4.jar.analyzer.sca.dto.SCARule;
import me.n1ar4.jar.analyzer.sca.utils.ReportUtil;
import me.n1ar4.jar.analyzer.sca.utils.SCAHashUtil;
import me.n1ar4.jar.analyzer.sca.utils.SCAMultiUtil;
import me.n1ar4.jar.analyzer.sca.utils.SCASingleUtil;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintCache;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.DbFileUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.SqlLogConfig;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.shell.analyzer.form.ShellForm;
import me.n1ar4.games.flappy.FBMainFrame;
import me.n1ar4.games.pocker.Main;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.ibatis.session.SqlSession;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RuntimeFacades {
    private static final Logger logger = LogManager.getLogger();
    private static volatile Consumer<ToolingWindowRequest> toolingWindowConsumer = request -> {
    };

    private static final RuntimeState STATE = new RuntimeState();
    private static final int STRIPE_MIN_WIDTH = 40;
    private static final int STRIPE_MAX_WIDTH = 100;
    private static final int DEFAULT_LANGUAGE = loadInitialLanguage();
    private static final String DEFAULT_THEME = loadInitialTheme();
    private static final boolean STRIPE_DEFAULT_SHOW_NAMES = loadInitialStripeShowNames();
    private static final int STRIPE_DEFAULT_WIDTH = loadInitialStripeWidth();

    private static final BuildFacade BUILD = new DefaultBuildFacade();
    private static final SearchFacade SEARCH = new DefaultSearchFacade();
    private static final CallGraphFacade CALL_GRAPH = new DefaultCallGraphFacade();
    private static final WebFacade WEB = new DefaultWebFacade();
    private static final NoteFacade NOTE = new DefaultNoteFacade();
    private static final ScaFacade SCA = new DefaultScaFacade();
    private static final LeakFacade LEAK = new DefaultLeakFacade();
    private static final GadgetFacade GADGET = new DefaultGadgetFacade();
    private static final ChainsFacade CHAINS = new DefaultChainsFacade();
    private static final ApiMcpFacade API_MCP = new DefaultApiMcpFacade();
    private static final EditorFacade EDITOR = new DefaultEditorFacade();
    private static final ProjectTreeFacade PROJECT_TREE = new DefaultProjectTreeFacade();
    private static final ToolingFacade TOOLING = new DefaultToolingFacade();

    private RuntimeFacades() {
    }

    public static BuildFacade build() {
        return BUILD;
    }

    public static SearchFacade search() {
        return SEARCH;
    }

    public static CallGraphFacade callGraph() {
        return CALL_GRAPH;
    }

    public static WebFacade web() {
        return WEB;
    }

    public static NoteFacade note() {
        return NOTE;
    }

    public static ScaFacade sca() {
        return SCA;
    }

    public static LeakFacade leak() {
        return LEAK;
    }

    public static GadgetFacade gadget() {
        return GADGET;
    }

    public static ChainsFacade chains() {
        return CHAINS;
    }

    public static ApiMcpFacade apiMcp() {
        return API_MCP;
    }

    public static EditorFacade editor() {
        return EDITOR;
    }

    public static ProjectTreeFacade projectTree() {
        return PROJECT_TREE;
    }

    public static ToolingFacade tooling() {
        return TOOLING;
    }

    public static void setToolingWindowConsumer(Consumer<ToolingWindowRequest> consumer) {
        if (consumer == null) {
            toolingWindowConsumer = request -> {
            };
            return;
        }
        toolingWindowConsumer = consumer;
    }

    private static void emitToolingWindow(ToolingWindowRequest request) {
        try {
            toolingWindowConsumer.accept(request == null
                    ? ToolingWindowRequest.of(ToolingWindowAction.TEXT_VIEWER)
                    : request);
        } catch (Throwable ex) {
            logger.warn("emit tooling window failed: {}", ex.toString());
        }
    }

    private static void emitToolingWindow(ToolingWindowAction action) {
        emitToolingWindow(ToolingWindowRequest.of(action));
    }

    private static void emitMarkdownWindow(String title, String markdownResource) {
        emitToolingWindow(new ToolingWindowRequest(
                ToolingWindowAction.MARKDOWN_VIEWER,
                new ToolingWindowPayload.MarkdownPayload(safe(title), safe(markdownResource))
        ));
    }

    private static void emitPathWindow(ToolingWindowAction action, String value) {
        emitToolingWindow(new ToolingWindowRequest(
                action,
                new ToolingWindowPayload.PathPayload(safe(value))
        ));
    }

    private static void emitTextWindow(String title, String content) {
        emitToolingWindow(new ToolingWindowRequest(
                ToolingWindowAction.TEXT_VIEWER,
                new ToolingWindowPayload.TextPayload(safe(title), safe(content))
        ));
    }

    public static String launchExternalTool(String toolId) {
        String key = safe(toolId).trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "remote-tomcat" -> launchRemoteTomcatTool();
            case "bytecode-debugger" -> launchBytecodeDebuggerTool();
            case "jd-gui" -> launchJdGuiTool();
            default -> "unknown tool: " + key;
        };
    }

    private static String launchRemoteTomcatTool() {
        Thread.ofVirtual().name("gui-runtime-shell-analyzer").start(() -> {
            try {
                ShellForm.start0();
            } catch (Throwable ex) {
                emitTextWindow("Remote Tomcat", "Start failed: " + ex.getMessage());
            }
        });
        return "remote tomcat analyzer started";
    }

    private static String launchBytecodeDebuggerTool() {
        Thread.ofVirtual().name("gui-runtime-bytecode-debugger").start(() -> {
            try {
                me.n1ar4.dbg.gui.MainForm.start();
            } catch (Throwable ex) {
                emitTextWindow("Bytecode Debugger", "Start failed: " + ex.getMessage());
            }
        });
        return "bytecode debugger started";
    }

    private static String launchJdGuiTool() {
        Thread.ofVirtual().name("gui-runtime-jd-gui").start(() -> {
            try {
                Path javaPath;
                String javaHome = safe(System.getProperty("java.home"));
                if (OSUtil.isWindows()) {
                    javaPath = Paths.get(javaHome, "bin", "java.exe");
                } else {
                    javaPath = Paths.get(javaHome, "bin", "java");
                }
                Path jdPath = Paths.get("lib", "jd-gui-1.6.6.jar");
                if (!Files.exists(jdPath)) {
                    jdPath = Paths.get("jd-gui-1.6.6.jar");
                }
                if (!Files.exists(jdPath)) {
                    emitTextWindow("JD-GUI", "jd-gui-1.6.6.jar not found in lib/ or current dir");
                    return;
                }
                List<String> cmd = new ArrayList<>();
                cmd.add(javaPath.toAbsolutePath().toString());
                cmd.add("-jar");
                cmd.add(jdPath.toAbsolutePath().toString());
                String input = safe(STATE.buildSettings.inputPath()).trim();
                if (!input.isEmpty()) {
                    cmd.add(Paths.get(input).toAbsolutePath().toString());
                }
                new ProcessBuilder(cmd).start();
            } catch (Throwable ex) {
                emitTextWindow("JD-GUI", "Start failed: " + ex.getMessage());
            }
        });
        return "jd-gui started";
    }

    private static int normalizeStripeWidth(int width) {
        if (width < STRIPE_MIN_WIDTH) {
            return STRIPE_MIN_WIDTH;
        }
        if (width > STRIPE_MAX_WIDTH) {
            return STRIPE_MAX_WIDTH;
        }
        return width;
    }

    private static boolean loadInitialStripeShowNames() {
        try {
            ConfigFile cfg = ConfigEngine.parseConfig();
            return cfg != null && cfg.isStripeShowNames();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int loadInitialStripeWidth() {
        try {
            ConfigFile cfg = ConfigEngine.parseConfig();
            if (cfg == null) {
                return STRIPE_MIN_WIDTH;
            }
            return normalizeStripeWidth(cfg.getStripeWidth());
        } catch (Throwable ignored) {
            return STRIPE_MIN_WIDTH;
        }
    }

    private static int loadInitialLanguage() {
        int lang = GlobalOptions.CHINESE;
        try {
            ConfigFile cfg = ConfigEngine.parseConfig();
            if (cfg != null && "en".equalsIgnoreCase(safe(cfg.getLang()))) {
                lang = GlobalOptions.ENGLISH;
            }
        } catch (Throwable ignored) {
        }
        GlobalOptions.setLang(lang);
        return lang;
    }

    private static String loadInitialTheme() {
        try {
            ConfigFile cfg = ConfigEngine.parseConfig();
            if (cfg == null) {
                return "default";
            }
            return normalizeTheme(cfg.getTheme());
        } catch (Throwable ignored) {
            return "default";
        }
    }

    private static String normalizeTheme(String theme) {
        String value = safe(theme).trim().toLowerCase(Locale.ROOT);
        if ("dark".equals(value) || "orange".equals(value)) {
            return value;
        }
        return "default";
    }

    private static final class RuntimeState {
        private final AtomicBoolean buildRunning = new AtomicBoolean(false);
        private final AtomicBoolean searchRunning = new AtomicBoolean(false);
        private final AtomicBoolean scaRunning = new AtomicBoolean(false);
        private final AtomicBoolean leakRunning = new AtomicBoolean(false);
        private final AtomicBoolean gadgetRunning = new AtomicBoolean(false);
        private final AtomicBoolean chainsRunning = new AtomicBoolean(false);

        private volatile BuildSettingsDto buildSettings = new BuildSettingsDto(
                "", "", false, false, false, true, false, true, false);
        private volatile int buildProgress = 0;
        private volatile String buildStatusText = "ready";
        private volatile String totalJar = "0";
        private volatile String totalClass = "0";
        private volatile String totalMethod = "0";
        private volatile String totalEdge = "0";
        private volatile String databaseSize = "0";
        private volatile boolean showInnerClass = false;
        private volatile boolean sortByMethod = false;
        private volatile boolean sortByClass = true;
        private volatile boolean groupTreeByJar = false;
        private volatile boolean mergePackageRoot = false;
        private volatile String theme = DEFAULT_THEME;
        private volatile int language = DEFAULT_LANGUAGE;
        private volatile boolean stripeShowNames = STRIPE_DEFAULT_SHOW_NAMES;
        private volatile int stripeWidth = STRIPE_DEFAULT_WIDTH;

        private volatile SearchQueryDto searchQuery = new SearchQueryDto(
                SearchMode.METHOD_CALL, SearchMatchMode.LIKE, "", "", "", false);
        private volatile List<SearchResultDto> searchResults = List.of();
        private volatile String searchStatusText = "ready";

        private volatile MethodNavDto currentMethod = null;
        private volatile String currentClass = "";
        private volatile String currentJar = "";
        private volatile List<MethodNavDto> allMethods = List.of();
        private volatile List<MethodNavDto> callers = List.of();
        private volatile List<MethodNavDto> callees = List.of();
        private volatile List<MethodNavDto> impls = List.of();
        private volatile List<MethodNavDto> superImpls = List.of();

        private volatile String webPathKeyword = "";

        private volatile ScaSettingsDto scaSettings = new ScaSettingsDto(
                true, true, true, "", ScaOutputMode.HTML, "");
        private volatile String scaLogTail = "";

        private volatile LeakRulesDto leakRules = new LeakRulesDto(
                true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, false);
        private volatile List<LeakItemDto> leakResults = List.of();
        private volatile String leakLogTail = "";

        private volatile GadgetSettingsDto gadgetSettings = new GadgetSettingsDto(
                "", true, true, true, true);
        private volatile List<GadgetRowDto> gadgetRows = List.of();

        private volatile ChainsSettingsDto chainsSettings = new ChainsSettingsDto(
                true, false,
                "", "", "",
                "", "", "",
                false, true, 10, false, false,
                "", "low", true, false, null, false, 30
        );

        private volatile EditorDocumentDto editorDocument = new EditorDocumentDto(
                "", "", null, "", "", "", 0, "ready");
        private final Object navLock = new Object();
        private final List<NavState> navStates = new ArrayList<>();
        private int navIndex = -1;
        private boolean navReplaying = false;
    }

    private record NavState(
            String className,
            String methodName,
            String methodDesc,
            Integer jarId
    ) {
    }

    private static final class DefaultBuildFacade implements BuildFacade {
        @Override
        public BuildSnapshotDto snapshot() {
            return new BuildSnapshotDto(
                    STATE.buildSettings,
                    engineStatus(),
                    STATE.buildProgress,
                    STATE.totalJar,
                    STATE.totalClass,
                    STATE.totalMethod,
                    STATE.totalEdge,
                    STATE.databaseSize,
                    STATE.buildStatusText
            );
        }

        @Override
        public void apply(BuildSettingsDto settings) {
            if (settings == null) {
                return;
            }
            STATE.buildSettings = settings;
            STATE.buildStatusText = "build settings updated";
        }

        @Override
        public void startBuild() {
            if (!STATE.buildRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-build").start(() -> {
                try {
                    doBuild();
                } finally {
                    STATE.buildRunning.set(false);
                }
            });
        }

        @Override
        public void clearCache() {
            Thread.ofVirtual().name("gui-runtime-clear-cache").start(() -> {
                try {
                    DecompileEngine.cleanCache();
                } catch (Throwable ex) {
                    logger.debug("clear fern cache failed: {}", ex.toString());
                }
                try {
                    CFRDecompileEngine.cleanCache();
                } catch (Throwable ex) {
                    logger.debug("clear cfr cache failed: {}", ex.toString());
                }
                try {
                    DatabaseManager.clearAllData();
                } catch (Throwable ex) {
                    logger.debug("clear db failed: {}", ex.toString());
                }
                try {
                    DbFileUtil.deleteDbSidecars();
                } catch (Throwable ex) {
                    logger.debug("clear db sidecars failed: {}", ex.toString());
                }
                STATE.buildProgress = 0;
                STATE.totalJar = "0";
                STATE.totalClass = "0";
                STATE.totalMethod = "0";
                STATE.totalEdge = "0";
                STATE.databaseSize = "0";
                STATE.buildStatusText = "cache cleaned";
            });
        }

        private void doBuild() {
            BuildSettingsDto settings = STATE.buildSettings;
            String inputPath = safe(settings.inputPath());
            if (inputPath.isEmpty()) {
                STATE.buildStatusText = "input path is empty";
                return;
            }
            Path input = Paths.get(inputPath);
            if (Files.notExists(input)) {
                STATE.buildStatusText = "input not exists";
                return;
            }
            Path rtPath = null;
            if (settings.addRuntimeJar()) {
                String runtime = safe(settings.runtimePath());
                if (runtime.isEmpty()) {
                    STATE.buildStatusText = "runtime path is empty";
                    return;
                }
                rtPath = Paths.get(runtime);
                if (Files.notExists(rtPath)) {
                    STATE.buildStatusText = "runtime path not exists";
                    return;
                }
            }

            STATE.buildProgress = 0;
            STATE.buildStatusText = "building...";
            try {
                WorkspaceContext.setInputPath(input);
            } catch (Throwable ex) {
                logger.debug("set input path failed: {}", ex.toString());
            }
            try {
                WorkspaceContext.setRuntimeJarPath(rtPath);
            } catch (Throwable ex) {
                logger.debug("set runtime path failed: {}", ex.toString());
            }
            try {
                WorkspaceContext.setResolveInnerJars(settings.resolveNestedJars());
            } catch (Throwable ex) {
                logger.debug("set resolve inner jars failed: {}", ex.toString());
            }
            try {
                CoreRunner.BuildResult result = CoreRunner.run(
                        input,
                        rtPath,
                        settings.fixClassPath(),
                        settings.quickMode(),
                        settings.fixMethodImpl(),
                        p -> STATE.buildProgress = p,
                        true
                );
                if (result == null) {
                    STATE.buildStatusText = "build failed";
                    return;
                }
                STATE.totalJar = String.valueOf(result.getJarCount());
                STATE.totalClass = String.valueOf(result.getClassCount());
                STATE.totalMethod = String.valueOf(result.getMethodCount());
                STATE.totalEdge = String.valueOf(result.getEdgeCount());
                STATE.databaseSize = result.getDbSizeLabel();
                STATE.buildProgress = 100;
                STATE.buildStatusText = "build finished";
                saveBuildConfig(inputPath, result);
            } catch (Throwable ex) {
                STATE.buildStatusText = "build error: " + ex.getMessage();
                logger.error("runtime build failed: {}", ex.toString());
            }
        }

        private void saveBuildConfig(String jarPath, CoreRunner.BuildResult result) {
            ConfigFile cfg = ConfigEngine.parseConfig();
            if (cfg == null) {
                cfg = new ConfigFile();
            }
            cfg.setJarPath(jarPath);
            cfg.setDbPath(Const.dbFile);
            cfg.setTempPath(Const.tempDir);
            cfg.setTotalJar(String.valueOf(result.getJarCount()));
            cfg.setTotalClass(String.valueOf(result.getClassCount()));
            cfg.setTotalMethod(String.valueOf(result.getMethodCount()));
            cfg.setTotalEdge(String.valueOf(result.getEdgeCount()));
            cfg.setDbSize(result.getDbSizeLabel());
            cfg.setLang(STATE.language == GlobalOptions.ENGLISH ? "en" : "zh");
            cfg.setDecompileCacheSize(String.valueOf(DecompileEngine.getCacheCapacity()));
            ConfigEngine.saveConfig(cfg);
        }
    }

    private static final class DefaultSearchFacade implements SearchFacade {
        @Override
        public SearchSnapshotDto snapshot() {
            return new SearchSnapshotDto(
                    STATE.searchQuery,
                    immutableList(STATE.searchResults),
                    STATE.searchStatusText
            );
        }

        @Override
        public void applyQuery(SearchQueryDto query) {
            if (query == null) {
                return;
            }
            STATE.searchQuery = query;
            STATE.searchStatusText = "search query updated";
        }

        @Override
        public void runSearch() {
            if (!STATE.searchRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-search").start(() -> {
                try {
                    doSearch();
                } finally {
                    STATE.searchRunning.set(false);
                }
            });
        }

        @Override
        public void openResult(int index) {
            List<SearchResultDto> list = STATE.searchResults;
            if (index < 0 || index >= list.size()) {
                return;
            }
            SearchResultDto item = list.get(index);
            if (item == null) {
                return;
            }
            if (safe(item.methodName()).isEmpty()) {
                STATE.searchStatusText = "result has no method navigation";
                return;
            }
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
        }

        @Override
        public void publishExternalResults(List<SearchResultDto> results, String statusText) {
            List<SearchResultDto> sorted = new ArrayList<>();
            if (results != null && !results.isEmpty()) {
                sorted.addAll(results);
                Comparator<SearchResultDto> comparator;
                if (STATE.sortByMethod) {
                    comparator = Comparator
                            .comparing((SearchResultDto item) -> safe(item.methodName()))
                            .thenComparing(item -> safe(item.className()));
                } else {
                    comparator = Comparator
                            .comparing((SearchResultDto item) -> safe(item.className()))
                            .thenComparing(item -> safe(item.methodName()));
                }
                sorted.sort(comparator);
            }
            STATE.searchResults = immutableList(sorted);
            if (statusText == null || statusText.isBlank()) {
                STATE.searchStatusText = "results: " + sorted.size();
            } else {
                STATE.searchStatusText = statusText;
            }
        }

        private void doSearch() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                STATE.searchResults = List.of();
                STATE.searchStatusText = "engine is not ready";
                return;
            }
            SearchQueryDto query = STATE.searchQuery;
            List<MethodResult> methods = new ArrayList<>();

            String className = normalizeClass(query.className());
            String methodName = safe(query.methodName());
            String keyword = safe(query.keyword());

            try {
                switch (query.mode()) {
                    case METHOD_CALL -> {
                        if (methodName.isEmpty()) {
                            STATE.searchResults = List.of();
                            STATE.searchStatusText = "method name is required";
                            return;
                        }
                        if (query.matchMode() == SearchMatchMode.EQUALS) {
                            methods = engine.getCallers(classNameOrNull(className), methodName, null);
                        } else {
                            methods = engine.getCallersLike(classNameOrNull(className), methodName, null);
                        }
                    }
                    case METHOD_DEFINITION -> {
                        if (methodName.isEmpty()) {
                            STATE.searchResults = List.of();
                            STATE.searchStatusText = "method name is required";
                            return;
                        }
                        if (query.matchMode() == SearchMatchMode.EQUALS) {
                            methods = engine.getMethod(classNameOrNull(className), methodName, null);
                        } else {
                            methods = engine.getMethodLike(classNameOrNull(className), methodName, null);
                        }
                    }
                    case STRING_CONTAINS -> {
                        if (keyword.isEmpty()) {
                            STATE.searchResults = List.of();
                            STATE.searchStatusText = "keyword is required";
                            return;
                        }
                        if (query.matchMode() == SearchMatchMode.EQUALS) {
                            methods = engine.getMethodsByStrEqual(keyword);
                        } else {
                            methods = engine.getMethodsByStr(keyword);
                        }
                        if (!className.isEmpty()) {
                            String finalClassName = className;
                            List<MethodResult> filtered = new ArrayList<>();
                            for (MethodResult m : methods) {
                                if (m != null && finalClassName.equals(normalizeClass(m.getClassName()))) {
                                    filtered.add(m);
                                }
                            }
                            methods = filtered;
                        }
                    }
                    case BINARY_CONTAINS -> {
                        List<SearchResultDto> binary = scanBinary(engine.getJarsPath(), keyword);
                        STATE.searchResults = immutableList(binary);
                        STATE.searchStatusText = "results: " + binary.size();
                        return;
                    }
                    default -> {
                        STATE.searchResults = List.of();
                        STATE.searchStatusText = "unsupported mode";
                        return;
                    }
                }
            } catch (Throwable ex) {
                STATE.searchResults = List.of();
                STATE.searchStatusText = "search error: " + ex.getMessage();
                logger.error("runtime search failed: {}", ex.toString());
                return;
            }

            List<SearchResultDto> resultDtos = new ArrayList<>();
            for (MethodResult m : methods) {
                if (m == null) {
                    continue;
                }
                if (query.nullParamFilter() && safe(m.getMethodDesc()).contains("()")) {
                    continue;
                }
                resultDtos.add(toSearchResult(m));
            }
            publishExternalResults(resultDtos, "results: " + resultDtos.size());
        }

        private List<SearchResultDto> scanBinary(List<String> jars, String keyword) {
            List<SearchResultDto> out = new ArrayList<>();
            if (keyword == null || keyword.isEmpty() || jars == null || jars.isEmpty()) {
                return out;
            }
            byte[] pattern = keyword.getBytes(StandardCharsets.UTF_8);
            Set<String> hit = new LinkedHashSet<>();
            for (String path : jars) {
                if (path == null || path.isBlank()) {
                    continue;
                }
                try {
                    Path p = Paths.get(path);
                    if (Files.notExists(p)) {
                        continue;
                    }
                    byte[] data = Files.readAllBytes(p);
                    if (contains(data, pattern)) {
                        hit.add(path);
                    }
                } catch (Exception ignored) {
                }
            }
            for (String path : hit) {
                out.add(new SearchResultDto(
                        path,
                        "",
                        "",
                        "",
                        0,
                        path
                ));
            }
            return out;
        }

        private boolean contains(byte[] data, byte[] target) {
            if (data == null || target == null || target.length == 0 || data.length < target.length) {
                return false;
            }
            for (int i = 0; i <= data.length - target.length; i++) {
                boolean ok = true;
                for (int j = 0; j < target.length; j++) {
                    if (data[i + j] != target[j]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class DefaultCallGraphFacade implements CallGraphFacade {
        @Override
        public CallGraphSnapshotDto snapshot() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return new CallGraphSnapshotDto(
                        STATE.currentJar,
                        STATE.currentClass,
                        formatCurrentMethod(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );
            }

            MethodNavDto current = STATE.currentMethod;
            String className = STATE.currentClass;
            List<MethodNavDto> all = new ArrayList<>();
            List<MethodNavDto> caller = new ArrayList<>();
            List<MethodNavDto> callee = new ArrayList<>();
            List<MethodNavDto> impl = new ArrayList<>();
            List<MethodNavDto> superImpl = new ArrayList<>();
            if (!safe(className).isEmpty()) {
                List<MethodResult> allMethods = engine.getMethodsByClass(normalizeClass(className));
                if (allMethods.isEmpty()) {
                    allMethods = engine.getMethodsByClassNoJar(normalizeClass(className));
                }
                for (MethodResult item : allMethods) {
                    if (item == null) {
                        continue;
                    }
                    all.add(toMethodNav(item));
                }
            }
            if (current != null) {
                List<MethodResult> callers = engine.getCallers(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc());
                List<MethodResult> callees = engine.getCallee(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc());
                List<MethodResult> impls = engine.getImpls(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc());
                List<MethodResult> superImpls = engine.getSuperImpls(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc());
                for (MethodResult item : callers) {
                    if (item != null) {
                        caller.add(toMethodNav(item));
                    }
                }
                for (MethodResult item : callees) {
                    if (item != null) {
                        callee.add(toMethodNav(item));
                    }
                }
                for (MethodResult item : impls) {
                    if (item != null) {
                        impl.add(toMethodNav(item));
                    }
                }
                for (MethodResult item : superImpls) {
                    if (item != null) {
                        superImpl.add(toMethodNav(item));
                    }
                }
            }

            STATE.allMethods = all;
            STATE.callers = caller;
            STATE.callees = callee;
            STATE.impls = impl;
            STATE.superImpls = superImpl;

            return new CallGraphSnapshotDto(
                    STATE.currentJar,
                    STATE.currentClass,
                    formatCurrentMethod(),
                    all,
                    caller,
                    callee,
                    impl,
                    superImpl
            );
        }

        @Override
        public void refreshCurrentContext() {
            snapshot();
        }

        @Override
        public void openAllMethod(int index) {
            openByIndex(STATE.allMethods, index);
        }

        @Override
        public void openCaller(int index) {
            openByIndex(STATE.callers, index);
        }

        @Override
        public void openCallee(int index) {
            openByIndex(STATE.callees, index);
        }

        @Override
        public void openImpl(int index) {
            openByIndex(STATE.impls, index);
        }

        @Override
        public void openSuperImpl(int index) {
            openByIndex(STATE.superImpls, index);
        }

        private void openByIndex(List<MethodNavDto> list, int index) {
            if (list == null || index < 0 || index >= list.size()) {
                return;
            }
            MethodNavDto item = list.get(index);
            if (item == null) {
                return;
            }
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
        }
    }

    private static final class DefaultWebFacade implements WebFacade {
        @Override
        public WebSnapshotDto snapshot() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return new WebSnapshotDto(
                        STATE.webPathKeyword,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );
            }

            List<ClassNavDto> controllers = mapClasses(engine.getAllSpringC());
            List<ClassNavDto> interceptors = mapClasses(engine.getAllSpringI());
            List<ClassNavDto> servlets = mapClasses(engine.getAllServlets());
            List<ClassNavDto> filters = mapClasses(engine.getAllFilters());
            List<ClassNavDto> listeners = mapClasses(engine.getAllListeners());
            String keyword = safe(STATE.webPathKeyword);
            List<MethodNavDto> mappings = mapMethods(engine.getSpringMappingsAll(
                    null,
                    keyword.isEmpty() ? null : keyword,
                    null,
                    2000
            ));
            return new WebSnapshotDto(
                    keyword,
                    controllers,
                    mappings,
                    interceptors,
                    servlets,
                    filters,
                    listeners
            );
        }

        @Override
        public void refreshAll() {
            snapshot();
        }

        @Override
        public void pathSearch(String keyword) {
            STATE.webPathKeyword = safe(keyword);
        }

        @Override
        public void openClass(WebClassBucket bucket, int index) {
            WebSnapshotDto snapshot = snapshot();
            List<ClassNavDto> list = switch (bucket) {
                case CONTROLLER -> snapshot.controllers();
                case INTERCEPTOR -> snapshot.interceptors();
                case SERVLET -> snapshot.servlets();
                case FILTER -> snapshot.filters();
                case LISTENER -> snapshot.listeners();
            };
            if (index < 0 || index >= list.size()) {
                return;
            }
            ClassNavDto item = list.get(index);
            RuntimeFacades.editor().openClass(item.className(), item.jarId());
        }

        @Override
        public void openMapping(int index) {
            WebSnapshotDto snapshot = snapshot();
            if (index < 0 || index >= snapshot.mappings().size()) {
                return;
            }
            MethodNavDto item = snapshot.mappings().get(index);
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
        }
    }

    private static final class DefaultNoteFacade implements NoteFacade {
        @Override
        public NoteSnapshotDto snapshot() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return new NoteSnapshotDto(List.of(), List.of());
            }
            return new NoteSnapshotDto(
                    mapMethods(engine.getAllHisMethods()),
                    mapMethods(engine.getAllFavMethods())
            );
        }

        @Override
        public void load() {
            snapshot();
        }

        @Override
        public void openHistory(int index) {
            NoteSnapshotDto snapshot = snapshot();
            if (index < 0 || index >= snapshot.history().size()) {
                return;
            }
            MethodNavDto item = snapshot.history().get(index);
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
        }

        @Override
        public void openFavorite(int index) {
            NoteSnapshotDto snapshot = snapshot();
            if (index < 0 || index >= snapshot.favorites().size()) {
                return;
            }
            MethodNavDto item = snapshot.favorites().get(index);
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
        }

        @Override
        public void clearHistory() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return;
            }
            engine.cleanHistory();
        }

        @Override
        public void clearFavorites() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return;
            }
            engine.cleanFav();
        }
    }

    private static final class DefaultScaFacade implements ScaFacade {
        private static final Map<String, CVEData> CVE_MAP = SCAVulDB.getCVEMap();
        private static final List<SCARule> LOG4J_RULES = SCAParser.getApacheLog4j2Rules();
        private static final List<SCARule> FASTJSON_RULES = SCAParser.getFastjsonRules();
        private static final List<SCARule> SHIRO_RULES = SCAParser.getShiroRules();

        @Override
        public ScaSnapshotDto snapshot() {
            return new ScaSnapshotDto(STATE.scaSettings, STATE.scaLogTail);
        }

        @Override
        public void apply(ScaSettingsDto settings) {
            if (settings == null) {
                return;
            }
            STATE.scaSettings = settings;
        }

        @Override
        public void chooseInput() {
            emitPathWindow(ToolingWindowAction.SCA_INPUT_PICKER, STATE.scaSettings.inputPath());
        }

        @Override
        public void start() {
            if (!STATE.scaRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-sca").start(() -> {
                try {
                    doScan();
                } finally {
                    STATE.scaRunning.set(false);
                }
            });
        }

        @Override
        public void openResult() {
            String output = safe(STATE.scaSettings.outputFile());
            if (output.isEmpty()) {
                return;
            }
            try {
                Desktop.getDesktop().browse(Paths.get(output).toAbsolutePath().toUri());
            } catch (Throwable ignored) {
            }
        }

        private void doScan() {
            ScaSettingsDto settings = STATE.scaSettings;
            String input = safe(settings.inputPath());
            if (input.isEmpty()) {
                appendSca("input is empty");
                return;
            }
            Path path = Paths.get(input);
            if (Files.notExists(path)) {
                appendSca("input not exists");
                return;
            }
            List<String> jarList = new ArrayList<>();
            if (Files.isDirectory(path)) {
                jarList.addAll(DirUtil.GetFiles(path.toAbsolutePath().toString()));
                appendSca("input is a dir");
            } else {
                jarList.add(path.toAbsolutePath().toString());
                appendSca("input is a file");
            }
            appendSca("start scan and wait...");

            List<SCAResult> all = new ArrayList<>();
            for (String jar : jarList) {
                List<String> exist = new ArrayList<>();
                if (settings.scanLog4j()) {
                    execWithOneRule(all, jar, exist, LOG4J_RULES);
                }
                if (settings.scanFastjson()) {
                    execWithOneRule(all, jar, exist, FASTJSON_RULES);
                }
                if (settings.scanShiro()) {
                    execWithManyRules(all, jar, exist, SHIRO_RULES);
                }
            }
            if (all.isEmpty()) {
                appendSca("no vulnerability found");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (SCAResult item : all) {
                CVEData cve = CVE_MAP.get(item.getCVE());
                String desc = cve == null ? "" : safe(cve.getDesc());
                String cvss = cve == null ? "" : String.valueOf(cve.getCvss());
                String hash = safe(item.getHash());
                if (hash.length() > 16) {
                    hash = hash.substring(0, 16);
                }
                sb.append("CVE-ID: ").append(item.getCVE()).append('\n');
                sb.append("DESC  : ").append(desc).append('\n');
                sb.append("CVSS  : ").append(cvss).append('\n');
                sb.append("JAR   : ").append(item.getJarPath()).append('\n');
                sb.append("CLASS : ").append(item.getKeyClass()).append('\n');
                sb.append("HASH(16): ").append(hash).append("\n\n");
            }

            if (settings.outputMode() == ScaOutputMode.HTML) {
                try {
                    String output = safe(settings.outputFile());
                    if (output.isEmpty()) {
                        output = String.format("jar-analyzer-sca-%d.html", System.currentTimeMillis());
                    }
                    ReportUtil.generateHtmlReport(sb.toString(), output);
                    STATE.scaSettings = new ScaSettingsDto(
                            settings.scanLog4j(),
                            settings.scanShiro(),
                            settings.scanFastjson(),
                            settings.inputPath(),
                            settings.outputMode(),
                            output
                    );
                } catch (Throwable ex) {
                    appendSca("generate html failed: " + ex.getMessage());
                }
            }
            appendSca(sb.toString());
        }

        private void execWithOneRule(List<SCAResult> cveList,
                                     String jarPath,
                                     List<String> exist,
                                     List<SCARule> rules) {
            if (rules == null || rules.isEmpty()) {
                return;
            }
            String keyClass = rules.get(0).getOnlyClassName();
            byte[] data = SCASingleUtil.exploreJar(Paths.get(jarPath).toFile(), keyClass);
            if (data == null) {
                return;
            }
            String targetHash = SCAHashUtil.sha256(data);
            for (SCARule rule : rules) {
                if (rule == null) {
                    continue;
                }
                if (!safe(rule.getOnlyHash()).equals(targetHash)) {
                    continue;
                }
                if (exist.contains(rule.getCVE())) {
                    continue;
                }
                exist.add(rule.getCVE());
                SCAResult result = new SCAResult();
                result.setHash(rule.getOnlyHash());
                result.setCVE(rule.getCVE());
                result.setVersion(rule.getVersion());
                result.setJarPath(jarPath);
                result.setProject(rule.getProjectName());
                result.setKeyClass(keyClass);
                cveList.add(result);
            }
        }

        private void execWithManyRules(List<SCAResult> cveList,
                                       String jarPath,
                                       List<String> exist,
                                       List<SCARule> rules) {
            if (rules == null || rules.isEmpty()) {
                return;
            }
            Map<String, String> baseMap = rules.get(0).getHashMap();
            Map<String, byte[]> resultMap = SCAMultiUtil.exploreJarEx(Paths.get(jarPath).toFile(), baseMap);
            if (resultMap == null || resultMap.isEmpty()) {
                return;
            }
            for (SCARule rule : rules) {
                if (rule == null || exist.contains(rule.getCVE())) {
                    continue;
                }
                boolean ok = true;
                Map<String, String> ruleHashMap = rule.getHashMap();
                for (String key : resultMap.keySet()) {
                    String actual = SCAHashUtil.sha256(resultMap.get(key));
                    String expect = ruleHashMap.get(key);
                    if (!actual.equals(expect)) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) {
                    continue;
                }
                exist.add(rule.getCVE());
                SCAResult result = new SCAResult();
                Map.Entry<String, String> first = baseMap.entrySet().iterator().next();
                result.setHash(first.getValue());
                result.setCVE(rule.getCVE());
                result.setVersion(rule.getVersion());
                result.setJarPath(jarPath);
                result.setProject(rule.getProjectName());
                result.setKeyClass(first.getKey());
                cveList.add(result);
            }
        }

        private void appendSca(String msg) {
            STATE.scaLogTail = appendTail(STATE.scaLogTail, "[LOG] " + safe(msg) + '\n');
        }
    }

    private static final class DefaultLeakFacade implements LeakFacade {
        @Override
        public LeakSnapshotDto snapshot() {
            return new LeakSnapshotDto(STATE.leakRules, immutableList(STATE.leakResults), STATE.leakLogTail);
        }

        @Override
        public void apply(LeakRulesDto rules) {
            if (rules == null) {
                return;
            }
            STATE.leakRules = rules;
        }

        @Override
        public void start() {
            if (!STATE.leakRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-leak").start(() -> {
                try {
                    doScan();
                } finally {
                    STATE.leakRunning.set(false);
                }
            });
        }

        @Override
        public void clear() {
            STATE.leakResults = List.of();
            STATE.leakLogTail = appendTail("", "clean data finish\n");
        }

        @Override
        public void export() {
            List<LeakItemDto> snapshot = STATE.leakResults;
            if (snapshot == null || snapshot.isEmpty()) {
                STATE.leakLogTail = appendTail(STATE.leakLogTail, "no leak result to export\n");
                return;
            }
            List<LeakResult> list = new ArrayList<>();
            for (LeakItemDto item : snapshot) {
                list.add(toLeakResult(item));
            }
            LeakCsvExporter exporter = new LeakCsvExporter(list);
            boolean ok = exporter.doExport();
            if (ok) {
                STATE.leakLogTail = appendTail(STATE.leakLogTail, "export success: " + exporter.getFileName() + "\n");
            } else {
                STATE.leakLogTail = appendTail(STATE.leakLogTail, "export failed\n");
            }
        }

        @Override
        public void openResult(int index) {
            List<LeakItemDto> list = STATE.leakResults;
            if (index < 0 || index >= list.size()) {
                return;
            }
            LeakItemDto item = list.get(index);
            RuntimeFacades.editor().openClass(item.className(), item.jarId());
        }

        private void doScan() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                appendLeak("engine not ready");
                return;
            }
            LeakRulesDto rules = STATE.leakRules;
            List<MemberEntity> members = engine.getAllMembersInfo();
            Map<String, String> stringMap = engine.getStringMap();
            Set<LeakResult> resultSet = new LinkedHashSet<>();
            LeakContext.runWithDetectBase64(rules.detectBase64(), () -> {
                RuleConfig[] configs = new RuleConfig[]{
                        new RuleConfig(rules.jwt(), JWTRule::match, "JWT-TOKEN", "jwt-token"),
                        new RuleConfig(rules.idCard(), IDCardRule::match, "ID-CARD", "id-card"),
                        new RuleConfig(rules.ip(), IPAddressRule::match, "IP-ADDR", "ip-addr"),
                        new RuleConfig(rules.email(), EmailRule::match, "EMAIL", "email"),
                        new RuleConfig(rules.url(), UrlRule::match, "URL", "url"),
                        new RuleConfig(rules.jdbc(), JDBCRule::match, "JDBC", "jdbc"),
                        new RuleConfig(rules.filePath(), FilePathRule::match, "FILE-PATH", "file-path"),
                        new RuleConfig(rules.mac(), MacAddressRule::match, "MAC-ADDR", "mac-addr"),
                        new RuleConfig(rules.phone(), PhoneRule::match, "PHONE", "phone"),
                        new RuleConfig(rules.apiKey(), ApiKeyRule::match, "API-KEY", "api-key"),
                        new RuleConfig(rules.bankCard(), BankCardRule::match, "BANK-CARD", "bank-card"),
                        new RuleConfig(rules.cloudAkSk(), CloudAKSKRule::match, "CLOUD-AKSK", "cloud-aksk"),
                        new RuleConfig(rules.cryptoKey(), CryptoKeyRule::match, "CRYPTO-KEY", "crypto-key"),
                        new RuleConfig(rules.aiKey(), OpenAITokenRule::match, "AI-KEY", "ai-key"),
                        new RuleConfig(rules.password(), PasswordRule::match, "PASSWORD", "password")
                };
                for (RuleConfig config : configs) {
                    processLeakRule(config, members, stringMap, resultSet);
                }
            });

            List<LeakItemDto> items = new ArrayList<>();
            for (LeakResult leak : resultSet) {
                if (leak == null) {
                    continue;
                }
                items.add(new LeakItemDto(
                        safe(leak.getClassName()),
                        safe(leak.getTypeName()),
                        safe(leak.getValue()),
                        safe(leak.getJarName()),
                        leak.getJarId() == null ? 0 : leak.getJarId()
                ));
            }
            items.sort(Comparator.comparing(LeakItemDto::typeName).thenComparing(LeakItemDto::className));
            STATE.leakResults = immutableList(items);
            appendLeak("total leak results: " + items.size());
        }

        private void processLeakRule(RuleConfig config,
                                     List<MemberEntity> members,
                                     Map<String, String> stringMap,
                                     Set<LeakResult> results) {
            if (config == null || !config.enabled) {
                return;
            }
            appendLeak(config.logName + " leak start");
            if (members != null) {
                for (MemberEntity member : members) {
                    if (member == null) {
                        continue;
                    }
                    String className = member.getClassName();
                    if (CommonFilterUtil.isFilteredClass(className)) {
                        continue;
                    }
                    List<String> data = config.ruleFn.apply(safe(member.getValue()));
                    if (data == null || data.isEmpty()) {
                        continue;
                    }
                    for (String s : data) {
                        LeakResult leakResult = new LeakResult();
                        leakResult.setClassName(className);
                        leakResult.setTypeName(config.typeName);
                        leakResult.setValue(safe(s));
                        results.add(leakResult);
                    }
                }
            }

            Path tempDir = Paths.get(Const.tempDir).toAbsolutePath();
            try {
                List<String> allFiles = DirUtil.GetFiles(tempDir.toString());
                for (String filePath : allFiles) {
                    Path file = Paths.get(filePath);
                    String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    boolean isConfigFile = false;
                    for (String ext : JarUtil.CONFIG_EXTENSIONS) {
                        if (fileName.endsWith(ext.toLowerCase(Locale.ROOT))) {
                            isConfigFile = true;
                            break;
                        }
                    }
                    if (!isConfigFile) {
                        continue;
                    }
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        List<String> data = config.ruleFn.apply(content);
                        if (data == null || data.isEmpty()) {
                            continue;
                        }
                        for (String s : data) {
                            LeakResult leakResult = new LeakResult();
                            String relative = tempDir.relativize(file).toString().replace("\\", "/");
                            leakResult.setClassName(relative);
                            leakResult.setTypeName(config.typeName);
                            leakResult.setValue(safe(s));
                            results.add(leakResult);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ex) {
                logger.debug("scan config files failed: {}", ex.toString());
            }

            if (stringMap != null) {
                for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                    String className = entry.getKey();
                    if (CommonFilterUtil.isFilteredClass(className)) {
                        continue;
                    }
                    List<String> data = config.ruleFn.apply(safe(entry.getValue()));
                    if (data == null || data.isEmpty()) {
                        continue;
                    }
                    for (String s : data) {
                        LeakResult leakResult = new LeakResult();
                        leakResult.setClassName(className);
                        leakResult.setTypeName(config.typeName);
                        leakResult.setValue(safe(s));
                        results.add(leakResult);
                    }
                }
            }
            appendLeak(config.logName + " leak finish");
        }

        private void appendLeak(String msg) {
            STATE.leakLogTail = appendTail(STATE.leakLogTail, "[LOG] " + safe(msg) + '\n');
        }
    }

    private static final class DefaultGadgetFacade implements GadgetFacade {
        @Override
        public GadgetSnapshotDto snapshot() {
            return new GadgetSnapshotDto(STATE.gadgetSettings, immutableList(STATE.gadgetRows));
        }

        @Override
        public void apply(GadgetSettingsDto settings) {
            if (settings == null) {
                return;
            }
            STATE.gadgetSettings = settings;
        }

        @Override
        public void chooseDir() {
            emitPathWindow(ToolingWindowAction.GADGET_DIR_PICKER, STATE.gadgetSettings.inputDir());
        }

        @Override
        public void start() {
            if (!STATE.gadgetRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-gadget").start(() -> {
                try {
                    doAnalyze();
                } finally {
                    STATE.gadgetRunning.set(false);
                }
            });
        }

        private void doAnalyze() {
            GadgetSettingsDto settings = STATE.gadgetSettings;
            String input = safe(settings.inputDir());
            if (input.isEmpty()) {
                STATE.gadgetRows = List.of();
                return;
            }
            EnumSet<GadgetAnalyzer.GadgetType> types = EnumSet.noneOf(GadgetAnalyzer.GadgetType.class);
            if (settings.nativeMode()) {
                types.add(GadgetAnalyzer.GadgetType.NATIVE);
            }
            if (settings.hessian()) {
                types.add(GadgetAnalyzer.GadgetType.HESSIAN);
            }
            if (settings.fastjson()) {
                types.add(GadgetAnalyzer.GadgetType.FASTJSON);
            }
            if (settings.jdbc()) {
                types.add(GadgetAnalyzer.GadgetType.JDBC);
            }
            List<GadgetInfo> result = new GadgetAnalyzer(input, types).process();
            List<GadgetRowDto> rows = new ArrayList<>();
            if (result != null) {
                for (GadgetInfo info : result) {
                    if (info == null) {
                        continue;
                    }
                    StringBuilder def = new StringBuilder();
                    for (String jar : info.getJarsName()) {
                        if (def.length() > 0) {
                            def.append(' ');
                        }
                        def.append(jar);
                    }
                    rows.add(new GadgetRowDto(
                            String.valueOf(info.getID()),
                            def.toString(),
                            safe(info.getResult())
                    ));
                }
            }
            STATE.gadgetRows = immutableList(rows);
        }
    }

    private static final class DefaultChainsFacade implements ChainsFacade {
        @Override
        public ChainsSnapshotDto snapshot() {
            return new ChainsSnapshotDto(
                    STATE.chainsSettings,
                    TaintCache.dfsCache.size(),
                    TaintCache.cache.size()
            );
        }

        @Override
        public void apply(ChainsSettingsDto settings) {
            if (settings == null) {
                return;
            }
            STATE.chainsSettings = settings;
        }

        @Override
        public void startDfs() {
            if (!STATE.chainsRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-dfs").start(() -> {
                try {
                    ChainsSettingsDto cfg = STATE.chainsSettings;
                    List<DFSResult> resultList = runDfsWithGraphFallback(cfg);
                    DFSUtil.save(resultList);
                    TaintCache.dfsCache.clear();
                    TaintCache.dfsCache.addAll(resultList);
                    TaintCache.cache.clear();
                    if (cfg.taintEnabled()) {
                        List<TaintResult> taintResult = runTaintWithGraphFallback(cfg, resultList);
                        TaintCache.cache.addAll(taintResult);
                    }
                } catch (Throwable ex) {
                    logger.error("runtime dfs failed: {}", ex.toString());
                } finally {
                    STATE.chainsRunning.set(false);
                }
            });
        }

        @Override
        public void startTaint() {
            Thread.ofVirtual().name("gui-runtime-taint").start(() -> {
                try {
                    if (TaintCache.dfsCache.isEmpty()) {
                        return;
                    }
                    ChainsSettingsDto cfg = STATE.chainsSettings;
                    List<DFSResult> snapshot = new ArrayList<>(TaintCache.dfsCache);
                    List<TaintResult> taintResult = runTaintWithGraphFallback(cfg, snapshot);
                    TaintCache.cache.clear();
                    TaintCache.cache.addAll(taintResult);
                } catch (Throwable ex) {
                    logger.error("runtime taint failed: {}", ex.toString());
                }
            });
        }

        @CompatibilityCode(
                primary = "Graph Procedure ja.path.from_to",
                reason = "Compatibility orchestration keeps legacy DFS available when graph path is unavailable"
        )
        private static List<DFSResult> runDfsWithGraphFallback(ChainsSettingsDto cfg) {
            GraphDfsAttempt graphAttempt = tryGraphDfs(cfg);
            if (graphAttempt.attempted && !graphAttempt.results.isEmpty()) {
                logger.info("runtime dfs backend=graph paths={} detail={}",
                        graphAttempt.results.size(), safe(graphAttempt.detail));
                return graphAttempt.results;
            }
            if (graphAttempt.attempted) {
                logger.info("runtime dfs backend fallback=classic detail={}", safe(graphAttempt.detail));
            }
            return runClassicDfs(cfg);
        }

        @CompatibilityCode(
                primary = "Graph Procedure ja.path.from_to",
                reason = "Legacy DFS engine path retained for compatibility fallback"
        )
        private static List<DFSResult> runClassicDfs(ChainsSettingsDto cfg) {
            if (cfg == null) {
                return List.of();
            }
            DFSEngine dfsEngine = new DFSEngine(
                    DfsOutputs.noop(),
                    cfg.sinkSelected(),
                    cfg.sourceNull(),
                    cfg.maxDepth()
            );
            dfsEngine.setMaxLimit(Math.max(1, cfg.maxResultLimit()));
            dfsEngine.setMinEdgeConfidence(safe(cfg.minEdgeConfidence()).isEmpty() ? "low" : cfg.minEdgeConfidence());
            dfsEngine.setShowEdgeMeta(cfg.showEdgeMeta());
            dfsEngine.setSummaryEnabled(cfg.summaryEnabled());
            dfsEngine.setOnlyFromWeb(cfg.onlyFromWeb());
            dfsEngine.setBlacklist(parseBlacklist(cfg.blacklist()));
            dfsEngine.setSink(
                    normalizeClass(cfg.sinkClass()),
                    safe(cfg.sinkMethod()),
                    safe(cfg.sinkDesc())
            );
            dfsEngine.setSource(
                    normalizeClass(cfg.sourceClass()),
                    safe(cfg.sourceMethod()),
                    safe(cfg.sourceDesc())
            );
            dfsEngine.doAnalyze();
            List<DFSResult> resultList = dfsEngine.getResults();
            if (resultList == null || resultList.isEmpty()) {
                return List.of();
            }
            return resultList;
        }

        private static GraphDfsAttempt tryGraphDfs(ChainsSettingsDto cfg) {
            if (!isGraphChainsEnabled()) {
                return GraphDfsAttempt.notAttempted();
            }
            if (cfg == null || !cfg.sourceEnabled() || cfg.sourceNull()) {
                return GraphDfsAttempt.notAttempted();
            }
            String sourceRef = buildMethodRef(cfg.sourceClass(), cfg.sourceMethod(), cfg.sourceDesc());
            String sinkRef = buildMethodRef(cfg.sinkClass(), cfg.sinkMethod(), cfg.sinkDesc());
            if (sourceRef.isEmpty() || sinkRef.isEmpty()) {
                return GraphDfsAttempt.notAttempted();
            }
            try {
                QueryOptions options = toGraphQueryOptions(cfg);
                GraphSnapshot snapshot = new GraphStore().loadSnapshot();
                if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
                    return GraphDfsAttempt.of(List.of(), "graph_snapshot_empty");
                }
                QueryResult result = new ProcedureRegistry().execute(
                        "ja.path.from_to",
                        List.of(
                                sourceRef,
                                sinkRef,
                                String.valueOf(Math.max(1, cfg.maxDepth())),
                                String.valueOf(Math.max(1, cfg.maxResultLimit()))
                        ),
                        Map.of(),
                        options,
                        snapshot
                );
                List<DFSResult> dfs = toDfsResults(result, snapshot, cfg);
                return GraphDfsAttempt.of(dfs, summarizeWarnings(result));
            } catch (Exception ex) {
                return GraphDfsAttempt.of(List.of(), "graph_dfs_error:" + safe(ex.getMessage()));
            }
        }

        @CompatibilityCode(
                primary = "Graph Procedure ja.taint.track",
                reason = "Legacy taint analyzer path retained for compatibility fallback"
        )
        private static List<TaintResult> runTaintWithGraphFallback(ChainsSettingsDto cfg, List<DFSResult> dfsSnapshot) {
            GraphTaintAttempt graphAttempt = tryGraphTaint(cfg);
            if (graphAttempt.attempted && !graphAttempt.results.isEmpty()) {
                logger.info("runtime taint backend=graph chains={} detail={}",
                        graphAttempt.results.size(), safe(graphAttempt.detail));
                return graphAttempt.results;
            }
            if (graphAttempt.attempted) {
                logger.info("runtime taint backend fallback=classic detail={}", safe(graphAttempt.detail));
            }
            List<DFSResult> safeSnapshot = dfsSnapshot == null ? List.of() : dfsSnapshot;
            List<TaintResult> taintResult = TaintAnalyzer.analyze(
                    safeSnapshot,
                    null,
                    null,
                    null,
                    cfg == null ? null : cfg.taintSeedParam(),
                    cfg != null && cfg.taintSeedStrict()
            );
            if (taintResult == null || taintResult.isEmpty()) {
                return List.of();
            }
            return taintResult;
        }

        private static GraphTaintAttempt tryGraphTaint(ChainsSettingsDto cfg) {
            if (!isGraphChainsEnabled()) {
                return GraphTaintAttempt.notAttempted();
            }
            if (cfg == null || !cfg.sourceEnabled() || cfg.sourceNull()) {
                return GraphTaintAttempt.notAttempted();
            }
            String sourceClass = normalizeClass(cfg.sourceClass());
            String sourceMethod = safe(cfg.sourceMethod());
            String sourceDesc = safe(cfg.sourceDesc());
            String sinkClass = normalizeClass(cfg.sinkClass());
            String sinkMethod = safe(cfg.sinkMethod());
            String sinkDesc = safe(cfg.sinkDesc());
            if (sourceClass.isBlank() || sourceMethod.isBlank() || sourceDesc.isBlank()
                    || sinkClass.isBlank() || sinkMethod.isBlank() || sinkDesc.isBlank()) {
                return GraphTaintAttempt.notAttempted();
            }
            try {
                QueryOptions options = toGraphQueryOptions(cfg);
                GraphSnapshot snapshot = new GraphStore().loadSnapshot();
                if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
                    return GraphTaintAttempt.of(List.of(), "graph_snapshot_empty");
                }
                QueryResult result = new ProcedureRegistry().execute(
                        "ja.taint.track",
                        List.of(
                                sourceClass,
                                sourceMethod,
                                sourceDesc,
                                sinkClass,
                                sinkMethod,
                                sinkDesc,
                                String.valueOf(Math.max(1, cfg.maxDepth())),
                                String.valueOf(options.getMaxMs()),
                                String.valueOf(Math.max(1, cfg.maxResultLimit()))
                        ),
                        Map.of(),
                        options,
                        snapshot
                );
                List<TaintResult> taint = toTaintResults(result, snapshot, cfg);
                return GraphTaintAttempt.of(taint, summarizeWarnings(result));
            } catch (Exception ex) {
                return GraphTaintAttempt.of(List.of(), "graph_taint_error:" + safe(ex.getMessage()));
            }
        }

        private static QueryOptions toGraphQueryOptions(ChainsSettingsDto cfg) {
            int maxRows = Math.max(1, cfg == null ? 30 : cfg.maxResultLimit());
            int maxHops = Math.max(1, cfg == null ? 10 : cfg.maxDepth());
            boolean longChain = maxHops > 64;
            int maxMs = longChain ? 30_000 : 15_000;
            int maxPaths = Math.max(1, Math.min(5_000, maxRows));
            int expandBudget = longChain ? Math.max(300_000, maxHops * 8_000) : Math.max(150_000, maxHops * 4_000);
            int pathBudget = Math.max(maxPaths, maxRows * 8);
            return new QueryOptions(
                    maxRows,
                    maxMs,
                    maxHops,
                    maxPaths,
                    longChain ? QueryOptions.PROFILE_LONG_CHAIN : QueryOptions.PROFILE_DEFAULT,
                    expandBudget,
                    pathBudget,
                    longChain ? 64 : 128
            );
        }

        private static List<DFSResult> toDfsResults(QueryResult result, GraphSnapshot snapshot, ChainsSettingsDto cfg) {
            if (result == null || result.getRows() == null || result.getRows().isEmpty()) {
                return List.of();
            }
            MethodReference.Handle sourceHandle = toConfigHandle(cfg == null ? "" : cfg.sourceClass(),
                    cfg == null ? "" : cfg.sourceMethod(),
                    cfg == null ? "" : cfg.sourceDesc());
            MethodReference.Handle sinkHandle = toConfigHandle(cfg == null ? "" : cfg.sinkClass(),
                    cfg == null ? "" : cfg.sinkMethod(),
                    cfg == null ? "" : cfg.sinkDesc());
            int mode = resolveMode(cfg);
            String warningSummary = summarizeWarnings(result);

            List<DFSResult> out = new ArrayList<>();
            for (List<Object> row : result.getRows()) {
                if (row == null || row.isEmpty()) {
                    continue;
                }
                String nodeIdsText = valueAt(row, 2);
                String edgeIdsText = valueAt(row, 3);
                List<MethodReference.Handle> methods = parseMethodHandles(nodeIdsText, snapshot);
                if (methods.isEmpty()) {
                    if (sourceHandle != null) {
                        methods.add(sourceHandle);
                    }
                    if (sinkHandle != null && (methods.isEmpty() || !methods.get(methods.size() - 1).equals(sinkHandle))) {
                        methods.add(sinkHandle);
                    }
                }
                if (methods.isEmpty()) {
                    continue;
                }
                MethodReference.Handle source = sourceHandle != null ? sourceHandle : methods.get(0);
                MethodReference.Handle sink = sinkHandle != null ? sinkHandle : methods.get(methods.size() - 1);

                DFSResult dfs = new DFSResult();
                dfs.setMethodList(methods);
                dfs.setDepth(Math.max(0, toInt(valueAt(row, 1), methods.size() - 1)));
                dfs.setSource(source);
                dfs.setSink(sink);
                dfs.setMode(mode);
                dfs.setNodeCount(methods.size());
                dfs.setEdgeCount(countCsv(edgeIdsText));
                dfs.setPathCount(1);
                dfs.setElapsedMs(0L);
                dfs.setTruncated(result.isTruncated());
                if (result.isTruncated()) {
                    String reason = warningSummary.isBlank() ? "graph_result_truncated" : warningSummary;
                    dfs.setTruncateReason(reason);
                }
                String recommend = buildRecommend(valueAt(row, 4), valueAt(row, 5), valueAt(row, 6), warningSummary);
                dfs.setRecommend(recommend);
                out.add(dfs);
            }
            return out;
        }

        private static List<TaintResult> toTaintResults(QueryResult result, GraphSnapshot snapshot, ChainsSettingsDto cfg) {
            List<DFSResult> dfsResults = toDfsResults(result, snapshot, cfg);
            if (dfsResults.isEmpty()) {
                return List.of();
            }
            List<TaintResult> out = new ArrayList<>();
            List<List<Object>> rows = result.getRows();
            int limit = Math.min(dfsResults.size(), rows == null ? 0 : rows.size());
            for (int i = 0; i < limit; i++) {
                DFSResult dfs = dfsResults.get(i);
                List<Object> row = rows.get(i);
                String confidence = valueAt(row, 5);
                String evidence = valueAt(row, 6);
                TaintResult taint = new TaintResult();
                taint.setDfsResult(dfs);
                taint.setSuccess(true);
                taint.setLowConfidence("low".equalsIgnoreCase(confidence));
                taint.setTaintText(evidence.isBlank() ? "graph_taint_chain" : evidence);
                out.add(taint);
            }
            return out;
        }

        private static List<MethodReference.Handle> parseMethodHandles(String nodeIdsCsv, GraphSnapshot snapshot) {
            if (snapshot == null || nodeIdsCsv == null || nodeIdsCsv.isBlank()) {
                return List.of();
            }
            String[] parts = nodeIdsCsv.split(",");
            List<MethodReference.Handle> out = new ArrayList<>(parts.length);
            MethodReference.Handle last = null;
            for (String part : parts) {
                long nodeId;
                try {
                    nodeId = Long.parseLong(safe(part));
                } catch (Exception ignored) {
                    continue;
                }
                if (nodeId <= 0L) {
                    continue;
                }
                GraphNode node = snapshot.getNode(nodeId);
                MethodReference.Handle handle = toHandle(node);
                if (handle == null) {
                    continue;
                }
                if (last != null && last.equals(handle)) {
                    continue;
                }
                out.add(handle);
                last = handle;
            }
            return out;
        }

        private static MethodReference.Handle toHandle(GraphNode node) {
            if (node == null) {
                return null;
            }
            String clazz = normalizeClass(node.getClassName());
            String method = safe(node.getMethodName());
            String desc = safe(node.getMethodDesc());
            if (clazz.isBlank() || method.isBlank() || desc.isBlank()) {
                return null;
            }
            return new MethodReference.Handle(new ClassReference.Handle(clazz, node.getJarId()), method, desc);
        }

        private static MethodReference.Handle toConfigHandle(String className, String methodName, String methodDesc) {
            String clazz = normalizeClass(className);
            String method = safe(methodName);
            String desc = safe(methodDesc);
            if (clazz.isBlank() || method.isBlank() || desc.isBlank()) {
                return null;
            }
            return new MethodReference.Handle(new ClassReference.Handle(clazz, -1), method, desc);
        }

        private static String buildMethodRef(String className, String methodName, String methodDesc) {
            String clazz = normalizeClass(className);
            String method = safe(methodName);
            String desc = safe(methodDesc);
            if (clazz.isBlank() || method.isBlank() || desc.isBlank()) {
                return "";
            }
            return clazz + "#" + method + "#" + desc;
        }

        private static int resolveMode(ChainsSettingsDto cfg) {
            if (cfg == null) {
                return DFSResult.FROM_SOURCE_TO_SINK;
            }
            if (cfg.sourceNull()) {
                return DFSResult.FROM_SOURCE_TO_ALL;
            }
            if (cfg.sinkSelected()) {
                return DFSResult.FROM_SINK_TO_SOURCE;
            }
            return DFSResult.FROM_SOURCE_TO_SINK;
        }

        private static String summarizeWarnings(QueryResult result) {
            if (result == null || result.getWarnings() == null || result.getWarnings().isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (String warning : result.getWarnings()) {
                String value = safe(warning);
                if (value.isBlank()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(value);
            }
            return sb.toString();
        }

        private static String valueAt(List<Object> row, int index) {
            if (row == null || index < 0 || index >= row.size()) {
                return "";
            }
            Object value = row.get(index);
            return value == null ? "" : safe(String.valueOf(value));
        }

        private static int toInt(String raw, int def) {
            String value = safe(raw);
            if (value.isBlank()) {
                return def;
            }
            try {
                return Integer.parseInt(value);
            } catch (Exception ignored) {
                return def;
            }
        }

        private static int countCsv(String csv) {
            String value = safe(csv);
            if (value.isBlank()) {
                return 0;
            }
            String[] parts = value.split(",");
            int count = 0;
            for (String part : parts) {
                if (!safe(part).isBlank()) {
                    count++;
                }
            }
            return count;
        }

        private static String buildRecommend(String score, String confidence, String evidence, String warnings) {
            StringBuilder sb = new StringBuilder();
            String c = safe(confidence);
            if (!c.isBlank()) {
                sb.append("confidence=").append(c);
            }
            String s = safe(score);
            if (!s.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("score=").append(s);
            }
            String e = safe(evidence);
            if (!e.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("evidence=").append(e);
            }
            String w = safe(warnings);
            if (!w.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("warnings=").append(w);
            }
            return sb.toString();
        }

        private static boolean isGraphChainsEnabled() {
            String raw = System.getProperty("jar.analyzer.gui.chains.graph.enable");
            if (raw == null || raw.isBlank()) {
                return true;
            }
            return !"false".equalsIgnoreCase(raw.strip());
        }

        private record GraphDfsAttempt(boolean attempted, List<DFSResult> results, String detail) {
            private static GraphDfsAttempt notAttempted() {
                return new GraphDfsAttempt(false, List.of(), "");
            }

            private static GraphDfsAttempt of(List<DFSResult> results, String detail) {
                return new GraphDfsAttempt(true, results == null ? List.of() : results, safe(detail));
            }
        }

        private record GraphTaintAttempt(boolean attempted, List<TaintResult> results, String detail) {
            private static GraphTaintAttempt notAttempted() {
                return new GraphTaintAttempt(false, List.of(), "");
            }

            private static GraphTaintAttempt of(List<TaintResult> results, String detail) {
                return new GraphTaintAttempt(true, results == null ? List.of() : results, safe(detail));
            }
        }

        @Override
        public void clearResults() {
            TaintCache.dfsCache.clear();
            TaintCache.cache.clear();
        }

        @Override
        public void openAdvanceSettings() {
            emitToolingWindow(ToolingWindowAction.CHAINS_ADVANCED);
        }

        @Override
        public void setSource(String className, String methodName, String methodDesc) {
            String c = normalizeClass(className);
            String m = safe(methodName).trim();
            if (c.isBlank() || m.isBlank()) {
                return;
            }
            ChainsSettingsDto cfg = STATE.chainsSettings;
            STATE.chainsSettings = new ChainsSettingsDto(
                    cfg.sinkSelected(),
                    true,
                    cfg.sinkClass(),
                    cfg.sinkMethod(),
                    cfg.sinkDesc(),
                    c,
                    m,
                    safe(methodDesc),
                    false,
                    true,
                    cfg.maxDepth(),
                    cfg.onlyFromWeb(),
                    cfg.taintEnabled(),
                    cfg.blacklist(),
                    cfg.minEdgeConfidence(),
                    cfg.showEdgeMeta(),
                    cfg.summaryEnabled(),
                    cfg.taintSeedParam(),
                    cfg.taintSeedStrict(),
                    cfg.maxResultLimit()
            );
        }

        @Override
        public void setSink(String className, String methodName, String methodDesc) {
            String c = normalizeClass(className);
            String m = safe(methodName).trim();
            if (c.isBlank() || m.isBlank()) {
                return;
            }
            ChainsSettingsDto cfg = STATE.chainsSettings;
            STATE.chainsSettings = new ChainsSettingsDto(
                    true,
                    cfg.sourceSelected(),
                    c,
                    m,
                    safe(methodDesc),
                    cfg.sourceClass(),
                    cfg.sourceMethod(),
                    cfg.sourceDesc(),
                    cfg.sourceNull(),
                    cfg.sourceEnabled(),
                    cfg.maxDepth(),
                    cfg.onlyFromWeb(),
                    cfg.taintEnabled(),
                    cfg.blacklist(),
                    cfg.minEdgeConfidence(),
                    cfg.showEdgeMeta(),
                    cfg.summaryEnabled(),
                    cfg.taintSeedParam(),
                    cfg.taintSeedStrict(),
                    cfg.maxResultLimit()
            );
        }
    }

    private static final class DefaultApiMcpFacade implements ApiMcpFacade {
        @Override
        public ApiInfoDto apiInfo() {
            ServerConfig cfg = GlobalOptions.getServerConfig();
            if (cfg == null) {
                cfg = new ServerConfig();
            }
            return new ApiInfoDto(
                    safe(cfg.getBind()).isEmpty() ? "0.0.0.0" : cfg.getBind(),
                    cfg.isAuth(),
                    cfg.getPort(),
                    mask(cfg.getToken())
            );
        }

        @Override
        public McpConfigDto currentConfig() {
            ConfigFile cfg = ensureConfig();
            McpManager manager = McpManager.get();
            return new McpConfigDto(
                    safe(cfg.getMcpBind()).isEmpty() ? "0.0.0.0" : cfg.getMcpBind(),
                    cfg.isMcpAuth(),
                    safe(cfg.getMcpToken()).isEmpty() ? "JAR-ANALYZER-MCP-TOKEN" : cfg.getMcpToken(),
                    List.of(
                            line(McpLineKey.AUDIT_FAST, cfg.isMcpAuditFastEnabled(), cfg.getMcpAuditFastPort(), manager.isRunning(McpLine.AUDIT_FAST)),
                            line(McpLineKey.GRAPH_LITE, cfg.isMcpGraphLiteEnabled(), cfg.getMcpGraphLitePort(), manager.isRunning(McpLine.GRAPH_LITE)),
                            line(McpLineKey.DFS_TAINT, cfg.isMcpDfsTaintEnabled(), cfg.getMcpDfsTaintPort(), manager.isRunning(McpLine.DFS_TAINT)),
                            line(McpLineKey.SCA_LEAK, cfg.isMcpScaLeakEnabled(), cfg.getMcpScaLeakPort(), manager.isRunning(McpLine.SCA_LEAK)),
                            line(McpLineKey.VUL_RULES, cfg.isMcpVulRulesEnabled(), cfg.getMcpVulRulesPort(), manager.isRunning(McpLine.VUL_RULES)),
                            line(McpLineKey.REPORT, cfg.isMcpReportEnabled(), cfg.getMcpReportPort(), manager.isRunning(McpLine.REPORT))
                    ),
                    cfg.isMcpReportWebEnabled(),
                    safe(cfg.getMcpReportWebHost()).isEmpty() ? "127.0.0.1" : cfg.getMcpReportWebHost(),
                    cfg.getMcpReportWebPort(),
                    manager.isReportWebRunning()
            );
        }

        @Override
        public List<String> applyAndRestart(McpConfigDto config) {
            ConfigFile cfg = ensureConfig();
            Map<McpLineKey, McpLineConfigDto> lines = new HashMap<>();
            if (config.lines() != null) {
                for (McpLineConfigDto line : config.lines()) {
                    lines.put(line.key(), line);
                }
            }
            cfg.setMcpBind(config.bind());
            cfg.setMcpAuth(config.authEnabled());
            cfg.setMcpToken(config.token());
            cfg.setMcpAuditFastEnabled(getEnabled(lines, McpLineKey.AUDIT_FAST, cfg.isMcpAuditFastEnabled()));
            cfg.setMcpAuditFastPort(normalizePort(getPort(lines, McpLineKey.AUDIT_FAST, cfg.getMcpAuditFastPort()), cfg.getMcpAuditFastPort()));
            cfg.setMcpGraphLiteEnabled(getEnabled(lines, McpLineKey.GRAPH_LITE, cfg.isMcpGraphLiteEnabled()));
            cfg.setMcpGraphLitePort(normalizePort(getPort(lines, McpLineKey.GRAPH_LITE, cfg.getMcpGraphLitePort()), cfg.getMcpGraphLitePort()));
            cfg.setMcpDfsTaintEnabled(getEnabled(lines, McpLineKey.DFS_TAINT, cfg.isMcpDfsTaintEnabled()));
            cfg.setMcpDfsTaintPort(normalizePort(getPort(lines, McpLineKey.DFS_TAINT, cfg.getMcpDfsTaintPort()), cfg.getMcpDfsTaintPort()));
            cfg.setMcpScaLeakEnabled(getEnabled(lines, McpLineKey.SCA_LEAK, cfg.isMcpScaLeakEnabled()));
            cfg.setMcpScaLeakPort(normalizePort(getPort(lines, McpLineKey.SCA_LEAK, cfg.getMcpScaLeakPort()), cfg.getMcpScaLeakPort()));
            cfg.setMcpVulRulesEnabled(getEnabled(lines, McpLineKey.VUL_RULES, cfg.isMcpVulRulesEnabled()));
            cfg.setMcpVulRulesPort(normalizePort(getPort(lines, McpLineKey.VUL_RULES, cfg.getMcpVulRulesPort()), cfg.getMcpVulRulesPort()));
            cfg.setMcpReportEnabled(getEnabled(lines, McpLineKey.REPORT, cfg.isMcpReportEnabled()));
            cfg.setMcpReportPort(normalizePort(getPort(lines, McpLineKey.REPORT, cfg.getMcpReportPort()), cfg.getMcpReportPort()));
            cfg.setMcpReportWebEnabled(config.reportWebEnabled());
            cfg.setMcpReportWebHost(config.reportWebHost());
            cfg.setMcpReportWebPort(normalizePort(config.reportWebPort(), cfg.getMcpReportWebPort()));
            ConfigEngine.saveConfig(cfg);
            return McpManager.get().restartAll(
                    readLineConfigs(cfg),
                    readReportWebConfig(cfg),
                    safeApiConfig()
            );
        }

        @Override
        public List<String> startConfigured() {
            ConfigFile cfg = ensureConfig();
            return McpManager.get().restartAll(
                    readLineConfigs(cfg),
                    readReportWebConfig(cfg),
                    safeApiConfig()
            );
        }

        @Override
        public void stopAll() {
            McpManager.get().stopAll();
        }

        @Override
        public void openApiDoc() {
            openDoc("doc/README-api.md");
        }

        @Override
        public void openMcpDoc() {
            openDoc("doc/mcp/README.md");
        }

        @Override
        public void openN8nDoc() {
            openDoc("doc/n8n/README.md");
        }

        @Override
        public void openReportWeb(String host, int port) {
            String safeHost = safe(host).isEmpty() ? "127.0.0.1" : host.trim();
            int safePort = normalizePort(port, 20080);
            try {
                Desktop.getDesktop().browse(URI.create("http://" + safeHost + ":" + safePort + "/"));
            } catch (Throwable ignored) {
            }
        }

        private McpLineConfigDto line(McpLineKey key, boolean enabled, int port, boolean running) {
            return new McpLineConfigDto(key, enabled, port, running);
        }

        private void openDoc(String path) {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/jar-analyzer/jar-analyzer/blob/master/" + path));
            } catch (Throwable ignored) {
            }
        }

        private ConfigFile ensureConfig() {
            ConfigFile cfg = ConfigEngine.parseConfig();
            return cfg == null ? new ConfigFile() : cfg;
        }
    }

    private static final class DefaultEditorFacade implements EditorFacade {
        @Override
        public EditorDocumentDto current() {
            return STATE.editorDocument;
        }

        @Override
        public void openClass(String className, Integer jarId) {
            openClassInternal(className, jarId, true);
        }

        @Override
        public void openMethod(String className, String methodName, String methodDesc, Integer jarId) {
            openMethodInternal(className, methodName, methodDesc, jarId, true);
        }

        @Override
        public boolean goPrev() {
            NavState target;
            synchronized (STATE.navLock) {
                if (STATE.navIndex <= 0 || STATE.navStates.isEmpty()) {
                    return false;
                }
                STATE.navIndex--;
                target = STATE.navStates.get(STATE.navIndex);
                STATE.navReplaying = true;
            }
            try {
                replayNavigation(target);
                return true;
            } finally {
                synchronized (STATE.navLock) {
                    STATE.navReplaying = false;
                }
            }
        }

        @Override
        public boolean goNext() {
            NavState target;
            synchronized (STATE.navLock) {
                if (STATE.navStates.isEmpty() || STATE.navIndex >= STATE.navStates.size() - 1) {
                    return false;
                }
                STATE.navIndex++;
                target = STATE.navStates.get(STATE.navIndex);
                STATE.navReplaying = true;
            }
            try {
                replayNavigation(target);
                return true;
            } finally {
                synchronized (STATE.navLock) {
                    STATE.navReplaying = false;
                }
            }
        }

        @Override
        public boolean addCurrentToFavorites() {
            CoreEngine engine = EngineContext.getEngine();
            MethodNavDto current = STATE.currentMethod;
            if (engine == null || !engine.isEnabled() || current == null) {
                return false;
            }
            if (safe(current.className()).isBlank() || safe(current.methodName()).isBlank()) {
                return false;
            }
            MethodResult fav = new MethodResult();
            fav.setClassName(normalizeClass(current.className()));
            fav.setMethodName(safe(current.methodName()));
            fav.setMethodDesc(safe(current.methodDesc()));
            fav.setJarId(current.jarId() <= 0 ? null : current.jarId());
            String jarName = safe(current.jarName());
            if (jarName.isBlank()) {
                try {
                    jarName = safe(engine.getJarByClass(normalizeClass(current.className())));
                } catch (Throwable ignored) {
                }
            }
            fav.setJarName(jarName);
            try {
                engine.addFav(fav);
                STATE.editorDocument = new EditorDocumentDto(
                        STATE.editorDocument.className(),
                        STATE.editorDocument.jarName(),
                        STATE.editorDocument.jarId(),
                        STATE.editorDocument.methodName(),
                        STATE.editorDocument.methodDesc(),
                        STATE.editorDocument.content(),
                        STATE.editorDocument.caretOffset(),
                        "added to favorites"
                );
                return true;
            } catch (Throwable ex) {
                logger.warn("add favorite failed: {}", ex.toString());
                return false;
            }
        }

        private void replayNavigation(NavState target) {
            if (target == null) {
                return;
            }
            if (safe(target.methodName()).isBlank()) {
                openClassInternal(target.className(), target.jarId(), false);
            } else {
                openMethodInternal(
                        target.className(),
                        target.methodName(),
                        target.methodDesc(),
                        target.jarId(),
                        false
                );
            }
        }

        private void openClassInternal(String className, Integer jarId, boolean recordNav) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                STATE.editorDocument = new EditorDocumentDto(
                        safe(className),
                        "",
                        null,
                        "",
                        "",
                        "",
                        0,
                        "engine is not ready"
                );
                return;
            }
            String normalizedClass = normalizeClass(className);
            Integer normalizedJarId = normalizeJarId(jarId);
            String absPath;
            if (normalizedJarId == null) {
                absPath = engine.getAbsPath(normalizedClass);
            } else {
                absPath = engine.getAbsPath(normalizedClass, normalizedJarId);
            }
            String content = "";
            String status = "ready";
            if (safe(absPath).isEmpty()) {
                status = "class bytecode path not found";
            } else {
                try {
                    content = DecompileDispatcher.decompile(Paths.get(absPath), DecompileDispatcher.resolvePreferred());
                    if (content == null) {
                        status = "decompile output is empty";
                        content = "";
                    }
                } catch (Throwable ex) {
                    status = "decompile failed: " + ex.getMessage();
                }
            }
            String jarName = "";
            try {
                if (normalizedJarId != null) {
                    jarName = safe(engine.getJarNameById(normalizedJarId));
                } else {
                    jarName = safe(engine.getJarByClass(normalizedClass));
                }
            } catch (Throwable ignored) {
            }
            STATE.currentClass = normalizedClass;
            STATE.currentJar = jarName;
            STATE.currentMethod = null;
            STATE.editorDocument = new EditorDocumentDto(
                    normalizedClass,
                    jarName,
                    normalizedJarId,
                    "",
                    "",
                    safe(content),
                    0,
                    status
            );
            if (recordNav) {
                pushNavigation(normalizedClass, "", "", normalizedJarId);
            }
        }

        private void openMethodInternal(
                String className,
                String methodName,
                String methodDesc,
                Integer jarId,
                boolean recordNav
        ) {
            Integer normalizedJarId = normalizeJarId(jarId);
            openClassInternal(className, normalizedJarId, false);
            String normalizedClass = normalizeClass(className);
            String jarName = STATE.editorDocument.jarName();
            int caretOffset = STATE.editorDocument.caretOffset();
            try {
                DecompiledMethodLocator.JumpTarget jump = DecompiledMethodLocator.locate(
                        STATE.editorDocument.content(),
                        normalizedClass,
                        safe(methodName),
                        safe(methodDesc),
                        null
                );
                if (jump != null) {
                    caretOffset = Math.max(0, jump.startOffset);
                }
            } catch (Throwable ex) {
                logger.debug("editor locate method failed: {}", ex.toString());
            }
            STATE.currentMethod = new MethodNavDto(
                    normalizedClass,
                    safe(methodName),
                    safe(methodDesc),
                    jarName,
                    normalizedJarId == null ? 0 : normalizedJarId
            );
            STATE.currentClass = normalizedClass;
            STATE.editorDocument = new EditorDocumentDto(
                    STATE.editorDocument.className(),
                    STATE.editorDocument.jarName(),
                    STATE.editorDocument.jarId(),
                    safe(methodName),
                    safe(methodDesc),
                    STATE.editorDocument.content(),
                    caretOffset,
                    "method opened"
            );
            CoreEngine engine = EngineContext.getEngine();
            if (engine != null && engine.isEnabled()) {
                MethodResult history = new MethodResult();
                history.setClassName(normalizedClass);
                history.setMethodName(safe(methodName));
                history.setMethodDesc(safe(methodDesc));
                history.setJarName(jarName);
                history.setJarId(normalizedJarId == null ? 0 : normalizedJarId);
                engine.insertHistory(history);
            }
            if (recordNav) {
                pushNavigation(normalizedClass, safe(methodName), safe(methodDesc), normalizedJarId);
            }
        }

        private Integer normalizeJarId(Integer jarId) {
            if (jarId == null || jarId <= 0) {
                return null;
            }
            return jarId;
        }

        private void pushNavigation(String className, String methodName, String methodDesc, Integer jarId) {
            NavState next = new NavState(
                    normalizeClass(className),
                    safe(methodName),
                    safe(methodDesc),
                    normalizeJarId(jarId)
            );
            synchronized (STATE.navLock) {
                if (STATE.navReplaying) {
                    return;
                }
                if (STATE.navIndex >= 0 && STATE.navIndex < STATE.navStates.size()) {
                    NavState current = STATE.navStates.get(STATE.navIndex);
                    if (sameNavigation(current, next)) {
                        return;
                    }
                }
                if (STATE.navIndex < STATE.navStates.size() - 1) {
                    STATE.navStates.subList(STATE.navIndex + 1, STATE.navStates.size()).clear();
                }
                STATE.navStates.add(next);
                final int maxStates = 400;
                if (STATE.navStates.size() > maxStates) {
                    int overflow = STATE.navStates.size() - maxStates;
                    STATE.navStates.subList(0, overflow).clear();
                }
                STATE.navIndex = STATE.navStates.size() - 1;
            }
        }

        private boolean sameNavigation(NavState a, NavState b) {
            if (a == null || b == null) {
                return false;
            }
            String aClass = safe(a.className());
            String bClass = safe(b.className());
            String aMethod = safe(a.methodName());
            String bMethod = safe(b.methodName());
            String aDesc = safe(a.methodDesc());
            String bDesc = safe(b.methodDesc());
            Integer aJar = normalizeJarId(a.jarId());
            Integer bJar = normalizeJarId(b.jarId());
            return aClass.equals(bClass)
                    && aMethod.equals(bMethod)
                    && aDesc.equals(bDesc)
                    && ((aJar == null && bJar == null) || (aJar != null && aJar.equals(bJar)));
        }

        @Override
        public void searchInCurrent(String keyword, boolean forward) {
            EditorDocumentDto doc = STATE.editorDocument;
            String text = safe(doc.content());
            String kw = safe(keyword);
            if (kw.isEmpty() || text.isEmpty()) {
                return;
            }
            int cursor = Math.max(0, doc.caretOffset());
            int idx;
            if (forward) {
                idx = text.indexOf(kw, cursor + 1);
                if (idx < 0) {
                    idx = text.indexOf(kw);
                }
            } else {
                idx = text.lastIndexOf(kw, Math.max(0, cursor - 1));
                if (idx < 0) {
                    idx = text.lastIndexOf(kw);
                }
            }
            if (idx < 0) {
                STATE.editorDocument = new EditorDocumentDto(
                        doc.className(),
                        doc.jarName(),
                        doc.jarId(),
                        doc.methodName(),
                        doc.methodDesc(),
                        doc.content(),
                        doc.caretOffset(),
                        "keyword not found"
                );
                return;
            }
            STATE.editorDocument = new EditorDocumentDto(
                    doc.className(),
                    doc.jarName(),
                    doc.jarId(),
                    doc.methodName(),
                    doc.methodDesc(),
                    doc.content(),
                    idx,
                    "keyword found at: " + idx
            );
        }

        @Override
        public void applyEditorText(String content, int caretOffset) {
            EditorDocumentDto doc = STATE.editorDocument;
            STATE.editorDocument = new EditorDocumentDto(
                    doc.className(),
                    doc.jarName(),
                    doc.jarId(),
                    doc.methodName(),
                    doc.methodDesc(),
                    safe(content),
                    Math.max(0, caretOffset),
                    "editor updated"
            );
        }
    }

    private static final class DefaultProjectTreeFacade implements ProjectTreeFacade {
        private static final int RESOURCE_TREE_FETCH_LIMIT = 200_000;
        private static final String CATEGORY_INPUT = "cat:input";
        private static final String CATEGORY_SOURCE = "cat:source";
        private static final String CATEGORY_RESOURCE = "cat:resource";
        private static final String CATEGORY_DEPENDENCY = "cat:dependency";

        @Override
        public List<TreeNodeDto> snapshot() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return List.of();
            }
            return buildTree(null);
        }

        @Override
        public List<TreeNodeDto> search(String keyword) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return List.of();
            }
            String key = safe(keyword).trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                return buildTree(null);
            }
            return buildTree(key);
        }

        @Override
        public void openNode(String value) {
            String raw = safe(value).trim();
            if (raw.isEmpty()) {
                return;
            }
            if (raw.startsWith("res:")) {
                openResourceNode(raw);
                return;
            }
            if (raw.startsWith("jarpath:")) {
                openJarPathNode(raw);
                return;
            }
            if (raw.startsWith("cls:")) {
                raw = raw.substring(4);
            } else {
                return;
            }
            int split = raw.lastIndexOf('|');
            Integer jarId = null;
            String className = raw;
            if (split > 0 && split < raw.length() - 1) {
                className = raw.substring(0, split);
                try {
                    jarId = Integer.parseInt(raw.substring(split + 1));
                } catch (NumberFormatException ignored) {
                    jarId = null;
                }
            }
            className = normalizeClass(className);
            if (className.endsWith(".class")) {
                className = className.substring(0, className.length() - 6);
            }
            if (className.isEmpty()) {
                return;
            }
            RuntimeFacades.editor().openClass(className, jarId);
        }

        private void openResourceNode(String rawValue) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return;
            }
            int rid;
            try {
                rid = Integer.parseInt(rawValue.substring(4).trim());
            } catch (Throwable ignored) {
                return;
            }
            ResourceEntity resource = engine.getResourceById(rid);
            if (resource == null) {
                emitTextWindow("Resource", "resource not found: " + rid);
                return;
            }
            Path filePath;
            try {
                filePath = Paths.get(safe(resource.getPathStr()));
            } catch (Throwable ex) {
                emitTextWindow("Resource", "invalid resource path: " + ex.getMessage());
                return;
            }
            if (Files.notExists(filePath)) {
                emitTextWindow("Resource", "resource file not found: " + filePath);
                return;
            }
            String title = "Resource: " + safe(resource.getResourcePath());
            if (resource.getIsText() == 1) {
                emitToolingWindow(new ToolingWindowRequest(
                        ToolingWindowAction.TEXT_VIEWER,
                        new ToolingWindowPayload.TextPayload(title, renderTextResource(resource, filePath))
                ));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("resource: ").append(safe(resource.getResourcePath())).append('\n');
                sb.append("jar: ").append(safe(resource.getJarName())).append('\n');
                sb.append("jar id: ").append(resource.getJarId()).append('\n');
                sb.append("size: ").append(resource.getFileSize()).append('\n');
                sb.append("text: ").append(resource.getIsText() == 1).append('\n');
                sb.append("file: ").append(filePath.toAbsolutePath()).append('\n');
                emitToolingWindow(new ToolingWindowRequest(
                        ToolingWindowAction.TEXT_VIEWER,
                        new ToolingWindowPayload.TextPayload(title, sb.toString())
                ));
            }
        }

        private String renderTextResource(ResourceEntity resource, Path path) {
            final int maxBytes = 512 * 1024;
            try {
                long size = Files.size(path);
                byte[] bytes;
                try (InputStream input = Files.newInputStream(path)) {
                    bytes = input.readNBytes(maxBytes + 1);
                }
                boolean truncated = bytes.length > maxBytes;
                int len = truncated ? maxBytes : bytes.length;
                String body = new String(bytes, 0, len, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();
                sb.append("// resource: ").append(safe(resource.getResourcePath())).append('\n');
                sb.append("// jar: ").append(safe(resource.getJarName())).append('\n');
                sb.append("// size: ").append(size).append('\n');
                if (truncated) {
                    sb.append("// preview truncated to ").append(maxBytes).append(" bytes").append('\n');
                }
                sb.append('\n').append(body);
                return sb.toString();
            } catch (Throwable ex) {
                return "read resource failed: " + ex.getMessage();
            }
        }

        private void openJarPathNode(String rawValue) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return;
            }
            int jarId;
            try {
                jarId = Integer.parseInt(rawValue.substring("jarpath:".length()).trim());
            } catch (Throwable ignored) {
                return;
            }
            JarEntity matched = null;
            List<JarEntity> jars = engine.getJarsMeta();
            if (jars != null) {
                for (JarEntity jar : jars) {
                    if (jar != null && jar.getJid() == jarId) {
                        matched = jar;
                        break;
                    }
                }
            }
            if (matched == null) {
                emitTextWindow("Dependency", "jar not found: " + jarId);
                return;
            }
            String absPath = safe(matched.getJarAbsPath());
            Path path = absPath.isBlank() ? null : Paths.get(absPath);
            StringBuilder sb = new StringBuilder();
            sb.append("jar: ").append(safe(matched.getJarName())).append('\n');
            sb.append("jar id: ").append(matched.getJid()).append('\n');
            sb.append("path: ").append(absPath).append('\n');
            if (path != null) {
                sb.append("exists: ").append(Files.exists(path)).append('\n');
            }
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.TEXT_VIEWER,
                    new ToolingWindowPayload.TextPayload("Dependency Path", sb.toString())
            ));
        }

        @Override
        public void refresh() {
            ClassIndex.refresh();
        }

        private List<TreeNodeDto> buildTree(String filterKeywordLower) {
            List<ClassFileEntity> classRows = loadClassFiles();
            List<ResourceEntity> resourceRows = loadResources();
            List<JarEntity> jarRows = loadJarMeta();
            BuildSettingsDto settings = STATE.buildSettings;
            boolean hasInput = settings != null
                    && (!safe(settings.inputPath()).isBlank() || !safe(settings.runtimePath()).isBlank());
            if (classRows.isEmpty() && resourceRows.isEmpty() && jarRows.isEmpty() && !hasInput) {
                return List.of();
            }

            Map<Integer, String> jarNameById = new HashMap<>();
            for (JarEntity row : jarRows) {
                if (row == null) {
                    continue;
                }
                String name = safe(row.getJarName());
                if (name.isBlank()) {
                    continue;
                }
                jarNameById.put(row.getJid(), name);
            }

            List<TreeNodeDto> out = new ArrayList<>(4);
            out.add(buildInputCategory(settings, filterKeywordLower));
            out.add(buildSourceCategory(classRows, jarNameById, filterKeywordLower));
            out.add(buildResourceCategory(resourceRows, jarNameById, filterKeywordLower));
            out.add(buildDependencyCategory(jarRows, filterKeywordLower));
            return out;
        }

        private List<ClassFileEntity> loadClassFiles() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                ClassFileMapper mapper = session.getMapper(ClassFileMapper.class);
                List<ClassFileEntity> rows = mapper.selectAllClassPaths();
                if (rows == null || rows.isEmpty()) {
                    return List.of();
                }
                return rows;
            } catch (Exception ex) {
                logger.warn("load class files failed: {}", ex.toString());
                return List.of();
            }
        }

        private List<ResourceEntity> loadResources() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                ResourceMapper mapper = session.getMapper(ResourceMapper.class);
                int total = mapper.selectCount(null, null);
                if (total <= 0) {
                    return List.of();
                }
                int limit = Math.min(total, RESOURCE_TREE_FETCH_LIMIT);
                List<ResourceEntity> rows = mapper.selectResources(null, null, 0, limit);
                if (rows == null || rows.isEmpty()) {
                    return List.of();
                }
                if (total > limit) {
                    logger.warn("resource tree truncated: {} > {}", total, limit);
                }
                return rows;
            } catch (Exception ex) {
                logger.warn("load resources failed: {}", ex.toString());
                return List.of();
            }
        }

        private List<JarEntity> loadJarMeta() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                JarMapper mapper = session.getMapper(JarMapper.class);
                List<JarEntity> rows = mapper.selectAllJarMeta();
                if (rows == null || rows.isEmpty()) {
                    return List.of();
                }
                return rows;
            } catch (Exception ex) {
                logger.warn("load jar meta failed: {}", ex.toString());
                return List.of();
            }
        }

        private TreeNodeDto buildInputCategory(BuildSettingsDto settings, String filterKeywordLower) {
            List<TreeNodeDto> children = new ArrayList<>();
            String inputPath = settings == null ? "" : safe(settings.inputPath()).trim();
            String runtimePath = settings == null ? "" : safe(settings.runtimePath()).trim();
            if (!inputPath.isBlank() && matchesFilter(filterKeywordLower, inputPath)) {
                children.add(new TreeNodeDto("输入: " + inputPath, "input:main", false, List.of()));
            }
            if (!runtimePath.isBlank() && matchesFilter(filterKeywordLower, runtimePath)) {
                children.add(new TreeNodeDto("运行时: " + runtimePath, "input:runtime", false, List.of()));
            }
            sortNodes(children);
            return new TreeNodeDto("输入", CATEGORY_INPUT, true, children);
        }

        private TreeNodeDto buildSourceCategory(
                List<ClassFileEntity> rows,
                Map<Integer, String> jarNameById,
                String filterKeywordLower
        ) {
            MutableTreeNode root = new MutableTreeNode("", "source:root", true);
            boolean groupByJar = STATE.groupTreeByJar;
            for (ClassFileEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String normalized = normalizeClassName(row.getClassName());
                if (normalized == null) {
                    continue;
                }
                if (filterKeywordLower != null && !normalized.toLowerCase(Locale.ROOT).contains(filterKeywordLower)) {
                    continue;
                }
                int jarId = row.getJarId() == null ? 0 : row.getJarId();
                String jarName = resolveJarName(jarId, row.getJarName(), jarNameById);
                MutableTreeNode cursor = groupByJar
                        ? root.children.computeIfAbsent(
                        "srcjar:" + jarName + "|" + jarId,
                        key -> new MutableTreeNode(jarName, "srcjar:" + jarName + "|" + jarId, true)
                )
                        : root;

                String[] parts = normalized.split("/");
                StringBuilder packagePath = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (part == null || part.isBlank()) {
                        continue;
                    }
                    boolean leaf = i == parts.length - 1;
                    if (leaf) {
                        String label = part + ".class";
                        if (!groupByJar) {
                            label = label + " [" + jarName + "]";
                        }
                        String value = "cls:" + normalized + "|" + jarId;
                        String leafKey = "srccls:" + normalized + "|" + jarId;
                        String finalLabel = label;
                        String finalValue = value;
                        cursor.children.computeIfAbsent(leafKey,
                                key -> new MutableTreeNode(finalLabel, finalValue, false));
                    } else {
                        if (packagePath.length() > 0) {
                            packagePath.append('/');
                        }
                        packagePath.append(part);
                        String dirKey = "srcpkg:" + (groupByJar ? jarId + ":" : "")
                                + packagePath.toString().replace('/', '.');
                        cursor = cursor.children.computeIfAbsent(
                                dirKey,
                                key -> new MutableTreeNode(part, dirKey, true)
                        );
                    }
                }
            }
            List<TreeNodeDto> out = new ArrayList<>();
            for (MutableTreeNode node : root.children.values()) {
                out.add(node.freeze());
            }
            out.sort(Comparator.comparing(TreeNodeDto::label));
            if (STATE.mergePackageRoot) {
                if (groupByJar) {
                    List<TreeNodeDto> merged = new ArrayList<>();
                    for (TreeNodeDto node : out) {
                        if (node == null || !node.directory()) {
                            continue;
                        }
                        merged.add(new TreeNodeDto(
                                node.label(),
                                node.value(),
                                true,
                                mergeNodeList(node.children())
                        ));
                    }
                    out = merged;
                } else {
                    out = mergeNodeList(out);
                }
            }
            return new TreeNodeDto("源代码", CATEGORY_SOURCE, true, out);
        }

        private TreeNodeDto buildResourceCategory(
                List<ResourceEntity> rows,
                Map<Integer, String> jarNameById,
                String filterKeywordLower
        ) {
            MutableTreeNode root = new MutableTreeNode("", "resource:root", true);
            boolean groupByJar = STATE.groupTreeByJar;
            for (ResourceEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String normalized = normalizeResourcePath(row.getResourcePath());
                if (normalized == null) {
                    continue;
                }
                int jarId = row.getJarId() == null ? 0 : row.getJarId();
                String jarName = resolveJarName(jarId, row.getJarName(), jarNameById);
                if (!matchesFilter(filterKeywordLower, normalized, jarName)) {
                    continue;
                }
                MutableTreeNode cursor = groupByJar
                        ? root.children.computeIfAbsent(
                        "resjar:" + jarName + "|" + jarId,
                        key -> new MutableTreeNode(jarName, "resjar:" + jarName + "|" + jarId, true)
                )
                        : root;
                String[] parts = normalized.split("/");
                StringBuilder path = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (part == null || part.isBlank()) {
                        continue;
                    }
                    boolean leaf = i == parts.length - 1;
                    if (leaf) {
                        String label = part;
                        if (!groupByJar) {
                            label = label + " [" + jarName + "]";
                        }
                        String leafKey = "resleaf:" + row.getRid();
                        String leafValue = "res:" + row.getRid();
                        String finalLabel = label;
                        cursor.children.computeIfAbsent(leafKey,
                                key -> new MutableTreeNode(finalLabel, leafValue, false));
                    } else {
                        if (path.length() > 0) {
                            path.append('/');
                        }
                        path.append(part);
                        String dirKey = "resdir:" + (groupByJar ? jarId + ":" : "")
                                + path.toString().replace('/', '.');
                        cursor = cursor.children.computeIfAbsent(
                                dirKey,
                                key -> new MutableTreeNode(part, dirKey, true)
                        );
                    }
                }
            }
            List<TreeNodeDto> out = new ArrayList<>();
            for (MutableTreeNode node : root.children.values()) {
                out.add(node.freeze());
            }
            sortNodes(out);
            if (STATE.mergePackageRoot) {
                if (groupByJar) {
                    List<TreeNodeDto> merged = new ArrayList<>();
                    for (TreeNodeDto node : out) {
                        if (node == null || !node.directory()) {
                            continue;
                        }
                        merged.add(new TreeNodeDto(
                                node.label(),
                                node.value(),
                                true,
                                mergeNodeList(node.children())
                        ));
                    }
                    out = merged;
                } else {
                    out = mergeNodeList(out);
                }
            }
            return new TreeNodeDto("资源文件", CATEGORY_RESOURCE, true, out);
        }

        private TreeNodeDto buildDependencyCategory(List<JarEntity> rows, String filterKeywordLower) {
            List<TreeNodeDto> out = new ArrayList<>();
            for (JarEntity row : rows) {
                if (row == null) {
                    continue;
                }
                int jarId = row.getJid();
                String jarName = resolveJarName(jarId, row.getJarName(), null);
                String absPath = safe(row.getJarAbsPath()).trim();
                if (!matchesFilter(filterKeywordLower, jarName, absPath)) {
                    continue;
                }
                List<TreeNodeDto> children = absPath.isBlank()
                        ? List.of()
                        : List.of(new TreeNodeDto(absPath, "jarpath:" + jarId, false, List.of()));
                out.add(new TreeNodeDto(jarName, "jar:" + jarId, true, children));
            }
            sortNodes(out);
            return new TreeNodeDto("依赖库", CATEGORY_DEPENDENCY, true, out);
        }

        private void sortNodes(List<TreeNodeDto> nodes) {
            nodes.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                    .thenComparing(TreeNodeDto::label));
        }

        private boolean matchesFilter(String filterKeywordLower, String... values) {
            if (filterKeywordLower == null || filterKeywordLower.isBlank()) {
                return true;
            }
            for (String value : values) {
                if (safe(value).toLowerCase(Locale.ROOT).contains(filterKeywordLower)) {
                    return true;
                }
            }
            return false;
        }

        private String resolveJarName(int jarId, String jarName, Map<Integer, String> jarNameById) {
            String direct = safe(jarName).trim();
            if (!direct.isEmpty()) {
                return direct;
            }
            if (jarNameById != null) {
                String mapped = safe(jarNameById.get(jarId)).trim();
                if (!mapped.isEmpty()) {
                    return mapped;
                }
            }
            return jarId == 0 ? "jar-unknown" : "jar-" + jarId;
        }

        private List<TreeNodeDto> mergeNodeList(List<TreeNodeDto> nodes) {
            if (nodes == null || nodes.isEmpty()) {
                return List.of();
            }
            List<TreeNodeDto> out = new ArrayList<>();
            for (TreeNodeDto node : nodes) {
                if (node == null) {
                    continue;
                }
                out.add(mergeNode(node));
            }
            out.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                    .thenComparing(TreeNodeDto::label));
            return out;
        }

        private TreeNodeDto mergeNode(TreeNodeDto node) {
            if (node == null || !node.directory()) {
                return node;
            }
            List<TreeNodeDto> mergedChildren = mergeNodeList(node.children());
            TreeNodeDto merged = new TreeNodeDto(node.label(), node.value(), true, mergedChildren);
            while (merged.children() != null
                    && merged.children().size() == 1
                    && merged.children().get(0).directory()) {
                TreeNodeDto only = merged.children().get(0);
                merged = new TreeNodeDto(
                        merged.label() + "/" + only.label(),
                        only.value(),
                        true,
                        only.children()
                );
            }
            return merged;
        }

        private String normalizeClassName(String raw) {
            String value = safe(raw).trim();
            if (value.isEmpty()) {
                return null;
            }
            value = value.replace('\\', '/');
            if (value.endsWith(".class")) {
                value = value.substring(0, value.length() - 6);
            }
            if (value.startsWith("/")) {
                value = value.substring(1);
            }
            if (value.isEmpty()) {
                return null;
            }
            if (!STATE.showInnerClass && value.contains("$")) {
                return null;
            }
            return value;
        }

        private String normalizeResourcePath(String raw) {
            String value = safe(raw).trim();
            if (value.isEmpty()) {
                return null;
            }
            value = value.replace('\\', '/');
            while (value.startsWith("/")) {
                value = value.substring(1);
            }
            if (value.isEmpty()) {
                return null;
            }
            return value;
        }

        private static final class MutableTreeNode {
            private final String label;
            private final String value;
            private final boolean directory;
            private final Map<String, MutableTreeNode> children = new HashMap<>();

            private MutableTreeNode(String label, String value, boolean directory) {
                this.label = label;
                this.value = value;
                this.directory = directory;
            }

            private TreeNodeDto freeze() {
                List<TreeNodeDto> nodes = new ArrayList<>();
                for (MutableTreeNode child : children.values()) {
                    if (child == null) {
                        continue;
                    }
                    nodes.add(child.freeze());
                }
                nodes.sort(Comparator.comparing(TreeNodeDto::directory).reversed()
                        .thenComparing(TreeNodeDto::label));
                return new TreeNodeDto(
                        label,
                        value,
                        directory,
                        nodes
                );
            }
        }
    }

    private static final class DefaultToolingFacade implements ToolingFacade {
        @Override
        public void openExportTool() {
            emitToolingWindow(ToolingWindowAction.EXPORT);
        }

        @Override
        public void openRemoteLoadTool() {
            emitToolingWindow(ToolingWindowAction.REMOTE_LOAD);
        }

        @Override
        public void openProxyTool() {
            emitToolingWindow(ToolingWindowAction.PROXY);
        }

        @Override
        public void openPartitionTool() {
            emitToolingWindow(ToolingWindowAction.PARTITION);
        }

        @Override
        public void openGlobalSearchTool() {
            emitToolingWindow(ToolingWindowAction.GLOBAL_SEARCH);
        }

        @Override
        public void openSystemMonitorTool() {
            emitToolingWindow(ToolingWindowAction.SYSTEM_MONITOR);
        }

        @Override
        public void openMarkdownViewer(String title, String markdownResource) {
            emitMarkdownWindow(title, markdownResource);
        }

        @Override
        public void openCfgTool() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.CFG,
                    new ToolingWindowPayload.TextPayload("CFG Analyze", renderCfgText())
            ));
        }

        @Override
        public void openFrameTool(boolean full) {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.FRAME,
                    new ToolingWindowPayload.TextPayload(
                            full ? "Frame Analyze (Full)" : "Frame Analyze",
                            renderFrameText(full)
                    )
            ));
        }

        @Override
        public void openOpcodeTool() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.OPCODE,
                    new ToolingWindowPayload.TextPayload("Opcode Viewer", renderOpcodeText())
            ));
        }

        @Override
        public void openAsmTool() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.ASM,
                    new ToolingWindowPayload.TextPayload("ASM Viewer", renderAsmText())
            ));
        }

        @Override
        public void openElSearchTool() {
            emitToolingWindow(ToolingWindowAction.EL_SEARCH);
        }

        @Override
        public void openAllStringsTool() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.ALL_STRINGS,
                    new ToolingWindowPayload.TextPayload("All Strings", renderAllStringsText())
            ));
        }

        @Override
        public void openSqlConsoleTool() {
            emitToolingWindow(ToolingWindowAction.SQL_CONSOLE);
        }

        @Override
        public void openCypherConsoleTool() {
            emitToolingWindow(ToolingWindowAction.CYPHER_CONSOLE);
        }

        @Override
        public void openEncodeTool() {
            emitToolingWindow(ToolingWindowAction.ENCODE_TOOL);
        }

        @Override
        public void openListenerTool() {
            emitToolingWindow(ToolingWindowAction.SOCKET_LISTENER);
        }

        @Override
        public void openSerializationTool() {
            emitToolingWindow(ToolingWindowAction.SERIALIZATION);
        }

        @Override
        public void openBcelTool() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.BCEL_TOOL,
                    new ToolingWindowPayload.TextPayload("BCEL Tool", renderBcelText())
            ));
        }

        @Override
        public void openRepeaterTool() {
            emitToolingWindow(ToolingWindowAction.REPEATER);
        }

        @Override
        public void openObfuscationTool() {
            emitToolingWindow(ToolingWindowAction.OBFUSCATION);
        }

        @Override
        public void openHtmlGraph() {
            try {
                Path dir = Paths.get(Const.tempDir).toAbsolutePath();
                Files.createDirectories(dir);
                Path out = dir.resolve("call-graph-" + System.currentTimeMillis() + ".html");
                Files.writeString(out, renderCallGraphHtml(), StandardCharsets.UTF_8);
                try {
                    Desktop.getDesktop().browse(out.toUri());
                } catch (Throwable ignored) {
                }
                emitTextWindow("HTML Graph", "Graph generated: " + out.toAbsolutePath());
            } catch (Throwable ex) {
                emitTextWindow("HTML Graph", "Graph generation failed: " + ex.getMessage());
            }
        }

        @Override
        public void openChainsDfsResult() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.CHAINS_RESULT,
                    new ToolingWindowPayload.ChainsResultPayload(
                            false,
                            "DFS Result",
                            "No DFS result. Start DFS first.",
                            buildDfsResultItems()
                    )
            ));
        }

        @Override
        public void openChainsTaintResult() {
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.CHAINS_RESULT,
                    new ToolingWindowPayload.ChainsResultPayload(
                            true,
                            "Taint Result",
                            "No taint result. Start Taint first.",
                            buildTaintResultItems()
                    )
            ));
        }

        @Override
        public void openRemoteTomcatAnalyzer() {
            emitPathWindow(ToolingWindowAction.EXTERNAL_TOOLS, "remote-tomcat");
        }

        @Override
        public void openBytecodeDebugger() {
            emitPathWindow(ToolingWindowAction.EXTERNAL_TOOLS, "bytecode-debugger");
        }

        @Override
        public void openJdGui() {
            emitPathWindow(ToolingWindowAction.EXTERNAL_TOOLS, "jd-gui");
        }

        @Override
        public void openFlappyGame() {
            Thread.ofPlatform().name("flappy-game").daemon(true).start(() -> {
                try {
                    new FBMainFrame().startGame();
                } catch (Throwable ex) {
                    emitTextWindow("Flappy Bird", "Start failed: " + ex.getMessage());
                }
            });
        }

        @Override
        public void openPockerGame() {
            Thread.ofPlatform().name("pocker-game").daemon(true).start(() -> {
                try {
                    new Main();
                } catch (Throwable ex) {
                    emitTextWindow("Pocker", "Start failed: " + ex.getMessage());
                }
            });
        }

        @Override
        public void openDocs() {
            openUrl(Const.docsUrl, "Docs");
        }

        @Override
        public void openReportBug() {
            openUrl(Const.newIssueUrl, "Report Bug");
        }

        @Override
        public void openProjectSite() {
            openUrl(Const.projectUrl, "Project");
        }

        @Override
        public void openVersionInfo() {
            emitTextWindow("Version", "Jar Analyzer version: " + Const.version);
        }

        @Override
        public void openChangelog() {
            openMarkdownViewer("CHANGELOG", "src/main/resources/CHANGELOG.MD");
        }

        @Override
        public void openThanks() {
            openMarkdownViewer("Thanks", "src/main/resources/thanks.md");
        }

        @Override
        public void setLanguageChinese() {
            GlobalOptions.setLang(GlobalOptions.CHINESE);
            STATE.language = GlobalOptions.CHINESE;
            persistConfig(cfg -> cfg.setLang("zh"));
        }

        @Override
        public void setLanguageEnglish() {
            GlobalOptions.setLang(GlobalOptions.ENGLISH);
            STATE.language = GlobalOptions.ENGLISH;
            persistConfig(cfg -> cfg.setLang("en"));
        }

        @Override
        public void useThemeDefault() {
            STATE.theme = normalizeTheme("default");
            persistConfig(cfg -> cfg.setTheme("default"));
        }

        @Override
        public void useThemeDark() {
            STATE.theme = normalizeTheme("dark");
            persistConfig(cfg -> cfg.setTheme("dark"));
        }

        @Override
        public void useThemeOrange() {
            STATE.theme = normalizeTheme("orange");
            persistConfig(cfg -> cfg.setTheme("orange"));
        }

        @Override
        public void setStripeShowNames(boolean showNames) {
            STATE.stripeShowNames = showNames;
            persistConfig(cfg -> cfg.setStripeShowNames(showNames));
        }

        @Override
        public void setStripeWidth(int width) {
            int normalized = normalizeStripeWidth(width);
            STATE.stripeWidth = normalized;
            persistConfig(cfg -> cfg.setStripeWidth(normalized));
        }

        @Override
        public void toggleShowInnerClass() {
            STATE.showInnerClass = !STATE.showInnerClass;
            emitTextWindow("Config", "show inner class: " + STATE.showInnerClass);
        }

        @Override
        public void toggleFixClassPath() {
            updateBuildSettings(s -> new BuildSettingsDto(
                    s.inputPath(),
                    s.runtimePath(),
                    s.resolveNestedJars(),
                    s.autoFindRuntimeJar(),
                    s.addRuntimeJar(),
                    s.deleteTempBeforeBuild(),
                    !s.fixClassPath(),
                    s.fixMethodImpl(),
                    s.quickMode()
            ));
            emitTextWindow("Config", "fix class path: " + STATE.buildSettings.fixClassPath());
        }

        @Override
        public void setSortByMethod() {
            STATE.sortByMethod = true;
            STATE.sortByClass = false;
            emitTextWindow("Config", "search sort: method");
        }

        @Override
        public void setSortByClass() {
            STATE.sortByMethod = false;
            STATE.sortByClass = true;
            emitTextWindow("Config", "search sort: class");
        }

        @Override
        public void toggleLogAllSql() {
            boolean enabled = !SqlLogConfig.isEnabled();
            SqlLogConfig.setEnabled(enabled);
            emitTextWindow("Config", "save all sql statement: " + enabled);
        }

        @Override
        public void toggleGroupTreeByJar() {
            STATE.groupTreeByJar = !STATE.groupTreeByJar;
            emitTextWindow("Config", "group tree by jar: " + STATE.groupTreeByJar);
        }

        @Override
        public void toggleMergePackageRoot() {
            STATE.mergePackageRoot = !STATE.mergePackageRoot;
            emitTextWindow("Config", "merge package root: " + STATE.mergePackageRoot);
        }

        @Override
        public void toggleFixMethodImpl() {
            updateBuildSettings(s -> new BuildSettingsDto(
                    s.inputPath(),
                    s.runtimePath(),
                    s.resolveNestedJars(),
                    s.autoFindRuntimeJar(),
                    s.addRuntimeJar(),
                    s.deleteTempBeforeBuild(),
                    s.fixClassPath(),
                    !s.fixMethodImpl(),
                    s.quickMode()
            ));
            emitTextWindow("Config", "fix method impl/override: " + STATE.buildSettings.fixMethodImpl());
        }

        @Override
        public void toggleQuickMode() {
            updateBuildSettings(s -> new BuildSettingsDto(
                    s.inputPath(),
                    s.runtimePath(),
                    s.resolveNestedJars(),
                    s.autoFindRuntimeJar(),
                    s.addRuntimeJar(),
                    s.deleteTempBeforeBuild(),
                    s.fixClassPath(),
                    s.fixMethodImpl(),
                    !s.quickMode()
            ));
            emitTextWindow("Config", "quick mode: " + STATE.buildSettings.quickMode());
        }

        @Override
        public ToolingConfigSnapshotDto configSnapshot() {
            return new ToolingConfigSnapshotDto(
                    STATE.showInnerClass,
                    STATE.buildSettings.fixClassPath(),
                    STATE.sortByMethod,
                    STATE.sortByClass,
                    SqlLogConfig.isEnabled(),
                    STATE.groupTreeByJar,
                    STATE.mergePackageRoot,
                    STATE.buildSettings.fixMethodImpl(),
                    STATE.buildSettings.quickMode(),
                    STATE.language == GlobalOptions.ENGLISH ? "en" : "zh",
                    STATE.theme,
                    STATE.stripeShowNames,
                    normalizeStripeWidth(STATE.stripeWidth)
            );
        }

        private void updateBuildSettings(Function<BuildSettingsDto, BuildSettingsDto> mutator) {
            BuildSettingsDto current = STATE.buildSettings;
            BuildSettingsDto next = mutator == null ? current : mutator.apply(current);
            if (next != null) {
                STATE.buildSettings = next;
            }
        }

        private void persistConfig(Consumer<ConfigFile> updater) {
            try {
                ConfigFile cfg = ConfigEngine.parseConfig();
                if (cfg == null) {
                    cfg = new ConfigFile();
                }
                updater.accept(cfg);
                ConfigEngine.saveConfig(cfg);
            } catch (Throwable ex) {
                logger.debug("persist config failed: {}", ex.toString());
            }
        }

        private void openUrl(String url, String name) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Throwable ex) {
                emitTextWindow(name, "open url failed: " + ex.getMessage());
            }
        }

        private String renderAsmText() {
            MethodContext ctx = resolveMethodContext(false);
            if (ctx == null) {
                return "Open a class first.";
            }
            try (InputStream in = Files.newInputStream(Paths.get(ctx.absPath()))) {
                String text = ASMPrint.getPrint(in, true);
                return text == null ? "ASM output is empty." : text;
            } catch (Throwable ex) {
                return "ASM render failed: " + ex.getMessage();
            }
        }

        private String renderOpcodeText() {
            MethodContext ctx = resolveMethodContext(true);
            if (ctx == null) {
                return "Open a method first.";
            }
            try {
                String text = IdentifyCallEngine.run(ctx.absPath(), ctx.methodName(), ctx.methodDesc());
                if (text != null && !text.isBlank()) {
                    return text;
                }
            } catch (Throwable ignored) {
            }
            try {
                MethodNode node = loadMethodNode(ctx, false);
                if (node == null) {
                    return "Method not found in class bytecode.";
                }
                return renderInstructionLines(node);
            } catch (Throwable ex) {
                return "Opcode render failed: " + ex.getMessage();
            }
        }

        private String renderCfgText() {
            MethodContext ctx = resolveMethodContext(true);
            if (ctx == null) {
                return "Open a method first.";
            }
            try {
                MethodNode node = loadMethodNode(ctx, false);
                if (node == null) {
                    return "Method not found in class bytecode.";
                }
                Map<AbstractInsnNode, Integer> indexMap = indexMap(node);
                StringBuilder sb = new StringBuilder();
                sb.append("CFG for ").append(ctx.owner()).append('#')
                        .append(ctx.methodName()).append(ctx.methodDesc()).append('\n');
                int size = node.instructions.size();
                for (int i = 0; i < size; i++) {
                    AbstractInsnNode insn = node.instructions.get(i);
                    List<Integer> successors = cfgSuccessors(insn, i, size, indexMap);
                    sb.append(String.format("%04d %-18s -> %s%n", i, opcodeName(insn), successors));
                }
                return sb.toString();
            } catch (Throwable ex) {
                return "CFG render failed: " + ex.getMessage();
            }
        }

        private String renderFrameText(boolean full) {
            MethodContext ctx = resolveMethodContext(true);
            if (ctx == null) {
                return "Open a method first.";
            }
            try {
                MethodNode node = loadMethodNode(ctx, true);
                if (node == null) {
                    return "Method not found in class bytecode.";
                }
                Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
                Frame<BasicValue>[] frames = analyzer.analyze(ctx.owner(), node);
                StringBuilder sb = new StringBuilder();
                sb.append("Frame for ").append(ctx.owner()).append('#')
                        .append(ctx.methodName()).append(ctx.methodDesc()).append('\n');
                for (int i = 0; i < node.instructions.size(); i++) {
                    AbstractInsnNode insn = node.instructions.get(i);
                    Frame<BasicValue> frame = frames[i];
                    if (frame == null) {
                        sb.append(String.format("%04d %-18s %s%n", i, opcodeName(insn), "UNREACHABLE"));
                        continue;
                    }
                    if (!full) {
                        sb.append(String.format(
                                "%04d %-18s locals=%d stack=%d%n",
                                i,
                                opcodeName(insn),
                                frame.getLocals(),
                                frame.getStackSize()
                        ));
                        continue;
                    }
                    sb.append(String.format(
                            "%04d %-18s locals=%s stack=%s%n",
                            i,
                            opcodeName(insn),
                            frameLocals(frame),
                            frameStack(frame)
                    ));
                }
                return sb.toString();
            } catch (Throwable ex) {
                return "Frame render failed: " + ex.getMessage();
            }
        }

        private String renderBcelText() {
            MethodContext ctx = resolveMethodContext(false);
            if (ctx == null) {
                return "Open a class first.";
            }
            try {
                JavaClass javaClass = new ClassParser(ctx.absPath()).parse();
                ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
                StringBuilder sb = new StringBuilder();
                sb.append("BCEL for ").append(javaClass.getClassName()).append('\n');
                for (Method method : javaClass.getMethods()) {
                    if (method == null) {
                        continue;
                    }
                    if (!ctx.methodName().isBlank() && !ctx.methodName().equals(method.getName())) {
                        continue;
                    }
                    if (!ctx.methodDesc().isBlank() && !ctx.methodDesc().equals(method.getSignature())) {
                        continue;
                    }
                    sb.append('\n').append(method.getName()).append(method.getSignature()).append('\n');
                    MethodGen methodGen = new MethodGen(method, javaClass.getClassName(), cpg);
                    InstructionList instructionList = methodGen.getInstructionList();
                    if (instructionList == null) {
                        sb.append("  <no instructions>").append('\n');
                        continue;
                    }
                    for (InstructionHandle ih = instructionList.getStart(); ih != null; ih = ih.getNext()) {
                        sb.append(String.format("  %04d %s%n", ih.getPosition(), ih.getInstruction()));
                    }
                }
                return sb.toString();
            } catch (Throwable ex) {
                return "BCEL render failed: " + ex.getMessage();
            }
        }

        private String renderAllStringsText() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return "engine is not ready";
            }
            int total = 0;
            try {
                total = engine.getStringCount();
            } catch (Throwable ignored) {
            }
            final int limit = 2000;
            List<String> strings;
            try {
                strings = engine.getStrings(1);
            } catch (Throwable ex) {
                return "load strings failed: " + ex.getMessage();
            }
            if (strings == null || strings.isEmpty()) {
                return "no strings";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("total strings: ").append(total).append('\n');
            sb.append("display limit: ").append(limit).append('\n');
            sb.append('\n');
            int count = 0;
            for (String s : strings) {
                if (s == null) {
                    continue;
                }
                sb.append(s).append('\n');
                count++;
                if (count >= limit) {
                    break;
                }
            }
            if (total > limit) {
                sb.append('\n').append("... truncated ... use Search tab for precise query");
            }
            return sb.toString();
        }

        private String renderCallGraphHtml() {
            CallGraphSnapshotDto snapshot = RuntimeFacades.callGraph().snapshot();
            StringBuilder sb = new StringBuilder();
            sb.append("<!doctype html><html><head><meta charset=\"utf-8\"/>");
            sb.append("<title>Jar Analyzer Graph</title>");
            sb.append("<style>body{font-family:Menlo,Consolas,monospace;padding:18px;line-height:1.5}");
            sb.append("h1,h2{margin:8px 0;}ul{margin:6px 0 14px 18px;}code{background:#f2f4f7;padding:2px 4px;}");
            sb.append(".muted{color:#666;}</style></head><body>");
            sb.append("<h1>Call Graph Snapshot</h1>");
            sb.append("<p class=\"muted\">current: <code>")
                    .append(escapeHtml(snapshot.currentMethod()))
                    .append("</code></p>");
            appendMethodListHtml(sb, "All Methods", snapshot.allMethods());
            appendMethodListHtml(sb, "Callers", snapshot.callers());
            appendMethodListHtml(sb, "Callees", snapshot.callees());
            appendMethodListHtml(sb, "Impls", snapshot.impls());
            appendMethodListHtml(sb, "Super Impls", snapshot.superImpls());
            sb.append("</body></html>");
            return sb.toString();
        }

        private String renderDfsResultText() {
            if (TaintCache.dfsCache == null || TaintCache.dfsCache.isEmpty()) {
                return "No DFS result. Start DFS first.";
            }
            StringBuilder sb = new StringBuilder();
            int index = 1;
            for (DFSResult result : TaintCache.dfsCache) {
                if (result == null) {
                    continue;
                }
                sb.append("[").append(index).append("] ")
                        .append("depth=").append(result.getDepth())
                        .append(" path=").append(result.getPathCount())
                        .append(" node=").append(result.getNodeCount())
                        .append(" edge=").append(result.getEdgeCount())
                        .append('\n');
                if (result.getSource() != null) {
                    sb.append("  source: ").append(result.getSource()).append('\n');
                }
                if (result.getSink() != null) {
                    sb.append("  sink  : ").append(result.getSink()).append('\n');
                }
                if (result.getMethodList() != null) {
                    int i = 1;
                    for (Object method : result.getMethodList()) {
                        sb.append("    ").append(i).append(". ").append(String.valueOf(method)).append('\n');
                        i++;
                    }
                }
                if (result.isTruncated()) {
                    sb.append("  truncated: ").append(safe(result.getTruncateReason())).append('\n');
                }
                if (!safe(result.getRecommend()).isBlank()) {
                    sb.append("  recommend: ").append(result.getRecommend()).append('\n');
                }
                sb.append('\n');
                index++;
            }
            return sb.toString();
        }

        private List<ChainsResultItemDto> buildDfsResultItems() {
            List<ChainsResultItemDto> items = new ArrayList<>();
            if (TaintCache.dfsCache == null || TaintCache.dfsCache.isEmpty()) {
                return items;
            }
            int index = 1;
            for (DFSResult result : TaintCache.dfsCache) {
                if (result == null) {
                    continue;
                }
                items.add(new ChainsResultItemDto(
                        index,
                        formatHandle(result.getSink()),
                        formatHandle(result.getSource()),
                        result.getDepth(),
                        result.getPathCount(),
                        result.getNodeCount(),
                        result.getEdgeCount(),
                        result.getElapsedMs(),
                        result.isTruncated(),
                        safe(result.getTruncateReason()),
                        safe(result.getRecommend()),
                        false,
                        false,
                        false,
                        "",
                        "",
                        toMethodNavList(result.getMethodList())
                ));
                index++;
            }
            return items;
        }

        private String renderTaintResultText() {
            if (TaintCache.cache == null || TaintCache.cache.isEmpty()) {
                return "No taint result. Start Taint first.";
            }
            StringBuilder sb = new StringBuilder();
            int index = 1;
            for (TaintResult result : TaintCache.cache) {
                if (result == null) {
                    continue;
                }
                sb.append("[").append(index).append("] ")
                        .append(result.isSuccess() ? "PASS" : "BLOCK")
                        .append(result.isLowConfidence() ? " (LOW-CONFIDENCE)" : "")
                        .append('\n');
                DFSResult dfs = result.getDfsResult();
                if (dfs != null) {
                    sb.append("  sink: ").append(dfs.getSink()).append('\n');
                    sb.append("  source: ").append(dfs.getSource()).append('\n');
                }
                sb.append("  detail:\n");
                sb.append(safe(result.getTaintText())).append('\n');
                sb.append('\n');
                index++;
            }
            return sb.toString();
        }

        private List<ChainsResultItemDto> buildTaintResultItems() {
            List<ChainsResultItemDto> items = new ArrayList<>();
            if (TaintCache.cache == null || TaintCache.cache.isEmpty()) {
                return items;
            }
            int index = 1;
            for (TaintResult result : TaintCache.cache) {
                if (result == null) {
                    continue;
                }
                DFSResult dfs = result.getDfsResult();
                items.add(new ChainsResultItemDto(
                        index,
                        formatHandle(dfs == null ? null : dfs.getSink()),
                        formatHandle(dfs == null ? null : dfs.getSource()),
                        dfs == null ? 0 : dfs.getDepth(),
                        dfs == null ? 0 : dfs.getPathCount(),
                        dfs == null ? 0 : dfs.getNodeCount(),
                        dfs == null ? 0 : dfs.getEdgeCount(),
                        dfs == null ? 0L : dfs.getElapsedMs(),
                        dfs != null && dfs.isTruncated(),
                        dfs == null ? "" : safe(dfs.getTruncateReason()),
                        dfs == null ? "" : safe(dfs.getRecommend()),
                        true,
                        result.isSuccess(),
                        result.isLowConfidence(),
                        safe(result.getTaintText()),
                        extractSanitizerLines(result.getTaintText()),
                        dfs == null ? List.of() : toMethodNavList(dfs.getMethodList())
                ));
                index++;
            }
            return items;
        }

        private List<MethodNavDto> toMethodNavList(List<MethodReference.Handle> handles) {
            if (handles == null || handles.isEmpty()) {
                return List.of();
            }
            List<MethodNavDto> out = new ArrayList<>();
            for (MethodReference.Handle handle : handles) {
                if (handle == null) {
                    continue;
                }
                String className = "";
                if (handle.getClassReference() != null) {
                    className = normalizeClass(handle.getClassReference().getName());
                }
                out.add(new MethodNavDto(
                        className,
                        safe(handle.getName()),
                        safe(handle.getDesc()),
                        "",
                        0
                ));
            }
            return out;
        }

        private String formatHandle(MethodReference.Handle handle) {
            if (handle == null) {
                return "";
            }
            String className = "";
            if (handle.getClassReference() != null) {
                className = normalizeClass(handle.getClassReference().getName());
            }
            if (className.isBlank()) {
                return safe(handle.getName()) + safe(handle.getDesc());
            }
            return className + "#" + safe(handle.getName()) + safe(handle.getDesc());
        }

        private String extractSanitizerLines(String taintText) {
            String raw = safe(taintText);
            if (raw.isBlank()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            String[] lines = raw.split("\\R");
            for (String line : lines) {
                String current = safe(line);
                String low = current.toLowerCase(Locale.ROOT);
                if (low.contains("sanitizer") || low.contains("barrier")) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(current);
                }
            }
            return sb.toString();
        }

        private void appendMethodListHtml(StringBuilder sb, String title, List<MethodNavDto> list) {
            sb.append("<h2>").append(escapeHtml(title)).append(" (")
                    .append(list == null ? 0 : list.size()).append(")</h2><ul>");
            if (list != null) {
                for (MethodNavDto m : list) {
                    if (m == null) {
                        continue;
                    }
                    sb.append("<li><code>")
                            .append(escapeHtml(m.className())).append('#')
                            .append(escapeHtml(m.methodName()))
                            .append(escapeHtml(m.methodDesc()))
                            .append("</code></li>");
                }
            }
            sb.append("</ul>");
        }

        private String escapeHtml(String value) {
            return safe(value)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }

        private MethodContext resolveMethodContext(boolean requireMethod) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return null;
            }
            MethodNavDto current = STATE.currentMethod;
            String className = current != null ? current.className() : STATE.editorDocument.className();
            String methodName = current != null ? current.methodName() : STATE.editorDocument.methodName();
            String methodDesc = current != null ? current.methodDesc() : STATE.editorDocument.methodDesc();
            Integer jarId = current != null ? current.jarId() : null;
            String owner = normalizeClass(className);
            if (owner.isEmpty()) {
                return null;
            }
            if (requireMethod && (safe(methodName).isBlank() || safe(methodDesc).isBlank())) {
                return null;
            }
            String absPath;
            if (jarId != null && jarId > 0) {
                absPath = engine.getAbsPath(owner, jarId);
            } else {
                absPath = engine.getAbsPath(owner);
            }
            if (safe(absPath).isBlank()) {
                absPath = engine.getAbsPath(owner);
            }
            if (safe(absPath).isBlank()) {
                return null;
            }
            return new MethodContext(absPath, owner, safe(methodName), safe(methodDesc));
        }

        private MethodNode loadMethodNode(MethodContext ctx, boolean expandFrames) throws Exception {
            byte[] data = Files.readAllBytes(Paths.get(ctx.absPath()));
            ClassReader reader = new ClassReader(data);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, expandFrames ? ClassReader.EXPAND_FRAMES : ClassReader.SKIP_DEBUG);
            MethodNode fallback = null;
            for (MethodNode node : classNode.methods) {
                if (node == null) {
                    continue;
                }
                if (!ctx.methodName().isBlank()
                        && node.name.equals(ctx.methodName())
                        && node.desc.equals(ctx.methodDesc())) {
                    return node;
                }
                if (fallback == null && !ctx.methodName().isBlank() && node.name.equals(ctx.methodName())) {
                    fallback = node;
                }
            }
            return fallback;
        }

        private String renderInstructionLines(MethodNode node) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < node.instructions.size(); i++) {
                AbstractInsnNode insn = node.instructions.get(i);
                sb.append(String.format("%04d %s%n", i, opcodeName(insn)));
            }
            return sb.toString();
        }

        private Map<AbstractInsnNode, Integer> indexMap(MethodNode node) {
            Map<AbstractInsnNode, Integer> map = new HashMap<>();
            for (int i = 0; i < node.instructions.size(); i++) {
                map.put(node.instructions.get(i), i);
            }
            return map;
        }

        private List<Integer> cfgSuccessors(
                AbstractInsnNode insn,
                int idx,
                int size,
                Map<AbstractInsnNode, Integer> indexMap
        ) {
            List<Integer> out = new ArrayList<>();
            int opcode = insn.getOpcode();
            if (insn instanceof JumpInsnNode jumpInsn) {
                Integer target = indexMap.get(jumpInsn.label);
                if (target != null) {
                    out.add(target);
                }
                if (opcode != org.objectweb.asm.Opcodes.GOTO
                        && opcode != org.objectweb.asm.Opcodes.JSR
                        && idx + 1 < size) {
                    out.add(idx + 1);
                }
                return out;
            }
            if (insn instanceof TableSwitchInsnNode tableSwitchInsn) {
                Integer dflt = indexMap.get(tableSwitchInsn.dflt);
                if (dflt != null) {
                    out.add(dflt);
                }
                for (LabelNode label : tableSwitchInsn.labels) {
                    Integer t = indexMap.get(label);
                    if (t != null && !out.contains(t)) {
                        out.add(t);
                    }
                }
                return out;
            }
            if (insn instanceof LookupSwitchInsnNode lookupSwitchInsn) {
                Integer dflt = indexMap.get(lookupSwitchInsn.dflt);
                if (dflt != null) {
                    out.add(dflt);
                }
                for (LabelNode label : lookupSwitchInsn.labels) {
                    Integer t = indexMap.get(label);
                    if (t != null && !out.contains(t)) {
                        out.add(t);
                    }
                }
                return out;
            }
            if (opcode == org.objectweb.asm.Opcodes.ATHROW
                    || opcode == org.objectweb.asm.Opcodes.RETURN
                    || opcode == org.objectweb.asm.Opcodes.IRETURN
                    || opcode == org.objectweb.asm.Opcodes.LRETURN
                    || opcode == org.objectweb.asm.Opcodes.FRETURN
                    || opcode == org.objectweb.asm.Opcodes.DRETURN
                    || opcode == org.objectweb.asm.Opcodes.ARETURN) {
                return out;
            }
            if (idx + 1 < size) {
                out.add(idx + 1);
            }
            return out;
        }

        private String opcodeName(AbstractInsnNode insn) {
            if (insn == null) {
                return "<null>";
            }
            int opcode = insn.getOpcode();
            if (opcode >= 0 && opcode < Printer.OPCODES.length) {
                String value = Printer.OPCODES[opcode];
                return value == null ? insn.getClass().getSimpleName() : value;
            }
            return insn.getClass().getSimpleName();
        }

        private String frameLocals(Frame<BasicValue> frame) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < frame.getLocals(); i++) {
                values.add(String.valueOf(frame.getLocal(i)));
            }
            return values.toString();
        }

        private String frameStack(Frame<BasicValue> frame) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < frame.getStackSize(); i++) {
                values.add(String.valueOf(frame.getStack(i)));
            }
            return values.toString();
        }

        private record MethodContext(String absPath, String owner, String methodName, String methodDesc) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> immutableList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static String engineStatus() {
        CoreEngine engine = EngineContext.getEngine();
        return engine == null ? "CLOSED" : "OPEN";
    }

    private static String classNameOrNull(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        return className;
    }

    private static String normalizeClass(String className) {
        String value = safe(className).trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private static MethodNavDto toMethodNav(MethodResult m) {
        return new MethodNavDto(
                safe(m.getClassName()),
                safe(m.getMethodName()),
                safe(m.getMethodDesc()),
                safe(m.getJarName()),
                m.getJarId()
        );
    }

    private static ClassNavDto toClassNav(ClassResult c) {
        return new ClassNavDto(
                safe(c.getClassName()),
                safe(c.getJarName()),
                c.getJarId()
        );
    }

    private static List<MethodNavDto> mapMethods(List<MethodResult> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<MethodNavDto> out = new ArrayList<>();
        for (MethodResult m : list) {
            if (m != null) {
                out.add(toMethodNav(m));
            }
        }
        out.sort(Comparator.comparing(MethodNavDto::className).thenComparing(MethodNavDto::methodName));
        return out;
    }

    private static List<ClassNavDto> mapClasses(List<ClassResult> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<ClassNavDto> out = new ArrayList<>();
        for (ClassResult c : list) {
            if (c != null) {
                out.add(toClassNav(c));
            }
        }
        out.sort(Comparator.comparing(ClassNavDto::className));
        return out;
    }

    private static SearchResultDto toSearchResult(MethodResult m) {
        String preview = safe(m.getClassName()) + "#" + safe(m.getMethodName()) + safe(m.getMethodDesc());
        return new SearchResultDto(
                safe(m.getClassName()),
                safe(m.getMethodName()),
                safe(m.getMethodDesc()),
                safe(m.getJarName()),
                m.getJarId(),
                preview
        );
    }

    private static String formatCurrentMethod() {
        MethodNavDto current = STATE.currentMethod;
        if (current == null) {
            return "";
        }
        return current.className() + "#" + current.methodName() + current.methodDesc();
    }

    private static Set<String> parseBlacklist(String text) {
        Set<String> out = new HashSet<>();
        String value = safe(text);
        if (value.isBlank()) {
            return out;
        }
        String[] arr = value.split("[,;\\r\\n]+");
        for (String s : arr) {
            if (s == null) {
                continue;
            }
            String v = s.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }

    private static String appendTail(String old, String msg) {
        String merged = safe(old) + safe(msg);
        int max = 8000;
        if (merged.length() <= max) {
            return merged;
        }
        return merged.substring(merged.length() - max);
    }

    private static String mask(String token) {
        String value = safe(token);
        if (value.isEmpty()) {
            return "";
        }
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    private static boolean getEnabled(Map<McpLineKey, McpLineConfigDto> lines, McpLineKey key, boolean def) {
        McpLineConfigDto line = lines.get(key);
        return line == null ? def : line.enabled();
    }

    private static Integer getPort(Map<McpLineKey, McpLineConfigDto> lines, McpLineKey key, int def) {
        McpLineConfigDto line = lines.get(key);
        return line == null ? def : line.port();
    }

    private static int normalizePort(int port, int def) {
        return port >= 1 && port <= 65535 ? port : def;
    }

    private static EnumMap<McpLine, McpServiceConfig> readLineConfigs(ConfigFile cfg) {
        EnumMap<McpLine, McpServiceConfig> map = new EnumMap<>(McpLine.class);
        String bind = safe(cfg.getMcpBind()).isEmpty() ? "0.0.0.0" : cfg.getMcpBind();
        boolean auth = cfg.isMcpAuth();
        String token = safe(cfg.getMcpToken()).isEmpty() ? "JAR-ANALYZER-MCP-TOKEN" : cfg.getMcpToken();
        map.put(McpLine.AUDIT_FAST, service(cfg.isMcpAuditFastEnabled(), bind, cfg.getMcpAuditFastPort(), auth, token));
        map.put(McpLine.GRAPH_LITE, service(cfg.isMcpGraphLiteEnabled(), bind, cfg.getMcpGraphLitePort(), auth, token));
        map.put(McpLine.DFS_TAINT, service(cfg.isMcpDfsTaintEnabled(), bind, cfg.getMcpDfsTaintPort(), auth, token));
        map.put(McpLine.SCA_LEAK, service(cfg.isMcpScaLeakEnabled(), bind, cfg.getMcpScaLeakPort(), auth, token));
        map.put(McpLine.VUL_RULES, service(cfg.isMcpVulRulesEnabled(), bind, cfg.getMcpVulRulesPort(), auth, token));
        map.put(McpLine.REPORT, service(cfg.isMcpReportEnabled(), bind, cfg.getMcpReportPort(), auth, token));
        return map;
    }

    private static McpReportWebConfig readReportWebConfig(ConfigFile cfg) {
        McpReportWebConfig web = new McpReportWebConfig();
        web.setEnabled(cfg.isMcpReportWebEnabled());
        web.setHost(cfg.getMcpReportWebHost());
        web.setPort(cfg.getMcpReportWebPort());
        return web;
    }

    private static McpServiceConfig service(boolean enabled, String bind, int port, boolean auth, String token) {
        McpServiceConfig cfg = new McpServiceConfig();
        cfg.setEnabled(enabled);
        cfg.setBind(bind);
        cfg.setPort(port);
        cfg.setAuth(auth);
        cfg.setToken(token);
        return cfg;
    }

    private static ServerConfig safeApiConfig() {
        ServerConfig cfg = GlobalOptions.getServerConfig();
        return cfg == null ? new ServerConfig() : cfg;
    }

    private record RuleConfig(
            boolean enabled,
            Function<String, List<String>> ruleFn,
            String typeName,
            String logName
    ) {
    }

    private static LeakResult toLeakResult(LeakItemDto dto) {
        LeakResult leak = new LeakResult();
        leak.setClassName(dto.className());
        leak.setTypeName(dto.typeName());
        leak.setValue(dto.value());
        leak.setJarName(dto.jarName());
        leak.setJarId(dto.jarId());
        return leak;
    }
}
