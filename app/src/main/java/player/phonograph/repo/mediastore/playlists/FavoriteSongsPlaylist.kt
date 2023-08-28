/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import player.phonograph.mechanism.Favorite
import player.phonograph.model.Song
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.repo.database.FavoritesStore
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class FavoriteSongsPlaylistImpl : FavoriteSongsPlaylist {


    constructor(context: Context) : super(context)


    override fun getSongs(context: Context): List<Song> =
        FavoritesStore.get().getAllSongs(context)

    override fun containsSong(context: Context, songId: Long): Boolean =
        FavoritesStore.get().containsSong(songId, "")

    override fun removeSong(context: Context, song: Song) {
        Favorite.toggleFavorite(context, song)
    }

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) {} // meaningless

    override fun appendSong(context: Context, song: Song) {
        Favorite.toggleFavorite(context, song)
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) Favorite.toggleFavorite(context, song)
    }

    override fun clear(context: Context) {
        FavoritesStore.get().clearAllSongs()
    }


    constructor(parcel: Parcel) : super(parcel)
    companion object {
        @Keep
        @JvmField
        val CREATOR: Parcelable.Creator<FavoriteSongsPlaylistImpl?> =
            object : Parcelable.Creator<FavoriteSongsPlaylistImpl?> {
                override fun createFromParcel(source: Parcel): FavoriteSongsPlaylistImpl {
                    return FavoriteSongsPlaylistImpl(source)
                }

                override fun newArray(size: Int): Array<FavoriteSongsPlaylistImpl?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
