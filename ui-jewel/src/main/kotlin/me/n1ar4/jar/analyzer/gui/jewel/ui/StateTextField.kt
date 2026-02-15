package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun BoundTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
) {
    val state = rememberTextFieldState(value)
    SyncTextFieldState(
        state = state,
        value = value,
        onValueChange = onValueChange,
    )
    TextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
    )
}

@Composable
private fun SyncTextFieldState(
    state: TextFieldState,
    value: String,
    onValueChange: (String) -> Unit,
) {
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text }
            .map { it.toString() }
            .drop(1)
            .collectLatest { onValueChange(it) }
    }
}
