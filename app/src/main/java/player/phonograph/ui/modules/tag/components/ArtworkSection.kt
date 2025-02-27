/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.ui.modules.tag.AbsMetadataViewModel
import player.phonograph.ui.modules.tag.MetadataUIEvent
import player.phonograph.ui.modules.tag.MetadataUIEvent.Edit
import player.phonograph.ui.modules.tag.util.selectImage
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun <VM : AbsMetadataViewModel> ArtworkSection(
    viewModel: VM,
    artworkExist: Boolean,
    editMode: Boolean,
    cacheFileName: String,
    content: @Composable (BoxScope.() -> Unit),
) {
    Box(modifier = Modifier.clickable { viewModel.coverImageDetailDialogState.show() }, content = content)
    val context = LocalContext.current
    ImageActionMenuDialog(
        state = viewModel.coverImageDetailDialogState,
        artworkExist = artworkExist,
        onSave = { viewModel.submitEvent(context, MetadataUIEvent.ExtractArtwork) },
        onDelete = { viewModel.submitEvent(context, Edit.RemoveArtwork) },
        onUpdate = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
                if (uri != null) {
                    viewModel.submitEvent(context, Edit.UpdateArtwork.from(context, uri, cacheFileName))
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, android.R.string.cancel, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            viewModel.coverImageDetailDialogState.hide()
        },
        editMode = editMode
    )
}


@Composable
private fun ImageActionMenuDialog(
    state: MaterialDialogState,
    artworkExist: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    editMode: Boolean,
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok, textStyle = accentColoredButtonStyle()) { state.hide() }
    }
) {
    title(res = R.string.label_details)
    ImageActionMenu(artworkExist, editMode, onSave, onDelete, onUpdate)
}