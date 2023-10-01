/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import org.koin.core.context.GlobalContext
import player.phonograph.repo.room.dao.MediaStoreSongDao
import player.phonograph.repo.room.dao.PlaylistDao
import player.phonograph.repo.room.dao.PlaylistSongDao
import player.phonograph.repo.room.dao.AlbumDao
import player.phonograph.repo.room.dao.ArtistDao
import player.phonograph.repo.room.dao.QueryDao
import player.phonograph.repo.room.dao.RelationShipDao
import player.phonograph.repo.room.dao.SongDao
import player.phonograph.repo.room.entity.FavoritePlaylistEntity
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Metadata
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.SongEntity
import androidx.room.AutoMigration
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
        FavoritePlaylistEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        LinkageAlbumAndArtist::class,
        LinkageSongAndArtist::class,
        SongEntity::class,
        Metadata::class,
    ],
    version = MusicDatabase.DATABASE_REVISION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class MusicDatabase : RoomDatabase(), Closeable {
    abstract fun MediaStoreSongDao(): MediaStoreSongDao
    abstract fun PlaylistDao(): PlaylistDao
    abstract fun PlaylistSongDao(): PlaylistSongDao
    abstract fun SongDao(): SongDao
    abstract fun AlbumDao(): AlbumDao
    abstract fun ArtistDao(): ArtistDao
    abstract fun RelationShipDao(): RelationShipDao
    abstract fun QueryDao(): QueryDao
    override fun close() {
        super.close()
    }

    companion object {
        const val DATABASE_NAME = "music_database_v1.db"
        const val DATABASE_REVISION = 2

        fun instance(context: Context): MusicDatabase =
            Room.databaseBuilder(context, MusicDatabase::class.java, DATABASE_NAME)
                .enableMultiInstanceInvalidation()
                .build()
                .also { db ->
                    DatabaseUtil.syncWithMediastore(context.applicationContext, db)
                }

        val koinInstance: MusicDatabase get() = GlobalContext.get().get()
    }
}