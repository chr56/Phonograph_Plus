/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.MediaStoreSongDao
import player.phonograph.repo.room.entity.FavoriteSongEntity
import androidx.room.withTransaction
import android.content.Context

object RoomFavoriteSongs : RoomLoader(), IFavoriteSongs {

    override suspend fun all(context: Context): List<Song> {
        val entities = db.FavoritesSongsDao().all()
        return entities.mapNotNull { locate(it) }
    }

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        db.FavoritesSongsDao().contains(song.id, song.data)

    override suspend fun add(context: Context, song: Song): Boolean =
        db.FavoritesSongsDao().add(store(song)) > 0

    override suspend fun add(context: Context, songs: List<Song>): Boolean =
        !db.FavoritesSongsDao().add(songs.map(::store)).contains(-1)

    override suspend fun remove(context: Context, song: Song): Boolean {
        return db.FavoritesSongsDao().remove(song.id, song.data) > 0
    }

    override suspend fun toggleState(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) {
            !remove(context, song)
        } else {
            add(context, song)
        }

    override suspend fun cleanMissing(context: Context): Boolean {
        val favoritesSongsDao = db.FavoritesSongsDao()
        val songDao = db.MediaStoreSongDao()
        db.withTransaction {
            for (song in favoritesSongsDao.all()) {
                if (locate(songDao, song.mediastoreId, song.path) == null) {
                    favoritesSongsDao.remove(song.mediastoreId, song.path)
                }
            }
        }
        return true
    }

    override suspend fun clearAll(context: Context): Boolean =
        db.FavoritesSongsDao().purge() > 0

    private fun store(song: Song, timestamp: Long = System.currentTimeMillis()): FavoriteSongEntity =
        FavoriteSongEntity(
            mediastoreId = song.id,
            path = song.data,
            title = song.title,
            date = timestamp
        )

    private suspend fun locate(entity: FavoriteSongEntity): Song? =
        locate(db.MediaStoreSongDao(), entity.mediastoreId, entity.path)

    private suspend fun locate(dao: MediaStoreSongDao, mediastoreId: Long, path: String): Song? {
        val result = dao.id(mediastoreId) ?: dao.path(path)
        return result?.let(EntityConverter::toSongModel)
    }
}