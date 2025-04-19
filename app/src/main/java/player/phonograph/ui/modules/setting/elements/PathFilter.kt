/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.elements

import player.phonograph.R
import player.phonograph.ui.compose.components.TempPopupContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun ColumnScope.PathFilterSettings(
    mode: Boolean,
    switchMode: () -> Unit,
    paths: List<String>,
    actionAdd: () -> Unit,
    actionRefresh: () -> Unit,
    actionClear: () -> Unit,
    actionRemove: (String) -> Unit,
) {
    val textMode: String = stringResource(if (mode) R.string.excluded_paths else R.string.included_paths)
    val textDescription = stringResource(
        if (mode) R.string.pref_summary_path_filter_excluded_mode
        else R.string.pref_summary_path_filter_included_mode
    )
    Row(
        Modifier
            .clickable { switchMode() }
            .padding(vertical = 16.dp)
    ) {
        Column(Modifier.weight(4f)) {
            Text(textMode)
            Text(
                textDescription,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Switch(
            mode,
            null,
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
    Row {
        ActionButton(
            contentDescription = R.string.add_action,
            icon = Icons.Default.Add,
            modifier = Modifier.weight(2f),
            onClick = actionAdd
        )
        ActionButton(
            contentDescription = R.string.refresh,
            icon = Icons.Default.Refresh,
            modifier = Modifier.weight(2f),
            onClick = actionRefresh
        )
        Spacer(modifier = Modifier.weight(3f))
        ActionButton(
            contentDescription = R.string.clear_action,
            icon = Icons.Default.Delete,
            modifier = Modifier.weight(2f),
            confirmationText = { "${stringResource(R.string.clear_action)}\n$textMode" },
            onClick = actionClear
        )
    }
    for (path in paths) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                path,
                Modifier
                    .weight(4f)
                    .align(Alignment.CenterVertically),
                color = Color.Gray,
                fontSize = 12.sp
            )
            ActionButton(
                contentDescription = R.string.delete_action,
                icon = Icons.Default.Close,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                confirmationText = {
                    "${stringResource(R.string.delete_action)}($textMode)\n$path"
                },
                onClick = { actionRemove(path) },
            )
        }
    }
}


/**
 * @param confirmationText tips for confirmation, null if disable
 */
@Composable
private fun ActionButton(
    contentDescription: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    confirmationText: (@Composable () -> String)? = null,
    onClick: () -> Unit,
) {
    var showPopup: Boolean by remember { mutableStateOf(false) }
    val dismissPopup = { showPopup = false }
    TextButton(
        onClick = {
            if (confirmationText != null) showPopup = !showPopup else onClick()
        },
        modifier
    ) {
        Icon(icon, stringResource(contentDescription), tint = MaterialTheme.colors.secondary)
    }
    if (showPopup) Popup(onDismissRequest = dismissPopup) {
        TempPopupContent(dismissPopup = dismissPopup, onClick = onClick) {
            Text(
                text = confirmationText?.invoke() ?: stringResource(contentDescription),
                style = MaterialTheme.typography.button,
            )
        }
    }
}


