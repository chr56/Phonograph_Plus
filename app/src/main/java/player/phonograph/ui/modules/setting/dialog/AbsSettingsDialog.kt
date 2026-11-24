/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.dialogs.LimitedDialog
import player.phonograph.ui.modules.setting.components.SettingsDialogFrame
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

abstract class AbsSettingsDialog : ComposeViewDialogFragment() {

    @Composable
    protected fun SettingsDialog(
        modifier: Modifier,
        title: String,
        actions: List<ActionEntry>,
        scrollable: Boolean = false,
        innerShadow: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                SettingsDialogFrame(
                    modifier = modifier,
                    title = title,
                    onDismissRequest = ::dismiss,
                    actions = actions,
                    scrollable = scrollable,
                    innerShadow = innerShadow,
                    content = content,
                )
            }
        }
    }
}