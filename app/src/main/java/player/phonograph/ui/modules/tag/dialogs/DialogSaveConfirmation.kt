/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.modules.tag.MetadataChanges
import player.phonograph.ui.modules.tag.components.MetadataDifferenceItem
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SaveConfirmationDialog(
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
            MetadataDifferenceScreen(changes = changes())
        }
    }
}


@Composable
private fun MetadataDifferenceScreen(changes: MetadataChanges) {
    if (changes.changes.isEmpty()) {
        Text(text = stringResource(id = R.string.no_changes))
    } else {
        LazyColumn(Modifier.padding(8.dp)) {
            for (change in changes.changes) {
                item {
                    MetadataDifferenceItem(change.first, change.second)
                }
            }
        }
    }
}
