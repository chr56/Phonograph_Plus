/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import player.phonograph.ui.compose.dialogHorizontalPadding
import player.phonograph.ui.compose.dialogMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun LimitedDialog(
    elevation: Dp = 24.dp,
    backgroundColor: Color = MaterialTheme.colors.surface,
    properties: DialogProperties = DialogProperties(),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val containerSize = LocalWindowInfo.current
    val maxHeight = dialogMaxHeight(containerSize)
    val horizontalPadding = dialogHorizontalPadding(containerSize)
    Dialog(
        properties = properties,
        onDismissRequest = { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxHeight = maxHeight)
                .padding(horizontal = horizontalPadding)
                .clipToBounds()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor,
            elevation = elevation
        ) {
            content()
        }
    }
}


/**
 * Basic Frame for some advanced or complex dialogs that feel likes a subpage
 */
@Composable
fun AdvancedDialogFrame(
    modifier: Modifier,
    title: String,
    onDismissRequest: () -> Unit,
    actions: List<ActionItem>,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        TopAppBar(
            title = { Text(title) },
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = {
                ActionIconButton(
                    Icons.AutoMirrored.Default.ArrowBack,
                    tint = MaterialTheme.colors.onPrimary,
                    text = stringResource(R.string.action_exit),
                    onClick = onDismissRequest
                )
            },
            actions = {
                for (item in actions) {
                    ActionIconButton(item)
                }
            }
        )
        content()
    }
}
