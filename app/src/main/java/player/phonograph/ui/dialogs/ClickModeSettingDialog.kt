/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.ClickModeSettingDialogContent
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

class ClickModeSettingDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            PhonographTheme {
                MaterialDialog(
                    dialogState = dialogState,
                    elevation = 0.dp,
                    onCloseRequest = { dismiss() },
                    buttons = {
                        button(
                            res = android.R.string.ok,
                            textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                        ) {
                            dismiss()
                        }
                    }
                ) {
                    title(res = R.string.pref_title_click_behavior)
                    ClickModeSettingDialogContent(requireContext(), ::dismiss)
                }
            }
        }
    }
}