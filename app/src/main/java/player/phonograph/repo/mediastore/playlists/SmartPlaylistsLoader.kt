/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import org.koin.core.context.GlobalContext
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.ShuffleAllPlaylist

object SmartPlaylistsLoader {
    val favoriteSongsPlaylist: FavoriteSongsPlaylist get() = GlobalContext.get().get()
    val historyPlaylist: HistoryPlaylist get() = GlobalContext.get().get()
    val lastAddedPlaylist: LastAddedPlaylist get() = GlobalContext.get().get()
    val myTopTracksPlaylist: MyTopTracksPlaylist get() = GlobalContext.get().get()
    val shuffleAllPlaylist: ShuffleAllPlaylist get() = GlobalContext.get().get()
}