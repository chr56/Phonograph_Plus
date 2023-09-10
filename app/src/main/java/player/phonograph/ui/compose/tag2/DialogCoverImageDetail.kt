/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
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
internal fun CoverImageDetailDialog(
    state: MaterialDialogState,
    artworkExist: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    editMode: Boolean,
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok) { state.hide() }
    }
) {
    title(res = player.phonograph.R.string.label_details)
    Column(
        modifier = Modifier
            .padding(horizontal = 48.dp)
            .wrapContentWidth()
    ) {
        if (artworkExist) {
            MenuItem(textRes = player.phonograph.R.string.save, onSave)
            if (editMode) {
                MenuItem(textRes = player.phonograph.R.string.remove_cover, onDelete)
            }
        }
        if (editMode) {
            MenuItem(textRes = player.phonograph.R.string.update_image, onUpdate)
        }
    }
}


@Composable
private fun MenuItem(@StringRes textRes: Int, onClick: () -> Unit) =
    Text(
        text = stringResource(textRes),
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(56.dp)
            .clickable(onClick = onClick),
    )