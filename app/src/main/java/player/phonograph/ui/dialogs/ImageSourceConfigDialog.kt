/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.ImageSourceConfigDialog
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.runtime.Composable

class ImageSourceConfigDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        PhonographTheme {
            ImageSourceConfigDialog(context = requireContext(), ::dismiss)
        }
    }
}