/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.ImageSourceConfigDialogContent
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

class ImageSourceConfigDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            PhonographTheme {
                MaterialDialog(
                    dialogState = dialogState,
                    elevation = 0.dp,
                    onCloseRequest = { dismiss() }
                ) {
                    title(res = R.string.image_source_config)
                    customView {
                        ImageSourceConfigDialogContent(requireContext(), ::dismiss)
                    }
                }
            }
        }
    }
}