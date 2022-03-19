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
import player.phonograph.provider.FavoriteSongsStore
import player.phonograph.util.FavoriteUtil

class FavoriteSongsPlaylist : SmartPlaylist, EditablePlaylist {

    constructor(context: Context) : super(
        "favorites".hashCode() * 31L + R.drawable.ic_favorite_border_white_24dp,
        context.getString(R.string.favorites)
    )

    override val type: Int
        get() = PlaylistType.FAVORITE

    override var iconRes: Int = R.drawable.ic_favorite_border_white_24dp

    override fun getSongs(context: Context): List<Song> =
        FavoriteSongsStore.instance.getAllSongs(context)

    override fun containsSong(context: Context, songId: Long): Boolean =
        FavoriteSongsStore.instance.contains(songId, "")

    override fun removeSong(context: Context, song: Song) {
        FavoriteUtil.toggleFavorite(context, song)
    }

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) { } // meaningless

    override fun appendSong(context: Context, song: Song) {
        FavoriteUtil.toggleFavorite(context, song)
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) FavoriteUtil.toggleFavorite(context, song)
    }

    override fun clear(context: Context) {
        FavoriteSongsStore.instance.clear()
    }

    override fun toString(): String = "FavoritePlaylist"

    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<FavoriteSongsPlaylist?> = object : Parcelable.Creator<FavoriteSongsPlaylist?> {
            override fun createFromParcel(source: Parcel): FavoriteSongsPlaylist {
                return FavoriteSongsPlaylist(source)
            }
            override fun newArray(size: Int): Array<FavoriteSongsPlaylist?> {
                return arrayOfNulls(size)
            }
        }
    }
}
