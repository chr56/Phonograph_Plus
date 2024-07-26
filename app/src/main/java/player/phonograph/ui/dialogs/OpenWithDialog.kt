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
import player.phonograph.ui.activities.MainActivity
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.os.Bundle

class OpenWithDialog : ComposeViewDialogFragment() {

    private var isMultipleSong: Boolean = false
    private lateinit var playRequest: PlayRequest
    private var gotoMainActivity: Boolean = false

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
        gotoMainActivity = arguments?.getBoolean(EXTRA_GOTO_MAIN_ACTIVITY) ?: false
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
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val context = LocalContext.current
                    OpenWithContent(playRequest)
                    Spacer(Modifier.height(8.dp))
                    OpenWithOptions(context, isMultipleSong, currentMode) { newValue: Int ->
                        currentMode.intValue = newValue
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    private fun play(mode: Int) {
        val queueManager: QueueManager = (context as? AppCompatActivity)?.get() ?: GlobalContext.get().get()
        executePlayRequest(queueManager, playRequest, mode)
        MusicPlayerRemote.resumePlaying()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (gotoMainActivity) {
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            requireActivity().finish()
        }
    }

    companion object {
        private const val EXTRA_GOTO_MAIN_ACTIVITY = "goto_main_activity"
        private const val EXTRA_IS_MULTIPLE_SONGS = "is_multiple_song"
        private const val EXTRA_SONG = "song"
        private const val EXTRA_SONGS = "songs"


        fun create(song: Song, gotoMainActivity: Boolean): OpenWithDialog = OpenWithDialog().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_IS_MULTIPLE_SONGS, false)
                putParcelable(EXTRA_SONG, song)
                putBoolean(EXTRA_GOTO_MAIN_ACTIVITY, gotoMainActivity)
            }
        }

        fun create(songs: List<Song>, gotoMainActivity: Boolean): OpenWithDialog = OpenWithDialog().apply {
            arguments = Bundle().apply {
                putBoolean(EXTRA_IS_MULTIPLE_SONGS, true)
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
                putBoolean(EXTRA_GOTO_MAIN_ACTIVITY, gotoMainActivity)
            }
        }

        fun create(playRequest: PlayRequest, gotoMainActivity: Boolean = true): OpenWithDialog? = when (playRequest) {
            is PlayRequest.SongRequest -> create(playRequest.song, gotoMainActivity)
            is PlayRequest.SongsRequest -> create(playRequest.songs, gotoMainActivity)
            else -> null
        }
    }
}


@Composable
private fun OpenWithContent(playRequest: PlayRequest) {
    when (playRequest) {
        is PlayRequest.SongRequest  -> SongItem(song = playRequest.song)
        is PlayRequest.SongsRequest -> for (song in playRequest.songs) SongItem(song = song)
        else                        -> {}
    }
}

@Composable
private fun SongItem(song: Song) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(
            song.title,
            Modifier.padding(2.dp),
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            song.data, Modifier.padding(2.dp),
            fontSize = 12.sp
        )
    }
}


@Composable
private fun OpenWithOptions(
    context: Context,
    isMultipleSong: Boolean,
    currentMode: MutableIntState,
    setCurrentMode: (Int) -> Unit,
) {
    val modes = if (isMultipleSong) SongClickMode.multipleItemsModes else SongClickMode.singleItemModes
    for (id in modes) {
        ModeRadioBox(
            mode = id,
            name = SongClickMode.modeName(context.resources, id),
            currentMode = currentMode,
            setCurrentMode = setCurrentMode
        )
    }
}


