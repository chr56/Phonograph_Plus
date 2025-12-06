/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.repo.loader.IArtists
import player.phonograph.repo.mediastore.MediaStoreArtists

/**
 * Endpoint for accessing artists
 */
object Artists : IArtists by MediaStoreArtists