/*
 *  Copyright (c) 2022~2024 chr_56
 */
package player.phonograph.ui.modules.auxiliary

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.foundation.compat.parcelableArrayList
import player.phonograph.foundation.concurrent.runOnMainHandler
import player.phonograph.foundation.mediastore.mediaStoreUriSongExternal
import player.phonograph.foundation.permission.StoragePermissionChecker
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.MediaStoreSongsActions
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.navigateToStorageSetting
import player.phonograph.ui.util.alertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import java.io.File

class DeletionDialog : ComposeViewDialogFragment() {

    private lateinit var songs: List<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songs = requireArguments().parcelableArrayList(KEY_SONGS)!!
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val executor = remember(context) { DeletionExecutor(context) }
        MainContent(
            executor = executor,
            songs = songs,
            dismiss = ::dismiss,
        )
    }

    companion object {

        private const val KEY_SONGS = "SONGS"

        fun create(songs: ArrayList<Song>): DeletionDialog =
            DeletionDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_SONGS, songs)
                }
            }
    }
}

@Composable
private fun MainContent(
    executor: DeletionExecutor,
    songs: List<Song>,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    PhonographTheme {
        MaterialDialog(
            dialogState = rememberMaterialDialogState(true),
            onCloseRequest = { dismiss() },
            buttons = {
                positiveButton(
                    res = R.string.action_delete,
                    textStyle = MaterialTheme.typography.button.copy(color = Color(200, 9, 9))
                ) {
                    dismiss()
                    showDeletionResult(context, executor.delete(songs))
                }
                negativeButton(
                    res = android.R.string.cancel,
                    textStyle = MaterialTheme.typography.button.copy(color = Color(33, 139, 130))
                ) { dismiss() }
            }
        ) {
            title(res = R.string.action_delete)
            BoxWithConstraints {
                val limit = maxHeight / 3
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {

                    if (!executor.hasPermission) {
                        Card(elevation = 3.dp) {
                            NoPermissionTips { navigateToStorageSetting(context) }
                        }
                    }

                    Text(
                        resources.getQuantityString(
                            R.plurals.msg_header_delete_items,
                            songs.size,
                            songs.size
                        ),
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        Modifier
                            .heightIn(max = limit)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        for (song in songs) {
                            TargetItem(song)
                        }
                    }


                    Text(
                        stringResource(R.string.warning_can_not_retract),
                        Modifier.padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold
                    )

                    DeleteOption(
                        stringResource(R.string.action_remove_from_playing_queue),
                        executor.withQueueItem,
                        { executor.withQueueItem = !executor.withQueueItem }
                    )

                    DeleteOption(
                        stringResource(R.string.option_delete_with_lyrics),
                        executor.withLyrics,
                        { executor.withLyrics = !executor.withLyrics }
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetItem(song: Song) {
    Column(Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        Text(song.title)
        SelectionContainer {
            Text(song.data, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DeleteOption(
    text: String,
    checked: Boolean,
    invert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        Modifier
            .clickable(onClick = invert)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(checked, null, Modifier.align(Alignment.CenterVertically))
        Text(
            text,
            Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
private fun NoPermissionTips(navigateToStorageSetting: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.err_no_storage_write_permission),
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )
        TextButton(
            onClick = { navigateToStorageSetting() },
            Modifier.align(Alignment.End)
        ) {
            Text(
                stringResource(R.string.action_grant_permission),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

private fun showDeletionResult(context: Context, result: DeletionResult) {
    for (fails in result.failedLyricsBySong) {
        runOnMainHandler {
            Toast.makeText(
                context,
                context.getString(R.string.err_failed_to_delete) + fails.fold("") { a, n -> "$a,$n" },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (result.failedSongs.isNotEmpty()) runOnMainHandler {
        val msg: String = context.resources.getQuantityString(
            R.plurals.msg_deletion_result,
            result.total,
            result.total - result.failedSongs.size,
            result.total
        )
        showFailDialog(context, msg, result.failedSongs)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}

private fun showFailDialog(context: Context, msg: String, failList: Collection<Song>) {
    alertDialog(context) {
        val t = context.getString(R.string.err_failed_to_delete)
        title(t)
        message(
            buildString {
                append("$msg\n")
                append("$t: \n")
                append(failList.fold("") { acc, song -> "$acc${song.title}\n" })
            }
        )
        neutralButton(R.string.action_grant_permission) {
            navigateToStorageSetting(context)
        }
        positiveButton(android.R.string.ok) { dialog ->
            dialog.dismiss()
        }
        if (SDK_INT >= VERSION_CODES.R && context is Activity) {
            negativeButton(R.string.action_retry) {
                val uris = failList.map { song ->
                    mediaStoreUriSongExternal(song.id)
                }
                context.startIntentSenderForResult(
                    MediaStore.createDeleteRequest(
                        context.contentResolver, uris
                    ).intentSender, 0, null, 0, 0, 0
                )
            }
        }
    }.show()
}

private data class DeletionResult(
    val total: Int,
    val failedSongs: List<Song>,
    val failedLyricsBySong: List<List<String>>,
)

private class DeletionExecutor(context: Context) {

    private val applicationContext = context.applicationContext

    val hasPermission: Boolean get() = StoragePermissionChecker.hasStorageWritePermission(applicationContext)
    var withLyrics: Boolean by mutableStateOf(false)
    var withQueueItem: Boolean by mutableStateOf(true)

    fun delete(songs: List<Song>): DeletionResult {
        if (withQueueItem) {
            val queueManager = MusicPlayerRemote.queueManager
            queueManager.playingQueue
                .filter { queuedSong -> songs.any { it.data == queuedSong.data } }
                .forEach { queueManager.removeSong(it) }
        }
        val failedLyricsBySong = if (withLyrics) deleteLyrics(songs) else emptyList()
        val failedSongs = MediaStoreSongsActions.delete(applicationContext, songs)
        return DeletionResult(songs.size, failedSongs, failedLyricsBySong)
    }

    private fun deleteLyrics(songs: List<Song>): List<List<String>> =
        songs.mapNotNull { song ->
            val file = File(song.data)
            val preciseFiles = LyricsLoader.searchExternalPreciseLyricsFiles(file)
            val fails = mutableListOf<String>()
            preciseFiles.forEach {
                if (!it.delete()) fails.add(it.name)
            }
            fails.takeIf { it.isNotEmpty() }
        }
}