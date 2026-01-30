/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.error.record
import player.phonograph.foundation.mediastore.intoFirstSong
import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.foundation.mediastore.queryMediastoreAudio
import player.phonograph.foundation.mediastore.withBaseAudioFilter
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.internal.defaultSongQuerySortOrder
import player.phonograph.repo.mediastore.internal.withPathFilter
import android.content.Context
import android.database.Cursor
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

    override suspend fun lastest(context: Context): Song? {
        val cursor = querySongs(context = context, sortOrder = "$DATE_MODIFIED DESC")
        return cursor.intoFirstSong()
    }

    override suspend fun total(context: Context): Int = querySongs(context = context)?.count ?: 0

    /**
     * Raw query songs with path filter
     * @param selection SQL where clause
     * @param selectionValues SQL where clause binding values
     * @param sortOrder SQL where clause sorting by
     * @param withoutPathFilter true if bypass path filter
     * @param extended true if using extended projection
     * @return cursor of queried songs for more fields
     */
    suspend fun querySongs(
        context: Context,
        selection: String = "",
        selectionValues: Array<String> = emptyArray(),
        sortOrder: String? = defaultSongQuerySortOrder(context),
        withoutPathFilter: Boolean = false,
        extended: Boolean = false,
    ): Cursor? = try {
        withPathFilter(
            context,
            selection = withBaseAudioFilter { selection },
            selectionValues = selectionValues,
            escape = withoutPathFilter
        ) { actualSelection, actualSelectionValues ->
            queryMediastoreAudio(
                context,
                selection = actualSelection,
                selectionArgs = actualSelectionValues,
                sortOrder = sortOrder,
                extended = extended,
            )
        }
    } catch (_: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        record(context, e, "QueryMediastore")
        null
    }


    /**
     * Search Songs
     */
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

    /**
     * search songs by keywords for tittle, artist, album fields
     */
    private suspend fun locateSongs(context: Context, title: String?, album: String?, artist: String?) = run {
        val selections = mutableListOf<String>()
        val selectionValues = mutableListOf<String>()

        if (artist != null) {
            selections.add(ARTIST_SELECTION)
            selectionValues.add(artist.lowercase().trim())
        }

        if (album != null) {
            selections.add(ALBUM_SELECTION)
            selectionValues.add(album.lowercase().trim())
        }

        if (title != null) {
            selections.add(TITLE_SELECTION)
            selectionValues.add(title.lowercase().trim())
        }

        if (selections.isNotEmpty()) {
            querySongs(context, selections.joinToString(separator = AND), selectionValues.toTypedArray())
        } else {
            null
        }
    }

    /**
     * search songs by keyword for tittle, artist, album fields
     * @param keyword keyword
     */
    private suspend fun locateSongs(
        context: Context,
        keyword: String,
    ) = run {
        for (selection in listOf(TITLE_SELECTION, ALBUM_SELECTION, ARTIST_SELECTION)) {
            val cursor = querySongs(context, selection, arrayOf(keyword.trim()))
            if (cursor != null) return@run cursor
        }
        null
    }

    private const val TITLE_SELECTION = "lower(${AudioColumns.TITLE}) LIKE ?"
    private const val ALBUM_SELECTION = "lower(${AudioColumns.ALBUM}) LIKE ?"
    private const val ARTIST_SELECTION = "lower(${AudioColumns.ARTIST}) LIKE ?"
    private const val AND = " AND "


}