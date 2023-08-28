/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import org.koin.core.context.GlobalContext
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.HistoryPlaylist

object SmartPlaylistsLoader {
    val favoriteSongsPlaylist: FavoriteSongsPlaylist get() = GlobalContext.get().get()
    val historyPlaylist: HistoryPlaylist get() = GlobalContext.get().get()
}