/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import org.koin.dsl.module
import player.phonograph.mechanism.FavoriteDatabaseImpl
import player.phonograph.mechanism.FavoritePlaylistImpl
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.ShuffleAllPlaylist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.MusicPlaybackQueueStore
import player.phonograph.repo.database.PathFilterStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import player.phonograph.repo.mediastore.playlist.FavoriteSongsPlaylistImpl
import player.phonograph.repo.mediastore.playlist.FilePlaylistImpl
import player.phonograph.repo.mediastore.playlist.HistoryPlaylistImpl
import player.phonograph.repo.mediastore.playlist.LastAddedPlaylistImpl
import player.phonograph.repo.mediastore.playlist.MyTopTracksPlaylistImpl
import player.phonograph.repo.mediastore.playlist.ShuffleAllPlaylistImpl
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Setting
import android.content.Context
import android.os.Parcel

val moduleStatus = module {
    single { QueueManager(get()) }
    single { MediaStoreTracker(get()) }
}

val moduleLoaders = module {
    single { PathFilterStore(get()) }
    single { HistoryStore(get()) }
    single { SongPlayCountStore(get()) }
    single { FavoritesStore(get()) }
    single { MusicPlaybackQueueStore(get()) }

    factory {
        if (Setting.instance.useLegacyFavoritePlaylistImpl) FavoritePlaylistImpl() else FavoriteDatabaseImpl()
    }

    factory { TopTracksLoader(get()) }
    factory { RecentlyPlayedTracksLoader(get()) }

    factory<FavoriteSongsPlaylist> { FavoriteSongsPlaylistImpl(get<Context>()) }
    factory<HistoryPlaylist> { HistoryPlaylistImpl(get<Context>()) }
    factory<LastAddedPlaylist> { LastAddedPlaylistImpl(get<Context>()) }
    factory<MyTopTracksPlaylist> { MyTopTracksPlaylistImpl(get<Context>()) }
    factory<ShuffleAllPlaylist> { ShuffleAllPlaylistImpl(get<Context>()) }

    // factory<FilePlaylist> { (id: Long, name: String?, path: String, dateAdded: Long, dateModified: Long) ->
    //     FilePlaylistImpl(id, name, path, dateAdded, dateModified)
    // }
    factory<FilePlaylist> { (parcel: Parcel) -> FilePlaylistImpl(parcel) }
}