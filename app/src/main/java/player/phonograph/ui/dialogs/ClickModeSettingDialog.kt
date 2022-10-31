/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.ClickModeSettingDialog
import androidx.compose.runtime.Composable

class ClickModeSettingDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        ClickModeSettingDialog(requireContext(), ::dismiss)
    }
}