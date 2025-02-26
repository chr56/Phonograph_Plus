/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun SaveConfirmationDialog(
    dialogState: MaterialDialogState,
    changes: () -> MetadataChanges,
    onSave: () -> Unit,
) {
    val dismiss = { dialogState.hide() }
    val save = {
        dismiss()
        onSave()
    }
    MaterialDialog(
        dialogState = dialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            button(res = R.string.save, onClick = save, textStyle = accentColoredButtonStyle())
            button(res = android.R.string.cancel, onClick = dismiss, textStyle = accentColoredButtonStyle())
        }
    ) {
        title(res = R.string.save)
        customView {
            MetadataDifferenceScreen(diff = changes())
        }
    }
}