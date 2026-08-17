/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.resource.interpreter
import player.phonograph.ui.resource.text
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        for (entry in entries) {
            DropdownMenuItem(
                onClick = {
                    onDismiss()
                    scope.launch { entry.action() }
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
