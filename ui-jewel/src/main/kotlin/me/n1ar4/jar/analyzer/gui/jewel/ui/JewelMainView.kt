package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.hoverable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import me.n1ar4.jar.analyzer.gui.jewel.service.AdvanceService
import me.n1ar4.jar.analyzer.gui.jewel.service.BuildService
import me.n1ar4.jar.analyzer.gui.jewel.service.CallImplService
import me.n1ar4.jar.analyzer.gui.jewel.service.ChainsService
import me.n1ar4.jar.analyzer.gui.jewel.service.EditorService
import me.n1ar4.jar.analyzer.gui.jewel.service.GadgetService
import me.n1ar4.jar.analyzer.gui.jewel.service.LeakService
import me.n1ar4.jar.analyzer.gui.jewel.service.McpService
import me.n1ar4.jar.analyzer.gui.jewel.service.NoteService
import me.n1ar4.jar.analyzer.gui.jewel.service.ProjectTreeService
import me.n1ar4.jar.analyzer.gui.jewel.service.ScaService
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchService
import me.n1ar4.jar.analyzer.gui.jewel.service.WebService
import me.n1ar4.jar.analyzer.gui.jewel.state.CoreTab
import me.n1ar4.jar.analyzer.gui.jewel.state.LeftToolPanel
import me.n1ar4.jar.analyzer.gui.jewel.state.JewelThemeMode
import me.n1ar4.jar.analyzer.gui.jewel.state.MainUiState
import me.n1ar4.jar.analyzer.gui.jewel.state.MainViewModel
import me.n1ar4.jar.analyzer.gui.jewel.state.RightToolPanel
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.starter.Const
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icon.IntelliJIconKey
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import java.awt.Cursor
import java.awt.Desktop
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private data class IslandPalette(
    val mainBackground: Color,
    val toolWindowBackground: Color,
    val editorBackground: Color,
    val stripeBackground: Color,
    val stripeForeground: Color,
    val panelBorder: Color,
    val divider: Color,
    val stripeOpenedBackground: Color,
    val stripeSelectedBackground: Color,
    val stripeSelectedBorder: Color,
    val stripeHoverBackground: Color,
    val topBarBackground: Color,
    val topBarBorder: Color,
    val topBarButtonHover: Color,
    val topBarButtonPressed: Color,
    val topBarTextMuted: Color,
    val titleText: Color,
    val islandCorner: Dp,
)

private fun paletteFor(mode: JewelThemeMode): IslandPalette {
    return when (mode) {
        JewelThemeMode.ISLAND_DARK -> IslandPalette(
            mainBackground = Color(0xFF26282C),
            toolWindowBackground = Color(0xFF191A1C),
            editorBackground = Color(0xFF191A1C),
            stripeBackground = Color(0xFF26282C),
            stripeForeground = Color(0xFF9FA2A8),
            panelBorder = Color(0xFF26282C),
            divider = Color(0x33FFFFFF),
            stripeOpenedBackground = Color(0x1AFFFFFF),
            stripeSelectedBackground = Color(0x803671E5),
            stripeSelectedBorder = Color(0x8C3871E1),
            stripeHoverBackground = Color(0x16FFFFFF),
            topBarBackground = Color(0xFF26282C),
            topBarBorder = Color(0xFF40434A),
            topBarButtonHover = Color(0x16FFFFFF),
            topBarButtonPressed = Color(0x26FFFFFF),
            topBarTextMuted = Color(0xFF9FA2A8),
            titleText = Color(0xFFD1D3D9),
            islandCorner = 20.dp,
        )

        JewelThemeMode.ISLAND_LIGHT -> IslandPalette(
            mainBackground = Color(0xFFE9EAEE),
            toolWindowBackground = Color(0xFFFFFFFF),
            editorBackground = Color(0xFFFFFFFF),
            stripeBackground = Color(0xFFE9EAEE),
            stripeForeground = Color(0xFF5F6269),
            panelBorder = Color(0xFFE9EAEE),
            divider = Color(0x20000000),
            stripeOpenedBackground = Color(0x12000000),
            stripeSelectedBackground = Color(0x4DA0BDF8),
            stripeSelectedBorder = Color(0x663871E1),
            stripeHoverBackground = Color(0x12000000),
            topBarBackground = Color(0xFFE8E8EB),
            topBarBorder = Color(0xFFD1D3D9),
            topBarButtonHover = Color(0x12000000),
            topBarButtonPressed = Color(0x20000000),
            topBarTextMuted = Color(0xFF5F6269),
            titleText = Color(0xFF080808),
            islandCorner = 20.dp,
        )
    }
}

@Composable
fun JewelMainView(
    viewModel: MainViewModel,
    stateFlow: StateFlow<MainUiState>,
    onRightPanelClick: (RightToolPanel) -> Unit,
    buildService: BuildService,
    projectTreeService: ProjectTreeService,
    editorService: EditorService,
    searchService: SearchService,
    callImplService: CallImplService,
    webService: WebService,
    noteService: NoteService,
    scaService: ScaService,
    leakService: LeakService,
    gadgetService: GadgetService,
    chainsService: ChainsService,
    advanceService: AdvanceService,
    mcpService: McpService,
    embeddedTopBar: Boolean = true,
) {
    val state by stateFlow.collectAsStateCompat()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshRuntimeState()
            delay(1200)
        }
    }

    val palette = remember(state.themeMode) { paletteFor(state.themeMode) }
    val projectName = remember {
        runCatching { Paths.get("").toAbsolutePath().fileName.toString() }.getOrDefault("jar-analyzer")
    }
    Column(modifier = Modifier.fillMaxSize().background(palette.mainBackground).padding(2.dp)) {
        if (embeddedTopBar) {
            IdeaTopBar(
                projectName = projectName,
                branchName = "dev",
                palette = palette,
                darkMode = state.themeMode == JewelThemeMode.ISLAND_DARK,
                onOpenProject = { onRightPanelClick(RightToolPanel.START) },
                onOpenSearch = { onRightPanelClick(RightToolPanel.SEARCH) },
                onRun = {
                    RuntimeFacades.build().startBuild()
                    onRightPanelClick(RightToolPanel.START)
                },
                onToggleTheme = {
                    if (state.themeMode == JewelThemeMode.ISLAND_DARK) {
                        RuntimeFacades.tooling().useThemeDefault()
                        viewModel.setThemeMode(JewelThemeMode.ISLAND_LIGHT)
                    } else {
                        RuntimeFacades.tooling().useThemeDark()
                        viewModel.setThemeMode(JewelThemeMode.ISLAND_DARK)
                    }
                },
                onOpenNotifications = { RuntimeFacades.tooling().openChangelog() },
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val dividerSize = 4.dp
            val leftStripeWidth = 40.dp
            val rightStripeWidth = 40.dp
            val dividerPx = with(density) { dividerSize.toPx() }
            val leftStripePx = with(density) { leftStripeWidth.toPx() }
            val rightStripePx = with(density) { rightStripeWidth.toPx() }

            val minLeftPanelPx = with(density) { 220.dp.toPx() }
            val minCenterPx = with(density) { 560.dp.toPx() }
            val minRightPanelPx = with(density) { 360.dp.toPx() }
            val defaultLeftPanelPx = with(density) { 280.dp.toPx() }
            val defaultRightPx = with(density) { 420.dp.toPx() }

            val totalWidthPx = with(density) { maxWidth.toPx() }

            var leftPanelPx by remember { mutableFloatStateOf(defaultLeftPanelPx) }
            var rightPanelPx by remember { mutableFloatStateOf(defaultRightPx) }
            var leftPrimaryPanel by remember { mutableStateOf<LeftToolPanel?>(LeftToolPanel.PROJECT) }
            var leftSecondaryPanel by remember { mutableStateOf<LeftToolPanel?>(null) }
            var leftSplitRatio by remember { mutableFloatStateOf(0.58f) }
            var rightPrimaryPanel by remember { mutableStateOf<RightToolPanel?>(state.activeRightPanel) }
            var rightSecondaryPanel by remember { mutableStateOf<RightToolPanel?>(null) }
            var rightSplitRatio by remember { mutableFloatStateOf(0.56f) }
            val leftTopPanels = remember { mutableStateListOf(LeftToolPanel.PROJECT) }
            val leftBottomPanels = remember { mutableStateListOf(LeftToolPanel.STRUCTURE) }
            val rightTopPanels = remember { mutableStateListOf(*RightToolPanel.topStripePanels.toTypedArray()) }
            val rightBottomPanels = remember { mutableStateListOf(*RightToolPanel.bottomStripePanels.toTypedArray()) }

            fun alignLeftOpenedPanelsToGroups() {
                val opened = listOfNotNull(leftPrimaryPanel, leftSecondaryPanel).distinct()
                leftPrimaryPanel = opened.firstOrNull { it in leftTopPanels }
                leftSecondaryPanel = opened.firstOrNull { it in leftBottomPanels }
            }

            fun alignRightOpenedPanelsToGroups() {
                val opened = listOfNotNull(rightPrimaryPanel, rightSecondaryPanel).distinct()
                rightPrimaryPanel = opened.firstOrNull { it in rightTopPanels }
                rightSecondaryPanel = opened.firstOrNull { it in rightBottomPanels }
            }

            fun onLeftPanelClick(panel: LeftToolPanel) {
                if (panel in leftTopPanels) {
                    leftPrimaryPanel = if (leftPrimaryPanel == panel) null else panel
                } else {
                    leftSecondaryPanel = if (leftSecondaryPanel == panel) null else panel
                }
                alignLeftOpenedPanelsToGroups()
            }

            fun onLeftStripeReordered(
                newTopPanels: List<LeftToolPanel>,
                newBottomPanels: List<LeftToolPanel>,
            ) {
                leftTopPanels.clear()
                leftTopPanels.addAll(newTopPanels.distinct())
                leftBottomPanels.clear()
                leftBottomPanels.addAll(newBottomPanels.distinct().filter { it !in leftTopPanels })
                alignLeftOpenedPanelsToGroups()
            }

            fun onRightPanelClickLocal(panel: RightToolPanel) {
                if (panel in rightTopPanels) {
                    val opened = rightPrimaryPanel == panel
                    val active = state.activeRightPanel == panel
                    if (!opened) {
                        rightPrimaryPanel = panel
                        onRightPanelClick(panel)
                    } else if (!active) {
                        onRightPanelClick(panel)
                    } else {
                        rightPrimaryPanel = null
                        if (rightSecondaryPanel != null) {
                            onRightPanelClick(rightSecondaryPanel!!)
                        }
                    }
                } else {
                    val opened = rightSecondaryPanel == panel
                    val active = state.activeRightPanel == panel
                    if (!opened) {
                        rightSecondaryPanel = panel
                        onRightPanelClick(panel)
                    } else if (!active) {
                        onRightPanelClick(panel)
                    } else {
                        rightSecondaryPanel = null
                        if (rightPrimaryPanel != null) {
                            onRightPanelClick(rightPrimaryPanel!!)
                        }
                    }
                }
                alignRightOpenedPanelsToGroups()
            }

            fun onRightStripeReordered(
                newTopPanels: List<RightToolPanel>,
                newBottomPanels: List<RightToolPanel>,
            ) {
                rightTopPanels.clear()
                rightTopPanels.addAll(newTopPanels.distinct())
                rightBottomPanels.clear()
                rightBottomPanels.addAll(newBottomPanels.distinct().filter { it !in rightTopPanels })
                alignRightOpenedPanelsToGroups()
            }

            LaunchedEffect(state.activeRightPanel) {
                val target = state.activeRightPanel
                if (target in rightTopPanels) {
                    rightPrimaryPanel = target
                } else {
                    rightSecondaryPanel = target
                }
                alignRightOpenedPanelsToGroups()
            }

            val leftOpenPanels = listOfNotNull(leftPrimaryPanel, leftSecondaryPanel)
            val leftHasAnyPanel = leftOpenPanels.isNotEmpty()
            val rightOpenPanels = listOfNotNull(rightPrimaryPanel, rightSecondaryPanel)
            val rightHasAnyPanel = rightOpenPanels.isNotEmpty()

            val rightPanelForLeftCalcPx = if (rightHasAnyPanel) {
                rightPanelPx.coerceAtLeast(minRightPanelPx)
            } else {
                0f
            }
            val rightBlockPx = if (rightHasAnyPanel) rightPanelForLeftCalcPx + rightStripePx else rightStripePx
            val maxLeftPanelPx = max(
                minLeftPanelPx,
                totalWidthPx - rightBlockPx - minCenterPx - leftStripePx - dividerPx * 2f,
            )
            val leftPanelForLayoutPx = if (leftHasAnyPanel) {
                leftPanelPx.coerceIn(minLeftPanelPx, maxLeftPanelPx)
            } else {
                0f
            }

            val leftBlockPxForRightCalc = if (leftHasAnyPanel) leftPanelForLayoutPx + leftStripePx else leftStripePx
            val maxRightPanelPx = max(
                minRightPanelPx,
                totalWidthPx - leftBlockPxForRightCalc - minCenterPx - rightStripePx - dividerPx * 2f,
            )
            val rightPanelForLayoutPx = if (rightHasAnyPanel) {
                rightPanelPx.coerceIn(minRightPanelPx, maxRightPanelPx)
            } else {
                0f
            }

            val appliedLeftPanelPx = if (leftHasAnyPanel) leftPanelForLayoutPx else 0f
            val appliedLeftBlockPx = appliedLeftPanelPx + leftStripePx
            val rightPanelHostPx = if (rightHasAnyPanel) rightPanelForLayoutPx else 0f
            val appliedRightBlockPx = rightPanelHostPx + rightStripePx
            val centerWidthPx = max(
                with(density) { 240.dp.toPx() },
                totalWidthPx - appliedLeftBlockPx - appliedRightBlockPx - dividerPx * 2f,
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.width(with(density) { appliedLeftBlockPx.toDp() }).fillMaxHeight(),
                ) {
                    LeftStripe(
                        openPanels = leftOpenPanels.toSet(),
                        darkMode = state.themeMode == JewelThemeMode.ISLAND_DARK,
                        palette = palette,
                        topPanels = leftTopPanels.toList(),
                        bottomPanels = leftBottomPanels.toList(),
                        onClick = ::onLeftPanelClick,
                        onReorder = ::onLeftStripeReordered,
                    )
                    if (leftHasAnyPanel) {
                        PanelSurface(
                            modifier = Modifier.width(with(density) { appliedLeftPanelPx.toDp() }).fillMaxHeight(),
                            borderColor = palette.panelBorder,
                            backgroundColor = palette.toolWindowBackground,
                            corner = palette.islandCorner,
                        ) {
                            if (leftPrimaryPanel != null && leftSecondaryPanel != null) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val totalHeightPx = with(density) { maxHeight.toPx() }
                                    val minSectionPx = with(density) { 160.dp.toPx() }
                                    val sectionDividerPx = with(density) { dividerSize.toPx() }
                                    val maxTopPx = max(minSectionPx, totalHeightPx - minSectionPx - sectionDividerPx)
                                    val leftRatioMin = if (totalHeightPx > 1f) minSectionPx / totalHeightPx else leftSplitRatio
                                    val leftRatioMax = if (totalHeightPx > 1f) maxTopPx / totalHeightPx else leftSplitRatio
                                    val effectiveLeftSplitRatio = if (totalHeightPx > 1f) {
                                        leftSplitRatio.coerceIn(leftRatioMin, leftRatioMax)
                                    } else {
                                        leftSplitRatio
                                    }
                                    val topPx = (totalHeightPx * effectiveLeftSplitRatio).coerceIn(minSectionPx, maxTopPx)

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        LeftDockPanelSection(
                                            panel = leftPrimaryPanel,
                                            onHide = { leftPrimaryPanel?.let(::onLeftPanelClick) },
                                            projectTreeService = projectTreeService,
                                            editorService = editorService,
                                            callImplService = callImplService,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(with(density) { topPx.toDp() }),
                                        )
                                        HorizontalDragDivider(
                                            modifier = Modifier.height(dividerSize).fillMaxWidth(),
                                            color = palette.divider,
                                            hoverColor = palette.topBarButtonHover,
                                            onDrag = { deltaY ->
                                                if (totalHeightPx <= 1f) {
                                                    return@HorizontalDragDivider
                                                }
                                                val nextTop = ((totalHeightPx * effectiveLeftSplitRatio) + deltaY)
                                                    .coerceIn(minSectionPx, maxTopPx)
                                                leftSplitRatio = nextTop / totalHeightPx
                                            },
                                            onDoubleClick = { leftSplitRatio = 0.58f },
                                        )
                                        LeftDockPanelSection(
                                            panel = leftSecondaryPanel,
                                            onHide = { leftSecondaryPanel?.let(::onLeftPanelClick) },
                                            projectTreeService = projectTreeService,
                                            editorService = editorService,
                                            callImplService = callImplService,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            } else {
                                LeftDockPanelSection(
                                    panel = leftPrimaryPanel ?: leftSecondaryPanel,
                                    onHide = { (leftPrimaryPanel ?: leftSecondaryPanel)?.let(::onLeftPanelClick) },
                                    projectTreeService = projectTreeService,
                                    editorService = editorService,
                                    callImplService = callImplService,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }

                VerticalDragDivider(
                    modifier = Modifier.width(dividerSize).fillMaxHeight(),
                    color = palette.divider,
                    hoverColor = palette.topBarButtonHover,
                    onDrag = { deltaX ->
                        if (!leftHasAnyPanel) {
                            return@VerticalDragDivider
                        }
                        val nowRightBlockPx = if (rightHasAnyPanel) rightPanelForLayoutPx + rightStripePx else rightStripePx
                        val nowMaxLeftPanelPx = max(
                            minLeftPanelPx,
                            totalWidthPx - nowRightBlockPx - minCenterPx - leftStripePx - dividerPx * 2f,
                        )
                        leftPanelPx = (leftPanelForLayoutPx + deltaX).coerceIn(minLeftPanelPx, nowMaxLeftPanelPx)
                    },
                    onDoubleClick = {
                        if (!leftHasAnyPanel) {
                            leftPrimaryPanel = LeftToolPanel.PROJECT
                        }
                        leftPanelPx = defaultLeftPanelPx.coerceIn(minLeftPanelPx, maxLeftPanelPx)
                    },
                )

                PanelSurface(
                    modifier = Modifier.width(with(density) { centerWidthPx.toDp() }).fillMaxHeight(),
                    borderColor = palette.panelBorder,
                    backgroundColor = palette.editorBackground,
                    corner = palette.islandCorner,
                ) {
                    PanelTitle("Java Decompile Code", palette)
                    Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                    ComposeCodeEditor(editorService)
                }

                VerticalDragDivider(
                    modifier = Modifier.width(dividerSize).fillMaxHeight(),
                    color = palette.divider,
                    hoverColor = palette.topBarButtonHover,
                    onDrag = { deltaX ->
                        if (!rightHasAnyPanel) {
                            return@VerticalDragDivider
                        }
                        val leftBlockPx = if (leftHasAnyPanel) leftPanelForLayoutPx + leftStripePx else leftStripePx
                        val maxRightPx = max(
                            minRightPanelPx,
                            totalWidthPx - leftBlockPx - minCenterPx - rightStripePx - dividerPx * 2f,
                        )
                        rightPanelPx = (rightPanelForLayoutPx - deltaX).coerceIn(minRightPanelPx, maxRightPx)
                    },
                    onDoubleClick = {
                        if (!rightHasAnyPanel) {
                            val target = state.activeRightPanel
                            if (target in rightTopPanels) {
                                rightPrimaryPanel = target
                            } else {
                                rightSecondaryPanel = target
                            }
                            alignRightOpenedPanelsToGroups()
                        }
                        val leftBlockPx = if (leftHasAnyPanel) leftPanelForLayoutPx + leftStripePx else leftStripePx
                        val maxRightPx = max(
                            minRightPanelPx,
                            totalWidthPx - leftBlockPx - minCenterPx - rightStripePx - dividerPx * 2f,
                        )
                        rightPanelPx = defaultRightPx.coerceIn(minRightPanelPx, maxRightPx)
                    },
                )

                Row(
                    modifier = Modifier.width(with(density) { appliedRightBlockPx.toDp() }).fillMaxHeight(),
                ) {
                    if (rightHasAnyPanel) {
                        PanelSurface(
                            modifier = Modifier.width(with(density) { rightPanelHostPx.toDp() }).fillMaxHeight(),
                            borderColor = palette.panelBorder,
                            backgroundColor = palette.toolWindowBackground,
                            corner = palette.islandCorner,
                        ) {
                            if (rightPrimaryPanel != null && rightSecondaryPanel != null) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val totalHeightPx = with(density) { maxHeight.toPx() }
                                    val minSectionPx = with(density) { 160.dp.toPx() }
                                    val sectionDividerPx = with(density) { dividerSize.toPx() }
                                    val maxTopPx = max(minSectionPx, totalHeightPx - minSectionPx - sectionDividerPx)
                                    val rightRatioMin = if (totalHeightPx > 1f) minSectionPx / totalHeightPx else rightSplitRatio
                                    val rightRatioMax = if (totalHeightPx > 1f) maxTopPx / totalHeightPx else rightSplitRatio
                                    val effectiveRightSplitRatio = if (totalHeightPx > 1f) {
                                        rightSplitRatio.coerceIn(rightRatioMin, rightRatioMax)
                                    } else {
                                        rightSplitRatio
                                    }
                                    val topPx = (totalHeightPx * effectiveRightSplitRatio).coerceIn(minSectionPx, maxTopPx)

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        RightDockPanelSection(
                                            panel = rightPrimaryPanel,
                                            onHide = { rightPrimaryPanel?.let(::onRightPanelClickLocal) },
                                            state = state,
                                            buildService = buildService,
                                            searchService = searchService,
                                            callImplService = callImplService,
                                            webService = webService,
                                            noteService = noteService,
                                            scaService = scaService,
                                            leakService = leakService,
                                            gadgetService = gadgetService,
                                            chainsService = chainsService,
                                            advanceService = advanceService,
                                            mcpService = mcpService,
                                            editorService = editorService,
                                            onStartMcp = { viewModel.startMcpConfigured() },
                                            onStopMcp = { viewModel.stopAllMcp() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(with(density) { topPx.toDp() }),
                                        )
                                        HorizontalDragDivider(
                                            modifier = Modifier.height(dividerSize).fillMaxWidth(),
                                            color = palette.divider,
                                            hoverColor = palette.topBarButtonHover,
                                            onDrag = { deltaY ->
                                                if (totalHeightPx <= 1f) {
                                                    return@HorizontalDragDivider
                                                }
                                                val nextTop = ((totalHeightPx * effectiveRightSplitRatio) + deltaY)
                                                    .coerceIn(minSectionPx, maxTopPx)
                                                rightSplitRatio = nextTop / totalHeightPx
                                            },
                                            onDoubleClick = { rightSplitRatio = 0.56f },
                                        )
                                        RightDockPanelSection(
                                            panel = rightSecondaryPanel,
                                            onHide = { rightSecondaryPanel?.let(::onRightPanelClickLocal) },
                                            state = state,
                                            buildService = buildService,
                                            searchService = searchService,
                                            callImplService = callImplService,
                                            webService = webService,
                                            noteService = noteService,
                                            scaService = scaService,
                                            leakService = leakService,
                                            gadgetService = gadgetService,
                                            chainsService = chainsService,
                                            advanceService = advanceService,
                                            mcpService = mcpService,
                                            editorService = editorService,
                                            onStartMcp = { viewModel.startMcpConfigured() },
                                            onStopMcp = { viewModel.stopAllMcp() },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            } else {
                                RightDockPanelSection(
                                    panel = rightPrimaryPanel ?: rightSecondaryPanel,
                                    onHide = { (rightPrimaryPanel ?: rightSecondaryPanel)?.let(::onRightPanelClickLocal) },
                                    state = state,
                                    buildService = buildService,
                                    searchService = searchService,
                                    callImplService = callImplService,
                                    webService = webService,
                                    noteService = noteService,
                                    scaService = scaService,
                                    leakService = leakService,
                                    gadgetService = gadgetService,
                                    chainsService = chainsService,
                                    advanceService = advanceService,
                                    mcpService = mcpService,
                                    editorService = editorService,
                                    onStartMcp = { viewModel.startMcpConfigured() },
                                    onStopMcp = { viewModel.stopAllMcp() },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    RightStripe(
                        openPanels = rightOpenPanels.toSet(),
                        activePanel = state.activeRightPanel,
                        darkMode = state.themeMode == JewelThemeMode.ISLAND_DARK,
                        palette = palette,
                        topPanels = rightTopPanels.toList(),
                        bottomPanels = rightBottomPanels.toList(),
                        onClick = ::onRightPanelClickLocal,
                        onReorder = ::onRightStripeReordered,
                    )
                }
            }
        }
    }
}

@Composable
fun DecoratedWindowScope.JewelDecoratedTitleBar(
    themeMode: JewelThemeMode,
    projectName: String,
    branchName: String,
    onOpenProject: () -> Unit,
    onOpenSearch: () -> Unit,
    onRun: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val palette = remember(themeMode) { paletteFor(themeMode) }
    val darkMode = themeMode == JewelThemeMode.ISLAND_DARK

    TitleBar(
        Modifier
            .newFullscreenControls()
            .background(palette.topBarBackground)
            .border(1.dp, palette.topBarBorder),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Start),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(
                onClick = onOpenProject,
                modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
            ) {
                Text(projectName)
            }
            Text(branchName, color = palette.topBarTextMuted)
        }

        Text(Const.app)

        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(
                onClick = onRun,
                modifier = Modifier.width(76.dp).height(24.dp),
            ) {
                Text("Analyze")
            }
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.search, darkMode),
                contentDescription = "search",
                palette = palette,
                onClick = onOpenSearch,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.notifications, darkMode),
                contentDescription = "notifications",
                palette = palette,
                onClick = onOpenNotifications,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.settings, darkMode),
                contentDescription = "theme",
                palette = palette,
                onClick = onToggleTheme,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.more, darkMode),
                contentDescription = "more",
                palette = palette,
                onClick = { RuntimeFacades.tooling().openProjectSite() },
            )
        }
    }
}

private data class IconVariants(
    val light: String,
    val dark: String,
)

private object IdeaIconRegistry {
    fun key(path: String): IconKey {
        return IntelliJIconKey(path, path, IdeaIconRegistry::class.java)
    }
}

private object IdeaTopBarIcons {
    val project = IconVariants(
        light = "icons/idea/expui/toolwindows/project.svg",
        dark = "icons/idea/expui/toolwindows/project_dark.svg",
    )
    val vcs = IconVariants(
        light = "icons/idea/expui/general/vcs.svg",
        dark = "icons/idea/expui/general/vcs_dark.svg",
    )
    val run = IconVariants(
        light = "icons/idea/expui/toolwindows/run.svg",
        dark = "icons/idea/expui/toolwindows/run_dark.svg",
    )
    val search = IconVariants(
        light = "icons/idea/expui/general/search.svg",
        dark = "icons/idea/expui/general/search_dark.svg",
    )
    val notifications = IconVariants(
        light = "icons/idea/expui/toolwindows/notifications.svg",
        dark = "icons/idea/expui/toolwindows/notifications_dark.svg",
    )
    val settings = IconVariants(
        light = "icons/idea/expui/general/settings.svg",
        dark = "icons/idea/expui/general/settings_dark.svg",
    )
    val more = IconVariants(
        light = "icons/idea/expui/general/moreHorizontal.svg",
        dark = "icons/idea/expui/general/moreHorizontal_dark.svg",
    )
}

@Composable
private fun IdeaTopBar(
    projectName: String,
    branchName: String,
    palette: IslandPalette,
    darkMode: Boolean,
    onOpenProject: () -> Unit,
    onOpenSearch: () -> Unit,
    onRun: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(palette.topBarBackground, RoundedCornerShape(10.dp))
            .border(1.dp, palette.topBarBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.project, darkMode),
                contentDescription = "project",
                palette = palette,
                onClick = onOpenProject,
            )
            Text(projectName, color = palette.titleText)
            Spacer(modifier = Modifier.width(6.dp))
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.vcs, darkMode),
                contentDescription = "branch",
                palette = palette,
                onClick = {},
            )
            Text(branchName, color = palette.topBarTextMuted)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(
                onClick = onRun,
                modifier = Modifier.width(76.dp).height(24.dp),
            ) {
                Text("Analyze")
            }
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.search, darkMode),
                contentDescription = "search",
                palette = palette,
                onClick = onOpenSearch,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.notifications, darkMode),
                contentDescription = "notifications",
                palette = palette,
                onClick = onOpenNotifications,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.settings, darkMode),
                contentDescription = "theme",
                palette = palette,
                onClick = onToggleTheme,
            )
            TopBarIconButton(
                icon = iconForTopBar(IdeaTopBarIcons.more, darkMode),
                contentDescription = "more",
                palette = palette,
                onClick = { RuntimeFacades.tooling().openProjectSite() },
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: IconKey,
    contentDescription: String,
    palette: IslandPalette,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(26.dp)
            .background(
                when {
                    pressed -> palette.topBarButtonPressed
                    hovered -> palette.topBarButtonHover
                    else -> Color.Transparent
                },
                RoundedCornerShape(6.dp),
            )
            .hoverable(interactions)
            .clickable(
                interactionSource = interactions,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun RightPanelHeader(
    panel: RightToolPanel,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(panel.title)
        IdeaOutlinedButton(
            onClick = onCollapse,
            modifier = Modifier.height(24.dp).width(58.dp),
        ) {
            Text("Hide")
        }
    }
}

@Composable
private fun RightDockPanelSection(
    panel: RightToolPanel?,
    onHide: () -> Unit,
    state: MainUiState,
    buildService: BuildService,
    searchService: SearchService,
    callImplService: CallImplService,
    webService: WebService,
    noteService: NoteService,
    scaService: ScaService,
    leakService: LeakService,
    gadgetService: GadgetService,
    chainsService: ChainsService,
    advanceService: AdvanceService,
    mcpService: McpService,
    editorService: EditorService,
    onStartMcp: () -> Unit,
    onStopMcp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (panel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No panel")
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        RightPanelHeader(
            panel = panel,
            onCollapse = onHide,
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.fillMaxSize()) {
            RightToolPanelContent(
                panel = panel,
                state = state,
                buildService = buildService,
                searchService = searchService,
                callImplService = callImplService,
                webService = webService,
                noteService = noteService,
                scaService = scaService,
                leakService = leakService,
                gadgetService = gadgetService,
                chainsService = chainsService,
                advanceService = advanceService,
                mcpService = mcpService,
                editorService = editorService,
                onStartMcp = onStartMcp,
                onStopMcp = onStopMcp,
            )
        }
    }
}

@Composable
private fun LeftDockPanelSection(
    panel: LeftToolPanel?,
    onHide: () -> Unit,
    projectTreeService: ProjectTreeService,
    editorService: EditorService,
    callImplService: CallImplService,
    modifier: Modifier = Modifier,
) {
    if (panel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No panel")
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (panel == LeftToolPanel.PROJECT) "Project" else "Structure")
            IdeaOutlinedButton(
                onClick = onHide,
                modifier = Modifier.height(24.dp).width(58.dp),
            ) {
                Text("Hide")
            }
        }
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.fillMaxSize()) {
            when (panel) {
                LeftToolPanel.PROJECT -> ProjectTreePane(projectTreeService)
                LeftToolPanel.STRUCTURE -> ProjectStructurePane(
                    editorService = editorService,
                    callImplService = callImplService,
                )
            }
        }
    }
}

@Composable
private fun LeftStripe(
    openPanels: Set<LeftToolPanel>,
    darkMode: Boolean,
    palette: IslandPalette,
    topPanels: List<LeftToolPanel>,
    bottomPanels: List<LeftToolPanel>,
    onClick: (LeftToolPanel) -> Unit,
    onReorder: (List<LeftToolPanel>, List<LeftToolPanel>) -> Unit,
) {
    val density = LocalDensity.current
    val buttonHeightPx = with(density) { 40.dp.toPx() }
    val buttonLayouts = remember { mutableMapOf<LeftToolPanel, StripeButtonLayout>() }
    var stripeRootY by remember { mutableFloatStateOf(0f) }
    var stripeHeightPx by remember { mutableFloatStateOf(0f) }

    var draggingPanel by remember { mutableStateOf<LeftToolPanel?>(null) }
    var dragSnapshot by remember { mutableStateOf<StripeDragSnapshot<LeftToolPanel>?>(null) }
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    var dragAnchorY by remember { mutableFloatStateOf(buttonHeightPx / 2f) }
    var dropTarget by remember { mutableStateOf<StripeDropTarget?>(null) }

    LaunchedEffect(topPanels, bottomPanels) {
        val visible = (topPanels + bottomPanels).toSet()
        val stale = buttonLayouts.keys.filter { it !in visible }
        stale.forEach { buttonLayouts.remove(it) }
    }

    fun computeDropTarget(pointerY: Float, snapshot: StripeDragSnapshot<LeftToolPanel>): StripeDropTarget {
        val orderedCenters = snapshot.orderedCentersWithoutDragged
        var insertCombinedIndex = orderedCenters.size
        for ((index, center) in orderedCenters.withIndex()) {
            if (pointerY < center) {
                insertCombinedIndex = index
                break
            }
        }
        return if (insertCombinedIndex <= snapshot.topSizeWithoutDragged) {
            StripeDropTarget(inBottom = false, index = insertCombinedIndex)
        } else {
            StripeDropTarget(inBottom = true, index = insertCombinedIndex - snapshot.topSizeWithoutDragged)
        }
    }

    fun applyDragReorder() {
        val panel = draggingPanel ?: return
        val snapshot = dragSnapshot ?: return
        val target = dropTarget ?: return
        val nextTop = snapshot.topPanels.filterNot { it == panel }.toMutableList()
        val nextBottom = snapshot.bottomPanels.filterNot { it == panel }.toMutableList()
        if (target.inBottom) {
            nextBottom.add(target.index.coerceIn(0, nextBottom.size), panel)
        } else {
            nextTop.add(target.index.coerceIn(0, nextTop.size), panel)
        }
        onReorder(nextTop, nextBottom)
    }

    fun stopDragging(apply: Boolean) {
        if (apply) {
            applyDragReorder()
        }
        draggingPanel = null
        dragSnapshot = null
        dropTarget = null
        dragPointerY = 0f
    }

    fun startDragging(panel: LeftToolPanel, localTouchY: Float) {
        val topSnapshot = topPanels.toList()
        val bottomSnapshot = bottomPanels.toList()
        val layout = buttonLayouts[panel]
        val fallbackIndex = (topSnapshot + bottomSnapshot).indexOf(panel).coerceAtLeast(0)
        val fallbackTop = fallbackIndex * buttonHeightPx
        val orderedWithoutDragged = (topSnapshot + bottomSnapshot).filterNot { it == panel }
        val centersWithoutDragged = orderedWithoutDragged.mapIndexed { index, candidate ->
            buttonLayouts[candidate]?.centerY ?: ((index + 0.5f) * buttonHeightPx)
        }
        val snapshot = StripeDragSnapshot(
            panel = panel,
            topPanels = topSnapshot,
            bottomPanels = bottomSnapshot,
            topSizeWithoutDragged = topSnapshot.count { it != panel },
            orderedCentersWithoutDragged = centersWithoutDragged,
        )
        draggingPanel = panel
        dragSnapshot = snapshot
        dragAnchorY = localTouchY
        dragPointerY = (layout?.topY ?: fallbackTop) + localTouchY
        dropTarget = computeDropTarget(dragPointerY, snapshot)
    }

    fun updateDragging(panel: LeftToolPanel, deltaY: Float) {
        val snapshot = dragSnapshot
        if (draggingPanel != panel || snapshot == null) {
            return
        }
        dragPointerY += deltaY
        val nextDropTarget = computeDropTarget(dragPointerY, snapshot)
        if (nextDropTarget != dropTarget) {
            dropTarget = nextDropTarget
        }
    }

    @Composable
    fun RenderStripeGroup(
        panels: List<LeftToolPanel>,
        inBottomGroup: Boolean,
    ) {
        val activeDragPanel = dragSnapshot?.panel
        val shiftStepPx = buttonHeightPx + with(density) { 1.dp.toPx() }
        val target = dropTarget
        val snapshot = dragSnapshot

        fun shiftSlotsFor(panel: LeftToolPanel): Int {
            val currentSnapshot = snapshot ?: return 0
            val draggedPanel = currentSnapshot.panel
            if (panel == draggedPanel) {
                return 0
            }
            val sourceGroup = if (inBottomGroup) currentSnapshot.bottomPanels else currentSnapshot.topPanels
            val baseIndex = sourceGroup.indexOf(panel)
            if (baseIndex < 0) {
                return 0
            }
            val withoutDragged = sourceGroup.filterNot { it == draggedPanel }
            val previewGroup = if (target != null && target.inBottom == inBottomGroup) {
                withoutDragged.toMutableList().also { list ->
                    list.add(target.index.coerceIn(0, list.size), draggedPanel)
                }
            } else {
                withoutDragged
            }
            val previewIndex = previewGroup.indexOf(panel)
            if (previewIndex < 0) {
                return 0
            }
            return previewIndex - baseIndex
        }

        for (panel in panels) {
            LeftStripeButton(
                panel = panel,
                opened = openPanels.contains(panel),
                hidden = activeDragPanel == panel,
                shiftYPx = shiftSlotsFor(panel) * shiftStepPx,
                darkMode = darkMode,
                palette = palette,
                onClick = { onClick(panel) },
                onPlaced = { absoluteTopY, height ->
                    updateStripeLayout(
                        store = buttonLayouts,
                        key = panel,
                        topY = absoluteTopY - stripeRootY,
                        height = height,
                    )
                },
                onDragStart = { localTouchY -> startDragging(panel, localTouchY) },
                onDragDelta = { deltaY -> updateDragging(panel, deltaY) },
                onDragEnd = { stopDragging(apply = true) },
                onDragCancel = { stopDragging(apply = false) },
            )
        }
    }

    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(palette.stripeBackground, RoundedCornerShape(10.dp))
            .border(1.dp, palette.panelBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 3.dp)
            .onGloballyPositioned { coords ->
                stripeRootY = coords.positionInRoot().y
                stripeHeightPx = coords.size.height.toFloat()
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                RenderStripeGroup(
                    panels = topPanels,
                    inBottomGroup = false,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                RenderStripeGroup(
                    panels = bottomPanels,
                    inBottomGroup = true,
                )
            }
        }

        val snapshot = dragSnapshot
        val target = dropTarget
        if (snapshot != null && target != null) {
            val placeholderRawTop = if (snapshot.orderedCentersWithoutDragged.isEmpty()) {
                dragPointerY - dragAnchorY
            } else {
                computeDropPlaceholderTop(snapshot, target, buttonHeightPx)
            }
            val placeholderTopPx = placeholderRawTop.let { raw ->
                val maxTop = stripeHeightPx - buttonHeightPx
                if (maxTop > 0f) raw.coerceIn(0f, maxTop) else raw
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = 1f
                        translationY = placeholderTopPx
                    }
                    .zIndex(4f),
            ) {
                StripeDropPlaceholder(palette = palette)
            }
        }

        val panel = snapshot?.panel
        if (panel != null) {
            val targetGhostTopPx = (dragPointerY - dragAnchorY)
                .let { raw ->
                    val maxTop = stripeHeightPx - buttonHeightPx
                    if (maxTop > 0f) raw.coerceIn(0f, maxTop) else raw
                }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = 1f
                        translationY = targetGhostTopPx
                    }
                    .size(width = 37.dp, height = 40.dp)
                    .background(
                        color = palette.stripeSelectedBackground.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = palette.stripeSelectedBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .zIndex(5f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconForLeftPanel(panel, darkMode = darkMode, selected = true),
                    panel.title,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LeftStripeButton(
    panel: LeftToolPanel,
    opened: Boolean,
    hidden: Boolean,
    shiftYPx: Float,
    darkMode: Boolean,
    palette: IslandPalette,
    onClick: () -> Unit,
    onPlaced: (Float, Float) -> Unit,
    onDragStart: (Float) -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    var dragDistance by remember(panel) { mutableFloatStateOf(0f) }
    var suppressClick by remember(panel) { mutableStateOf(false) }
    val animatedShiftY by animateFloatAsState(
        targetValue = if (hidden) 0f else shiftYPx,
        animationSpec = tween(durationMillis = 140),
        label = "leftStripeShift",
    )

    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .background(palette.toolWindowBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("${panel.title} (drag to reorder)")
            }
        },
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = animatedShiftY }
                .alpha(if (hidden) 0f else 1f)
                .padding(horizontal = 1.dp, vertical = 0.dp)
                .size(width = 37.dp, height = 40.dp)
                .background(
                    when {
                        hidden -> Color.Transparent
                        pressed -> palette.topBarButtonPressed
                        opened -> palette.stripeOpenedBackground
                        hovered -> palette.stripeHoverBackground
                        else -> Color.Transparent
                    },
                    RoundedCornerShape(8.dp),
                )
                .border(
                    1.dp,
                    if (!hidden && opened) palette.divider else Color.Transparent,
                    RoundedCornerShape(8.dp),
                )
                .onGloballyPositioned { coords ->
                    onPlaced(coords.positionInRoot().y, coords.size.height.toFloat())
                }
                .pointerInput(panel) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragDistance = 0f
                            suppressClick = false
                            onDragStart(offset.y)
                        },
                        onDrag = { change, amount ->
                            dragDistance += amount.y
                            if (abs(dragDistance) > 6f) {
                                suppressClick = true
                            }
                            onDragDelta(amount.y)
                            change.consume()
                        },
                        onDragEnd = {
                            if (abs(dragDistance) > 6f) {
                                onDragEnd()
                            } else {
                                onDragCancel()
                            }
                            dragDistance = 0f
                            suppressClick = false
                        },
                        onDragCancel = {
                            dragDistance = 0f
                            onDragCancel()
                            suppressClick = false
                        },
                    )
                }
                .hoverable(interactions)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    onClick = {
                        if (!hidden && !suppressClick) {
                            onClick()
                        }
                        suppressClick = false
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconForLeftPanel(panel, darkMode = darkMode, selected = opened),
                panel.title,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun RightStripe(
    openPanels: Set<RightToolPanel>,
    activePanel: RightToolPanel,
    darkMode: Boolean,
    palette: IslandPalette,
    topPanels: List<RightToolPanel>,
    bottomPanels: List<RightToolPanel>,
    onClick: (RightToolPanel) -> Unit,
    onReorder: (List<RightToolPanel>, List<RightToolPanel>) -> Unit,
) {
    val density = LocalDensity.current
    val buttonHeightPx = with(density) { 40.dp.toPx() }
    val buttonLayouts = remember { mutableMapOf<RightToolPanel, StripeButtonLayout>() }
    var stripeRootY by remember { mutableFloatStateOf(0f) }
    var stripeHeightPx by remember { mutableFloatStateOf(0f) }

    var draggingPanel by remember { mutableStateOf<RightToolPanel?>(null) }
    var dragSnapshot by remember { mutableStateOf<StripeDragSnapshot<RightToolPanel>?>(null) }
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    var dragAnchorY by remember { mutableFloatStateOf(buttonHeightPx / 2f) }
    var dropTarget by remember { mutableStateOf<StripeDropTarget?>(null) }

    LaunchedEffect(topPanels, bottomPanels) {
        val visible = (topPanels + bottomPanels).toSet()
        val stale = buttonLayouts.keys.filter { it !in visible }
        stale.forEach { buttonLayouts.remove(it) }
    }

    fun computeDropTarget(pointerY: Float, snapshot: StripeDragSnapshot<RightToolPanel>): StripeDropTarget {
        val orderedCenters = snapshot.orderedCentersWithoutDragged
        var insertCombinedIndex = orderedCenters.size
        for ((index, center) in orderedCenters.withIndex()) {
            if (pointerY < center) {
                insertCombinedIndex = index
                break
            }
        }
        return if (insertCombinedIndex <= snapshot.topSizeWithoutDragged) {
            StripeDropTarget(inBottom = false, index = insertCombinedIndex)
        } else {
            StripeDropTarget(inBottom = true, index = insertCombinedIndex - snapshot.topSizeWithoutDragged)
        }
    }

    fun applyDragReorder() {
        val panel = draggingPanel ?: return
        val snapshot = dragSnapshot ?: return
        val target = dropTarget ?: return

        val nextTop = snapshot.topPanels.filterNot { it == panel }.toMutableList()
        val nextBottom = snapshot.bottomPanels.filterNot { it == panel }.toMutableList()
        if (target.inBottom) {
            val idx = target.index.coerceIn(0, nextBottom.size)
            nextBottom.add(idx, panel)
        } else {
            val idx = target.index.coerceIn(0, nextTop.size)
            nextTop.add(idx, panel)
        }
        onReorder(nextTop, nextBottom)
    }

    fun stopDragging(apply: Boolean) {
        if (apply) {
            applyDragReorder()
        }
        draggingPanel = null
        dragSnapshot = null
        dropTarget = null
        dragPointerY = 0f
    }

    fun startDragging(panel: RightToolPanel, localTouchY: Float) {
        val topSnapshot = topPanels.toList()
        val bottomSnapshot = bottomPanels.toList()
        val layout = buttonLayouts[panel]
        val fallbackIndex = (topSnapshot + bottomSnapshot).indexOf(panel).coerceAtLeast(0)
        val fallbackTop = fallbackIndex * buttonHeightPx
        val orderedWithoutDragged = (topSnapshot + bottomSnapshot).filterNot { it == panel }
        val centersWithoutDragged = orderedWithoutDragged.mapIndexed { index, candidate ->
            buttonLayouts[candidate]?.centerY ?: ((index + 0.5f) * buttonHeightPx)
        }
        val snapshot = StripeDragSnapshot(
            panel = panel,
            topPanels = topSnapshot,
            bottomPanels = bottomSnapshot,
            topSizeWithoutDragged = topSnapshot.count { it != panel },
            orderedCentersWithoutDragged = centersWithoutDragged,
        )
        draggingPanel = panel
        dragSnapshot = snapshot
        dragAnchorY = localTouchY
        dragPointerY = (layout?.topY ?: fallbackTop) + localTouchY
        dropTarget = computeDropTarget(dragPointerY, snapshot)
    }

    fun updateDragging(panel: RightToolPanel, deltaY: Float) {
        val snapshot = dragSnapshot
        if (draggingPanel != panel || snapshot == null) {
            return
        }
        dragPointerY += deltaY
        val nextDropTarget = computeDropTarget(dragPointerY, snapshot)
        if (nextDropTarget != dropTarget) {
            dropTarget = nextDropTarget
        }
    }

    @Composable
    fun RenderStripeGroup(
        panels: List<RightToolPanel>,
        inBottomGroup: Boolean,
    ) {
        val activeDragPanel = dragSnapshot?.panel
        val shiftStepPx = buttonHeightPx + with(density) { 1.dp.toPx() }
        val target = dropTarget
        val snapshot = dragSnapshot

        fun shiftSlotsFor(panel: RightToolPanel): Int {
            val currentSnapshot = snapshot ?: return 0
            val draggedPanel = currentSnapshot.panel
            if (panel == draggedPanel) {
                return 0
            }
            val sourceGroup = if (inBottomGroup) currentSnapshot.bottomPanels else currentSnapshot.topPanels
            val baseIndex = sourceGroup.indexOf(panel)
            if (baseIndex < 0) {
                return 0
            }
            val withoutDragged = sourceGroup.filterNot { it == draggedPanel }
            val previewGroup = if (target != null && target.inBottom == inBottomGroup) {
                withoutDragged.toMutableList().also { list ->
                    list.add(target.index.coerceIn(0, list.size), draggedPanel)
                }
            } else {
                withoutDragged
            }
            val previewIndex = previewGroup.indexOf(panel)
            if (previewIndex < 0) {
                return 0
            }
            return previewIndex - baseIndex
        }

        for (panel in panels) {
            RightStripeButton(
                panel = panel,
                opened = openPanels.contains(panel),
                active = openPanels.contains(panel) && panel == activePanel,
                hidden = activeDragPanel == panel,
                shiftYPx = shiftSlotsFor(panel) * shiftStepPx,
                darkMode = darkMode,
                palette = palette,
                onClick = { onClick(panel) },
                onPlaced = { absoluteTopY, height ->
                    updateStripeLayout(
                        store = buttonLayouts,
                        key = panel,
                        topY = absoluteTopY - stripeRootY,
                        height = height,
                    )
                },
                onDragStart = { localTouchY -> startDragging(panel, localTouchY) },
                onDragDelta = { deltaY -> updateDragging(panel, deltaY) },
                onDragEnd = { stopDragging(apply = true) },
                onDragCancel = { stopDragging(apply = false) },
            )
        }
    }

    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(palette.stripeBackground, RoundedCornerShape(10.dp))
            .border(1.dp, palette.panelBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 3.dp)
            .onGloballyPositioned { coords ->
                stripeRootY = coords.positionInRoot().y
                stripeHeightPx = coords.size.height.toFloat()
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                RenderStripeGroup(
                    panels = topPanels,
                    inBottomGroup = false,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                RenderStripeGroup(
                    panels = bottomPanels,
                    inBottomGroup = true,
                )
            }
        }

        val snapshot = dragSnapshot
        val target = dropTarget
        if (snapshot != null && target != null) {
            val placeholderRawTop = if (snapshot.orderedCentersWithoutDragged.isEmpty()) {
                dragPointerY - dragAnchorY
            } else {
                computeDropPlaceholderTop(snapshot, target, buttonHeightPx)
            }
            val placeholderTopPx = placeholderRawTop.let { raw ->
                val maxTop = stripeHeightPx - buttonHeightPx
                if (maxTop > 0f) raw.coerceIn(0f, maxTop) else raw
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = 1f
                        translationY = placeholderTopPx
                    }
                    .zIndex(4f),
            ) {
                StripeDropPlaceholder(palette = palette)
            }
        }

        val panel = snapshot?.panel
        if (panel != null) {
            val targetGhostTopPx = (dragPointerY - dragAnchorY)
                .let { raw ->
                    val maxTop = stripeHeightPx - buttonHeightPx
                    if (maxTop > 0f) raw.coerceIn(0f, maxTop) else raw
                }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = 1f
                        translationY = targetGhostTopPx
                    }
                    .size(width = 37.dp, height = 40.dp)
                    .background(
                        color = palette.stripeSelectedBackground.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = palette.stripeSelectedBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .zIndex(5f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconForPanel(panel, darkMode = darkMode, selected = true),
                    panel.title,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RightStripeButton(
    panel: RightToolPanel,
    opened: Boolean,
    active: Boolean,
    hidden: Boolean,
    shiftYPx: Float,
    darkMode: Boolean,
    palette: IslandPalette,
    onClick: () -> Unit,
    onPlaced: (Float, Float) -> Unit,
    onDragStart: (Float) -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    var dragDistance by remember(panel) { mutableFloatStateOf(0f) }
    var suppressClick by remember(panel) { mutableStateOf(false) }
    val animatedShiftY by animateFloatAsState(
        targetValue = if (hidden) 0f else shiftYPx,
        animationSpec = tween(durationMillis = 140),
        label = "rightStripeShift",
    )

    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .background(palette.toolWindowBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, palette.panelBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(panel.title)
            }
        },
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = animatedShiftY }
                .alpha(if (hidden) 0f else 1f)
                .padding(horizontal = 1.dp, vertical = 0.dp)
                .size(width = 37.dp, height = 40.dp)
                .background(
                    when {
                        hidden -> Color.Transparent
                        pressed -> palette.topBarButtonPressed
                        active -> palette.stripeSelectedBackground
                        opened -> palette.stripeOpenedBackground
                        hovered -> palette.stripeHoverBackground
                        else -> Color.Transparent
                    },
                    RoundedCornerShape(8.dp),
                )
                .border(
                    1.dp,
                    when {
                        hidden -> Color.Transparent
                        active -> palette.stripeSelectedBorder
                        opened -> palette.divider
                        else -> Color.Transparent
                    },
                    RoundedCornerShape(8.dp),
                )
                .onGloballyPositioned { coords ->
                    onPlaced(coords.positionInRoot().y, coords.size.height.toFloat())
                }
                .pointerInput(panel) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragDistance = 0f
                            suppressClick = false
                            onDragStart(offset.y)
                        },
                        onDrag = { change, amount ->
                            dragDistance += amount.y
                            if (abs(dragDistance) > 6f) {
                                suppressClick = true
                            }
                            onDragDelta(amount.y)
                            change.consume()
                        },
                        onDragEnd = {
                            if (abs(dragDistance) > 6f) {
                                onDragEnd()
                            } else {
                                onDragCancel()
                            }
                            dragDistance = 0f
                            suppressClick = false
                        },
                        onDragCancel = {
                            dragDistance = 0f
                            onDragCancel()
                            suppressClick = false
                        },
                    )
                }
                .hoverable(interactions)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    onClick = {
                        if (!hidden && !suppressClick) {
                            onClick()
                        }
                        suppressClick = false
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconForPanel(panel, darkMode = darkMode, selected = active),
                panel.title,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class StripeButtonLayout(
    val topY: Float,
    val height: Float,
) {
    val centerY: Float
        get() = topY + height / 2f
}

private data class StripeDragSnapshot<T>(
    val panel: T,
    val topPanels: List<T>,
    val bottomPanels: List<T>,
    val topSizeWithoutDragged: Int,
    val orderedCentersWithoutDragged: List<Float>,
)

private fun <T> updateStripeLayout(
    store: MutableMap<T, StripeButtonLayout>,
    key: T,
    topY: Float,
    height: Float,
) {
    val prev = store[key]
    if (prev != null && abs(prev.topY - topY) < 0.5f && abs(prev.height - height) < 0.5f) {
        return
    }
    store[key] = StripeButtonLayout(topY = topY, height = height)
}

private data class StripeDropTarget(
    val inBottom: Boolean,
    val index: Int,
)

private fun <T> computeDropPlaceholderTop(
    snapshot: StripeDragSnapshot<T>,
    target: StripeDropTarget,
    buttonHeightPx: Float,
): Float {
    val centers = snapshot.orderedCentersWithoutDragged
    if (centers.isEmpty()) {
        return 0f
    }

    val combinedIndex = if (target.inBottom) {
        snapshot.topSizeWithoutDragged + target.index
    } else {
        target.index
    }.coerceIn(0, centers.size)

    val centerY = when {
        combinedIndex == 0 -> centers.first() - buttonHeightPx
        combinedIndex == centers.size -> centers.last() + buttonHeightPx
        else -> (centers[combinedIndex - 1] + centers[combinedIndex]) / 2f
    }
    return centerY - buttonHeightPx / 2f
}

@Composable
private fun StripeDropPlaceholder(
    palette: IslandPalette,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .size(width = 37.dp, height = 40.dp)
            .background(
                color = palette.stripeSelectedBackground.copy(alpha = 0.28f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = palette.stripeSelectedBorder.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
                .width(24.dp)
                .height(2.dp)
                .background(
                    color = palette.stripeSelectedBorder,
                    shape = RoundedCornerShape(50),
                ),
        )
    }
}

private object IdeaLeftStripeIcons {
    val project = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/project.svg",
        dark = "icons/idea/expui/toolwindows/project_dark.svg",
        selected = "icons/idea/expui/toolwindows/project.svg",
    )
    val structure = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/hierarchy.svg",
        dark = "icons/idea/expui/toolwindows/hierarchy_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/hierarchy_selected.svg",
    )
}

private object IdeaStripeIcons {
    val start = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/run.svg",
        dark = "icons/idea/expui/toolwindows/run_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/run_selected.svg",
    )
    val search = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/find.svg",
        dark = "icons/idea/expui/toolwindows/find_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/find_selected.svg",
    )
    val call = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/hierarchy.svg",
        dark = "icons/idea/expui/toolwindows/hierarchy_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/hierarchy_selected.svg",
    )
    val impl = StripeIconVariants(
        light = "icons/idea/expui/gutter/implementingMethod.svg",
        dark = "icons/idea/expui/gutter/implementingMethod_dark.svg",
        selected = "icons/idea/selected/expui/gutter/implementingMethod_selected.svg",
    )
    val web = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/web.svg",
        dark = "icons/idea/expui/toolwindows/web_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/web_selected.svg",
    )
    val note = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/task.svg",
        dark = "icons/idea/expui/toolwindows/task_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/task_selected.svg",
    )
    val sca = StripeIconVariants(
        light = "icons/idea/expui/general/inspections/inspectionsEye.svg",
        dark = "icons/idea/expui/general/inspections/inspectionsEye_dark.svg",
        selected = "icons/idea/selected/expui/general/inspections/inspectionsEye_selected.svg",
    )
    val leak = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/problems.svg",
        dark = "icons/idea/expui/toolwindows/problems_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/problems_selected.svg",
    )
    val gadget = StripeIconVariants(
        light = "icons/idea/expui/nodes/plugin.svg",
        dark = "icons/idea/expui/nodes/plugin_dark.svg",
        selected = "icons/idea/selected/expui/nodes/plugin_selected.svg",
    )
    val advance = StripeIconVariants(
        light = "icons/idea/expui/general/settings.svg",
        dark = "icons/idea/expui/general/settings_dark.svg",
        selected = "icons/idea/selected/expui/general/settings_selected.svg",
    )
    val chains = StripeIconVariants(
        light = "icons/idea/toolwindows/toolWindowAnalyzeDataflow.svg",
        dark = "icons/idea/toolwindows/toolWindowAnalyzeDataflow_dark.svg",
        selected = "icons/idea/selected/toolwindows/toolWindowAnalyzeDataflow_selected.svg",
    )
    val api = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/services.svg",
        dark = "icons/idea/expui/toolwindows/services_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/services_selected.svg",
    )
    val methods = StripeIconVariants(
        light = "icons/idea/expui/nodes/method.svg",
        dark = "icons/idea/expui/nodes/method_dark.svg",
        selected = "icons/idea/selected/expui/nodes/method_selected.svg",
    )
    val log = StripeIconVariants(
        light = "icons/idea/expui/toolwindows/messages.svg",
        dark = "icons/idea/expui/toolwindows/messages_dark.svg",
        selected = "icons/idea/selected/expui/toolwindows/messages_selected.svg",
    )
}

private data class StripeIconVariants(
    val light: String,
    val dark: String,
    val selected: String,
)

private fun iconForTopBar(icon: IconVariants, darkMode: Boolean): IconKey {
    val path = if (darkMode) icon.dark else icon.light
    return IdeaIconRegistry.key(path)
}

private fun iconForLeftPanel(panel: LeftToolPanel, darkMode: Boolean, selected: Boolean): IconKey {
    val variants = when (panel) {
        LeftToolPanel.PROJECT -> IdeaLeftStripeIcons.project
        LeftToolPanel.STRUCTURE -> IdeaLeftStripeIcons.structure
    }
    val path = when {
        selected && darkMode -> variants.selected
        selected -> variants.light
        darkMode -> variants.dark
        else -> variants.light
    }
    return IdeaIconRegistry.key(path)
}

private fun iconForPanel(panel: RightToolPanel, darkMode: Boolean, selected: Boolean): IconKey {
    val variants = when (panel) {
        RightToolPanel.START -> IdeaStripeIcons.start
        RightToolPanel.SEARCH -> IdeaStripeIcons.search
        RightToolPanel.CALL -> IdeaStripeIcons.call
        RightToolPanel.IMPL -> IdeaStripeIcons.impl
        RightToolPanel.WEB -> IdeaStripeIcons.web
        RightToolPanel.NOTE -> IdeaStripeIcons.note
        RightToolPanel.SCA -> IdeaStripeIcons.sca
        RightToolPanel.LEAK -> IdeaStripeIcons.leak
        RightToolPanel.GADGET -> IdeaStripeIcons.gadget
        RightToolPanel.ADVANCE -> IdeaStripeIcons.advance
        RightToolPanel.CHAINS -> IdeaStripeIcons.chains
        RightToolPanel.API -> IdeaStripeIcons.api
        RightToolPanel.METHODS -> IdeaStripeIcons.methods
        RightToolPanel.LOG -> IdeaStripeIcons.log
    }
    val path = when {
        selected && darkMode -> variants.selected
        selected -> variants.light
        darkMode -> variants.dark
        else -> variants.light
    }
    return IdeaIconRegistry.key(path)
}

@Composable
private fun RightToolPanelContent(
    panel: RightToolPanel,
    state: MainUiState,
    buildService: BuildService,
    searchService: SearchService,
    callImplService: CallImplService,
    webService: WebService,
    noteService: NoteService,
    scaService: ScaService,
    leakService: LeakService,
    gadgetService: GadgetService,
    chainsService: ChainsService,
    advanceService: AdvanceService,
    mcpService: McpService,
    editorService: EditorService,
    onStartMcp: () -> Unit,
    onStopMcp: () -> Unit,
) {
    when (panel) {
        RightToolPanel.METHODS -> MethodsPanel(callImplService)
        RightToolPanel.LOG -> LogPanel(state, callImplService)
        RightToolPanel.START -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    MigratedStartTab(buildService)
                }
                Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                RightCurrentPanel(
                    state = state,
                    editorService = editorService,
                    onStartMcp = onStartMcp,
                    onStopMcp = onStopMcp,
                )
            }
        }

        else -> {
            val tab = panel.coreTab
            if (tab == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("unsupported panel")
                }
            } else {
                TabContent(
                    tab = tab,
                    buildService = buildService,
                    searchService = searchService,
                    callImplService = callImplService,
                    webService = webService,
                    noteService = noteService,
                    scaService = scaService,
                    leakService = leakService,
                    gadgetService = gadgetService,
                    chainsService = chainsService,
                    advanceService = advanceService,
                    mcpService = mcpService,
                )
            }
        }
    }
}

@Composable
private fun PanelSurface(
    modifier: Modifier,
    borderColor: Color,
    backgroundColor: Color,
    corner: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(0.dp),
        content = content,
    )
}

@Composable
private fun VerticalDragDivider(
    modifier: Modifier,
    color: Color,
    hoverColor: Color,
    onDrag: (Float) -> Unit,
    onDoubleClick: (() -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val dividerColor = if (dragging || hovered) hoverColor else color

    Box(
        modifier = modifier
            .hoverable(interactions)
            .pointerInput(onDoubleClick) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleClick?.invoke()
                    },
                )
            }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                    },
                    onDragEnd = {
                        dragging = false
                    },
                    onDragCancel = {
                        dragging = false
                    },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor),
        )
    }
}

@Composable
private fun HorizontalDragDivider(
    modifier: Modifier,
    color: Color,
    hoverColor: Color,
    onDrag: (Float) -> Unit,
    onDoubleClick: (() -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val dividerColor = if (dragging || hovered) hoverColor else color

    Box(
        modifier = modifier
            .hoverable(interactions)
            .pointerInput(onDoubleClick) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleClick?.invoke()
                    },
                )
            }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                    },
                    onDragEnd = {
                        dragging = false
                    },
                    onDragCancel = {
                        dragging = false
                    },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(1.dp)
                .fillMaxWidth()
                .background(dividerColor),
        )
    }
}

@Composable
private fun PanelTitle(
    title: String,
    palette: IslandPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = palette.titleText)
    }
}

@Composable
private fun RightCurrentPanel(
    state: MainUiState,
    editorService: EditorService,
    onStartMcp: () -> Unit,
    onStopMcp: () -> Unit,
) {
    var editor by remember { mutableStateOf(editorService.snapshot()) }
    var opStatus by remember { mutableStateOf("ready") }
    LaunchedEffect(Unit) {
        while (true) {
            editor = editorService.snapshot()
            delay(700)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Current")
        Text("Jar: ${editor.jarName.ifBlank { "-" }}")
        Text("Class: ${editor.className.ifBlank { "-" }}")
        Text(
            "Method: ${
                if (editor.methodName.isBlank()) "-" else "${editor.methodName}${editor.methodDesc}"
            }",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = {
                val ok = editorService.goPrev()
                opStatus = if (ok) "moved prev" else "no previous"
            }, modifier = Modifier.size(width = 84.dp, height = 28.dp)) {
                Text("Prev")
            }
            IdeaOutlinedButton(onClick = {
                val ok = editorService.goNext()
                opStatus = if (ok) "moved next" else "no next"
            }, modifier = Modifier.size(width = 84.dp, height = 28.dp)) {
                Text("Next")
            }
            IdeaOutlinedButton(onClick = {
                RuntimeFacades.build().clearCache()
                opStatus = "clean requested"
            }, modifier = Modifier.size(width = 84.dp, height = 28.dp)) {
                Text("Clean")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = {
                RuntimeFacades.tooling().openAllStringsTool()
                opStatus = "all strings opened"
            }, modifier = Modifier.size(width = 98.dp, height = 28.dp)) {
                Text("All Strings")
            }
            IdeaOutlinedButton(onClick = {
                RuntimeFacades.tooling().openElSearchTool()
                opStatus = "el search opened"
            }, modifier = Modifier.size(width = 88.dp, height = 28.dp)) {
                Text("EL Search")
            }
            IdeaDefaultButton(onClick = {
                val ok = editorService.addCurrentToFavorites()
                opStatus = if (ok) "added to favorites" else "no current method"
            }, modifier = Modifier.size(width = 84.dp, height = 28.dp)) {
                Text("Add Fav")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaDefaultButton(onClick = onStartMcp, modifier = Modifier.size(width = 96.dp, height = 28.dp)) {
                Text("Start MCP")
            }
            IdeaOutlinedButton(onClick = onStopMcp, modifier = Modifier.size(width = 96.dp, height = 28.dp)) {
                Text("Stop MCP")
            }
        }
        Text("MCP Running: ${state.mcpStatus.runningCount()}/7")
        Text(opStatus)
    }
}

@Composable
private fun ProjectStructurePane(
    editorService: EditorService,
    callImplService: CallImplService,
) {
    var editorSnapshot by remember { mutableStateOf(editorService.snapshot()) }
    var callSnapshot by remember { mutableStateOf(callImplService.snapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            editorSnapshot = editorService.snapshot()
            callSnapshot = callImplService.snapshot()
            delay(900)
        }
    }

    val currentClass = editorSnapshot.className
    val entries = remember(callSnapshot.allMethods, currentClass) {
        val targetClass = currentClass.replace('/', '.')
        callSnapshot.allMethods.mapIndexedNotNull { index, item ->
            val methodClass = item.className.replace('/', '.')
            if (targetClass.isNotBlank() && methodClass == targetClass) index to item else null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Class")
        Text(if (currentClass.isBlank()) "-" else currentClass)
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Text("Methods ${entries.size}")
        if (entries.isEmpty()) {
            Text("Open a class/method first to show structure.")
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(entries, key = { _, item -> item.first }) { index, (methodIndex, item) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ideaInteractiveClickable { callImplService.openAllMethod(methodIndex) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}.")
                    Text(
                        "${item.methodName}${item.methodDesc}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MethodsPanel(callImplService: CallImplService) {
    var snapshot by remember { mutableStateOf(callImplService.snapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = callImplService.snapshot()
            delay(900)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("All Methods")
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(snapshot.allMethods) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ideaInteractiveClickable { callImplService.openAllMethod(index) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}.")
                    Text(
                        "${item.className}#${item.methodName}${item.methodDesc}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LogPanel(
    state: MainUiState,
    callImplService: CallImplService,
) {
    var snapshot by remember { mutableStateOf(callImplService.snapshot()) }
    var logTail by remember { mutableStateOf(readRuntimeLogTail(null)) }
    var logStatus by remember { mutableStateOf("ready") }
    var clearOffset by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = callImplService.snapshot()
            val latestLog = readRuntimeLogTail(clearOffset)
            val cleared = clearOffset
            if (cleared != null && latestLog.fileSize < cleared) {
                clearOffset = 0L
            }
            logTail = latestLog
            logStatus = latestLog.status
            delay(900)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Log")
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IdeaOutlinedButton(onClick = {
                val file = runtimeLogFileForToday()
                val size = if (Files.exists(file)) safeFileSize(file) else 0L
                clearOffset = size
                logTail = RuntimeLogTail(
                    path = file.toAbsolutePath().toString(),
                    lines = emptyList(),
                    fileSize = size,
                    status = "log view cleared",
                )
                logStatus = "log view cleared"
            }) {
                Text("Clear View")
            }
            IdeaOutlinedButton(onClick = {
                logStatus = openRuntimeLogsDir()
            }) {
                Text("Open Logs Dir")
            }
        }
        Text("Engine: ${state.engineStatus}")
        Text("MCP Running: ${state.mcpStatus.runningCount()}/7")
        Text("Current Tab: ${state.selectedTab.title}")
        Text("Status: ${state.statusText}")
        if (snapshot.currentJar.isNotBlank()) {
            Text("Jar: ${snapshot.currentJar}")
        }
        if (snapshot.currentClass.isNotBlank()) {
            Text("Class: ${snapshot.currentClass}")
        }
        if (snapshot.currentMethod.isNotBlank()) {
            Text("Method: ${snapshot.currentMethod}")
        }
        Text("Source: ${logTail.path}")
        Text(logStatus)
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(logTail.lines) { index, line ->
                Text(line)
                if (index != logTail.lines.lastIndex) {
                    Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private data class RuntimeLogTail(
    val path: String,
    val lines: List<String>,
    val fileSize: Long,
    val status: String,
)

private fun runtimeLogFileForToday(): Path {
    return Paths.get("logs").resolve("${LocalDate.now()}.log").toAbsolutePath()
}

private fun readRuntimeLogTail(
    clearOffset: Long?,
    maxTailBytes: Int = 256 * 1024,
    maxLines: Int = 320,
): RuntimeLogTail {
    val logsDir = Paths.get("logs").toAbsolutePath()
    if (!Files.isDirectory(logsDir)) {
        return RuntimeLogTail(logsDir.toString(), emptyList(), 0L, "logs dir not found")
    }

    val logFile = runtimeLogFileForToday()
    if (!Files.exists(logFile)) {
        return RuntimeLogTail(logFile.toString(), emptyList(), 0L, "today log not created")
    }

    val fileSize = safeFileSize(logFile)
    if (fileSize <= 0L) {
        return RuntimeLogTail(logFile.toString(), emptyList(), fileSize, "log file empty")
    }

    val start = when {
        clearOffset != null -> clearOffset.coerceIn(0L, fileSize)
        else -> max(0L, fileSize - maxTailBytes.toLong())
    }
    val readLenLong = fileSize - start
    if (readLenLong <= 0L) {
        return RuntimeLogTail(logFile.toString(), emptyList(), fileSize, "waiting new logs")
    }

    val readLen = min(Int.MAX_VALUE.toLong(), readLenLong).toInt()
    val bytes = ByteArray(readLen)
    return try {
        Files.newByteChannel(logFile, StandardOpenOption.READ).use { channel ->
            channel.position(start)
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) <= 0) {
                    break
                }
            }
        }
        var text = String(bytes, StandardCharsets.UTF_8)
        if (clearOffset == null && start > 0L) {
            val firstLf = text.indexOf('\n')
            if (firstLf >= 0 && firstLf < text.length - 1) {
                text = text.substring(firstLf + 1)
            }
        }
        val allLines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val lines = if (allLines.size > maxLines) allLines.takeLast(maxLines) else allLines
        val status = if (lines.isEmpty()) {
            "waiting new logs"
        } else if (clearOffset != null) {
            "showing ${lines.size} lines after clear"
        } else {
            "showing tail ${lines.size} lines"
        }
        RuntimeLogTail(logFile.toString(), lines, fileSize, status)
    } catch (ex: Throwable) {
        RuntimeLogTail(
            logFile.toString(),
            emptyList(),
            fileSize,
            "read log failed: ${ex.message ?: ex::class.java.simpleName}",
        )
    }
}

private fun safeFileSize(file: Path): Long {
    return try {
        Files.size(file)
    } catch (_: Throwable) {
        0L
    }
}

private fun openRuntimeLogsDir(): String {
    val dir = Paths.get("logs").toAbsolutePath()
    return try {
        Files.createDirectories(dir)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(dir.toFile())
            "opened logs dir: $dir"
        } else {
            "logs dir: $dir"
        }
    } catch (ex: Throwable) {
        "open logs dir failed: ${ex.message ?: ex::class.java.simpleName}"
    }
}

@Composable
private fun TabContent(
    tab: CoreTab,
    buildService: BuildService,
    searchService: SearchService,
    callImplService: CallImplService,
    webService: WebService,
    noteService: NoteService,
    scaService: ScaService,
    leakService: LeakService,
    gadgetService: GadgetService,
    chainsService: ChainsService,
    advanceService: AdvanceService,
    mcpService: McpService,
) {
    when (tab) {
        CoreTab.START -> MigratedStartTab(buildService)
        CoreTab.SEARCH -> MigratedSearchTab(searchService)
        CoreTab.CALL -> MigratedCallTab(callImplService)
        CoreTab.IMPL -> MigratedImplTab(callImplService)
        CoreTab.WEB -> MigratedWebTab(webService)
        CoreTab.NOTE -> MigratedNoteTab(noteService)
        CoreTab.SCA -> MigratedScaTab(scaService)
        CoreTab.LEAK -> MigratedLeakTab(leakService)
        CoreTab.GADGET -> MigratedGadgetTab(gadgetService)
        CoreTab.ADVANCE -> MigratedAdvanceTab(advanceService)
        CoreTab.CHAINS -> MigratedChainsTab(chainsService)
        CoreTab.API -> MigratedApiTab(mcpService)
    }
}
