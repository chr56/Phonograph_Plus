/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import org.koin.android.ext.android.get
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.PlayRequest
import player.phonograph.model.Song
import player.phonograph.model.SongClickMode
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.executePlayRequest
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ModeRadioBox
import player.phonograph.util.parcelable
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import android.os.Bundle

class OpenWithDialog : ComposeViewDialogFragment() {

    private var isMultipleSong: Boolean = false
    private lateinit var playRequest: PlayRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readArgument()
    }

    private fun readArgument() {
        isMultipleSong = arguments?.getBoolean(EXTRA_IS_MULTIPLE_SONGS) ?: throw IllegalArgumentException("No content!")
        playRequest = if (isMultipleSong) {
            PlayRequest.SongsRequest(arguments?.parcelableArrayList<Song>(EXTRA_SONGS)!!, 0)
        } else {
            PlayRequest.SongRequest(arguments?.parcelable<Song>(EXTRA_SONG)!!)
        }
    }


    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        val currentMode = remember {
            mutableIntStateOf(-1)
        }
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(
                        res = android.R.string.cancel,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        dismiss()
                    }
                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        play(currentMode.intValue)
                        dismiss()
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                title(res = R.string.app_name)
                OpenWithDialogContent(requireContext(), isMultipleSong, currentMode)
            }
        }
    }

    private fun play(mode: Int) {
        val queueManager: QueueManager = (context as? AppCompatActivity)?.get() ?: GlobalContext.get().get()
        executePlayRequest(queueManager, playRequest, mode)
        MusicPlayerRemote.resumePlaying()
    }

    companion object {
        private const val EXTRA_IS_MULTIPLE_SONGS = "is_multiple_song"
        private const val EXTRA_SONG = "song"
        private const val EXTRA_SONGS = "songs"


        fun create(song: Song): OpenWithDialog = OpenWithDialog().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_IS_MULTIPLE_SONGS, false)
                putParcelable(EXTRA_SONG, song)
            }
        }

        fun create(songs: List<Song>): OpenWithDialog = OpenWithDialog().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_IS_MULTIPLE_SONGS, true)
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }

        fun create(playRequest: PlayRequest): OpenWithDialog? = when (playRequest) {
            is PlayRequest.SongRequest -> create(playRequest.song)
            is PlayRequest.SongsRequest -> create(playRequest.songs)
            else -> null
        }
    }
}

@Composable
private fun OpenWithDialogContent(context: Context, isMultipleSong: Boolean, currentMode: MutableIntState) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val modes = if (isMultipleSong) SongClickMode.multipleItemsModes else SongClickMode.singleItemModes
        val setCurrentMode = { new: Int ->
            currentMode.intValue = new
        }
        for (id in modes) {
            ModeRadioBox(
                mode = id,
                name = SongClickMode.modeName(context.resources, id),
                currentMode = currentMode,
                setCurrentMode = setCurrentMode
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}


