/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import player.phonograph.model.Song
import player.phonograph.model.playlist.ShuffleAllPlaylist
import player.phonograph.repo.mediastore.loaders.SongLoader
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class ShuffleAllPlaylistImpl : ShuffleAllPlaylist {

    constructor(context: Context) : super(context)

    override fun getSongs(context: Context): List<Song> = SongLoader.all(context)

    override fun containsSong(context: Context, songId: Long): Boolean = true

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<ShuffleAllPlaylistImpl?> =
            object : Parcelable.Creator<ShuffleAllPlaylistImpl?> {
                override fun createFromParcel(source: Parcel): ShuffleAllPlaylistImpl {
                    return ShuffleAllPlaylistImpl(source)
                }

                override fun newArray(size: Int): Array<ShuffleAllPlaylistImpl?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
