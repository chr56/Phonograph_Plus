/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.model.Artist
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.mediastore.internal.generateArtists
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object MediaStoreArtists : IArtists {

    override suspend fun all(context: Context): List<Artist> {
        val songs = MediaStoreSongs.querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) emptyList()
        else generateArtists(context, songs, sortMode = Settings(context)[Keys.artistSortMode].read())
    }

    override suspend fun all(context: Context, sortMode: SortMode): List<Artist> {
        val songs = MediaStoreSongs.querySongs(context, sortOrder = null).intoSongs()
        return if (songs.isEmpty()) emptyList() else generateArtists(context, songs, sortMode)
    }

    override suspend fun id(context: Context, id: Long): Artist {
        val songs = MediaStoreSongs.artist(context, id)
        val albums = MediaStoreAlbums.artist(context, id)
        return Artist(id, songs.firstOrNull()?.artistName ?: Artist.UNKNOWN_ARTIST_DISPLAY_NAME, albums.size, songs.size)
    }

    override suspend fun searchByName(context: Context, query: String): List<Artist> {
        val songs = MediaStoreSongs.querySongs(context, "${AudioColumns.ARTIST} LIKE ?", arrayOf("%$query%"), null).intoSongs()
        return if (songs.isEmpty()) emptyList()
        else generateArtists(context, songs, sortMode = Settings(context)[Keys.artistSortMode].read())
    }

}