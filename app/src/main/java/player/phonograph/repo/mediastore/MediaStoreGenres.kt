/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.internal.BASE_AUDIO_SELECTION
import player.phonograph.repo.mediastore.internal.BASE_SONG_PROJECTION
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriGenreMembers
import player.phonograph.util.mediastoreUriGenres
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore.Audio.Genres

object MediaStoreGenres : IGenres {

    override suspend fun all(context: Context): List<Genre> =
        queryGenre(context)?.intoGenres(context)?.sortAll(context) ?: emptyList()

    override suspend fun id(context: Context, id: Long): Genre? =
        queryGenre(context, id)?.intoGenres(context)?.first()

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        querySongs(context, genreId).intoSongs()

    private fun queryGenre(context: Context): Cursor? =
        queryGenreImpl(context, null, null)

    private fun queryGenre(context: Context, genreId: Long): Cursor? =
        queryGenreImpl(context, "${Genres._ID} == ?", arrayOf(genreId.toString()))

    private fun queryGenreImpl(context: Context, selection: String?, selectionArgs: Array<String>?): Cursor? =
        context.contentResolver.query(
            mediastoreUriGenres(MEDIASTORE_VOLUME_EXTERNAL),
            arrayOf(Genres._ID, Genres.NAME),
            selection,
            selectionArgs,
            null
        )

    private fun querySongs(context: Context, genreId: Long): Cursor? =
        context.contentResolver.query(
            mediastoreUriGenreMembers(MEDIASTORE_VOLUME_EXTERNAL, genreId),
            BASE_SONG_PROJECTION,
            BASE_AUDIO_SELECTION,
            null,
            null
        )

    private fun Cursor.intoGenres(context: Context): List<Genre> = this.use {
        val genres = mutableListOf<Genre>()
        if (moveToFirst()) {
            do {
                val id = getLong(0)
                val name = getString(1)
                val count = querySongs(context, id)?.use { count } ?: 0

                if (count > 0) {
                    genres.add(Genre(id = id, name = name, songCount = count))
                } else {
                    removeEmptyGenre(context, id)
                }
            } while (moveToNext())
        }
        genres
    }

    private fun removeEmptyGenre(context: Context, genreId: Long) {
        // try to remove the empty genre from the media store
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.delete(
                    mediastoreUriGenres(MEDIASTORE_VOLUME_EXTERNAL),
                    "${Genres._ID} == $genreId",
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun List<Genre>.sortAll(context: Context): List<Genre> {
        val sortMode = Setting(context)[Keys.genreSortMode].data
        val revert = sortMode.revert
        return when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME -> this.sort(revert) { it.name }
            SortRef.SONG_COUNT   -> this.sort(revert) { it.songCount }
            else                 -> this
        }
    }

    private inline fun List<Genre>.sort(revert: Boolean, crossinline selector: (Genre) -> Comparable<*>?): List<Genre> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }


}