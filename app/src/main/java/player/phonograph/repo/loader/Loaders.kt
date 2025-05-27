/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.model.repo.loader.IFavoritePlaylists
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.model.repo.loader.IGenres
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.MediaStoreAlbums
import player.phonograph.repo.mediastore.MediaStoreArtists
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.mediastore.MediaStoreSongs

object Albums : IAlbums by MediaStoreAlbums

object Artists : IArtists by MediaStoreArtists

object Genres : IGenres by MediaStoreGenres

object Playlists : IPlaylists by RouterPlaylists

object Songs : ISongs by MediaStoreSongs

object FavoriteSongs: IFavoriteSongs by RouterFavoriteSongs

object FavoritePlaylists: IFavoritePlaylists by RouterFavoritePlaylists