package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.analyze.asm.ASMPrint;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCallEngine;
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.graph.flow.FlowTruncation;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.exporter.LeakCsvExporter;
import me.n1ar4.jar.analyzer.gadget.GadgetAnalyzer;
import me.n1ar4.jar.analyzer.gadget.GadgetInfo;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.util.DecompiledMethodLocator;
import me.n1ar4.jar.analyzer.gui.util.EditorDeclarationResolver;
import me.n1ar4.jar.analyzer.gui.util.EditorSymbolNavigationResolver;
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiStartupConfigDto;
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
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;
import me.n1ar4.jar.analyzer.leak.LeakScanService;
import me.n1ar4.jar.analyzer.mcp.McpLine;
import me.n1ar4.jar.analyzer.mcp.McpManager;
import me.n1ar4.jar.analyzer.mcp.McpReportWebConfig;
import me.n1ar4.jar.analyzer.mcp.McpServiceConfig;
import me.n1ar4.jar.analyzer.sca.ScaScanService;
import me.n1ar4.jar.analyzer.sca.ScaReportFormatter;
import me.n1ar4.jar.analyzer.sca.utils.ReportUtil;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintCache;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    private static final GraphFlowService GRAPH_FLOW_SERVICE = new GraphFlowService();
    private static final LeakScanService LEAK_SCAN_SERVICE = new LeakScanService();
    private static final ScaScanService SCA_SCAN_SERVICE = new ScaScanService();

    private static final BuildFacade BUILD = new BuildRuntimeFacade(
            buildState(),
            new BuildWorkflowSupport(RuntimeFacades::tr),
            EngineContext::getEngine,
            RuntimeFacades::tr,
            RuntimeFacades::engineStatus
    );
    private static final SearchFacade SEARCH = new SearchRuntimeFacade(
            searchState(),
            new SearchWorkflowSupport(RuntimeFacades::tr),
            EngineContext::getEngine,
            RuntimeFacades::tr
    );
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
    private static final ProjectTreeFacade PROJECT_TREE = new ProjectTreeRuntimeFacade(
            treeState(),
            new ProjectTreeSupport(projectTreeUiActions()),
            EngineContext::getEngine,
            ClassIndex::refresh
    );
    private static final ToolingFacade TOOLING = new DefaultToolingFacade();

    private RuntimeFacades() {
    }

    private static BuildRuntimeFacade.BuildState buildState() {
        return new BuildRuntimeFacade.BuildState() {
            @Override
            public BuildSettingsDto buildSettings() {
                return STATE.buildSettings;
            }

            @Override
            public void setBuildSettings(BuildSettingsDto settings) {
                STATE.buildSettings = settings;
            }

            @Override
            public int buildProgress() {
                return STATE.buildProgress;
            }

            @Override
            public void setBuildProgress(int progress) {
                STATE.buildProgress = progress;
            }

            @Override
            public String buildStatusText() {
                return STATE.buildStatusText;
            }

            @Override
            public void setBuildStatusText(String statusText) {
                STATE.buildStatusText = statusText;
            }

            @Override
            public String totalJar() {
                return STATE.totalJar;
            }

            @Override
            public void setTotalJar(String value) {
                STATE.totalJar = value;
            }

            @Override
            public String totalClass() {
                return STATE.totalClass;
            }

            @Override
            public void setTotalClass(String value) {
                STATE.totalClass = value;
            }

            @Override
            public String totalMethod() {
                return STATE.totalMethod;
            }

            @Override
            public void setTotalMethod(String value) {
                STATE.totalMethod = value;
            }

            @Override
            public String totalEdge() {
                return STATE.totalEdge;
            }

            @Override
            public void setTotalEdge(String value) {
                STATE.totalEdge = value;
            }

            @Override
            public String databaseSize() {
                return STATE.databaseSize;
            }

            @Override
            public void setDatabaseSize(String value) {
                STATE.databaseSize = value;
            }

            @Override
            public int language() {
                return STATE.language;
            }

            @Override
            public boolean tryStartBuild() {
                return STATE.buildRunning.compareAndSet(false, true);
            }

            @Override
            public void finishBuild() {
                STATE.buildRunning.set(false);
            }
        };
    }

    private static SearchRuntimeFacade.SearchState searchState() {
        return new SearchRuntimeFacade.SearchState() {
            @Override
            public SearchQueryDto searchQuery() {
                return STATE.searchQuery;
            }

            @Override
            public void setSearchQuery(SearchQueryDto query) {
                STATE.searchQuery = query;
            }

            @Override
            public List<SearchResultDto> searchResults() {
                return STATE.searchResults;
            }

            @Override
            public void setSearchResults(List<SearchResultDto> results) {
                STATE.searchResults = results;
            }

            @Override
            public String searchStatusText() {
                return STATE.searchStatusText;
            }

            @Override
            public void setSearchStatusText(String statusText) {
                STATE.searchStatusText = statusText;
            }

            @Override
            public boolean sortByMethod() {
                return STATE.sortByMethod;
            }

            @Override
            public boolean tryStartSearch() {
                return STATE.searchRunning.compareAndSet(false, true);
            }

            @Override
            public void finishSearch() {
                STATE.searchRunning.set(false);
            }
        };
    }

    private static ProjectTreeRuntimeFacade.TreeState treeState() {
        return new ProjectTreeRuntimeFacade.TreeState() {
            @Override
            public boolean showInnerClass() {
                return STATE.showInnerClass;
            }

            @Override
            public boolean groupTreeByJar() {
                return STATE.groupTreeByJar;
            }

            @Override
            public boolean mergePackageRoot() {
                return STATE.mergePackageRoot;
            }
        };
    }

    private static ProjectTreeSupport.UiActions projectTreeUiActions() {
        return new ProjectTreeSupport.UiActions() {
            @Override
            public void openClass(String className, Integer jarId) {
                RuntimeFacades.editor().openClass(className, jarId);
            }

            @Override
            public void showText(String title, String text) {
                emitTextWindow(title, text);
            }

            @Override
            public void showTooling(ToolingWindowRequest request) {
                emitToolingWindow(request);
            }
        };
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
            case "jd-gui" -> launchJdGuiTool();
            default -> "unknown tool: " + key;
        };
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
        // best-effort UI fallback.
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
                "",
                "",
                false,
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
                "", "low", true, false, 30
        );
        private volatile String chainsStatusText = initialTr("就绪", "ready");

        private volatile EditorDocumentDto editorDocument = new EditorDocumentDto(
                "", "", null, "", "", "", 0, initialTr("就绪", "ready"));
        private final AtomicLong editorOpenTicket = new AtomicLong(0L);
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
                        positiveJarId(jarId),
                        tr("引擎尚未就绪", "engine is not ready")
                );
            }
            String ownerClass = normalizeClass(className);
            if (ownerClass.isBlank()) {
                return StructureSnapshotDto.empty(
                        "",
                        positiveJarId(jarId),
                        tr("类名为空", "class is empty")
                );
            }
            Integer ownerJarId = positiveJarId(jarId);
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
            items.add(new StructureItemDto("section", 0, "interfaces (" + deduped.size() + ")", "", "", "", null, "", false));
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
                // best-effort UI fallback.
                }
                items.add(new StructureItemDto(
                        "interface",
                        1,
                        "I " + iface + formatJarSuffix(jarName, jarId > 0 ? jarId : null),
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
                Integer memberJarId = positiveJarId(member.getJarId());
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
            items.add(new StructureItemDto("section", 0, "fields (" + filtered.size() + (truncated ? "+" : "") + ")", "", "", "", null, "", false));
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

            items.add(new StructureItemDto("section", 0, "methods (" + filtered.size() + (truncated ? "+" : "") + ")", "", "", "", null, "", false));
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
            items.add(new StructureItemDto("section", 0, "inner classes (" + inners.size() + ")", "", "", "", null, "", false));
            for (InnerClassRow inner : inners) {
                items.add(new StructureItemDto(
                        "inner-class",
                        1,
                        "C " + inner.className() + formatJarSuffix(inner.jarName(), inner.jarId() > 0 ? inner.jarId() : null),
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
            String owner = normalizeClass(ownerClass);
            if (owner.isBlank()) {
                return out;
            }
            String prefix = owner + "$";
            Set<String> seen = new LinkedHashSet<>();
            for (ClassReference row : DatabaseManager.getClassReferences()) {
                if (row == null) {
                    continue;
                }
                String className = normalizeClass(row.getName());
                if (className.isBlank() || !className.startsWith(prefix) || className.equals(owner)) {
                    continue;
                }
                String remain = className.substring(prefix.length());
                if (remain.contains("$")) {
                    continue;
                }
                int jarId = row.getJarId() == null ? -1 : row.getJarId();
                if (ownerJarId != null && ownerJarId >= 0 && jarId != ownerJarId) {
                    continue;
                }
                String key = className + "|" + jarId;
                if (!seen.add(key)) {
                    continue;
                }
                out.add(new InnerClassRow(className, jarId, safe(row.getJarName())));
            }
            if (out.isEmpty()) {
                for (ClassFileEntity row : DatabaseManager.getClassFiles()) {
                    if (row == null) {
                        continue;
                    }
                    String className = normalizeClass(row.getClassName());
                    if (className.isBlank() || !className.startsWith(prefix) || className.equals(owner)) {
                        continue;
                    }
                    String remain = className.substring(prefix.length());
                    if (remain.contains("$")) {
                        continue;
                    }
                    int jarId = row.getJarId() == null ? -1 : row.getJarId();
                    if (ownerJarId != null && ownerJarId >= 0 && jarId != ownerJarId) {
                        continue;
                    }
                    String key = className + "|" + jarId;
                    if (!seen.add(key)) {
                        continue;
                    }
                    out.add(new InnerClassRow(className, jarId, safe(row.getJarName())));
                }
            }
            out.sort(Comparator.comparing(InnerClassRow::className).thenComparingInt(InnerClassRow::jarId));
            if (out.size() > MAX_INNER_CLASS_COUNT) {
                return new ArrayList<>(out.subList(0, MAX_INNER_CLASS_COUNT));
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
            // best-effort UI fallback.
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

        private record InnerClassRow(String className, int jarId, String jarName) {
        }
    }

    private static final class DefaultCallGraphFacade implements CallGraphFacade {
        private volatile long resolverVersion = -1L;
        private volatile ProjectJarOriginResolver resolver = ProjectJarOriginResolver.empty();

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
            ProjectJarOriginResolver localResolver = scope == CallGraphScope.ALL
                    ? ProjectJarOriginResolver.empty()
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

        private boolean isAccepted(MethodResult method, CallGraphScope scope, ProjectJarOriginResolver resolver) {
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

        private ProjectJarOriginResolver loadResolver() {
            long latestRuntimeVersion = ProjectStateUtil.runtimeSnapshot();
            if (latestRuntimeVersion <= 0) {
                return ProjectJarOriginResolver.empty();
            }
            ProjectJarOriginResolver cached = resolver;
            if (latestRuntimeVersion == resolverVersion && cached != null) {
                return cached;
            }
            synchronized (this) {
                if (latestRuntimeVersion == resolverVersion && resolver != null) {
                    return resolver;
                }
                ProjectJarOriginResolver reloaded = loadResolverFromModel();
                resolverVersion = latestRuntimeVersion;
                resolver = reloaded;
                return reloaded;
            }
        }

        private ProjectJarOriginResolver loadResolverFromModel() {
            try {
                return ProjectJarOriginResolver.fromProjectModel(
                        ProjectStateUtil.runtimeProjectModel(),
                        DatabaseManager.getJarsMeta());
            } catch (Exception ex) {
                logger.debug("load call scope resolver fail: {}", ex.toString());
                return ProjectJarOriginResolver.empty();
            }
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
            // best-effort UI fallback.
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
            if (Files.isDirectory(path)) {
                appendSca("input is a dir");
            } else {
                appendSca("input is a file");
            }
            List<String> jarList = ScaScanService.resolveJarList(input);
            if (jarList.isEmpty()) {
                appendSca("no supported archive found");
                return;
            }
            appendSca("start scan and wait...");

            List<me.n1ar4.jar.analyzer.sca.dto.SCAApiResult> all = SCA_SCAN_SERVICE.scan(
                    new ScaScanService.Request(
                            jarList,
                            enabledScaRules(settings)
                    )
            );
            if (all.isEmpty()) {
                appendSca("no vulnerability found");
                return;
            }

            String reportText = ScaReportFormatter.buildConsoleReport(all);

            if (settings.outputMode() == ScaOutputMode.HTML) {
                try {
                    String output = safe(settings.outputFile());
                    if (output.isEmpty()) {
                        output = String.format("jar-analyzer-sca-%d.html", System.currentTimeMillis());
                    }
                    ReportUtil.generateHtmlReport(all, output);
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
            appendSca(reportText);
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

        private void doScan() {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                appendLeak("engine not ready");
                return;
            }
            LeakRulesDto rules = STATE.leakRules;
            List<LeakResult> resultSet = LEAK_SCAN_SERVICE.scan(
                    engine,
                    new LeakScanService.Request(
                            selectedLeakTypes(rules),
                            rules.detectBase64(),
                            null,
                            List.of(),
                            List.of(),
                            Set.of(),
                            Set.of()
                    ),
                    new LeakScanService.Listener() {
                        @Override
                        public void onRuleStart(String typeName) {
                            appendLeak(typeName + " leak start");
                        }

                        @Override
                        public void onRuleFinish(String typeName, int addedCount, int totalCount) {
                            appendLeak(typeName + " leak finish");
                        }
                    }
            );

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
                        leak.getJarId()
                ));
            }
            items.sort(Comparator.comparing(LeakItemDto::typeName).thenComparing(LeakItemDto::className));
            STATE.leakResults = immutableList(items);
            appendLeak("total leak results: " + items.size());
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
            GraphFlowService.DfsOutcome outcome = GRAPH_FLOW_SERVICE.runDfs(options, null);
            List<DFSResult> results = outcome == null ? List.of() : outcome.results();
            FlowStats stats = outcome == null ? FlowStats.empty() : outcome.stats();
            logger.info("runtime dfs backend=graph paths={}", results.size());
            return new DfsRunOutcome(
                    results,
                    formatGraphStatus(tr("DFS 后端: 图引擎", "DFS backend: graph"), stats)
            );
        }

        private static TaintRunOutcome runTaintGraphOnly(ChainsSettingsDto cfg, List<DFSResult> dfsSnapshot) {
            List<DFSResult> safeSnapshot = dfsSnapshot == null ? List.of() : dfsSnapshot;
            GraphFlowService.TaintOutcome outcome = GRAPH_FLOW_SERVICE.analyzeDfsResults(
                    safeSnapshot,
                    resolveFlowTimeoutMs(cfg),
                    resolveFlowMaxPaths(cfg),
                    null,
                    null
            );
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
                    cfg.showEdgeMeta(),
                    cfg.summaryEnabled(),
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
        public ApiStartupConfigDto startupApiConfig() {
            ConfigFile cfg = ensureConfig();
            return new ApiStartupConfigDto(
                    safe(cfg.getApiBind()).isEmpty() ? "0.0.0.0" : cfg.getApiBind(),
                    cfg.isApiAuth(),
                    normalizePort(cfg.getApiPort(), 10032),
                    safe(cfg.getApiToken()).isEmpty() ? "JAR-ANALYZER-API-TOKEN" : cfg.getApiToken()
            );
        }

        @Override
        public List<String> saveStartupApiConfig(ApiStartupConfigDto config) {
            if (config == null) {
                return List.of("api config is null");
            }
            ConfigFile cfg = ensureConfig();
            int fallbackPort = normalizePort(cfg.getApiPort(), 10032);
            cfg.setApiBind(config.bind());
            cfg.setApiAuth(config.authEnabled());
            cfg.setApiPort(normalizePort(config.port(), fallbackPort));
            cfg.setApiToken(config.token());
            ConfigEngine.saveConfig(cfg);
            return List.of(
                    "api startup config saved",
                    "restart application to apply api bind/port/auth/token"
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
                            new McpLineConfigDto(McpLineKey.AUDIT_FAST, cfg.isMcpAuditFastEnabled(), cfg.getMcpAuditFastPort(), manager.isRunning(McpLine.AUDIT_FAST)),
                            new McpLineConfigDto(McpLineKey.GRAPH_LITE, cfg.isMcpGraphLiteEnabled(), cfg.getMcpGraphLitePort(), manager.isRunning(McpLine.GRAPH_LITE)),
                            new McpLineConfigDto(McpLineKey.DFS_TAINT, cfg.isMcpDfsTaintEnabled(), cfg.getMcpDfsTaintPort(), manager.isRunning(McpLine.DFS_TAINT)),
                            new McpLineConfigDto(McpLineKey.SCA_LEAK, cfg.isMcpScaLeakEnabled(), cfg.getMcpScaLeakPort(), manager.isRunning(McpLine.SCA_LEAK)),
                            new McpLineConfigDto(McpLineKey.SINK_RULES, cfg.isMcpSinkRulesEnabled(), cfg.getMcpSinkRulesPort(), manager.isRunning(McpLine.SINK_RULES)),
                            new McpLineConfigDto(McpLineKey.REPORT, cfg.isMcpReportEnabled(), cfg.getMcpReportPort(), manager.isRunning(McpLine.REPORT))
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
            cfg.setMcpSinkRulesEnabled(getEnabled(lines, McpLineKey.SINK_RULES, cfg.isMcpSinkRulesEnabled()));
            cfg.setMcpSinkRulesPort(normalizePort(getPort(lines, McpLineKey.SINK_RULES, cfg.getMcpSinkRulesPort()), cfg.getMcpSinkRulesPort()));
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
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/jar-analyzer/jar-analyzer/blob/master/doc/README-api.md"));
            } catch (Throwable ignored) {
            // best-effort UI fallback.
            }
        }

        @Override
        public void openMcpDoc() {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/jar-analyzer/jar-analyzer/blob/master/doc/mcp/README.md"));
            } catch (Throwable ignored) {
            // best-effort UI fallback.
            }
        }

        @Override
        public void openN8nDoc() {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/jar-analyzer/jar-analyzer/blob/master/doc/n8n/README.md"));
            } catch (Throwable ignored) {
            // best-effort UI fallback.
            }
        }

        @Override
        public void openReportWeb(String host, int port) {
            String safeHost = safe(host).isEmpty() ? "127.0.0.1" : host.trim();
            int safePort = normalizePort(port, 20080);
            try {
                Desktop.getDesktop().browse(URI.create("http://" + safeHost + ":" + safePort + "/"));
            } catch (Throwable ignored) {
            // best-effort UI fallback.
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
            long ticket = nextEditorOpenTicket();
            openClassInternal(className, jarId, true, ticket);
        }

        @Override
        public void openMethod(String className, String methodName, String methodDesc, Integer jarId) {
            long ticket = nextEditorOpenTicket();
            openMethodInternal(className, methodName, methodDesc, jarId, true, null, ticket);
        }

        @Override
        public void openMethod(String className,
                               String methodName,
                               String methodDesc,
                               Integer jarId,
                               int lineNumber) {
            long ticket = nextEditorOpenTicket();
            openMethodInternal(
                    className,
                    methodName,
                    methodDesc,
                    jarId,
                    true,
                    lineNumber > 0 ? lineNumber : null,
                    ticket
            );
        }

        @Override
        public void activateDocument(EditorDocumentDto doc) {
            if (doc == null) {
                return;
            }
            String className = normalizeClass(doc.className());
            if (className.isBlank()) {
                return;
            }
            long ticket = nextEditorOpenTicket();
            Integer jarId = positiveJarId(doc.jarId());
            String jarName = safe(doc.jarName());
            if (jarName.isBlank()) {
                try {
                    CoreEngine engine = EngineContext.getEngine();
                    if (engine != null && engine.isEnabled()) {
                        if (jarId != null) {
                            jarName = safe(engine.getJarNameById(jarId));
                        } else {
                            jarName = safe(engine.getJarByClass(className));
                        }
                    }
                } catch (Throwable ignored) {
                // best-effort UI fallback.
                }
            }
            if (!isEditorOpenTicketActive(ticket)) {
                return;
            }
            String methodName = safe(doc.methodName());
            String methodDesc = safe(doc.methodDesc());
            int caretOffset = Math.max(0, doc.caretOffset());
            STATE.currentClass = className;
            STATE.currentJar = jarName;
            if (methodName.isBlank()) {
                STATE.currentMethod = null;
            } else {
                STATE.currentMethod = new MethodNavDto(
                        className,
                        methodName,
                        methodDesc,
                        jarName,
                        jarId == null ? 0 : jarId
                );
            }
            STATE.editorDocument = new EditorDocumentDto(
                    className,
                    jarName,
                    jarId,
                    methodName,
                    methodDesc,
                    safe(doc.content()),
                    caretOffset,
                    safe(doc.statusText()).isBlank() ? "tab activated" : safe(doc.statusText())
            );
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
            long ticket = nextEditorOpenTicket();
            int caretOffset = target.caretOffset();
            if (target.localTarget()) {
                return applyCaretOnly(caretOffset, "declaration opened", ticket);
            }
            String className = normalizeClass(target.className());
            if (className.isBlank()) {
                className = normalizeClass(STATE.editorDocument.className());
            }
            Integer jarId = positiveJarId(target.jarId() <= 0 ? null : target.jarId());
            if (target.methodTarget()) {
                if (className.isBlank()) {
                    return false;
                }
                openMethodInternal(
                        className,
                        target.methodName(),
                        target.methodDesc(),
                        jarId,
                        true,
                        null,
                        ticket
                );
                return isEditorOpenTicketActive(ticket);
            }
            if (className.isBlank()) {
                return false;
            }
            if (!openClassInternal(className, jarId, true, ticket)) {
                return false;
            }
            if (caretOffset >= 0) {
                return applyCaretOnly(caretOffset, "declaration opened", ticket);
            }
            return isEditorOpenTicketActive(ticket);
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
            long ticket = nextEditorOpenTicket();
            try {
                return replayNavigation(target, ticket);
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
            long ticket = nextEditorOpenTicket();
            try {
                return replayNavigation(target, ticket);
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
                // best-effort UI fallback.
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

        private boolean replayNavigation(NavState target, long ticket) {
            if (target == null) {
                return false;
            }
            if (safe(target.methodName()).isBlank()) {
                return openClassInternal(target.className(), target.jarId(), false, ticket);
            } else {
                openMethodInternal(
                        target.className(),
                        target.methodName(),
                        target.methodDesc(),
                        target.jarId(),
                        false,
                        null,
                        ticket
                );
                return isEditorOpenTicketActive(ticket);
            }
        }

        private boolean openClassInternal(String className,
                                          Integer jarId,
                                          boolean recordNav,
                                          long ticket) {
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null || !engine.isEnabled()) {
                if (!isEditorOpenTicketActive(ticket)) {
                    return false;
                }
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
                return true;
            }
            String normalizedClass = normalizeClass(className);
            Integer normalizedJarId = positiveJarId(jarId);
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
                        content = DecompileDispatcher.decompile(classPath);
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
            // best-effort UI fallback.
            }
            if (!isEditorOpenTicketActive(ticket)) {
                return false;
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
            return true;
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
                boolean recordNav,
                Integer lineNumberHint,
                long ticket
        ) {
            Integer normalizedJarId = positiveJarId(jarId);
            if (!openClassInternal(className, normalizedJarId, false, ticket)) {
                return;
            }
            if (!isEditorOpenTicketActive(ticket)) {
                return;
            }
            String normalizedClass = normalizeClass(className);
            String jarName = STATE.editorDocument.jarName();
            int caretOffset = STATE.editorDocument.caretOffset();
            try {
                DecompiledMethodLocator.RangeHint rangeHint = toRangeHint(lineNumberHint);
                DecompiledMethodLocator.JumpTarget jump = DecompiledMethodLocator.locate(
                        STATE.editorDocument.content(),
                        normalizedClass,
                        safe(methodName),
                        safe(methodDesc),
                        rangeHint
                );
                if (jump != null) {
                    caretOffset = Math.max(0, jump.startOffset);
                } else if (lineNumberHint != null && lineNumberHint > 0) {
                    caretOffset = Math.max(0, offsetByLineNumber(STATE.editorDocument.content(), lineNumberHint));
                } else {
                    int fallbackOffset = offsetByMethodName(STATE.editorDocument.content(), safe(methodName));
                    if (fallbackOffset > 0) {
                        caretOffset = fallbackOffset;
                    }
                }
            } catch (Throwable ex) {
                logger.debug("editor locate method failed: {}", ex.toString());
            }
            if (!isEditorOpenTicketActive(ticket)) {
                return;
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
            if (engine != null && engine.isEnabled() && isEditorOpenTicketActive(ticket)) {
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

        private DecompiledMethodLocator.RangeHint toRangeHint(Integer lineNumberHint) {
            if (lineNumberHint == null || lineNumberHint <= 0) {
                return null;
            }
            int min = Math.max(1, lineNumberHint - 2);
            int max = Math.max(min, lineNumberHint + 2);
            return new DecompiledMethodLocator.RangeHint(min, max);
        }

        private int offsetByLineNumber(String content, int lineNumber) {
            String text = safe(content);
            if (text.isEmpty() || lineNumber <= 1) {
                return 0;
            }
            int currentLine = 1;
            int offset = 0;
            while (offset < text.length() && currentLine < lineNumber) {
                if (text.charAt(offset) == '\n') {
                    currentLine++;
                }
                offset++;
            }
            return Math.max(0, Math.min(text.length(), offset));
        }

        private int offsetByMethodName(String content, String methodName) {
            String text = safe(content);
            String name = safe(methodName).trim();
            if (text.isEmpty() || name.isEmpty()) {
                return 0;
            }
            if ("<clinit>".equals(name)) {
                int staticIdx = text.indexOf("static {");
                return staticIdx >= 0 ? staticIdx : 0;
            }
            if ("<init>".equals(name)) {
                int ctorIdx = text.indexOf('(');
                return ctorIdx >= 0 ? ctorIdx : 0;
            }
            int idx = text.indexOf(name + "(");
            return idx >= 0 ? idx : 0;
        }

        private void pushNavigation(String className, String methodName, String methodDesc, Integer jarId) {
            NavState next = new NavState(
                    normalizeClass(className),
                    safe(methodName),
                    safe(methodDesc),
                    positiveJarId(jarId)
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
            Integer aJar = positiveJarId(a.jarId());
            Integer bJar = positiveJarId(b.jarId());
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

        private boolean applyCaretOnly(int caretOffset, String statusText, long ticket) {
            if (!isEditorOpenTicketActive(ticket)) {
                return false;
            }
            EditorDocumentDto doc = STATE.editorDocument;
            String content = safe(doc.content());
            int bounded = Math.max(0, Math.min(content.length(), Math.max(0, caretOffset)));
            if (!isEditorOpenTicketActive(ticket)) {
                return false;
            }
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

        private long nextEditorOpenTicket() {
            return STATE.editorOpenTicket.incrementAndGet();
        }

        private boolean isEditorOpenTicketActive(long ticket) {
            return ticket == STATE.editorOpenTicket.get();
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
                // best-effort UI fallback.
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
        public void openJdGui() {
            emitPathWindow(ToolingWindowAction.EXTERNAL_TOOLS, "jd-gui");
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
            openMarkdownViewer("Changelog", "CHANGELOG.md");
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
                    s.inputPath(),
                    s.sdkPath(),
                    s.resolveNestedJars(),
                    !s.fixClassPath()
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
        public ToolingConfigSnapshotDto configSnapshot() {
            return new ToolingConfigSnapshotDto(
                    STATE.showInnerClass,
                    STATE.buildSettings.fixClassPath(),
                    STATE.sortByMethod,
                    STATE.sortByClass,
                    STATE.groupTreeByJar,
                    STATE.mergePackageRoot,
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
            // best-effort UI fallback.
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
            // best-effort UI fallback.
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
        // best-effort UI fallback.
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

    private static String normalizeClass(String className) {
        String value = safe(className).trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private static Integer positiveJarId(Integer jarId) {
        if (jarId == null || jarId <= 0) {
            return null;
        }
        return jarId;
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
        map.put(McpLine.SINK_RULES, service(cfg.isMcpSinkRulesEnabled(), bind, cfg.getMcpSinkRulesPort(), auth, token));
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
        ServerConfig running = GlobalOptions.getServerConfig();
        if (running != null) {
            return running;
        }
        ConfigFile cfg = ConfigEngine.parseConfig();
        ServerConfig fallback = new ServerConfig();
        if (cfg == null) {
            fallback.setBind("0.0.0.0");
            fallback.setPort(10032);
            fallback.setAuth(false);
            fallback.setToken("JAR-ANALYZER-API-TOKEN");
            return fallback;
        }
        fallback.setBind(safe(cfg.getApiBind()).isEmpty() ? "0.0.0.0" : cfg.getApiBind());
        fallback.setPort(normalizePort(cfg.getApiPort(), 10032));
        fallback.setAuth(cfg.isApiAuth());
        fallback.setToken(safe(cfg.getApiToken()).isEmpty() ? "JAR-ANALYZER-API-TOKEN" : cfg.getApiToken());
        return fallback;
    }

    private static Set<String> selectedLeakTypes(LeakRulesDto rules) {
        if (rules == null) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (rules.jwt()) {
            out.add("jwt-token");
        }
        if (rules.idCard()) {
            out.add("id-card");
        }
        if (rules.ip()) {
            out.add("ip-addr");
        }
        if (rules.email()) {
            out.add("email");
        }
        if (rules.url()) {
            out.add("url");
        }
        if (rules.jdbc()) {
            out.add("jdbc");
        }
        if (rules.filePath()) {
            out.add("file-path");
        }
        if (rules.mac()) {
            out.add("mac-addr");
        }
        if (rules.phone()) {
            out.add("phone");
        }
        if (rules.apiKey()) {
            out.add("api-key");
        }
        if (rules.bankCard()) {
            out.add("bank-card");
        }
        if (rules.cloudAkSk()) {
            out.add("cloud-aksk");
        }
        if (rules.cryptoKey()) {
            out.add("crypto-key");
        }
        if (rules.aiKey()) {
            out.add("ai-key");
        }
        if (rules.password()) {
            out.add("password");
        }
        return out;
    }

    private static EnumSet<ScaScanService.RuleKind> enabledScaRules(ScaSettingsDto settings) {
        if (settings == null) {
            return EnumSet.noneOf(ScaScanService.RuleKind.class);
        }
        return ScaScanService.enabledRules(
                settings.scanLog4j(),
                settings.scanFastjson(),
                settings.scanShiro()
        );
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
