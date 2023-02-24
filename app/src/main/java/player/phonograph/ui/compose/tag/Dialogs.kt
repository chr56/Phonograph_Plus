/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
internal fun SaveConfirmationDialog(
    dialogState: MaterialDialogState,
    diffScreen: @Composable () -> Unit,
    onSave: () -> Unit
) {
    val dismiss = { dialogState.hide() }
    val save = {
        dismiss()
    }
    MaterialDialog(
        dialogState = dialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            button(res = R.string.save, onClick = save)
            button(res = android.R.string.cancel, onClick = dismiss)
        }
    ) {
        title(res = R.string.save)
        customView {
            diffScreen()
        }
    }
}