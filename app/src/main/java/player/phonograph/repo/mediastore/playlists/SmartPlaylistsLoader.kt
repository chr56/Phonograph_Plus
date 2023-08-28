/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.playlists

import org.koin.core.context.GlobalContext
import player.phonograph.model.playlist.FavoriteSongsPlaylist

object SmartPlaylistsLoader {
    val favoriteSongsPlaylist: FavoriteSongsPlaylist get() = GlobalContext.get().get()
}