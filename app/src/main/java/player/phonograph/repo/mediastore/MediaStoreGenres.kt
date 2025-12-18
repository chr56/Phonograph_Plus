/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.mediastore.intoFirstGenre
import player.phonograph.foundation.mediastore.intoGenres
import player.phonograph.foundation.mediastore.intoSongs
import player.phonograph.foundation.mediastore.mediastoreGenreSortRefKey
import player.phonograph.foundation.mediastore.queryGenre
import player.phonograph.foundation.mediastore.queryGenreSongs
import player.phonograph.foundation.mediastore.querySongGenre
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.model.sort.SortMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.sort
import android.content.Context
import android.provider.MediaStore.Audio.Genres

object MediaStoreGenres : IGenres {

    override suspend fun all(context: Context): List<Genre> =
        queryGenre(
            context
        )?.intoGenres(context)?.sortAll(context) ?: emptyList()

    override suspend fun id(context: Context, id: Long): Genre? =
        queryGenre(
            context,
            selection = "${Genres._ID} == ?",
            selectionArgs = arrayOf(id.toString())
        )?.intoFirstGenre(context)

    override suspend fun of(context: Context, songId: Long): List<Genre> =
        querySongGenre(context, songId)?.intoGenres(context) ?: emptyList()

    override suspend fun songs(context: Context, genreId: Long): List<Song> =
        queryGenreSongs(context, genreId).intoSongs()

    private fun List<Genre>.sortAll(context: Context): List<Genre> =
        sortAll(Setting(context)[Keys.genreSortMode].data)

    private fun List<Genre>.sortAll(sortMode: SortMode): List<Genre> {
        return this.sort(sortMode.revert, mediastoreGenreSortRefKey(sortMode.sortRef))
    }


}