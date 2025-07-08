/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.mediastore.intoFirstSong
import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.internal.locateSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.MediaColumns.DATE_ADDED
import android.provider.MediaStore.MediaColumns.DATE_MODIFIED


object MediaStoreSongs : ISongs {

    override suspend fun all(context: Context): List<Song> =
        querySongs(context).intoSongs()

    override suspend fun id(context: Context, id: Long): Song? =
        querySongs(context, "${AudioColumns._ID} =? ", arrayOf(id.toString())).intoFirstSong()

    override suspend fun path(context: Context, path: String): Song? =
        querySongs(context, "${AudioColumns.DATA} =? ", arrayOf(path)).intoFirstSong()

    override suspend fun artist(context: Context, artistId: Long): List<Song> =
        querySongs(context, "${AudioColumns.ARTIST_ID} =?", arrayOf(artistId.toString())).intoSongs()

    override suspend fun album(context: Context, albumId: Long): List<Song> =
        querySongs(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), AudioColumns.TRACK).intoSongs()
            .sortedBy { it.trackNumber }

    override suspend fun genres(context: Context, genreId: Long): List<Song> = MediaStoreGenres.songs(context, genreId)


    /**
     * @param withoutPathFilter true if disable path filter
     */
    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        querySongs(
            context,
            "${AudioColumns.DATA} LIKE ? ",
            arrayOf(path),
            withoutPathFilter = withoutPathFilter
        ).intoSongs()

    override suspend fun searchByTitle(context: Context, title: String): List<Song> {
        val cursor =
            querySongs(context, "${AudioColumns.TITLE} LIKE ?", arrayOf("%$title%"))
        return cursor.intoSongs()
    }

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> {
        val dateRef = if (useModifiedDate) DATE_MODIFIED else DATE_ADDED
        val cursor =
            querySongs(
                context = context,
                selection = "$dateRef > ?",
                selectionValues = arrayOf(timestamp.toString()),
                sortOrder = "$dateRef DESC"
            )
        return cursor.intoSongs()
    }

    suspend fun search(context: Context, query: String?, title: String?, album: String?, artist: String?): List<Song> {

        val termsCombinations = listOf(
            Triple(title, album, artist),
            Triple(title, album, null),
            Triple(title, null, artist),
            Triple(title, null, null),
            Triple(null, album, artist),
            Triple(null, album, null),
            Triple(null, null, artist)
        )

        for ((titleQuery, albumQuery, artistQuery) in termsCombinations) {
            val results = locateSongs(context, title = titleQuery, album = albumQuery, artist = artistQuery).intoSongs()
            if (results.isNotEmpty()) {
                return results
            }
        }

        if (query != null) {
            val results = locateSongs(context, keyword = query).intoSongs()
            if (results.isNotEmpty()) {
                return results
            }
        }

        return emptyList()
    }

}