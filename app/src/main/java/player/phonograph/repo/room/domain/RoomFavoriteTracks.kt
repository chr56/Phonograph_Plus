/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.IFavoriteTracks
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.SongQueryDao
import player.phonograph.repo.room.entity.FavoriteSongEntity
import androidx.room.withTransaction
import android.content.Context

object RoomFavoriteTracks : RoomLoader(), IFavoriteTracks {

    override suspend fun all(context: Context): List<Song> {
        val entities = db.FavoriteSongQueryDao().all()
        return entities.mapNotNull { locate(it) }
    }

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        db.FavoriteSongQueryDao().contains(song.id, song.data)

    override suspend fun add(context: Context, song: Song): Boolean =
        db.FavoriteSongManipulateDao().add(store(song)) > 0

    override suspend fun add(context: Context, songs: List<Song>): Boolean =
        !db.FavoriteSongManipulateDao().add(songs.map(::store)).contains(-1)

    override suspend fun remove(context: Context, song: Song): Boolean {
        return db.FavoriteSongManipulateDao().remove(song.id, song.data) > 0
    }

    override suspend fun toggleState(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) {
            !remove(context, song)
        } else {
            add(context, song)
        }

    override suspend fun cleanMissing(context: Context): Boolean {
        val favoritesQueryDao = db.FavoriteSongQueryDao()
        val favoritesManipulateDao = db.FavoriteSongManipulateDao()
        val songDao = db.SongQueryDao()
        db.withTransaction {
            for (song in favoritesQueryDao.all()) {
                if (locate(songDao, song.mediastoreId, song.path) == null) {
                    favoritesManipulateDao.remove(song.mediastoreId, song.path)
                }
            }
        }
        return true
    }

    override suspend fun clearAll(context: Context): Boolean =
        db.FavoriteSongManipulateDao().purge() > 0

    private fun store(song: Song, timestamp: Long = System.currentTimeMillis()): FavoriteSongEntity =
        FavoriteSongEntity(
            mediastoreId = song.id,
            path = song.data,
            title = song.title,
            date = timestamp
        )

    private suspend fun locate(entity: FavoriteSongEntity): Song? =
        locate(db.SongQueryDao(), entity.mediastoreId, entity.path)

    private suspend fun locate(dao: SongQueryDao, mediastoreId: Long, path: String): Song? {
        val result = dao.id(mediastoreId) ?: dao.path(path)
        return result?.let(EntityConverter::toSongModel)
    }
}
