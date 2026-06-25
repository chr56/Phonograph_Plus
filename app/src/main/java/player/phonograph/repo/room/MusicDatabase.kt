/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import org.koin.core.context.GlobalContext
import player.phonograph.repo.room.dao.AlbumManipulateDao
import player.phonograph.repo.room.dao.AlbumQueryDao
import player.phonograph.repo.room.dao.ArtistManipulateDao
import player.phonograph.repo.room.dao.ArtistQueryDao
import player.phonograph.repo.room.dao.RelationshipManipulateDao
import player.phonograph.repo.room.dao.RelationshipQueryDao
import player.phonograph.repo.room.dao.FavoriteSongManipulateDao
import player.phonograph.repo.room.dao.FavoriteSongQueryDao
import player.phonograph.repo.room.dao.GenreManipulateDao
import player.phonograph.repo.room.dao.GenreQueryDao
import player.phonograph.repo.room.dao.ImageCacheDao
import player.phonograph.repo.room.dao.MetadataDao
import player.phonograph.repo.room.dao.PinedPlaylistManipulateDao
import player.phonograph.repo.room.dao.PinedPlaylistQueryDao
import player.phonograph.repo.room.dao.PlaylistManipulateDao
import player.phonograph.repo.room.dao.PlaylistQueryDao
import player.phonograph.repo.room.dao.PlaylistSongManipulateDao
import player.phonograph.repo.room.dao.PlaylistSongQueryDao
import player.phonograph.repo.room.dao.SongManipulateDao
import player.phonograph.repo.room.dao.SongQueryDao
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.ImageCacheEntity
import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageGenreAndSong
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Metadata
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
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
        PinedPlaylistsEntity::class,
        ImageCacheEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        LinkageAlbumAndArtist::class,
        LinkageSongAndArtist::class,
        GenreEntity::class,
        LinkageGenreAndSong::class,
        Metadata::class,
    ],
    version = MusicDatabase.DATABASE_REVISION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
abstract class MusicDatabase : RoomDatabase(), Closeable {
    abstract fun MetadataDao(): MetadataDao
    abstract fun SongQueryDao(): SongQueryDao
    abstract fun SongManipulateDao(): SongManipulateDao
    abstract fun PlaylistQueryDao(): PlaylistQueryDao
    abstract fun AlbumQueryDao(): AlbumQueryDao
    abstract fun AlbumManipulateDao(): AlbumManipulateDao
    abstract fun ArtistQueryDao(): ArtistQueryDao
    abstract fun ArtistManipulateDao(): ArtistManipulateDao
    abstract fun GenreQueryDao(): GenreQueryDao
    abstract fun GenreManipulateDao(): GenreManipulateDao
    abstract fun RelationshipQueryDao(): RelationshipQueryDao
    abstract fun RelationshipManipulateDao(): RelationshipManipulateDao
    abstract fun PlaylistManipulateDao(): PlaylistManipulateDao
    abstract fun PlaylistSongQueryDao(): PlaylistSongQueryDao
    abstract fun PlaylistSongManipulateDao(): PlaylistSongManipulateDao
    abstract fun FavoriteSongQueryDao(): FavoriteSongQueryDao
    abstract fun FavoriteSongManipulateDao(): FavoriteSongManipulateDao
    abstract fun PinedPlaylistQueryDao(): PinedPlaylistQueryDao
    abstract fun PinedPlaylistManipulateDao(): PinedPlaylistManipulateDao
    abstract fun ImageCacheDao(): ImageCacheDao
    override fun close() {
        super.close()
    }

    companion object {
        const val DATABASE_NAME = "music_database_v1.db"
        const val DATABASE_REVISION = 3

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
