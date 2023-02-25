/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import mt.pref.ThemeColor.primaryColor
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.misc.CreateFileStorageAccessTool
import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.misc.IOpenFileStorageAccess
import player.phonograph.misc.OpenFileStorageAccessTool
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.tageditor.applyEdit
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class BatchTagEditorActivity :
        ComposeToolbarActivity(),
        ICreateFileStorageAccess,
        IOpenFileStorageAccess {

    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()
    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()

    private lateinit var songs: List<Song>
    private val model: BatchTagEditScreenViewModel
            by viewModels { BatchTagEditScreenViewModel.Factory(songs, Color(primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        songs = parseIntent(this, intent)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        super.onCreate(savedInstanceState)
        setupObservers()
    }


    private fun setupObservers() {
    }

    @Composable
    override fun SetUpContent() {
        PhonographTheme {
            BatchTagEditScreen(model, this)
        }
    }

    override val title: String get() = getString(R.string.action_tag_editor)

    override val toolbarActions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { model.saveConfirmationDialogState.show() }) {
            Icon(painterResource(id = R.drawable.ic_save_white_24dp), null)
        }
    }

    override val toolbarBackPressed: () -> Unit = {
        back()
    }

    override fun onBackPressed() {
        back()
    }

    private fun back() {
        if (model.infoTableState.allEditRequests.isEmpty()) {
            finish()
        } else {
            model.exitWithoutSavingDialogState.show()
        }
    }

    companion object {
        private fun parseIntent(context: Context, intent: Intent): List<Song> {
            val songs: ArrayList<Song>? = intent.extras?.getParcelableArrayList(SONGS)
            val ids: LongArray? = intent.extras?.getLongArray(SONG_IDS)
            return songs ?: (ids?.map { SongLoader.getSong(context, it) }
                ?: emptyList())
        }

        private const val SONG_IDS = "SONG_IDS"
        private const val SONGS = "SONGS"

        fun launch(context: Context, songsIds: LongArray) {
            context.startActivity(
                Intent(context.applicationContext, BatchTagEditorActivity::class.java).apply {
                    putExtra(SONG_IDS, songsIds)
                }
            )
        }

        fun launch(context: Context, songs: ArrayList<Song>) {
            context.startActivity(
                Intent(context.applicationContext, BatchTagEditorActivity::class.java).apply {
                    putExtra(SONGS, songs)
                }
            )
        }
    }
}

@Composable
fun BatchTagEditScreen(viewModel: BatchTagEditScreenViewModel, context: Context) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        BatchTagEditTable(viewModel.infoTableState, context)
        // dialogs
        SaveConfirmationDialog(viewModel.saveConfirmationDialogState, { DiffScreen(viewModel) }) {
            saveImpl(viewModel, context)
        }
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) {
            (context as? Activity)?.finish()
        }
    }
}

class BatchTagEditScreenViewModel(
    val songs: List<Song>,
    val defaultColor: Color
) : ViewModel() {

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)

    private var _batchTagEditTableState: BatchTagEditTableState? = null
    val infoTableState: BatchTagEditTableState
        @Synchronized get() {
            if (_batchTagEditTableState == null) {
                _batchTagEditTableState =
                    BatchTagEditTableState(songs.map { SongDetailUtil.readSong(it) }, defaultColor)
            }
            return _batchTagEditTableState!!
        }

    class Factory(
        private val songs: List<Song>,
        private val color: Color
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BatchTagEditScreenViewModel(songs, color) as T
        }
    }
}


internal fun BatchTagEditScreenViewModel.generateDiff(): TagDiff {
    val tagDiff = infoTableState.allEditRequests.map { (key, new) ->
        Triple(key, "(${key.name})", new)
    }
    val artworkDiff =
        if (infoTableState.needReplaceCover) {
            TagDiff.ArtworkDiff.Replaced(infoTableState.newCover)
        } else if (infoTableState.needDeleteCover) {
            TagDiff.ArtworkDiff.Deleted
        } else {
            TagDiff.ArtworkDiff.None
        }
    return TagDiff(tagDiff, artworkDiff)
}

private fun saveImpl(model: BatchTagEditScreenViewModel, context: Context) =
    applyEdit(
        CoroutineScope(Dispatchers.Unconfined),
        context,
        model.songs.map { File(it.data) },
        model.infoTableState.allEditRequests,
        model.infoTableState.needDeleteCover,
        model.infoTableState.needReplaceCover,
        model.infoTableState.newCover,
    )