/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.model.repo.loader.IPinedPlaylists
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.MediaStoreGenres

object Albums : IAlbums by RouterAlbums

object Artists : IArtists by RouterArtists

object Genres : IGenres by MediaStoreGenres

object Playlists : IPlaylists by RouterPlaylists

object Songs : ISongs by RouterSongs

object FavoriteSongs: IFavoriteSongs by RouterFavoriteSongs

object PinedPlaylists: IPinedPlaylists by RouterPinedPlaylists