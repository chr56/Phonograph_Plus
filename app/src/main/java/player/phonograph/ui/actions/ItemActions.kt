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
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Playlists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.PathFilterSetting
import player.phonograph.ui.goToAlbumDetail
import player.phonograph.ui.goToArtistDetail
import player.phonograph.ui.goToSongDetail
import player.phonograph.ui.modules.auxiliary.DeletionDialog
import player.phonograph.ui.modules.playlist.dialogs.AddToPlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.ClearPlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.RenamePlaylistDialogActivity
import player.phonograph.ui.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("UNUSED_PARAMETER")
object ItemActions {

    //region Song

    fun play(context: Context, item: Song): Boolean = playNow(context, item)

    fun playNow(context: Context, item: Song): Boolean =
        MusicPlayerRemote.playNow(item)

    fun playNext(context: Context, item: Song): Boolean =
        MusicPlayerRemote.playNext(item)

    fun enqueue(context: Context, item: Song): Boolean =
        MusicPlayerRemote.enqueue(item)

    fun gotoDetail(context: Context, item: Song): Boolean = fragmentActivity(context) { activity ->
        goToSongDetail(activity, item.data)
        true
    }

    fun gotoAlbum(context: Context, item: Song): Boolean {
        goToAlbumDetail(context, item.albumId)
        return true
    }

    fun gotoArtist(context: Context, item: Song): Boolean {
        context.lifecycleScopeOrNewOne().launch {
            goToArtistDetail(context, item)
        }
        return true
    }

    fun share(context: Context, item: Song): Boolean {
        context.startActivity(
            Intent.createChooser(
                shareFileIntent(context, item), null
            )
        )
        return true
    }

    fun setAsRingtone(context: Context, item: Song): Boolean =
        if (checkModificationSystemSettingsPermission(context)) {
            showRingtoneDialog(context)
            true
        } else {
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_ALARM,
                mediaStoreUriSongExternal(item.id)
            )
            true
        }

    fun addToBlacklist(context: Context, item: Song): Boolean {
        if (item.data.isNotBlank()) addToBlacklist(context, item.data.dropLastWhile { it != '/' }.dropLast(1))
        return true
    }

    fun tagEditor(context: Context, item: Song): Boolean {
        goToSongDetail(context, item.data)
        return true
    }

    /** Play queue with target [ShuffleMode]. */
    fun play(context: Context, items: List<Song>, shuffleMode: ShuffleMode?, position: Int): Boolean =
        MusicPlayerRemote.playQueue(items, position, true, shuffleMode)

    fun playNow(context: Context, items: List<Song>): Boolean =
        MusicPlayerRemote.playNow(items)

    fun playNext(context: Context, items: List<Song>): Boolean =
        MusicPlayerRemote.playNext(items)

    fun enqueue(context: Context, items: List<Song>): Boolean =
        MusicPlayerRemote.enqueue(items)

    fun addToPlaylist(context: Context, items: List<Song>): Boolean =
        fragmentActivity(context) { activity ->
            activity.lifecycleScope.launch {
                val playlists = withContext(Dispatchers.IO) { Playlists.all(activity) }
                activity.startActivity(
                    AddToPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, items, playlists)
                )
            }
            true
        }

    fun delete(context: Context, items: List<Song>): Boolean =
        fragmentActivity(context) {
            DeletionDialog
                .create(ArrayList(items)).show(it.supportFragmentManager, "ADD_DELETE")
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

    //endregion

    //region Playlist

    suspend fun play(context: Context, item: Playlist): Boolean =
        songs(context, item).let { songs ->
            if (songs.isNotEmpty())
                play(context, songs, ShuffleMode.NONE, 0)
            else
                false
        }

    suspend fun shuffleAndPlay(context: Context, item: Playlist): Boolean =
        songs(context, item).let { songs ->
            if (songs.isNotEmpty())
                play(context, songs, ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
            else
                false
        }

    suspend fun playNext(context: Context, item: Playlist): Boolean =
        MusicPlayerRemote.playNext(ArrayList(songs(context, item)))

    suspend fun addToCurrentQueue(context: Context, item: Playlist): Boolean =
        MusicPlayerRemote.enqueue(ArrayList(songs(context, item)))

    suspend fun addToPlaylist(context: Context, item: Playlist): Boolean = fragmentActivity(context) { activity ->
        val songs = withContext(Dispatchers.IO) { songs(activity, item) }
        val playlists = withContext(Dispatchers.IO) { Playlists.all(activity) }
        activity.startActivity(
            AddToPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, songs, playlists)
        )
        true
    }

    fun renamePlaylist(context: Context, item: Playlist): Boolean = fragmentActivity(context) { activity ->
        activity.startActivity(
            RenamePlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, item)
        )
        true
    }

    fun deletePlaylist(context: Context, item: Playlist): Boolean = fragmentActivity(context) { activity ->
        activity.startActivity(
            ClearPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, listOf(item))
        )
        true
    }

    fun savePlaylist(context: Context, item: Playlist): Boolean = fragmentActivity(context) { activity ->
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val songs = PlaylistSongsActions.reader(item).allSongs(activity)
            withContext(Dispatchers.Main) {
                activity.startActivity(
                    CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForDuplicate(
                        activity,
                        songs, item.name
                    )
                )
            }
        }
        true
    }

    fun deletePlaylists(context: Context, items: List<Playlist>): Boolean = fragmentActivity(context) { activity ->
        activity.startActivity(
            ClearPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, items)
        )
        true
    }

    fun savePlaylists(context: Context, items: List<Playlist>): Boolean = fragmentActivity(context) { activity ->
        activity.lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                activity.startActivity(
                    CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForDuplicate(
                        activity,
                        items
                    )
                )
            }
        }
        true
    }

    private suspend fun songs(context: Context, item: Playlist): List<Song> =
        withContext(Dispatchers.IO) { PlaylistSongsActions.reader(item).allSongs(context) }

    //endregion


    //region Misc
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
    //endregion
}