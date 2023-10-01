/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.room.loader.RoomAlbums
import player.phonograph.repo.room.loader.RoomArtists
import player.phonograph.repo.room.loader.RoomSongs

object Albums : IAlbums by RoomAlbums

object Artists : IArtists by RoomArtists

object Genres : IGenres by MediaStoreGenres

object Songs : ISongs by RoomSongs