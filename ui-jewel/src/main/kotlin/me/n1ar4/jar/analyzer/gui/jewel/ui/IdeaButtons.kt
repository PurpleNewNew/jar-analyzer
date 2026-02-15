package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import org.jetbrains.jewel.foundation.Stroke
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton as JewelDefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton as JewelOutlinedButton
import org.jetbrains.jewel.ui.component.styling.ButtonColors
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.theme.defaultButtonStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle

@Composable
fun IdeaDefaultButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val style = rememberIdeaPrimaryButtonStyle()
    JewelDefaultButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        content = content,
    )
}

@Composable
fun IdeaOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val style = rememberIdeaSecondaryButtonStyle()
    JewelOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        content = content,
    )
}

@Composable
private fun rememberIdeaPrimaryButtonStyle(): ButtonStyle {
    val base = JewelTheme.defaultButtonStyle
    val isDark = JewelTheme.isDark
    return remember(base, isDark) {
        val colors = if (isDark) {
            createColors(
                bg = Color(0xFF3871E1),
                bgHover = Color(0xFF538AF9),
                bgPressed = Color(0xFF2F5EB9),
                bgDisabled = Color(0xFF40434A),
                content = Color(0xFFFFFFFF),
                contentDisabled = Color(0xFF4C4F56),
                border = Color(0xFF3871E1),
                borderHover = Color(0xFF538AF9),
                borderPressed = Color(0xFF2F5EB9),
                borderDisabled = Color(0xFF40434A),
            )
        } else {
            createColors(
                bg = Color(0xFF3871E1),
                bgHover = Color(0xFF538AF9),
                bgPressed = Color(0xFF2F5EB9),
                bgDisabled = Color(0xFFDDDFE4),
                content = Color(0xFFFFFFFF),
                contentDisabled = Color(0xFF9FA2A8),
                border = Color(0xFF3871E1),
                borderHover = Color(0xFF538AF9),
                borderPressed = Color(0xFF2F5EB9),
                borderDisabled = Color(0xFFDDDFE4),
            )
        }
        ButtonStyle(
            colors = colors,
            metrics = base.metrics,
            focusOutlineAlignment = Stroke.Alignment.Inside,
        )
    }
}

@Composable
private fun rememberIdeaSecondaryButtonStyle(): ButtonStyle {
    val base = JewelTheme.outlinedButtonStyle
    val isDark = JewelTheme.isDark
    return remember(base, isDark) {
        val colors = if (isDark) {
            createColors(
                bg = Color(0x00000000),
                bgHover = Color(0x16FFFFFF),
                bgPressed = Color(0x26FFFFFF),
                bgDisabled = Color(0x00000000),
                content = Color(0xFFD1D3D9),
                contentDisabled = Color(0xFF4C4F56),
                border = Color(0xFF40434A),
                borderHover = Color(0xFF5F6269),
                borderPressed = Color(0xFF73767C),
                borderDisabled = Color(0xFF33353B),
            )
        } else {
            createColors(
                bg = Color(0x00000000),
                bgHover = Color(0x12000000),
                bgPressed = Color(0x20000000),
                bgDisabled = Color(0x00000000),
                content = Color(0xFF40434A),
                contentDisabled = Color(0xFF9FA2A8),
                border = Color(0xFFD1D3D9),
                borderHover = Color(0xFFB5B7BD),
                borderPressed = Color(0xFF9FA2A8),
                borderDisabled = Color(0xFFDDDFE4),
            )
        }
        ButtonStyle(
            colors = colors,
            metrics = base.metrics,
            focusOutlineAlignment = Stroke.Alignment.Inside,
        )
    }
}

private fun createColors(
    bg: Color,
    bgHover: Color,
    bgPressed: Color,
    bgDisabled: Color,
    content: Color,
    contentDisabled: Color,
    border: Color,
    borderHover: Color,
    borderPressed: Color,
    borderDisabled: Color,
): ButtonColors {
    return ButtonColors(
        background = SolidColor(bg),
        backgroundDisabled = SolidColor(bgDisabled),
        backgroundFocused = SolidColor(bgHover),
        backgroundPressed = SolidColor(bgPressed),
        backgroundHovered = SolidColor(bgHover),
        content = content,
        contentDisabled = contentDisabled,
        contentFocused = content,
        contentPressed = content,
        contentHovered = content,
        border = SolidColor(border),
        borderDisabled = SolidColor(borderDisabled),
        borderFocused = SolidColor(borderHover),
        borderPressed = SolidColor(borderPressed),
        borderHovered = SolidColor(borderHover),
    )
}
