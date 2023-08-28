/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import player.phonograph.model.Song
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class HistoryPlaylistImpl : HistoryPlaylist {

    constructor(context: Context) : super(context)

    override fun getSongs(context: Context): List<Song> =
        RecentlyPlayedTracksLoader.get().tracks(context)

    override fun containsSong(context: Context, songId: Long): Boolean = false // todo

    override fun clear(context: Context) {
        HistoryStore.get().clear()
    }

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<HistoryPlaylistImpl?> = object : Parcelable.Creator<HistoryPlaylistImpl?> {
            override fun createFromParcel(source: Parcel): HistoryPlaylistImpl {
                return HistoryPlaylistImpl(source)
            }

            override fun newArray(size: Int): Array<HistoryPlaylistImpl?> {
                return arrayOfNulls(size)
            }
        }
    }
}