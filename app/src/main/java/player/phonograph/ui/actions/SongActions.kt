/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.R
import player.phonograph.foundation.concurrent.lifecycleScopeOrNewOne
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.fragmentActivity
import player.phonograph.foundation.mediastore.mediaStoreUriSongExternal
import player.phonograph.foundation.permission.checkModificationSystemSettingsPermission
import player.phonograph.model.Song
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Playlists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.PathFilterSetting
import player.phonograph.ui.goToAlbumDetail
import player.phonograph.ui.goToArtistDetail
import player.phonograph.ui.modules.auxiliary.DeletionDialog
import player.phonograph.ui.modules.playlist.dialogs.AddToPlaylistDialogActivity
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


fun Song.actionPlay(): Boolean = actionPlayNow()

/**
 * Play queue with target [ShuffleMode]
 */
fun List<Song>.actionPlay(shuffleMode: ShuffleMode?, position: Int) =
    MusicPlayerRemote.playQueue(this, position, true, shuffleMode)

fun Song.actionPlayNow(): Boolean =
    MusicPlayerRemote.playNow(this)

fun List<Song>.actionPlayNow(): Boolean =
    MusicPlayerRemote.playNow(this)

fun Song.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)

fun List<Song>.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)


fun Song.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)

fun List<Song>.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)


fun Song.actionGotoDetail(activity: FragmentActivity): Boolean {
    TagBrowserActivity.launch(activity, data)
    return true
}

fun Song.actionGotoAlbum(context: Context): Boolean {
    goToAlbumDetail(context, albumId)
    return true
}


fun Song.actionGotoArtist(context: Context): Boolean {
    context.lifecycleScopeOrNewOne().launch {
        goToArtistDetail(context, this@actionGotoArtist)
    }
    return true
}

fun Song.actionShare(context: Context): Boolean {
    context.startActivity(
        Intent.createChooser(
            shareFileIntent(context, this), null
        )
    )
    return true
}


fun Song.actionSetAsRingtone(context: Context): Boolean =
    if (checkModificationSystemSettingsPermission(context)) {
        showRingtoneDialog(context)
        true
    } else {
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_ALARM,
            mediaStoreUriSongExternal(id)
        )
        true
    }


fun Song.actionAddToBlacklist(context: Context): Boolean {
    if (data.isNotBlank()) addToBlacklist(context, data.dropLastWhile { it != '/' }.dropLast(1))
    return true
}

fun Song.actionTagEditor(context: Context): Boolean {
    TagBrowserActivity.launch(context, data)
    return true
}

fun List<Song>.actionAddToPlaylist(context: Context) =
    fragmentActivity(context) { activity ->
        activity.lifecycleScope.launch {
            val songs = this@actionAddToPlaylist
            val playlists = withContext(Dispatchers.IO) { Playlists.all(activity) }
            activity.startActivity(
                AddToPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, songs, playlists)
            )
        }
        true
    }

fun List<Song>.actionDelete(context: Context) =
    fragmentActivity(context) {
        DeletionDialog
            .create(ArrayList(this)).show(it.supportFragmentManager, "ADD_DELETE")
        true
    }

private fun showRingtoneDialog(context: Context): AlertDialog =
    AlertDialog.Builder(context)
        .setTitle(R.string.title_dialog_ringtone)
        .setMessage(R.string.title_dialog_ringtone)
        .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
        .setPositiveButton(android.R.string.ok) { _, _ ->
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
        .create().tintButtons()

private fun shareFileIntent(context: Context, song: Song): Intent = try {
    Intent()
        .setAction(Intent.ACTION_SEND)
        .putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(context, context.applicationContext.packageName, File(song.data))
        )
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .setType("audio/*")
} catch (e: IllegalArgumentException) {
    // the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
    warning(context, "Share", "Physical external SD card is not fully support!", e)
    Intent()
}

fun addToBlacklist(context: Context, path: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val candidatesPaths = mutableListOf<String>()
        var parent: String = path // parent folder
        while (parent.isNotEmpty()) {
            if (parent.endsWith("/emulated/0", true)
                or parent.endsWith("/emulated", true)
                or parent.endsWith("/storage", true)
            ) break
            candidatesPaths.add(parent)
            parent = parent.dropLastWhile { it != '/' }.dropLast(1) // last char is '/'
        }
        if (candidatesPaths.isEmpty()) candidatesPaths.add(path)

        var selectedPathText = ""
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle(R.string.label_file_path)
                .setSingleChoiceItems(candidatesPaths.toTypedArray(), -1) { dialog, which ->
                    selectedPathText = candidatesPaths[which]
                }
                .setPositiveButton(android.R.string.ok) { parentDialog, _ ->
                    if (selectedPathText.isNotBlank()) {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.tips_add_to_blacklist)
                            .setMessage(selectedPathText)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    PathFilterSetting(true).add(context, selectedPathText)
                                }
                                dialog.dismiss()
                                parentDialog.dismiss()
                            }
                            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                                parentDialog.dismiss()
                            }
                            .create().tintButtons().show()
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create().tintButtons().show()
        }
    }
}