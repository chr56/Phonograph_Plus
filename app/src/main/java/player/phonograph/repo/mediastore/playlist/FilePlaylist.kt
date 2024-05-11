/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.repo.mediastore.playlist

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import player.phonograph.ui.dialogs.ClearPlaylistDialog
import player.phonograph.util.warning
import util.phonograph.playlist.PlaylistsManager
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore.VOLUME_EXTERNAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FilePlaylistImpl : FilePlaylist {

    constructor(id: Long, name: String?, path: String, dateAdded: Long, dateModified: Long) :
            super(id, name, path, dateAdded, dateModified)

    override val mediastoreUri: Uri
        get() = Playlists.Members.getContentUri(if (SDK_INT >= Q) VOLUME_EXTERNAL else "external", id)

    override fun getSongs(context: Context): List<Song> =
        PlaylistSongLoader.getPlaylistSongList(context, id).map { it.song }

    override fun containsSong(context: Context, songId: Long): Boolean =
        PlaylistSongLoader.doesPlaylistContain(context, id, songId)


    override fun removeSong(context: Context, song: Song) = runBlocking {
        removeFromPlaylistViaMediastore(context, song, id)
        Unit
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        CoroutineScope(Dispatchers.Default).launch {
            PlaylistsManager.appendPlaylist(context, songs, this@FilePlaylistImpl)
        }
    }

    override fun appendSong(context: Context, song: Song) = appendSongs(context, listOf(song))

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) {
        runBlocking {
            moveItemViaMediastore(context, id, from, to)
        }
    }

    override fun clear(context: Context) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            fragmentActivity.lifecycleScope.launch(Dispatchers.Main) {
                ClearPlaylistDialog.create(listOf(this@FilePlaylistImpl))
                    .show(fragmentActivity.supportFragmentManager, "CLEAR_PLAYLIST_DIALOG")
            }
        } else {
            warning("FilePlaylist", context.getString(R.string.failed))
        }
    }

    override fun describeContents(): Int = 0

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<FilePlaylist?> = object : Parcelable.Creator<FilePlaylist?> {
            override fun createFromParcel(source: Parcel): FilePlaylist {
                return FilePlaylistImpl(source)
            }

            override fun newArray(size: Int): Array<FilePlaylist?> {
                return arrayOfNulls(size)
            }
        }

        val EMPTY_PLAYLIST: FilePlaylist =
            FilePlaylistImpl(id = -1, name = "N/A", path = "-", dateAdded = -1, dateModified = -1)
    }
}
