/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.repo.room.converter.EntityConverter
import android.content.Context

object RoomGenres : RoomLoader(), IGenres {

    override suspend fun all(context: Context): List<Genre> =
        db.GenreQueryDao().all(genreSortMode(context)).map(EntityConverter::toGenreModel)

    override suspend fun id(context: Context, id: Long): Genre? =
        db.GenreQueryDao().id(id)?.let(EntityConverter::toGenreModel)

    override suspend fun searchByName(context: Context, query: String): List<Genre> =
        db.GenreQueryDao().searchByName("%$query%").map(EntityConverter::toGenreModel)

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        db.GenreQueryDao().genreSongs(genreId).map(EntityConverter::toSongModel)

    override suspend fun of(context: Context, songId: Long): List<Genre> =
        db.GenreQueryDao().of(songId).map(EntityConverter::toGenreModel)
}
