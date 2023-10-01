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
        db.GenreDao().all(genreSortMode(context)).map(EntityConverter::toGenreModel)

    override suspend fun id(context: Context, id: Long): Genre? =
        db.GenreDao().id(id)?.let(EntityConverter::toGenreModel)

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        db.RelationshipGenreSongDao().genre(genreId).mapNotNull { RoomSongs.id(context, it.songId) }

    override suspend fun of(context: Context, songId: Long): List<Genre> =
        db.RelationshipGenreSongDao().song(songId).mapNotNull { id(context, it.genreId) }
}