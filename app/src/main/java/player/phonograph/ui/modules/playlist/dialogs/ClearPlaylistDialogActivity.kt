/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.documentProviderUriAbsolutePath
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.model.playlist.Playlist
import player.phonograph.ui.basis.MultiLanguageActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ButtonPanel
import player.phonograph.ui.compose.components.CheckBoxItem
import player.phonograph.ui.compose.components.ListItem
import player.phonograph.ui.modules.playlist.dialogs.ClearPlaylistDialogActivity.ClearPlaylistViewModel.State.ConfirmToDelete
import player.phonograph.ui.modules.playlist.dialogs.ClearPlaylistDialogActivity.ClearPlaylistViewModel.State.PreparedToDelete
import player.phonograph.ui.modules.playlist.dialogs.ClearPlaylistDialogActivity.ClearPlaylistViewModel.State.Success
import player.phonograph.util.parcelableArrayListExtra
import player.phonograph.util.permissions.StoragePermissionChecker
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClearPlaylistDialogActivity : MultiLanguageActivity(),
                                    IOpenFileStorageAccessible,
                                    IOpenDirStorageAccessible {

    private val viewModel: ClearPlaylistViewModel by viewModels<ClearPlaylistViewModel>()

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.init(intent)
        super.onCreate(savedInstanceState)
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

        setContent {
            PhonographTheme {
                RootContent()
            }
        }
    }

    @Composable
    private fun RootContent() {

        val currentState by viewModel.state.collectAsState()

        Column(Modifier.padding(8.dp)) {
            Text(
                stringResource(R.string.delete_action),
                Modifier.padding(8.dp),
                style = MaterialTheme.typography.h5
            )
            PermissionNotice()

            when (val state = currentState) {
                is PreparedToDelete -> ScreenPrepare(state)
                is ConfirmToDelete  -> ScreenConfirm(state)
                is Success          -> ScreenSuccess()
            }

        }
    }

    @Composable
    private fun ScreenSuccess() {
        Text(stringResource(R.string.success), Modifier.padding(16.dp))
        ButtonPanel(stringResource(android.R.string.ok), ::finish)
    }

    @Composable
    private fun ScreenPrepare(state: PreparedToDelete) {
        val context = LocalContext.current
        val useSAF by viewModel.useSAF.collectAsState()

        ReportResult(state)
        DisplayPlaylist(state.targetPlaylists)
        CheckBoxItem(stringResource(R.string.behaviour_force_saf), useSAF, true, viewModel::flipUseSAF)

        val text = stringResource(if (state.failedLastTime) R.string.retry else android.R.string.ok)
        ButtonPanel(stringResource(android.R.string.cancel), ::finish, text, {
            lifecycleScope.launch { viewModel.startDelete(context) }
        })
    }

    @Composable
    private fun ReportResult(state: PreparedToDelete) {
        val context = LocalContext.current
        if (state.failedLastTime) {
            val report = remember {
                val total = viewModel.targetPlaylists.size
                val failed = state.targetPlaylists.size
                context.resources.getQuantityString(
                    R.plurals.msg_deletion_result, total, total - failed, total
                )
            }
            Text(report, Modifier.padding(8.dp))
            Text(stringResource(R.string.failed_to_delete), Modifier.padding(8.dp))
        } else {
            Text(stringResource(R.string.delete_action), Modifier.padding(8.dp))
        }
    }


    @Composable
    private fun ScreenConfirm(state: ConfirmToDelete) {
        val context = LocalContext.current
        val playlists = state.targetPlaylists
        val text = remember { resources.getQuantityString(R.plurals.item_files, playlists.size, playlists.size) }
        Text(text, Modifier.padding(8.dp))

        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(playlists) { (playlist, uri) ->

                val actualPath =
                    if (uri != null) {
                        documentProviderUriAbsolutePath(uri, context) ?: uri.toString()
                    } else {
                        "N/A"
                    }
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .wrapContentHeight()
                        .fillMaxWidth()
                ) {
                    Text(
                        playlist.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        actualPath,
                        style = MaterialTheme.typography.body2,
                        color = if (uri == null) MaterialTheme.colors.error else Color.DarkGray
                    )
                }

            }
        }

        ButtonPanel(stringResource(android.R.string.cancel), ::finish, stringResource(R.string.delete_action), {
            lifecycleScope.launch { viewModel.finalDelete(context) }
        })
    }

    @Composable
    private fun DisplayPlaylist(playlists: List<Playlist>) {
        val context = LocalContext.current
        val hint = remember(playlists) {
            resources.getQuantityString(
                R.plurals.item_playlists,
                playlists.size,
                playlists.size
            )
        }
        Text(hint, Modifier.padding(8.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            for (playlist in playlists) {
                item(playlist.id) {
                    val name = remember { playlist.name }
                    val description = remember { playlist.location.text(context).toString() }
                    ListItem(
                        modifier = Modifier,
                        title = name,
                        subtitle = description,
                        onClick = {},
                        painter = painterResource(playlist.iconRes),
                        colorFilter = BlendModeColorFilter(Color.Black, BlendMode.SrcIn)
                    )
                }
            }
        }
    }

    @Composable
    private fun PermissionNotice() {
        val context = LocalContext.current
        val hasPermission = remember { viewModel.checkPermission(context) }
        if (SDK_INT >= VERSION_CODES.R && !hasPermission) {
            Card(Modifier.padding(8.dp), elevation = 4.dp) {
                Column {
                    Text(stringResource(R.string.permission_manage_external_storage_denied), Modifier.padding(8.dp))
                    TextButton({ viewModel.requirePermission(context) }) {
                        Text(
                            stringResource(R.string.grant_permission),
                            style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.primary)
                        )
                    }
                }
            }
        }
    }

    class ClearPlaylistViewModel : ViewModel() {
        lateinit var parameter: Parameter private set
        val targetPlaylists: List<Playlist> get() = parameter.playlists

        private lateinit var session: PlaylistManager.BatchDeleteSession

        fun init(intent: Intent) {
            parameter = Parameter.fromLaunchingIntent(intent)
            session = PlaylistManager.BatchDeleteSession(targetPlaylists)
            _state.value = PreparedToDelete(targetPlaylists, false)
        }

        private val _state: MutableStateFlow<State> = MutableStateFlow(Success)
        val state get() = _state.asStateFlow()

        sealed interface State {
            class PreparedToDelete(val targetPlaylists: List<Playlist>, val failedLastTime: Boolean) : State
            class ConfirmToDelete(val targetPlaylists: List<Pair<Playlist, Uri?>>) : State
            object Success : State
        }


        private val _useSAF: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val useSAF get() = _useSAF.asStateFlow()
        fun flipUseSAF() {
            _useSAF.value = !_useSAF.value
        }

        suspend fun startDelete(context: Context): Boolean {

            session.useSAF = _useSAF.value

            //Phase 1
            if (session.currentPhrase != 1) return false

            // Phase 1 -> 2 (other playlists delete)
            if (session.execute(context) != 2) return false

            // Phase 2 -> 3 or 4 (file playlists delete)
            if (session.execute(context) <= 2) return false

            // Checks
            if (session.currentPhrase == 4) { // Media Store Result
                val failed = session.failed
                _state.value = if (failed.isNotEmpty()) PreparedToDelete(failed, true) else Success
                return true
            } else if (session.currentPhrase == 3) { // SAF Confirmation
                _state.value = ConfirmToDelete(session.linkedUris)
                return true
            } else {
                return false
            }

        }

        suspend fun finalDelete(context: Context): Boolean {
            // Phase 3
            if (session.currentPhrase != 3) return false
            // Phase 3 -> 4
            if (session.execute(context) != 4) return false

            // Checks
            val failed = session.failed
            _state.value =
                if (failed.isEmpty()) {
                    Success
                } else {
                    PreparedToDelete(failed, true)
                }
            return true
        }


        fun checkPermission(context: Context): Boolean = StoragePermissionChecker.hasStorageWritePermission(context)

        @RequiresApi(VERSION_CODES.R)
        fun requirePermission(context: Context) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

    }

    class Parameter(
        val playlists: List<Playlist>,
    ) {
        companion object {
            private const val PLAYLISTS = "playlists"

            fun fromLaunchingIntent(intent: Intent): Parameter {
                val playlist = intent.parcelableArrayListExtra<Playlist>(PLAYLISTS)!!
                return Parameter(playlist)
            }

            fun buildLaunchingIntent(context: Context, playlists: List<Playlist>): Intent =
                Intent(context, ClearPlaylistDialogActivity::class.java).apply {
                    putParcelableArrayListExtra(PLAYLISTS, ArrayList(playlists))
                }
        }
    }

}