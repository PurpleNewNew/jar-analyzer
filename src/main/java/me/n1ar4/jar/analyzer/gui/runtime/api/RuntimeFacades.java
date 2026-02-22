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
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DFSUtil;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.graph.flow.FlowTruncation;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
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
import me.n1ar4.jar.analyzer.gui.util.EditorDeclarationResolver;
import me.n1ar4.jar.analyzer.gui.util.EditorSymbolNavigationResolver;
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphScope;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsResultItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ClassNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationTargetDto;
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
import me.n1ar4.jar.analyzer.gui.runtime.model.StructureItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.StructureSnapshotDto;
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
import me.n1ar4.jar.analyzer.taint.TaintCache;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.utils.BuildToolClasspathResolver;
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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RuntimeFacades {
    private static final Logger logger = LogManager.getLogger();
    private static volatile Consumer<ToolingWindowRequest> toolingWindowConsumer = request -> {
    };

    private static final int STRIPE_MIN_WIDTH = 40;
    private static final int STRIPE_MAX_WIDTH = 100;
    private static final int DEFAULT_LANGUAGE = loadInitialLanguage();
    private static final String DEFAULT_THEME = loadInitialTheme();
    private static final boolean STRIPE_DEFAULT_SHOW_NAMES = loadInitialStripeShowNames();
    private static final int STRIPE_DEFAULT_WIDTH = loadInitialStripeWidth();
    private static final RuntimeState STATE = new RuntimeState();

    private static final BuildFacade BUILD = new DefaultBuildFacade();
    private static final SearchFacade SEARCH = new DefaultSearchFacade();
    private static final StructureFacade STRUCTURE = new DefaultStructureFacade();
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

    public static StructureFacade structure() {
        return STRUCTURE;
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
                String input = safe(STATE.buildSettings.activeInputPath()).trim();
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
        if ("dark".equals(value)) {
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
                BuildSettingsDto.MODE_ARTIFACT,
                "",
                "",
                "",
                false,
                false,
                false,
                true,
                false,
                true,
                false
        );
        private volatile int buildProgress = 0;
        private volatile String buildStatusText = initialTr("就绪", "ready");
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
        private volatile String searchStatusText = initialTr("就绪", "ready");

        private volatile MethodNavDto currentMethod = null;
        private volatile String currentClass = "";
        private volatile String currentJar = "";
        private volatile String callGraphScope = CallGraphScope.APP.value();
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
                "", "low", 30
        );
        private volatile String chainsStatusText = initialTr("就绪", "ready");

        private volatile EditorDocumentDto editorDocument = new EditorDocumentDto(
                "", "", null, "", "", "", 0, initialTr("就绪", "ready"));
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
        private static final String CLASSPATH_EXTRA_PROP = "jar.analyzer.classpath.extra";
        private static final String CLASSPATH_SOURCE_PROP = "jar.analyzer.classpath.source";

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
            STATE.buildStatusText = tr("构建设置已更新", "build settings updated");
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
                STATE.buildStatusText = tr("缓存已清理", "cache cleaned");
            });
        }

        private void doBuild() {
            BuildSettingsDto settings = STATE.buildSettings;
            SdkResolution sdkResolution = resolveSdk(settings);
            if (sdkResolution.error != null) {
                STATE.buildStatusText = sdkResolution.error;
                return;
            }
            Path workspaceSdkPath = sdkResolution.sdkPath;
            Path rtPath = sdkResolution.runtimeArchivePath;
            BuildInputResolution inputResolution = resolveBuildInput(settings, workspaceSdkPath, rtPath);
            if (inputResolution.error != null) {
                STATE.buildStatusText = inputResolution.error;
                return;
            }
            Path input = inputResolution.inputPath;

            STATE.buildProgress = 0;
            STATE.buildStatusText = tr("构建中...", "building...");
            try {
                prepareWorkspaceContext(settings, inputResolution, workspaceSdkPath);
            } catch (Throwable ex) {
                logger.debug("set workspace context failed: {}", ex.toString());
            }

            String previousExtra = System.getProperty(CLASSPATH_EXTRA_PROP);
            String previousSource = System.getProperty(CLASSPATH_SOURCE_PROP);
            applyBuildClasspathProperties(inputResolution.extraClasspath, previousExtra);
            try {
                if (inputResolution.sourceIndexMode) {
                    System.setProperty(CLASSPATH_SOURCE_PROP, "source-index");
                } else {
                    System.clearProperty(CLASSPATH_SOURCE_PROP);
                }

                CoreRunner.BuildResult result;
                if (inputResolution.sourceIndexMode) {
                    result = CoreRunner.runSourceIndex(
                            inputResolution.selectedInputPath,
                            inputResolution.projectRootPath,
                            inputResolution.sourceRoots,
                            inputResolution.sourceFiles,
                            settings.quickMode(),
                            settings.fixMethodImpl(),
                            p -> STATE.buildProgress = p,
                            true
                    );
                } else {
                    result = CoreRunner.run(
                            input,
                            rtPath,
                            settings.fixClassPath(),
                            settings.quickMode(),
                            settings.fixMethodImpl(),
                            p -> STATE.buildProgress = p,
                            true
                    );
                }
                if (result == null) {
                    STATE.buildStatusText = tr("构建失败", "build failed");
                    return;
                }
                STATE.totalJar = String.valueOf(result.getJarCount());
                STATE.totalClass = String.valueOf(result.getClassCount());
                STATE.totalMethod = String.valueOf(result.getMethodCount());
                STATE.totalEdge = String.valueOf(result.getEdgeCount());
                STATE.databaseSize = result.getDbSizeLabel();
                STATE.buildProgress = 100;
                STATE.buildStatusText = tr("构建完成", "build finished");
                saveBuildConfig(inputResolution.selectedInputPath == null
                        ? settings.activeInputPath()
                        : inputResolution.selectedInputPath.toString(), result);
            } catch (Throwable ex) {
                STATE.buildStatusText = tr("构建异常: ", "build error: ") + safe(ex.getMessage());
                logger.error("runtime build failed: {}", ex.toString());
            } finally {
                restoreBuildClasspathProperties(previousExtra, previousSource);
            }
        }

        private BuildInputResolution resolveBuildInput(BuildSettingsDto settings,
                                                       Path workspaceSdkPath,
                                                       Path runtimeArchivePath) {
            if (settings == null) {
                return BuildInputResolution.error(tr("构建设置为空", "build settings is empty"));
            }
            String inputPath = safe(settings.activeInputPath()).trim();
            if (inputPath.isEmpty()) {
                return BuildInputResolution.error(tr("输入路径为空", "input path is empty"));
            }
            Path selectedInput = Paths.get(inputPath).toAbsolutePath().normalize();
            if (Files.notExists(selectedInput)) {
                return BuildInputResolution.error(tr("输入路径不存在", "input path not exists"));
            }

            if (Files.isRegularFile(selectedInput) && !isArchiveOrClassFile(selectedInput)) {
                String lower = selectedInput.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".java")) {
                    return BuildInputResolution.error(tr(
                            "输入文件必须是 .jar/.war/.class/.java 或目录",
                            "input file must be .jar/.war/.class/.java or a directory"
                    ));
                }
            }

            Path projectRoot = resolveLikelyProjectRoot(selectedInput);
            boolean projectLayout = projectRoot != null
                    && Files.isDirectory(projectRoot)
                    && isLikelyProjectLayout(projectRoot);
            Path normalizedProjectRoot = projectRoot == null
                    ? null
                    : projectRoot.toAbsolutePath().normalize();

            SourceCollection sourceCollection = collectSourceInput(selectedInput, normalizedProjectRoot);
            List<Path> extraClasspath = collectProjectExtraClasspath(normalizedProjectRoot);
            Path analysisInput = resolveAnalyzableInput(
                    selectedInput,
                    normalizedProjectRoot,
                    settings,
                    workspaceSdkPath,
                    runtimeArchivePath,
                    extraClasspath,
                    sourceCollection
            );
            if (analysisInput == null) {
                if (!sourceCollection.sourceFiles.isEmpty()) {
                    return BuildInputResolution.sourceIndex(
                            selectedInput,
                            normalizedProjectRoot,
                            projectLayout || normalizedProjectRoot != null,
                            sourceCollection.sourceRoots,
                            sourceCollection.sourceFiles,
                            extraClasspath
                    );
                }
                String missing = Files.isDirectory(selectedInput) || isSourceFile(selectedInput)
                        ? tr("输入中没有可分析的字节码或源码", "input has no analyzable bytecode or source files")
                        : tr("输入中没有可分析的字节码（.class/.jar/.war）", "input has no analyzable bytecode (.class/.jar/.war)");
                return BuildInputResolution.error(missing);
            }

            return BuildInputResolution.ok(
                    analysisInput.toAbsolutePath().normalize(),
                    selectedInput,
                    normalizedProjectRoot,
                    projectLayout,
                    extraClasspath
            );
        }

        private SdkResolution resolveSdk(BuildSettingsDto settings) {
            if (settings == null || !settings.includeSdk()) {
                return SdkResolution.none();
            }
            String raw = safe(settings.sdkPath()).trim();
            if (raw.isEmpty()) {
                if (!settings.autoDetectSdk()) {
                    return SdkResolution.error(tr("SDK 路径为空", "sdk path is empty"));
                }
                Path auto = detectSdkFromEnv();
                if (auto == null) {
                    return SdkResolution.error(tr("自动检测 SDK 失败", "auto detect sdk failed"));
                }
                raw = auto.toString();
            }
            Path sdk = Paths.get(raw).toAbsolutePath().normalize();
            if (Files.notExists(sdk)) {
                return SdkResolution.error(tr("SDK 路径不存在", "sdk path not exists"));
            }
            Path runtimeArchive = resolveRuntimeArchiveForBuild(sdk);
            return SdkResolution.ok(sdk, runtimeArchive);
        }

        private Path detectSdkFromEnv() {
            String javaHome = safe(System.getProperty("java.home")).trim();
            if (!javaHome.isEmpty()) {
                Path home = Paths.get(javaHome).toAbsolutePath().normalize();
                if (Files.exists(home)) {
                    return home;
                }
            }
            String envJavaHome = safe(System.getenv("JAVA_HOME")).trim();
            if (!envJavaHome.isEmpty()) {
                Path home = Paths.get(envJavaHome).toAbsolutePath().normalize();
                if (Files.exists(home)) {
                    return home;
                }
            }
            return null;
        }

        private Path resolveRuntimeArchiveForBuild(Path sdkPath) {
            if (sdkPath == null || Files.notExists(sdkPath)) {
                return null;
            }
            if (Files.isRegularFile(sdkPath) && isArchiveOrClassFile(sdkPath)) {
                return sdkPath;
            }
            if (!Files.isDirectory(sdkPath)) {
                return null;
            }
            Path rtJar = sdkPath.resolve(Paths.get("lib", "rt.jar"));
            if (Files.isRegularFile(rtJar)) {
                return rtJar;
            }
            Path jreRtJar = sdkPath.resolve(Paths.get("jre", "lib", "rt.jar"));
            if (Files.isRegularFile(jreRtJar)) {
                return jreRtJar;
            }
            return null;
        }

        private boolean hasAnalyzableBytecode(Path root) {
            if (root == null || !Files.isDirectory(root)) {
                return false;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(root, 8)) {
                return stream.anyMatch(path ->
                        Files.isRegularFile(path) && isArchiveOrClassFile(path));
            } catch (Exception ex) {
                logger.debug("scan project bytecode failed: {}", ex.toString());
                return false;
            }
        }

        private Path resolveLikelyProjectRoot(Path input) {
            if (input == null) {
                return null;
            }
            Path cursor = Files.isDirectory(input) ? input : input.getParent();
            if (cursor == null) {
                return null;
            }
            Path normalized = cursor.toAbsolutePath().normalize();
            Path current = normalized;
            for (int i = 0; i < 8 && current != null; i++) {
                if (isLikelyProjectLayout(current)) {
                    return current;
                }
                current = current.getParent();
            }
            return normalized;
        }

        private boolean isLikelyProjectLayout(Path root) {
            if (root == null || !Files.isDirectory(root)) {
                return false;
            }
            return Files.exists(root.resolve("pom.xml"))
                    || Files.exists(root.resolve("build.gradle"))
                    || Files.exists(root.resolve("build.gradle.kts"))
                    || Files.exists(root.resolve("settings.gradle"))
                    || Files.exists(root.resolve("settings.gradle.kts"))
                    || Files.exists(root.resolve(".idea"))
                    || Files.exists(root.resolve("src"));
        }

        private Path resolveAnalyzableInput(Path selectedInput,
                                            Path projectRoot,
                                            BuildSettingsDto settings,
                                            Path workspaceSdkPath,
                                            Path runtimeArchivePath,
                                            List<Path> extraClasspath,
                                            SourceCollection sourceCollection) {
            if (selectedInput == null) {
                return null;
            }
            if (Files.isRegularFile(selectedInput) && isArchiveOrClassFile(selectedInput)) {
                return selectedInput;
            }
            Path directory = selectedInput;
            if (Files.isRegularFile(selectedInput) && isSourceFile(selectedInput)) {
                directory = selectedInput.getParent();
            }
            if (directory != null && Files.isDirectory(directory) && hasAnalyzableBytecode(directory)) {
                return directory;
            }

            Path base = projectRoot;
            if (base == null || !Files.isDirectory(base)) {
                return null;
            }
            Path[] candidates = new Path[]{
                    base.resolve(Paths.get("target", "classes")),
                    base.resolve(Paths.get("target", "test-classes")),
                    base.resolve(Paths.get("build", "classes")),
                    base.resolve(Paths.get("build", "classes", "java", "main")),
                    base.resolve(Paths.get("build", "classes", "kotlin", "main")),
                    base.resolve(Paths.get("out", "production")),
                    base.resolve(Paths.get("out", "classes")),
                    base.resolve("bin")
            };
            for (Path candidate : candidates) {
                if (Files.isDirectory(candidate) && hasAnalyzableBytecode(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
            }
            Path fromSources = tryCompileSourcesToTemp(
                    selectedInput,
                    base,
                    settings,
                    workspaceSdkPath,
                    runtimeArchivePath,
                    extraClasspath,
                    sourceCollection
            );
            if (fromSources != null && hasAnalyzableBytecode(fromSources)) {
                return fromSources;
            }
            return null;
        }

        private Path tryCompileSourcesToTemp(Path selectedInput,
                                             Path projectRoot,
                                             BuildSettingsDto settings,
                                             Path workspaceSdkPath,
                                             Path runtimeArchivePath) {
            return tryCompileSourcesToTemp(
                    selectedInput,
                    projectRoot,
                    settings,
                    workspaceSdkPath,
                    runtimeArchivePath,
                    List.of(),
                    null
            );
        }

        private Path tryCompileSourcesToTemp(Path selectedInput,
                                             Path projectRoot,
                                             BuildSettingsDto settings,
                                             Path workspaceSdkPath,
                                             Path runtimeArchivePath,
                                             List<Path> extraClasspath,
                                             SourceCollection sourceCollection) {
            SourceCollection sourcesInfo = sourceCollection == null
                    ? collectSourceInput(selectedInput, projectRoot)
                    : sourceCollection;
            List<Path> sources = sourcesInfo.sourceFiles;
            if (sources.isEmpty()) {
                return null;
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                logger.warn("source compile fallback unavailable: no system java compiler");
                return null;
            }
            Path base = projectRoot == null ? selectedInput : projectRoot;
            String key = Integer.toHexString(safe(base.toString()).hashCode());
            Path outputDir = Paths.get(Const.tempDir, "source-bytecode", key);
            try {
                Files.createDirectories(outputDir);
                clearDirectoryKeepRoot(outputDir);
            } catch (Exception ex) {
                logger.debug("prepare source compile output failed: {}", ex.toString());
                return null;
            }

            List<Path> classpathEntries = collectCompileClasspath(
                    projectRoot,
                    workspaceSdkPath,
                    runtimeArchivePath,
                    extraClasspath
            );
            List<String> options = new ArrayList<>();
            options.add("-proc:none");
            options.add("-Xlint:none");
            options.add("-encoding");
            options.add("UTF-8");
            options.add("-d");
            options.add(outputDir.toString());
            if (!classpathEntries.isEmpty()) {
                options.add("-classpath");
                options.add(joinClasspath(classpathEntries));
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), java.nio.charset.StandardCharsets.UTF_8)) {
                Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sources);
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, units);
                boolean ok = Boolean.TRUE.equals(task.call());
                if (ok) {
                    logger.info("source compile fallback success (sources={}, output={})", sources.size(), outputDir);
                } else {
                    logger.warn("source compile fallback has errors (sources={}, output={})", sources.size(), outputDir);
                }
                if (hasAnalyzableBytecode(outputDir)) {
                    return outputDir;
                }
                return null;
            } catch (Exception ex) {
                logger.warn("source compile fallback failed: {}", ex.toString());
                return null;
            }
        }

        private SourceCollection collectSourceInput(Path selectedInput, Path projectRoot) {
            LinkedHashSet<Path> roots = new LinkedHashSet<>();
            if (selectedInput != null && Files.isRegularFile(selectedInput) && isSourceFile(selectedInput)) {
                Path parent = selectedInput.getParent();
                if (parent != null && Files.isDirectory(parent)) {
                    roots.add(parent.toAbsolutePath().normalize());
                }
            }
            if (projectRoot != null && Files.isDirectory(projectRoot)) {
                roots.add(projectRoot.resolve(Paths.get("src", "main", "java")).toAbsolutePath().normalize());
                roots.add(projectRoot.resolve(Paths.get("src", "test", "java")).toAbsolutePath().normalize());
                roots.add(projectRoot.resolve(Paths.get("src", "main", "kotlin")).toAbsolutePath().normalize());
                roots.add(projectRoot.resolve(Paths.get("src", "test", "kotlin")).toAbsolutePath().normalize());
                roots.add(projectRoot.resolve(Paths.get("target", "generated-sources")).toAbsolutePath().normalize());
                roots.add(projectRoot.resolve("src").toAbsolutePath().normalize());
            }
            if (selectedInput != null && Files.isDirectory(selectedInput)) {
                roots.add(selectedInput.toAbsolutePath().normalize());
            } else if (selectedInput != null && selectedInput.getParent() != null) {
                roots.add(selectedInput.getParent().toAbsolutePath().normalize());
            }

            List<Path> sourceRoots = new ArrayList<>();
            LinkedHashSet<Path> sourceFiles = new LinkedHashSet<>();
            for (Path root : roots) {
                if (root == null || !Files.isDirectory(root)) {
                    continue;
                }
                sourceRoots.add(root);
                try (java.util.stream.Stream<Path> stream = Files.walk(root, 12)) {
                    stream.filter(Files::isRegularFile)
                            .filter(this::isSourceFile)
                            .map(path -> path.toAbsolutePath().normalize())
                            .forEach(sourceFiles::add);
                } catch (Exception ex) {
                    logger.debug("scan source files failed: {}", ex.toString());
                }
            }
            return new SourceCollection(
                    sourceRoots.isEmpty() ? List.of() : List.copyOf(sourceRoots),
                    sourceFiles.isEmpty() ? List.of() : List.copyOf(sourceFiles)
            );
        }

        private List<Path> collectCompileClasspath(Path projectRoot,
                                                   Path workspaceSdkPath,
                                                   Path runtimeArchivePath,
                                                   List<Path> extraClasspath) {
            LinkedHashSet<Path> out = new LinkedHashSet<>();
            if (runtimeArchivePath != null && Files.exists(runtimeArchivePath)) {
                out.add(runtimeArchivePath.toAbsolutePath().normalize());
            }
            if (workspaceSdkPath != null && Files.isRegularFile(workspaceSdkPath) && isArchiveOrClassFile(workspaceSdkPath)) {
                out.add(workspaceSdkPath.toAbsolutePath().normalize());
            }
            if (extraClasspath != null && !extraClasspath.isEmpty()) {
                for (Path entry : extraClasspath) {
                    if (entry == null || Files.notExists(entry)) {
                        continue;
                    }
                    out.add(entry.toAbsolutePath().normalize());
                }
            }
            Path base = projectRoot;
            if (base != null && Files.isDirectory(base)) {
                Path[] candidates = new Path[]{
                        base.resolve("lib"),
                        base.resolve("libs"),
                        base.resolve(Paths.get("target", "dependency")),
                        base.resolve(Paths.get("build", "libs"))
                };
                for (Path candidate : candidates) {
                    if (candidate == null || !Files.isDirectory(candidate)) {
                        continue;
                    }
                    try (java.util.stream.Stream<Path> stream = Files.walk(candidate, 3)) {
                        stream.filter(Files::isRegularFile)
                                .filter(path -> {
                                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                                    return name.endsWith(".jar");
                                })
                                .map(path -> path.toAbsolutePath().normalize())
                                .forEach(out::add);
                    } catch (Exception ex) {
                        logger.debug("scan compile classpath failed: {}", ex.toString());
                    }
                }
            }
            return new ArrayList<>(out);
        }

        private List<Path> collectProjectExtraClasspath(Path projectRoot) {
            LinkedHashSet<Path> out = new LinkedHashSet<>();
            if (projectRoot != null && Files.isDirectory(projectRoot)) {
                try {
                    out.addAll(BuildToolClasspathResolver.resolveProjectClasspath(projectRoot));
                } catch (Exception ex) {
                    logger.debug("resolve build-tool classpath failed: {}", ex.toString());
                }
                out.addAll(collectConventionalProjectClasspath(projectRoot));
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }

        private List<Path> collectConventionalProjectClasspath(Path projectRoot) {
            if (projectRoot == null || !Files.isDirectory(projectRoot)) {
                return List.of();
            }
            LinkedHashSet<Path> out = new LinkedHashSet<>();
            Path[] jarDirs = new Path[]{
                    projectRoot.resolve("lib"),
                    projectRoot.resolve("libs"),
                    projectRoot.resolve(Paths.get("target", "dependency")),
                    projectRoot.resolve(Paths.get("build", "libs"))
            };
            for (Path dir : jarDirs) {
                if (dir == null || !Files.isDirectory(dir)) {
                    continue;
                }
                try (java.util.stream.Stream<Path> stream = Files.walk(dir, 4)) {
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                            .map(path -> path.toAbsolutePath().normalize())
                            .forEach(out::add);
                } catch (Exception ex) {
                    logger.debug("scan project jar classpath failed: {}", ex.toString());
                }
            }
            Path[] classDirs = new Path[]{
                    projectRoot.resolve(Paths.get("target", "classes")),
                    projectRoot.resolve(Paths.get("target", "test-classes")),
                    projectRoot.resolve(Paths.get("build", "classes")),
                    projectRoot.resolve(Paths.get("build", "classes", "java", "main")),
                    projectRoot.resolve(Paths.get("build", "classes", "kotlin", "main")),
                    projectRoot.resolve(Paths.get("out", "production")),
                    projectRoot.resolve(Paths.get("out", "test")),
                    projectRoot.resolve("bin")
            };
            for (Path dir : classDirs) {
                if (dir != null && Files.isDirectory(dir)) {
                    out.add(dir.toAbsolutePath().normalize());
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }

        private String joinClasspath(List<Path> entries) {
            if (entries == null || entries.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (Path entry : entries) {
                if (entry == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(java.io.File.pathSeparatorChar);
                }
                sb.append(entry.toString());
            }
            return sb.toString();
        }

        private void applyBuildClasspathProperties(List<Path> extraClasspath, String previous) {
            LinkedHashSet<String> merged = new LinkedHashSet<>();
            if (previous != null && !previous.isBlank()) {
                String[] items = previous.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator));
                for (String item : items) {
                    String value = safe(item).trim();
                    if (!value.isBlank()) {
                        merged.add(value);
                    }
                }
            }
            if (extraClasspath != null && !extraClasspath.isEmpty()) {
                for (Path entry : extraClasspath) {
                    if (entry == null || Files.notExists(entry)) {
                        continue;
                    }
                    merged.add(entry.toAbsolutePath().normalize().toString());
                }
            }
            if (merged.isEmpty()) {
                System.clearProperty(CLASSPATH_EXTRA_PROP);
                return;
            }
            String value = String.join(java.io.File.pathSeparator, merged);
            System.setProperty(CLASSPATH_EXTRA_PROP, value);
        }

        private void restoreBuildClasspathProperties(String previousExtra, String previousSource) {
            if (previousExtra == null) {
                System.clearProperty(CLASSPATH_EXTRA_PROP);
            } else {
                System.setProperty(CLASSPATH_EXTRA_PROP, previousExtra);
            }
            if (previousSource == null) {
                System.clearProperty(CLASSPATH_SOURCE_PROP);
            } else {
                System.setProperty(CLASSPATH_SOURCE_PROP, previousSource);
            }
        }

        private void clearDirectoryKeepRoot(Path root) {
            if (root == null || !Files.isDirectory(root)) {
                return;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                List<Path> all = stream.sorted(java.util.Comparator.reverseOrder()).toList();
                for (Path path : all) {
                    if (path == null || path.equals(root)) {
                        continue;
                    }
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ex) {
                logger.debug("clear temp compile dir failed: {}", ex.toString());
            }
        }

        private boolean isSourceFile(Path path) {
            if (path == null || !Files.isRegularFile(path)) {
                return false;
            }
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return name.endsWith(".java");
        }

        private boolean isArchiveOrClassFile(Path path) {
            if (path == null) {
                return false;
            }
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".class");
        }

        private void prepareWorkspaceContext(BuildSettingsDto settings,
                                             BuildInputResolution inputResolution,
                                             Path workspaceSdkPath) {
            if (inputResolution == null) {
                return;
            }
            if (inputResolution.projectLayout) {
                Path projectRoot = inputResolution.projectRootPath == null
                        ? inputResolution.selectedInputPath
                        : inputResolution.projectRootPath;
                Path analysisInput = inputResolution.inputPath == null
                        ? inputResolution.selectedInputPath
                        : inputResolution.inputPath;
                WorkspaceContext.setProjectModel(buildProjectModel(
                        settings,
                        projectRoot,
                        analysisInput,
                        workspaceSdkPath,
                        inputResolution.extraClasspath,
                        inputResolution.sourceRoots
                ));
                return;
            }
            Path artifactInput = inputResolution.inputPath == null
                    ? inputResolution.selectedInputPath
                    : inputResolution.inputPath;
            if (artifactInput == null) {
                return;
            }
            WorkspaceContext.ensureArtifactProjectModel(
                    artifactInput,
                    workspaceSdkPath,
                    settings != null && settings.resolveNestedJars()
            );
        }

        private ProjectModel buildProjectModel(BuildSettingsDto settings,
                                               Path projectRoot,
                                               Path analysisInputPath,
                                               Path workspaceSdkPath,
                                               List<Path> extraClasspath,
                                               List<Path> sourceRoots) {
            Path normalizedProjectRoot = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
            Path normalizedAnalysisInput = analysisInputPath == null
                    ? normalizedProjectRoot
                    : analysisInputPath.toAbsolutePath().normalize();
            ProjectModel.Builder builder = ProjectModel.builder()
                    .buildMode(ProjectBuildMode.PROJECT)
                    .primaryInputPath(normalizedAnalysisInput)
                    .runtimePath(workspaceSdkPath)
                    .resolveInnerJars(settings != null && settings.resolveNestedJars());
            if (normalizedProjectRoot != null) {
                builder.addRoot(new ProjectRoot(
                        ProjectRootKind.CONTENT_ROOT,
                        ProjectOrigin.APP,
                        normalizedProjectRoot,
                        "",
                        false,
                        false,
                        10
                ));
                addProjectConventionalRoots(builder, normalizedProjectRoot);
            }
            if (sourceRoots != null && !sourceRoots.isEmpty()) {
                int priority = 18;
                for (Path sourceRoot : sourceRoots) {
                    if (sourceRoot == null || Files.notExists(sourceRoot) || !Files.isDirectory(sourceRoot)) {
                        continue;
                    }
                    addRootIfExists(
                            builder,
                            sourceRoot,
                            ProjectRootKind.SOURCE_ROOT,
                            ProjectOrigin.APP,
                            false,
                            priority++
                    );
                }
            }
            if (normalizedAnalysisInput != null
                    && !Objects.equals(normalizedAnalysisInput, normalizedProjectRoot)
                    && Files.exists(normalizedAnalysisInput)) {
                builder.addRoot(new ProjectRoot(
                        ProjectRootKind.GENERATED,
                        ProjectOrigin.GENERATED,
                        normalizedAnalysisInput,
                        "",
                        Files.isRegularFile(normalizedAnalysisInput),
                        false,
                        15
                ));
            }
            if (workspaceSdkPath != null && Files.exists(workspaceSdkPath)) {
                builder.addRoot(new ProjectRoot(
                        ProjectRootKind.SDK,
                        ProjectOrigin.SDK,
                        workspaceSdkPath,
                        "",
                        Files.isRegularFile(workspaceSdkPath),
                        false,
                        100
                ));
            }
            addResolvedLibraryRoots(builder, normalizedProjectRoot, extraClasspath);
            return builder.build();
        }

        private void addResolvedLibraryRoots(ProjectModel.Builder builder,
                                             Path projectRoot,
                                             List<Path> extraClasspath) {
            if (builder == null || extraClasspath == null || extraClasspath.isEmpty()) {
                return;
            }
            int priority = 110;
            for (Path entry : extraClasspath) {
                if (entry == null || Files.notExists(entry)) {
                    continue;
                }
                Path normalized = entry.toAbsolutePath().normalize();
                if (projectRoot != null) {
                    try {
                        if (normalized.startsWith(projectRoot)) {
                            continue;
                        }
                    } catch (Exception ignored) {
                    }
                }
                addRootIfExists(
                        builder,
                        normalized,
                        ProjectRootKind.LIBRARY,
                        ProjectOrigin.LIBRARY,
                        false,
                        priority++
                );
            }
        }

        private void addProjectConventionalRoots(ProjectModel.Builder builder, Path projectRoot) {
            if (builder == null || projectRoot == null) {
                return;
            }
            addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "main", "java")),
                    ProjectRootKind.SOURCE_ROOT, ProjectOrigin.APP, false, 20);
            addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "main", "resources")),
                    ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, false, 25);
            addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "test", "java")),
                    ProjectRootKind.SOURCE_ROOT, ProjectOrigin.APP, true, 30);
            addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "test", "resources")),
                    ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, true, 35);
            addRootIfExists(builder, projectRoot.resolve(Paths.get("target", "generated-sources")),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 40);
            addRootIfExists(builder, projectRoot.resolve(Paths.get("build", "generated")),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 45);
            addRootIfExists(builder, projectRoot.resolve("generated"),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 46);
            addRootIfExists(builder, projectRoot.resolve("lib"),
                    ProjectRootKind.LIBRARY, ProjectOrigin.LIBRARY, false, 50);
            addRootIfExists(builder, projectRoot.resolve("libs"),
                    ProjectRootKind.LIBRARY, ProjectOrigin.LIBRARY, false, 55);
            addRootIfExists(builder, projectRoot.resolve("out"),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 60);
            addRootIfExists(builder, projectRoot.resolve("target"),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 65);
            addRootIfExists(builder, projectRoot.resolve("build"),
                    ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 70);
            addRootIfExists(builder, projectRoot.resolve(".git"),
                    ProjectRootKind.EXCLUDED, ProjectOrigin.EXCLUDED, false, 90);
            addRootIfExists(builder, projectRoot.resolve(".idea"),
                    ProjectRootKind.EXCLUDED, ProjectOrigin.EXCLUDED, false, 91);
        }

        private void addRootIfExists(ProjectModel.Builder builder,
                                     Path path,
                                     ProjectRootKind kind,
                                     ProjectOrigin origin,
                                     boolean test,
                                     int priority) {
            if (builder == null || path == null || Files.notExists(path)) {
                return;
            }
            builder.addRoot(new ProjectRoot(
                    kind,
                    origin,
                    path,
                    "",
                    Files.isRegularFile(path),
                    test,
                    priority
            ));
        }

        private static final class BuildInputResolution {
            private final Path inputPath;
            private final Path selectedInputPath;
            private final Path projectRootPath;
            private final boolean projectLayout;
            private final boolean sourceIndexMode;
            private final List<Path> sourceRoots;
            private final List<Path> sourceFiles;
            private final List<Path> extraClasspath;
            private final String error;

            private BuildInputResolution(Path inputPath,
                                         Path selectedInputPath,
                                         Path projectRootPath,
                                         boolean projectLayout,
                                         boolean sourceIndexMode,
                                         List<Path> sourceRoots,
                                         List<Path> sourceFiles,
                                         List<Path> extraClasspath,
                                         String error) {
                this.inputPath = inputPath;
                this.selectedInputPath = selectedInputPath;
                this.projectRootPath = projectRootPath;
                this.projectLayout = projectLayout;
                this.sourceIndexMode = sourceIndexMode;
                this.sourceRoots = sourceRoots == null ? List.of() : List.copyOf(sourceRoots);
                this.sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
                this.extraClasspath = extraClasspath == null ? List.of() : List.copyOf(extraClasspath);
                this.error = error;
            }

            private static BuildInputResolution ok(Path inputPath,
                                                   Path selectedInputPath,
                                                   Path projectRootPath,
                                                   boolean projectLayout,
                                                   List<Path> extraClasspath) {
                return new BuildInputResolution(
                        inputPath,
                        selectedInputPath,
                        projectRootPath,
                        projectLayout,
                        false,
                        List.of(),
                        List.of(),
                        extraClasspath,
                        null
                );
            }

            private static BuildInputResolution sourceIndex(Path selectedInputPath,
                                                            Path projectRootPath,
                                                            boolean projectLayout,
                                                            List<Path> sourceRoots,
                                                            List<Path> sourceFiles,
                                                            List<Path> extraClasspath) {
                return new BuildInputResolution(
                        selectedInputPath,
                        selectedInputPath,
                        projectRootPath,
                        projectLayout,
                        true,
                        sourceRoots,
                        sourceFiles,
                        extraClasspath,
                        null
                );
            }

            private static BuildInputResolution error(String error) {
                return new BuildInputResolution(
                        null,
                        null,
                        null,
                        false,
                        false,
                        List.of(),
                        List.of(),
                        List.of(),
                        error
                );
            }
        }

        private static final class SourceCollection {
            private final List<Path> sourceRoots;
            private final List<Path> sourceFiles;

            private SourceCollection(List<Path> sourceRoots, List<Path> sourceFiles) {
                this.sourceRoots = sourceRoots == null ? List.of() : sourceRoots;
                this.sourceFiles = sourceFiles == null ? List.of() : sourceFiles;
            }
        }

        private static final class SdkResolution {
            private final Path sdkPath;
            private final Path runtimeArchivePath;
            private final String error;

            private SdkResolution(Path sdkPath, Path runtimeArchivePath, String error) {
                this.sdkPath = sdkPath;
                this.runtimeArchivePath = runtimeArchivePath;
                this.error = error;
            }

            private static SdkResolution none() {
                return new SdkResolution(null, null, null);
            }

            private static SdkResolution ok(Path sdkPath, Path runtimeArchivePath) {
                return new SdkResolution(sdkPath, runtimeArchivePath, null);
            }

            private static SdkResolution error(String error) {
                return new SdkResolution(null, null, error);
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
        private volatile long scopeResolverBuildSeq = -1L;
        private volatile SearchOriginResolver scopeResolver = SearchOriginResolver.empty();

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
            STATE.searchStatusText = tr("搜索条件已更新", "search query updated");
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
            String navigateValue = safe(item.navigateValue()).trim();
            if (!navigateValue.isBlank()) {
                RuntimeFacades.projectTree().openNode(navigateValue);
                STATE.searchStatusText = tr("结果已打开", "result opened");
                return;
            }
            if (!safe(item.methodName()).isBlank()) {
                RuntimeFacades.editor().openMethod(
                        item.className(),
                        item.methodName(),
                        item.methodDesc(),
                        item.jarId()
                );
                STATE.searchStatusText = tr("结果已打开", "result opened");
                return;
            }
            String className = normalizeClass(item.className());
            if (!className.isBlank()) {
                RuntimeFacades.editor().openClass(className, item.jarId());
                STATE.searchStatusText = tr("结果已打开", "result opened");
                return;
            }
            STATE.searchStatusText = tr("当前结果无法跳转", "result has no navigation");
        }

        @Override
        public void publishExternalResults(List<SearchResultDto> results, String statusText) {
            List<SearchResultDto> sorted = new ArrayList<>();
            if (results != null && !results.isEmpty()) {
                sorted.addAll(results);
                Comparator<SearchResultDto> comparator;
                if (STATE.sortByMethod) {
                    comparator = Comparator
                            .comparing((SearchResultDto item) -> safe(item.contributor()))
                            .thenComparing(item -> safe(item.methodName()))
                            .thenComparing(item -> safe(item.className()));
                } else {
                    comparator = Comparator
                            .comparing((SearchResultDto item) -> safe(item.contributor()))
                            .thenComparing(item -> safe(item.className()))
                            .thenComparing(item -> safe(item.methodName()));
                }
                sorted.sort(comparator);
            }
            STATE.searchResults = immutableList(sorted);
            if (statusText == null || statusText.isBlank()) {
                STATE.searchStatusText = tr("结果数: ", "results: ") + sorted.size();
            } else {
                STATE.searchStatusText = statusText;
            }
        }

        private void doSearch() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                STATE.searchResults = List.of();
                STATE.searchStatusText = tr("引擎尚未就绪", "engine is not ready");
                return;
            }
            SearchQueryDto query = STATE.searchQuery == null
                    ? new SearchQueryDto(SearchMode.METHOD_CALL, SearchMatchMode.LIKE,
                    "", "", "", false)
                    : STATE.searchQuery;
            CallGraphScope scope = CallGraphScope.fromValue(query.scope());
            SearchOriginResolver resolver = scope == CallGraphScope.ALL
                    ? SearchOriginResolver.empty()
                    : loadScopeResolver();

            SearchRunResult result;
            try {
                result = switch (query.mode()) {
                    case GLOBAL_CONTRIBUTOR, METHOD_CALL, METHOD_DEFINITION, STRING_CONTAINS, BINARY_CONTAINS ->
                            runContributorSearch(engine, query, scope, resolver);
                    case SQL_QUERY -> runQueryLanguageSearch(query, scope, resolver);
                };
            } catch (Throwable ex) {
                logger.error("runtime search failed: {}", ex.toString());
                result = new SearchRunResult(List.of(),
                        tr("搜索异常: ", "search error: ") + safe(ex.getMessage()));
            }
            publishExternalResults(result.results(), result.statusText());
        }

        private SearchRunResult runContributorSearch(CoreEngine engine,
                                                     SearchQueryDto query,
                                                     CallGraphScope scope,
                                                     SearchOriginResolver resolver) {
            SearchMode mode = query.mode();
            boolean hasContributor = query.contributorClass()
                    || query.contributorMethod()
                    || query.contributorString()
                    || query.contributorResource();
            if (mode == SearchMode.GLOBAL_CONTRIBUTOR && !hasContributor) {
                return new SearchRunResult(List.of(), tr("至少启用一个 contributor", "at least one contributor is required"));
            }
            String classFilter = normalizeClass(query.className());
            String methodFilter = safe(query.methodName()).trim();
            String keyword = safe(query.keyword()).trim();
            if (mode == SearchMode.METHOD_CALL || mode == SearchMode.METHOD_DEFINITION) {
                String methodTerm = methodFilter.isBlank() ? keyword : methodFilter;
                if (methodTerm.isBlank()) {
                    return new SearchRunResult(List.of(), tr("方法名不能为空", "method name is required"));
                }
            } else if ((mode == SearchMode.STRING_CONTAINS || mode == SearchMode.BINARY_CONTAINS)
                    && keyword.isBlank()) {
                return new SearchRunResult(List.of(), tr("关键字不能为空", "keyword is required"));
            } else if (mode == SearchMode.GLOBAL_CONTRIBUTOR
                    && classFilter.isBlank()
                    && methodFilter.isBlank()
                    && keyword.isBlank()) {
                return new SearchRunResult(List.of(),
                        tr("需要关键字或类/方法过滤条件", "keyword or class/method filter is required"));
            }

            final int perContributorLimit = 300;
            Map<String, SearchResultDto> merged = new LinkedHashMap<>();

            if (mode == SearchMode.METHOD_CALL) {
                for (SearchResultDto item : searchCallerContributor(engine, classFilter, methodFilter, keyword,
                        query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            } else if (mode == SearchMode.METHOD_DEFINITION) {
                for (SearchResultDto item : searchMethodContributor(engine, classFilter, methodFilter, keyword,
                        query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            } else if (mode == SearchMode.STRING_CONTAINS) {
                for (SearchResultDto item : searchStringContributor(engine, classFilter, keyword, query.matchMode(),
                        query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            } else if (mode == SearchMode.BINARY_CONTAINS) {
                for (SearchResultDto item : searchBinaryContributor(engine, keyword, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            } else {
                if (query.contributorClass()) {
                    String classTerm = classFilter.isBlank() ? keyword : classFilter;
                    for (SearchResultDto item : searchClassContributor(classTerm, query.matchMode(),
                            perContributorLimit, scope, resolver)) {
                        merged.putIfAbsent(resultKey(item), item);
                    }
                }
                if (query.contributorMethod()) {
                    for (SearchResultDto item : searchMethodContributor(engine, classFilter, methodFilter, keyword,
                            query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                        merged.putIfAbsent(resultKey(item), item);
                    }
                }
                if (query.contributorString()) {
                    for (SearchResultDto item : searchStringContributor(engine, classFilter, keyword, query.matchMode(),
                            query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                        merged.putIfAbsent(resultKey(item), item);
                    }
                }
                if (query.contributorResource()) {
                    String resourceTerm = keyword.isBlank() ? classFilter : keyword;
                    for (SearchResultDto item : searchResourceContributor(engine, resourceTerm, scope, resolver, perContributorLimit)) {
                        merged.putIfAbsent(resultKey(item), item);
                    }
                }
            }

            List<SearchResultDto> out = new ArrayList<>(merged.values());
            String status = tr("结果数: ", "results: ") + out.size() + tr(" (contributors)", " (contributors)");
            return new SearchRunResult(out, status);
        }

        private SearchRunResult runQueryLanguageSearch(SearchQueryDto query,
                                                       CallGraphScope scope,
                                                       SearchOriginResolver resolver) {
            String script = safe(query.keyword()).trim();
            if (script.isBlank()) {
                return new SearchRunResult(List.of(), tr("SQL 语句不能为空", "sql query is required"));
            }
            QueryResult queryResult;
            try {
                QueryOptions options = QueryOptions.defaults();
                queryResult = QueryServices.sql().execute(script, Map.of(), options);
            } catch (Exception ex) {
                String msg = safe(ex.getMessage());
                if (msg.isBlank()) {
                    msg = ex.toString();
                }
                return new SearchRunResult(List.of(),
                        tr("SQL 异常: ", "sql error: ") + msg);
            }
            List<SearchResultDto> out = mapQueryResult(
                    queryResult,
                    "sql",
                    scope,
                    resolver
            );
            String status = tr("结果数: ", "results: ") + out.size();
            if (queryResult.isTruncated()) {
                status = status + tr("（已截断）", " (truncated)");
            }
            return new SearchRunResult(out, status);
        }

        private List<SearchResultDto> mapMethodResults(List<MethodResult> methods,
                                                       String contributor,
                                                       boolean nullParamFilter,
                                                       CallGraphScope scope,
                                                       SearchOriginResolver resolver) {
            List<SearchResultDto> out = new ArrayList<>();
            if (methods == null || methods.isEmpty()) {
                return out;
            }
            for (MethodResult method : methods) {
                if (method == null) {
                    continue;
                }
                if (nullParamFilter && safe(method.getMethodDesc()).contains("()")) {
                    continue;
                }
                int jarId = method.getJarId();
                if (!acceptScope(scope, resolver, jarId)) {
                    continue;
                }
                String origin = resolveOrigin(resolver, jarId);
                out.add(toSearchResult(method, contributor, origin));
            }
            return out;
        }

        private List<SearchResultDto> searchClassContributor(String term,
                                                             SearchMatchMode matchMode,
                                                             int limit,
                                                             CallGraphScope scope,
                                                             SearchOriginResolver resolver) {
            List<SearchResultDto> out = new ArrayList<>();
            String keyword = safe(term).trim();
            if (keyword.isBlank()) {
                return out;
            }
            String normalized = normalizeClass(keyword);
            String sql = matchMode == SearchMatchMode.EQUALS
                    ? "SELECT DISTINCT c.class_name AS class_name, c.jar_id AS jar_id, " +
                    "COALESCE(j.jar_name, '') AS jar_name " +
                    "FROM class_table c LEFT JOIN jar_table j ON c.jar_id = j.jid " +
                    "WHERE c.class_name = ? ORDER BY c.jar_id ASC, c.class_name ASC LIMIT ?"
                    : "SELECT DISTINCT c.class_name AS class_name, c.jar_id AS jar_id, " +
                    "COALESCE(j.jar_name, '') AS jar_name " +
                    "FROM class_table c LEFT JOIN jar_table j ON c.jar_id = j.jid " +
                    "WHERE c.class_name LIKE ? ORDER BY c.jar_id ASC, c.class_name ASC LIMIT ?";
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, matchMode == SearchMatchMode.EQUALS ? normalized : "%" + normalized + "%");
                    statement.setInt(2, Math.max(1, limit));
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            String className = safe(rs.getString("class_name"));
                            int jarId = rs.getInt("jar_id");
                            if (!acceptScope(scope, resolver, jarId)) {
                                continue;
                            }
                            String jarName = safe(rs.getString("jar_name"));
                            String origin = resolveOrigin(resolver, jarId);
                            String navigate = "cls:" + normalizeClass(className) + "|" + jarId;
                            out.add(new SearchResultDto(
                                    className,
                                    "",
                                    "",
                                    jarName,
                                    jarId,
                                    className + (jarName.isBlank() ? "" : " [" + jarName + "]"),
                                    "class",
                                    origin,
                                    navigate
                            ));
                        }
                    }
                }
            } catch (Exception ex) {
                logger.debug("search class contributor failed: {}", ex.toString());
            }
            return out;
        }

        private List<SearchResultDto> searchMethodContributor(CoreEngine engine,
                                                              String classFilter,
                                                              String methodFilter,
                                                              String keyword,
                                                              SearchMatchMode matchMode,
                                                              boolean nullParamFilter,
                                                              CallGraphScope scope,
                                                              SearchOriginResolver resolver,
                                                              int limit) {
            String methodTerm = safe(methodFilter).trim();
            if (methodTerm.isBlank()) {
                methodTerm = safe(keyword).trim();
            }
            if (methodTerm.isBlank()) {
                return List.of();
            }
            List<MethodResult> methods;
            if (matchMode == SearchMatchMode.EQUALS) {
                methods = engine.getMethod(classNameOrNull(classFilter), methodTerm, null);
            } else {
                methods = engine.getMethodLike(classNameOrNull(classFilter), methodTerm, null);
            }
            List<SearchResultDto> out = mapMethodResults(
                    trimLimit(methods, limit),
                    "method",
                    nullParamFilter,
                    scope,
                    resolver
            );
            return out;
        }

        private List<SearchResultDto> searchCallerContributor(CoreEngine engine,
                                                              String classFilter,
                                                              String methodFilter,
                                                              String keyword,
                                                              SearchMatchMode matchMode,
                                                              boolean nullParamFilter,
                                                              CallGraphScope scope,
                                                              SearchOriginResolver resolver,
                                                              int limit) {
            String methodTerm = safe(methodFilter).trim();
            if (methodTerm.isBlank()) {
                methodTerm = safe(keyword).trim();
            }
            if (methodTerm.isBlank()) {
                return List.of();
            }
            List<MethodResult> methods;
            if (matchMode == SearchMatchMode.EQUALS) {
                methods = engine.getCallers(classNameOrNull(classFilter), methodTerm, null, null);
            } else {
                methods = engine.getCallersLike(classNameOrNull(classFilter), methodTerm, null);
            }
            return mapMethodResults(
                    trimLimit(methods, limit),
                    "caller",
                    nullParamFilter,
                    scope,
                    resolver
            );
        }

        private List<SearchResultDto> searchStringContributor(CoreEngine engine,
                                                              String classFilter,
                                                              String keyword,
                                                              SearchMatchMode matchMode,
                                                              boolean nullParamFilter,
                                                              CallGraphScope scope,
                                                              SearchOriginResolver resolver,
                                                              int limit) {
            String term = safe(keyword).trim();
            if (term.isBlank()) {
                return List.of();
            }
            List<MethodResult> methods = matchMode == SearchMatchMode.EQUALS
                    ? engine.getMethodsByStrEqual(term)
                    : engine.getMethodsByStr(term);
            if (!classFilter.isBlank()) {
                List<MethodResult> filtered = new ArrayList<>();
                for (MethodResult method : methods) {
                    if (method == null) {
                        continue;
                    }
                    String className = normalizeClass(method.getClassName());
                    if (className.contains(classFilter)) {
                        filtered.add(method);
                    }
                }
                methods = filtered;
            }
            return mapMethodResults(trimLimit(methods, limit), "string", nullParamFilter, scope, resolver);
        }

        private List<SearchResultDto> searchResourceContributor(CoreEngine engine,
                                                                String keyword,
                                                                CallGraphScope scope,
                                                                SearchOriginResolver resolver,
                                                                int limit) {
            String term = safe(keyword).trim();
            if (term.isBlank()) {
                return List.of();
            }
            List<ResourceEntity> rows = engine.getResources(term, null, 0, Math.max(1, limit));
            List<SearchResultDto> out = new ArrayList<>();
            for (ResourceEntity row : rows) {
                if (row == null) {
                    continue;
                }
                int jarId = row.getJarId() == null ? 0 : row.getJarId();
                if (!acceptScope(scope, resolver, jarId)) {
                    continue;
                }
                String origin = resolveOrigin(resolver, jarId);
                String resourcePath = safe(row.getResourcePath());
                String preview = resourcePath + " (" + row.getFileSize() + " bytes)";
                out.add(new SearchResultDto(
                        resourcePath,
                        "",
                        "",
                        safe(row.getJarName()),
                        jarId,
                        preview,
                        "resource",
                        origin,
                        "res:" + row.getRid()
                ));
            }
            return out;
        }

        private List<SearchResultDto> searchBinaryContributor(CoreEngine engine,
                                                              String keyword,
                                                              int limit) {
            List<SearchResultDto> out = scanBinary(engine.getJarsPath(), safe(keyword).trim());
            return trimLimit(out, limit);
        }

        private List<SearchResultDto> mapQueryResult(QueryResult queryResult,
                                                     String contributor,
                                                     CallGraphScope scope,
                                                     SearchOriginResolver resolver) {
            if (queryResult == null || queryResult.getRows() == null || queryResult.getRows().isEmpty()) {
                return List.of();
            }
            List<String> columns = normalizeColumns(queryResult);
            Map<String, Integer> indexes = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                indexes.put(safe(columns.get(i)).trim().toLowerCase(Locale.ROOT), i);
            }
            int classIndex = firstIndex(indexes, "class_name", "class");
            int methodIndex = firstIndex(indexes, "method_name", "method");
            int descIndex = firstIndex(indexes, "method_desc", "desc", "descriptor");
            int jarIdIndex = firstIndex(indexes, "jar_id", "jid");
            int jarNameIndex = firstIndex(indexes, "jar_name");
            int resourceIdIndex = firstIndex(indexes, "rid", "resource_id");
            int resourcePathIndex = firstIndex(indexes, "resource_path", "path");

            List<SearchResultDto> out = new ArrayList<>();
            int rowNo = 0;
            for (List<Object> row : queryResult.getRows()) {
                rowNo++;
                String className = valueAt(row, classIndex);
                String methodName = valueAt(row, methodIndex);
                String methodDesc = valueAt(row, descIndex);
                int jarId = intValueAt(row, jarIdIndex);
                String jarName = valueAt(row, jarNameIndex);
                String resourcePath = valueAt(row, resourcePathIndex);
                int resourceId = intValueAt(row, resourceIdIndex);

                if (!acceptScope(scope, resolver, jarId)) {
                    continue;
                }
                String origin = resolveOrigin(resolver, jarId);
                String navigate = "";
                if (resourceId > 0) {
                    navigate = "res:" + resourceId;
                } else if (!resourcePath.isBlank()) {
                    navigate = "path:" + resourcePath;
                } else if (!className.isBlank()) {
                    navigate = "cls:" + normalizeClass(className) + "|" + jarId;
                }
                if (className.isBlank()) {
                    className = contributor + " row " + rowNo;
                }
                String preview = buildRowPreview(columns, row);
                out.add(new SearchResultDto(
                        className,
                        methodName,
                        methodDesc,
                        jarName,
                        jarId,
                        preview,
                        contributor,
                        origin,
                        navigate
                ));
            }
            return out;
        }

        private List<String> normalizeColumns(QueryResult queryResult) {
            List<String> columns = queryResult.getColumns();
            if (columns != null && !columns.isEmpty()) {
                return columns;
            }
            List<List<Object>> rows = queryResult.getRows();
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            List<Object> row = rows.get(0);
            if (row == null || row.isEmpty()) {
                return List.of();
            }
            List<String> generated = new ArrayList<>(row.size());
            for (int i = 0; i < row.size(); i++) {
                generated.add("col" + (i + 1));
            }
            return generated;
        }

        private String buildRowPreview(List<String> columns, List<Object> row) {
            if (columns == null || columns.isEmpty() || row == null || row.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            int size = Math.min(columns.size(), row.size());
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                sb.append(safe(columns.get(i))).append('=').append(safe(String.valueOf(row.get(i))));
            }
            return sb.toString();
        }

        private int firstIndex(Map<String, Integer> indexes, String... names) {
            if (indexes == null || indexes.isEmpty() || names == null) {
                return -1;
            }
            for (String name : names) {
                if (name == null) {
                    continue;
                }
                Integer index = indexes.get(name.toLowerCase(Locale.ROOT));
                if (index != null) {
                    return index;
                }
            }
            return -1;
        }

        private String valueAt(List<Object> row, int index) {
            if (row == null || index < 0 || index >= row.size()) {
                return "";
            }
            Object val = row.get(index);
            return val == null ? "" : String.valueOf(val);
        }

        private int intValueAt(List<Object> row, int index) {
            if (row == null || index < 0 || index >= row.size()) {
                return 0;
            }
            Object val = row.get(index);
            if (val == null) {
                return 0;
            }
            if (val instanceof Number n) {
                return n.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(val).trim());
            } catch (Exception ignored) {
                return 0;
            }
        }

        private boolean acceptScope(CallGraphScope scope, SearchOriginResolver resolver, int jarId) {
            if (scope == null || scope == CallGraphScope.ALL) {
                return true;
            }
            ProjectOrigin origin = resolver.resolve(jarId);
            return switch (scope) {
                case APP -> origin == ProjectOrigin.APP;
                case LIBRARY -> origin == ProjectOrigin.LIBRARY;
                case SDK -> origin == ProjectOrigin.SDK;
                case GENERATED -> origin == ProjectOrigin.GENERATED;
                case EXCLUDED -> origin == ProjectOrigin.EXCLUDED;
                case ALL -> true;
            };
        }

        private String resolveOrigin(SearchOriginResolver resolver, int jarId) {
            return resolver.resolve(jarId).value();
        }

        private <T> List<T> trimLimit(List<T> input, int limit) {
            if (input == null || input.isEmpty()) {
                return List.of();
            }
            if (limit <= 0 || input.size() <= limit) {
                return input;
            }
            return new ArrayList<>(input.subList(0, limit));
        }

        private String resultKey(SearchResultDto item) {
            return safe(item.contributor()) + "|" +
                    safe(item.className()) + "|" +
                    safe(item.methodName()) + "|" +
                    safe(item.methodDesc()) + "|" +
                    item.jarId() + "|" +
                    safe(item.navigateValue()) + "|" +
                    safe(item.preview());
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
                        path,
                        "binary",
                        "unknown",
                        "path:" + path
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

        private SearchOriginResolver loadScopeResolver() {
            long latestBuildSeq = readLatestBuildSeq();
            if (latestBuildSeq <= 0) {
                return SearchOriginResolver.empty();
            }
            SearchOriginResolver cached = scopeResolver;
            if (latestBuildSeq == scopeResolverBuildSeq && cached != null) {
                return cached;
            }
            synchronized (this) {
                if (latestBuildSeq == scopeResolverBuildSeq && scopeResolver != null) {
                    return scopeResolver;
                }
                SearchOriginResolver reloaded = loadScopeResolverFromDb(latestBuildSeq);
                scopeResolverBuildSeq = latestBuildSeq;
                scopeResolver = reloaded;
                return reloaded;
            }
        }

        private long readLatestBuildSeq() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT build_seq FROM project_model_meta ORDER BY build_seq DESC LIMIT 1");
                     ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } catch (Exception ex) {
                logger.debug("load search scope build_seq fail: {}", ex.toString());
            }
            return -1L;
        }

        private SearchOriginResolver loadScopeResolverFromDb(long buildSeq) {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                List<OriginPathRule> rules = loadOriginPathRules(connection, buildSeq);
                Map<Integer, ProjectOrigin> jarOrigins = loadJarOrigins(connection, rules);
                return new SearchOriginResolver(jarOrigins);
            } catch (Exception ex) {
                logger.debug("load search scope resolver fail: {}", ex.toString());
                return SearchOriginResolver.empty();
            }
        }

        private List<OriginPathRule> loadOriginPathRules(Connection connection, long buildSeq) {
            List<OriginPathRule> out = new ArrayList<>();
            String rootSql = "SELECT root_path, origin_kind FROM project_model_root WHERE build_seq = ?";
            try (PreparedStatement statement = connection.prepareStatement(rootSql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Path path = normalizeFsPath(rs.getString("root_path"));
                        if (path == null) {
                            continue;
                        }
                        out.add(new OriginPathRule(path, ProjectOrigin.fromValue(rs.getString("origin_kind"))));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load search scope root rules fail: {}", ex.toString());
            }
            String entrySql = "SELECT entry_path, origin_kind FROM project_model_entry " +
                    "WHERE build_seq = ? AND entry_kind = 'archive'";
            try (PreparedStatement statement = connection.prepareStatement(entrySql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Path path = normalizeFsPath(rs.getString("entry_path"));
                        if (path == null) {
                            continue;
                        }
                        out.add(new OriginPathRule(path, ProjectOrigin.fromValue(rs.getString("origin_kind"))));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load search scope archive rules fail: {}", ex.toString());
            }
            out.sort(Comparator.comparingInt((OriginPathRule item) -> item.path().getNameCount()).reversed());
            return out;
        }

        private Map<Integer, ProjectOrigin> loadJarOrigins(Connection connection, List<OriginPathRule> rules) {
            if (rules == null || rules.isEmpty()) {
                return Map.of();
            }
            Map<Integer, ProjectOrigin> out = new HashMap<>();
            String sql = "SELECT jid, jar_abs_path FROM jar_table ORDER BY jid ASC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int jarId = rs.getInt("jid");
                    Path jarPath = normalizeFsPath(rs.getString("jar_abs_path"));
                    ProjectOrigin origin = resolveOriginByPath(jarPath, rules);
                    out.put(jarId, origin);
                }
            } catch (Exception ex) {
                logger.debug("load search scope jar origins fail: {}", ex.toString());
            }
            return out;
        }

        private ProjectOrigin resolveOriginByPath(Path path, List<OriginPathRule> rules) {
            if (path == null || rules == null || rules.isEmpty()) {
                return ProjectOrigin.APP;
            }
            for (OriginPathRule rule : rules) {
                if (rule == null || rule.path() == null) {
                    continue;
                }
                try {
                    if (path.startsWith(rule.path())) {
                        return rule.origin();
                    }
                } catch (Exception ignored) {
                }
            }
            return ProjectOrigin.APP;
        }

        private Path normalizeFsPath(String raw) {
            String value = safe(raw).trim();
            if (value.isBlank()) {
                return null;
            }
            try {
                return Paths.get(value).toAbsolutePath().normalize();
            } catch (Exception ex) {
                try {
                    return Paths.get(value).normalize();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        private record SearchRunResult(List<SearchResultDto> results, String statusText) {
        }

        private record OriginPathRule(Path path, ProjectOrigin origin) {
        }

        private record SearchOriginResolver(Map<Integer, ProjectOrigin> jarOrigins) {
            private static SearchOriginResolver empty() {
                return new SearchOriginResolver(Map.of());
            }

            private ProjectOrigin resolve(int jarId) {
                if (jarId <= 0) {
                    return ProjectOrigin.APP;
                }
                ProjectOrigin origin = jarOrigins.get(jarId);
                return origin == null ? ProjectOrigin.APP : origin;
            }
        }
    }

    private static final class DefaultStructureFacade implements StructureFacade {
        private static final int MAX_FIELD_COUNT = 500;
        private static final int MAX_METHOD_COUNT = 500;
        private static final int MAX_INNER_CLASS_COUNT = 200;
        private static final int MAX_OVERRIDE_RELATIONS = 8;

        @Override
        public StructureSnapshotDto snapshot(String className, Integer jarId) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return StructureSnapshotDto.empty(
                        normalizeClass(className),
                        normalizeJarId(jarId),
                        tr("引擎尚未就绪", "engine is not ready")
                );
            }
            String ownerClass = normalizeClass(className);
            if (ownerClass.isBlank()) {
                return StructureSnapshotDto.empty(
                        "",
                        normalizeJarId(jarId),
                        tr("类名为空", "class is empty")
                );
            }
            Integer ownerJarId = normalizeJarId(jarId);
            try {
                ClassResult classResult = engine.getClassByClass(ownerClass);
                if (ownerJarId == null && classResult != null && classResult.getJarId() > 0) {
                    ownerJarId = classResult.getJarId();
                }
                String ownerJarName = resolveJarName(engine, classResult, ownerJarId);

                List<StructureItemDto> items = new ArrayList<>();
                items.add(new StructureItemDto(
                        "class",
                        0,
                        buildClassLabel(ownerClass, classResult),
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        ownerJarName,
                        true
                ));
                appendInterfaces(engine, ownerClass, ownerJarId, items);
                appendFields(engine, ownerClass, ownerJarId, ownerJarName, items);
                appendMethods(engine, ownerClass, ownerJarId, ownerJarName, items);
                appendInnerClasses(ownerClass, ownerJarId, items);
                return new StructureSnapshotDto(
                        ownerClass,
                        ownerJarId,
                        items,
                        tr("条目数: ", "items: ") + items.size()
                );
            } catch (Throwable ex) {
                logger.debug("structure snapshot failed: {}", ex.toString());
                return StructureSnapshotDto.empty(
                        ownerClass,
                        ownerJarId,
                        tr("结构异常: ", "structure error: ") + safe(ex.getMessage())
                );
            }
        }

        private void appendInterfaces(CoreEngine engine,
                                      String ownerClass,
                                      Integer ownerJarId,
                                      List<StructureItemDto> items) {
            List<String> interfaces = engine.getInterfacesByClass(ownerClass);
            if (interfaces == null || interfaces.isEmpty()) {
                return;
            }
            List<String> deduped = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (String item : interfaces) {
                String normalized = normalizeClass(item);
                if (normalized.isBlank()) {
                    continue;
                }
                if (!seen.add(normalized)) {
                    continue;
                }
                deduped.add(normalized);
            }
            if (deduped.isEmpty()) {
                return;
            }
            items.add(section("interfaces (" + deduped.size() + ")"));
            for (String iface : deduped) {
                Integer jarId = ownerJarId;
                String jarName = "";
                try {
                    Integer resolved = engine.getJarIdByClass(iface);
                    if (resolved != null && resolved > 0) {
                        jarId = resolved;
                    }
                    jarName = safe(engine.getJarByClass(iface));
                } catch (Exception ignored) {
                }
                items.add(new StructureItemDto(
                        "interface",
                        1,
                        "I " + iface + formatJarSuffix(jarName, jarId),
                        iface,
                        "",
                        "",
                        jarId,
                        jarName,
                        true
                ));
            }
        }

        private void appendFields(CoreEngine engine,
                                  String ownerClass,
                                  Integer ownerJarId,
                                  String ownerJarName,
                                  List<StructureItemDto> items) {
            List<MemberEntity> members = engine.getMembersByClass(ownerClass);
            if (members == null || members.isEmpty()) {
                return;
            }
            List<MemberEntity> filtered = new ArrayList<>();
            for (MemberEntity member : members) {
                if (member == null) {
                    continue;
                }
                Integer memberJarId = normalizeJarId(member.getJarId());
                if (ownerJarId != null && !ownerJarId.equals(memberJarId)) {
                    continue;
                }
                filtered.add(member);
            }
            filtered.sort(Comparator
                    .comparing((MemberEntity item) -> safe(item.getMemberName()))
                    .thenComparing(item -> safe(item.getMethodDesc()))
                    .thenComparing(item -> safe(item.getTypeClassName())));

            boolean truncated = filtered.size() > MAX_FIELD_COUNT;
            if (truncated) {
                filtered = new ArrayList<>(filtered.subList(0, MAX_FIELD_COUNT));
            }
            items.add(section("fields (" + filtered.size() + (truncated ? "+" : "") + ")"));
            for (MemberEntity member : filtered) {
                items.add(new StructureItemDto(
                        "field",
                        1,
                        buildFieldLabel(member),
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        ownerJarName,
                        false
                ));
            }
            if (truncated) {
                items.add(new StructureItemDto(
                        "hint",
                        1,
                        "... more fields omitted",
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        ownerJarName,
                        false
                ));
            }
        }

        private void appendMethods(CoreEngine engine,
                                   String ownerClass,
                                   Integer ownerJarId,
                                   String ownerJarName,
                                   List<StructureItemDto> items) {
            List<MethodResult> methods = engine.getMethodsByClass(ownerClass);
            if (methods == null || methods.isEmpty()) {
                return;
            }
            List<MethodResult> filtered = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (MethodResult method : methods) {
                if (method == null) {
                    continue;
                }
                int methodJarId = method.getJarId();
                if (ownerJarId != null && ownerJarId != methodJarId) {
                    continue;
                }
                String key = safe(method.getMethodName()) + "|" + safe(method.getMethodDesc()) + "|" + methodJarId;
                if (!seen.add(key)) {
                    continue;
                }
                filtered.add(method);
            }
            filtered.sort(Comparator
                    .comparing((MethodResult item) -> safe(item.getMethodName()))
                    .thenComparing(item -> safe(item.getMethodDesc())));
            boolean truncated = filtered.size() > MAX_METHOD_COUNT;
            if (truncated) {
                filtered = new ArrayList<>(filtered.subList(0, MAX_METHOD_COUNT));
            }

            items.add(section("methods (" + filtered.size() + (truncated ? "+" : "") + ")"));
            for (MethodResult method : filtered) {
                items.add(new StructureItemDto(
                        "method",
                        1,
                        buildMethodLabel(method),
                        ownerClass,
                        safe(method.getMethodName()),
                        safe(method.getMethodDesc()),
                        method.getJarId() > 0 ? method.getJarId() : ownerJarId,
                        safe(method.getJarName()),
                        true
                ));
                appendOverrideRelations(engine, ownerClass, ownerJarId, method, items);
            }
            if (truncated) {
                items.add(new StructureItemDto(
                        "hint",
                        1,
                        "... more methods omitted",
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        ownerJarName,
                        false
                ));
            }
        }

        private void appendOverrideRelations(CoreEngine engine,
                                             String ownerClass,
                                             Integer ownerJarId,
                                             MethodResult ownerMethod,
                                             List<StructureItemDto> items) {
            List<MethodResult> supers = dedupeMethodRelations(engine.getSuperImpls(
                    ownerClass,
                    ownerMethod.getMethodName(),
                    ownerMethod.getMethodDesc(),
                    ownerJarId
            ));
            List<MethodResult> impls = dedupeMethodRelations(engine.getImpls(
                    ownerClass,
                    ownerMethod.getMethodName(),
                    ownerMethod.getMethodDesc(),
                    ownerJarId
            ));

            supers.removeIf(item -> sameMethod(item, ownerClass, ownerMethod.getMethodName(), ownerMethod.getMethodDesc(), ownerJarId));
            impls.removeIf(item -> sameMethod(item, ownerClass, ownerMethod.getMethodName(), ownerMethod.getMethodDesc(), ownerJarId));

            if (!supers.isEmpty()) {
                int total = supers.size();
                List<MethodResult> limited = trimMethodRelations(supers);
                items.add(new StructureItemDto(
                        "relation",
                        2,
                        "\u2191 overrides (" + total + ")",
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        "",
                        false
                ));
                for (MethodResult target : limited) {
                    items.add(new StructureItemDto(
                            "override-up",
                            3,
                            buildRelationLabel(target),
                            safe(target.getClassName()),
                            safe(target.getMethodName()),
                            safe(target.getMethodDesc()),
                            target.getJarId() > 0 ? target.getJarId() : null,
                            safe(target.getJarName()),
                            true
                    ));
                }
                if (total > limited.size()) {
                    items.add(new StructureItemDto(
                            "hint",
                            3,
                            "... more override targets omitted",
                            ownerClass,
                            "",
                            "",
                            ownerJarId,
                            "",
                            false
                    ));
                }
            }
            if (!impls.isEmpty()) {
                int total = impls.size();
                List<MethodResult> limited = trimMethodRelations(impls);
                items.add(new StructureItemDto(
                        "relation",
                        2,
                        "\u2193 overridden-by (" + total + ")",
                        ownerClass,
                        "",
                        "",
                        ownerJarId,
                        "",
                        false
                ));
                for (MethodResult target : limited) {
                    items.add(new StructureItemDto(
                            "override-down",
                            3,
                            buildRelationLabel(target),
                            safe(target.getClassName()),
                            safe(target.getMethodName()),
                            safe(target.getMethodDesc()),
                            target.getJarId() > 0 ? target.getJarId() : null,
                            safe(target.getJarName()),
                            true
                    ));
                }
                if (total > limited.size()) {
                    items.add(new StructureItemDto(
                            "hint",
                            3,
                            "... more override targets omitted",
                            ownerClass,
                            "",
                            "",
                            ownerJarId,
                            "",
                            false
                    ));
                }
            }
        }

        private void appendInnerClasses(String ownerClass,
                                        Integer ownerJarId,
                                        List<StructureItemDto> items) {
            List<InnerClassRow> inners = loadInnerClasses(ownerClass, ownerJarId);
            if (inners.isEmpty()) {
                return;
            }
            items.add(section("inner classes (" + inners.size() + ")"));
            for (InnerClassRow inner : inners) {
                items.add(new StructureItemDto(
                        "inner-class",
                        1,
                        "C " + inner.className() + formatJarSuffix(inner.jarName(), inner.jarId()),
                        inner.className(),
                        "",
                        "",
                        inner.jarId(),
                        inner.jarName(),
                        true
                ));
            }
        }

        private List<InnerClassRow> loadInnerClasses(String ownerClass, Integer ownerJarId) {
            List<InnerClassRow> out = new ArrayList<>();
            String sql = "SELECT DISTINCT c.class_name AS class_name, c.jar_id AS jar_id, " +
                    "COALESCE(j.jar_name, '') AS jar_name " +
                    "FROM class_table c LEFT JOIN jar_table j ON c.jar_id = j.jid " +
                    "WHERE c.class_name LIKE ? AND c.class_name <> ? " +
                    "AND instr(substr(c.class_name, length(?) + 2), '$') = 0 " +
                    (ownerJarId != null ? "AND c.jar_id = ? " : "") +
                    "ORDER BY c.class_name ASC, c.jar_id ASC LIMIT ?";
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int index = 1;
                    String prefix = ownerClass + "$";
                    statement.setString(index++, prefix + "%");
                    statement.setString(index++, ownerClass);
                    statement.setString(index++, ownerClass);
                    if (ownerJarId != null) {
                        statement.setInt(index++, ownerJarId);
                    }
                    statement.setInt(index, MAX_INNER_CLASS_COUNT);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            out.add(new InnerClassRow(
                                    safe(rs.getString("class_name")),
                                    rs.getInt("jar_id"),
                                    safe(rs.getString("jar_name"))
                            ));
                        }
                    }
                }
            } catch (Exception ex) {
                logger.debug("load inner classes failed: {}", ex.toString());
            }
            return out;
        }

        private List<MethodResult> dedupeMethodRelations(List<MethodResult> raw) {
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<MethodResult> out = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (MethodResult item : raw) {
                if (item == null) {
                    continue;
                }
                String key = safe(item.getClassName()) + "|" +
                        safe(item.getMethodName()) + "|" +
                        safe(item.getMethodDesc()) + "|" +
                        item.getJarId();
                if (!seen.add(key)) {
                    continue;
                }
                out.add(item);
            }
            out.sort(Comparator
                    .comparing((MethodResult item) -> safe(item.getClassName()))
                    .thenComparing(item -> safe(item.getMethodName()))
                    .thenComparing(item -> safe(item.getMethodDesc()))
                    .thenComparingInt(MethodResult::getJarId));
            return out;
        }

        private List<MethodResult> trimMethodRelations(List<MethodResult> input) {
            if (input == null || input.isEmpty()) {
                return List.of();
            }
            if (input.size() <= MAX_OVERRIDE_RELATIONS) {
                return input;
            }
            return new ArrayList<>(input.subList(0, MAX_OVERRIDE_RELATIONS));
        }

        private boolean sameMethod(MethodResult candidate,
                                   String className,
                                   String methodName,
                                   String methodDesc,
                                   Integer jarId) {
            if (candidate == null) {
                return false;
            }
            if (!safe(candidate.getClassName()).equals(safe(className))) {
                return false;
            }
            if (!safe(candidate.getMethodName()).equals(safe(methodName))) {
                return false;
            }
            if (!safe(candidate.getMethodDesc()).equals(safe(methodDesc))) {
                return false;
            }
            if (jarId == null || jarId <= 0) {
                return true;
            }
            return candidate.getJarId() == jarId;
        }

        private String buildClassLabel(String className, ClassResult result) {
            if (result == null) {
                return "class " + className;
            }
            StringBuilder sb = new StringBuilder();
            if (result.getIsInterfaceInt() == 1) {
                sb.append("interface ");
            } else {
                sb.append("class ");
            }
            sb.append(className);
            String superClass = normalizeClass(result.getSuperClassName());
            if (!superClass.isBlank() && !"java/lang/Object".equals(superClass)) {
                sb.append(" extends ").append(superClass);
            }
            return sb.toString();
        }

        private String buildFieldLabel(MemberEntity member) {
            String access = formatAccess(member.getModifiers());
            String type = formatFieldType(member.getMethodDesc(), member.getTypeClassName());
            String name = safe(member.getMemberName());
            StringBuilder sb = new StringBuilder("F ");
            if (!access.isBlank()) {
                sb.append(access).append(' ');
            }
            sb.append(name);
            if (!type.isBlank()) {
                sb.append(" : ").append(type);
            }
            return sb.toString();
        }

        private String buildMethodLabel(MethodResult method) {
            String access = formatAccess(method.getAccessInt());
            String signature = formatMethodSignature(method.getMethodName(), method.getMethodDesc());
            StringBuilder sb = new StringBuilder("M ");
            if (!access.isBlank()) {
                sb.append(access).append(' ');
            }
            sb.append(signature);
            return sb.toString();
        }

        private String buildRelationLabel(MethodResult method) {
            return "M " + safe(method.getClassName()) + "#" +
                    formatMethodSignature(method.getMethodName(), method.getMethodDesc()) +
                    formatJarSuffix(method.getJarName(), method.getJarId());
        }

        private String formatFieldType(String desc, String fallbackType) {
            String normalizedFallback = normalizeClass(fallbackType);
            try {
                String value = safe(desc).trim();
                if (!value.isBlank()) {
                    return simpleTypeName(Type.getType(value));
                }
            } catch (Exception ignored) {
            }
            if (!normalizedFallback.isBlank()) {
                return normalizedFallback;
            }
            return "";
        }

        private String formatMethodSignature(String methodName, String methodDesc) {
            String normalizedName = safe(methodName);
            if ("<init>".equals(normalizedName)) {
                normalizedName = "[init]";
            } else if ("<clinit>".equals(normalizedName)) {
                normalizedName = "[clinit]";
            }
            try {
                Type type = Type.getMethodType(safe(methodDesc));
                Type[] argTypes = type.getArgumentTypes();
                StringBuilder sb = new StringBuilder();
                sb.append(normalizedName).append('(');
                for (int i = 0; i < argTypes.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(simpleTypeName(argTypes[i]));
                }
                sb.append(") : ").append(simpleTypeName(type.getReturnType()));
                return sb.toString();
            } catch (Exception ignored) {
                return normalizedName + safe(methodDesc);
            }
        }

        private String simpleTypeName(Type type) {
            if (type == null) {
                return "Object";
            }
            try {
                String className = type.getClassName();
                int index = className.lastIndexOf('.');
                if (index >= 0 && index < className.length() - 1) {
                    return className.substring(index + 1);
                }
                return className;
            } catch (Exception ignored) {
                return type.toString();
            }
        }

        private String formatAccess(int access) {
            if (access <= 0) {
                return "";
            }
            List<String> parts = new ArrayList<>(8);
            if ((access & Opcodes.ACC_PUBLIC) != 0) {
                parts.add("public");
            } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
                parts.add("protected");
            } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
                parts.add("private");
            }
            if ((access & Opcodes.ACC_STATIC) != 0) {
                parts.add("static");
            }
            if ((access & Opcodes.ACC_FINAL) != 0) {
                parts.add("final");
            }
            if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                parts.add("abstract");
            }
            if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
                parts.add("synchronized");
            }
            if ((access & Opcodes.ACC_NATIVE) != 0) {
                parts.add("native");
            }
            if ((access & Opcodes.ACC_VOLATILE) != 0) {
                parts.add("volatile");
            }
            if ((access & Opcodes.ACC_TRANSIENT) != 0) {
                parts.add("transient");
            }
            return String.join(" ", parts);
        }

        private String resolveJarName(CoreEngine engine, ClassResult result, Integer jarId) {
            if (result != null && jarId != null && jarId == result.getJarId()) {
                return safe(result.getJarName());
            }
            if (jarId == null) {
                return result == null ? "" : safe(result.getJarName());
            }
            String fromEngine = safe(engine.getJarNameById(jarId));
            if (!fromEngine.isBlank()) {
                return fromEngine;
            }
            return result == null ? "" : safe(result.getJarName());
        }

        private Integer normalizeJarId(Integer jarId) {
            if (jarId == null || jarId <= 0) {
                return null;
            }
            return jarId;
        }

        private StructureItemDto section(String label) {
            return new StructureItemDto("section", 0, safe(label), "", "", "", null, "", false);
        }

        private String formatJarSuffix(String jarName, Integer jarId) {
            String normalizedName = safe(jarName).trim();
            if (!normalizedName.isBlank()) {
                return " [" + normalizedName + "]";
            }
            if (jarId != null && jarId > 0) {
                return " [jar:" + jarId + "]";
            }
            return "";
        }

        private String formatJarSuffix(String jarName, int jarId) {
            return formatJarSuffix(jarName, jarId > 0 ? jarId : null);
        }

        private record InnerClassRow(String className, int jarId, String jarName) {
        }
    }

    private static final class DefaultCallGraphFacade implements CallGraphFacade {
        private volatile long resolverBuildSeq = -1L;
        private volatile OriginScopeResolver resolver = OriginScopeResolver.empty();

        @Override
        public CallGraphSnapshotDto snapshot() {
            CallGraphScope scope = CallGraphScope.fromValue(STATE.callGraphScope);
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                return new CallGraphSnapshotDto(
                        STATE.currentJar,
                        STATE.currentClass,
                        formatCurrentMethod(),
                        scope.value(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );
            }

            MethodNavDto current = STATE.currentMethod;
            String className = STATE.currentClass;
            OriginScopeResolver localResolver = scope == CallGraphScope.ALL
                    ? OriginScopeResolver.empty()
                    : loadResolver();
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
                    if (!isAccepted(item, scope, localResolver)) {
                        continue;
                    }
                    all.add(toMethodNav(item));
                }
            }
            if (current != null) {
                Integer jarId = current.jarId() <= 0 ? null : current.jarId();
                List<MethodResult> callers = engine.getCallers(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc(),
                        jarId);
                List<MethodResult> callees = engine.getCallee(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc(),
                        jarId);
                List<MethodResult> impls = engine.getImpls(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc(),
                        jarId);
                List<MethodResult> superImpls = engine.getSuperImpls(
                        normalizeClass(current.className()),
                        current.methodName(),
                        current.methodDesc(),
                        jarId);
                for (MethodResult item : callers) {
                    if (!isAccepted(item, scope, localResolver)) {
                        continue;
                    }
                    caller.add(toMethodNav(item));
                }
                for (MethodResult item : callees) {
                    if (!isAccepted(item, scope, localResolver)) {
                        continue;
                    }
                    callee.add(toMethodNav(item));
                }
                for (MethodResult item : impls) {
                    if (!isAccepted(item, scope, localResolver)) {
                        continue;
                    }
                    impl.add(toMethodNav(item));
                }
                for (MethodResult item : superImpls) {
                    if (!isAccepted(item, scope, localResolver)) {
                        continue;
                    }
                    superImpl.add(toMethodNav(item));
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
                    scope.value(),
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
        public String scope() {
            return CallGraphScope.fromValue(STATE.callGraphScope).value();
        }

        @Override
        public void setScope(String scope) {
            STATE.callGraphScope = CallGraphScope.fromValue(scope).value();
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

        private boolean isAccepted(MethodResult method, CallGraphScope scope, OriginScopeResolver resolver) {
            if (method == null) {
                return false;
            }
            if (scope == CallGraphScope.ALL) {
                return true;
            }
            ProjectOrigin origin = resolver.resolve(method.getJarId());
            return switch (scope) {
                case APP -> origin == ProjectOrigin.APP;
                case LIBRARY -> origin == ProjectOrigin.LIBRARY;
                case SDK -> origin == ProjectOrigin.SDK;
                case GENERATED -> origin == ProjectOrigin.GENERATED;
                case EXCLUDED -> origin == ProjectOrigin.EXCLUDED;
                case ALL -> true;
            };
        }

        private OriginScopeResolver loadResolver() {
            long latestBuildSeq = readLatestProjectModelBuildSeq();
            if (latestBuildSeq <= 0) {
                return OriginScopeResolver.empty();
            }
            OriginScopeResolver cached = resolver;
            if (latestBuildSeq == resolverBuildSeq && cached != null) {
                return cached;
            }
            synchronized (this) {
                if (latestBuildSeq == resolverBuildSeq && resolver != null) {
                    return resolver;
                }
                OriginScopeResolver reloaded = loadResolverFromDb(latestBuildSeq);
                resolverBuildSeq = latestBuildSeq;
                resolver = reloaded;
                return reloaded;
            }
        }

        private long readLatestProjectModelBuildSeq() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT build_seq FROM project_model_meta ORDER BY build_seq DESC LIMIT 1");
                     ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } catch (Exception ex) {
                logger.debug("load call scope build_seq fail: {}", ex.toString());
            }
            return -1L;
        }

        private OriginScopeResolver loadResolverFromDb(long buildSeq) {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                List<OriginPathRule> rules = loadOriginPathRules(connection, buildSeq);
                Map<Integer, ProjectOrigin> jarOrigins = loadJarOrigins(connection, rules);
                return new OriginScopeResolver(jarOrigins);
            } catch (Exception ex) {
                logger.debug("load call scope resolver fail: {}", ex.toString());
                return OriginScopeResolver.empty();
            }
        }

        private List<OriginPathRule> loadOriginPathRules(Connection connection, long buildSeq) {
            List<OriginPathRule> out = new ArrayList<>();
            String rootSql = "SELECT root_path, origin_kind FROM project_model_root WHERE build_seq = ?";
            try (PreparedStatement statement = connection.prepareStatement(rootSql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Path path = normalizePath(rs.getString("root_path"));
                        if (path == null) {
                            continue;
                        }
                        out.add(new OriginPathRule(path, ProjectOrigin.fromValue(rs.getString("origin_kind"))));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load call scope root rules fail: {}", ex.toString());
            }
            String entrySql = "SELECT entry_path, origin_kind FROM project_model_entry " +
                    "WHERE build_seq = ? AND entry_kind = 'archive'";
            try (PreparedStatement statement = connection.prepareStatement(entrySql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Path path = normalizePath(rs.getString("entry_path"));
                        if (path == null) {
                            continue;
                        }
                        out.add(new OriginPathRule(path, ProjectOrigin.fromValue(rs.getString("origin_kind"))));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load call scope archive rules fail: {}", ex.toString());
            }
            out.sort(Comparator.comparingInt((OriginPathRule item) -> item.path().getNameCount()).reversed());
            return out;
        }

        private Map<Integer, ProjectOrigin> loadJarOrigins(Connection connection, List<OriginPathRule> rules) {
            if (rules == null || rules.isEmpty()) {
                return Map.of();
            }
            Map<Integer, ProjectOrigin> out = new HashMap<>();
            String sql = "SELECT jid, jar_abs_path FROM jar_table ORDER BY jid ASC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int jarId = rs.getInt("jid");
                    Path jarPath = normalizePath(rs.getString("jar_abs_path"));
                    ProjectOrigin origin = resolveOriginByPath(jarPath, rules);
                    out.put(jarId, origin);
                }
            } catch (Exception ex) {
                logger.debug("load call scope jar origins fail: {}", ex.toString());
            }
            return out;
        }

        private ProjectOrigin resolveOriginByPath(Path path, List<OriginPathRule> rules) {
            if (path == null || rules == null || rules.isEmpty()) {
                return ProjectOrigin.APP;
            }
            for (OriginPathRule rule : rules) {
                if (rule == null || rule.path() == null) {
                    continue;
                }
                try {
                    if (path.startsWith(rule.path())) {
                        return rule.origin();
                    }
                } catch (Exception ignored) {
                }
            }
            return ProjectOrigin.APP;
        }

        private Path normalizePath(String raw) {
            String value = safe(raw).trim();
            if (value.isBlank()) {
                return null;
            }
            try {
                return Paths.get(value).toAbsolutePath().normalize();
            } catch (Exception ex) {
                try {
                    return Paths.get(value).normalize();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        private record OriginPathRule(Path path, ProjectOrigin origin) {
        }

        private record OriginScopeResolver(Map<Integer, ProjectOrigin> jarOrigins) {
            private static OriginScopeResolver empty() {
                return new OriginScopeResolver(Map.of());
            }

            private ProjectOrigin resolve(Integer jarId) {
                if (jarId == null || jarId <= 0) {
                    return ProjectOrigin.APP;
                }
                ProjectOrigin origin = jarOrigins.get(jarId);
                return origin == null ? ProjectOrigin.APP : origin;
            }
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
                    TaintCache.cache.size(),
                    STATE.chainsStatusText
            );
        }

        @Override
        public void apply(ChainsSettingsDto settings) {
            if (settings == null) {
                return;
            }
            STATE.chainsSettings = settings;
            STATE.chainsStatusText = tr("链路设置已更新", "chains settings updated");
        }

        @Override
        public void startDfs() {
            if (!STATE.chainsRunning.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().name("gui-runtime-dfs").start(() -> {
                try {
                    ChainsSettingsDto cfg = STATE.chainsSettings;
                    STATE.chainsStatusText = tr("DFS 执行中...", "DFS running...");
                    DfsRunOutcome dfsOutcome = runDfsGraphOnly(cfg);
                    List<DFSResult> resultList = dfsOutcome.results();
                    DFSUtil.save(resultList);
                    TaintCache.dfsCache.clear();
                    TaintCache.dfsCache.addAll(resultList);
                    TaintCache.cache.clear();
                    STATE.chainsStatusText = dfsOutcome.statusText();
                    if (cfg.taintEnabled()) {
                        TaintRunOutcome taintOutcome = runTaintGraphOnly(cfg, resultList);
                        List<TaintResult> taintResult = taintOutcome.results();
                        TaintCache.cache.addAll(taintResult);
                        STATE.chainsStatusText = dfsOutcome.statusText()
                                + " | " + taintOutcome.statusText();
                    }
                } catch (Throwable ex) {
                    STATE.chainsStatusText = tr("DFS 执行异常: ", "DFS error: ") + safe(ex.getMessage());
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
                        STATE.chainsStatusText = tr("请先执行 DFS", "run DFS first");
                        return;
                    }
                    ChainsSettingsDto cfg = STATE.chainsSettings;
                    List<DFSResult> snapshot = new ArrayList<>(TaintCache.dfsCache);
                    STATE.chainsStatusText = tr("污点分析执行中...", "taint running...");
                    TaintRunOutcome taintOutcome = runTaintGraphOnly(cfg, snapshot);
                    List<TaintResult> taintResult = taintOutcome.results();
                    TaintCache.cache.clear();
                    TaintCache.cache.addAll(taintResult);
                    STATE.chainsStatusText = taintOutcome.statusText();
                } catch (Throwable ex) {
                    STATE.chainsStatusText = tr("污点分析异常: ", "taint error: ") + safe(ex.getMessage());
                    logger.error("runtime taint failed: {}", ex.toString());
                }
            });
        }

        private static DfsRunOutcome runDfsGraphOnly(ChainsSettingsDto cfg) {
            FlowOptions options = toFlowOptions(cfg);
            GraphFlowService.DfsOutcome outcome = new GraphFlowService().runDfs(options, null);
            List<DFSResult> results = outcome == null ? List.of() : outcome.results();
            FlowStats stats = outcome == null ? FlowStats.empty() : outcome.stats();
            logger.info("runtime dfs backend=graph paths={}", results.size());
            return new DfsRunOutcome(
                    results,
                    formatGraphStatus(tr("DFS 后端: 图引擎", "DFS backend: graph"), stats)
            );
        }

        private static TaintRunOutcome runTaintGraphOnly(ChainsSettingsDto cfg, List<DFSResult> dfsSnapshot) {
            GraphFlowService flowService = new GraphFlowService();
            List<DFSResult> safeSnapshot = dfsSnapshot == null ? List.of() : dfsSnapshot;
            GraphFlowService.TaintOutcome outcome;
            if (hasExplicitSource(cfg)) {
                outcome = flowService.runTaint(toFlowOptions(cfg), null);
            } else {
                outcome = flowService.analyzeDfsResults(
                        safeSnapshot,
                        resolveFlowTimeoutMs(cfg),
                        resolveFlowMaxPaths(cfg),
                        null,
                        null
                );
            }
            List<TaintResult> results = outcome == null ? List.of() : outcome.results();
            FlowStats stats = outcome == null ? FlowStats.empty() : outcome.stats();
            logger.info("runtime taint backend=graph chains={}", results.size());
            return new TaintRunOutcome(
                    results,
                    formatGraphStatus(tr("污点后端: 图引擎", "Taint backend: graph"), stats)
            );
        }

        private static FlowOptions toFlowOptions(ChainsSettingsDto cfg) {
            int depth = Math.max(1, cfg == null ? 10 : cfg.maxDepth());
            int maxLimit = Math.max(1, cfg == null ? 30 : cfg.maxResultLimit());
            boolean searchAllSources = cfg != null && cfg.sourceNull();
            boolean onlyFromWeb = searchAllSources && cfg.onlyFromWeb();
            return FlowOptions.builder()
                    .fromSink(cfg == null || cfg.sinkSelected())
                    .searchAllSources(searchAllSources)
                    .depth(depth)
                    .maxLimit(maxLimit)
                    .maxPaths(resolveFlowMaxPaths(cfg))
                    .timeoutMs(resolveFlowTimeoutMs(cfg))
                    .onlyFromWeb(onlyFromWeb)
                    .blacklist(parseBlacklist(cfg == null ? "" : cfg.blacklist()))
                    .minEdgeConfidence(cfg == null ? "low" : cfg.minEdgeConfidence())
                    .sink(
                            normalizeClass(cfg == null ? "" : cfg.sinkClass()),
                            safe(cfg == null ? "" : cfg.sinkMethod()),
                            safe(cfg == null ? "" : cfg.sinkDesc())
                    )
                    .source(
                            normalizeClass(cfg == null ? "" : cfg.sourceClass()),
                            safe(cfg == null ? "" : cfg.sourceMethod()),
                            safe(cfg == null ? "" : cfg.sourceDesc())
                    )
                    .build();
        }

        private static boolean hasExplicitSource(ChainsSettingsDto cfg) {
            if (cfg == null || cfg.sourceNull() || !cfg.sourceEnabled()) {
                return false;
            }
            return !normalizeClass(cfg.sourceClass()).isBlank()
                    && !safe(cfg.sourceMethod()).isBlank()
                    && !safe(cfg.sourceDesc()).isBlank();
        }

        private static Integer resolveFlowTimeoutMs(ChainsSettingsDto cfg) {
            int depth = Math.max(1, cfg == null ? 10 : cfg.maxDepth());
            return depth > 64 ? 30_000 : 15_000;
        }

        private static Integer resolveFlowMaxPaths(ChainsSettingsDto cfg) {
            int maxRows = Math.max(1, cfg == null ? 30 : cfg.maxResultLimit());
            return Math.min(5_000, maxRows);
        }

        private static String formatGraphStatus(String base, FlowStats stats) {
            String status = safe(base);
            if (stats == null) {
                return status;
            }
            FlowTruncation truncation = stats.getTruncation();
            if (truncation == null || !truncation.truncated()) {
                return status;
            }
            String reason = safe(truncation.reason());
            if (reason.isBlank()) {
                return status + tr("，结果已截断", ", results truncated");
            }
            return status + tr("，截断原因: ", ", truncation reason: ") + reason;
        }

        private record DfsRunOutcome(List<DFSResult> results, String statusText) {
            private DfsRunOutcome {
                results = results == null ? List.of() : results;
                statusText = safe(statusText);
            }
        }

        private record TaintRunOutcome(List<TaintResult> results, String statusText) {
            private TaintRunOutcome {
                results = results == null ? List.of() : results;
                statusText = safe(statusText);
            }
        }

        @Override
        public void clearResults() {
            TaintCache.dfsCache.clear();
            TaintCache.cache.clear();
            STATE.chainsStatusText = tr("结果已清空", "results cleared");
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
        public EditorDeclarationResultDto resolveDeclaration(int caretOffset) {
            CoreEngine engine = EngineContext.getEngine();
            return EditorDeclarationResolver.resolve(engine, STATE.editorDocument, caretOffset);
        }

        @Override
        public EditorDeclarationResultDto resolveUsages(int caretOffset) {
            CoreEngine engine = EngineContext.getEngine();
            return EditorSymbolNavigationResolver.resolveUsages(engine, STATE.editorDocument, caretOffset);
        }

        @Override
        public EditorDeclarationResultDto resolveImplementations(int caretOffset) {
            CoreEngine engine = EngineContext.getEngine();
            return EditorSymbolNavigationResolver.resolveImplementations(engine, STATE.editorDocument, caretOffset);
        }

        @Override
        public EditorDeclarationResultDto resolveTypeHierarchy(int caretOffset) {
            CoreEngine engine = EngineContext.getEngine();
            return EditorSymbolNavigationResolver.resolveHierarchy(engine, STATE.editorDocument, caretOffset);
        }

        @Override
        public boolean openDeclarationTarget(EditorDeclarationTargetDto target) {
            if (target == null) {
                return false;
            }
            int caretOffset = target.caretOffset();
            if (target.localTarget()) {
                return applyCaretOnly(caretOffset, "declaration opened");
            }
            String className = normalizeClass(target.className());
            if (className.isBlank()) {
                className = normalizeClass(STATE.editorDocument.className());
            }
            Integer jarId = normalizeJarId(target.jarId() <= 0 ? null : target.jarId());
            if (target.methodTarget()) {
                if (className.isBlank()) {
                    return false;
                }
                openMethodInternal(
                        className,
                        target.methodName(),
                        target.methodDesc(),
                        jarId,
                        true
                );
                return true;
            }
            if (className.isBlank()) {
                return false;
            }
            openClassInternal(className, jarId, true);
            if (caretOffset >= 0) {
                applyCaretOnly(caretOffset, "declaration opened");
            }
            return true;
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
                    Path classPath = Paths.get(absPath);
                    if (isSourceTextPath(classPath)) {
                        content = readSourceText(classPath);
                        status = content.isEmpty() ? "source file is empty" : "source opened";
                    } else {
                        content = DecompileDispatcher.decompile(classPath, DecompileDispatcher.resolvePreferred());
                        if (content == null) {
                            status = "decompile output is empty";
                            content = "";
                        }
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

        private boolean isSourceTextPath(Path path) {
            if (path == null) {
                return false;
            }
            String name = safe(path.getFileName() == null ? "" : path.getFileName().toString())
                    .toLowerCase(Locale.ROOT);
            return name.endsWith(".java")
                    || name.endsWith(".kt")
                    || name.endsWith(".groovy");
        }

        private String readSourceText(Path path) {
            if (path == null || Files.notExists(path) || !Files.isRegularFile(path)) {
                return "";
            }
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.debug("read source text failed: {}", ex.toString());
                return "";
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

        private boolean applyCaretOnly(int caretOffset, String statusText) {
            EditorDocumentDto doc = STATE.editorDocument;
            String content = safe(doc.content());
            int bounded = Math.max(0, Math.min(content.length(), Math.max(0, caretOffset)));
            STATE.editorDocument = new EditorDocumentDto(
                    doc.className(),
                    doc.jarName(),
                    doc.jarId(),
                    doc.methodName(),
                    doc.methodDesc(),
                    doc.content(),
                    bounded,
                    safe(statusText)
            );
            return true;
        }
    }

    private static final class DefaultProjectTreeFacade implements ProjectTreeFacade {
        private static final int RESOURCE_TREE_FETCH_LIMIT = 200_000;
        private static final String CATEGORY_INPUT = "cat:input";
        private static final String CATEGORY_SOURCE = "cat:source";
        private static final String CATEGORY_RESOURCE = "cat:resource";
        private static final String CATEGORY_DEPENDENCY = "cat:dependency";
        private static final String CATEGORY_ORIGIN_APP = "origin:app";
        private static final String CATEGORY_ORIGIN_LIBRARY = "origin:library";
        private static final String CATEGORY_ORIGIN_SDK = "origin:sdk";
        private static final String CATEGORY_ORIGIN_GENERATED = "origin:generated";
        private static final String CATEGORY_ORIGIN_EXCLUDED = "origin:excluded";
        private static final List<ProjectOrigin> ORIGIN_ORDER = List.of(
                ProjectOrigin.APP,
                ProjectOrigin.LIBRARY,
                ProjectOrigin.SDK,
                ProjectOrigin.GENERATED,
                ProjectOrigin.EXCLUDED
        );

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
            if (raw.startsWith("path:")) {
                openPathNode(raw);
                return;
            }
            if (raw.startsWith("error:")) {
                emitTextWindow("Project Tree", raw.substring("error:".length()));
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

        private void openPathNode(String rawValue) {
            String text = safe(rawValue.substring("path:".length())).trim();
            if (text.isBlank()) {
                return;
            }
            Path path = normalizeFsPath(text);
            if (path == null) {
                emitTextWindow("Path", "invalid path: " + text);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("path: ").append(path).append('\n');
            sb.append("exists: ").append(Files.exists(path)).append('\n');
            sb.append("directory: ").append(Files.isDirectory(path)).append('\n');
            if (Files.exists(path) && !Files.isDirectory(path)) {
                try {
                    sb.append("size: ").append(Files.size(path)).append('\n');
                } catch (Exception ex) {
                    sb.append("size: unknown (").append(ex.getMessage()).append(")").append('\n');
                }
            }
            emitToolingWindow(new ToolingWindowRequest(
                    ToolingWindowAction.TEXT_VIEWER,
                    new ToolingWindowPayload.TextPayload("Path", sb.toString())
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
            ProjectModelSnapshot snapshot = loadProjectModelSnapshot();
            if (!snapshot.available()) {
                return projectModelMissingTree();
            }
            return buildSemanticTree(snapshot, classRows, resourceRows, jarRows, filterKeywordLower);
        }

        private List<TreeNodeDto> projectModelMissingTree() {
            TreeNodeDto reason = new TreeNodeDto(
                    "project_model_missing_rebuild",
                    "error:project_model_missing_rebuild",
                    false,
                    List.of()
            );
            return List.of(new TreeNodeDto("Project Model", "cat:project-model", true, List.of(reason)));
        }

        private List<TreeNodeDto> buildSemanticTree(ProjectModelSnapshot snapshot,
                                                    List<ClassFileEntity> classRows,
                                                    List<ResourceEntity> resourceRows,
                                                    List<JarEntity> jarRows,
                                                    String filterKeywordLower) {
            Map<Integer, String> jarNameById = new HashMap<>();
            Map<Integer, Path> jarPathById = new HashMap<>();
            for (JarEntity row : jarRows) {
                if (row == null) {
                    continue;
                }
                int jarId = row.getJid();
                String jarName = safe(row.getJarName()).trim();
                if (!jarName.isBlank()) {
                    jarNameById.put(jarId, jarName);
                }
                Path jarPath = normalizeFsPath(row.getJarAbsPath());
                if (jarPath != null) {
                    jarPathById.put(jarId, jarPath);
                }
            }
            SemanticOriginResolver resolver = new SemanticOriginResolver(snapshot, jarPathById);
            Map<ProjectOrigin, MutableTreeNode> categories = new EnumMap<>(ProjectOrigin.class);
            categories.put(ProjectOrigin.APP, new MutableTreeNode("App", CATEGORY_ORIGIN_APP, true));
            categories.put(ProjectOrigin.LIBRARY, new MutableTreeNode("Libraries", CATEGORY_ORIGIN_LIBRARY, true));
            categories.put(ProjectOrigin.SDK, new MutableTreeNode("SDK", CATEGORY_ORIGIN_SDK, true));
            categories.put(ProjectOrigin.GENERATED, new MutableTreeNode("Generated", CATEGORY_ORIGIN_GENERATED, true));
            categories.put(ProjectOrigin.EXCLUDED, new MutableTreeNode("Excluded", CATEGORY_ORIGIN_EXCLUDED, true));

            addSemanticRootNodes(snapshot, categories, filterKeywordLower);
            addSemanticArchiveNodes(snapshot, jarRows, resolver, categories, filterKeywordLower);
            addSemanticClassNodes(classRows, jarNameById, resolver, categories, filterKeywordLower);
            addSemanticResourceNodes(resourceRows, jarNameById, resolver, categories, filterKeywordLower);

            List<TreeNodeDto> out = new ArrayList<>();
            boolean hasFilter = filterKeywordLower != null && !filterKeywordLower.isBlank();
            for (ProjectOrigin origin : ORIGIN_ORDER) {
                MutableTreeNode category = categories.get(origin);
                if (category == null) {
                    continue;
                }
                TreeNodeDto node = category.freeze();
                if (STATE.mergePackageRoot) {
                    node = mergeSemanticSections(node);
                }
                if (hasFilter) {
                    if (node.children().isEmpty() && !matchesFilter(filterKeywordLower, node.label())) {
                        continue;
                    }
                } else if (node.children().isEmpty()) {
                    continue;
                }
                out.add(node);
            }
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

        private ProjectModelSnapshot loadProjectModelSnapshot() {
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                long buildSeq = loadLatestProjectModelBuildSeq(connection);
                if (buildSeq <= 0) {
                    return ProjectModelSnapshot.empty();
                }
                List<ProjectRootRecord> roots = loadProjectRoots(connection, buildSeq);
                List<ProjectEntryRecord> entries = loadProjectEntries(connection, buildSeq);
                return new ProjectModelSnapshot(buildSeq, roots, entries);
            } catch (Exception ex) {
                logger.debug("load project model snapshot failed: {}", ex.toString());
                return ProjectModelSnapshot.empty();
            }
        }

        private long loadLatestProjectModelBuildSeq(Connection connection) {
            String sql = "SELECT build_seq FROM project_model_meta ORDER BY build_seq DESC LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return -1L;
            } catch (Exception ex) {
                logger.debug("load latest project_model build_seq failed: {}", ex.toString());
                return -1L;
            }
        }

        private List<ProjectRootRecord> loadProjectRoots(Connection connection, long buildSeq) {
            String sql = "SELECT root_id, root_kind, origin_kind, root_path, presentable_name, " +
                    "is_archive, is_test, priority FROM project_model_root WHERE build_seq = ? " +
                    "ORDER BY priority ASC, root_kind ASC, root_path ASC";
            List<ProjectRootRecord> out = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        out.add(new ProjectRootRecord(
                                rs.getInt("root_id"),
                                ProjectRootKind.fromValue(rs.getString("root_kind")),
                                ProjectOrigin.fromValue(rs.getString("origin_kind")),
                                normalizeFsPath(rs.getString("root_path")),
                                safe(rs.getString("presentable_name")),
                                rs.getInt("is_archive") == 1,
                                rs.getInt("is_test") == 1,
                                rs.getInt("priority")
                        ));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load project_model roots failed: {}", ex.toString());
                return List.of();
            }
            return out;
        }

        private List<ProjectEntryRecord> loadProjectEntries(Connection connection, long buildSeq) {
            String sql = "SELECT root_id, entry_kind, origin_kind, entry_path FROM project_model_entry " +
                    "WHERE build_seq = ? ORDER BY entry_id ASC";
            List<ProjectEntryRecord> out = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, buildSeq);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String entryKind = safe(rs.getString("entry_kind")).trim();
                        if (entryKind.isBlank()) {
                            continue;
                        }
                        out.add(new ProjectEntryRecord(
                                rs.getInt("root_id"),
                                entryKind,
                                ProjectOrigin.fromValue(rs.getString("origin_kind")),
                                normalizeFsPath(rs.getString("entry_path"))
                        ));
                    }
                }
            } catch (Exception ex) {
                logger.debug("load project_model entries failed: {}", ex.toString());
                return List.of();
            }
            return out;
        }

        private void addSemanticRootNodes(ProjectModelSnapshot snapshot,
                                          Map<ProjectOrigin, MutableTreeNode> categories,
                                          String filterKeywordLower) {
            for (ProjectRootRecord root : snapshot.roots()) {
                if (root == null) {
                    continue;
                }
                ProjectOrigin origin = root.origin() == null ? ProjectOrigin.APP : root.origin();
                MutableTreeNode category = resolveCategory(categories, origin);
                if (category == null) {
                    continue;
                }
                String rootPath = pathToString(root.path());
                String presentable = safe(root.presentableName()).trim();
                if (presentable.isBlank()) {
                    Path fileName = root.path() == null ? null : root.path().getFileName();
                    presentable = fileName == null ? rootPath : fileName.toString();
                }
                if (presentable.isBlank()) {
                    presentable = root.kind().value();
                }
                String rootKind = rootKindLabel(root.kind());
                String label = presentable + " [" + rootKind + "]";
                if (root.test()) {
                    label = label + " (test)";
                }
                if (root.archive()) {
                    label = label + " (archive)";
                }
                if (!matchesFilter(filterKeywordLower, label, rootPath, rootKind, origin.value())) {
                    continue;
                }
                MutableTreeNode section = ensureOriginSection(category, origin, "roots", "Roots");
                String nodeKey = "origin-root:" + origin.value() + ":" + root.rootId();
                String nodeValue = "origin-root:" + origin.value() + ":" + root.kind().value() + ":" + root.rootId();
                String finalLabel = label;
                MutableTreeNode rootNode = section.children.computeIfAbsent(
                        nodeKey,
                        ignored -> new MutableTreeNode(finalLabel, nodeValue, true)
                );
                if (!rootPath.isBlank()) {
                    rootNode.children.computeIfAbsent(
                            "origin-root-path:" + origin.value() + ":" + root.rootId(),
                            ignored -> new MutableTreeNode(rootPath, "path:" + rootPath, false)
                    );
                }
            }
        }

        private void addSemanticArchiveNodes(ProjectModelSnapshot snapshot,
                                             List<JarEntity> jarRows,
                                             SemanticOriginResolver resolver,
                                             Map<ProjectOrigin, MutableTreeNode> categories,
                                             String filterKeywordLower) {
            Set<String> seenArchivePaths = new HashSet<>();
            for (JarEntity row : jarRows) {
                if (row == null) {
                    continue;
                }
                int jarId = row.getJid();
                String jarName = resolveJarName(jarId, row.getJarName(), null);
                String absPath = safe(row.getJarAbsPath()).trim();
                ProjectOrigin origin = resolver.resolve(jarId, absPath);
                if (!matchesFilter(filterKeywordLower, jarName, absPath, origin.value(), String.valueOf(jarId))) {
                    continue;
                }
                MutableTreeNode category = resolveCategory(categories, origin);
                if (category == null) {
                    continue;
                }
                MutableTreeNode section = ensureOriginSection(category, origin, "archives", "Archives");
                String nodeKey = "origin-archive:" + origin.value() + ":" + jarId;
                String nodeValue = "origin-archive:" + origin.value() + ":" + jarId;
                String finalJarName = jarName;
                MutableTreeNode archiveNode = section.children.computeIfAbsent(
                        nodeKey,
                        ignored -> new MutableTreeNode(finalJarName, nodeValue, true)
                );
                if (!absPath.isBlank()) {
                    String absPathKey = pathKey(normalizeFsPath(absPath));
                    if (!absPathKey.isBlank()) {
                        seenArchivePaths.add(absPathKey);
                    }
                    archiveNode.children.computeIfAbsent(
                            "origin-archive-path:" + origin.value() + ":" + jarId,
                            ignored -> new MutableTreeNode(absPath, "jarpath:" + jarId, false)
                    );
                }
            }
            for (ProjectEntryRecord entry : snapshot.entries()) {
                if (entry == null || !"archive".equals(entry.entryKind()) || entry.path() == null) {
                    continue;
                }
                String entryPath = pathToString(entry.path());
                if (entryPath.isBlank()) {
                    continue;
                }
                String entryPathKey = pathKey(entry.path());
                if (!entryPathKey.isBlank() && seenArchivePaths.contains(entryPathKey)) {
                    continue;
                }
                String name = entry.path().getFileName() == null ? entryPath : entry.path().getFileName().toString();
                if (!matchesFilter(filterKeywordLower, name, entryPath, entry.origin().value())) {
                    continue;
                }
                ProjectOrigin origin = entry.origin();
                MutableTreeNode category = resolveCategory(categories, origin);
                if (category == null) {
                    continue;
                }
                MutableTreeNode section = ensureOriginSection(category, origin, "archives", "Archives");
                String nodeKey = "origin-archive-entry:" + origin.value() + ":" + entryPath;
                String nodeValue = "origin-archive-entry:" + origin.value() + ":" + entryPath;
                String finalName = name;
                MutableTreeNode archiveNode = section.children.computeIfAbsent(
                        nodeKey,
                        ignored -> new MutableTreeNode(finalName, nodeValue, true)
                );
                archiveNode.children.computeIfAbsent(
                        "origin-archive-entry-path:" + origin.value() + ":" + entryPath,
                        ignored -> new MutableTreeNode(entryPath, "path:" + entryPath, false)
                );
            }
        }

        private void addSemanticClassNodes(List<ClassFileEntity> rows,
                                           Map<Integer, String> jarNameById,
                                           SemanticOriginResolver resolver,
                                           Map<ProjectOrigin, MutableTreeNode> categories,
                                           String filterKeywordLower) {
            boolean groupByJar = STATE.groupTreeByJar;
            for (ClassFileEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String normalized = normalizeClassName(row.getClassName());
                if (normalized == null) {
                    continue;
                }
                int jarId = row.getJarId() == null ? 0 : row.getJarId();
                String jarName = resolveJarName(jarId, row.getJarName(), jarNameById);
                String classPath = safe(row.getPathStr()).trim();
                ProjectOrigin origin = resolver.resolve(jarId, classPath);
                if (!matchesFilter(filterKeywordLower, normalized, jarName, classPath, origin.value())) {
                    continue;
                }
                MutableTreeNode category = resolveCategory(categories, origin);
                if (category == null) {
                    continue;
                }
                MutableTreeNode section = ensureOriginSection(category, origin, "classes", "Classes");
                MutableTreeNode cursor = section;
                if (groupByJar) {
                    String jarKey = "origin-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                    String jarValue = "origin-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                    String finalJarName = jarName;
                    cursor = section.children.computeIfAbsent(
                            jarKey,
                            ignored -> new MutableTreeNode(finalJarName, jarValue, true)
                    );
                }
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
                        String leafKey = "origin-class-leaf:" + origin.value() + ":" + normalized + "|" + jarId;
                        String finalLabel = label;
                        String finalValue = value;
                        cursor.children.computeIfAbsent(
                                leafKey,
                                ignored -> new MutableTreeNode(finalLabel, finalValue, false)
                        );
                    } else {
                        if (packagePath.length() > 0) {
                            packagePath.append('/');
                        }
                        packagePath.append(part);
                        String pkg = packagePath.toString().replace('/', '.');
                        String dirKey = "origin-pkg:" + origin.value() + ":" + (groupByJar ? jarId + ":" : "") + pkg;
                        String dirValue = "origin-pkg:" + origin.value() + ":" + pkg;
                        cursor = cursor.children.computeIfAbsent(
                                dirKey,
                                ignored -> new MutableTreeNode(part, dirValue, true)
                        );
                    }
                }
            }
        }

        private void addSemanticResourceNodes(List<ResourceEntity> rows,
                                              Map<Integer, String> jarNameById,
                                              SemanticOriginResolver resolver,
                                              Map<ProjectOrigin, MutableTreeNode> categories,
                                              String filterKeywordLower) {
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
                String resourcePath = safe(row.getPathStr()).trim();
                ProjectOrigin origin = resolver.resolve(jarId, resourcePath);
                if (!matchesFilter(filterKeywordLower, normalized, jarName, resourcePath, origin.value())) {
                    continue;
                }
                MutableTreeNode category = resolveCategory(categories, origin);
                if (category == null) {
                    continue;
                }
                MutableTreeNode section = ensureOriginSection(category, origin, "resources", "Resources");
                MutableTreeNode cursor = section;
                if (groupByJar) {
                    String jarKey = "origin-res-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                    String jarValue = "origin-res-jar:" + origin.value() + ":" + jarName + "|" + jarId;
                    String finalJarName = jarName;
                    cursor = section.children.computeIfAbsent(
                            jarKey,
                            ignored -> new MutableTreeNode(finalJarName, jarValue, true)
                    );
                }
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
                        String leafKey = "origin-res-leaf:" + origin.value() + ":" + row.getRid();
                        String leafValue = "res:" + row.getRid();
                        String finalLabel = label;
                        cursor.children.computeIfAbsent(
                                leafKey,
                                ignored -> new MutableTreeNode(finalLabel, leafValue, false)
                        );
                    } else {
                        if (path.length() > 0) {
                            path.append('/');
                        }
                        path.append(part);
                        String p = path.toString().replace('/', '.');
                        String dirKey = "origin-resdir:" + origin.value() + ":" + (groupByJar ? jarId + ":" : "") + p;
                        String dirValue = "origin-resdir:" + origin.value() + ":" + p;
                        cursor = cursor.children.computeIfAbsent(
                                dirKey,
                                ignored -> new MutableTreeNode(part, dirValue, true)
                        );
                    }
                }
            }
        }

        private MutableTreeNode ensureOriginSection(MutableTreeNode category,
                                                    ProjectOrigin origin,
                                                    String suffix,
                                                    String label) {
            String value = "origin-sec:" + origin.value() + ":" + suffix;
            return category.children.computeIfAbsent(value, ignored -> new MutableTreeNode(label, value, true));
        }

        private MutableTreeNode resolveCategory(Map<ProjectOrigin, MutableTreeNode> categories,
                                                ProjectOrigin origin) {
            ProjectOrigin safeOrigin = origin == null ? ProjectOrigin.APP : origin;
            MutableTreeNode result = categories.get(safeOrigin);
            if (result != null) {
                return result;
            }
            return categories.get(ProjectOrigin.APP);
        }

        private TreeNodeDto buildInputCategory(BuildSettingsDto settings, String filterKeywordLower) {
            List<TreeNodeDto> children = new ArrayList<>();
            String inputPath = settings == null ? "" : safe(settings.activeInputPath()).trim();
            String projectPath = settings == null ? "" : safe(settings.projectPath()).trim();
            String sdkPath = settings == null ? "" : safe(settings.sdkPath()).trim();
            if (!inputPath.isBlank() && matchesFilter(filterKeywordLower, inputPath)) {
                children.add(new TreeNodeDto("输入: " + inputPath, "input:path", false, List.of()));
            }
            if (!projectPath.isBlank()
                    && !projectPath.equals(inputPath)
                    && matchesFilter(filterKeywordLower, projectPath)) {
                children.add(new TreeNodeDto("项目根: " + projectPath, "input:project", false, List.of()));
            }
            if (!sdkPath.isBlank() && matchesFilter(filterKeywordLower, sdkPath)) {
                children.add(new TreeNodeDto("SDK: " + sdkPath, "input:sdk", false, List.of()));
            }
            sortNodes(children);
            return new TreeNodeDto("输入", CATEGORY_INPUT, true, children);
        }

        private TreeNodeDto buildSourceCategory(List<ClassFileEntity> rows,
                                                Map<Integer, String> jarNameById,
                                                String filterKeywordLower) {
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

        private TreeNodeDto buildResourceCategory(List<ResourceEntity> rows,
                                                  Map<Integer, String> jarNameById,
                                                  String filterKeywordLower) {
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

        private TreeNodeDto mergeSemanticSections(TreeNodeDto category) {
            if (category == null || !category.directory()) {
                return category;
            }
            List<TreeNodeDto> children = category.children();
            if (children == null || children.isEmpty()) {
                return category;
            }
            List<TreeNodeDto> out = new ArrayList<>();
            for (TreeNodeDto child : children) {
                if (child == null) {
                    continue;
                }
                if (child.directory() && shouldMergeSection(child.value())) {
                    out.add(new TreeNodeDto(
                            child.label(),
                            child.value(),
                            true,
                            mergeNodeList(child.children())
                    ));
                } else {
                    out.add(child);
                }
            }
            sortNodes(out);
            return new TreeNodeDto(category.label(), category.value(), true, out);
        }

        private boolean shouldMergeSection(String value) {
            String normalized = safe(value);
            if (!normalized.startsWith("origin-sec:")) {
                return false;
            }
            return normalized.endsWith(":classes") || normalized.endsWith(":resources");
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

        private String rootKindLabel(ProjectRootKind kind) {
            if (kind == null) {
                return "content-root";
            }
            return switch (kind) {
                case CONTENT_ROOT -> "content-root";
                case SOURCE_ROOT -> "source-root";
                case RESOURCE_ROOT -> "resource-root";
                case LIBRARY -> "library";
                case SDK -> "sdk";
                case GENERATED -> "generated";
                case EXCLUDED -> "excluded";
            };
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

        private static Path normalizeFsPath(String raw) {
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Paths.get(value).toAbsolutePath().normalize();
            } catch (Exception ignored) {
                try {
                    return Paths.get(value).normalize();
                } catch (Exception ignoredAgain) {
                    return null;
                }
            }
        }

        private static String pathToString(Path path) {
            return path == null ? "" : path.toString();
        }

        private static String pathKey(Path path) {
            if (path == null) {
                return "";
            }
            try {
                return path.toAbsolutePath().normalize().toString();
            } catch (Exception ex) {
                return path.normalize().toString();
            }
        }

        private record ProjectModelSnapshot(long buildSeq,
                                            List<ProjectRootRecord> roots,
                                            List<ProjectEntryRecord> entries) {
            private static ProjectModelSnapshot empty() {
                return new ProjectModelSnapshot(-1L, List.of(), List.of());
            }

            private static ProjectModelSnapshot of(long buildSeq,
                                                   List<ProjectRootRecord> roots,
                                                   List<ProjectEntryRecord> entries) {
                return new ProjectModelSnapshot(
                        buildSeq <= 0 ? System.currentTimeMillis() : buildSeq,
                        roots == null ? List.of() : List.copyOf(roots),
                        entries == null ? List.of() : List.copyOf(entries)
                );
            }

            private boolean available() {
                return buildSeq > 0
                        && ((roots != null && !roots.isEmpty()) || (entries != null && !entries.isEmpty()));
            }
        }

        private record ProjectRootRecord(int rootId,
                                         ProjectRootKind kind,
                                         ProjectOrigin origin,
                                         Path path,
                                         String presentableName,
                                         boolean archive,
                                         boolean test,
                                         int priority) {
        }

        private record ProjectEntryRecord(int rootId,
                                          String entryKind,
                                          ProjectOrigin origin,
                                          Path path) {
        }

        private record OriginPathRule(Path path, ProjectOrigin origin, int depth) {
        }

        private static final class SemanticOriginResolver {
            private final Map<Integer, Path> jarPathById;
            private final Map<String, ProjectOrigin> exactPathOrigins = new HashMap<>();
            private final List<OriginPathRule> pathRules = new ArrayList<>();

            private SemanticOriginResolver(ProjectModelSnapshot snapshot, Map<Integer, Path> jarPathById) {
                this.jarPathById = jarPathById == null ? Map.of() : jarPathById;
                if (snapshot == null) {
                    return;
                }
                if (snapshot.roots() != null) {
                    for (ProjectRootRecord root : snapshot.roots()) {
                        if (root == null || root.path() == null) {
                            continue;
                        }
                        registerPath(root.path(), root.origin());
                    }
                }
                if (snapshot.entries() != null) {
                    for (ProjectEntryRecord entry : snapshot.entries()) {
                        if (entry == null || entry.path() == null) {
                            continue;
                        }
                        registerPath(entry.path(), entry.origin());
                    }
                }
                pathRules.sort(Comparator.comparingInt(OriginPathRule::depth).reversed());
            }

            private void registerPath(Path path, ProjectOrigin origin) {
                if (path == null) {
                    return;
                }
                ProjectOrigin safeOrigin = origin == null ? ProjectOrigin.UNKNOWN : origin;
                String key = pathKey(path);
                if (!key.isBlank()) {
                    exactPathOrigins.putIfAbsent(key, safeOrigin);
                }
                int depth = Math.max(0, path.getNameCount());
                pathRules.add(new OriginPathRule(path, safeOrigin, depth));
            }

            private ProjectOrigin resolve(Integer jarId, String pathText) {
                if (jarId != null && jarId > 0) {
                    ProjectOrigin byJarPath = resolveByPath(jarPathById.get(jarId));
                    if (byJarPath != ProjectOrigin.UNKNOWN) {
                        return byJarPath;
                    }
                }
                ProjectOrigin byPath = resolveByPath(normalizeFsPath(pathText));
                if (byPath != ProjectOrigin.UNKNOWN) {
                    return byPath;
                }
                return ProjectOrigin.APP;
            }

            private ProjectOrigin resolveByPath(Path candidate) {
                if (candidate == null) {
                    return ProjectOrigin.UNKNOWN;
                }
                String key = pathKey(candidate);
                if (!key.isBlank()) {
                    ProjectOrigin exact = exactPathOrigins.get(key);
                    if (exact != null && exact != ProjectOrigin.UNKNOWN) {
                        return exact;
                    }
                }
                for (OriginPathRule rule : pathRules) {
                    if (rule == null || rule.path() == null) {
                        continue;
                    }
                    try {
                        if (candidate.startsWith(rule.path())) {
                            return rule.origin();
                        }
                    } catch (Exception ignored) {
                    }
                }
                return ProjectOrigin.UNKNOWN;
            }
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
        public void openSqlAuditTool() {
            emitToolingWindow(ToolingWindowAction.SQL_AUDIT);
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
                    s.buildMode(),
                    s.artifactPath(),
                    s.projectPath(),
                    s.sdkPath(),
                    s.resolveNestedJars(),
                    s.includeSdk(),
                    s.autoDetectSdk(),
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
                    s.buildMode(),
                    s.artifactPath(),
                    s.projectPath(),
                    s.sdkPath(),
                    s.resolveNestedJars(),
                    s.includeSdk(),
                    s.autoDetectSdk(),
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
                    s.buildMode(),
                    s.artifactPath(),
                    s.projectPath(),
                    s.sdkPath(),
                    s.resolveNestedJars(),
                    s.includeSdk(),
                    s.autoDetectSdk(),
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
            sb.append("<p class=\"muted\">scope: <code>")
                    .append(escapeHtml(snapshot.scope()))
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

    private static String initialTr(String zh, String en) {
        try {
            ConfigFile cfg = ConfigEngine.parseConfig();
            if (cfg != null && "en".equalsIgnoreCase(safe(cfg.getLang()))) {
                return safe(en);
            }
        } catch (Throwable ignored) {
        }
        return safe(zh);
    }

    private static String tr(String zh, String en) {
        int lang = GlobalOptions.getLang();
        if (lang == GlobalOptions.ENGLISH) {
            return safe(en);
        }
        return safe(zh);
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

    private static SearchResultDto toSearchResult(MethodResult m, String contributor, String origin) {
        String preview = safe(m.getClassName()) + "#" + safe(m.getMethodName()) + safe(m.getMethodDesc());
        return new SearchResultDto(
                safe(m.getClassName()),
                safe(m.getMethodName()),
                safe(m.getMethodDesc()),
                safe(m.getJarName()),
                m.getJarId(),
                preview,
                safe(contributor),
                safe(origin),
                "cls:" + normalizeClass(m.getClassName()) + "|" + m.getJarId()
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
