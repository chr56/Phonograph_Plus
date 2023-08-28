/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.mediastore.loaders.dynamics.TopAndRecentlyPlayedTracksLoader
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class HistoryPlaylist : SmartPlaylist, ResettablePlaylist {
    constructor(context: Context) : super(
        "recently_played".hashCode() * 31L + R.drawable.ic_access_time_white_24dp,
        context.getString(R.string.history)
    )

    override val type: Int
        get() = PlaylistType.HISTORY

    override var iconRes: Int = R.drawable.ic_access_time_white_24dp

    override fun getSongs(context: Context): List<Song> =
        TopAndRecentlyPlayedTracksLoader.recentlyPlayedTracks(context)

    override fun containsSong(context: Context, songId: Long): Boolean = false // todo

    override fun clear(context: Context) {
        HistoryStore.get().clear()
    }

    override fun toString(): String = "HistoryPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<HistoryPlaylist?> = object : Parcelable.Creator<HistoryPlaylist?> {
            override fun createFromParcel(source: Parcel): HistoryPlaylist {
                return HistoryPlaylist(source)
            }
            override fun newArray(size: Int): Array<HistoryPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}