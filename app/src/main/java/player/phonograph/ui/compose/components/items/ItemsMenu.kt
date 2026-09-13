/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.resource.text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ItemMenu(
    expanded: Boolean,
    entries: List<ActionEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var groups by remember(expanded, entries) { mutableStateOf<List<ActionEntry.GroupedEntries>>(emptyList()) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val group = groups.lastOrNull()
        if (group != null) {
            DropdownMenuItem(onClick = { groups = groups.dropLast(1) }, modifier = modifier) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Text(group.semantic.interpreter().text(), modifier = textModifier)
            }
        }
        for (entry in group?.entries ?: entries) {
            DropdownMenuItem(
                onClick = {
                    when (entry) {
                        is ActionEntry.GroupedEntries -> groups = groups + entry
                        is ActionEntry.ToolbarEntry -> {
                            onDismiss()
                            scope.launch { entry.action() }
                        }
                    }
                },
                modifier = modifier,
            ) {
                Text(
                    entry.semantic.interpreter().text(),
                    modifier = textModifier,
                )
            }
        }
    }
}
