/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.repo.mediastore.MediaStoreAlbums
import player.phonograph.repo.mediastore.MediaStoreArtists
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.mediastore.MediaStoreSongs

object Albums : IAlbums by MediaStoreAlbums

object Artists : IArtists by MediaStoreArtists

object Genres : IGenres by MediaStoreGenres

object Playlists : IPlaylists by RouterPlaylists

object Songs : ISongs by MediaStoreSongs