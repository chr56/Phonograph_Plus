/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import player.phonograph.PlaylistType
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil

class ShuffleAllPlaylist : SmartPlaylist {
    constructor(context: Context) : super(
        "shuffle_all".hashCode() * 31L + R.drawable.ic_shuffle_white_24dp,
        context.getString(R.string.action_shuffle_all)
    )

    override val type: Int
        get() = PlaylistType.RANDOM

    override val iconRes: Int = R.drawable.ic_shuffle_white_24dp

    override fun getSongs(context: Context): List<Song> = MediaStoreUtil.getAllSongs(context) as List<Song>

    override fun containsSong(context: Context, songId: Long): Boolean = true
    override fun toString(): String = "ShuffleAllPlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<ShuffleAllPlaylist?> = object : Parcelable.Creator<ShuffleAllPlaylist?> {
            override fun createFromParcel(source: Parcel): ShuffleAllPlaylist {
                return ShuffleAllPlaylist(source)
            }
            override fun newArray(size: Int): Array<ShuffleAllPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}
