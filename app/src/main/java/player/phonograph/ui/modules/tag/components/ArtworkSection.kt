/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.model.metadata.InteractiveAction
import player.phonograph.model.metadata.InteractiveAction.Edit
import player.phonograph.ui.modules.tag.AbsMetadataViewModel
import player.phonograph.util.file.selectImage
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


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
        onSave = { viewModel.submitEvent(context, InteractiveAction.ExtractArtwork) },
        onDelete = { viewModel.submitEvent(context, Edit.RemoveArtwork) },
        onUpdate = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                select(context, cacheFileName, viewModel::submitEvent)
            }
            viewModel.coverImageDetailDialogState.hide()
        },
        editMode = editMode
    )
}

private suspend fun select(
    context: Context, fileName: String,
    submitEvent: (context: Context, event: InteractiveAction) -> Unit,
) {
    val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
    if (uri != null) {
        val file = createCacheFile(context, uri, fileName)
        submitEvent(context, Edit.UpdateArtwork(file.absolutePath))
    } else {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, android.R.string.cancel, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun createCacheFile(context: Context, uri: Uri, name: String): File {
    val cacheFile = File(context.externalCacheDir, "Cover_$name.png")
    if (cacheFile.exists()) cacheFile.delete() else cacheFile.createNewFile()
    context.contentResolver.openInputStream(uri).use { inputStream ->
        if (inputStream != null) {
            inputStream.buffered(8192).use { bufferedInputStream ->
                cacheFile.outputStream().buffered(8192).use { outputStream ->
                    // transfer stream
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (bufferedInputStream.read(buffer, 0, 8192).also { read = it } >= 0
                    ) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
        } else {
            warning(context, "Cache", "Can not open selected file! (uri: $uri)")
        }
    }
    cacheFile.deleteOnExit()
    return cacheFile
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