/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.loaders.dynamics.TopAndRecentlyPlayedTracksLoader
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class MyTopTracksPlaylist : SmartPlaylist, ResettablePlaylist {
    constructor(context: Context) : super(
        "top_tracks".hashCode() * 31L + R.drawable.ic_trending_up_white_24dp,
        context.getString(R.string.my_top_tracks)
    )

    override val type: Int
        get() = PlaylistType.MY_TOP_TRACK

    override var iconRes: Int = R.drawable.ic_trending_up_white_24dp

    override fun getSongs(context: Context): List<Song> = TopAndRecentlyPlayedTracksLoader.topTracks(context)

    override fun containsSong(context: Context, songId: Long): Boolean = false // todo

    override fun clear(context: Context) {
        songPlayCountStore.clear()
    }

    override fun refresh(context: Context) {
        songPlayCountStore.reCalculateScore(context)
    }

    override fun toString(): String = "MyTopTracksPlaylist"

    private val songPlayCountStore: SongPlayCountStore by GlobalContext.get().inject()

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<MyTopTracksPlaylist?> = object : Parcelable.Creator<MyTopTracksPlaylist?> {
            override fun createFromParcel(source: Parcel): MyTopTracksPlaylist {
                return MyTopTracksPlaylist(source)
            }
            override fun newArray(size: Int): Array<MyTopTracksPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}