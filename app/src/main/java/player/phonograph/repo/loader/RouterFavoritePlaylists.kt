/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IFavoritePlaylists
import player.phonograph.repo.database.loaders.DatabaseFavoritePlaylistLoader

object RouterFavoritePlaylists : IFavoritePlaylists by DatabaseFavoritePlaylistLoader()