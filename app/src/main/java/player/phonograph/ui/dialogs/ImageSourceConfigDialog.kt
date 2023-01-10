/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.ImageSourceConfigDialog
import androidx.compose.runtime.Composable

class ImageSourceConfigDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        ImageSourceConfigDialog(context = requireContext())
    }
}