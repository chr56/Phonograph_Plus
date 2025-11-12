/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import player.phonograph.util.concurrent.coroutineToast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import kotlin.math.min

@Composable
fun PathEditor(
    modifier: Modifier,
    title: String,
    textDescription: String,
    paths: List<String>,
    onDismissRequest: () -> Unit,
    actionAdd: () -> Unit,
    actionRefresh: () -> Unit,
    actionClear: () -> Unit,
    actionRemove: (target: String) -> Unit,
    actionEdit: (oldPath: String, newPath: String) -> Unit,
) {
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    AdvancedDialogFrame(
        modifier = modifier,
        title = title,
        onDismissRequest = onDismissRequest,
        actions = listOf(
            ActionItem(
                imageVector = Icons.Default.Refresh,
                textRes = R.string.action_refresh,
                onClick = actionRefresh,
            ),
            ActionItem(
                imageVector = Icons.Default.Delete,
                textRes = R.string.action_clear,
                onClick = { showClearConfirmationDialog = true },
            ),
        )
    ) {
        Text(
            textDescription,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, start = 12.dp, end = 12.dp),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(8f)
                .padding(horizontal = 8.dp)
        ) {
            val estimatedHeight = remember { (96 + min(384, 48 * paths.size)).dp }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = estimatedHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (paths.isNotEmpty()) {
                    items(paths) { path ->
                        PathItem(
                            path = path,
                            actionRemove = actionRemove,
                            actionEdit = actionEdit
                        )
                    }
                    item {
                        Spacer(Modifier.height(72.dp))
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.msg_empty),
                            modifier = Modifier.padding(36.dp),
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = actionAdd,
                backgroundColor = MaterialTheme.colors.secondary,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add),
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        }
        if (showClearConfirmationDialog) {
            ConfirmationDialog(
                stringResource(R.string.action_clear),
                stringResource(R.string.tips_are_you_sure),
                { actionClear() },
                { showClearConfirmationDialog = false }
            )
        }
    }
}


@Composable
private fun PathItem(
    path: String,
    actionRemove: (String) -> Unit,
    actionEdit: (oldPath: String, newPath: String) -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
        var isEditing by remember { mutableStateOf(false) }

        if (isEditing) {

            var editedPath by remember { mutableStateOf(path) }
            OutlinedTextField(
                value = editedPath,
                onValueChange = { editedPath = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                maxLines = 3,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colors.secondary,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            )

            IconButton(onClick = {
                actionEdit(path, editedPath)
                isEditing = false
            }) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = stringResource(R.string.action_save),
                    tint = MaterialTheme.colors.secondary
                )
            }

            IconButton(onClick = {
                editedPath = path
                isEditing = false
            }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = MaterialTheme.colors.secondary
                )
            }
        } else {

            val clipboard = LocalClipboard.current
            var textToCopy by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(textToCopy) {
                if (textToCopy != null) {
                    clipboard.setClipEntry(ClipData.newPlainText("PATH", textToCopy).toClipEntry())
                    coroutineToast(context, R.string.action_copy_to_clipboard)
                }
            }

            Text(
                path,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clickable { textToCopy = path },
                color = MaterialTheme.colors.onSurface,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.StartEllipsis
            )

            IconButton(onClick = { isEditing = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colors.secondary
                )
            }

            IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colors.secondary
                )
            }

            if (showDeleteConfirmationDialog) {
                ConfirmationDialog(
                    stringResource(R.string.action_delete),
                    "$path\n${stringResource(R.string.tips_are_you_sure)}",
                    { actionRemove(path) },
                    { showDeleteConfirmationDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmButtonText: String = stringResource(android.R.string.ok),
    dismissButtonText: String = stringResource(android.R.string.cancel),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, style = MaterialTheme.typography.h6) },
        text = { Text(text, style = MaterialTheme.typography.body1) },
        confirmButton = {
            TextButton(onClick = {
                onConfirmation()
                onDismissRequest()
            }) {
                Text(
                    confirmButtonText, color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.button
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    dismissButtonText, color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.button
                )
            }
        }
    )
}