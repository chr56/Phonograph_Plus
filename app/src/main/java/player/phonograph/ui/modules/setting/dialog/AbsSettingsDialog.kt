/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.LimitedDialog
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

abstract class AbsSettingsDialog : ComposeViewDialogFragment() {

    @Composable
    protected fun SettingsDialog(
        modifier: Modifier,
        title: String,
        actions: List<ActionItem>,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                AdvancedDialogFrame(
                    modifier = modifier,
                    title = stringResource(R.string.action_settings),
                    onDismissRequest = ::dismiss,
                    actions = actions,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 24.dp, end = 16.dp)
                    )
                    content()
                }
            }
        }
    }

}