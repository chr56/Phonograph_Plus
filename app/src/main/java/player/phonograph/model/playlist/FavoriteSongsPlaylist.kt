/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.mechanism.Favorite
import player.phonograph.model.Song
import player.phonograph.repo.database.FavoritesStore
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class FavoriteSongsPlaylist : SmartPlaylist, EditablePlaylist {

    constructor(context: Context) : super(
        "favorites".hashCode() * 31L + R.drawable.ic_favorite_border_white_24dp,
        context.getString(R.string.favorites)
    )

    override val type: Int
        get() = PlaylistType.FAVORITE

    override var iconRes: Int = R.drawable.ic_favorite_border_white_24dp

    override fun getSongs(context: Context): List<Song> =
        FavoritesStore.get().getAllSongs(context)

    override fun containsSong(context: Context, songId: Long): Boolean =
        FavoritesStore.get().containsSong(songId, "")

    override fun removeSong(context: Context, song: Song) {
        Favorite.toggleFavorite(context, song)
    }

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) { } // meaningless

    override fun appendSong(context: Context, song: Song) {
        Favorite.toggleFavorite(context, song)
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) Favorite.toggleFavorite(context, song)
    }

    override fun clear(context: Context) {
        FavoritesStore.get().clearAllSongs()
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
