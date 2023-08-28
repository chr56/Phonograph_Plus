/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.settings.Setting
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class LastAddedPlaylist : SmartPlaylist {
    constructor(context: Context) : super(
        "last_added".hashCode() * 31L + R.drawable.ic_library_add_white_24dp,
        context.getString(R.string.last_added)
    )

    override val type: Int
        get() = PlaylistType.LAST_ADDED

    override var iconRes: Int = R.drawable.ic_library_add_white_24dp

    override fun getSongs(context: Context): List<Song> =
        SongLoader.since(context, Setting.instance.lastAddedCutoff)

    override fun containsSong(context: Context, songId: Long): Boolean =
        getSongs(context).find { it.id == songId } != null

    override fun toString(): String = "LastAddedPlaylist"

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<LastAddedPlaylist?> = object : Parcelable.Creator<LastAddedPlaylist?> {
            override fun createFromParcel(source: Parcel): LastAddedPlaylist {
                return LastAddedPlaylist(source)
            }

            override fun newArray(size: Int): Array<LastAddedPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}