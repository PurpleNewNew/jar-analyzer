package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme

internal data class IdeaInteractionPalette(
    val hoverOverlay: Color,
    val pressedOverlay: Color,
    val selectedBackground: Color,
    val selectedHoverBackground: Color,
    val selectedPressedBackground: Color,
    val selectedBorder: Color,
)

@Composable
internal fun rememberIdeaInteractionPalette(): IdeaInteractionPalette {
    val isDark = JewelTheme.isDark
    return remember(isDark) {
        if (isDark) {
            IdeaInteractionPalette(
                hoverOverlay = Color(0x16FFFFFF),
                pressedOverlay = Color(0x26FFFFFF),
                selectedBackground = Color(0xFF2A4371),
                selectedHoverBackground = Color(0xFF2E4D89),
                selectedPressedBackground = Color(0xFF2F5EB9),
                selectedBorder = Color(0x8C3871E1),
            )
        } else {
            IdeaInteractionPalette(
                hoverOverlay = Color(0x12000000),
                pressedOverlay = Color(0x20000000),
                selectedBackground = Color(0xFFD0DFFE),
                selectedHoverBackground = Color(0xFFBDD3FF),
                selectedPressedBackground = Color(0xFFA7C5FF),
                selectedBorder = Color(0x663871E1),
            )
        }
    }
}

internal fun Modifier.ideaInteractiveClickable(
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(6.dp),
    onClick: () -> Unit,
): Modifier = composed {
    val palette = rememberIdeaInteractionPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val latestOnClick by rememberUpdatedState(onClick)

    val backgroundColor = when {
        selected && pressed -> palette.selectedPressedBackground
        selected && hovered -> palette.selectedHoverBackground
        selected -> palette.selectedBackground
        pressed -> palette.pressedOverlay
        hovered -> palette.hoverOverlay
        else -> Color.Transparent
    }

    this
        .background(backgroundColor, shape)
        .border(
            width = 1.dp,
            color = if (selected) palette.selectedBorder else Color.Transparent,
            shape = shape,
        )
        .hoverable(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = latestOnClick,
        )
}
