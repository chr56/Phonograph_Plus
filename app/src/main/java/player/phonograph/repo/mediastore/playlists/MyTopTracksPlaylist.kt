/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import org.koin.core.context.GlobalContext
import player.phonograph.model.Song
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class MyTopTracksPlaylistImpl : MyTopTracksPlaylist {

    constructor(context: Context) : super(context)

    override fun getSongs(context: Context): List<Song> = TopTracksLoader.get().tracks(context)

    override fun containsSong(context: Context, songId: Long): Boolean = false // todo

    override fun clear(context: Context) {
        songPlayCountStore.clear()
    }

    override fun refresh(context: Context) {
        songPlayCountStore.reCalculateScore(context)
    }

    private val songPlayCountStore: SongPlayCountStore by GlobalContext.get().inject()

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<MyTopTracksPlaylistImpl?> =
            object : Parcelable.Creator<MyTopTracksPlaylistImpl?> {
                override fun createFromParcel(source: Parcel): MyTopTracksPlaylistImpl {
                    return MyTopTracksPlaylistImpl(source)
                }

                override fun newArray(size: Int): Array<MyTopTracksPlaylistImpl?> {
                    return arrayOfNulls(size)
                }
            }
    }
}