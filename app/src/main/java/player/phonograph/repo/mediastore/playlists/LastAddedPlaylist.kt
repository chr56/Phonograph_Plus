/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import player.phonograph.model.Song
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.settings.Setting
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class LastAddedPlaylistImpl : LastAddedPlaylist {


    constructor(context: Context) : super(context)

    override fun getSongs(context: Context): List<Song> =
        SongLoader.since(context, Setting.instance.lastAddedCutoff)

    override fun containsSong(context: Context, songId: Long): Boolean =
        getSongs(context).find { it.id == songId } != null

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<LastAddedPlaylistImpl?> = object : Parcelable.Creator<LastAddedPlaylistImpl?> {
            override fun createFromParcel(source: Parcel): LastAddedPlaylistImpl {
                return LastAddedPlaylistImpl(source)
            }

            override fun newArray(size: Int): Array<LastAddedPlaylistImpl?> {
                return arrayOfNulls(size)
            }
        }
    }
}