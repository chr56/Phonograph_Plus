/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.documentProviderUriAbsolutePath
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.model.playlist.Playlist
import player.phonograph.ui.compose.ComposeThemeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ListItem
import player.phonograph.util.coroutineToast
import player.phonograph.util.file.selectDocumentUris
import player.phonograph.util.parcelableArrayListExtra
import player.phonograph.util.permissions.StoragePermissionChecker
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.ItemGroup
import player.phonograph.util.text.buildDeletionMessage
import player.phonograph.util.theme.tintButtons
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClearPlaylistDialogActivity : ComposeThemeActivity(),
                                    IOpenFileStorageAccessible,
                                    IOpenDirStorageAccessible {

    private val viewModel: ClearPlaylistViewModel by viewModels<ClearPlaylistViewModel>()

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.parameter = Parameter.fromLaunchingIntent(intent)
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
        val context = LocalContext.current
        val playlists = remember { viewModel.targetPlaylists }
        val useSAF by viewModel.useSAF.collectAsState()

        Column(Modifier.padding(8.dp)) {
            Text(
                stringResource(R.string.delete_action),
                Modifier.padding(8.dp),
                style = MaterialTheme.typography.h5
            )
            PermissionNotice()
            DisplayPlaylist(playlists)
            // CheckBoxItem(stringResource(R.string.behaviour_force_saf), useSAF, true, viewModel::flipUseSAF)
            Buttons(::finish, stringResource(R.string.delete_action)) {
                lifecycleScope.launch {
                    viewModel.delete(this@ClearPlaylistDialogActivity, playlists)
                }
            }
        }
    }

    @Composable
    private fun DisplayPlaylist(playlists: List<Playlist>) {
        val context = LocalContext.current
        val hint = remember { resources.getQuantityString(R.plurals.item_playlists, playlists.size, playlists.size) }
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
                        onMenuClick = null,
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
        val hasPermission = remember { StoragePermissionChecker.hasStorageWritePermission(context) }
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

    @Composable
    private fun Buttons(onCanceled: () -> Unit, confirmText: String, onConfirmed: () -> Unit) {
        Row {
            TextButton(onCanceled) {
                Text(
                    stringResource(android.R.string.cancel),
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onConfirmed) {
                Text(
                    confirmText,
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                )
            }
        }
    }

    class ClearPlaylistViewModel : ViewModel() {
        lateinit var parameter: Parameter
        val targetPlaylists: List<Playlist> get() = parameter.playlists

        private val _useSAF: MutableStateFlow<Boolean> = MutableStateFlow(true)
        val useSAF get() = _useSAF.asStateFlow()
        fun flipUseSAF() {
            _useSAF.value = !_useSAF.value
        }


        suspend fun delete(context: FragmentActivity, playlists: List<Playlist>) {

            /* Normally Delete (MediaStore + Internal database) */
            val results = playlists.map { playlist ->
                PlaylistManager.delete(playlist, false).delete(context)
            }

            /* Check */
            val allCount = results.size
            val failureCount = results.count { !it }
            val failures = results.mapIndexedNotNull { index, result -> if (!result) playlists[index] else null }
            val errorMessages = buildString {
                appendLine(
                    context.resources.getQuantityString(
                        R.plurals.msg_deletion_result,
                        allCount, allCount - failureCount, allCount
                    )
                )
                if (failureCount > 0) {
                    appendLine(
                        "${context.getString(R.string.failed_to_delete)}: "
                    )
                    for (failure in failures) {
                        appendLine("${failure.name}(${failure.location})")
                    }
                }
            }

            /* Again */
            withContext(Dispatchers.Main) {
                MaterialDialog(context)
                    .title(R.string.action_delete_from_device)
                    .message(text = errorMessages)
                    .positiveButton(android.R.string.ok)
                    .apply {
                        if (failureCount > 0) negativeButton(R.string.delete_with_saf) {
                            CoroutineScope(Dispatchers.IO).launch {
                                if (context is IOpenDirStorageAccessible) {
                                    deleteViaSAF(context, failures)
                                } else {
                                    coroutineToast(context, R.string.failed)
                                }
                            }
                        }
                    }
                    .tintButtons()
                    .show()
            }
        }

        /**
         * use SAF to choose a directory, and delete playlist inside this directory with user's confirmation
         * @param activity host Activity
         * @param playlists playlists to delete
         */
        suspend fun deleteViaSAF(activity: Activity, playlists: List<Playlist>) {
            require(activity is IOpenDirStorageAccessible)

            val paths = playlists.mapNotNull { playlist -> playlist.path() }
            val uris = selectDocumentUris(activity, paths)

            val warnings = buildDeletionMessage(
                context = activity,
                itemSize = uris.size,
                "",
                ItemGroup(
                    activity.resources.getQuantityString(R.plurals.item_files, playlists.size, playlists.size),
                    uris.mapNotNull { uri -> documentProviderUriAbsolutePath(uri, activity) ?: uri.path }
                )
            )

            withContext(Dispatchers.Main) {
                val dialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.delete_action)
                    .setMessage(warnings)
                    .setPositiveButton(R.string.delete_action) { dialog, _ ->
                        val failed = deleteUri(activity, uris)
                        sentPlaylistChangedLocalBoardCast()
                        dialog.dismiss()
                        if (failed.isNotEmpty()) {
                            val msg = failed.fold("Failed to delete: ") { acc, uri ->
                                val absolutePath = documentProviderUriAbsolutePath(uri, activity) ?: uri.path
                                "$acc, $absolutePath"
                            }
                            reportError(Exception(msg), TAG, msg)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .create().tintButtons()

                dialog.also {
                    it.getButton(DialogInterface.BUTTON_POSITIVE)
                        ?.setTextColor(activity.getColor(util.theme.materials.R.color.md_red_800))
                    it.getButton(DialogInterface.BUTTON_NEGATIVE)
                        ?.setTextColor(activity.getColor(util.theme.materials.R.color.md_grey_500))
                }

                dialog.show()
            }
        }

        /**
         * Delete Document Uri
         * @return failed list
         */
        private fun deleteUri(context: Context, uris: Collection<Uri>): Collection<Uri> {
            return uris.mapNotNull { uri ->
                if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                    uri
                } else {
                    null
                }
            }
        }

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

    companion object {
        private const val TAG = "ClearPlaylistDialog"
    }
}