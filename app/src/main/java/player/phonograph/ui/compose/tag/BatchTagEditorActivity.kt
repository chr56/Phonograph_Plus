/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenFileStorageAccessTool
import mt.pref.ThemeColor.primaryColor
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.applyEdit
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.parcelableArrayList
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        ComposeThemeActivity(),
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

        setContent {
            val highlightColor by primaryColor.collectAsState()
            PhonographTheme(highlightColor) {
                val scaffoldState = rememberScaffoldState()
                Scaffold(
                    Modifier.statusBarsPadding(),
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.action_tag_editor)) },
                            navigationIcon = {
                                Box(Modifier.padding(16.dp)) {
                                    Icon(
                                        Icons.Default.ArrowBack, null,
                                        Modifier.clickable {
                                            onBackPressedDispatcher.onBackPressed()
                                        }
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { model.saveConfirmationDialogState.show() }) {
                                    Icon(painterResource(id = R.drawable.ic_save_white_24dp), null)
                                }
                            },
                            backgroundColor = highlightColor
                        )
                    }
                ) {
                    Box(Modifier.padding(it)) {
                        BatchTagEditScreen(model)
                    }
                }

            }

        }

        setupObservers()
    }


    private fun setupObservers() {
    }

    override fun onBackPressed() {
        if (!model.infoTableState.hasEdited) {
            finish()
        } else {
            model.exitWithoutSavingDialogState.show()
        }
    }

    companion object {
        private fun parseIntent(context: Context, intent: Intent): List<Song> {
            val songs: ArrayList<Song>? = intent.extras?.parcelableArrayList(SONGS)
            val ids: LongArray? = intent.extras?.getLongArray(SONG_IDS)
            return songs ?: (ids?.map { SongLoader.id(context, it) }
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
fun BatchTagEditScreen(viewModel: BatchTagEditScreenViewModel) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        val context = LocalContext.current
        BatchTagEditTable(viewModel.infoTableState)
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
    val defaultColor: Color,
) : ViewModel() {

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)

    private var _batchTagEditTableState: BatchTagEditTableState? = null
    val infoTableState: BatchTagEditTableState
        @Synchronized get() {
            if (_batchTagEditTableState == null) {
                _batchTagEditTableState =
                    BatchTagEditTableState(songs.map { loadSongInfo(it) }, defaultColor)
            }
            return _batchTagEditTableState!!
        }

    class Factory(
        private val songs: List<Song>,
        private val color: Color,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BatchTagEditScreenViewModel(songs, color) as T
        }
    }
}


internal fun BatchTagEditScreenViewModel.generateDiff(): TagDiff {
    val tagDiff = infoTableState.pendingEditRequests.map { action ->
        val new = when (action) {
            is EditAction.Delete -> null
            is EditAction.Update -> action.newValue
        }
        Triple(action.key, ("(${action.key.name})"), new)
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
        model.infoTableState.pendingEditRequests,
        model.infoTableState.needDeleteCover,
        model.infoTableState.needReplaceCover,
        model.infoTableState.newCover,
    )