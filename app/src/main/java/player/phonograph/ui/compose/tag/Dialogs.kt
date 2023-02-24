/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
        onSave()
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

@Composable
internal fun ExitWithoutSavingDialog(
    dialogState: MaterialDialogState,
    onExit: () -> Unit
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
        title(res = R.string.exit_without_saving)
    }
}

@Composable
internal fun CoverImageDetailDialog(
    state: MaterialDialogState,
    artworkExist: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    editMode: Boolean
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok) { state.hide() }
    }
) {
    title(res = R.string.label_details)
    Column(
        modifier = Modifier
            .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
            .wrapContentWidth()
    ) {
        if (artworkExist) {
            MenuItem(textRes = R.string.save, onSave)
            if (editMode) {
                MenuItem(textRes = R.string.remove_cover, onDelete)
            }
        }
        if (editMode) {
            MenuItem(textRes = R.string.update_image, onUpdate)
        }
    }
}

@Composable
private fun MenuItem(@StringRes textRes: Int, onClick: () -> Unit) =
    Text(
        text = stringResource(textRes),
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.button,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(32.dp)
            .clickable(onClick = onClick),
    )