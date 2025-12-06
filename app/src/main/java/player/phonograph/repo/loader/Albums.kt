/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.mediastore.MediaStoreAlbums

/**
 * Endpoint for accessing albums
 */
object Albums : IAlbums by MediaStoreAlbums