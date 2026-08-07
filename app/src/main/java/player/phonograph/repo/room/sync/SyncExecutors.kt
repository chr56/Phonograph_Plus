/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import player.phonograph.mechanism.metadata.RelationshipResolver
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.SyncReport
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import androidx.room.withTransaction
import android.content.Context

object SyncExecutors {

    /**
     * Obtain the correct [SyncExecutor] based on Settings
     */
    suspend fun obtain(context: Context, musicDatabase: MusicDatabase): SyncExecutor {
        val backend = Settings(context)[Keys.musicLibraryBackend].read()
        val syncExecutor = when {
            backend.syncBasicDatabase -> BasicSyncExecutor(musicDatabase)
            else                      -> FullSyncExecutor(
                musicDatabase,
                withGenres = backend.syncWithGenres,
                countComposerAsArtist = backend.regardComposerAsArtist
            )
        }
        return syncExecutor
    }

    /**
     * Simple [SyncExecutor], not relationship solving
     */
    class BasicSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

        override suspend fun check(context: Context): Boolean = defaultCheck(context, musicDatabase)

        override suspend fun sync(
            context: Context,
            channel: ProgressConnection?,
        ): SyncReport {
            val songsMediastore = MediaStoreSongs.all(context)
            val total = songsMediastore.size
            channel?.onProcessUpdate(0, total)
            val songManipulateDao = musicDatabase.SongManipulateDao()
            musicDatabase.withTransaction {
                songManipulateDao.deleteAll()
                songManipulateDao.update(songsMediastore.map(EntityConverter::fromSongModel))
            }
            channel?.onProcessUpdate(total, total)
            return SyncReport(success = true, modified = total)
        }

    }

    /**
     * [SyncExecutor] with complex relationship solving
     */
    class FullSyncExecutor(
        private val musicDatabase: MusicDatabase,
        private val withGenres: Boolean = true,
        private val countComposerAsArtist: Boolean = true,
    ) : SyncExecutor {

        override suspend fun check(context: Context): Boolean = SyncExecutors.defaultCheck(context, musicDatabase) ||
                (musicDatabase.ArtistQueryDao().count() == 0) || (musicDatabase.AlbumQueryDao().count() == 0)

        override suspend fun sync(
            context: Context,
            channel: ProgressConnection?,
        ): SyncReport {
            val session = RelationshipSyncExecutiveKernel(
                musicDatabase = DefaultDatabaseDataSink(musicDatabase),
                musicDataSource = MediaStoreDataSource(context),
                relationshipResolver = RelationshipResolver.fromSettings(context),
                withGenres = withGenres,
                countComposerAsArtist = countComposerAsArtist,
                channel = channel,
            )
            return session.execute()
        }
    }

    /**
     * default function to check database sync status;
     * used internally
     */
    suspend fun defaultCheck(context: Context, musicDatabase: MusicDatabase): Boolean {
        val songsCountMediastore = MediaStoreSongs.total(context)
        val latestMediastore = MediaStoreSongs.lastest(context)

        val songsCountDatabase = musicDatabase.SongQueryDao().total()
        val latestDatabase = musicDatabase.SongQueryDao().latest()

        return if (songsCountMediastore != songsCountDatabase || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }
    }

    class DefaultDatabaseDataSink(private val musicDatabase: MusicDatabase) : MusicDatabaseDataSink {
        override fun SongQueryDao() = musicDatabase.SongQueryDao()
        override fun SongManipulateDao() = musicDatabase.SongManipulateDao()
        override fun AlbumQueryDao() = musicDatabase.AlbumQueryDao()
        override fun AlbumManipulateDao() = musicDatabase.AlbumManipulateDao()
        override fun ArtistQueryDao() = musicDatabase.ArtistQueryDao()
        override fun ArtistManipulateDao() = musicDatabase.ArtistManipulateDao()
        override fun GenreQueryDao() = musicDatabase.GenreQueryDao()
        override fun GenreManipulateDao() = musicDatabase.GenreManipulateDao()
        override fun RelationshipQueryDao() = musicDatabase.RelationshipQueryDao()
        override fun RelationshipManipulateDao() = musicDatabase.RelationshipManipulateDao()
        override suspend fun <R> withTransaction(block: suspend () -> R): R =
            musicDatabase.withTransaction(block)
    }
}
