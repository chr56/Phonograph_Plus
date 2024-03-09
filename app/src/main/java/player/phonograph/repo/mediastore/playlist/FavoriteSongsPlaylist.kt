/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.IFavorite
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.repo.loader.Songs
import androidx.annotation.Keep
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

class FavoriteSongsPlaylistImpl : FavoriteSongsPlaylist {

    val favorite: IFavorite by GlobalContext.get().inject()

    constructor(context: Context) : super(context)


    override fun getSongs(context: Context): List<Song> =
        favorite.allSongs(context)

    override fun containsSong(context: Context, songId: Long): Boolean =
        favorite.isFavorite(context, Songs.id(context, songId))

    override fun removeSong(context: Context, song: Song) {
        favorite.toggleFavorite(context, song)
    }

    override fun moveSong(context: Context, song: Song, from: Int, to: Int) {} // meaningless

    override fun appendSong(context: Context, song: Song) {
        favorite.toggleFavorite(context, song)
        notifyMediaStoreChanged()
    }

    override fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) favorite.toggleFavorite(context, song)
        notifyMediaStoreChanged()
    }

    override fun clear(context: Context) = favorite.clearAll(context)


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
        private fun notifyMediaStoreChanged() = GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()
    }
}
