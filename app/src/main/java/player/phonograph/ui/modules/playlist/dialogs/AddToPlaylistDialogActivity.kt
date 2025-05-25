/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.ui.basis.DialogActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ButtonPanel
import player.phonograph.ui.compose.components.CheckBoxItem
import player.phonograph.ui.compose.components.ListItem
import player.phonograph.util.parcelableArrayListExtra
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddToPlaylistDialogActivity : DialogActivity(),
                                    IOpenFileStorageAccessible,
                                    IOpenDirStorageAccessible,
                                    ICreateFileStorageAccessible {

    private val viewModel: AddToPlaylistViewModel by viewModels<AddToPlaylistViewModel>()

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.parameter = Parameter.fromLaunchingIntent(intent)
        super.onCreate(savedInstanceState)
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            createFileStorageAccessDelegate,
        )
        setContent {
            PhonographTheme {
                Surface { RootContent() }
            }
        }
    }

    @Composable
    private fun RootContent() {
        val context = LocalContext.current
        val playlists = remember { viewModel.parameter.playlists }
        val useSAF by viewModel.useSAF.collectAsState()
        Column(Modifier.padding(8.dp)) {
            Text(
                stringResource(R.string.action_add_to_playlist),
                Modifier.padding(8.dp),
                style = MaterialTheme.typography.h5
            )
            ListItem(
                modifier = Modifier,
                title = stringResource(R.string.action_new_playlist),
                subtitle = stringResource(R.string.title_new_playlist),
                onClick = { viewModel.createNewPlaylist(context as FragmentActivity) },
                painter = rememberVectorPainter(Icons.Default.Add)
            )
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                for ((index, playlist) in playlists.withIndex()) {
                    item(playlist.id) {
                        val name = remember { playlist.name }
                        val description = remember { playlist.location.text(context).toString() }
                        ListItem(
                            modifier = Modifier,
                            title = name,
                            subtitle = description,
                            onClick = { viewModel.addToExistedPlaylist(context, index) },
                            painter = painterResource(playlist.iconRes),
                            colorFilter = BlendModeColorFilter(Color.Black, BlendMode.SrcIn)
                        )
                    }
                }
            }
            CheckBoxItem(stringResource(R.string.behaviour_force_saf), useSAF, true, viewModel::flipUseSAF)
            ButtonPanel(stringResource(android.R.string.cancel), ::finish, left = true)
        }
    }

    class AddToPlaylistViewModel : ViewModel() {
        lateinit var parameter: Parameter
        val songs: List<Song> get() = parameter.songs
        val playlists: List<Playlist> get() = parameter.playlists

        private val _useSAF: MutableStateFlow<Boolean> = MutableStateFlow(true)
        val useSAF get() = _useSAF.asStateFlow()
        fun flipUseSAF() {
            _useSAF.value = !_useSAF.value
        }

        fun createNewPlaylist(activity: FragmentActivity) {
            activity.startActivity(
                CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForCreating(activity, songs)
            )
            activity.finish()
        }

        fun addToExistedPlaylist(context: Context, position: Int) {
            val playlist = playlists[position]
            val useSaf = useSAF.value
            viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
                PlaylistProcessors.writer(playlist, useSaf)!!.appendSongs(context, songs)
                (context as? Activity)?.finish()
            }
        }
    }

    data class Parameter(
        val songs: List<Song>,
        val playlists: List<Playlist>,
    ) {
        companion object {

            const val SONGS: String = "songs"
            const val ALL_PLAYLISTS: String = "playlists"

            fun fromLaunchingIntent(intent: Intent): Parameter {
                val songs = intent.parcelableArrayListExtra<Song>(SONGS) ?: emptyList()
                val playlists = intent.parcelableArrayListExtra<Playlist>(ALL_PLAYLISTS) ?: emptyList()
                return Parameter(songs, playlists)
            }

            fun buildLaunchingIntent(
                context: Context,
                songs: List<Song>,
                playlists: List<Playlist>,
            ): Intent = Intent(context, AddToPlaylistDialogActivity::class.java).apply {
                putParcelableArrayListExtra(SONGS, ArrayList(songs))
                putParcelableArrayListExtra(ALL_PLAYLISTS, ArrayList(playlists))
            }
        }
    }
}