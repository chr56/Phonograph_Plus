/*
 *  Copyright (c) 2022~2024 chr_56
 */
package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.phonograph.dialog.alertDialog
import player.phonograph.R
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.deleteSongsViaMediaStore
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.concurrent.runOnMainHandler
import player.phonograph.util.concurrent.withLooper
import player.phonograph.util.mediaStoreUriSong
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.permissions.StoragePermissionChecker
import player.phonograph.util.permissions.navigateToStorageSetting
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
        MainContent(context, songs, ::dismiss)
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
private fun MainContent(context: Context, songs: List<Song>, dismiss: () -> Unit) {
    PhonographTheme {
        var withLyrics: Boolean by remember { mutableStateOf(false) }
        val hasPermission: Boolean = remember { StoragePermissionChecker.hasStorageWritePermission(context) }
        MaterialDialog(
            dialogState = rememberMaterialDialogState(true),
            onCloseRequest = { dismiss() },
            buttons = {
                negativeButton(
                    res = android.R.string.cancel,
                    textStyle = accentColoredButtonStyle()
                ) { dismiss() }
            }
        ) {
            title(res = R.string.delete_action)
            BoxWithConstraints {
                val limit = maxHeight / 3
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {

                    if (!hasPermission) {
                        Card(elevation = 3.dp) {
                            NoPermissionTips { navigateToStorageSetting(context) }
                        }
                    }

                    Text(
                        context.resources.getQuantityString(
                            R.plurals.msg_header_delete_items,
                            songs.size,
                            songs.size
                        ),
                        Modifier.padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        Modifier
                            .heightIn(max = limit)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        for (song in songs) {
                            Column(Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
                                Text(song.title)
                                SelectionContainer {
                                    Text(song.data, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }


                    Text(
                        stringResource(R.string.warning_can_not_retract),
                        Modifier.padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        Modifier.clickable { withLyrics = !withLyrics },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(withLyrics, null, Modifier.align(Alignment.CenterVertically))
                        Text(
                            stringResource(R.string.delete_with_lyrics),
                            Modifier
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.button
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                dismiss()
                                executeDeletion(context, songs, withLyrics)
                            },
                            Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                stringResource(R.string.delete_action).uppercase(),
                                style = MaterialTheme.typography.button,
                                color = Color.Red,
                            )
                        }
                    }


                }
            }
        }
    }
}

@Composable
private fun NoPermissionTips(navigateToStorageSetting: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.tips_no_storage_write_permission),
            Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )
        TextButton(
            onClick = { navigateToStorageSetting() },
            Modifier.align(Alignment.End)
        ) {
            Text(
                stringResource(R.string.grant_permission),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

private fun executeDeletion(context: Context, songs: List<Song>, withLyrics: Boolean) {
    if (withLyrics) deleteLyrics(context, songs)
    delete(context, songs)
}

private fun delete(context: Context, songs: List<Song>) {
    val total = songs.size
    val fails = deleteSongsViaMediaStore(context, songs)

    val msg: String = context.resources.getQuantityString(
        R.plurals.msg_deletion_result,
        total,
        total - fails.size,
        total
    )

    if (fails.isNotEmpty()) runOnMainHandler {
        // handle fail , report and try again
        showFailDialog(context, msg, fails)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}

private fun deleteLyrics(activity: Context, songs: Collection<Song>) {
    for (song in songs) {
        val file = File(song.data)
        val preciseFiles = LyricsLoader.getExternalPreciseLyricsFile(file)
        val fails = mutableListOf<String>()
        preciseFiles.forEach {
            val result = it.delete()
            if (!result) fails.add(it.name)
        }
        if (fails.isNotEmpty()) {
            withLooper {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.failed_to_delete) + fails.fold("") { a, n -> "$a,$n" },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

private fun showFailDialog(context: Context, msg: String, failList: Collection<Song>) {
    alertDialog(context) {
        val t = context.getString(R.string.failed_to_delete)
        title(t)
        message(
            buildString {
                append("$msg\n")
                append("$t: \n")
                append(failList.fold("") { acc, song -> "$acc${song.title}\n" })
            }
        )
        neutralButton(R.string.grant_permission) {
            navigateToStorageSetting(context)
        }
        positiveButton(android.R.string.ok) { dialog ->
            dialog.dismiss()
        }
        if (SDK_INT >= VERSION_CODES.R && context is Activity) {
            negativeButton(R.string.retry) {
                val uris = failList.map { song ->
                    mediaStoreUriSong(MEDIASTORE_VOLUME_EXTERNAL, song.id)
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