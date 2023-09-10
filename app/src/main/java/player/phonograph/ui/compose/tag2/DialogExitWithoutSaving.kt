/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun ExitWithoutSavingDialog(
    dialogState: MaterialDialogState,
    onExit: () -> Unit,
) {
    val dismiss = { dialogState.hide() }
    MaterialDialog(
        dialogState = dialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            positiveButton(res = android.R.string.cancel, onClick = dismiss)
            button(res = android.R.string.ok) {
                dismiss()
                onExit()
            }
        }
    ) {
        title(res = player.phonograph.R.string.exit_without_saving)
    }
}