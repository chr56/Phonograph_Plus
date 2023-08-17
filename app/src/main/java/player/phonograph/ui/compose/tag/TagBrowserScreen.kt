/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.applyEdit
import player.phonograph.ui.compose.ColorTools
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun TagBrowserScreen(viewModel: TagBrowserScreenViewModel, context: Context?) {
    val wrapper by viewModel.artwork.collectAsState()
    val paletteColor =
        ColorTools.makeSureContrastWith(MaterialTheme.colors.surface) {
            if (wrapper != null) {
                Color(wrapper!!.paletteColor)
            } else {
                MaterialTheme.colors.primaryVariant
            }
        }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        if (wrapper != null || viewModel is TagEditorScreenViewModel) {
            CoverImage(
                bitmap = wrapper?.bitmap,
                backgroundColor = paletteColor,
                modifier = Modifier.clickable {
                    viewModel.coverImageDetailDialogState.show()
                }
            )
        }
        InfoTable(viewModel.audioDetailState)
    }
    CoverImageDetailDialog(
        state = viewModel.coverImageDetailDialogState,
        artworkExist = wrapper != null,
        onSave = { viewModel.saveArtwork(context!!) },
        onDelete = { (viewModel as? TagEditorScreenViewModel)?.deleteArtwork() },
        onUpdate = { (viewModel as? TagEditorScreenViewModel)?.replaceArtwork(context!!) },
        editMode = viewModel is TagEditorScreenViewModel
    )
    // edit mode
    if (viewModel is TagEditorScreenViewModel) {
        SaveConfirmationDialog(viewModel.saveConfirmationDialogState, { DiffScreen(viewModel) }) {
            val songFile = File(viewModel.song.data)
            val coroutineScope = CoroutineScope(Dispatchers.Unconfined)
            if (songFile.canWrite()) {
                viewModel.mergeActions()
                saveImpl(viewModel, songFile, coroutineScope, context ?: App.instance)
            } else {
                coroutineScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        App.instance, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT
                    ).show()
                }
                navigateToStorageSetting(context ?: App.instance)
            }
        }
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) {
            (context as? Activity)?.finish()
        }
    }
}

private fun saveImpl(
    model: TagEditorScreenViewModel,
    songFile: File,
    coroutineScope: CoroutineScope,
    context: Context,
) =
    applyEdit(
        coroutineScope,
        context,
        songFile,
        model.audioDetailState.pendingEditRequests,
        model.needDeleteCover,
        model.needReplaceCover,
        model.newCover
    )
