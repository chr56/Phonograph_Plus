/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import org.koin.core.context.GlobalContext
import player.phonograph.repo.room.dao.FavoritesSongsDao
import player.phonograph.repo.room.dao.MediaStoreSongDao
import player.phonograph.repo.room.dao.MetadataDao
import player.phonograph.repo.room.dao.PinedPlaylistsDao
import player.phonograph.repo.room.dao.PlaylistDao
import player.phonograph.repo.room.dao.PlaylistSongDao
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Metadata
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import java.io.Closeable


@Database(
    entities = [
        MediastoreSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteSongEntity::class,
        PinedPlaylistsEntity::class,
        Metadata::class,
    ],
    version = MusicDatabase.DATABASE_REVISION,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase(), Closeable {
    abstract fun MetadataDao(): MetadataDao
    abstract fun MediaStoreSongDao(): MediaStoreSongDao
    abstract fun PlaylistDao(): PlaylistDao
    abstract fun PlaylistSongDao(): PlaylistSongDao
    abstract fun FavoritesSongsDao(): FavoritesSongsDao
    abstract fun PinedPlaylistsDao(): PinedPlaylistsDao
    override fun close() {
        super.close()
    }

    companion object {
        const val DATABASE_NAME = "music_database_v1.db"
        const val DATABASE_REVISION = 1

        fun instance(context: Context): MusicDatabase =
            Room.databaseBuilder(context, MusicDatabase::class.java, DATABASE_NAME)
                .enableMultiInstanceInvalidation()
                .build()
                .also { db ->
                    RoomSyncProcessor.observeMediastoreForSync(context.applicationContext, db)
                }

        val koinInstance: MusicDatabase get() = GlobalContext.get().get()
    }
}