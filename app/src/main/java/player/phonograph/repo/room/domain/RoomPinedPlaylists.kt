/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.repo.loader.IPinedPlaylists
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.PinedPlaylistsEntity.Companion.TYPE_DATABASE_PLAYLIST
import player.phonograph.repo.room.entity.PinedPlaylistsEntity.Companion.TYPE_FILE_PLAYLIST
import android.content.Context

object RoomPinedPlaylists : RoomLoader(), IPinedPlaylists {

    override suspend fun all(context: Context): List<Playlist> {
        val entities = db.PinedPlaylistsDao().all()
        return entities.mapNotNull { locate(context, it) }
    }

    override suspend fun isPined(
        context: Context,
        playlist: Playlist,
    ): Boolean {
        val dao = db.PinedPlaylistsDao()
        return when (val location = playlist.location) {

            is FilePlaylistLocation     -> dao.contains(
                TYPE_FILE_PLAYLIST,
                location.mediastoreId,
                location.path
            )

            is DatabasePlaylistLocation -> dao.contains(
                TYPE_DATABASE_PLAYLIST,
                location.databaseId,
                location.databaseId.toString()
            )

            is VirtualPlaylistLocation  -> false
        }
    }

    override suspend fun isPined(
        context: Context,
        id: Long?,
        path: String?,
    ): Boolean = db.PinedPlaylistsDao().contains(TYPE_FILE_PLAYLIST, id ?: 0, path ?: "")

    override suspend fun add(context: Context, playlist: Playlist): Boolean =
        db.PinedPlaylistsDao().add(store(playlist)) > 0

    override suspend fun add(context: Context, playlists: List<Playlist>): Boolean =
        !db.PinedPlaylistsDao().add(playlists.map(::store)).contains(-1)

    override suspend fun remove(
        context: Context,
        playlist: Playlist,
    ): Boolean {
        val dao = db.PinedPlaylistsDao()
        return when (val location = playlist.location) {

            is FilePlaylistLocation     ->
                dao.remove(TYPE_FILE_PLAYLIST, location.mediastoreId, location.path) > 0

            is DatabasePlaylistLocation ->
                dao.remove(TYPE_DATABASE_PLAYLIST, location.databaseId, location.databaseId.toString()) > 0

            is VirtualPlaylistLocation  -> false
        }
    }

    override suspend fun toggleState(
        context: Context,
        playlist: Playlist,
    ): Boolean =
        if (isPined(context, playlist)) {
            !remove(context, playlist)
        } else {
            add(context, playlist)
        }

    override suspend fun clearAll(context: Context): Boolean =
        db.PinedPlaylistsDao().purge() > 0

    private fun store(playlist: Playlist, timestamp: Long = System.currentTimeMillis()): PinedPlaylistsEntity =
        when (val location = playlist.location) {
            is FilePlaylistLocation     -> PinedPlaylistsEntity(
                id = location.id(),
                type = TYPE_DATABASE_PLAYLIST,
                sub = location.mediastoreId,
                data = location.path,
                title = playlist.name,
                date = timestamp
            )

            is DatabasePlaylistLocation -> PinedPlaylistsEntity(
                id = location.id(),
                type = TYPE_FILE_PLAYLIST,
                sub = location.databaseId,
                data = location.databaseId.toString(),
                title = playlist.name,
                date = timestamp
            )

            else                        -> throw IllegalArgumentException("Unsupported Playlist $playlist.location")
        }

    private suspend fun locate(context: Context, entity: PinedPlaylistsEntity): Playlist? = when (entity.type) {

        TYPE_FILE_PLAYLIST     ->
            MediaStorePlaylists.id(context, entity.sub) ?: MediaStorePlaylists.searchByPath(context, entity.data)

        TYPE_DATABASE_PLAYLIST ->
            db.PlaylistDao().id(entity.sub)?.let(EntityConverter::toPlaylist)

        else                   -> null
    }
}